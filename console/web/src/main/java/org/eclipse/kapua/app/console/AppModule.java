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
package org.eclipse.kapua.app.console;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.util.xml.JAXBContextProvider;

import com.google.inject.Provides;

public class AppModule extends AbstractKapuaModule {

    @Override
    protected void configureModule() {

    }

    @Provides
    @Named("metricModuleName")
    String metricModuleName() {
        return "web-console";
    }

    @Provides
    @Named("eventsModuleName")
    String eventModuleName() {
        return "console";
    }

    @Provides
    @Singleton
    JAXBContextProvider jaxbContextProvider() {
        return new ConsoleJAXBContextProvider();
    }
}
