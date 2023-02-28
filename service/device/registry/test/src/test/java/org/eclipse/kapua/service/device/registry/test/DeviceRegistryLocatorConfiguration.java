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
package org.eclipse.kapua.service.device.registry.test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.cucumber.java.Before;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.AccountChildrenFinder;
import org.eclipse.kapua.commons.configuration.RootUserTester;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManager;
import org.eclipse.kapua.commons.configuration.metatype.KapuaMetatypeFactoryImpl;
import org.eclipse.kapua.commons.jpa.EntityManagerSession;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.message.KapuaMessageFactory;
import org.eclipse.kapua.message.internal.KapuaMessageFactoryImpl;
import org.eclipse.kapua.model.config.metatype.KapuaMetatypeFactory;
import org.eclipse.kapua.qa.common.MockedLocator;
import org.eclipse.kapua.service.account.AccountFactory;
import org.eclipse.kapua.service.account.AccountService;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.Permission;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.registry.DeviceFactory;
import org.eclipse.kapua.service.device.registry.DeviceRegistryService;
import org.eclipse.kapua.service.device.registry.DeviceTransactedRepository;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionFactory;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionTransactedRepository;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionService;
import org.eclipse.kapua.service.device.registry.connection.internal.DeviceConnectionFactoryImpl;
import org.eclipse.kapua.service.device.registry.connection.internal.DeviceConnectionListResultImpl;
import org.eclipse.kapua.service.device.registry.connection.internal.DeviceConnectionRepositoryImplJpaTransactedRepository;
import org.eclipse.kapua.service.device.registry.connection.internal.DeviceConnectionServiceImpl;
import org.eclipse.kapua.service.device.registry.event.DeviceEventFactory;
import org.eclipse.kapua.service.device.registry.event.DeviceEventTransactedRepository;
import org.eclipse.kapua.service.device.registry.event.DeviceEventService;
import org.eclipse.kapua.service.device.registry.event.internal.DeviceEventFactoryImpl;
import org.eclipse.kapua.service.device.registry.event.internal.DeviceEventImplJpaTransactedRepository;
import org.eclipse.kapua.service.device.registry.event.internal.DeviceEventListResultImpl;
import org.eclipse.kapua.service.device.registry.event.internal.DeviceEventServiceImpl;
import org.eclipse.kapua.service.device.registry.internal.DeviceEntityManagerFactory;
import org.eclipse.kapua.service.device.registry.internal.DeviceFactoryImpl;
import org.eclipse.kapua.service.device.registry.internal.DeviceImplJpaTransactedRepository;
import org.eclipse.kapua.service.device.registry.internal.DeviceListResultImpl;
import org.eclipse.kapua.service.device.registry.internal.DeviceRegistryCacheFactory;
import org.eclipse.kapua.service.device.registry.internal.DeviceRegistryServiceImpl;
import org.mockito.Matchers;
import org.mockito.Mockito;

@Singleton
public class DeviceRegistryLocatorConfiguration {

    @Before(value = "@setup", order = 1)
    public void setupDI() {
        System.setProperty("locator.class.impl", "org.eclipse.kapua.qa.common.MockedLocator");
        MockedLocator mockedLocator = (MockedLocator) KapuaLocator.getInstance();

        AbstractModule module = new AbstractModule() {

            @Override
            protected void configure() {

                // Inject mocked Authorization Service method checkPermission
                AuthorizationService mockedAuthorization = Mockito.mock(AuthorizationService.class);
                // Inject mocked Permission Factory
                final PermissionFactory permissionFactory = Mockito.mock(PermissionFactory.class);
                bind(PermissionFactory.class).toInstance(permissionFactory);
                try {
                    Mockito.doNothing().when(mockedAuthorization).checkPermission(Matchers.any(Permission.class));
                } catch (KapuaException e) {
                    // skip
                }
                bind(AuthorizationService.class).toInstance(mockedAuthorization);
                bind(AccountChildrenFinder.class).toInstance(Mockito.mock(AccountChildrenFinder.class));
                bind(AccountFactory.class).toInstance(Mockito.mock(AccountFactory.class));
                bind(AccountService.class).toInstance(Mockito.mock(AccountService.class));

                bind(RootUserTester.class).toInstance(Mockito.mock(RootUserTester.class));
                // Set KapuaMetatypeFactory for Metatype configuration
                bind(KapuaMetatypeFactory.class).toInstance(new KapuaMetatypeFactoryImpl());

                // Inject actual Device registry service related services
                final DeviceEntityManagerFactory deviceEntityManagerFactory = DeviceEntityManagerFactory.getInstance();
                bind(DeviceEntityManagerFactory.class).toInstance(deviceEntityManagerFactory);

                final DeviceRegistryCacheFactory deviceRegistryCacheFactory = new DeviceRegistryCacheFactory();
                bind(DeviceRegistryCacheFactory.class).toInstance(deviceRegistryCacheFactory);

                bind(DeviceFactory.class).toInstance(new DeviceFactoryImpl());
                bind(ServiceConfigurationManager.class)
                        .annotatedWith(Names.named("DeviceConnectionServiceConfigurationManager"))
                        .toInstance(Mockito.mock(ServiceConfigurationManager.class));
                bind(DeviceConnectionService.class).to(DeviceConnectionServiceImpl.class);
                bind(DeviceConnectionFactory.class).toInstance(new DeviceConnectionFactoryImpl());

                bind(DeviceTransactedRepository.class).toInstance(new DeviceImplJpaTransactedRepository(
                        () -> new DeviceListResultImpl(),
                        new EntityManagerSession(deviceEntityManagerFactory)));
                bind(DeviceConnectionTransactedRepository.class).toInstance(new DeviceConnectionRepositoryImplJpaTransactedRepository(
                                () -> new DeviceConnectionListResultImpl(),
                                new EntityManagerSession(deviceEntityManagerFactory)
                        )
                );
                bind(DeviceEventTransactedRepository.class).toInstance(new DeviceEventImplJpaTransactedRepository(
                        () -> new DeviceEventListResultImpl(),
                        new EntityManagerSession(deviceEntityManagerFactory)));
                bind(DeviceEventService.class).to(DeviceEventServiceImpl.class);
                bind(DeviceEventFactory.class).toInstance(new DeviceEventFactoryImpl());
                bind(KapuaMessageFactory.class).toInstance(new KapuaMessageFactoryImpl());
                bind(ServiceConfigurationManager.class)
                        .annotatedWith(Names.named("DeviceRegistryServiceConfigurationManager"))
                        .toInstance(Mockito.mock(ServiceConfigurationManager.class));
                bind(DeviceRegistryService.class).to(DeviceRegistryServiceImpl.class);
            }
        };

        Injector injector = Guice.createInjector(module);
        mockedLocator.setInjector(injector);
    }
}
