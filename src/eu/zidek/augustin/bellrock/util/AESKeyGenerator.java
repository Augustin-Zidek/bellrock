package eu.zidek.augustin.bellrock.util;

import java.util.Base64;

import javax.xml.bind.DatatypeConverter;

import eu.zidek.augustin.bellrock.server.RandomKeyGenerator;

class AESKeyGenerator {

	public static void main(String[] args) {
		final byte[] keyBytes = RandomKeyGenerator.getKey(128)
				.getEncoded();
		System.out.println(Base64.getEncoder().encodeToString(keyBytes));
		System.out.println(DatatypeConverter.printHexBinary(keyBytes));
	}

}
