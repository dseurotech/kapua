/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.client.security.context;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kapua.client.security.bean.ConnectionInfo;
import org.eclipse.kapua.commons.model.id.KapuaEid;
import org.eclipse.kapua.commons.security.KapuaSession;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authentication.KapuaPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionContext {

    protected static final Logger logger = LoggerFactory.getLogger(SessionContext.class);

    public static final String PARAM_KEY_PROFILE_ADMIN = "profile_admin";
    public static final String PARAM_KEY_STATUS_MISSING = "status_missing";

    private KapuaPrincipal principal;
    private KapuaId scopeId;
    private String accountName;
    private KapuaId userId;
    private String username;
    private String clientId;
    private String clientIp;
    private String brokerId;

    private String connectionId;
    private KapuaId kapuaConnectionId;
    private String brokerHost;
    private String connectorName;
    private String transportProtocol;
    private KapuaSession kapuaSession;

    private Map<String, Object> properties = new HashMap<>();

    public SessionContext(KapuaPrincipal principal, ConnectionInfo connectionInfo, String kapuaConnectionId, String brokerId, String brokerHost) {
        this(principal, connectionInfo, brokerId, brokerHost);
        this.kapuaConnectionId = KapuaEid.parseCompactId(kapuaConnectionId);
    }

    public SessionContext(KapuaPrincipal principal, ConnectionInfo connectionInfo, String brokerId, String brokerHost) {
        this.principal = principal;
        username = principal.getName();
        kapuaSession = new KapuaSession(principal);
        this.scopeId = principal.getAccountId();
        this.userId = principal.getUserId();
        this.brokerId = brokerId;
        this.brokerHost = brokerHost;
        connectionId = connectionInfo.getConnectionId();
        clientId = connectionInfo.getClientId();
        clientIp = connectionInfo.getClientIp();
        connectorName = connectionInfo.getConnectorName();
        transportProtocol = connectionInfo.getTransportProtocol();
    }

    public boolean isInternal() {
        return principal.isInternal();
    }

    public void setAdmin(boolean admin) {
        setProperty(PARAM_KEY_PROFILE_ADMIN, admin);
    }

    public boolean isAdmin() {
        return getProperty(PARAM_KEY_PROFILE_ADMIN, Boolean.FALSE);
    }

    public void setMissing(boolean missing) {
        setProperty(PARAM_KEY_STATUS_MISSING, missing);
    }

    public boolean isMissing() {
        return getProperty(PARAM_KEY_STATUS_MISSING, Boolean.FALSE);
    }

    public KapuaPrincipal getPrincipal() {
        return principal;
    }

    public KapuaId getScopeId() {
        return scopeId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public KapuaId getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public KapuaId getKapuaConnectionId() {
        return kapuaConnectionId;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    private void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public KapuaSession getKapuaSession() {
        return kapuaSession;
    }

    /**
    *
    * @param key
    * @param defaultValue
    *
    * @throws ClassCastException if the property object is different from what expected by the caller (in other words no check before casting is done!)
    * @return
    */
    public <T> T getProperty(String key, T defaultValue) {
        T value = (T)properties.get(key);
        return value!=null ? value : defaultValue;
    }

}
