package netgrok.view.network;

import java.util.*;
import java.awt.geom.*;

import prefuse.action.layout.*;
import prefuse.visual.*;

/**
 * Set label positions. Labels are assumed to be DecoratorItem instances,
 * decorating their respective nodes. The layout simply gets the bounds
 * of the decorated node and assigns the label coordinates to the center
 * of those bounds.
 */

class LabelLayout extends Layout {
	
    public LabelLayout(String group) {
        super(group);
    }

    public void run(double frac) {
        Iterator<?> iter = m_vis.items(m_group);
        
        while ( iter.hasNext() ) {
            DecoratorItem decorator = (DecoratorItem)iter.next();
            VisualItem decoratedItem = decorator.getDecoratedItem();
            Rectangle2D bounds = decoratedItem.getBounds();
            
            double x = bounds.getCenterX()+1.5;
            double y = bounds.getCenterY();
            	
            setX(decorator, null, x);
            setY(decorator, null, y);
        }
    }
} 