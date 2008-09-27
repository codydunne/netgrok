package test;

import netgrok.data.*;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import prefuse.action.layout.Layout;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;

public class GridMinusCircle extends Layout {
	
	private double height = 10.0, width = 10.0;
	private double minx = 0.0, miny = 0.0;
	private double circle_x = 5.0, circle_y = 5.0;
	private double circle_r = 4.0;
	private boolean find_circle_center = false;
	
	private double[] line_lengths;
	private double[] cumulative_line_lengths;
	private double line_spacing;
	private double total_length;
	private int num_lines;
	private boolean reinitialize = true;
	private Set<Object> layedOutObjects = new HashSet<Object>();
	private Predicate m_filter = null;
	
	public GridMinusCircle(double circle_x, double circle_y, double circle_r, double line_spacing, String m_group)
	{
		super(m_group);
		this.circle_r = circle_r;
		this.circle_x = circle_x;
		this.circle_y = circle_y;
		this.line_spacing = line_spacing;
	}
	
	public GridMinusCircle(double circle_r, double line_spacing, String m_group)
	{
		super(m_group);
		this.circle_r = circle_r;
		this.line_spacing = line_spacing;
		this.find_circle_center = true;
	}
	
	public GridMinusCircle(double circle_r, String m_group, String filter)
	{
		super(m_group);
		this.line_spacing = 10;
		this.circle_r = circle_r;
		this.find_circle_center = true;
		filter = filter + " and visible() and isnode()";
		this.m_filter = (Predicate)ExpressionParser.parse(filter);
	}
	
	private void initialize()
	{
		if(reinitialize == false)
			return;
		reinitialize = false;
		this.width = getLayoutBounds().getWidth();
		this.height = getLayoutBounds().getHeight();
		this.minx = getLayoutBounds().getMinX();
		this.miny = getLayoutBounds().getMinY();
		if(find_circle_center)
		{
			this.circle_x = getLayoutBounds().getCenterX();
			this.circle_y = getLayoutBounds().getCenterY();
		}
		setLineSpacing(line_spacing);
		//System.out.println("Num lines: "+num_lines);
		//System.out.println("Height: "+height);
		//System.out.println("Width: "+width);
		//System.out.println("Circle x,y,r: "+circle_x+","+circle_y+","+circle_r);
		//System.out.println("Total length: "+total_length);
	}
	
	public void setLineSpacing(double line_spacing)
	{
		num_lines = (int)(height/line_spacing);
		line_lengths = new double[num_lines];
		cumulative_line_lengths = new double[num_lines];
		double cumulative_length = 0.0;
		for(int line = 0; line < num_lines; line++)
		{
			double current_y = ((double)line + 0.5) * line_spacing + miny;
			double current_length = calculateLineLength(current_y);
			//System.out.println("Line spacing: "+line+","+current_length);
			cumulative_line_lengths[line] = cumulative_length;
			line_lengths[line] = current_length;
			cumulative_length += current_length;
		}
		total_length = cumulative_length;
	}
	
	public double getY(double distance)
	{
		// TODO: add a constant jitter
		// TODO: scale this differently
		distance = distance % total_length;
		{
			double y = ((double)num_lines - 0.5) * line_spacing + miny;
			// TODO: use a binary search here
			for(int line = 1; line < num_lines; line++)
			{
				if(cumulative_line_lengths[line] > distance)
				{
					y = ((double)line - 0.5) * line_spacing + miny;
					break;
				}
			}
			return y;
		}
	}
	public double getX(double distance, double y)
	{
		int line = (int)(((y - miny)/line_spacing) - 0.5);
		double x = distance - cumulative_line_lengths[line];
		double void_width = width - line_lengths[line];
		//System.out.println("In getx: "+distance+","+y+","+line+","+cumulative_line_lengths[line]+","+line_lengths[line]);
		if(x > circle_x - minx - void_width * 0.5)
			return x + void_width + minx;
		return x + minx;
	}
	
	private double calculateLineLength(double y)
	{
		if(y < circle_y - circle_r)
			return width;
		else if(y < circle_y + circle_r)
		{
			//TODO: this assumes the whole circle fits in the screen
			double line_length = width - 2.0 * Math.sqrt(circle_r * circle_r - (y - circle_y)*(y - circle_y));
			if(line_length < 0.0)
				return 0.0;
			return line_length;
		}
		else
			return width;
	}

	static byte[] addrStringToBytes(String addr){

		String[] s = addr.trim().split("\\.");
		int val = 0;

		byte[] res = new byte[4];

		for (int i = 0; i < 4; i++) {
			val = Integer.parseInt(s[i]);
			res[i] = (byte) (val & 0xff);
		}

		return res;
	}

	public static class AeSimpleMD5 {

		private static String convertToHex(byte[] data) {
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
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			byte[] md5hash = new byte[32];
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			md5hash = md.digest();
			//System.err.println(md5hash.toString());
			return (md5hash);
		}

		public static byte[] MD5(byte[] text) throws NoSuchAlgorithmException, UnsupportedEncodingException  {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			byte[] md5hash = new byte[32];
			md.update(text, 0, text.length);
			md5hash = md.digest();
			return md5hash;
		}
	}

	static int eightBytesToInt(byte[] hash){

		int t = 0;
		if (hash == null || hash.length < 8) {
			throw new IllegalArgumentException("Need 8 bytes");
		}

		t  = (hash[3] ^ hash[7]) & 0xFF;
		t |= (((hash[2] ^ hash[6]) << 8) & 0xFF00);
		t |= (((hash[1] ^ hash[5]) << 16) & 0xFF0000);
		t |= (((hash[0] ^ hash[4]) << 24) & 0xFF000000);

		return t;
	}

	private double hashIP(String ip_address)
	{
		double hash_value = 0.0;
		byte[] res = null;
		try {
			res = AeSimpleMD5.MD5(addrStringToBytes(ip_address));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hash_value = ((double)eightBytesToInt(res) - (double)Integer.MIN_VALUE) / ((double)Integer.MAX_VALUE - (double)Integer.MIN_VALUE) * total_length;
		return hash_value;
		
	}

	@Override
	public void run(double frac) {
		// TODO Auto-generated method stub
		initialize();
        TupleSet ts = m_vis.getGroup(m_group);
        
        Iterator<?> iter;
        
        if (m_filter != null)
        	iter = ts.tuples();
        else 
        	iter = ts.tuples(m_filter);
        	
        // layout grid contents
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            // layout items only once
            if(layedOutObjects.add(item) == false)
            	continue;
            item.setVisible(true);
            // TODO: find the ip address somewhere in the linked tuples
            InetAddress ip_address = (InetAddress)item.get(Data.ADDRESS);
            //System.out.println(ip_address);
            //int ip_hash = ip_address.hashCode();
            //double distance = (double) ip_hash / (double)Integer.MAX_VALUE * total_length;
            double distance = hashIP(ip_address.getHostAddress());
            double y = getY(distance);
            double x = getX(distance,y);
            //System.out.println("Distance, X, Y: "+distance+", "+x+", "+y);
            setX(item,null,x);
            setY(item,null,y);
        }

	}
}
