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
package org.eclipse.kapua.consumer.telemetry.listener;

import com.codahale.metrics.Counter;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriEndpoint;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.metric.MetricServiceFactory;
import org.eclipse.kapua.commons.metric.MetricsService;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.service.camel.listener.AbstractProcessor;
import org.eclipse.kapua.service.camel.message.CamelKapuaMessage;
import org.eclipse.kapua.service.datastore.MessageStoreService;
import org.eclipse.kapua.service.datastore.internal.mediator.DatastoreCommunicationException;
import org.eclipse.kapua.service.device.management.asset.store.DeviceAssetStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data storage message listener
 *
 * @since 1.0
 */
@UriEndpoint(title = "Data storage message processor", syntax = "bean:dataMessageProcessor", scheme = "bean")
public class DataStorageMessageProcessor extends AbstractProcessor<CamelKapuaMessage<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(DataStorageMessageProcessor.class);

    private final MessageStoreService messageStoreService = KapuaLocator.getInstance().getService(MessageStoreService.class);

    private final DeviceAssetStoreService deviceAssetStoreService = KapuaLocator.getInstance().getService(DeviceAssetStoreService.class);

    // queues counters
    private final Counter metricQueueCommunicationErrorCount;
    private final Counter metricQueueConfigurationErrorCount;
    private final Counter metricQueueGenericErrorCount;

    public DataStorageMessageProcessor() {
        super("DataStorage");
        MetricsService metricService = MetricServiceFactory.getInstance();

        metricQueueCommunicationErrorCount = metricService.getCounter(DataStoreMetrics.METRIC_MODULE_NAME, DataStoreMetrics.METRIC_COMPONENT_NAME, DataStoreMetrics.METRIC_STORE, DataStoreMetrics.METRIC_QUEUE, DataStoreMetrics.METRIC_COMMUNICATION, DataStoreMetrics.METRIC_ERROR, DataStoreMetrics.METRIC_COUNT);
        metricQueueConfigurationErrorCount = metricService.getCounter(DataStoreMetrics.METRIC_MODULE_NAME, DataStoreMetrics.METRIC_COMPONENT_NAME, DataStoreMetrics.METRIC_STORE, DataStoreMetrics.METRIC_QUEUE, DataStoreMetrics.METRIC_CONFIGURATION, DataStoreMetrics.METRIC_ERROR, DataStoreMetrics.METRIC_COUNT);
        metricQueueGenericErrorCount = metricService.getCounter(DataStoreMetrics.METRIC_MODULE_NAME, DataStoreMetrics.METRIC_COMPONENT_NAME, DataStoreMetrics.METRIC_STORE, DataStoreMetrics.METRIC_QUEUE, DataStoreMetrics.METRIC_GENERIC, DataStoreMetrics.METRIC_ERROR, DataStoreMetrics.METRIC_COUNT);
    }

    /**
     * Process a data message.
     *
     * @throws KapuaException
     */
    @Override
    public void processMessage(CamelKapuaMessage<?> message) throws KapuaException {
        // data messages
        LOG.debug("Received data message from device channel: client id '{}' - {}", message.getMessage().getClientId(), message.getMessage().getChannel());
        try {
            messageStoreService.store(message.getMessage(), message.getDatastoreId());
        } catch (DatastoreCommunicationException e) {
            message.setDatastoreId(e.getUuid());
            throw e;
        }

        // Update asset values in AssetStoreService
        if (message.getMessage().getChannel().toString().startsWith("W1/A1")) {
            deviceAssetStoreService.storeAssetValues((KapuaDataMessage) message.getMessage());
        } else {
            LOG.debug("This message did not matched W1/A1. Channel was: {}", message.getMessage().getChannel());
        }
    }

    public void processCommunicationErrorMessage(Exchange exchange, CamelKapuaMessage<?> message) throws KapuaException {
        LOG.info("Message datastoreId: '{}' - Message Id: '{}'", message.getDatastoreId(), message.getMessage().getId());
        processMessage(message);
        metricQueueCommunicationErrorCount.dec();
    }

    public void processConfigurationErrorMessage(Exchange exchange, CamelKapuaMessage<?> message) throws KapuaException {
        LOG.info("Message datastoreId: '{}' - Message Id '{}'", message.getDatastoreId(), message.getMessage().getId());
        metricQueueConfigurationErrorCount.dec();
    }

    public void processGenericErrorMessage(Exchange exchange, CamelKapuaMessage<?> message) throws KapuaException {
        LOG.info("Message datastoreId: '{}' - Message Id '{}'", message.getDatastoreId(), message.getMessage().getId());
        metricQueueGenericErrorCount.dec();
    }

}
