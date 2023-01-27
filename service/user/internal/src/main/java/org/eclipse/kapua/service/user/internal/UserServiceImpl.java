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
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kapua.service.user.internal;

import org.eclipse.kapua.KapuaDuplicateExternalIdException;
import org.eclipse.kapua.KapuaDuplicateExternalUsernameException;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaIllegalArgumentException;
import org.eclipse.kapua.commons.configuration.KapuaConfigurableServiceBase;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManager;
import org.eclipse.kapua.commons.jpa.EntityManagerContainer;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.service.internal.KapuaNamedEntityServiceUtils;
import org.eclipse.kapua.commons.setting.system.SystemSetting;
import org.eclipse.kapua.commons.setting.system.SystemSettingKey;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.commons.util.CommonsValidationRegex;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.user.User;
import org.eclipse.kapua.service.user.UserCreator;
import org.eclipse.kapua.service.user.UserDomains;
import org.eclipse.kapua.service.user.UserListResult;
import org.eclipse.kapua.service.user.UserNamedEntityService;
import org.eclipse.kapua.service.user.UserQuery;
import org.eclipse.kapua.service.user.UserService;
import org.eclipse.kapua.service.user.UserStatus;
import org.eclipse.kapua.service.user.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * {@link UserService} implementation.
 *
 * @since 1.0.0
 */
@Singleton
public class UserServiceImpl extends KapuaConfigurableServiceBase implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserNamedEntityService getUserNamedEntityService() {
        if (userNamedEntityService == null) {
            userNamedEntityService = KapuaLocator.getInstance().getService(UserNamedEntityService.class);
        }
        return userNamedEntityService;
    }

    private AuthorizationService authorizationService;
    private PermissionFactory permissionFactory;
    private UserNamedEntityService userNamedEntityService;


    /**
     * Constructor.
     *
     * @since 1.0.0
     * @deprecated since 2.0.0 - Please use {@link #UserServiceImpl(AuthorizationService, PermissionFactory, UserEntityManagerFactory, UserCacheFactory, UserNamedEntityService, ServiceConfigurationManager)} instead. This constructor may be removed in a next release
     */
    @Deprecated
    public UserServiceImpl() {
        super(new UserEntityManagerFactory(), new UserCacheFactory(), null);
        this.authorizationService = null;
        this.permissionFactory = null;
    }


    /**
     * Injectable Constructor
     *
     * @param authorizationService        The {@link AuthorizationService} instance.
     * @param permissionFactory           The {@link PermissionFactory} instance.
     * @param userEntityManagerFactory    The {@link UserEntityManagerFactory} instance.
     * @param userCacheFactory            The {@link UserCacheFactory} instance.
     * @param userNamedEntityService      The {@link UserNamedEntityService} instance.
     * @param serviceConfigurationManager The {@link ServiceConfigurationManager} instance.
     */
    @Inject
    public UserServiceImpl(
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            UserEntityManagerFactory userEntityManagerFactory,
            UserCacheFactory userCacheFactory,
            UserNamedEntityService userNamedEntityService,
            @Named("UserServiceConfigurationManager") ServiceConfigurationManager serviceConfigurationManager) {
        super(userEntityManagerFactory, userCacheFactory, serviceConfigurationManager);
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
        this.userNamedEntityService = userNamedEntityService;
    }

    @Override
    public User create(UserCreator userCreator) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(userCreator.getScopeId().getId(), "userCreator.scopeId");
        ArgumentValidator.notEmptyOrNull(userCreator.getName(), "userCreator.name");
        ArgumentValidator.match(userCreator.getName(), CommonsValidationRegex.NAME_REGEXP, "userCreator.name");
        ArgumentValidator.lengthRange(userCreator.getName(), 3, 255, "userCreator.name");
        ArgumentValidator.match(userCreator.getEmail(), CommonsValidationRegex.EMAIL_REGEXP, "userCreator.email");
        ArgumentValidator.notNull(userCreator.getStatus(), "userCreator.status");

        ArgumentValidator.notNull(userCreator.getUserType(), "userCreator.userType");
        if (userCreator.getUserType() == UserType.EXTERNAL) {
            if (userCreator.getExternalId() != null) {
                ArgumentValidator.notEmptyOrNull(userCreator.getExternalId(), "userCreator.externalId");
                ArgumentValidator.lengthRange(userCreator.getExternalId(), 3, 255, "userCreator.externalId");
            } else {
                ArgumentValidator.notEmptyOrNull(userCreator.getExternalUsername(), "userCreator.externalUsername");
                ArgumentValidator.lengthRange(userCreator.getExternalUsername(), 3, 255, "userCreator.externalUsername");
            }
        } else if (userCreator.getUserType() == UserType.INTERNAL) {
            ArgumentValidator.isEmptyOrNull(userCreator.getExternalId(), "userCreator.externalId");
            ArgumentValidator.isEmptyOrNull(userCreator.getExternalUsername(), "userCreator.externalUsername");
        }

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.write, userCreator.getScopeId()));

        //
        // Check entity limit
        serviceConfigurationManager.checkAllowedEntities(userCreator.getScopeId(), "Users");

        //
        // Check duplicate name
        KapuaNamedEntityServiceUtils.checkEntityNameUniqueness(this, userCreator);
        KapuaNamedEntityServiceUtils.checkEntityNameUniquenessInAllScopes(this, userCreator);

        //
        // Check User.userType
        if (userCreator.getUserType() == UserType.EXTERNAL) {
            // Check duplicate externalId
            if (userCreator.getExternalId() != null) {
                User userByExternalId = KapuaSecurityUtils.doPrivileged(() -> findByExternalId(userCreator.getExternalId()));
                if (userByExternalId != null) {
                    throw new KapuaDuplicateExternalIdException(userCreator.getExternalId());
                }
            }

            // Check duplicate externalUsername
            if (userCreator.getExternalUsername() != null) {
                User userByExternalPreferredUserame = KapuaSecurityUtils.doPrivileged(() -> findByExternalId(userCreator.getExternalUsername()));
                if (userByExternalPreferredUserame != null) {
                    throw new KapuaDuplicateExternalUsernameException(userCreator.getExternalUsername());
                }
            }
        }

        //
        // Do create
        return entityManagerSession.doTransactedAction(EntityManagerContainer.<User>create().onResultHandler(em -> UserDAO.create(em, userCreator)));
    }

    @Override
    //@RaiseServiceEvent
    public User update(User user) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(user.getId(), "user.id");
        ArgumentValidator.notNull(user.getScopeId(), "user.scopeId");
        ArgumentValidator.notEmptyOrNull(user.getName(), "user.name");
        ArgumentValidator.match(user.getName(), CommonsValidationRegex.NAME_REGEXP, "user.name");
        ArgumentValidator.lengthRange(user.getName(), 3, 255, "user.name");
        ArgumentValidator.match(user.getEmail(), CommonsValidationRegex.EMAIL_REGEXP, "user.email");
        ArgumentValidator.notNull(user.getStatus(), "user.status");
        ArgumentValidator.notNull(user.getUserType(), "user.userType");

        if (user.getUserType() == UserType.EXTERNAL) {
            if (user.getExternalId() != null) {
                ArgumentValidator.notEmptyOrNull(user.getExternalId(), "user.externalId");
                ArgumentValidator.lengthRange(user.getExternalId(), 3, 255, "user.externalId");
            } else {
                ArgumentValidator.notEmptyOrNull(user.getExternalUsername(), "user.externalUsername");
                ArgumentValidator.lengthRange(user.getExternalUsername(), 3, 255, "user.externalUsername");
            }
        } else if (user.getUserType() == UserType.INTERNAL) {
            ArgumentValidator.isEmptyOrNull(user.getExternalId(), "user.externalId");
            ArgumentValidator.isEmptyOrNull(user.getExternalUsername(), "user.externalUsername");
        }

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.write, user.getScopeId()));

        //
        // Check existence
        User currentUser = find(user.getScopeId(), user.getId());
        if (currentUser == null) {
            throw new KapuaEntityNotFoundException(User.TYPE, user.getId());
        }

        //
        // Check action on Sys admin user
        if (user.getExpirationDate() != null || !currentUser.getName().equals(user.getName())) {
            //
            // Check not deleting environment admin
            validateSystemUser(user.getName());
        }

        //
        // Check disabling on logged user
        if (user.getId().equals(KapuaSecurityUtils.getSession().getUserId())) {
            if (user.getStatus().equals(UserStatus.DISABLED)) {
                throw new KapuaIllegalArgumentException("user.status", user.getStatus().name());
            }
        }

        //
        // Check not updatable fields

        // User.userType
        if (!Objects.equals(currentUser.getUserType(), user.getUserType())) {
            throw new KapuaIllegalArgumentException("user.userType", user.getUserType().toString());
        }

        // User.name
        if (!Objects.equals(currentUser.getName(), user.getName())) {
            throw new KapuaIllegalArgumentException("user.name", user.getName());
        }

        //
        // Check duplicates

        // User.externalId
        if (user.getExternalId() != null) {
            User userByExternalId = KapuaSecurityUtils.doPrivileged(() -> findByExternalId(user.getExternalId()));
            if (userByExternalId != null && !userByExternalId.getId().equals(user.getId())) {
                throw new KapuaDuplicateExternalIdException(user.getExternalId());
            }
        }

        // User.externalUsername
        if (user.getExternalUsername() != null) {
            User userByExternalPreferredUsername = KapuaSecurityUtils.doPrivileged(() -> findByExternalId(user.getExternalUsername()));
            if (userByExternalPreferredUsername != null && !userByExternalPreferredUsername.getId().equals(user.getId())) {
                throw new KapuaDuplicateExternalUsernameException(user.getExternalUsername());
            }
        }

        //
        // Do update
        return entityManagerSession.doTransactedAction(EntityManagerContainer.<User>create().onResultHandler(em -> UserDAO.update(em, user))
                .onBeforeHandler(() -> {
                    entityCache.remove(null, user);
                    return null;
                }));
    }

    @Override
    public void delete(User user) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(user, "user");

        //
        // Do delete
        delete(user.getScopeId(), user.getId());
    }

    @Override
    //@RaiseServiceEvent
    public void delete(KapuaId scopeId, KapuaId userId) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(userId.getId(), "user.id");
        ArgumentValidator.notNull(scopeId.getId(), "user.scopeId");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.delete, scopeId));

        //
        // Check existence
        User user = find(scopeId, userId);
        if (user == null) {
            throw new KapuaEntityNotFoundException(User.TYPE, userId);
        }

        //
        // Check not deleting environment admin
        validateSystemUser(user.getName());

        //
        // Check not deleting self
        validateSelf(user);

        //
        // Do  delete
        entityManagerSession.doTransactedAction(EntityManagerContainer.<User>create().onResultHandler(em -> UserDAO.delete(em, scopeId, userId))
                .onAfterHandler((emptyParam) -> entityCache.remove(scopeId, userId)));
    }

    @Override
    public User find(KapuaId scopeId, KapuaId userId)
            throws KapuaException {
        // Validation of the fields
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(userId, "userId");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.read, scopeId));

        // Do the find
        return entityManagerSession.doAction(EntityManagerContainer.<User>create().onResultHandler(em -> UserDAO.find(em, scopeId, userId))
                .onBeforeHandler(() -> (User) entityCache.get(scopeId, userId))
                .onAfterHandler((entity) -> entityCache.put(entity))
        );
    }

    @Override
    public User findByName(String name) throws KapuaException {
        return getUserNamedEntityService().findByName(name);
    }

    @Override
    public User findByExternalId(String externalId) throws KapuaException {
        //
        // Validation of the fields
        ArgumentValidator.notEmptyOrNull(externalId, "externalId");

        //
        // Do the find
        return entityManagerSession.doAction(EntityManagerContainer.<User>create().onResultHandler(em -> checkReadAccess(UserDAO.findByExternalId(em, externalId)))
                .onAfterHandler((entity) -> entityCache.put(entity)));
    }

    @Override
    public User findByExternalUsername(String externalUsername) throws KapuaException {
        //
        // Validation of the fields
        ArgumentValidator.notEmptyOrNull(externalUsername, "externalUsername");

        //
        // Do the find
        return entityManagerSession.doAction(EntityManagerContainer.<User>create().onResultHandler(em -> checkReadAccess(UserDAO.findByExternalUsername(em, externalUsername)))
                .onAfterHandler((entity) -> entityCache.put(entity)));
    }

    @Override
    public UserListResult query(KapuaQuery query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return entityManagerSession.doAction(EntityManagerContainer.<UserListResult>create().onResultHandler(em -> UserDAO.query(em, query)));
    }

    @Override
    public long count(KapuaQuery query)
            throws KapuaException {
        //
        // Argument Validator
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do count
        return entityManagerSession.doAction(EntityManagerContainer.<Long>create().onResultHandler(em -> UserDAO.count(em, query)));
    }

    // -----------------------------------------------------------------------------------------
    //
    // Private Methods
    //
    // -----------------------------------------------------------------------------------------

    private User checkReadAccess(User user) throws KapuaException {
        if (user != null) {
            getAuthorizationService().checkPermission(getPermissionFactory().newPermission(UserDomains.USER_DOMAIN, Actions.read, user.getScopeId()));
        }
        return user;
    }

    private void validateSystemUser(String name) throws KapuaException {
        String adminUsername = SystemSetting.getInstance().getString(SystemSettingKey.SYS_ADMIN_USERNAME);

        if (adminUsername.equals(name)) {
            throw new KapuaIllegalArgumentException("name", adminUsername);
        }
    }

    private void validateSelf(User user) throws KapuaException {
        if (user.getId().equals(KapuaSecurityUtils.getSession().getUserId())) {
            throw new KapuaIllegalArgumentException("name", user.getName());
        }
    }

    //@ListenServiceEvent(fromAddress = "account")
    public void onKapuaEvent(ServiceEvent kapuaEvent) throws KapuaException {
        if (kapuaEvent == null) {
            // service bus error. Throw some exception?
        }
        LOGGER.info("UserService: received kapua event from {}, operation {}", kapuaEvent.getService(), kapuaEvent.getOperation());
        if ("account".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteUserByAccountId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        }
    }

    private void deleteUserByAccountId(KapuaId scopeId, KapuaId accountId) throws KapuaException {
        UserQuery query = new UserQueryImpl(accountId);
        UserListResult usersToDelete = query(query);

        for (User u : usersToDelete.getItems()) {
            delete(u.getScopeId(), u.getId());
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
}
