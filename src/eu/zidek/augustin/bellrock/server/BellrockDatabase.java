package eu.zidek.augustin.bellrock.server;

import java.security.Key;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hsqldb.jdbc.JDBCPool;

import eu.zidek.augustin.bellrock.celltowerapi.CellTower;
import eu.zidek.augustin.bellrock.celltowerapi.CoarseLocation;
import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.UID;

/**
 * The Bellrock database wrapper.
 * 
 * @author Augustin Zidek
 * 
 */
public class BellrockDatabase {
    private final JDBCPool connectionPool;
    private Connection connection;
    private static BellrockDatabase instance = null;
    private final BellrockDBKeyStore keyStore;
    // The number of uncommitted statements in the buffer
    private int uncommittedStmtCount = 0;

    /**
     * 
     * Initialises Bellrock database wrapper.
     * 
     * @param databasePath The path of the database.
     * @throws SQLException Thrown if the database is corrupted.
     */
    private BellrockDatabase(final String databasePath) throws SQLException {
        this.connectionPool = new JDBCPool();
        this.connectionPool.setUrl("jdbc:hsqldb:file:" + databasePath);
        this.connectionPool.setUser("SA");
        this.connectionPool.setPassword("");

        // Initialise the database engine
        this.initialiseDB(databasePath);

        // The key store will be used instead of the DB to store keys securely
        this.keyStore = BellrockDBKeyStore.getInstance();

        // Create all tables. If they exist, do nothing.
        this.createUIDsTable();
        this.createPeersTable();
        this.createObservationsTable();
        this.createUserLocationsTable();

        this.startAutoCommitDaemon();
    }

    /**
     * Initialises the Bellrock database wrapper.
     * 
     * @return An instance of a Bellrock database (singleton).
     * @throws SQLException Thrown if the database is corrupted or there were
     *             problems opening it.
     */
    public static BellrockDatabase getInstance() throws SQLException {
        if (instance == null) {
            instance = new BellrockDatabase(ServerConsts.DB_PATH);
        }
        return instance;
    }

    private void initialiseDB(final String databasePath) throws SQLException {
        this.connection = this.connectionPool.getConnection();
        // Always update data on disk
        try (final Statement delayStmt = this.connection.createStatement();) {
            delayStmt.execute("SET WRITE_DELAY FALSE");
        }
        this.connection.setAutoCommit(false);
    }

    /**
     * Creates a table using the given SQL command.
     * 
     * @throws SQLException
     */
    private void createDB(final String sqlCommand) throws SQLException {
        try (final Statement sqlStmt = this.connection.createStatement();) {
            sqlStmt.execute(sqlCommand);
        }
    }

    private void createUIDsTable() throws SQLException {
        this.createDB(ServerConsts.SQL_CREATE_UIDS_TABLE);
    }

    private void createPeersTable() throws SQLException {
        this.createDB(ServerConsts.SQL_CREATE_PEERS_TABLE);
    }

    private void createObservationsTable() throws SQLException {
        this.createDB(ServerConsts.SQL_CREATE_OBSERVATIONS_TABLE);
    }

    private void createUserLocationsTable() throws SQLException {
        this.createDB(ServerConsts.SQL_CREATE_LOCATIONS_TABLE);
    }

    /**
     * Starts the daemon that periodically commits to the database. This is done
     * to prevent statements from never getting committed if the buffer doesn't
     * reach its size.
     */
    private void startAutoCommitDaemon() {
        // The runnable loops forever, committing periodically
        final Runnable commitRunnable = () -> {
            while (true) {
                try {
                    Thread.sleep(ServerConsts.DATABASE_AUTO_COMMIT_TIME_MS);
                }
                catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                commit();
            }
        };

        // Create a thread using the runnable and start it as a daemon
        final Thread commitThread = new Thread(commitRunnable);
        commitThread.setDaemon(true);
        commitThread.start();
    }

    /**
     * Commits and resets the counter of uncommitted statements.
     * 
     * @return <code>true</code> on successful commit, <code>false</code>
     *         otherwise.
     */
    private boolean commit() {
        // Nothing to commit
        if (this.uncommittedStmtCount == 0) {
            return true;
        }
        try {
            this.connection.commit();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return false;
        }
        // Reset buffer counter
        this.uncommittedStmtCount = 0;
        return true;
    }

    /**
     * Commits only if the buffer is full.
     * 
     * @throws SQLException If there was an exception when committing to the
     *             database.
     */
    private void bufferedCommit() throws SQLException {
        // Increase the count of uncommitted statements
        this.uncommittedStmtCount++;

        // If buffer full, commit and reset buffer counter
        if (this.uncommittedStmtCount % ServerConsts.COMMIT_BUFFER_SIZE == 0) {
            this.connection.commit();
            this.uncommittedStmtCount = 0;
        }
    }

    private boolean addPeerNonSymmetric(final UID user, final UID peer) {
        try (final PreparedStatement addPeerStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_PEER);) {
            addPeerStmt.setString(1, user.toHexString());
            addPeerStmt.setString(2, peer.toHexString());
            addPeerStmt.executeUpdate();

            // Commit the update
            // this.connection.commit();
            return this.commit();
        }
        catch (final SQLException e) {
            return false;
        }
    }

    /**
     * Adds the given user into the database.
     * 
     * @param user The user to be added.
     * @param key The key of the user.
     * @return <code>true</code> if the user was successfully added into the
     *         database. <code>false</code> otherwise, e.g. when the user
     *         already exists in the database or there was an error adding them
     *         into the database.
     */
    public boolean addUser(final UID user, final Key key) {
        // Add the user into the DB
        try (final PreparedStatement addUserStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_USER);) {
            addUserStmt.setString(1, user.toHexString());

            // Execute the update and commit it
            addUserStmt.executeUpdate();
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        // Add also the user's key
        final boolean status = this.keyStore.addSecretKey(user, key);
        return status;
    }

    /**
     * Adds multiple users into the database. This method is much faster than
     * using the <code>addUser</code> method in a loop, since batch insert into
     * the database is used. Use this method preferably if multiple UIDs are to
     * be inserted.
     * 
     * @param users The list of users to be added into the database.
     * @return <code>true</code> if users were successfully added into the
     *         database, <code>false</code> otherwise.
     */
    public boolean batchAddUsers(final List<BellrockUser> users) {
        // Use batch add to add multiple users into the DB.
        try (final PreparedStatement batchAddUserStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_USER);) {
            // Go over every user in the list
            for (final BellrockUser user : users) {
                batchAddUserStmt.setString(1, user.getUID().toHexString());
                batchAddUserStmt.addBatch();
            }
            // Execute the batch and commit it
            batchAddUserStmt.executeBatch();
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        // Add also the users' keys
        final boolean status = this.keyStore.batchAddSecretKeys(users);
        return status;
    }

    /**
     * Deletes the given user from all databases, i.e. from the UID DB, Keys DB,
     * Peers DB and Observations DB. After this operation, no history of the
     * user should be left in the system. Watch out, this is not reversible!
     * 
     * @param uid The UID of the user to be removed.
     * @return <code>true</code> if the user was successfully removed from all
     *         tables in the database. <code>false</code> otherwise.
     */
    public boolean deleteUser(final UID uid) {
        try {
            // Delete from the UID table
            try (final PreparedStatement delUserStmt = this.connection
                    .prepareStatement(ServerConsts.SQL_DEL_USER_FROM_UID);) {
                delUserStmt.setString(1, uid.toHexString());
                delUserStmt.executeUpdate();
            }

            // Delete from the Peers table
            try (final PreparedStatement delUserStmt = this.connection
                    .prepareStatement(ServerConsts.SQL_DEL_USER_FROM_PEERS);) {
                delUserStmt.setString(1, uid.toHexString());
                delUserStmt.setString(2, uid.toHexString());
                delUserStmt.executeUpdate();
            }

            // Delete from the Keys table
            this.keyStore.deleteSecretKey(uid);

            // Delete from the Observations table
            try (final PreparedStatement delUserStmt = this.connection
                    .prepareStatement(
                            ServerConsts.SQL_DEL_USER_FROM_OBSERVATIONS);) {
                delUserStmt.setString(1, uid.toHexString());
                delUserStmt.setString(2, uid.toHexString());
                delUserStmt.executeUpdate();
            }

            // Commit the updates
            this.connection.commit();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Retrieves UIDs of all users in the database.
     * 
     * @return A set of UIDs of all users that are in the database. A set is
     *         used since the UIDs are unique by definition.
     */
    public Set<UID> getAllUIDs() {
        final Set<UID> uids = new HashSet<>();
        // Execute the command
        try (final PreparedStatement getUIDsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_ALL_UIDS);
                final ResultSet result = getUIDsStmt.executeQuery();) {

            // Retrieve the UIDs and add them to a list
            while (result.next()) {
                final String uidHex = result.getString(1);
                final UID uid = new UID(uidHex);
                uids.add(uid);
            }
        }
        catch (final SQLException e) {
            return null;
        }
        return uids;
    }

    /**
     * Checks if the database contains user with the given UID.
     * 
     * @param uid The UID of the user.
     * @return <code>true</code> if there is such user in the dabatase,
     *         <code>false</code> otherwise.
     */
    public boolean containsUser(final UID uid) {
        // Execute the command
        try (final PreparedStatement getUIDsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_CONTAINS_UID);
                final ResultSet result = getUIDsStmt.executeQuery();) {

            // Return if the user is in the database
            return result.getInt(1) == 1;
        }
        catch (final SQLException e) {
            return false;
        }
    }

    /**
     * Adds the given key to the given user. If the user already has a key then
     * the old key will be replaced by the new key.
     * 
     * @param user The UID of the user.
     * @param key The key of the user.
     * @return <code>true</code> on success, <code>false</code> otherwise.
     */
    public boolean addUserKey(final UID user, final Key key) {
        final boolean success = this.keyStore.addSecretKey(user, key);
        return success;
    }

    /**
     * Gets the key for the given user.
     * 
     * @param uid The UID of the user.
     * @return The key for that user or <code>null</code> on error.
     */
    public Key getUserKey(final UID uid) {
        return this.keyStore.getSecretKey(uid);
    }

    /**
     * @return A set of Bellrock users <code>null</code> is returned if there
     *         was an error with the underlying key store.
     */
    public Map<UID, BellrockUser> getAllUsers() {
        return this.keyStore.getAllUserKeyMap();
    }

    /**
     * Adds the given pair of peers into the database of peers. The peer
     * relationship is symmetric, i.e. if A is a peer with B, then B is a peer
     * with A. Hence if an (A,B) peer is added, (B,A) peer is added
     * automatically by this method.
     * 
     * @param user The user.
     * @param peer The peer of that user.
     * @return <code>true</code> if the peer was successfully added,
     *         <code>false</code> otherwise.
     */
    public boolean addPeer(final UID user, final UID peer) {
        // Add the peer pair into the Peers DB. This is symmetric relation,
        // hence add both directions.
        final boolean op1Success = this.addPeerNonSymmetric(user, peer);
        final boolean op2Success = this.addPeerNonSymmetric(peer, user);
        return op1Success & op2Success;
    }

    /**
     * Deletes the given pair of peers from the database. Note that this
     * relation is symmetric, hence if (A,B) pair is deleted, (B,A) pair is
     * deleted as well.
     * 
     * @param user The user.
     * @param peer The peer of that user.
     * @return <code>true</code> if the peer was successfully deleted,
     *         <code>false</code> otherwise.
     */
    public boolean deletePeer(final UID user, final UID peer) {
        try (final PreparedStatement delUserStmt = this.connection
                .prepareStatement(ServerConsts.SQL_DEL_PEER);) {
            delUserStmt.setString(1, user.toHexString());
            delUserStmt.setString(2, peer.toHexString());
            delUserStmt.setString(3, peer.toHexString());
            delUserStmt.setString(4, user.toHexString());
            delUserStmt.executeUpdate();

            // Commit the update
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Gets the list of peers of the given user.
     * 
     * @param user The user.
     * @return A list of peers of the given user or <code>null</code> on error.
     */
    public List<UID> getPeers(final UID user) {
        final List<UID> peers = new ArrayList<>();
        // Execute the command
        try (final PreparedStatement getUIDsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_PEERS);) {
            getUIDsStmt.setString(1, user.toHexString());
            try (final ResultSet result = getUIDsStmt.executeQuery();) {

                // Retrieve the UIDs and add them to a list
                while (result.next()) {
                    final String uidHex = result.getString(1);
                    final UID uid = new UID(uidHex);
                    peers.add(uid);
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        return peers;
    }

    /**
     * Adds the given observation into the database.
     * 
     * @param observation The observation.
     * @return <code>true</code> if the observation was successfully added,
     *         <code>false</code> otherwise.
     */
    public boolean addObservation(final Observation observation) {
        // Add the observations into the DB
        try (final PreparedStatement addObsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_OBSERVATION);) {
            this.prepareObservationStatement(addObsStmt, observation);
            addObsStmt.executeUpdate();

            // Commit the update
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Adds all the observations made by the user into the database. This is a
     * batch add, hence it is recommended to use this method rather than the
     * <code>addObservation()</code> in a for loop, since this performs about
     * 100 times better, especially if the number of observations is high.
     * 
     * @param observations The observations made by the user.
     * @return <code>true</code> if all observations were successfully written
     *         into the database, <code>false</code> otherwise.
     */
    public boolean batchAddObservations(final Observations observations) {
        // Add the observations into the DB
        try (final PreparedStatement batchAddObsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_OBSERVATION);) {
            // Go through all observations in the list
            for (final Observation observation : observations
                    .getObservations()) {
                this.prepareObservationStatement(batchAddObsStmt, observation);
                batchAddObsStmt.addBatch();
            }
            // Execute the batch and commit it
            batchAddObsStmt.executeBatch();
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    private void prepareObservationStatement(final PreparedStatement addObsStmt,
            final Observation observation) throws SQLException {
        addObsStmt.setString(1, observation.getObserverUID().toHexString());
        addObsStmt.setString(2, observation.getAID().toHexString());
        final String resolvedUID = observation.getResolvedUID() != null
                ? observation.getResolvedUID().toHexString() : "";
        addObsStmt.setString(3, resolvedUID);
        addObsStmt.setLong(4, observation.getTime().toEpochMilli());
        addObsStmt.setDouble(5, observation.getLocation().getLatitude());
        addObsStmt.setDouble(6, observation.getLocation().getLongitude());
        addObsStmt.setString(7, observation.getLocation().getName());
    }

    /**
     * Delete the given observation.
     * 
     * @param observation The observation to be deleted.
     * @return <code>true</code> if the observation was successfully deleted,
     *         <code>false</code> otherwise.
     */
    public boolean deleteObservation(final Observation observation) {
        try (final PreparedStatement delObsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_DEL_OBSERVATION);) {
            delObsStmt.setString(1, observation.getObserverUID().toHexString());
            delObsStmt.setString(2, observation.getAID().toHexString());
            final String resolvedUID = observation.getResolvedUID() != null
                    ? observation.getResolvedUID().toHexString() : "";
            delObsStmt.setString(3, resolvedUID);
            delObsStmt.setLong(4, observation.getTime().toEpochMilli());
            delObsStmt.setDouble(5, observation.getLocation().getLatitude());
            delObsStmt.setDouble(6, observation.getLocation().getLongitude());
            delObsStmt.setString(7, observation.getLocation().getName());
            delObsStmt.executeUpdate();

            // Commit the update
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Gets all observations of the given user.
     * 
     * @param user The user.
     * @return A list of all observations of the given user or <code>null</code>
     *         on error.
     */
    public List<Observation> getObservations(final UID user) {
        final List<Observation> observations = new ArrayList<>();
        // Execute the command
        try (final PreparedStatement getUIDsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_OBSERVATIONS);) {
            getUIDsStmt.setString(1, user.toHexString());
            try (final ResultSet result = getUIDsStmt.executeQuery();) {

                // Retrieve the observations and add them to a list
                while (result.next()) {
                    // Get all fields
                    final String observerUID = result.getString(1);
                    final String observedAID = result.getString(2);
                    final String resolvedUID = result.getString(3);
                    final long tsmpRaw = result.getLong(4);
                    final double lat = result.getDouble(5);
                    final double lon = result.getDouble(6);
                    final String locName = result.getString(7);

                    // Process the raw data
                    final UID observer = new UID(observerUID);
                    final AnonymousID observed = new AnonymousID(observedAID);
                    final Instant timestamp = Instant.ofEpochMilli(tsmpRaw);
                    final Location location = new Location(lat, lon, locName);

                    // Create new observation object
                    final Observation obs = new Observation(observer, observed,
                            timestamp, location);

                    // Add the resolved UID if non-empty and has proper length
                    if (resolvedUID.trim()
                            .length() == ServerConsts.UID_HEX_STR_LEN) {
                        final UID resolved = new UID(resolvedUID);
                        obs.addResolvedUID(resolved);
                    }
                    observations.add(obs);
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        return observations;
    }

    /**
     * Adds the location of the given user.
     * 
     * @param user The user.
     * @param location The location.
     * @return <code>true</code> if the location was successfully added into the
     *         database, <code>false</code> otherwise.
     */
    public boolean addUserLocation(final UID user,
            final UserLocation location) {
        // Add the location into the DB
        try (final PreparedStatement addLocStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_LOCATION);) {
            addLocStmt.setString(1, user.toHexString());
            addLocStmt.setLong(2, location.getStart().toEpochMilli());
            addLocStmt.setLong(3, location.getEnd().toEpochMilli());
            addLocStmt.setFloat(4, location.getLocation().getLatitude());
            addLocStmt.setFloat(5, location.getLocation().getLongitude());
            addLocStmt.setLong(6, location.getCellTowerInfo().pack());

            // Execute the batch and commit it
            addLocStmt.executeUpdate();
            this.bufferedCommit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Batch adds the locations of the given user.
     * 
     * @param user The user.
     * @param locations The list of locations.
     * @return <code>true</code> if the locations were successfully added into
     *         the database, <code>false</code> otherwise.
     */
    public boolean batchAddUserLocations(final UID user,
            final List<UserLocation> locations) {
        // Add the list of locations into the DB
        try (final PreparedStatement batchAddLocStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_LOCATION);) {
            for (final UserLocation location : locations) {
                batchAddLocStmt.setString(1, user.toHexString());
                batchAddLocStmt.setLong(2, location.getStart().toEpochMilli());
                batchAddLocStmt.setLong(3, location.getEnd().toEpochMilli());
                batchAddLocStmt.setFloat(4,
                        location.getLocation().getLatitude());
                batchAddLocStmt.setFloat(5,
                        location.getLocation().getLongitude());
                batchAddLocStmt.setLong(6, location.getCellTowerInfo().pack());
                batchAddLocStmt.executeUpdate();
                batchAddLocStmt.addBatch();
            }

            // Execute the batch and commit it
            batchAddLocStmt.executeBatch();
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Gets all previous coarse locations of the given user.
     * 
     * @param user The user.
     * @return A list of coarse locations.
     */
    public List<UserLocation> getUserCoarseLocations(final UID user) {
        final List<UserLocation> observations = new ArrayList<>();
        // Execute the command
        try (final PreparedStatement getLocsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_LOCATIONS);) {
            getLocsStmt.setString(1, user.toHexString());
            try (final ResultSet result = getLocsStmt.executeQuery();) {

                // Retrieve the coarse locations and add them to a list
                while (result.next()) {
                    observations.add(this.parseCoarseLocationResult(result));
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        return observations;
    }

    /**
     * Gets all previous coarse locations of the given user in the given
     * interval. That is, any coarse location of the given user that overlaps
     * with the given time interval will be returned.
     * 
     * @param user The user.
     * @param start The start of the time interval.
     * @param end The end of the time interval.
     * @return A list of coarse locations.
     */
    public List<UserLocation> getUserCoarseLocations(final UID user,
            final Instant start, final Instant end) {
        final List<UserLocation> observations = new ArrayList<>();
        // Execute the command
        try (final PreparedStatement getLocsStmt = this.connection
                .prepareStatement(
                        ServerConsts.SQL_GET_LOCATIONS_IN_INTERVAL);) {
            getLocsStmt.setString(1, user.toHexString());
            getLocsStmt.setLong(2, start.toEpochMilli());
            getLocsStmt.setLong(3, start.toEpochMilli());
            getLocsStmt.setLong(4, end.toEpochMilli());
            getLocsStmt.setLong(5, end.toEpochMilli());
            try (final ResultSet result = getLocsStmt.executeQuery();) {

                // Retrieve the coarse locations and add them to a list
                while (result.next()) {
                    observations.add(this.parseCoarseLocationResult(result));
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        return observations;
    }

    /**
     * Retrieve UIDs of all users that were at the wrong time at the wrong place
     * :).
     * 
     * @param location The coarse location of the event, i.e. rounded WGS84
     *            coordinates.
     * @param timestamp The time of the event.
     * @return A list of UIDs of users who were at the given place at the given
     *         time.
     */
    public List<UID> getUsersAtPlaceAtTime(final CoarseLocation location,
            final Instant timestamp) {
        final List<UID> users = new ArrayList<>();

        // Execute the command
        try (final PreparedStatement getLocsStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_USERS_AT_LOCATION);) {
            getLocsStmt.setFloat(1, location.getLatitude());
            getLocsStmt.setFloat(2, location.getLongitude());
            getLocsStmt.setLong(3, timestamp.toEpochMilli());
            getLocsStmt.setLong(4, timestamp.toEpochMilli());
            try (final ResultSet result = getLocsStmt.executeQuery();) {
                // Retrieve the coarse locations and add them to a list
                while (result.next()) {
                    users.add(new UID(result.getString(1)));
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        return users;
    }

    /**
     * Retrieve UIDs of all users that were at the wrong time at the wrong place
     * :).
     * 
     * @param location The coarse location of the event, i.e. rounded WGS84
     *            coordinates.
     * @param start The start time of the event.
     * @param end The end time of the event.
     * @return A list of UIDs of users who were at the given place durin the
     *         given time interval.
     */
    public List<UID> getUsersAtPlaceAtTime(final CoarseLocation location,
            final Instant start, final Instant end) {
        final List<UID> users = new ArrayList<>();

        // Execute the command
        try (final PreparedStatement getLocsStmt = this.connection
                .prepareStatement(
                        ServerConsts.SQL_GET_USERS_AT_LOCATION_AT_INTERVAL);) {
            getLocsStmt.setFloat(1, location.getLatitude());
            getLocsStmt.setFloat(2, location.getLongitude());
            getLocsStmt.setLong(3, start.toEpochMilli());
            getLocsStmt.setLong(4, start.toEpochMilli());
            getLocsStmt.setLong(5, end.toEpochMilli());
            getLocsStmt.setLong(6, end.toEpochMilli());
            try (final ResultSet result = getLocsStmt.executeQuery();) {
                // Retrieve the coarse locations and add them to a list
                while (result.next()) {
                    users.add(new UID(result.getString(1)));
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        return users;
    }

    private UserLocation parseCoarseLocationResult(final ResultSet result)
            throws SQLException {
        // Get all fields
        final long startRaw = result.getLong(1);
        final long endRaw = result.getLong(2);
        final double lat = result.getFloat(3);
        final double lon = result.getFloat(4);
        final long cellTowerInfoRaw = result.getLong(5);

        // Process the raw data
        final Instant start = Instant.ofEpochMilli(startRaw);
        final Instant end = Instant.ofEpochMilli(endRaw);
        final CoarseLocation location = new CoarseLocation((float) lat,
                (float) lon);
        final CellTower cellTowerInfo = new CellTower(cellTowerInfoRaw);

        // Return new coarse location object
        return new UserLocation(start, end, location, cellTowerInfo);
    }

    /**
     * Clears data from all tables.
     * 
     * @return <code>true</code> if the database was successfully cleared,
     *         <code>false</code> otherwise.
     */
    public boolean clearDB() {
        try (final PreparedStatement clearAllStmt = this.connection
                .prepareStatement(ServerConsts.SQL_CLEAR_ALL);) {
            clearAllStmt.executeUpdate();
            // Commit the update
            this.connection.commit();

            // Clear the key store
            this.keyStore.clearKeyStore();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Shuts down the database.
     * 
     * @return <code>true</code> if database successfully shut down,
     *         <code>false</code> otherwise.
     */
    public boolean shutDown() {
        try (final PreparedStatement shutDownStmt = this.connection
                .prepareStatement(ServerConsts.SQL_SHUT_DOWN);) {
            shutDownStmt.executeUpdate();
            // Commit the update
            this.connection.commit();

            // Shutdown the key store
            this.keyStore.shutDown();

            // Close the connection
            this.connection.close();
        }
        catch (final SQLException e) {
            return false;
        }
        // Shut down the key store
        final boolean keyStoreStatus = this.keyStore.shutDown();
        return true & keyStoreStatus;
    }

}
