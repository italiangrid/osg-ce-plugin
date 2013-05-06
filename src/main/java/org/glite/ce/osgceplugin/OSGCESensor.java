/*
 * Copyright (c) Members of the EGEE Collaboration. 2004. 
 * See http://www.eu-egee.org/partners/ for details on the copyright
 * holders.  
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
 
/*
 *
 * Authors: Luigi Zangrando <zangrando@pd.infn.it>
 *
 */

package org.glite.ce.osgceplugin;

import java.io.*;
import java.lang.StringBuffer;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.resource.types.Property;
import org.glite.ce.monitorapij.sensor.AbstractSensor;
import org.glite.ce.monitorapij.sensor.Sensor;
import org.glite.ce.monitorapij.sensor.SensorException;
import org.glite.ce.osgceplugin.format.LDIF;
import org.glite.ce.osgceplugin.format.NewClassAd;
import org.glite.ce.osgceplugin.format.OldClassAd;
import org.glite.ce.osgceplugin.format.Raw;

public class OSGCESensor extends AbstractSensor {
   private static final long serialVersionUID = 1L;
   private Runtime runtime;
   private String scriptURI;
   private String path = ".";
   private final static Logger logger = Logger.getLogger(OSGCESensor.class.getName());

   /** Creates a new instance of CEMonitor */
   public OSGCESensor() {
      super("OSG CE Sensor", "OSG_CE");

      //The first time that the sensor is installed into the CEMon environment, it adds the following properties to the
      //configuration file "sensorConfig.xml". Is up to the administrator to change the default values of properties.
      //During the initialization session, the sensor checks the configuration file and replaces the default
      //values with the new ones (if any).
      Property[] properties = new Property[] {
            new Property("executionDelay", "60000"),
            new Property("scriptURI", "/opt/glite/etc/glite-ce-ce-plugin/glite-ce-info"),
            new Property(
                  "not_supported_attributes",
                  "GlueSiteDescription,GlueSiteLocation,GlueSiteWeb,GlueSiteSponsor,GlueSiteOtherInfo,"+
                  "GlueSiteLatitude,GlueSiteLongitude,"+
                  "GlueForeignKey,GlueChunkKey,createTimestamp,GlueCESEBindCEUniqueID,"+
                  "GlueCESEBindCEAccesspoint,GlueCESEBindGroupCEUniqueID,GlueCESEBindSEUniqueID,"+
                  "GlueCESEBindGroupSEUniqueID,entryTtl,modifyTimestamp,dn,ObjectClass"),
            new Property(
                  "multiple_values_attributes",
                  "GlueSiteSponsor,GlueSiteOtherInfo,"+
                  "GlueServiceAccessPointURL,GlueServiceAccessControlRule,GlueInformationServiceURL,"+
                  "GlueChunkKey,GlueForeignKey,GlueCEAccessControlBaseRule,GlueClusterService,GlueHostService,"+
                  "GlueHostApplicationSoftwareRunTimeEnvironment,GlueHostLocalFileSystemClient," +
                  "GlueHostRemoteFileSystemServer,GlueCESEBindGroupSEUniqueID,GlueSEHostingSL,"+
                  "GlueSEAccessProtocolSupportedSecurity,GlueSLService,GlueSLLocalFileSystemClient,GlueSAAccessControlBaseRule")
      };

      setProperty(properties);
      setEventOverwriteModeActive(true);        
      setScope(Sensor.HIGH);
   }



   /**
    * Called by the sensor holder to initialize the sensor.
    * It is always called before the first time that the execute method is called.
    */
   public void init() throws SensorException {
      super.init();
 
      // gets the uri of the script that generates the Glue schema
      Property scriptURIproperty = getProperty("scriptURI");

      if(scriptURIproperty == null) {
         throw new SensorException(SensorException.ERROR, "scriptURI not defined");
      }

      scriptURI = scriptURIproperty.getValue();

      if(scriptURI != null) {
         int index = scriptURI.lastIndexOf("/");

         if(index != -1) {
            path = scriptURI.substring(0, index);
         }
      }


      // gets the list of attributes which may have multiple values from the "multiple_values_attributes" property  
      ArrayList multipleValuesAttributeList = new ArrayList();

      Property property = getProperty("multiple_values_attributes");
      if(property != null) {
         String prop_value = property.getValue();

         if(prop_value != null) {
            String[] attributes = prop_value.split(",");
            
            for (int i = 0; i < attributes.length; i++) {
               multipleValuesAttributeList.add(attributes[i].toLowerCase());
            }
         }
      }


      // gets the list of not supported attributes from the "not_supported_attributes" property  
      ArrayList notSupportedAttributeList = new ArrayList();

      property = getProperty("not_supported_attributes");
      if(property != null) {
         String prop_value = property.getValue();

         if(prop_value != null) {
            String[] attributes = prop_value.split(",");
            
            for (int i = 0; i < attributes.length; i++) {
               notSupportedAttributeList.add(attributes[i].toLowerCase());
            }
         }
      }

      // makes an instance of the outputFormats supported by the sensor      
      OldClassAd defaultFormat = new OldClassAd(multipleValuesAttributeList, notSupportedAttributeList);

      addFormat(defaultFormat);
      addFormat(new NewClassAd(multipleValuesAttributeList, notSupportedAttributeList));
      addFormat(new LDIF());
      addFormat(new Raw());

      // sets the default outputFormat ("OSGClassAd_1_1")
      setDefaultFormat(defaultFormat);

      // gets the runtime
      runtime = Runtime.getRuntime();
   }



   /**
    * The execute method implements the sensor's "action". In particular the action concerns the
    * execution of the script that generates the Glue schema. It is called by the sensor holder
    * periodically, and the period is defined by the executionDelay property (expressed in milliseconds).
    */
   public void execute() throws SensorException {
      try {
         String GLUESchema = "", inputLine;
         
         logger.info("Executing the sensor OSG_CE...");
         if(scriptURI == null) {
            throw new SensorException(SensorException.ERROR, "scriptURI not found!");
         }

         Process p = runtime.exec(scriptURI, null, new File(path));

         InputStreamReader isr = new InputStreamReader(p.getInputStream());
         BufferedReader in = new BufferedReader(isr);
         
         char[] buffer = new char[10000];
         int n = 1;

         while(n > 0) {
            n = in.read(buffer, 0, buffer.length);

            if(n > 0) {
                GLUESchema += new String(buffer, 0, n);
            }
         }
         in.close();
         
         if(GLUESchema.equals("")) {
            String error = "";
            isr = new InputStreamReader(p.getErrorStream());
            in = new BufferedReader(isr);

            while((inputLine = in.readLine()) != null) {
               error += inputLine + "\n";
            }

            in.close();

            if(!error.equals("")) {
               throw (new Exception(error));
            }
         } else {
            fireSensorEvent(new OSGCESensorEvent(this, System.currentTimeMillis(), GLUESchema));
         }
      } catch (IOException ioe) {
         logger.info("OSGCESensor ERROR: " + ioe.toString());
         throw new SensorException(SensorException.ERROR, ioe.getMessage());
      } catch (Exception e) {
         logger.info("OSGCESensor ERROR: " + e.toString());
         throw new SensorException(SensorException.ERROR, e.getMessage());
      }
      logger.info("Executing the sensor OSG_CE... done!");
   }

}
