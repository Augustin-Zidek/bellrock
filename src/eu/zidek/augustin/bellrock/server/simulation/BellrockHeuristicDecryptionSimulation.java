package eu.zidek.augustin.bellrock.server.simulation;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import eu.zidek.augustin.bellrock.celltowerapi.CellTower;
import eu.zidek.augustin.bellrock.celltowerapi.CellTowerLocator;
import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.IDAnonymizer;
import eu.zidek.augustin.bellrock.server.BellrockServer;
import eu.zidek.augustin.bellrock.server.BellrockUser;
import eu.zidek.augustin.bellrock.server.Location;
import eu.zidek.augustin.bellrock.server.Observation;
import eu.zidek.augustin.bellrock.server.Observations;
import eu.zidek.augustin.bellrock.server.UserLocation;

/**
 * Simulator that simulates a lot of users distributed across multiple
 * transmitters. The users hear each other's AIDs and the server is thence
 * tested if it uses the efficient heuristic AID decryption.
 * 
 * @author Augustin Zidek
 *
 */
public class BellrockHeuristicDecryptionSimulation {
    private static final int USER_NO = 20_000;
    private static final int OBSERVATION_NO = 1000;
    private static final int CT_LIMIT = 20;
    // Monaco : 212, 1 CT
    // San Marino : 292, 2 CTs
    // Andorra : 213, 191 CTs
    // Lichtenstein: 295, 764 CTs
    // Malta : 278, 2808 CTs
    // Luxembourg : 270, 16,620 CTs
    // Czech Republ: 230, 70,000 CTs
    private static final short COUNTRY = 295;
    private static Instant SIM_START = Instant.now().minus(Duration.ofHours(2));
    private static Instant SIM_END = Instant.now().plus(Duration.ofHours(2));

    private final Random rand = new Random();
    private final IDAnonymizer anonymizer = new IDAnonymizer();
    private final Map<CellTower, List<BellrockUser>> users = new HashMap<>();
    private final BellrockServer server;
    private final CellTowerLocator cellLocator;
    private final List<CellTower> cells;

    private long taskStartTime = System.currentTimeMillis();

    /**
     * Starts the server and initialises the simulation.
     * 
     * @throws SQLException If there was an error with the Bellrock server
     *             database.
     * @throws IOException If there was an error with the cell tower locator.
     * @throws ClassNotFoundException If there was an error with the cell tower
     *             locator.
     */
    public BellrockHeuristicDecryptionSimulation()
            throws SQLException, ClassNotFoundException, IOException {
        // Start the server and clear its database
        this.server = new BellrockServer();
        this.server.clearDatabase();

        this.cellLocator = CellTowerLocator.getInstance();
        // Get all cells for the given country
        this.cells = this.cellLocator.filterCellTowers(COUNTRY);
    }

    private void generateCellTowers() {
        // Limit the CT number
        final Iterator<CellTower> cellsIterator = this.cells.iterator();

        int cnt = 0;
        while (cellsIterator.hasNext()) {
            if (cnt >= CT_LIMIT) {
                cellsIterator.remove();
            }
            cellsIterator.next();
            cnt++;
        }

        // Fill the map with cells and empty lists for users.
        for (final CellTower cell : this.cells) {
            // There will be about USER_COUNT / CELLS_COUNT users per cell
            this.users.put(cell, new ArrayList<>(USER_NO / this.cells.size()));
        }
    }

    private void generateUsers() {
        // Get the new users in batch, much (MUCH) faster
        final List<BellrockUser> users = this.server.newUsers(USER_NO);
        // Assign each user to a random cell and set their location accordingly
        for (final BellrockUser user : users) {
            // Assign them to a random cell
            final int randCellNo = this.rand.nextInt(this.cells.size());
            final CellTower randCell = this.cells.get(randCellNo);
            this.users.get(randCell).add(user);

            // Set the user's location according to the cell's
            final UserLocation location = new UserLocation(SIM_START, SIM_END,
                    randCell);
            user.addLocation(location);
            this.server.addLocation(user.getUID(), location);
        }
    }

    private Map<CellTower, BellrockUser> selectObservers() {
        final Map<CellTower, BellrockUser> observers = new HashMap<>(
                this.cells.size());
        // Go over all cells
        for (final Map.Entry<CellTower, List<BellrockUser>> cellWithUsers : this.users
                .entrySet()) {
            // Get cell's users
            final List<BellrockUser> cellUsers = cellWithUsers.getValue();
            // Select a random user -- the observer -- from every cell
            final BellrockUser observer = cellUsers
                    .get(this.rand.nextInt(cellUsers.size()));
            observers.put(cellWithUsers.getKey(), observer);
            // Remove the observer from cell users so they won't hear themselves
            cellUsers.remove(observer);
        }
        return observers;
    }

    private List<Observations> getRandomObservations(
            final Map<CellTower, BellrockUser> cellsWithUsers) {
        final List<Observations> allObservations = new ArrayList<>(
                this.cells.size());
        // Go over every cell tower and the observer there
        for (final Map.Entry<CellTower, BellrockUser> cTWithObserver : cellsWithUsers
                .entrySet()) {
            // Get the cell and the observer
            final CellTower cell = cTWithObserver.getKey();
            final BellrockUser user = cTWithObserver.getValue();

            final List<Observation> userObs = new ArrayList<>(OBSERVATION_NO);

            // Get the users that are on the same cell as the observer
            final List<BellrockUser> ctUsers = this.users.get(cell);

            // Generate observations
            for (int i = 0; i < OBSERVATION_NO; i++) {
                // Get a random user from the ones on the cell
                final BellrockUser observee = ctUsers
                        .get(this.rand.nextInt(ctUsers.size()));
                // Get an Anonymous ID from them
                final AnonymousID obsAID = this.anonymizer
                        .getAnonymousID(observee.getUID(), observee.getKey());
                final Location location = cell.getLocation()
                        .toPreciseLocation();
                final Observation o = new Observation(user.getUID(), obsAID,
                        Instant.now(), location);
                userObs.add(o);
            }
            allObservations.add(new Observations(user.getUID(), userObs));
        }
        return allObservations;
    }

    private long resolveObservationAIDs(final List<Observations> observations) {
        long resolvedOK = 0;
        for (final Observations ctObservations : observations) {
            resolvedOK += this.server.addObservations(ctObservations);
        }
        return resolvedOK;
    }

    private void timeTaskAndPrintDone() {
        final long taskEndTime = System.currentTimeMillis();
        final long taskTime = taskEndTime - this.taskStartTime;
        System.out.printf("[ %6d ms ]%n", taskTime);
        this.taskStartTime = System.currentTimeMillis();
    }

    /**
     * Call with at least 2 GB of memory for optimal performance. This also
     * means 64-bit JVM has to be used.
     * 
     * @param args
     */
    public static void main(String[] args) {
        final String format = "%-55s";

        System.out.println("Simulation started.");

        // Start the server
        System.out.printf(format, "Starting server and getting cell towers. ");
        final BellrockHeuristicDecryptionSimulation simulation;
        try {
            simulation = new BellrockHeuristicDecryptionSimulation();
        }
        catch (final ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
            return;
        }
        simulation.timeTaskAndPrintDone();

        // Initialise the user lists for each cell
        simulation.generateCellTowers();
        final int cellCount = simulation.cells.size();
        System.out.printf(format,
                String.format("Populating %d cell towers for the country %d. ",
                        cellCount, COUNTRY));
        simulation.timeTaskAndPrintDone();

        // Generate random users and populate the cell towers
        System.out.printf(format, String.format(
                "Generating %d users, adding them to cell towers. ", USER_NO));
        simulation.generateUsers();
        simulation.timeTaskAndPrintDone();

        // Select an observer at each cell
        System.out.printf(format, "Selecting observer for each cell tower. ");
        final Map<CellTower, BellrockUser> cellsWithObservers = simulation
                .selectObservers();
        simulation.timeTaskAndPrintDone();

        // Randomly select observees from each cell tower and let them transmit
        // their AID to the observer at their cell tower
        System.out.printf(format, String.format(
                "Generating %d observations per user. ", OBSERVATION_NO));
        final List<Observations> observations = simulation
                .getRandomObservations(cellsWithObservers);
        simulation.timeTaskAndPrintDone();

        // Resolve the AIDs of each observer. Each decryption should about T
        // decryptions, where T is the average number of people per transmitter
        System.out.printf(format,
                String.format("Resolving %d * %d = %d observations. ",
                        cellCount, OBSERVATION_NO, OBSERVATION_NO * cellCount));
        final long resolved = simulation.resolveObservationAIDs(observations);
        simulation.timeTaskAndPrintDone();
        System.out.printf("%d observations resolved correctly.%n", resolved);

        simulation.server.shutDownDatabase();

        // Done. Beep.
        java.awt.Toolkit.getDefaultToolkit().beep();

    }
}
