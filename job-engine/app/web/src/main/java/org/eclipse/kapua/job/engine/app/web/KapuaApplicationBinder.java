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

import org.eclipse.kapua.job.engine.rest.service.errors.ExceptionConfigurationProvider;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;

public class KapuaApplicationBinder extends AbstractBinder {
    @Override
    protected void configure() {
        this.bind(ExceptionConfigurationProviderImpl.class)
                .to(ExceptionConfigurationProvider.class)
                .in(Singleton.class);
    }
}
