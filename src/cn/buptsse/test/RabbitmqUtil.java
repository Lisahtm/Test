package cn.buptsse.test;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class RabbitmqUtil {

	private final static String MQ_HOST = "192.168.1.104";
	private static final String EXCHANGE_NAME = "Iqq_Test_Exchange";
	

	public static void MqSend(String type, String senderId, String receiverId) {
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(MQ_HOST);
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();
			channel.exchangeDeclare(EXCHANGE_NAME, "direct");// routing 模式

			String routingKey = "route:" + receiverId;
			String messageToSend = type + ";" + senderId + ";" + receiverId;
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

	// 接收 rabbitMQ 消息的线程
	public static class MqWatcher implements Runnable {
		private ConnectionFactory factory;
		private Connection connection;
		private Channel channel;
		private String queueName;
		private String routingKey;
		private QueueingConsumer consumer;

		public MqWatcher(String id) {
			try {
				factory = new ConnectionFactory();
				factory.setHost(MQ_HOST);
				connection = factory.newConnection();
				channel = connection.createChannel();
				channel.exchangeDeclare(EXCHANGE_NAME, "direct");// 声明Exchange

				queueName = "queue:" + id;// 用自己的用户名构造唯一的Queue
				channel.queueDeclare(queueName, false, false, false, null);

				routingKey = "route:" + id;// 路由规则:
											// 只接受“路由键==自己的用户名构造的路由键”的消息
				channel.queueBind(queueName, EXCHANGE_NAME, routingKey);// 把Queue、Exchange及路由绑定

				System.out.println(" [MQ] Queue" + queueName
						+ "Waiting for messages.");

				consumer = new QueueingConsumer(channel);
				channel.basicConsume(queueName, true, consumer);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public void run() {
			while (true) {
				try {
					QueueingConsumer.Delivery delivery = consumer
							.nextDelivery();
					String message = new String(delivery.getBody());
					String routingKeyGotFromDelivery = delivery.getEnvelope()
							.getRoutingKey();

					System.out
							.println(" [MQ] Received '"
									+ routingKeyGotFromDelivery + "':'"
									+ message + "'");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		public void onMessage() {// 检查己方状态；确认良好后，通过 MqSend 方法回发
			// checkStatusAllRight();
			
		}
	}
}
