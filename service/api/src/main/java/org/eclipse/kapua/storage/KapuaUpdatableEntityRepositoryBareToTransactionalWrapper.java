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
import org.eclipse.kapua.model.query.KapuaListResult;

public class KapuaUpdatableEntityRepositoryBareToTransactionalWrapper<E extends KapuaUpdatableEntity, L extends KapuaListResult<E>>
        extends KapuaEntityRepositoryBareToTransactionalWrapper<E, L>
        implements KapuaUpdatableEntityTransactedRepository<E, L> {

    protected final KapuaUpdatableEntityBareRepository<E, L> updatableEntityBareRepository;

    public KapuaUpdatableEntityRepositoryBareToTransactionalWrapper(TxManager txManager, KapuaUpdatableEntityBareRepository<E, L> bareRepository) {
        super(txManager, bareRepository);
        this.updatableEntityBareRepository = bareRepository;
    }

    @Override
    public E update(E entity) throws KapuaException {
        return txManager.executeWithResult(txHolder -> updatableEntityBareRepository.update(txHolder, entity));
    }
}
