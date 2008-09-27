package test;

import java.awt.BorderLayout;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import javax.swing.WindowConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import netgrok.data.*;
import netgrok.view.table.*;
import netgrok.view.network.*;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Graph;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class AdamTestWindow extends javax.swing.JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JSplitPane jSplitPane1;

	//Graph graph;
	Data data;
	Visualization vis;
	//Visualization vis2;
	Display display;
	//Display display2;
	JTable node_table;

	private static final String GRAPH = "graph";
	private static final String NODES = "graph.nodes";
	private static final String EDGES = "graph.edges";
	private static final String COLOR = "color";
	private static final String LAYOUT = "layout";
	
	/**
	* Auto-generated main method to display this JFrame
	*/
	public static void main(String[] args) {
		Data d = Data.getData();
		SensorServer ss = new SensorServer(1234,d);
		ss.start();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				AdamTestWindow inst = new AdamTestWindow();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
				inst.setTitle("Prefuse Test Application");
			}
		});
	}
	
	public AdamTestWindow() {
		super();
		data = Data.getData();
		//readData();
		initVisualization();
		initGUI();
	}
	
	private void initVisualization() {
		/*try
		{
			graph = new GraphMLReader().readGraph("/socialnet.xml");
		} catch ( DataIOException e ) {
			e.printStackTrace();
			System.err.println("Error loading graph. Exiting...");
			System.exit(1);
		}*/
		Graph graph = data.getIPGraph();
		
		vis = new Visualization();
		vis.add(GRAPH, graph);

		LabelRenderer r = new LabelRenderer(data.ADDRESS);
		r.setRoundedCorner(8,8);
		r.setRenderType(LabelRenderer.RENDER_TYPE_DRAW_AND_FILL);
		vis.setRendererFactory(new DefaultRendererFactory(r));
		
		/*int [] palette = new int[] {
				ColorLib.rgb(255,180,180), ColorLib.rgb(190,190,255)
		};
		DataColorAction fill = new DataColorAction("graph.nodes","gender",
				Constants.NOMINAL,VisualItem.FILLCOLOR,palette);*/
		
		ColorAction draw = new ColorAction(NODES,VisualItem.STROKECOLOR,ColorLib.gray(0));
		ColorAction fill = new ColorAction(NODES,VisualItem.FILLCOLOR,ColorLib.rgb(210, 210, 255));
		ColorAction text = new ColorAction(NODES,VisualItem.TEXTCOLOR,ColorLib.gray(0));
		ColorAction edges = new ColorAction(EDGES,VisualItem.STROKECOLOR,ColorLib.gray(200));
		ActionList color = new ActionList(Activity.INFINITY,1000);
		color.add(data.getLockAction());
		color.add(draw);
		color.add(fill);
		color.add(text);
		color.add(edges);
		color.add(data.getUnlockAction());
		
		//ActionList layout = new ActionList(Activity.INFINITY,100);
		ActionList layout = new ActionList(Activity.INFINITY,1000);
		layout.add(data.getLockAction());
		layout.add(new GridMinusCircle(150,10,NODES));
		layout.add(data.getUnlockAction());
		layout.add(new RepaintAction());
		
		vis.putAction(COLOR, color);
		vis.putAction(LAYOUT,layout);

		display = new Display(vis);
		display.setSize(720,500);
		display.addControlListener(new ZoomControl());
		display.addControlListener(new DragControl());
		display.addControlListener(new PanControl());
		
		TupleTableModel ttm = new TupleTableModel(graph.getEdgeTable());
		node_table = new JTable(ttm);
		node_table.getTableHeader().addMouseListener(ttm);
		//ttm.addTableModelListener(node_table);
		//node_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		node_table.addMouseListener(new JTableMouseListener());
	}

	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			{
				jSplitPane1 = new JSplitPane();
				getContentPane().add(jSplitPane1, BorderLayout.CENTER);
				jSplitPane1.add(display, JSplitPane.LEFT);
				JScrollPane jScrollPane1 = new JScrollPane();
				jScrollPane1.setViewportView(node_table);
				jSplitPane1.add(jScrollPane1, JSplitPane.RIGHT);
			}
			pack();
			//setSize(400, 300);
			
			vis.run(LAYOUT);
			vis.run(COLOR);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
