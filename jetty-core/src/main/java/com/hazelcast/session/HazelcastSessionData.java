package com.hazelcast.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * Session data to be distributed by Hazelcast.
 *
 * Since org.eclipse.jetty.server.session.AbstractSession does not provide access to
 * its private fields(like <code>created</code>, <code>accessed</code>), required fields to create a
 * org.eclipse.jetty.server.session.AbstractSession is added to this class instead of extending it.
 *
 */
public class HazelcastSessionData implements Serializable {

    private boolean valid;

    /**
     * Session creation time in ms
     */
    private long creationTime;
    private Object version;

    /**
     * Last access time in ms
     */
    private long accessed;

    private Map<String, Object> attributeMap = new HashMap<String, Object>();


    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public Object getVersion() {
        return version;
    }

    public void setVersion(Object version) {
        this.version = version;
    }

    public long getAccessed() {
        return accessed;
    }

    public void setAccessed(long accessed) {
        this.accessed = accessed;
    }

    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(Map<String, Object> attributeMap) {
        this.attributeMap = attributeMap;
    }
}
