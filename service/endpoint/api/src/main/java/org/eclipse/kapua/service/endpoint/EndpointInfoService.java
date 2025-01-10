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
package org.eclipse.kapua.service.endpoint;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.KapuaEntityService;
import org.eclipse.kapua.service.KapuaUpdatableEntityService;

/**
 * {@link EndpointInfo} service definition.
 *
 * @since 1.0.0
 */
public interface EndpointInfoService extends KapuaEntityService<EndpointInfo, EndpointInfoCreator>,
        KapuaUpdatableEntityService<EndpointInfo> {

    KapuaListResult<EndpointInfo> query(KapuaQuery query, String section)
            throws KapuaException;

    long count(KapuaQuery query, String section)
            throws KapuaException;

}
