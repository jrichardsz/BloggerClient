package blogger.ui;

import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;

import blogger.BloggerAPIBuilder;

public class AutoSyncPanel extends JPanel {
	private static final long serialVersionUID = -3124121818719696806L;

	private final BloggerClientFrame frame;

	public AutoSyncPanel(BloggerClientFrame frame) {
		this.frame = frame;
		createGUI();
	}

	private void createGUI() {
		final AutoSyncPanel p = this;
		p.setLayout(new GridBagLayout());

		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				final Blogger blogger = BloggerAPIBuilder.getInstance("zz83").build();
				List<Blog> blogList = blogger.blogs().listByUser("self").execute().getItems();
				if (blogList != null) {
					for (Blog blog : blogList) {
						System.out.println(blog.toPrettyString());
					}
				}
				// TODO Auto-generated method stub
				return null;
			}
			
		}.execute();
		//TODO
	}







}
