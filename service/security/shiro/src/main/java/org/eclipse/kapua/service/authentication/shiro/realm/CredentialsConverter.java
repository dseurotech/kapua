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
package org.eclipse.kapua.service.authentication.shiro.realm;

import org.apache.shiro.authc.AuthenticationToken;
import org.eclipse.kapua.service.authentication.AuthenticationCredentials;
import org.eclipse.kapua.service.authentication.exception.KapuaAuthenticationException;

/**
 * {@link CredentialsConverter} definition.
 * <p>
 * It converts a {@link AuthenticationCredentials} to a specific implementation of Apache Shiro {@link AuthenticationToken}.
 *
 * @since 2.0.0
 */
public interface CredentialsConverter {

    /**
     * Whether the given {@link AuthenticationCredentials} is processable from this {@link CredentialsConverter}.
     *
     * @param credentials The {@link AuthenticationCredentials} to check.
     * @return {@code true} if it is processable, {@code false} if not.
     * @since 2.0.0
     */
    boolean canProcess(AuthenticationCredentials credentials);

    /**
     * Maps the {@link AuthenticationCredentials} to a specific Apache Shiro {@link AuthenticationToken}
     *
     * @param credentials The {@link AuthenticationCredentials} to map.
     * @return The corresponding Apache Shiro {@link AuthenticationToken}.
     * @throws KapuaAuthenticationException if the given {@link AuthenticationCredentials} are invalid.
     * @since 2.0.0
     */
    KapuaAuthenticationToken convertToShiro(AuthenticationCredentials credentials) throws KapuaAuthenticationException;
}
