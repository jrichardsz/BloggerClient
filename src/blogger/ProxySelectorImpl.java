package blogger;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * References:
 * http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html
 * http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e571
 * http://hc.apache.org/httpcomponents-client-ga/examples.html
 * com.google.api.client.http.apache.ApacheHttpTransport.newDefaultHttpClient
 */
public final class ProxySelectorImpl extends ProxySelector {
	private final List<Proxy> proxyList;

	public ProxySelectorImpl(Proxy... proxies) {
		if (proxies.length == 0) {
			throw new RuntimeException("at least one proxy");
		}
		List<Proxy> list = new ArrayList<>();
		for (Proxy proxy : proxies) {
			list.add(proxy);
		}
		proxyList = Collections.unmodifiableList(list);
	}

	@Override
	public List<Proxy> select(URI uri) {
		return proxyList;
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		System.out.println(String.format("connectFailed(%s, %s, %s)", uri, sa, ioe));
	}

}
