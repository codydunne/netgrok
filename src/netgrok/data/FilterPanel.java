package netgrok.data;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.Table;
import prefuse.data.event.ExpressionListener;
import prefuse.data.expression.ExpressionVisitor;
import prefuse.data.expression.Predicate;
import prefuse.util.ui.JRangeSlider;
import prefuse.data.event.TableListener;


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
public class FilterPanel extends JPanel implements VetoableChangeListener, Predicate, ChangeListener, KeyListener, TableListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Data data = null;
	private JTextPane searchText = null;
	private JTextField searchTextField = null;
	private JTextPane bandwidthText = null;
	private JTextPane lowBandwidth = null;
	private JTextPane highBandwidth = null;
	private JPanel bandwidthLabels = null;
	private JButton clearSearchButton = null;
	private JRangeSlider bandwidthRangeSlider = null;
	private JTextPane degreeText = null;
	private JTextPane lowDegree = null;
	private JTextPane highDegree = null;
	private JPanel degreeLabels = null;
	private JRangeSlider degreeRangeSlider = null;

	public FilterPanel()
	{
		data = Data.getData();
		data.addNodeTableListener(this);
		
		setBorder(BorderFactory.createTitledBorder("Filtering"));
		JPanel searchPanel = new JPanel();
		this.add(searchPanel);
		searchPanel.add(getSearchText());
		searchPanel.add(getSearchTextField());
		searchPanel.add(getClearSearchButton());
		this.add(getBandwidthText());
		this.add(getBandwidthLabels());
		this.add(getBandwidthSlider());
		this.add(getDegreeText());
		this.add(getDegreeLabels());
		this.add(getDegreeSlider());
		this.add(new JPanel());
		
		data.setFilterPredicate(this);
	}

	private JTextPane getSearchText() {
		if(searchText == null) {
			searchText = new JTextPane();
			searchText.setText("Search Hosts:");
			searchText.setEditable(false);
			searchText.setOpaque(false);
		}
		return searchText;
	}
	
	private JTextField getSearchTextField() {
		if(searchTextField == null) {
			searchTextField = new JTextField();
			searchTextField.setText("0.0.0.0/0");
			searchTextField.setColumns(10);
		}
		searchTextField.addKeyListener(this);
		return searchTextField;
	}
	
	private JButton getClearSearchButton()
	{
		if(clearSearchButton == null)
		{
			clearSearchButton = new JButton();
			//clearSearchButton.setFont(Font.getFont("CONSOLE"));
			clearSearchButton.setAction(new AbstractAction("Clear"){
				public void actionPerformed(ActionEvent arg0) {
					searchTextField.setText("0.0.0.0/0");
					updateAddressFilter(searchTextField.getText());
				}
			});
		}
		return clearSearchButton;
	}

	private JTextPane getBandwidthText() {
		if(bandwidthText == null) {
			bandwidthText = new JTextPane();
			bandwidthText.setText("Normalized Bandwidth:");
			bandwidthText.setEditable(false);
			bandwidthText.setOpaque(false);
		}
		return bandwidthText;
	}

	private JTextPane getLowBandwidthText() {
		if(lowBandwidth == null) {
			lowBandwidth = new JTextPane();
			lowBandwidth.setText("1");
			lowBandwidth.setEditable(false);
			lowBandwidth.setOpaque(false);
			lowBandwidth.setAlignmentX(0.0f);
		}
		return lowBandwidth;
	}

	private JTextPane getHighBandwidthText() {
		if(highBandwidth == null) {
			highBandwidth = new JTextPane();
			highBandwidth.setText(Integer.toString(data.getMaxNodeBandwidth()));
			highBandwidth.setEditable(false);
			highBandwidth.setOpaque(false);
			highBandwidth.setAlignmentX(1.0f);
		}
		return highBandwidth;
	}

	private JPanel getBandwidthLabels() {
		if (bandwidthLabels == null) {
			bandwidthLabels = new JPanel();
			bandwidthLabels.setPreferredSize(new java.awt.Dimension(1000, 20));
			bandwidthLabels.add(getLowBandwidthText());
			JPanel filler = new JPanel();
			filler.setPreferredSize(new java.awt.Dimension(200, 20));
			bandwidthLabels.add(filler);
			bandwidthLabels.add(getHighBandwidthText());
		}
		return bandwidthLabels;
	}

	private JTextPane getDegreeText() {
		if(degreeText == null) {
			degreeText = new JTextPane();
			degreeText.setText("Normalized Degree:");
			degreeText.setEditable(false);
			degreeText.setOpaque(false);
		}
		return degreeText;
	}

	private JTextPane getLowDegreeText() {
		if(lowDegree == null) {
			lowDegree = new JTextPane();
			lowDegree.setText("1");
			lowDegree.setEditable(false);
			lowDegree.setOpaque(false);
			lowDegree.setAlignmentX(0.0f);
		}
		return lowDegree;
	}

	private JTextPane getHighDegreeText() {
		if(highDegree == null) {
			highDegree = new JTextPane();
			highDegree.setText(Integer.toString(data.getMaxNodeDegree()));
			highDegree.setEditable(false);
			highDegree.setOpaque(false);
			highDegree.setAlignmentX(1.0f);
		}
		return highDegree;
	}

	private JPanel getDegreeLabels() {
		if (degreeLabels == null) {
			degreeLabels = new JPanel();
			degreeLabels.setPreferredSize(new java.awt.Dimension(1000, 20));
			degreeLabels.add(getLowDegreeText());
			JPanel filler = new JPanel();
			filler.setPreferredSize(new java.awt.Dimension(200, 20));
			degreeLabels.add(filler);
			degreeLabels.add(getHighDegreeText());
		}
		return degreeLabels;
	}
	
	private JRangeSlider getBandwidthSlider()
	{
		if(bandwidthRangeSlider == null)
		{
			bandwidthRangeSlider = new JRangeSlider(0, 300, 0, 300, SwingConstants.VERTICAL);
		}
		bandwidthRangeSlider.addChangeListener(this);
		return bandwidthRangeSlider;
	}
	
	private JRangeSlider getDegreeSlider()
	{
		if(degreeRangeSlider == null)
		{
			degreeRangeSlider = new JRangeSlider(0,300,0,300,SwingConstants.VERTICAL);
		}
		degreeRangeSlider.addChangeListener(this);
		return degreeRangeSlider;
	}

	public void tableChanged(Table t, int start, int end, int col, int type) {
		getHighBandwidthText().setText(Integer.toString(data.getMaxNodeBandwidth()));
		getHighDegreeText().setText(Integer.toString(data.getMaxNodeDegree()));
	}
	
	boolean min_degree_enabled = false;
	private float min_degree_rank;
	boolean max_degree_enabled = false;
	private float max_degree_rank;
	boolean min_bandwidth_enabled = false;
	private float min_bandwidth_rank;
	boolean max_bandwidth_enabled = false;
	private float max_bandwidth_rank;
	
	boolean ip_filter_enabled = false;
	InetAddress filter_address = null;
	InetAddress filter_mask = null;
	
	private boolean updateAddressFilter(String filter)
	{
		String[] pieces = filter.split("/");
		if(pieces.length != 2)
		{
			searchTextField.setBackground(Color.RED);
			return false;
		}
		InetAddress new_filter_address;
		InetAddress new_filter_mask;
		boolean new_filter_enabled = !(pieces[1].equals("0"));
		try {
			new_filter_address = InetAddress.getByName(pieces[0]);
			
			// compute the subnet mask address
			byte[] byte_mask = new byte[4];
			int slash = Integer.parseInt(pieces[1]);
			
			// compute the mask address 
			// there is probably a way easier way to do this
			for (int j=0; slash-- > 0; j++) {
				if (j<8)  byte_mask[0] += Math.pow(2,j);
				if (j<16) byte_mask[1] += Math.pow(2,j-8);
				if (j<24) byte_mask[2] += Math.pow(2,j-16);
				if (j<32) byte_mask[3] += Math.pow(2,j-24);
			}
			
			new_filter_mask = InetAddress.getByAddress(byte_mask);
		}
		catch (Exception e) {
			searchTextField.setBackground(Color.RED);
			return false;
		}
		
		ip_filter_enabled = new_filter_enabled;
		filter_address = new_filter_address;
		filter_mask = new_filter_mask;
		fireExpressionChangedEvent();
		
		//TODO: make data listen to this so that we don't need this call
		data.setFilterPredicate(this);
		System.out.println("Updating filter succeeded!");
		searchTextField.setBackground(Color.WHITE);
		return true;
	}
	
	private void updateRanges()
	{
		// get the minimum degree rank
		min_degree_rank = (float)(degreeRangeSlider.getLowValue() - degreeRangeSlider.getMinimum()) / 
				(float)(degreeRangeSlider.getMaximum() - degreeRangeSlider.getMinimum());
		max_degree_rank = (float)(degreeRangeSlider.getHighValue() - degreeRangeSlider.getMinimum()) / 
				(float)(degreeRangeSlider.getMaximum() - degreeRangeSlider.getMinimum());
		min_bandwidth_rank = (float)(bandwidthRangeSlider.getLowValue() - bandwidthRangeSlider.getMinimum()) / 
				(float)(bandwidthRangeSlider.getMaximum() - bandwidthRangeSlider.getMinimum());
		max_bandwidth_rank = (float)(bandwidthRangeSlider.getHighValue() - bandwidthRangeSlider.getMinimum()) / 
				(float)(bandwidthRangeSlider.getMaximum() - bandwidthRangeSlider.getMinimum());
		min_degree_enabled = degreeRangeSlider.getLowValue() != degreeRangeSlider.getMinimum();
		max_degree_enabled = degreeRangeSlider.getHighValue() != degreeRangeSlider.getMaximum();
		min_bandwidth_enabled = bandwidthRangeSlider.getLowValue() != bandwidthRangeSlider.getMinimum();
		max_bandwidth_enabled = bandwidthRangeSlider.getHighValue() != bandwidthRangeSlider.getMaximum();
				
		fireExpressionChangedEvent();
		
		//TODO: make data listen to this so that we don't need this call
		data.setFilterPredicate(this);
		
	}
	
	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void keyTyped(KeyEvent arg0) {
		if(arg0.getKeyChar() == '\n')
		updateAddressFilter(searchTextField.getText());
	}

	public void vetoableChange(PropertyChangeEvent e)
			throws PropertyVetoException {
		System.out.println("Saw vetoable change!");
		if(e.getSource() == searchTextField)
		{
			// TODO: make sure that the data entered can be parsed as an IP range
			// if not, then throw a PropertyVetoException
			if(!updateAddressFilter(searchTextField.getText()))
			{
				throw new PropertyVetoException("Address Filter Unrecognized",null);
			}
		}
	}

	public void stateChanged(ChangeEvent e) {
		//System.out.println("Saw change!");
		updateRanges();
		if(e.getSource() == degreeRangeSlider)
		{
			// nothing to do here
		}
		else if(e.getSource() == bandwidthRangeSlider)
		{
			// nothing to do here
		}
	}
	
	private Set<ExpressionListener> expression_listeners = new HashSet<ExpressionListener>();

	private void fireExpressionChangedEvent()
	{
		Iterator<ExpressionListener> i = expression_listeners.iterator();
		while(i.hasNext())
		{
			ExpressionListener el = i.next();
			el.expressionChanged(this);
		}
	}
	
	public void addExpressionListener(ExpressionListener lstnr) {
		expression_listeners.add(lstnr);
	}

	public void removeExpressionListener(ExpressionListener lstnr) {
		expression_listeners.remove(lstnr);
	}

	public Object get(Tuple t) {
		// TODO Auto-generated method stub
		return new Boolean(getBoolean(t));
	}

	public boolean getBoolean(Tuple t) {
		// only operate on nodes for now
		if(!(t instanceof Node))
		{
			//System.out.println("Not a node!");
			return true;
		}
		//System.out.println("Got a node!");
		// check the bandwidth rank
		if(min_bandwidth_enabled && min_bandwidth_rank > t.getFloat(Data.BANDWIDTH_RANK))
		{
			//System.out.println("Checking min bandwidth");
			return false;
		}
		if(max_bandwidth_enabled && max_bandwidth_rank < t.getFloat(Data.BANDWIDTH_RANK))
		{
			//System.out.println("Checking max bandwidth");
			return false;
		}
		// check the degree rank
		if(min_degree_enabled && min_degree_rank > t.getFloat(Data.DEGREE_RANK))
		{
			//System.out.println("Checking min degree");
			return false;
		}
		if(max_degree_enabled && max_degree_rank < t.getFloat(Data.DEGREE_RANK))
		{
			//System.out.println("Checking max degree");
			return false;
		}
		
		// check the inet address
		if(ip_filter_enabled)
		{
			InetAddress a = (InetAddress)t.get(Data.ADDRESS);
			byte[] b_node = a.getAddress();
			
			byte[] b_addr = filter_address.getAddress();
			byte[] b_mask = filter_mask.getAddress();
				
			for (int j=0; j<4; j++) 
				if ((b_addr[j] & b_mask[j]) != (b_node[j] & b_mask[j])) 
					return false;
		}	
		
		return true;
	}

	public double getDouble(Tuple t) {
		// should never be called
		return 0;
	}

	public float getFloat(Tuple t) {
		// should never be called
		return 0;
	}

	public int getInt(Tuple t) {
		// should never be called
		return 0;
	}

	public long getLong(Tuple t) {
		// should never be called
		return 0;
	}

	public Class getType(Schema s) {
		return boolean.class;
	}

	public void visit(ExpressionVisitor v) {
		// TODO:  not sure what we're supposed to do here...
	}

}
