package edu.pitt.dbmi.domainbuilder.util;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Vector;
import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.knowledge.ConceptHandler;
import edu.pitt.dbmi.domainbuilder.knowledge.OntologyAction;
import edu.pitt.dbmi.domainbuilder.knowledge.OntologySynchronizer;
import edu.pitt.dbmi.domainbuilder.widgets.TreePanel;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.client.OntologyTerminology;
import edu.pitt.terminology.lexicon.Concept;
import edu.pitt.text.tools.TextTools;


/**
 * keep track of all ontology related constants in one place
 * @author tseytlin
 */
public class OntologyHelper {
	// relevant events
	public static final String KB_LOADED_EVENT = "KNOWLEDGE_BASE_LOADED";
	public static final String TREE_LOADED_EVENT = "TREE_LOADED";
	public static final String CASE_KB_RELOADED_EVENT = "CASE_KB_RELOADED_EVENT";
	public static final String CASE_KB_UPDATED_EVENT = "CASE_KB_UPDATED_EVENT";
	public static final String SUGGEST_TERM_EVENT ="SUGGEST_TERM_EVENT";
	public static final String CASE_OPEN_EVENT = "CASE_OPEN_EVENT";
	public static final String OPEN_DIAGNOSIS_EVENT = "OPEN_DIAGNOSIS_EVENT";
	public static final String CLEAR_DIAGNOSIS_EVENT = "CLEAR_DIAGNOSIS_EVENT";
	public static final String SHOW_CONCEPT_EVENT = "SHOW_CONCEPT_EVENT";
	public static final String CLASS_RENAME_EVENT = "CLASS_RENAME_EVENT";
	public static final String CLASS_DELETE_EVENT = "CLASS_DELETE_EVENT";
	public static final String RELOAD_UI_EVENT = "RELOAD_UI_EVENT";
	
	// ontology names
	public static final String EVS_TERMINOLOGY = "Enterprise Vocabulary Service";
	public static final String ONTOLOGY_TERMINOLOGY = "Ontology Terminology";
	public static final String LUCENE_TERMINOLOGY = "Lucene Terminology";
	public static final String REMOTE_TERMINOLOGY = "Remote Terminology";
	public static final String KNOWLEDGE_BASE = "KnowledgeBase.owl";
	public static final String ANATOMY_ONTOLOGY = "AnatomicalSites.owl";
	public static final String OWL_SUFFIX = ".owl";
	public static final String CASE_SUFFIX = ".case";
	public static final String INSTANCES_ONTOLOGY = "Instances"+OWL_SUFFIX;
	public static final String EXAMPLES_FOLDER = "examples";
	public static final String CASES_FOLDER = "cases";
	public static final String SPREADSHEET_FOLDER = "spreadsheets";
	public static final String DEFAULT_HOST_URL = "http://slidetutor.upmc.edu";
	public static final String CURRICULUM_ROOT = "curriculum";
	public static final String KNOWLEDGE_FOLDER = "owl";
	public static final String CONFIG_FOLDER = "config";
	public static final String DEFAULT_FILE_MANAGER_SERVLET = DEFAULT_HOST_URL+"/domainbuilder/servlet/FileManagerServlet";
	public static final String DEFAULT_INSTITUTION = "PITT";
	public static final String TERMS_SUFFIX = ".terms";
	
	
	//public static final String WORD = "_word";
	public static final String SCHEMA = "_template";
	public static final String NO = "no_";
	public static final String IN = "in";
	public static final String NEW_CASE = "NEW_CASE";
	public static final String ONTOLOGY_ACTION = "ONTOLOGY_ACTION";
	public static final String DEFAULT_BASE_URI = "http://slidetutor.upmc.edu/curriculum/owl/";
	public static final String FILE_MANAGER_SERVLET = "/servlet/FileManagerServlet";
	public static final String [] LOCATION_PREFIXES = new String [] {"in","at","within","on","along","of"};
	public static final double NO_VALUE = -777;
	public static final URI KNOWLEDGE_BASE_URI = URI.create(DEFAULT_BASE_URI+KNOWLEDGE_BASE);
	public static final String CONFIG_FILE = "DomainBuilder.conf";
	public static final String DEFAULT_PART = "Pathology-1";
	
	// meta data stuff
	public static final String TITLE = "dc:title";
	public static final String DESCRIPTION = "dc:description";
	public static final String VERSION = "version";
	public static final String CREATOR = "dc:creator";
	public static final String CONTRIBUTOR = "dc:contributor";
	public static final String SOURCE = "dc:source";
	
	// class names
	public static final String CONCEPTS = "CONCEPTS";
	public static final String CASES = "CASES";
	public static final String SCHEMAS = "TEMPLATES";
	//public static final String LEXICON = "LEXICON";
	public static String DISEASES = "DIAGNOSES"; //"DISEASES";
	public static final String ACTIONS = "ACTIONS";
	public static final String WORKSHEET = "WORKSHEET";
	public static final String NUMERIC = "Number";
	public static final String ANATOMIC_LOCATION = "Anatomic_Location";
	/*
	public static final String FEATURES = "FEATURES";
	public static final String ATTRIBUTES = "ATTRIBUTES";
	public static final String DIAGNOSTIC_FEATURES = "DIAGNOSTIC_FEATURES";
	public static final String PROGNOSTIC_FEATURES = "PROGNOSTIC_FEATURES";
	*/
	public static final String FEATURES = "FINDINGS";
	public static final String ATTRIBUTES = "ATTRIBUTES";
	public static final String MODIFIERS = "MODIFIERS";
	public static final String LOCATIONS = "LOCATION";
	public static final String RECOMMENDATIONS = "RECOMMENDATIONS";
	public static final String ANCILLARY_STUDIES = "ANCILLARY_STUDIES";
	public static final String VALUES = "VALUES";
	public static final String DIAGNOSTIC_FEATURES = "DIAGNOSTIC_FINDINGS";
	public static final String PROGNOSTIC_FEATURES = "PROGNOSTIC_FINDINGS";
	public static final String CLINICAL_FEATURES = "CLINICAL_FINDINGS";
	public static final String ARCHITECTURAL_FEATURES = "ARCHITECTURAL_FEATURES";
	public static final String CYTOLOGIC_FEATURES = "CYTOLOGIC_FEATURES";
	/*
	public static final String SEMANTIC_TYPE_DISEASE = "Disease";
	public static final String SEMANTIC_TYPE_DIAGNOSTIC = "Diagnostic";
	public static final String SEMANTIC_TYPE_PROGNOSTIC = "Prognostic";
	public static final String SEMANTIC_TYPE_CLINICAL = "Clinical";
	public static final String SEMANTIC_TYPE_MODIFIER = "Modifier";
	*/
	
	// property names
	//public static final String HAS_CONCEPT = "isConceptOf";
	//public static final String HAS_SEMANTIC_TYPE = "hasSemanticType";
	//public static final String HAS_DISEASE = "hasDisease";
	//public static final String HAS_ATTRIBUTE = "hasAttribute";
	public static final String HAS_CLINICAL = "hasClinicalFinding";
	public static final String HAS_ANCILLARY = "hasAncillaryStudies";
	public static final String HAS_FINDING = "hasFinding";
	public static final String HAS_NO_FINDING = "hasAbsentFinding";
	public static final String HAS_PROGNOSTIC = "hasPrognostic";
	public static final String HAS_TRIGGER    = "hasTrigger";
	public static final String HAS_ACTION 	  = "hasAction";
	public static final String [] ALL_FEATURE_PROPERTIES = new String [] {HAS_FINDING,HAS_NO_FINDING,HAS_CLINICAL,HAS_ANCILLARY,HAS_PROGNOSTIC};
	
	public static final String HAS_TAG = "tags";
	public static final String HAS_STATUS = "hasStatus";
	public static final String HAS_CONCEPT_CODE = "code";
	public static final String HAS_EXAMPLE = "example";
	public static final String HAS_POWER = "power";
	public static final String HAS_ORDER = "order";
	public static final String HAS_PROBABILITY = "probability";
	public static final String HAS_REPORT = "hasReport";
	public static final String HAS_SLIDE = "hasImage";
	public static final String HAS_RESOURCE_LINK = "hasResourceLink";
	public static final String HAS_NUMERIC_VALUE = "hasNumericValue";
	public static final String IS_ABSENT = "isAbsent";
	//public static final String RESOURCE_LINK = "resource_link";

	// constant names
	public static final String STATUS_INCOMPLETE = "incomplete";
	public static final String STATUS_COMPLETE = "complete";
	public static final String STATUS_TESTED = "tested";
	
	public static final String POWER_LOW = "low";
	public static final String POWER_MEDIUM = "medium";
	public static final String POWER_HIGH = "high";
	
	
	// some internal fields
	private static Map<IOntology,ConceptHandler> creatorRegistry;
	private static Set<TreePanel> treelist = new HashSet<TreePanel>();
	private static Terminology anatomyTerminology;
	private static Map<IOntology,Boolean> readOnlyRegistry;
	private static Connection conn;
	private static Vector<String> institutions;
	private static String defaultServer;
	

	public static Vector<String> getInstitutions(){
		if(institutions == null)
			institutions = new Vector<String>();
		return institutions;
	}


	public static void setInstitutions(Vector<String> institutions) {
		OntologyHelper.institutions = institutions;
	}

	
	public static String getDomainBuilderServer(){
		if(defaultServer == null)
			defaultServer = DEFAULT_HOST_URL+"/domainbuilder";
		return defaultServer;
	}
	
	public static void setDomainBuilderServer(String s){
		defaultServer = s;
	}

	public static URL getConfigFile(String place){
		try{
			return new URL(getDomainBuilderServer()+"/config/"+place+"/"+CONFIG_FILE);
		}catch(MalformedURLException ex){
			
		}
		return null;
	}
	
	/**
	 * this is for backword compatibility
	 * @param ont
	 */
	public static void checkOntologyVersion(IOntology ont){
		if(ont.getClass(DISEASES) == null)
			DISEASES = "DISEASES";
	}
	
	
	/**
	 * is given URI from same instition
	 * @param uri
	 * @param place
	 * @return
	 */
	
	public static boolean isSameInstitution(URI uri, String place){
		// if one of parameters is missing, then simply acknowledge
		if(uri == null || TextHelper.isEmpty(place))
			return true;
		
		// check URI
		return uri.toString().contains("/"+place+"/");
	}
	
	/**
	 * is dirty
	 * @return
	 */
	public static void setDirty(){
		for(TreePanel p: treelist)
			p.setDirty(true);
	}
	
	/**
	 * register tree panel
	 * @param p
	 */
	public static void registerTreePanel(TreePanel p){
		treelist.add(p);
	}
	
	/**
	 * register tree panel
	 * @param p
	 */
	public static void unregisterTreePanel(TreePanel p){
		treelist.remove(p);
	}
	
	
	public static Terminology getAnatomicTerminology(){
		if(anatomyTerminology == null){
			IOntology [] ont = DomainBuilder.getRepository().getOntologies(ANATOMY_ONTOLOGY);
			if(ont.length > 0)
				anatomyTerminology = new OntologyTerminology(ont[0]);
			//ont.dispose();
		}
		return anatomyTerminology;
	}
	/**
	 * create ontology friendly class name
	 * @param name
	 * @return
	 */
	public static String getClassName(String name){
		//return name.trim().replaceAll("\\s*\\(.+\\)\\s*","").replaceAll("\\W","_").replaceAll("_+","_");
		return name.trim().replaceAll("\\s*\\(.+\\)\\s*","").replaceAll("[^\\w\\-]","_").replaceAll("_+","_");
	}
	
	
	/**
	 * is something a knowledge base class
	 * @param cls
	 * @return
	 */
	public static boolean isSystemClass(IClass cls){
		return cls != null && cls.getNameSpace().contains(KNOWLEDGE_BASE);
	}
	
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isAttribute(IClass c){
		return isOfParent(c,ATTRIBUTES);
	}
	
	
	
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isModifier(IClass c){
		return isOfParent(c,MODIFIERS);
	}
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isValue(IClass c){
		return isOfParent(c,VALUES);
	}
	
	/**
	 * is something an attribute?
	 * @param c
	 * @return
	 */
	public static boolean isNumber(IClass c){
		return NUMERIC.equals(c.getName()) || isOfParent(c,NUMERIC);
	}
	
	/**
	 * is something a location
	 * @param c
	 * @return
	 */
	public static boolean isLocation(IClass c){
		return isOfParent(c,LOCATIONS);
	}
	
	/**
	 * is something a location
	 * @param c
	 * @return
	 */
	public static boolean isDirectLocation(IClass c){
		return c.hasDirectSuperClass(c.getOntology().getClass(LOCATIONS));
	}
	
	
	/**
	 * check if this entry is a feature NOTE that FAVs are also features
	 * @return
	 */
	public static boolean isFeature(IClass c){
		return isOfParent(c,FEATURES);
	}
	
	
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isDisease(IClass c){
		return isOfParent(c,DISEASES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isDiagnosticFeature(IClass cls){
		return isOfParent(cls,DIAGNOSTIC_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isArchitecturalFeature(IClass cls){
		return isOfParent(cls,ARCHITECTURAL_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isCytologicFeature(IClass cls){
		return isOfParent(cls,CYTOLOGIC_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isPrognosticFeature(IClass cls){
		return isOfParent(cls,PROGNOSTIC_FEATURES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isClinicalFeature(IClass cls){
		return isOfParent(cls,CLINICAL_FEATURES);
	}
	
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isAncillaryStudy(IClass cls){
		return isOfParent(cls,ANCILLARY_STUDIES);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isRecommendation(IClass cls){
		return isOfParent(cls,RECOMMENDATIONS);
	}
	
	
	
	/**
	 * is this class an attribute category
	 * @param cls
	 * @return
	 */
	public static boolean isAttributeCategory(IClass cls){
		return isAttribute(cls) && !isDisease(cls) && cls.getName().matches("[A-Z_]+");
	}
	
	
	/**
	 * is class an anatomic location
	 * @param cls
	 * @return
	 */
	public static boolean isAnatomicLocation(IClass cls){
		return isOfParent(cls,ANATOMIC_LOCATION);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	private static boolean isOfParent(IClass cls,String parent){
		if(cls == null)
			return false;		
		IOntology o = cls.getOntology();
		IClass p = o.getClass(parent);
		return p != null && (cls.equals(p) || cls.hasSuperClass(p));
	}
	
	
	/**
	 * get knowledge base ontology
	 * @param ont
	 * @return
	 */
	public static URI getKnowledgeBase(URI ont){
		String u = ""+ont;
		if(u.endsWith(INSTANCES_ONTOLOGY))
			u = u.substring(0,u.length()-INSTANCES_ONTOLOGY.length())+OWL_SUFFIX;
		return URI.create(u);
	}
	
	/**
	 * get knowledge base ontology
	 * @param ont
	 * @return
	 */
	public static IOntology getKnowledgeBase(IOntology ont){
		URI u = getKnowledgeBase(ont.getURI());
		// if we can simply derive the name,
		if(ont.getRepository().hasOntology(""+u))
			return ont.getRepository().getOntology(u);
		// else return first imported ontology
		for(IOntology o: ont.getImportedOntologies()){
			return o;
		}
		return null;
	}
	
	/**
	 * get case base ontology from knowledge base
	 * @param ont
	 * @return
	 */
	public static IOntology getCaseBase(IOntology ont){
		String u = ""+ont.getURI();
		if(u.endsWith(".owl"))
			u = u.substring(0,u.length()-4);
		u = u + INSTANCES_ONTOLOGY;
		// if we can simply derive the name,
		if(ont.getRepository().hasOntology(""+u))
			return ont.getRepository().getOntology(URI.create(u));
		return null;
	}
	
	
	
	/**
	 * shortcut to get property value
	 * @param r
	 * @param prop
	 * @return
	 */
	public static Object getPropertyValue(IResource r, String prop){
		IProperty p = r.getOntology().getProperty(prop);
		return (p != null)?r.getPropertyValue(p):null;
	}
	
	/**
	 * shortcut to get property values
	 * @param r
	 * @param prop
	 * @return
	 */
	public static Object [] getPropertyValues(IResource r, String prop){
		return r.getPropertyValues(r.getOntology().getProperty(prop));
	}
	
	
	// utility methods
	/**
	 * extract property values from expression and put them into a list
	 * @param exp
	 * @return
	 */
	public static Set<IClass> getPropetyValues(ILogicExpression exp,IProperty prop,Set<IClass> list){
		for(Object obj : exp){
			if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				if(prop.equals(p))
					getPropetyValues(r.getParameter(),p,list);
			}else if(obj instanceof IClass && prop != null){
				// convert class to a concept entry
				IClass c = (IClass) obj;
				list.add(c);
			}else if(obj instanceof ILogicExpression){
				// recurse into expression
				getPropetyValues((ILogicExpression) obj,prop,list);
			}
		}
		return list;
	}
	
	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	public static Collection<ConceptEntry> getPropertyConcepts(ILogicExpression exp, IProperty prop, Collection<ConceptEntry> list){
		return getPropertyConcepts(exp,prop,list,(prop != null)?prop.getName():"");
	}
	
	
	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	public static Collection<ConceptEntry> getPropertyConcepts(ILogicExpression exp, IProperty prop, Collection<ConceptEntry> list, String pname){
		for(Object obj : exp){
			if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				getPropertyConcepts(r.getParameter(),p,list,pname);
			}else if(obj instanceof IClass && prop != null){
				// convert class to a concept entry
				IClass c = (IClass) obj;
				ConceptEntry entry = new ConceptEntry(c);
				if(prop.getName().contains(pname)){
					list.add(entry);
				}
			}else if(obj instanceof ILogicExpression){
				// recurse into expression
				getPropertyConcepts((ILogicExpression) obj,prop,list,pname);
			}
		}
		return list;
	}

	/**
	 * Get instance with given name, if it doesn't exist
	 * create it
	 * @param name
	 * @return
	 */
	public static IInstance getInstance(IClass cls, String name){
		// OMG, if class name is the same as instance name, then
		// we are in trouble, big trouble
		if(cls.getName().equals(name))
			name = name +"1";
		
		// now do your little thing
		IInstance inst = cls.getOntology().getInstance(name);
		if(inst == null)
			inst = cls.createInstance(name);
		return inst;
	}
	
	/**
	 * Get instance with given name, if it doesn't exist
	 * create it
	 * @param name
	 * @return
	 */
	public static IInstance getInstance(IClass cls){
		return getInstance(cls,cls.getName().toLowerCase());
	}
	
	
	/**
	 * get appropriate concept creator for a given ontology
	 * @param ont
	 * @return
	 */
	public static ConceptHandler getConceptHandler(IOntology ont){
		if(creatorRegistry == null){
			creatorRegistry = new HashMap<IOntology,ConceptHandler>();
		}
		// if no such entry in registry, then insert it
		if(!creatorRegistry.containsKey(ont)){
			creatorRegistry.put(ont,new ConceptHandler(ont));
		}
		return creatorRegistry.get(ont);
	}
	
	
	/**
	 * clear ontology related caches
	 */
	public static void clearCache(){
		OntologySynchronizer.getInstance().clear();
		if(creatorRegistry != null){
			creatorRegistry.clear();
		}
	}
	
	
	/**
	 * get restriction for given value
	 * @param exp
	 * @param value
	 * @return
	 */
	public static IRestriction getRestriction(ILogicExpression exp, Object value){
		for(Object obj : exp){
			if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				if(r.getParameter().equals(value)){
					return r;
				}
			}else if(obj instanceof ILogicExpression){
				// recurse into expression
				return getRestriction((ILogicExpression) obj,value);
			}
		}
		return null;
	}
	
	
	/**
	 * gets  location prefix or null if not availabpe
	 * @param text
	 * @return
	 */
	public static String getLocationPrefix(String text){
		text = text.toLowerCase();
		for(String prefix: LOCATION_PREFIXES){
			if(text.startsWith(prefix+" "))
				return prefix;
		}
		return null;
	}
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static File getDefaultImageFolder(){
		String dir    =  DomainBuilder.getParameter("repository.folder");
		File folder = new File(System.getProperty("user.home")+File.separator+dir+File.separator+"images");
		if(!folder.exists())
			folder.mkdirs();
		return folder;
	}
	
	
	/**
	 * get base URL for ontology 
	 * Example: for ontology http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/Melanocytic.owl
	 * it returns http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/
	 * @param ont
	 * @return
	 */
	public static URL getBaseURL(IOntology ont){
		try{
			String uri = ""+ont.getURI().toURL();
			if(uri.endsWith(ont.getName()));
				uri = uri.substring(0,uri.length()-ont.getName().length());
			return new URL(uri);
		}catch(MalformedURLException ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * get base URL for ontology 
	 * Example: for ontology http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/Melanocytic.owl
	 * it returns http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/
	 * @param ont
	 * @return
	 */
	public static URL getExampleURL(IOntology ont){
		try{
			String path = ""+ont.getURI();
			
			// strip the owl suffix
			if(path.endsWith(OWL_SUFFIX))
				path = path.substring(0,path.length()-OWL_SUFFIX.length());
			
			// replace /owl/ with /CASE/
			path = path.replaceAll("/owl/","/"+EXAMPLES_FOLDER+"/");
			
			// replace /domainbuilder/ with /curriculum/
			// for backword compatibility
			path = path.replaceAll("/domainbuilder/","/curriculum/");
		
			return new URL(path);
		}catch(MalformedURLException ex){
			ex.printStackTrace();
		}
		return null;
	}
	
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static File getLocalExampleFolder(IOntology ont){
		//File folder = new File(getLocalOntologyFolder(ont),EXAMPLES_FOLDER);
		//if(!folder.exists())
		//	folder.mkdirs();
		String dir =  getLocalRepositoryFolder();
		String path = getExamplePath(ont);
		
		// convert to file separatorors
		path = path.replace('/',File.separatorChar);
		File folder = new File(dir+File.separator+path);
		if(!folder.exists())
			folder.mkdirs();
		
		return folder;
	}
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static File getLocalSpreadsheetFolder(IOntology ont){
		//File folder = new File(getLocalOntologyFolder(ont),EXAMPLES_FOLDER);
		//if(!folder.exists())
		//	folder.mkdirs();
		String dir =  getLocalRepositoryFolder();
		String path = getSpreadsheetPath(ont);
		
		// convert to file separatorors
		path = path.replace('/',File.separatorChar);
		File folder = new File(dir+File.separator+path);
		if(!folder.exists())
			folder.mkdirs();
		
		return folder;
	}
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static File getLocalCaseFolder(IOntology ont){
		//File folder = new File(getLocalOntologyFolder(ont),CASES_FOLDER);
		//if(!folder.exists())
		//	folder.mkdirs();
		String dir =  getLocalRepositoryFolder();
		String path = getCasePath(ont);
		
		// convert to file separatorors
		path = path.replace('/',File.separatorChar);
		File folder = new File(dir+File.separator+path);
		if(!folder.exists())
			folder.mkdirs();
		
		return folder;
	}
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getCasePath(IOntology ont){
		String path = ont.getURI().getPath();
		// strip the owl suffix
		if(path.endsWith(OWL_SUFFIX))
			path = path.substring(0,path.length()-OWL_SUFFIX.length());
		
		// replace /owl/ with /CASE/
		path = path.replaceAll("/owl/","/"+CASES_FOLDER+"/");
		
		// replace /domainbuilder/ with /curriculum/
		// for backword compatibility
		path = path.replaceAll("/domainbuilder/","/curriculum/");
		
		return path;
	}
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getSpreadsheetPath(IOntology ont){
		String path = ont.getURI().getPath();
		// strip the owl suffix
		if(path.endsWith(ont.getName()))
			path = path.substring(0,path.length()-ont.getName().length());
		
		// replace /owl/ with /CASE/
		path = path.replaceAll("/owl/","/"+SPREADSHEET_FOLDER+"/");
		
		// replace /domainbuilder/ with /curriculum/
		// for backword compatibility
		path = path.replaceAll("/domainbuilder/","/curriculum/");
		
		return path;
	}
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static String getExamplePath(IOntology ont){
		String path = ont.getURI().getPath();
		// strip the owl suffix
		if(path.endsWith(OWL_SUFFIX))
			path = path.substring(0,path.length()-OWL_SUFFIX.length());
		
		// replace /owl/ with /CASE/
		path = path.replaceAll("/owl/","/"+EXAMPLES_FOLDER+"/");
		
		// replace /domainbuilder/ with /curriculum/
		// for backword compatibility
		path = path.replaceAll("/domainbuilder/","/curriculum/");
		
		return path;
	}
	
	
	/**
	 * get default image folder for domain builder
	 * @return
	 */
	public static File getLocalOntologyFolder(IOntology ont){
		String dir =  getLocalRepositoryFolder();
		String path = ont.getURI().getPath();
		if(path.endsWith(ont.getName()))
			path = path.substring(0,path.length()-ont.getName().length());
		path = path.replace('/',File.separatorChar);
		File folder = new File(dir+path);
		if(!folder.exists())
			folder.mkdirs();
		return folder;
	}
	
	
	/**
	 * get local repository folder
	 * @return
	 */
	public static String getLocalRepositoryFolder(){
		String path = DomainBuilder.getParameter("curriculum.path");
		if(TextHelper.isEmpty(path)){
			String dir    =  DomainBuilder.getParameter("repository.folder");
			dir = System.getProperty("user.home")+File.separator+dir+File.separator;
			return dir;
		}else{
			File f= new File(path);
			if(f.exists())
				return (f.getName().endsWith(CURRICULUM_ROOT))?f.getParentFile().getAbsolutePath():f.getAbsolutePath();
		}
		return "";
	}
	
	
	/**
	 * save example image for given class in approprate location
	 * @param file
	 * @param cls
	 * @return
	 */
	public static void removeExampleImage(ExampleImage img){
		/*
		final ExampleImage image = img;
		//remove remote file
		OntologyAction action = new OntologyAction(){
			public void run(){
				// remove file
				File f = image.getFile();
				if(f != null && f.exists()){
					f.delete();
				}
				// send st
				String path = image.getURL().getPath();
				// figure out the root
				if(path.startsWith("/"))
					path = path.substring(1);
				int i = path.indexOf("/");
				String root = "";
				
				// this should always be the case
				if(i > -1){
					root = path.substring(0,i);
					path = path.substring(i+1);
				}
				try{
					Communicator.delete(root,path);
				}catch(IOException ex){
					ex.printStackTrace();
				}
			}
			public void undo(){
				
			}
			public String toString(){
				return "remove example image: "+image.getName();
			}
		};
		// add to syncronizer
		OntologySynchronizer.getInstance().addOntologyAction(action);
		*/
		
	}
	
	/**
	 * save example image for given class in approprate location
	 * @param file
	 * @param cls
	 * @return
	 */
	public static File saveExampleImage(File source,IClass cls){
		// don't bother if class is not there
		if(cls == null)
			return null;
		
		String name = source.getName();
		int i= name.lastIndexOf(".");
		String suffix = (i>-1)?name.substring(i):""; 
		
		// figure out offset for examples
		int offset = 0;
		Pattern pt = Pattern.compile(".+\\.(\\d+)\\.\\w+");
		for(Object o: cls.getPropertyValues(cls.getOntology().getProperty(HAS_EXAMPLE))){
			Matcher m = pt.matcher(""+o);
			if(m.matches()){
				int x = Integer.parseInt(m.group(1));
				if(x > offset)
					offset = x;
			}
		}
		name = cls.getName()+"."+(offset+1)+suffix;
		
		// create target filename
		File dir = getLocalExampleFolder(cls.getOntology());
		File target = new File(dir,name);
		// find file that doesn't exist
		while(cls != null && target.exists()){
			target = new File(dir,cls.getName()+"."+(offset++)+suffix);
		}
		
		try{
			UIHelper.copy(source,target);
			final File file = target;
			// add to upload in the future
			OntologyAction action = new OntologyAction(){
				public void run(){
					try{
						UIHelper.upload(file);
					}catch(IOException ex){
						ex.printStackTrace();
					}
				}
				public void undo(){
					
				}
				public String toString(){
					return "upload example image: "+file;
				}
			};
			// add to syncronizer
			OntologySynchronizer.getInstance().addOntologyAction(action);
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return target;
	}

	
	/**
	 * find value parent
	 * @param cls
	 * @return
	 */
	public static IClass getValueParent(IClass cls){
		IOntology o = cls.getOntology();
		for(IClass c: cls.getDirectSuperClasses()){
			if(c.hasDirectSuperClass(o.getClass(VALUES))){
				return c;
			}
		}
		return null;
	}

	/**
	 * is given ontology a read-only ontology.
	 * @param ont
	 * @return
	 */
	public static boolean isReadOnly(IOntology ont){
		if(readOnlyRegistry == null)
			readOnlyRegistry = new HashMap<IOntology, Boolean>();
		
		Boolean value = readOnlyRegistry.get(ont);
		if(value == null){
			//IF DB mode,
			if(DomainBuilder.getParameters().containsKey("repository.driver")){
				// now I need to do a dummy update that won't do anything, but raise
				// an exception in case of failure
				try{
					Statement st = getConnection().createStatement();
					st.executeUpdate("UPDATE "+ont.getLocation()+" SET facet = '' WHERE facet = 'yaba-daba-do'");
					value = Boolean.FALSE;
				}catch(Exception ex){
					value = Boolean.TRUE;
				}
			}else{
				value = Boolean.FALSE;
			}
			readOnlyRegistry.put(ont,value);
		}
		return value.booleanValue();
	}
	
	
	/**
	 * set read only status
	 * @param ont
	 */
	public static void setReadOnly(IOntology ont, boolean b){
		if(ont == null)
			return;
		
		if(readOnlyRegistry == null)
			readOnlyRegistry = new HashMap<IOntology, Boolean>();
		
		// put/overwrite balut
		readOnlyRegistry.put(ont,b);
	}
	
	/**
	 * get sql connection
	 * @return
	 */
	private static Connection getConnection() throws Exception{
		if(conn == null){
			Properties props = DomainBuilder.getParameters();
			String driver = props.getProperty("repository.driver");
			String url    = props.getProperty("repository.url");
			String user   = props.getProperty("repository.username");
			String pass   = props.getProperty("repository.password");
			Class.forName(driver).newInstance();
		    conn = DriverManager.getConnection(url,user,pass);
		}
		return conn;
	}
	
	
	
	/**
	 * sort class to have the most specific first
	 * @author Eugene Tseytlin
	 */
	public static class HierarchySort implements Comparator<IClass> {
		public int compare(IClass o1, IClass o2) {
			if(o1.hasSubClass(o2))
				return 1;
			if(o1.hasSuperClass(o2))
				return -1;
			return 0;
		}
		
	}
	
	
	
	/**
	 * is given class a feature or diagnoses, but not an attribute
	 * @param c
	 * @return
	 */
	public static boolean isNamedFeature(IClass c){
		return (isFeature(c) || isDisease(c)) && !isSystemClass(c) && (!isAttribute(c) || isLocation(c));
	}
	
	/**
	 * is given class a attribute, but not finding
	 * @param c
	 * @return
	 */
	public static boolean isNamedAttribute(IClass parent){
		return (isAttribute(parent) && (!isSystemClass(parent) || isValue(parent)) && !isAttributeCategory(parent) &&
				  (!(isFeature(parent) || isDisease(parent)) || isDirectLocation(parent)));
	}
	
	/**
	 * find a feature/disease inside a potential finding
	 * @param cls
	 * @return
	 *
	public static IClass findFeature(IClass cls){
		if(cls == null)
			return null;
		
		Queue<IClass> queue = new LinkedList<IClass>();
		queue.add(cls);
		
		// bredth first search
		while(!queue.isEmpty()){
			IClass c = queue.poll();
			if(isNamedFeature(c)){
				return c;
			}
			
			// look at next level
			for(IClass p: c.getDirectSuperClasses()){
				queue.add(p);
			}
			
		}
		return null;
	}
	*/
	
	/**
	 * find a list of attributes
	 * @param cls
	 * @retur
	 *
	public static List<IClass> findAttributes(IClass cls){
		return findAttributes(cls,findFeature(cls));
	}
	*/
	
	/**
	 * find a list of attributes
	 * @param cls
	 * @return
	 *
	public static List<IClass> findAttributes(IClass cls, IClass feature){
		List<IClass> list = new ArrayList<IClass>();
				
		// if class is null, or class is just a feature
		if(cls == null || cls.equals(feature))
			return list;
		
		Queue<IClass> queue = new LinkedList<IClass>();
		queue.add(cls);
		
		// bredth first search
		boolean stop = false;
		Set<IClass> set = new LinkedHashSet<IClass>();
		while(!queue.isEmpty()){
			IClass c = queue.poll();
			// if named attribute, add
			// if parent is feature, stop recursion further
			if(isNamedAttribute(c)){
				set.add(c);
			}else if(c.equals(feature)){
				stop = true;
			}
			
			// look at next level
			if(!stop && !isNamedAttribute(c)){
				for(IClass p: c.getDirectSuperClasses()){
					queue.add(p);
				}
			}
			
		}
		// now add the set to list
		list.addAll(set);
		return list;
	}
	*/
	
	/**
	 * find a feature/disease inside a potential finding
	 * @param cls
	 * @return
	 */
	public static IClass getFeature(IClass cls){
		if(cls == null)
			return null;
		
		// feature is the class itself by default
		IClass parent = cls;
		for(IClass p: cls.getDirectSuperClasses() ){
			// if direct super class is more general, then lets look further
			// once in a blue moon, we have a direct superclass not being in general form, but its parent is
			// Ex:  Infectious_Cause -> Bacterial_Infectious_Cause -> Actinomycotic_Infectious_Cause
			if(isFeature(p) && (isGeneralForm(p,cls,false) || isGeneralForm(getFeature(p),cls,false))){
				// reset feature if it is equal to class or it is NOT preposition
				if(parent.equals(cls) || isGeneralForm(p,cls,true))
					parent = getFeature(p);
				//break;
			}
		}
		return parent;
	}
	
	/**
	 * get a list of attributes/modifiers belong to this finding
	 * @param cls
	 * @return
	 */
	public static List<IClass> getAttributes(IClass cls){
		List<IClass> list = new ArrayList<IClass>();
		
		// if feature is itself, don't bother
		IClass feature = getFeature(cls);
		if(feature.equals(cls))
			return list;
		
		for(IClass p: cls.getSuperClasses()){
			if(isNamedAttribute(p)){
				// make sure that list contains only the most specific attributes
				// we don't want four AND number appearing here
				IClass torem = null;
				for(IClass c: list){
					if(p.hasSubClass(c))
						// do not insert a more general class
						torem = p;
					else if(p.hasSuperClass(c))
						// remove more general class
						torem = c;
				}
				// add new item, remove old item (or itself)
				list.add(p);
				list.remove(torem);
			}
		}
		// if feature is itself an attribute, we want to exclude it
		list.remove(feature);
		
		return list;
	}

	
	/**
	 * get a list of available attributes/modifiers for a given finding
	 * @param cls
	 * @return
	 */
	public static List<IClass> getPotentialAttributes(IClass cls){
		Set<IClass> list = new LinkedHashSet<IClass>();
		for(IClass p: getFeature(cls).getSubClasses()){
			list.addAll(getAttributes(p));
		}
		return new ArrayList<IClass>(list);
	}
	
	/**
	 * is parent a more general version of child?
	 * @param parent
	 * @param child
	 * @return
	 */
	public static String getGeneralForm(IClass parent, IClass child){
		// get words from parents and children
		String [] pnames = TextTools.getWords(UIHelper.getPrettyClassName(parent.getName()));
		String [] cnames = TextTools.getWords(UIHelper.getPrettyClassName(child.getName()));
		
		// normalize words
		List<String> plist = new ArrayList<String>();
		for(String s: pnames){
			plist.add(TextTools.stem(s));
		}
		// this is a map, to make lookup constant
		Map<String,String> clist = new LinkedHashMap<String,String>();
		for(String s: cnames){
			clist.put(TextTools.stem(s),"");
		}
		
		// now check for general form
		boolean general = true;
		for(String s: plist){
			general &= clist.containsKey(s);
		}
		
		// figure out offsets in parent list
		//int st,en;
		//for(st = 0; !clist.containsKey(plist.get(st));st ++);
		//for(en = plist.size()-1; !clist.containsKey(plist.get(en));en --);
		
		String s  = null;
		if(general){
			// find the offset of the first VALID parent words
			// Example: parent: Moderate Nutrophils child: Moderate Diffuse Neutrophils
			// basicly we need to avoid gaps
			//int z = 0;
			//do{
			s = child.getName().toLowerCase();
			int x = s.indexOf(plist.get(0));
			int y = s.indexOf("_",s.indexOf(plist.get(plist.size()-1)));
			//int x = s.indexOf(plist.get(st));
			//int y = s.indexOf("_",s.indexOf(plist.get(en)));
			// if last word in parent is also last in child
			if( y == -1)
				y = s.length();
			
			// now get a substring
			if(x > -1 && y > -1 && y <= s.length())
				s = child.getName().substring(x,y);
			
			// if we could not do it w/ parent go to more general parent
			if(!parent.getName().contains(s)){
				for(IClass c: parent.getDirectSuperClasses()){
					if(isFeature(c)){
						s = getGeneralForm(c,child);
						break;
					}
				}
			}
					
				//else
					//break;
			//}while(z < plist.size() && !parent.getName().contains(s));
		}
		return s;
	}
	
	/**
	 * is parent a more general version of child?
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean isGeneralForm(IClass parent, IClass child){
		return isGeneralForm(parent, child,true);
	}
	
	/**
	 * is parent a more general version of child?
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean isGeneralForm(IClass parent, IClass child, boolean filterPrepositionalFeature){
		// shortcut to save time
		if(child.getName().contains(parent.getName()) && !filterPrepositionalFeature)
			return true;
		
		
		// get words from parents and children
		String [] pnames = TextTools.getWords(UIHelper.getPrettyClassName(parent.getName()));
		String [] cnames = TextTools.getWords(UIHelper.getPrettyClassName(child.getName()));
		
		// normalize words
		List<String> plist = new ArrayList<String>();
		for(String s: pnames){
			plist.add(TextTools.stem(s));
		}
		// this is a map, to make lookup constant
		Map<String,String> clist = new LinkedHashMap<String,String>();
		for(String s: cnames){
			clist.put(TextTools.stem(s),"");
		}
		
		// now check for general form
		boolean general = true;
		for(String s: plist){
			general &= clist.containsKey(s);
		}
		
		// now check for prepositions in features
		// if we have a positive match
		if(general && filterPrepositionalFeature){
			// if in front of first parent word there is a preposition
			// then this maybe a false positive
			boolean preposition = false;
			for(String c: clist.keySet()){
				if(TextTools.isPrepositionWord(c)){
					preposition = true;
				}
				
				// as soon as we have a feature (contained in parent list)
				// check preposition, if preposition is true, then false positive
				if(plist.contains(c)){
					general = !preposition;
					break;
				}
			}
		}
		
		
		return general;
	}
	
	/**
	 * get best anatomical class
	 * @param ont
	 * @param c
	 * @return
	 */
	
	public static IClass getAnatomicalClass(IOntology ont, Concept concept){
		// by default, anything anatomic matches the entire ontology
		IClass a = ont.getClass(OntologyHelper.ANATOMIC_LOCATION);
		IProperty p = ont.getProperty(HAS_CONCEPT_CODE);
		IClass candidate = concept.getConceptClass();
		if(candidate != null){
			IOntology aont = candidate.getOntology();		
			// lets check further
			for(IClass c : a.getSubClasses()){
				String u = ""+c.getPropertyValue(p);
				IClass ac = aont.getClass(u);
				if(ac != null && (ac.equals(candidate) || ac.hasSubClass(candidate))){
					return c;
				}
			}
		}
		return a;
	}
	
	
	
	/**
	 * get comparator that will comare entries based ont he order 
	 * assigned in a given class
	 * @param cls
	 * @return
	 */
	public static Comparator getOrderComparator(IClass cls){
		// re-order the list
		final Map<String,Integer> map = new HashMap<String, Integer>();
		IOntology ont = cls.getOntology();
		for(Object o: cls.getPropertyValues(ont.getProperty(HAS_ORDER))){
			String [] s = (""+o).split("\\s*:\\s*");
			if(s.length == 2 && s[1].matches("\\d+")){
				map.put(s[0],new Integer(s[1]));
			}
		}
		return new Comparator() {
			public int compare(Object o1, Object o2) {
				if(o1 instanceof IResource && o2 instanceof IResource){
					IResource r1 = (IResource) o1;
					IResource r2 = (IResource) o2;
					int n1 = (map.containsKey(r1.getName()))?map.get(r1.getName()):0;
					int n2 = (map.containsKey(r2.getName()))?map.get(r2.getName()):0;
					return n1 - n2;
				}else if(o1 instanceof ConceptEntry && o2 instanceof ConceptEntry){
					ConceptEntry r1 = (ConceptEntry) o1;
					ConceptEntry r2 = (ConceptEntry) o2;
					int n1 = (map.containsKey(r1.getName()))?map.get(r1.getName()):0;
					int n2 = (map.containsKey(r2.getName()))?map.get(r2.getName()):0;
					return n1 - n2;
				}
				return 0;
			}
		};
		
	}
	
	
	/**
	 * check if local ontology is local and prompt user when appropriate
	 * @param ont
	 */
	public static void checkLocalOntology(IOntology ont){
		// if user is not connected, there is no point
		if(!Communicator.isConnected())
			return;
		IRepository r = DomainBuilder.getRepository();
		
		// now do the tricks
		if(ont instanceof POntology && r instanceof FileRepository){
			POntology o = (POntology) ont;
			//System.out.println(o.getURI()+" | "+o.getResourceProperties());
			String loc = o.getResourceProperties().getProperty("location");
			File l = new File(loc);
			if(l.exists() && !UIHelper.promptUseLocalCopy(o.getName()) && Communicator.exists(TextHelper.toURL(o.getURI()))){
				o.getResourceProperties().setProperty("location",""+o.getURI());
				o.setFilePath(((FileRepository) r).getProjectDirectory());
			}
		}
	}
	
	
	/**
	 * is finding in diagnostic rule of dx, on property prop
	 * @param fn
	 * @param prop
	 * @param dx
	 * @return
	 */
	public static boolean isFindingInDiagnosticRule(IClass fn,ILogicExpression rule){
		return isFindingInDiagnosticRule(fn, null, rule);
	}
	
	/**
	 * is finding in diagnostic rule of dx, on property prop
	 * @param fn
	 * @param prop
	 * @param dx
	 * @return
	 */
	public static boolean isFindingInDiagnosticRule(IClass fn, IProperty prop, ILogicExpression rule){
		for(Object obj: rule){
			if(obj instanceof ILogicExpression){
				if(isFindingInDiagnosticRule(fn, prop,(ILogicExpression) obj))
					return true;
			}else if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				if(prop == null || r.getProperty().equals(prop)){
					if(isFindingInDiagnosticRule(fn, prop,r.getParameter()))
						return true;
				}
			}else if(obj instanceof IClass){
				IClass c = (IClass) obj;
				if(c.equals(fn))
					return true;
				/*
				// we want to be a little bit more lenient
				// and allow say Blister in case to match Subepidermal_Blister in rule
				// In this scenario for WE are ok if rule finding is more specific
				// then what is in case as long as what is in case is at least as specific
				// as a feature
				IClass feature = OntologyHelper.getFeature(c);
				if(c.equals(fn) || c.hasSubClass(fn) ||
				   ((feature.equals(fn) || feature.hasSubClass(fn)) && c.hasSuperClass(fn)))
					return true;
				*/
			}
		}
		return false;
	}
	
	
	/**
	 * make a copy of an expression
	 * @param old
	 * @param ont
	 * @return
	 */
	public static ILogicExpression copy(ILogicExpression old, IOntology ont){
		ILogicExpression exp = ont.createLogicExpression();
		exp.setExpressionType(old.getExpressionType());
		
		for(Object o: old){
			if(o instanceof ILogicExpression){
				exp.add(copy((ILogicExpression)o,ont));
			}else if(o instanceof IRestriction){
				IRestriction or = (IRestriction) o;
				IRestriction nr = ont.createRestriction(or.getRestrictionType());
				nr.setProperty(or.getProperty());
				nr.setParameter(copy(or.getParameter(),ont));
				exp.add(nr);
			}else{
				exp.add(o);
			}
		}
		return exp;
	}
	
	/**
	 * get institution location from URI
	 * @param u
	 * @return
	 */
	public static String getInstitution(URI uri){
		Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
		Matcher m = pt.matcher(""+uri);
		if(m.matches()){
			String organ = m.group(2);
			String inst  = m.group(3);
			return inst;
		}
		return null;
	}
	
	/**
	 * get institution location from URI
	 * @param u
	 * @return
	 */
	public static String getOrgan(URI uri){
		Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
		Matcher m = pt.matcher(""+uri);
		if(m.matches()){
			String organ = m.group(2);
			String inst  = m.group(3);
			return organ;
		}
		return null;
	}
	
	
	
	public static void main(String [] args) throws Exception{
		IOntology ont = POntology.loadOntology("http://slidetutor.upmc.edu/curriculum/owl/skin/UPMC/Subepidermal.owl");
		IClass c = ont.getClass("Isolated_Neutrophils_in_Blister");
		System.out.println("feature of "+c+" is "+getFeature(c));
		c = ont.getClass("Isolated_Eosinophils_in_Blister");
		System.out.println("feature of "+c+" is "+getFeature(c));
		c = ont.getClass("Marked_Neutrophilic_Inflammatory_Infiltrate");
		System.out.println("feature of "+c+" is "+getFeature(c));
		c = ont.getClass("Subepidermal_Blister");
		System.out.println("feature of "+c+" is "+getFeature(c));
	}
}
