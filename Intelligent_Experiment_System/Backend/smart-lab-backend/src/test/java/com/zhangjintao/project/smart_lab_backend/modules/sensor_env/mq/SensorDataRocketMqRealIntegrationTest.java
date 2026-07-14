package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.mq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.utils.SensorDataEventNoGenerator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "rocketmq.real.enabled", matches = "true")
class SensorDataRocketMqRealIntegrationTest {

	private static final String TOPIC = "iot-sensor-data";
	private static final String TAG = "lab_sensor_data";

	@Test
	void shouldConsumeSameSensorMessagesInSendOrderWithRealRocketMq() throws Exception {
		String suffix = UUID.randomUUID().toString().replace("-", "");
		String messageKey = "real-rocketmq-" + suffix;
		String shardingKey = "SENSOR-ORDER-" + suffix.substring(0, 8);
		String nameServer = System.getProperty("rocketmq.name-server", "localhost:9876");
		List<String> receivedBodies = new CopyOnWriteArrayList<>();
		CountDownLatch latch = new CountDownLatch(2);
		DefaultMQProducer producer = new DefaultMQProducer("iot-route-real-producer-" + suffix);
		DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("iot-route-real-consumer-" + suffix);

		try {
			consumer.setNamesrvAddr(nameServer);
			consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
			consumer.subscribe(TOPIC, TAG);
			consumer.registerMessageListener((MessageListenerOrderly) (messages, context) -> {
				for (MessageExt message : messages) {
					if (messageKey.equals(message.getKeys())) {
						receivedBodies.add(new String(message.getBody(), StandardCharsets.UTF_8));
						latch.countDown();
					}
				}
				return ConsumeOrderlyStatus.SUCCESS;
			});
			consumer.start();

			producer.setNamesrvAddr(nameServer);
			producer.start();
			ensureTopicReady(producer);
			MessageQueueSelector selector = (queues, message, argument) ->
					queues.get(Math.floorMod(argument.hashCode(), queues.size()));
			String body1 = payload("first", shardingKey);
			String body2 = payload("second", shardingKey);
			assertThat(producer.send(message(messageKey, body1), selector, shardingKey).getSendStatus())
					.isEqualTo(SendStatus.SEND_OK);
			assertThat(producer.send(message(messageKey, body2), selector, shardingKey).getSendStatus())
					.isEqualTo(SendStatus.SEND_OK);

			assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
			assertThat(receivedBodies).containsExactly(body1, body2);
		}
		finally {
			producer.shutdown();
			consumer.shutdown();
		}
	}

	private Message message(String key, String body) {
		return new Message(TOPIC, TAG, key, body.getBytes(StandardCharsets.UTF_8));
	}

	private void ensureTopicReady(DefaultMQProducer producer) throws Exception {
		Exception lastException = null;
		for (int i = 0; i < 10; i++) {
			try {
				producer.createTopic("TBW102", TOPIC, 4, Map.of());
				if (!producer.fetchPublishMessageQueues(TOPIC).isEmpty()) {
					return;
				}
			}
			catch (MQClientException ex) {
				lastException = ex;
			}
			Thread.sleep(1000);
		}
		throw new AssertionError("RocketMQ Topic 路由未就绪：" + TOPIC, lastException);
	}

	private String payload(String seq, String sensorNo) {
		return """
				{"messageId":"%s","eventNo":"%s","sourceSystem":"iot","sensor":{"sensorNo":"%s"}}
				""".formatted(seq, SensorDataEventNoGenerator.generate(), sensorNo).trim();
	}
}
