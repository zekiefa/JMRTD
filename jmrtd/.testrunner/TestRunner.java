import java.io.PrintWriter;
import java.util.List;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

public class TestRunner {
	public static void main(String[] args) throws Exception {
		String pkg = args.length > 0 ? args[0] : "org.jmrtd.lds";
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectPackage(pkg)).build();
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.execute(request, listener);
		TestExecutionSummary summary = listener.getSummary();
		summary.printTo(new PrintWriter(System.out));
		summary.printFailuresTo(new PrintWriter(System.out));
		System.exit(summary.getTotalFailureCount() > 0 ? 1 : 0);
	}
}
