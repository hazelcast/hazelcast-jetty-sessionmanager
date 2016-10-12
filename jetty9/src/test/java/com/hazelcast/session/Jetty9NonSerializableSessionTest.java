package com.hazelcast.session;

public class Jetty9NonSerializableSessionTest extends AbstractNonSerializableSessionTest {
    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator();
    }
}
