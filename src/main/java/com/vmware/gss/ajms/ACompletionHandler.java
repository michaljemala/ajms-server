package com.vmware.gss.ajms;

import java.nio.channels.CompletionHandler;

public class ACompletionHandler<V,A> implements CompletionHandler<V,A> {
	@Override
	public void completed(V result, A attachment) {
	}

	@Override
	public void failed(Throwable exc, A attachment) {
		exc.printStackTrace();
	}
}