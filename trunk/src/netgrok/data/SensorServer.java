package netgrok.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;

public class SensorServer extends Thread{

	ServerSocket sock;
	
	Data data;
	
	public SensorServer(int port, Data data)
	{
		this.data = data;
		try {
			sock = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Failed to create socket");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args)
	{
		SensorServer s = new SensorServer(1234,Data.getData());
		s.run();
	}

	public void run() {
		while(true)
		{
			try {
				System.out.println("Listening for new connection");
				Socket client = sock.accept();
				System.out.println("Accepted connection from client "+client.getRemoteSocketAddress().toString());
				Thread t = new Thread(new SensorSocketHandler(client,this));
				t.start();
			} catch (IOException e) {
				System.out.println("Failed to listen on socket.");
			}
		}
	}
	
	public synchronized void addPackets(Vector<SimplePacket> packets)
	{
		Iterator<SimplePacket> i = packets.iterator();
		while(i.hasNext())
		{
			SimplePacket p = i.next();
			//System.out.println("Saw packet from "+p.src_ip+" to "+p.dst_ip+" at time "+p.time);
			data.addPacket(p);
		}
	}
	
	public class SensorSocketHandler implements Runnable
	{
		Socket client;
		SensorServer server;
		public SensorSocketHandler(Socket client, SensorServer server)
		{
			this.client = client;
			this.server = server;
		}

		public void run() {
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(client.getInputStream());
			} catch (IOException e) {
				System.out.println("Could not create object input stream");
				e.printStackTrace();
				System.exit(1);
			}
			try {
				while(true)
				{
					Object o = ois.readObject();
					try{
						Vector<SimplePacket> packets = (Vector<SimplePacket>) o;
						server.addPackets(packets);	
					}
					catch(ClassCastException e)
					{
						System.out.println("Received some other object!");
						e.printStackTrace();
						System.exit(1);
					}
				}
			} catch (IOException e) {
				try {
					Thread.currentThread().join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
