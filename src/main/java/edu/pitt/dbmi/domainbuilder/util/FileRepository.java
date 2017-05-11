package edu.pitt.dbmi.domainbuilder.util;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IOntologyException;
import edu.pitt.ontology.IReasoner;
import edu.pitt.ontology.IRepository;
import edu.pitt.ontology.IResource;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.ontology.protege.PReasoner;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.client.OntologyTerminology;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;


/**
 * manage OWL ontologies in specific directory OR on a server
 * where URI is a valid URL
 * @author tseytlin
 *
 */
public class FileRepository implements IRepository, PropertyChangeListener {
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private URL server;
	private File sourceDir,projectDir;
	private Map<URI,IOntology> ontologies;
	private Map<String,Terminology> terminologies;
	
	
	/**
	 * initialize file based repository
	 * @param dir
	 */
	public FileRepository(File dir){
		sourceDir = dir;
		projectDir = dir;
		setup();
	}
	
	/**
	 * initialize server based repository
	 * @param s
	 */
	public FileRepository(URL s){
		server = s;
		projectDir = new File(System.getProperty("user.home"),".ontologyRepository");
		setup();
	}
	
	/**
	 * initialize server based repository
	 * @param s
	 */
	public FileRepository(URL s, File d){
		server = s;
		projectDir = d;
		setup();
	}
	
	
	public boolean isServerMode(){
		return server != null;
	}
	
	private void setup(){
		if(projectDir.getName().endsWith(CURRICULUM_ROOT))
			projectDir = projectDir.getParentFile();
		
		// set custom plugin folder
		try{
			System.setProperty("protege.dir",projectDir.getAbsolutePath());
		}catch(Exception ex){
			// do nothing on security exception, not such a bid deal anyway
		}
		setupGlobalRepository(new File(projectDir,CURRICULUM_ROOT));
		
	}
	
	/**
	 * save global repository file
	 */
	private void setupGlobalRepository(File curriculum){
		File directory = new File(projectDir+
						File.separator+"plugins"+File.separator+
						"edu.stanford.smi.protegex.owl");
		//System.out.println(directory);
		// create if necessary
		if(!directory.exists())
			directory.mkdirs();
		
		// setup global repository
		File repository = new File(curriculum,KNOWLEDGE_FOLDER);
		try{
			File f = new File(directory,"global.repository");
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.write(repository.toURI()+"?forceReadOnly=true&Recursive=true\n");
			writer.close();
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}
	
	
	public void addOntology(IOntology ontology) {
		if(ontologies == null)
			ontologies = new HashMap<URI, IOntology>();
		ontologies.put(ontology.getURI(),ontology);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void addTerminology(Terminology terminology) {
		if(terminologies == null)
			terminologies = new HashMap<String, Terminology>();
		terminologies.put(terminology.getName(),terminology);
	}

	public IOntology createOntology(URI path) throws IOntologyException {
		POntology ont =  POntology.createOntology(path);
		ont.setFilePath(projectDir);
		ont.setRepository(this);
		ont.addPropertyChangeListener(this);
		return ont;
	}

	public void exportOntology(IOntology ontology, int format, OutputStream out) throws IOntologyException {
		ontology.write(out,format);
	}

	/**
	 * list content of 
	 * @param path
	 * @return
	 */
	private String [] list(String path){
		Set<String> list = new LinkedHashSet<String>();
		if(sourceDir != null && sourceDir.exists()){
			File f = new File(sourceDir,path);
			return listRecursive(f.getAbsolutePath(),"").toArray(new String [0]);
		}else if(server != null){
			// get list from server
			Map<String,String> map = new HashMap<String, String>();
			map.put("action","list");
			map.put("root",CURRICULUM_ROOT);
			map.put("path",path);
			map.put("recurse","true");
			try{
				String str = Communicator.doGet(server,map);
				Collections.addAll(list,str.split("\n"));
			}catch(IOException ex){
				ex.printStackTrace();
			}
			
			// get list from cache
			if(projectDir != null && projectDir.exists()){
				File f = new File(projectDir,CURRICULUM_ROOT+File.separator+path);
				list.addAll(listRecursive(f.getAbsolutePath(),""));
			}
		}
		return list.toArray(new String [0]);
	}
	
	/**
	 * list content of director
	 * @param filename
	 * @return
	 */
	private List<String> listRecursive(String filename, String prefix){
		File file = new File(filename);
		if(file.isDirectory()){
			List<String> buffer = new ArrayList<String>();
			for(File f: file.listFiles()){
				if(!f.isHidden() && !f.getName().startsWith(".")){
					if(f.isDirectory()){
						buffer.addAll(listRecursive(f.getAbsolutePath(),prefix+f.getName()+"/"));
					}else
						buffer.add(prefix+f.getName());
				}
			}
			return buffer;
		}
		return Collections.EMPTY_LIST;
	}
	
	/**
	 * get a list of ontologies
	 */
	public IOntology[] getOntologies() {
		if(ontologies == null){
			try{
				for(String u : list(KNOWLEDGE_FOLDER)){
					if(u.endsWith(OWL_SUFFIX)){
						POntology ont = POntology.loadOntology(""+TextHelper.toURI(DEFAULT_BASE_URI+u));
						ont.setRepository(this);
						//ont.addPropertyChangeListener(this);
						
						// look for ontology in local directory
						if(sourceDir != null){
							String path = sourceDir.getAbsolutePath()+"/"+KNOWLEDGE_FOLDER+"/"+u;
							path = path.replace('/',File.separatorChar);
							ont.getResourceProperties().setProperty("location",path);
						}else{
							// check the cached version first
							String path = projectDir.getAbsolutePath()+"/"+CURRICULUM_ROOT+"/"+KNOWLEDGE_FOLDER+"/"+u;
							path = path.replace('/',File.separatorChar);
							File f = new File(path);
							
							// if local version exists, then 
							if(f.exists())
								ont.getResourceProperties().setProperty("location",path);
							else
								ont.setFilePath(projectDir);
						}
						
						addOntology(ont);
					}
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		return (new TreeSet<IOntology>(ontologies.values())).toArray(new IOntology [0]);
	}

	
	/**
	 * get project directory for the local cache.
	 * @return
	 */
	public File getProjectDirectory(){
		return projectDir;
	}
	

	public String getDescription() {
		return "File/URL OWL repository";
	}

	public String getName() {
		return "File Repository";
	}


	/**
	 * get ontologies that are loaded in repository
	 * @return
	 */
	public IOntology [] getOntologies(String name){
		ArrayList<IOntology> onts = new ArrayList<IOntology>();
		for(IOntology o : getOntologies()){
			if(o.getURI().toString().contains(name)){
				onts.add(o);
			}
		}
		return onts.toArray(new IOntology [0]);
	}
	
	
	
	public IOntology getOntology(URI name) {
		if(ontologies == null){
			getOntologies();
		}
		IOntology ont = ontologies.get(name);
		// if ontology is not in a list, try to load it
		// directly
		/*
		if(ont == null){
			try {
				// check if there is a local version of this file
				File f = new File(projectDir,name.getPath().replace('/',File.separatorChar));
				ont = POntology.loadOntology(""+name);
				// set location
				if(f.exists())
					((POntology) ont).getResourceProperties().setProperty("location",f.getAbsolutePath());
				ont.addPropertyChangeListener(this);
				ont.setRepository(this);
				((POntology) ont).setFilePath(projectDir);
				
			} catch (IOntologyException e) {
				e.printStackTrace();
			}
		}*/
		return ont;
	}

	/**
	 * get reasoner that can handle this ontology
	 * you can configure the type of reasoner by 
	 * specifying reasoner class and optional URL
	 * in System.getProperties()
	 * reasoner.class and reasoner.url
	 * @return null if no reasoner is available
	 */
	public IReasoner getReasoner(IOntology ont){
		if(ont instanceof POntology){
			return new PReasoner((POntology)ont);
		}
		return null;
	}

	/**
	 * convinience method
	 * get resource from one of the loaded ontologies
	 * @param path - input uri
	 * @return resource or null if resource was not found
	 */
	public IResource getResource(URI path){
		String uri = ""+path;
		int i = uri.lastIndexOf("#");
		uri = (i > -1)?uri.substring(0,i):uri;
		// get ontology
		IOntology ont = getOntology(URI.create(uri));
		// if ontology is all you want, fine Girish
		if(i == -1)
			return ont;
		// 
		if(ont != null){
			return ont.getResource(""+path);
		}
		return null;
	}

	public Terminology[] getTerminologies() {
		if(terminologies == null)
			terminologies = new HashMap<String, Terminology>();
		return terminologies.values().toArray(new Terminology [0]);
	}

	public Terminology getTerminology(String path) {
		if(terminologies == null)
			getTerminologies();
		
		// check list first
		if(terminologies.containsKey(path))
			return terminologies.get(path);
		// get ontology terminology if possible
		IOntology ont = null;
		try{
			ont = getOntology(URI.create(path));
		}catch(Exception ex){
			//if failed for whatever reason, then we return null
		}
		return (ont != null)?new OntologyTerminology(ont):null;
	}

	public boolean hasOntology(String name) {
		if(ontologies == null){
			getOntologies();
		}
		/*
		if(ontologies.containsKey(name) || getOntologies(name).length > 0)
			return true;
		
		// see if maybe there is a valid file somewhere in prject dir
		try{
			URL u = new URL(name);
			File f = new File(projectDir,u.getPath().replace('/',File.separatorChar));
			return f.exists();
		}catch(Exception ex){}
		
		return false;
		*/
		return ontologies.containsKey(name) || getOntologies(name).length > 0;
	}

	public IOntology importOntology(URI path) throws IOntologyException {
		POntology ont = null;
		// load ontology
		try{
			ont = POntology.loadOntology(path);
			ont.setFilePath(projectDir);
			ont.setRepository(this);
			ont.save();
			
			// add ontologies to a list
			addOntology(ont);
			IOntology [] o = ont.getImportedOntologies();
			for(int i=0;i<o.length;i++){
				if(!hasOntology(o[i].getName()))
					addOntology(o[i]);
			}
		}catch(Exception ex){
			throw new IOntologyException("problem loading "+path,ex);
		}
		return ont;
	}

	public void importOntology(IOntology ont) throws IOntologyException {
		// load ontology
		try{
			ont.setRepository(this);
			
			// save to database if possible
			if(ont instanceof POntology){
				((POntology)ont).setFilePath(projectDir);
				ont.save();
			}
			
			// add ontologies to a list
			addOntology(ont);
			
			// add imported ontologies
			IOntology [] o = ont.getImportedOntologies();
			for(int i=0;i<o.length;i++){
				if(!hasOntology(o[i].getName()))
					addOntology(o[i]);
			}
		}catch(Exception ex){
			ex.printStackTrace();
			throw new IOntologyException("problem importing "+ont.getURI(),ex);
		}
	}

	public void removeOntology(IOntology ontology) {
		if(ontologies == null)
			getOntologies();
		ontologies.remove(ontology.getURI());
		ontology.removePropertyChangeListener(this);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
		
	}

	public void removeTerminology(Terminology terminology) {
		if(terminologies == null)
			getTerminologies();
		terminologies.remove(terminology.getName());
	}
	

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if(IOntology.ONTOLOGY_SAVED_EVENT.equals(prop)){
			save((IOntology)evt.getSource());
		}else if(IOntology.ONTOLOGY_LOADED_EVENT.equals(prop)){
		
		}
	}
	
	/**
	 * ontology was saved
	 * @param ont
	 */
	private void save(IOntology ont){
		//System.out.println("saved "+ont);
	}
	

	public IOntology getOntology(URI name, String version) {
		return getOntology(name);
	}

	public String[] getVersions(IOntology ont) {
		return (ont != null)?new String [] {ont.getVersion()}:new String [0];
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		//FileRepository repository = new  FileRepository(new File("/home/tseytlin/Work/curriculum"));
		FileRepository repository = new  FileRepository(new URL(DEFAULT_FILE_MANAGER_SERVLET));
		
		//RepositoryManager manager = new RepositoryManager();
		//manager.start(repository);
		
		// load old
		//IOntology ont = repository.getOntology(URI.create("http://slidetutor.upmc.edu/curriculum/owl/skin/UPMC/SubepidermalSuperInstances.owl"));
		//ont.load();
		//ont.save();
		
		// load new
		//IOntology nont = repository.createOntology(URI.create("http://slidetutor.upmc.edu/curriculum/owl/skin/UPMC/SubepidermalSuperInstances.owl"));
		//nont.addImportedOntology(ont);
		//nont.save();
		
		/*
		IClass c = ont.getClass("Blister");
		c.createSubClass("Funny_Blister");
		ont.save();
		ont.reload();
		*/
		/*
		IOntology ont = repository.createOntology(URI.create("http://www.ontologies.com/test/Test.owl"));
		ont.load();
		ont.createClass("TEST");
		ont.save();
		ont.reload();
		*/
		
	}

}
