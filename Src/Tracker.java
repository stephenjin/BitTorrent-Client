


/*Julia Sutula, Stephen Jin, Sehaj Singh
	Phase 1 Bit Torrent Client
*/
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



public class Tracker
{

	public byte[] infohash;
	public ByteBuffer[] piecesHash;
	public int pieceLength;
	public int fileLength;
	private static byte[] peerid;
	private int port;
	private URL announce;
	private int filesize;
	public int left;
	private String event;
	public int uploaded;
	public int downloaded;
	private URL requestString;
	public int interval;
	private String fileName;
	
	public static ArrayList<String> peers = new ArrayList<String>();
	
	/**
	 * Key that says why the request string failed.
	 */
	public static final ByteBuffer KEY_FAILURE = ByteBuffer.wrap(new byte[] {
			'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o',
			'n' });

	/** 
	 * Key used to retrieve a dictionary of peers.
	 */
	public static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's' });
	
	/**
     * Key used to retrieve the peerid.
     */
	public static final ByteBuffer KEY_PEERID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' });

	/**
     * Key used to retrieve peer's ip.
     */
	public static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] { 'i',
			'p' });

	/**
     * Key used to retrieve peer's port.
     */
	public static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] { 'p',
			'o', 'r', 't' });

	/**
     * Key used to retrieve the interval.
     */
	public static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {
			'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });
	

	//Inititalizes a tracker
   public Tracker(TorrentInfo ti, String file) throws Exception {
		infohash = ti.info_hash.array();		
		port = ti.announce_url.getPort();
		event = null;
		filesize = ti.file_length;
		this.left = filesize;
		String peerIdString = makePeerID();
		setPeerid(peerIdString.getBytes());
		setPeerid(peerIdString.getBytes(Charset.forName("UTF-8")));
		piecesHash = ti.piece_hashes;
		pieceLength = ti.piece_length;
		fileLength = ti.file_length;
		
		announce = ti.announce_url;
		requestString = createURL(ti.announce_url);
		fileName = file;
		
	}
   
   //creates a Tracker announce request url
   private URL createURL(URL announce_url) throws Exception {
	   
	   String infoHash = "?info_hash="+hexToEncodeURL(bytesToHex(infohash));
	   String peerID = "&peer_id="+hexToEncodeURL(bytesToHex(getPeerid()));
	   String Port = "&port="+port;
	   String uploaded = "&uploaded="+this.uploaded;
	   String downloaded = "&downloaded="+this.downloaded;
	   String left = "&left="+this.left;
	   event = "started";
	   String Event = "&event="+event;
	   
	   String url = announce_url.toString()+infoHash+peerID+Port+uploaded+downloaded+left;
	   if(event != null)
		   url = url + Event;
	   
	   try {
			return new URL(url);
		} catch (MalformedURLException e) { //ensures that url is a valid format
			return null;
		}
	   
   }

   //sends an HTTP GET request to the tracker
   public byte[] connectTracker() throws IOException{
	   byte[] tresponse = null;
	   int responseSize;
	   HttpURLConnection trackerRequest = (HttpURLConnection)requestString.openConnection();
	   trackerRequest.setRequestMethod("GET");
	   
	   DataInputStream response = new DataInputStream(trackerRequest.getInputStream());
	   
	   responseSize = trackerRequest.getContentLength();
	   tresponse = new byte[responseSize];
	   
	   response.read(tresponse);
	   response.close();
		
	   return tresponse;
   }
   
	
	@SuppressWarnings("unchecked")
	public ArrayList<Peer> decodeTracker(byte[] tresponse) throws BencodingException{
		Map<ByteBuffer, Object> responseMap;   
		ArrayList<Peer> peerslist = new ArrayList<Peer>(); //Arraylist of peer objects
		
		//check for valid response
		if (tresponse == null || tresponse.length == 0){
			System.out.println("No response from tracker.");
			return null;
		}
		//Assign map of the tracker response
		responseMap = (Map<ByteBuffer, Object>) Bencoder2.decode(tresponse);
		
		if (responseMap.containsKey(KEY_FAILURE)){
			System.out.println("Key failure.");
			return null;
		}
		
		//extract interval
		this.interval = (int) responseMap.get(KEY_INTERVAL);
		
		//extract list of peers
		List<Map<ByteBuffer,Object>> peers = (List<Map<ByteBuffer,Object>>) responseMap.get(KEY_PEERS);
		
		if(peers == null){
			System.out.println("List of peers is null.");
			return null;
			
		}
		
		for(Map<ByteBuffer, Object> currentPeer : peers){
			
			byte[] peerID = ((ByteBuffer) currentPeer.get(KEY_PEERID)).array(); //converts bytebuffer to byte array
			int port = (int) currentPeer.get(KEY_PORT);
			String ip = null;
			
			try{
			ip = new String(((ByteBuffer) currentPeer.get(KEY_IP)).array(), "UTF-8");
			}
			catch (UnsupportedEncodingException e){
				System.out.print("Unsupported encoding on the IP.");  //check for correct encoding 
				continue; 
			}
			
			peerslist.add(new Peer(peerID, ip, port, infohash, peerid, piecesHash, pieceLength, fileName, fileLength, Tracker.this));
		}
		
		return peerslist;
	 
	}

    public static String makePeerID() 
    {
		StringBuilder sb = new StringBuilder("abcdefghij1234567890");
		Random rand = new Random();
		for (int i = sb.length() - 1; i > 1; i--) 
		{
			int swapWith = rand.nextInt(i);
			char tmp = sb.charAt(swapWith);
			sb.setCharAt(swapWith, sb.charAt(i));
			sb.setCharAt(i, tmp);
		}		
		
		String s = sb.toString();
		
		if(UniquePeerID(s))
		{
			peers.add(s);
		}		
		else
		{
			s = makePeerID();
		}
		return s;
		

		
	}
	
	public static boolean UniquePeerID(String peerid)
	{	
		
		if(peers.contains(peerid))
		{
			return false;
		}
		else
		{	
			return true;
		}
		
	}
	
	//converts a byte array to a hex string
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	

		//converts hex string into a string that is url encoded
	public static String hexToEncodeURL(String hexString) throws Exception {	
		int hexLength = hexString.length();
		if(hexString==null || hexString.isEmpty() || hexLength%2 != 0){
			throw new Exception("There was a problem encoding this hex string to encoded URL: "+hexString);
		}				
		char[] encodeURL = new char[hexLength+hexLength/2];
		int i=0;
		int j=0;
		while(i<hexLength){
			encodeURL[j++]='%';
			encodeURL[j++]=hexString.charAt(i++);
			encodeURL[j++]=hexString.charAt(i++);
		}
		return new String(encodeURL);
	}
	
	//takes a byte array and encodes to SHA-1 encoded byte[] of 20 bytes.
	public static byte[] computeSHA1Hash(byte[] hashThis) {
       
        try {
			byte[] sha1Hash = new byte[20];
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            sha1Hash = md.digest(hashThis);
			return sha1Hash;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-1 algorithm is currently not obtainable");
            System.exit(1);
			return null;
        }
      
    }

	public static byte[] getPeerid() {
		return peerid;
	}

	public static void setPeerid(byte[] peerid) {
		Tracker.peerid = peerid;
	}
	
	public void setEvent(String k){
		event = k;
	}
	
}
