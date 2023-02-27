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
package org.eclipse.kapua.service.device.management.job.internal;

import com.google.inject.Provides;
import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.jpa.AbstractEntityManagerFactory;
import org.eclipse.kapua.commons.jpa.EntityManagerSession;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationFactory;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationRepository;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationService;

import javax.inject.Singleton;

public class DeviceManagementJobModule extends AbstractKapuaModule {
    @Override
    protected void configureModule() {
        bind(JobDeviceManagementOperationFactory.class).to(JobDeviceManagementOperationFactoryImpl.class);
        bind(JobDeviceManagementOperationService.class).to(JobDeviceManagementOperationServiceImpl.class);
    }

    @Provides
    @Singleton
    JobDeviceManagementOperationRepository jobDeviceManagementOperationRepository(JobDeviceManagementOperationFactory entityFactory) {
        return new JobDeviceManagementOperationImplJpaRepository(
                () -> entityFactory.newListResult(),
                new EntityManagerSession(new AbstractEntityManagerFactory("kapua-job-device-management-operation") {
                }));
    }
}
