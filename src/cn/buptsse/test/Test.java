package cn.buptsse.test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import cn.buptsse.test.RabbitmqUtil;
import cn.buptsse.test.RabbitmqUtil.MqWatcher;

public class Test {
	private final String SIP_HOST = "192.168.1.104";

	private final String JITSI_BUILDXML_FILE_LOCATION_LINUX = "/home/snowonion/workshop/jitsi-src-2.2.4603.9615(changed)/build.xml";
	private final String ANT_LINUX = "ant";
	private final String JITSI_BUILDXML_FILE_LOCATION_WIN = "F:/Data/Yunio/Workshop/JavaWorkspace/jitsi-src-2.2.4603.9615(changed)/build.xml";
	private final String ANT_WIN = "D:/Software/Work/apache-ant-1.9.2/bin/ant.bat";

	private String JITSI_BUILDXML_FILE_LOCATION = null;
	private String ANT = null;

	public boolean isConnected = false;
	public BufferedReader account_br = null;
	public BufferedReader login_br = null;
	public PrintWriter account_pw = null;
	public PrintWriter login_pw = null;

	private JFrame jFrame = null;
	private JPanel jp_loginInfo = null;
	private JLabel jl_username = null;
	private JTextField jtf_username = null;
	private JLabel jl_password = null;
	private JPasswordField jpf_password = null;
	private JPanel jp_callWho = null;
	private JLabel jl_callWho = null;
	private JTextField jtf_callWho = null;
	private JButton jb_jitsilogin = null;
	private JButton jb_call = null;
	private JButton jb_mqcall = null;
	private JButton jb_offline = null;

	private JPanel jp_login = null;
	private JPanel jp_call = null;

	private Process jitsiProcess = null;

	private String currentSipId = null;

	public Test() {

		// 系统的适配
		if (OSValidator.isWindows()) {
			JITSI_BUILDXML_FILE_LOCATION = JITSI_BUILDXML_FILE_LOCATION_WIN;
			ANT = ANT_WIN;
		} else if (OSValidator.isUnix()) {
			JITSI_BUILDXML_FILE_LOCATION = JITSI_BUILDXML_FILE_LOCATION_LINUX;
			ANT = ANT_LINUX;
		} else {
			System.err
					.println("This software will not likely to work on your OS.");
			return;
		}

		jFrame = new JFrame("Test");
		jFrame.setLayout(new BorderLayout());

		jp_loginInfo = new JPanel();
		jp_loginInfo.setLayout(new FlowLayout());

		jl_username = new JLabel("Username: ");
		jp_loginInfo.add(jl_username);

		jtf_username = new JTextField(10);
		jp_loginInfo.add(jtf_username);

		jl_password = new JLabel("Password: ");
		jp_loginInfo.add(jl_password);

		jpf_password = new JPasswordField(10);
		jp_loginInfo.add(jpf_password);

		jFrame.add(jp_loginInfo, BorderLayout.NORTH);

		jp_callWho = new JPanel();
		jp_callWho.setLayout(new FlowLayout());

		jl_callWho = new JLabel("Call: ");
		jp_callWho.add(jl_callWho);

		jtf_callWho = new JTextField(30);
		jp_callWho.add(jtf_callWho);

		jFrame.add(jp_callWho, BorderLayout.SOUTH);

		jb_jitsilogin = new JButton("Jitsi-Login");
		jb_jitsilogin.addActionListener(new ActionListener() {
			// 先用Ant启动Jitsi, Jitsi启动后连上socket, 然后执行原来login的内容
			@Override
			public void actionPerformed(ActionEvent e) {

				// 启动jitsi
				try {
					ProcessBuilder pb = null;

					pb = new ProcessBuilder(ANT, "run", "-f",
							JITSI_BUILDXML_FILE_LOCATION);
					jitsiProcess = pb.start();

					// 接收 ant 输出的线程
					new Thread(new SubprocessOutputListener(jitsiProcess))
							.start();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		jp_login = new JPanel();
		jp_login.add(jb_jitsilogin);
		jFrame.add(jp_login, BorderLayout.WEST);

		jb_call = new JButton("Call");
		jb_call.addActionListener(new ActionListener() {

			// 传被呼叫方sip地址
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isConnected && !jtf_callWho.getText().isEmpty()) {
					account_pw.println("call:" + jtf_callWho.getText());
					account_pw.flush();
					System.out.println("Call " + jtf_callWho.getText()
							+ " command sent.");
					jtf_callWho.setText("");
				} else
					System.out.println("No client connected.");
			}

		});

		jb_mqcall = new JButton("MQ-Call");
		jb_mqcall.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String receiverId = jtf_callWho.getText();
				// 发送check消息，让对方iqq察看jitsi状态
				RabbitmqUtil.sendJitsiCheckRequest(currentSipId, receiverId);
			}

		});

		jp_call = new JPanel();
		jp_call.add(jb_call);
		jp_call.add(jb_mqcall);
		jFrame.add(jp_call, BorderLayout.CENTER);

		jb_offline = new JButton("Offline");
		jb_offline.addActionListener(new ActionListener() {
			// 传 offline 信令
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isConnected) {
					account_pw.println("offline");
					account_pw.flush();
					jtf_username.setText("");
					jpf_password.setText("");
					System.out.println("Offline command sent.");

					// 干掉jitsi
					System.out.println("Try to destroy jitsiProcess.");
					jitsiProcess.destroy();

				} else
					System.out.println("No client connected.");
			}

		});
		jFrame.add(jb_offline, BorderLayout.EAST);

		jFrame.setBounds(100, 100, 400, 150);
		jFrame.setVisible(true);

		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// 监听 login 模块的线程
		// lee: 我不太确定上面这句注释的意思……
		new Thread() {
			@Override
			public void run() {
				ServerSocket server;
				try {
					server = new ServerSocket(12345);
					System.out.println("Listening on 12345...");
					while (true) {
						Socket socket = server.accept();
						System.out.println("account_client connected...");
						isConnected = true;
						account_br = new BufferedReader(new InputStreamReader(
								socket.getInputStream()));
						account_pw = new PrintWriter(socket.getOutputStream());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		// 监听 call 和 offline 模块的线程
		// lee: 我不太确定上面这句注释的意思……
		new Thread() {
			@Override
			public void run() {
				ServerSocket server;
				try {
					server = new ServerSocket(54321);
					System.out.println("Listening on 54321...");
					while (true) {
						Socket socket = server.accept();
						System.out.println("login_client connected...");
						isConnected = true;
						login_br = new BufferedReader(new InputStreamReader(
								socket.getInputStream()));
						login_pw = new PrintWriter(socket.getOutputStream());

						// 认为此时jitsi启动成功。。 login
						if (isConnected && !jtf_username.getText().isEmpty()
								&& jpf_password.getPassword().length > 0) {

							currentSipId = jtf_username.getText() + "@"
									+ SIP_HOST;

							login_pw.println(jtf_username.getText() + "@"
									+ SIP_HOST + ";"
									+ new String(jpf_password.getPassword()));
							login_pw.flush();
							System.out.println("Login with "
									+ jtf_username.getText());

							// login 之后，启动监听 mq 消息的进程，其中的 queue 用 currentSipId
							// 构造

							new Thread(
									new RabbitmqUtil.MqWatcher(currentSipId) {

										@Override
										public void onJitsiCheckRequest(
												String requesterId,
												String responserId) {
											// 假设此时瞬间检测完成
											System.out.println("假设此时瞬间检测完成");
											RabbitmqUtil.sendJitsiOkResponse(
													responserId, requesterId);
										}

										@Override
										public void onJitsiOkResponse(
												String requesterId,
												String responserId) {
											// call the responser~
											System.out
													.println("call the responser~ "
															+ responserId);
											// // call 的部分
											String callWho = responserId;
											if (isConnected
													&& !callWho.isEmpty()) {
												account_pw.println("call:"
														+ callWho);
												account_pw.flush();
												System.out.println("Call "
														+ callWho
														+ " command sent.");
												jtf_callWho.setText("");
											} else {
												System.out
														.println("No client connected.");
											}
										}
									}).start();

						} else
							System.out.println("Disconnected");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void finalize() {
		// 尝试在关闭Test窗口时干掉jitsi // 貌似不行
		System.out.println("Try to destroy jitsiProcess in finalize().");
		jitsiProcess.destroy();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Test();
	}

	// 接收 ant 输出的线程
	class SubprocessOutputListener implements Runnable {
		private BufferedReader stdout;
		private String line;

		public SubprocessOutputListener(Process pr) {
			stdout = new BufferedReader(new InputStreamReader(
					pr.getInputStream()));
		}

		@Override
		public void run() {
			try {
				while ((line = stdout.readLine()) != null) {
					System.out.println("[jitsi]" + line);
				}
				stdout.close();
				System.out
						.println("================'stdout' closed.================");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
