package blogger;

import java.io.*;

public class StartupScriptGenerator {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage:");
			System.out.println("java blogger.StartupScriptGenerator /path/to/project/");
			System.exit(1);
		}
		String projectPath = args[0];
		generate(new File(projectPath, "target/lib"), new File(projectPath, "build/bin"), false);
		generate(new File(projectPath, "target/lib"), new File(projectPath, "build/bin"), true);
	}

	private static void generate(File libDir, File binDir, boolean forWindows) throws Exception {
		File[] files = libDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return !pathname.isHidden() && pathname.isFile();
			}
		});
		File scriptFile = new File(binDir, (forWindows ? "startup.bat" : "startup.sh"));
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
				scriptFile), "UTF-8"))) {
			writer.write(forWindows ? "set bchome=%~dp0.."
					: "bchome=$(cd -P -- \"$(dirname -- \"$0\")\" && pwd -P)/..");
			writer.newLine();
			writer.write(forWindows ? "echo bchome=%bchome%" : "echo bchome=$bchome");
			writer.newLine();
			writer.write("java -Xms64m -Xmx256m -cp ");
			for (int i = 0, len = files.length; i < len; i++) {
				writer.write(forWindows ? "%bchome%/lib/" : "$bchome/lib/");
				writer.write(files[i].getName());
				if (i != len - 1)
					writer.write(forWindows ? ";" : ":");
			}
			writer.write(" blogger.BloggerClient");
			writer.newLine();
		}
	}

}
