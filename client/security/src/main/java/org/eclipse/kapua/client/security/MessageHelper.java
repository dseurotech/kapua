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
package org.eclipse.kapua.client.security;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.eclipse.kapua.client.security.bean.EntityRequest;
import org.eclipse.kapua.client.security.bean.MessageConstants;
import org.eclipse.kapua.client.security.bean.AuthRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class MessageHelper {

    private static ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter writer = mapper.writer();//check if it's thread safe

    private MessageHelper() {
    }

    static TextMessage getBrokerConnectMessage(TextMessage message, AuthRequest authRequest) throws JMSException, JsonProcessingException {
        buildBaseMessage(message, authRequest);
        String textPayload = null;
        if (authRequest!=null) {
            textPayload = writer.writeValueAsString(authRequest);
            message.setText(textPayload);
        }
        return message;
    }

    static TextMessage getBrokerDisconnectMessage(TextMessage message, AuthRequest authRequest) throws JMSException, JsonProcessingException {
        buildBaseMessage(message, authRequest);
        String textPayload = null;
        if (authRequest!=null) {
            textPayload = writer.writeValueAsString(authRequest);
            message.setText(textPayload);
        }
        return message;
    }

    static TextMessage getEntityMessage(TextMessage message, EntityRequest entityRequest) throws JMSException, JsonProcessingException {
        buildBaseMessage(message, entityRequest);
        String textPayload = null;
        if (entityRequest!=null) {
            textPayload = writer.writeValueAsString(entityRequest);
            message.setText(textPayload);
        }
        return message;
    }

    static void buildBaseMessage(TextMessage message, AuthRequest authRequest) throws JMSException {
        message.setStringProperty(MessageConstants.HEADER_REQUEST_ID, authRequest.getRequestId());
        message.setStringProperty(MessageConstants.HEADER_ACTION, authRequest.getAction());
        message.setStringProperty(MessageConstants.HEADER_USERNAME, authRequest.getUsername());
        message.setStringProperty(MessageConstants.HEADER_CLIENT_ID, authRequest.getClientId());
        message.setStringProperty(MessageConstants.HEADER_CLIENT_IP, authRequest.getClientIp());
        message.setStringProperty(MessageConstants.HEADER_CONNECTION_ID, authRequest.getConnectionId());
    }

    static void buildBaseMessage(TextMessage message, EntityRequest entityRequest) throws JMSException {
        message.setStringProperty(MessageConstants.HEADER_REQUEST_ID, entityRequest.getRequestId());
        message.setStringProperty(MessageConstants.HEADER_ACTION, entityRequest.getAction());
        message.setStringProperty(MessageConstants.HEADER_NAME, entityRequest.getName());
    }

    static String getNewRequestId() {
        return UUID.randomUUID().toString();
    }
}
