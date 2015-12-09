

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class PeerConnections extends Thread{
	
	public Tracker tracker;
	public static ServerSocket acceptServerSocket;
	public static Socket acceptSocket;
	static DataInputStream input;
	static DataOutputStream output;
	public String IP;
	public int port;
	byte[] peerId;
	public static byte[] trackerPeerId;

	
	
	PeerConnections(Tracker tracker){
		
		for (int i = 6881; i < 6890; i++){
			
			try{
			acceptServerSocket = new ServerSocket(i);
			System.out.println("ServerSocket with peer on port: "+i);
			break;
			}
			catch(Exception e){
				
			}		
			
			this.tracker = tracker;
			this.trackerPeerId = tracker.getPeerid();
	}
		
		
	}
	public void run(){
		
		try {
		
		if(acceptServerSocket.accept() != null)
		acceptSocket = acceptServerSocket.accept();
		input = new DataInputStream(acceptSocket.getInputStream());
		output  = new DataOutputStream(acceptSocket.getOutputStream());
		
		try{
			output.write(handshake());
			output.flush();
		}
		catch(IOException e)
		{
			System.err.println("Cannot send handshake to peer");
			return;
		}
		
		byte[] response = new byte[68];
		
		acceptSocket.setSoTimeout(1000);
		input.readFully(response);		
		acceptSocket.setSoTimeout(1000);
		
		InetAddress peerIP = acceptSocket.getInetAddress();
		IP = peerIP.toString();
		port = acceptSocket.getPort();
		peerId = Arrays.copyOfRange(response, 48, 68);
		
		//Create a new peer and add in to the peers list
		Peer p = new Peer(peerId, IP, port, tracker.infohash, tracker.getPeerid(), tracker.piecesHash, tracker.pieceLength, tracker.fileName, tracker.fileLength, tracker);

		
		Thread peer = new Thread(p);
		peer.start();
	}
	
	catch(IOException e) {
		e.printStackTrace();
	}
		
	}
	
	public byte[]handshake() throws UnsupportedEncodingException{
		
		//handshake: <pstrlen><pstr><reserved><info_hash><peer_id>
		byte pstrlen = 0x13;
		byte[] pstr= { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
				'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
		byte[] reserved = {'0','0','0','0','0','0','0','0'}; 
		byte[] ihash = tracker.infohash;
	    byte[] handshake = new byte[1 + pstr.length + reserved.length + ihash.length + trackerPeerId.length];
	    handshake[0] = pstrlen;
		System.arraycopy(pstr,0,handshake,1,19);
		System.arraycopy(reserved,0,handshake,20,8);
		System.arraycopy(ihash,0,handshake,28,ihash.length);
		System.arraycopy(trackerPeerId,0,handshake,28+ihash.length,trackerPeerId.length);

	    return handshake;

	
}
}

	