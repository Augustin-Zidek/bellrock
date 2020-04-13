package eu.zidek.augustin.bellrock.server;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import eu.zidek.augustin.bellrock.celltowerapi.CoarseLocation;

/**
 * Class to store a location on the Earth using WGS84 (GPS) coordinates.
 * 
 * @author Augustin Zidek
 *
 */
public class Location implements Serializable {
    private static final long serialVersionUID = -2881896256582498975L;
    private final double latitude;
    private final double longitude;
    private final String name;

    /**
     * Constructs a new location. See WGS84 for specification. The string name
     * will be set to <code>null</code> using this constructor.
     * 
     * @param latitude The latitude.
     * @param longitude The longitude.
     */
    public Location(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = null;
    }

    /**
     * Constructs a new location. See WGS84 for specification.
     * 
     * @param latitude The latitude.
     * @param longitude The longitude.
     * @param name The name of the location, e.g. address or informal name, such
     *            as "William Gates Building, LT1".
     */
    public Location(final double latitude, final double longitude,
            final String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    /**
     * Constructs a new location. See WGS84 for specification. The longitude and
     * latitude will be both set to 0 using this constructor, as only the name
     * of the location is known but the coordinates are not.
     * 
     * @param name The name of the location, e.g. address or informal name, such
     *            as "William Gates Building, LT1".
     */
    public Location(final String name) {
        this.latitude = 0;
        this.longitude = 0;
        this.name = name;
    }

    /**
     * Constructs a new Location from the compact representation. Note that the
     * compact representation might have lower precision than the precision this
     * class is able to handle.
     * 
     * @param compactRep The compact representation. See
     *            <code>toCompact()</code> for details of the compact
     *            representation.
     * @return A location corresponding to the compact representation.
     */
    public static Location fromCompact(final byte[] compactRep) {
        final double latitude = ByteBuffer.wrap(compactRep).getDouble();
        final double longitude = ByteBuffer.wrap(compactRep).getDouble();
        return new Location(latitude, longitude);
    }

    /**
     * 
     * @return The name of the location or <code>null</code> if the name is
     *         unknown.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return The latitude.
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * @return The longitude.
     */
    public double getLongitude() {
        return this.longitude;
    }

    /**
     * @return A byte array representing the location.
     */
    public byte[] toCompact() {
        // TODO: This takes 16 bytes, it has to fit in < 8.
        final long latitudeRaw = Double.doubleToLongBits(this.latitude);
        final long longitudeRaw = Double.doubleToLongBits(this.longitude);
        final byte[] binRep = new byte[16];

        for (int i = 0; i < 8; i++) {
            binRep[i] = (byte) ((longitudeRaw >> ((7 - i) * 8)) & 0xff);
            binRep[i + 8] = (byte) ((latitudeRaw >> ((7 - i) * 8)) & 0xff);
        }
        return binRep;
    }

    /**
     * @return An approximate location, i.e. this location downsampled to
     *         floats.
     */
    public CoarseLocation toCoarseLocation() {
        return new CoarseLocation((float) this.latitude,
                (float) this.longitude);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Location)) {
            return false;
        }
        final Location other = (Location) obj;

        return this.getLatitude() == other.getLatitude()
                && this.getLongitude() == other.getLongitude()
                && this.getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new double[] { this.latitude, this.longitude });
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.latitude);
        sb.append(", ");
        sb.append(this.longitude);
        if (this.name != null) {
            sb.append(", ");
            sb.append(this.name);
        }
        sb.append(")");
        return sb.toString();
    }
}
