package com.hazelcast.session;

import com.hazelcast.core.IMap;
import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.NoSqlSessionManager;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import java.util.Map;
import java.util.Set;

/**
 *  Responsible of managing the session lifecycle
 *  There is one session manager per web application
 *
 */
public class HazelcastSessionManager extends NoSqlSessionManager {

    private static final Logger LOG = Log.getLogger(HazelcastSessionManager.class);

    /**
     * the context id is only set when this class has been started
     */
    private String contextId = null;

    /**
     * Distributed session data,
     * for local copies of sessions see {@link org.eclipse.jetty.nosql.NoSqlSessionManager#_sessions}
     *
     */
    private IMap<String, HazelcastSessionData> sessions;


    @Override
    public void doStart() throws Exception {

        LOG.info("HazelcastSessionManager.doStart()");
        super.doStart();
        String[] hosts = getContextHandler().getVirtualHosts();
        if (hosts == null || hosts.length == 0) {
            hosts = getContextHandler().getConnectorNames();
        }
        if (hosts == null || hosts.length == 0) {
            hosts = new String[] {"::"}; // IPv6 equiv of 0.0.0.0
        }

        String contextPath = getContext().getContextPath();
        if (contextPath == null || "".equals(contextPath))
        {
            contextPath = "*";
        }
        LOG.debug("HazelcastSessionManager:doStart():contextpath: " + contextPath);
        contextId = createContextId(hosts,contextPath);
        LOG.debug("HazelcastSessionManager:doStart():contextId: " + contextId);
        sessions= ((HazelcastSessionIdManager) getSessionIdManager()).getSessions();

    }

    @Override
    protected synchronized Object save(NoSqlSession session, Object version, boolean activateAfterSave)
    {
        LOG.info("HazelcastSessionManager:save: " + session);
        try {
            session.willPassivate();

            HazelcastSessionData sessionData = sessions.get(session.getClusterId());
            if (sessionData == null) {
                sessionData = new HazelcastSessionData();
            }

            // handle valid or invalid
            if (session.isValid())
            {
                // handle new or existing
                if (version == null) {
                    // New session
                    version = new Long(1);
                    sessionData.setCreationTime(session.getCreationTime());
                    sessionData.setValid(true);
                    sessionData.setVersion(version);
                } else {
                    version = new Long(((Number)version).longValue() + 1);
                    sessionData.setVersion(version);
                }

                sessionData.setAccessed(session.getAccessed());
                Set<String> names = session.takeDirty();
                if (isSaveAllAttributes()) {
                    names.addAll(session.getNames()); // note dirty may include removed names
                }

                for (String name : names) {
                    Object value = session.getAttribute(name);
                    if (value == null) {
                        sessionData.getAttributeMap().remove(name);
                    } else {
                        sessionData.getAttributeMap().put(name, value);
                    }
                }
            } else {
                sessionData.setValid(false);
             //   sessionData.setInvalidationTime(System.currentTimeMillis());
                //sessionData.getAttributeMap().put(getContextKey(),1);
            }

            sessions.put(session.getClusterId(), sessionData);

            if (activateAfterSave) {
                session.didActivate();
            }

            return version;
        } catch (Exception e) {
            LOG.warn("HazelcastSessionManager:save:exception", e);
        }
        return null;
    }

    @Override
    protected Object refresh(NoSqlSession session, Object version) {

        LOG.info("HazelcastSessionManager:refresh: " + session);

        // check if in memory version is the same as in hazelcast
        if (version != null) {
            HazelcastSessionData o = sessions.get(session.getClusterId());
            if (o != null) {
                Object saved = o.getVersion();

                if (saved != null && saved.equals(version)) {
                    return version;
                }
                version = saved;
            }
        }

        HazelcastSessionData o = sessions.get(session.getClusterId());

        // If it doesn't exist, invalidate
        if (o == null) {
            session.invalidate();
            return null;
        }

        // If it has been flagged invalid, invalidate
        Boolean valid = o.isValid();
        if (valid == null || !valid) {
            session.invalidate();
            return null;
        }

        // We need to update the attributes. We will model this as a passivate,
        // followed by bindings and then activation.
        session.willPassivate();
        try {
            session.clearAttributes();

            Map<String, Object> attrs = o.getAttributeMap();

            if (attrs != null) {
                for (String name : attrs.keySet()) {

                    String attr = name;
                    Object value = attrs.get(name);

                    if (attrs.keySet().contains(name)) {
                        session.doPutOrRemove(attr,value);
                        session.bindValue(attr,value);
                    } else {
                        session.doPutOrRemove(attr,value);
                    }
                }
                // cleanup, remove values from session, that don't exist in data anymore:
                for (String name : session.getNames()) {
                    if (!attrs.keySet().contains(name)) {
                        session.doPutOrRemove(name,null);
                        session.unbindValue(name,session.getAttribute(name));
                    }
                }
            }

            /*
             * We are refreshing so we should update the last accessed time.
             */

            // Form updates
            o.setAccessed(System.currentTimeMillis());

            // apply the update
            sessions.put(session.getClusterId(), o);

            session.didActivate();

            return version;
        }
        catch (Exception e) {
            LOG.warn("HazelcastSessionManager:refresh:exception", e);
        }

        return null;
    }

    /*------------------------------------------------------------ */
    @Override
    protected synchronized NoSqlSession loadSession(String clusterId) {

        LOG.info("HazelcastSessionManager:loadSession: " + clusterId);
        HazelcastSessionData o = sessions.get(clusterId);

        if (o == null) {
            return null;
        }

        Boolean valid = o.isValid();
        if (valid == null || !valid) {
            return null;
        }

        try {
            Object version = o.getVersion();
            Long created = o.getCreationTime();
            Long accessed = o.getAccessed();

            NoSqlSession session = new NoSqlSession(this,created,accessed,clusterId,version);

            // get the attributes for the context
            Map<String, Object> attrs = o.getAttributeMap();

            if (attrs != null) {
                for (String name : attrs.keySet()) {

                    String attr = name;
                    Object value = attrs.get(name);

                    session.doPutOrRemove(attr,value);
                    session.bindValue(attr,value);

                }
            }
            session.didActivate();

            return session;
        }
        catch (Exception e)
        {
            LOG.warn("HazelcastSessionManager:loadSession:exception", e);
        }
        return null;
    }


    @Override
    protected boolean remove(NoSqlSession session) {

        /*
         * Check if the session exists and if it does remove the context
         * associated with this session
         */

        HazelcastSessionData o = sessions.get(session.getClusterId());

        if (o != null) {
            //TODO: ??sessions.remove(session.getClusterId());
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    protected void invalidateSession(String idInCluster) {

        super.invalidateSession(idInCluster);
        
        /*
         * pull back the 'valid' value, we can check if its false, if is we don't need to
         * reset it to false
         */
        HazelcastSessionData o = sessions.get(idInCluster);

        if (o != null && o.isValid()) {
            o.setValid(false);
            sessions.put(idInCluster, o);
        }
    }

    /**
     * returns the total number of session objects in the session store
     *
     * the count() operation itself is optimized to perform on the server side
     * and avoid loading to client side.
     */
    public long getSessionStoreCount()
    {
        return sessions.size();
    }

    private String createContextId(String[] virtualHosts, String contextPath)
    {
        String contextId = virtualHosts[0] + contextPath;

        return contextId;
    }

}
