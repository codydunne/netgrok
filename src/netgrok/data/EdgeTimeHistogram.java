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

public class EdgeTimeHistogram extends Table implements Runnable, TupleSetListener {

	double max_height;
	public static final String X = "x";
	public static final String Y = "y";
		
	private static final int num_buckets = 200;
	// double-buffer the buckets so they can be drawn while they're updated
	private double[] buckets1 = new double[num_buckets];
	private double[] buckets2 = new double[num_buckets];
	private boolean updating_buckets1 = true;
	public Lock buckets_lock = new ReentrantLock();
	
	public double[] getBuckets()
	{
		if(updating_buckets1)
			return buckets2;
		else
			return buckets1;
	}

	private void swapBuffers()
	{
		buckets_lock.lock();
		updating_buckets1 = !updating_buckets1;
		buckets_lock.unlock();
	}
	
	private double[] getBucketsToUpdate()
	{
		if(updating_buckets1)
			return buckets1;
		else
			return buckets2;
	}
	
	Data d;

	private double minx1, maxx1;
	private double miny1, maxy1;
	private double minx2, maxx2;
	private double miny2, maxy2;
	private boolean needToRecalculate;
	
	public class ETHUpdateActivity extends Action
	{
		EdgeTimeHistogram eth;
		public ETHUpdateActivity(EdgeTimeHistogram eth) {
			this.eth = eth;
		}
		@Override
		public void run(double arg0) {
			eth.run();
		}
		
	}
	
	public ETHUpdateActivity getUpdateActivity()
	{
		return new ETHUpdateActivity(this);
	}
	
	public EdgeTimeHistogram(Data d)
	{
		this.d = d;
		this.addColumn(X,double.class);
		this.addColumn(Y,double.class);
		// create a point for each bucket
		for(int i =0; i < num_buckets; i++)
		{
			addRow();
		}
		recalculate();
		d.getIPGraph().addTupleSetListener(this);
	}
	
	public void recalculate()
	{
		//System.out.println("Acquiring lock");
		d.getLock().lock();
		//System.out.println("Lock acquired");
		Iterator<EdgeTime> edge_times = d.getEdgeTimes();
		double minx = (double)d.getMinTime();
		double maxx = (double)d.getMaxTime();
		double time_width = (double)(d.getMaxTime()) - (double)(d.getMinTime());
		double bucket_width = time_width / num_buckets;
		// TODO: scale X to time bounds?
		double buckets[] = getBucketsToUpdate();
		for(int i = 0; i < num_buckets; i++)
		{
			set(i,X,minx + ((double)i + 0.5f) * time_width);
			buckets[i] = 0.0f;
		}
		
		while(edge_times.hasNext())
		{
			//System.out.print(".");
			EdgeTime et = edge_times.next();
			int bucket = (int)((double)(et.time - d.getMinTime()) / bucket_width);
			//System.out.print(bucket);
			if(bucket >= buckets.length)
				bucket = buckets.length - 1;
			if(bucket < 0)
				bucket = 0;
			// TODO: verify that bucket is in range?
			// TODO: use length instead?
			buckets[bucket] += 1;
		}
		//System.out.println("Unlocking");
		d.getLock().unlock();
		//System.out.println("Unlocked");

		// TODO: scale Y?
		double miny = 0;
		double maxy = 0;
		for(int i = 0; i < num_buckets; i++)
		{
			if(buckets[i] > maxy)
			{
				//System.out.println("New maxy: "+maxy);
				maxy = buckets[i];
			}
			set(i,Y,buckets[i]);
		}
		if(updating_buckets1)
		{
			minx1 = minx;
			maxx1 = maxx;
			miny1 = miny;
			maxy1 = maxy;
		}
		else
		{
			minx2 = minx;
			maxx2 = maxx;
			miny2 = miny;
			maxy2 = maxy;
		}
		swapBuffers();
		//System.out.println("Finished recalculating");
	}
	
	public double getMinX()
	{
		if(updating_buckets1)
			return minx2;
		else
			return minx1;
	}
	
	public double getMinY()
	{
		if(updating_buckets1)	
			return miny2;
		else
			return miny1;
	}
	
	public double getMaxX()
	{
		if(updating_buckets1)
			return maxx2;
		else
			return maxx1;
	}
	
	public double getMaxY()
	{
		if(updating_buckets1)
			return maxy2;
		else
			return maxy1;
	}
	
	public void run()
	{
		//System.out.println("Running");
		if(needToRecalculate)
		{
			needToRecalculate = false;
			recalculate();
		}
	}

	public void tupleSetChanged(TupleSet arg0, Tuple[] arg1, Tuple[] arg2) {
		// TODO Auto-generated method stub
		needToRecalculate = true;
	}

	public void addActionListener(ActionListener actionListener) {
		// TODO Auto-generated method stub
		System.out.println("action listener not added due to unsupported action");
	}
}
