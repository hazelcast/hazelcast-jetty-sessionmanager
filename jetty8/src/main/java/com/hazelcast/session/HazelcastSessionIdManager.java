package com.hazelcast.session;


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
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.hazelcast.session.JettySessionUtils.DAY_IN_MILLISECONDS;
import static com.hazelcast.session.JettySessionUtils.DEFAULT_MAP_NAME;
import static com.hazelcast.session.JettySessionUtils.HOUR_IN_MILLISECONDS;


/**
 * <p>
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
 *        See {@link com.hazelcast.session.JettySessionUtils#createHazelcastClientInstance(String)} for details
 *     </li>
 *     <li>
 *         <code>clientOnly</code> If this attribute is set to <code>false</code> (default behaviour), then a hazelcast
 *         instance is started<br/>
 *         See {@link com.hazelcast.session.JettySessionUtils#createHazelcastFullInstance(String)} for details
 *     </li>
 *     <li>
 *         A preconfigured {@link com.hazelcast.core.HazelcastInstance} can be provided in construction time
 *         <br/>
 *         See
 *         {@link com.hazelcast.session.HazelcastSessionIdManager#HazelcastSessionIdManager(org.eclipse.jetty.server.Server,
 *                                                                      com.hazelcast.core.HazelcastInstance)}
 *     </li>
 * </ul>
 *
 * Also, this class has a clean-up timer {@link com.hazelcast.session.HazelcastSessionIdManager#cleanUpTimer}
 * which is responsible of
 * removing expired session data from session map.
 * </p>
 */
public class HazelcastSessionIdManager extends AbstractSessionIdManager {

    private static final Logger LOG = Log.getLogger("com.hazelcast.session");

    protected final Server server;

    /**
     * the (local) collection of session ids known to this manager
     */
    protected final Set<String> sessionsIds = new HashSet<String>();

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
     * clean-up process working period in milliseconds
     * Default value => everday
     */
    private long cleanUpPeriod = DAY_IN_MILLISECONDS;

    /**
     * Maximum age of session data entries to be swiped by clean-up task
     * Default value => one hour
     */
    private long cleanUpInvalidAge = HOUR_IN_MILLISECONDS;

    /**
     * Distributed session data
     */
    private IMap<String, HazelcastSessionData> sessions;

    private HazelcastInstance instance;

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
    @Override
    public boolean idInUse(String sessionId) {
        LOG.debug("HazelcastSessionIdManager:idInUse:sessionId= " + sessionId);
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
    @Override
    public void addSession(HttpSession session) {

        LOG.debug("HazelcastSessionIdManager:addSession:" + session.getId());

        synchronized (sessionsIds) {
            sessionsIds.add(session.getId());
        }

    }

    /**
     * Remove session from known local ids
     * @param session
     */
    @Override
    public void removeSession(HttpSession session) {
        synchronized (sessionsIds) {
            sessionsIds.remove(session.getId());
        }
    }

    @Override
    public void invalidateAll(String sessionId) {

        synchronized (sessionsIds) {
            sessionsIds.remove(sessionId);

            //tell all contexts that may have a session object with this id to
            //get rid of them
            Handler[] contexts = server.getChildHandlersByClass(ContextHandler.class);
            for (int i = 0; contexts != null && i < contexts.length; i++) {
                SessionHandler sessionHandler = ((ContextHandler) contexts[i]).getChildHandlerByClass(SessionHandler.class);
                if (sessionHandler != null) {
                    SessionManager manager = sessionHandler.getSessionManager();
                    if (manager != null && manager instanceof HazelcastSessionManager) {
                        ((HazelcastSessionManager) manager).invalidateSession(sessionId);
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
    @Override
    public String getNodeId(String clusterId, HttpServletRequest request) {
        // used in Ajp13Parser
        String worker = request == null ? null : (String) request.getAttribute("org.eclipse.jetty.ajp.JVMRoute");
        if (worker != null) {
            return clusterId + '.' + worker;
        }
        if (_workerName != null) {
            return clusterId + '.' + _workerName;
        }

        return clusterId;
    }

    /** Get the session ID without any worker ID.
     *
     * @param nodeId the node id
     * @return sessionId without any worker ID.
     */
    @Override
    public String getClusterId(String nodeId) {
        int dot = nodeId.lastIndexOf('.');
        return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
    }

    /**
     * Create a hazelcast client/server instance depending on the clientOnly property
     */
    private HazelcastInstance createHazelcastInstance() {

        HazelcastInstance instance;
        if (clientOnly) {
            instance = JettySessionUtils.createHazelcastClientInstance(getConfigLocation());
        } else {
            instance = JettySessionUtils.createHazelcastFullInstance(getConfigLocation());
        }
        return instance;
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
    protected void cleanUp() {
        for (Map.Entry<String, HazelcastSessionData> entry : sessions.entrySet()) {
            if (entry.getValue().getAccessed() < System.currentTimeMillis() - cleanUpInvalidAge) {
                sessions.remove(entry.getKey());
            }
        }

    }

    @Override
    protected void doStart() throws Exception {

        if (instance == null) {
            instance = createHazelcastInstance();
        }
        sessions = initializeSessionMap();

        synchronized (this) {
            if (cleanUp) {
                cleanUpTimer = new Timer("HazelcastSessionCleaner", true);
                if (cleanUpTask != null) {
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
        synchronized (this) {
            if (cleanUpTimer != null) {
                cleanUpTimer.cancel();
                cleanUpTimer = null;
            }
            if (clientOnly && instance != null) {
                instance.shutdown();
                instance = null;
            }
        }
        super.doStop();
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public void setClientOnly(boolean clientOnly) {
        this.clientOnly = clientOnly;
    }

    public IMap<String, HazelcastSessionData> getSessions() {
        return sessions;
    }

    public void setCleanUp(boolean cleanUp) {
        this.cleanUp = cleanUp;
    }

    public void setCleanUpInvalidAge(long cleanUpInvalidAge) {
        this.cleanUpInvalidAge = cleanUpInvalidAge;
    }

    public void setCleanUpPeriod(long cleanUpPeriod) {
        this.cleanUpPeriod = cleanUpPeriod;
    }
}
