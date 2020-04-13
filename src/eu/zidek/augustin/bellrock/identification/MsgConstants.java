package eu.zidek.augustin.bellrock.identification;

/**
 * Created by Augustin Zidek on 2016-02-23.
 */
public class MsgConstants {

    // Encryption settings
    @SuppressWarnings("javadoc")
    public static final String CIPHER_PARAMETERS = "AES/ECB/NoPadding";

    // Message settings
    @SuppressWarnings("javadoc")
    public static final int UID_LENGTH = 8;
    @SuppressWarnings("javadoc")
    public static final int AID_LENGTH = 16;
    @SuppressWarnings("javadoc")
    public static final int NONCE_LENGTH = 8;
}
