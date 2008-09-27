package netgrok.view.network;

import netgrok.data.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.JFrame;

import prefuse.*;
import prefuse.action.*;
import prefuse.action.assignment.*;
import prefuse.action.filter.*;
import prefuse.activity.*;
import prefuse.controls.*;
import prefuse.data.*;
import prefuse.data.expression.parser.*;
import prefuse.data.event.*;
import prefuse.render.*;
import prefuse.util.*;
import prefuse.visual.*;

import java.util.logging.*;

// TODO: See if Adam's lock & unlock Actions allow us to add selected edges without 
//       searching the entire graph
// TODO: add edge arrows?
// TODO: create labels for edges
// TODO: really make edges non-interactive (click events should only only process 
//       visible edges)
// TODO: clean up code to make more compact (e.g. create an addLabels(group))

public class NetworkView extends Display {
	
	private static final long serialVersionUID = 1;
	
	public static final String RING = "ring";
	public static final String NETWORK = "network";
	public static final String HOSTS = "network.nodes";
	public static final String CONNECTIONS = "network.edges";
	
	public static final String GROUP = "group";
	public static final String GROUPS = "groups";
	public static final String GROUP_ID = "group_id";
	public static final String GROUP_SIZE = "group_size";
	
	public static final String SIZE_BY_DEGREE = "size_by_degree";
	public static final String COLOR_BY_BANDWIDTH = "color_by_bandwidth";
	
	public static final String HOST_LABELS = "host_labels";
	public static final String GROUP_LABELS = "group_labels";
	
	public static final String HIDE_EDGE = "hide_edge";
	public static final String FIXED = "fixed";
	public static final String SELECT_HOST = "select_host";
	public static final String IN_CLICK_GROUP = "in_click_group";
	public static final String IN_HOVER_GROUP = "in_hover_group";
	public static final String IS_LOCAL = "is_local";
	public static final String IS_FOREIGN = "is_foreign";
	public static final String EDGE_SIZE = "edge_size";
	
	// i'm not happy with this
	private NetworkZoomControl zoom = new NetworkZoomControl(this);
	
	public NetworkView() {
		// create the network visualization
		super(new Visualization());
		
		// set up our static (for now) display parameters
		setSize(500,500);
		zoom(new Point2D.Float(0, 0), 1.4);
		pan(333, 217);
		
		setHighQuality(true);
		
		// create the renderer for this visualization
		DefaultRendererFactory renderer = new DefaultRendererFactory();
		m_vis.setRendererFactory(renderer);
		
		// add the network graph to the visualization as the NETWORK group
		Graph data = Data.getData().getIPGraph();
		
		// create the visual graph
		VisualGraph visual = m_vis.addGraph(NETWORK, data);
		
		// these attribute determines when to show edges and node details
		visual.addColumn(FIXED, boolean.class, false);
		visual.addColumn(IN_HOVER_GROUP, boolean.class, false);
		visual.addColumn(IN_CLICK_GROUP, boolean.class, false);
		visual.addColumn(HIDE_EDGE, "!in_hover_group and !in_click_group");
		visual.addColumn(SELECT_HOST, "in_hover_group or in_click_group");
		visual.addColumn(IS_LOCAL, "position('Local Network', groups) != -1");
		visual.addColumn(IS_FOREIGN, "position('Foreign Network', groups) != -1");
		visual.addColumn(EDGE_SIZE, 
				"if bandwidth < 1048576 then 1 else " +
				"(if bandwidth < 10485760 then 2 else 3)"); 
		
		// create the label for a host
		visual.addColumn(VisualItem.LABEL,
				"if hostname = '" + Data.UNRESOLVED + "' or " +
				   "hostname = '" + Data.NO_HOSTNAME + "' " +
				"then host_address else hostname");
		
		// create expression columns for our datasize and datacolor
		// actions that require the data to be normalized
		visual.addColumn(COLOR_BY_BANDWIDTH, 
				ExpressionParser.parse("round(bandwidth_rank*254)"));
		visual.addColumn(SIZE_BY_DEGREE, 
				ExpressionParser.parse("(degree_rank+1)*25"));
		
		ActionList groups = createGroups(visual, renderer);
		ActionList boundary = createBoundary(renderer);
		ActionList hosts = createHosts(renderer);
		ActionList edges = createEdges(renderer);
		
		ActionList view = new ActionList(Activity.INFINITY);
		
		view.add(Data.getData().getLockAction());
		view.add(hosts);
		view.add(edges);
		view.add(groups);
		view.add(boundary);
		view.add(Data.getData().getUnlockAction());
		view.add(new RepaintAction());
		
		m_vis.putAction("view", view);
		
		// the group drag control is "bubble-aware" and increments the 
		// selected field on clicks, and zooms on hosts		
		addControlListener(zoom);
		addControlListener(new MouseController());
		addControlListener(new PopupMenuController(zoom));
		addControlListener(new PanControl());
		
		// add the ring regardless of whether or not we have data
		boundary.run();
		
		// wait for data to appear before starting the visualization
		visual.addGraphModelListener(new GraphListener() {
			public void graphChanged(Graph graph, String node_type, int s, int e, int c, int t) {	
				m_vis.run("view");
				graph.removeGraphModelListener(this);
			}
		});
	}
	
	// creates a host representation for every node added to the graph
	private ActionList createHosts(DefaultRendererFactory renderer) { 
		ActionList hosts = new ActionList();

		// create a visual schema for the labels
	    Schema schema = PrefuseLib.getVisualItemSchema(); 
	    schema.setDefault(VisualItem.INTERACTIVE, false); 
	    schema.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(0)); 
	    schema.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma", 1));

		// instantiate the schema to get our label group
		m_vis.addDecorators(HOST_LABELS, HOSTS, schema);
		
		// these labels are laid out atop the hosts
		hosts.add(new LabelLayout(HOST_LABELS));
		
		// make nodes visible only if they are TIME_SELECTED (works with time slider)
		VisibilityFilter vf_hosts = new VisibilityFilter(HOSTS,ExpressionParser.predicate(Data.TIME_SELECTED+"=true"));
		VisibilityFilter vf_labels = new VisibilityFilter(HOST_LABELS, ExpressionParser.predicate(Data.TIME_SELECTED+"=true"));
		hosts.add(vf_hosts);
		hosts.add(vf_labels);
		
		// hosts are sizes from 1-10 (the largest node is 10 times bigger than
		// the smallest). they get their size from their degree rank
		DataSizeAction font_size = new DataSizeAction(HOST_LABELS, SIZE_BY_DEGREE);
		
		// since we want the area to grow linearly, use an exponential scale
		font_size.setScale(Constants.SQRT_SCALE);
		font_size.setMinimumSize(1);
		font_size.setMaximumSize(25);
		hosts.add(font_size);
		
		// add the labels to the renderer
		LabelRenderer label = new LabelRenderer(VisualItem.LABEL);
		renderer.add("ingroup('host_labels')", label);
		
		// hosts *are* interactive
		m_vis.setInteractive(HOSTS, null, true);
		
		// render hosts with a base *diameter* of 10
		renderer.setDefaultRenderer(new ShapeRenderer(15));
		
		// make nodes visible only if they are TIME_SELECTED (works with time slider)
		VisibilityFilter vf = new VisibilityFilter(HOSTS,ExpressionParser.predicate(Data.TIME_SELECTED+"=true"));
		hosts.add(vf);
		
		// create the fill for hosts
		int[] palette = ColorLib.getInterpolatedPalette(255, 
	  		ColorLib.rgba(100, 255, 0, 255), ColorLib.rgba(255, 100, 0, 255));		
		
		ColorAction fill = new DataColorAction(HOSTS, COLOR_BY_BANDWIDTH,
		    Constants.NUMERICAL, VisualItem.FILLCOLOR, palette);
		fill.add(Data.IS_ZERO_BYTE, ColorLib.gray(255));
		hosts.add(fill);
		
		// hosts are sizes from 1-10 (the largest node is 10 times bigger than
		// the smallest). they get their size from their degree rank
		DataSizeAction size = new DataSizeAction(HOSTS, SIZE_BY_DEGREE);
		
		// since we want the area to grow linearly, use an exponential scale
		size.setScale(Constants.SQRT_SCALE);
		size.setMinimumSize(1);
		size.setMaximumSize(25);
		hosts.add(size);
		
		// make hosts round
		ShapeAction shape = new ShapeAction(HOSTS, Constants.SHAPE_ELLIPSE);
		hosts.add(shape);
		
		// create a dashed-ring for zero-byte hosts
		StrokeAction stroke = new StrokeAction(HOSTS, 
				StrokeLib.getStroke(1)); 
		stroke.add(Data.IS_ZERO_BYTE, StrokeLib.getStroke(1, StrokeLib.DASHES));
		hosts.add(stroke); 
		
		// highlight selected nodes
		ColorAction stroke_color = new ColorAction(HOSTS, VisualItem.STROKECOLOR, 
				ColorLib.gray(128));
		stroke_color.add(SELECT_HOST, ColorLib.rgba(100, 100, 255, 255));
		hosts.add(stroke_color);
		
		return hosts;
	}
	
	// creates a visual edge for every edge added to the graph
	private ActionList createEdges(DefaultRendererFactory renderer) {		
		ActionList edges = new ActionList();
		
		// edges are not interactive
		m_vis.setInteractive(NETWORK,  null, false);
		
		// edge width
		StrokeAction size = new StrokeAction(CONNECTIONS, StrokeLib.getStroke(1)); 
		size.add("edge_size = 2", StrokeLib.getStroke(2));
		size.add("edge_size = 3", StrokeLib.getStroke(3));
		edges.add(size);
		
		// make nodes visible only if they are TIME_SELECTED (works with time slider)
		VisibilityFilter vf = new VisibilityFilter(CONNECTIONS,ExpressionParser.predicate(Data.TIME_SELECTED+"=true"));
		edges.add(vf);

		// render edges using the default edge renderer
		renderer.setDefaultEdgeRenderer(new EdgeRenderer());
		
		// create the color for edges
		ColorAction color = new ColorAction(CONNECTIONS, VisualItem.STROKECOLOR, 
				ColorLib.gray(128));
		color.add(HIDE_EDGE, ColorLib.alpha(0));
		edges.add(color);
		
		return edges;
	}
	
	// creates groups and returns an action list to draw them
	private ActionList createGroups(VisualGraph graph, DefaultRendererFactory renderer) {
		// the ActionList this function returns
		ActionList groups = new ActionList();
		
		// create the groups table that stores the bubble that is
		// rendered around groups
		VisualTable table = (VisualTable) m_vis.addAggregates(GROUPS);
		table.addColumn(GROUP, String.class);
		table.addColumn(GROUP_ID, int.class);
		table.addColumn(GROUP_SIZE, int.class, 0);
		table.addColumn(Data.HOME_IP, byte[].class);
		table.addColumn(VisualItem.POLYGON, float[].class);
	
		Iterator<?> it = Data.getData().getGroups().nodes();
		
		// add an aggregate item for each group
		for (int i=0; it.hasNext(); i++) {
			Node node = (Node)it.next();
			String group = node.getString(GROUP);
			
			// don't add an aggregate for foreign network nodes
			if (group.equals(Data.FOREIGN_GROUP) || group.equals("Network"))
				continue;
			
			AggregateItem item = (AggregateItem) table.addItem();
			
			item.setString(GROUP, group);
			item.setInt(GROUP_ID, i);
			item.set(Data.HOME_IP, node.get(Data.HOME_IP));
		}
		
		// watch for nodes and add them to their respective groups
		graph.addGraphModelListener(new GraphListener() {
			public void graphChanged(Graph graph, String node_type, int s, int e, int c, int t) {
				// there are two ways group info can come in: with a newly 
				// created node, or with an old node
				if (node_type.equals("nodes")) {
					for (int i=s; i<=e; i++) {
						Node node = (Node) graph.getNode(i);
						
						if (t == EventConstants.UPDATE) {
							if (node.getColumnName(c).equals(GROUPS)) {
								String groups = node.getString(GROUPS);
								addToAggregates((VisualItem)node, groups);
							}
						}
							
						if (t == EventConstants.INSERT) {
							String groups = node.getString(GROUPS);
							
							if (groups != null)
								addToAggregates((VisualItem)node, groups);
						}
					}
				}
				
				if (t == EventConstants.INSERT) {
					Iterator<?> hover = m_vis.items(HOSTS, IN_HOVER_GROUP);
					
					while (hover.hasNext()) {
						Iterator<?> edges = ((Node)hover.next()).edges();
							
						while (edges.hasNext())
							((Edge)edges.next()).setBoolean(IN_HOVER_GROUP, true);
					}
					
					Iterator<?> click = m_vis.items(HOSTS, IN_CLICK_GROUP);
					
					while (click.hasNext()) {
						Iterator<?> edges = ((Node)click.next()).edges();
							
						while (edges.hasNext())
							((Edge)edges.next()).setBoolean(IN_CLICK_GROUP, true);
					}
				}
			}
		});
		
		// render the bubbles surrounding groups
		Renderer render = new PolygonRenderer(Constants.POLY_TYPE_CURVE);
		((PolygonRenderer) render).setCurveSlack(0.15f);
		renderer.add("ingroup('groups')", render);
		
		// create the fill for bubbles surrounding groups 
		int[] palette = ColorLib.getCategoryPalette(255);
		
		for (int i=0; i<palette.length; i++) 
			palette[i] = ColorLib.setAlpha(palette[i], 50); 
		
		ColorAction fill = new DataColorAction(GROUPS, GROUP_ID,
			Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
		groups.add(fill);
		
		// create the stroke for group bubbles
		ColorAction stroke = new ColorAction(GROUPS, VisualItem.STROKECOLOR);
		stroke.setDefaultColor(ColorLib.gray(200));
		stroke.add("_hover", ColorLib.rgb(255,100,100));
		groups.add(stroke);
		
		// the layout computes the convex hull around the groups
		groups.add(new GroupLayout(GROUPS));
		
		// create a visual schema for the group labels
	    Schema schema = PrefuseLib.getVisualItemSchema(); 
	    schema.setDefault(VisualItem.INTERACTIVE, false); 
	    schema.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(255, 128)); 
	    schema.setDefault(VisualItem.FONT, FontLib.getFont("Arial", Font.BOLD, 10));
	    
		// instantiate the schema to get our label group
		m_vis.addDecorators(GROUP_LABELS, GROUPS, 
				ExpressionParser.predicate("group_size > 0 and _hover"), 
				schema);
		
		// these labels are laid out atop the group bubble
		groups.add(new LabelLayout(GROUP_LABELS));
		
		// add the labels to the renderer
		LabelRenderer label = new LabelRenderer(GROUP);
		renderer.add("ingroup('group_labels')", label);
		
		return groups;
	}
	
	private void addToAggregates(VisualItem node, String groups) {	
		AggregateTable table = (AggregateTable)m_vis.getGroup(GROUPS);
		
		for (Iterator<?> it=table.tuples(); it.hasNext();) {
			AggregateItem item = (AggregateItem)it.next();
			
			// the node belongs in this group
			if (groups.indexOf(item.getString(GROUP)) >= 0) {
				item.addItem(node);
				item.setInt(GROUP_SIZE, item.getInt(GROUP_SIZE)+1);
			}
		}
	}
	
	// the boundary is a ring that separates local and foreign hosts
	private ActionList createBoundary(DefaultRendererFactory renderer) {
		// the ActionList this function returns
		ActionList boundary = new ActionList();
		
		// create the visual ring item and add it to the visualization
		m_vis.addTable(RING).addItem();
		
		// the ring is not interactive
		m_vis.setInteractive(RING, null, false);
		
		// render the ring
		renderer.add("ingroup('ring')", new ShapeRenderer(290));

		// create an ellipse shape for the ring
		ShapeAction shape = new ShapeAction(RING, Constants.SHAPE_ELLIPSE);
		boundary.add(shape);
		
		// create a dashed-ring
		StrokeAction stroke = new StrokeAction(RING, 
				StrokeLib.getStroke(1, StrokeLib.DASHES)); 
		boundary.add(stroke); 
		
		// make the ring a light semi-transparent blue
		ColorAction color = new ColorAction(RING, VisualItem.STROKECOLOR, 
				ColorLib.rgba(0, 0, 100, 150));
		boundary.add(color);
		
		// this boundary restricts local hosts inside the ring
		// and foreign hosts to the outside
		// FOR SOME REASON, ForeignHostLayout *MUST* COME BEFORE LocalHostLayout
		boundary.add(new ForeignHostLayout(zoom, 150, 500, 500, NETWORK, IS_FOREIGN));
		boundary.add(new LocalHostLayout(140, 0, 0, NETWORK, IS_LOCAL));
		
		return boundary;
	}
	
	public static void main(String[] argv) {
		// prefuse has issues, so ignore the non-fatal warnings
		Logger.getLogger("prefuse").setLevel(Level.WARNING);
		
		// should someone wish to run this directly
		LocalSensor local = new LocalSensor();
		local.sniff("en1");
		
		JFrame frame = frame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static JFrame frame() {
		NetworkView view = new NetworkView();
		JFrame frame = new JFrame("NetGrok - Network View");
		frame.getContentPane().add(view);
		frame.pack();
		return frame;
	}
}