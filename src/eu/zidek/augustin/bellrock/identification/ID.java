package eu.zidek.augustin.bellrock.identification;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

/**
 * Abstract ID class. Created by Augustin Zidek on 2016-01-19.
 */
public abstract class ID {
	private final byte[] idBytes;

	/**
	 * Constructs a new ID object with the underlying bytes.
	 *
	 * @param idBytes The bytes of the ID.
	 */
	protected ID(final byte[] idBytes) {
		this.idBytes = idBytes;
	}

	protected ID(final String hexString) throws IllegalArgumentException {
		this.idBytes = this.fromHexString(hexString);
	}

	private byte[] fromHexString(final String s)
			throws IllegalArgumentException {
		return DatatypeConverter.parseHexBinary(s);
	}

	/**
	 * @return The bytes of the ID.
	 */
	public byte[] getBytes() {
		return this.idBytes;
	}

	/**
	 * @return The length of the ID in bytes.
	 */
	public int length() {
		return this.idBytes.length;
	}

	/**
	 * @return The representation of the ID in hexadecimal format.
	 */
	public String toHexString() {
		final char[] hexArray = new char[] { '0', '1', '2', '3', '4', '5', '6',
				'7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

		final char[] hexChars = new char[this.idBytes.length * 2];
		for (int byteIndex = 0; byteIndex < this.idBytes.length; byteIndex++) {
			final int byteBits = this.idBytes[byteIndex] & 0xFF;
			hexChars[byteIndex * 2] = hexArray[byteBits >>> 4];
			hexChars[byteIndex * 2 + 1] = hexArray[byteBits & 0x0F];
		}
		return new String(hexChars);
	}

	@Override
	public boolean equals(final Object otherID) {
		if (!(otherID instanceof ID)) {
			return false;
		}
		final byte[] otherIDBytes = ((ID) otherID).getBytes();
		// Check if lengths match
		if (this.idBytes.length != otherIDBytes.length) {
			return false;
		}
		// Check if individual bytes match
		for (int i = 0; i < this.idBytes.length; i++) {
			if (this.idBytes[i] != otherIDBytes[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.idBytes);
	}

}
