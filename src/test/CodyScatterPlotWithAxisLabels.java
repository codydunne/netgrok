package test;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataShapeAction;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.action.layout.AxisLayout;
import prefuse.controls.ToolTipControl;
import prefuse.data.Table;
import prefuse.data.io.DelimitedTextTableReader;
import prefuse.render.AxisRenderer;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;

/**
 * A simple scatter plot visualization that allows visual encodings to
 * be changed at runtime. 
 * Original by Jeffrey Heer, extended by Kaitlin Duck Sherwood
 * to allow axes to be shown.  
 * 
 * Kaitlin Duck Sherwood's modifications are granted as is for any
 * commercial or non-commercial use, with or without attribution.
 * The only conditions are that you can't pretend that you wrote it,
 * and that you leave this notice in the source.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 */
public class CodyScatterPlotWithAxisLabels extends Display {

	protected static final String group = "data";
	private Rectangle2D m_dataB = new Rectangle2D.Double();
	private Rectangle2D m_xlabB = new Rectangle2D.Double();
	private Rectangle2D m_ylabB = new Rectangle2D.Double();

	private ShapeRenderer m_shapeR = new ShapeRenderer(2);

	public CodyScatterPlotWithAxisLabels(Table t, String xfield, String yfield) {
		this(t, xfield, yfield, null);
	}

	public CodyScatterPlotWithAxisLabels(Table t, String xfield, String yfield, String sfield) {
		super(new Visualization());

		// --------------------------------------------------------------------
		// STEP 1: setup the visualized data

		m_vis.addTable(group, t);

		m_vis.setRendererFactory(new RendererFactory() {
			Renderer yAxisRenderer = new AxisRenderer(Constants.RIGHT, Constants.TOP);
			Renderer xAxisRenderer = new AxisRenderer(Constants.CENTER, Constants.FAR_BOTTOM);

			public Renderer getRenderer(VisualItem item) {
				if(item.isInGroup("ylabels"))
					return yAxisRenderer;
				if(item.isInGroup("xlabels"))
					return xAxisRenderer;
				return m_shapeR;

			}
		});

		// --------------------------------------------------------------------
		// STEP 2: create actions to process the visual data

		// set up the x and y axes 
		AxisLayout x_axis = new AxisLayout(group, xfield, 
				Constants.X_AXIS, VisiblePredicate.TRUE);
		x_axis.setScale(Constants.LINEAR_SCALE);
		x_axis.setDataType(Constants.NUMERICAL);
		m_vis.putAction("x", x_axis);

		AxisLayout y_axis = new AxisLayout(group, yfield, 
				Constants.Y_AXIS, VisiblePredicate.TRUE);
		y_axis.setDataType(Constants.NUMERICAL);
		m_vis.putAction("y", y_axis);

		y_axis.setLayoutBounds(m_dataB);
		x_axis.setLayoutBounds(m_dataB);

		// set up the axis labels
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setMaximumFractionDigits(0);

		AxisLabelLayout xlabels = new AxisLabelLayout("xlabels", x_axis, m_xlabB);
		xlabels.setNumberFormat(nf);
		xlabels.setScale(Constants.LINEAR_SCALE);
		m_vis.putAction("xlabels", xlabels);

		AxisLabelLayout ylabels = new AxisLabelLayout("ylabels", y_axis, m_ylabB);
		ylabels.setNumberFormat(nf);
		m_vis.putAction("ylabels", ylabels);



		ColorAction color = new ColorAction(group, 
				VisualItem.STROKECOLOR, ColorLib.rgb(100,100,255));
		m_vis.putAction("color", color);

		DataShapeAction shape = new DataShapeAction(group, sfield);
		m_vis.putAction("shape", shape);

		ActionList draw = new ActionList();
		draw.add(x_axis);
		draw.add(y_axis);
		if ( sfield != null )
			draw.add(shape);
		draw.add(xlabels);  
		draw.add(ylabels);  
		draw.add(color);
		draw.add(new RepaintAction());
		m_vis.putAction("draw", draw);

		// --------------------------------------------------------------------
		// STEP 3: set up a display and ui components to show the visualization

		setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		setSize(700,450);
		setHighQuality(true);

		ToolTipControl ttc = new ToolTipControl(new String[] {xfield,yfield});
		addControlListener(ttc);

		setLayoutBoundsForDisplay(); 


		// --------------------------------------------------------------------        
		// STEP 4: launching the visualization

		m_vis.run("draw");

	}

	// taken from CongressDemo.displayLayout
	// this puts the axes on the right
	public void setLayoutBoundsForDisplay() {
		Insets i = getInsets();
		int w = getWidth();
		int h = getHeight();
		int insetWidth = i.left+i.right;
		int insetHeight = i.top+i.bottom;
		int yAxisWidth = 85;
		int xAxisHeight = 15;

		m_dataB.setRect(i.left, i.top, w-insetWidth-yAxisWidth, h-insetHeight-xAxisHeight); 
		m_xlabB.setRect(i.left, h-xAxisHeight-i.bottom, w-insetWidth-yAxisWidth, xAxisHeight);
		m_ylabB.setRect(i.left, i.top, w-insetWidth, h-insetHeight-xAxisHeight);

		m_vis.run("update");
		m_vis.run("xlabels");
	}

	private static JToolBar getEncodingToolbar(final CodyScatterPlotWithAxisLabels sp,
			final String xfield, final String yfield, final String sfield)
	{
		int spacing = 10;

		// create list of column names
		Table t = (Table)sp.getVisualization().getSourceData(group);
		String[] colnames = new String[t.getColumnCount()];
		for ( int i=0; i<colnames.length; ++i )
			colnames[i] = t.getColumnName(i);

		// create toolbar that allows visual mappings to be changed
		JToolBar toolbar = new JToolBar();
		toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
		toolbar.add(Box.createHorizontalStrut(spacing));

		final JComboBox xcb = new JComboBox(colnames);
		xcb.setSelectedItem(xfield);
		xcb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				axisUpdateAction(sp, xcb, "x", "xlabels");
			}
		});
		toolbar.add(new JLabel("X: "));
		toolbar.add(xcb);
		toolbar.add(Box.createHorizontalStrut(2*spacing));

		final JComboBox ycb = new JComboBox(colnames);
		ycb.setSelectedItem(yfield);
		ycb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				axisUpdateAction(sp, ycb, "y", "ylabels");
			}

		});
		toolbar.add(new JLabel("Y: "));
		toolbar.add(ycb);
		toolbar.add(Box.createHorizontalStrut(2*spacing));

		final JComboBox scb = new JComboBox(colnames);
		scb.setSelectedItem(sfield);
		scb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Visualization vis = sp.getVisualization();
				DataShapeAction s = (DataShapeAction)vis.getAction("shape");
				s.setDataField((String)scb.getSelectedItem());
				vis.run("draw");
			}
		});
		toolbar.add(new JLabel("Shape: "));
		toolbar.add(scb);
		toolbar.add(Box.createHorizontalStrut(spacing));
		toolbar.add(Box.createHorizontalGlue());

		return toolbar;
	}

	private static void axisUpdateAction(final CodyScatterPlotWithAxisLabels sp, 
			final JComboBox axisComboBox, 
			final String axisFieldName, final String axisLabelFieldName) {
		Visualization vis = sp.getVisualization();
		AxisLabelLayout labels = (AxisLabelLayout)vis.getAction(axisLabelFieldName);
		AxisLayout axis = (AxisLayout)vis.getAction(axisFieldName);
		String dataField = (String)axisComboBox.getSelectedItem();
		Table t = (Table)vis.getSourceData(group);
		boolean isNumeric = t.getColumn(dataField).canGetDouble();
		axis.setDataField(dataField);

		if( isNumeric ) {
			// need to set range model to null to force recalculation
			labels.setRangeModel(null);
			axis.setDataType(Constants.NUMERICAL);
		} else {     // completely untested with derived columns
			axis.setDataType(Constants.ORDINAL);
			labels.setRangeModel(null);
		} 

		vis.run("draw");
	}



	public int getPointSize() {
		return m_shapeR.getBaseSize();
	}

	public void setPointSize(int size) {
		m_shapeR.setBaseSize(size);
		repaint();
	}



	public static CodyScatterPlotWithAxisLabels demo(String data, String xfield,
			String yfield, String sfield) 
	{
		Table table = null;
		try {
			table = new DelimitedTextTableReader().readTable(data);
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}

		CodyScatterPlotWithAxisLabels scatter = new CodyScatterPlotWithAxisLabels(table, xfield, yfield, sfield);
		scatter.setPointSize(10);
		return scatter;
	}

	public static void main(String[] argv) {
		String data = "/scatterplot.txt";
		String xfield = "X";
		String yfield = "Y";
		String sfield = null;
		if ( argv.length >= 3 ) {
			data = argv[0];
			xfield = argv[1];
			yfield = argv[2];
			sfield = ( argv.length > 3 ? argv[3] : null );
		}

		final CodyScatterPlotWithAxisLabels sp = demo(data, xfield, yfield, sfield);
		JToolBar toolbar = getEncodingToolbar(sp, xfield, yfield, sfield);


		JFrame frame = new JFrame("p r e f u s e  |  s c a t t e r");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(toolbar, BorderLayout.NORTH);
		frame.getContentPane().add(sp, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}


} // end of class ScatterPlotWithAxisLabels