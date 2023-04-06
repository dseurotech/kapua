/*******************************************************************************
 * Copyright (c) 2020, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.security.test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.cucumber.java.Before;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.AccountChildrenFinder;
import org.eclipse.kapua.commons.configuration.RootUserTester;
import org.eclipse.kapua.commons.configuration.ServiceConfigImplJpaRepository;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManager;
import org.eclipse.kapua.commons.configuration.metatype.KapuaMetatypeFactoryImpl;
import org.eclipse.kapua.commons.jpa.DuplicateNameCheckerImpl;
import org.eclipse.kapua.commons.jpa.JpaTxManager;
import org.eclipse.kapua.commons.jpa.KapuaEntityManagerFactory;
import org.eclipse.kapua.commons.model.query.QueryFactoryImpl;
import org.eclipse.kapua.commons.setting.system.SystemSetting;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.config.metatype.KapuaMetatypeFactory;
import org.eclipse.kapua.model.query.QueryFactory;
import org.eclipse.kapua.qa.common.MockedLocator;
import org.eclipse.kapua.service.authentication.credential.CredentialFactory;
import org.eclipse.kapua.service.authentication.credential.CredentialService;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialFactoryImpl;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialServiceImpl;
import org.eclipse.kapua.service.authentication.shiro.AuthenticationEntityManagerFactory;
import org.eclipse.kapua.service.authentication.shiro.CredentialServiceConfigurationManagerImpl;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.group.GroupFactory;
import org.eclipse.kapua.service.authorization.group.GroupService;
import org.eclipse.kapua.service.authorization.group.shiro.GroupFactoryImpl;
import org.eclipse.kapua.service.authorization.group.shiro.GroupImplJpaRepository;
import org.eclipse.kapua.service.authorization.group.shiro.GroupQueryImpl;
import org.eclipse.kapua.service.authorization.group.shiro.GroupServiceImpl;
import org.eclipse.kapua.service.authorization.permission.Permission;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.authorization.role.RoleFactory;
import org.eclipse.kapua.service.authorization.role.RolePermissionFactory;
import org.eclipse.kapua.service.authorization.role.RoleService;
import org.eclipse.kapua.service.authorization.role.shiro.RoleFactoryImpl;
import org.eclipse.kapua.service.authorization.role.shiro.RoleImplJpaRepository;
import org.eclipse.kapua.service.authorization.role.shiro.RolePermissionFactoryImpl;
import org.eclipse.kapua.service.authorization.role.shiro.RolePermissionImplJpaRepository;
import org.eclipse.kapua.service.authorization.role.shiro.RoleQueryImpl;
import org.eclipse.kapua.service.authorization.role.shiro.RoleServiceImpl;
import org.eclipse.kapua.service.authorization.shiro.AuthorizationEntityManagerFactory;
import org.eclipse.kapua.service.user.UserFactory;
import org.eclipse.kapua.service.user.UserNamedEntityService;
import org.eclipse.kapua.service.user.UserService;
import org.eclipse.kapua.service.user.internal.UserCacheFactory;
import org.eclipse.kapua.service.user.internal.UserEntityManagerFactory;
import org.eclipse.kapua.service.user.internal.UserFactoryImpl;
import org.eclipse.kapua.service.user.internal.UserServiceImpl;
import org.mockito.Matchers;
import org.mockito.Mockito;

@Singleton
public class SecurityLocatorConfiguration {

    @Before(value = "@setup", order = 1)
    public void setupDI() {
        MockedLocator mockedLocator = (MockedLocator) KapuaLocator.getInstance();

        AbstractModule module = new AbstractModule() {

            @Override
            protected void configure() {
                // Inject mocked Authorization Service method checkPermission
                AuthorizationService mockedAuthorization = Mockito.mock(AuthorizationService.class);
                try {
                    Mockito.doNothing().when(mockedAuthorization).checkPermission(Matchers.any(Permission.class));
                } catch (KapuaException e) {
                    // skip
                }

                bind(QueryFactory.class).toInstance(new QueryFactoryImpl());

                bind(AuthorizationService.class).toInstance(mockedAuthorization);
                // Inject mocked Permission Factory
                PermissionFactory mockPermissionFactory = Mockito.mock(PermissionFactory.class);
                bind(PermissionFactory.class).toInstance(mockPermissionFactory);
                // Set KapuaMetatypeFactory for Metatype configuration
                bind(KapuaMetatypeFactory.class).toInstance(new KapuaMetatypeFactoryImpl());

                // Inject actual Role service related services
                AuthorizationEntityManagerFactory authorizationEntityManagerFactory = AuthorizationEntityManagerFactory.getInstance();
                bind(AuthorizationEntityManagerFactory.class).toInstance(authorizationEntityManagerFactory);
                bind(RoleService.class).toInstance(new RoleServiceImpl(
                        mockPermissionFactory,
                        mockedAuthorization,
                        new RolePermissionFactoryImpl(),
                        Mockito.mock(ServiceConfigurationManager.class),
                        new JpaTxManager(new KapuaEntityManagerFactory("kapua-authorization")),
                        new RoleImplJpaRepository(),
                        new RolePermissionImplJpaRepository(),
                        new DuplicateNameCheckerImpl<>(new RoleImplJpaRepository(), scopeId -> new RoleQueryImpl(scopeId)))
                );
                bind(RoleFactory.class).toInstance(new RoleFactoryImpl());
                bind(RolePermissionFactory.class).toInstance(new RolePermissionFactoryImpl());

                bind(GroupService.class).toInstance(new GroupServiceImpl(
                        mockPermissionFactory,
                        mockedAuthorization,
                        Mockito.mock(ServiceConfigurationManager.class),
                        new JpaTxManager(new KapuaEntityManagerFactory("kapua-authorization")),
                        new GroupImplJpaRepository(),
                        new DuplicateNameCheckerImpl<>(new GroupImplJpaRepository(), scopeId -> new GroupQueryImpl(scopeId))
                ));
                bind(GroupFactory.class).toInstance(new GroupFactoryImpl());
                bind(CredentialFactory.class).toInstance(new CredentialFactoryImpl());
                bind(CredentialService.class).toInstance(new CredentialServiceImpl(
                        new AuthenticationEntityManagerFactory(),
                        new CredentialServiceConfigurationManagerImpl(
                                new JpaTxManager(new KapuaEntityManagerFactory("kapua-authorization")),
                                new ServiceConfigImplJpaRepository(),
                                mockPermissionFactory,
                                mockedAuthorization,
                                Mockito.mock(RootUserTester.class))
                ));
                final UserFactoryImpl userFactory = new UserFactoryImpl();
                bind(UserFactory.class).toInstance(userFactory);
                final RootUserTester rootUserTester = Mockito.mock(RootUserTester.class);
                bind(RootUserTester.class).toInstance(rootUserTester);
                final AccountChildrenFinder accountChildrenFinder = Mockito.mock(AccountChildrenFinder.class);
                bind(AccountChildrenFinder.class).toInstance(accountChildrenFinder);
                bind(UserService.class).toInstance(new UserServiceImpl(
                        mockedAuthorization,
                        mockPermissionFactory,
                        new UserEntityManagerFactory(),
                        new UserCacheFactory(),
                        Mockito.mock(UserNamedEntityService.class),
                        Mockito.mock(ServiceConfigurationManager.class),
                        SystemSetting.getInstance()
                ));
            }
        };

        Injector injector = Guice.createInjector(module);
        mockedLocator.setInjector(injector);
    }
}
