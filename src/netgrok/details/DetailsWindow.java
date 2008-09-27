package netgrok.details;

import javax.swing.JTextArea;

import netgrok.data.Data;
import netgrok.data.SelectListener;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Tuple;

public class DetailsWindow extends JTextArea implements SelectListener {

	private String no_selection_string = "Hover over a node to see\nits details here.";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DetailsWindow()
	{
		Data d = Data.getData();
		d.addNodeSelectListener(this);
		this.setEditable(false);
		this.setText(no_selection_string);
	}

	public void nodeSelected(Node n) {
		StringBuffer b = new StringBuffer();
		b.append("IP: ");
		b.append(n.get(Data.HOST_ADDRESS));
		b.append("\nHostname: ");
		b.append(n.get(Data.HOSTNAME));
		b.append("\nBandwidth: ");
		b.append(n.get(Data.BANDWIDTH));
		b.append("\nOut Degree: ");
		b.append(n.getOutDegree());
		b.append("\nIn Degree: ");
		b.append(n.getInDegree());
		String group_string = n.get(Data.GROUPS).toString();
		String [] groups = group_string.split(",");
		for(int i = 0; i < groups.length; i++)
		{
			b.append("\nGroup "+(i+1)+": ");
			b.append(groups[i]);
		}
		
		this.setText(b.toString());
		this.setCaretPosition(0);
	}

	public void selectionCleared() {
		this.setText(no_selection_string);
	}

	public void edgeSelected(Edge e) {
		StringBuffer b = new StringBuffer();
		b.append("Source IP: ");
		b.append(e.getSourceNode().get(Data.HOST_ADDRESS));
		b.append("\nTarget IP: ");
		b.append(e.getTargetNode().get(Data.HOST_ADDRESS));
		b.append("\nBandwidth: ");
		b.append(e.get(Data.BANDWIDTH));
		
		this.setText(b.toString());
		this.setCaretPosition(0);	
	}

	public void groupSelected(Node n) {
		// TODO: gather info about the group
		System.out.println("Group selected: "+n.toString());
	}

}
