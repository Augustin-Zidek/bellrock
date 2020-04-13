package eu.zidek.augustin.bellrock.celltowerapi;

import java.io.IOException;

/**
 * Class for storing information about GSM cell towers.
 * 
 * @author Augustin Zidek
 *
 */
public class CellTower {
    // Mobile Country Code
    private final short mcc;
    // Mobile Network Code
    private final short mnc;
    // Location Area Code
    private final int lac;
    // The unique ID of the cell within the LAC
    private final int cellID;
    // The coarse location of the cell tower
    private CoarseLocation location;

    /**
     * Creates the cell information object by unpacking the long containing all
     * necessary information. The format is following (most significant bit to
     * the most significant bit):<br>
     * 0--9: MCC<br>
     * 10--19: MNC<br>
     * 20--35: LAC<br>
     * 35--63: CID
     * 
     * @param cellTowerInfoPacked The information about the cell in a packed
     *            form.
     */
    public CellTower(final long cellTowerInfoPacked) {
        // Take last 28 bits
        this.cellID = (int) (cellTowerInfoPacked & 0x0000_0000_0FFF_FFFFL);
        // Next 16 bits
        this.lac = (int) ((cellTowerInfoPacked
                & 0x0000_0FFF_F000_0000L) >>> 28);
        // Next 10 bits
        this.mnc = (short) ((cellTowerInfoPacked
                & 0x003F_F000_0000_0000L) >>> 44);
        // Next 10 bits
        this.mcc = (short) ((cellTowerInfoPacked
                & 0xFFC0_0000_0000_0000L) >>> 54);
    }

    /**
     * Creates the cell information object by unpacking the long containing all
     * necessary information. The format is following (most significant bit to
     * the most significant bit):<br>
     * 0--9: MCC<br>
     * 10--19: MNC<br>
     * 20--35: LAC<br>
     * 35--63: CID
     * 
     * @param cellTowerInfoPacked The information about the cell in a packed
     *            form.
     * @param location The location of the cell tower (if known).
     */
    public CellTower(final long cellTowerInfoPacked,
            final CoarseLocation location) {
        // Take last 28 bits
        this.cellID = (int) (cellTowerInfoPacked & 0x0000_0000_0FFF_FFFFL);
        // Next 16 bits
        this.lac = (int) ((cellTowerInfoPacked
                & 0x0000_0FFF_F000_0000L) >>> 28);
        // Next 10 bits
        this.mnc = (short) ((cellTowerInfoPacked
                & 0x003F_F000_0000_0000L) >>> 44);
        // Next 10 bits
        this.mcc = (short) ((cellTowerInfoPacked
                & 0xFFC0_0000_0000_0000L) >>> 54);
    }

    /**
     * Creates a new object that stores information about a cell tower.
     * 
     * @param mcc The Mobile Country Code, 10 bits.
     * @param mnc The Mobile Network Code, 10 bits.
     * @param lac The Location Area Code, 16 bits. Integer is used here to avoid
     *            having to use an unsigned short.
     * @param cellID The Cell ID within the LAC, 16 or 28 bits.
     */
    public CellTower(final short mcc, final short mnc, final int lac,
            final int cellID) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.lac = lac;
        this.cellID = cellID;
    }

    /**
     * @return The Mobile Country Code.
     */
    public short getMcc() {
        return this.mcc;
    }

    /**
     * @return The Mobile Network Code.
     */
    public short getMnc() {
        return this.mnc;
    }

    /**
     * @return The Location Area Code.
     */
    public int getLac() {
        return this.lac;
    }

    /**
     * @return The Cell ID.
     */
    public int getCellID() {
        return this.cellID;
    }

    /**
     * @return The location of the cell tower according the OpenCellID database.
     *         If the location is unknown the method automatically tries to
     *         resolve it. If a <code>null</code> is returned it means the
     *         position of the cell tower can't be resolved by the OpenCellID
     *         database.
     */
    public CoarseLocation getLocation() {
        if (this.location == null) {
            this.resolveLocation();
        }
        return this.location;
    }

    /**
     * Tries to resolve the location of the cell tower using the OpenCellID
     * database.
     * 
     * @return <code>true</code> on successfull resolution, <code>false</code>
     *         otherwise.
     */
    public boolean resolveLocation() {
        try {
            CellTowerLocator locator = CellTowerLocator.getInstance();
            this.location = locator.getLocation(this.mcc, this.mnc, this.lac,
                    this.cellID);
            return true;
        }
        catch (final ClassNotFoundException | IOException e) {
            return false;
        }
    }

    /**
     * Packs all the information into a single long with the following format
     * (most significant bit to the most significant bit):<br>
     * 0--9: MCC<br>
     * 10--19: MNC<br>
     * 20--35: LAC<br>
     * 35--63: CID
     * 
     * @return The packed version of the information.
     */
    public long pack() {
        return CellTower.pack(this.mcc, this.mnc, this.lac, this.cellID);
    }

    /**
     * Packs the given information into a single long variable with the
     * following format (most significant bit to the most significant bit):<br>
     * 0--9: MCC<br>
     * 10--19: MNC<br>
     * 20--35: LAC<br>
     * 35--63: CID
     * 
     * @param mcc The Mobile Country Code, 10 bits.
     * @param mnc The Mobile Network Code, 10 bits.
     * @param lac The Location Area Code, 16 bits. Integer is used here to avoid
     *            having to use an unsigned short.
     * @param cellID The Cell ID within the LAC, 16 or 28 bits.
     * @return The packed version of the information.
     */
    public static long pack(final short mcc, final short mnc, final int lac,
            final int cellID) {
        long packed = 0;
        packed += mcc;
        packed <<= 10;
        packed += mnc;
        packed <<= 16;
        packed += lac;
        packed <<= 28;
        packed += cellID;
        return packed;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MCC: ");
        sb.append(this.mcc);
        sb.append(System.lineSeparator());
        sb.append("MNC: ");
        sb.append(this.mnc);
        sb.append(System.lineSeparator());
        sb.append("LAC: ");
        sb.append(this.lac);
        sb.append(System.lineSeparator());
        sb.append("CID: ");
        sb.append(this.cellID);
        return sb.toString();
    }

}
