package test;

import prefuse.data.Tree;
import prefuse.data.Node;
import prefuse.data.io.*;

/**
 * Generate an example xml groups file and put it in data/groups.xml
 * 
 * @author Cody Dunne
 */
public class XMLTreeGen {

	public static void main(String[] args) {
		Tree t = new Tree();
		t.addColumn("group", String.class);
		t.addColumn("subnets", String.class);
		t.addColumn("x", int.class);
		t.addColumn("y", int.class);
		
		Node root = t.addRoot();
		root.setString("group", "Network");
		root.setString("subnets", null);
		
		Node local = t.addChild(root);
		local.setString("group", "Local Network");
		
		Node foreign = t.addChild(root);
		foreign.setString("group", "Foreign Network");
		
		Node umd = t.addChild(local);
		umd.setString("group", "UMDCP");
		umd.setString("subnets", "128.8.0.0/16");
		
		Node local0 = t.addChild(local);
		local0.setString("group", "local0");
		local0.setString("subnets", "192.168.0.0/24");
		
		Node local1 = t.addChild(local);
		local1.setString("group", "local1");
		local1.setString("subnets", "192.168.1.0/24");
		
		Node kyle = t.addChild(local);
		kyle.setString("group", "Kyle");
		kyle.setString("subnets", "129.2.135.103/32");
		
		Node cody = t.addChild(local);
		cody.setString("group", "Cody");
		cody.setString("subnets", "69.140.12.62/32");
		
		Node slashdot = t.addChild(foreign);
		slashdot.setString("group", "Slashdot");
		slashdot.setString("subnets", "66.35.250.55/24");
		slashdot.setInt("x", 100);
		slashdot.setInt("y", 100);
		
		Node google = t.addChild(foreign);
		google.setString("group", "Google");
		google.setString("subnets", 
				"216.239.0.0/16," +
				"64.233.0.0/16," +
				"64.68.0.0/16," +
				"66.249.0.0/16," +
				"209.85.0.0/16," +
				"209.185.0.0/16," +
				"66.102.0.0/16," +
				"74.125.0.0/16," +
				"72.14.0.0/16");
		google.setInt("x", 50);
		google.setInt("y", 100);
		
		TreeMLWriter tml = new TreeMLWriter(); 
		try{
		tml.writeGraph(t, "data/groups.xml");
		} catch (DataIOException e){
			e.printStackTrace();
		}
	}

}
