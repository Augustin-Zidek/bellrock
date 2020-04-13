package eu.zidek.augustin.bellrock.server;

import java.io.IOException;
import java.time.Instant;

import eu.zidek.augustin.bellrock.celltowerapi.CellTower;
import eu.zidek.augustin.bellrock.celltowerapi.CellTowerLocator;
import eu.zidek.augustin.bellrock.celltowerapi.CoarseLocation;

/**
 * Stores coarse location and the time in which the user was at this coarse
 * location.
 * 
 * @author Augustin Zidek
 *
 */
public class UserLocation {
    private final Instant start;
    private final Instant end;
    private CoarseLocation location;
    private final CellTower cellTowerInfo;

    /**
     * @param start The time when the user started being at the coarse location.
     * @param end The time when the user stopped being at the coarse location.
     * @param location The approximate location where the user was during the
     *            given time interval.
     * @param cellTowerInfo Information about the cell tower. This information
     *            is used to determine the approximate location if the location
     *            was known at the time of creation of this object. Set to
     *            <code>null</code> if the location is known.
     */
    public UserLocation(final Instant start, final Instant end,
            final CoarseLocation location, final CellTower cellTowerInfo) {
        this.start = start;
        this.end = end;
        this.location = location;
        this.cellTowerInfo = cellTowerInfo;
    }

    /**
     * @param start The time when the user started being at the coarse location.
     * @param end The time when the user stopped being at the coarse location.
     * @param cellTowerInfo Information about the cell tower. This information
     *            is used to determine the approximate location if the location
     *            was known at the time of creation of this object.
     */
    public UserLocation(final Instant start, final Instant end,
            final CellTower cellTowerInfo) {
        this.start = start;
        this.end = end;
        this.location = null;
        this.cellTowerInfo = cellTowerInfo;
    }

    /**
     * @return The time when the user started being at the coarse location.
     */
    public Instant getStart() {
        return this.start;
    }

    /**
     * @return The time when the user stopped being at the coarse location.
     */
    public Instant getEnd() {
        return this.end;
    }

    /**
     * @return The approximate (coarse) location.
     */
    public CoarseLocation getLocation() {
        return this.location;
    }

    /**
     * @return Information about the cell tower.
     */
    public CellTower getCellTowerInfo() {
        return this.cellTowerInfo;
    }

    /**
     * Use the cell tower information to resolve the approximate location.
     */
    public void resolveCoarseLocation() {
        if (this.location != null) {
            return;
        }
        try {
            final CellTowerLocator locator = CellTowerLocator.getInstance();
            this.location = locator.getLocation(this.cellTowerInfo);
        }
        // Fail silently. Don't care if this location won't get resolved.
        catch (final ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     * @param time The time to compare to.
     * @return <code>true</code> if the time interval spent at this location
     *         started and ended before the given time, <code>false</code>
     *         otherwise.
     */
    public boolean isBefore(final Instant time) {
        return this.start.isBefore(time) && this.end.isBefore(time);
    }

    /**
     * 
     * @param start The start of the interval.
     * @param end The end of the interval.
     * @return <code>true</code> if the interval of this user location overlaps
     *         with the given time interval, <code>false</code> otherwise.
     */
    public boolean overlapsWith(final Instant start, final Instant end) {
        // If this interval starts and ends before the given interval or starts
        // and ends after the given interval, then it doesn't overlap with the
        // given interval.
        return !((this.start.isBefore(start) && this.end.isBefore(start))
                || (this.start.isAfter(end) && this.end.isAfter(end)));
    }

}
