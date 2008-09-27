package netgrok.data;

import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;

import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

public class SimplePacket implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final InetAddress src_ip, dst_ip;
	public final long time;
	public final boolean src_is_local, dst_is_local;
	public final int length;

	/*public SimplePacket(String src_ip, String dst_ip, boolean src_is_local, boolean dst_is_local, int length)
	{
		this.src_ip = src_ip;
		this.dst_ip = dst_ip;
		time = System.currentTimeMillis();
		this.src_is_local = src_is_local;
		this.dst_is_local = dst_is_local;
		this.length = length;
	}*/
	
	public SimplePacket(IPPacket p)
	{
		this.src_ip = p.src_ip;
		this.dst_ip = p.dst_ip;
		long packet_time = 1000 * p.sec + p.usec / 1000;
		//System.out.println("Packet time: "+DateFormat.getDateTimeInstance().format(new Date(packet_time)));
		time = packet_time;//System.currentTimeMillis();
		this.src_is_local = p.src_ip.isSiteLocalAddress();
		this.dst_is_local = p.dst_ip.isSiteLocalAddress();
		this.length = p.length;
	}
}
