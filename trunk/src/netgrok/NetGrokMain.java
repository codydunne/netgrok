package netgrok;

import netgrok.data.*;
import netgrok.details.DetailsWindow;
import netgrok.view.*;
import netgrok.view.network.*;
import netgrok.view.treemap.*;
import netgrok.view.table.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import prefuse.util.ui.*;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

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
public class NetGrokMain extends javax.swing.JFrame {

	private static final long serialVersionUID = -3900023108481632919L;

	{
		//Set Look & Feel - system look & feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	// Data
	//private Table raw_data;
	private Data data;

	// UI components
	private JMenuItem exitMenuItem;
	private JMenuItem saveMenuItem;
	private TimeOverviewView timeOverviewView1;
	private NetworkView networkView1;
	private AbstractAction saveConfigurationAction;
	private AbstractAction openFileAction;
	private AbstractAction openPcapFileAction;
	private JSeparator jSeparator2;
	private AbstractAction closeApplicationAction;
	private JSeparator jSeparator3;
	private JCheckBox jCheckBox1;
	private JPanel timeOverviewPane;
	private JPanel sidePane;
	private JTabbedPane visTabbedPane;
	private JCheckBox protocolSelect;
	private JTextPane protocolTextBox;
	private JTextField searchTextField;
	private JTextPane searchText;
	private JMenuItem openFileMenuItem;
	private JMenuItem openPcapFileMenuItem;
	private JMenu jMenu3;
	private JMenuBar menuBar;
	private JScrollPane dodPane;
	private TreeMapView treeMapView;
	private JPanel parallelView;
	private JPanel networkGraph;
	private JRangeSlider timeRangeSlider;
	private FilterPanel filterPanel;
	private LocalSensor ls;

	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args) {
		Logger.getLogger("prefuse").setLevel(Level.WARNING);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				NetGrokMain inst = new NetGrokMain();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}

	public NetGrokMain() {
		super();
		
		// TODO: parse command line args if we're doing that sort of thing
		ls = new LocalSensor();

		//raw_data = new Table();
		data = Data.getData();// new TupleTableModel(raw_data);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);		
		initGUI();
	}

	private void initGUI() {
		try {
			GroupLayout thisLayout = new GroupLayout((JComponent)getContentPane());
			getContentPane().setLayout(thisLayout);
			thisLayout.setVerticalGroup(thisLayout.createSequentialGroup()
					.addContainerGap()
					.add(thisLayout.createParallelGroup()
							.add(GroupLayout.LEADING, thisLayout.createSequentialGroup()
									.add(0, 0, GroupLayout.PREFERRED_SIZE)
									.add(getVisTabbedPane(), 0, 386, Short.MAX_VALUE)
									.addPreferredGap(LayoutStyle.RELATED)
									.add(getTimeOverviewPane(), GroupLayout.PREFERRED_SIZE, 118, GroupLayout.PREFERRED_SIZE))
									.add(GroupLayout.LEADING, getSidePane(), 0, 510, Short.MAX_VALUE))
									.addContainerGap());
			thisLayout.setHorizontalGroup(thisLayout.createSequentialGroup()
					.addContainerGap()
					.add(thisLayout.createParallelGroup()
							.add(GroupLayout.LEADING, thisLayout.createSequentialGroup()
									.add(0, 0, GroupLayout.PREFERRED_SIZE)
									.add(getVisTabbedPane(), 0, 704, Short.MAX_VALUE))
									.add(GroupLayout.LEADING, getTimeOverviewPane(), 0, 704, Short.MAX_VALUE))
									.addPreferredGap(LayoutStyle.RELATED)
									.add(getSidePane(), GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
									.addContainerGap());
			this.setTitle("NetGrok");
			this.setSize(1024, 768);
			{
				menuBar = new JMenuBar();
				setJMenuBar(menuBar);
				{
					jMenu3 = new JMenu();
					menuBar.add(jMenu3);
					jMenu3.setText("File");
					{
						JMenu interfaces = new JMenu("Open Interface");
						String[] devices = LocalSensor.getDeviceNames();
						
						for (String device : devices) {
							JCheckBoxMenuItem i = new JCheckBoxMenuItem(device);
							
							i.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									ls.sniff(e.getActionCommand());			
									
									JCheckBoxMenuItem check = (JCheckBoxMenuItem) e.getSource();
									Component[] items = check.getParent().getComponents();
									
									for (Component item : items) 
										item.setEnabled(false);
								}
							});
							
							interfaces.add(i);
						}
						
						// display "No Devices Found" 
						if (interfaces.getSubElements().length == 0) {
							JMenuItem none = new JMenuItem("No Devices Found");
							none.setEnabled(false);
							interfaces.add(none);
						}
						
						jMenu3.add(interfaces);
					}
					{
						openFileMenuItem = new JMenuItem();
						jMenu3.add(openFileMenuItem);
						openFileMenuItem.setText("Open File...");
						openFileMenuItem.setAction(getOpenFileAction());
					}
					{
						openPcapFileMenuItem = new JMenuItem();
						jMenu3.add(openPcapFileMenuItem);
						jMenu3.add(getJSeparator3());
						openPcapFileMenuItem.setText("Open Pcap File...");
						openPcapFileMenuItem.setAction(getOpenPcapFileAction());
					}
					{
						saveMenuItem = new JMenuItem();
						jMenu3.add(saveMenuItem);
						jMenu3.add(getJSeparator2());
						saveMenuItem.setText("Save Configuration");
						saveMenuItem.setAction(getSaveConfigurationAction());
					}
					{
						exitMenuItem = new JMenuItem();
						jMenu3.add(exitMenuItem);
						exitMenuItem.setText("Exit");
						exitMenuItem.setAction(getCloseApplicationAction());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO: select from the data using the search query
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
			searchTextField.setText("192.168.0.0/24");
		}
		return searchTextField;
	}

	private JTextPane getProtocolTextBox() {
		if(protocolTextBox == null) {
			protocolTextBox = new JTextPane();
			protocolTextBox.setText("Show Protocols:");
			protocolTextBox.setEditable(false);
			protocolTextBox.setOpaque(false);
		}
		return protocolTextBox;
	}

	// TODO: add these dynamically based on the data in memory
	private JCheckBox getProtocolSelect() {
		if(protocolSelect == null) {
			protocolSelect = new JCheckBox();
			protocolSelect.setText("SSH (25)");
		}
		return protocolSelect;
	}

	private JCheckBox getJCheckBox1() {
		if(jCheckBox1 == null) {
			jCheckBox1 = new JCheckBox();
			jCheckBox1.setText("HTTP (80)");
		}
		return jCheckBox1;
	}

	private JSeparator getJSeparator3() {
		if(jSeparator3 == null) {
			jSeparator3 = new JSeparator();
		}
		return jSeparator3;
	}

	private AbstractAction getCloseApplicationAction() {
		if(closeApplicationAction == null) {
			closeApplicationAction = new AbstractAction("Exit", null) {
				public void actionPerformed(ActionEvent evt) {
					System.exit(0);
				}
			};
		}
		return closeApplicationAction;
	}

	private JSeparator getJSeparator2() {
		if(jSeparator2 == null) {
			jSeparator2 = new JSeparator();
		}
		return jSeparator2;
	}

	private AbstractAction getOpenFileAction() {
		if(openFileAction == null) {
			final NetGrokMain ngm = this;
			openFileAction = new AbstractAction("Open File...", null) {
				public void actionPerformed(ActionEvent evt) {
					// TODO: something to populate the data object
					// private TupleTableModel data; (global, above)
					JFileChooser jfc = new JFileChooser();
					jfc.showOpenDialog(ngm);
					try
					{
						File f = jfc.getSelectedFile();
						System.out.println("Opening file "+f.getName());
						Data.getData().loadPackets(f);
					}
					catch (NullPointerException e)
					{

					}
				}
			};
			openFileAction.putValue(javax.swing.Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl pressed O"));
		}
		return openFileAction;
	}

	private AbstractAction getOpenPcapFileAction() {
		if(openPcapFileAction == null) {
			final NetGrokMain ngm = this;
			openPcapFileAction = new AbstractAction("Open Pcap File...", null) {
				public void actionPerformed(ActionEvent evt) {
					JFileChooser jfc = new JFileChooser();
					jfc.showOpenDialog(ngm);
					try
					{
						File f = jfc.getSelectedFile();
						String filename = f.getPath();
						System.out.println("Opening pcap file "+filename);
						PcapReader.readFile(filename);
					}
					catch (NullPointerException e)
					{

					}
				}
			};
			openPcapFileAction.putValue(javax.swing.Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl pressed P"));
		}
		return openPcapFileAction;
	}

	private AbstractAction getSaveConfigurationAction() {
		if(saveConfigurationAction == null) {
			final NetGrokMain ngm = this;
			saveConfigurationAction = new AbstractAction("Save Configuration", null) {
				public void actionPerformed(ActionEvent evt) {
					// TODO: save the data set's current semantic substrates to
					// disk so we can load them later.  ~/.netgrokrc?
					JFileChooser jfc = new JFileChooser();
					jfc.showSaveDialog(ngm);
					try
					{
						File f = jfc.getSelectedFile();
						System.out.println("Saving to file "+f.getName());
						Data.getData().saveAllPackets(f);
					}
					catch (NullPointerException e)
					{

					}
				}
			};
			saveConfigurationAction.putValue(javax.swing.Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl pressed S"));
		}
		return saveConfigurationAction;
	}

	private NetworkView getNetworkView1() {
		if(networkView1 == null) {
			networkView1 = new NetworkView();
		}
		return networkView1;
	}

	private TimeOverviewView getTimeOverviewView1(JRangeSlider jrs) {
		if(timeOverviewView1 == null) {
			timeOverviewView1 = new TimeOverviewView(jrs);
		}
		return timeOverviewView1;
	}

	private JTabbedPane getVisTabbedPane() {
		if(visTabbedPane == null) {
			visTabbedPane = new JTabbedPane();
			{
				parallelView = new JPanel();
				//visTabbedPane.addTab("Aggregate View", null, parallelView, null);
				GroupLayout parallelViewLayout = new GroupLayout((JComponent)parallelView);
				parallelView.setLayout(parallelViewLayout);
			}
			{
				networkGraph = new JPanel();
				visTabbedPane.addTab("Graph View", null, networkGraph, null);
				visTabbedPane.addTab("TreeMap View", null, getTreeMapView(), null);
				visTabbedPane.addTab("Edge Table", null, getEdgeTableViz(), null);
				GroupLayout networkGraphLayout = new GroupLayout((JComponent)networkGraph);
				networkGraph.setLayout(networkGraphLayout);
				networkGraphLayout.setHorizontalGroup(networkGraphLayout.createSequentialGroup()
						.add(getNetworkView1(), 0, 684, Short.MAX_VALUE));
				networkGraphLayout.setVerticalGroup(networkGraphLayout.createSequentialGroup()
						.add(getNetworkView1(), 0, 366, Short.MAX_VALUE));
			}
		}
		return visTabbedPane;
	}

	private JPanel getSidePane() {
		if(sidePane == null) {
			sidePane = new JPanel();
			GroupLayout dataFilterPaneLayout = new GroupLayout((JComponent)sidePane);
			sidePane.setLayout(dataFilterPaneLayout);
			dataFilterPaneLayout.setHorizontalGroup(dataFilterPaneLayout.createParallelGroup()
					.add(GroupLayout.LEADING, getDataFilterPane(), 0, 300, Short.MAX_VALUE)
					.add(GroupLayout.LEADING, getDodPane(), 0, 300, Short.MAX_VALUE));
			dataFilterPaneLayout.setVerticalGroup(dataFilterPaneLayout.createSequentialGroup()
					.addContainerGap()
					.add(getDataFilterPane(), GroupLayout.PREFERRED_SIZE, 281, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.UNRELATED)
					.add(getDodPane(), 0, 201, Short.MAX_VALUE));
		}
		return sidePane;
	}

	private JPanel getTimeOverviewPane() {
		if(timeOverviewPane == null) {
			timeOverviewPane = new JPanel();
			GroupLayout timeOverviewPaneLayout = new GroupLayout((JComponent)timeOverviewPane);
			timeOverviewPane.setLayout(timeOverviewPaneLayout);
			{
				timeRangeSlider = new JRangeSlider(0, 1000, 0, 1000, SwingConstants.VERTICAL);
			}
			timeOverviewPaneLayout.setHorizontalGroup(timeOverviewPaneLayout.createParallelGroup()
					.add(GroupLayout.LEADING, getTimeOverviewView1(timeRangeSlider), 0, 704, Short.MAX_VALUE)
					.add(GroupLayout.LEADING, timeRangeSlider, 0, 704, Short.MAX_VALUE));
			timeOverviewPaneLayout.setVerticalGroup(timeOverviewPaneLayout.createSequentialGroup()
					.add(getTimeOverviewView1(timeRangeSlider), 0, 102, Short.MAX_VALUE)
					.add(timeRangeSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
		}
		return timeOverviewPane;
	}

	public TreeMapView getTreeMapView() {
		if(treeMapView == null) {
			treeMapView = new TreeMapView();
		}
		return treeMapView;
	}

	public JScrollPane getEdgeTableViz()
	{
		TupleTableModel ttm = new TupleTableModel(data.getIPGraph().getEdgeTable());
		JTable node_table = new JTable(ttm);
		node_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		node_table.getTableHeader().addMouseListener(ttm);
		JScrollPane jsp = new JScrollPane();
		jsp.setViewportView(node_table);
		return jsp;
	}

	private JPanel getDataFilterPane() {
		/*if(dataFilterPane == null) {
			dataFilterPane = new JPanel();
			dataFilterPane.setBorder(BorderFactory.createTitledBorder("Filtering"));
			dataFilterPane.add(getSearchText());
			dataFilterPane.add(getSearchTextField());
			dataFilterPane.add(getProtocolTextBox());
			dataFilterPane.add(getJCheckBox1());
			dataFilterPane.add(getProtocolSelect());
		}
		return dataFilterPane;*/
		if(filterPanel == null)
		{
			filterPanel = new FilterPanel();
		}
		return filterPanel;
	}

	private JScrollPane getDodPane() {
		if(dodPane == null) {
			dodPane = new JScrollPane();
			dodPane.setBorder(BorderFactory.createTitledBorder("Details on Demand"));
			dodPane.setBackground(Color.WHITE);
			dodPane.setViewportView(new DetailsWindow());
		}
		return dodPane;
	}
}
