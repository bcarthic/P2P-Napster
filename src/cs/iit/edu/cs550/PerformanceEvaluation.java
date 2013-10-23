package cs.iit.edu.cs550;

import java.io.IOException;

public class PerformanceEvaluation {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException, InterruptedException {
		int NUM_PEERS = 5;
		// Starting indexing server

		Process proc = Runtime.getRuntime().exec(
				"java -jar IndexingServer.jar 4554");

		
		
		Process peer = Runtime.getRuntime().exec(
				"java -jar PeerNode.jar 127.0.0.1 4554 4999 E:\\P\\");
		
		int portNum = 4555;
		for (int count = 1; count <= NUM_PEERS; count++) {
			String folderName = " E:\\P" + count + "\\ ";

			Process proc1 = Runtime.getRuntime().exec(
					"java -jar PeerNode.jar 127.0.0.1 4554 " + portNum++
							+ folderName + "-p testfile.txt");
		}
	}

}
