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
package org.eclipse.kapua.commons.configuration;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaIllegalArgumentException;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.account.Account;
import org.eclipse.kapua.service.account.AccountRepository;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.config.ServiceComponentConfiguration;
import org.eclipse.kapua.service.config.ServiceConfiguration;
import org.eclipse.kapua.storage.TxContext;
import org.eclipse.kapua.storage.TxManager;

public class ServiceConfigurationsFacadeImpl implements ServiceConfigurationsFacade {

    private final Map<String, ServiceConfigurationManager> serviceConfigurationManagersByServiceClass;
    //which one? Most likely move <class>org.eclipse.kapua.commons.configuration.ServiceConfigImpl</class> in its own persistence unit
    //This way we can abstract all services from the details of how and where the service configuration is stored (would be about time....)
    protected final TxManager txManager;
    protected final AuthorizationService authorizationService;
    protected final PermissionFactory permissionFactory;
    private final AccountRepository accountRepository;

    @Inject
    public ServiceConfigurationsFacadeImpl(Map<Class<?>, ServiceConfigurationManager> serviceConfigurationManagersByServiceClass, TxManager txManager, AuthorizationService authorizationService,
            PermissionFactory permissionFactory, AccountRepository accountRepository) {
        this.serviceConfigurationManagersByServiceClass = serviceConfigurationManagersByServiceClass
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        kv -> kv.getKey().toString(),
                        kv -> kv.getValue()));
        this.txManager = txManager;
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
        this.accountRepository = accountRepository;
    }

    @Override
    public ServiceConfiguration fetchAllConfigurations(KapuaId scopeId) throws KapuaException {
        return txManager.execute(tx -> {
            final ServiceConfiguration res = new ServiceConfiguration();

            for (ServiceConfigurationManager configurableService : serviceConfigurationManagersByServiceClass.values()) {
                if (!authorizationService.isPermitted(permissionFactory.newPermission(configurableService.getDomain(), Actions.read, scopeId))) {
                    continue;
                }
                res.getComponentConfigurations().add(configurableService.extractServiceComponentConfiguration(tx, scopeId));
            }
            return res;
        });
    }

    @Override
    public ServiceComponentConfiguration fetchConfiguration(KapuaId scopeId, String serviceId) throws KapuaException {
        final ServiceConfigurationManager serviceConfigurationManager = serviceConfigurationManagersByServiceClass.get(serviceId);
        if (serviceConfigurationManager == null) {
            throw new KapuaIllegalArgumentException("service.pid", serviceId);
        }
        authorizationService.checkPermission(permissionFactory.newPermission(serviceConfigurationManager.getDomain(), Actions.read, scopeId));
        return txManager.execute(tx ->
                serviceConfigurationManager.extractServiceComponentConfiguration(tx, scopeId));
    }

    @Override
    public void update(KapuaId scopeId, ServiceConfiguration newServiceConfiguration) throws KapuaException {
        txManager.execute(tx -> {
            final Account account = accountRepository.find(tx, KapuaId.ANY, scopeId)
                    .orElseThrow(() -> new KapuaEntityNotFoundException(Account.TYPE, scopeId));

            for (ServiceComponentConfiguration newServiceComponentConfiguration : newServiceConfiguration.getComponentConfigurations()) {
                doUpdateServiceComponentConfiguration(tx, account, scopeId, newServiceComponentConfiguration);
            }
            return null;
        });
    }

    @Override
    public void update(KapuaId scopeId, String serviceId, ServiceComponentConfiguration newServiceComponentConfiguration) throws KapuaException {
        txManager.execute(tx -> {
            final Account account = accountRepository.find(tx, KapuaId.ANY, scopeId)
                    .orElseThrow(() -> new KapuaEntityNotFoundException(Account.TYPE, scopeId));
            doUpdateServiceComponentConfiguration(tx, account, scopeId, newServiceComponentConfiguration);
            return null;
        });
    }

    private void doUpdateServiceComponentConfiguration(TxContext tx, Account account, KapuaId scopeId, ServiceComponentConfiguration newServiceComponentConfiguration) throws KapuaException {
        final ServiceConfigurationManager serviceConfigurationManager = serviceConfigurationManagersByServiceClass.get(newServiceComponentConfiguration.getId());
        if (serviceConfigurationManager == null) {
            throw new KapuaIllegalArgumentException("serviceConfiguration.componentConfiguration.id", newServiceComponentConfiguration.getId());
        }
        if (!authorizationService.isPermitted(permissionFactory.newPermission(serviceConfigurationManager.getDomain(), Actions.write, scopeId))) {
            //TODO: Or maybe throw?
            return;
        }
        serviceConfigurationManager.setConfigValues(scopeId, Optional.ofNullable(account.getScopeId()), newServiceComponentConfiguration.getProperties());
    }
}
