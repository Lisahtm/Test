package cn.buptsse.test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * (临时的) Rabbitmq 工具类
 * 
 * 用法： 1. 在 login 之后，用己方 sip 地址做参数，构造 RabbitmqUtil.MqWatcher
 * 对象，补完其onJitsiCheckRequest方法和onJitsiOkResponce方法，然后用其构造线程并 start(). ex: new
 * Thread(new RabbitmqUtil.MqWatcher("me@192.168.1.104"){
 * 
 * public void onJitsiCheckRequest(String requesterId){}
 * 
 * public void onJitsiOkResponce(String responcerId){}
 * 
 * }).start();
 * 
 * 2. 呼叫前调用 RabbitmqUtil.sendJitsiCheckRequest
 * 
 * 3. 确保己方jitsi正常后调用 RabbitmqUtil.sendJitsiOkResponse
 * 
 * @author lee
 * @version 201309301644 *
 */
public class RabbitmqUtil {

	private final static String MQ_HOST = "192.168.1.104";
	private static final String EXCHANGE_NAME = "Iqq_Test_Exchange";

	public static void MqSend(String tag, String senderId, String receiverId) {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(MQ_HOST);
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();
			channel.exchangeDeclare(EXCHANGE_NAME, "direct");// routing 模式

			String routingKey = "route:" + receiverId;
			String messageToSend = tag + ";" + senderId + ";" + receiverId;
			channel.basicPublish(EXCHANGE_NAME, routingKey, null,
					messageToSend.getBytes());
			System.out.println(" [MQ] Sent '" + routingKey + "':'"
					+ messageToSend + "'");

			channel.close();
			connection.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * 呼叫前调用
	 * */
	public static void sendJitsiCheckRequest(String senderId, String receiverId) {
		MqSend("checkjitsi", senderId, receiverId);
	}

	/**
	 * 确保己方 jitsi 正常后调用
	 * */
	public static void sendJitsiOkResponse(String senderId, String receiverId) {
		MqSend("jitsiok", senderId, receiverId);
	}

	// 接收 rabbitMQ 消息的 Runnable
	public static abstract class MqWatcher implements Runnable {
		private ConnectionFactory factory;
		private Connection connection;
		private Channel channel;
		private String queueName;
		private String routingKey;
		private QueueingConsumer consumer;

		public MqWatcher(String id) {
			queueName = "queue:" + id;// 用自己的用户名构造唯一的Queue
			routingKey = "route:" + id;// 路由规则:
			// 只接受“路由键==自己的用户名构造的路由键”的消息
		}

		@Override
		public void run() {
			try {
				factory = new ConnectionFactory();
				factory.setHost(MQ_HOST);
				connection = factory.newConnection();
				channel = connection.createChannel();
				channel.exchangeDeclare(EXCHANGE_NAME, "direct");// 声明Exchange
				channel.queueDeclare(queueName, false, false, false, null);
				channel.queueBind(queueName, EXCHANGE_NAME, routingKey);// 把Queue、Exchange及路由绑定

				System.out.println(" [MQ] Queue" + queueName
						+ "Waiting for messages.");

				consumer = new QueueingConsumer(channel);
				channel.basicConsume(queueName, true, consumer);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			while (true) {
				try {
					QueueingConsumer.Delivery delivery = consumer
							.nextDelivery();
					String message = new String(delivery.getBody());
					// String routingKeyGotFromDelivery = delivery.getEnvelope()
					// .getRoutingKey();
					System.out.println(" [MQ] Received '" + "':'" + message
							+ "'");

					String[] sp = message.split(";");
					onMessage(sp[0], sp[1], sp[2]);

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		public void onMessage(String tag, String senderId, String receiverId) {
			if (tag.equals("checkjitsi")) {
				onJitsiCheckRequest(senderId, receiverId);
			} else if (tag.equals("jitsiok")) {
				onJitsiOkResponse(receiverId, senderId);
			} else {
				System.err.println("[MQ] Receive message with unknown tag '"
						+ tag + "'.");
			}
		}

		/**
		 * 触发条件：接收到对方IQQ的“检查jitsi状态，确保开启”的请求。
		 * 
		 * @param requestSenderId
		 *            : 对方sip id
		 * */
		public abstract void onJitsiCheckRequest(String requesterId,
				String responserId);

		/**
		 * 触发条件：接收到对方IQQ的“jitsi状态已经正常”的回应。
		 * */
		public abstract void onJitsiOkResponse(String requesterId,
				String responserId);
	}
}
