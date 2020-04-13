package eu.zidek.augustin.bellrock.server;

import java.security.Key;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import eu.zidek.augustin.bellrock.identification.UID;

/**
 * Key Store for Bellrock client-server secret keys which are used to encrypt
 * UIDs into an Anonymous IDs.
 * 
 * @author Augustin Zidek
 *
 */
public class BellrockDBKeyStore {
    private static BellrockDBKeyStore instance = null;
    private Connection connection;

    private BellrockDBKeyStore() throws SQLException {
        // Open the connection to the database
        this.initialiseDB(ServerConsts.KEY_STORE_DB_PATH);

        // Create the key store table, if it doesn't exist
        this.createKeyStoreTable();
    }

    /**
     * @return An instance of the Bellrock Key Store.
     * @throws SQLException Thrown if the database is corrupted or there were
     *             problems opening it.
     */
    public static BellrockDBKeyStore getInstance() throws SQLException {
        if (BellrockDBKeyStore.instance == null) {
            BellrockDBKeyStore.instance = new BellrockDBKeyStore();
        }
        return BellrockDBKeyStore.instance;
    }

    private void initialiseDB(final String databasePath) throws SQLException {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        }
        catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.connection = DriverManager.getConnection(
                "jdbc:hsqldb:file:" + databasePath + ";crypt_key="
                        + ServerConsts.KEY_STORE_DB_KEY + ";crypt_type=AES",
                "SA", "");
        // Always update data on disk
        try (final Statement delayStmt = this.connection.createStatement();) {
            delayStmt.execute("SET WRITE_DELAY FALSE");
        }
        // Disable auto commit
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

    private void createKeyStoreTable() throws SQLException {
        this.createDB(ServerConsts.SQL_CREATE_KEY_STORE_TABLE);
    }

    /**
     * Adds the given (UID, key) pair into the Bellrock Key Store.
     * 
     * @param user The Bellrock user whose key should be added into the store.
     * @param key The key.
     * @return <code>true</code> if the key was successfully added,
     *         <code>false</code> otherwise.
     */
    public boolean addSecretKey(final UID user, final Key key) {
        try (final PreparedStatement addKeyStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_KEY);) {
            addKeyStmt.setString(1, user.toHexString());
            addKeyStmt.setString(2,
                    Base64.getEncoder().encodeToString(key.getEncoded()));
            addKeyStmt.executeUpdate();

            // Commit the update
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Adds the given (UID, key) pair for every user into the Bellrock Key
     * Store.
     * 
     * @param users Bellrock users whose key should be added into the store.
     * @return <code>true</code> if the key were successfully added,
     *         <code>false</code> otherwise.
     */
    public boolean batchAddSecretKeys(final List<BellrockUser> users) {
        try (final PreparedStatement batchAddKeyStmt = this.connection
                .prepareStatement(ServerConsts.SQL_ADD_KEY);) {
            for (final BellrockUser user : users) {
                batchAddKeyStmt.setString(1, user.getUID().toHexString());
                batchAddKeyStmt.setString(2, Base64.getEncoder()
                        .encodeToString(user.getKey().getEncoded()));
                batchAddKeyStmt.addBatch();
            }
            // Execute the batch and commit it
            batchAddKeyStmt.executeUpdate();
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Removes the key stored under the given UID.
     * 
     * @param uid The UID.
     * @return <code>true</code> if the key was successfully removed,
     *         <code>false</code> otherwise.
     */
    public boolean deleteSecretKey(final UID uid) {
        try (final PreparedStatement delKeyStmt = this.connection
                .prepareStatement(ServerConsts.SQL_DEL_KEY);) {
            delKeyStmt.setString(1, uid.toHexString());
            delKeyStmt.executeUpdate();

            // Commit the update
            this.connection.commit();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves the secret key from the Bellrock Key Store.
     * 
     * @param uid The UID corresponding to the secret key.
     * @return The secret key if there is such for the given UID,
     *         <code>null</code> otherwise.
     */
    public Key getSecretKey(final UID uid) {
        Key key = null;
        // Execute the command
        try (final PreparedStatement getKeyStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_KEY);) {
            getKeyStmt.setString(1, uid.toHexString());
            try (final ResultSet result = getKeyStmt.executeQuery();) {

                // Retrieve the key
                while (result.next()) {
                    // Decode the Base64 encoded key
                    byte[] decodedKey = Base64.getDecoder()
                            .decode(result.getString(1));
                    // Rebuild the key using SecretKeySpec
                    key = new SecretKeySpec(decodedKey, 0, decodedKey.length,
                            "AES");
                }
            }
        }
        catch (final SQLException e) {
            e.printStackTrace();
            return null;
        }
        // Return key or null if not found
        return key;
    }

    /**
     * @return A set of all Bellrock users. <code>null</code> is returned if
     *         there was an error contacting the SQL database.
     */
    public Map<UID, BellrockUser> getAllUserKeyMap() {
        final Map<UID, BellrockUser> users = new HashMap<>();
        // Execute the command
        try (final PreparedStatement getAllUserKeyMapStmt = this.connection
                .prepareStatement(ServerConsts.SQL_GET_ALL_USER_KEY_MAP);
                final ResultSet result = getAllUserKeyMapStmt.executeQuery();) {

            // Retrieve all users and their keys
            while (result.next()) {
                final String uidHex = result.getString(1);
                final UID uid = new UID(uidHex);
                // Decode the Base64 encoded key
                byte[] decodedKey = Base64.getDecoder()
                        .decode(result.getString(1));
                // Rebuild the key using SecretKeySpec
                final Key key = new SecretKeySpec(decodedKey, 0,
                        decodedKey.length, "AES");
                users.put(uid, new BellrockUser(uid, key));
            }
        }
        catch (final SQLException e) {
            return null;
        }
        return users;
    }

    /**
     * Removes all data from the key store.
     * 
     * @return <code>true</code> if successfully emptied, <code>false</code>
     *         otherwise.
     */
    public boolean clearKeyStore() {
        try (final PreparedStatement clearAllStmt = this.connection
                .prepareStatement(ServerConsts.SQL_CLEAR_ALL);) {
            clearAllStmt.executeUpdate();
            // Commit the update
            this.connection.commit();
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
            // Close the connection
            this.connection.close();
        }
        catch (final SQLException e) {
            return false;
        }
        return true;
    }

}
