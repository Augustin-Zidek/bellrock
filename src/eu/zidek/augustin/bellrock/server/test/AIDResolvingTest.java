package eu.zidek.augustin.bellrock.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.IDAnonymizer;
import eu.zidek.augustin.bellrock.server.BellrockServer;
import eu.zidek.augustin.bellrock.server.BellrockUser;
import eu.zidek.augustin.bellrock.server.Location;
import eu.zidek.augustin.bellrock.server.Observation;

/**
 * Class to test the performance of the Anonymous ID resolving.
 * 
 * @author Augustin Zidek
 *
 */
public class AIDResolvingTest {
    private static BellrockServer server;

    /**
     * Initialises the server.
     */
    @BeforeClass
    public static void initDB() {
        try {
            server = new BellrockServer();
            server.clearDatabase();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            fail();
            return;
        }
    }

    /**
     * Create a lot of user and test whether they have been created and also the
     * performance of the process. Test both individual and bulk user creation.
     */
    @Test
    public void testUserCreation() {
        final int noOfUsersStart = server.getUserCount();
        System.out.printf("The server has %d users.%n", noOfUsersStart);

        final int noOfUsers1 = 100;
        final int noOfUsers2 = 10000;
        for (int i = 0; i < noOfUsers1; i++) {
            final BellrockUser u = server.newUser();
            assertTrue(u != null);
        }
        System.out.printf("The server has %d users.%n", server.getUserCount());
        final List<BellrockUser> users = server.newUsers(noOfUsers2);
        for (final BellrockUser u : users) {
            assertTrue(u != null);
        }

        System.out.printf("The server has %d users.%n", server.getUserCount());
        System.out.println();

        assertEquals(noOfUsers1 + noOfUsers2,
                server.getUserCount() - noOfUsersStart);
    }

    /**
     * Test the functionality and the performance of the AID decryption.
     */
    @Test
    public void testAIDNaiveDecryption() {
        System.out.println("Started");
        final long t0 = System.currentTimeMillis();

        // Create a lot of users in the system
        final int noOfUsers = 100_000;
        final List<BellrockUser> users = server.newUsers(noOfUsers);
        for (final BellrockUser u : users) {
            assertTrue(u != null);
        }

        System.out.printf("%n%d users populating the system%n",
                server.getUserCount());
        final long t1 = System.currentTimeMillis();
        System.out.printf("Took %d ms%n%n", t1 - t0);

        final Random rand = new Random();

        // Pick a random observer
        final BellrockUser observer = users.get(rand.nextInt(noOfUsers));

        // Create observations
        final IDAnonymizer anonymizer = new IDAnonymizer();

        final int noOfObservations = 1_000;
        final List<BellrockUser> observedUsers = new ArrayList<>(
                noOfObservations);
        final List<Observation> observations = new ArrayList<>(
                noOfObservations);
        for (int i = 0; i < noOfObservations; i++) {
            // Get a random user that has been observed
            final BellrockUser obsUser = users.get(rand.nextInt(noOfUsers));
            // Get an anonymous ID for the user
            final AnonymousID obsAID = anonymizer
                    .getAnonymousID(obsUser.getUID(), obsUser.getKey());
            // Time and location of the observation
            final Instant obsTime = Instant.now();
            final Location obsLoc = new Location(rand.nextDouble() * 180,
                    rand.nextDouble() * 180);
            // Create the observation
            final Observation obs = new Observation(observer.getUID(), obsAID,
                    obsTime, obsLoc);
            observations.add(obs);
            observedUsers.add(obsUser);
        }

        System.out.printf("%d observations created%n", noOfObservations);
        final long t2 = System.currentTimeMillis();
        System.out.printf("Took %d ms%n%n", t2 - t1);

        // Let's decrypt!
        for (final Observation obs : observations) {
            obs.resolveAID(users);
        }

        System.out.printf("%d UIDs resolved %n", noOfObservations);
        final long t3 = System.currentTimeMillis();
        System.out.printf("Took %d ms%n%n", t3 - t2);

        // Check that the resolved UIDs have been resolved correctly
        for (int i = 0; i < noOfObservations; i++) {
            assertEquals(observedUsers.get(i).getUID(),
                    observations.get(i).getResolvedUID());
        }
        System.out.println("Resolved UIDs checked.");
        System.out.println();
    }

    /**
     * Clears the database, i.e. erases all data from all tables.
     */
    @After
    public void clearDB() {
        server.clearDatabase();
    }

    /**
     * Clears the database, i.e. erases all data from all tables and shuts it
     * down.
     */
    @AfterClass
    public static void clearAndShutDownDB() {
        server.clearDatabase();
        server.shutDownDatabase();
    }

}
