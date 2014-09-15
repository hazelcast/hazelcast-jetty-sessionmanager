/*
 *
 *  * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.hazelcast.session;

import org.eclipse.jetty.server.session.AbstractSessionIdManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class HazelcastSessionIdManager extends AbstractSessionIdManager{

    @Override
    public boolean idInUse(String s) {
        System.out.println("com.hazelcast.session.HazelcastSessionIdManager.idInUse");
        System.out.println("s = " + s);
        return false;
    }

    @Override
    public void addSession(HttpSession httpSession) {
        System.out.println("com.hazelcast.session.HazelcastSessionIdManager.addSession");
        System.out.println("httpSession = " + httpSession);

    }

    @Override
    public void removeSession(HttpSession httpSession) {
        System.out.println("com.hazelcast.session.HazelcastSessionIdManager.removeSession");
        System.out.println("httpSession = " + httpSession);

    }

    @Override
    public void invalidateAll(String s) {

    }

    @Override
    public String getClusterId(String s) {
        System.out.println("com.hazelcast.session.HazelcastSessionIdManager.getClusterId");
        System.out.println("s = " + s);
        return null;
    }

    @Override
    public String getNodeId(String s, HttpServletRequest httpServletRequest) {
        System.out.println("com.hazelcast.session.HazelcastSessionIdManager.getNodeId");
        System.out.println("s = " + s);
        return null;
    }

//    @Override
//    public boolean idInUse(String s) {
//        return false;
//    }
//
//    @Override
//    public void addSession(HttpSession httpSession) {
//
//    }
//
//    @Override
//    public void removeSession(HttpSession httpSession) {
//
//    }
//
//    @Override
//    public void invalidateAll(String s) {
//
//    }
//
//    @Override
//    public void renewSessionId(String s, String s2, HttpServletRequest httpServletRequest) {
//
//    }
}
