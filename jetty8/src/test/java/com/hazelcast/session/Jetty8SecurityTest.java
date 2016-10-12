package com.hazelcast.session;

public class Jetty8SecurityTest extends AbstractJettySecurityTest {
    @Override
    protected WebContainerConfigurator<?> getJettyConfigurator(String appName) {
        return new JettyConfigurator(appName);
    }
}
