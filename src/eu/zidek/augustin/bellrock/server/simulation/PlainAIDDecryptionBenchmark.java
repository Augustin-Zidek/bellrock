package eu.zidek.augustin.bellrock.server.simulation;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import eu.zidek.augustin.bellrock.batlogs.Constants;
import eu.zidek.augustin.bellrock.batlogs.Histogram;
import eu.zidek.augustin.bellrock.identification.AnonymousID;
import eu.zidek.augustin.bellrock.identification.IDAnonymizer;
import eu.zidek.augustin.bellrock.server.BellrockServer;
import eu.zidek.augustin.bellrock.server.BellrockUser;
import eu.zidek.augustin.bellrock.server.Location;
import eu.zidek.augustin.bellrock.server.Observation;

/**
 * Tests the speed of AID decryption against the number of users.
 * 
 * @author Augustin Zidek
 *
 */
public class PlainAIDDecryptionBenchmark {
    private BellrockServer server;
    private final Random rand = new Random();
    private final IDAnonymizer anonymizer = new IDAnonymizer();

    /**
     * Starts the server that will be benchmarked.
     */
    public PlainAIDDecryptionBenchmark() {
        try {
            System.out.println("Starting server.");
            this.server = new BellrockServer();
            this.server.clearDatabase();
            System.out.println("Server started");
        }
        catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the speed of AID decryption.
     * 
     * @param userCount The number of users to be generated.
     * @param noOfObservations The number of observations the observer should
     *            get from each user.
     * @return The time that it took the server to decrypt all observed AIDs.
     */
    public long testRawAIDDecryption(final int userCount,
            final int noOfObservations) {

        // Create a lot of users in the system
        final List<BellrockUser> users = this.server.newUsers(userCount);
        System.out.printf("%d users on server%n", this.server.getUserCount());

        // Pick a random observer
        final BellrockUser observer = users.get(this.rand.nextInt(userCount));

        // Create observations
        final List<BellrockUser> observedUsers = new ArrayList<>(
                noOfObservations);
        final List<Observation> observations = new ArrayList<>(
                noOfObservations);
        for (int i = 0; i < noOfObservations; i++) {
            // Get a random user that has been observed
            final BellrockUser obsUser = users
                    .get(this.rand.nextInt(userCount));
            // Get an anonymous ID for the user
            final AnonymousID obsAID = this.anonymizer
                    .getAnonymousID(obsUser.getUID(), obsUser.getKey());
            // Time and location of the observation
            final Instant obsTime = Instant.now();
            final Location obsLoc = new Location(0, 0);
            // Create the observation
            final Observation obs = new Observation(observer.getUID(), obsAID,
                    obsTime, obsLoc);
            observations.add(obs);
            observedUsers.add(obsUser);
        }

        System.out.printf("%d observations generated.%n", noOfObservations);

        final long decryptionBeginTime = System.currentTimeMillis();

        // Let's decrypt!
        for (final Observation obs : observations) {
            obs.resolveAID(users);
        }

        System.out.println("Decryption done.");

        final long decryptionEndTime = System.currentTimeMillis();

        // Check that the resolved UIDs have been resolved correctly
        boolean doDecryptedUIDsMatch = true;
        for (int i = 0; i < noOfObservations; i++) {
            doDecryptedUIDsMatch &= observedUsers.get(i).getUID()
                    .equals(observations.get(i).getResolvedUID());
        }
        System.out.println(doDecryptedUIDsMatch ? "Encryption OK"
                : "ERROR: Encrypted UIDs don't match.");

        return (decryptionEndTime - decryptionBeginTime);
    }

    /**
     * Tests the relationship between the number of users and the time needed to
     * decrypt a fixed number of observations.
     * 
     * @param start The minimum number of users to test.
     * @param end The maximum number of users to test.
     * @param step The step size by which the number of users will be increased.
     * @param path Path where the figure should be saved.
     */
    public void testUserNumberImpact(final int start, final int end,
            final int step, final String path) {
        final Histogram<Integer> computingTime = new Histogram<>();
        for (int userCount = start; userCount <= end; userCount += step) {
            this.server.clearDatabase();
            final long decryptionTime = this.testRawAIDDecryption(userCount,
                    1_000);
            computingTime.put(userCount, decryptionTime);
        }
        computingTime.saveAsTeXandPDF("Number of users", "Time (ms)", false,
                true, path);
    }

    /**
     * Tests the relationship between the number of observations and the time
     * needed to decrypt a fixed number of observations.
     * 
     * @param start The minimum number of observations to test.
     * @param end The maximum number of observations to test.
     * @param step The step size by which the number of users should be
     *            increased.
     * @param path Path where the figure should be saved.
     */
    public void testObservationNumberImpact(final int start, final int end,
            final int step, final String path) {
        final Histogram<Integer> computingTime = new Histogram<>();
        for (int observationCount = start; observationCount <= end; observationCount += step) {
            this.server.clearDatabase();
            final long decryptionTime = this.testRawAIDDecryption(1_000,
                    observationCount);
            computingTime.put(observationCount, decryptionTime);
        }
        computingTime.saveAsTeXandPDF("Number of observations", "Time (ms)",
                false, true, path);
    }

    /**
     * Tests the random fluctuations in the time needed to decrypt the given
     * number of observations on a server with the given number of users. The
     * number of iterations is 100.
     * 
     * @param userNo The number of users.
     * @param observationNo The number of observations.
     * @param path The path of the output figure.
     */
    public void testTriedAIDNoFluctuations(final int userNo,
            final int observationNo, final String path) {
        final Histogram<Integer> computingTime = new Histogram<>();
        for (int i = 0; i < 100; i++) {
            this.server.clearDatabase();
            final long decryptionTime = this.testRawAIDDecryption(userNo,
                    observationNo);
            computingTime.put(i, decryptionTime);
        }
        computingTime.saveAsTeXandPDF("Iteration", "Time (ms)", false, true,
                path);
    }

    /**
     * Tests the scalability of the server and its performance with varying
     * number of threads in the parallel stream's ForkJoinPool.
     * 
     * @param path The path of the output figure.
     */
    public void testScalability(final String path) {
        final Histogram<Integer> computingTime = new Histogram<>();
        for (int threadNo = 1; threadNo <= 16; threadNo++) {
            this.server.clearDatabase();
            System.setProperty(
                    "java.util.concurrent.ForkJoinPool.common.parallelism",
                    Integer.toString(threadNo));
            final long decryptionTime = this.testRawAIDDecryption(10_000,
                    1_000);
            computingTime.put(threadNo, decryptionTime);
        }
        computingTime.saveAsTeXandPDF("Thread count", "Time (ms)", false, true,
                path);
    }

    /**
     * Runs the tests.
     * 
     * @param args Ignored.
     */
    public static void main(String[] args) {
        final PlainAIDDecryptionBenchmark benchmark = new PlainAIDDecryptionBenchmark();
        benchmark.testUserNumberImpact(100, 10_000, 100,
                Constants.PERF_RAW_DECRYPTION_PER_USER_PATH1);
        System.out.printf("%nUser number impact: DONE.%n%n");
        benchmark.testObservationNumberImpact(100, 10_000, 100,
                Constants.PERF_RAW_DECRYPTION_PER_OBS_PATH1);
        System.out.printf("%nObservation number impact: DONE.%n%n");

        benchmark.testUserNumberImpact(10_000, 50_000, 10_000,
                Constants.PERF_RAW_DECRYPTION_PER_USER_PATH2);
        System.out.printf("%nUser number impact: DONE.%n%n");
        benchmark.testObservationNumberImpact(10_000, 50_000, 10_000,
                Constants.PERF_RAW_DECRYPTION_PER_OBS_PATH2);
        System.out.printf("%nObservation number impact: DONE.%n%n");

        benchmark.testTriedAIDNoFluctuations(10_000, 1_000,
                Constants.PERF_RAW_DECRYPTION_FLUCT_PATH);
        System.out.printf(
                "%nRandom fluctuations in time with fixed user and obs count: DONE.%n%n");

        benchmark.testScalability(Constants.PERF_SCALABILITY_PATH);
        System.out.printf(
                "%nPerformance depending on the thread number: DONE.%n%n");
    }

}
