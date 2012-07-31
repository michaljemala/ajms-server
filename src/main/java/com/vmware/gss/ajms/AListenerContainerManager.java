package com.vmware.gss.ajms;

import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class AListenerContainerManager {

	private final ConnectionFactory targetConnectionFactory;
	private final HashMap<String, DefaultMessageListenerContainer> containers;

	public AListenerContainerManager(ConnectionFactory connectionFactory) {
		this.targetConnectionFactory = connectionFactory;
		this.containers = new LinkedHashMap<>();
	}

	protected DefaultMessageListenerContainer getListenerContainer(String topic) {
		DefaultMessageListenerContainer container;
		synchronized (containers) {
			container = containers.get(topic);
			if (container == null) {
				container = createListenerContainer(topic);
				this.containers.put(topic, container);
			}
		}

		return container;
	}

	protected DefaultMessageListenerContainer createListenerContainer(String topic) {
		DefaultMessageListenerContainer container;
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
				this.targetConnectionFactory);
		connectionFactory.setReconnectOnException(true);
		connectionFactory.setExceptionListener(new ExceptionListener() {
			public void onException(JMSException e) {
				e.printStackTrace();
			}
		});
		connectionFactory.afterPropertiesSet();

		container = new DefaultMessageListenerContainer();
		String threadNamePrefix = "DMLC[" + topic + "]-";
		container.setTaskExecutor(new SimpleAsyncTaskExecutor(threadNamePrefix));
		container.setConnectionFactory(connectionFactory);
		container.setDestinationName(topic);
		container.setMessageListener(new AMessageListenerDelegate(container));
		container.setPubSubDomain(true);
		container.afterPropertiesSet();
		return container;
	}

	public void registerSubsciberWithListenerContainer(ASubscriber subscriber, String topic) {
		AMessageListenerDelegate listenerDelegate = getListenerContainerDelegate(topic);
		listenerDelegate.addSubscriber(subscriber);
	}
	
	public void unregisterSubsciberWithListenerContainer(ASubscriber subscriber, String topic) {
		AMessageListenerDelegate listenerDelegate = getListenerContainerDelegate(topic);
		listenerDelegate.removeSubscriber(subscriber);
	}

	public AMessageListenerDelegate getListenerContainerDelegate(String topic) {
		DefaultMessageListenerContainer listenerContainer = getListenerContainer(topic);
		AMessageListenerDelegate listenerDelegate = (AMessageListenerDelegate) listenerContainer.getMessageListener();
		return listenerDelegate;
	}

	public void destroy() {
		synchronized (containers) {
			for (DefaultMessageListenerContainer container : this.containers.values()) {
				CachingConnectionFactory connectionFactory = (CachingConnectionFactory) container.getConnectionFactory();
				
				if (container != null) {
					container.stop();
					container.destroy();
					container = null;
				}
				System.out.println("Listener container destroyed");
				
				if (connectionFactory != null) {
					connectionFactory.destroy();
					connectionFactory = null;
				}
				System.out.println("Caching conection factory destroyed");
			}
		}
	}

}
