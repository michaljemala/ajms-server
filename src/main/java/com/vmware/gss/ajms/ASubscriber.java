package com.vmware.gss.ajms;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class ASubscriber implements MessageListener {

	private String topic;
	private final AsynchronousSocketChannel clientChannel;
	private final AListenerContainerManager containerManager;

	public ASubscriber(AsynchronousSocketChannel clientChannel, AListenerContainerManager containerManager) {
		this.clientChannel = clientChannel;
		this.containerManager = containerManager;
	}

	public void readTopic(final CompletionHandler<String, Void> handler) {
		readLine(new ACompletionHandler<String, Void>(){
			@Override
			public void completed(String line, Void attachment) {
				ASubscriber.this.topic = line;
				handler.completed(line, attachment);
				
				// Wait for character 'q' to close connection
				readChar(new ACompletionHandler<Character, Void>() {
					@Override
					public void completed(Character character, Void a) {
						if(character.charValue() == 'q')
							destroy();
						else
							readChar(this);
					}
				});
			}
		});
	}

	@Override
	public void onMessage(Message msg) {
		if (msg instanceof TextMessage)
			sendMessage((TextMessage) msg);
	}
	
	protected void sendMessage(TextMessage msg) {
		try {
			String text = msg.getText();
			if (clientChannel.isOpen())
				clientChannel.write(ByteBuffer.wrap(text.getBytes()));
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
	}
	
	protected void destroy() {
		try {
			containerManager.unregisterSubsciberWithListenerContainer(this, this.topic);
			clientChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Client disconnected");
	}

	private void readLine(CompletionHandler<String, Void> handler) {
		readLine(new StringBuilder(), handler);
	}
	
	private void readLine(final StringBuilder lineBuffer, final CompletionHandler<String, Void> handler) {
		readChar(new ACompletionHandler<Character, Void>() {
			@Override
			public void completed(Character character, Void attachment) {
				if(Character.LINE_SEPARATOR == character) {
					readChar(new ACompletionHandler<Character, Void>(){
						@Override
						public void completed(Character character, Void attachment) {
							if(Character.LETTER_NUMBER == character)
								handler.completed(lineBuffer.toString(), null);
						}
					});
				} else if(Character.LETTER_NUMBER == character) {
					handler.completed(lineBuffer.toString(), null);
				} else {
					readLine(lineBuffer.append(character), handler);
				}
			}
		});
	}
	
	private void readChar(final CompletionHandler<Character, Void> handler) {
		final ByteBuffer buffer = ByteBuffer.allocate(1);
		clientChannel.read(buffer, null, new ACompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void a) {
				if (result < 0) {
					destroy();
				} else {
					buffer.flip();
					handler.completed((char)buffer.get(), null);
				}
			}
		});
	}
	
}
