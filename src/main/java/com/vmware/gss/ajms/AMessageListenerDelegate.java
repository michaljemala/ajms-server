package com.vmware.gss.ajms;

import java.util.LinkedList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class AMessageListenerDelegate implements MessageListener {

	private final DefaultMessageListenerContainer container;
	private final List<ASubscriber> subscribers;

	public AMessageListenerDelegate(DefaultMessageListenerContainer container) {
		this.container = container;
		this.subscribers = new LinkedList<>();
	}

	public void addSubscriber(ASubscriber subscriber) {
		synchronized (subscribers) {
			this.subscribers.add(subscriber);
			if(this.subscribers.size() == 1)
				this.container.start();
		}
	}

	public void removeSubscriber(ASubscriber subscriber) {
		synchronized (subscribers) {
			this.subscribers.remove(subscriber);
			if(this.subscribers.size() == 0)
				this.container.stop();
		}
	}

	@Override
	public void onMessage(Message msg) {
		for (ASubscriber subscriber : subscribers)
			subscriber.onMessage(msg);
	}

}
