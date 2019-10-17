**!!!Jetty Session Persistence Support via Hazelcast is maintained by Jetty Project. !!!***

**Please refer to <a href="https://www.eclipse.org/jetty/documentation/9.4.21.v20190926/configuring-sessions-hazelcast.html" target="_blank">Jetty Documentation</a> and <a href="https://github.com/eclipse/jetty.project/tree/jetty-9.4.x/jetty-hazelcast" target="_blank">Github</a>**


# Table of Contents

* [Jetty Based Web Session Replication](#jetty-based-web-session-replication)
* [Features and Requirements](#features-and-requirements)
* [How Jetty Session Replication Works](#how-jetty-session-replication-works)
  * [Deploying P2P for Jetty](#deploying-p2p-for-jetty)
  * [Deploying Client/Server for Jetty](#deploying-client-server-for-jetty)
* [Configuring HazelcastSessionIdManager for Jetty](#configuring-hazelcastsessionidmanager-for-jetty)
* [Configuring HazelcastSessionManager for Jetty](#configuring-hazelcastsessionmanager-for-jetty)
* [Setting Session Expiration](#setting-session-expiration)
* [Sticky Sessions and Jetty](#sticky-sessions-and-jetty)


# Jetty Based Web Session Replication

***Sample Code:*** *Please see our <a href="https://github.com/hazelcast/hazelcast-code-samples/tree/master/hazelcast-integration/manager-based-session-replication" target="_blank">sample application</a> for Jetty Based Web Session Replication.*


# Features and Requirements

<a href="https://github.com/hazelcast/hazelcast-jetty-sessionmanager" target="_blank">Hazelcast Jetty Session Manager</a> is a container specific module that enables session replication for JEE Web Applications without requiring changes to the application.

***Features***

- Jetty 7 & 8 & 9 support
- Support for sticky and non-sticky sessions
- Jetty failover
- Deferred write for performance boost
- Client/Server and P2P modes
- Declarative and programmatic configuration

***Supported Containers***

Jetty Web Session Replication Module has been tested against the following containers.

- Jetty 7, 8 and 9  - They can be downloaded from <a href="http://download.eclipse.org/jetty/" target="_blank">here</a>.

Latest tested versions are **7.6.16.v20140903**, **8.1.16.v20140903** and **9.2.3.v20140905**


***Requirements***

 - Jetty instance must be running with Java 1.6 or higher.
 - Session objects that need to be clustered have to be Serializable.
 - Hazelcast Jetty-based Web Session Replication is built on top of the `jetty-nosql` module. This module (`jetty-nosql-<*jettyversion*>.jar`) needs to be added to `$JETTY_HOME/lib/ext`.
   This module can be found <a href="http://mvnrepository.com/artifact/org.eclipse.jetty/jetty-nosql" target="_blank">here</a>.

# How Jetty Session Replication Works

Hazelcast Jetty Session Manager is a Hazelcast Module where each created `HttpSession` Object's state is kept in Hazelcast Distributed Map. 

Since the session data are in Hazelcast Distributed Map, you can use all the available features offered by Hazelcast Distributed Map implementation, such as MapStore and WAN Replication.

Jetty Web Session Replication runs in two different modes:

- **P2P**: all Jetty instances launch its own Hazelcast Instance and join to the Hazelcast Cluster and,
- **Client/Server**: all Jetty instances put/retrieve the session data to/from an existing Hazelcast Cluster.


## Deploying P2P for Jetty

P2P deployment launches embedded Hazelcast member in each server instance.

This type of deployment is simple: just configure your Jetty and launch. There is no need for an external Hazelcast cluster.

The following steps configure a sample P2P for Hazelcast Session Replication.

1. Go to <a href="http://www.hazelcast.org/" target="_blank">hazelcast.org</a> and download the latest Hazelcast.
2. Unzip the Hazelcast zip file into the folder `$HAZELCAST_ROOT`.
3. Put `hazelcast.xml` in the folder `$JETTY_HOME/etc`.
4. Go to <a href="https://github.com/hazelcast/hazelcast-jetty-sessionmanager/releases" target="_blank">hazelcast-jetty-sessionmanager</a> repository and download the latest for your Jetty version.
5. Put `$HAZELCAST_ROOT/lib/hazelcast-all-`<*version*>`.jar`  and `hazelcast-jetty`<*jettyversion*>`-sessionmanager-`<*version*>`.jar`in the folder `$JETTY_HOME/lib/ext`.
6. Configure the Session ID Manager. You need to configure a `com.hazelcast.session.HazelcastSessionIdManager` instance in `jetty.xml`. Add the following lines to your `jetty.xml`.

 ```xml
        <Set name="sessionIdManager">
            <New id="hazelcastIdMgr" class="com.hazelcast.session.HazelcastSessionIdManager">
                <Arg><Ref id="Server"/></Arg>
                <Set name="configLocation">etc/hazelcast.xml</Set>
            </New>
        </Set>
 ```

7. Configure the Session Manager. You can configure `HazelcastSessionManager` from a `context.xml` file. Each application has a context file in the `$CATALINA_HOME$/contexts` folder. You need to create this context file if it does not exist. The context filename must be the same as the application name, e.g. `example.war` should have a context file named `example.xml`. The file `context.xml` should have the following content.

 ```xml
        <Ref name="Server" id="Server">
            <Call id="hazelcastIdMgr" name="getSessionIdManager"/>
        </Ref>
        <Set name="sessionHandler">
            <New class="org.eclipse.jetty.server.session.SessionHandler">
                <Arg>
                    <New id="hazelcastMgr" class="com.hazelcast.session.HazelcastSessionManager">
                        <Set name="idManager">
                            <Ref id="hazelcastIdMgr"/>
                        </Set>
                    </New>
                </Arg>
            </New>
        </Set>
 ```

8. Start Jetty instances with a configured load balancer and deploy the web application.

![image](images/NoteSmall.jpg) ***NOTE:*** *In Jetty 9, there is no folder with the name *`contexts`*. You have to put the file *`context.xml`* under the *`webapps`* directory. And you need to add the following lines to *`context.xml`*.*:

 ```xml
        <Ref name="Server" id="Server">
            <Call id="hazelcastIdMgr" name="getSessionIdManager"/>
        </Ref>
        <Set name="sessionHandler">
            <New class="org.eclipse.jetty.server.session.SessionHandler">
                <Arg>
                    <New id="hazelcastMgr" class="com.hazelcast.session.HazelcastSessionManager">
                        <Set name="sessionIdManager">
                            <Ref id="hazelcastIdMgr"/>
                        </Set>
                    </New>
                </Arg>
            </New>
        </Set>
 ```


## Deploying Client/Server for Jetty

In client/server deployment type, Jetty instances work as clients to an existing Hazelcast Cluster.

-	Existing Hazelcast cluster is used as the Session Replication Cluster.
-	The architecture is completely independent. Complete reboot of Jetty instances without losing data.
<br></br>

The following steps configure a sample Client/Server for Hazelcast Session Replication.

1. Go to <a href="http://www.hazelcast.org/" target="_blank">hazelcast.org</a> and download the latest Hazelcast.
2. Unzip the Hazelcast zip file into the folder `$HAZELCAST_ROOT`.
3. Put `hazelcast.xml` in the folder `$JETTY_HOME/etc`.
4. Go to <a href="https://github.com/hazelcast/hazelcast-jetty-sessionmanager/releases" target="_blank">hazelcast-jetty-sessionmanager</a> repository and download the latest for your Jetty version.
5. Put `$HAZELCAST_ROOT/lib/hazelcast-all-`<*version*>`.jar`  and `hazelcast-jetty`<*jettyversion*>`-sessionmanager-`<*version*>`.jar`in the folder `$JETTY_HOME/lib/ext`.
6. Configure the Session ID Manager. You need to configure a `com.hazelcast.session.HazelcastSessionIdManager` instance in `jetty.xml`. Add the following lines to your `jetty.xml`.

 ```xml
        <Set name="sessionIdManager">
            <New id="hazelcastIdMgr" class="com.hazelcast.session.HazelcastSessionIdManager">
                <Arg><Ref id="Server"/></Arg>
                <Set name="configLocation">etc/hazelcast.xml</Set>
                <Set name="clientOnly">true</Set>
            </New>
        </Set>
 ```

7. Configure the Session Manager. You can configure `HazelcastSessionManager` from a `context.xml` file. Each application has a context file under the `$CATALINA_HOME$/contexts` folder. You need to create this context file if it does not exist. The context filename must be the same as the application name, e.g. `example.war` should have a context file named `example.xml`.

 ```xml
            <Ref name="Server" id="Server">
                <Call id="hazelcastIdMgr" name="getSessionIdManager"/>
            </Ref>    
            <Set name="sessionHandler">
                <New class="org.eclipse.jetty.server.session.SessionHandler">
                    <Arg>
                        <New id="hazelMgr" class="com.hazelcast.session.HazelcastSessionManager">
                            <Set name="idManager">
                                <Ref id="hazelcastIdMgr"/>
                            </Set>
                        </New>
                    </Arg>
                </New>
            </Set>
 ```

![image](images/NoteSmall.jpg) ***NOTE:*** *In Jetty 9, there is no folder with name *`contexts`*. You have to put the file *`context.xml`* file under *`webapps`* directory. And you need to add below lines to *`context.xml`*.*

 ```xml
            <Ref name="Server" id="Server">
                <Call id="hazelcastIdMgr" name="getSessionIdManager"/>
            </Ref>    
            <Set name="sessionHandler">
                <New class="org.eclipse.jetty.server.session.SessionHandler">
                    <Arg>
                        <New id="hazelMgr" class="com.hazelcast.session.HazelcastSessionManager">
                            <Set name="sessionIdManager">
                                <Ref id="hazelcastIdMgr"/>
                            </Set>
                        </New>
                    </Arg>
                </New>
            </Set>
 ```

8. Launch a Hazelcast Instance using `$HAZELCAST_ROOT/bin/server.sh` or `$HAZELCAST_ROOT/bin/server.bat`.

9. Start Jetty instances with a configured load balancer and deploy the web application.



# Configuring HazelcastSessionIdManager for Jetty

`HazelcastSessionIdManager` is used both in P2P and Client/Server mode. Use the following parameters to configure the Jetty Session Replication Module to better serve your needs.

- `workerName`: Set this attribute to a unique value for each Jetty instance to enable session affinity with a sticky-session configured load balancer.
- `cleanUpPeriod`: Defines the working period of session clean-up task in milliseconds.
- `configLocation`: specifies the location of `hazelcast.xml`.



# Configuring HazelcastSessionManager for Jetty

`HazelcastSessionManager` is used both in P2P and Client/Server mode. Use the following parameters to configure Jetty Session Replication Module to better serve your needs.

- `savePeriod`: Sets the interval of saving session data to the Hazelcast cluster. Jetty Web Session Replication Module has its own nature of caching. Attribute changes during the HTTP Request/HTTP Response cycle are cached by default. Distributing those changes to the Hazelcast Cluster is costly, so Session Replication is only done at the end of each request for updated and deleted attributes. The risk with this approach is losing data if a Jetty crash happens in the middle of the HTTP Request operation.
You can change that behavior by setting the `savePeriod` attribute.

Notes:

- If `savePeriod` is set to **-2**, `HazelcastSessionManager.save` method is called for every `doPutOrRemove` operation.
- If it is set to **-1**, the same method is never called if Jetty is not shut down.
- If it is set to **0** (the default value), the same method is called at the end of request.
- If it is set to **1**, the same method is called at the end of request if session is dirty.


# Setting Session Expiration

Based on Jetty configuration or `sessionTimeout` setting in `web.xml`, the sessions are expired over time. This requires a cleanup on Hazelcast Cluster, since there is no need to keep expired sessions in it. 

`cleanUpPeriod`, which is defined in `HazelcastSessionIdManager`, is the only setting that controls the behavior of session expiry policy in Jetty Web Session Replication Module. By setting this, you can set the frequency of the session expiration checks in the Jetty Instance.


# Sticky Sessions and Jetty

`HazelcastSessionIdManager` can work in sticky and non-sticky setups.

The clustered session mechanism works in conjunction with a load balancer that supports stickiness. Stickiness can be based on various data items, such as source IP address, or characteristics of the session ID, or a load-balancer specific mechanism. 
For those load balancers that examine the session ID, `HazelcastSessionIdManager` appends a member ID to the session ID, which can be used for routing.
You must configure the `HazelcastSessionIdManager` with a `workerName` that is unique across the cluster. 
Typically the name relates to the physical member on which the instance is executed. If this name is not unique, your load balancer might fail to distribute your sessions correctly.
If sticky sessions are enabled, the `workerName` parameter has to be set, as shown below.


```xml
<Set name="sessionIdManager">
    <New id="hazelcastIdMgr" class="com.hazelcast.session.HazelcastSessionIdManager">
        <Arg><Ref id="Server"/></Arg>
        <Set name="configLocation">etc/hazelcast.xml</Set>
        <Set name="workerName">unique-worker-1</Set>
    </New>
</Set>
```
