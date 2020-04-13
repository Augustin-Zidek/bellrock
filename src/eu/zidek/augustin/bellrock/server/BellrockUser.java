package eu.zidek.augustin.bellrock.server;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.MsgConstants;
import eu.zidek.augustin.bellrock.identification.UID;
import eu.zidek.augustin.bellrock.util.LRUCache;

/**
 * Class for storing user of the Bellrock system. The most essential information
 * is the UID and the shared server-client key.
 * 
 * @author Augustin Zidek
 *
 */
public final class BellrockUser {
    // User information
    final UID userUID;
    final Key userKey;
    Cipher cipher = null;

    // Locations of the users
    final List<UserLocation> locations = new ArrayList<>();

    // Peers and lastly seen UIDs
    final List<BellrockUser> peers = new ArrayList<>();
    final Map<UID, BellrockUser> userLRUCache = new LRUCache<>(
            ServerConsts.LAST_SEEN_USER_CACHE_SIZE);

    BellrockUser(final UID userUID, final Key key) {
        this.userUID = userUID;
        this.userKey = key;
    }

    BellrockUser(final UID userUID, final Key key,
            final Cipher initialisedCipher) {
        this.userUID = userUID;
        this.userKey = key;
        this.cipher = initialisedCipher;
    }

    /**
     * @return The unique user ID.
     */
    public UID getUID() {
        return this.userUID;
    }

    /**
     * @return The user-server shared key used for encrypting UIDs when
     *         broadcasting Anonymous IDs.
     */
    public Key getKey() {
        return this.userKey;
    }

    /**
     * @return The list of peers of this Bellrock user.
     */
    public List<BellrockUser> getPeers() {
        return this.peers;
    }

    /**
     * Adds a peer to this Bellrock user.
     * 
     * @param peer The new peer of the user.
     */
    public void addPeer(final BellrockUser peer) {
        this.peers.add(peer);
    }

    /**
     * Removes a peer of this Bellrock user.
     * 
     * @param peer The peer to be removed.
     */
    public void deletePeer(final BellrockUser peer) {
        this.peers.remove(peer);
    }

    /**
     * @return The collection of Bellrock users this Bellrock user saw recently.
     */
    public List<BellrockUser> getLastSeenUIDs() {
        final List<BellrockUser> recentUsers = new ArrayList<>(
                ServerConsts.LAST_SEEN_USER_CACHE_SIZE);
        for (final BellrockUser u : this.userLRUCache.values()) {
            recentUsers.add(u);
        }
        return recentUsers;
    }

    /**
     * Add a user into the list of users the Bellrock user has seen lately. A
     * LRU cache is used.
     * 
     * @param user The UID to be added.
     */
    public void addLastSeenUser(final BellrockUser user) {
        // The cache doesn't contain the user, add it.
        if (!this.userLRUCache.containsKey(user.getUID())) {
            this.userLRUCache.put(user.getUID(), user);
        }
        // The cache contains the user. Remove the user from its old position
        // and put it in the new position in order to increase its freshness.
        else {
            this.userLRUCache.remove(user.getUID());
            this.userLRUCache.put(user.getUID(), user);
        }
    }

    /**
     * Adds a new location into the list of locations the user visited.
     * 
     * @param location The location.
     */
    public void addLocation(final UserLocation location) {
        this.locations.add(location);
    }

    /**
     * Adds a new list of locations into the list of locations the user visited.
     * 
     * @param locations The list of locations.
     */
    public void addLocations(final List<UserLocation> locations) {
        this.locations.addAll(locations);
    }

    /**
     * Goes through the locations of this user and purges all that happened
     * before the given time instant.
     * 
     * @param purgeEnd The time instant after which the locations should be
     *            kept.
     */
    public void purgeOldLocations(final Instant purgeEnd) {
        final Iterator<UserLocation> i = this.locations.iterator();

        while (i.hasNext()) {
            final UserLocation location = i.next();
            if (location.isBefore(purgeEnd)) {
                i.remove();
            }
        }
    }

    /**
     * 
     * @param start The start of the interval.
     * @param end The end of the interval.
     * @return All user locations where the user was during the given time
     *         interval.
     */
    public List<UserLocation> getLocations(final Instant start,
            final Instant end) {
        final List<UserLocation> locationsInInterval = new ArrayList<>();
        for (final UserLocation location : this.locations) {
            if (location.overlapsWith(start, end)) {
                locationsInInterval.add(location);
            }
        }
        return locationsInInterval;
    }

    /**
     * Tries to resolve the given observation using the user's last seen users
     * and user's peers.
     * 
     * @param o The observation to be resolved, i.e. whose sender's UID should
     *            be identified.
     * @return <code>true</code> if the observation sender's UID was
     *         successfully resolved, <code>false</code> otherwise.
     */
    public boolean resolveObservationUsingRecentAndPeers(final Observation o) {
        // Decrypt using user's most recently seen users
        BellrockUser resolvedUser = o.resolveAID(this.getLastSeenUIDs());
        // If resolved, add the user into the list of recently seen users
        if (resolvedUser != null) {
            this.addLastSeenUser(resolvedUser);
            return true;
        }

        // Decrypt using user's peers
        resolvedUser = o.resolveAID(this.peers);
        // If resolved, add the peer into the list of recently seen users
        if (resolvedUser != null) {
            this.addLastSeenUser(resolvedUser);
            return true;
        }
        return false;
    }

    /**
     * Initialising Cipher in decryption mode with a certain key is expensive.
     * It is therefore optimal to do it only once and then reuse the initialised
     * cipher. This method initialises new cipher that can decode Anonymous IDs
     * using this user's key only when called for the fist time. Afterwards, it
     * returns the same, already initialised, cipher.
     * 
     * @return A cipher initialised for decryption using the user's Anonymous
     *         ID. <code>null</code> is returned in case the cipher can't be
     *         initialised.
     */
    public Cipher getInitialisedCipher() {
        // Cipher not initialised, initialise it
        if (this.cipher == null) {
            try {
                this.cipher = Cipher
                        .getInstance(MsgConstants.CIPHER_PARAMETERS);
                this.cipher.init(Cipher.DECRYPT_MODE, this.userKey);
            }
            // None of these exceptions will happen, since the algorithm is
            // valid, the padding is valid and the key also.
            catch (final NoSuchAlgorithmException | NoSuchPaddingException
                    | InvalidKeyException e) {
                e.printStackTrace();
                return null;
            }
        }
        return this.cipher;
    }

    /**
     * 
     * Decrypts the given Anonymous ID using this user's key.
     *
     * @param anonymousID The Anonymous ID.
     * @return The UID which was encrypted into Anonymous ID, the decrypted
     *         nonce is also included.
     * @throws SecurityException if the decryption fails.
     */
    public byte[] decryptAID(final AnonymousID anonymousID)
            throws SecurityException {
        try {
            // Decrypt the AID using the cipher of this user
            return this.getInitialisedCipher().update(anonymousID.getBytes());
        }
        // In case of problem in decryption, throw a SecurityException
        catch (final IllegalArgumentException e) {
            throw new SecurityException(e.getMessage());
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof BellrockUser)) {
            return false;
        }

        return this.getUID().equals(((BellrockUser) obj).getUID());
    }

    @Override
    public int hashCode() {
        return this.getUID().hashCode();
    }

}
