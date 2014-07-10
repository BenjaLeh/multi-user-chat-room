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
	private static final long serialVersionUID = 1L;
	/**
	 * La partie de top panel de l'interface graphique
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
	 * La partie du centre de l'interface graphique
	 */
	private JTextArea textArea;
	private JScrollPane scrollPane;
	/**
	 * La partie de bottom panel de l'interface graphique
	 */
	private JTextField textField;
	private JButton buttonSend;
	private Socket socket;
	private boolean connected;
	/**
	 * un objet PrintWriter qui nous permet d'écrire les données dedans
	 */
	private PrintWriter pw;


	/**
	 * Le constructeur: créer la fenêtre du client
	 */
	public ClientWindow() {
		this.connected = false;
		this.setSize(600, 400);
		this.setTitle("ChatRoom");
		/**
		 * Initialize the top panel
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
		ConnectActListener connectionAction = new ConnectActListener();
		buttonConnection.addActionListener(connectionAction);
		paneltop.add(buttonConnection);
		buttonDisconnection = new JButton("Disconnect");
		DisconnectActListener disconnectAction = new DisconnectActListener();
		buttonDisconnection.addActionListener(disconnectAction);
		buttonDisconnection.setEnabled(false);
		paneltop.add(buttonDisconnection);

		/**
		 * Initialize the center
		 */
		textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		// Add a document listener to the text area refresh it when the new message appears
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
		scrollPane = new JScrollPane(textArea);
		
		/**
		 * Initialize the bottom panel
		 */
		JPanel panelBottom = new JPanel();
		panelBottom.setLayout(new FlowLayout());
		textField = new JTextField(30);
		panelBottom.add(textField);
		buttonSend = new JButton("Send");
		SendActListener sendAction = new SendActListener();
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
	 * Le thread client: qui est chargé de recevoir le message et l'afficher
	 * 
	 * @author Yu LIU
	 * 
	 */
	class ClientReceiveThread extends Thread {

		public void run() {
			try {
				// envoyer le nom du client quand on lance le Thread
				pw.println(textName.getText());

				// créer un objet Reader en utilisant le InputStream du socket pour récupérer les données dedans
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while (true) {
					// attendre jusqu'à ce qu'il y a le nouveau message à recevoir dans le socket
					while(!socket.isClosed() && socket.getInputStream().available()<=0)
						;

					if(socket.isClosed()){
						System.out.println("The client thread has been closed");
						break;
					}
					
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
	 * Create a ActionListener to register on the button Send
	 * Send the message to the server
	 * 
	 * @author Yu LIU
	 * 
	 */
	class SendActListener implements ActionListener {

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
	 * Create a ActionListener to register on the button Connect
	 * Connect with the indicated IP and Port then open a new Thread for this connection
	 * 
	 * @author Yu LIU
	 *
	 */
	class ConnectActListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				String IP;
				int port;
				IP = textIP.getText();
				port = Integer.parseInt(textPort.getText());
				socket = new Socket(IP,port);
				if(socket.isConnected()){
					System.out.println("successfully connected");
					connected = true;
					buttonConnection.setEnabled(false);
					buttonDisconnection.setEnabled(true);
					buttonSend.setEnabled(true);
					pw = new PrintWriter(socket.getOutputStream(), true);
					new ClientReceiveThread().start();
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
	 * A ActionListener bind with the button Disconnect
	 * 
	 * @author Yu LIU
	 *
	 */
	class DisconnectActListener implements ActionListener {

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
