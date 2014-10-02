package com.hazelcast.session.nonsticky;

import com.hazelcast.session.JettyConfigurator;
import com.hazelcast.session.WebContainerConfigurator;
import com.hazelcast.session.sticky.ClientServerStickySessionsTest;

public class JettyClientServerNonStickySessionsTest extends ClientServerStickySessionsTest {

    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator();
    }
}
