package org.rhq.modules.plugins.xmonitor.test;

import java.io.IOException;

import org.junit.Test;
import org.rhq.modules.plugins.xmonitor.util.JMXServerFacade;
import org.testng.Assert;

public class JMXServerFacadeTest {

	@Test
	public void testConnection(){
		try {
			JMXServerFacade.prepareConnection();
			Assert.assertTrue(JMXServerFacade.executeSample("java.lang:type=Memory","ObjectPendingFinalizationCount")==0);
			Assert.assertTrue(true);
		} catch (Exception e) {
			Assert.assertTrue(false);
			e.printStackTrace();
		}
	}
}
