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

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.KapuaNamedEntity;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.storage.KapuaNamedEntityRepository;
import org.eclipse.kapua.storage.TxContext;

public class KapuaNamedEntityInMemoryRepository<E extends KapuaNamedEntity>
        extends KapuaUpdatableEntityInMemoryRepository<E>
        implements KapuaNamedEntityRepository<E> {

    public KapuaNamedEntityInMemoryRepository(Class<E> clazz, Supplier<KapuaListResult<E>> listSupplier,
            KapuaQueryConverter kapuaQueryConverter,
            Map<String, Function<E, Object>> fieldPluckers) {
        super(clazz, listSupplier, kapuaQueryConverter, fieldPluckers);
    }

    @Override
    public Optional<E> findByName(TxContext txContext, String value) {
        if (value == null) {
            return Optional.empty();
        }
        return this.entities
                .stream()
                .filter(e -> value.equals(e.getName()))
                .findFirst();
    }

    @Override
    public Optional<E> findByName(TxContext txContext, KapuaId scopeId, String value) {
        if (value == null) {
            return Optional.empty();
        }
        return this.entities
                .stream()
                .filter(e -> value.equals(e.getName()))
                .filter(e -> scopeId == null || KapuaId.ANY.equals(scopeId) ? true : scopeId.equals(e.getScopeId()))
                .findFirst();
    }

    @Override
    public long countOtherEntitiesWithNameInScope(TxContext tx, KapuaId scopeId, KapuaId excludedId, String name) throws KapuaException {
        //TODO: implement
        return 0;
    }

    @Override
    public long countEntitiesWithNameInScope(TxContext tx, KapuaId scopeId, String name) throws KapuaException {
        //TODO: implement
        return 0;
    }

    @Override
    public long countEntitiesWithName(TxContext tx, String name) throws KapuaException {
        //TODO: implement
        return 0;
    }
}
