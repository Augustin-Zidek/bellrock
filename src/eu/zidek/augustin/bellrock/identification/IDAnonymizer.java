package eu.zidek.augustin.bellrock.identification;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Class for turning UIDs into Anonymous IDs (AIDs).
 * Created by Augustin Zidek on 2016-01-18.
 */
public class IDAnonymizer {

    /**
     * @param byteCount Number of random bytes.
     * @return Array with the given number of random bytes.
     */
    private byte[] getRandomByteArray(final int byteCount) {
        final SecureRandom random = new SecureRandom();
        final byte[] randomBytes = new byte[byteCount];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    private byte[] padID(final byte[] id) {
        final byte[] idWithPad = new byte[id.length + MsgConstants.NONCE_LENGTH];
        final byte[] pad = getRandomByteArray(MsgConstants.NONCE_LENGTH);

        // Copy original ID
        for (int i = 0; i < id.length; i++) {
            idWithPad[i] = id[i];
        }

        // Copy the padding
        for (int i = id.length; i < idWithPad.length; i++) {
            idWithPad[i] = pad[i - id.length];
        }

        return idWithPad;
    }

    /**
     * Anonymizes the given ID by appending a random 8 byte nonce and encrypting the whole thing.
     *
     * @param uid The ID to be anonymized.
     * @param key The key to initialize the Cipher with.
     * @return An anonymous ID. The ID is anonymized by appending a random
     * 8 byte nonce to it and then encrypting in using a AES block cipher.
     * @throws SecurityException 
     */
    public AnonymousID getAnonymousID(final UID uid, final Key key) throws SecurityException {
        try {
            // Initialize the cipher: AES in ECB (Electronic Code Book) mode, since we are
            // encrypting only a single block here. Hence, use also no padding.
            final Cipher cipher = Cipher.getInstance(MsgConstants.CIPHER_PARAMETERS);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Pad the ID with a nonce
            final byte[] idWithPad = this.padID(uid.getBytes());

            // Encrypt the ID+pad, i.e. return a random ID from observer's point of view
            final byte[] anonymousID = cipher.doFinal(idWithPad);
            return new AnonymousID(anonymousID);

        }
        // Catch the problems with the AES initialisation and throw as a SecurityException
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new SecurityException("The AES could not be initiated: " + e.getMessage());
        }
        // Catch the problems with the encryption and throw as a SecurityException
        catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new SecurityException("There was an error encrypting the ID: " + e.getMessage());
        }

    }
}
