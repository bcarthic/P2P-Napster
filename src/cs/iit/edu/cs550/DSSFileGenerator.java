package cs.iit.edu.cs550;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

class DSSFileGenerator {
	private final static int MAX_BYTES_LINE = 100;
	private static int cnt =0;
	public static void main(String args[]) throws IOException {
		if(args.length !=1)
		{
			System.out.println("Please specify java -jar FileGenerator.jar FolderPath");
			System.exit(0);
		}
		if(args[0]==null)
		{
			System.out.println("Argument should not null" + args.length );
			System.exit(0);
		}
		String folderName= args[0];
		GenearateFile(1024, 1,folderName);
		GenearateFile(1024*2, 1,folderName);
		GenearateFile(1024*3, 1,folderName);
		GenearateFile(1024*4, 1,folderName);
		GenearateFile(1024*5, 1,folderName);
		GenearateFile(1024*6, 1,folderName);
		GenearateFile(1024*7, 1,folderName);
		GenearateFile(1024*8, 1,folderName);
		GenearateFile(1024*9, 1,folderName);
		GenearateFile(1024*10, 1,folderName);
		/*GenearateFile(1024*10, 100,folderName);
		GenearateFile(1024*100, 100,folderName);
		GenearateFile(1024*1024, 100,folderName);
		GenearateFile(1024*1024*10, 10,folderName);
		GenearateFile(1024*1024*31, 1,folderName);
		GenearateFile(1024*1024*15 + 4*1000, 1,folderName);*/
	}

	public static void GenearateFile(int size, int count,String folderName) throws IOException {
		String str = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		for (int j = 0; j < count; j++) {
			Random rnd = new Random();
			FileWriter fr = new FileWriter(folderName				
							+ "file1" + ++cnt + ".txt");
			int cnt = Math.round(size / MAX_BYTES_LINE);
			for (int k = 0; k < cnt; k++) {
				StringBuilder sb = new StringBuilder(MAX_BYTES_LINE);
				for (int i = 0; i < MAX_BYTES_LINE; i++) {
					sb.append(str.charAt(rnd.nextInt(str.length())));
					
				}
				sb.append("\n");
				fr.write(sb.toString());
				
			}
			fr.close();
		}
		System.out.println("Completed for size " + size);

	}

}