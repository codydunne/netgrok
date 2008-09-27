package test;

import netgrok.data.*;

import java.awt.geom.Point2D;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Class for simple IP hashing methods and point calculations
 * 
 * @author Cody Dunne
 *
 */
public class IPHash {

	public static String ipBytesToString(byte[] ip){
		return (ip[0] & 0xff) + "." + (ip[1] & 0xff) + "." + (ip[2] & 0xff) + "." + (ip[3] & 0xff);
	}

	public static byte[] ipAddrStringToBytes(String addr){

		String[] s = addr.trim().split("\\.");
		int val = 0;

		byte[] res = new byte[4];

		for (int i = 0; i < 4; i++) {
			val = Integer.parseInt(s[i]);
			res[i] = (byte) (val & 0xff);
		}

		return res;
	}

	public static byte[] ipAddrIntToBytes(int ipAddr){

		byte[] t = new byte[4];

		t[0] = (byte) ((ipAddr >>> 24) & 0xFF);
		t[1] = (byte) ((ipAddr >>> 16) & 0xFF);
		t[2] = (byte) ((ipAddr >>> 8) & 0xFF);
		t[3] = (byte) (ipAddr & 0xFF);

		return t;
	}

	public static int ipAddrBytesToInt(byte[] ipAddr){

		int t = 0;
		if (ipAddr != null) {
			if (ipAddr.length == 4) {
				t  = ipAddr[3] & 0xFF;
				t |= ((ipAddr[2] << 8) & 0xFF00);
				t |= ((ipAddr[1] << 16) & 0xFF0000);
				t |= ((ipAddr[0] << 24) & 0xFF000000);
			} 
		}
		return t;
	}

	public static int eightBytesToInt(byte[] hash){

		int t = 0;
		if (hash == null || hash.length != 8) {
			throw new IllegalArgumentException("Need 8 bytes");
		}

		t  = (hash[3] ^ hash[7]) & 0xFF;
		t |= (((hash[2] ^ hash[6]) << 8) & 0xFF00);
		t |= (((hash[1] ^ hash[5]) << 16) & 0xFF0000);
		t |= (((hash[0] ^ hash[4]) << 24) & 0xFF000000);

		return t;
	}

	public static int sixteenBytesToInt(byte[] hash){

		int t = 0;
		if (hash == null || hash.length != 16) {
			throw new IllegalArgumentException("Need 16 bytes");
		}
		/*t  = hash[7] & 0xFF;
				t |= ((hash[6] << 8) & 0xFF00);
				t |= ((hash[5] << 16) & 0xFF0000);
				t |= ((hash[4] << 24) & 0xFF000000);*/

		t  = (hash[3] ^ hash[7] ^ hash[11] ^ hash[15]) & 0xFF;
		t |= (((hash[2] ^ hash[6] ^ hash[10] ^ hash[14]) << 8) & 0xFF00);
		t |= (((hash[1] ^ hash[5] ^ hash[9] ^ hash[13]) << 16) & 0xFF0000);
		t |= (((hash[0] ^ hash[4] ^ hash[8] ^ hash[12]) << 24) & 0xFF000000);

		return t;
	}

	public static int ipToHash(String ip){
		try{
			byte[] res = SimpleMD5.MD5(ipAddrStringToBytes(ip));
			int ret = sixteenBytesToInt(res);
			return ret;
		} catch (NoSuchAlgorithmException e){
			throw new IllegalStateException("MD5 IP Hash Problem: " + e.getMessage());
		} catch (UnsupportedEncodingException e){
			throw new IllegalStateException("MD5 IP Hash Problem: " + e.getMessage());
		}
	}

	/**
	 * Class for simple MD5 calculation with simple modifications to code at:
	 * 
	 * http://www.anyexample.com/programming/java/java_simple_class_to_compute_md5_hash.xml
	 */
	public static class SimpleMD5 {

		public static String convertToHex(byte[] data) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < data.length; i++) {
				int halfbyte = (data[i] >>> 4) & 0x0F;
				int two_halfs = 0;
				do {
					if ((0 <= halfbyte) && (halfbyte <= 9))
						buf.append((char) ('0' + halfbyte));
					else
						buf.append((char) ('a' + (halfbyte - 10)));
					halfbyte = data[i] & 0x0F;
				} while(two_halfs++ < 1);
			}
			return buf.toString();
		}

		public static byte[] MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException  {
			return MD5(text.getBytes("iso-8859-1"));
		}

		public static byte[] MD5(byte[] text) throws NoSuchAlgorithmException, UnsupportedEncodingException  {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			byte[] md5hash = new byte[32];
			md.update(text, 0, text.length);
			md5hash = md.digest();
			return md5hash;//could call convertToHex here
		}
	}
}
