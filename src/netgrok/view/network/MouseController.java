package netgrok.view.network;

import netgrok.data.*;

import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.*;

import javax.swing.SwingUtilities;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.data.*;
import prefuse.visual.*;

public class MouseController extends ControlAdapter {

	protected boolean dragged;
    private VisualItem activeItem;    
    protected Point2D down = new Point2D.Double();
    protected Point2D temp = new Point2D.Double();
    
    public void itemEntered(VisualItem item, MouseEvent e) {
    	activeItem = item;
    	
        if (item instanceof AggregateItem) {
        	//Data.getData().selectGroup((AggregateItem) item);
        }    
        
		if (item instanceof NodeItem) {			
			setFixed(item, true);
			Data.getData().selectNode((Node) item);
			item.setBoolean(NetworkView.IN_HOVER_GROUP, true);
			
			Iterator<?> it = ((NodeItem) item).edges();
			while (it.hasNext()) 
				((Edge) it.next()).setBoolean(NetworkView.IN_HOVER_GROUP, true);
		}
		
		if (item instanceof EdgeItem) {
			if (!item.getBoolean(NetworkView.HIDE_EDGE)) {
				setFixed(item, true);
				Data.getData().selectEdge((Edge) item);
			}
		}
    }
 
    public void itemExited(VisualItem item, MouseEvent e) {
        if (activeItem == item) {
            activeItem = null;
            setFixed(item, false);
            Data.getData().clearSelection();
        }
        
		if (item instanceof NodeItem) {
			item.setBoolean(NetworkView.IN_HOVER_GROUP, false);
			
			Iterator<?> it = ((NodeItem) item).edges();
			while (it.hasNext()) 
				((Edge) it.next()).setBoolean(NetworkView.IN_HOVER_GROUP, false);
		}
    }

    public void itemPressed(VisualItem item, MouseEvent e) {
        
    	if (!SwingUtilities.isLeftMouseButton(e)) 
        	return;
        
    	dragged = false;
        Display d = (Display)e.getComponent();
        d.getAbsoluteCoordinate(e.getPoint(), down);
        
        if ( item instanceof AggregateItem )
            setFixed(item, true);
    }

    public void itemReleased(VisualItem item, MouseEvent e) {
        
    	if (!SwingUtilities.isLeftMouseButton(e)) 
    		return;
        
        if ( dragged ) {
            activeItem = null;
            setFixed(item, false);
            dragged = false;
        }            
    }
    
    public void itemDragged(VisualItem item, MouseEvent e) {
        
    	if (!SwingUtilities.isLeftMouseButton(e)) 
    		return;
    	
        dragged = true;
        Display d = (Display)e.getComponent();
        d.getAbsoluteCoordinate(e.getPoint(), temp);
        double dx = temp.getX()-down.getX();
        double dy = temp.getY()-down.getY();
        
        move(item, dx, dy);
        
        down.setLocation(temp);
    }

    protected void setFixed(VisualItem item, boolean fixed) {
    	
    	if ( item instanceof AggregateItem ) {
            Iterator<?> items = ((AggregateItem)item).items();
            while ( items.hasNext() )
                setFixed((VisualItem)items.next(), fixed);
        } 
    	
    	else
            item.setFixed(fixed || item.getBoolean(NetworkView.FIXED));
    }
    
    protected static void move(VisualItem item, double dx, double dy) {
        if ( item instanceof AggregateItem ) {
            Iterator<?> items = ((AggregateItem)item).items();
            while ( items.hasNext() ) {
                move((VisualItem)items.next(), dx, dy);
            }
        } 
        
        else {
            double x = item.getX();
            double y = item.getY();
            item.setStartX(x);  item.setStartY(y);
            item.setX(x+dx);    item.setY(y+dy);
            item.setEndX(x+dx); item.setEndY(y+dy);
        }
    }
    
}