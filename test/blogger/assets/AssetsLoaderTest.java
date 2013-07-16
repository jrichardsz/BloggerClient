package blogger.assets;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

public class AssetsLoaderTest {

	@Test
	public void readAssetAsTextTest() throws IOException, URISyntaxException {
		String name = "js/showhide.js";
		System.out.println(AssetsLoader.readAssetAsText(name));
	}

}
