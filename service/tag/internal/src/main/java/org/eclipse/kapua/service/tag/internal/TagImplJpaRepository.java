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
package org.eclipse.kapua.service.tag.internal;

import org.eclipse.kapua.commons.jpa.EntityManagerSession;
import org.eclipse.kapua.commons.jpa.KapuaUpdateableEntityRepositoryJpaImpl;
import org.eclipse.kapua.model.query.KapuaListResult;
import org.eclipse.kapua.service.tag.Tag;
import org.eclipse.kapua.service.tag.TagRepository;

import javax.inject.Singleton;
import java.util.function.Supplier;

@Singleton
public class TagImplJpaRepository extends KapuaUpdateableEntityRepositoryJpaImpl<Tag, TagImpl> implements TagRepository {
    public TagImplJpaRepository(Supplier<? extends KapuaListResult<Tag>> listProvider, EntityManagerSession entityManagerSession) {
        super(TagImpl.class, listProvider, entityManagerSession);
    }

}
