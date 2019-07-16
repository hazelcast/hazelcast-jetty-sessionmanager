package com.hazelcast.session.sticky;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.session.AbstractHazelcastSessionsTest;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public abstract class AbstractStickySessionsTest extends AbstractHazelcastSessionsTest {

    @Test
    public void testContextReloadSticky() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        executeRequest("write", SERVER_PORT_1, cookieStore);
        System.out.println("reloading");
        instance1.reload();
        System.out.println("reloaded");
        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value", value);
    }

    @Test
    public void testReadWriteRead() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("null", value);

        executeRequest("write", SERVER_PORT_1, cookieStore);

        value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value", value);
    }

    @Test(timeout = 80000)
    public void testAttributeDistribution() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        executeRequest("write", SERVER_PORT_1, cookieStore);

        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value", value);
    }

    @Test(timeout = 80000)
    public void testAttributeRemoval() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        executeRequest("write", SERVER_PORT_1, cookieStore);

        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value", value);

        value = executeRequest("remove", SERVER_PORT_1, cookieStore);
        assertEquals("true", value);

        value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("null", value);
    }

    @Test(timeout = 80000)
    public void testAttributeUpdate() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        executeRequest("write", SERVER_PORT_1, cookieStore);

        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value", value);

        value = executeRequest("update", SERVER_PORT_1, cookieStore);
        assertEquals("true", value);

        value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value-updated", value);
    }

    @Test(timeout = 80000)
    public void testAttributeInvalidate() throws Exception {

        CookieStore cookieStore = new BasicCookieStore();
        executeRequest("write", SERVER_PORT_1, cookieStore);

        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
        assertEquals("value", value);

        value = executeRequest("invalidate", SERVER_PORT_1, cookieStore);
        assertEquals("true", value);

        HazelcastInstance instance = createHazelcastInstance();
        IMap<Object, Object> map = instance.getMap("default");
        assertEquals(0, map.size());
    }

//    @Test
//    public void testSessionExpire() throws Exception {
//
//        int DEFAULT_SESSION_TIMEOUT = 10;
//        CookieStore cookieStore = new BasicCookieStore();
//        executeRequest("write", SERVER_PORT_1, cookieStore);
//        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
//        assertEquals("value", value);
//
//        sleepSeconds(DEFAULT_SESSION_TIMEOUT + instance1.getManager().getProcessExpiresFrequency());
//
//        value = executeRequest("read", SERVER_PORT_1, cookieStore);
//        assertEquals("null", value);
//    }

    @Test(timeout = 80000)
    public void testAttributeNames() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        executeRequest("read", SERVER_PORT_1, cookieStore);

        String commaSeparatedAttributeNames = executeRequest("names", SERVER_PORT_1, cookieStore);

        //no name should be created
        assertEquals("", commaSeparatedAttributeNames);

        executeRequest("write", SERVER_PORT_1, cookieStore);

        commaSeparatedAttributeNames = executeRequest("names", SERVER_PORT_1, cookieStore);
        assertEquals("key", commaSeparatedAttributeNames);
    }

    @Test(timeout = 80000)
    public void test_isNew() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();

        assertEquals("true", executeRequest("isNew", SERVER_PORT_1, cookieStore));
        assertEquals("false", executeRequest("isNew", SERVER_PORT_1, cookieStore));
    }

    @Test(timeout = 80000)
    public void test_LastAccessTime() throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        String lastAccessTime1 = executeRequest("lastAccessTime", SERVER_PORT_1, cookieStore);
        executeRequest("lastAccessTime", SERVER_PORT_1, cookieStore);
        String lastAccessTime2 = executeRequest("lastAccessTime", SERVER_PORT_1, cookieStore);

        assertNotEquals(lastAccessTime1, lastAccessTime2);
    }

//    @Test
//    public void testCleanupAfterSessionExpire() throws Exception {
//        int DEFAULT_SESSION_TIMEOUT = 10;
//        CookieStore cookieStore = new BasicCookieStore();
//        executeRequest("write", SERVER_PORT_1, cookieStore);
//        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
//        assertEquals("value", value);
//
//        sleepSeconds(DEFAULT_SESSION_TIMEOUT+instance1.getManager().getProcessExpiresFrequency());
//
//        HazelcastInstance instance = Hazelcast.newHazelcastInstance();
//        IMap<Object, Object> map = instance.getMap("default");
//        assertEquals(0, map.size());
//    }
}
