package com.hazelcast.session.nonsticky;

import com.hazelcast.session.JettyConfigurator;
import com.hazelcast.session.WebContainerConfigurator;

public class JettyClientServerNonStickySessionsTest extends ClientServerNonStickySessionsTest {

    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator();
    }
}
