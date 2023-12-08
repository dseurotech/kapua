/*******************************************************************************
 * Copyright (c) 2019, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.broker.artemis.plugin.security;

import java.util.Base64;
import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.remoting.FailureListener;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.utils.critical.CriticalComponent;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.kapua.broker.artemis.plugin.security.connector.AcceptorHandler;
import org.eclipse.kapua.broker.artemis.plugin.security.event.BrokerEvent;
import org.eclipse.kapua.broker.artemis.plugin.security.event.BrokerEvent.EventType;
import org.eclipse.kapua.broker.artemis.plugin.security.event.BrokerEventHanldler;
import org.eclipse.kapua.broker.artemis.plugin.security.metric.LoginMetric;
import org.eclipse.kapua.broker.artemis.plugin.security.metric.PublishMetric;
import org.eclipse.kapua.broker.artemis.plugin.security.metric.SubscribeMetric;
import org.eclipse.kapua.broker.artemis.plugin.security.setting.BrokerSetting;
import org.eclipse.kapua.broker.artemis.plugin.security.setting.BrokerSettingKey;
import org.eclipse.kapua.client.security.AuthErrorCodes;
import org.eclipse.kapua.client.security.KapuaIllegalDeviceStateException;
import org.eclipse.kapua.client.security.ServiceClient.SecurityAction;
import org.eclipse.kapua.client.security.bean.AuthRequest;
import org.eclipse.kapua.client.security.context.SessionContext;
import org.eclipse.kapua.client.security.context.Utils;
import org.eclipse.kapua.commons.metric.CommonsMetric;
import org.eclipse.kapua.commons.setting.system.SystemSetting;
import org.eclipse.kapua.commons.setting.system.SystemSettingKey;
import org.eclipse.kapua.commons.util.KapuaDateUtils;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authentication.exception.KapuaAuthenticationErrorCodes;
import org.eclipse.kapua.service.authentication.exception.KapuaAuthenticationException;
import org.eclipse.kapua.service.client.DatabaseCheckUpdate;
import org.eclipse.kapua.service.client.message.MessageConstants;
import org.eclipse.kapua.service.device.connection.listener.DeviceConnectionEventListenerService;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnection;
import org.eclipse.kapua.service.device.registry.connection.DeviceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer.Context;

/**
 * Server Plugin implementation.
 * This plugin does session cleanup on disconnection and enrich the message context with Kapua session infos.
 *
 */
public class ServerPlugin implements ActiveMQServerPlugin {

    protected static Logger logger = LoggerFactory.getLogger(ServerPlugin.class);

    private static final int DEFAULT_PUBLISHED_MESSAGE_SIZE_LOG_THRESHOLD = 100000;
    private static final String MISSING_TOPIC_SUFFIX = "MQTT.LWT";
    private static final String DISCONNECT_EVENT_OPERATION = "disconnect";

    enum Failure {
        CLOSED,
        FAILED,
        DESTROY
    }

    public static enum MessageType {

        Broker("BRK"),
        Control("CTR"),
        Telemetry("TEL"),
        System("SYS");

        private String asUrl;

        MessageType(String asUrl) {
            this.asUrl = asUrl;
        }

        public String getAsUrl() {
            return asUrl;
        }
    }

    /**
     * publish message size threshold for printing message information
     */
    private int publishInfoMessageSizeLimit;

    //TODO inject!!!
    private LoginMetric loginMetric;
    private PublishMetric publishMetric;
    private SubscribeMetric subscribeMetric;

    protected BrokerEventHanldler brokerEventHanldler;
    protected AcceptorHandler acceptorHandler;
    protected String version;
    protected ServerContext serverContext;

    protected DeviceConnectionEventListenerService deviceConnectionEventListenerService;

    public ServerPlugin() {
        //TODO find which is the right plugin to use to set this parameter (ServerPlugin or SecurityPlugin???)
        CommonsMetric.module = MetricsSecurityPlugin.BROKER_TELEMETRY;
        loginMetric = LoginMetric.getInstance();
        publishMetric = PublishMetric.getInstance();
        subscribeMetric = SubscribeMetric.getInstance();
        publishInfoMessageSizeLimit = BrokerSetting.getInstance().getInt(BrokerSettingKey.PUBLISHED_MESSAGE_SIZE_LOG_THRESHOLD, DEFAULT_PUBLISHED_MESSAGE_SIZE_LOG_THRESHOLD);
        //TODO find a proper way to initialize database
        DatabaseCheckUpdate databaseCheckUpdate = new DatabaseCheckUpdate();
        serverContext = ServerContext.getInstance();
        brokerEventHanldler = BrokerEventHanldler.getInstance();
        brokerEventHanldler.registerConsumer((brokerEvent) -> disconnectClient(brokerEvent));
        brokerEventHanldler.start();

        deviceConnectionEventListenerService = KapuaLocator.getInstance().getService(DeviceConnectionEventListenerService.class);
    }

    @Override
    public void registered(ActiveMQServer server) {
        logger.info("registering plugin {}...", this.getClass().getName());
        try {
            String clusterName = SystemSetting.getInstance().getString(SystemSettingKey.CLUSTER_NAME);
            serverContext.init(server, clusterName);
            acceptorHandler = new AcceptorHandler(server,
                BrokerSetting.getInstance().getMap(String.class, BrokerSettingKey.ACCEPTORS));
            //init acceptors
            acceptorHandler.syncAcceptors();

            deviceConnectionEventListenerService.addReceiver(serviceEvent -> processDeviceConnectionEvent(serviceEvent));
        } catch (Exception e) {
            logger.error("Error while initializing {} plugin: {}", this.getClass().getName(), e.getMessage(), e);
        }
        logger.info("registering plugin {}... DONE", this.getClass().getName());
        ActiveMQServerPlugin.super.registered(server);
    }

    @Override
    public void unregistered(ActiveMQServer server) {
        logger.info("Unregistering plugin {}...", this.getClass().getName());
        ActiveMQServerPlugin.super.unregistered(server);
        try {
            serverContext.shutdown(server);
        } catch (Exception e) {
            logger.error("Error while stopping {} plugin: {}", this.getClass().getName(), e.getMessage(), e);
        }
        logger.info("Unregistering plugin {}... DONE", this.getClass().getName());
    }

    @Override
    public void init(Map<String, String> properties) {
        version = properties.get("version");
        logger.info("Init plugin {} (version {})", this.getClass().getName(), version);
        ActiveMQServerPlugin.super.init(properties);
    }

    /**
     * CONNECT
     */
    @Override
    public void afterCreateConnection(RemotingConnection connection) throws ActiveMQException {
        connection.addCloseListener(() -> cleanUpConnectionData(connection, Failure.CLOSED));
        connection.addFailureListener(new FailureListener() {

            @Override
            public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID) {
                cleanUpConnectionData(connection, Failure.FAILED, exception);
            }

            @Override
            public void connectionFailed(ActiveMQException exception, boolean failedOver) {
                cleanUpConnectionData(connection, Failure.FAILED, exception);
            }
        });
        ActiveMQServerPlugin.super.afterCreateConnection(connection);
    }

    /**
     * DISCONNECT
     */
    @Override
    public void afterDestroyConnection(RemotingConnection connection) throws ActiveMQException {
        ActiveMQServerPlugin.super.afterDestroyConnection(connection);
        cleanUpConnectionData(connection, Failure.DESTROY);
    }

    /**
     * SUBSCRIBE
     */
    @Override
    public void afterCreateConsumer(ServerConsumer consumer) throws ActiveMQException {
        subscribeMetric.getAllowedMessages().inc();
        ActiveMQServerPlugin.super.afterCreateConsumer(consumer);
    }

    /**
     * PUBLISH
     */
    @Override
    public void beforeSend(ServerSession session, Transaction tx, Message message, boolean direct,
        boolean noAutoCreateQueue) throws ActiveMQException {
        String address = message.getAddress();
        int messageSize = message.getEncodeSize();
        SessionContext sessionContext = serverContext.getSecurityContext().getSessionContextWithCacheFallback(PluginUtility.getConnectionId(session));
        logger.debug("Publishing message on address {} from clientId: {} - clientIp: {}", address, sessionContext.getClientId(), sessionContext.getClientIp());
        message.putStringProperty(MessageConstants.HEADER_KAPUA_CLIENT_ID, sessionContext.getClientId());
        message.putStringProperty(MessageConstants.HEADER_KAPUA_CONNECTOR_NAME, sessionContext.getConnectorName());
        message.putStringProperty(MessageConstants.HEADER_KAPUA_SESSION, Base64.getEncoder().encodeToString(SerializationUtils.serialize(sessionContext.getKapuaSession())));
        message.putLongProperty(MessageConstants.HEADER_KAPUA_RECEIVED_TIMESTAMP, KapuaDateUtils.getKapuaSysDate().getEpochSecond());
        message.putStringProperty(MessageConstants.HEADER_KAPUA_MESSAGE_TYPE, getMessgeType(address));
        if (!sessionContext.isInternal()) {
            if (isLwt(address)) {
                //handle the missing message case
                logger.info("Detected missing message for client {}... Flag session to tell disconnector to avoid disconnect event sending", sessionContext.getClientId());
                sessionContext.setMissing(true);
            }
            // FIX #164
            message.putStringProperty(MessageConstants.HEADER_KAPUA_CONNECTION_ID, Base64.getEncoder().encodeToString(SerializationUtils.serialize(sessionContext.getKapuaConnectionId())));
            message.putBooleanProperty(MessageConstants.HEADER_KAPUA_BROKER_CONTEXT, false);
            if (publishInfoMessageSizeLimit < messageSize) {
                logger.info("Published message size over threshold. size: {} - destination: {} - account id: {} - username: {} - clientId: {}",
                        messageSize, address, sessionContext.getAccountName(), sessionContext.getUsername(), sessionContext.getClientId());
            }
        } else {
            if (publishInfoMessageSizeLimit < messageSize) {
                logger.info("Published message size over threshold. size: {} - destination: {}",
                        messageSize, address);
            }
            message.putBooleanProperty(MessageConstants.HEADER_KAPUA_BROKER_CONTEXT, true);
        }
        message.putStringProperty(MessageConstants.PROPERTY_ORIGINAL_TOPIC, address);
        publishMetric.getMessageSizeAllowed().update(messageSize);
        serverContext.getAddressAccessTracker().update(address);
        logger.debug("Published message on address {} from clientId: {} - clientIp: {}", address, sessionContext.getClientId(), sessionContext.getClientIp());
        ActiveMQServerPlugin.super.beforeSend(session, tx, message, direct, noAutoCreateQueue);
    }

    private boolean isLwt(String originalTopic) {
        return originalTopic != null && originalTopic.endsWith(MISSING_TOPIC_SUFFIX);
    }

    private String getMessgeType(String address) {
        if (address!=null) {
            if (address.startsWith("active")) {
                return MessageType.Broker.getAsUrl();
            }
            else if (address.startsWith("$")) {
                if (address.startsWith("$SYS")) {
                    return MessageType.System.getAsUrl();
                }
                else {
                    return MessageType.Control.getAsUrl();
                }
            }
            else {
                return MessageType.Telemetry.getAsUrl();
            }
        }
        return "N/A";
    }

    /**
     * UTILS
     * @throws ActiveMQException
     */

    private int disconnectClient(BrokerEvent brokerEvent) {
        int disconnectedClients = 0;
        if (EventType.disconnectClientByClientId.equals(brokerEvent.getEventType())) {
            disconnectedClients = disconnectClient(brokerEvent.getScopeId(), brokerEvent.getClientId());
        }
        else if (EventType.disconnectClientByConnectionId.equals(brokerEvent.getEventType())) {
            disconnectedClients = disconnectClient(brokerEvent.getOldConnectionId());
        }
        logger.info("Disconnected clients: {}", disconnectedClients);
        loginMetric.getDisconnectByEvent().inc(disconnectedClients);
        return disconnectedClients;
    }

    private int disconnectClient(KapuaId scopeId, String clientId) {
        logger.info("Disconnecting client for scopeId: {} - client id: {}", scopeId.toCompactId(), clientId);
        String fullClientId = Utils.getFullClientId(scopeId, clientId);
        return serverContext.getServer().getSessions().stream().map(session -> {
            RemotingConnection remotingConnection = session.getRemotingConnection();
            String clientIdToCheck = PluginUtility.getConnectionId(remotingConnection);
            SessionContext sessionContext = serverContext.getSecurityContext().getSessionContextByClientId(clientIdToCheck);
            String connectionFullClientId = Utils.getFullClientId(sessionContext);
            if (fullClientId.equals(connectionFullClientId)) {
                logger.info("\tclientId to check: {} - full client id: {}... CLOSE", clientIdToCheck, connectionFullClientId);
                remotingConnection.disconnect(false);
                remotingConnection.destroy();
                return 1;
            }
            else {
                logger.info("\tclientId to check: {} - full client id: {}... no action", clientIdToCheck, connectionFullClientId);
                return 0;
            }
        }).mapToInt(Integer::new).sum();
    }

    private int disconnectClient(String connectionId) {
        logger.info("Disconnecting client for connection: {}", connectionId);
        return serverContext.getServer().getRemotingService().getConnections().stream().map(remotingConnection -> {
            int removed = 0;
            String connectionIdTmp = PluginUtility.getConnectionId(remotingConnection);
            if (connectionId.equals(connectionIdTmp)) {
                logger.info("\tconnection: {} - compared to: {} ... CLOSE", connectionId, connectionIdTmp);
                remotingConnection.disconnect(false);
                remotingConnection.destroy();
                removed++;
            }
            else {
                logger.info("\tclientId to check: {} - compared to: {} ... no action", connectionId, connectionIdTmp);
            }
            return removed;
        }).mapToInt(Integer::new).sum();
    }

    protected void processDeviceConnectionEvent(ServiceEvent event) {
        logger.debug("Received event: {}", event);

        if(!DISCONNECT_EVENT_OPERATION.equals(event.getOperation())) {
            logger.debug("Ignoring event with operation: {}", event.getOperation());
            return;
        }

        try {
            DeviceConnection deviceConnection = KapuaLocator.getInstance().getService(DeviceConnectionService.class).find(event.getEntityScopeId(), event.getEntityId());
            if(deviceConnection == null) {
                logger.warn("DeviceConnection not found - scopeId: {}, id: {} - ", event.getEntityScopeId(), event.getEntityId());
                return;
            }

            String fullClientId = Utils.getFullClientId(deviceConnection.getScopeId(), deviceConnection.getClientId());
            SessionContext sessionContext = serverContext.getSecurityContext().getSessionContextByClientId(fullClientId);
            BrokerEvent disconnectEvent = new BrokerEvent(EventType.disconnectClientByConnectionId, sessionContext, sessionContext);

            logger.info("Submitting broker event to disconnect clientId: {}, connectionId: {}", fullClientId, sessionContext.getConnectionId());
            BrokerEventHanldler.getInstance().enqueueEvent(disconnectEvent);
        } catch (Exception e) {
            logger.warn("Error processing event: {}", e.getMessage());
        }
    }

    @Override
    public void duplicateSessionMetadataFailure(ServerSession session, String key, String data) throws ActiveMQException {
        logger.error("Duplicate session for key: {} - data: {}", key, data);
        loginMetric.getDuplicateSessionMetadataFailure().inc();
        ActiveMQServerPlugin.super.duplicateSessionMetadataFailure(session, key, data);
    }

    @Override
    public void criticalFailure(CriticalComponent components) throws ActiveMQException {
        logger.error("Critical failure on component {}", components.toString());
        ActiveMQServerPlugin.super.criticalFailure(components);
    }

    private void cleanUpConnectionData(RemotingConnection connection, Failure reason) {
        cleanUpConnectionData(connection, reason, null);
    }

    private void cleanUpConnectionData(RemotingConnection connection, Failure reason, Exception exception) {
        Context timeTotal = loginMetric.getRemoveConnection().time();
        try {
            String connectionId = PluginUtility.getConnectionId(connection);
            serverContext.getSecurityContext().updateConnectionTokenOnDisconnection(connectionId);
            logger.info("### cleanUpConnectionData connection: {} - reason: {} - Error: {}", connectionId, reason, exception!=null?exception.getMessage():"N/A");
            if (exception!=null && logger.isDebugEnabled()) {
                logger.debug("### cleanUpConnectionData error", exception);
            }
            SessionContext sessionContext = serverContext.getSecurityContext().getSessionContext(connectionId);
            if (sessionContext!=null) {
                SessionContext sessionContextByClient = serverContext.getSecurityContext().cleanSessionContext(sessionContext);
                if (!PluginUtility.isInternal(connection)) {
                    AuthRequest authRequest = new AuthRequest(
                        serverContext.getClusterName(),
                        serverContext.getBrokerIdentity().getBrokerHost(),
                        SecurityAction.brokerDisconnect.name(), sessionContext);
                    if (exception!=null) {
                        updateError(authRequest, exception);
                    }
                    serverContext.getSecurityContext().updateStealingLinkAndIllegalState(authRequest, connectionId, sessionContextByClient!=null ? sessionContextByClient.getConnectionId() : null);
                    serverContext.getAuthServiceClient().brokerDisconnect(authRequest);
                }
            }
            else {
                logger.warn("Cannot find any session context for connection id: {}", connectionId);
                loginMetric.getCleanupNullSessionFailure().inc();
            }
        }
        catch (Exception e) {
            loginMetric.getCleanupGenericFailure().inc();
            logger.error("Cleanup connection data error: {}", e.getMessage(), e);
        }
        finally {
            timeTotal.stop();
        }
    }

    private void updateError(AuthRequest authRequest, Exception exception) {
        //Exception must be not null!
        authRequest.setExceptionClass(exception.getClass().getName());
        String errorCode = KapuaAuthenticationErrorCodes.AUTHENTICATION_ERROR.name();
        if (exception instanceof ActiveMQException) {
            ActiveMQException activeMQException = (ActiveMQException) exception;
            //analyze the exception code
            ActiveMQExceptionType exceptionType = activeMQException.getType();
            if (!ActiveMQExceptionType.REMOTE_DISCONNECT.equals(exceptionType)) {
                errorCode = AuthErrorCodes.UNEXPECTED_STATUS.name();
            }
        }
        else if (exception instanceof KapuaIllegalDeviceStateException) {
            AuthErrorCodes authErrorCode = (AuthErrorCodes)((KapuaIllegalDeviceStateException) exception).getCode();
            if (authErrorCode!=null) {
                errorCode = authErrorCode.name();
            }
        }
        else if (exception instanceof KapuaAuthenticationException) {
            KapuaAuthenticationErrorCodes authErrorCode = (KapuaAuthenticationErrorCodes)((KapuaAuthenticationException) exception).getCode();
            if (authErrorCode!=null) {
                errorCode = authErrorCode.name();
            }
        }
        authRequest.setErrorCode(errorCode);
    }
}