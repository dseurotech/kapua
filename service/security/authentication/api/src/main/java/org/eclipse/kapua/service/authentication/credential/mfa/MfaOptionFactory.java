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
package org.eclipse.kapua.service.authentication.credential.mfa;

import org.eclipse.kapua.model.KapuaEntityFactory;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authentication.token.AccessToken;

/**
 * {@link MfaOption} {@link KapuaEntityFactory} definition.
 *
 * @see KapuaEntityFactory
 * @since 1.3.0
 */
public interface MfaOptionFactory extends KapuaEntityFactory<MfaOption, MfaOptionCreator, MfaOptionQuery> {

    /**
     * Instantiates a new {@link MfaOptionCreator}.
     *
     * @param scopeId
     *         The scope {@link KapuaId} to set into the {@link MfaOptionCreator}.
     * @param userId
     *         The {@link org.eclipse.kapua.service.user.User} {@link KapuaId} to set into the{@link AccessToken}.
     * @return The newly instantiated {@link MfaOptionCreator}
     * @since 1.3.0
     */
    MfaOptionCreator newCreator(KapuaId scopeId, KapuaId userId);

}
