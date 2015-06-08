package com.hazelcast.session.license;

import com.hazelcast.session.JettyConfigurator;
import com.hazelcast.session.WebContainerConfigurator;

public class Jetty7SecurityOnlyLicenseTest extends AbstractInvalidLicenseTest {

    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator("hazelcast-with-security-license.xml", "hazelcast-client-with-security-license.xml");
    }
}
