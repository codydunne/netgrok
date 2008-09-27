package netgrok.data;

import java.io.IOException;
import java.util.Vector;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.PacketReceiver;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

public class PcapReader{

	public static void readFile(String filename) {
		
		System.out.println("Reading file "+filename);
		
		JpcapCaptor jpcap = null;
		// third argument is true for promiscuous mode
		try {
			jpcap = JpcapCaptor.openFile(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Could not read pcap file "+filename);
			e.printStackTrace();
		}
		
		// we only consider network layer traffic
		// this of course means we don't see things like ARP poisoning
		try {
			jpcap.setFilter("ip", true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error in setting packet capture filter");
			e.printStackTrace();
		}
		
		final Data d = Data.getData();
		final Vector<SimplePacket> packet_set = new Vector<SimplePacket>();
		jpcap.loopPacket(-1, new PacketReceiver(){
			public void receivePacket(Packet p)
			{
				if(p instanceof IPPacket)
				packet_set.add(new SimplePacket((IPPacket)p));
			}
		});
		d.addPackets(packet_set.iterator());
	}
	
	public static void main(String[] args)
	{
		Data d = Data.getData();
		readFile("data/trace.pcap");
		System.out.println("Data now has "+d.getIPGraph().getNodeCount()+" nodes!");
	}
}
