import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
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


public class Test {

	public boolean isConnected = false;
	public BufferedReader account_br = null; 	//account_br用于在login时储存接收的socket信令
	public BufferedReader login_br = null;		//login_br用于在登陆后储存接收的socket信令
	public PrintWriter account_pw = null;		//account_pw用于在login时储存要发送的信令
	public PrintWriter login_pw = null;			//login_pw用于在登陆后储存要发送的信令
	
	private JFrame jFrame = null;
	private JPanel jp_loginInfo = null;
	private JLabel jl_username = null;
	private JTextField jtf_username = null;
	private JLabel jl_password = null;
	private JPasswordField jpf_password = null;
	private JPanel jp_callWho = null;
	private JLabel jl_callWho = null;
	private JTextField jtf_callWho = null;
	private JButton jb_login = null;
	private JButton jb_call = null;
	private JButton jb_offline = null;

	public Test()
	{
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
		
		jb_login = new JButton("Login");
		//监听login按钮事件
		jb_login.addActionListener(new ActionListener() {
			//传输用户名和密码
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isConnected && !jtf_username.getText().isEmpty() && jpf_password.getPassword().length>0)
				{
					
					login_pw.println(jtf_username.getText() + "@192.168.1.104;" + new String(jpf_password.getPassword()));
					login_pw.flush();
					System.out.println("Login with " + jtf_username.getText());
				}
				else
					System.out.println("Disconnected");
					
			}
		});
		jFrame.add(jb_login,BorderLayout.WEST);
		
		jb_call = new JButton("Call");
		//监听call按钮事件
		jb_call.addActionListener(new ActionListener(){

			//传输被呼叫方用户名
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (isConnected && !jtf_callWho.getText().isEmpty())
				{
					account_pw.println("call:" + jtf_callWho.getText());
					account_pw.flush();
					System.out.println("Call " + jtf_callWho.getText() + " command sent.");
					jtf_callWho.setText("");
				}
				else
					System.out.println("No client connected.");
			}
			
		});
		jFrame.add(jb_call, BorderLayout.CENTER);
		
		jb_offline = new JButton("Offline");
		//监听offline按钮事件
		jb_offline.addActionListener(new ActionListener()
		{
			//传输offline信令
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isConnected)
				{
					account_pw.println("offline");
					account_pw.flush();
					jtf_username.setText("");
					jpf_password.setText("");
					System.out.println("Offline command sent.");
				}
				else
					System.out.println("No client connected.");
			}
			
		});
		jFrame.add(jb_offline, BorderLayout.EAST);
		
		jFrame.setBounds(100, 100, 400, 150);
		jFrame.setVisible(true);
		
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//建立线程对login模块进行监听
		new Thread(){
			@Override
			public void run()
			{
				ServerSocket server;
				try {
					server = new ServerSocket(12345);
					System.out.println("Listening on 12345...");
					while (true)
					{
						Socket socket = server.accept();
						System.out.println("account_client connected...");
						isConnected = true;
				        account_br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				        account_pw = new PrintWriter(socket.getOutputStream());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
		//建立线程对call模块和offline模块进行监听
		new Thread(){
			@Override
			public void run()
			{
				ServerSocket server;
				try {
					server = new ServerSocket(54321);
					System.out.println("Listening on 54321...");
					while (true)
					{
						Socket socket = server.accept();
						System.out.println("login_client connected...");
						isConnected = true;
						login_br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				        login_pw = new PrintWriter(socket.getOutputStream());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Test();
	}

}
