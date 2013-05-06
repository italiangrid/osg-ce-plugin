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

package org.glite.ce.osgceplugin.format;

import java.util.Hashtable;

import org.glite.ce.monitorapij.sensor.SensorException;
import org.glite.ce.monitorapij.sensor.SensorOutputDataFormat;
import org.glite.ce.osgceplugin.format.util.CEGlueSchema;

public class LDIF extends SensorOutputDataFormat {

   public LDIF() {
      super("LDIF");

      
      String[] supportedQueryLang = new String[] {
            "RegEx", "ClassAd"
      };
      
      //Sets the supported query languages
      setSupportedQueryLang(supportedQueryLang);
   }



   public String[] apply(Hashtable parameters) throws Exception {
      if(parameters == null) {
         throw (new Exception("LDIF apply error: parameter is null"));
      }

      String glueSchema = (String) parameters.get("glueSchema");

      if(glueSchema != null) {
         String[] msg = CEGlueSchema.makeCEGlueSchemaInfo(glueSchema);

         return msg;
      } else {
         throw (new SensorException("LDIF apply error: glueSchema is null"));
      }
   }

}
