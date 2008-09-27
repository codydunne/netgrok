package netgrok.data;

import java.io.*;
import java.util.Scanner;

import prefuse.data.Node;
import prefuse.data.Tree;

/**
 * Reads in group data from a text file and returns a prefuse.data.Tree object.
 * 
 * @author Cody Dunne
 *
 */
public class GroupFileReader {

	private Tree t;
	private Node root,local,foreign;

	public GroupFileReader(){
		t = new Tree();
		t.addColumn("group", String.class);
		t.addColumn("subnets", String.class);
		t.addColumn("x", float.class);
		t.addColumn("y", float.class);

		root = t.addRoot();
		root.setString("group", "Network");
		root.setString("subnets", null);

		local = t.addChild(root);
		local.setString("group", "Local Network");

		foreign = t.addChild(root);
		foreign.setString("group", "Foreign Network");
	}

	/**
	 * Read in the groups from the specified text file
	 * 
	 * @param filename the file to read the groups from
	 * @return a prefuse.data.Tree object representing the groups
	 * @throws FileNotFoundException
	 */
	public Tree readGroups(String filename) throws FileNotFoundException{

		Scanner s = null;
		try {
			s = new Scanner(new BufferedReader(new FileReader(filename)));

			boolean local = true;

			int count = 0;
			while (s.hasNextLine()) {
				String line = s.nextLine();
				line = line.trim();

				if(line.equals("")){
					continue;
				}

				if(line.equals("[local]")){
					local = true;
					continue;
				}

				if(line.equals("[foreign]")){
					local = false;
					continue;
				}

				String[] tokens = line.split("=");
				if(tokens.length == 2){
					if(local){
						addLoc(tokens[0], tokens[1]);
					} else {
						addFor(tokens[0], tokens[1]);
					}
				} else if (tokens.length == 4){
					try{
						if(local){
							addLoc(tokens[0], tokens[1], Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]));
						} else {
							addFor(tokens[0], tokens[1], Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]));
						}
					} catch (NumberFormatException e){
						throw new IllegalArgumentException("Unable to parse line " + count + ": invalid float: " + line);
					}
				} else {
					throw new IllegalArgumentException("Unable to parse line " + count + ": invalid number of tokens: " + line);
				}
				count++;
			}
		} finally {
			if (s != null) {
				s.close();
			}
		}

		return t;
	}

	private Node addLoc(String name, String subnets){
		Node n = t.addChild(local);
		n.setString("group", name);
		n.setString("subnets", subnets);
		return n;
	}

	private Node addLoc(String name, String subnets, float x, float y){
		Node n = addLoc(name, subnets);
		n.setFloat("x", x);
		n.setFloat("y", y);
		return n;		
	}

	private Node addFor(String name, String subnets){
		Node n = t.addChild(foreign);
		n.setString("group", name);
		n.setString("subnets", subnets);
		return n;
	}

	private Node addFor(String name, String subnets, float x, float y){
		Node n = addFor(name, subnets);
		n.setFloat("x", x);
		n.setFloat("y", y);
		return n;	
	}
	
	public static void main(String[] args){
		XMLTreeGen xtg = new XMLTreeGen();
		try{
		xtg.t = new GroupFileReader().readGroups("data/groups.ini");
		} catch (java.io.FileNotFoundException e){
		}
		xtg.write("outtext.xml");
	}
}
