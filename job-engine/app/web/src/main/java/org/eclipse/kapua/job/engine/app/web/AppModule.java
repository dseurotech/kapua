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
package org.eclipse.kapua.job.engine.app.web;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.liquibase.DatabaseCheckUpdate;
import org.eclipse.kapua.commons.util.xml.JAXBContextProvider;
import org.eclipse.kapua.job.engine.app.web.jaxb.JobEngineJAXBContextProvider;

import com.google.inject.Provides;

public class AppModule extends AbstractKapuaModule {

    @Override
    protected void configureModule() {
        bind(DatabaseCheckUpdate.class).asEagerSingleton();
    }

    @Provides
    @Named("metricModuleName")
    String metricModuleName() {
        return "job-engine";
    }

    @Provides
    @Named("eventsModuleName")
    String eventModuleName() {
        return "job_engine";
    }

    @Provides
    @Singleton
    JAXBContextProvider jaxbContextProvider() {
        return new JobEngineJAXBContextProvider();
    }
}
