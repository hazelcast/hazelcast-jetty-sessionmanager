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

import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.NoSqlSessionManager;

public class HazelcastSessionManager extends NoSqlSessionManager{

    @Override
    protected NoSqlSession loadSession(String s) {
        System.out.println("com.hazelcast.session.HazelcastSessionManager.loadSession");
        System.out.println("s = " + s);
        System.out.println("push test");
        return null;
    }

    @Override
    protected Object save(NoSqlSession noSqlSession, Object o, boolean b) {
        System.out.println("com.hazelcast.session.HazelcastSessionManager.save");
        System.out.println("noSqlSession = " + noSqlSession);
        return null;
    }

    @Override
    protected Object refresh(NoSqlSession noSqlSession, Object o) {
        System.out.println("com.hazelcast.session.HazelcastSessionManager.refresh");
        System.out.println("noSqlSession = " + noSqlSession);
        return null;
    }

    @Override
    protected boolean remove(NoSqlSession noSqlSession) {
        System.out.println("com.hazelcast.session.HazelcastSessionManager.remove");
        System.out.println("noSqlSession = " + noSqlSession);
        return false;
    }

//    @Override
//    protected void update(NoSqlSession noSqlSession, String s, String s2) throws Exception {
//
//    }
}
