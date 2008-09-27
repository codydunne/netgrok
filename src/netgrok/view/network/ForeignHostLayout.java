package netgrok.view.network;

import netgrok.data.*;

import java.security.*;
import java.util.*;
import java.net.*;

import prefuse.action.layout.graph.*;
import prefuse.data.tuple.TupleSet;
import prefuse.data.expression.*;
import prefuse.data.expression.parser.*;
import prefuse.util.force.*;
import prefuse.visual.*;

// A summarization of Cody's work
public class ForeignHostLayout extends ForceDirectedLayout {
	
	final static double TWO_TO_THE_32 = Math.pow(2,32);
	
	private HashMap<VisualItem, float[]> location = 
		new HashMap<VisualItem, float[]>(); 
	
	private double offset;
	private double scale_x;
	private double scale_y;
	
	private NetworkZoomControl zoom;
	private Predicate m_filter;
	
	public ForeignHostLayout(NetworkZoomControl zoom, 
			double radius, double width, double height, 
			String m_group, String filter) {
		super(m_group, false, false);
		
		offset = (radius*2) / Math.min(width, height) - .6;
		scale_x = width / (1 + 2 * (.12 + offset));
		scale_y = height / (1 + 2 * (.12 + offset));
		
		// compile the filter
		m_filter = (Predicate) ExpressionParser.parse(filter + " and visible()");
		getForceSimulator().setSpeedLimit(.2f);
		
		this.zoom = zoom;
	}
	
	protected void initSimulator(ForceSimulator fsim) {	 
		// make sure we have force items to work with
		TupleSet ts = m_vis.getGroup(m_nodeGroup);
		if ( ts == null ) return;
		try {
			ts.addColumns(FORCEITEM_SCHEMA);
		} catch ( IllegalArgumentException iae ) { /* ignored */ }
		
		float startX = (referrer == null ? 0f : (float)referrer.getX());
		float startY = (referrer == null ? 0f : (float)referrer.getY());
		startX = Float.isNaN(startX) ? 0f : startX;
		startY = Float.isNaN(startY) ? 0f : startY;

		Iterator<?> iter = ts.tuples(m_filter);
		while ( iter.hasNext() ) {
			VisualItem item = (VisualItem)iter.next();
			ForceItem fitem = (ForceItem)item.get(FORCEITEM);
			
			fitem.mass = getMassValue(item);
			double x = item.getEndX();
			double y = item.getEndY();
			fitem.location[0] = (Double.isNaN(x) ? startX : (float)x);
			fitem.location[1] = (Double.isNaN(y) ? startY : (float)y);
			fsim.addItem(fitem);
			
			// add a force between this node and its initial location
			ForceItem anchor = new ForceItem();
			
			if (location.containsKey(item)) {
				anchor.location = location.get(item);
			}
			
			else {
				byte[] ip = ((InetAddress)item.get(Data.ADDRESS)).getAddress();
				float[] loc = null;
				
				// if the new host is in a group, place it's anchor
				// at it's group's default location
				AggregateTable table = (AggregateTable) m_vis.getGroup(NetworkView.GROUPS);
				Iterator<?> groups = table.tuples();
				
				while (groups.hasNext()) {
					AggregateItem group = (AggregateItem) groups.next();
					
					if (group.containsItem(item)) {
						int size = group.getInt(NetworkView.GROUP_SIZE);
						loc = pointFromIP((byte[])group.get(Data.HOME_IP));
						
						// spiral nodes around the group
						if (size > 1) {
							loc[0] += 5 * Math.cos(size) + size; 
							loc[1] += 5 * Math.sin(size) + size;
						}

						break;
					}
				}	
				
				// they weren't in a group
				if (loc == null) {
					loc = pointFromIP(ip);
				}
				
				anchor.location = loc;
				location.put(item, loc);
			}
			//fitem.location = anchor.location;//NOMOVE HACK
			getForceSimulator().addSpring(fitem, anchor, .001f, .001f);
			//this.setEnabled(false);//NOMOVE HACK
		}
	}
	
	// given a byte representation of an IP, returns a unique 
	// location on the screen
	public float[] pointFromIP(byte[] ip) {
		if (ip.length != 4) {
			System.err.println("An IP must be four bytes!");
			System.exit(1);
		}
		
		// get the 128 bit hash of the IP
		MessageDigest md = null;
		
		try {
			md = MessageDigest.getInstance("MD5");
		}
		
		catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		md.update(ip, 0, 4);
		byte[] hash = md.digest();
		
		// convert the lower half of the hash to an x location and the 
		// upper half to a y location
		double x = eightBytesToInt(hash, 0, 7);
		double y = eightBytesToInt(hash, 8, 15);
		
		// scale the number to between [-.5, .5] so that the polar 
		// conversion can handle the number
		x /= TWO_TO_THE_32;
		y /= TWO_TO_THE_32;
		
		//Convert to polar
		double radius = Math.hypot(x, y);
		double theta = Math.atan2(y, x);

		//Do the polar alterations
		double nradius = Math.exp(radius - 1) + offset;

		//Convert to rectangular
		double nx = nradius * Math.cos(theta); 
		double ny = nradius * Math.sin(theta);

		// offsets from screen and interior circle
		nx = scale_x * (nx + offset);
		ny = scale_y * (ny + offset);

		return new float[] {(float)nx, (float)ny};
	}

	public int eightBytesToInt(byte[] source, int start, int range) {
		int ret = 0;
		byte[] hash = new byte[range-start+1];
		
		System.arraycopy(source, start, hash, 0, range-start+1);
		
		ret  = (hash[3] ^ hash[7]) & 0xFF;
		ret |= (((hash[2] ^ hash[6]) << 8) & 0xFF00);
		ret |= (((hash[1] ^ hash[5]) << 16) & 0xFF0000);
		ret |= (((hash[0] ^ hash[4]) << 24) & 0xFF000000);

		return ret;
	}	
}