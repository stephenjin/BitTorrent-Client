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

public class Message{
	
	/*The various message types that can be sent and their byte[] encoding*/
	public static final byte[] keepAliveLength = {0,0,0,0};
	public static final byte[] have = {0,0,0,5};
	public static final byte[] request = {0,0,0,13};
	
	

	//public Message have = new Message({0,0,0,5},4 );
	
	private byte[] length;
	private int ID;
	
	/*The basic message*/
	public Message(byte[] len, int id){
		this.length = len;
		this.ID = id;
		
	}
	
	public byte[] makePayload() throws IOException
	{
		return null;
	}
	
	/*A message of the have type*/
	public static class Have extends Message{
		
		public final int index;
		
		public Have(int index) {
			super(have, 4);
			this.index = index;
		}
		
		public byte[] makePayload (){
			
			return ByteBuffer.allocate(4).putInt(index).array();
		}
	}
	
	/*A message of the request type*/
	public static class Request extends Message{
		
		public final int index;
		public final int begin;
		public final int length;
		
			
		public Request(int index, int begin, int length) {
			super(request, 6);
			
			this.index = index;
			this.begin = begin;
			this.length = length;
		}
			
			public byte[] makePayload () throws IOException{ //constructs the payload in the form of a byte array
				
			ByteArrayOutputStream output = new ByteArrayOutputStream( );
			output.write(ByteBuffer.allocate(4).putInt(index).array());
			output.write(ByteBuffer.allocate(4).putInt(begin).array());
			output.write(ByteBuffer.allocate(4).putInt(length).array());
			
			return output.toByteArray();
			}
			
	}
		
	
	/*A message of the piece type*/
	public static class Piece extends Message{
		
		public final int index;
		public final int start;
		public final byte[] block;
		
		public Piece(int index,int start, byte[] block) {
			super(ByteBuffer.allocate(4).putInt(9 + block.length).array(), 7);
			
			this.index = index;
			this.start = start;
			this.block = block;
			
		}
		
		public byte[] makePayload() throws IOException{ //constructs the payload in the form of a byte array
			
			ByteArrayOutputStream output = new ByteArrayOutputStream( );
			output.write(ByteBuffer.allocate(4).putInt(index).array());
			output.write(ByteBuffer.allocate(4).putInt(start).array()); 
			output.write(block); 
			return output.toByteArray();

		}
	}
	
	
	/*A message of the bitfield type*/
	public static class Bitfield extends Message {
			
		public final byte[] bitfield;
			
		public Bitfield(final byte[] bf){
			super(ByteBuffer.allocate(4).putInt(1 + bf.length).array(), 5);
			this.bitfield = bf;
			
			}
			
		public byte[] makePayload() throws IOException{ //constructs the payload in the form of a byte array
				
			return bitfield;
				
			}	
		}
		
	
	           
	public static void sendMessage(Message m, DataOutputStream output) throws IOException{
		
		if (m != null && output != null){
			
			
			if(m.makePayload() == null)
			{
				byte[] mess = new byte[m.length.length + 1]; 
				System.arraycopy(m.length, 0, mess, 0, m.length.length);
				
				mess[4]= (byte)m.ID;
				output.write(mess);
			}
			else
			{
				byte[] mess = new byte[m.length.length + 1 + m.makePayload().length]; 
				System.arraycopy(m.length, 0, mess, 0, m.length.length);
				mess[4]=(byte)m.ID;
				System.arraycopy(m.makePayload(), 0, mess, m.length.length+1, m.makePayload().length);
				output.write(mess);
			}
			
			return;
			
		}
	}
	
	public  static void sendKeepAliveMessage(Message m, DataOutputStream output) throws IOException{

			output.write(keepAliveLength);
			return;
			
		}


}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

