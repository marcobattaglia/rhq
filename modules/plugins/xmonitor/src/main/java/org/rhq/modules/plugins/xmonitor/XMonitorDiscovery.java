package org.rhq.modules.plugins.xmonitor;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.ConnectionFactory;
import org.mc4j.ems.connection.EmsConnection;



import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.settings.ConnectionSettings;
import org.mc4j.ems.connection.support.ConnectionProvider;
import org.mc4j.ems.connection.support.metadata.JSR160ConnectionTypeDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.BaseServerComponent;
import org.rhq.modules.plugins.jbossas7.ManagedASComponent;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;

import javax.management.ObjectName;

/**
 * Discovery class
 */
@SuppressWarnings("unused")
public class XMonitorDiscovery implements ResourceDiscoveryComponent<StandaloneASComponent<?>>

{

    private static final Log log = LogFactory.getLog(XMonitorDiscovery.class);
    private final ManagerConfig mc = new ManagerConfig();
    public static Configuration pluginConf;
    
    private static final String HOSTNAME = "hostname";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CLIENT_JAR_LOCATION = "clientJarLocation";

    private static final int STANDALONE_REMOTING_PORT_OFFSET = 9;
    private static final int DOMAIN_REMOTING_PORT_DEFAULT = 4447;
    private static final String MANAGED_SERVER_PORT_OFFSET_PROPERTY_NAME = "socket-binding-port-offset";

    /**
     * Run the auto-discovery
     */
    
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<StandaloneASComponent<?>>  discoveryContext) throws Exception {
       log.info("discoverResources is called");
       
       Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
       Collection<String> parentProperties = discoveryContext.getParentResourceContext().getPluginConfiguration().getNames();
       for(String prop:parentProperties){
    	   try{
    	   log.info("Parent property name: " + prop+"="+discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue(prop));
    	   }
    	   catch(Exception ex){}
       }
       mc.configMe(discoveryContext.getDefaultPluginConfiguration());
       List<EmsBean> onResources =   mc.discover(this.getJMXConnection(discoveryContext));
       for(EmsBean on : onResources){
    	   DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                   discoveryContext.getResourceType(), // ResourceType
                   on.getBeanName().getCanonicalName(),
                   on.getBeanName().getCanonicalName(),
                   "",
                   "XMonitor on " + on.getBeanName().getCanonicalName(),
                   discoveryContext.getDefaultPluginConfiguration(),
                   null
               );
    	   discoveredResources.add(detail);
//    	   Configuration resConf = detail.getPluginConfiguration();
//    	   resConf.setSimpleValue("discoveryFilter", mc.getDiscoveryFilter());
//    	   resConf.setSimpleValue("bindingFilter", mc.getBindingFilter());
//    	   resConf.setSimpleValue("keyProperty", mc.getKeyProperty());
//    	   resConf.setSimpleValue("mapCode", mc.getMapCode());
//    	   resConf.setSimpleValue("reduceCode", mc.getReduceCode());
//    	   resConf.setSimpleValue("frequency", ""+mc.getFrequency());
       }
        //  only discover if the home path contains /var/lib/openshift
         return discoveredResources;

        }
    
    public EmsConnection getJMXConnection(ResourceDiscoveryContext<StandaloneASComponent<?>>  context) throws Exception{
    	
    	BaseComponent<?> parentComponent = context.getParentResourceComponent();
        BaseServerComponent baseServerComponent = parentComponent.getServerComponent();
        ServerPluginConfiguration serverPluginConfiguration = baseServerComponent.getServerPluginConfiguration();

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        pluginConfig.setSimpleValue(HOSTNAME, serverPluginConfiguration.getHostname());

        int port;
        String username, password;
        if (parentComponent instanceof ManagedASComponent) {
            ManagedASComponent managedASComponent = (ManagedASComponent) parentComponent;
            Configuration managedASConfig = managedASComponent.loadResourceConfiguration();
            PropertySimple offsetProp = managedASConfig.getSimple(MANAGED_SERVER_PORT_OFFSET_PROPERTY_NAME);
            if (offsetProp == null) {
                log.warn("Could not find Managed Server socket binding offset, skipping discovery");
                return null;
            }
            port = offsetProp.getIntegerValue() + DOMAIN_REMOTING_PORT_DEFAULT;
            String[] credentials = getCredentialsForManagedAS();
            username = credentials[0];
            password = credentials[1];
        } else if (parentComponent instanceof StandaloneASComponent) {
            port = serverPluginConfiguration.getPort() + STANDALONE_REMOTING_PORT_OFFSET;
            username = serverPluginConfiguration.getUser();
            password = serverPluginConfiguration.getPassword();
        } else {
            log.warn(parentComponent + " is not a supported parent component");
            return null;
        }
        pluginConfig.setSimpleValue(PORT, String.valueOf(port));
        pluginConfig.setSimpleValue(USERNAME, username);
        pluginConfig.setSimpleValue(PASSWORD, password);

        File clientJarFile = new File(serverPluginConfiguration.getHomeDir(), "bin" + File.separator + "client"
            + File.separator + "jboss-client.jar");
        if (!clientJarFile.isFile()) {
            log.warn(clientJarFile + " does not exist.");
            return null;
        }
        pluginConfig.setSimpleValue(CLIENT_JAR_LOCATION, clientJarFile.getAbsolutePath());

        EmsConnection emsConnection = null;
        try {
            emsConnection = loadEmsConnection(pluginConfig);
           
            if (emsConnection == null) {
                // An error occured while creating the connection
                return null;
            }
            else{
            	return emsConnection;
            }

            
        } catch(Exception ex) {
            return null;
        }
    }
    protected String[] getCredentialsForManagedAS() {
        return new String[] { "admin", "pippo" };
    }
    public static EmsConnection loadEmsConnection(Configuration pluginConfig) {
        EmsConnection emsConnection = null;
        try {
            File clientJarFile = new File(pluginConfig.getSimpleValue(CLIENT_JAR_LOCATION));

            ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.initializeConnectionType(new A7ConnectionTypeDescriptor(clientJarFile));
            connectionSettings.setLibraryURI(clientJarFile.getParent());
            connectionSettings.setServerUrl( //
                "service:jmx:remoting-jmx://" //
                    + pluginConfig.getSimpleValue(HOSTNAME) //
                    + ":" //
                    + pluginConfig.getSimpleValue(PORT));
            connectionSettings.setPrincipal(pluginConfig.getSimpleValue(USERNAME));
            connectionSettings.setCredentials(pluginConfig.getSimpleValue(PASSWORD));

            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.discoverServerClasses(connectionSettings);
            ConnectionProvider connectionProvider = connectionFactory.getConnectionProvider(connectionSettings);

            return connectionProvider.connect();

        } catch (Exception e) {
           
                log.debug("Could not create EmsConnection", e);
            
            if (emsConnection != null) {
                emsConnection.close();
            }
            return null;
        }
    }
    private static class A7ConnectionTypeDescriptor extends JSR160ConnectionTypeDescriptor {
        private final File clientJarFile;

        public A7ConnectionTypeDescriptor(File clientJarFile) {
            this.clientJarFile = clientJarFile;
        }

        @Override
        public String[] getConnectionClasspathEntries() {
            return new String[] { clientJarFile.getName() };
        }
    }

}