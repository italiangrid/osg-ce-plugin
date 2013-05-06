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
 *          Tanya Levshina <tlevshin@fnal.gov>
 *
 */

package org.glite.ce.osgceplugin.format.util;

import java.util.ArrayList;

public class CEGlueSchema {

	public static int GlueCE = 0;
	public static int GlueCluster = 1;
	public static int GlueSubCluster = 2;
	public static int GlueCESEBindSE = 3;
	public static int GlueCESEBindGroupCE = 4;
	public static int GlueSite = 5;
	public static int GlueSE=6;
	public static int GlueSA=7;
	public static int GlueSEControlProtocol=8;
	public static int GlueSEAccessProtocol=9;
	public static int GlueVOInfo=10;
	public static int GlueVOView=11;
	public static int GlueSoftware=12;
	public static int GlueSoftwareData=13;
	
	public static String[] keys = new String[] { "GlueCE", "GlueCluster",
		"GlueSubCluster", "GlueCESEBindSE", "GlueCESEBindGroupCE","GlueSite","GlueSE",
		"GlueSA","GlueSEControlProtocol","GlueSEAccessProtocol","GlueVOInfo","GlueVOView" ,
		"GlueSoftware","GlueSoftwareData" };

	public static String[] softwareKeys=new String[] {
		"GlueSoftwareLocalID","GlueSoftwareName","GlueSoftwareVersion",
		"GlueSoftwareInstalledRoot","GlueSoftwareEnvironmentSetup","GlueSoftwareModuleName"};



	public static String[] makeCEGlueSchemaInfo( String glueSchema ) {
		String[] msg = null;
		ArrayList[] elements = new ArrayList[keys.length];
		ArrayList ce_list  = new ArrayList(); 
		String siteInfo="";
		
		
		if( glueSchema == null ) {
			return null;
		}
		
		String[] dn = glueSchema.split( "\n\n" );

		
		for (int i=0;i<keys.length;i++) {
			elements[i]=new ArrayList( );
		}

		//elements are uniquely identified either by UniqueID or LocalID or
		//special case for software - Key
		//we are interested only in elements listed in "keys"
		for( int i = 0; i < dn.length; i++ ) {
			int type = checkElementType( dn[i],"UniqueID" );

			if( type != -1 ) {
				elements[type].add( dn[i] );
			} else {
				type = checkElementType( dn[i],"LocalID" );

				if( type != -1 ) {
					elements[type].add( dn[i] );
				} else {
					type = checkElementType( dn[i],"Key" );
					if( type != -1 ) {
						elements[type].add( dn[i] );
					}
				}
			}

		}

		//Collects site info
		for( int x = 0; x < elements[GlueSite].size( ); x++ ) {
			try{
				String site = (String) elements[GlueSite].get( x );
				siteInfo += "\n\n" + site;
			} catch( Exception e ) {
				e.printStackTrace( );
			}
		}

		//Starts building a classad info
		
		for( int x = 0; x < elements[GlueCE].size( ); x++ ) {
			String ceUniqueID, clusterUniqueID;
			String ceInfo = (String) elements[GlueCE].get( x );
			
			try {
				String voView="";
				
				ceUniqueID = getID( ceInfo, "dn: GlueCEUniqueID", "," ).trim( );
				//Finds all the VOs listed in GlueCEAccessControlBaseRule list
				ArrayList ceAccessControlBaseRule = getList(ceInfo, "GlueCEAccessControlBaseRule" );
				
				
				//find all voViews  for this cluster
				ArrayList voViews=getElements( elements[GlueVOView], ceUniqueID, "", "GlueCEUniqueID","," );
			
				//for each voView build one classad
				for (int v=0;v<voViews.size();v++) {
					voView=(String) voViews.get(v);
					String[] voViewAttributes=voView.split("\n");
					ArrayList voViewAccessControlBaseRule = getList(voView, "GlueCEAccessControlBaseRule" );
					//Gets list of VOs that is supported by this CE, the list of the overlapped 
					//supported VO is in the supportedVO
					ArrayList supportedVO=new ArrayList();
					boolean isAlike=isAlike(ceAccessControlBaseRule,voViewAccessControlBaseRule,supportedVO);
					if (supportedVO.size()>0) {
						for (int j=0;j<voViewAttributes.length;j++) {
							String voAttr=voViewAttributes[j];

							if (voAttr.startsWith("GlueCE")) {

								if (! voAttr.startsWith("GlueCEAccessControlBaseRule:")) {
									String attr=voAttr.split(": ")[0].trim();
									int index=ceInfo.indexOf(attr+": ");

									if (index >=0 ) {
										int index1=ceInfo.indexOf(voAttr);

										if (index1 >= 0) {
											ceInfo=ceInfo.replace(voAttr+"\n","");
										} else {
											ceInfo=ceInfo.replace(attr+":",attr+"Original"+":");
										}

									}

								} 

							}

						}
						
						if (!isAlike) {
							ceInfo=ceInfo.replaceAll("GlueCEAccessControlBaseRule:", "GlueCEAccessControlBaseRuleOriginal:");
						}else {
							for (int l=0;l<supportedVO.size();l++) {
								voView=voView.replace("\nGlueCEAccessControlBaseRule: "+(String) supportedVO.get(l),"");
							}
						}
						
					}
					
					//End of gathering voViewInfo


					

					clusterUniqueID = getID( ceInfo, "GlueForeignKey", "\n" ).trim( );

					//Finds all the binding elements between CE and SE
					ArrayList bindInfo = getElements( elements[GlueCESEBindSE], ceUniqueID, "", "GlueCESEBindGroupCEUniqueID", "," );

					//Finds clusters
					ArrayList clusterInfo = getElements( elements[GlueCluster],     clusterUniqueID, "GlueClusterUniqueID=", "dn: GlueClusterUniqueID", "," );

					//for each cluster 
					for( int i = 0; i < clusterInfo.size( ); i++ ) {
						ceInfo += "\n\n" + clusterInfo.get( i );
						ArrayList subclusterInfo = getElements( elements[GlueSubCluster], clusterUniqueID, "",  "GlueChunkKey", "\n" );

						//for each subcluster
						for(int j=0; j<subclusterInfo.size(); j++) {
							String subClusterUniqueID = getID((String) subclusterInfo.get(j),
									"dn: GlueSubClusterUniqueID", "," ).trim( );
							String subInfo=(String) subclusterInfo.get(j)+"\n";
							//Deals with software info
							
							ArrayList tmp=getElements(elements[GlueSoftware],subClusterUniqueID,"","GlueSubClusterUniqueID", ",");
							ArrayList softwareInfo=getElements(tmp,clusterUniqueID, "GlueClusterUniqueID=","GlueClusterUniqueID", ",");
							//Finds all the software for cluster and subcluster
							//Looks for software Data for software,cluster,subcluster
							for (int m=0;m<softwareInfo.size();m++) {
								String softwareID=getID((String) softwareInfo.get(m),"dn: GlueSoftwareLocalID", "," ).trim( );
								tmp=getElements(elements[GlueSoftwareData],softwareID, "","GlueSoftwareLocalID", ",");
								ArrayList tmp1=getElements(tmp,subClusterUniqueID,"","GlueSubClusterUniqueID", ",");
								ArrayList softwareData=getElements(tmp1,clusterUniqueID, "GlueClusterUniqueID=","GlueClusterUniqueID", ",");
								//Adds this information to subcluster
								subInfo=subInfo+"\n"+(String) softwareInfo.get(m)+"\n"+addSoftwareDataKey(softwareData);
							}
							// Used to identify classads which do not have seinfo,
							// but the there are still some ce-to-se bindings
 							// Common use cases sites that have SE available for
							// only a subset of VOs.
							boolean classAdGenerated = false;

							if (bindInfo.size()>0) {
								//for each SE
								for (int k=0;k<bindInfo.size();k++) {
									//Find SE information
									String seUniqueID = getID((String) bindInfo.get(k), "dn: GlueCESEBindSEUniqueID", "," ).trim( );
									String seInfo = "\n\n" + elementsToString(getElements( elements[GlueSE], seUniqueID, "", "dn: GlueSEUniqueID", "," ));
									//Add Control Protocol Info
									seInfo += "\n\n" + elementsToString(getElements( elements[GlueSEControlProtocol], seUniqueID, "", "GlueSEUniqueID", "," ));
									//Add Access Protocol Info
									seInfo += "\n\n" + elementsToString(getElements( elements[GlueSEAccessProtocol], seUniqueID, "", "GlueSEUniqueID", "," ));

									//Add Storage Area
									ArrayList saInfo = getElements( elements[GlueSA], seUniqueID, "", "GlueSEUniqueID", "," );

									//for each Storage Area
									if (saInfo.size()==0) { 
										//We will create just CE related classad
										ce_list.add( ceInfo+ "\n\n"+ siteInfo+ "\n\n" + "\n\n" + voView+"\n\n"+ subInfo);
										classAdGenerated = true;
										continue;
									}																		
									for (int l=0;l<saInfo.size();l++) {
										ArrayList supportedSAVO=new ArrayList();
										ArrayList saAccessControlBaseRule = getList((String)saInfo.get(l), "GlueSAAccessControlBaseRule" );
										isAlike=isAlike(supportedVO,saAccessControlBaseRule,supportedSAVO);
										if (supportedSAVO.size()==0) {
											continue;
										}

										String saLocalID=getID((String) saInfo.get(l), "dn: GlueSALocalID", "," ).trim( );
										ArrayList voInfo = getElements( elements[GlueVOInfo], saLocalID, "", "GlueSALocalID", "," );
										String voinfo =  "";
										for (int n=0;n<voInfo.size();n++) {
											ArrayList supportedVOInfoVO=new ArrayList();
											ArrayList voInfoAccessControlBaseRule = getList((String)voInfo.get(n), "GlueVOInfoAccessControlBaseRule" );
											isAlike=isAlike(supportedSAVO,voInfoAccessControlBaseRule,supportedVOInfoVO);
											if (supportedVOInfoVO.size()==0) {
												continue;
											}
											voinfo=(String)voInfo.get(n);
											break;
											//assume that we have vo defined only once in VO Info
										}

										ce_list.add( ceInfo+ "\n\n"+(String) bindInfo.get(k)+"\n\n"+ siteInfo+ "\n\n" +
												voView+"\n\n"+ subInfo+
												"\n\n" + seInfo+"\n\n"+saInfo.get(l)+"\n\n"+voinfo);

										classAdGenerated = true;
									}
								}
							} else {
								//no storage binding
								ce_list.add( ceInfo+ "\n\n"+ siteInfo+ "\n\n" + "\n\n" + voView+"\n\n"+ subInfo);
								classAdGenerated = true;
							}
							// Generate classads if there are se-ce bindings and
							// still the classads were not generated
							if (classAdGenerated == false) {
								ce_list.add(ceInfo + "\n\n" + siteInfo + "\n\n" + "\n\n" + voView + "\n\n" + subInfo);
							}

						}
					}

				} //end of voView 
			} catch( Exception e ) {
				e.printStackTrace( );
			}

		}// end of CE
		msg = new String[ce_list.size( )];
		return (String[])ce_list.toArray(msg);
	}


	private static String getID( String dn, String key, String delim ) throws Exception {
		if( dn == null || key == null ) {
			throw ( new Exception( "dn or key is null!" ) ); }

		int index = dn.indexOf( key );

		if( index > -1 ) {
			index += key.length( ) + 1;
			String id = dn.substring( index, dn.indexOf( delim, index + 1 ) );
			return id;
		} else {
			throw ( new Exception( "key " + key + " not found!" ) );
		}
	}


	private static int checkElementType( String dn, String id ) {
		for( int i = 0; i < keys.length; i++ ) {
			if( dn.startsWith( "dn: " + keys[i] + id ) ) { return i; }
		}

		return -1;
	}


	private static ArrayList getElements( ArrayList elements, String uniqueID,
			String prefix, String key, String delim ) throws Exception {
		ArrayList list = new ArrayList( );

		for( int y = 0; y < elements.size( ); y++ ) {
			String info = (String) elements.get( y );
			String id = getID( info, key, delim ).trim( );

			if( uniqueID.equals( prefix + id ) ) {
				list.add( info );
			}
		}

		return list;
	}

	private static String addSoftwareDataKey(ArrayList softwareData) throws Exception {
		String params="";
		if (softwareData.size()==0) {
			return "GlueSoftwareDataParams: ";
		}
		for (int i=0;i<softwareData.size();i++) {

			
			String key=getID((String) softwareData.get(i),"GlueSoftwareDataKey:", "\n");
			
			String value=getID((String) softwareData.get(i)+"\n","GlueSoftwareDataValue:", "\n");
			if (params.length()==0) {
				params="GlueSoftwareDataParams: ";
			} else {
				params +="; ";
			}

			params += key+"="+value;
		}
		return params;
	}



	private static ArrayList getList( String dn, String prefix) throws Exception {
		ArrayList list = new ArrayList( );
		String[] elements=dn.split("\n");

		for( int y = 0; y < elements.length; y++ ) {
			String info = elements[ y ].trim();
			if( info.startsWith( prefix.trim()) ) {
				list.add( info.split(": ")[1].trim() );
			}
		}

		return list;
	}
	private static boolean  isAlike(ArrayList arr1 , ArrayList arr2, ArrayList supportedVO) throws Exception {
		boolean isAlike=true;

		for (int i=0;i<arr1.size();i++) {
			boolean found=false;
			String v1=(String) arr1.get(i);
			if (v1.indexOf(":") > 0) {
				v1=v1.split(":")[1].trim();
			}
			for (int j=0;j<arr2.size();j++) {
				String v2=(String) arr2.get(j);
				if (v2.indexOf(":") > 0) {
					v2=v2.split(":")[1].trim();
				}

				if (v1.equals(v2)) {
					supportedVO.add(v1);
					found=true;
				}
			}
			if (isAlike && !found) {
				isAlike=false;
			}
		}

		return isAlike;
	}
	private static String elementsToString( ArrayList elements ) {
		String result = "";

		if( elements != null ) {
			for( int i = 0; i < elements.size( ); i++ ) {
				String info = (String) elements.get( i );

				if( info != null ) {
					result += "\n\n" + info;
				}
			}
		}

		return result;
	}

}
