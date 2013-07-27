package blogger.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoAES128 {
	private final byte[] key, iv;

	/**
	 * @param key
	 *          used for {@link javax.crypto.spec.SecretKeySpec}
	 * @param iv
	 *          used for {@link javax.crypto.spec.IvParameterSpec}
	 * @throws IllegalArgumentException
	 *           if key/iv length is not 16
	 */
	public CryptoAES128(byte[] key, byte[] iv) {
		if (key.length != 16 || iv.length != 16) {
			throw new IllegalArgumentException("param bytes length must be 16");
		}
		this.key = key;
		this.iv = iv;
	}

	/**
	 * @throws InternalError
	 *           if initialize AES failed
	 */
	public Cipher getEncryptCipher() {
		return getCipher(Cipher.ENCRYPT_MODE);
	}

	/**
	 * @throws InternalError
	 *           if initialize AES failed
	 */
	public Cipher getDecryptCipher() {
		return getCipher(Cipher.DECRYPT_MODE);
	}

	private Cipher getCipher(int opmode) {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			throw new InternalError("init AES failed");
		}
		return cipher;
	}

}
