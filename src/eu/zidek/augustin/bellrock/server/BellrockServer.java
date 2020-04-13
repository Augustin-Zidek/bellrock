package eu.zidek.augustin.bellrock.server;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import eu.zidek.augustin.bellrock.celltowerapi.CoarseLocation;
import eu.zidek.augustin.bellrock.identification.UID;

/**
 * The Bellrock server that takes care of user management and inferring
 * information from the observations.
 * 
 * @author Augustin Zidek
 *
 */
public class BellrockServer {
    final UserManager um;
    final BellrockDatabase db;

    /**
     * Initialises a new Bellrock server. This involves the following
     * operations:<br>
     * 1) Database. If the server is run for the first time, the tables are
     * initialised. If the tables already exist, the server initialises its
     * users, etc. according to the data in the database.<br>
     * 2) User manager. User manager is a layer on top of the database. It
     * creates users (and makes sure they get unique UID) and provides other
     * convenience methods for dealing with users. To make it faster, it
     * pre-loads some things from the database. Therefore the fresh start could
     * take a while, but then the operations it provides should be much faster
     * than using directly the database.<br>
     * The Bellrock server is quite memory intensive (for performance reasons),
     * hence launch at least with 1 GB of memory (using <code>-Xmx1024m</code>).
     * <br>
     * The Bellrock server uses AES extensively and it is one of the main
     * features that have a huge impact on the server's performance. Make sure
     * the server is running on a modern Java 8 VM which supports the native
     * AES-NI instructions.
     * 
     * @throws SQLException In case there was an error initialising the SQL
     *             database. This could be cause by number of reasons: The
     *             HSQLDB jar might not be present, the permissions might not be
     *             set correctly or other reasons.
     */
    public BellrockServer() throws SQLException {
        this.um = UserManager.getInstance();
        this.db = BellrockDatabase.getInstance();
    }

    /**
     * @return A new Bellrock user. A random unique UID is assigned to them and
     *         a random secret key that will be used for turning UID into AID
     *         will be generated.
     */
    public synchronized BellrockUser newUser() {
        return this.um.newUser();
    }

    /**
     * Creates a <code>count</code> number of new Bellrock users, each having a
     * unique UID and a random secret key. They are also added to the internal
     * database. If lot of users need to be created at the same time, this
     * method is much faster than using <code>newUser</code> in a loop.
     * 
     * @param count The number of new users to be generated.
     * @return A list of new Bellrock user. A random unique UID is assigned to
     *         each and a random secret key that will be used for turning UID
     *         into AID will be generated.
     */
    public synchronized List<BellrockUser> newUsers(final int count) {
        return this.um.newUsers(count);
    }

    /**
     * Renews a key of the given user.
     * 
     * @param user The Bellrock whose secret key should be renewed.
     * @return The new random secret key.
     */
    public synchronized SecretKey renewUserKey(final BellrockUser user) {
        return this.um.renewUserKey(user.userUID);
    }

    /**
     * Deletes the given user from the system.
     * 
     * @param user The user to be deleted
     * @return <code>true</code> if the user was successfully deleted,
     *         <code>false</code> otherwise.
     */
    public synchronized boolean deleteUser(final BellrockUser user) {
        return this.um.deleteUser(user.getUID());
    }

    /**
     * Adds all the observations into the database. Moreover, Anonymous IDs in
     * the observations are resolved, if possible.
     * 
     * @param observations The observations made by a user. The observations
     *            must be sorted by the time.
     * @return The number of observations that were successfully resolved.
     */
    public int addObservations(final Observations observations) {
        // Get the observer
        final BellrockUser observer = this.um
                .getUser(observations.getObserver());

        // Get the list of observer's locations
        final Observation firstObs = observations.getFirstObservation();
        final Observation lastObs = observations.getLastObservation();
        final List<UserLocation> observerLocations = observer
                .getLocations(firstObs.getTime(), lastObs.getTime());

        // The observer went to several locations. At each location, they could
        // have met some users. Get the lists of them and then use these lists
        // later when resolving the individual observations.
        final Map<CoarseLocation, List<BellrockUser>> usersMetAtLocations = new HashMap<>();
        for (final UserLocation observerLoc : observerLocations) {
            final List<UID> potentialUsersUIDs = this.db.getUsersAtPlaceAtTime(
                    observerLoc.getLocation(), observerLoc.getStart(),
                    observerLoc.getEnd());
            final List<BellrockUser> usersMetAtLocation = this.um
                    .getUsers(potentialUsersUIDs);
            usersMetAtLocations.put(observerLoc.getLocation(),
                    usersMetAtLocation);
        }

        int resolvedObsCount = 0;
        for (final Observation observation : observations.getObservations()) {
            // Decrypt using user's most recently seen UIDs and peers
            final boolean resolvedUsingRecentAndPeers = observer
                    .resolveObservationUsingRecentAndPeers(observation);
            if (resolvedUsingRecentAndPeers) {
                resolvedObsCount++;
                continue;
            }

            // Decrypt using UIDs who were at the same location at the same time
            final List<BellrockUser> potentialUsers = usersMetAtLocations
                    .get(observation.getLocation().toCoarseLocation());

            final BellrockUser resolvedUser = observation
                    .resolveAID(potentialUsers);
            if (resolvedUser != null) {
                observer.addLastSeenUser(resolvedUser);
                resolvedObsCount++;
            }
        }

        // TODO: Update the database with user's recent acquaintances

        // Store all (some possible resolved) observations in the db
        this.db.batchAddObservations(observations);
        return resolvedObsCount;
    }

    /**
     * @param user The user whose inferred information should be returned.
     * @return The information the user inferred from the data, e.g. position,
     *         peer proximity, etc.
     */
    public InferredInformation getInferredInformation(final BellrockUser user) {
        // TODO: Return inferred information, such as position, peer proximity,
        // etc. Update the JavaDoc for this method. Make sure this method can't
        // be called by people who are not the user.
        return null;
    }

    /**
     * Adds a new location into the list of locations the user visited. The
     * location is also resolved, i.e. if only information about the cell tower
     * is provided, this method automatically tries to resolve it into a coarse
     * location.
     * 
     * @param user The user.
     * @param location The location.
     */
    public void addLocation(final UID user, final UserLocation location) {
        this.um.addLocation(user, location);
    }

    /**
     * Adds a new list of locations into the list of locations the user visited.
     * The location are also resolved, i.e. if only information about the cell
     * tower is provided, this method automatically tries to resolve it into a
     * coarse location.
     * 
     * @param user The user.
     * @param locations The list of locations.
     */
    public void addLocations(final UID user,
            final List<UserLocation> locations) {
        this.um.addLocations(user, locations);
    }

    /**
     * Registers a peer relationship between two users. The relationship is
     * symmetric, hence if (A,B) pair is registered, the (B,A) pair is
     * registered automatically.
     * 
     * @param user The user.
     * @param peer The user's peer.
     */
    public void addPeer(final BellrockUser user, final BellrockUser peer) {
        this.um.addPeer(user.userUID, peer.getUID());
    }

    /**
     * Deletes a peer relationship between two users. The relationship is
     * symmetric, hence if (A,B) pair is registered, the (B,A) pair is
     * registered automatically.
     * 
     * @param user The user.
     * @param peer The user's peer.
     */
    public void deletePeer(final UID user, final UID peer) {
        this.um.deletePeer(user, peer);
    }

    /**
     * @return The number of users in the system.
     */
    public int getUserCount() {
        return this.um.userCount();
    }

    /**
     * Remove data from all tables in the server database.
     */
    public void clearDatabase() {
        this.db.clearDB();
        this.um.clearCachedUsers();
    }

    /**
     * Shuts down the database. Use when turning off the server.
     */
    public void shutDownDatabase() {
        this.db.shutDown();
    }

}
