package com.ekeneijeoma.geofencing;

import android.location.Location;

import processing.core.PApplet;

/**
 * Created by ekeneijeoma on 2/4/16.
 */
class GeofenceLocation {
    String name ="";
    float distance = 0;

    Location location = new Location("test");

    boolean isInside = false;

    boolean checkInside(Location l, float radius) {
        distance = PApplet.round(location.distanceTo(l));
        isInside = distance < radius;
        return isInside;
    }
}
