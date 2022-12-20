/*******************************************************************************
 * Copyright (c) 2020, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.job.engine.rest.service.errors;

import org.eclipse.kapua.job.engine.rest.model.errors.SelfManagedOnlyExceptionInfo;
import org.eclipse.kapua.service.authentication.exception.SelfManagedOnlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SelfManagedOnlyExceptionMapper implements ExceptionMapper<SelfManagedOnlyException> {

    private static final Logger LOG = LoggerFactory.getLogger(SelfManagedOnlyExceptionMapper.class);

    private final boolean showStackTrace;

    @Inject
    public SelfManagedOnlyExceptionMapper(ExceptionConfigurationProvider exceptionConfigurationProvider) {
        this.showStackTrace = exceptionConfigurationProvider.showStackTrace();
    }

    @Override
    public Response toResponse(SelfManagedOnlyException exception) {
        LOG.error("Only self managed allowed!", exception);
        return Response//
                .status(Status.FORBIDDEN) //
                .entity(new SelfManagedOnlyExceptionInfo(Status.FORBIDDEN, exception, showStackTrace)) //
                .build();
    }
}
