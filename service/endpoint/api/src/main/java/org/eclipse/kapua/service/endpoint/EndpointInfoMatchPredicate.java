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

import java.util.Arrays;

import org.eclipse.kapua.model.query.predicate.AbstractMatchPredicate;

public class EndpointInfoMatchPredicate<T> extends AbstractMatchPredicate<T> {

    /**
     * Constructor.
     *
     * @param matchTerm
     * @since 2.1.0
     */
    public EndpointInfoMatchPredicate(T matchTerm) {
        this.attributeNames = Arrays.asList(
                EndpointInfoAttributes.SCHEMA,
                EndpointInfoAttributes.DNS
        );
        this.matchTerm = matchTerm;
    }
}
