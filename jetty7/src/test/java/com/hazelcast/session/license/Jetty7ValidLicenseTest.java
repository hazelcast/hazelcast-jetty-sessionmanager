package com.hazelcast.session.license;

import com.hazelcast.session.JettyConfigurator;
import com.hazelcast.session.WebContainerConfigurator;

public class Jetty7ValidLicenseTest extends AbstractValidLicenseTest {

    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator("hazelcast-with-valid-license.xml","hazelcast-client-with-valid-license.xml");
    }
}
