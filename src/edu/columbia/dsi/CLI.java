/**
 * 
 */
package edu.columbia.dsi;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * @author badrashiny Apr 10, 2019
 */
public class CLI {

	/**
	 * Construct and provide GNU-compatible Options.
	 *
	 * @return Options expected from command-line of GNU form.
	 */
	protected static Options constructGnuOptions() {
		final Options gnuOptions = new Options();
		gnuOptions.addOption("i", true, "input configuration file path").addOption("w", true, "work directory path")
				.addOption("o", true, "output directory path").addOption("m", true, "input matcher list path") // for the tuning pipeline
				.addOption("l", true, "maximum number of matchers limit") // for the tuning pipeline
				.addOption("c", true, "matcher store path") // optional value to be used by the query pipeline for caching
				.addOption("r", false, "matcher override flag") // optional value to be used by the query pipeline for caching
				.addOption("t", false, "test mode flag");

		return gnuOptions;
	}

	/**
	 * Print usage information to provided OutputStream.
	 *
	 */
	protected static void printUsage() {
		final HelpFormatter usageFormatter = new HelpFormatter();
		usageFormatter.printHelp("java -jar scripts-release-yyyymmdd-0.1.jar",
				"[-i <arg> -w <arg> \nOR\n -i <arg> -w <arg> -o <arg> [-c <arg> (optional)] [-r (optional)] \nOR\n -i <arg> -w <arg> -o <arg> -m <arg> -l <arg> OR\n -t",
				constructGnuOptions(), "");
	}
}
