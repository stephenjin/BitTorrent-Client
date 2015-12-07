

/*Julia Sutula, Stephen Jin, Sehaj Singh
	Phase 1 Bit Torrent Client
*/

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.net.URL;
import java.util.Scanner;

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
import GivenTools.Bencoder2;
import GivenTools.BencodingException;


public class RUBTClient implements Runnable{

	private static String saveAsFile = null;
	private static ArrayList<Peer> peerMatches = new ArrayList<Peer>();
	private static ArrayList<Thread> threads = new ArrayList<Thread>();
	
    public static void main(String[] args) throws Exception 
    {
			
		String torrentName = null;
		
		if(args.length!=2)
		{
				System.out.println("Invalid number of arguments. Exiting client...");
				System.exit(0); 
		}	
		
		//Extracts cmd line arguments into variables
		torrentName  = args[0];
		saveAsFile = args[1];
	
	
		RUBTClient userInputCheck = new RUBTClient();
        Thread inputCheck = new Thread(userInputCheck);
        inputCheck.start();
		
		File torrentFile = new File(torrentName);
		
		
		TorrentInfo tInfo = parseTorrentFile(torrentFile);
		
		Tracker tracker = new Tracker(tInfo, saveAsFile);
		Peer.initializeBitfield(tracker); //initialize the shared client bitfield
		byte[] trackerResponse = tracker.connectTracker();
		ArrayList<Peer> peers = tracker.decodeTracker(trackerResponse); //an arraylist of peers
		//getPeerMatch(peers);
		peerMatches = peers;
		PeerList.updatePeerList(peerMatches);
		for(Peer p: peerMatches) //start each peer thread
		{
			Thread temp = new Thread(p);
			temp.start();
			threads.add(temp);

		}

            
    }
    

	//RUBT Client thread that looks for client user input to exit the client
    public void run() {
		Scanner scanner = new Scanner(System.in);
        System.out.println("Enter 'exit' to exit the client");
        while (!scanner.next().equals("exit"));
        System.out.println("Exiting client...");
        for(Thread t:threads)
        {
			t.interrupt();
			System.out.println("Interrupted thread...");
		}
        return;
    }


	/*parseTorrentFile(1): Reads and parses a given torrent file
     *Parameter:
     * 	 String torrentName: the name of the torrent file to be parsed */
	public static TorrentInfo parseTorrentFile(File torrentFile)
	{
		
	  try
	  {
		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(torrentFile));
		
		
		long size = torrentFile.length();	
			
		byte[] torrentData = new byte[(int)size];
			
		reader.read(torrentData);
		
		TorrentInfo k = new TorrentInfo(torrentData);
			
		reader.close();
		
	
		return k;
	 }
	  catch (Exception e)
	  {
		System.err.format("Exception occurred trying to read '%s'.", torrentFile);
		e.printStackTrace();
		return null;
	  }
	
	}	
    
	public static void getPeerMatch(ArrayList<Peer> peer)
	{
		int i;
		int match = 0;
		//Checks to match with a peer with an IP address of 128.6.171.130 or 128.6.171.131
		String ipCheck1 = "172.31.236.190";
		String ipCheck2 = "172.31.236.190";
		for(i = 0; i< peer.size(); i++) 
		{
			//once a connection is established with a peer, update the peers boolean value to true
			//make sure the boolean value is true and the ip is equal inorder to indicate that this is a peer to connect to
			if((peer.get(i).IP.equals(ipCheck1)) || (peer.get(i).IP.equals(ipCheck2)))
			{	
				System.out.println(peer.get(i).IP);
				peerMatches.add(peer.get(i));
				//break;
			}
		}
		
		return;
	}
	
	public static String getFileName()
	{
		return saveAsFile;
	}
	
	//Returns the list of matched peers
	public static ArrayList<Peer> getPeers(){
		return peerMatches;
	}
	

	
	
}
