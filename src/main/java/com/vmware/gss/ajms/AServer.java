package com.vmware.gss.ajms;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Executors;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

public class AServer {
	
	private static final String JMSBOKER_URL = "tcp://localhost:61616";
	private static final int SERVER_PORT = 9999;

	public static void main(String[] args) throws Exception {
		BrokerService broker = new BrokerService();
		broker.addConnector(JMSBOKER_URL);
		broker.start();
		
		ActiveMQConnectionFactory coonectionFactory = 
				new ActiveMQConnectionFactory(JMSBOKER_URL);
		
		final AListenerContainerManager containerManager = 
				new AListenerContainerManager(coonectionFactory);
		
		AsynchronousChannelGroup channelGroup = 
				AsynchronousChannelGroup.withFixedThreadPool(
						Runtime.getRuntime().availableProcessors(),
						Executors.defaultThreadFactory());
		
		AsynchronousServerSocketChannel listener = 
				AsynchronousServerSocketChannel
					.open(channelGroup)
					.bind(new InetSocketAddress(SERVER_PORT));
		
		System.out.printf("Server listening on port [%d]...\n\n", SERVER_PORT);
		
		while(true) {
			AsynchronousSocketChannel clientChannel = listener.accept().get();
			System.out.printf("Client connected from [%s]\n", clientChannel.getRemoteAddress());
			
			final ASubscriber subscriber = new ASubscriber(clientChannel, containerManager);
			subscriber.readTopic(new ACompletionHandler<String, Void>() {
				@Override
				public void completed(String topic, Void attachment) {
					System.out.printf("Client requested to subscribe to [%s]\n", topic);
					containerManager.registerSubsciberWithListenerContainer(subscriber, topic);
				}
			});
		}
	}

}
