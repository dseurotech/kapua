/*******************************************************************************
 * Copyright (c) 2016, 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.commons.storage.memory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.KapuaEntity;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.id.KapuaIdImpl;
import org.eclipse.kapua.model.query.FieldSortCriteria;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.storage.KapuaEntityRepository;
import org.eclipse.kapua.storage.TxContext;

public class KapuaEntityInMemoryRepository<E extends KapuaEntity> implements KapuaEntityRepository<E> {

    protected final List<E> entities;
    protected final Class<E> clazz;
    protected final Supplier<KapuaListResult<E>> listSupplier;
    protected final KapuaQueryConverter kapuaQueryConverter;
    protected final Map<String, Function<E, Object>> pluckers;

    public KapuaEntityInMemoryRepository(Class<E> clazz,
            Supplier<KapuaListResult<E>> listSupplier,
            KapuaQueryConverter kapuaQueryConverter,
            Map<String, Function<E, Object>> fieldPluckers) {
        this.clazz = clazz;
        this.listSupplier = listSupplier;
        this.kapuaQueryConverter = kapuaQueryConverter;
        this.pluckers = fieldPluckers;
        this.entities = new ArrayList<>();
    }

    @Override
    public E create(TxContext txContext, E entity) throws KapuaException {
        if (entity.getId() == null) {
            entity.setId(new KapuaIdImpl(BigInteger.valueOf(RandomUtils.nextLong(2, Long.MAX_VALUE))));
        }
        this.entities.add(entity);
        return entity;
    }

    @Override
    public Optional<E> find(TxContext txContext, KapuaId scopeId, KapuaId entityId) {
        return doFind(scopeId, entityId);
    }

    protected Optional<E> doFind(KapuaId scopeId, KapuaId entityId) {
        return this.entities
                .stream()
                .filter(e -> scopeId == null || KapuaId.ANY.equals(scopeId) ? true : scopeId.equals(e.getScopeId()))
                .filter(e -> entityId == null ? true : entityId.equals(e.getId()))
                .findFirst();
    }

    @Override
    public KapuaListResult<E> query(TxContext txContext, KapuaQuery kapuaQuery) throws KapuaException {
        final KapuaListResult<E> res = this.listSupplier.get();
        final Predicate<E> scopePredicate = extractScopePredicate(kapuaQuery);
        final Predicate<E> queryPredicate = Optional.ofNullable(kapuaQuery.getPredicate())
                .map(p -> kapuaQueryConverter.convert(p, pluckers))
                .orElse(e -> true);
        res.addItems(this.entities
                .stream()
                .filter(e -> scopePredicate.test(e) && queryPredicate.test(e))
                .skip(Optional.ofNullable(kapuaQuery.getOffset()).orElse(0))
                .limit(Optional.ofNullable(kapuaQuery.getLimit()).orElse(this.entities.size()))
                .sorted(createComparator(kapuaQuery))
                .collect(Collectors.toList()));
        return res;
    }

    @Override
    public long count(TxContext txContext, KapuaQuery kapuaQuery) throws KapuaException {
        final Predicate<E> scopePredicate = extractScopePredicate(kapuaQuery);
        final Predicate<E> queryPredicate = Optional.ofNullable(kapuaQuery.getPredicate())
                .map(p -> kapuaQueryConverter.convert(p, pluckers))
                .orElse(e -> true);
        return this.entities
                .stream()
                .filter(e -> scopePredicate.test(e) && queryPredicate.test(e))
                .count();
    }

    @Override
    public E delete(TxContext txContext, KapuaId scopeId, KapuaId entityId) throws KapuaException {
        final Optional<E> toDelete = doFind(scopeId, entityId);
        if (!toDelete.isPresent()) {
            throw new KapuaEntityNotFoundException(clazz.getSimpleName(), entityId);
        }
        toDelete.ifPresent(e -> this.entities.remove(e));
        return null;
    }

    @Override
    public E delete(TxContext txContext, E entityToDelete) {
        if (this.entities.remove(entityToDelete)) {
            return entityToDelete;
        }
        return null;
    }

    private Predicate<E> extractScopePredicate(KapuaQuery kapuaQuery) {
        return Optional.ofNullable(kapuaQuery.getScopeId())
                .filter(scopeId -> !KapuaId.ANY.equals(scopeId))
                .map(scopeId -> {
                    final Predicate<E> predicate = entity -> scopeId.equals(entity.getScopeId());
                    return predicate;
                })
                .orElse(e -> true);
    }

    private Comparator<E> createComparator(KapuaQuery kapuaQuery) {
        return Optional.ofNullable(
                        Optional.ofNullable(kapuaQuery.getSortCriteria())
                                .orElse(kapuaQuery.getDefaultSortCriteria())
                )
                .filter(sc -> sc instanceof FieldSortCriteria)
                .map(sc -> (FieldSortCriteria) sc)
                .flatMap(fsc -> Optional.ofNullable(
                        pluckers.get(fsc.getAttributeName())
                ).map(plucker -> {
                    final Function<E, Comparator> entitySpecificComparator = (E entity) -> {
                        final Object lhsField = plucker.apply(entity);
                        if (lhsField instanceof Comparable) {
                            Function<E, ? extends Comparable> comparablePlucker = (E e) -> (Comparable) plucker.apply(e);
                            return Comparator.comparing(comparablePlucker);
                        }
                        return Comparator.naturalOrder();
                    };
                    final Comparator<E> comparator = (E lhs, E rhs) -> entitySpecificComparator.apply(lhs).compare(lhs, rhs);
                    switch (fsc.getSortOrder()) {
                    default:
                    case ASCENDING:
                        return comparator;
                    case DESCENDING:
                        return comparator.reversed();
                    }
                }))
                .orElse((E lhs, E rhs) -> 0);
    }

}
