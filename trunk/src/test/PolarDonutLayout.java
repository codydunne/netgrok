package test;

import netgrok.data.*;

import java.net.*;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import prefuse.action.layout.*;
import prefuse.data.*;
import prefuse.data.expression.*;
import prefuse.data.expression.parser.*;
import prefuse.data.tuple.*;
import prefuse.visual.*;

public class PolarDonutLayout extends Layout {

	private double height, width;
	private double circleRadius;
	private Predicate m_filter;
	private boolean reinitialize = true;
	private Set<Object> layedOutObjects = new HashSet<Object>();

	public PolarDonutLayout(double circleRadius, String m_group, String filter){
		super(m_group);
		this.circleRadius = circleRadius;
		filter = filter + " and visible() and isnode()";
		this.m_filter = (Predicate)ExpressionParser.parse(filter);
	}

	private void initialize(){
		if(reinitialize == false)
			return;
		reinitialize = false;
		this.width = getLayoutBounds().getWidth();
		this.height = getLayoutBounds().getHeight();
	}

	@Override
	public void run(double frac) {
		initialize();
		TupleSet ts = m_vis.getGroup(m_group);
		//System.out.println("width "+ width + " height " + height);
		PolarDonutGen pd = new PolarDonutGen(width, height, circleRadius * 2);

		Iterator<?> iter;

		if (m_filter == null)
			iter = ts.tuples();
		else 
			iter = ts.tuples(m_filter);
		
		// layout grid contents
		while ( iter.hasNext() ) {
			VisualItem item = (VisualItem)iter.next();
			
			if(layedOutObjects.add(item) == false)
            	continue;
            item.setVisible(true);
            // TODO: find the ip address somewhere in the linked tuples
            Tuple graph_node = m_vis.getSourceTuple(item);
            InetAddress ip_address = (InetAddress)graph_node.get(Data.ADDRESS);
            //System.out.println(ip_address);
            
            String ipString = ((InetAddress)graph_node.get("address")).getHostAddress();
           // System.out.println(ipString);

			Point2D.Double p = pd.getPointFromIP(ipString);
			double x = p.x;
			double y = p.y;

			setX(item,null,x);
			setY(item,null,y);			
		}
	}
}
