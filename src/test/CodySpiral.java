package test;

import java.applet.*;
import java.awt.*;
import java.util.*;


//Spiral.java
//By Linda Melin & Lars Erik Holmquist
//PLAY Studio, Interactive Institute, January 2001


public class CodySpiral extends Applet {

	// constants
	public Color PLAYBLUE = new Color (13158);

	// public variables
	public Calendar cal = Calendar.getInstance ();
	public Date start;
	public Date end;
	public Date now;
	public double elapsed;
	public double left;
	public double random;
	java.util.Random generator;

	public void init() {

	}

	// Paint Method
	public void paint(Graphics g) {

		// draw a white background & a bounding rectangle


		g.setColor (PLAYBLUE);
		
		//Ni = sqrt[(Ni-1 )^2 + 1], where i is greater than or equal to 1, and N0 = 1.
		
		double Ni = 0;
		
		Ni = Math.sqrt(Math.pow(Ni, 2) + 1);
		
/*
		double rnd, x1, x2, y1, y2;
		double t= 0;
		double div = 10; // max divergence in pixels

		// start in the middle and draw outwards

		x1 = 250; y1 = 250;

		for (double r = 1; r < 25000 ; r += 1) {

			//t = t + 0.1;

			x2 = 1 * Math.cos (t) + 250;
			y2 = 1 * Math.sin (t) + 250;


			double arr = 1 * t;

			t = t + 0.1;

			double x = arr * Math.cos(t);
			double y = arr * Math.sin(t);

			g.drawLine 
			((int) x1, (int) y1, (int) x, (int) y);



			g.drawRect((int)x, (int)y, 1, 1);

			x1 = x; y1 = y;
			}*/
	/*	int N = 100;
		double s = 3.6/Math.sqrt(N);
		double lon = 0;
		double dz = 2.0/N;
		double z = 1 - dz/2;
		for(int k = 0; k < N; k++){
			double r = Math.sqrt(1-z*z);

			double x = Math.cos(lon)*r;
			double y = Math.sin(lon)*r;
			lon = lon + s/r;
			z = z - dz;
			g.drawRect((int)x, (int)y, 1, 1);
		}*/

		
		


	}
	public String getAppletInfo() {
		return "SpiralClock Lars Erik Holmquist & Linda Melin PLAY / II 2001";
	}


}
