/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

/**
 * Thus test script works with a real env including at least one platform resource and a running agent on
 * said platform.
 */

var TestsEnabled = true;

var bundleName = 'Test Ant Bundle';

// note, super-user, will not test any security constraints
var subject = rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testDeployment() {
   if ( !TestsEnabled ) {
      return;
   }
   
   // delete the test bundle if it exists
   var bc = new BundleCriteria();
   bc.addFilterName( bundleName );
   var bundles = BundleManager.findBundlesByCriteria( bc );
   if ( null != bundles && bundles.size() > 0 ) {
      print( "\n Deleting existing " + bundleName + " bundle in order to test a fresh deploy...")
      BundleManager.deleteBundle( bundles.get(0).getId() );      
   }

   // create the test bundle
   var testBundle = BundleManager.createBundle( bundleName, "my fake bundle", getBundleType() );
   
   // define the recipe for bundleVersion 1.0 
   var recipe = scriptUtil.getFileString("./src/test/resources/test-ant-bundle-v1.xml"); 

   // create bundleVersion 1.0
   var testBundleVersion = BundleManager.createBundleVersion( testBundle.getId(), bundleName, "my fake bundle version", "1.0", recipe);

   // add the single bundleFile, the test war file
   var fileBytes = scriptUtil.getFileBytes("./src/test/resources/test-ant-bundle-v1.zip");
   var bundleFile = BundleManager.addBundleFileViaByteArray(testBundleVersion.getId(), "test-ant-bundle-v1.zip",
         "1.0", null, fileBytes);

   // create the config, setting the required properties from the recipe
   var config = new Configuration();
   config.put( new PropertySimple("http.port", "8080") );
   config.put( new PropertySimple("https.port", "8443") );

   // create a deployment using the above config
   var testDeployment = BundleManager.createBundleDeployment(testBundleVersion.getId(), "Test Ant Bundle Deployment", "my fakse bundle deployment", config);

   // Find a target platform
   var rc = new ResourceCriteria();
   rc.addFilterResourceTypeName("in"); // wINdows, lINux
   var winPlatforms = ResourceManager.findResourcesByCriteria(rc);
   var platformId = winPlatforms.get(0).getId();
   
   var bundleScheduleResponse = BundleManager.scheduleBundleResourceDeployment(testDeployment.getId(), platformId);
}

function getBundleType() {
 
   var types = BundleManager.getAllBundleTypes();
   for (i=0; ( i < types.size()); ++i ) {
      if ( types.get(i).getName().equals( "Ant Bundle" )) {
         return types.get(i).getId();
      }
   }
   
   print( "\n Could not find Ant bundle type - is the plugin loaded?");
}
