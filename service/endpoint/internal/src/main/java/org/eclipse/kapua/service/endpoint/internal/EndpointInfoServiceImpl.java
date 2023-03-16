/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.endpoint.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaEntityUniquenessException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.commons.util.CommonsValidationRegex;
import org.eclipse.kapua.model.KapuaEntityAttributes;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.model.query.predicate.AndPredicate;
import org.eclipse.kapua.model.query.predicate.AttributePredicate.Operator;
import org.eclipse.kapua.model.query.predicate.QueryPredicate;
import org.eclipse.kapua.service.account.Account;
import org.eclipse.kapua.service.account.AccountRepository;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.endpoint.EndpointInfo;
import org.eclipse.kapua.service.endpoint.EndpointInfoAttributes;
import org.eclipse.kapua.service.endpoint.EndpointInfoCreator;
import org.eclipse.kapua.service.endpoint.EndpointInfoDomains;
import org.eclipse.kapua.service.endpoint.EndpointInfoFactory;
import org.eclipse.kapua.service.endpoint.EndpointInfoListResult;
import org.eclipse.kapua.service.endpoint.EndpointInfoQuery;
import org.eclipse.kapua.service.endpoint.EndpointInfoRepository;
import org.eclipse.kapua.service.endpoint.EndpointInfoService;
import org.eclipse.kapua.storage.TxContext;
import org.eclipse.kapua.storage.TxManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link EndpointInfoService} implementation.
 *
 * @since 1.0.0
 */
@Singleton
public class EndpointInfoServiceImpl
        implements EndpointInfoService {
    private final AuthorizationService authorizationService;
    private final PermissionFactory permissionFactory;
    private final EndpointInfoFactory endpointInfoFactory;
    private final EndpointInfoRepository repository;
    private final AccountRepository accountRepository;
    private final TxManager txManager;

    @Inject
    public EndpointInfoServiceImpl(
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            EndpointInfoFactory endpointInfoFactory,
            TxManager txManager,
            AccountRepository accountRepository,
            EndpointInfoRepository endpointInfoRepository) {
        this.accountRepository = accountRepository;
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
        this.endpointInfoFactory = endpointInfoFactory;
        this.repository = endpointInfoRepository;
        this.txManager = txManager;
    }

    private static final String ENDPOINT_INFO_CREATOR_SCHEMA = "endpointInfoCreator.schema";
    private static final String ENDPOINT_INFO_CREATOR_DNS = "endpointInfoCreator.dns";
    private static final String ENDPOINT_INFO_SCHEMA = "endpointInfo.schema";
    private static final String ENDPOINT_INFO_DNS = "endpointInfo.dns";

    @Override
    public EndpointInfo create(EndpointInfoCreator endpointInfoCreator)
            throws KapuaException {
        ArgumentValidator.notNull(endpointInfoCreator, "endpointInfoCreator");
        ArgumentValidator.notNull(endpointInfoCreator.getScopeId(), "endpointInfoCreator.scopeId");
        ArgumentValidator.notNull(endpointInfoCreator.getEndpointType(), "endpointInfoCreator.endpointType");

        ArgumentValidator.notEmptyOrNull(endpointInfoCreator.getSchema(), ENDPOINT_INFO_CREATOR_SCHEMA);
        ArgumentValidator.match(endpointInfoCreator.getSchema(), CommonsValidationRegex.URI_SCHEME, ENDPOINT_INFO_CREATOR_SCHEMA);
        ArgumentValidator.lengthRange(endpointInfoCreator.getSchema(), 1, 64, ENDPOINT_INFO_CREATOR_SCHEMA);

        ArgumentValidator.notEmptyOrNull(endpointInfoCreator.getDns(), ENDPOINT_INFO_CREATOR_DNS);
        ArgumentValidator.match(endpointInfoCreator.getDns(), CommonsValidationRegex.URI_DNS, ENDPOINT_INFO_CREATOR_DNS);
        ArgumentValidator.lengthRange(endpointInfoCreator.getDns(), 1, 1024, ENDPOINT_INFO_CREATOR_DNS);

        ArgumentValidator.notNegative(endpointInfoCreator.getPort(), "endpointInfoCreator.port");
        ArgumentValidator.numRange(endpointInfoCreator.getPort(), 1, 65535, "endpointInfoCreator.port");

        //
        // Check Access
        KapuaId scopeIdPermission = endpointInfoCreator.getEndpointType().equals(EndpointInfo.ENDPOINT_TYPE_CORS) ?
                endpointInfoCreator.getScopeId() : null;
        authorizationService.checkPermission(
                permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.write, scopeIdPermission)
        );

        //
        // Check duplicate endpoint
        checkDuplicateEndpointInfo(
                endpointInfoCreator.getScopeId(),
                null,
                endpointInfoCreator.getSchema(),
                endpointInfoCreator.getDns(),
                endpointInfoCreator.getPort());

        //
        // Do create

        final EndpointInfo endpointInfo = new EndpointInfoImpl(endpointInfoCreator.getScopeId());
        endpointInfo.setSchema(endpointInfoCreator.getSchema());
        endpointInfo.setDns(endpointInfoCreator.getDns());
        endpointInfo.setPort(endpointInfoCreator.getPort());
        endpointInfo.setSecure(endpointInfoCreator.getSecure());
        endpointInfo.setUsages(endpointInfoCreator.getUsages());
        endpointInfo.setEndpointType(endpointInfoCreator.getEndpointType());

        return txManager.execute(tx -> repository.create(tx, endpointInfo));
    }

    @Override
    public EndpointInfo update(EndpointInfo endpointInfo) throws KapuaException {
        ArgumentValidator.notNull(endpointInfo, "endpointInfo");
        ArgumentValidator.notNull(endpointInfo.getScopeId(), "endpointInfo.scopeId");

        ArgumentValidator.notEmptyOrNull(endpointInfo.getSchema(), ENDPOINT_INFO_SCHEMA);
        ArgumentValidator.match(endpointInfo.getSchema(), CommonsValidationRegex.URI_SCHEME, ENDPOINT_INFO_SCHEMA);
        ArgumentValidator.lengthRange(endpointInfo.getSchema(), 1, 64, ENDPOINT_INFO_SCHEMA);

        ArgumentValidator.notEmptyOrNull(endpointInfo.getDns(), ENDPOINT_INFO_DNS);
        ArgumentValidator.match(endpointInfo.getDns(), CommonsValidationRegex.URI_DNS, ENDPOINT_INFO_DNS);
        ArgumentValidator.lengthRange(endpointInfo.getDns(), 1, 1024, ENDPOINT_INFO_DNS);

        ArgumentValidator.notNegative(endpointInfo.getPort(), "endpointInfo.port");
        ArgumentValidator.numRange(endpointInfo.getPort(), 1, 65535, "endpointInfo.port");

        //
        // Check Access
        KapuaId scopeIdPermission = endpointInfo.getEndpointType().equals(EndpointInfo.ENDPOINT_TYPE_CORS) ?
                endpointInfo.getScopeId() : null;
        authorizationService.checkPermission(
                permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.write, scopeIdPermission)
        );

        //
        // Check duplicate endpoint
        checkDuplicateEndpointInfo(
                endpointInfo.getScopeId(),
                endpointInfo.getId(),
                endpointInfo.getSchema(),
                endpointInfo.getDns(),
                endpointInfo.getPort());

        //
        // Do update
        return txManager.execute(tx -> repository.update(tx, endpointInfo));
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId endpointInfoId) throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(endpointInfoId, "endpointInfoId");

        //
        // Check Access
        txManager.execute(tx -> {
            EndpointInfo endpointInfoToDelete = repository.find(tx, scopeId, endpointInfoId);
            KapuaId scopeIdPermission = null;
            if (endpointInfoToDelete != null && endpointInfoToDelete.getEndpointType().equals(EndpointInfo.ENDPOINT_TYPE_CORS)) {
                scopeIdPermission = scopeId;
            }

            authorizationService.checkPermission(
                    permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.delete, scopeIdPermission)
            );

            //
            // Do delete
            return repository.delete(tx, scopeId, endpointInfoId);
        });
    }

    @Override
    public EndpointInfo find(KapuaId scopeId, KapuaId endpointInfoId)
            throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(endpointInfoId, "endpointInfoId");

        //
        // Check Access
        return txManager.execute(tx -> {
            authorizationService.checkPermission(
                    permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.read, scopeId)
            );

            EndpointInfo endpointInfoToFind = repository.find(tx, KapuaId.ANY, endpointInfoId); // search the endpoint in any scope

            if (endpointInfoToFind == null) {
                throw new KapuaEntityNotFoundException(EndpointInfo.TYPE, endpointInfoId);
            }

            if (endpointInfoToFind.getScopeId().equals(scopeId)) { //found in the specified scope, search finish here
                return endpointInfoToFind;
            }
            //found but in another scope...is defined in the scope of the first Account that has defined endpoints? (proceeding upwards)
            String type = endpointInfoToFind.getEndpointType();
            //now find the endpoints of the search type that I can use (aka, the nearest proceeding upwards in Accounts hierarchy)
            EndpointInfoQuery query = endpointInfoFactory.newQuery(scopeId);
            EndpointInfoListResult nearestUsableEndpoints = doQuery(tx, query, type);

            if (nearestUsableEndpoints.isEmpty() || !nearestUsableEndpoints.getFirstItem().getScopeId().equals(endpointInfoToFind.getScopeId())) { //the second condition is equivalent to verify if the searched endpoint is in this list
                throw new KapuaEntityNotFoundException(EndpointInfo.TYPE, endpointInfoId);
            } else {
                return endpointInfoToFind;
            }
        });
    }

    @Override
    public EndpointInfoListResult query(KapuaQuery query) throws KapuaException {
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(
                permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.read, query.getScopeId())
        );
        //
        // Do Query
        return txManager.execute(tx -> {
            return doQuery(tx, query, EndpointInfo.ENDPOINT_TYPE_RESOURCE);
        });
    }

    @Override
    public EndpointInfoListResult query(KapuaQuery query, String section)
            throws KapuaException {
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(
                permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.read, query.getScopeId())
        );

        //
        // Do Query
        return txManager.execute(tx -> {
            return doQuery(tx, query, section);
        });
    }

    private EndpointInfoListResult doQuery(TxContext tx, KapuaQuery query, String section) throws KapuaException {
        addSectionToPredicate(query, section);

        EndpointInfoListResult endpointInfoListResult;
        endpointInfoListResult = repository.query(tx, query);
        if (endpointInfoListResult.isEmpty() && query.getScopeId() != null) {

            // First check if there are any endpoint specified at all
            if (countAllEndpointsInScope(tx, query.getScopeId(), section)) {
                // if there are endpoints (even not matching the query), return the empty list
                return endpointInfoListResult;
            }

            KapuaId originalScopeId = query.getScopeId();

            do {
                Account account = accountRepository.find(tx, KapuaId.ANY, query.getScopeId());

                if (account == null) {
                    throw new KapuaEntityNotFoundException(Account.TYPE, query.getScopeId());
                }
                if (account.getScopeId() == null) {
                    // Query was originally on root account, and querying on parent scope id would mean querying in null,
                    // i.e. querying on all accounts. Since that's not what we want, break away.
                    break;
                }
                query.setScopeId(account.getScopeId());
                endpointInfoListResult = repository.query(tx, query);
            }
            while (query.getScopeId() != null && endpointInfoListResult.isEmpty());

            query.setScopeId(originalScopeId);
        }

        return endpointInfoListResult;
    }

    @Override
    public long count(KapuaQuery query) throws KapuaException {
        return doCount(query, EndpointInfo.ENDPOINT_TYPE_RESOURCE);
    }

    @Override
    public long count(KapuaQuery query, String section)
            throws KapuaException {
        return doCount(query, section);
    }

    private Long doCount(KapuaQuery query, String section) throws KapuaException {
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(
                permissionFactory.newPermission(EndpointInfoDomains.ENDPOINT_INFO_DOMAIN, Actions.read, query.getScopeId())
        );

        //
        // Do count
        return txManager.execute(tx -> {
            long endpointInfoCount = repository.count(tx, query);

            if (endpointInfoCount == 0 && query.getScopeId() != null) {

                // First check if there are any endpoint specified at all
                if (countAllEndpointsInScope(tx, query.getScopeId(), section)) {
                    // if there are endpoints (even not matching the query), return 0
                    return endpointInfoCount;
                }

                KapuaId originalScopeId = query.getScopeId();

                do {
                    Account account = accountRepository.find(tx, KapuaId.ANY, query.getScopeId());

                    if (account == null) {
                        throw new KapuaEntityNotFoundException(Account.TYPE, query.getScopeId());
                    }

                    query.setScopeId(account.getScopeId());
                    endpointInfoCount = repository.count(tx, query);
                }
                while (query.getScopeId() != null && endpointInfoCount == 0);

                query.setScopeId(originalScopeId);
            }
            return endpointInfoCount;
        });
    }

    //
    // Private methods
    //

    /**
     * Checks whether or not another {@link EndpointInfo} already exists with the given values.
     *
     * @param scopeId  The ScopeId of the {@link EndpointInfo}
     * @param entityId The entity id, if exists. On update you need to exclude the same entity.
     * @param schema   The {@link EndpointInfo#getSchema()}  value.
     * @param dns      The {@link EndpointInfo#getDns()}  value.
     * @param port     The {@link EndpointInfo#getPort()} value.
     * @throws KapuaException if the values provided matches another {@link EndpointInfo}
     * @since 1.0.0
     */
    private void checkDuplicateEndpointInfo(KapuaId scopeId, KapuaId entityId, String schema, String dns, int port) throws KapuaException {

        EndpointInfoQuery query = new EndpointInfoQueryImpl(scopeId);

        AndPredicate andPredicate = query.andPredicate(
                query.attributePredicate(EndpointInfoAttributes.SCHEMA, schema),
                query.attributePredicate(EndpointInfoAttributes.DNS, dns),
                query.attributePredicate(EndpointInfoAttributes.PORT, port)
        );

        if (entityId != null) {
            andPredicate.and(query.attributePredicate(KapuaEntityAttributes.ENTITY_ID, entityId, Operator.NOT_EQUAL));
        }

        query.setPredicate(andPredicate);

        if (doCount(query, EndpointInfo.ENDPOINT_TYPE_RESOURCE) > 0) {
            List<Map.Entry<String, Object>> uniquesFieldValues = new ArrayList<>();
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(KapuaEntityAttributes.SCOPE_ID, scopeId));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(EndpointInfoAttributes.SCHEMA, schema));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(EndpointInfoAttributes.DNS, dns));
            uniquesFieldValues.add(new AbstractMap.SimpleEntry<>(EndpointInfoAttributes.PORT, port));

            throw new KapuaEntityUniquenessException(EndpointInfo.TYPE, uniquesFieldValues);
        }
    }

    private boolean countAllEndpointsInScope(TxContext txContext, KapuaId scopeId, String section) throws KapuaException {
        EndpointInfoQuery totalQuery = endpointInfoFactory.newQuery(scopeId);
        addSectionToPredicate(totalQuery, section);
        long totalCount = repository.count(txContext, totalQuery);
        return totalCount != 0;
    }

    private void addSectionToPredicate(KapuaQuery query, String section) {
        QueryPredicate sectionPredicate = query.attributePredicate(EndpointInfoAttributes.ENDPOINT_TYPE, section);
        QueryPredicate currentPredicate = query.getPredicate();
        query.setPredicate(currentPredicate == null ? sectionPredicate : query.andPredicate(currentPredicate, sectionPredicate));
    }

}
