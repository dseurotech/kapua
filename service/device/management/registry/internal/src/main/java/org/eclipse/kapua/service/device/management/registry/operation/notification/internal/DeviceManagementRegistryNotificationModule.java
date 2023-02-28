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
package org.eclipse.kapua.service.device.management.registry.operation.notification.internal;

import com.google.inject.Provides;
import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.jpa.AbstractEntityManagerFactory;
import org.eclipse.kapua.commons.jpa.EntityManagerSession;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationFactory;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationTransactedRepository;
import org.eclipse.kapua.service.device.management.registry.operation.notification.ManagementOperationNotificationService;

import javax.inject.Singleton;

public class DeviceManagementRegistryNotificationModule extends AbstractKapuaModule {
    @Override
    protected void configureModule() {
        bind(ManagementOperationNotificationFactory.class).to(ManagementOperationNotificationFactoryImpl.class);
        bind(ManagementOperationNotificationService.class).to(ManagementOperationNotificationServiceImpl.class);
    }

    @Provides
    @Singleton
    ManagementOperationNotificationTransactedRepository managementOperationNotificationRepository(ManagementOperationNotificationFactory entityFactory) {
        return new ManagementOperationNotificationImplJpaTransactedRepository(
                () -> entityFactory.newListResult(),
                new EntityManagerSession(new AbstractEntityManagerFactory("kapua-device_management_operation_registry") {
                }));
    }

}
