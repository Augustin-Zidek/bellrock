package eu.zidek.augustin.bellrock.identification;

import java.util.List;
import java.util.NoSuchElementException;

import eu.zidek.augustin.bellrock.server.BellrockUser;

/**
 * Class for Anonymous ID decryption. Created by Augustin Zidek on 2016-01-25.
 */
public class IDDecryptor {

    /**
     * Decrypt the Anonymous ID without knowing the key, but rather only the map
     * between the keys and the UIDs used in the system. The nonce that was
     * encrypted together with the UID is discarded.
     *
     * @param anonymousID The Anonymous ID to be encrypted.
     * @param decryptingUsers Bellrock users that should be tried to decrypt
     *            this AID.
     * @return The user whose UID was encrypted as the Anonymous ID.
     */
    public static BellrockUser decryptAnonymousID(final AnonymousID anonymousID,
            final List<BellrockUser> decryptingUsers) {
        // Try to decrypt the AID using all user keys in the system
        userLoop: for (final BellrockUser user : decryptingUsers) {
            final byte[] userUID = user.getUID().getBytes();
            try {
                // Decrypt the AID using this user's key
                final byte[] decryptedUID = user.decryptAID(anonymousID);
                // If the decrypted UID matches this user's UID, we found it!
                for (int i = 0; i < MsgConstants.UID_LENGTH; i++) {
                    if (decryptedUID[i] != userUID[i]) {
                        continue userLoop;
                    }
                }
                return user;
            }
            // In case there is a Security Exception, skip this particular key
            catch (final SecurityException e) {
                continue userLoop;
            }
        }
        // No UID matches the given Anonymous ID and the key to UID map
        return null;
    }

    /**
     * Decrypt the Anonymous ID without knowing the key, but rather only the map
     * between the keys and the UIDs used in the system. The nonce that was
     * encrypted together with the UID is discarded. This method uses parallel
     * stream to speed up the decryption. If there is sufficient memory
     * available to the system this allows performance improvement.
     *
     * @param anonymousID The Anonymous ID to be encrypted.
     * @param decryptingUsers Bellrock users that should be tried to decrypt
     *            this AID.
     * @return The user whose UID was encrypted as the Anonymous ID.
     */
    public static BellrockUser decryptAnonymousIDParallel(
            final AnonymousID anonymousID,
            final List<BellrockUser> decryptingUsers) {
        try {
            // Use each of the key to decrypt the Anonymous ID
            return decryptingUsers.parallelStream().filter(user -> {
                final byte[] userUID = user.getUID().getBytes();
                try {
                    // Decrypt the AID using this user's key
                    final byte[] decryptedUID = user.decryptAID(anonymousID);
                    // If the decrypted UID matches this user's UID, found it!
                    for (int i = 0; i < MsgConstants.UID_LENGTH; i++) {
                        if (decryptedUID[i] != userUID[i]) {
                            return false;
                        }
                    }
                    return true;
                }
                // If there is a Security Exception, skip this particular key
                catch (final SecurityException e) {
                    return false;
                }
            }).findAny().get();
        }
        catch (final NoSuchElementException e) {
            return null;
        }
    }
}
