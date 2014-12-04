package com.hazelcast.session.nonsticky;

import com.hazelcast.session.Java6ExcludeRule;
import com.hazelcast.session.JettyConfigurator;
import com.hazelcast.session.WebContainerConfigurator;
import org.junit.Rule;

public class JettyClientServerNonStickySessionsTest extends ClientServerNonStickySessionsTest {

    @Rule
    public Java6ExcludeRule java6ExcludeRule = new Java6ExcludeRule();

    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return new JettyConfigurator();
    }
}
