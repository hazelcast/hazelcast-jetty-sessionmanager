package com.hazelcast.session;

public class Jetty9SecurityTest extends AbstractJettySecurityTest {
    @Override
    protected WebContainerConfigurator<?> getJettyConfigurator(String appName) {
        return new JettyConfigurator(appName);
    }
}
