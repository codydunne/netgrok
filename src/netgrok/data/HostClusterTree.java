package netgrok.data;

// TODO: watch the graph for changes
//		add accessors 

import java.util.Iterator;

import prefuse.data.Graph;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.tuple.TableNode;
import prefuse.data.tuple.TupleSet;

public class HostClusterTree {
	private Tree cluster_hierarchy;
	private Table cluster_table;
	private Tuple root_cluster;
	private Node root_node;
	
	public HostClusterTree(Graph g)
	{
		// Add all nodes to the tree under a single cluster
		initClusterTable();
		root_cluster = addCluster("root_cluster");
		initHierarchyTree(root_cluster, g);
	}
	
	private void initHierarchyTree(Tuple root, Graph g)
	{
		cluster_hierarchy = new Tree();
		cluster_hierarchy.addColumn("tuple_ref", Tuple.class);
		root_node = cluster_hierarchy.addRoot();
		root_node.set("tuple_ref",root_cluster);
		TupleSet graph_nodes = g.getNodes();
		Iterator graph_tuples_iter = graph_nodes.tuples();
		while(graph_tuples_iter.hasNext())
		{
			Tuple t = (Tuple)graph_tuples_iter.next();
			Node child = cluster_hierarchy.addChild(root_node);
			child.set("tuple_ref", t);
		}
	}
	
	private void initClusterTable()
	{
		cluster_table = new Table();
		cluster_table.addColumn("name", String.class);
	}
	
	private Tuple addCluster(String cluster_name)
	{
		int new_node = cluster_table.addRow();
		cluster_table.set(new_node, "name", cluster_name);
		return cluster_table.getTuple(new_node);
	}
	
	public Tree getTree()
	{
		return cluster_hierarchy;
	}
}
