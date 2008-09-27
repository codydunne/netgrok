package netgrok.view.network;

import java.awt.geom.*;
import java.awt.Point;
import java.awt.event.*;

import prefuse.*;
import prefuse.controls.*;
import prefuse.util.*;
import prefuse.util.display.*;
import prefuse.visual.*;

public class NetworkZoomControl extends AbstractZoomControl {
	
	private VisualItem zoomed_item;
    private Point m_point = new Point();
    private Display display;
    
    public NetworkZoomControl(Display display) {
    	this.display = display;
    }
    
    public void itemWheelMoved(VisualItem item, MouseWheelEvent e) {
        if ( m_zoomOverItem )
            mouseWheelMoved(e);
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        Display display = (Display)e.getComponent();
        m_point.x = display.getWidth()/2;
        m_point.y = display.getHeight()/2;
        
        // for some reason, prefuse's zoom wheel is backward
        zoom(display, m_point,
             1 + 0.1f * (e.getWheelRotation()*-1.0), false);
    }
    
    public void itemClicked(VisualItem t, MouseEvent e) {
		if (e.getClickCount() == 2) {
			
			// double clicking zooms in & out of hosts
			if (zoomed_item != t) {
				Display display = (Display)e.getComponent();
		    	
			    if ( !display.isTranformInProgress() )
		        {
		            Rectangle2D bounds = t.getBounds();
		            GraphicsLib.expand(bounds, 5 + (int)(1/display.getScale()));
		            DisplayLib.fitViewToBounds(display, bounds, 2000);
		        }
		    	
		    	zoomed_item = t;
			}
			
			else {
				reset();
			}
		}
	}
    
    public void reset() {
    	if ( !display.isTranformInProgress() ) {
    		Rectangle2D bounds = display.getItemBounds();
            bounds.add(bounds.getMaxX(), bounds.getMaxY());
            bounds.add(bounds.getMinX(), bounds.getMinY());
            GraphicsLib.expand(bounds, (int)(1/display.getScale()));
            DisplayLib.fitViewToBounds(display, bounds, 2000);
        }
    	
    	zoomed_item = null;
    }
    
    public void addToZoom(float[] loc) {
    	if ( zoomed_item == null ) {
    		Rectangle2D bounds = display.getItemBounds();
    		
    		if (!bounds.contains(loc[0], loc[1])) {
    			float x = loc[0];
    			float y = (loc[1] < 250 ? -30 : 0) + loc[1];

	            bounds.add(Math.max(x,bounds.getMaxX()), Math.max(y, bounds.getMaxY()));
	            bounds.add(Math.min(x,bounds.getMinX()), Math.min(y, bounds.getMinY()));
	            GraphicsLib.expand(bounds, (int)1/display.getScale());
	            DisplayLib.fitViewToBounds(display, bounds, 2000);
    		}
        }
    }
} 
