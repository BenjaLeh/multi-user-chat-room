package Server;

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
 * Le serveur qui tourne permanent en permettant la connexion de nouveau client
 * Une fois il reçoit un message d'un client, il le renvoie aux tous les clients
 * 
 * @author Yu LIU
 * 
 */
public class Server {
	/**
	 * Une collection pour stocker tous les sockets de client
	 * qui sont connectés avec le serveur
	 */
	private HashSet<Socket> clientSocketSet = new HashSet<>();
	

	/**
	 * Le constructeur: Créer et initialiser le socket du serveur et attendre la connexion de client
	 * Une fois un client est connecté, on crée un nouveau thread pour recevoir le message de ce client
	 * et le renvoie aux tous les autres
	 */
	public Server(int port) {
		try {
			// créer le socket du serveur sur le port indiqué
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(port);

			while (true) {
				// attendre la connexion de client
				Socket clientSocket = serverSocket.accept();

				// vérrouiller la variale partagée "clientSocketSet"
				synchronized (clientSocketSet) {
					// ajouter le socket connecté à la collection
					clientSocketSet.add(clientSocket);

					// créer un Thread uniquement pour récuperer le message de ce client et le renvoyer
					new ServerThread(clientSocket, clientSocketSet).start();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Un Thread du coté serveur juste pour servir à un client
	 * en récuperant le message envoyé de ce client et le renvoyant aux tous les clients
	 * 
	 * @author Yu LIU
	 * 
	 */
	class ServerThread extends Thread {
		/**
		 * Le socket du client connecté dans ce Thread
		 */
		private Socket clientSocket;
		private String clientName;
		/**
		 * La collection de tous les clients connectés
		 */
		private HashSet<Socket> clientSocketSet;


		/**
		 * @param clientSocket est le client connecté dans ce Thread
		 * @param clientSocketSet est la collection de tous les clients
		 */
		public ServerThread(Socket clientSocket, HashSet<Socket> clientSocketSet) {
			this.clientSocket = clientSocket;
			this.clientSocketSet = clientSocketSet;
		}

		/**
		 * Pour démarrer le Thread
		 */
		public void run() {
			try {
				// quand on démarre le Thread, on récupère d'abord le nom du client et informer tous les clients qu'il a rejoint la sallon 
				BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				clientName = br.readLine();
				synchronized (clientSocketSet) {  // vérrouiller la variale partagée "clientSocketSet"
					sendToAll(clientName+" rejoint la salon de discussion");
				}
				System.out.println(clientName+" rejoint la salon de discussion");
				String messageReceived;

				while (true) {
					// attendre jusqu'à ce qu'il y a le nouveau message à recevoir dans le socket
					while(clientSocket.getInputStream().available()<=0)
						;
					
					messageReceived = br.readLine();
					
					// Si le serveur reçoit le String "shutdown", il ferme le socket connecté avec ce client et puis il envoie le message "ce client a quitté la salon" à tous les autres clients
					if(messageReceived.equals("shutdown")){
						System.out.println(clientName+" quitte la salon de discussion");
						clientSocket.close();
						synchronized (clientSocketSet) {
							if(clientSocketSet.remove(clientSocket)){
								sendToAll(clientName+" quitte la salon de discussion");
								System.out.println(clientName+" a ete supprime dans la collection des clients");
							}
						}
						break;  // sort du thread et termine
					}

					//  renvoyer le message recu vers tous les clients
					synchronized (clientSocketSet) {
						sendToAll(messageReceived);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Envoyer un message vers tous les clients
		 * 
		 * @param s est le message à envoyer
		 */
		private void sendToAll(String s) {
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

		new Server(port);
	}
}
