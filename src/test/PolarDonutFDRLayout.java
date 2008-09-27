package test;

import java.net.*;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import prefuse.data.*;
import prefuse.data.expression.*;
import prefuse.data.expression.parser.*;

import java.awt.geom.Rectangle2D;

import prefuse.action.layout.Layout;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.tuple.TupleSet;
import prefuse.util.PrefuseLib;
import prefuse.util.force.DragForce;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.util.force.NBodyForce;
import prefuse.util.force.SpringForce;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

/**
 * <p>Lays out foreign nodes like PolarDonutLayout, but with springs to keep them 
 * from overlaying each other. Based on {@link prefuse.action.layout.graph.ForceDirectedLayout}.</p>
 * 
 * @author Cody Dunne
 */
public class PolarDonutFDRLayout extends Layout {

	private double height, width;
	private double circleRadius;
	private Predicate m_filter;
	private String filter;
	private boolean reinitialize = true;
	private Map<NodeItem, Point2D.Double> layedOutObjects = new HashMap<NodeItem, Point2D.Double>();

	private ForceSimulator m_fsim;
	private long m_lasttime = -1L;
	private long m_maxstep = 50L;
	private boolean m_runonce;
	private int m_iterations = 100;
	private boolean m_enforceBounds;

	protected transient VisualItem referrer;

	protected String m_nodeGroup;
	protected String m_edgeGroup;

	/**
	 * Create a new PolarDonutFDRLayout. By default, this layout will not
	 * restrict the layout to the layout bounds and will assume it is being
	 * run in animated (rather than run-once) fashion.
	 * @param graph the data group to layout. Must resolve to a Graph instance.
	 */
	public PolarDonutFDRLayout(double circleRadius, String graph, String filter){
		this(circleRadius, graph, filter, false, false);
	}

	/**
	 * Create a new PolarDonutFDRLayout. The layout will assume it is being
	 * run in animated (rather than run-once) fashion.
	 * @param group the data group to layout. Must resolve to a Graph instance.
	 * @param enforceBounds indicates whether or not the layout should require
	 * that all node placements stay within the layout bounds.
	 */
	public PolarDonutFDRLayout(double circleRadius, String m_group, String filter,
			boolean enforceBounds){
		this(circleRadius, m_group, filter, enforceBounds, false);
	}

	/**
	 * Create a new PolarDonutFDRLayout.
	 * @param group the data group to layout. Must resolve to a Graph instance.
	 * @param enforceBounds indicates whether or not the layout should require
	 * that all node placements stay within the layout bounds.
	 * @param runonce indicates if the layout will be run in a run-once or
	 * animated fashion. In run-once mode, the layout will run for a set number
	 * of iterations when invoked. In animation mode, only one iteration of the
	 * layout is computed.
	 */
	public PolarDonutFDRLayout(double circleRadius, String m_group, String filter,
			boolean enforceBounds, boolean runonce){
		super(m_group);

		this.circleRadius = circleRadius;
		this.filter = filter + " and visible()";// and isnode()";
		this.m_filter = (Predicate)ExpressionParser.parse(this.filter);

		m_nodeGroup = PrefuseLib.getGroupName(m_group, Graph.NODES);
		m_edgeGroup = PrefuseLib.getGroupName(m_group, Graph.EDGES);

		m_enforceBounds = enforceBounds;
		m_runonce = runonce;
		m_fsim = new ForceSimulator();
		m_fsim.addForce(new NBodyForce());
		m_fsim.addForce(new SpringForce());
		m_fsim.addForce(new DragForce());
		
		//getForceSimulator().setSpeedLimit(.3f);//XXX: Tweak
	}

	/**
	 * Create a new PolarDonutFDRLayout. The layout will assume it is being
	 * run in animated (rather than run-once) fashion.
	 * @param group the data group to layout. Must resolve to a Graph instance.
	 * @param fsim the force simulator used to drive the layout computation
	 * @param enforceBounds indicates whether or not the layout should require
	 * that all node placements stay within the layout bounds.
	 */
	public PolarDonutFDRLayout(double circleRadius, String m_group, String filter,
			ForceSimulator fsim, boolean enforceBounds) {
		this(circleRadius, m_group, filter, fsim, enforceBounds, false);
	}

	/**
	 * Create a new PolarDonutFDRLayout.
	 * @param group the data group to layout. Must resolve to a Graph instance.
	 * @param fsim the force simulator used to drive the layout computation
	 * @param enforceBounds indicates whether or not the layout should require
	 * that all node placements stay within the layout bounds.
	 * @param runonce indicates if the layout will be run in a run-once or
	 * animated fashion. In run-once mode, the layout will run for a set number
	 * of iterations when invoked. In animation mode, only one iteration of the
	 * layout is computed.
	 */
	public PolarDonutFDRLayout(double circleRadius, String m_group, String filter,
			ForceSimulator fsim, boolean enforceBounds, boolean runonce){	
		super(m_group);

		this.circleRadius = circleRadius;
		filter = filter + " and visible() and isnode()";
		this.m_filter = (Predicate)ExpressionParser.parse(filter);

		m_nodeGroup = PrefuseLib.getGroupName(m_group, Graph.NODES);
		m_edgeGroup = PrefuseLib.getGroupName(m_group, Graph.EDGES);

		m_enforceBounds = enforceBounds;
		m_runonce = runonce;
		m_fsim = fsim;
		
		//getForceSimulator().setSpeedLimit(.3f);
	}

	// ------------------------------------------------------------------------

	/**
	 * Get the maximum timestep allowed for integrating node settings between
	 * runs of this layout. When computation times are longer than desired,
	 * and node positions are changing dramatically between animated frames,
	 * the max step time can be lowered to suppress node movement.
	 * @return the maximum timestep allowed for integrating between two
	 * layout steps.
	 */
	public long getMaxTimeStep() {
		return m_maxstep;
	}

	/**
	 * Set the maximum timestep allowed for integrating node settings between
	 * runs of this layout. When computation times are longer than desired,
	 * and node positions are changing dramatically between animated frames,
	 * the max step time can be lowered to suppress node movement.
	 * @param maxstep the maximum timestep allowed for integrating between two
	 * layout steps
	 */
	public void setMaxTimeStep(long maxstep) {
		this.m_maxstep = maxstep;
	}

	/**
	 * Get the force simulator driving this layout.
	 * @return the force simulator
	 */
	public ForceSimulator getForceSimulator() {
		return m_fsim;
	}

	/**
	 * Set the force simulator driving this layout.
	 * @param fsim the force simulator
	 */
	public void setForceSimulator(ForceSimulator fsim) {
		m_fsim = fsim;
	}

	/**
	 * Get the number of iterations to use when computing a layout in
	 * run-once mode.
	 * @return the number of layout iterations to run
	 */
	public int getIterations() {
		return m_iterations;
	}

	/**
	 * Set the number of iterations to use when computing a layout in
	 * run-once mode.
	 * @param iter the number of layout iterations to run
	 */
	public void setIterations(int iter) {
		if ( iter < 1 )
			throw new IllegalArgumentException(
			"Iterations must be a positive number!");
		m_iterations = iter;
	}

	/**
	 * Explicitly sets the node and edge groups to use for this layout,
	 * overriding the group setting passed to the constructor.
	 * @param nodeGroup the node data group
	 * @param edgeGroup the edge data group
	 */
	public void setDataGroups(String nodeGroup, String edgeGroup) {
		m_nodeGroup = nodeGroup;
		m_edgeGroup = edgeGroup;
	}

	// ------------------------------------------------------------------------

	private void initialize(){
		if(reinitialize == false)
			return;
		reinitialize = false;
		this.width = getLayoutBounds().getWidth();
		this.height = getLayoutBounds().getHeight();
	}

	//XXX: Look
	/**
	 * @see prefuse.action.Action#run(double)
	 */
	public void run(double frac) {
		try{
			// perform different actions if this is a run-once or
			// run-continuously layout
			if ( m_runonce ) {
				Point2D anchor = getLayoutAnchor();
				Iterator<?> iter = m_vis.visibleItems(m_nodeGroup);
				while ( iter.hasNext() ) {
					VisualItem  item = (NodeItem)iter.next();
					item.setX(anchor.getX());
					item.setY(anchor.getY());
				}
				m_fsim.clear();
				long timestep = 1000L;
				initSimulator(m_fsim);
				for ( int i = 0; i < m_iterations; i++ ) {
					// use an annealing schedule to set time step
					timestep *= (1.0 - i/(double)m_iterations);
					long step = timestep+50;
					// run simulator
					m_fsim.runSimulator(step);
					// debugging output
					/*if (i % 10 == 0 ) {
                    System.out.println("iter: "+i);
                }*/
				}
				updateNodePositions();
			} else {
				// get timestep
				if ( m_lasttime == -1 )
					m_lasttime = System.currentTimeMillis()-20;
				long time = System.currentTimeMillis();
				long timestep = Math.min(m_maxstep, time - m_lasttime);
				m_lasttime = time;

				// run force simulator
				m_fsim.clear();
				initSimulator(m_fsim);
				m_fsim.runSimulator(timestep);
				updateNodePositions();
			}
			if ( frac == 1.0 ) {
				reset();
			}

		} catch (Exception e){//XXX: Why do I have to do this to see exceptions?
			e.printStackTrace();
		}
	}

	//XXX: Look
	private void updateNodePositions() {
		Rectangle2D bounds = getLayoutBounds();
		double x1=0, x2=0, y1=0, y2=0;
		if ( bounds != null ) {
			x1 = bounds.getMinX(); y1 = bounds.getMinY();
			x2 = bounds.getMaxX(); y2 = bounds.getMaxY();
		}

		// update positions
		
		TupleSet ts = m_vis.getGroup(m_nodeGroup);
		
		Iterator<?> iter;

		if (m_filter == null)
			iter = ts.tuples();
		else 
			iter = ts.tuples(m_filter);
		
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

			if ( m_enforceBounds && bounds != null) {
				Rectangle2D b = item.getBounds();
				double hw = b.getWidth()/2;
				double hh = b.getHeight()/2;
				if ( x+hw > x2 ) x = x2-hw;
				if ( x-hw < x1 ) x = x1+hw;
				if ( y+hh > y2 ) y = y2-hh;
				if ( y-hh < y1 ) y = y1+hh;
			}

			// set the actual position
			setX(item, referrer, x);
			setY(item, referrer, y);
		}
	}

	/**
	 * Reset the force simulation state for all nodes processed
	 * by this layout.
	 */
	public void reset() {
		Iterator<?> iter = m_vis.visibleItems(m_nodeGroup);
		while ( iter.hasNext() ) {
			VisualItem item = (VisualItem)iter.next();
			ForceItem fitem = (ForceItem)item.get(FORCEITEM);
			if ( fitem != null ) {
				fitem.location[0] = (float)item.getEndX();
				fitem.location[1] = (float)item.getEndY();
				fitem.force[0]    = fitem.force[1]    = 0;
				fitem.velocity[0] = fitem.velocity[1] = 0;
			}
		}
		m_lasttime = -1L;
	}

	//XXX: Look
	/**
	 * Loads the simulator with all relevant force items and springs.
	 * @param fsim the force simulator driving this layout
	 */
	protected void initSimulator(ForceSimulator fsim) {  

		//   |  PolarDonut stuff
		//  \|/

		initialize();
		TupleSet tsP = m_vis.getGroup(m_group);
		//System.out.println("width "+ width + " height " + height);
		PolarDonutGen pd = new PolarDonutGen(width, height, circleRadius * 2);

		Iterator<?> iterP;

		if (m_filter == null)
			iterP = tsP.tuples();
		else 
			iterP = tsP.tuples((Predicate)ExpressionParser.parse(filter + " and isnode()"));

		// layout grid contents
		while ( iterP.hasNext() ) {
			VisualItem item = (VisualItem)iterP.next();

			if(!(item instanceof NodeItem))
				continue;

			NodeItem nItem = (NodeItem) item;

			if(layedOutObjects.containsKey(nItem))
				continue;

			item.setVisible(true);

			// TODO: find the ip address somewhere in the linked tuples
			Tuple graph_node = m_vis.getSourceTuple(item);
			//InetAddress ip_address = (InetAddress)graph_node.get(Data.ADDRESS);
			//System.out.println(ip_address);

			String ipString = ((InetAddress)graph_node.get("address")).getHostAddress();
			// System.out.println(ipString);

			Point2D.Double p = pd.getPointFromIP(ipString);
			layedOutObjects.put(nItem, p);

			double x = p.x;
			double y = p.y;

			item.setX(x);//XXX: remove? jumpy?
			item.setY(y);					
		}

		//  /|\  PolarDonut stuff
		//   |

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

		Iterator<?> iter;

		if (m_filter == null)
			iter = ts.tuples();
		else 
			iter = ts.tuples(m_filter);

		while ( iter.hasNext() ) {
			VisualItem item = (VisualItem)iter.next();
			ForceItem fitem = (ForceItem)item.get(FORCEITEM);
			fitem.mass = getMassValue(item);
			double x = item.getEndX();

			double y = item.getEndY();
			fitem.location[0] = (Double.isNaN(x) ? startX : (float)x);
			fitem.location[1] = (Double.isNaN(y) ? startY : (float)y);
			fsim.addItem(fitem);

			ForceItem f1 = fitem;

			//Create origin for spring - should never change x & y! 
			//NodeItem  n2 = new TableNodeItem();

			Point2D.Double p = layedOutObjects.get(item);
			if(p == null){
				throw new IllegalStateException("Haven't processed before: wtf?");
			}

			double px = p.x;
			double py = p.y;
			/*n2.setX(px);
			n2.setY(py);*/

			ForceItem f2 = new ForceItem();//(ForceItem)n2.get(FORCEITEM);

			//f2.mass = Float.MAX_VALUE;//1.0f;
			//f2.force = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
			//f2.velocity = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
			f2.location = new float[] { (float)px, (float)py };
			//f2.plocation = new float[] { (float)px, (float)py };
			//f2.k = new float[4][2];
			//f2.l = new float[4][2];


			//float coeff = getSpringCoefficient(null);	//Null OK because it isn't used
			//float slen = getSpringLength(null);			//Null OK because it isn't used
			//m_fsim.addSpring(f1, f2, (coeff>=0?coeff:-1.f), (slen>=0?slen:-1.f));
			m_fsim.addSpring(f1, f2, .001f, 0.01f);//XXX: Tweak vals
		}

		if ( m_edgeGroup != null ) {
			if (m_filter == null)
				iter = tsP.tuples();
			else 
				iter = tsP.tuples((Predicate)ExpressionParser.parse(m_filter.toString() + " and isedge()"));
			//iter = m_vis.visibleItems(m_edgeGroup);
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


	}

	/**
	 * Get the mass value associated with the given node. Subclasses should
	 * override this method to perform custom mass assignment.
	 * @param n the node for which to compute the mass value
	 * @return the mass value for the node. By default, all items are given
	 * a mass value of 1.0.
	 */
	protected float getMassValue(VisualItem n) {
		return 1.0f;
	}

	/**
	 * Get the spring length for the given edge. Subclasses should
	 * override this method to perform custom spring length assignment.
	 * @param e the edge for which to compute the spring length
	 * @return the spring length for the edge. A return value of
	 * -1 means to ignore this method and use the global default.
	 */
	protected float getSpringLength(EdgeItem e) {
		return -1.f;
	}

	/**
	 * Get the spring coefficient for the given edge, which controls the
	 * tension or strength of the spring. Subclasses should
	 * override this method to perform custom spring tension assignment.
	 * @param e the edge for which to compute the spring coefficient.
	 * @return the spring coefficient for the edge. A return value of
	 * -1 means to ignore this method and use the global default.
	 */
	protected float getSpringCoefficient(EdgeItem e) {
		return -1.f;
	}

	/**
	 * Get the referrer item to use to set x or y coordinates that are
	 * initialized to NaN.
	 * @return the referrer item.
	 * @see prefuse.util.PrefuseLib#setX(VisualItem, VisualItem, double)
	 * @see prefuse.util.PrefuseLib#setY(VisualItem, VisualItem, double)
	 */
	public VisualItem getReferrer() {
		return referrer;
	}

	/**
	 * Set the referrer item to use to set x or y coordinates that are
	 * initialized to NaN.
	 * @param referrer the referrer item to use.
	 * @see prefuse.util.PrefuseLib#setX(VisualItem, VisualItem, double)
	 * @see prefuse.util.PrefuseLib#setY(VisualItem, VisualItem, double)
	 */
	public void setReferrer(VisualItem referrer) {
		this.referrer = referrer;
	}

	// ------------------------------------------------------------------------
	// ForceItem Schema Addition

	/**
	 * The data field in which the parameters used by this layout are stored.
	 */
	public static final String FORCEITEM = "_forceItem";
	/**
	 * The schema for the parameters used by this layout.
	 */
	public static final Schema FORCEITEM_SCHEMA = new Schema();
	static {
		FORCEITEM_SCHEMA.addColumn(FORCEITEM,
				ForceItem.class,
				new ForceItem());
	}

} // end of class PolarDonutFDRLayout