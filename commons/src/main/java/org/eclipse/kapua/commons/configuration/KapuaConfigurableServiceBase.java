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

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.config.metatype.KapuaTocd;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.domain.Domain;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.KapuaService;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.config.KapuaConfigurableService;
import org.eclipse.kapua.storage.TxManager;

import java.util.Map;
import java.util.Optional;

/**
 * Base {@link KapuaConfigurableService} implementation, build upon {@link ServiceConfigurationManager}.
 * <p>
 * Note: at first glance, this might seems like a violation of Composition over Inheritance principle, however:
 * - in this case inheritance is an acceptable strategy due to the strong link between {@link ServiceConfigurationManager#isServiceEnabled(org.eclipse.kapua.storage.TxContext, KapuaId)}
 * and {@link org.eclipse.kapua.service.KapuaService#isServiceEnabled(KapuaId)} (the latter being dependent from the first for configurable services).
 * - this class is nothing more than glue and convenience, demanding all of its logic to the {@link ServiceConfigurationManager}'s instance provided, so no flexibility has been sacrificed
 *
 * @since 2.0.0
 */
public class KapuaConfigurableServiceBase
        implements KapuaConfigurableService,
        KapuaService {

    protected final TxManager txManager;
    protected final ServiceConfigurationManager serviceConfigurationManager;
    private final Domain domain;
    protected final AuthorizationService authorizationService;
    protected final PermissionFactory permissionFactory;

    public KapuaConfigurableServiceBase(
            TxManager txManager,
            ServiceConfigurationManager serviceConfigurationManager,
            Domain authorizationDomain,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory) {
        this.txManager = txManager;
        this.serviceConfigurationManager = serviceConfigurationManager;
        this.domain = authorizationDomain;
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
    }

    @Override
    public boolean isServiceEnabled(KapuaId scopeId) throws KapuaException {
        return txManager.execute(tx -> serviceConfigurationManager.isServiceEnabled(tx, scopeId));
    }

    @Override
    public KapuaTocd getConfigMetadata(KapuaId scopeId) throws KapuaException {
        // Check access
        authorizationService.checkPermission(permissionFactory.newPermission(domain, Actions.read, scopeId));
        return txManager.execute(tx -> serviceConfigurationManager.getConfigMetadata(tx, scopeId, true));
    }

    @Override
    public Map<String, Object> getConfigValues(KapuaId scopeId) throws KapuaException {
        // Check access
        authorizationService.checkPermission(permissionFactory.newPermission(domain, Actions.read, scopeId));

        return txManager.execute(tx -> serviceConfigurationManager.getConfigValues(tx, scopeId, true));
    }

    @Override
    public void setConfigValues(KapuaId scopeId, KapuaId parentId, Map<String, Object> values) throws KapuaException {
        authorizationService.checkPermission(permissionFactory.newPermission(domain, Actions.write, scopeId));

        txManager.<Void>execute(tx -> {
            serviceConfigurationManager.setConfigValues(tx, scopeId, Optional.ofNullable(parentId), values);
            return null;
        });
    }
}
