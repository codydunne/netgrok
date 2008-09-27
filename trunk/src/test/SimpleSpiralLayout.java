package test;

import netgrok.data.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import prefuse.action.layout.Layout;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;


public class SimpleSpiralLayout extends Layout {
	
	private Map<Object, Integer> object_indices;
	private int last_index = 0;
	
	public SimpleSpiralLayout(String m_group)
	{
		super(m_group);
		object_indices = new HashMap<Object, Integer>();
	}
	
	private int getIndex(Object o)
	{
		if(object_indices.containsKey(o))
		{
			Integer i = object_indices.get(o);
			return i.intValue();
		}
		else
		{
			object_indices.put(o, new Integer(++last_index));
			return last_index;
		}
	}
	
	@Override
	public void run(double frac) {
		// TODO Auto-generated method stub
        Rectangle2D b = getLayoutBounds();
        double bx = b.getMinX(), by = b.getMinY();
        double w = b.getWidth(), h = b.getHeight();
        
        TupleSet ts = m_vis.getGroup(m_group);
        
        Iterator<?> iter = ts.tuples();
        // layout grid contents
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            item.setVisible(true);
            int i = getIndex(item);
            double radius = Math.sqrt((double)i) * 25.0;
            double angle = Math.sqrt((double)i) * Math.PI;
            double x = bx + w/2 + (radius * Math.sin(angle));
            double y = by + h/2 + (radius * Math.cos(angle));
            setX(item,null,x);
            setY(item,null,y);
        }

	}

}
