package netgrok.view.network;

import java.util.*;

import prefuse.action.layout.graph.*;
import prefuse.data.expression.*;
import prefuse.data.expression.parser.*;
import prefuse.data.tuple.TupleSet;
import prefuse.util.force.*;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

public class LocalHostLayout extends ForceDirectedLayout {
	
	private long m_lasttime = -1L;
	private double circle_r;
	private double circle_x;
	private double circle_y;
	private Predicate m_filter = null;
	
	public LocalHostLayout(double circle_r, double circle_x, double circle_y, String graph, String filter)
	{
		super(graph, true, false);
		this.circle_r = circle_r;
		this.circle_x = circle_x;
		this.circle_y = circle_y;

		filter += " and visible()";
		this.m_filter = (Predicate)ExpressionParser.parse(filter);
		
		getForceSimulator().setSpeedLimit(.1f);
	}

	public void run(double frac) {
		if ( m_lasttime == -1 )
			m_lasttime = System.currentTimeMillis()-20;
		
		long time = System.currentTimeMillis();
		long timestep = Math.min(getMaxTimeStep(), time - m_lasttime);
		m_lasttime = time;
		
		// run force simulator
		getForceSimulator().clear();
		initSimulator(getForceSimulator());
		getForceSimulator().runSimulator(timestep);
		updateNodePositions();
	
		if ( frac == 1.0 )
			reset();
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
		
		ArrayList<EdgeItem> edges = new ArrayList<EdgeItem>();
		
		Iterator<?> iter = m_vis.items(m_nodeGroup, m_filter);
		while ( iter.hasNext() ) {
			NodeItem item = (NodeItem)iter.next();
			ForceItem fitem = (ForceItem)item.get(FORCEITEM);
			fitem.mass = getMassValue(item);
			double x = item.getEndX();
			double y = item.getEndY();
			fitem.location[0] = (Double.isNaN(x) ? startX : (float)x);
			fitem.location[1] = (Double.isNaN(y) ? startY : (float)y);
			fsim.addItem(fitem);
			
			for (Iterator<?> it=item.outEdges(); it.hasNext();) {
				EdgeItem edge = (EdgeItem) it.next();
				NodeItem adjacent = edge.getAdjacentItem(item);
				
				if (adjacent.getBoolean("is_local"))
					edges.add(edge);
			}
		}

		iter = edges.iterator();
		while ( iter.hasNext() ) {
			EdgeItem  e  = (EdgeItem)iter.next();
			NodeItem  n1 = e.getSourceItem();
			ForceItem f1 = (ForceItem)n1.get(FORCEITEM);
			NodeItem  n2 = e.getTargetItem();
			ForceItem f2 = (ForceItem)n2.get(FORCEITEM);
			float coeff = getSpringCoefficient(e);
			float slen = getSpringLength(e);
			fsim.addSpring(f1, f2, (coeff>=0?coeff:-1.f), (slen>=0?slen:-1.f));
		}
	}

	private void updateNodePositions() {
		// update positions
		Iterator<?> iter = m_vis.items(m_nodeGroup, m_filter);
		
		while ( iter.hasNext() ) {
			VisualItem item = (VisualItem)iter.next();
			ForceItem fitem = (ForceItem)item.get(FORCEITEM);
			
			if ( item.isFixed() ) {
				// clear any force computations
				fitem.force[0] = 0.0f;
				fitem.force[1] = 0.0f;
				fitem.velocity[0] = 0.0f;
				fitem.velocity[1] = 0.0f;
				
				if ( Double.isNaN(item.getX()) ) {
					setX(item, referrer, 0.0);
					setY(item, referrer, 0.0);
				}
				
				continue;
			}
			
			double x = fitem.location[0];
			double y = fitem.location[1];
			
			// adam rules!
			double xy_r = Math.sqrt((x - circle_x)*(x - circle_x) + (y-circle_y)*(y-circle_y));
			
			if(xy_r > circle_r) {
				x *= circle_r / xy_r;
				y *= circle_r / xy_r;
			}
			
			// set the actual position
			setX(item, referrer, x);
			setY(item, referrer, y);
		}
	}
	
	public void reset() {
		Iterator<?> iter = m_vis.items(m_nodeGroup, m_filter);
		while ( iter.hasNext() ) {
			VisualItem item = (VisualItem)iter.next();
			ForceItem fitem = (ForceItem)item.get(FORCEITEM);
			if ( fitem != null ) {
				fitem.location[0] = (float)item.getEndX();
				fitem.location[1] = (float)item.getEndY();
				fitem.force[0]	= fitem.force[1]	= 0;
				fitem.velocity[0] = fitem.velocity[1] = 0;
			}
		}
		m_lasttime = -1L;
	}
}
