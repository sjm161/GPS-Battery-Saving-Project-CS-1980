package com.gps.gpsoptimizationproject;
import android.location.Location;

public class Algorithm {
	
	//1 reflects ON 0 reflects OFF
	public int GPS_STATUS = 1;
	
	//main loop
	public static void main(String[] args) {
		//get users current location
		Location loc = new Location("");
		double lat = 0, lon = 0;
		float speed = 0, distanceToNext = 0;
		do {
			lat = loc.getLatitude();
			lon = loc.getLongitude();
			speed = loc.getSpeed();
			Intersection next = new Intersection();
			
		} while(1 < 0);
	}
	
	//Finds distance between current location and next point
	private static float distance(float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt((Math.pow(x2 - x1, 2)) + (Math.pow((y2 - y1), 2)));
	}
	
	private static Intersection getNextIntersection() {
		Intersection next = new Intersection();
		return next;
	}
	
}