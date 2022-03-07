/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.authentication;

import javax.inject.Inject;
import javax.jms.JMSException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.UriEndpoint;
import org.eclipse.kapua.client.security.bean.AccountRequest;
import org.eclipse.kapua.client.security.bean.AccountResponse;
import org.eclipse.kapua.client.security.bean.MessageConstants;
import org.eclipse.kapua.client.security.bean.Request;
import org.eclipse.kapua.client.security.bean.AuthRequest;
import org.eclipse.kapua.client.security.bean.AuthResponse;
import org.eclipse.kapua.service.camel.CommonMetrics;
import org.eclipse.kapua.service.camel.listener.AbstractListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@UriEndpoint(title = "authentication service listener", syntax = "bean:authenticationServiceListener", scheme = "bean")
public class AuthenticationServiceListener extends AbstractListener {

    protected static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceListener.class);

    public static final String METRIC_AUTHENTICATION = "authentication";
    public static final String METRIC_LOGIN = "login";
    public static final String METRIC_LOGOUT = "logout";
    public static final String METRIC_GET_ACCOUNT = "getAccount";

    private Counter metricLoginCount;
    private Counter metricLoginRequestCount;
    private Counter metricLogoutCount;
    private Counter metricLogoutRequestCount;
    private Counter metricGetAccountCount;
    private Counter metricGetAccountRequestCount;

    @Inject
    private AuthenticationServiceBackEndCall authenticationServiceBackEndCall;

    public AuthenticationServiceListener() {
        super(METRIC_AUTHENTICATION);
        metricLoginCount = registerCounter(CommonMetrics.MESSAGES, METRIC_LOGIN, CommonMetrics.COUNT);
        metricLoginRequestCount = registerCounter(CommonMetrics.MESSAGES, METRIC_LOGIN, CommonMetrics.REQUEST, CommonMetrics.COUNT);
        metricLogoutCount = registerCounter(CommonMetrics.MESSAGES, METRIC_LOGOUT, CommonMetrics.COUNT);
        metricLogoutRequestCount = registerCounter(CommonMetrics.MESSAGES, METRIC_LOGOUT, CommonMetrics.REQUEST, CommonMetrics.COUNT);
        metricGetAccountCount = registerCounter(CommonMetrics.MESSAGES, METRIC_GET_ACCOUNT, CommonMetrics.COUNT);
        metricGetAccountRequestCount = registerCounter(CommonMetrics.MESSAGES, METRIC_GET_ACCOUNT, CommonMetrics.REQUEST, CommonMetrics.COUNT);
    }

    private static ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter writer = mapper.writer();//check if it's thread safe

    public void brokerConnect(Exchange exchange, AuthRequest authRequest) throws JsonProcessingException, JMSException {
        metricLoginRequestCount.inc();
        logRequest(exchange, authRequest);
        AuthResponse authResponse = authenticationServiceBackEndCall.brokerConnect(authRequest);
        updateMessage(exchange, authRequest, authResponse);
        metricLoginCount.inc();
    }

    public void brokerDisconnect(Exchange exchange, AuthRequest authRequest) throws JsonProcessingException, JMSException {
        metricLogoutRequestCount.inc();
        logRequest(exchange, authRequest);
        logger.info("Checking stealing link for clientId: {} old connection: {} new connection: {}", authRequest.getClientId(), authRequest.getOldConnectionId(), authRequest.getConnectionId());
        AuthResponse authResponse = authenticationServiceBackEndCall.brokerDisconnect(authRequest);
        updateMessage(exchange, authRequest, authResponse);
        metricLogoutCount.inc();
    }

    public void getAccount(Exchange exchange, AccountRequest accountRequest) throws JsonProcessingException, JMSException {
        metricGetAccountRequestCount.inc();
        logRequest(exchange, accountRequest);
        AccountResponse accountResponse = authenticationServiceBackEndCall.getAccount(accountRequest);
        updateMessage(exchange, accountRequest, accountResponse);
        metricGetAccountCount.inc();
    }

    public void updateMessage(Exchange exchange, AuthRequest authRequest, AuthResponse authResponse) throws JMSException, JsonProcessingException {
        Message message = exchange.getIn();
        String textPayload = null;
        if (authResponse!=null) {
            textPayload = writer.writeValueAsString(authResponse);
            message.setBody(textPayload, String.class);
        }
        message.setHeader(MessageConstants.HEADER_REQUESTER, authRequest.getRequester());
        message.setHeader(MessageConstants.HEADER_REQUEST_ID, authRequest.getRequestId());
        message.setHeader(MessageConstants.HEADER_ACTION, authRequest.getAction());
        message.setHeader(MessageConstants.HEADER_USERNAME, authRequest.getUsername());
        message.setHeader(MessageConstants.HEADER_CLIENT_ID, authRequest.getClientId());
        message.setHeader(MessageConstants.HEADER_CLIENT_IP, authRequest.getClientIp());
        message.setHeader(MessageConstants.HEADER_CONNECTION_ID, authRequest.getConnectionId());
        message.setHeader(MessageConstants.HEADER_RESULT_CODE, authResponse.getResultCode());
        message.setHeader(MessageConstants.HEADER_ERROR_CODE, authResponse.getErrorCode());
    }

    public void updateMessage(Exchange exchange, AccountRequest accountRequest, AccountResponse accountResponse) throws JMSException, JsonProcessingException {
        Message message = exchange.getIn();
        String textPayload = null;
        if (accountResponse!=null) {
            textPayload = writer.writeValueAsString(accountResponse);
            message.setBody(textPayload, String.class);
        }
        message.setHeader(MessageConstants.HEADER_REQUESTER, accountRequest.getRequester());
        message.setHeader(MessageConstants.HEADER_REQUEST_ID, accountRequest.getRequestId());
        message.setHeader(MessageConstants.HEADER_ACTION, accountRequest.getAction());
        message.setHeader(MessageConstants.HEADER_USERNAME, accountRequest.getUsername());
        message.setHeader(MessageConstants.HEADER_RESULT_CODE, accountResponse.getResultCode());
        message.setHeader(MessageConstants.HEADER_ERROR_CODE, accountResponse.getErrorCode());
    }

    private void logRequest(Exchange exchange, Request request) {
        logger.info("Message id: {} - request id: {} - action: {} - requester: {}",
            exchange.getIn().getMessageId(),
            request.getRequestId(), request.getAction(), request.getRequester());
    }
}
