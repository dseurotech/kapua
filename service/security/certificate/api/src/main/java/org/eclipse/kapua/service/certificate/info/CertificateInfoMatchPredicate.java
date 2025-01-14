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

import java.util.Arrays;

import org.eclipse.kapua.model.query.predicate.AbstractMatchPredicate;

public class CertificateInfoMatchPredicate<T> extends AbstractMatchPredicate<T> {

    /**
     * Constructor.
     *
     * @param matchTerm
     * @since 2.1.0
     */
    public CertificateInfoMatchPredicate(T matchTerm) {
        this.attributeNames = Arrays.asList(
                CertificateInfoAttributes.NAME,
                CertificateInfoAttributes.SERIAL,
                CertificateInfoAttributes.SIGNATURE,
                CertificateInfoAttributes.ALGORITHM,
                CertificateInfoAttributes.SUBJECT
        );
        this.matchTerm = matchTerm;
    }
}