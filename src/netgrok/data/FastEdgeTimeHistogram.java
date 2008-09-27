package netgrok.data;

import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import netgrok.data.Data.EdgeTime;
import prefuse.action.Action;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.tuple.TupleSet;

public class FastEdgeTimeHistogram implements EdgeTimeListener {

	double max_height;
	public static final String X = "x";
	public static final String Y = "y";
		
	private static final int num_buckets = 200;
	// double-buffer the buckets so they can be drawn while they're updated
	private double[] buckets = new double[num_buckets];
	private double minx, maxx;
	private double miny, maxy;
	private double[] temp_buckets = new double[num_buckets];
	private boolean initialized = false;
	public Lock buckets_lock = new ReentrantLock();
	
	private void growRight(int growth_factor)
	{
		//System.out.println("Growing right!");
		buckets_lock.lock();
		double time_width = maxx - minx;
		maxx = minx+time_width*(double)growth_factor;
		for(int i = 0; i < buckets.length; i++)
		{
			temp_buckets[i] = buckets[i];
			buckets[i] = 0;
		}
		for(int i = 0; i < temp_buckets.length;i++)
		{
			buckets[i/growth_factor] += temp_buckets[i];
		}
		for(int i = 0; i < buckets.length; i++)
		{
			if(buckets[i] > maxy)
				maxy = buckets[i];
		}
		buckets_lock.unlock();
		//System.out.println("minx: "+minx+", maxx: "+maxx);
	}
	
	private void growLeft(int growth_factor)
	{
		//System.out.println("Growing left!");
		buckets_lock.lock();
		double time_width = maxx - minx;
		minx = maxx-time_width*(double)growth_factor;
		for(int i = 0; i < buckets.length; i++)
		{
			temp_buckets[i] = buckets[i];
			buckets[i] = 0;
		}
		for(int i = 0; i < temp_buckets.length;i++)
		{
			buckets[buckets.length - 1 - i/growth_factor] += temp_buckets[temp_buckets.length - 1 - i];
		}
		for(int i = 0; i < buckets.length; i++)
		{
			if(buckets[i] > maxy)
				maxy = buckets[i];
		}
		buckets_lock.unlock();
	}
	
	public double[] getBuckets()
	{
		return buckets;
	}
		
	Data d;
	
	public FastEdgeTimeHistogram(Data d)
	{
		this.d = d;
		// create a point for each bucket
		d.addEdgeTimeListener(this);
	}
		
	public double getMinX()
	{
		return minx;
	}
	
	public double getMinY()
	{
		return miny;
	}
	
	public double getMaxX()
	{
		return maxx;
	}
	
	public double getMaxY()
	{
		return maxy;
	}
	
	public void addEdgeTime(long time)
	{
		if(initialized == false)
		{
			minx = time;
			// initialize to 1 second width
			maxx = time + 1000;
			miny = 0;
			maxy = 1;
			//System.out.println("Initialized to "+minx+","+maxx);
			initialized = true;
		}
		else if((double)time < minx)
		{
			int growth_factor = (int)Math.ceil((maxx - (double)time)/(maxx-minx));
			growLeft(growth_factor);
		}
		else if((double)time > maxx)
		{
			int growth_factor = (int)Math.ceil(((double)time - minx)/(maxx-minx));
			growRight(growth_factor);
		}
		double time_width = maxx - minx;
		int bucket = (int)((double)buckets.length * ((double)time - minx) / time_width);
		if(bucket >= buckets.length)
			bucket = buckets.length - 1;
		if(bucket < 0)
			bucket = 0;
		buckets[bucket] += 1;
		if(buckets[bucket] > maxy)
			maxy = buckets[bucket];
	}
	
	public void addActionListener(ActionListener actionListener) {
		// TODO Auto-generated method stub
		System.out.println("action listener not added due to unsupported action");
	}
}
