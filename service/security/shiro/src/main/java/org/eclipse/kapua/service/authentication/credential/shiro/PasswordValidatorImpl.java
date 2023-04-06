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
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.commons.util.CommonsValidationRegex;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.authentication.exception.PasswordLengthException;
import org.eclipse.kapua.service.authentication.shiro.CredentialServiceConfigurationManager;
import org.eclipse.kapua.service.authentication.shiro.CredentialServiceConfigurationManagerImpl;

import java.util.Map;

public class PasswordValidatorImpl implements PasswordValidator {

    private final CredentialServiceConfigurationManager credentialServiceConfigurationManager;

    public PasswordValidatorImpl(CredentialServiceConfigurationManager credentialServiceConfigurationManager) {
        this.credentialServiceConfigurationManager = credentialServiceConfigurationManager;
    }

    @Override
    public void validatePassword(KapuaId scopeId, String plainPassword) throws KapuaException {
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notEmptyOrNull(plainPassword, "plainPassword");

        // Validate Password length
        int minPasswordLength = getMinimumPasswordLength(scopeId);
        if (plainPassword.length() < minPasswordLength || plainPassword.length() > CredentialServiceConfigurationManagerImpl.SYSTEM_MAXIMUM_PASSWORD_LENGTH) {
            throw new PasswordLengthException(minPasswordLength, CredentialServiceConfigurationManagerImpl.SYSTEM_MAXIMUM_PASSWORD_LENGTH);
        }
        // Validate Password regex
        ArgumentValidator.match(plainPassword, CommonsValidationRegex.PASSWORD_REGEXP, "plainPassword");
    }

    @Override
    public int getMinimumPasswordLength(KapuaId scopeId) throws KapuaException {
        Object minPasswordLengthConfigValue = getConfigValues(scopeId).get(CredentialServiceConfigurationManagerImpl.PASSWORD_MIN_LENGTH);
        if (minPasswordLengthConfigValue != null) {
            return Integer.parseInt(minPasswordLengthConfigValue.toString());
        }
        return credentialServiceConfigurationManager.getSystemMinimumPasswordLength();
    }

    private Map<String, Object> getConfigValues(KapuaId scopeId) throws KapuaException {
        return credentialServiceConfigurationManager.getConfigValues(scopeId, true);
    }
}
