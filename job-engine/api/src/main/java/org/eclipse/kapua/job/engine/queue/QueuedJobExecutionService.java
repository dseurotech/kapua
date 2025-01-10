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
package org.eclipse.kapua.job.engine.queue;

import org.eclipse.kapua.service.KapuaEntityService;
import org.eclipse.kapua.service.KapuaUpdatableEntityService;

/**
 * {@link QueuedJobExecutionService} exposes APIs to manage {@link QueuedJobExecution} objects.<br> It includes APIs to create, update, find, list and delete {@link QueuedJobExecution}s.<br> Instances
 * of the {@link QueuedJobExecutionService} can be acquired through the {@link org.eclipse.kapua.locator.KapuaLocator} object.
 *
 * @since 1.1.0
 */
public interface QueuedJobExecutionService extends KapuaEntityService<QueuedJobExecution, QueuedJobExecutionCreator>,
        KapuaUpdatableEntityService<QueuedJobExecution> {

}
