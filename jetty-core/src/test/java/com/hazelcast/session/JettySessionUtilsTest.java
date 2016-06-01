package com.hazelcast.session;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.SlowTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.session.JettySessionUtils.createHazelcastClientInstance;
import static com.hazelcast.session.JettySessionUtils.createHazelcastFullInstance;
import static org.junit.Assert.assertNotNull;

@RunWith(HazelcastSerialClassRunner.class)
@Category(SlowTest.class)
public class JettySessionUtilsTest extends HazelcastTestSupport {

    @After
    public void tearDown() throws Exception {
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }

    @Test
    public void testConstructor() throws Exception {
        assertUtilityConstructor(JettySessionUtils.class);
    }

    @Test
    public void testCreateHazelcastClientInstance() {
        Hazelcast.newHazelcastInstance();
        HazelcastInstance instance = createHazelcastClientInstance();
        assertNotNull(instance);
    }

    @Test
    public void testCreateHazelcastClientInstance_configurationLocationIsNull() {
        Hazelcast.newHazelcastInstance();
        HazelcastInstance instance = createHazelcastClientInstance(null);
        assertNotNull(instance);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateHazelcastClientInstance_configurationNotFound() {
        createHazelcastClientInstance("http://example.com/notFound.xml");
    }

    @Test
    public void testCreateHazelcastFullInstance() {
        HazelcastInstance instance = createHazelcastFullInstance();
        assertNotNull(instance);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateHazelcastFullInstance_configurationNotFound() {
        createHazelcastFullInstance("http://example.com/notFound.xml");
    }
}
