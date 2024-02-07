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
package org.eclipse.kapua.service.account.internal;

import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.eclipse.kapua.commons.configuration.AccountRelativeFinder;
import org.eclipse.kapua.commons.configuration.CachingServiceConfigRepository;
import org.eclipse.kapua.commons.configuration.ResourceLimitedServiceConfigurationManagerImpl;
import org.eclipse.kapua.commons.configuration.RootUserTester;
import org.eclipse.kapua.commons.configuration.ServiceConfigImplJpaRepository;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManager;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManagerCachingWrapper;
import org.eclipse.kapua.commons.configuration.UsedEntitiesCounterImpl;
import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.core.ServiceModule;
import org.eclipse.kapua.commons.event.ServiceEventHouseKeeperFactoryImpl;
import org.eclipse.kapua.commons.jpa.EntityCacheFactory;
import org.eclipse.kapua.commons.jpa.EventStorer;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.commons.jpa.KapuaJpaTxManagerFactory;
import org.eclipse.kapua.commons.jpa.NamedCacheFactory;
import org.eclipse.kapua.commons.model.domains.Domains;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreFactory;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreRecordRepository;
import org.eclipse.kapua.commons.service.event.store.internal.EventStoreServiceImpl;
import org.eclipse.kapua.commons.service.internal.cache.NamedEntityCache;
import org.eclipse.kapua.event.ServiceEventBus;
import org.eclipse.kapua.event.ServiceEventBusException;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.domain.Domain;
import org.eclipse.kapua.model.domain.DomainEntry;
import org.eclipse.kapua.service.account.AccountFactory;
import org.eclipse.kapua.service.account.AccountRepository;
import org.eclipse.kapua.service.account.AccountService;
import org.eclipse.kapua.service.account.internal.setting.KapuaAccountSetting;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code kapua-account-internal} {@link Module} implementation.
 *
 * @since 2.0.0
 */
public class AccountModule extends AbstractKapuaModule implements Module {

    @Override
    protected void configureModule() {
        bind(AccountFactory.class).to(AccountFactoryImpl.class).in(Singleton.class);
        bind(KapuaAccountSetting.class).in(Singleton.class);
    }

    @ProvidesIntoSet
    public Domain accountDomain() {
        return new DomainEntry(Domains.ACCOUNT, AccountService.class.getName(), false, Actions.read, Actions.delete, Actions.write);
    }

    @Provides
    @Singleton
    AccountRelativeFinder accountRelativeFinder(
            AccountFactory accountFactory,
            AccountService accountService) {
        return new AccountRelativeFinderImpl(
                accountFactory,
                accountService);
    }

    @ProvidesIntoSet
    ServiceModule accountServiceModule(AccountService accountService,
                                       AuthorizationService authorizationService,
                                       PermissionFactory permissionFactory,
                                       KapuaJpaTxManagerFactory txManagerFactory,
                                       EventStoreFactory eventStoreFactory,
                                       EventStoreRecordRepository eventStoreRecordRepository,
                                       ServiceEventBus serviceEventBus,
                                       KapuaAccountSetting kapuaAccountSetting
    ) throws ServiceEventBusException {
        return new AccountServiceModule(
                accountService,
                kapuaAccountSetting,
                new ServiceEventHouseKeeperFactoryImpl(
                        new EventStoreServiceImpl(
                                authorizationService,
                                permissionFactory,
                                txManagerFactory.create("kapua-account"),
                                eventStoreFactory,
                                eventStoreRecordRepository
                        ),
                        txManagerFactory.create("kapua-account"),
                        serviceEventBus
                ),
                serviceEventBus);
    }

    @Provides
    @Singleton
    AccountService accountService(AccountRepository accountRepository,
                                  AccountFactory accountFactory,
                                  PermissionFactory permissionFactory,
                                  AuthorizationService authorizationService,
                                  @Named("AccountServiceConfigurationManager") ServiceConfigurationManager serviceConfigurationManager,
                                  EventStorer eventStorer,
                                  KapuaJpaTxManagerFactory jpaTxManagerFactory) {
        return new AccountServiceImpl(
                jpaTxManagerFactory.create("kapua-account"),
                accountRepository,
                permissionFactory,
                authorizationService,
                serviceConfigurationManager,
                eventStorer);
    }

    @Provides
    @Singleton
    @Named("AccountServiceConfigurationManager")
    ServiceConfigurationManager accountServiceConfigurationManager(
            AccountFactory factory,
            RootUserTester rootUserTester,
            AccountRelativeFinder accountRelativeFinder,
            AccountRepository accountRepository,
            KapuaJpaRepositoryConfiguration jpaRepoConfig,
            EntityCacheFactory entityCacheFactory
    ) {
        return new ServiceConfigurationManagerCachingWrapper(
                new ResourceLimitedServiceConfigurationManagerImpl(
                        AccountService.class.getName(),
                        new CachingServiceConfigRepository(
                                new ServiceConfigImplJpaRepository(jpaRepoConfig),
                                entityCacheFactory.createCache("AbstractKapuaConfigurableServiceCacheId")
                        ),
                        rootUserTester,
                        accountRelativeFinder,
                        new UsedEntitiesCounterImpl(
                                factory,
                                accountRepository)
                ));
    }

    @Provides
    @Singleton
    AccountRepository accountRepository(NamedCacheFactory namedCacheFactory, KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        final AccountImplJpaRepository wrapped = new AccountImplJpaRepository(jpaRepoConfig);
        final NamedEntityCache cache = namedCacheFactory.createCache("AccountId", "AccountName");
        return new CachingAccountRepository(wrapped, cache);
    }
}
