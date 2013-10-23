package cs.iit.edu.cs550;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

public class PeerNode {
	static String SERVER_IP = "127.0.0.1";
	static String CLIENT_IP = "127.0.0.1";
	static int SERVER_PORT = 4554;
	static int CLIENT_PORT = 4555;
	static String homeDirectory;
	static long lastModified;
	static boolean isPerformance = false;
	final static int NUM_REQUEST = 1000;

	/**
	 * @param args
	 * @throws IOException
	 * @throws JAXBException
	 */
	public static void main(String[] args) throws IOException, JAXBException {
		String fileName = null;
		if (args.length != 4 && args.length != 6) {
			System.out
					.println("Please specify java -jar PeerNode.jar SERVER_IP SERVER_PORT CLIENT_PORT HOME_DIRECTORY");
			System.out
					.println("Example: java -jar PeerNode.jar 127.0.0.1 4554 4555 E:\\P1\\");
			System.exit(0);
		}

		if (args[0] == null || args[1] == null || args[2] == null
				|| args[3] == null) {
			System.out
					.println("Arguments cannot be null. Please specify java -jar PeerNode.jar SERVER_IP SERVER_PORT CLIENT_PORT");
			System.exit(0);
		}

		SERVER_IP = args[0];
		SERVER_PORT = Integer.parseInt(args[1]);
		CLIENT_PORT = Integer.parseInt(args[2]);
		CLIENT_IP = InetAddress.getLocalHost().getHostAddress();
		homeDirectory = args[3];
		if (args.length == 6) {
			if (args[4].equals("-p")) {
				isPerformance = true;
				fileName = args[5];
			}
		}
		// Register to the indexing server first.
		registerContent(homeDirectory);

		// Create a separate thread for
		// notifying any change in the file structure
		NotifyServer notifyServer = new NotifyServer(homeDirectory);
		Thread notifyThread = new Thread(notifyServer);
		notifyThread.start();

		// Peer acts as a server, creates multiple threads on client request.
		// Listens on the port
		PeerThread peerThread = new PeerThread(CLIENT_PORT, homeDirectory);
		Thread thread = new Thread(peerThread);
		thread.start();

		// ClientThread acts as Peer client. It gets user requests to obtain the
		// file from other peer
		ClientThread clientThread = new ClientThread(homeDirectory,
				isPerformance, fileName);
		Thread cThread = new Thread(clientThread);
		cThread.start();

	}

	public static void registerContent(String homeDirectory)
			throws JAXBException, IOException {
		File folder = new File(homeDirectory);
		lastModified = folder.lastModified();
		File[] listOfFiles = folder.listFiles();
		Map<String, Long> fileProp = new HashMap<String, Long>();
		Socket clientSocket = null;
		for (File file : listOfFiles) {
			if (file.isFile()) {
				// fileList.add();
				fileProp.put(file.getName(), file.length());

			}
		}
		JAXBContext context = JAXBContext.newInstance(Metadata.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		try {
			clientSocket = new Socket(SERVER_IP, SERVER_PORT);
		} catch (UnknownHostException e) {
			System.out.println("Server not ready");
		} catch (ConnectException e) {
			System.out.println("Server not ready");
			System.exit(-1);
		}

		Metadata metadata = new Metadata();
		metadata.setIPAddress(CLIENT_IP);
		metadata.setPort(CLIENT_PORT);
		metadata.setFileProp(fileProp);
		metadata.setType("REGISTER");
		if (clientSocket != null) {

			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			m.marshal(metadata, outToServer);
			clientSocket.shutdownOutput();
			outToServer.flush();
			clientSocket.close();
		} else
			System.out.println("Client not connected to server");

	}

	static class PeerThread implements Runnable {
		int port;
		String homeDirectory;

		public PeerThread(int port, String homeDirectory) {
			this.port = port;
			this.homeDirectory = homeDirectory;
		}

		@Override
		public void run() {

			ServerSocket serverSocket;
			try {
				serverSocket = new ServerSocket(port);

				while (true) {
					Socket clientSocket = serverSocket.accept();
					DataInputStream in = new DataInputStream(
							clientSocket.getInputStream());
					String fileName = in.readUTF();
					System.out.println(fileName
							+ "-filename requested from the peer");
					File file = new File(this.homeDirectory + fileName);
					byte[] bytearray = new byte[(int) file.length()];
					FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis);
					bis.read(bytearray, 0, bytearray.length);
					OutputStream os = clientSocket.getOutputStream();
					os.write(bytearray, 0, bytearray.length);
					System.out.println("Requested file sent");
					os.flush();
					bis.close();
					clientSocket.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	// Here the peer continuously checks for the directory changes, if any file
	// are added or deleted then it will notify
	static class NotifyServer implements Runnable {
		String homeDirectory;

		public NotifyServer(String homeDirectory) {
			this.homeDirectory = homeDirectory;
		}

		@Override
		public void run() {

			File folder = new File(homeDirectory);
			while (true) {
				long currentTime = folder.lastModified();
				if (lastModified != currentTime) {
					try {
						System.out
								.println("File as been modified. Notifying Server.....");
						registerContent(homeDirectory);
					} catch (JAXBException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}

	}

	static class ClientThread implements Runnable {
		String homeDirectory;
		boolean isPerformance;
		String pfileName;

		public ClientThread(String homeDirectory, boolean isPerformance,
				String fileName) {
			this.homeDirectory = homeDirectory;
			this.isPerformance = isPerformance;
			this.pfileName = fileName;
		}

		public boolean isFilePresent(String fileName) {
			File folder = new File(this.homeDirectory);
			File[] listOfFiles = folder.listFiles();
			for (File file : listOfFiles) {
				if (file.isFile() && file.getName().equals(fileName)) {
					return true;
				}
			}
			return false;
		}

		public void performanceEvaluation() throws IOException, JAXBException {
			long start, end;
			JAXBContext context = JAXBContext.newInstance(Metadata.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			Metadata metadata = new Metadata();
			metadata.setIPAddress(CLIENT_IP);
			metadata.setPort(CLIENT_PORT);
			metadata.setType("SEARCH");
			metadata.setFileName(this.pfileName);
			Socket clientSocket = null;
			System.out
					.println("Sending 1000 sequential search request to indexing server.");
			start = System.currentTimeMillis();

			for (int i = 0; i < NUM_REQUEST; i++) {

				try {
					clientSocket = new Socket(SERVER_IP, SERVER_PORT);
				} catch (UnknownHostException e) {
					System.out.println("Server not ready");
				} catch (ConnectException e) {
					System.out.println("Server not ready");
					System.exit(-1);
				}

				if (clientSocket != null) {
					DataOutputStream outToServer = new DataOutputStream(
							clientSocket.getOutputStream());
					m.marshal(metadata, outToServer);
					clientSocket.shutdownOutput();
					outToServer.flush();
				} else
					System.out.println("Client not connected to server");

				Unmarshaller um = context.createUnmarshaller();
				Metadata resultMetadata = (Metadata) um
						.unmarshal(new MyInputStream(clientSocket
								.getInputStream()));

				if (resultMetadata != null) {
					List<String> peerList = resultMetadata.getPeerList();
					if (peerList != null && peerList.size() > 0) {

					} else {
						System.out
								.println("No peers found for the entered filename.");
					}
				}

				clientSocket.close();

			}
			end = System.currentTimeMillis();

			java.io.PrintWriter writer = new java.io.PrintWriter(Thread
					.currentThread().getId() + end + ".txt", "UTF-8");
			writer.println("Time taken to process 1000 search request : "
					+ (end - start) + " ms");
			writer.close();
			System.out.println("Time taken to process 1000 search request : "
					+ (end - start) + " ms");
		}

		@Override
		public void run() {

			if (isPerformance) {
				try {
					performanceEvaluation();
					System.exit(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JAXBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else {

				boolean isFound;
				BufferedReader br;
				String fileName = null;
				while (true) {
					isFound = false;

					System.out.println("Enter fileName to search: ");
					br = new BufferedReader(new InputStreamReader(System.in));
					try {
						fileName = br.readLine();
						if (isFilePresent(fileName)) {
							System.out
									.println("File already present in current home directory");
							continue;
						}
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					try {
						if (fileName.equals("exit")) {

							break;
						} else {
							JAXBContext context = JAXBContext
									.newInstance(Metadata.class);
							Marshaller m = context.createMarshaller();
							m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
									Boolean.TRUE);
							Socket clientSocket = null;
							try {
								clientSocket = new Socket(SERVER_IP,
										SERVER_PORT);
							} catch (UnknownHostException e) {
								System.out.println("Server not ready");
							} catch (ConnectException e) {
								System.out.println("Server not ready");
								System.exit(-1);
							}

							Metadata metadata = new Metadata();
							metadata.setIPAddress(CLIENT_IP);
							metadata.setPort(CLIENT_PORT);
							metadata.setType("SEARCH");
							metadata.setFileName(fileName);
							if (clientSocket != null) {
								DataOutputStream outToServer = new DataOutputStream(
										clientSocket.getOutputStream());
								m.marshal(metadata, outToServer);
								clientSocket.shutdownOutput();
								outToServer.flush();
								// clientSocket.close();
							} else
								System.out
										.println("Client not connected to server");

							Unmarshaller um = context.createUnmarshaller();
							Metadata resultMetadata = (Metadata) um
									.unmarshal(new MyInputStream(clientSocket
											.getInputStream()));
							long fileSize = 6000000;
							long fileLength = 0;
							if (resultMetadata != null) {
								List<String> peerList = resultMetadata
										.getPeerList();
								if (peerList != null && peerList.size() > 0) {
									System.out.println("Peer Id's:");
									for (String peers : peerList) {
										System.out.println(peers.split("-")[0]);
										fileLength = Long.parseLong(peers
												.split("-")[1]);
									}
									isFound = true;
								} else {
									System.out
											.println("No peers found for the entered filename.");
								}
							}

							clientSocket.close();
							String peerId = null;

							if (isFound) {
								System.out.println("Enter Peer Id to search: ");
								br = new BufferedReader(new InputStreamReader(
										System.in));
								if ((peerId = br.readLine()) != null) {
									try {

										clientSocket = new Socket(
												peerId.split(":")[0],
												Integer.parseInt(peerId
														.split(":")[1]));
									} catch (UnknownHostException e) {
										System.out.println("Server not ready");
									} catch (ConnectException e) {
										System.out.println("Server not ready");
										System.exit(-1);
									}

									OutputStream outToServer = clientSocket
											.getOutputStream();
									DataOutputStream out = new DataOutputStream(
											outToServer);

									out.writeUTF(fileName);

									// Request for file
									int bytesRead;
									int current = 0;
									byte[] bytearray = new byte[(int) fileSize];
									InputStream is = clientSocket
											.getInputStream();
									FileOutputStream fos = new FileOutputStream(
											homeDirectory + fileName);
									BufferedOutputStream bos = new BufferedOutputStream(
											fos);
									bytesRead = is.read(bytearray, 0,
											bytearray.length);
									current = bytesRead;

									do {
										bytesRead = is.read(bytearray, current,
												(bytearray.length - current));
										if (bytesRead >= 0)
											current += bytesRead;
									} while (bytesRead > -1);

									bos.write(bytearray, 0, current);
									bos.flush();

									bos.close();
									clientSocket.close();
									System.out
											.println("File "
													+ fileName
													+ " downloaded to the Home directory");

									if (fileLength < 1024) {
										String line = null;
										System.out.println("File Contents: \n");

										BufferedReader bufferedReader = new BufferedReader(
												new FileReader(homeDirectory
														+ fileName));
										while ((line = bufferedReader
												.readLine()) != null) {
											System.out.println(line);
										}
										bufferedReader.close();
									}

								}

							}

						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (PropertyException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (JAXBException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
	}

	public static class MyInputStream extends FilterInputStream {
		public MyInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() {
			// do nothing
		}
	}

}
