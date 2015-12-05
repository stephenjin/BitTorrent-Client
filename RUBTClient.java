
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

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import Src.Tracker;
import Src.Peer;
import Src.Message;

public class RUBTClient{

	private static String saveAsFile = null;
    public static void main(String[] args) throws Exception 
    {
		
	String torrentName = null;
	
	if(args.length!=2)
	{
			System.out.println("Invalid number of arguments. Exiting client...");
			System.exit(0); //should probably change this later
	}	
	
	//Extracts cmd line arguments into variables
	torrentName= args[0];
	saveAsFile = args[1];
	
	
	File torrentFile = new File(torrentName);
	
	TorrentInfo tInfo = parseTorrentFile(torrentFile);
	
	Tracker tracker = new Tracker(tInfo, saveAsFile);
	byte[] trackerResponse = tracker.connectTracker();
	ArrayList<Peer> peers = tracker.decodeTracker(trackerResponse); //an arraylist of peers
	Peer match = getPeerMatch(peers);
	match.run();
	System.out.print("File successfully downloaded. File saved as: ");
	System.out.println(saveAsFile);
	
	
            
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
    
	public static Peer getPeerMatch(ArrayList<Peer> peer)
	{
		int i;
		int match = 0;
		byte[] check = {'-','R','U'};
		for(i = 0; i< peer.size(); i++) 
		{

			if(peer.get(i).PeerId[0] == check[0] && peer.get(i).PeerId[1] == check[1] && peer.get(i).PeerId[2] == check[2])
			{	
				match=i;
				break;
			}
		}
		
		return peer.get(match);
	}
	public static String getFileName()
	{
		return saveAsFile;
	}
	
	
	
}
