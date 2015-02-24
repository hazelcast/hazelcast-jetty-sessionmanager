package com.hazelcast.session;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;

public class JettyConfigurator extends WebContainerConfigurator<Server>{

    Server server;
    private SessionManager manager;


    @Override
    public Server configure() throws Exception {
        Server server = new Server(port);
        final URL root = new URL(JettyConfigurator.class.getResource("/"), "../test-classes");
        // use file to get correct separator char, replace %20 introduced by URL for spaces
        final String cleanedRoot = new File(root.getFile().replaceAll("%20", " ")).toString();

        final String fileSeparator = File.separator.equals("\\") ? "\\\\" : File.separator;
        final String sourceDir = cleanedRoot + File.separator + JettyConfigurator.class.getPackage().getName().replaceAll("\\.", fileSeparator) + File.separator + "webapp" + File.separator;


        WebAppContext context = new WebAppContext();
        context.setResourceBase(sourceDir);
        context.setDescriptor(sourceDir + "WEB-INF/web.xml");
        context.setLogUrlOnStart(true);
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        HazelcastSessionIdManager idManager;

        if (!clientOnly) {
            idManager = new HazelcastSessionIdManager(server);
        } else {
            idManager = new HazelcastSessionIdManager(server, clientOnly);
        }

        idManager.setWorkerName("worker-"+port);
        server.setSessionIdManager(idManager);

        HazelcastSessionManager sessionManager = new HazelcastSessionManager();
        sessionManager.setSessionIdManager(idManager);

        SessionHandler handler = new SessionHandler(sessionManager);
        context.setSessionHandler(handler);

        server.setHandler(context);

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
            System.out.println("Stopping Jetty");
            server.stop();
            System.out.println("Jetty stopped");
        } catch (Exception e) {
            System.out.println("JETTY_STOP_EXCEPTION");
            e.printStackTrace();
        }
        try {
            System.out.println("Starting Jetty");
            server.start();
            System.out.println("Jetty started");
        } catch (Exception e) {
            System.out.println("JETTY_START_EXCEPTION");
            e.printStackTrace();
        }

    }

    @Override
    public SessionManager getManager() {
        return manager;
    }
}
