
/*Julia Sutula, Stephen Jin, Sehaj Singh
	Phase 1 Bit Torrent Client
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.security.*; //importing everything just in case

import java.net.*;

public class PeerList{

	private static ArrayList<Peer> peerMatches = new ArrayList<Peer>();
	private static boolean resume = false;

	
	public static void updatePeerList(ArrayList<Peer> list)
	{
		peerMatches = list;
	}

	public static ArrayList<Peer> getPeerList()
	{
		return peerMatches;
	}
	
	
	public static void updateResume(boolean value)
	{
		resume = value;
	}
		public static boolean getResume()
	{
		return resume;
	}

}
