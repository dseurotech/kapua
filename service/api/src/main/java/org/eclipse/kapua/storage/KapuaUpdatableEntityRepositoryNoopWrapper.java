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
package org.eclipse.kapua.storage;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.KapuaUpdatableEntity;

/**
 * This utility class is provided as syntactic sugar for classes that need to wrap around a {@link KapuaUpdatableEntityRepository}, decorating it with additional functionalities. This way your wrapper
 * only needs to override significant methods, avoiding boilerplate clutter.
 *
 * @param <E>
 *         The specific subclass of {@link KapuaUpdatableEntity} handled by this repository
 * @since 2.0.0
 */

public abstract class KapuaUpdatableEntityRepositoryNoopWrapper<E extends KapuaUpdatableEntity>
        extends KapuaEntityRepositoryNoopWrapper<E>
        implements KapuaUpdatableEntityRepository<E> {

    protected final KapuaUpdatableEntityRepository<E> wrapped;

    public KapuaUpdatableEntityRepositoryNoopWrapper(KapuaUpdatableEntityRepository<E> wrapped) {
        super(wrapped);
        this.wrapped = wrapped;
    }

    @Override
    public E update(TxContext txContext, E entity) throws KapuaException {
        return wrapped.update(txContext, entity);
    }

    @Override
    public E update(TxContext txContext, E currentEntity, E updatedEntity) {
        return wrapped.update(txContext, currentEntity, updatedEntity);
    }
}
