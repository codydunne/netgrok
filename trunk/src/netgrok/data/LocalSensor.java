package netgrok.data;

import java.io.IOException;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.PacketReceiver;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

public class LocalSensor extends Thread implements PacketReceiver {
	
	private boolean sniffing = false;
	private Data data;
	
	// the data itself
	JpcapCaptor jpcap = null;
	
	public LocalSensor() {
		data = Data.getData();
	}
	
	public boolean isSniffing() {
		return sniffing;
	}
	
	private void addPacket(SimplePacket p)
	{
		data.addPacket(p);
	}

	public static String[] getDeviceNames() {
		NetworkInterface[] devices = JpcapCaptor.getDeviceList();
		String[] names = new String[devices.length];
		for (int i=0; i<devices.length; i++)
			names[i] = devices[i].name;
		return names;
	}

	public void sniff(String device_name) {
		
		// watch for packets in another thread
		// fill tables and graphs
		sniffing = true;
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
		LocalSensor sensor = new LocalSensor();
		sensor.sniff("eth1");
	}
}
