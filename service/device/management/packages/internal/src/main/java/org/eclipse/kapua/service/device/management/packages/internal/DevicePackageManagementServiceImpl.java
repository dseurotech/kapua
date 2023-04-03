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
package org.eclipse.kapua.service.device.management.packages.internal;

import com.google.common.base.MoreObjects;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.model.id.IdGenerator;
import org.eclipse.kapua.commons.model.id.KapuaEid;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.type.ObjectTypeConverter;
import org.eclipse.kapua.model.type.ObjectValueConverter;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.management.DeviceManagementDomains;
import org.eclipse.kapua.service.device.management.commons.AbstractDeviceManagementTransactionalServiceImpl;
import org.eclipse.kapua.service.device.management.commons.call.DeviceCallExecutor;
import org.eclipse.kapua.service.device.management.commons.setting.DeviceManagementSetting;
import org.eclipse.kapua.service.device.management.commons.setting.DeviceManagementSettingKey;
import org.eclipse.kapua.service.device.management.message.KapuaMethod;
import org.eclipse.kapua.service.device.management.message.notification.NotifyStatus;
import org.eclipse.kapua.service.device.management.message.request.KapuaRequestMessage;
import org.eclipse.kapua.service.device.management.message.response.KapuaResponseMessage;
import org.eclipse.kapua.service.device.management.packages.DevicePackageFactory;
import org.eclipse.kapua.service.device.management.packages.DevicePackageManagementService;
import org.eclipse.kapua.service.device.management.packages.internal.setting.PackageManagementServiceSetting;
import org.eclipse.kapua.service.device.management.packages.internal.setting.PackageManagementServiceSettingKeys;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageAppProperties;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageRequestChannel;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageRequestMessage;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageRequestPayload;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageResource;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageResponseMessage;
import org.eclipse.kapua.service.device.management.packages.message.internal.PackageResponsePayload;
import org.eclipse.kapua.service.device.management.packages.model.DevicePackages;
import org.eclipse.kapua.service.device.management.packages.model.download.AdvancedPackageDownloadOptions;
import org.eclipse.kapua.service.device.management.packages.model.download.DevicePackageDownloadOperation;
import org.eclipse.kapua.service.device.management.packages.model.download.DevicePackageDownloadOptions;
import org.eclipse.kapua.service.device.management.packages.model.download.DevicePackageDownloadRequest;
import org.eclipse.kapua.service.device.management.packages.model.download.internal.DevicePackageDownloadOperationImpl;
import org.eclipse.kapua.service.device.management.packages.model.install.DevicePackageInstallOperation;
import org.eclipse.kapua.service.device.management.packages.model.install.DevicePackageInstallOptions;
import org.eclipse.kapua.service.device.management.packages.model.install.DevicePackageInstallRequest;
import org.eclipse.kapua.service.device.management.packages.model.uninstall.DevicePackageUninstallOperation;
import org.eclipse.kapua.service.device.management.packages.model.uninstall.DevicePackageUninstallOptions;
import org.eclipse.kapua.service.device.management.packages.model.uninstall.DevicePackageUninstallRequest;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperation;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationCreator;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationFactory;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationProperty;
import org.eclipse.kapua.service.device.management.registry.operation.DeviceManagementOperationRepository;
import org.eclipse.kapua.service.device.registry.DeviceRepository;
import org.eclipse.kapua.service.device.registry.event.DeviceEventFactory;
import org.eclipse.kapua.service.device.registry.event.DeviceEventRepository;
import org.eclipse.kapua.storage.TxManager;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * {@link DevicePackageManagementService} implementation.
 *
 * @since 1.0.0
 */
@Singleton
public class DevicePackageManagementServiceImpl extends AbstractDeviceManagementTransactionalServiceImpl implements DevicePackageManagementService {

    private static final String CHAR_ENCODING = DeviceManagementSetting.getInstance().getString(DeviceManagementSettingKey.CHAR_ENCODING);

    private final PackageManagementServiceSetting packageManagementServiceSetting = PackageManagementServiceSetting.getInstance();

    private final DeviceManagementOperationRepository deviceManagementOperationRepository;
    private final DeviceManagementOperationFactory deviceManagementOperationFactory;
    private final DevicePackageFactory devicePackageFactory;

    private static final String SCOPE_ID = "scopeId";
    private static final String DEVICE_ID = "deviceId";

    public DevicePackageManagementServiceImpl(
            TxManager txManager,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            DeviceEventRepository deviceEventRepository,
            DeviceEventFactory deviceEventFactory,
            DeviceRepository deviceRepository,
            DeviceManagementOperationRepository deviceManagementOperationRepository,
            DeviceManagementOperationFactory deviceManagementOperationFactory,
            DevicePackageFactory devicePackageFactory) {
        super(txManager,
                authorizationService,
                permissionFactory,
                deviceEventRepository,
                deviceEventFactory,
                deviceRepository);
        this.deviceManagementOperationRepository = deviceManagementOperationRepository;
        this.deviceManagementOperationFactory = deviceManagementOperationFactory;
        this.devicePackageFactory = devicePackageFactory;
    }
    // Installed

    @Override
    public DevicePackages getInstalled(KapuaId scopeId, KapuaId deviceId, Long timeout) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.read, scopeId));
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.READ);
        packageRequestChannel.setPackageResource(null);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Do get
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, timeout);
        PackageResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        return checkResponseAcceptedOrThrowError(responseMessage, () -> responseMessage.getPayload().getDevicePackages());
    }
    // Download

    @Override
    public KapuaId downloadExec(KapuaId scopeId, KapuaId deviceId, DevicePackageDownloadRequest packageDownloadRequest, Long timeout) throws KapuaException {
        DevicePackageDownloadOptions packageDownloadOptions = devicePackageFactory.newPackageDownloadOptions();
        packageDownloadOptions.setTimeout(timeout);

        return downloadExec(scopeId, deviceId, packageDownloadRequest, packageDownloadOptions);
    }

    @Override
    public KapuaId downloadExec(KapuaId scopeId, KapuaId deviceId, DevicePackageDownloadRequest packageDownloadRequest, DevicePackageDownloadOptions packageDownloadOptions) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        ArgumentValidator.notNull(packageDownloadRequest, "packageDownloadRequest");
        ArgumentValidator.notNull(packageDownloadRequest.getUri(), "packageDownloadRequest.uri");
        ArgumentValidator.notNull(packageDownloadRequest.getName(), "packageDownloadRequest.name");
        ArgumentValidator.notNull(packageDownloadRequest.getVersion(), "packageDownloadRequest.version");
        ArgumentValidator.notNull(packageDownloadOptions, "packageDownloadOptions");
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.write, scopeId));
        // Generate requestId
        KapuaId operationId = new KapuaEid(IdGenerator.generate());
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.EXECUTE);
        packageRequestChannel.setPackageResource(PackageResource.DOWNLOAD);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();

        // Basic options
        packageRequestPayload.setOperationId(operationId);
        packageRequestPayload.setPackageDownloadURI(packageDownloadRequest.getUri());
        packageRequestPayload.setPackageDownloadName(packageDownloadRequest.getName());
        packageRequestPayload.setPackageDownloadVersion(packageDownloadRequest.getVersion());
        packageRequestPayload.setPackageDownloadUsername(packageDownloadRequest.getUsername());
        packageRequestPayload.setPackageDownloadPassword(packageDownloadRequest.getPassword());
        packageRequestPayload.setPackageDownloadFileHash(packageDownloadRequest.getFileHash());
        packageRequestPayload.setPackageDownloadFileType(packageDownloadRequest.getFileType());
        packageRequestPayload.setPackageDownloadnstall(packageDownloadRequest.getInstall());
        packageRequestPayload.setReboot(packageDownloadRequest.getReboot());
        packageRequestPayload.setRebootDelay(packageDownloadRequest.getRebootDelay());

        // Advanced ones
        AdvancedPackageDownloadOptions advancedOptions = packageDownloadRequest.getAdvancedOptions();

        packageRequestPayload.setPackageDownloadRestart(advancedOptions.getRestart());
        packageRequestPayload.setPackageDownloadBlockSize(MoreObjects.firstNonNull(advancedOptions.getBlockSize(), packageManagementServiceSetting.getInt(PackageManagementServiceSettingKeys.PACKAGE_MANAGEMENT_SERVICE_SETTING_DOWDLOAD_DEFAULT_BLOCK_SIZE)));
        packageRequestPayload.setPackageDownloadBlockDelay(MoreObjects.firstNonNull(advancedOptions.getBlockDelay(), packageManagementServiceSetting.getInt(PackageManagementServiceSettingKeys.PACKAGE_MANAGEMENT_SERVICE_SETTING_DOWDLOAD_DEFAULT_BLOCK_DELAY)));
        packageRequestPayload.setPackageDownloadBlockTimeout(MoreObjects.firstNonNull(advancedOptions.getBlockTimeout(), packageManagementServiceSetting.getInt(PackageManagementServiceSettingKeys.PACKAGE_MANAGEMENT_SERVICE_SETTING_DOWDLOAD_DEFAULT_BLOCK_TIMEOUT)));
        packageRequestPayload.setPackageDownloadBlockSize(MoreObjects.firstNonNull(advancedOptions.getNotifyBlockSize(), packageManagementServiceSetting.getInt(PackageManagementServiceSettingKeys.PACKAGE_MANAGEMENT_SERVICE_SETTING_DOWDLOAD_DEFAULT_NOTIFY_BLOCK_SIZE)));
        packageRequestPayload.setPackageDownloadInstallVerifierURI(advancedOptions.getInstallVerifierURI());

        // Message
        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Create device management operation
        KapuaId deviceManagementOperationId = createManagementOperation(scopeId, deviceId, operationId, packageRequestMessage);
        // Do exec
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, packageDownloadOptions.getTimeout());
        PackageResponseMessage responseMessage;
        try {
            responseMessage = deviceApplicationCall.send();
        } catch (Exception e) {
            closeManagementOperation(scopeId, deviceId, operationId);
            throw e;
        }
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        try {
            checkResponseAcceptedOrThrowError(responseMessage);
        } catch (Exception e) {
            closeManagementOperation(scopeId, deviceId, operationId, responseMessage);
            throw e;
        }
        // Return operation id
        return deviceManagementOperationId;
    }

    @Override
    public DevicePackageDownloadOperation downloadStatus(KapuaId scopeId, KapuaId deviceId, Long timeout) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.read, scopeId));
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.READ);
        packageRequestChannel.setPackageResource(PackageResource.DOWNLOAD);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Do get
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, timeout);
        PackageResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        return checkResponseAcceptedOrThrowError(responseMessage, () -> {
            PackageResponsePayload responsePayload = responseMessage.getPayload();

            DevicePackageDownloadOperation downloadOperation = new DevicePackageDownloadOperationImpl();
            downloadOperation.setId(responsePayload.getPackageDownloadOperationId());
            downloadOperation.setStatus(responsePayload.getPackageDownloadOperationStatus());
            downloadOperation.setSize(responsePayload.getPackageDownloadOperationSize());
            downloadOperation.setProgress(responsePayload.getPackageDownloadOperationProgress());

            return downloadOperation;
        });
    }

    @Override
    public void downloadStop(KapuaId scopeId, KapuaId deviceId, Long timeout) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.write, scopeId));
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.DELETE);
        packageRequestChannel.setPackageResource(PackageResource.DOWNLOAD);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Do del
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, timeout);
        PackageResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        checkResponseAcceptedOrThrowError(responseMessage);
    }
    // Install

    @Override
    public KapuaId installExec(KapuaId scopeId, KapuaId deviceId, DevicePackageInstallRequest packageInstallRequest, Long timeout) throws KapuaException {
        DevicePackageInstallOptions packageInstallOptions = devicePackageFactory.newPackageInstallOptions();
        packageInstallOptions.setTimeout(timeout);

        return installExec(scopeId, deviceId, packageInstallRequest, packageInstallOptions);
    }

    @Override
    public KapuaId installExec(KapuaId scopeId, KapuaId deviceId, DevicePackageInstallRequest deployInstallRequest, DevicePackageInstallOptions packageInstallOptions) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        ArgumentValidator.notNull(deployInstallRequest, "deployInstallRequest");
        ArgumentValidator.notNull(packageInstallOptions, "packageInstallOptions");
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.write, scopeId));
        // Generate requestId
        KapuaId operationId = new KapuaEid(IdGenerator.generate());
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.EXECUTE);
        packageRequestChannel.setPackageResource(PackageResource.INSTALL);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();
        packageRequestPayload.setOperationId(operationId);
        packageRequestPayload.setPackageDownloadName(deployInstallRequest.getName());
        packageRequestPayload.setPackageDownloadVersion(deployInstallRequest.getVersion());
        packageRequestPayload.setReboot(deployInstallRequest.isReboot());
        packageRequestPayload.setRebootDelay(deployInstallRequest.getRebootDelay());

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Create device management operation
        KapuaId deviceManagementOperationId = createManagementOperation(scopeId, deviceId, operationId, packageRequestMessage);
        // Do get
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, packageInstallOptions.getTimeout());
        PackageResponseMessage responseMessage;
        try {
            responseMessage = deviceApplicationCall.send();
        } catch (Exception e) {
            closeManagementOperation(scopeId, deviceId, operationId);
            throw e;
        }
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        try {
            checkResponseAcceptedOrThrowError(responseMessage);
        } catch (Exception e) {
            closeManagementOperation(scopeId, deviceId, operationId, responseMessage);
            throw e;
        }
        // Return operation id
        return deviceManagementOperationId;
    }

    @Override
    public DevicePackageInstallOperation installStatus(KapuaId scopeId, KapuaId deviceId, Long timeout) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.read, scopeId));
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.READ);
        packageRequestChannel.setPackageResource(PackageResource.INSTALL);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Do get
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, timeout);
        PackageResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        return checkResponseAcceptedOrThrowError(responseMessage, () -> responseMessage.getPayload().getDevicePackageInstallOperation());
    }
    // Uninstall

    @Override
    public KapuaId uninstallExec(KapuaId scopeId, KapuaId deviceId, DevicePackageUninstallRequest packageUninstallRequest, Long timeout) throws KapuaException {
        DevicePackageUninstallOptions packageUninstallOptions = devicePackageFactory.newPackageUninstallOptions();
        packageUninstallOptions.setTimeout(timeout);

        return uninstallExec(scopeId, deviceId, packageUninstallRequest, packageUninstallOptions);
    }

    @Override
    public KapuaId uninstallExec(KapuaId scopeId, KapuaId deviceId, DevicePackageUninstallRequest packageUninstallRequest, DevicePackageUninstallOptions packageUninstallOptions) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        ArgumentValidator.notNull(packageUninstallRequest, "packageUninstallRequest");
        ArgumentValidator.notNull(packageUninstallOptions, "packageUninstallOptions");
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.write, scopeId));
        // Generate requestId
        KapuaId operationId = new KapuaEid(IdGenerator.generate());
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.EXECUTE);
        packageRequestChannel.setPackageResource(PackageResource.UNINSTALL);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();
        packageRequestPayload.setOperationId(operationId);
        packageRequestPayload.setPackageUninstallName(packageUninstallRequest.getName());
        packageRequestPayload.setPackageUninstallVersion(packageUninstallRequest.getVersion());
        packageRequestPayload.setReboot(packageUninstallRequest.isReboot());
        packageRequestPayload.setRebootDelay(packageUninstallRequest.getRebootDelay());

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Create device management operation
        KapuaId deviceManagementOperationId = createManagementOperation(scopeId, deviceId, operationId, packageRequestMessage);
        // Do get
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, packageUninstallOptions.getTimeout());
        PackageResponseMessage responseMessage;
        try {
            responseMessage = deviceApplicationCall.send();
        } catch (Exception e) {
            closeManagementOperation(scopeId, deviceId, operationId);
            throw e;
        }
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        try {
            checkResponseAcceptedOrThrowError(responseMessage);
        } catch (Exception e) {
            closeManagementOperation(scopeId, deviceId, operationId, responseMessage);
            throw e;
        }
        // Return operation id
        return deviceManagementOperationId;
    }

    @Override
    public DevicePackageUninstallOperation uninstallStatus(KapuaId scopeId, KapuaId deviceId, Long timeout) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, SCOPE_ID);
        ArgumentValidator.notNull(deviceId, DEVICE_ID);
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(DeviceManagementDomains.DEVICE_MANAGEMENT_DOMAIN, Actions.read, scopeId));
        // Prepare the request
        PackageRequestChannel packageRequestChannel = new PackageRequestChannel();
        packageRequestChannel.setAppName(PackageAppProperties.APP_NAME);
        packageRequestChannel.setVersion(PackageAppProperties.APP_VERSION);
        packageRequestChannel.setMethod(KapuaMethod.READ);
        packageRequestChannel.setPackageResource(PackageResource.UNINSTALL);

        PackageRequestPayload packageRequestPayload = new PackageRequestPayload();

        PackageRequestMessage packageRequestMessage = new PackageRequestMessage();
        packageRequestMessage.setScopeId(scopeId);
        packageRequestMessage.setDeviceId(deviceId);
        packageRequestMessage.setCapturedOn(new Date());
        packageRequestMessage.setPayload(packageRequestPayload);
        packageRequestMessage.setChannel(packageRequestChannel);
        // Do get
        DeviceCallExecutor<?, ?, ?, PackageResponseMessage> deviceApplicationCall = new DeviceCallExecutor<>(packageRequestMessage, timeout);
        PackageResponseMessage responseMessage = deviceApplicationCall.send();
        // Create event
        createDeviceEvent(scopeId, deviceId, packageRequestMessage, responseMessage);
        // Check response
        return checkResponseAcceptedOrThrowError(responseMessage, () -> responseMessage.getPayload().getDevicePackageUninstallOperation());
    }

    // Private methods

    private List<DeviceManagementOperationProperty> extractInputProperties(KapuaRequestMessage<?, ?> requestMessage) {

        List<DeviceManagementOperationProperty> inputProperties = new ArrayList<>();
        Map<String, Object> properties = requestMessage.getPayload().getMetrics();

        properties.forEach((k, v) -> {
            if (v != null) {
                inputProperties.add(
                        deviceManagementOperationFactory.newStepProperty(
                                k,
                                ObjectTypeConverter.toString(v.getClass()),
                                ObjectValueConverter.toString(v))
                );
            }
        });

        return inputProperties;
    }

    // Device Management Operations
    protected KapuaId createManagementOperation(KapuaId scopeId, KapuaId deviceId, KapuaId operationId, KapuaRequestMessage<?, ?> requestMessage) throws KapuaException {

        DeviceManagementOperationCreator deviceManagementOperationCreator = deviceManagementOperationFactory.newCreator(scopeId);
        deviceManagementOperationCreator.setDeviceId(deviceId);
        deviceManagementOperationCreator.setOperationId(operationId);
        deviceManagementOperationCreator.setStartedOn(new Date());
        deviceManagementOperationCreator.setAppId(requestMessage.getChannel().getAppName().getValue());
        deviceManagementOperationCreator.setAction(requestMessage.getChannel().getMethod());
        deviceManagementOperationCreator.setResource(!requestMessage.getChannel().getSemanticParts().isEmpty() ? requestMessage.getChannel().getSemanticParts().get(0) : null);
        deviceManagementOperationCreator.setStatus(NotifyStatus.RUNNING);
        deviceManagementOperationCreator.setInputProperties(extractInputProperties(requestMessage));

        DeviceManagementOperation deviceManagementOperation = deviceManagementOperationFactory.newEntity(deviceManagementOperationCreator.getScopeId());
        deviceManagementOperation.setStartedOn(deviceManagementOperationCreator.getStartedOn());
        deviceManagementOperation.setDeviceId(deviceManagementOperationCreator.getDeviceId());
        deviceManagementOperation.setOperationId(deviceManagementOperationCreator.getOperationId());
        deviceManagementOperation.setAppId(deviceManagementOperationCreator.getAppId());
        deviceManagementOperation.setAction(deviceManagementOperationCreator.getAction());
        deviceManagementOperation.setResource(deviceManagementOperationCreator.getResource());
        deviceManagementOperation.setStatus(deviceManagementOperationCreator.getStatus());
        deviceManagementOperation.setStatus(deviceManagementOperationCreator.getStatus());
        deviceManagementOperation.setInputProperties(deviceManagementOperationCreator.getInputProperties());
        DeviceManagementOperation res = txManager.execute(tx ->
                deviceManagementOperationRepository.create(tx, deviceManagementOperation));

        return res.getId();
    }

    protected void closeManagementOperation(KapuaId scopeId, KapuaId deviceId, KapuaId operationId) throws KapuaException {
        closeManagementOperation(scopeId, deviceId, operationId, null);
    }

    protected void closeManagementOperation(KapuaId scopeId, KapuaId deviceId, KapuaId operationId, KapuaResponseMessage<?, ?> responseMessageMessage) throws KapuaException {
        txManager.execute(tx -> {
            DeviceManagementOperation deviceManagementOperation = deviceManagementOperationRepository.findByOperationId(tx, scopeId, operationId);

            if (deviceManagementOperation == null) {
                throw new KapuaEntityNotFoundException(DeviceManagementOperation.TYPE, operationId);
            }

            if (responseMessageMessage != null) {
                deviceManagementOperation.setStatus(responseMessageMessage.getResponseCode().isAccepted() ? NotifyStatus.COMPLETED : NotifyStatus.FAILED);
                deviceManagementOperation.setEndedOn(responseMessageMessage.getReceivedOn());
            } else {
                deviceManagementOperation.setStatus(NotifyStatus.FAILED);
                deviceManagementOperation.setEndedOn(new Date());
            }
            return deviceManagementOperationRepository.update(tx, deviceManagementOperation);
        });
    }
}
