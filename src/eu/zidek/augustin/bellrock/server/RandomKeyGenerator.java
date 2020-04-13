package eu.zidek.augustin.bellrock.server;

import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Class for generating random AES keys. This class is just a wrapper on top of
 * Java Security API.
 * 
 * @author Augustin Zidek
 *
 */
public class RandomKeyGenerator {

    /**
     * @param keyBits The number of bits of the key. Usually 128 bits are used,
     *            using more (192 or 256) requires Java Cryptography Extension
     *            (JCE) unlimited strength jurisdiction policy files.
     * @return A new random AES secret key.
     * @throws InvalidParameterException If the length of the key is not valid.
     */
    public static SecretKey getKey(final int keyBits)
            throws InvalidParameterException {
        KeyGenerator kgen = null;
        try {
            kgen = KeyGenerator.getInstance("AES");
        }
        catch (final NoSuchAlgorithmException e) {
            // The algorithm is valid so this should not be an issue.
        }
        kgen.init(keyBits);
        return kgen.generateKey();
    }

}
