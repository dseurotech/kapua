/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.service.device.management.message;

/**
 * Kapua request/reply method definition.<br>
 * This object defines the request types that can be sent to a Device.
 *
 * @since 1.0.0
 */
public enum KapuaMethod {
    /**
     * Read request.
     *
     * @since 1.0.0
     */
    READ,
    /**
     * Same as {@link #READ} but with a name that matches Kura naming.
     *
     * @since 1.2.0
     */
    GET,
    /**
     * Create request.
     *
     * @since 1.0.0
     */
    CREATE,
    /**
     * Same as {@link #CREATE} but with a name that matches Kura naming.
     *
     * @since 1.2.0
     */
    POST,
    /**
     * Write request.
     *
     * @since 1.0.0
     */
    WRITE,
    /**
     * Same as {@link #WRITE} but with a name that matches Kura naming.
     *
     * @since 1.2.0
     */
    PUT,
    /**
     * Delete request.
     *
     * @since 1.0.0
     */
    DELETE,
    /**
     * Same as {@link #DELETE} but with a name that matches Kura naming.
     *
     * @since 1.2.0
     */
    DEL,
    /**
     * Execute request.
     *
     * @since 1.0.0
     */
    EXECUTE,
    /**
     * Same as {@link #EXECUTE} but with a name that matches Kura naming.
     *
     * @since 1.2.0
     */
    EXEC,
    /**
     * Options request.
     *
     * @since 1.0.0
     */
    OPTIONS;

    /**
     * Converts the value
     *
     * @return
     */
    public KapuaMethod normalizeAction() {

        switch (this) {
            case POST:
                return KapuaMethod.CREATE;
            case GET:
                return KapuaMethod.READ;
            case DEL:
                return KapuaMethod.DELETE;
            case EXEC:
                return KapuaMethod.EXECUTE;
            case PUT:
                return KapuaMethod.WRITE;
            default:
                return this;
        }
    }
}
