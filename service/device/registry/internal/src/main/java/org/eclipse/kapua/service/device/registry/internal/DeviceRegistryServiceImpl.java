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
package org.eclipse.kapua.service.device.registry.internal;

import com.google.common.collect.Lists;
import org.eclipse.kapua.KapuaDuplicateNameException;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManager;
import org.eclipse.kapua.commons.jpa.EntityManagerContainer;
import org.eclipse.kapua.commons.service.internal.AbstractKapuaService;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.config.metatype.KapuaTocd;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.device.registry.Device;
import org.eclipse.kapua.service.device.registry.DeviceAttributes;
import org.eclipse.kapua.service.device.registry.DeviceCreator;
import org.eclipse.kapua.service.device.registry.DeviceFactory;
import org.eclipse.kapua.service.device.registry.DeviceListResult;
import org.eclipse.kapua.service.device.registry.DeviceQuery;
import org.eclipse.kapua.service.device.registry.DeviceRegistryService;
import org.eclipse.kapua.service.device.registry.common.DeviceValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * {@link DeviceRegistryService} implementation.
 *
 * @since 1.0.0
 */
@Singleton
public class DeviceRegistryServiceImpl
        extends AbstractKapuaService
        implements DeviceRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRegistryServiceImpl.class);
    private ServiceConfigurationManager serviceConfigurationManager;

    /**
     * Constructor.
     *
     * @param deviceEntityManagerFactory The {@link DeviceEntityManagerFactory#getInstance()}.
     * @since 1.0.0
     * @deprecated since 2.0.0 - Please use {@link #DeviceRegistryServiceImpl(DeviceEntityManagerFactory, DeviceRegistryCacheFactory, ServiceConfigurationManager)} instead. This constructor may be removed in future releases
     */
    @Deprecated
    public DeviceRegistryServiceImpl(DeviceEntityManagerFactory deviceEntityManagerFactory) {
        super(deviceEntityManagerFactory, DeviceRegistryCacheFactory.getInstance());
    }

    /**
     * Constructor.
     *
     * @since 1.0.0
     * @deprecated since 2.0.0 - Please use {@link #DeviceRegistryServiceImpl(DeviceEntityManagerFactory, DeviceRegistryCacheFactory, ServiceConfigurationManager)} instead. This constructor may be removed in future releases
     */
    @Deprecated
    public DeviceRegistryServiceImpl() {
        this(DeviceEntityManagerFactory.getInstance());
    }

    /**
     * Injectable Constructor
     *
     * @param deviceEntityManagerFactory  The {@link DeviceEntityManagerFactory} instance
     * @param deviceRegistryCacheFactory  The {@link DeviceRegistryCacheFactory} instance
     * @param serviceConfigurationManager The {@link ServiceConfigurationManager} instance
     * @since 2.0.0
     */
    @Inject
    public DeviceRegistryServiceImpl(DeviceEntityManagerFactory deviceEntityManagerFactory,
                                     DeviceRegistryCacheFactory deviceRegistryCacheFactory,
                                     @Named("DeviceRegistryServiceConfigurationManager") ServiceConfigurationManager serviceConfigurationManager) {
        super(deviceEntityManagerFactory, deviceRegistryCacheFactory);
        this.serviceConfigurationManager = serviceConfigurationManager;
    }

    @Override
    public Device create(DeviceCreator deviceCreator)
            throws KapuaException {
        DeviceValidation.validateCreatePreconditions(deviceCreator);

        //
        // Check entity limit
        serviceConfigurationManager.checkAllowedEntities(deviceCreator.getScopeId(), "Devices");

        //
        // Check duplicate clientId
        DeviceQuery query = new DeviceQueryImpl(deviceCreator.getScopeId());
        query.setPredicate(query.attributePredicate(DeviceAttributes.CLIENT_ID, deviceCreator.getClientId()));

        if (count(query) > 0) {
            throw new KapuaDuplicateNameException(deviceCreator.getClientId());
        }

        //
        // Do create
        return entityManagerSession.doTransactedAction(
                EntityManagerContainer
                        .<Device>create()
                        .onResultHandler(entityManager -> DeviceDAO.create(entityManager, deviceCreator)));
    }

    @Override
    public Device update(Device device)
            throws KapuaException {
        DeviceValidation.validateUpdatePreconditions(device);

        //
        // Do update
        return entityManagerSession.doTransactedAction(
                EntityManagerContainer
                        .<Device>create()
                        .onResultHandler(entityManager -> {
                            Device currentDevice = DeviceDAO.find(entityManager, device.getScopeId(), device.getId());
                            if (currentDevice == null) {
                                throw new KapuaEntityNotFoundException(Device.TYPE, device.getId());
                            }
                            // Update
                            return DeviceDAO.update(entityManager, device);
                        }).onBeforeHandler(() -> {
                                    entityCache.remove(device.getScopeId(), device);
                                    return null;
                                }
                        ));
    }

    @Override
    public Device find(KapuaId scopeId, KapuaId entityId)
            throws KapuaException {
        DeviceValidation.validateFindPreconditions(scopeId, entityId);

        //
        // Do find
        return entityManagerSession.doAction(
                EntityManagerContainer
                        .<Device>create()
                        .onResultHandler(entityManager -> DeviceDAO.find(entityManager, scopeId, entityId))
                        .onBeforeHandler(() -> (Device) entityCache.get(scopeId, entityId))
                        .onAfterHandler(entityCache::put));
    }

    @Override
    public Device findByClientId(KapuaId scopeId, String clientId) throws KapuaException {
        DeviceValidation.validateFindByClientIdPreconditions(scopeId, clientId);

        //
        // Check cache and/or do find
        Device device = (Device) ((DeviceRegistryCache) entityCache).getByClientId(scopeId, clientId);
        if (device == null) {
            DeviceQueryImpl query = new DeviceQueryImpl(scopeId);
            query.setPredicate(query.attributePredicate(DeviceAttributes.CLIENT_ID, clientId));
            query.setFetchAttributes(Lists.newArrayList(DeviceAttributes.CONNECTION, DeviceAttributes.LAST_EVENT));

            //
            // Query and parse result
            DeviceListResult result = query(query);
            if (!result.isEmpty()) {
                device = result.getFirstItem();
                entityCache.put(device);
            }
        }

        return device;
    }

    @Override
    public DeviceListResult query(KapuaQuery query)
            throws KapuaException {
        DeviceValidation.validateQueryPreconditions(query);

        //
        // Do query
        return entityManagerSession.doAction(
                EntityManagerContainer
                        .<DeviceListResult>create()
                        .onResultHandler(entityManager -> DeviceDAO.query(entityManager, query)));
    }

    @Override
    public long count(KapuaQuery query) throws KapuaException {
        DeviceValidation.validateCountPreconditions(query);

        // Do count
        return entityManagerSession.doAction(
                EntityManagerContainer
                        .<Long>create()
                        .onResultHandler(entityManager -> DeviceDAO.count(entityManager, query)));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId deviceId) throws KapuaException {
        DeviceValidation.validateDeletePreconditions(scopeId, deviceId);

        //
        // Do delete
        entityManagerSession.doTransactedAction(
                EntityManagerContainer
                        .create()
                        .onResultHandler(entityManager -> DeviceDAO.delete(entityManager, scopeId, deviceId))
                        .onAfterHandler((emptyParam) -> entityCache.remove(scopeId, deviceId)));
    }

    public void onKapuaEvent(ServiceEvent kapuaEvent) throws KapuaException {
        //@ListenServiceEvent(fromAddress="account")
        //@ListenServiceEvent(fromAddress="authorization")
        if (kapuaEvent == null) {
            //service bus error. Throw some exception?
        }
        LOGGER.info("DeviceRegistryService: received kapua event from {}, operation {}", kapuaEvent.getService(), kapuaEvent.getOperation());
        if ("group".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteDeviceByGroupId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        } else if ("account".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteDeviceByAccountId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        }
    }

    //
    // Private methods
    //

    private void deleteDeviceByGroupId(KapuaId scopeId, KapuaId groupId) throws KapuaException {
        KapuaLocator locator = KapuaLocator.getInstance();
        DeviceFactory deviceFactory = locator.getFactory(DeviceFactory.class);

        DeviceQuery query = deviceFactory.newQuery(scopeId);
        query.setPredicate(query.attributePredicate(DeviceAttributes.GROUP_ID, groupId));

        DeviceListResult devicesToDelete = query(query);

        for (Device d : devicesToDelete.getItems()) {
            d.setGroupId(null);
            update(d);
        }
    }

    private void deleteDeviceByAccountId(KapuaId scopeId, KapuaId accountId) throws KapuaException {
        KapuaLocator locator = KapuaLocator.getInstance();
        DeviceFactory deviceFactory = locator.getFactory(DeviceFactory.class);

        DeviceQuery query = deviceFactory.newQuery(accountId);

        DeviceListResult devicesToDelete = query(query);

        for (Device d : devicesToDelete.getItems()) {
            delete(d.getScopeId(), d.getId());
        }
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
