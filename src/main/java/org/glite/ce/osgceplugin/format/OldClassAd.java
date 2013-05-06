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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The OSGClassAd_1_1 class implements the SensorOutputDataFormat relative to the ClassAd version 1.1 (used by OSG)
 */

public class OldClassAd extends AbstractClassAdOutputFormat {
   Pattern pattern = Pattern.compile(".*\\-.*|.*\\+.*|.*\\=.*|.*\\!.*|.*\\!=.*|.*\\*.*|.*/.*|.*\\..*|.*:.*|.*<.*|.*>.*|.*\\^.*|.*\\%.*");   //modify this regex in order to match more special characters contained into the attribute value 
   Pattern special=Pattern.compile("[0-9]+|[0-9]+\\.[0-9]+|undefined|true|t|false|f|error|\".*\"");

   Matcher matcher;

   public OldClassAd(ArrayList multipleAttributesList, ArrayList notSupportedAttributesList) {
      super("OLD_CLASSAD", multipleAttributesList, notSupportedAttributesList);
   }

   protected String ldifToClassad(String ldif, ArrayList multipleAttributes, ArrayList notSupportedAttributes) {
      if(ldif == null) {
         return null;
      }

      String[] attributes = ldif.split("\n");
      Hashtable attributeTable = new Hashtable();

      if(attributes == null) {
         return null;
      }

      // gets all supported attributes whose name doesn't compare in to the notSupportedAttributes list
      for (int i = 0; i < attributes.length; i++) {
         String[] attribute = attributes[i].split(": ",2);

         if(attribute != null && attribute.length == 2) {
            // parse the attribute's value
            String name = attribute[0].trim();
            
            // checks if the notSupportedAttributes list contains the selected attribute
            if( checkAttribute( name, notSupportedAttributes ) ) {
                continue;
            }
            
            String value = parseAttributeValue(attribute[1], false);

            if( checkAttribute( name, multipleAttributes ) ) {
                ArrayList values = null;

                if( attributeTable.containsKey( name ) ) {
                    values = (ArrayList) attributeTable.get( name );
                } else {
                    values = new ArrayList( );
                    attributeTable.put( name, values );
                }

                values.add( value );
            } else 
               attributeTable.put( name, value );
                        
         }
      }

      String classad = "[\n";

    
      for (Enumeration e = attributeTable.keys(); e.hasMoreElements();) {
         String attrName = (String) e.nextElement();
         Object attrValue = attributeTable.get(attrName);

         classad += "\t" + attrName + " = ";
    	  
    	  
    	  if (attrValue instanceof String) { 
          	//checking for variables
          	if (attributeTable.containsKey(attrValue)) {
               	   	classad += attrValue + ";\n";
                	continue;
          	}
    		matcher = pattern.matcher((String)attrValue);
    			  
    	  	if(matcher.matches()) {
    			  classad += "\"" + attrValue + "\";\n";
    	  	} else {
			 if (special.matcher((String)attrValue).matches()) {
    				  classad += attrValue + ";\n";
    			  } else {	  
    				  classad += "\"" + attrValue + "\";\n";
    			  }
		}
	 } else {
    		  classad += "\"";
    		  ArrayList values = (ArrayList) attrValue;
    			  
    		  for (int i = 0; i < values.size(); i++) {
    			  classad += values.get(i);
    			  classad += (i < values.size() - 1) ? "," : "\";\n";
    		  }
    	  }

      }

      classad += "\n]";
      return classad;
   }
}
