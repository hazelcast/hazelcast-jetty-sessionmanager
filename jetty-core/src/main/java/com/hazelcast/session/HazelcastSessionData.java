/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Session data to be distributed by Hazelcast.
 *
 * Since org.eclipse.jetty.server.session.AbstractSession does not provide access to
 * its private fields(like <code>created</code>, <code>accessed</code>), required fields to create a
 * org.eclipse.jetty.server.session.AbstractSession is added to this class instead of extending it.
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
