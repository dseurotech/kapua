/*******************************************************************************
 * Copyright (c) 2019, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.device.management.job.internal;

import org.eclipse.kapua.KapuaEntityNotFoundException;
import org.eclipse.kapua.KapuaEntityUniquenessException;
import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.model.query.predicate.AndPredicateImpl;
import org.eclipse.kapua.commons.model.query.predicate.AttributePredicateImpl;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.model.domain.Actions;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperation;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationAttributes;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationCreator;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationFactory;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationListResult;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationQuery;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationRepository;
import org.eclipse.kapua.service.device.management.job.JobDeviceManagementOperationService;
import org.eclipse.kapua.service.job.JobDomains;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link JobDeviceManagementOperationService} implementation
 *
 * @since 1.1.0
 */
@Singleton
public class JobDeviceManagementOperationServiceImpl
        implements JobDeviceManagementOperationService {

    private final JobDeviceManagementOperationFactory entityFactory;
    private final AuthorizationService authorizationService;
    private final PermissionFactory permissionFactory;
    private final JobDeviceManagementOperationRepository repository;

    @Inject
    public JobDeviceManagementOperationServiceImpl(
            JobDeviceManagementOperationFactory entityFactory,
            AuthorizationService authorizationService,
            PermissionFactory permissionFactory,
            JobDeviceManagementOperationRepository repository) {
        this.entityFactory = entityFactory;
        this.authorizationService = authorizationService;
        this.permissionFactory = permissionFactory;
        this.repository = repository;
    }

    @Override
    public JobDeviceManagementOperation create(JobDeviceManagementOperationCreator jobDeviceManagementOperationCreator) throws KapuaException {
        //
        // Argument validation
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator, "jobDeviceManagementOperationCreator");
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator.getScopeId(), "jobDeviceManagementOperationCreator.scopeId");
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator.getJobId(), "jobDeviceManagementOperationCreator.jobId");
        ArgumentValidator.notNull(jobDeviceManagementOperationCreator.getDeviceManagementOperationId(), "jobDeviceManagementOperationCreator.deviceManagementOperationId");

        //
        // Check access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.write, null));

        //
        // Check duplicate
        JobDeviceManagementOperationQuery query = new JobDeviceManagementOperationQueryImpl(jobDeviceManagementOperationCreator.getScopeId());
        query.setPredicate(
                new AndPredicateImpl(
                        new AttributePredicateImpl<>(JobDeviceManagementOperationAttributes.JOB_ID, jobDeviceManagementOperationCreator.getJobId()),
                        new AttributePredicateImpl<>(JobDeviceManagementOperationAttributes.DEVICE_MANAGEMENT_OPERATION_ID, jobDeviceManagementOperationCreator.getDeviceManagementOperationId())
                )
        );

        if (repository.count(query) > 0) {
            List<Map.Entry<String, Object>> uniqueAttributes = new ArrayList<>();

            uniqueAttributes.add(new AbstractMap.SimpleEntry<>(JobDeviceManagementOperationAttributes.JOB_ID, jobDeviceManagementOperationCreator.getJobId()));
            uniqueAttributes.add(new AbstractMap.SimpleEntry<>(JobDeviceManagementOperationAttributes.DEVICE_MANAGEMENT_OPERATION_ID, jobDeviceManagementOperationCreator.getDeviceManagementOperationId()));

            throw new KapuaEntityUniquenessException(JobDeviceManagementOperation.TYPE, uniqueAttributes);
        }

        //
        // Create JobDeviceManagementOperation
        JobDeviceManagementOperation newEntity = entityFactory.newEntity(jobDeviceManagementOperationCreator.getScopeId());
        newEntity.setJobId(jobDeviceManagementOperationCreator.getJobId());
        newEntity.setDeviceManagementOperationId(jobDeviceManagementOperationCreator.getDeviceManagementOperationId());

        //
        // Do create
        return repository.create(newEntity);
    }

    @Override
    public JobDeviceManagementOperation find(KapuaId scopeId, KapuaId jobDeviceManagementOperationId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(jobDeviceManagementOperationId, "jobDeviceManagementOperationId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.write, scopeId));

        //
        // Do find
        return repository.find(scopeId, jobDeviceManagementOperationId);
    }

    @Override
    public JobDeviceManagementOperationListResult query(KapuaQuery query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return repository.query(query);
    }

    @Override
    public long count(KapuaQuery query) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(query, "query");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.read, query.getScopeId()));

        //
        // Do query
        return repository.count(query);
    }

    @Override
    public void delete(KapuaId scopeId, KapuaId jobDeviceManagementOperationId) throws KapuaException {
        //
        // Argument Validation
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(jobDeviceManagementOperationId, "jobDeviceManagementOperationId");

        //
        // Check Access
        authorizationService.checkPermission(permissionFactory.newPermission(JobDomains.JOB_DOMAIN, Actions.delete, scopeId));

        //
        // Check existence
        if (find(scopeId, jobDeviceManagementOperationId) == null) {
            throw new KapuaEntityNotFoundException(JobDeviceManagementOperation.TYPE, jobDeviceManagementOperationId);
        }

        //
        // Do delete
        repository.delete(scopeId, jobDeviceManagementOperationId);
    }
}
