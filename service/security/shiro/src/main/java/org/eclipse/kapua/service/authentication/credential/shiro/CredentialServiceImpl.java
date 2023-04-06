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

import org.apache.shiro.codec.Base64;
import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaIllegalArgumentException;
import org.eclipse.kapua.commons.configuration.KapuaConfigurableServiceLinker;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.commons.util.CommonsValidationRegex;
import org.eclipse.kapua.event.ServiceEvent;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.KapuaEntityAttributes;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.model.query.predicate.AndPredicate;
import org.eclipse.kapua.model.query.predicate.AttributePredicate;
import org.eclipse.kapua.model.query.predicate.QueryPredicate;
import org.eclipse.kapua.service.authentication.AuthenticationDomains;
import org.eclipse.kapua.service.authentication.credential.Credential;
import org.eclipse.kapua.service.authentication.credential.CredentialAttributes;
import org.eclipse.kapua.service.authentication.credential.CredentialCreator;
import org.eclipse.kapua.service.authentication.credential.CredentialFactory;
import org.eclipse.kapua.service.authentication.credential.CredentialListResult;
import org.eclipse.kapua.service.authentication.credential.CredentialQuery;
import org.eclipse.kapua.service.authentication.credential.CredentialRepository;
import org.eclipse.kapua.service.authentication.credential.CredentialService;
import org.eclipse.kapua.service.authentication.credential.CredentialType;
import org.eclipse.kapua.service.authentication.exception.DuplicatedPasswordCredentialException;
import org.eclipse.kapua.service.authentication.exception.PasswordLengthException;
import org.eclipse.kapua.service.authentication.shiro.CredentialServiceConfigurationManager;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaAuthenticationSetting;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaAuthenticationSettingKeys;
import org.eclipse.kapua.service.authentication.shiro.utils.AuthenticationUtils;
import org.eclipse.kapua.service.authentication.shiro.utils.CryptAlgorithm;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.storage.TxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link CredentialService} implementation.
 *
 * @since 1.0
 */
@Singleton
public class CredentialServiceImpl extends KapuaConfigurableServiceLinker implements CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialServiceImpl.class);

    public static final String PASSWORD_MIN_LENGTH = "password.minLength";

    public static final int SYSTEM_MAXIMUM_PASSWORD_LENGTH = 255;

    private final AuthorizationService authorizationService;
    private final PermissionFactory permissionFactory;
    private final TxManager txManager;
    private final CredentialRepository credentialRepository;
    private final CredentialFactory credentialFactory;

    public CredentialServiceImpl(
            CredentialServiceConfigurationManager serviceConfigurationManager,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            TxManager txManager,
            CredentialRepository credentialRepository,
            CredentialFactory credentialFactory) {
        super(serviceConfigurationManager);
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
        this.txManager = txManager;
        this.credentialRepository = credentialRepository;
        this.credentialFactory = credentialFactory;
    }

    @Override
    public Credential create(final CredentialCreator credentialCreator)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(credentialCreator, "credentialCreator");
        ArgumentValidator.notNull(credentialCreator.getScopeId(), "credentialCreator.scopeId");
        ArgumentValidator.notNull(credentialCreator.getUserId(), "credentialCreator.userId");
        ArgumentValidator.notNull(credentialCreator.getCredentialType(), "credentialCreator.credentialType");
        ArgumentValidator.notNull(credentialCreator.getCredentialStatus(), "credentialCreator.credentialStatus");
        if (credentialCreator.getCredentialType() != CredentialType.API_KEY) {
            ArgumentValidator.notEmptyOrNull(credentialCreator.getCredentialPlainKey(), "credentialCreator.credentialKey");
        }
        final AtomicReference<String> fullKey = new AtomicReference<>(null);
        final Credential res = txManager.executeWithResult(tx -> {
            if (credentialCreator.getCredentialType() == CredentialType.PASSWORD) {
                //
                //
                // Check if a PASSWORD credential already exists for the user
                CredentialListResult existingCredentials = credentialRepository.findByUserId(tx, credentialCreator.getScopeId(), credentialCreator.getUserId());
                for (Credential credential : existingCredentials.getItems()) {
                    if (credential.getCredentialType().equals(CredentialType.PASSWORD)) {
                        throw new DuplicatedPasswordCredentialException();
                    }
                }

                try {
                    validatePassword(credentialCreator.getScopeId(), credentialCreator.getCredentialPlainKey());
                } catch (KapuaIllegalArgumentException ignored) {
                    throw new KapuaIllegalArgumentException("credentialCreator.credentialKey", credentialCreator.getCredentialPlainKey());
                }
            }

            //
            // Check access
            authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.write, credentialCreator.getScopeId()));

            //
            // Do create
            // Do pre persist magic on key values
            switch (credentialCreator.getCredentialType()) {
                case API_KEY: // Generate new api key
                    SecureRandom random = null;
                    try {
                        random = SecureRandom.getInstance("SHA1PRNG");
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }

                    KapuaAuthenticationSetting setting = KapuaAuthenticationSetting.getInstance();
                    int preLength = setting.getInt(KapuaAuthenticationSettingKeys.AUTHENTICATION_CREDENTIAL_APIKEY_PRE_LENGTH);
                    int keyLength = setting.getInt(KapuaAuthenticationSettingKeys.AUTHENTICATION_CREDENTIAL_APIKEY_KEY_LENGTH);

                    byte[] bPre = new byte[preLength];
                    random.nextBytes(bPre);
                    String pre = Base64.encodeToString(bPre).substring(0, preLength);

                    byte[] bKey = new byte[keyLength];
                    random.nextBytes(bKey);
                    String key = Base64.encodeToString(bKey);

                    fullKey.set(pre + key);

                    final CredentialCreatorImpl creator = new CredentialCreatorImpl(credentialCreator.getScopeId(),
                            credentialCreator.getUserId(),
                            credentialCreator.getCredentialType(),
                            fullKey.get(),
                            credentialCreator.getCredentialStatus(),
                            credentialCreator.getExpirationDate());

                    break;
                case PASSWORD:
                default:
                    // Don't do anything special
                    break;
            }

            //
            // Crypto credential
            String cryptedCredential;
            switch (credentialCreator.getCredentialType()) {
                case API_KEY:
                    cryptedCredential = cryptApiKey(credentialCreator.getCredentialPlainKey());
                    break;
                case PASSWORD:
                default:
                    cryptedCredential = cryptPassword(credentialCreator.getCredentialPlainKey());
                    break;
            }

            //
            // Create Credential
            Credential credentialImpl = new CredentialImpl(credentialCreator.getScopeId(),
                    credentialCreator.getUserId(),
                    credentialCreator.getCredentialType(),
                    cryptedCredential,
                    credentialCreator.getCredentialStatus(),
                    credentialCreator.getExpirationDate());

            //
            // Do create
            return credentialRepository.create(tx, credentialImpl);
        });
        // Do post persist magic on key values
        res.setCredentialKey(fullKey.get());
        return res;
    }

    @Override
    public Credential update(Credential credential)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(credential, "credential");
        ArgumentValidator.notNull(credential.getId(), "credential.id");
        ArgumentValidator.notNull(credential.getScopeId(), "credential.scopeId");
        ArgumentValidator.notNull(credential.getUserId(), "credential.userId");
        ArgumentValidator.notNull(credential.getCredentialType(), "credential.credentialType");

        //
        // Check access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.write, credential.getScopeId()));

        final Credential updatedCredential = txManager.executeWithResult(tx -> {
            Credential currentCredential = credentialRepository.find(tx, credential.getScopeId(), credential.getId());

            if (currentCredential == null) {
                throw new KapuaEntityNotFoundException(Credential.TYPE, credential.getId());
            }

            if (currentCredential.getCredentialType() != credential.getCredentialType()) {
                throw new KapuaIllegalArgumentException("credentialType", credential.getCredentialType().toString());
            }

            // Passing attributes??
            return credentialRepository.update(tx, credential);
        });
        updatedCredential.setCredentialKey(null);
        return updatedCredential;
    }

    @Override
    public Credential find(KapuaId scopeId, KapuaId credentialId)
            throws KapuaException {
        // Validation of the fields
        ArgumentValidator.notNull(scopeId, KapuaEntityAttributes.SCOPE_ID);
        ArgumentValidator.notNull(credentialId, "credentialId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.read, scopeId));

        Credential credential = txManager.executeWithResult(tx -> credentialRepository.find(tx, scopeId, credentialId));
        credential.setCredentialKey(null);
        return credential;
    }

    @Override
    public CredentialListResult query(KapuaQuery query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.read, query.getScopeId()));

        final CredentialListResult credentials = txManager.executeWithResult(tx -> credentialRepository.query(tx, query));
        credentials.getItems().forEach(credential -> credential.setCredentialKey(null));
        return credentials;
    }

    @Override
    public long count(KapuaQuery query)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        KapuaLocator locator = KapuaLocator.getInstance();
        AuthorizationService authorizationService = locator.getService(AuthorizationService.class);
        PermissionFactory permissionFactory = locator.getFactory(PermissionFactory.class);
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.read, query.getScopeId()));
        return txManager.executeWithResult(tx -> credentialRepository.count(tx, query));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId credentialId)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(credentialId, "credential.id");
        ArgumentValidator.notNull(scopeId, "credential.scopeId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.delete, scopeId));
        txManager.executeNoResult(tx -> credentialRepository.delete(tx, scopeId, credentialId));
    }

    @Override
    public CredentialListResult findByUserId(KapuaId scopeId, KapuaId userId)
            throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, KapuaEntityAttributes.SCOPE_ID);
        ArgumentValidator.notNull(userId, "userId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.read, scopeId));

        final CredentialListResult credentials = txManager.executeWithResult(tx -> credentialRepository.findByUserId(tx, scopeId, userId));
        credentials.getItems().forEach(credential -> credential.setCredentialKey(null));
        return credentials;
    }

    @Override
    public Credential findByApiKey(String apiKey) throws KapuaException {
        KapuaAuthenticationSetting setting = KapuaAuthenticationSetting.getInstance();
        int preLength = setting.getInt(KapuaAuthenticationSettingKeys.AUTHENTICATION_CREDENTIAL_APIKEY_PRE_LENGTH);

        //
        // Argument Validation
        ArgumentValidator.notEmptyOrNull(apiKey, "apiKey");
        ArgumentValidator.lengthRange(apiKey, preLength, null, "apiKey");

        //
        // Do the find
        Credential credential = txManager.executeWithResult(tx -> {
            //
            // Build search query
            String preSeparator = setting.getString(KapuaAuthenticationSettingKeys.AUTHENTICATION_CREDENTIAL_APIKEY_PRE_SEPARATOR);
            String apiKeyPreValue = apiKey.substring(0, preLength).concat(preSeparator);

            //
            // Build query
            KapuaQuery query = new CredentialQueryImpl();
            AttributePredicate<CredentialType> typePredicate = query.attributePredicate(CredentialAttributes.CREDENTIAL_TYPE, CredentialType.API_KEY);
            AttributePredicate<String> keyPredicate = query.attributePredicate(CredentialAttributes.CREDENTIAL_KEY, apiKeyPreValue, AttributePredicate.Operator.STARTS_WITH);

            AndPredicate andPredicate = query.andPredicate(
                    typePredicate,
                    keyPredicate
            );

            query.setPredicate(andPredicate);

            //
            // Query
            CredentialListResult credentialListResult = credentialRepository.query(tx, query);

            //
            // Parse the result
            return credentialListResult.getFirstItem();
        });

        ///FIXME: why the permission check here? it does not rollback!
        // Check Access
        if (credential != null) {
            authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.read, credential.getId()));
            credential.setCredentialKey(null);
        }

        return credential;
    }

    @Override
    public void unlock(KapuaId scopeId, KapuaId credentialId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, KapuaEntityAttributes.SCOPE_ID);
        ArgumentValidator.notNull(credentialId, "credentialId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.write, scopeId));

        txManager.executeNoResult(tx -> {
            Credential credential = credentialRepository.find(tx, scopeId, credentialId);
            credential.setLoginFailures(0);
            credential.setFirstLoginFailure(null);
            credential.setLoginFailuresReset(null);
            credential.setLockoutReset(null);
            credentialRepository.update(tx, credential);
        });
    }

    @Override
    public int getMinimumPasswordLength(KapuaId scopeId) throws KapuaException {
        Object minPasswordLengthConfigValue = getConfigValues(scopeId).get(PASSWORD_MIN_LENGTH);
        int minPasswordLength = ((CredentialServiceConfigurationManager) serviceConfigurationManager).getSystemMinimumPasswordLength();
        if (minPasswordLengthConfigValue != null) {
            minPasswordLength = Integer.parseInt(minPasswordLengthConfigValue.toString());
        }
        return minPasswordLength;
    }

    private long countExistingCredentials(CredentialType credentialType, KapuaId scopeId, KapuaId userId) throws KapuaException {
        KapuaQuery query = credentialFactory.newQuery(scopeId);

        QueryPredicate credentialTypePredicate = query.attributePredicate(CredentialAttributes.CREDENTIAL_TYPE, credentialType);
        QueryPredicate userIdPredicate = query.attributePredicate(CredentialAttributes.USER_ID, userId);

        QueryPredicate andPredicate = query.andPredicate(
                credentialTypePredicate,
                userIdPredicate
        );

        query.setPredicate(andPredicate);

        return count(query);
    }

    //@ListenServiceEvent(fromAddress="account")
    //@ListenServiceEvent(fromAddress="user")
    public void onKapuaEvent(ServiceEvent kapuaEvent) throws KapuaException {
        if (kapuaEvent == null) {
            //service bus error. Throw some exception?
        }
        LOGGER.info("CredentialService: received kapua event from {}, operation {}", kapuaEvent.getService(), kapuaEvent.getOperation());
        if ("user".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteCredentialByUserId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        } else if ("account".equals(kapuaEvent.getService()) && "delete".equals(kapuaEvent.getOperation())) {
            deleteCredentialByAccountId(kapuaEvent.getScopeId(), kapuaEvent.getEntityId());
        }
    }

    private void deleteCredentialByUserId(KapuaId scopeId, KapuaId userId) throws KapuaException {
        KapuaLocator locator = KapuaLocator.getInstance();
        CredentialFactory credentialFactory = locator.getFactory(CredentialFactory.class);

        CredentialQuery query = credentialFactory.newQuery(scopeId);
        query.setPredicate(query.attributePredicate(CredentialAttributes.USER_ID, userId));

        CredentialListResult credentialsToDelete = query(query);

        for (Credential c : credentialsToDelete.getItems()) {
            delete(c.getScopeId(), c.getId());
        }
    }

    private void deleteCredentialByAccountId(KapuaId scopeId, KapuaId accountId) throws KapuaException {
        KapuaLocator locator = KapuaLocator.getInstance();
        CredentialFactory credentialFactory = locator.getFactory(CredentialFactory.class);

        CredentialQuery query = credentialFactory.newQuery(accountId);

        CredentialListResult credentialsToDelete = query(query);

        for (Credential c : credentialsToDelete.getItems()) {
            delete(c.getScopeId(), c.getId());
        }
    }

    @Override
    public void validatePassword(KapuaId scopeId, String plainPassword) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notEmptyOrNull(plainPassword, "plainPassword");

        // Validate Password length
        int minPasswordLength = getMinimumPasswordLength(scopeId);
        if (plainPassword.length() < minPasswordLength || plainPassword.length() > SYSTEM_MAXIMUM_PASSWORD_LENGTH) {
            throw new PasswordLengthException(minPasswordLength, SYSTEM_MAXIMUM_PASSWORD_LENGTH);
        }

        //
        // Validate Password regex
        ArgumentValidator.match(plainPassword, CommonsValidationRegex.PASSWORD_REGEXP, "plainPassword");
    }

    @Override
    public Credential findWithKey(KapuaId scopeId, KapuaId credentialId) throws KapuaException {
        // Validation of the fields
        ArgumentValidator.notNull(scopeId, KapuaEntityAttributes.SCOPE_ID);
        ArgumentValidator.notNull(credentialId, "credentialId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(AuthenticationDomains.CREDENTIAL_DOMAIN, Actions.read, null));

        return txManager.executeWithResult(tx -> credentialRepository.find(tx, scopeId, credentialId));
    }

    private String cryptPassword(String credentialPlainKey) throws KapuaException {
        return AuthenticationUtils.cryptCredential(CryptAlgorithm.BCRYPT, credentialPlainKey);
    }

    private String cryptApiKey(String credentialPlainKey) throws KapuaException {
        KapuaAuthenticationSetting setting = KapuaAuthenticationSetting.getInstance();
        int preLength = setting.getInt(KapuaAuthenticationSettingKeys.AUTHENTICATION_CREDENTIAL_APIKEY_PRE_LENGTH);
        String preSeparator = setting.getString(KapuaAuthenticationSettingKeys.AUTHENTICATION_CREDENTIAL_APIKEY_PRE_SEPARATOR);

        String hashedValue = credentialPlainKey.substring(0, preLength); // Add the pre in clear text
        hashedValue += preSeparator; // Add separator
        hashedValue += AuthenticationUtils.cryptCredential(CryptAlgorithm.BCRYPT, credentialPlainKey.substring(preLength, credentialPlainKey.length())); // Bcrypt the rest

        return hashedValue;
    }
}
