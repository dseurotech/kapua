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
package org.eclipse.kapua.service.job.step.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.kapua.model.query.KapuaListResult;

/**
 * {@link JobStepDefinitionListResult} definition.
 *
 * @since 1.0.0
 */
@XmlRootElement(name = "jobStepDefinitionListResult")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType
public class JobStepDefinitionListResult extends KapuaListResult<JobStepDefinition> {

    private static final long serialVersionUID = 977813250632719295L;

}
