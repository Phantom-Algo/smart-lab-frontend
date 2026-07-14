package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcherSchedule {

	private final MessageDispatchWorker messageDispatchWorker;

	@Qualifier("messageInboxDispatcherExecutor")
	private final Executor messageInboxDispatcherExecutor;

	private final AtomicBoolean running = new AtomicBoolean(false);

	@Scheduled(fixedDelayString = "${message.inbox.dispatcher.fixed-delay-ms:5000}")
	public void triggerDispatch() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		messageInboxDispatcherExecutor.execute(() -> {
			try {
				messageDispatchWorker.dispatchOnce();
			}
			catch (Exception ex) {
				log.warn("Message dispatcher execution failed", ex);
			}
			finally {
				running.set(false);
			}
		});
	}
}
