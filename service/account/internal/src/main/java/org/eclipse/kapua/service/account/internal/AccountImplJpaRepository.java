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
package org.eclipse.kapua.service.account.internal;

import org.eclipse.kapua.commons.jpa.JpaTxContext;
import org.eclipse.kapua.commons.jpa.KapuaNamedEntityJpaRepository;
import org.eclipse.kapua.service.account.Account;
import org.eclipse.kapua.service.account.AccountListResult;
import org.eclipse.kapua.service.account.AccountRepository;
import org.eclipse.kapua.storage.TxContext;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

public class AccountImplJpaRepository
        extends KapuaNamedEntityJpaRepository<Account, AccountImpl, AccountListResult>
        implements AccountRepository {

    public AccountImplJpaRepository() {
        super(AccountImpl.class, () -> new AccountListResultImpl());
    }

    @Override
    public AccountListResult findChildAccountsRecursive(TxContext tx, String parentAccountPath) {
        final EntityManager em = JpaTxContext.extractEntityManager(tx);
        TypedQuery<Account> q = em.createNamedQuery("Account.findChildAccountsRecursive", Account.class);
        q.setParameter("parentAccountPath", "\\" + parentAccountPath + "/%");
        final AccountListResult result = new AccountListResultImpl();
        result.addItems(q.getResultList());
        return result;
    }
}
