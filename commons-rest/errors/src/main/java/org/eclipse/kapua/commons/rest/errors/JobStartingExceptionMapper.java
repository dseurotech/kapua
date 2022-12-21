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
package org.eclipse.kapua.commons.rest.errors;

import org.eclipse.kapua.commons.rest.model.errors.JobStartingExceptionInfo;
import org.eclipse.kapua.job.engine.exception.JobStartingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JobStartingExceptionMapper implements ExceptionMapper<JobStartingException> {

    private static final Logger LOG = LoggerFactory.getLogger(JobStartingExceptionMapper.class);

    private final boolean showStackTrace;

    @Inject
    public JobStartingExceptionMapper(ExceptionConfigurationProvider exceptionConfigurationProvider) {
        this.showStackTrace = exceptionConfigurationProvider.showStackTrace();
    }

    @Override
    public Response toResponse(JobStartingException jobStartingException) {
        LOG.error(jobStartingException.getMessage(), jobStartingException);

        return Response
                .status(Status.INTERNAL_SERVER_ERROR)
                .entity(new JobStartingExceptionInfo(jobStartingException, showStackTrace))
                .build();
    }

}
