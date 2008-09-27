package netgrok.data;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Tuple;

public interface SelectListener {
	public void nodeSelected(Node n);
	public void edgeSelected(Edge e);
	public void groupSelected(Node n);
	public void selectionCleared();
}
