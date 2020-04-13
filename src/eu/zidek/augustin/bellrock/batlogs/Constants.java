package eu.zidek.augustin.bellrock.batlogs;

/**
 * Constants for the simulation.
 * 
 * @author Augustin Zidek
 *
 */
public class Constants {

    static final String STATS_PATH = System.getProperty("java.io.tmpdir");

    // Server performance tests
    /**
     * Figure showing the user count to time to decrypt AID relationship.
     **/
    public static final String PERF_RAW_DECRYPTION_PER_USER_PATH1 = STATS_PATH
            + "AID-decryption-per_user1.tex";
    /**
     * Figure showing the number of AIDs to time to decrypt AID relationship.
     **/
    public static final String PERF_RAW_DECRYPTION_PER_OBS_PATH1 = STATS_PATH
            + "AID-decryption-per_observation1.tex";
    /**
     * Figure showing the user count to time to decrypt AID relationship.
     **/
    public static final String PERF_RAW_DECRYPTION_PER_USER_PATH2 = STATS_PATH
            + "AID-decryption-per_user2.tex";
    /**
     * Figure showing the number of AIDs to time to decrypt AID relationship.
     **/
    public static final String PERF_RAW_DECRYPTION_PER_OBS_PATH2 = STATS_PATH
            + "AID-decryption-per_observation2.tex";
    /**
     * Figure showing the random fluctuations in decryption times with fixed
     * user number and observations number.
     */
    public static final String PERF_RAW_DECRYPTION_FLUCT_PATH = STATS_PATH
            + "AID-decryption-fluctuation.tex";
    /**
     * Figure showing the performance to thread number relationship.
     */
    public static final String PERF_SCALABILITY_PATH = STATS_PATH
            + "AID-decryption-scalability.tex";
}
