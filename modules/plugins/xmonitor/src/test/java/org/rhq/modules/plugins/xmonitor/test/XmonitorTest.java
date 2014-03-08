package org.rhq.modules.plugins.xmonitor.test;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Test(groups = "tomcat.plugin")
public class XmonitorTest {

	private Log log = LogFactory.getLog(this.getClass());
    private static final String PLUGIN_NAME = "XMonitor";
    
	@BeforeSuite
    public void start() {
        try {
            File pluginDir = new File("target/itest/plugins");
            PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
            pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
            pcConfig.setPluginDirectory(pluginDir);

            pcConfig.setInsideAgent(false);
            PluginContainer.getInstance().setConfiguration(pcConfig);
            PluginContainer.getInstance().initialize();
            log.info("PC started.");
            for (String plugin : PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames()) {
                log.info("...Loaded plugin: " + plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	@Test
    public void testPluginLoad() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
        assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
        assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
    }
	@Test(dependsOnMethods = "testPluginLoad")
    public void testDiscovery() throws Exception {
        InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
        assert report != null;
        log.info("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");
        Set<Resource> resources = findResource(PluginContainer.getInstance().getInventoryManager().getPlatform(),
            "Tomcat");
        log.info("Found " + resources.size() + " ews / apache tomcat instance(s).");
    }
	private Set<Resource> findResource(Resource parent, String typeName) {
        Set<Resource> foundResources = new HashSet<Resource>();

        Queue<Resource> discoveryQueue = new LinkedList<Resource>();
        discoveryQueue.add(parent);

        while (!discoveryQueue.isEmpty()) {
            Resource currentResource = discoveryQueue.poll();

            log.info("Discovered resource of type: " + currentResource.getResourceType().getName());
            if (currentResource.getResourceType().getName().equals(typeName)) {
                foundResources.add(currentResource);
            }

            for (Resource child : currentResource.getChildResources()) {
                discoveryQueue.add(child);
            }
        }

        return foundResources;
    }
	
	@AfterSuite
    public void stop() {
        PluginContainer.getInstance().shutdown();
    }
}
