package eu.zidek.augustin.bellrock.celltowerapi;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.zidek.augustin.bellrock.server.ServerConsts;

/**
 * Class that stores the positions of cell towers around the world. It also
 * stores their approximate locations and given the cell tower ID it can tell
 * the approximate location.
 * 
 * @author Augustin Zidek
 *
 */
public class CellTowerLocator {
    private static CellTowerLocator instance = null;
    private Map<Long, CoarseLocation> cellTowerLocations = new HashMap<>();

    private CellTowerLocator() throws IOException, ClassNotFoundException {
        this.loadCellTowerLocations();
    }

    /**
     * The cell tower locator is backed by a rather big hash map (8.6 million
     * records) for fast access. It therefore consumes couple of hundreds of MBs
     * of memory and it would be wasteful to have multiple instances of this
     * class.
     * 
     * @return A new singleton instance of the cell tower locator.
     * @throws IOException If the serialized hash map storing the cell tower
     *             data can't be accessed.
     * @throws ClassNotFoundException If the serialized object is not of the
     *             right type, i.e. when the serialized is readable but
     *             corrupted.
     */
    public static CellTowerLocator getInstance()
            throws IOException, ClassNotFoundException {
        if (instance == null) {
            instance = new CellTowerLocator();
        }
        return instance;
    }

    /**
     * Returns the coarse (approximate) location for the given cell tower.
     * 
     * @param ct The cell tower.
     * @return The approximate location of the cell tower or <code>null</code>
     *         if the given cell tower is not in the database.
     */
    public CoarseLocation getLocation(final CellTower ct) {
        return this.cellTowerLocations.get(ct.pack());
    }

    /**
     * Returns the coarse (approximate) location for the given cell tower.
     * 
     * @param mcc The Mobile Country Code, 10 bits.
     * @param mnc The Mobile Network Code, 10 bits.
     * @param lac The Location Area Code, 16 bits.
     * @param cellID The Cell ID within the LAC, 16 or 28 bits.
     * @return The approximate location of the cell tower or <code>null</code>
     *         if the given cell tower is not in the database.
     */
    public CoarseLocation getLocation(final short mcc, final short mnc,
            final int lac, final int cellID) {
        return this.cellTowerLocations
                .get(CellTower.pack(mcc, mnc, lac, cellID));
    }

    /**
     * Filters out cells that have the given country code.
     * 
     * @param countryCode The MCC - Mobile Country Code.
     * @return A list of cell towers in the given country.
     */
    public List<CellTower> filterCellTowers(final short countryCode) {
        final List<CellTower> cellsInCountry = new ArrayList<>();

        // Go over all cell towers
        for (final Map.Entry<Long, CoarseLocation> cell : this.cellTowerLocations
                .entrySet()) {
            final CellTower cellTower = new CellTower(cell.getKey(),
                    cell.getValue());
            // Add to the list to be returned on MCC match
            if (cellTower.getMcc() == countryCode) {
                cellsInCountry.add(cellTower);
            }
        }
        return cellsInCountry;
    }

    @SuppressWarnings("unchecked")
    private void loadCellTowerLocations()
            throws IOException, ClassNotFoundException {
        try (final ObjectInputStream objInStream = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(
                        ServerConsts.CELL_TOWER_LOCATIONS_FILE)));) {
            this.cellTowerLocations = (Map<Long, CoarseLocation>) objInStream
                    .readObject();
        }

    }
}
