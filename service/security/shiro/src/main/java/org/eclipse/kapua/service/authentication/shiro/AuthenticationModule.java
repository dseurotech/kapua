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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.KapuaRuntimeException;
import org.eclipse.kapua.commons.configuration.CachingServiceConfigRepository;
import org.eclipse.kapua.commons.configuration.RootUserTester;
import org.eclipse.kapua.commons.configuration.ServiceConfigImplJpaRepository;
import org.eclipse.kapua.commons.configuration.ServiceConfigurationManagerCachingWrapper;
import org.eclipse.kapua.commons.core.AbstractKapuaModule;
import org.eclipse.kapua.commons.core.ServiceModule;
import org.eclipse.kapua.commons.event.ServiceEventHouseKeeperFactoryImpl;
import org.eclipse.kapua.commons.jpa.EntityCacheFactory;
import org.eclipse.kapua.commons.jpa.KapuaJpaRepositoryConfiguration;
import org.eclipse.kapua.commons.jpa.KapuaJpaTxManagerFactory;
import org.eclipse.kapua.commons.model.domains.Domains;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreFactory;
import org.eclipse.kapua.commons.service.event.store.api.EventStoreRecordRepository;
import org.eclipse.kapua.commons.service.event.store.internal.EventStoreServiceImpl;
import org.eclipse.kapua.commons.util.qr.QRCodeBuilder;
import org.eclipse.kapua.commons.util.xml.XmlUtil;
import org.eclipse.kapua.event.ServiceEventBus;
import org.eclipse.kapua.event.ServiceEventBusException;
import org.eclipse.kapua.model.config.metatype.KapuaTocd;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.domain.Domain;
import org.eclipse.kapua.model.domain.DomainEntry;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.account.AccountService;
import org.eclipse.kapua.service.authentication.AuthenticationService;
import org.eclipse.kapua.service.authentication.CredentialsFactory;
import org.eclipse.kapua.service.authentication.credential.CredentialFactory;
import org.eclipse.kapua.service.authentication.credential.CredentialRepository;
import org.eclipse.kapua.service.authentication.credential.CredentialService;
import org.eclipse.kapua.service.authentication.credential.cache.CacheMetric;
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
import org.eclipse.kapua.service.authentication.credential.shiro.PasswordResetter;
import org.eclipse.kapua.service.authentication.credential.shiro.PasswordValidator;
import org.eclipse.kapua.service.authentication.credential.shiro.PasswordValidatorImpl;
import org.eclipse.kapua.service.authentication.exception.KapuaAuthenticationErrorCodes;
import org.eclipse.kapua.service.authentication.mfa.MfaAuthenticator;
import org.eclipse.kapua.service.authentication.registration.RegistrationService;
import org.eclipse.kapua.service.authentication.shiro.mfa.MfaAuthenticatorImpl;
import org.eclipse.kapua.service.authentication.shiro.realm.AccessTokenCredentialsHandler;
import org.eclipse.kapua.service.authentication.shiro.realm.ApiKeyCredentialsHandler;
import org.eclipse.kapua.service.authentication.shiro.realm.CredentialsHandler;
import org.eclipse.kapua.service.authentication.shiro.realm.JwtCredentialsHandler;
import org.eclipse.kapua.service.authentication.shiro.realm.UserPassCredentialsHandler;
import org.eclipse.kapua.service.authentication.shiro.registration.RegistrationServiceImpl;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaAuthenticationSetting;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaAuthenticationSettingKeys;
import org.eclipse.kapua.service.authentication.shiro.setting.KapuaCryptoSetting;
import org.eclipse.kapua.service.authentication.shiro.utils.AuthenticationUtils;
import org.eclipse.kapua.service.authentication.token.AccessTokenFactory;
import org.eclipse.kapua.service.authentication.token.AccessTokenRepository;
import org.eclipse.kapua.service.authentication.token.AccessTokenService;
import org.eclipse.kapua.service.authentication.token.shiro.AccessTokenFactoryImpl;
import org.eclipse.kapua.service.authentication.token.shiro.AccessTokenImplJpaRepository;
import org.eclipse.kapua.service.authentication.token.shiro.AccessTokenServiceImpl;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.user.UserService;
import org.eclipse.kapua.storage.TxContext;

import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;

public class AuthenticationModule extends AbstractKapuaModule {

    @Override
    protected void configureModule() {
        bind(KapuaAuthenticationSetting.class).in(Singleton.class);
        bind(AuthenticationService.class).to(AuthenticationServiceShiroImpl.class).in(Singleton.class);
        bind(CredentialFactory.class).to(CredentialFactoryImpl.class).in(Singleton.class);
        bind(CredentialsFactory.class).to(CredentialsFactoryImpl.class).in(Singleton.class);
        bind(MfaOptionFactory.class).to(MfaOptionFactoryImpl.class).in(Singleton.class);
        bind(ScratchCodeFactory.class).to(ScratchCodeFactoryImpl.class).in(Singleton.class);
        bind(AccessTokenFactory.class).to(AccessTokenFactoryImpl.class).in(Singleton.class);
        bind(RegistrationService.class).to(RegistrationServiceImpl.class).in(Singleton.class);
        bind(MfaAuthenticator.class).to(MfaAuthenticatorImpl.class).in(Singleton.class);
        bind(KapuaCryptoSetting.class).in(Singleton.class);
        bind(CacheMetric.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    AuthenticationUtils authenticationUtils(KapuaCryptoSetting kapuaCryptoSetting) {
        final SecureRandom random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new KapuaRuntimeException(KapuaAuthenticationErrorCodes.CREDENTIAL_CRYPT_ERROR, e);
        }
        return new AuthenticationUtils(random, kapuaCryptoSetting);
    }

    @ProvidesIntoSet
    public Domain accessTokenDomain() {
        return new DomainEntry(Domains.ACCESS_TOKEN, AccessTokenService.class.getName(), false, Actions.read, Actions.delete, Actions.write);
    }

    @ProvidesIntoSet
    public Domain credentialDomain() {
        return new DomainEntry(Domains.CREDENTIAL, CredentialService.class.getName(), false, Actions.read, Actions.delete, Actions.write);
    }

    @ProvidesIntoSet
    public ServiceModule authenticationServiceModule(AccessTokenService accessTokenService,
            CredentialService credentialService,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            KapuaJpaTxManagerFactory txManagerFactory,
            EventStoreFactory eventStoreFactory,
            EventStoreRecordRepository eventStoreRecordRepository,
            ServiceEventBus serviceEventBus,
            KapuaAuthenticationSetting kapuaAuthenticationSetting,
            @Named("eventsModuleName") String eventModuleName
    ) throws ServiceEventBusException {
        return new AuthenticationServiceModule(
                credentialService,
                accessTokenService,
                kapuaAuthenticationSetting,
                new ServiceEventHouseKeeperFactoryImpl(
                        new EventStoreServiceImpl(
                                authorizationService,
                                permissionFactory,
                                txManagerFactory.create("kapua-authentication"),
                                eventStoreFactory,
                                eventStoreRecordRepository
                        ),
                        txManagerFactory.create("kapua-authentication"),
                        serviceEventBus
                ),
                serviceEventBus,
                eventModuleName);
    }

    @ProvidesIntoSet
    public CredentialsHandler usernamePasswordCredentialsHandler() {
        return new UserPassCredentialsHandler();
    }

    @ProvidesIntoSet
    public CredentialsHandler apiKeyCredentialsHandler() {
        return new ApiKeyCredentialsHandler();
    }

    @ProvidesIntoSet
    public CredentialsHandler jwtCredentialsHandler() {
        return new JwtCredentialsHandler();
    }

    @ProvidesIntoSet
    public CredentialsHandler accessTokenCredentialsHandler() {
        return new AccessTokenCredentialsHandler();
    }

    @Provides
    @Singleton
    PasswordValidator passwordValidator(CredentialServiceConfigurationManager credentialServiceConfigurationManager) {
        return new PasswordValidatorImpl(credentialServiceConfigurationManager);
    }

    @Provides
    @Singleton
    CredentialMapper credentialMapper(CredentialFactory credentialFactory, KapuaAuthenticationSetting kapuaAuthenticationSetting,
            AuthenticationUtils authenticationUtils) {
        return new CredentialMapperImpl(credentialFactory, kapuaAuthenticationSetting, authenticationUtils);
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
            AccountService accountService,
            ScratchCodeRepository scratchCodeRepository,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            UserService userService,
            KapuaJpaTxManagerFactory jpaTxManagerFactory,
            KapuaAuthenticationSetting kapuaAuthenticationSetting,
            AuthenticationUtils authenticationUtils,
            QRCodeBuilder qrCodeBuilder) {
        int trustKeyDuration = kapuaAuthenticationSetting.getInt(KapuaAuthenticationSettingKeys.AUTHENTICATION_MFA_TRUST_KEY_DURATION);

        return new MfaOptionServiceImpl(
                trustKeyDuration,
                mfaAuthenticator,
                jpaTxManagerFactory.create("kapua-authentication"),
                mfaOptionRepository,
                accountService,
                scratchCodeRepository,
                authorizationService,
                permissionFactory,
                userService,
                authenticationUtils,
                qrCodeBuilder
        );
    }

    @Provides
    @Singleton
    ScratchCodeService scratchCodeService(
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            ScratchCodeRepository scratchCodeRepository,
            KapuaJpaTxManagerFactory jpaTxManagerFactory,
            AuthenticationUtils authenticationUtils) {
        return new ScratchCodeServiceImpl(
                authorizationService,
                permissionFactory,
                jpaTxManagerFactory.create("kapua-authentication"),
                scratchCodeRepository);
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
            PasswordValidator passwordValidator,
            KapuaAuthenticationSetting kapuaAuthenticationSetting,
            PasswordResetter passwordResetter) {
        return new CredentialServiceImpl(serviceConfigurationManager,
                authorizationService,
                permissionFactory,
                jpaTxManagerFactory.create("kapua-authentication"),
                credentialRepository,
                credentialFactory,
                credentialMapper,
                passwordValidator,
                kapuaAuthenticationSetting,
                passwordResetter);
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
            KapuaJpaRepositoryConfiguration jpaRepoConfig,
            KapuaAuthenticationSetting kapuaAuthenticationSetting,
            EntityCacheFactory entityCacheFactory,
            XmlUtil xmlUtil) {
        final CredentialServiceConfigurationManagerImpl credentialServiceConfigurationManager = new CredentialServiceConfigurationManagerImpl(
                new CachingServiceConfigRepository(
                        new ServiceConfigImplJpaRepository(jpaRepoConfig),
                        entityCacheFactory.createCache("AbstractKapuaConfigurableServiceCacheId")
                ),
                rootUserTester,
                kapuaAuthenticationSetting,
                xmlUtil);

        final ServiceConfigurationManagerCachingWrapper cached = new ServiceConfigurationManagerCachingWrapper(credentialServiceConfigurationManager);
        return new CredentialServiceConfigurationManager() {

            @Override
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
