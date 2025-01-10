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
package org.eclipse.kapua.service.authentication.credential.shiro;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.commons.jpa.KapuaUpdatableEntityJpaRepository;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.service.authentication.credential.Credential;
import org.eclipse.kapua.service.authentication.credential.CredentialRepository;
import org.eclipse.kapua.storage.TxContext;

/**
 * The {@link CredentialRepository} JPA implementation.
 *
 * @since 2.0.0
 */
public class CredentialImplJpaRepository
        extends KapuaUpdatableEntityJpaRepository<Credential, CredentialImpl>
        implements CredentialRepository {

    /**
     * Constructor.
     *
     * @param jpaRepoConfig
     *         The {@link KapuaJpaRepositoryConfiguration}.
     * @since 2.0.0
     */
    public CredentialImplJpaRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        super(CredentialImpl.class, Credential.TYPE, CredentialListResultImpl::new, jpaRepoConfig);
    }

    @Override
    public KapuaListResult<Credential> findByUserId(TxContext txContext, KapuaId scopeId, KapuaId userId) throws KapuaException {
        final KapuaListResult<Credential> res = listSupplier.get();
        res.addItems(this.doFindAllByField(txContext, scopeId, CredentialImpl_.USER_ID, userId));
        return res;
    }
}
