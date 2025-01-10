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

import org.eclipse.kapua.model.id.KapuaId;

public class KapuaEntityDto implements KapuaEntity {

    private String type;
    private KapuaId id;
    private KapuaId scopeId;
    private Date createdOn;
    private KapuaId createdBy;

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public KapuaId getId() {
        return this.id;
    }

    @Override
    public void setId(KapuaId id) {
        this.id = id;
    }

    @Override
    public KapuaId getScopeId() {
        return this.scopeId;
    }

    @Override
    public void setScopeId(KapuaId scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public Date getCreatedOn() {
        return this.createdOn;
    }

    @Override
    public KapuaId getCreatedBy() {
        return this.createdBy;
    }
}
