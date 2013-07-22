package blogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;

import blogger.util.UniqueKeyProperties;

public class Configuration {

	private final UniqueKeyProperties props = new UniqueKeyProperties();

	public Configuration(final File cfgFile, final URI defaultCfgFileUri) throws IOException {
		super();
		if (!cfgFile.exists()) {
			// generate configuration file
			try (FileChannel in = new FileInputStream(new File(defaultCfgFileUri)).getChannel();
					FileChannel out = new FileOutputStream(cfgFile).getChannel()) {
				in.transferTo(0, Integer.MAX_VALUE, out);
			}
		}
		else if (cfgFile.isDirectory()) {
			throw new RuntimeException(String.format(
					"Error: %s is used as configuration file, but it is a directory.", cfgFile));
		}
		try (InputStream inStream = new FileInputStream(cfgFile)) {
			props.load(inStream);
		}
	}

	/*
	proxy.enabled=false
	proxy.type=
	proxy.host=
	proxy.port=
	*/
	public ProxySelector getProxySelector() {
		if (Boolean.parseBoolean(props.getProperty("proxy.enabled"))) {
			Proxy.Type type = "socks".equalsIgnoreCase(props.getProperty("proxy.type")) ? Proxy.Type.SOCKS
					: Proxy.Type.HTTP;
			SocketAddress sa = new InetSocketAddress(props.getProperty("proxy.host"),
					Short.parseShort(props.getProperty("proxy.port")));
			return new ProxySelectorImpl(new Proxy(type, sa));
		}
		else {
			return ProxySelector.getDefault();
		}
	}

}
