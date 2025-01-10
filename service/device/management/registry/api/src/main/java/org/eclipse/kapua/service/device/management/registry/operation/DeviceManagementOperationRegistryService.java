/*******************************************************************************
 * Copyright (c) 2018, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.device.management.registry.operation;

import java.util.Date;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.KapuaEntityService;
import org.eclipse.kapua.service.KapuaUpdatableEntityService;
import org.eclipse.kapua.service.device.management.message.notification.NotifyStatus;

public interface DeviceManagementOperationRegistryService
        extends KapuaEntityService<DeviceManagementOperation, DeviceManagementOperationCreator>, KapuaUpdatableEntityService<DeviceManagementOperation> {

    /**
     * Gets a {@link DeviceManagementOperation} by its {@link DeviceManagementOperation#getOperationId()}
     *
     * @param scopeId
     *         The {@link DeviceManagementOperation#getScopeId()}
     * @param operationId
     *         The {@link DeviceManagementOperation#getOperationId()}.
     * @return The {@link DeviceManagementOperation} found, or {@code null}
     * @throws KapuaException
     * @since 1.2.0
     */
    DeviceManagementOperation findByOperationId(KapuaId scopeId, KapuaId operationId) throws KapuaException;

    void updateStatus(KapuaId scopeId, KapuaId operationId, NotifyStatus notifyStatus, Date endedOnDate) throws KapuaException;
}
