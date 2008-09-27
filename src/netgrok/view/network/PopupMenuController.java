package netgrok.view.network;

import netgrok.data.*;

import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

import javax.swing.*;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.data.*;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
import prefuse.visual.*;

public class PopupMenuController extends ControlAdapter implements ActionListener {
	
	private Display display;
	private VisualItem action_item;
	private NetworkZoomControl zoom;
	
	private static final String DNS_LOOKUP = "dns_lookup";
	private static final String FIX_POSITION = "fix_position";
	private static final String SHOW_EDGES = "show_edges";
	private static final String RESET_ZOOM = "reset_zoom";
	private static final String SHOW_GROUP_EDGES = "show_group_edges";
	private static final String HIDE_GROUP_EDGES = "hide_group_edges";
	private static final String FIX_GROUP = "fix_group";
	private static final String UNFIX_GROUP = "unfix_group";
	private static final String FIX_ALL = "fix_all";
	private static final String UNFIX_ALL = "unfix_all";
	private static final String SHOW_ALL_EDGES = "show_all_edges";
	private static final String HIDE_ALL_EDGES = "hide_all_edges";
	
	public PopupMenuController(NetworkZoomControl zoom) {
		this.zoom = zoom;
	}
	
	public void actionPerformed(ActionEvent e) {
		// look up the dns hostname for this node
		if (e.getActionCommand().equals(DNS_LOOKUP))
			Data.getHostname((NodeItem)action_item);
		
		else if (e.getActionCommand().equals(FIX_POSITION)) {
			boolean fixed = action_item.getBoolean(NetworkView.FIXED);
			action_item.setFixed(!fixed);
			action_item.setBoolean(NetworkView.FIXED, !fixed);
		}
		
		else if (e.getActionCommand().equals(SHOW_EDGES)) {
			boolean click = action_item.getBoolean(NetworkView.IN_CLICK_GROUP);
			action_item.setBoolean(NetworkView.IN_CLICK_GROUP, !click);
			action_item.setBoolean(NetworkView.IN_CLICK_GROUP, !click);
			
			// add all adjacent edges to click group as well
			Iterator<?> it = ((Node) action_item).edges();
			while (it.hasNext()) {
				Edge edge = (Edge) it.next();
				Node adjacent = edge.getAdjacentNode((Node) action_item);
				
				edge.setBoolean(NetworkView.IN_CLICK_GROUP, 
						!click || adjacent.getBoolean(NetworkView.IN_CLICK_GROUP));
			}
		}
		
		else if (e.getActionCommand().equals(RESET_ZOOM)) {
			zoom.reset();
		}
		
		else if (e.getActionCommand().equals(SHOW_ALL_EDGES)) {
			Iterator<?> it = display.getVisualization().visibleItems(); 
			while (it.hasNext()) {
				VisualItem item = (VisualItem) it.next();
				
				if (!(item instanceof AggregateItem))
					item.setBoolean(NetworkView.IN_CLICK_GROUP, true);
			}
		}
		
		else if (e.getActionCommand().equals(HIDE_ALL_EDGES)) {
			Iterator<?> it = display.getVisualization().visibleItems(); 
			while (it.hasNext()) {
				VisualItem item = (VisualItem) it.next();
				
				if (!(item instanceof AggregateItem))
					item.setBoolean(NetworkView.IN_CLICK_GROUP, false);
			}
		}
		
		else if (e.getActionCommand().equals(SHOW_GROUP_EDGES)) {
			Iterator<?> nodes = ((AggregateItem)action_item).items(); 			
			
			while (nodes.hasNext()) {
				Node node = (Node) nodes.next();
				node.setBoolean(NetworkView.IN_CLICK_GROUP, true);
				
				// add all adjacent edges to click group as well
				Iterator<?> edges = node.edges();
				
				while (edges.hasNext()) {
					Edge edge = (Edge) edges.next();
					edge.setBoolean(NetworkView.IN_CLICK_GROUP, true);
				}
			}
		}
		
		else if (e.getActionCommand().equals(HIDE_GROUP_EDGES)) {
			Iterator<?> nodes = ((AggregateItem)action_item).items(); 			
			
			while (nodes.hasNext()) {
				Node node = (Node) nodes.next();
				node.setBoolean(NetworkView.IN_CLICK_GROUP, false);
				
				// add all adjacent edges to click group as well
				Iterator<?> edges = node.edges();
				
				while (edges.hasNext()) {
					Edge edge = (Edge) edges.next();
					Node adjacent = edge.getAdjacentNode((Node) node);
					
					edge.setBoolean(NetworkView.IN_CLICK_GROUP, 
							adjacent.getBoolean(NetworkView.IN_CLICK_GROUP));
				}
			}
		}
		
		else if (e.getActionCommand().equals(FIX_GROUP)) {
			Iterator<?> nodes = ((AggregateItem)action_item).items(); 			
			
			while (nodes.hasNext()) {
				Node node = (Node) nodes.next();
				((VisualItem) node).setFixed(true);
				node.setBoolean(NetworkView.FIXED, true);
			}
		}
		
		else if (e.getActionCommand().equals(UNFIX_GROUP)) {
			Iterator<?> nodes = ((AggregateItem)action_item).items(); 			
			
			while (nodes.hasNext()) {
				Node node = (Node) nodes.next();
				((VisualItem) node).setFixed(false);
				node.setBoolean(NetworkView.FIXED, false);
			}
		}
		
		else if (e.getActionCommand().equals(FIX_ALL)) {
			Iterator<?> nodes = display.getVisualization().items(NetworkView.HOSTS); 			
			
			while (nodes.hasNext()) {
				Node node = (Node) nodes.next();
				((VisualItem) node).setFixed(true);
				node.setBoolean(NetworkView.FIXED, true);
			}
		}
		
		else if (e.getActionCommand().equals(UNFIX_ALL)) {
			Iterator<?> nodes = display.getVisualization().items(NetworkView.HOSTS); 			
			
			while (nodes.hasNext()) {
				Node node = (Node) nodes.next();
				((VisualItem) node).setFixed(false);
				node.setBoolean(NetworkView.FIXED, false);
			}
		}
	}
	
	public void itemClicked(VisualItem item, MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			
			// set the action item
			action_item = item;
			
			// create the menu
			JPopupMenu menu = new JPopupMenu();
			
			if (item instanceof NodeItem) { 
				
				// create the DNS lookup item
				boolean looked_up = !item.getString(Data.HOSTNAME).equals(Data.UNRESOLVED);
				
				JCheckBoxMenuItem lookup = 
					new JCheckBoxMenuItem("DNS Lookup", looked_up);
				lookup.setActionCommand(DNS_LOOKUP);
				lookup.addActionListener(this);
				menu.add(lookup);
				
				// set the always show edges option
				JCheckBoxMenuItem clicked = 
					new JCheckBoxMenuItem("Show Edges", 
						item.getBoolean(NetworkView.IN_CLICK_GROUP));
				clicked.setActionCommand(SHOW_EDGES);
				clicked.addActionListener(this);
				menu.add(clicked);
				
				// allow user to fix item position
				JCheckBoxMenuItem fix = 
					new JCheckBoxMenuItem("Fix Position", 
						item.getBoolean(NetworkView.FIXED));
				fix.setActionCommand(FIX_POSITION);
				fix.addActionListener(this);
				menu.add(fix);
			}
			
			if (item instanceof AggregateItem) { 
				JMenuItem show_all = new JMenuItem("Show Group Edges");
				show_all.setActionCommand(SHOW_GROUP_EDGES);
				show_all.addActionListener(this);
				menu.add(show_all);
				
				JMenuItem hide_all = new JMenuItem("Hide Group Edges");
				hide_all.setActionCommand(HIDE_GROUP_EDGES);
				hide_all.addActionListener(this);
				menu.add(hide_all);
				
				JMenuItem fix_group = new JMenuItem("Fix Group Position");
				fix_group.setActionCommand(FIX_GROUP);
				fix_group.addActionListener(this);
				menu.add(fix_group);
				
				JMenuItem unfix_group = new JMenuItem("UnFix Group Position");
				unfix_group.setActionCommand(UNFIX_GROUP);
				unfix_group.addActionListener(this);
				menu.add(unfix_group);
			}
			
			JMenuItem reset = new JMenuItem("Reset Zoom");
			reset.setActionCommand(RESET_ZOOM);
			reset.addActionListener(this);
			menu.add(reset);
			
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			
			// save the display for zoom reset 
			this.display = (Display) e.getComponent();
			
			// create the menu
			JPopupMenu menu = new JPopupMenu();

			JMenuItem show_all = new JMenuItem("Show All Edges");
			show_all.setActionCommand(SHOW_ALL_EDGES);
			show_all.addActionListener(this);
			menu.add(show_all);
			
			JMenuItem hide_all = new JMenuItem("Hide All Edges");
			hide_all.setActionCommand(HIDE_ALL_EDGES);
			hide_all.addActionListener(this);
			menu.add(hide_all);
			
			JMenuItem fix_all = new JMenuItem("Fix All Hosts");
			fix_all.setActionCommand(FIX_ALL);
			fix_all.addActionListener(this);
			menu.add(fix_all);
			
			JMenuItem unfix_all = new JMenuItem("Unix All Hosts");
			unfix_all.setActionCommand(UNFIX_ALL);
			unfix_all.addActionListener(this);
			menu.add(unfix_all);
			
			JMenuItem reset = new JMenuItem("Reset Zoom");
			reset.setActionCommand(RESET_ZOOM);
			reset.addActionListener(this);
			menu.add(reset);
			
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
}