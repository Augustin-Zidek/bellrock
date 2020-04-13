package eu.zidek.augustin.bellrock.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Key;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.UID;
import eu.zidek.augustin.bellrock.server.BellrockDatabase;
import eu.zidek.augustin.bellrock.server.Location;
import eu.zidek.augustin.bellrock.server.Observation;
import eu.zidek.augustin.bellrock.server.RandomKeyGenerator;

/**
 * Test of the Bellrock database.
 * 
 * @author Augustin Zidek
 *
 */
public class BellrockDatabaseTest {
    private static BellrockDatabase db;
    private final Key key = RandomKeyGenerator.getKey(128);

    /**
     * Initialises the DB, i.e. creates all the tables if they not exist.
     */
    @BeforeClass
    public static void initDB() {
        try {
            db = BellrockDatabase.getInstance();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            fail();
            return;
        }
    }

    /**
     * Two distinct users added successfully.
     */
    @Test
    public void testAddDistinctUsers() {
        final UID user1 = new UID("0011223344556677");
        final UID user2 = new UID("0A11223344556677");

        assertTrue(db.addUser(user1, this.key));
        assertTrue(db.addUser(user2, this.key));
    }

    /**
     * Two users with the same ID can't be added twice.
     */
    @Test
    public void testAddTheSameUserTwice() {
        final UID user1 = new UID("0000000000000000");
        final UID user2 = new UID("0000000000000000");

        assertTrue(db.addUser(user1, this.key));
        assertFalse(db.addUser(user2, this.key));
    }

    /**
     * Multiple users added to the database are all retrieved.
     */
    @Test
    public void testGetAllUIDs() {
        final UID user1 = new UID("0000000000000000");
        final UID user2 = new UID("0000000000000001");
        final UID user3 = new UID("0000000000000002");
        assertTrue(db.addUser(user1, this.key));
        assertTrue(db.addUser(user2, this.key));
        assertTrue(db.addUser(user3, this.key));

        final Set<UID> uids = db.getAllUIDs();
        assertEquals(3, uids.size());
        assertTrue(uids.contains(user1));
        assertTrue(uids.contains(user2));
        assertTrue(uids.contains(user3));
    }

    /**
     * Add two users and remove the latter. First must remain intact.
     */
    @Test
    public void testDeleteExistingUser() {
        final UID user1 = new UID("0000000000000000");
        final UID user2 = new UID("0000000000000001");
        assertTrue(db.addUser(user1, this.key));
        assertTrue(db.addUser(user2, this.key));

        Set<UID> uids = db.getAllUIDs();
        assertEquals(2, uids.size());
        assertTrue(uids.contains(user1));
        assertTrue(uids.contains(user2));

        // Delete user1, user2 should remain in the DB
        assertTrue(db.deleteUser(user1));
        uids = db.getAllUIDs();
        assertEquals(1, uids.size());
        assertTrue(uids.contains(user2));
    }

    /**
     * Add a user, remove a different one. The user in the DB must remain
     * intact.
     */
    @Test
    public void testDeleteeNonExistingUser() {
        final UID userInDB = new UID("0000000000000000");
        final UID userNotInDB = new UID("0000000000000001");
        assertTrue(db.addUser(userInDB, this.key));

        Set<UID> uids = db.getAllUIDs();
        assertEquals(1, uids.size());
        assertTrue(uids.contains(userInDB));

        // Delete userNotInDB, nothing should happen to the users in the DB
        assertTrue(db.deleteUser(userNotInDB));
        uids = db.getAllUIDs();
        assertEquals(1, uids.size());
        assertTrue(uids.contains(userInDB));
    }

    /**
     * Test if deleting user deletes them from all tables.
     */
    @Test
    public void testComplexUserDelete() {
        // Add 4 users into the DB
        final UID user1 = new UID("0000000000000000");
        final UID user2 = new UID("0000000000000001");
        final UID user3 = new UID("0000000000000002");
        final UID user4 = new UID("0000000000000003");
        assertTrue(db.addUser(user1, this.key));
        assertTrue(db.addUser(user2, this.key));
        assertTrue(db.addUser(user3, this.key));
        assertTrue(db.addUser(user4, this.key));

        // Add peers into the DB
        assertTrue(db.addPeer(user1, user2));
        assertTrue(db.addPeer(user1, user3));
        assertTrue(db.addPeer(user2, user4));

        // Add 2 observations involving user 2
        final AnonymousID aid = new AnonymousID(new byte[] { 0x0, 0x0, 0x0, 0x0,
                0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 });
        final Instant time = Instant.now();
        final Location location = new Location(52.211207, 0.091646, "WGB");
        final Observation obs1 = new Observation(user1, aid, time, location);
        obs1.addResolvedUID(user2);
        final Observation obs2 = new Observation(user2, aid, time, location);
        obs2.addResolvedUID(user1);
        assertTrue(db.addObservation(obs1));
        assertTrue(db.addObservation(obs2));

        // Delete user 2
        assertTrue(db.deleteUser(user2));

        final Set<UID> users = db.getAllUIDs();
        assertEquals(3, users.size());

        final List<UID> user1Peers = db.getPeers(user1);
        assertEquals(1, user1Peers.size());

        final List<Observation> observations = db.getObservations(user1);
        assertEquals(0, observations.size());
    }

    /**
     * Add a couple of peers and test if users have correct peers.
     */
    @Test
    public void testAddPeer() {
        final UID user1 = new UID("0000000000000000");
        final UID user2 = new UID("0000000000000001");
        final UID user3 = new UID("0000000000000002");
        final UID user4 = new UID("0000000000000002");
        assertTrue(db.addPeer(user1, user2));
        assertTrue(db.addPeer(user1, user3));
        assertTrue(db.addPeer(user1, user4));
        assertTrue(db.addPeer(user2, user3));

        final List<UID> user1Peers = db.getPeers(user1);
        assertEquals(3, user1Peers.size());
        assertEquals(user2, user1Peers.get(0));
        assertEquals(user3, user1Peers.get(1));
        assertEquals(user4, user1Peers.get(2));

        final List<UID> user2Peers = db.getPeers(user2);
        assertEquals(2, user2Peers.size());
        assertEquals(user1, user2Peers.get(0));
        assertEquals(user3, user2Peers.get(1));
    }

    /**
     * Add a couple of peers and then remove one.
     */
    @Test
    public void testDeletePeer() {
        final UID user1 = new UID("0000000000000000");
        final UID user2 = new UID("0000000000000001");
        final UID user3 = new UID("0000000000000002");
        final UID user4 = new UID("0000000000000002");
        assertTrue(db.addPeer(user1, user2));
        assertTrue(db.addPeer(user1, user3));
        assertTrue(db.addPeer(user1, user4));
        assertTrue(db.addPeer(user2, user3));
        assertTrue(db.deletePeer(user1, user2));

        final List<UID> user1Peers = db.getPeers(user1);
        assertEquals(2, user1Peers.size());
        assertEquals(user3, user1Peers.get(0));
        assertEquals(user4, user1Peers.get(1));

        final List<UID> user2Peers = db.getPeers(user2);
        assertEquals(1, user2Peers.size());
        assertEquals(user3, user2Peers.get(0));
    }

    /**
     * Add an observation and test its presence in the db.
     */
    @Test
    public void testAddObservation() {
        final UID uid1 = new UID("0000000000000000");
        final UID uid2 = new UID("0000000000000001");
        final AnonymousID aid = new AnonymousID(new byte[] { 0x0, 0x0, 0x0, 0x0,
                0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 });
        final Instant time = Instant.now();
        final Location location = new Location(52.211207, 0.091646, "WGB");
        final Observation obs1 = new Observation(uid1, aid, time, location);
        final Observation obs2 = new Observation(uid2, aid, time, location);

        assertTrue(db.addObservation(obs1));
        assertTrue(db.addObservation(obs2));

        final List<Observation> observations = db.getObservations(uid1);
        assertEquals(1, observations.size());
        assertEquals(obs1, observations.get(0));
    }

    /**
     * Add couple of observations and test if they are properly deleted.
     */
    @Test
    public void testDeleteObservation() {
        final UID uid1 = new UID("0000000000000000");
        final UID uid2 = new UID("0000000000000001");
        final AnonymousID aid = new AnonymousID(new byte[] { 0x0, 0x0, 0x0, 0x0,
                0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 });
        final Instant time = Instant.now();
        final Location location = new Location(52.211207, 0.091646, "WGB");
        final Observation obs1 = new Observation(uid1, aid, time, location);
        final Observation obs2 = new Observation(uid2, aid, time, location);

        assertTrue(db.addObservation(obs1));
        assertTrue(db.addObservation(obs2));
        assertTrue(db.deleteObservation(obs2));

        List<Observation> observations = db.getObservations(uid1);
        assertEquals(1, observations.size());
        assertEquals(obs1, observations.get(0));

        assertTrue(db.deleteObservation(obs1));
        observations = db.getObservations(uid1);
        assertEquals(0, observations.size());
    }

    /**
     * Clears the database, i.e. erases all data from all tables.
     */
    @After
    public void clearDB() {
        assertTrue(db.clearDB());
    }
}
