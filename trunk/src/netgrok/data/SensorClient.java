package netgrok.data;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.PacketReceiver;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

public class SensorClient extends Thread implements PacketReceiver {
	
	// singleton
	
	private Vector<SimplePacket> packets;
	private final int chunk_size = 50;
	
	// the data itself
	JpcapCaptor jpcap = null;
	
	Socket server;
	ObjectOutputStream oos;
		
	public SensorClient() {
		packets = new Vector<SimplePacket>();
		try {
			server = new Socket("localhost",1234);
			oos = new ObjectOutputStream(server.getOutputStream());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println("Unknown host!");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not connect to host");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void sendPackets()
	{
		// TODO: for some reason the same set of packets is sent everytime!
		try {
			//System.out.println("Sending packet vector");
			oos.reset();
			oos.writeObject(packets);
			//System.out.println("Packet vector sent");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error sending packet vector");
			e.printStackTrace();
			System.exit(1);
		}
		packets.clear();
	}
	
	private void addPacket(SimplePacket p)
	{
		packets.add(p);
		if(packets.size() >= chunk_size)
		{
			sendPackets();
		}
	}
	
	private void flush()
	{
		if(packets.size() > 0)
		{
			sendPackets();
		}
	}
	
	public void sniff(String device_name) {		
		// watch for packets in another thread
		// fill tables and graphs
		NetworkInterface[] devices = JpcapCaptor.getDeviceList();

		if (devices.length == 0) {
			System.out.println("Error: no devices found (try running again as root)");
			System.exit(1);
		}
		
		// we need to get the device name from the GUI
		int device_number = -1;
		
		for (int i=0; i<devices.length; i++)
			if (devices[i].name.equals(device_name))
				device_number = i;
		
		if (device_number == -1) {
			System.out.println("Error: device " + device_name + " not found");
			System.exit(1);
		}
		
		System.out.println("Sniffing device "+device_name);
		
		try {
			// third argument is true for promiscuous mode
			jpcap = JpcapCaptor.openDevice(devices[device_number], 2000, true, 20);
			
			// we only consider network layer traffic
			// this of course means we don't see things like ARP poisoning
			jpcap.setFilter("ip", true);
		}
		
		catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			System.exit(1);
		}
	
		start();
	}
	
	// THIS SHOULD NOT BE RUN FROM OUTSIDE THIS CLASS!
	public void run() {
		jpcap.loopPacket(-1, this);
	}
	
	// adds new packets to the database and ages off ones
	public void receivePacket(Packet p) {
		if(p instanceof IPPacket)
			addPacket(new SimplePacket((IPPacket)p));
		else
		{
			// TODO
		}
	}

	public static void main(String[] args)
	{
		SensorClient sensor = new SensorClient();
		sensor.sniff("en1");
	}
}
