package blogger.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoAES128 {
	private final byte[] keyBytes, ivBytes;

	public CryptoAES128(byte[] keyBytes, byte[] ivBytes) {
		this.keyBytes = keyBytes;
		this.ivBytes = ivBytes;
	}

	public Cipher getEncryptCipher() {
		return getCipher(Cipher.ENCRYPT_MODE);
	}

	public Cipher getDecryptCipher() {
		return getCipher(Cipher.DECRYPT_MODE);
	}

	private Cipher getCipher(int opmode) {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(opmode, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(ivBytes));
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException e) {
			throw new RuntimeException("init AES failed", e);
		}
		return cipher;
	}

}
