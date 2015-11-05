package Client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ClientWindow extends JFrame {
	/**
	 * 上部面板区域
	 * La partie du top panel
	 */
	private JLabel labelName;
	private JTextField textName;
	private JLabel labelIP;
	private JTextField textIP;
	private JLabel labelPort;
	private JTextField textPort;
	private JButton buttonConnection;
	private JButton buttonDisconnection;
	/**
	 * 中间文本区域
	 * La partie du centre
	 */
	private JTextArea textArea;
	private JScrollPane scrollPane;
	/**
	 * 下部面板区域
	 * La partie du bottom panel
	 */
	private JTextField textField;
	private JButton buttonSend;
	private Socket socket;
	private boolean connected;
	/**
	 * 一个可以向其中写入数据的Writer
	 * un objet PrintWriter qui nous permet d'écrire les données dedans
	 */
	private PrintWriter pw;


	/**
	 * 构造函数:创建客户端窗口
	 * Le constructeur: créer la fenêtre du client
	 */
	public ClientWindow() {
		this.connected = false;
		this.setSize(600, 400);
		this.setTitle("ChatRoom");
		/**
		 * 创建上部面板
		 * Creer le top panel
		 */
		JPanel paneltop = new JPanel();
		paneltop.setLayout(new FlowLayout());
		labelName = new JLabel("Name: ");
		paneltop.add(labelName);
		textName = new JTextField("Yu LIU",5);
		paneltop.add(textName);
		labelIP = new JLabel("IP: ");
		paneltop.add(labelIP);
		textIP = new JTextField("127.0.0.1",10);
		paneltop.add(textIP);
		labelPort = new JLabel("Port: ");
		paneltop.add(labelPort);
		textPort = new JTextField(4);
		paneltop.add(textPort);
		buttonConnection = new JButton("Connect");
		Connection connectionAction = new Connection();
		buttonConnection.addActionListener(connectionAction);
		paneltop.add(buttonConnection);
		buttonDisconnection = new JButton("Disconnect");
		Disconnection disconnectAction = new Disconnection();
		buttonDisconnection.addActionListener(disconnectAction);
		buttonDisconnection.setEnabled(false);
		paneltop.add(buttonDisconnection);

		/**
		 * 创建中部滚动面板
		 * Créer le scroll panel au centre
		 */
		textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		scrollPane = new JScrollPane(textArea);
		textArea.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				textArea.setCaretPosition(textArea.getText().length());
				
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});

		/**
		 * 创建下部面板
		 * Creer le bottom panel
		 */
		JPanel panelBottom = new JPanel();
		panelBottom.setLayout(new FlowLayout());
		textField = new JTextField(30);
		panelBottom.add(textField);
		buttonSend = new JButton("Send");
		SendMessage sendAction = new SendMessage();
		buttonSend.addActionListener(sendAction);
		buttonSend.setEnabled(false);
		panelBottom.add(buttonSend);
		textField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke){
				if(ke.getKeyChar()==KeyEvent.VK_ENTER){
					buttonSend.doClick();
				}
			}
		});

		this.setLayout(new BorderLayout(5,5));
		this.add(paneltop, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(panelBottom, BorderLayout.SOUTH);

		/**
		 * 设置关闭窗口时触发的事件
		 * l'évenement trigger quand on ferme la fenêtre
		 */
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				if(connected){
					try {
						if(connected){
							// envoyer "shutdown" au serveur
							pw.println("shutdown");
							pw.flush();
							socket.close();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				System.out.println("The client program has been closed");
				System.exit(0);
			}
		}); 

		this.setVisible(true);
	}


	/**
	 * Client线程:负责接收并显示消息
	 * Le thread client: qui est chargé de recevoir le message et l'afficher
	 * 
	 * @author Yu LIU
	 * 
	 */
	class ClientReceive extends Thread {

		public void run() {
			try {
				// 启动线程时先将用户名发送
				// envoyer le nom du client quand on lance le Thread
				pw.println(textName.getText());

				// 用套接口输入流创建一个Reader以读取其中数据
				// créer un objet Reader en utilisant le InputStream du socket pour récupérer les données dedans
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while (true) {
					// 一直等到套接口有新信息可以接收
					// attendre jusqu'à ce qu'il y a le nouveau message à recevoir dans le socket
					while(!socket.isClosed() && socket.getInputStream().available()<=0)
						;

					if(socket.isClosed()){
						System.out.println("The client thread has been closed");
						break;
					}
					
					// 打印日期和信息内容
					// afficher la date et le message
					Date date = new Date();
					DateFormat df = DateFormat.getTimeInstance(DateFormat.LONG,Locale.FRANCE);
					textArea.append(df.format(date) + ":\n");
					textArea.append(br.readLine() + '\n');
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 创建一个动作监听器用来绑定到Send按钮上
	 * 向服务器端发送消息
	 * Create a ActionListener to register on the button Send
	 * Send the message to the server
	 * 
	 * @author Yu LIU
	 * 
	 */
	class SendMessage implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if(connected){
				pw.println(textName.getText() + ": " + textField.getText());
				pw.flush();
				textField.setText("");
			}
		}

	}
	
	/**
	 * 创建一个动作监听器用来绑定到Connect按钮上
	 * 和指定IP,Port的服务器连接,成功后为其开启一个新线程
	 * Create a ActionListener to register on the button Connect
	 * Connect with the indicate IP and Port then open a new Thread for this connection
	 * 
	 * @author Yu LIU
	 *
	 */
	class Connection implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				String IP;
				int port;
				IP = textIP.getText();
				port = Integer.parseInt(textPort.getText());
				// 初始化套接口并连接
				socket = new Socket(IP,port);
				if(socket.isConnected()){
					System.out.println("successfully connected");
					connected = true;
					buttonConnection.setEnabled(false);
					buttonDisconnection.setEnabled(true);
					buttonSend.setEnabled(true);
					pw = new PrintWriter(socket.getOutputStream(), true);
					new ClientReceive().start();
				}
			} catch (UnknownHostException e1) {
				System.out.println("Please enter the correct IP address");
			} catch (ConnectException e2) {
				System.out.println("Please enter the correct port");
			} catch (IOException e3) {
				e3.printStackTrace();
			}
		}

	}
	
	/**
	 * 动作监听器,用来断开连接
	 * A ActionListener bind with the button Disconnect
	 * 
	 * @author Yu LIU
	 *
	 */
	class Disconnection implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if(connected){
				try {
					// envoyer "shutdown" au serveur
					pw.println("shutdown");
					pw.flush();
					socket.close();
					if(socket.isClosed()){
						connected = false;
						buttonConnection.setEnabled(true);
						buttonDisconnection.setEnabled(false);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
	}

	public static void main(String[] args) {
		new ClientWindow();
	}
}
