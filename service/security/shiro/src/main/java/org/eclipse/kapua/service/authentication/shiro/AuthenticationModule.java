/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.authentication.shiro;

import com.google.inject.Provides;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.configuration.AbstractKapuaConfigurableServiceCache;
import org.eclipse.kapua.commons.configuration.CachingServiceConfigRepository;
import org.eclipse.kapua.commons.configuration.RootUserTester;
import org.eclipse.kapua.commons.configuration.ServiceConfigImplJpaRepository;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManagerCachingWrapper;
import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.commons.jpa.KapuaJpaTxManagerFactory;
import org.eclipse.kapua.model.config.metatype.KapuaTocd;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.account.AccountRepository;
import org.eclipse.kapua.service.authentication.AuthenticationService;
import org.eclipse.kapua.service.authentication.CredentialsFactory;
import org.eclipse.kapua.service.authentication.credential.CredentialFactory;
import org.eclipse.kapua.service.authentication.credential.CredentialRepository;
import org.eclipse.kapua.service.authentication.credential.CredentialService;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionFactory;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionRepository;
import org.eclipse.kapua.service.authentication.credential.mfa.MfaOptionService;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeFactory;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeRepository;
import org.eclipse.kapua.service.authentication.credential.mfa.ScratchCodeService;
import org.eclipse.kapua.service.authentication.credential.mfa.shiro.MfaOptionFactoryImpl;
import org.eclipse.kapua.service.authentication.credential.mfa.shiro.MfaOptionImplJpaRepository;
import org.eclipse.kapua.service.authentication.credential.mfa.shiro.MfaOptionServiceImpl;
import org.eclipse.kapua.service.authentication.credential.mfa.shiro.ScratchCodeFactoryImpl;
import org.eclipse.kapua.service.authentication.credential.mfa.shiro.ScratchCodeImplJpaRepository;
import org.eclipse.kapua.service.authentication.credential.mfa.shiro.ScratchCodeServiceImpl;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialFactoryImpl;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialImplJpaRepository;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialMapper;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialMapperImpl;
import org.eclipse.kapua.service.authentication.credential.shiro.CredentialServiceImpl;
import org.eclipse.kapua.service.authentication.credential.shiro.PasswordValidator;
import org.eclipse.kapua.service.authentication.credential.shiro.PasswordValidatorImpl;
import org.eclipse.kapua.service.authentication.mfa.MfaAuthenticator;
import org.eclipse.kapua.service.authentication.registration.RegistrationService;
import org.eclipse.kapua.service.authentication.shiro.mfa.MfaAuthenticatorImpl;
import org.eclipse.kapua.service.authentication.shiro.registration.RegistrationServiceImpl;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaAuthenticationSetting;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaAuthenticationSettingKeys;
import org.eclipse.kapua.service.authentication.token.AccessTokenFactory;
import org.eclipse.kapua.service.authentication.token.AccessTokenRepository;
import org.eclipse.kapua.service.authentication.token.AccessTokenService;
import org.eclipse.kapua.service.authentication.token.shiro.AccessTokenFactoryImpl;
import org.eclipse.kapua.service.authentication.token.shiro.AccessTokenImplJpaRepository;
import org.eclipse.kapua.service.authentication.token.shiro.AccessTokenServiceImpl;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.user.UserRepository;
import org.eclipse.kapua.storage.TxContext;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

public class AuthenticationModule extends AbstractKapuaModule {
    @Override
    protected void configureModule() {
        bind(AuthenticationService.class).to(AuthenticationServiceShiroImpl.class);
        bind(CredentialFactory.class).to(CredentialFactoryImpl.class);
        bind(CredentialsFactory.class).to(CredentialsFactoryImpl.class);
        bind(MfaOptionFactory.class).to(MfaOptionFactoryImpl.class);
        bind(ScratchCodeFactory.class).to(ScratchCodeFactoryImpl.class);
        bind(AccessTokenFactory.class).to(AccessTokenFactoryImpl.class);
        bind(RegistrationService.class).to(RegistrationServiceImpl.class);
        bind(MfaAuthenticator.class).toInstance(new MfaAuthenticatorImpl());
    }

    @Provides
    @Singleton
    PasswordValidator passwordValidator(CredentialServiceConfigurationManager credentialServiceConfigurationManager) {
        return new PasswordValidatorImpl(credentialServiceConfigurationManager);
    }

    @Provides
    @Singleton
    CredentialMapper credentialMapper(CredentialFactory credentialFactory) {
        return new CredentialMapperImpl(credentialFactory);
    }

    @Provides
    @Singleton
    AccessTokenService accessTokenService(
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            AccessTokenRepository accessTokenRepository,
            AccessTokenFactory accessTokenFactory,
            KapuaJpaTxManagerFactory jpaTxManagerFactory) {
        return new AccessTokenServiceImpl(
                authorizationService,
                permissionFactory,
                jpaTxManagerFactory.create("kapua-authentication"),
                accessTokenRepository,
                accessTokenFactory);
    }

    @Provides
    @Singleton
    MfaOptionService mfaOptionService(
            MfaAuthenticator mfaAuthenticator,
            MfaOptionRepository mfaOptionRepository,
            AccountRepository accountRepository,
            ScratchCodeRepository scratchCodeRepository,
            ScratchCodeFactory scratchCodeFactory,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            UserRepository userRepository,
            KapuaJpaTxManagerFactory jpaTxManagerFactory) {

        final KapuaAuthenticationSetting authenticationSetting = KapuaAuthenticationSetting.getInstance();
        int trustKeyDuration = authenticationSetting.getInt(KapuaAuthenticationSettingKeys.AUTHENTICATION_MFA_TRUST_KEY_DURATION);

        return new MfaOptionServiceImpl(
                trustKeyDuration,
                mfaAuthenticator,
                jpaTxManagerFactory.create("kapua-authentication"),
                mfaOptionRepository,
                accountRepository,
                scratchCodeRepository,
                scratchCodeFactory,
                authorizationService,
                permissionFactory,
                userRepository
        );
    }

    @Provides
    @Singleton
    ScratchCodeService scratchCodeService(
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            ScratchCodeRepository scratchCodeRepository,
            ScratchCodeFactory scratchCodeFactory,
            KapuaJpaTxManagerFactory jpaTxManagerFactory) {
        return new ScratchCodeServiceImpl(
                authorizationService,
                permissionFactory,
                jpaTxManagerFactory.create("kapua-authentication"),
                scratchCodeRepository,
                scratchCodeFactory);
    }

    @Provides
    @Singleton
    public AccessTokenRepository accessTokenRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        return new AccessTokenImplJpaRepository(jpaRepoConfig);
    }

    @Provides
    @Singleton
    public MfaOptionRepository mfaOptionRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        return new MfaOptionImplJpaRepository(jpaRepoConfig);
    }

    @Provides
    @Singleton
    public ScratchCodeRepository scratchCodeRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        return new ScratchCodeImplJpaRepository(jpaRepoConfig);
    }

    @Provides
    @Singleton
    public CredentialService credentialService(
            CredentialServiceConfigurationManager serviceConfigurationManager,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            CredentialRepository credentialRepository,
            CredentialFactory credentialFactory,
            KapuaJpaTxManagerFactory jpaTxManagerFactory,
            CredentialMapper credentialMapper,
            PasswordValidator passwordValidator) {
        return new CredentialServiceImpl(serviceConfigurationManager,
                authorizationService,
                permissionFactory,
                jpaTxManagerFactory.create("kapua-authentication"),
                credentialRepository,
                credentialFactory,
                credentialMapper,
                passwordValidator);
    }

    @Provides
    @Singleton
    CredentialRepository credentialRepository(KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        return new CredentialImplJpaRepository(jpaRepoConfig);
    }

    @Provides
    @Singleton
    public CredentialServiceConfigurationManager credentialServiceConfigurationManager(
            RootUserTester rootUserTester,
            KapuaJpaRepositoryConfiguration jpaRepoConfig) {
        final CredentialServiceConfigurationManagerImpl credentialServiceConfigurationManager = new CredentialServiceConfigurationManagerImpl(
                new CachingServiceConfigRepository(
                        new ServiceConfigImplJpaRepository(jpaRepoConfig),
                        new AbstractKapuaConfigurableServiceCache().createCache()
                ),
                rootUserTester);

        final ServiceConfigurationManagerCachingWrapper cached = new ServiceConfigurationManagerCachingWrapper(credentialServiceConfigurationManager);
        return new CredentialServiceConfigurationManager() {
            public int getSystemMinimumPasswordLength() {
                return credentialServiceConfigurationManager.getSystemMinimumPasswordLength();
            }

            @Override
            public void checkAllowedEntities(TxContext txContext, KapuaId scopeId, String entityType) throws KapuaException {
                cached.checkAllowedEntities(txContext, scopeId, entityType);
            }

            @Override
            public void setConfigValues(TxContext txContext, KapuaId scopeId, Optional<KapuaId> parentId, Map<String, Object> values) throws KapuaException {
                cached.setConfigValues(txContext, scopeId, parentId, values);
            }

            @Override
            public Map<String, Object> getConfigValues(TxContext txContext, KapuaId scopeId, boolean excludeDisabled) throws KapuaException {
                return cached.getConfigValues(txContext, scopeId, excludeDisabled);
            }

            @Override
            public KapuaTocd getConfigMetadata(TxContext txContext, KapuaId scopeId, boolean excludeDisabled) throws KapuaException {
                return cached.getConfigMetadata(txContext, scopeId, excludeDisabled);
            }
        };
    }
}
