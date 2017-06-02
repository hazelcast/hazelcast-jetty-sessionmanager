package com.hazelcast.session;

import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyConfigurator extends WebContainerConfigurator<Server> {

    private final String appName;

    private Server server;

    public JettyConfigurator(String appName) {
        this.appName = appName;
    }

    public JettyConfigurator() {
        this.appName = "defaultApp";
    }

    @Override
    public Server configure() throws Exception {
        Server server = new Server(port);

        WebAppContext context = new WebAppContext();
        context.setResourceBase("../jetty-core/src/test/resources/" + appName);
        context.setLogUrlOnStart(true);
        context.setContextPath("/");
        context.setParentLoaderPriority(true);

        SessionContext sessionContext = new SessionContext( "foo", null );

        HazelcastSessionDataStoreFactory hazelcastSessionDataStoreFactory = new HazelcastSessionDataStoreFactory();
        HazelcastSessionDataStore hazelcastSessionDataStore = (HazelcastSessionDataStore) hazelcastSessionDataStoreFactory.getSessionDataStore(
            context.getSessionHandler() );
        hazelcastSessionDataStore.initialize( sessionContext );

        DefaultSessionCache defaultSessionCache = new DefaultSessionCache( context.getSessionHandler() );
        defaultSessionCache.setSessionDataStore( hazelcastSessionDataStore );
        context.getSessionHandler().setSessionCache( defaultSessionCache );

        server.setHandler(context);
        server.setStopTimeout(0);

        HashLoginService loginService = new HashLoginService();

        //loginService.putUser("someuser", Credential.getCredential("somepass"), new String[]{"role1", "role2"});
        context.getSecurityHandler().setLoginService(loginService);

        return server;
    }

    @Override
    public void start() throws Exception {
        server = configure();
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void reload() {
        try {
            server.stop();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
