package com.hazelcast.session.sticky;

import com.hazelcast.session.JettyConfigurator;
import com.hazelcast.session.WebContainerConfigurator;
import org.eclipse.jetty.server.SessionManager;

public class JettyP2PStickySessionsTest extends P2PStickySessionsTest {

    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator();
    }
}
