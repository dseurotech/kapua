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
package org.eclipse.kapua.app.console.module.api.client.ui.panel;

import com.extjs.gxt.ui.client.widget.Layout;
import org.eclipse.kapua.app.console.module.api.client.resources.icons.KapuaIcon;

public class ContentPanel extends com.extjs.gxt.ui.client.widget.ContentPanel {

    private String originalHeading;
    private KapuaIcon icon;

    public ContentPanel() {
        super();
    }

    public ContentPanel(Layout layout) {
        super(layout);
    }

    @Override
    public String getHeadingHtml() {
        return originalHeading;
    }

    @Override
    public void setHeadingHtml(String heading) {
        super.setHeadingHtml((icon != null ? icon.getInlineHTML() + "&nbsp;&nbsp;" : "") + heading);
        this.originalHeading = heading;
    }

    public void setIcon(KapuaIcon icon) {
        super.setHeadingHtml(icon.getInlineHTML() + "&nbsp;&nbsp;" + originalHeading);
        this.icon = icon;
    }

}
