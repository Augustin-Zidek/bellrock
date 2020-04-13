package eu.zidek.augustin.bellrock.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import eu.zidek.augustin.bellrock.identification.UID;

/**
 * Key Store for Bellrock client-server secret keys which are used to encrypt
 * UIDs into an Anonymous IDs.
 * 
 * @author Augustin Zidek
 *
 */
public class BellrockFileKeyStore {
	private static BellrockFileKeyStore instance = null;
	private final KeyStore keyStore;
	private final File ksFile = new File(ServerConsts.KEY_STORE_PATH);

	private BellrockFileKeyStore() throws KeyStoreException, IOException {
		this.keyStore = this.createKeyStore();
	}

	/**
	 * @return An instance of the Bellrock Key Store.
	 * @throws KeyStoreException If there was an error initialising underlying
	 *             the Key Store.
	 * @throws IOException If there was an error reading the file storing the
	 *             underlying Key Store.
	 * @throws FileNotFoundException If the file storing the underlying Key
	 *             Store doesn't exist.
	 * @throws CertificateException If there was an error initialising
	 *             underlying the Key Store.
	 * @throws NoSuchAlgorithmException If there was an error initialising the
	 *             underlying Key Store.
	 */
	public static BellrockFileKeyStore getInstance() throws KeyStoreException,
			IOException {
		if (BellrockFileKeyStore.instance == null) {
			BellrockFileKeyStore.instance = new BellrockFileKeyStore();
		}
		return BellrockFileKeyStore.instance;
	}

	/**
	 * Adds the given UID, key pair into the Bellrock Key Store.
	 * 
	 * @param uid The UID which the key belongs to.
	 * @param key The secret key bounded to be UID.
	 * @return <code>true</code> if the key was successfully added,
	 *         <code>false</code> otherwise.
	 */
	public boolean addSecretKey(final UID uid, final SecretKey key) {
		final KeyStore.SecretKeyEntry ksEntry = new KeyStore.SecretKeyEntry(key);
		final PasswordProtection keyPassword = new PasswordProtection(
				ServerConsts.KS_PSWD_LOCAL);
		try {
			this.keyStore.setEntry(uid.toHexString(), ksEntry, keyPassword);
			this.saveKeyStore(this.keyStore);
		}
		catch (final KeyStoreException e) {
			return false;
		}
		return true;
	}

	/**
	 * Removes the key stored under the given UID.
	 * 
	 * @param uid The UID.
	 * @return <code>true</code> if the key was successfully removed,
	 *         <code>false</code> otherwise.
	 */
	public boolean deleteSecretKey(final UID uid) {
		try {
			this.keyStore.deleteEntry(uid.toHexString());
			this.saveKeyStore(this.keyStore);
		}
		catch (final KeyStoreException e) {
			return false;
		}
		return true;
	}

	/**
	 * Retrieves the secret key from the Bellrock Key Store.
	 * 
	 * @param uid The UID corresponding to the secret key.
	 * @return The secret key if there is such for the given UID,
	 *         <code>null</code> otherwise.
	 */
	public SecretKey getSecretKey(final UID uid) {
		final PasswordProtection keyPassword = new PasswordProtection(
				ServerConsts.KS_PSWD_LOCAL);
		KeyStore.Entry entry;
		try {
			entry = this.keyStore.getEntry(uid.toHexString(), keyPassword);
			return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
		}
		catch (final NoSuchAlgorithmException | UnrecoverableEntryException
				| NullPointerException | KeyStoreException e) {
			return null;
		}
	}

	/**
	 * 
	 * @return A map of pairs of user UIDs and keys belonging to them.
	 * @throws KeyStoreException If the keystore has not been initialized.
	 * @throws NoSuchAlgorithmException If the algorithm for recovering the key
	 *             cannot be found.
	 * @throws UnrecoverableKeyException If the key cannot be recovered (e.g.,
	 *             the given password is wrong).
	 */
	public Map<UID, Key> getAllUserKeyMap() throws KeyStoreException,
			UnrecoverableKeyException, NoSuchAlgorithmException {
		final Enumeration<String> users = this.keyStore.aliases();
		final Map<UID, Key> userKeyMap = new HashMap<>();

		String userUID;
		while (users.hasMoreElements()) {
			userUID = users.nextElement();
			userKeyMap.put(new UID(userUID),
					this.keyStore.getKey(userUID, ServerConsts.KS_PSWD_LOCAL));
			userUID = users.nextElement();
		}
		return userKeyMap;
	}

	/**
	 * @param users The list of users which keys should be returned.
	 * @return A map of pairs of user UIDs and keys belonging to them.
	 * @throws KeyStoreException If the keystore has not been initialized.
	 * @throws NoSuchAlgorithmException If the algorithm for recovering the key
	 *             cannot be found.
	 * @throws UnrecoverableKeyException If the key cannot be recovered (e.g.,
	 *             the given password is wrong).
	 */
	public Map<UID, Key> getUserKeyMap(final Set<UID> users)
			throws UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException {
		final Map<UID, Key> userKeyMap = new HashMap<>(users.size());

		for (final UID uid : users) {
			userKeyMap.put(uid, this.keyStore.getKey(uid.toHexString(),
					ServerConsts.KS_PSWD_LOCAL));
		}
		return userKeyMap;
	}

	/**
	 * Removes all data from the key store.
	 * 
	 * @throws IOException If the file can't be emptied
	 */
	public void clearKeyStore() throws IOException {
		try (final BufferedWriter writer = Files.newBufferedWriter(
				Paths.get(ServerConsts.KEY_STORE_PATH),
				StandardOpenOption.TRUNCATE_EXISTING);) {
			// Removes the contents of the file
		}
	}

	private void saveKeyStore(final KeyStore keyStore) {
		try (final BufferedOutputStream bus = new BufferedOutputStream(
				new FileOutputStream(this.ksFile));) {
			keyStore.store(bus, ServerConsts.KS_PSWD_GLOBAL);
		}
		catch (final KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialise the key store, either load it from the disk or create a new
	 * one if it doesn't exist.
	 */
	private KeyStore createKeyStore() throws KeyStoreException, IOException {
		final File ksFile = new File(ServerConsts.KEY_STORE_PATH);
		final KeyStore keyStore = KeyStore.getInstance("JCEKS");

		try {
			// Keystore file exists, load it
			if (ksFile.exists() && ksFile.length() != 0) {
				try (final BufferedInputStream bis = new BufferedInputStream(
						new FileInputStream(ksFile));) {
					keyStore.load(bis, ServerConsts.KS_PSWD_GLOBAL);
				}

			}
			// Keystore file doesn't exist, create it
			else {
				keyStore.load(null, null);
				this.saveKeyStore(keyStore);
			}
		}
		catch (final NoSuchAlgorithmException | CertificateException e) {
			// Silently catch, since the algorithm is valid and no certificates
			// are stored in the store
			e.printStackTrace();
		}
		return keyStore;
	}

}
