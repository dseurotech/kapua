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
package org.eclipse.kapua.service.authorization.group.shiro;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManager;
import org.eclipse.kapua.commons.service.internal.AbstractKapuaService;
import org.eclipse.kapua.commons.service.internal.KapuaNamedEntityServiceUtils;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.config.metatype.KapuaTocd;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationDomains;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.group.Group;
import org.eclipse.kapua.service.authorization.group.GroupCreator;
import org.eclipse.kapua.service.authorization.group.GroupFactory;
import org.eclipse.kapua.service.authorization.group.GroupListResult;
import org.eclipse.kapua.service.authorization.group.GroupQuery;
import org.eclipse.kapua.service.authorization.group.GroupService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.authorization.shiro.AuthorizationEntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * {@link GroupService} implementation.
 *
 * @since 1.0.0
 */
@Singleton
public class GroupServiceImpl extends AbstractKapuaService implements GroupService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupServiceImpl.class);
    private PermissionFactory permissionFactory;
    private AuthorizationService authorizationService;
    private ServiceConfigurationManager serviceConfigurationManager;

    /**
     * @deprecated since 2.0.0 - please use {@link #GroupServiceImpl(AuthorizationEntityManagerFactory, GroupFactory, PermissionFactory, AuthorizationService, ServiceConfigurationManager)} instead. This constructor might be removed in later releases.
     */
    @Deprecated
    public GroupServiceImpl() {
        super(AuthorizationEntityManagerFactory.getInstance(), null);
    }

    /**
     * Injectable constructor
     *
     * @param authorizationEntityManagerFactory The {@link AuthorizationEntityManagerFactory} instance.
     * @param factory                           The {@link GroupFactory} instance.
     * @param permissionFactory                 The {@link PermissionFactory} instance.
     * @param authorizationService              The {@link AuthorizationService} instance.
     * @param serviceConfigurationManager       The {@link ServiceConfigurationManager} instance.
     * @since 2.0.0
     */
    @Inject
    public GroupServiceImpl(AuthorizationEntityManagerFactory authorizationEntityManagerFactory,
                            GroupFactory factory,
                            PermissionFactory permissionFactory,
                            AuthorizationService authorizationService,
                            @Named("GroupServiceConfigurationManager") ServiceConfigurationManager serviceConfigurationManager
    ) {
        super(authorizationEntityManagerFactory, null);
        this.permissionFactory = permissionFactory;
        this.authorizationService = authorizationService;
        this.serviceConfigurationManager = serviceConfigurationManager;
    }

    @Override
    public Group create(GroupCreator groupCreator) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(groupCreator, "groupCreator");
        ArgumentValidator.notNull(groupCreator.getScopeId(), "roleCreator.scopeId");
        ArgumentValidator.validateEntityName(groupCreator.getName(), "groupCreator.name");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.write, groupCreator.getScopeId()));

        //
        // Check entity limit
        serviceConfigurationManager.checkAllowedEntities(groupCreator.getScopeId(), "Groups");

        //
        // Check duplicate name
        KapuaNamedEntityServiceUtils.checkEntityNameUniqueness(this, groupCreator);

        //
        // Do create
        return entityManagerSession.doTransactedAction(em -> GroupDAO.create(em, groupCreator));
    }

    @Override
    public Group update(Group group) throws KapuaException {
        //
        // Argument validator
        ArgumentValidator.notNull(group, "group");
        ArgumentValidator.notNull(group.getId(), "group.id");
        ArgumentValidator.notNull(group.getScopeId(), "group.scopeId");
        ArgumentValidator.validateEntityName(group.getName(), "group.name");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.write, group.getScopeId()));

        //
        // Check existence
        if (find(group.getScopeId(), group.getId()) == null) {
            throw new KapuaEntityNotFoundException(Group.TYPE, group.getId());
        }

        //
        // Check duplicate name
        KapuaNamedEntityServiceUtils.checkEntityNameUniqueness(this, group);

        //
        // Do update
        return entityManagerSession.doTransactedAction(em -> GroupDAO.update(em, group));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId groupId) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(groupId, "groupId");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.delete, scopeId));

        //
        // Check existence
        if (find(scopeId, groupId) == null) {
            throw new KapuaEntityNotFoundException(Group.TYPE, groupId);
        }

        //
        // Do delete
        entityManagerSession.doTransactedAction(em -> GroupDAO.delete(em, scopeId, groupId));
    }

    @Override
    public Group find(KapuaId scopeId, KapuaId groupId) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(groupId, "groupId");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.read, scopeId));

        //
        // Do find
        return entityManagerSession.doAction(em -> GroupDAO.find(em, scopeId, groupId));
    }

    @Override
    public GroupListResult query(KapuaQuery query) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(em -> GroupDAO.query(em, query));
    }

    @Override
    public long count(KapuaQuery query) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(AuthorizationDomains.GROUP_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do count
        return entityManagerSession.doAction(em -> GroupDAO.count(em, query));
    }

    //@ListenServiceEvent(fromAddress="account")
    public void onKapuaEvent(ServiceEvent kapuaEvent) throws KapuaException {
        if (kapuaEvent == null) {
            //service bus error. Throw some exception?
        }

        LOG.info("GroupService: received kapua event from {}, operation {}", kapuaEvent.getService(), kapuaEvent.getOperation());
        if ("account".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteGroupByAccountId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        }
    }

    private void deleteGroupByAccountId(KapuaId scopeId, KapuaId accountId) throws KapuaException {
        GroupQuery query = new GroupQueryImpl(accountId);

        GroupListResult groupsToDelete = query(query);

        for (Group g : groupsToDelete.getItems()) {
            delete(g.getScopeId(), g.getId());
        }
    }


    /**
     * AuthorizationService should be provided by the Locator, but in most cases when this class is instantiated through the deprecated constructor the Locator is not yet ready,
     * therefore fetching of the required instance is demanded to this artificial getter.
     *
     * @return The instantiated (hopefully) {@link AuthorizationService} instance
     */
    //TODO: Remove as soon as deprecated constructors are removed, use field directly instead.
    protected AuthorizationService getAuthorizationService() {
        if (authorizationService == null) {
            authorizationService = KapuaLocator.getInstance().getService(AuthorizationService.class);
        }
        return authorizationService;
    }

    /**
     * PermissionFactory should be provided by the Locator, but in most cases when this class is instantiated through this constructor the Locator is not yet ready,
     * therefore fetching of the required instance is demanded to this artificial getter.
     *
     * @return The instantiated (hopefully) {@link PermissionFactory} instance
     */
    //TODO: Remove as soon as deprecated constructors are removed, use field directly instead.
    protected PermissionFactory getPermissionFactory() {
        if (permissionFactory == null) {
            permissionFactory = KapuaLocator.getInstance().getFactory(PermissionFactory.class);
        }
        return permissionFactory;
    }

    @Override
    public KapuaTocd getConfigMetadata(KapuaId scopeId) throws KapuaException {
        return serviceConfigurationManager.getConfigMetadata(scopeId, true);
    }

    @Override
    public Map<String, Object> getConfigValues(KapuaId scopeId) throws KapuaException {
        return serviceConfigurationManager.getConfigValues(scopeId, true);
    }

    @Override
    public void setConfigValues(KapuaId scopeId, KapuaId parentId, Map<String, Object> values) throws KapuaException {
        serviceConfigurationManager.setConfigValues(scopeId, Optional.ofNullable(parentId), values);
    }
}
