package blogger;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import blogger.ui.GBC;
import blogger.util.CryptoAES128;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;

/**
 * Notice:<br/>
 * It is a credential store for one user, so use a new store per user.
 */
public class EncryptedCredentialStore implements CredentialStore {

	private final File file;

	public EncryptedCredentialStore(File file) throws IOException {
		this.file = file;
		loadFromFile();
	}

	/** map<userId, credential> */
	private final Map<String, CredentialBean> credentialBeanCache = new HashMap<>();

	private static class CredentialBean {
		private String userId;
		private String accessToken;
		private String refreshToken;
		// if credential.expiresInSeconds is null, then set it to Long.MAX_VALUE
		private Long expiresInSeconds;

		void load(Credential credential) {
			credential.setAccessToken(accessToken);
			credential.setRefreshToken(refreshToken);
			credential.setExpiresInSeconds(expiresInSeconds);
		}

		void store(String userId, Credential credential) {
			if (this.userId != null) {
				if (!this.userId.equals(userId))
					throw new RuntimeException(String.format("userId doesn't match, [%s], [%s]", this.userId,
							userId));
			}
			else {
				this.userId = userId;
			}
			accessToken = credential.getAccessToken();
			refreshToken = credential.getRefreshToken();
			expiresInSeconds = credential.getExpiresInSeconds();
			if (expiresInSeconds == null) {
				expiresInSeconds = Long.MAX_VALUE;
			}
		}
	}

	@Override
	public synchronized boolean load(String userId, Credential credential) throws IOException {
		CredentialBean bean = credentialBeanCache.get(userId);
		if (bean != null) {
			bean.load(credential);
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public synchronized void store(String userId, Credential credential) throws IOException {
		CredentialBean bean = credentialBeanCache.get(userId);
		if (bean != null) {
			bean.store(userId, credential);
		}
		else {
			bean = new CredentialBean();
			bean.store(userId, credential);
			credentialBeanCache.put(userId, bean);
		}
		saveToFile();
	}

	@Override
	public synchronized void delete(String userId, Credential credential) throws IOException {
		credentialBeanCache.remove(userId);
		saveToFile();
	}

	private static final byte[] IV = "UQ ()*e-x]5|i_)d".getBytes();
	private byte[] KEY;

	private void loadFromFile() throws IOException {
		if (!file.exists()) {
			return;
		}
		String errorMessage = null;
		start: while (true) {
			if (KEY == null) {
				KEY = getKEY(errorMessage);
			}
			errorMessage = null;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				String line;
				while ((line = in.readLine()) != null) {
					final Cipher cipher = new CryptoAES128(KEY, IV).getDecryptCipher();
					final Properties props = new Properties();
					try {
						byte[] decryptedBytes = cipher.doFinal(Hex.decodeHex(line.toCharArray()));
						props.load(new ByteArrayInputStream(decryptedBytes));
					}
					catch (DecoderException e) {
						throw new RuntimeException(e.getMessage(), e);
					}
					catch (IllegalBlockSizeException | BadPaddingException e) {
						// credential password is wrong probably, retry
						e.printStackTrace();
						KEY = null;
						errorMessage = "Wrong password.";
						continue start;
					}
					CredentialBean bean = new CredentialBean();
					bean.userId = props.getProperty("userId");
					//TODO verify userId with user inputed userId?
					bean.accessToken = props.getProperty("accessToken");
					bean.refreshToken = props.getProperty("refreshToken");
					bean.expiresInSeconds = Long.parseLong(props.getProperty("expiresInSeconds"));
					credentialBeanCache.put(bean.userId, bean);
				}
			}
			break;
		}
	}

	private void saveToFile() throws IOException {
		if (KEY == null) {
			KEY = getKEY(null);
		}
		final StringBuilder strBuilder = new StringBuilder(1024);
		/*
		 * file format: {lines}
		 * lines = {line}+
		 * line = hex(aes128(props(CredentialBean[])))
		 */
		for (CredentialBean bean : credentialBeanCache.values()) {
			Properties props = new Properties();
			props.setProperty("userId", bean.userId);
			props.setProperty("accessToken", bean.accessToken);
			props.setProperty("refreshToken", bean.refreshToken);
			props.setProperty("expiresInSeconds", bean.expiresInSeconds + "");
			ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
			props.store(baos, "");
			final Cipher cipher = new CryptoAES128(KEY, IV).getEncryptCipher();
			try {
				byte[] encryptedBytes = cipher.doFinal(baos.toByteArray());
				strBuilder.append(Hex.encodeHexString(encryptedBytes)).append('\n');
			}
			catch (IllegalBlockSizeException | BadPaddingException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		try (Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
			out.write(new String(strBuilder));
		}
	}

	private byte[] getKEY(String errorMessage) {
		while (true) {
			final boolean keyExists = file.exists();
			final JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			final int distance = 5;
			panel.add(new JLabel(keyExists ? "Input password for local credential store."
					: "Create password for local credential store, at least 12 any characters.")
			, new GBC(0, 0, 2, 1).setFill(GBC.HORIZONTAL)
					.setInsets(distance));
			panel.add(new JLabel("Password:"), new GBC(0, 1, 1, 1).setInsets(distance));
			final JPasswordField pwdField = new JPasswordField();
			panel.add(pwdField, new GBC(1, 1, 1, 1).setFill(GBC.HORIZONTAL).setInsets(distance));
			JPasswordField pwdRepeatField = null;
			if (!keyExists) {
				panel.add(new JLabel("Repeat Password:"), new GBC(0, 2, 1, 1).setInsets(distance));
				pwdRepeatField = new JPasswordField();
				panel.add(pwdRepeatField, new GBC(1, 2, 1, 1).setFill(GBC.HORIZONTAL).setInsets(distance));
			}
			if (errorMessage != null) {
				JLabel label = new JLabel(errorMessage);
				label.setForeground(Color.RED);
				panel.add(label, new GBC(0, 3, 2, 2).setFill(GBC.HORIZONTAL).setInsets(distance));
			}
			final Object[] options = new Object[] { "   OK   ", " Cancel " };
			final int selection = JOptionPane.showOptionDialog(null, panel, "Credential Store Password",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
			if (selection == JOptionPane.YES_OPTION) {
				if (!keyExists) {
					char[] pwd = pwdField.getPassword(), pwdRepeat = pwdRepeatField.getPassword();
					if (!Arrays.equals(pwd, pwdRepeat)) {
						errorMessage = "Two passwords are not matched.";
						continue;
					}
					else {
						if (pwd.length < 12) {
							errorMessage = "Password must include at least 12 any characters.";
							continue;
						}
						else {
							return getMD5(pwd);
						}
					}
				}
				else {
					return getMD5(pwdField.getPassword());
				}
			}
			else {
				errorMessage = "Must input password.";
				continue;
			}
		}
	}

	/**
	 * @return MD5 byte array
	 */
	private byte[] getMD5(char[] intputChars) {
		try {
			return MessageDigest.getInstance("MD5").digest(new String(intputChars).getBytes());
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 algorithm doesn't exist", e);
		}
	}

}
