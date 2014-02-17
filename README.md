Sample AIO-based Server forwarding JMS messages received from various topics to connected clients
=================================================================================================
(NOTE: JDK 7 and Maven is required)

1. Run the embedded JMS broker and Subscriber Server:
mvn package exec:java

2. Connect to the Subcriber Server:
telnet localhost 9999

3. Register new subscriber with a topic by entering the topic name, e.g. topic1

4. Start JConsole and navigate to MBean created for the topic1 under org.apache.activemq->localhost->Topic->topic1

5. Invoke operation sendTextMessage(String) and check the telnet session

6. Telnet session can be closed by entering character q

(Repeat steps 2-6 as many times you wish)
