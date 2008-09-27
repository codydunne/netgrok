package netgrok.data;

import prefuse.data.Tree;
import prefuse.data.Node;
import prefuse.data.io.*;

/**
 * Generate an xml file readable by prefuse with groups data
 * 
 * @author Cody Dunne
 */
public class XMLTreeGen {
	public Tree t;
	private Node root,local,foreign;

	public XMLTreeGen(){
		t = new Tree();
		t.addColumn("group", String.class);
		t.addColumn("subnets", String.class);
//		t.addColumn("x", float.class);
//		t.addColumn("y", float.class);

		root = t.addRoot();
		root.setString("group", "Network");
		root.setString("subnets", null);

		local = t.addChild(root);
		local.setString("group", "Local Network");

		foreign = t.addChild(root);
		foreign.setString("group", "Foreign Network");
	}

	public void write(String filename){
		TreeMLWriter tml = new TreeMLWriter(); 
		try{
			tml.writeGraph(t, filename);
		} catch (DataIOException e){
			e.printStackTrace();
		}
	}
	
	public Node addLoc(String name, String subnets){
		Node n = t.addChild(local);
		n.setString("group", name);
		n.setString("subnets", subnets);
		return n;
	}

	public Node addLoc(String name, String subnets, float x, float y){
		Node n = addLoc(name, subnets);
		n.setFloat("x", x);
		n.setFloat("y", y);
		return n;		
	}

	public Node addFor(String name, String subnets){
		Node n = t.addChild(foreign);
		n.setString("group", name);
		n.setString("subnets", subnets);
		return n;
	}

	public Node addFor(String name, String subnets, float x, float y){
		Node n = addFor(name, subnets);
		n.setFloat("x", x);
		n.setFloat("y", y);
		return n;	
	}
	
	public static void main(String[] args) {
		XMLTreeGen xtg = new XMLTreeGen();
		xtg.addAll();
	}
	
	private void addAll(){
		addLoc("UMDCP", "128.8.0.0/16");
		addLoc("local0", "192.168.0.0/24");
		addLoc("local1", "192.168.0.1/24");
		addLoc("Kyle", "129.2.135.103/32");
		addLoc("Cody", "69.140.12.62/32");
		addLoc("Ryan", "98.204.236.51/32");
		
		addFor("Slashdot", "66.35.250.55/24");
		addFor("Google", 
				"216.239.0.0/16," +
				"64.233.0.0/16," +
				"64.68.0.0/16," +
				"66.249.0.0/16," +
				"209.85.0.0/16," +
				"209.185.0.0/16," +
				"66.102.0.0/16," +
				"74.125.0.0/16," +
				"72.14.0.0/16"
				);
		
		write("data/groups.xml");
	}

}
