package test;

import netgrok.data.*;

import java.awt.geom.Point2D;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import test.IPHash.SimpleMD5;


public class PolarDonutGen{
	
	//TUNING PARAMETERS / MAGIC NUMBERS!!!
	private final double OFFSET = .12;
	private final double INIT_CIRCLE_DIA = .6;
	
	private double circleDia;

	private double width;
	private double height;

	private double circleOffset;
	private double xScalar;
	private double yScalar;
			
	public PolarDonutGen(double width, double height, double circleDia) {
		super();
		this.circleDia = circleDia;
		this.width = width;
		this.height = height;
		updateVars();
	}
	
	private void updateVars(){
		this.circleOffset = circleDia / Math.min(width, height) - INIT_CIRCLE_DIA;
		this.xScalar = width / (1 + 2 * (OFFSET + circleOffset));
		this.yScalar = height / (1 + 2 * (OFFSET + circleOffset));
	}

	// copyOfRange is a Java 6 function, so we duplicate it here 
	// for our poor java 1.5 users :(
	public byte[] copyOfRange(byte[] source, int start, int range) {
		byte[] result = new byte[range-start+1];
		System.arraycopy(source,start,result,0,range-start+1);
		return result;
	}
	
	public Point2D.Double getPointFromIP(String ip){
		try{
			//Get the 128bit hash of the ip
			byte[] res = SimpleMD5.MD5(IPHash.ipAddrStringToBytes(ip));

			//Get x and y pieces from the two 64bit halves of the hash
			double x = IPHash.eightBytesToInt(copyOfRange(res, 0, 7));
			double y = IPHash.eightBytesToInt(copyOfRange(res, 8, 15));

			//Scale it down to [-.5, .5]
			x /= ((double)Integer.MAX_VALUE - (double)Integer.MIN_VALUE);
			y /= ((double)Integer.MAX_VALUE - (double)Integer.MIN_VALUE);

			//Convert to polar
			double radius = Math.hypot(x, y);
			double theta = Math.atan2(y, x);

			//Do the polar alterations
			double nradius = Math.exp(radius - 1) + circleOffset;//+radius
			//double nradius = Math.sqrt(radius);
			double ntheta = theta;

			//Convert to rectangular
			double nx = nradius * Math.cos(theta); 
			double ny = nradius * Math.sin(theta);

			//Calculate offsets for screen and interior circle
			nx = xScalar * (nx /*+ .5 + OFFSET*/ + circleOffset);
			ny = yScalar * (ny /*+ .5 + OFFSET */+ circleOffset);

			//System.out.println(radius + "\t" + theta + "\t" + nradius + "\t" + ntheta + "\t" + x + "\t" + y + "\t" + nx + "\t" + ny);

			return new Point2D.Double(nx, ny);

		} catch (NoSuchAlgorithmException e){
			throw new IllegalStateException("MD5 IP Hash Problem: " + e.getMessage());
		} catch (UnsupportedEncodingException e){
			throw new IllegalStateException("MD5 IP Hash Problem: " + e.getMessage());
		}
	}

	public double getCircleDia() {
		return circleDia;
	}

	public void setCircleDia(double circleDia) {
		this.circleDia = circleDia;
		updateVars();
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
		updateVars();
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
		updateVars();
	}
}