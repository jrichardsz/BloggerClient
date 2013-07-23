package blogger;

import java.io.InputStreamReader;
import java.util.Collections;

import blogger.util.UniqueKeyHashMap;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.BloggerScopes;

/*
 * References:
 * https://developers.google.com/blogger/docs/3.0/getting_started
 * https://developers.google.com/accounts/docs/OAuth2InstalledApp
 * https://developers.google.com/blogger/docs/3.0/using
 * http://samples.google-api-java-client.googlecode.com/hg/plus-cmdline-sample/instructions.html
 */
public class BloggerAPIBuilder {

	/**
	 * Be sure to specify the name of your application. If the application name is {@code null} or
	 * blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = "zenzhong8383-BloggerClient/1.0";

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private static final UniqueKeyHashMap<String, BloggerAPIBuilder> CACHE = new UniqueKeyHashMap<>();
	private final String userId;
	private final Blogger blogger;

	private BloggerAPIBuilder(String userId) throws Exception {
		this.userId = userId;
		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		blogger = new Blogger.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredential())
				.setApplicationName(APPLICATION_NAME).build();
	}

	public static BloggerAPIBuilder getInstance(String userId) throws Exception {
		synchronized (CACHE) {
			BloggerAPIBuilder stub = CACHE.get(userId);
			if (stub == null) {
				stub = new BloggerAPIBuilder(userId);
				CACHE.put(userId, stub);
			}
			return stub;
		}
	}

	/** Authorizes the installed application to access user's protected data. */
	private Credential getCredential() throws Exception {
		// load client secrets
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
				new InputStreamReader(BloggerAPIBuilder.class.getResourceAsStream("client_secrets.json")));
		// set up file credential store
		CredentialStore credentialStore = new EncryptedCredentialStore(LocalDataManager.getInstance()
				.getCredentialStoreFile(userId));
		// set up authorization code flow
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
				JSON_FACTORY, clientSecrets, Collections.singleton(BloggerScopes.BLOGGER))
				.setCredentialStore(credentialStore).build();
		// authorize
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(userId);
	}

	public Blogger build() {
		return blogger;
	}

}
