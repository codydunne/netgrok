package netgrok.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import netgrok.data.Data;
import netgrok.data.EdgeTimeHistogram;
import netgrok.data.FastEdgeTimeHistogram;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.ShapeAction;
import prefuse.action.layout.AxisLayout;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.RendererFactory;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.ui.JRangeSlider;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;

public class TimeOverviewView extends Display implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// our reference to the user's data
	private Data d;

	private static final String group = "data";
	
	private AxisLayout x_axis, y_axis;
	
	private FastEdgeTimeHistogram eth;
	
	private JRangeSlider time_window_slider;

	public void paint(Graphics g)
	{
		//super.paint(g);
		Graphics2D g2 = (Graphics2D)g;
		int width = this.getWidth();
		int num_ticks = 20;
		
		// draw the selected time window
		long time_width = d.getMaxTime() - d.getMinTime();
		int maxy = getHeight() - getFont().getSize() - 10;

		long min_time_bound = d.getMinTimeBound();
		int time_selected_min_x = 0;
		if(min_time_bound != -1)
			time_selected_min_x = (int)((float)(min_time_bound - d.getMinTime())/(float)time_width*(float)(getWidth()-1));
		long max_time_bound = d.getMaxTimeBound();
		int time_selected_max_x = getWidth()-1;
		if(max_time_bound != -1)
			time_selected_max_x = (int)((float)(max_time_bound - d.getMinTime())/(float)time_width*(float)(getWidth()-1));
		g2.setColor(ColorLib.getColor(0,255,255,128));
		g2.fillRect(time_selected_min_x,0,time_selected_max_x-time_selected_min_x,maxy);
		
		// draw the scale tick marks and time labels
		g2.setColor(Color.BLACK);
		for(int i = 0; i < num_ticks+1; i++)
		{
			int x = (int)((float)(width-1)*(float)i/(float)num_ticks);
			int date_width = 160;
			int date_x = x - (int)((float)i/(float)num_ticks*(float)date_width);
			int tick_height = 2;
			if(i % 10 == 0)
			{
				tick_height += 4;
				long time = d.getMinTime() + (long)((float)time_width*(float)i/(float)num_ticks);
				Date date = new Date(time);
				//String date_string = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(date);
				String date_string = DateFormat.getDateTimeInstance().format(date);
				g2.drawString(date_string, date_x, getHeight()-1);
			}
			int tick_max_y = getHeight() - getFont().getSize() - 1;
			g2.drawLine(x, tick_max_y, x, tick_max_y - tick_height);
		}
		
		// draw the line for the histogram
		eth.buckets_lock.lock();
		double buckets[] = eth.getBuckets();
		double histo_start = ((double)(d.getMinTime()) - eth.getMinX())*(double)(width - 1)/(double)time_width;
		double bucket_width = (double)(width-1)/(double)(buckets.length + 1)*
				(eth.getMaxX()-eth.getMinX())/(double)time_width;
		double y_scale = (double)maxy/eth.getMaxY();
		g2.setColor(Color.DARK_GRAY);
		for(int i = 0; i < buckets.length-1; i++)
		{
			int x1 = (int)((double)i * bucket_width - histo_start);
			if(x1 < 0)
				continue;
			int x2 = (int)((double)(i+1) * bucket_width - histo_start);
			if(x2 > getWidth())
				break;
			int y1 = maxy - (int)(y_scale * buckets[i]);
			int y2 = maxy - (int)(y_scale * buckets[i+1]);
			g2.drawLine(x1, y1, x2, y2);
		}
		eth.buckets_lock.unlock();
	}
	
	public TimeOverviewView(JRangeSlider time_window_slider) {
		super(new Visualization());
		
		this.time_window_slider = time_window_slider;
		time_window_slider.addChangeListener(this);
		
		// save the reference to global data
		this.d = Data.getData();
		eth = new FastEdgeTimeHistogram(d);
        
        ActionList draw = new ActionList(ActionList.INFINITY,200);
        draw.add(new RepaintAction());
        m_vis.putAction("draw", draw);
		
        // TODO: this might have a locking issue
        m_vis.run("draw");
	}

	public void stateChanged(ChangeEvent arg0) {
		// TODO Auto-generated method stub
		int minimum = time_window_slider.getMinimum();
		int low_value = time_window_slider.getLowValue();
		int high_value = time_window_slider.getHighValue();
		int maximum = time_window_slider.getMaximum();
		int tws_width = maximum - minimum;
		long min_time = (long)((double)(low_value - minimum)/(double)tws_width * (double)(d.getMaxTime() - d.getMinTime()) + (double)d.getMinTime());
		long max_time = (long)((double)(high_value - minimum)/(double)tws_width * (double)(d.getMaxTime() - d.getMinTime()) + (double)d.getMinTime());
		if(minimum == low_value)
			min_time = -1;
		if(maximum == high_value)
			max_time = -1;
		//System.out.println("Setting time bounds to "+eth.getMinX()+":"+eth.getMaxX()+":"+minimum+":"+low_value+":"+high_value+":"+maximum+" "+min_time+":"+max_time);
		d.setTimeBounds(min_time, max_time);
		repaint();
	}
	
}
