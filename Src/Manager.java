	import java.util.ArrayList;
	import java.util.Calendar;
	import java.util.PriorityQueue;
import java.awt.event.ActionEvent;
	import java.io.*;
	import java.net.*;
	import java.util.*;
	import java.nio.*;
	import java.nio.charset.Charset;
	import java.security.*;
	import java.util.concurrent.ThreadLocalRandom;//importing everything just in case

	import GivenTools.ToolKit;
	import GivenTools.TorrentInfo;
	import GivenTools.Bencoder2;
import GivenTools.BencodingException;
	
public class Manager implements Runnable{
	
	private ArrayList<Peer> peers;
	private boolean alive;
	private ArrayList<Peer> unchokedPeers;
	private ArrayList<Peer> chokedPeers;
	
	public Manager(ArrayList<Peer> peerList)
	{
		alive = true;
		peers=peerList;
	}
	
	public void run()
	{
		
		try {
			
			while(alive)
			{
				if(peers.isEmpty())
				{
					exitManager();
				}

				long check = Calendar.getInstance().getTimeInMillis() + 30000;
	
				if (Calendar.getInstance().getTimeInMillis() >= check)
				{
					
				
					chokeWorstPeer();
					unchokeRandomPeer();
					
					check += 30000;
				}
			}
			System.out.println("Exiting Manager");
			return;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Exception in Manager Thread. Exiting Client");
			System.exit(1);
		}
		
	}
	
	public ArrayList<Peer> getPeerList()
	{
		return peers;
	}
	
	public ArrayList<Peer> getChokedPeers()
	{
		return chokedPeers;
	}
	
	public ArrayList<Peer> getUnchokedPeers()
	{
		return unchokedPeers;
		
	}
	
	public void addToUnchokedPeers(Peer p)
	{
		unchokedPeers.add(p);
		
	}
	
	public  void addToChokedPeers(Peer p)
	{
		chokedPeers.add(p);
		
	}
	public  void removeFromChokedPeers(Peer p)
	{
		if(chokedPeers != null && chokedPeers.contains(p))
			chokedPeers.remove(p);
		
	}
	
	public  void removeFromUnchokedPeers(Peer p)
	{
		if(unchokedPeers != null && unchokedPeers.contains(p))
			unchokedPeers.remove(p);
		
	}

	public  void removeFromPeers(Peer p)
	{
		if(peers != null && peers.contains(p))
			peers.remove(p);
		
	}


	private void chokeWorstPeer()
	{
		ArrayList<Peer> temp = getUnchokedPeers();
		Peer toChoke =  temp.get(0);
		
		try{
		
		for (int i=1; i< temp.size(); i++)
		{
			
			 /* Commented out to prevent compilation errors
			  *  if (temp.get(i).getBytesDownloaded() == temp.get(i).getTotalBytes())
				{
					if (temp.get(i).getAverageDownloadRate() < toChoke.getAverageDownloadRate())
						toChoke = temp.get(i);
				} else
				{
					if (temp.get(i).getAverageUploadRate() < toChoke.getAverageUploadRate())
						toChoke = temp.get(i);
				}*/
		}
		
		//Chokes worst peer
		toChoke.choke();
		System.out.println(toChoke.IP + " choked.");
		
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("Exception when choking peer" + toChoke.IP);
			
		}
		
	}
	
	private void unchokeRandomPeer()
	{
		ArrayList<Peer> temp = getChokedPeers();
		int i =	ThreadLocalRandom.current().nextInt(0, temp.size() + 1);		
		try
		{
		
			temp.get(i).unchoke();
			System.out.println(temp.get(i).IP + " unchoked.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("Exception when unchoking peer" + temp.get(i).IP);
			
		}
	}
	
	public void exitManager()
	{
		alive = false;
		if(!peers.isEmpty())
		{
			for(Peer p: peers)
			{
					/* Commented out to prevent compilation errors 
					 * if(p.getSocket()!= null && !p.getSocket().isClosed())
					 */
					p.exitGracefully();
			}
		}
	}
	

}