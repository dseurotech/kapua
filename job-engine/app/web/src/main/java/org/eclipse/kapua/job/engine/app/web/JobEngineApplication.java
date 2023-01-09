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
package org.eclipse.kapua.job.engine.app.web;

import org.eclipse.kapua.commons.rest.errors.ExceptionConfigurationProvider;
import org.eclipse.kapua.job.engine.app.web.jaxb.JaxbContextResolver;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.UriConnegFilter;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;

public class JobEngineApplication extends ResourceConfig {

    public JobEngineApplication() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                this.bind(ExceptionConfigurationProviderImpl.class)
                        .to(ExceptionConfigurationProvider.class)
                        .in(Singleton.class);
            }
        });
        packages("org.eclipse.kapua.commons.rest", "org.eclipse.kapua.job.engine.app", "org.eclipse.kapua.app.api.core");

        // Bind media type to resource extension
        HashMap<String, MediaType> mappedMediaTypes = new HashMap<>();
        mappedMediaTypes.put("json", MediaType.APPLICATION_JSON_TYPE);

        property(ServerProperties.MEDIA_TYPE_MAPPINGS, mappedMediaTypes);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
        register(UriConnegFilter.class);
        register(JacksonFeature.class);
        register(JaxbContextResolver.class);
    }

}
