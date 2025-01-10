/*******************************************************************************
 * Copyright (c) 2018, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kapua.service.certificate.info;

import java.util.List;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.service.KapuaEntityService;
import org.eclipse.kapua.service.certificate.CertificateUsage;

/**
 * {@link CertificateInfo} {@link KapuaEntityService} definition.
 *
 * @since 1.1.0
 */
public interface CertificateInfoService extends KapuaEntityService<CertificateInfo, CertificateInfoCreator> {

    /**
     * @param scopeId
     * @param usage
     * @return
     * @throws KapuaException
     * @since 1.1.0
     * @deprecated Since 2.0.0 Use the query method with CertificateQuery.setIncludeInherited(true) instead
     */
    @Deprecated
    List<CertificateInfo> findAncestorsCertificates(KapuaId scopeId, CertificateUsage usage) throws KapuaException;
}
