package eu.zidek.augustin.bellrock.identification;

/**
 * Anonymous ID containing 20 bytes. Created by Augustin Zidek on 2016-01-19.
 */
public class AnonymousID extends ID {

	/**
	 * Constructs a new Anonymous ID object with the underlying bytes. The
	 * length of the idBytes must be exactly 20 bytes.
	 *
	 * @param idBytes The bytes of the ID. Must be exactly 20 bytes.
	 * @throws IllegalArgumentException If the length of the byte array doesn't
	 *             satisfy the required length of the UID.
	 */
	public AnonymousID(final byte[] idBytes) throws IllegalArgumentException {
		super(idBytes);

		if (idBytes.length != MsgConstants.AID_LENGTH) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Constructs a new Anonymous ID object with the underlying bytes given in
	 * the form of a hexadecimal String.
	 * 
	 * @param hexString The hexadecimal representation of the Anonymous ID.
	 * @throws IllegalArgumentException If the hexadecimal representation is not
	 *             a valid Anonymous ID.
	 */
	public AnonymousID(final String hexString) throws IllegalArgumentException {
		super(hexString);

		if (hexString.length() != MsgConstants.AID_LENGTH * 2) {
			throw new IllegalArgumentException();
		}
	}
}
