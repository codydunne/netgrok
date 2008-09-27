package netgrok.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import prefuse.action.Action;
import prefuse.data.*;
import prefuse.data.event.TableListener;
import prefuse.data.expression.Predicate;
import prefuse.data.io.TreeMLReader;

public class Data{
	
	// TODO: change bandwidth ranking stuff
	// TODO: create a schema with constant columns for max/min values
	// TODO: add a schema
	// TODO: merge the edge_times and all_packets vectors
	
	public static final String ADDRESS = "address";
	public static final String BANDWIDTH = "bandwidth";
	public static final String BANDWIDTH_RANK = "bandwidth_rank";
	public static final String DEGREE = "degree";
	public static final String DEGREE_RANK = "degree_rank";
	public static final String LASTSEEN = "lastseen";

	// a groups home ip is the location the group get placed
	public static final String HOME_IP = "home_ip";
	public static final String GROUP = "group";
	public static final String GROUPS = "groups";
	public static final String GROUP_CLASS = "group_class";
	public static final String GROUP_SUBNETS = "subnets";
	public static final String FOREIGN_GROUP = "Foreign Network";
	public static final String IS_ZERO_BYTE = "is_zero_byte";
	
	public static final String TEMP_TIME_SELECTED = "temp_time_selected";
	public static final String TIME_SELECTED = "time_selected";
	public static final String HOST_ADDRESS = "host_address";
	
	public static final String HOSTNAME = "hostname";
	public static final String UNRESOLVED = "<unresolved hostname>";
	public static final String NO_HOSTNAME = "<no hostname found>";
	
	// singleton
	static private Data data = null;

	// the data itself
	private Tree groups = new Tree();
	private Graph ip_graph = new Graph();

	// data for saving and loading packets
	private Vector<SimplePacket> all_packets = new Vector<SimplePacket>();
	// functions to load and store packets
	public boolean saveAllPackets(File file)
	{
		System.out.println("Saving packets");
		ip_graph_lock.lock();
		boolean success = true;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			Iterator<SimplePacket> i = all_packets.iterator();
			while(i.hasNext())
			{
				//System.out.print(".");
				oos.writeObject(i.next());
			}
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success = false;
		}
		ip_graph_lock.unlock();
		return success;
	}
	
	public boolean loadPackets(File file)
	{
		try
		{
			FileInputStream fis = new FileInputStream(file);
			final ObjectInputStream ois = new ObjectInputStream(fis);
			addPackets(new Iterator(){
				Object o = null;
				boolean o_primed = false;
				boolean eof = false;
				public boolean hasNext() {
					if(eof)
						return false;
					if(o_primed == true)
						return true;
					try
					{
						o = ois.readObject();
						o_primed = true;
					}
					catch (EOFException e)
					{
						eof = true;
						return false;
					} catch (IOException e) {
						eof = true;
						return false;
					} catch (ClassNotFoundException e) {
						eof = true;
						return false;
					}
					return true;
				}

				public Object next(){
					o_primed = false;
					return o;
				}

				public void remove() {
					// TODO Auto-generated method stub
					// what does remove do, anyway?
				}
				
			});
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	// variables to deal with time bounds
	private long min_time = System.currentTimeMillis();
	private long max_time = System.currentTimeMillis();
	private long min_time_bound = -1;
	private long max_time_bound = -1;

	// accessors for time bounds
	public long getMinTime()
	{
		return min_time;
	}
	
	public long getMaxTime()
	{
		return max_time;
	}

	public long getMinTimeBound()
	{
		return min_time_bound;
	}
	
	public long getMaxTimeBound()
	{
		return max_time_bound;
	}
	
	public void setTimeBounds(long min, long max) {
		boolean update_time_selected = false;
		if(min_time_bound != min || max_time_bound != max)
			update_time_selected = true;
		min_time_bound = min;
		max_time_bound = max;
		if(update_time_selected)
		{
			updateTimeSelected();
		}
	}

	// data and functions dealing with the timing of individual packets
	// TODO: support sorting of edge times
	// TODO: support getting only a subset of edge times
	public class EdgeTime
	{
		public final long time;
		public final Edge edge;
		
		public EdgeTime(long time, Edge edge)
		{
			this.time = time;
			this.edge = edge;
		}
	}
	
	private Vector<EdgeTime> edge_times = new Vector<EdgeTime>();
	
	public Iterator<EdgeTime> getEdgeTimes()
	{
		return edge_times.iterator();
	}
	
	// functions to deal with locking the data objects
	private Lock ip_graph_lock = new ReentrantLock();

	public Lock getLock()
	{
		return ip_graph_lock;
	}
	
	public Action getLockAction()
	{
		return new LockAction(this);
	}
	
	public Action getUnlockAction()
	{
		return new UnlockAction(this);
	}
	
	// Map of nodes that have already been seen
	private HashMap<InetAddress,Node> node_map = new HashMap<InetAddress,Node>();

	public static class Group {
		InetAddress[] masks = null;
		InetAddress[] addresses = null;
		
		public Group(String subnet_string) {
			
			if (subnet_string == null)
				return;
			
			String[] subnets = subnet_string.split(",");
			this.masks = new InetAddress [subnets.length];
			this.addresses = new InetAddress [subnets.length];
			
			for (int i=0; i<subnets.length; i++) {
				String [] pieces = subnets[i].split("/");
				
				try {
					addresses[i] = InetAddress.getByName(pieces[0]);
					
					// compute the subnet mask address
					byte[] byte_mask = new byte[4];
					int slash = Integer.parseInt(pieces[1]);
					
					// compute the mask address 
					// there is probably a way easier way to do this
					for (int j=0; slash-- > 0; j++) {
						if (j<8)  byte_mask[0] += Math.pow(2,j);
						if (j<16) byte_mask[1] += Math.pow(2,j-8);
						if (j<24) byte_mask[2] += Math.pow(2,j-16);
						if (j<32) byte_mask[3] += Math.pow(2,j-24);
					}
					
					masks[i] = InetAddress.getByAddress(byte_mask);
				}
				
				catch (Exception e) {
					System.err.println("Error: " + subnets[i] + " is not correctly formatted.");
					System.exit(1);
				}
			}
		}
		
		public boolean contains(Node host) {
			if (addresses == null)
				return false;
			
			byte[] b_node = ((InetAddress)host.get(ADDRESS)).getAddress();
			
			check_all_ips:
			for (int i=0; i<addresses.length; i++) {
				byte[] b_addr = addresses[i].getAddress();
				byte[] b_mask = masks[i].getAddress();
				
				for (int j=0; j<4; j++) 
					if ((b_addr[j] & b_mask[j]) != (b_node[j] & b_mask[j])) 
						continue check_all_ips;

				return true;
			}
				
			return false;
		}
		
		public byte[] getHomeIP() {
			return addresses[0].getAddress();
		}
		
		// prints the slash representation of the IPs in this group
		public String toString() {
			String str = new String();
			
			for (int i=0; i<addresses.length; i++) {
				str += i == 0 ? "" : ", "; 
				str += addresses[i].getHostAddress() + "/" + masks[i].getHostAddress();
			}
			
			return str;
		}
	}

	// keep track of the min and max bandwidth and degree seen on nodes and edges
	private int max_node_bandwidth = 1;
	private int max_edge_bandwidth = 1;
	private int max_degree = 1;
	
	private Data() {
		ip_graph.addColumn(ADDRESS, InetAddress.class, null);
		ip_graph.addColumn(GROUPS, String.class, null);
		ip_graph.addColumn(BANDWIDTH, int.class, 0);
		ip_graph.addColumn(DEGREE, int.class, 1);
		ip_graph.addColumn(LASTSEEN, long.class, 0);
		ip_graph.addColumn(BANDWIDTH_RANK, double.class, 0);
		ip_graph.addColumn(DEGREE_RANK, double.class, 0);
		
		ip_graph.addColumn(TEMP_TIME_SELECTED, boolean.class, false);
		ip_graph.addColumn(TIME_SELECTED, boolean.class, false);
		ip_graph.addColumn(IS_ZERO_BYTE, boolean.class, true);
		ip_graph.addColumn(HOST_ADDRESS, String.class, "");
		ip_graph.addColumn(HOSTNAME, String.class, UNRESOLVED);
		
		// read in groups
		try {
			groups = new GroupFileReader().readGroups("groups.ini");
		}
		catch (FileNotFoundException ioe) {
			System.err.println("Error: could not read groups.ini "+
				"(" + ioe.getMessage() + ")");
			System.out.println(System.getProperty("user.dir"));
			ioe.printStackTrace();
			System.exit(1);
		}
		
		groups.addColumn(GROUP_CLASS, Group.class, null);
		groups.addColumn(HOME_IP, byte[].class);
		
		// instantiate the Group class at each node
		for (Iterator<?> it=groups.nodes(); it.hasNext();) {
			Node group = (Node) it.next();		
						
			// add all the children's subnets to the parent
			// we need to do this. think about it. 
			Group group_class = new Group(getAllChildSubnets(group));
			group.set(GROUP_CLASS, group_class);
			
			// set the home ip for this group
			group.set(HOME_IP, group_class.getHomeIP());
		}
	}
	
	public int getMaxNodeBandwidth() {
		return max_node_bandwidth;
	}

	public int getMaxNodeDegree() {
		return max_degree;
	}

	// returns a valid subnet string for all children to the given node
	// or null if there are no subnets that match this group
	public String getAllChildSubnets(Node group) {
		String groups = group.getString(GROUP_SUBNETS);
		
		for (Iterator<?> it=group.children(); it.hasNext();) {
			String child_subnets = getAllChildSubnets((Node)it.next());
			
			if ( child_subnets != null ) { 
				if (groups == null)
					groups = child_subnets;
				else
					groups += "," + child_subnets;
			}
		}
		
		return groups;
	}
	
	public synchronized static Data getData() {
		if (data == null)
			data = new Data();
		
		return data;
	}
	
	public Graph getIPGraph() {
		return ip_graph;
	}
	
	public Tree getGroups() {
		return data.groups;
	}
	
	private String getGroups(Node host) {
		String grp_str = getGroups(groups.getRoot(), host);
		return grp_str == null ? FOREIGN_GROUP : grp_str; 
	}
	
	// propagates the GROUPS field with all groups that
	// this host belongs to
	private String getGroups(Node group, Node host) {		
		String grp_str = null;
		
		if (((Group) group.get(GROUP_CLASS)).contains(host)) {
			grp_str = group.getString(GROUP);
			
			// this node is eligible to be in children groups
			for (Iterator<?> it=group.children(); it.hasNext();){
				String child = getGroups((Node)it.next(), host);
				if (child != null) grp_str += "," + child; 
			}
		}
		
		return grp_str;
	}

	// re-ranks nodes by bandwidth usage
	private void reRankByBandwidth(Node host) {
		int bandwidth = host.getInt(BANDWIDTH);
		if(bandwidth > max_node_bandwidth)
		{
			max_node_bandwidth = bandwidth;
			//bandwidth_ranking_expression.setMaxNodeValue(max_node_bandwidth);
			for (Iterator<?> it=ip_graph.nodes(); it.hasNext();) {
				Node node = (Node)it.next();
				int band2 = node.getInt(BANDWIDTH);
				node.setDouble(BANDWIDTH_RANK, (double)band2/(double)max_node_bandwidth);
			}
			updateFilterColumn();
		}
		else
		{
			host.setDouble(BANDWIDTH_RANK,(double)bandwidth/(double)max_node_bandwidth);
		}
	}
	
	private void reRankByBandwidth(Edge e) {
		int bandwidth = e.getInt(BANDWIDTH);
		if(bandwidth > max_edge_bandwidth)
		{
			max_edge_bandwidth = bandwidth;
			//bandwidth_ranking_expression.setMaxEdgeValue(max_edge_bandwidth);
			for (Iterator<?> it=ip_graph.edges(); it.hasNext();) {
				Edge edge = (Edge)it.next();
				int band2 = edge.getInt(BANDWIDTH);
				edge.setDouble(BANDWIDTH_RANK, (double)band2/(double)max_edge_bandwidth);
			}
			updateFilterColumn();
		}
		else
		{
			e.setDouble(BANDWIDTH_RANK,(double)bandwidth/(double)max_edge_bandwidth);
		}
	}

	// re-ranks nodes by degree usage
	private void reRankByDegree(Node host) {
		int degree = host.getDegree();
		if(degree > max_degree)
		{
			max_degree = degree;
			//degree_ranking_expression.setMaxNodeValue(max_degree);
			for (Iterator<?> it=ip_graph.nodes(); it.hasNext();) {
				Node node = (Node)it.next();
				node.setDouble(DEGREE_RANK, (double)node.getDegree()/(double)max_degree);
			}
			updateFilterColumn();
		}
		else
		{
			host.setDouble(DEGREE_RANK,(double)degree/(double)max_degree);
		}
	}
	
	private void reRankAllNodesByBandwidth() {
		max_node_bandwidth = 1;
		for(Iterator<?> it = ip_graph.nodes(); it.hasNext();)
		{
			int bandwidth = ((Node)it.next()).getInt(BANDWIDTH);
			if(bandwidth > max_node_bandwidth)
			{
				max_node_bandwidth = bandwidth;
			}
		}
		for (Iterator<?> it=ip_graph.nodes(); it.hasNext();) {
			Node node = (Node)it.next();
			int bandwidth = node.getInt(BANDWIDTH);
			node.setDouble(BANDWIDTH_RANK, (double)bandwidth/(double)max_node_bandwidth);
		}
	}
	
	private void reRankAllEdgesByBandwidth() {
		max_edge_bandwidth = 1;
		for(Iterator<?> it = ip_graph.edges(); it.hasNext();)
		{
			int bandwidth = ((Edge)it.next()).getInt(BANDWIDTH);
			if(bandwidth > max_edge_bandwidth)
			{
				max_edge_bandwidth = bandwidth;
			}
		}
		for (Iterator<?> it=ip_graph.edges(); it.hasNext();) {
			Edge edge = (Edge)it.next();
			int band2 = edge.getInt(BANDWIDTH);
			edge.setDouble(BANDWIDTH_RANK, (double)band2/(double)max_edge_bandwidth);
		}
	}

	// re-ranks nodes by degree usage
	private void reRankAllNodesByDegree() {
		max_degree = 1;
		for(Iterator<?> it = ip_graph.nodes(); it.hasNext();)
		{
			Node node = (Node)it.next();
			int degree = node.getInt(DEGREE);
			if(degree > max_degree)
			{
				max_degree = degree;
			}
		}
		for (Iterator<?> it=ip_graph.nodes(); it.hasNext();) {
			Node node = (Node)it.next();
			node.setDouble(DEGREE_RANK, (double)node.getDegree()/(double)max_degree);
		}
	}
	
	public Node createNewNode(InetAddress address, boolean is_local, long time, int length)
	{
		Node new_node = ip_graph.addNode();
		new_node.set(ADDRESS, address);
		new_node.set(HOST_ADDRESS, address.getHostAddress());
		new_node.set(BANDWIDTH, length);
		new_node.set(LASTSEEN, time);
		String group_string = getGroups(new_node);
		new_node.setString(GROUPS, group_string);
		node_map.put(address, new_node);
		return new_node;
	}
	
	public Edge createNewEdge(Node src_node, Node dst_node, SimplePacket p)
	{
		Edge e = ip_graph.addEdge(src_node, dst_node);
		e.set(LASTSEEN, p.time);
		e.set(BANDWIDTH, p.length);
		return e;
	}
	
	public void addPacket(SimplePacket p)
	{
		ip_graph_lock.lock();
		all_packets.add(p);
		Node src_node = null, dst_node = null;
		
		// try to find the source node
		src_node = node_map.get(p.src_ip);
		
		if(src_node == null)
			src_node = createNewNode(p.src_ip,p.src_is_local,p.time,p.length);
		else
			updateNode(src_node,p.time,p.length);

		// if we see traffic from the host, it's not zero byte
		src_node.setBoolean(IS_ZERO_BYTE, false);
		
		// try to find the target node
		dst_node = node_map.get(p.dst_ip);
		if(dst_node == null)
			dst_node = createNewNode(p.dst_ip,p.dst_is_local,p.time,p.length);
		else
			updateNode(dst_node,p.time,p.length);
		
		// try to find the edge
		Edge e = ip_graph.getEdge(src_node,dst_node);
		if(e == null)
			e = createNewEdge(src_node, dst_node, p);
		else
			updateEdge(e,p);
		
		// keep track of each individual packet for time filtering
		edge_times.add(new EdgeTime(p.time,e));
		notifyEdgeTimeListeners(p.time);

		// keep the max and min times up to date
		if(max_time < p.time || all_packets.size() == 1)
			max_time = p.time;
		if(min_time > p.time || all_packets.size() == 1)
			min_time = p.time;

		// TODO: make ranking mechanism more efficient

		// check if this edge meets the time bounds
		if((max_time_bound == -1 || max_time_bound > p.time) &&
				(min_time_bound == -1 || min_time_bound < p.time))
		{
			e.set(TEMP_TIME_SELECTED, true);
			src_node.set(TEMP_TIME_SELECTED, true);
			dst_node.set(TEMP_TIME_SELECTED, true);
		}
		
		// update the ranking
		reRankByBandwidth(src_node);
		reRankByBandwidth(dst_node);
		reRankByDegree(src_node);
		reRankByDegree(dst_node);
		reRankByBandwidth(e);

		// initialize the filter column
		if(filter_predicate == null)
		{
			src_node.set(TIME_SELECTED, src_node.getBoolean(TEMP_TIME_SELECTED));
			dst_node.set(TIME_SELECTED, dst_node.getBoolean(TEMP_TIME_SELECTED));
			e.set(TIME_SELECTED, src_node.getBoolean(TIME_SELECTED) && dst_node.getBoolean(TIME_SELECTED) && e.getBoolean(TEMP_TIME_SELECTED));
		}
		else
		{
			src_node.set(TIME_SELECTED, filter_predicate.getBoolean(src_node) && src_node.getBoolean(TEMP_TIME_SELECTED));
			dst_node.set(TIME_SELECTED, filter_predicate.getBoolean(dst_node) && dst_node.getBoolean(TEMP_TIME_SELECTED));
			e.set(TIME_SELECTED, src_node.getBoolean(TIME_SELECTED) && dst_node.getBoolean(TIME_SELECTED) && 
					filter_predicate.getBoolean(e) && e.getBoolean(TEMP_TIME_SELECTED));
		}
		
		
		ip_graph_lock.unlock();

		notifyEdgeListeners(-1,-1,-1,-1);
		notifyNodeListeners(-1,-1,-1,-1);
	}
	
	public void addPackets(Iterator<?> input_packets)
	{
		ip_graph_lock.lock();
		
		while(input_packets.hasNext())
		{
			SimplePacket p = (SimplePacket)input_packets.next();
			all_packets.add(p);
			Node src_node = null, dst_node = null;
			
			// try to find the source node
			src_node = node_map.get(p.src_ip);
			
			if(src_node == null)
				src_node = createNewNode(p.src_ip,p.src_is_local,p.time,p.length);
			else
				updateNode(src_node,p.time,p.length);
			
			// if we see traffic from the host, it's not zero byte
			src_node.setBoolean(IS_ZERO_BYTE, false);
		
			// try to find the target node
			dst_node = node_map.get(p.dst_ip);
			if(dst_node == null)
				dst_node = createNewNode(p.dst_ip,p.dst_is_local,p.time,p.length);
			else
				updateNode(dst_node,p.time,p.length);
		
			// try to find the edge
			Edge e = ip_graph.getEdge(src_node,dst_node);
			if(e == null)
				e = createNewEdge(src_node, dst_node, p);
			else
				updateEdge(e,p);
		
			// keep track of each individual packet for time filtering
			edge_times.add(new EdgeTime(p.time,e));
			notifyEdgeTimeListeners(p.time);

			// keep the max and min times up to date
			if(max_time < p.time || all_packets.size() == 1)
				max_time = p.time;
			if(min_time > p.time || all_packets.size() == 1)
				min_time = p.time;

			// TODO: make ranking mechanism more efficient

			// check if this edge meets the time bounds
			if((max_time_bound == -1 || max_time_bound > p.time) &&
					(min_time_bound == -1 || min_time_bound < p.time))
			{
				e.set(TEMP_TIME_SELECTED, true);
				src_node.set(TEMP_TIME_SELECTED, true);
				dst_node.set(TEMP_TIME_SELECTED, true);
			}
		}

		// update the ranking
		reRankAllNodesByBandwidth();
		reRankAllNodesByDegree();
		reRankAllEdgesByBandwidth();

		updateFilterColumn();
		
		ip_graph_lock.unlock();

		notifyEdgeListeners(-1,-1,-1,-1);
		notifyNodeListeners(-1,-1,-1,-1);
	}

	private void updateEdge(Edge e, SimplePacket p) {
		int bandwidth = e.getInt(BANDWIDTH);
		e.set(BANDWIDTH, bandwidth+p.length);
		e.set(LASTSEEN, p.time);
	}

	private void updateNode(Node src_node, long time, int length) {
		int bandwidth = src_node.getInt(BANDWIDTH);
		src_node.set(BANDWIDTH, bandwidth + length);
		src_node.set(LASTSEEN, time);
		src_node.set(DEGREE,src_node.getDegree());
	}

	
	private Set<TableListener> node_table_listeners = new HashSet<TableListener>();
	private Set<TableListener> edge_table_listeners = new HashSet<TableListener>();
	
	public void addTableListener(TableListener l, Table t)
	{
		if(t == ip_graph.getNodeTable())
		{
			node_table_listeners.add(l);
			//System.out.println("Adding node listener");
		}
		else if(t == ip_graph.getEdgeTable())
		{
			edge_table_listeners.add(l);
			//System.out.println("Adding edge listener");
		}
		else
		{
			//System.out.println("Failed to add listener!");
		}
	}
	
	public void addNodeTableListener(TableListener l) {
		node_table_listeners.add(l);
	}
	
	private void notifyNodeListeners(int start, int end, int col, int type)
	{
		Iterator<TableListener> i = node_table_listeners.iterator();
		while(i.hasNext())
		{
			TableListener tml = i.next();
			tml.tableChanged(ip_graph.getNodeTable(),start,end,col, type);
		}
	}
	
	private void notifyEdgeListeners(int start, int end, int col, int type)
	{
		Iterator<TableListener> i = edge_table_listeners.iterator();
		while(i.hasNext())
		{
			TableListener tml = i.next();
			tml.tableChanged(ip_graph.getEdgeTable(), start, end, col, type);
		}
	}
	
	private void updateTimeSelected()
	{
		ip_graph_lock.lock();
		// clear the time selected columns
		for(int i = 0; i < ip_graph.getNodeCount(); i++)
		{
			ip_graph.getNode(i).set(TEMP_TIME_SELECTED, false);
		}
		for(int i = 0; i < ip_graph.getEdgeCount(); i++)
		{
			ip_graph.getEdge(i).set(TEMP_TIME_SELECTED, false);
		}
 
		// loop through the edge times, selecting nodes and edges to add to the window
		Iterator<EdgeTime> i = getEdgeTimes();
		while(i.hasNext())
		{
			EdgeTime et = i.next();
			if((min_time_bound != -1 && min_time_bound > et.time) ||
					(max_time_bound != -1 && max_time_bound < et.time))
				continue;
			Edge e = et.edge;
			Node src = e.getSourceNode();
			Node dst = e.getTargetNode();
			e.set(TEMP_TIME_SELECTED, true);
			src.set(TEMP_TIME_SELECTED, true);
			dst.set(TEMP_TIME_SELECTED, true);
		}
		updateFilterColumn();
		ip_graph_lock.unlock();
	}
	
	private Predicate filter_predicate = null;
	public void setFilterPredicate(Predicate p)
	{
		ip_graph_lock.lock();
		filter_predicate = p;
		updateFilterColumn();
		ip_graph_lock.unlock();
	}
	private void updateFilterColumn()
	{
		if(filter_predicate == null)
		{
			Iterator<?> i = ip_graph.nodes();
			while(i.hasNext())
			{
				Node n = (Node)i.next();
				n.set(TIME_SELECTED, n.getBoolean(TEMP_TIME_SELECTED));
			}
			i = ip_graph.edges();
			while(i.hasNext())
			{
				Edge e = (Edge)i.next();
				e.set(TIME_SELECTED, e.getSourceNode().getBoolean(TIME_SELECTED) &&
						e.getTargetNode().getBoolean(TIME_SELECTED) && e.getBoolean(TEMP_TIME_SELECTED));
			}
		} else {
			Iterator<?> i = ip_graph.nodes();
			while(i.hasNext())
			{
				Node n = (Node)i.next();
				n.set(TIME_SELECTED, filter_predicate.getBoolean(n) && n.getBoolean(TEMP_TIME_SELECTED));
			}
			i = ip_graph.edges();
			while(i.hasNext())
			{
				Edge e = (Edge)i.next();
				e.set(TIME_SELECTED, e.getSourceNode().getBoolean(TIME_SELECTED) &&
						e.getTargetNode().getBoolean(TIME_SELECTED) && 
						filter_predicate.getBoolean(e) &&
						e.getBoolean(TEMP_TIME_SELECTED));
			}
		}
	}
	
	public static void getHostname(Node node) {
		String hostname = node.getString(HOSTNAME);
		
		if (!hostname.equals(UNRESOLVED))
			return;
		
		// TODO: run this in another thread
		InetAddress address = (InetAddress)node.get("address");
		String name = address.getHostName();
		
		if (name.equals(address.getHostAddress()))
			node.set(HOSTNAME, NO_HOSTNAME);
		else
			node.set(HOSTNAME, name);
	}
	
	// node selection interface stuff
	private Set<SelectListener> node_select_listeners = new HashSet<SelectListener>();
	public void addNodeSelectListener(SelectListener l)
	{
		node_select_listeners.add(l);
	}
	
	public void selectGroup(int row_id)
	{
		selectGroup(groups.getNode(row_id));
	}
	
	public void selectGroup(Node n)
	{
		Iterator<SelectListener> i = node_select_listeners.iterator();
		while(i.hasNext())
		{
			SelectListener l = i.next();
			l.groupSelected(n);
		}
	}
	
	public void selectEdge(int row_id)
	{
		selectEdge(ip_graph.getEdge(row_id));
	}
	
	public void selectEdge(Edge e)
	{
		Iterator<SelectListener> i = node_select_listeners.iterator();
		while(i.hasNext())
		{
			SelectListener l = i.next();
			l.edgeSelected(e);
		}
	}
	
	public void selectNode(int row_id)
	{
		selectNode(ip_graph.getNode(row_id));
	}
	
	public void selectNode(Node n)
	{
		Iterator<SelectListener> i = node_select_listeners.iterator();
		while(i.hasNext())
		{
			SelectListener l = i.next();
			l.nodeSelected(n);
		}
	}
	
	public void clearSelection()
	{
		Iterator<SelectListener> i = node_select_listeners.iterator();
		while(i.hasNext())
		{
			SelectListener l = i.next();
			l.selectionCleared();
		}		
	}
	
	public Set<EdgeTimeListener> edge_time_listeners = new HashSet<EdgeTimeListener>();
	public void addEdgeTimeListener(EdgeTimeListener l)
	{
		edge_time_listeners.add(l);
	}
	private void notifyEdgeTimeListeners(long time)
	{
		for(Iterator<EdgeTimeListener> i = edge_time_listeners.iterator(); i.hasNext();)
		{
			i.next().addEdgeTime(time);
		}
	}
}
