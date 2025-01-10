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
package org.eclipse.kapua.model;

import java.util.Date;
import java.util.Properties;

import org.eclipse.kapua.entity.EntityPropertiesReadException;
import org.eclipse.kapua.entity.EntityPropertiesWriteException;
import org.eclipse.kapua.model.id.KapuaId;

public class KapuaUpdatableEntityDto extends KapuaEntityDto implements KapuaUpdatableEntity {

    private Date modifiedOn;
    private KapuaId modifiedBy;
    private int optLock;
    private Properties entityAttributes;
    private Properties entityProperties;

    @Override
    public Date getModifiedOn() {
        return this.modifiedOn;
    }

    @Override
    public KapuaId getModifiedBy() {
        return this.modifiedBy;
    }

    @Override
    public int getOptlock() {
        return this.optLock;
    }

    @Override
    public void setOptlock(int optlock) {
        this.optLock = optlock;
    }

    @Override
    public Properties getEntityAttributes() throws EntityPropertiesReadException {
        return this.entityAttributes;
    }

    @Override
    public void setEntityAttributes(Properties props) throws EntityPropertiesWriteException {
        this.entityAttributes = props;
    }

    @Override
    public Properties getEntityProperties() throws EntityPropertiesReadException {
        return this.entityProperties;
    }

    @Override
    public void setEntityProperties(Properties props) throws EntityPropertiesWriteException {
        this.entityProperties = props;
    }
}
