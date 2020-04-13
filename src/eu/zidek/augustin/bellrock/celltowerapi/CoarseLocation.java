package eu.zidek.augustin.bellrock.celltowerapi;

import java.io.Serializable;
import java.util.Arrays;

import eu.zidek.augustin.bellrock.server.Location;

/**
 * Class to store an coarse (approximate) location on the Earth using WGS84
 * (GPS) coordinates.
 * 
 * @author Augustin Zidek
 *
 */
public class CoarseLocation implements Serializable {
    private static final long serialVersionUID = 3406446880248250516L;
    private final float latitude;
    private final float longitude;

    /**
     * Constructs a new coarse location. Coarse means that only floats will be
     * used to store the longitude and latitude, thus permitting up to about 10
     * meter accuracy. See WGS84 for specification.
     * 
     * @param latitude The latitude.
     * @param longitude The longitude.
     */
    public CoarseLocation(final float latitude, final float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return The latitude.
     */
    public float getLatitude() {
        return this.latitude;
    }

    /**
     * @return The longitude.
     */
    public float getLongitude() {
        return this.longitude;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof CoarseLocation)) {
            return false;
        }
        final CoarseLocation other = (CoarseLocation) obj;

        return this.getLatitude() == other.getLatitude()
                && this.getLongitude() == other.getLongitude();
    }

    /**
     * This object is capable of storing coarse location only (floats are used
     * to store the latitude and the longitude). If more precision is needed,
     * this method returns a Location object that is significantly more precise
     * (doubles are used to store the latitude and the longitude).
     * 
     * @return A object capable of storing locations with bigger precision.
     */
    public Location toPreciseLocation() {
        return new Location(this.latitude, this.longitude);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new float[] { this.latitude, this.longitude });
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.latitude);
        sb.append(", ");
        sb.append(this.longitude);
        sb.append(")");
        return sb.toString();
    }
}
