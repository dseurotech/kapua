/*******************************************************************************
 * Copyright (c) 2019, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.model.xml.adapters;

public class FloatPropertyAdapter extends ClassBasedXmlPropertyAdapterBase<Float> {

    public FloatPropertyAdapter() {
        super(Float.class);
    }

    @Override
    public boolean canUnmarshallEmptyString() {
        return false;
    }

    @Override
    public Float unmarshallValue(String value) {
        return Float.parseFloat(value);
    }
}
