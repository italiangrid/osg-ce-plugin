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


/** 
 * The GLiteClassAd_2_2 class implements the SensorOutputDataFormat relative to the ClassAd version 2.2 (used by GLite)
 */
public class NewClassAd extends AbstractClassAdOutputFormat {

   public NewClassAd(ArrayList multipleAttributesList, ArrayList notSupportedAttributesList) {
      super("NEW_CLASSAD", multipleAttributesList, notSupportedAttributesList);
   }

   protected String ldifToClassad(String ldif, ArrayList multipleAttributes,  ArrayList notSupportedAttributes) {
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
         String[] attribute = attributes[i].split(": ");

         if(attribute != null && attribute.length == 2) {
            // parse the attribute's value
            String name = attribute[0].trim();
            String value = parseAttributeValue(attribute[1], true);

            // checks if the notSupportedAttributes list contains the selected attribute
            if( checkAttribute( name, notSupportedAttributes ) ) {
                continue;
            }

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

         if(attrValue instanceof String) {
            classad += attrValue + ";\n";
         } else {
            classad += "{\n\t\t";
            ArrayList values = (ArrayList) attrValue;

            for (int i = 0; i < values.size(); i++) {
               classad += values.get(i);
               classad += (i < values.size() - 1) ? ",\n\t\t" : " ";
            }

            classad += "\n\t};\n";
         }
      }

      classad += "\n]";
      return classad;
   }


}
