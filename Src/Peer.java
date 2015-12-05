package Src;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.security.*; //importing everything just in case

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import java.net.*;

public class Peer implements Runnable{

	public byte[] PeerId;
	public String IP;
	public int port;
	public int pieceLength;
	public static byte[] InfoHash;
	public static byte[] trackerPeerId;
	public static ByteBuffer[] piecesHash;
	public String fileName;
	public DataOutputStream output;
	public DataInputStream input;
	public Socket s;
	public Tracker tracker;

	
	public int amChoking;
	public int amInterested;
	public int peerChoking;
	public int peerInterested;
	
	public byte[] keepAliveLength = {0,0,0,0};
	public byte[] chokeLength = {0,0,0,1};
	public byte[] unchokeLength = {0,0,0,1};
	public byte[] interestedLength = {0,0,0,1};
	public byte[] uninterestedLength = {0,0,0,1};
	public static byte[] have = {0,0,0,5};
	public static byte[] request = {0,0,0,13};
	
	public final Message keepAlive = new Message(keepAliveLength, -1);
	public final Message choke = new Message(chokeLength, 0);
	public final Message unchoke = new Message(unchokeLength, 1);
	public final Message interested = new Message(interestedLength, 2);
	public final Message uninterested = new Message(uninterestedLength, 3);
	
	public int bytesDownloaded =0;
	public int totalBytes;
	public int offset=0;
	
	public boolean alive = true;
	
	File savedFile;
	
			/**
        * Creates a new instance of a peer

        * @return
        */
	//Need to change to pass in TorrentInfo as parameter
	public Peer(byte[] peerid, String IP, int port, byte[] infohash, byte[] trackerPID, ByteBuffer[] pieces, int pieceLen, String file, int fileLen, Tracker tracker ){
		
		this.PeerId = peerid;
		this.IP = IP;
		this.port = port;
		this.InfoHash = infohash;
		this.piecesHash = pieces;
		this.trackerPeerId = trackerPID;
		this.amChoking = 1;
		this.amInterested = 0;
		this.peerChoking = 1;
		this.peerInterested = 0;
		this.pieceLength = pieceLen;
		this.fileName = file;
		this.totalBytes = fileLen;
		this.savedFile = new File(fileName);
		this.tracker = tracker;
		
	}
	
		/**
        * Creates the initial connection with a peer

        * @return
        */

    //connects and maintains communication with the peer
public void connectPeer() throws IOException{
  //Open a TCP socket on the local machine and contact the peer using the BT peer protocol and request a piece of the file.
  
	s = new Socket(IP, port);
	output = new DataOutputStream(s.getOutputStream());
	input = new DataInputStream(s.getInputStream());
	System.out.println("Connected to peer");
   return;
}

public void sendHandshake()
{
	
	try{
		output.write(handshake());

	}
	catch(IOException e)
	{
		System.err.println("Cannot send handshake to peer");
		return;
	}
  
   return;
}

		/**
        * Verifies the handshake returned by the peer

        * @return boolean true if verified handshake
        */
public boolean verifyHandshake() throws UnsupportedEncodingException
{
	byte[] presponse = new byte[68];
	
	try{
		input.read(presponse);

	}
	catch(IOException e)
	{
		System.err.println("Cannot send handshake to peer");
		return false;
	}

	byte[] subarray = Arrays.copyOfRange(presponse, 48, 68);
	
	if(Arrays.equals(subarray,PeerId))
	{
		System.out.println("Handshake Verified");	
		return true;
			
	}
	else
	{
		return false;
	}
}

		/**
        * Send initital handshake message to peer following BT protocol

        * @return byte[] of peers responding handshake
        */
public byte[]handshake() throws UnsupportedEncodingException{
	
	//handshake: <pstrlen><pstr><reserved><info_hash><peer_id>
	byte pstrlen = 0x13;
	byte[] pstr= { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
			'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
	byte[] reserved = {'0','0','0','0','0','0','0','0'}; 
	byte[] ihash = InfoHash;
    byte[] handshake = new byte[1 + pstr.length + reserved.length + ihash.length + trackerPeerId.length];
    handshake[0] = pstrlen;
	System.arraycopy(pstr,0,handshake,1,19);
	System.arraycopy(reserved,0,handshake,20,8);
	System.arraycopy(ihash,0,handshake,28,ihash.length);
	System.arraycopy(trackerPeerId,0,handshake,28+ihash.length,trackerPeerId.length);

    return handshake;

}

public void run()
{
	try{
		connectPeer();
	}
	catch(IOException e)
	{
		System.err.println("Cannot connect to peer");
		return;
	}
	
	try{
		sendHandshake();
		boolean verified = verifyHandshake();
		if(verified == false)
		{
			s.close();
		}
		//System.out.println(verified);
		//unchoke();
		interested();
		System.out.println("Now downloading file...please wait");
		while((!s.isClosed()) && alive)
		{

			int length = input.readInt();
			if(length == 0) //keep alive
			{
				//No Messages
				continue;
			}
			//System.out.println(length);
			byte messageID = input.readByte();
			switch(messageID)
			{
				case 0: //choke message
					peerChoking = 1;
					//System.out.println("Peer Choke Message");
					break;
				case 1: //unchoke message
					peerChoking = 0;
					//System.out.println("Peer Unchoke Message");
					if(amInterested == 1)
					{
						for(int i = 0; i<piecesHash.length; i++)
						{	//i=piecesHash.length-1;
							if(i == piecesHash.length-1)
							{	
								//System.out.println("****");
								//System.out.println(pieceLength);
								bytesDownloaded = (piecesHash.length-1)* pieceLength;
								pieceLength = totalBytes - bytesDownloaded;
								//System.out.println(pieceLength);
								Message.Request requestPiece = new Message.Request(i, 0, pieceLength);
								Message.sendMessage(requestPiece, output);
								byte[] piece = extractPiece();
								boolean verify = verifyHASH(piece, i);
								//boolean verify = true;
								if(verify)
								{
									if(piece == null)
									{
										break;//break;//System.out.print("IT BROKE");
									}
									writePieceToFile(piece);
									Message.Have havePiece = new Message.Have(i);
									Message.sendMessage(havePiece, output);
									bytesDownloaded+=pieceLength;
									
									//sending completed message to the tracker
									tracker.setEvent("completed");
									tracker.downloaded = bytesDownloaded;
									tracker.left = tracker.left - bytesDownloaded;
									tracker.connectTracker();
									alive = false;
								}
								else
								{
									System.out.println("Piece Hash not Verified");
								}
							}
							else
							{
								Message.Request requestPiece = new Message.Request(i, 0, pieceLength);
								Message.sendMessage(requestPiece, output);
								byte[] piece = extractPiece();
								boolean verify = verifyHASH(piece, i);
								if(verify)
								{
									writePieceToFile(piece);
									Message.Have havePiece = new Message.Have(i);
									Message.sendMessage(havePiece, output);
									bytesDownloaded+=pieceLength;
								}
								else
								{
									System.out.println("Piece Hash not Verified");
								}
							}
							 
						}
	
						
					}
					break;
				case 2: //interested message
					peerInterested = 1;
					//System.out.println("Peer Interested Message");
					break;
				case 3: //uninterested message
					peerInterested = 0;
					//System.out.println("Peer Uninterested Message");
					break;
				case 4: //have message
					//System.out.println("Peer Have Message");
					break;
				case 5: //bitfield message
					//System.out.println("Peer Bitfield Message");
					break;
				case 6: //request message
					//System.out.println("Peer Request Message");
					break;
				case 7: //piece message
					//System.out.println("Peer Piece Message");
					//s.close();
					break;
				default:  //throw an error, unreadable message
					//System.out.println("Incorrect Message ID");
					break;
			}
		}
		
		//exiting gracefully
		tracker.setEvent("stopped");
		tracker.downloaded = bytesDownloaded;
		tracker.left = 0;
		tracker.connectTracker();
		
		input.close();
		output.close();
		s.close();
	}
	catch(Exception e)
	{
		System.err.println("Error in communicating with peer");
		e.printStackTrace();
		return;
	}
	

	return;
	
}

		/**
        * Send message to peer indicating client is now choked

        * @return
        */
	public void choke()
	{
		try{
			Message.sendMessage(choke, output);
			amChoking = 1;
			return;
		}
		catch(IOException e)
		{
		System.err.println("Cannot choke peer");
		return;
		}
	}
		
		/**
        * Send message to peer indicating peer is now unchoked

        * @return
        */
	public void unchoke()
		{
			try{
				Message.sendMessage(unchoke, output);
				amChoking = 0;
				return;
			}
			catch(IOException e)
			{
			System.err.println("Cannot unchoke peer");
			return;
			}
		}
	
			/**
        * Send message to peer indicating client is interested

        * @return
        */
		public void interested()
		{
			try{
				Message.sendMessage(interested, output);
				amInterested = 1;
				return;
			}
			catch(IOException e)
			{
			System.err.println("Cannot indicate interest to peer");
			return;
			}
		}
	
		/**
        * Send message to peer indicating client is uninterested

        * @return
        */
		public void uninterested()
		{
			try{
				Message.sendMessage(uninterested, output);
				amInterested = 0;
				return;
			}
			catch(IOException e)
			{
			System.err.println("Cannot indicate uninterest to peer");
			return;
			}
		}
		/**
        * Extracts the block of a piece of the torrent file into a returned byte array
        * 
        * @return byte[]
        */
		public byte[] extractPiece()
		{
			try{
				
				int length = input.readInt();
			
				byte messageID = input.readByte();
				
				int index = input.readInt();
						
				int begin = input.readInt();
				
				byte[] block = new byte[pieceLength];
				input.readFully(block);
				
				return block;
			}
			catch(IOException e)
			{
				System.err.println("Cannot decode message from peer");
				return null;
			}
			
		}
		
		
		/**
        * Appends the byte[] to the given file
        * @param block: The piece of the file as a byte array
        * 
        * @return
        */
		
		public void writePieceToFile(byte[] block)
		{
			try{
				//sending started message to the tracker
				tracker.setEvent("started");
				tracker.downloaded = bytesDownloaded;
				tracker.left = tracker.left - bytesDownloaded;
				tracker.connectTracker();
				
				FileOutputStream out = new FileOutputStream(savedFile, true);
				out.write(block);
				offset+=pieceLength;
				out.close();
			}
			catch(IOException e)
			{
				System.err.println("Cannot write to file");
				return;
			}
			return;
			
		}
		
		/**
        * Verifies the SHA1 has of the give byte[] with the overall piece hashes provided in torrentInfo
        * @param piece: The piece of the file as a byte array
        * @param index: The index number of the piece
        * @return
        */
                public boolean verifyHASH(byte[] piece, int index) {
               
                byte[] hashed = Tracker.computeSHA1Hash(piece);
                byte[] info_hash = piecesHash[index].array();
                for (int i = 0; i < hashed.length; i++) {
                    if (hashed[i] != info_hash[i]) {
                        return false;
                    }
                }
                return true;
            }
		
		
		 
			 

	
}
