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
package org.eclipse.kapua.service.device.management.bundle.internal;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.management.DeviceManagementDomains;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundleManagementService;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundles;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleRequestChannel;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleRequestMessage;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleRequestPayload;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleResponseMessage;
import org.eclipse.kapua.service.device.management.commons.AbstractDeviceManagementTransactionalServiceImpl;
import org.eclipse.kapua.service.device.management.commons.call.DeviceCallExecutor;
import org.eclipse.kapua.service.device.management.message.KapuaMethod;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationFactory;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationRepository;
import org.eclipse.kapua.service.device.registry.DeviceRepository;
import org.eclipse.kapua.service.device.registry.event.DeviceEventFactory;
import org.eclipse.kapua.service.device.registry.event.DeviceEventRepository;
import org.eclipse.kapua.storage.TxManager;

import javax.inject.Singleton;
import java.util.Date;

/**
 * DeviceBundleManagementService implementation.
 *
 * @since 1.0.0
 */
@Singleton
public class DeviceBundleManagementServiceImpl extends AbstractDeviceManagementTransactionalServiceImpl implements DeviceBundleManagementService {

    private static final String SCOPE_ID = "scopeId";
    private static final String DEVICE_ID = "deviceId";

    public DeviceBundleManagementServiceImpl(TxManager txManager,
                                             AuthorizationService authorizationService,
                                             PermissionFactory permissionFactory,
                                             DeviceEventRepository deviceEventRepository,
                                             DeviceEventFactory deviceEventFactory,
                                             DeviceRepository deviceRepository,
                                             DeviceManagementOperationRepository deviceManagementOperationRepository,
                                             DeviceManagementOperationFactory deviceManagementOperationFactory) {
        super(txManager,
                authorizationService,
                permissionFactory,
                deviceEventRepository,
                deviceEventFactory,
                deviceRepository,
                deviceManagementOperationRepository,
                deviceManagementOperationFactory);
    }

    @Override
    public DeviceBundles get(KapuaId scopeId, KapuaId deviceId, Long timeout)
            throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.read, scopeId));
        // Prepare the request
        BundleRequestChannel bundleRequestChannel = new BundleRequestChannel();
        bundleRequestChannel.setAppName(DeviceBundleAppProperties.APP_NAME);
        bundleRequestChannel.setVersion(DeviceBundleAppProperties.APP_VERSION);
        bundleRequestChannel.setMethod(KapuaMethod.READ);

        BundleRequestPayload bundleRequestPayload = new BundleRequestPayload();

        BundleRequestMessage bundleRequestMessage = new BundleRequestMessage();
        bundleRequestMessage.setScopeId(scopeId);
        bundleRequestMessage.setDeviceId(deviceId);
        bundleRequestMessage.setCapturedOn(new Date());
        bundleRequestMessage.setPayload(bundleRequestPayload);
        bundleRequestMessage.setChannel(bundleRequestChannel);
        // Do get
        DeviceCallExecutor<?, ?, ?, BundleResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(bundleRequestMessage, timeout);
        BundleResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, bundleRequestMessage, responseMessage);
        // Check response
        return checkResponseAcceptedOrThrowError(responseMessage, () -> responseMessage.getPayload().getDeviceBundles());
    }

    @Override
    public void start(KapuaId scopeId, KapuaId deviceId, String bundleId, Long timeout)
            throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        ArgumentValidator.notEmptyOrNull(bundleId, "bundleId");
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.execute, scopeId));
        // Prepare the request
        BundleRequestChannel bundleRequestChannel = new BundleRequestChannel();
        bundleRequestChannel.setAppName(DeviceBundleAppProperties.APP_NAME);
        bundleRequestChannel.setVersion(DeviceBundleAppProperties.APP_VERSION);
        bundleRequestChannel.setMethod(KapuaMethod.EXECUTE);
        bundleRequestChannel.setStart(true);
        bundleRequestChannel.setBundleId(bundleId);

        BundleRequestPayload bundleRequestPayload = new BundleRequestPayload();

        BundleRequestMessage bundleRequestMessage = new BundleRequestMessage();
        bundleRequestMessage.setScopeId(scopeId);
        bundleRequestMessage.setDeviceId(deviceId);
        bundleRequestMessage.setCapturedOn(new Date());
        bundleRequestMessage.setPayload(bundleRequestPayload);
        bundleRequestMessage.setChannel(bundleRequestChannel);
        // Do start
        DeviceCallExecutor<?, ?, ?, BundleResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(bundleRequestMessage, timeout);
        BundleResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, bundleRequestMessage, responseMessage);
        // Check response
        checkResponseAcceptedOrThrowError(responseMessage);
    }

    @Override
    public void stop(KapuaId scopeId, KapuaId deviceId, String bundleId, Long timeout)
            throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        ArgumentValidator.notEmptyOrNull(bundleId, "bundleId");
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.execute, scopeId));
        // Prepare the request
        BundleRequestChannel bundleRequestChannel = new BundleRequestChannel();
        bundleRequestChannel.setAppName(DeviceBundleAppProperties.APP_NAME);
        bundleRequestChannel.setVersion(DeviceBundleAppProperties.APP_VERSION);
        bundleRequestChannel.setMethod(KapuaMethod.EXECUTE);
        bundleRequestChannel.setStart(false);
        bundleRequestChannel.setBundleId(bundleId);

        BundleRequestPayload bundleRequestPayload = new BundleRequestPayload();

        BundleRequestMessage bundleRequestMessage = new BundleRequestMessage();
        bundleRequestMessage.setScopeId(scopeId);
        bundleRequestMessage.setDeviceId(deviceId);
        bundleRequestMessage.setCapturedOn(new Date());
        bundleRequestMessage.setPayload(bundleRequestPayload);
        bundleRequestMessage.setChannel(bundleRequestChannel);
        // Do stop
        DeviceCallExecutor<?, ?, ?, BundleResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(bundleRequestMessage, timeout);
        BundleResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, bundleRequestMessage, responseMessage);
        // Check response
        checkResponseAcceptedOrThrowError(responseMessage);
    }

}
