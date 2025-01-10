/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.commons.jpa;

import java.util.function.Supplier;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.model.AbstractKapuaUpdatableEntity;
import org.eclipse.kapua.model.KapuaUpdatableEntity;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.storage.KapuaUpdatableEntityRepository;
import org.eclipse.kapua.storage.TxContext;

public class KapuaUpdatableEntityImplJpaRepository<E extends KapuaUpdatableEntity>
        extends KapuaEntityImplJpaRepository<E>
        implements KapuaUpdatableEntityRepository<E> {

    public KapuaUpdatableEntityImplJpaRepository(
            Class<E> concreteClass,
            String entityName,
            Supplier<KapuaListResult<E>> listSupplier,
            KapuaJpaRepositoryConfiguration configuration) {
        super(concreteClass, entityName, listSupplier, configuration);
    }

    @Override
    public E update(TxContext txContext, E updatedEntity) throws KapuaException {
        final javax.persistence.EntityManager em = JpaAwareTxContext.extractEntityManager(txContext);
        // Checking existence
        return doFind(em, updatedEntity.getScopeId(), updatedEntity.getId())
                // Updating if present
                .map(currentEntity -> doUpdate(em, currentEntity, updatedEntity))
                .orElseThrow(() -> new KapuaEntityNotFoundException(entityName, updatedEntity.getId()));
    }

    @Override
    public E update(TxContext txContext, E currentEntity, E updatedEntity) {
        final javax.persistence.EntityManager em = JpaAwareTxContext.extractEntityManager(txContext);
        return doUpdate(em, currentEntity, updatedEntity);
    }

    protected E doUpdate(javax.persistence.EntityManager em, E currentEntity, E updatedEntity) {
        if (updatedEntity instanceof AbstractKapuaUpdatableEntity) {
            AbstractKapuaUpdatableEntity updatableEntity = (AbstractKapuaUpdatableEntity) updatedEntity;
            updatableEntity.setCreatedOn(currentEntity.getCreatedOn());
            updatableEntity.setCreatedBy(currentEntity.getCreatedBy());
        }
        em.merge(updatedEntity);
        em.flush();
        if (em.contains(currentEntity)) {
            em.refresh(currentEntity);
            return currentEntity;
        }
        return updatedEntity;
    }
}
