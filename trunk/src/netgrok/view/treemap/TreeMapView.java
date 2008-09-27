package netgrok.view.treemap;

import netgrok.*;
import netgrok.data.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;

import netgrok.data.*;
import netgrok.data.Data.Group;
import prefuse.*;
import prefuse.activity.*;
import prefuse.action.*;
import prefuse.action.animate.*;
import prefuse.action.assignment.*;
import prefuse.action.filter.VisibilityFilter;
import prefuse.action.layout.graph.*;
import prefuse.controls.*;
import prefuse.data.*;
import prefuse.data.event.*;
import prefuse.data.expression.*;
import prefuse.data.expression.parser.*;
import prefuse.render.*;
import prefuse.util.*;
import prefuse.util.ui.*;

import java.util.Iterator;
import prefuse.visual.*;
import prefuse.visual.sort.*;

/**
 * Code thoughtfully stolen from 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * And his TreeMap demo
 */
public class TreeMapView extends Display implements ComponentListener, GraphListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// our reference to the user's data
	private Data data;

	private Tree tree;
	private VisualTree visualTree;

	private Graph networkGraph;
	private Tree groupTree;

	private boolean hovering;

	// Map row in treemap to graph
	private HashMap<Integer, Integer> nodeGraphToTree;
	private HashMap<Integer, Integer> nodeTreeToGraph;

	private static final String TREE = "tree";
	private static final String TREENODES = "tree.nodes";
	private static final String TREEEDGES = "tree.edges";
	private static final String CONNECTEDNODES = "connected";
	private static final String GROUP = "group";

	private static final String COLOR_BY_BANDWIDTH = "color_by_bandwidth";
	public static final String SIZE_BY_DEGREE = "size_by_degree";

	public TreeMapView() {
		super(new Visualization());
		Data data = Data.getData();

		// Start assuming mouse is outside of window
		hovering = false;

		nodeGraphToTree = new HashMap<Integer, Integer>();
		nodeTreeToGraph = new HashMap<Integer, Integer>();

		this.networkGraph = data.getIPGraph();
		this.groupTree = data.getGroups();


		// Register this class to receive updates from Tree and Graph
		networkGraph.addGraphModelListener(this);
		//groupTree.addGraphModelListener(this);

		// Register this class to receive Component updates
		addComponentListener(this);

		// Setup the VisualTree backed up by a Tree structure
		tree = new Tree();

		// Add the graph's node columns to tree
		Table networkNodeTable = networkGraph.getNodeTable();
		for (int i = 0; i < networkNodeTable.getColumnCount(); i++) {
			String name = networkNodeTable.getColumnName(i);
			Class type = networkNodeTable.getColumnType(i);
			Object dflt = networkNodeTable.getDefault(name);
			tree.addColumn(name, type, dflt);
		}
		
		tree.addColumn(GROUP, String.class, null);
		
		// Add groups into the tree
		addGroups(groupTree);

		// Group flag for our tree
		visualTree = m_vis.addTree(TREE, tree);

		// Set all group nodes non interactive
		Predicate predicate = (Predicate)ExpressionParser.parse("(group != null)");
		m_vis.setInteractive(TREENODES, predicate, false);

		// Renderer 
		m_vis.setRendererFactory(new DefaultRendererFactory(
				new TreeMapRenderer()));

		// make nodes visible only if they are TIME_SELECTED (works with time slider)
		//VisibilityFilter vf = new VisibilityFilter(TREENODES,ExpressionParser.predicate(Data.TIME_SELECTED+"=true"));
		
		// setup colors
		final ColorAction borderColor = new BorderColorAction(TREENODES);

		final StrokeAction strokeWidth = new BorderStrokeAction(TREENODES);

		tree.addColumn(SIZE_BY_DEGREE, 
				ExpressionParser.parse("degree"));
		DataSizeAction size = new DataSizeAction(TREENODES, SIZE_BY_DEGREE);

		ColorAction highlightColor = new HighlightedColorAction(this, TREENODES);

		// Setup layout
		ActionList layout = new ActionList();
		//layout.add(vf);
		layout.add(data.getLockAction());
		layout.add(size);
		layout.add(new SquarifiedHostTreeMapLayout(TREE)); 
		layout.add(new RepaintAction());
		layout.add(data.getUnlockAction());
		m_vis.putAction("layout", layout);

		// full paint
		ActionList fullPaint = new ActionList();
		fullPaint.add(data.getLockAction());
		fullPaint.add(highlightColor);
		fullPaint.add(strokeWidth);
		fullPaint.add(borderColor);
		fullPaint.add(data.getUnlockAction());
		m_vis.putAction("fullPaint", fullPaint); 

		// Listen for mouse events
		addControlListener(new ControlAdapter() {
			public void itemEntered(VisualItem item, MouseEvent e) {
				// TODO: When you enter an item update the details on demand
				Data data = Data.getData();
				Integer row = nodeTreeToGraph.get(item.getRow());
				if (row != null) {
					data.selectNode(row);
					highlightConnected(item);
					treeMapChanged();
				}
			}
			public void itemExited(VisualItem item, MouseEvent e) {
				// TODO: When you exit and item clear out the details on demand
				unhighlightAllConencted();
				treeMapChanged();
			}
			public void mouseEntered (MouseEvent e) {
				hovering = true;
			}
			public void mouseExited(MouseEvent e) {
				hovering = false;
				unhighlightAllConencted();
				treeMapChanged();
			}
		}); 
		setSize(800,600);
		setItemSorter(new TreeDepthItemSorter());

		// paint treemap
		treeMapChanged();
	}

	private void addGroups(Tree groups) {
		Node curr_orig = groups.getRoot();
		Node curr_local = tree.addRoot();
		curr_local.set(GROUP, curr_orig.get(GROUP));

		recursiveGroupAdd(groups, curr_orig, curr_local);
	}

	private void recursiveGroupAdd(Tree groups, Node curr_orig, Node curr_local) {
		Iterator it = curr_orig.children();
		while (it.hasNext()) {
			Node o_node = (Node)it.next();
			Node n_node = tree.addChild(curr_local);
			n_node.set(GROUP, o_node.get(GROUP));
			recursiveGroupAdd(groups, o_node, n_node);
		}
	}

	private void highlightConnected(VisualItem item) {
		Data data = Data.getData();
		data.getLock().lock();
		if (item.get(GROUP) == null) {
			// Get graph node corresponding to highlighted node TODO FIX NPE
			item.setHighlighted(true);
			Node graphNode = networkGraph.getNode(
				nodeTreeToGraph.get(item.getRow()));
			// Highlight all connected nodes
			Iterator edges = networkGraph.edges(graphNode);
			while (edges.hasNext()) {
				Node dst = ((Edge)edges.next()).getAdjacentNode(graphNode);
				Node dstTreeNode = visualTree.getNode(nodeGraphToTree.get(dst.getRow()));
				((NodeItem)dstTreeNode).setHighlighted(true);
			}
		}
		data.getLock().unlock();
	}

	private void unhighlightAllConencted() {
		Data data = Data.getData();
		data.getLock().lock();
		Iterator nodeIt = visualTree.nodes();
		while (nodeIt.hasNext()) {
			NodeItem node = (NodeItem)nodeIt.next();
			node.setHighlighted(false);
		}
		data.getLock().unlock();
	}

	private void treeMapChanged() {
		m_vis.run("layout");
		m_vis.run("fullPaint");
	}

	public boolean isHovering() {
		return hovering;
	}

	public void graphChanged(Graph g, String table, int start, int end, int col, int type) {
		if (g == networkGraph) {
			switch (type) {
			case EventConstants.INSERT:
				if (table.equals("nodes")) {
					Node child = tree.addNode();
					nodeGraphToTree.put(start, child.getRow());
					nodeTreeToGraph.put(child.getRow(), start);
				}
				break;
			case EventConstants.UPDATE:
				if (table.equals("nodes")) {
					if (start == end) {
						// Update our value with the value passed in
						// since we have root in our tree we must skip one row
						tree.getNodeTable().set(nodeGraphToTree.get(start), col, 
								g.getNodeTable().get(start, col));
						// When address is set move node into the correct group
						if (networkGraph.getNodeTable().getColumnName(col).equals(
								Data.GROUPS)) {
							Node node = tree.getNode(nodeGraphToTree.get(start));
							if (node != null) {
								addHostToTree(tree.getRoot(), node);
							}
						}
					}
				}
				break;
			}
			treeMapChanged();
		}
	}

	private void addHostToTree(Node curr, Node to_insert) {
		String curr_group = null;
		// If this node is a group continue
		if ((curr_group = curr.getString(GROUP)) != null) {
			String groups = to_insert.getString(Data.GROUPS);
			if (groups != null && groups.contains(curr_group)) {
				boolean inchild = false;
				Iterator children = curr.children();
				while (children.hasNext()) {
					Node curr_child = (Node)children.next();
					String curr_child_group = null;
					if ((curr_child_group = curr_child.getString(GROUP)) != null) {
						if (groups != null && groups.contains(curr_child_group)) {
							inchild = true;
							addHostToTree(curr_child, to_insert);
						}
					}
				} 
				if (!inchild) {
					tree.addChildEdge(curr, to_insert);
				}
			}
		}
	}

	public static void main(String argv[]) {
		Data data = Data.getData();
		JComponent treemap = new TreeMapView();

		JFrame frame = new JFrame("TreeMapView");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(treemap);
		frame.pack();
		frame.setVisible(true);
		LocalSensor sensor = new LocalSensor();
		sensor.sniff("en1");
	}

	/* Unused Component methods */
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	/* -------------------------*/

	public void componentResized(ComponentEvent e) {
		treeMapChanged();
	}

	/**
	 * Set the stroke color for drawing treemap node outlines. A graded
	 * grayscale ramp is used, with higer nodes in the tree drawn in
	 * lighter shades of gray.
	 */
	public static class BorderColorAction extends ColorAction {

		public BorderColorAction(String group) {
			super(group, VisualItem.STROKECOLOR);
		}

		public int getColor(VisualItem item) {
			NodeItem nitem = (NodeItem)item;
			if ( nitem.isHover() )
				return ColorLib.rgb(255,255,255);
			return ColorLib.gray(0);
		}
	}

	public static class BorderStrokeAction extends StrokeAction {

		public BorderStrokeAction(String group) {
			super(group);
		}

		public BasicStroke getStroke(VisualItem item) {
			NodeItem nitem = (NodeItem)item;
			// Width is 1 by default
			int width = 1;
			if (item.get(GROUP) != null) {
				width = 5;
			}
			return new BasicStroke(width);
		}
	}

	public static class HighlightedColorAction extends ColorAction {

		private TreeMapView view;
		private int[] hotPalette, bwPalette;

		public HighlightedColorAction(TreeMapView view, String group) {
			super(group, VisualItem.FILLCOLOR);
			this.view = view;
			hotPalette = ColorLib.getInterpolatedPalette(255, 
					ColorLib.rgba(255, 255, 0, 255), ColorLib.rgba(255, 0, 0, 255));
			bwPalette = ColorLib.getInterpolatedPalette(255, 
					ColorLib.rgba(230, 230, 230, 255), ColorLib.rgba(100, 100, 100, 255));
		}

		public int getColor(VisualItem item) {
			long value = Math.round(item.getDouble(Data.BANDWIDTH_RANK)*254);
			if (view.isHovering() &&
					!item.isHighlighted()) {
				return bwPalette[(int)value];
			}
			if (item.get(Data.GROUP) != null) {
				return Color.WHITE.getRGB();
			}
			return hotPalette[(int)value];
		}
	}

	/**
	 * A renderer for treemap nodes. Draws simple rectangles, but defers
	 * the bounds management to the layout. Leaf nodes are drawn fully,
	 * higher level nodes only have their outlines drawn. Labels are
	 * rendered for top-level (i.e., depth 1) subtrees.
	 */
	public static class TreeMapRenderer extends ShapeRenderer {
		private Rectangle2D m_bounds = new Rectangle2D.Double();

		public TreeMapRenderer() {
			m_manageBounds = false;
		}
		public int getRenderType(VisualItem item) {
			if ( ((NodeItem)item).getChildCount() == 0 ) {
				// if a leaf node, both draw and fill the node
				return RENDER_TYPE_DRAW_AND_FILL;
			} else {
				// if not a leaf, only draw the node outline
				return RENDER_TYPE_DRAW;
			}
		}
		protected Shape getRawShape(VisualItem item) {
			m_bounds.setRect(item.getBounds());
			return m_bounds;
		}

		public void render(Graphics2D g, VisualItem item) {
			super.render(g, item);
			Rectangle2D b = item.getBounds();
			// Only draw name if node represents a group
			if (item.get(GROUP) == null &&
				item.get(Data.ADDRESS) != null) {
			}
		}
	} 
} 

