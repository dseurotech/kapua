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

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.jpa.JpaAwareTxContext;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.commons.jpa.KapuaUpdatableEntityJpaRepository;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authorization.access.AccessInfo;
import org.eclipse.kapua.service.authorization.access.AccessInfoAttributes;
import org.eclipse.kapua.service.authorization.access.AccessInfoListResult;
import org.eclipse.kapua.service.authorization.access.AccessInfoRepository;
import org.eclipse.kapua.storage.TxContext;

import javax.persistence.EntityManager;
import java.util.Optional;

public class AccessInfoImplJpaRepository
        extends KapuaUpdatableEntityJpaRepository<AccessInfo, AccessInfoImpl, AccessInfoListResult>
        implements AccessInfoRepository {
    public AccessInfoImplJpaRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        super(AccessInfoImpl.class, () -> new AccessInfoListResultImpl(), jpaRepoConfig);
    }

    @Override
    public Optional<AccessInfo> findByUserId(TxContext txContext, KapuaId scopeId, KapuaId userId) throws KapuaException {
        return doFindByField(txContext, scopeId, AccessInfoAttributes.USER_ID, userId);
    }

    // TODO: check if it is correct to remove this statement (already thrown by the delete method, but
    //  without TYPE)
    @Override
    public AccessInfo delete(TxContext txContext, KapuaId scopeId, KapuaId accessInfoId) throws KapuaException {
        final EntityManager em = JpaAwareTxContext.extractEntityManager(txContext);
        return super.doFind(em, scopeId, accessInfoId)
                .map(toBeDeleted -> doDelete(em, toBeDeleted))
                .orElseThrow(() -> new KapuaEntityNotFoundException(AccessInfo.TYPE, accessInfoId));
    }
}
