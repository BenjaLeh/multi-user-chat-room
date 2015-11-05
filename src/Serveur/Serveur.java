package Serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 服务器持续运行,客户可以通过IP地址和端口号连接服务器
 * 一旦服务器收到某个客户端发来的消息,便将其转发给所有客户
 * Le serveur qui tourne permanent en permettant la connexion de nouveau client
 * Une fois il reçoit un message d'un client, il le renvoie aux tous les clients
 * 
 * @author Yu LIU
 * 
 */
public class Serveur {
	/**
	 * 存储所有已连接的客户端的sokets的集合
	 * Une collection pour stocker tous les sockets de client
	 * qui sont connectés avec le serveur
	 */
	private HashSet<Socket> clientSocketSet = new HashSet<>();
	

	/**
	 * 构造函数:初始化套接口,等待连接
	 * 一旦有新的客户端连接上,就开启一个新的线程负责接收它发来的消息并转发给所有客户端
	 * Le constructeur: Créer et initialiser le socket du serveur et attendre la connexion de client
	 * Une fois un client est connecté, on crée un nouveau thread pour recevoir le message de ce client
	 * et le renvoie aux tous les autres
	 */
	public Serveur(int port) {
		try {
			// 在指定端口创建套接口
			// créer le socket du serveur sur le port indiqué
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(port);

			while (true) {
				// 监听(一旦有新的客户端连接时返回一个套接口专门用于和此客户端通信)
				// attendre la connexion de client
				Socket clientSocket = serverSocket.accept();

				// 访问全局变量clientSocketSet时加锁
				// vérrouiller la variale partagée "clientSocketSet"
				synchronized (clientSocketSet) {
					// 将客户端套接口加入集合
					// ajouter le socket connecté à la collection
					clientSocketSet.add(clientSocket);

					// 创建并启动一个专门负责此客户端的线程
					// créer un Thread uniquement pour récuperer le message de ce client et le renvoyer
					new ServerThread(clientSocket, clientSocketSet).start();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 专门负责一个客户端的服务器线程
	 * 将此客户端发来的消息转发给所有客户端
	 * Un Thread du coté serveur juste pour servir à un client
	 * en récuperant le message envoyé de ce client et le renvoyant aux tous les clients
	 * 
	 * @author Yu LIU
	 * 
	 */
	class ServerThread extends Thread {
		/**
		 * 此线程连接的client
		 * Le socket du client connecté dans ce Thread
		 */
		private Socket clientSocket;
		private String clientName;
		/**
		 * 保存所有客户套接口的集合
		 * La collection de tous les clients connectés
		 */
		private HashSet<Socket> clientSocketSet;


		/**
		 * 构造函数:创建时传入连接客户端的套接口和所有客户端集合
		 * 均为将地址传入
		 * 
		 * @param clientSocket est le client connecté dans ce Thread
		 * @param clientSocketSet est la collection de tous les clients
		 */
		public ServerThread(Socket clientSocket, HashSet<Socket> clientSocketSet) {
			this.clientSocket = clientSocket;
			this.clientSocketSet = clientSocketSet;
		}

		/**
		 * 启动线程
		 * pour démarrer le Thread
		 */
		public void run() {
			try {
				// 启动线程时先接收客户名然后发送给所有客户端"此客户加入聊天室"
				// quand on démarre le Thread, on récupère d'abord le nom du client et informer tous les clients qu'il a rejoint la sallon 
				BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				clientName = br.readLine();
				synchronized (clientSocketSet) {  // vérrouiller la variale partagée "clientSocketSet"
					sendMessage(clientName+" rejoint la salon de discussion");
				}
				System.out.println(clientName+" rejoint la salon de discussion");
				String messageReceived;

				while (true) {
					// 一直等到套接口有新信息可以接收
					// attendre jusqu'à ce qu'il y a le nouveau message à recevoir dans le socket
					while(clientSocket.getInputStream().available()<=0)
						;
					
					messageReceived = br.readLine();
					
					// 如果收到"shutdown"时关闭套接口,并将该用户从客户端集合中删除,之后通知所有其他客户"此客户已退出聊天室"
					// Si le serveur reçoit le String "shutdown", il ferme le socket connecté avec ce client et puis il envoie le message "ce client a quitté la salon" à tous les autres clients
					if(messageReceived.equals("shutdown")){  // 比较内容时用equals()
						System.out.println(clientName+" quitte la salon de discussion");
						clientSocket.close();
						synchronized (clientSocketSet) {
							if(clientSocketSet.remove(clientSocket)){
								sendMessage(clientName+" quitte la salon de discussion");
								System.out.println(clientName+" a ete supprime dans la collection des clients");
							}
						}
						break;  // sort du thread et termine
					}

					// 将接收信息发送给所有客户
					//  renvoyer le message recu vers tous les clients
					synchronized (clientSocketSet) {
						sendMessage(messageReceived);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 将字符串信息发送给所有客户端
		 * Envoyer un message vers tous les clients
		 * 
		 * @param s est le message à envoyer
		 */
		private void sendMessage(String s) {
			// 遍历集合中的所有元素
			// parcourir chaque socket des clients
			Iterator<Socket> it = clientSocketSet.iterator();
			while (it.hasNext()) {
				Socket tmp = it.next();
				try {
					// créer un PrintWriter à écrire le message dans tmp.getOutputStream()
					PrintWriter pw = new PrintWriter(tmp.getOutputStream(),true);
					// écrire le message dans le PrintWriter
					pw.println(s);
					// Refresh the writer immediately
					pw.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 显示本机已启动的IPv4地址
	 * affiche l'adresse IPv4 du serveur
	 */
	public static void printIPv4() {
		try {
			System.out.println("L'adresse IPv4 du serveur:");

			// retourne toutes les interfaces réseaux de la machine
			Enumeration<NetworkInterface> enumNetInterface = NetworkInterface.getNetworkInterfaces();

			while(enumNetInterface.hasMoreElements()){
				NetworkInterface netInterface = enumNetInterface.nextElement();
				if(!netInterface.isUp())
					continue;

				// retourne toutes les adresses IP de l'interface réseau
				Enumeration<InetAddress> enumAddress = netInterface.getInetAddresses();

				while(enumAddress.hasMoreElements()){
					InetAddress address = enumAddress.nextElement();

					// afficher l'adresse IPv4
					if(address.getHostAddress().indexOf(":") == -1){
						if(address.isLoopbackAddress()){
							System.out.print("Loop back address: ");
							System.out.println(address.getHostAddress());
						} else {
							System.out.print("IP address: ");
							System.out.println(address.getHostAddress());
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Le main Thread
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.print("Veuillez saisir le port: ");
		Scanner scanner = new Scanner(System.in);
		int port = scanner.nextInt();
		printIPv4();

		new Serveur(port);
	}
}
