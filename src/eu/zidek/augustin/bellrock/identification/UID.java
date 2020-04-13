package eu.zidek.augustin.bellrock.identification;

/**
 * Unique ID that every client in the system has. Created by Augustin Zidek on
 * 2016-01-19.
 */
public class UID extends ID {

	/**
	 * Constructs a new unique ID object with the underlying bytes.
	 *
	 * @param idBytes The bytes of the ID.
	 * @throws IllegalArgumentException If the length of the byte array doesn't
	 *             satisfy the required length of the UID.
	 */
	public UID(byte[] idBytes) throws IllegalArgumentException {
		super(idBytes);

		if (idBytes.length != MsgConstants.UID_LENGTH) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Constructs a new unique ID object with the underlying bytes given in the
	 * form of a hexadecimal String.
	 * 
	 * @param hexString The hexadecimal representation of the UID.
	 * @throws IllegalArgumentException If the hexadecimal representation is not
	 *             a valid UID.
	 */
	public UID(final String hexString) throws IllegalArgumentException {
		super(hexString);

		if (hexString.length() != MsgConstants.UID_LENGTH * 2) {
			throw new IllegalArgumentException();
		}
	}
}