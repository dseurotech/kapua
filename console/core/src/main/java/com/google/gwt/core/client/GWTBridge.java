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
package com.google.gwt.core.client;

public abstract class GWTBridge extends com.google.gwt.core.shared.GWTBridge {
    public GWTBridge() {
    }

    public abstract <T> T create(Class<?> var1);

    public String getThreadUniqueID() {
        return "";
    }

    public abstract String getVersion();

    public abstract boolean isClient();

    public abstract void log(String var1, Throwable var2);
}
