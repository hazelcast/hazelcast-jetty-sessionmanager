package com.hazelcast.session;

public class Jetty7SecurityTest extends AbstractJettySecurityTest {
    @Override
    protected WebContainerConfigurator<?> getJettyConfigurator(String appName) {
        return new JettyConfigurator(appName);
    }
}
