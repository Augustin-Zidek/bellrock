package eu.zidek.augustin.bellrock.server;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import eu.zidek.augustin.bellrock.identification.MsgConstants;
import eu.zidek.augustin.bellrock.identification.UID;

/**
 * Each user in the system needs to have a UID and a random secret key. This
 * class takes care of that. Also, User Manager automatically creates and
 * deletes users in the underlying database.
 * 
 * @author Augustin Zidek
 *
 */
public class UserManager {
    private static UserManager instance = null;
    private final Map<UID, BellrockUser> users;
    private final BellrockDatabase db;

    private UserManager() throws SQLException {
        // Initialise the database
        this.db = BellrockDatabase.getInstance();
        // Load users from the db. This is done to speed up the access time
        // to user lists. This has to be called after the db was initialised.
        this.users = this.loadUserKeysFromDB();
    }

    /**
     * Initializes the User Manager which takes care of user UIDs and their
     * private keys. The User Manager is singleton to prevent problems with
     * concurrent user addition.
     * 
     * @return A singleton instance of the User Manager.
     * @throws SQLException If the SQL database can't be initialised.
     */
    public static UserManager getInstance() throws SQLException {
        if (UserManager.instance == null) {
            UserManager.instance = new UserManager();
        }
        return UserManager.instance;
    }

    /**
     * Creates a new Bellrock user and gives them a random unique UID.
     * 
     * @return The new Bellrock user or <code>null</code> if there was an error
     *         when creating the new user.
     */
    public BellrockUser newUser() {
        // New user with random unique UID and random key
        final BellrockUser newUser = this.createNewUser();

        // Save the UID in the db and add it into the user UIDs set
        this.db.addUser(newUser.getUID(), newUser.getKey());

        return newUser;
    }

    /**
     * Creates a <code>count</code> number of new Bellrock users, each having a
     * unique UID and a random secret key. They are also added to the internal
     * database. If lot of users need to be created at the same time, this
     * method is much faster than using <code>newUser</code> in a loop.
     * 
     * @param count The number of users to be created.
     * @return A list of new Bellrock users <code>null</code> if there was an
     *         error when creating the new users.
     */
    public List<BellrockUser> newUsers(final int count) {
        final List<BellrockUser> newUsers = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            newUsers.add(this.createNewUser());
        }

        // Add the users into the database
        this.db.batchAddUsers(newUsers);

        return newUsers;
    }

    /**
     * Generates a new random secret key for the user with the given UID.
     * 
     * @param user The UID of the user.
     * @return The new random secret key.
     */
    public SecretKey renewUserKey(final UID user) {
        // Generate the new random key
        final SecretKey newKey = RandomKeyGenerator
                .getKey(ServerConsts.KEY_LENGTH);
        // Overwrite the key
        this.db.addUserKey(user, newKey);
        // Overwrite the key locally
        this.users.put(user, new BellrockUser(user, newKey));
        return newKey;
    }

    /**
     * Removes the user with the given UID from the system.
     * 
     * @param userUID The user UID.
     * @return <code>true</code> if user successfully removed,
     *         <code>false</code> otherwise.
     */
    public boolean deleteUser(final UID userUID) {
        final boolean status = this.db.deleteUser(userUID);
        this.users.remove(userUID);
        return status;
    }

    /**
     * Gets the Bellrock user object for the given UID.
     * 
     * @param uid The UID.
     * @return Bellrock user with the given UID or <code>null</code> if there is
     *         no such user.
     */
    public BellrockUser getUser(final UID uid) {
        return this.users.get(uid);
    }

    /**
     * Returns the Bellrock user objects for the given UIDs.
     * 
     * @param uids UIDs of users.
     * @return The list of users that have the given UIDs. If some user with the
     *         given UIDs don't exist in the system, their object is not
     *         returned in the list.
     */
    public List<BellrockUser> getUsers(final List<UID> uids) {
        final List<BellrockUser> filteredUsers = new ArrayList<>(uids.size());
        for (final UID uid : uids) {
            if (this.users.containsKey(uid)) {
                filteredUsers.add(this.users.get(uid));
            }
        }
        return filteredUsers;
    }

    /**
     * @return The number of users in the system.
     */
    public int userCount() {
        return this.users.size();
    }

    /**
     * Registers a peer relationship between two users. The relationship is
     * symmetric, hence if (A,B) pair is registered, the (B,A) pair is
     * registered automatically.
     * 
     * @param userUID The user.
     * @param peerUID The user's peer.
     */
    public void addPeer(final UID userUID, final UID peerUID) {
        final BellrockUser user = this.users.get(userUID);
        final BellrockUser peer = this.users.get(peerUID);
        user.addPeer(peer);
        peer.addPeer(user);
        this.db.addPeer(userUID, peerUID);
    }

    /**
     * Deletes a peer relationship between two users. The relationship is
     * symmetric, hence if (A,B) pair is registered, the (B,A) pair is
     * registered automatically.
     * 
     * @param userUID The user.
     * @param peerUID The user's peer.
     */
    public void deletePeer(final UID userUID, final UID peerUID) {
        final BellrockUser user = this.users.get(userUID);
        final BellrockUser peer = this.users.get(peerUID);
        user.deletePeer(peer);
        peer.deletePeer(user);
        this.db.deletePeer(userUID, peerUID);
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
        // Resolve the location
        location.resolveCoarseLocation();
        // Add into data structure and into the database
        this.users.get(user).addLocation(location);
        this.db.addUserLocation(user, location);
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
        // Resolve all locations
        for (final UserLocation location : locations) {
            location.resolveCoarseLocation();
        }
        // Add into data structure and into the database
        this.users.get(user).addLocations(locations);
        this.db.batchAddUserLocations(user, locations);
    }

    /**
     * Removes all users that are cached in the User Manager.
     */
    public void clearCachedUsers() {
        this.users.clear();
    }

    private UID getRandomUID() {
        final SecureRandom random = new SecureRandom();
        final byte[] randomUIDBytes = new byte[MsgConstants.UID_LENGTH];
        random.nextBytes(randomUIDBytes);
        return new UID(randomUIDBytes);
    }

    private Map<UID, BellrockUser> loadUserKeysFromDB() {
        return this.db.getAllUsers();
    }

    /**
     * @return Creates a new Bellrock user with a unique ID and a random secret
     *         key and adds the user and the key into the internal data
     *         structures.
     */
    private BellrockUser createNewUser() {
        // Make sure the UID is unique
        UID newUID = this.getRandomUID();
        while (this.db.containsUser(newUID)) {
            newUID = this.getRandomUID();
        }

        // Generate a secret key
        final SecretKey newKey = RandomKeyGenerator
                .getKey(ServerConsts.KEY_LENGTH);

        // Save the user locally for faster future access
        final BellrockUser newUser = new BellrockUser(newUID, newKey);
        this.users.put(newUID, newUser);

        return newUser;
    }
}
