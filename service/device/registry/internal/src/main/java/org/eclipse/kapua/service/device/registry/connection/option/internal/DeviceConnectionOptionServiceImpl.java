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
package org.eclipse.kapua.service.device.registry.connection.option.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaRuntimeErrorCodes;
import org.eclipse.kapua.KapuaRuntimeException;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.model.query.predicate.AndPredicate;
import org.eclipse.kapua.model.query.predicate.AttributePredicate.Operator;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.registry.DeviceDomains;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionAttributes;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionFactory;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionQuery;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionTransactedRepository;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOption;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionCreator;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionListResult;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionTransactedRepository;
import org.eclipse.kapua.service.device.registry.connection.option.DeviceConnectionOptionService;
import org.eclipse.kapua.service.device.registry.connection.option.UserAlreadyReservedException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * DeviceConnectionService exposes APIs to retrieve Device connections under a scope.
 * It includes APIs to find, list, and update devices connections associated with a scope.
 *
 * @since 1.0
 */
@Singleton
public class DeviceConnectionOptionServiceImpl implements DeviceConnectionOptionService {

    private final AuthorizationService authorizationService;
    private final PermissionFactory permissionFactory;
    private final DeviceConnectionTransactedRepository deviceConnectionRepository;
    private final DeviceConnectionFactory entityFactory;
    private final DeviceConnectionOptionTransactedRepository repository;

    @Inject
    public DeviceConnectionOptionServiceImpl(
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            DeviceConnectionTransactedRepository deviceConnectionRepository,
            DeviceConnectionFactory entityFactory,
            DeviceConnectionOptionTransactedRepository repository) {
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
        this.deviceConnectionRepository = deviceConnectionRepository;
        this.entityFactory = entityFactory;
        this.repository = repository;
    }

    @Override
    public DeviceConnectionOption create(DeviceConnectionOptionCreator deviceConnectionCreator)
            throws KapuaException {
        throw new KapuaRuntimeException(KapuaRuntimeErrorCodes.SERVICE_OPERATION_NOT_SUPPORTED);
    }

    @Override
    public DeviceConnectionOption update(DeviceConnectionOption deviceConnectionOptions)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(deviceConnectionOptions, "deviceConnection");
        ArgumentValidator.notNull(deviceConnectionOptions.getId(), "deviceConnection.id");
        ArgumentValidator.notNull(deviceConnectionOptions.getScopeId(), "deviceConnection.scopeId");

        //
        // Check Access

        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.write, deviceConnectionOptions.getScopeId()));

        if (deviceConnectionOptions.getReservedUserId() != null) {
            DeviceConnectionQuery query = entityFactory.newQuery(deviceConnectionOptions.getScopeId());

            AndPredicate deviceAndPredicate = query.andPredicate(
                    query.attributePredicate(DeviceConnectionAttributes.RESERVED_USER_ID, deviceConnectionOptions.getReservedUserId()),
                    query.attributePredicate(DeviceConnectionAttributes.ENTITY_ID, deviceConnectionOptions.getId(), Operator.NOT_EQUAL)
            );

            query.setPredicate(deviceAndPredicate);
            if (deviceConnectionRepository.count(query) > 0) {
                throw new UserAlreadyReservedException(deviceConnectionOptions.getScopeId(), deviceConnectionOptions.getId(), deviceConnectionOptions.getReservedUserId());
            }
        }
        if (deviceConnectionRepository.find(deviceConnectionOptions.getScopeId(), deviceConnectionOptions.getId()) == null) {
            throw new KapuaEntityNotFoundException(DeviceConnectionOption.TYPE, deviceConnectionOptions.getId());
        }

        return repository.update(deviceConnectionOptions);
    }

    @Override
    public DeviceConnectionOption find(KapuaId scopeId, KapuaId entityId)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(entityId, "entityId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.read, scopeId));

        return repository.find(scopeId, entityId);
    }

    @Override
    public DeviceConnectionOptionListResult query(KapuaQuery query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.read, query.getScopeId()));

        return repository.query(query);
    }

    @Override
    public long count(KapuaQuery query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceDomains.DEVICE_CONNECTION_DOMAIN, Actions.read, query.getScopeId()));

        return repository.count(query);
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId deviceConnectionOptionsId)
            throws KapuaException {
        throw new KapuaRuntimeException(KapuaRuntimeErrorCodes.SERVICE_OPERATION_NOT_SUPPORTED);
    }
}
