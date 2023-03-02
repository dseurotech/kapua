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
package org.eclipse.kapua.service.job.internal;

import org.eclipse.kapua.commons.jpa.AbstractEntityManagerFactory;
import org.eclipse.kapua.commons.jpa.EntityManagerFactory;
import org.eclipse.kapua.service.job.JobRepository;

import javax.inject.Singleton;

/**
 * {@link JobServiceImpl} {@link EntityManagerFactory} implementation.
 *
 * @since 1.0.0
 * @deprecated since 2.0.0 - use {@link JobRepository} instead
 */
@Deprecated
@Singleton
public class JobEntityManagerFactory extends AbstractEntityManagerFactory implements EntityManagerFactory {

    private static final String PERSISTENCE_UNIT_NAME = "kapua-job";

    private static final JobEntityManagerFactory INSTANCE = new JobEntityManagerFactory();

    /**
     * Constructor.
     *
     * @since 1.0.0
     */
    public JobEntityManagerFactory() {
        super(PERSISTENCE_UNIT_NAME);
    }

    /**
     * Returns the {@link EntityManagerFactory} instance.
     *
     * @return The {@link EntityManagerFactory} instance.
     * @since 1.0.0
     * @deprecated since 2.0.0 - Please use {@link JobEntityManagerFactory#JobEntityManagerFactory()} instead. This may be removed in future releases
     */
    @Deprecated
    public static JobEntityManagerFactory getInstance() {
        return INSTANCE;
    }
}
