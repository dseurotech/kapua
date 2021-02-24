/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kapua.translator.kura.kapua;

import org.eclipse.kapua.commons.util.xml.XmlUtil;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.service.device.call.kura.app.BundleMetrics;
import org.eclipse.kapua.service.device.call.kura.model.bundle.KuraBundles;
import org.eclipse.kapua.service.device.call.message.kura.app.response.KuraResponseChannel;
import org.eclipse.kapua.service.device.call.message.kura.app.response.KuraResponseMessage;
import org.eclipse.kapua.service.device.call.message.kura.app.response.KuraResponsePayload;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundle;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundleFactory;
import org.eclipse.kapua.service.device.management.bundle.DeviceBundles;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleResponseChannel;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleResponseMessage;
import org.eclipse.kapua.service.device.management.bundle.message.internal.BundleResponsePayload;
import org.eclipse.kapua.service.device.management.commons.setting.DeviceManagementSetting;
import org.eclipse.kapua.service.device.management.commons.setting.DeviceManagementSettingKey;
import org.eclipse.kapua.translator.exception.InvalidChannelException;
import org.eclipse.kapua.translator.exception.InvalidPayloadException;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * {@link org.eclipse.kapua.translator.Translator} implementation from {@link KuraResponseMessage} to {@link BundleResponseMessage}
 *
 * @since 1.0.0
 */
public class TranslatorAppBundleKuraKapua extends AbstractSimpleTranslatorResponseKuraKapua<BundleResponseChannel, BundleResponsePayload, BundleResponseMessage> {

    private static final String CHAR_ENCODING = DeviceManagementSetting.getInstance().getString(DeviceManagementSettingKey.CHAR_ENCODING);

    private static final KapuaLocator LOCATOR = KapuaLocator.getInstance();

    public TranslatorAppBundleKuraKapua() {
        super(BundleResponseMessage.class, BundleResponsePayload.class);
    }

    @Override
    protected BundleResponseChannel translateChannel(KuraResponseChannel kuraResponseChannel) throws InvalidChannelException {
        try {
            TranslatorKuraKapuaUtils.validateKuraResponseChannel(kuraResponseChannel, BundleMetrics.APP_ID, BundleMetrics.APP_VERSION);

            return new BundleResponseChannel();
        } catch (Exception e) {
            throw new InvalidChannelException(e, kuraResponseChannel);
        }
    }

    @Override
    protected BundleResponsePayload translatePayload(KuraResponsePayload kuraResponsePayload) throws InvalidPayloadException {
        BundleResponsePayload bundleResponsePayload = super.translatePayload(kuraResponsePayload);

        try {
            if (kuraResponsePayload.hasBody()) {
                KuraBundles kuraBundles = readXmlBodyAs(kuraResponsePayload.getBody(), KuraBundles.class);

                translate(bundleResponsePayload, kuraBundles);
            }

            // Return Kapua Payload
            return bundleResponsePayload;
        } catch (InvalidPayloadException ipe) {
            throw ipe;
        } catch (Exception e) {
            throw new InvalidPayloadException(e, kuraResponsePayload);
        }
    }

    private void translate(BundleResponsePayload bundleResponsePayload, KuraBundles kuraBundles) throws Exception {
        DeviceBundleFactory deviceBundleFactory = LOCATOR.getFactory(DeviceBundleFactory.class);

        DeviceBundles deviceBundles = deviceBundleFactory.newBundleListResult();
        List<DeviceBundle> deviceBundlesList = deviceBundles.getBundles();

        Arrays.stream(kuraBundles.getBundles()).forEach(kuraBundle -> {
            DeviceBundle deviceBundle = deviceBundleFactory.newDeviceBundle();
            deviceBundle.setId(kuraBundle.getId());
            deviceBundle.setName(kuraBundle.getName());
            deviceBundle.setVersion(kuraBundle.getVersion());
            deviceBundle.setState(kuraBundle.getState());
            deviceBundlesList.add(deviceBundle);
        });

        StringWriter sw = new StringWriter();
        XmlUtil.marshal(deviceBundles, sw);
        byte[] requestBody = sw.toString().getBytes(CHAR_ENCODING);

        bundleResponsePayload.setBody(requestBody);
    }
}
