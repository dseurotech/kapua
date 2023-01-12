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
package org.eclipse.kapua.consumer.telemetry;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.util.xml.JAXBContextProvider;
import org.eclipse.kapua.commons.util.xml.XmlUtil;
import org.eclipse.kapua.locator.KapuaLocator;
import org.slf4j.LoggerFactory;

public class TelemetryJAXBContextLoader {

    public void init() {
        LoggerFactory.getLogger(this.getClass()).info("HERE!!!!!!");

        final JAXBContextProvider factory = KapuaLocator.getInstance().getFactory(JAXBContextProvider.class);
        XmlUtil.setContextProvider(factory);
        try {
            XmlUtil.getContextProvider().getJAXBContext();
        } catch (KapuaException e) {
            throw new RuntimeException(e);
        }

    }
}
