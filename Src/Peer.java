/*Julia Sutula, Stephen Jin, Sehaj Singh
	Phase 1 Bit Torrent Client
*/

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
import java.lang.*;


public class Peer implements Runnable{

	private byte[] PeerId;
	
	private int port;
	private int pieceLength;
	private static byte[] InfoHash;
	private static byte[] trackerPeerId;
	private static ByteBuffer[] piecesHash;
	private String fileName;
	private DataOutputStream output;
	private DataInputStream input;
	private Socket s;
	private Tracker tracker;

	
	private int amChoking;
	private int amInterested;
	private int peerChoking;
	private int peerInterested;
	
	public static final byte[] keepAliveLength = {0,0,0,0};
	public static final byte[] chokeLength = {0,0,0,1};
	public static final byte[] unchokeLength = {0,0,0,1};
	public static final byte[] interestedLength = {0,0,0,1};
	public static final byte[] uninterestedLength = {0,0,0,1};
	public static byte[] have = {0,0,0,5};
	public static byte[] request = {0,0,0,13};
	
	
	public static final Message keepAlive = new Message(keepAliveLength, -1);
	public static final Message choke = new Message(chokeLength, 0);
	public static final Message unchoke = new Message(unchokeLength, 1);
	public static final Message interested = new Message(interestedLength, 2);
	public static final Message uninterested = new Message(uninterestedLength, 3);
	public String IP;
	private int bytesDownloaded =0;
	private int bytesUploaded = 0;
	private int totalBytes;
	private int pieceIndex;
	private int pieceOffset;
	
	private boolean alive = true;
	private boolean firstRequest = true;
	
	private boolean bitfieldInitialized = false;
	
	private boolean[] peerbf;
	private static boolean[] bitfield; //this needs to be synchronized among peers
	public static int[] pieceRarity;
	
	File savedFile;
	
	private int index = 0;
	
	private Timer timer;
	private Timer announceTimer;
	private int interval;
	
	private volatile boolean resume = false;
	private volatile boolean resumeInitialized = false;
	
	private long startTime;
    private long endTime;
	
			/**
        * Creates a new instance of a peer

        * @return
			 * @throws IOException 
        */
	//The peer constructor
	public Peer(byte[] peerid, String IP, int port, byte[] infohash, byte[] trackerPID, ByteBuffer[] pieces, int pieceLen, String file, int fileLen, Tracker tracker ) throws IOException{
		
		
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
		if(savedFile.exists() && !resumeInitialized)
		{	
			resume = true;
		}
		this.tracker = tracker;
		this.peerbf = null;
		this.interval = announceInterval();
		pieceRarity = new int[tracker.piecesHash.length]; //initialize array to keep track of how many peers have each piece
		
		if(resume)
		{	File log = new File("log.txt");
			
			//read in log file and set bitfield, upload and download amount
			if(log.isFile() && log.canRead()){
			BufferedReader in = new BufferedReader(new FileReader(log));
		
			for(int i = 0; i < bitfield.length; i++){
				
				if ((char)in.read() == 't')
					updateBitfield(i, true);
				else
					updateBitfield(i, false);
				
				}
				
				String temp = in.readLine();
				updateBytesUploaded(Integer.parseInt(temp));
				temp = in.readLine();
				updateBytesDownloaded(Integer.parseInt(temp));
				resumeInitialized = true;			
			}
			boolean[] test = bitfield; //log file not found, the file download is started over
			
			resume =false;
		}
		//688128
		//File Length 14285814
		

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

//Sends a handshake message to remote peer
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

//The peer thread where it maintains the timer
public void run()
{
	
	 try {
			
				connectPeer();
			
				sendHandshake();
				boolean verified = verifyHandshake();
				if(verified == false)
				{
					s.close();
				}
				

				//
				
				boolean check = true;
				
				timer = new Timer();
						timer.scheduleAtFixedRate(new TimerTask() { 
						@Override  
							public void run() {
							keepAlive();
							System.out.println("Sent Keep Alive");
							} 
						}, 120000,120000);
				
						
					announceTimer = new Timer();
															
						announceTimer.scheduleAtFixedRate(new TimerTask(){
						
						public void run(){					
							
							tracker.downloaded = bytesDownloaded;
							tracker.uploaded= bytesUploaded;
							tracker.left = totalBytes - bytesDownloaded;
							try {
								tracker.setRequestString(tracker.createURL(tracker.announce));
							} catch (Exception e) {		
								e.printStackTrace();
							}	
							try {
								tracker.connectTracker();
							} catch (IOException e) {
								System.out.println("Couldn't send Tracker announce");
							}
						}
					
						}, interval, interval);
				
				//starts timing the peer interaction
				startTime =  System.nanoTime();         
					
				while((!s.isClosed()) && alive && !Thread.currentThread().isInterrupted())
				{
								
					if(amInterested == 1 && peerChoking == 0)
					{
						if(getBytesDownloaded() != totalBytes)
						{
							requestPiece();
						}
						
					}
					
					
					int length = decodeIncomingMessage();
					if(length ==0) //keep alive message
					{
						continue; 
					}
					
					byte messageType = input.readByte();


					switch(messageType)
					{
						case 0: //choke message
							peerChoking = 1;
							System.out.print("Peer Choke Message from Peer: ");
							System.out.println(IP);
							break;
						case 1: //unchoke message
							peerChoking = 0;
							System.out.print("Peer Unchoke Message from Peer: ");
							System.out.println(IP);
							break;
						case 2: //interested message
							peerInterested = 1;
							unchoke();
							System.out.print("Peer Interested Message from Peer: ");
							System.out.println(IP);
							break;
						case 3: //uninterested message
							peerInterested = 0;
							System.out.print("Peer Uninterested Message from Peer: ");
							System.out.println(IP);
							break;
						case 4: //have message
							System.out.print("Peer Have Message from Peer: ");
							System.out.println(IP);
							decodeHaveMessage();
							break;
						case 5: //bitfield message
							System.out.print("Peer Bitfield Message from Peer: ");
							System.out.println(IP);
							decodeBitfieldMessage(length);
							break;
						case 6: //request message
							System.out.print("Peer Request Message from Peer: ");
							System.out.println(IP);
							decodeRequestMessage();
							break;
						case 7: //piece message
							System.out.print("Peer Piece Message from Peer: ");
							System.out.println(IP);
							decodePieceMessage();
							break;
						default:  //throw an error, unreadable message
							System.out.print("Incorrect Message ID from Peer: ");
							System.out.println(IP);
							break;
					}
					if(getBytesDownloaded() == totalBytes)
						break;
				}
				
				if(Thread.currentThread().isInterrupted()){
					File log = new File("log.txt");
					BufferedWriter out = new BufferedWriter(new FileWriter(log));
					
					for(int i = 0; i < bitfield.length; i++){
						
						if(getBitfield(i) == true)
							out.write('t');
						else
							out.write('f');
							
					}
					out.write(bytesUploaded+"\n");
					out.write(bytesDownloaded+"\n");
					out.flush();
					out.close();
				}
				
				exitGracefully();
				return;

	}
	catch(Exception w)
	{
		try{
			
			exitGracefully();
			return;
		}
		catch(Exception y)
		{
			System.out.println("Could not exit gracefully");
			return;
		}
		
	}

	
}

/**
 * Send keep alive message to peer

 * @return
 */
 public void keepAlive()
 {
         try{
                 Message.sendKeepAliveMessage(keepAlive, output);
                 return;
         }
         catch(IOException e)
         {
         System.err.println("Cannot keep alive");
         return;
         }
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
					pieceIndex = input.readInt();
							
					pieceOffset = input.readInt();
					
					byte[] block ;
					
					if(pieceIndex == piecesHash.length-1){
					block = new byte[totalBytes-((piecesHash.length-1)*pieceLength)];
					}
					else{
						block =  new byte[pieceLength];
					}
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
		
		
		public void requestPiece() throws IOException
		{
				if(!Thread.currentThread().isInterrupted())
				{
					if(firstRequest) 
					{
							//sending started message to the tracker
							tracker.setEvent("started");
							tracker.downloaded = getBytesDownloaded();
							tracker.left = tracker.left - getBytesDownloaded();
							tracker.connectTracker();
							firstRequest = false;
					}
					
					//take get bitfields for Peers that have sent a bitfield message
					ArrayList<boolean[]> peerBitfields = new ArrayList<boolean[]>();
					int[] k = pieceRarity;		
						for(Peer p :PeerList.getPeerList()){
							if(p.getPeerBF() != null)
								peerBitfields.add(p.getPeerBF());
								
						}
					
					
					// If no peers have sent any bitfield messages, request the first missing piece					
					if(peerBitfields.isEmpty()){
					for(int j = 0; j < bitfield.length; j++) //Checks for missing pieces to see which piece to request
					{
							if(peerbf[j] == true && getBitfield(j) == false){ 
							updateIndex(j);
							break;							
						}
						
					}
					}
					else{
					// Otherwise get the rarest piece
					calcRarities(peerBitfields);
					updateIndex(rarestPieceIndex());
					}
					
					
					if(index == piecesHash.length - 1)  //Last piece
					{	
						Message.Request requestPiece = new Message.Request(getIndex(), 0, (totalBytes-((piecesHash.length-1)*pieceLength)));
						Message.sendMessage(requestPiece, output);
						System.out.println("Sent Request for Piece");
					}
					else 
					{
						Message.Request requestPiece = new Message.Request(getIndex(), 0, pieceLength);
						Message.sendMessage(requestPiece, output);
						System.out.println("Sent Request for Piece");
					}
			 }			
			
			
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

	//Decodes the length of the next incoming message
	public  int decodeIncomingMessage() throws IOException{
		
			
				int length = input.readInt();
				if(length == 0) //keep alive
				{
					return 0;//dont do anything
				}
				else 
					return length;
	}
	
	//Reads in the bitfield sent from the remote peer
	public void decodeBitfieldMessage(int length) throws IOException
	{
		byte[] bf;
		bf = new byte[length - 1]; 
		input.readFully(bf);
		peerbf = byteArray2BitArray(bf);
		
		//System.out.print(peerbf.length);
				
	
		for(int j = 0; j < bitfield.length; j++)
		{
			//System.out.println("Comparing Bitfields");

			if(peerbf[j] == true && getBitfield(j) == false){ //METHOD
				interested();
				System.out.println("Sent Interest");
				break;
							
			}	
		}
	}
	
	//Decodes a peers request message
	public void decodeRequestMessage() throws IOException
	{
		int ind = input.readInt();
		//System.out.println(ind);
		int begin = input.readInt();
		//System.out.println(begin);
		int length = input.readInt();
		//System.out.println(length);
		//System.out.print(getBitfield(ind));
		if(getBitfield(ind) == true)
		{
	
			sendPiece(ind, begin, length);
		}
		else
		{
				System.out.print("Dont have requested piece");
		}
		return;
		
	}
	
	//Decodes a peers Have message, updates peer's bitfield
	public void decodeHaveMessage() throws IOException
	{
		int ind = input.readInt();
		//System.out.println(ind);
		if (ind >-1 && ind < peerbf.length)
		peerbf[ind] = true;
		
	}
		
	
	//Sends the request piece to the remote peer
	public void sendPiece(int ind, int begin, int length) throws IOException
	{
		
		byte[] block = getPieceFromFile(ind, begin, length);
		Message piece = new Message.Piece(ind, begin, block);
		Message.sendMessage(piece, output);
		updateBytesUploaded(length);
		System.out.print("Sent piece to Peer: ");
		System.out.println(IP);
		return;
	}
	
	//Gets the piece requested from the remote peer
	public synchronized byte[] getPieceFromFile(int ind, int begin, int length) throws IOException
	{
		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(savedFile));
					
		byte[] block = new byte[length];
		long offset = (pieceLength * ind) + begin;	
		reader.skip(offset);		
		reader.read(block,0 ,length);
		reader.close();
		return block;
		
	}
	
	//decodes a remote peers piece message	
	public void decodePieceMessage() throws IOException
	{
		byte[] piece = extractPiece();
		System.out.print("Extracted Piece from Peer: ");
		System.out.println(IP);
		boolean verify = verifyHASH(piece, pieceIndex);
		if(verify)
		{
			if(piece == null)
			{
				System.out.print("IT BROKE");
			}
			
			writePieceToFile(piece);
			
			updateBitfield(getIndex(), true);
			System.out.print("Bitfield index updated: "+getIndex());
			//System.out.print(getIndex());
			//System.out.print(bitfield[getIndex()]);
			//Send have message to all peers
			sendHavesToPeers(getIndex()); 	
			
			if(index == piecesHash.length-1)
				updateBytesDownloaded(totalBytes-((piecesHash.length-1)*pieceLength));
			else
				updateBytesDownloaded(pieceLength); 
			
			//offset += pieceLength;						
			if(getBytesDownloaded() == totalBytes)//sending completed message to the tracker if last piece
			{
				tracker.setEvent("completed");
				tracker.downloaded = getBytesDownloaded(); 
				tracker.left = tracker.left - getBytesDownloaded();
				tracker.connectTracker();
				
			}
		}
		else
		{
			System.out.println("Piece Hash not Verified");
		}
			
	}

	
	//converts Byte array to a boolean array
	public static boolean[] byteArray2BitArray(byte[] bytes) {
	    boolean[] bits = new boolean[bytes.length * 8];
	    for (int i = 0; i < bytes.length * 8; i++) {
	      if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0)
	        bits[i] = true;
	    }
	    return bits;
	  }
	
	  
	//converts boolean array to a Byte array
	 public byte[] booleanArray2ByteArray(boolean[] input) {
		 
			byte[] bf = new byte[input.length/8];
			
			int k = bf.length;
			
			for (int i = 0; i < bf.length; i++) {
				for (int j = 0; j < 8; j++) {
					if (input[i * 8 + j]) {
					bf[i] |= (128 >> j);
				}
			}	
		}
			
			
		return bf;
	 }	 
	 
	 /**
	  * @return the index of the calculated rarest piece
	  */
	 public int rarestPieceIndex(){
		 
		 int min = -1;
		 
		 if (pieceRarity != null){
			 min = pieceRarity[0];
		 }
		 
		 //goes through rarities to find the smallest number of occurrences
		 for(int i = 0; i < pieceRarity.length; i++){
			 if (min > pieceRarity[i]){
				 min = pieceRarity[i];
			 }
		 }
		 
		 if (min == -1)
			 return -1;
		 
		 //list of all rare pieces
		 ArrayList<Integer> matchedIndexes = new ArrayList<Integer>();
		 
		 //if we don't have the piece, and the piece is rare, add it to matchedIndexes
		 for (int i = 0; i < pieceRarity.length; i++){
			 
			 if (getBitfield(i) == false && this.peerbf[i] == true){
				 if (min == pieceRarity[i])  
					 matchedIndexes.add(i);
			 }
		 }
		 //choose a random rare piece
		 Random r = new Random();
		 int ind = r.nextInt(matchedIndexes.size());
		 
		 return matchedIndexes.get(ind); 
	 }
	 
	 
	 /* Getter and Setter Methods for Private variables*/
	 public static synchronized void updateBitfield(int j, boolean value)
	 {
		 bitfield[j] = value;
		 return;
	 }
	 
	 public static synchronized boolean getBitfield(int j)
	 {
		 return bitfield[j];
	 }
	 
	 public synchronized void updateBytesDownloaded(int amount)
	 {
		 bytesDownloaded += amount;
		 return;
	 }
	 
	 public synchronized int getBytesDownloaded()
	 {
		 return bytesDownloaded;
	 }
	 
	  public synchronized void updateBytesUploaded(int amount)
	 {
		 bytesUploaded += amount;
		 return;
	 }
	 
	 public synchronized int getBytesUploaded()
	 {
		 return bytesUploaded;
	 }
	 
	 	 
	 public synchronized void updateIndex(int value)
	 {
		 index = value;
		 return;
	 }
	 
	 public synchronized int getIndex()
	 {
		 return index;
	 }
	 
	 public synchronized void bitfieldInitializedTrue()
	 {
		 bitfieldInitialized = true;
		 return;
	 }
	 
	 	 public synchronized boolean getBitfieldInitialized()
	 {
		 return bitfieldInitialized;
	 }
	 
	 public synchronized boolean[] getPeerBF(){
		 return this.peerbf;
	 }
	 
	 public synchronized static boolean[] ourBitfield(){
		 return bitfield;
	 }
	 
	 
	//Writes a piece to the file
	 
	public synchronized void writePieceToFile(byte[] block) 
	{
			try{
				
				RandomAccessFile out = new RandomAccessFile(savedFile, "rw");
				out.seek((pieceIndex*pieceLength)+pieceOffset);
				//FileOutputStream out = new FileOutputStream(savedFile, true);
				out.write(block); //This should be changed to write piece at correct offset, just incase downloading out of order TBC
				out.close();
			}
			catch(IOException e)
			{
				System.err.println("Cannot write to file");
				return;
			}
			return;
			
	}
		
	//Sends a have message to all peers for a given piece
	 public void sendHavesToPeers(int ind) throws IOException{
		 
		 
		for(Peer p : PeerList.getPeerList()){
			
			Message have = new Message.Have(ind);
			Message.sendMessage(have, p.output);
		}
	 }
	 
	 //Calculates the interval time between announce requests
	 private int announceInterval(){
		 
		//sets interval time
			int interval = tracker.interval;
	
			if (tracker.min_interval == -1){
				tracker.min_interval = tracker.interval/2;
			}
			
			interval = tracker.interval*1000;
			
			return interval;
	 }
	 
		/**
		 * Gets the the rarity for each piece
		 */
		public static void calcRarities(ArrayList<boolean[]> peerBitfields){
			
			Arrays.fill(pieceRarity, 0); 
			
			for(boolean[] peerBF : peerBitfields){					
				boolean[] ourBF = ourBitfield();
				for(int i = 0; i < pieceRarity.length; i++){	
					if (peerBF[i] == true){
						pieceRarity[i] += 1;
						if (ourBF[i] == true) //if we already have the piece, set a high rarity
						pieceRarity[i] = 20;	
					}
				}
			}
		}
		

	
	//Initializes the Bitfield
	public static void initializeBitfield(Tracker t)

		{
			//Makes sure that the bitfield length is divisible by 8, so that it can be converted to bytes
			int i;
			int k = t.piecesHash.length%8;
			if(k!=0)
			{
				k = t.piecesHash.length + (8-k);
			}
			bitfield = new boolean[k];//make this the number of pieces
			for(i = 0; i< bitfield.length; i++)
			{
				updateBitfield(i, false);
				
			} 
			
	}
	
	//Exit method for the threads	
	public void exitGracefully() 
	{
		try
		{
		//exiting gracefully
		
		//Print out how much time it took to download
        endTime = System.nanoTime();
        double secondsTaken = (double)(endTime - startTime) / 1000000000.0;
        System.out.print("It took " +secondsTaken + "seconds for this download/upload session with Peer: ");
        System.out.println(IP);
        
		System.out.print("Closing Connection with peer ");
		System.out.println(IP);
		tracker.setEvent("stopped");
		tracker.downloaded = getBytesDownloaded();
		tracker.left = totalBytes - getBytesDownloaded();
		tracker.connectTracker();
		
		input.close();
		output.close();
		s.close();
		
		if(timer!= null)
		{	timer.cancel();
			timer.purge();
		}
		if(announceTimer!= null)
		{	announceTimer.cancel();
			announceTimer.purge();
		}
		System.out.print("Closed Connection with peer ");
		System.out.println(IP);
		return;
	}
	catch(Exception e)
	{
		System.out.println("Something broke");
		return;
	}
	}
			 

	
} 


