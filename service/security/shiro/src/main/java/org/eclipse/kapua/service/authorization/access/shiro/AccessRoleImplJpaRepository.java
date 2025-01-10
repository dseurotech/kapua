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
package org.eclipse.kapua.service.authorization.access.shiro;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.jpa.KapuaEntityJpaRepository;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.service.authorization.access.AccessRole;
import org.eclipse.kapua.service.authorization.access.AccessRoleAttributes;
import org.eclipse.kapua.service.authorization.access.AccessRoleRepository;
import org.eclipse.kapua.storage.TxContext;

public class AccessRoleImplJpaRepository
        extends KapuaEntityJpaRepository<AccessRole, AccessRoleImpl>
        implements AccessRoleRepository {

    public AccessRoleImplJpaRepository(KapuaJpaRepositoryConfiguration configuration) {
        super(AccessRoleImpl.class, AccessRole.TYPE, () -> new AccessRoleListResultImpl(), configuration);
    }

    @Override
    public KapuaListResult<AccessRole> findByAccessInfoId(TxContext txContext, KapuaId scopeId, KapuaId accessInfoId) throws KapuaException {
        final KapuaListResult<AccessRole> res = listSupplier.get();
        res.addItems(doFindAllByField(txContext, scopeId, AccessRoleAttributes.ACCESS_INFO_ID, accessInfoId));
        return res;
    }
}
