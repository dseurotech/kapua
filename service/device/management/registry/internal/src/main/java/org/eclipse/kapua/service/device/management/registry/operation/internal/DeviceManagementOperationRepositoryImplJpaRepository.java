/*******************************************************************************
 * Copyright (c) 2019, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.device.management.registry.operation.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.jpa.JpaAwareTxContext;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.commons.jpa.KapuaUpdatableEntityJpaRepository;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperation;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationListResult;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationRepository;
import org.eclipse.kapua.storage.TxContext;

import javax.persistence.EntityManager;
import java.util.Optional;

public class DeviceManagementOperationRepositoryImplJpaRepository
        extends KapuaUpdatableEntityJpaRepository<DeviceManagementOperation, DeviceManagementOperationImpl, DeviceManagementOperationListResult>
        implements DeviceManagementOperationRepository {
    public DeviceManagementOperationRepositoryImplJpaRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        super(DeviceManagementOperationImpl.class, () -> new DeviceManagementOperationListResultImpl(), jpaRepoConfig);
    }

    @Override
    public Optional<DeviceManagementOperation> findByOperationId(TxContext tx, KapuaId scopeId, KapuaId operationId) throws KapuaException {
        return doFindByField(tx, scopeId, DeviceManagementOperationImpl_.OPERATION_ID, operationId);
    }

    @Override
    public DeviceManagementOperation delete(TxContext txContext, KapuaId scopeId, KapuaId entityId) throws KapuaException {
        final EntityManager em = JpaAwareTxContext.extractEntityManager(txContext);
        // Check existence
        return this.doFind(em, scopeId, entityId)
                .map(toBeDeleted -> doDelete(em, toBeDeleted))
                .orElseThrow(() -> new KapuaEntityNotFoundException(DeviceManagementOperation.TYPE, entityId));
    }
}
