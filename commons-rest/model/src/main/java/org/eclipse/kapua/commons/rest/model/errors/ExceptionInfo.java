/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.commons.rest.model.errors;

import org.eclipse.kapua.KapuaException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "exceptionInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExceptionInfo extends ThrowableInfo {

    @XmlElement(name = "kapuaErrorCode")
    private String kapuaErrorCode;

    /**
     * Constructor.
     *
     * @since 1.0.0
     */
    protected ExceptionInfo() {
        super();
    }

    /**
     * Constructor.
     *
     * @param httpStatus The {@link Status} of the {@link Response}
     * @param exception  The cause of the error.
     * @since 2.0.0
     */
    public ExceptionInfo(Status httpStatus, KapuaException exception, boolean showStackTrace) {
        super(httpStatus, exception, showStackTrace);

        this.kapuaErrorCode = exception.getCode().name();
    }

    /**
     * Gets the {@link KapuaException#getCode()}.
     *
     * @return The {@link KapuaException#getCode()}.
     * @since 1.0.0
     */
    public String getKapuaErrorCode() {
        return kapuaErrorCode;
    }
}
