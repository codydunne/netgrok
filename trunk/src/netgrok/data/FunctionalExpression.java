package netgrok.data;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.column.IntColumn;
import prefuse.data.event.ExpressionListener;
import prefuse.data.expression.Expression;
import prefuse.data.expression.ExpressionVisitor;

public class FunctionalExpression implements Expression{

	String column;
	int max_node_value = 1;
	int max_edge_value = 1;
	
	Set<ExpressionListener> expression_listeners = new HashSet<ExpressionListener>();
	
	public FunctionalExpression(String column)
	{
		this.column = column;
	}

	private void notifyListeners()
	{
		Iterator<ExpressionListener> i = expression_listeners.iterator();
		while(i.hasNext())
		{
			ExpressionListener el = i.next();
			el.expressionChanged(this);
		}
	}
	
	public void setMaxEdgeValue(int max_value)
	{
		this.max_edge_value = max_value;
		notifyListeners();
		// TODO: notify that the value changed
	}

	public void setMaxNodeValue(int max_value)
	{
		this.max_node_value = max_value;
		notifyListeners();
		// TODO: notify that the value changed
	}
	
	public void addExpressionListener(ExpressionListener arg0) {
		expression_listeners.add(arg0);
	}

	public Object get(Tuple arg0) {
		if(arg0 instanceof Node)
			return new Double((double)arg0.getInt(column)/(double)max_node_value);
		else
			return new Double((double)arg0.getInt(column)/(double)max_edge_value);
	}

	public boolean getBoolean(Tuple arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public double getDouble(Tuple arg0) {
		// TODO Auto-generated method stub
		if(arg0 instanceof Node)
			return (double)arg0.getInt(column)/(double)max_node_value;
		else
			return (double)arg0.getInt(column)/(double)max_edge_value;
	}

	public float getFloat(Tuple arg0) {
		// TODO Auto-generated method stub
		if(arg0 instanceof Node)
			return (float)arg0.getInt(column)/(float)max_node_value;
		else
			return (float)arg0.getInt(column)/(float)max_edge_value;
	}

	public int getInt(Tuple arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getLong(Tuple arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Class getType(Schema arg0) {
		// TODO Auto-generated method stub
		return double.class;
	}

	public void removeExpressionListener(ExpressionListener arg0) {
		// TODO Auto-generated method stub
		expression_listeners.remove(arg0);
		
	}

	public void visit(ExpressionVisitor arg0) {
		// TODO Auto-generated method stub
		
	}
}
