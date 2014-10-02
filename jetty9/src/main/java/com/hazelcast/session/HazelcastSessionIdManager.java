package com.hazelcast.session;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigLoader;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * Based on the {@link org.eclipse.jetty.nosql.mongodb.MongoSessionIdManager}
 * <br/>
 * A jetty instance can only have one session id manager. HazelcastSessionIdManager is responsible of
 * creating Hazelcast instances (client/server/provided) to keep session data distributed and ensuring
 * the cluster-wide unique session id generation.
 *
 * This manager can use Hazelcast in three different ways
 * <ul>
 *     <li>
 *        <code>clientOnly</code> If this attribute is set to <code>true</code>, then an hazelcast client instance
 *        is started<br/>
 *
 *        See {@link HazelcastSessionIdManager#createHazelcastClientInstance()} for details
 *     </li>
 *     <li>
 *         <code>clientOnly</code> If this attribute is set to <code>false</code> (default behaviour), then a hazelcast
 *         instance is started<br/>
 *         See {@link HazelcastSessionIdManager#createHazelcastFullInstance()} for details
 *     </li>
 *     <li>
 *         A preconfigured {@link com.hazelcast.core.HazelcastInstance} can be provided in construction time
 *         <br/>
 *         See
 *         {@link HazelcastSessionIdManager#HazelcastSessionIdManager(org.eclipse.jetty.server.Server,
 *                                                                      com.hazelcast.core.HazelcastInstance)}
 *     </li>
 * </ul>
 *
 * Also, this class has a clean-up timer {@link HazelcastSessionIdManager#cleanUpTimer} which is responsible of
 * removing expired session data from session map.
 * </p>
 */
public class HazelcastSessionIdManager extends AbstractSessionIdManager {

    private final static Logger LOG = Log.getLogger("com.hazelcast.session");

    private final static String DEFAULT_MAP_NAME = "session-replication-map";
    private final static String DEFAULT_HZ_CONFIG_LOCATION = "hazelcast.xml";
    private final static String DEFAULT_HZ_CLIENT_CONFIG_LOCATION =  "hazelcast-client-default.xml";

    /**
     * Hazelcast config location, can be left <code>null</code> to use defaults
     */
    private String configLocation;

    /**
     * Hazelcast client/server switch, set to <code>true</code> to start hazelcast instance in client mode
     */
    private boolean clientOnly;


    /**
     * Clean-up process is enabled by default
     */
    private boolean cleanUp = true;
    private Timer cleanUpTimer;
    private TimerTask cleanUpTask;

    /**
     * clean-up process working period
     */
    private long cleanUpPeriod = 24 * 60 * 60 * 1000; // every day

    /**
     * Maximum age of session data entries to be swiped by clean-up task
     */
    private long cleanUpInvalidAge = 60 * 60 * 1000; //one hour

    protected Server server;

    /**
     * Distributed session data
     */
    private IMap<String, HazelcastSessionData> sessions;

    private HazelcastInstance instance;

    /**
     * the (local) collection of session ids known to this manager
     */
    protected final Set<String> sessionsIds = new HashSet<String>();

    /**
     * Creates a session manager with defaults<br/>
     */
    public HazelcastSessionIdManager(Server server) {
        this(server, false, null);
    }

    public HazelcastSessionIdManager(Server server, boolean clientOnly) {
        this(server, clientOnly, null);
    }

    public HazelcastSessionIdManager(Server server, boolean clientOnly, String configLocation) {
        super(new Random());
        this.server = server;
        this.configLocation = configLocation;
        this.clientOnly = clientOnly;
    }

    public HazelcastSessionIdManager(Server server, HazelcastInstance instance) {
        super(new Random());
        this.server = server;
        this.instance = instance;
    }

    /**
     * is the session id known to hazelcast, and is it valid
     */
    public boolean idInUse(String sessionId) {
        LOG.debug("HazelcastSessionIdManager:idInUse:sessionId= " + sessionId );
        HazelcastSessionData o = sessions.get(sessionId);

        boolean idInUse = false;
        if (o != null) {
            idInUse = o.isValid();
        }
        LOG.debug("HazelcastSessionIdManager:idInUse:sessionId= " + sessionId + "::" + idInUse);
        return idInUse;
    }

    /**
     *  Add session id to known local ids
     */
    public void addSession(HttpSession session) {
        if (session == null) {
            return;
        }

        LOG.debug("HazelcastSessionIdManager:addSession:" + session.getId());

        synchronized (sessionsIds) {
            sessionsIds.add(session.getId());
        }

    }

    /**
     * Remove session from known local ids
     * @param session
     */
    public void removeSession(HttpSession session)
    {
        if (session == null) {
            return;
        }

        synchronized (sessionsIds) {
            sessionsIds.remove(session.getId());
        }
    }

    public void invalidateAll(String sessionId) {

        synchronized (sessionsIds) {
            sessionsIds.remove(sessionId);

            //tell all contexts that may have a session object with this id to
            //get rid of them
            Handler[] contexts = server.getChildHandlersByClass(ContextHandler.class);
            for (int i=0; contexts!=null && i<contexts.length; i++) {
                SessionHandler sessionHandler = ((ContextHandler)contexts[i]).getChildHandlerByClass(SessionHandler.class);
                if (sessionHandler != null) {
                    SessionManager manager = sessionHandler.getSessionManager();
                    if (manager != null && manager instanceof HazelcastSessionManager) {
                        ((HazelcastSessionManager)manager).invalidateSession(sessionId);
                    }
                }
            }
        }
    }

    /** Get the session ID with any worker ID.
     *
     * @param clusterId
     * @param request
     * @return sessionId plus any worker ID.
     */
    public String getNodeId(String clusterId,HttpServletRequest request) {
        // used in Ajp13Parser
        String worker=request==null?null:(String)request.getAttribute("org.eclipse.jetty.ajp.JVMRoute");
        if (worker!=null)
            return clusterId+'.'+worker;

        if (_workerName!=null)
            return clusterId+'.'+_workerName;

        return clusterId;
    }

    /** Get the session ID without any worker ID.
     *
     * @param nodeId the node id
     * @return sessionId without any worker ID.
     */
    public String getClusterId(String nodeId) {
        int dot=nodeId.lastIndexOf('.');
        return (dot>0)?nodeId.substring(0,dot):nodeId;
    }

    /**
     * Create a hazelcast client/server instance depending on the clientOnly property
     */
    private HazelcastInstance createHazelcastInstance() {

        HazelcastInstance instance;
        if (clientOnly) {
            instance = createHazelcastClientInstance();
        } else {
            instance = createHazelcastFullInstance();
        }
        return instance;
    }

    /**
     * Create a Hazelcast client instance to connect an existing cluster
     * @return
     */
    private HazelcastInstance createHazelcastClientInstance() {
        ClientConfig config;
        if (getConfigLocation() == null) {
            setConfigLocation(DEFAULT_HZ_CLIENT_CONFIG_LOCATION);
        }
        try {
            XmlClientConfigBuilder builder = new XmlClientConfigBuilder(getConfigLocation());
            config = builder.build();
        } catch (IOException e) {
            throw new RuntimeException("failed to load Config:", e);
        }

        if (config == null) {
            throw new RuntimeException("failed to find configLocation:" + getConfigLocation());
        }

        return HazelcastClient.newHazelcastClient(config);
    }

    private HazelcastInstance createHazelcastFullInstance() {

        Config config;
        if (getConfigLocation() == null) {
            setConfigLocation(DEFAULT_HZ_CONFIG_LOCATION);
        }

        try {
            config = ConfigLoader.load(getConfigLocation());
        } catch (IOException e) {
            throw new RuntimeException("failed to load Config:", e);
        }

        if (config == null) {
            throw new RuntimeException("failed to find configLocation:" + getConfigLocation());
        }
        config.setInstanceName(com.hazelcast.session.SessionManager.DEFAULT_INSTANCE_NAME);
        return Hazelcast.getOrCreateHazelcastInstance(config);
    }

    private IMap<String, HazelcastSessionData> initializeSessionMap() {
        return instance.getMap(DEFAULT_MAP_NAME);
    }

    /**
     * Clean is a process that cleans the Hazelcast cluster of old sessions that are no
     * longer valid.
     *
     * There are two checks being done here:
     *
     *  - if the accessed time is older then the current time minus the cleanup invalid age
     *    and it is no longer valid then remove that session
     *
     *  NOTE: if your system supports long lived sessions then the cleanup invalid age should be
     *  set to zero so the check is skipped.
     *

     */
    protected void cleanUp()
    {
        for (Map.Entry<String, HazelcastSessionData> entry : sessions.entrySet()) {
            if(entry.getValue().getAccessed() < System.currentTimeMillis() - cleanUpInvalidAge) {
                sessions.remove(entry.getKey());
            }
        }

    }

    @Override
    protected void doStart() throws Exception {

        if(instance == null) {
            instance = createHazelcastInstance();
        }
        sessions = initializeSessionMap();

        if(cleanUp) {
            cleanUpTimer = new Timer("HazelcastSessionCleaner", true);
            synchronized (this) {
                if(cleanUpTask != null) {
                    cleanUpTask.cancel();
                }
                cleanUpTask = new TimerTask() {
                    @Override
                    public void run() {
                        cleanUp();
                    }
                };
                cleanUpTimer.schedule(cleanUpTask, 0, cleanUpPeriod);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        if(cleanUpTimer != null) {
            cleanUpTimer.cancel();
            cleanUpTimer = null;
        }

        super.doStop();
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public boolean isClientOnly() {
        return clientOnly;
    }

    public void setClientOnly(boolean clientOnly) {
        this.clientOnly = clientOnly;
    }


    public IMap<String, HazelcastSessionData> getSessions() {
        return sessions;
    }

    public boolean isCleanUpEnabled() {
        return cleanUp;
    }

    public void setCleanUp(boolean cleanUp) {
        cleanUp = cleanUp;
    }

    public long getCleanUpInvalidAge() {
        return cleanUpInvalidAge;
    }

    public void setCleanUpInvalidAge(long cleanUpInvalidAge) {
        cleanUpInvalidAge = cleanUpInvalidAge;
    }

    public void setCleanUpPeriod(long cleanUpPeriod) {
        this.cleanUpPeriod = cleanUpPeriod;
    }
}
