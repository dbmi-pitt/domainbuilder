package edu.pitt.dbmi.domainbuilder.beans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;
import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.util.MutableListModel;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.OrderedMap;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.ontology.*;

/**
 * this object encompases a tutor case 
 * @author tseytlin
 */
public class CaseEntry implements Serializable{
	private IInstance instance;
	private OrderedMap<String,SlideEntry> slides;
	private Map<String,OrderedMap<IClass,ConceptEntry>> concepts;
	private Map<String,MutableListModel> models;
	//private Map<String,MutableListModel> models;
	private SlideEntry currentSlide,primarySlide;
	private String report,name,status;
	private boolean modified,newcase,loading;
	private Properties meta;
	private StringBuffer warnings;
	private Set<String> parts;
	
	/**
	 * initialize stuff
	 */
	public CaseEntry(IInstance inst){
		load(inst);
	}
	
	public void load(IInstance inst){
		this.instance = inst;
		slides = new OrderedMap<String,SlideEntry>();
		concepts = initConceptMap();
		//models = new LinkedHashMap<String,MutableListModel>(5);
		loadInstance(instance);
		newcase = inst.getName().equals(OntologyHelper.NEW_CASE);
		name = inst.getName();
	}
	
	public Properties getProperties(){
		if(meta == null)
			meta = new Properties();
		return meta;
	}
	
	/**
	 * case name
	 */
	public String toString(){
		return name;
	}
	
	/**
	 * get a set of parts in this case
	 * @return
	 */
	public Set<String> getParts(){
		if(parts == null)
			parts = new LinkedHashSet<String>();
		return parts;
	}
	
	/**
	 * get ontology
	 * @return
	 */
	public IOntology getOntology(){
		return instance.getOntology();
	}
	
	
	/**
	 * THIS constructor should only be used if you know what you are doing
	 * normal case NEEDS an instance, this is used to save "empty" cases
	 * that only have annotations
	 */
	public CaseEntry(String name){
		this.name = name;
		slides = new OrderedMap<String,SlideEntry>();
		concepts = initConceptMap();
	}
	
	private Map<String,OrderedMap<IClass,ConceptEntry>> initConceptMap(){
		Map<String,OrderedMap<IClass,ConceptEntry>> concepts = 
			Collections.synchronizedMap(new LinkedHashMap<String,OrderedMap<IClass,ConceptEntry>>(5));
		concepts.put(DISEASES,new OrderedMap<IClass,ConceptEntry>());
		concepts.put(DIAGNOSTIC_FEATURES,new OrderedMap<IClass,ConceptEntry>());
		concepts.put(PROGNOSTIC_FEATURES,new OrderedMap<IClass,ConceptEntry>());
		concepts.put(CLINICAL_FEATURES,new OrderedMap<IClass,ConceptEntry>());
		concepts.put(ANCILLARY_STUDIES,new OrderedMap<IClass,ConceptEntry>());
		concepts.put(RECOMMENDATIONS,new OrderedMap<IClass,ConceptEntry>());
		return concepts;
	}
	
	/*
	private void checkConceptMap(){
		if(!concepts.containsKey(DISEASES))
			concepts.put(DISEASES,new OrderedMap<IClass,ConceptEntry>());
		if(!concepts.containsKey(DIAGNOSTIC_FEATURES));
			concepts.put(DIAGNOSTIC_FEATURES,new OrderedMap<IClass,ConceptEntry>());
		if(!concepts.containsKey(PROGNOSTIC_FEATURES));
			concepts.put(PROGNOSTIC_FEATURES,new OrderedMap<IClass,ConceptEntry>());
		if(!concepts.containsKey(CLINICAL_FEATURES));
			concepts.put(CLINICAL_FEATURES,new OrderedMap<IClass,ConceptEntry>());
		if(!concepts.containsKey(ANCILLARY_STUDIES));
			concepts.put(ANCILLARY_STUDIES,new OrderedMap<IClass,ConceptEntry>());
	}*/
	
	/**
	 * get warnings related to saving and loading this case
	 * @return
	 */
	public String getWarnings(){
		if(warnings != null)
			return warnings.toString();
		return "";
	}
	
	
	/**
	 * were there any warnings generated for this case
	 * @return
	 */
	public boolean hasWarnings(){
		return warnings != null && warnings.length() > 0;
	}
	
	/**
	 * load instance values
	 * @param inst
	 */
	private void loadInstance(IInstance inst){
		IOntology ont = inst.getOntology();
		loading = true;
		// load report
		String text = (String) inst.getPropertyValue(ont.getProperty(HAS_REPORT));
		report = (text != null)?text:"";
		
		// load case status
		String st = (String) inst.getPropertyValue(ont.getProperty(HAS_STATUS));
		if(st != null)
			status = st;
		
		// load tags
		List<String> tags = new ArrayList<String>();
		for(Object o: inst.getPropertyValues(ont.getProperty(HAS_TAG))){
			String [] s = (""+o).split("=");
			if(s.length == 2){
				getProperties().setProperty(s[0].trim(),s[1].trim());
			}else{
				tags.add(s[0].trim());
			}
		}
		if(!tags.isEmpty())
			getProperties().setProperty(HAS_TAG,tags.toString());
		
		
		// load slides 
		for(Object s: inst.getPropertyValues(ont.getProperty(HAS_SLIDE))){
			addSlide(new SlideEntry(""+s));
		}
		
		// update references to KnowledgeBase
		IOntology kb = OntologyHelper.getKnowledgeBase(ont);
		
		
		// put diagnostic entries
		for(String p: new String [] {
				HAS_FINDING,
				HAS_NO_FINDING}){
			for(Object o: inst.getPropertyValues(ont.getProperty(p))){
				if(o instanceof IInstance){
					ConceptEntry c = new ConceptEntry((IInstance) o);
					c.updateReference(kb);
					concepts.get(DIAGNOSTIC_FEATURES).
					put(c.getConceptClass(),c);
				}
			}
		}
		// put prognostic & other entries
		String [][] types = new String [][] {{HAS_PROGNOSTIC,PROGNOSTIC_FEATURES},
											 {HAS_CLINICAL,  CLINICAL_FEATURES},
											 {HAS_ANCILLARY,ANCILLARY_STUDIES}};
		for(int i=0;i<types.length;i++){
			for(Object o: inst.getPropertyValues(
					ont.getProperty(types[i][0]))){
				if(o instanceof IInstance){
					ConceptEntry c = new ConceptEntry((IInstance) o);
					c.updateReference(kb);
					if(c.getConceptClass() != null){
						concepts.get(types[i][1]).
						put(c.getConceptClass(),c);
					}
				}
			}
		}
		
		// put diagnoses
		for(Object o: inst.getDirectTypes()){
			if(o instanceof IClass){
				IClass c = (IClass) o;
				if(c.hasSuperClass(ont.getClass(DISEASES))){
					ConceptEntry d = new ConceptEntry(c);
					d.updateReference(kb);
					concepts.get(DISEASES).
					put(c,d);
				}
			}
		}
		modified = false;
		loading = false;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	
	
	/**
	 * update references to all instances in this case
	 * @param ont
	 */
	public void updateReferences(IOntology ont){
		instance = ont.getInstance(instance.getName());
		System.out.println("updating reference "+instance);
		if(instance == null){
			// create new instace
			instance = ont.getClass(OntologyHelper.CASES).createInstance(OntologyHelper.NEW_CASE);
		}
		
		// update stuff in maps 
		for(String type: concepts.keySet()){
			OrderedMap<IClass,ConceptEntry> map = concepts.get(type);
			//int i = 0;
			for(IClass c : new HashSet<IClass>(map.keySet())){
				ConceptEntry e = map.get(c);
				e.updateReference(ont);
				// if item was removed, delete it
				if(e.isInconsistent())
					map.remove(c);
			}
		}
	
	}
	
	/**
	 * was case modified
	 */
	public boolean isModified(){
		return modified;
	}
	
	/**
	 * new case is something that has not been saved for the first time
	 * @return
	 */
	public boolean isNewCase(){
		return newcase;
	}
	
	/**
	 * get info from case
	 * @param field
	 */
	public String getReportSection(String field){
		return "no info";
	}
	
	/**
	 * get entire report
	 * @param field
	 */
	public String getReport(){
		return report;
	}
	

	/**
	 * set report text
	 * @param report
	 */
	public void setReport(String report) {
		this.report = report;
		modified = true;
	}

	
	
	/**
	 * @return the name
	 */
	public String getName() {
		if(name != null)
			return name;
		return (instance != null)?instance.getName():NEW_CASE;
	}
	
	
	/**
	 * figure out the organ
	 * @return
	 */
	public String getOrgan(){
		Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
		Matcher mt = pt.matcher(""+instance.getOntology().getURI());
		if(mt.matches())
			return mt.group(2);
		return "unidentified";
	}
	
	/**
	 * figure out the organ
	 * @return
	 */
	public String getInstitution(){
		Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
		Matcher mt = pt.matcher(""+instance.getOntology().getURI());
		if(mt.matches())
			return mt.group(3);
		return "unidentified";
	}
	
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		if(instance != null && !instance.getName().equals(name)){
			instance.setName(name);
			
			IOntology ont = instance.getOntology();
			IInstance inst = instance;
			instance = ont.getInstance(name);
			
			// WTF!!! I don't know why, but once upon a time the instance is NULL
			if(instance == null)
				instance = inst;
			
			
			// create some meta-data
			try{
				if(instance.getLabels().length == 0)
					instance.addLabel(name);
				if(instance.getVersion() == null)
					instance.addVersion("1.0");
				IProperty p = ont.getProperty(OntologyHelper.CREATOR);
				if(p != null && instance.getPropertyValues(p).length == 0){
					if(DomainBuilder.getParameters().containsKey("repository.username")){
						String user = DomainBuilder.getParameter("repository.username");
						if(DomainBuilder.getParameters().containsKey("repository.institution")){
							user += " ("+DomainBuilder.getParameter("repository.institution")+")";
						}
						instance.setPropertyValue(p,user);
					}
				}
			}catch(Exception ex){
				System.out.println("WTF!!! Instance not ready for meta-data");
			}
		
		}else{
			// address the problem of LOST cases .......
			IClass cls = instance.getOntology().getClass(CASES);
			if(!instance.hasType(cls))
				instance.addType(cls);
		}
		this.name = name;
	}
	
	/**
	 * @return the slides
	 */
	public Collection<SlideEntry> getSlides() {
		return slides.values();
	}
	
	/**
	 * @return the slides
	 */
	public void setSlides(Collection<SlideEntry> list) {
		slides.clear();
		for(SlideEntry s : list){
			slides.put(s.getSlideName(),s);
		}
	}
	
	/**
	 * get specific slide 
	 * @param name
	 * @return
	 */
	public SlideEntry getSlide(String name){
		return slides.get(name);
	}
	
	/**
	 * has slide
	 * @param name
	 * @return
	 */
	public boolean hasSlide(String name){
		return slides.containsKey(name);
	}
	
	/**
	 * get specific slide 
	 * @param name
	 * @return
	 */
	public void addSlide(SlideEntry s){
		modified = true;
		// first slide should be primary slide
		if(slides.isEmpty()){
			s.setPrimarySlide(true);
			//primarySlide = s;
			if(getName().equals(NEW_CASE) && !loading)
				setName(suggestCaseName(s.getSlideName()));
		}
		slides.put(s.getSlideName(),s);
	}
	
	
	/**
	 * get specific slide 
	 * @param name
	 * @return
	 */
	public void removeSlide(SlideEntry s){
		slides.remove(s.getSlideName());
	}
	
	/**
	 * @return the currentSlide
	 */
	public SlideEntry getCurrentSlide() {
		return currentSlide;
	}
	
	/**
	 * @return the currentSlide
	 */
	public SlideEntry getPrimarySlide() {
		return primarySlide;
	}
	
	/**
	 * @param currentSlide the currentSlide to set
	 */
	public void setCurrentSlide(SlideEntry currentSlide) {
		this.currentSlide = currentSlide;
	}
	
	
	/**
	 * list model for easier management of slides
	 * @author tseytlin
	 */
	public MutableListModel createSlideListModel(){
		MutableListModel model =  new MutableListModel(){
			public void addElement(Object obj){
				if(obj instanceof SlideEntry){
					addSlide((SlideEntry)obj);
					int index = getSize();
					fireIntervalAdded(this, index, index);
				}
			}
			public boolean removeElement(Object obj){
				if(obj instanceof SlideEntry){
					int index = slides.getValues().indexOf(obj);
					removeSlide((SlideEntry)obj);
					fireIntervalRemoved(this,index,index);
				}
				return true;
			}
			public void setElementAt(Object obj, int index){
				// add new element to the list
				if(obj instanceof SlideEntry){
					// replaces an element
					SlideEntry s = (SlideEntry) obj;
					slides.set(s.getSlideName(),s,index);
					fireContentsChanged(this, index, index);
				}
			}
			public void insertElementAt(Object obj, int index) {
				if(obj instanceof SlideEntry){
					SlideEntry s = (SlideEntry)obj;
					slides.insert(s.getSlideName(),s,index);
					fireIntervalAdded(this,index,index);
				}	
			}
			public void removeAllElements() {
				int index1 = getSize()-1;
				slides.clear();
				fireIntervalRemoved(this,0,index1);
			}
			public int getSize(){
				return slides.getValues().size();
			}
			public boolean containsElement(Object obj){
				return slides.getValues().contains(obj);
			}
			public Object getElementAt(int i){
				return (i<getSize() && i >= 0)?slides.getValues().get(i):null;
			}
			public void sort(){
				slides.sort();
			}
		};	
		modified = false;
		return model;
	}
	
	/**
	 * add concept to map
	 * @param map
	 * @param e
	 * @return
	 */
	private boolean addConcept(OrderedMap<IClass,ConceptEntry> map, Object obj){
		return addConcept(map,obj,-1,false);
	}
	
	
	/**
	 * add concept to map
	 * @param map
	 * @param e
	 * @return
	 */
	private boolean addConcept(OrderedMap<IClass,ConceptEntry> map, Object obj,int x, boolean insert){
		synchronized (map){
			ConceptEntry e = null;
			if(obj instanceof ConceptEntry)
				e = (ConceptEntry) obj;
			//else if(obj instanceof IInstance)
			//	e = new ConceptEntry((IInstance)obj);
			//else if(obj instanceof IClass)
			//	e = new ConceptEntry((IClass)obj);
			if(e == null)
				return false;
			
			// if already there then, don't do anything
			if(map.containsKey(e.getConceptClass())){
				map.get(e.getConceptClass()).copyFrom(e);
				return false;
			}
			// check for subsumption for NON diseases
			if(!e.isDisease()){
				for(IClass c: map.keySet()){
					// make sure we don't subsume absent/present items
					// don't bother checking for EQ cause, they are not diseases
					// && !e.getConceptClass().hasEquivalentClass(c)
					if(e.isAbsent() == map.get(c).isAbsent() && !OntologyHelper.isSystemClass(c)){
						if(e.getConceptClass().hasSuperClass(c)){
							// remove concept entry
							x = map.getKeys().indexOf(c);
							ConceptEntry old = map.get(c);
							e.copyFrom(old);
							insert = false;
							break;
						}else if(e.getConceptClass().hasSubClass(c)){
							ConceptEntry old = map.get(c);
							old.copyFrom(e);
							old.flash();
							return false;
						}
					}
				}
			}
			// simply add this concept
			if(x == -1){
				map.put(e.getConceptClass(),e);
			}else {
				if(insert)
					map.insert(e.getConceptClass(),e,x);
				else 
					map.set(e.getConceptClass(),e,x);
			}
			modified = true;
			return true;
		}
	}
	
	/**
	 * remove concept
	 * @param map
	 * @param obj
	 */
	private boolean removeConcept(OrderedMap<IClass,ConceptEntry> map, Object obj){
		synchronized (map) {
			if(obj instanceof ConceptEntry){
				IClass cls = ((ConceptEntry)obj).getConceptClass();
				return cls == null || map.remove(cls) != null;
			}
			return false;
		}
	}
	/**
	 * list model for easier management of slides
	 * @author tseytlin
	 */
	public MutableListModel createConceptListModel(String root){
		final String rootCls = root;
		
		// put in entry for this root
		if(!concepts.containsKey(root))
			concepts.put(root, new OrderedMap<IClass,ConceptEntry>());
		
		// init models
		if(models == null)
			models = new HashMap<String, MutableListModel>();
		
		// create custom model
		MutableListModel model = new MutableListModel(){
			public void addElement(Object obj){
				if(addConcept(concepts.get(rootCls),obj)){
					if(obj instanceof ConceptEntry)
						addConceptClass((ConceptEntry) obj);
				}
				int index = getSize();
				fireIntervalAdded(this, index, index);
			}
			public boolean containsElement(Object obj){
				return concepts.get(rootCls).getValues().contains(obj);
			}
			public boolean removeElement(Object obj){
				int index = concepts.get(rootCls).getValues().indexOf(obj);
				if(removeConcept(concepts.get(rootCls),obj)){
					if(obj instanceof ConceptEntry)
						removeConceptClass((ConceptEntry) obj);
					fireIntervalRemoved(this,index,index);
					return true;
				}
				return false;
			}
			public void setElementAt(Object obj, int index){
				// get old element
				Object old = getElementAt(index);
				// add new element to the list
				if(addConcept(concepts.get(rootCls),obj,index,false)){
					if(obj instanceof ConceptEntry)
						addConceptClass((ConceptEntry) obj);
					//remove old one
					if(old != null && obj instanceof ConceptEntry){
						removeConceptClass((ConceptEntry) obj);
					}
				}
				fireContentsChanged(this, index, index);
			}
			public void insertElementAt(Object obj, int index) {
				if(addConcept(concepts.get(rootCls),obj,index,true)){
					if(obj instanceof ConceptEntry)
						addConceptClass((ConceptEntry) obj);
					fireIntervalAdded(this,index,index);
				}	
			}
			public void removeAllElements() {
				int index1 = getSize()-1;
				if(index1 < 0)
					return;
				concepts.get(rootCls).clear();
				fireIntervalRemoved(this,0,index1);
			}
			public int getSize(){
				return concepts.get(rootCls).getValues().size();
			}
			public Object getElementAt(int i){
				return (i<getSize() && i >= 0)?concepts.get(rootCls).getValues().get(i):null;
			}	
			public void sort(){
				concepts.get(rootCls).sort();
			}
			
		};
		
		// add this model to map
		models.put(root,model);
		
		modified = false;
		return model;
	}

	

	/**
	 * @return the instance
	 */
	public IInstance getInstance() {
		return instance;
	}

	/**
	 * get diagnoses
	 * @return
	 */
	public Collection<ConceptEntry> getDiagnoses(){
		return concepts.get(DISEASES).getValues();
	}
	
	
	/**
	 * get concept entry for some string
	 * @param name
	 * @return
	 */
	public ConceptEntry getConceptEntry(String name){
		IClass cls = instance.getOntology().getClass(name);
		if(cls != null){
			for(String key: concepts.keySet()){
				if(concepts.get(key).containsKey(cls))
					return concepts.get(key).get(cls);
			}
		}
		return null;
	}
	
	public ConceptEntry [] getConceptEntries(){
		ArrayList<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(String key: concepts.keySet()){
			list.addAll(concepts.get(key).getValues());
		}
		return list.toArray(new ConceptEntry [0]);
	}
	
	public Collection<ConceptEntry> getConceptEntries(String key){
		if(concepts.containsKey(key))
			return concepts.get(key).getValues();
		return Collections.EMPTY_LIST;
	}
	
	/**
	 * get property for class
	 * @param cls
	 * @return
	 *
	private IProperty getCaseProperty(IClass cls, boolean absent){
		IOntology o = cls.getOntology();
		// skip diseases and select property
		if(cls.hasSuperClass(o.getClass(DISEASES))){
			return null;
		}else if(cls.hasSuperClass(o.getClass(DIAGNOSTIC_FEATURES))){
			if(absent){
				return o.getProperty(HAS_NO_FINDING);
			}else
				return o.getProperty(HAS_FINDING);
		}else if(cls.hasSuperClass(o.getClass(PROGNOSTIC_FEATURES))){
			return o.getProperty(HAS_PROGNOSTIC);
		}else if(cls.hasSuperClass(o.getClass(CLINICAL_FEATURES))){
			return o.getProperty(HAS_CLINICAL);
		}else if(cls.hasSuperClass(o.getClass(ANCILLARY_STUDIES))){
			return o.getProperty(HAS_ANCILLARY);
		}
		return null;
	}
	*/
	/**
	 * get property for class
	 * @param cls
	 * @return
	 */
	private List<IProperty> getCaseProperties(IClass cls, boolean absent){
		List<IProperty> props = new ArrayList<IProperty>();
		IOntology o = cls.getOntology();
		// skip diseases and select property
		if(cls.hasSuperClass(o.getClass(DISEASES))){
			return props;
		}
		
		if(cls.hasSuperClass(o.getClass(DIAGNOSTIC_FEATURES))){
			if(absent){
				props.add(o.getProperty(HAS_NO_FINDING));
			}else
				props.add(o.getProperty(HAS_FINDING));
		}
		
		if(cls.hasSuperClass(o.getClass(PROGNOSTIC_FEATURES))){
			props.add(o.getProperty(HAS_PROGNOSTIC));
		}
		
		if(cls.hasSuperClass(o.getClass(CLINICAL_FEATURES))){
			if(absent){
				props.add(o.getProperty(HAS_NO_FINDING));
			}else
				props.add(o.getProperty(HAS_CLINICAL));
		}
		
		if(cls.hasSuperClass(o.getClass(ANCILLARY_STUDIES))){
			props.add(o.getProperty(HAS_ANCILLARY));
		}
		return props;
	}
	
	
	
	/**
	 * get case properties name for given type
	 * @param type
	 * @return
	 */
	private String [] getCaseProperties(String type){
		if(type.equals(DIAGNOSTIC_FEATURES))
			return new String [] { HAS_FINDING, HAS_NO_FINDING};
		if(type.equals(PROGNOSTIC_FEATURES))
			return new String [] { HAS_PROGNOSTIC};
		if(type.equals(CLINICAL_FEATURES))
			return new String [] { HAS_CLINICAL,HAS_NO_FINDING};
		if(type.equals(ANCILLARY_STUDIES))
			return new String [] { HAS_ANCILLARY};
		return new String [0];
	}
	
	/**
	 * add concept class to case instance
	 * @param cls
	 * @param absent
	 */
	public void addConceptClass(ConceptEntry e){
		IClass cls = instance.getOntology().getClass(e.getConceptClass().getName());
		
		// WOW, if class is NULL, what should we do?
		if(cls == null){
			e.setInconsistent(true);
			return;
		}
		
		boolean absent = e.isAbsent();
		double num = e.getNumericValue();
		IOntology o = cls.getOntology();
		List<IProperty> props = getCaseProperties(cls, absent);
		// skip diseases and select property
		if(props.isEmpty())
			return;
		
		// get an instance
		IInstance inst = OntologyHelper.getInstance(cls,e.getInstanceName());
		
		// add to case instance
		if(absent){
			inst.setPropertyValue(o.getProperty(IS_ABSENT),Boolean.TRUE);
		}
		if(e.hasNumericValue()){
			inst.setPropertyValue(o.getProperty(HAS_NUMERIC_VALUE),new Float(num));
		}
		if(e.hasResourceLink()){
			inst.setPropertyValue(o.getProperty(HAS_RESOURCE_LINK),e.getResourceLink());
		}
		
		for(IProperty prop: props){
			if(!instance.hasPropetyValue(prop, inst)){
				instance.addPropertyValue(prop,inst);
			}
			//System.out.println("add "+prop+" "+inst+" to "+instance+" "+Arrays.toString(instance.getPropertyValues(prop)));
		}
	}
	
	/**
	 * remove concept class
	 * @param e
	 */
	public void removeConceptClass(ConceptEntry e){
		if(e.getConceptClass() == null)
			return;
		
		IClass cls = instance.getOntology().getClass(e.getConceptClass().getName());
		
		List<IProperty> props = getCaseProperties(cls,e.isAbsent());
		// skip diseases and select property
		if(props.isEmpty())
			return;
		
		IInstance [] insts = cls.getDirectInstances();
		
		// add to case instance
		for(IProperty prop: props){
			for(int i=0;i<insts.length;i++){
				instance.removePropertyValue(prop,insts[i]);
			}
		}
	}
	
	
	/**
	 * remove diagnostic finding
	 * @param entry
	 *
	public void removeConceptClasses(String root){
		IOntology o = instance.getOntology();
		if(PROGNOSTIC_FEATURES.equals(root)){
			instance.removePropertyValues(o.getProperty(HAS_PROGNOSTIC));
		}else if(DIAGNOSTIC_FEATURES.equals(root)){
			instance.removePropertyValues(o.getProperty(HAS_NO_FINDING));
			instance.removePropertyValues(o.getProperty(HAS_FINDING));
		}
	}*/
	
	/**
     * Suggest a name for a case that is being saved
     * @return
     */
    private String suggestCaseName(String name){
    	Pattern p = Pattern.compile("([A-Za-z]+_\\d+).*\\.\\w+");
    	Matcher m = p.matcher(name);
    	if(m.matches()){
    		name = m.group(1);
    	}else{
    		int i = name.lastIndexOf(".");
    		if(i > -1){
    			name = OntologyHelper.getClassName(name.substring(0,i));
    		}
    	}
    	return name;
    }
    
    /**
     * delete case entry
     */
    public void delete(){
    	instance.delete();
    }
    
    
    /**
     * save case instance
     */
    public void save(){
    	IOntology o = instance.getOntology();
    	
    	// save stuff into properties
    	for(IProperty p :instance.getProperties()){
    		if(p.isAnnotationProperty()){
    			Object [] vals = instance.getPropertyValues(p);
    			String str = ""; 
    			if(vals.length == 1)
    				str = ""+vals[0];
    			else
    				str = Arrays.toString(vals);
    			getProperties().setProperty(p.getName(),str);
    			
    			// special case for tags if key=values
    			if(p.getName().equals(HAS_TAG)){
    				for(Object v : vals){
    					String [] kv = (""+v).split("=");
    					if(kv.length == 2)
    						getProperties().setProperty(kv[0].trim(),kv[1].trim());
    				}
    			}
    			
    		}
    	}
    	
    	// remove all properties
		for(String p : ALL_FEATURE_PROPERTIES )
			instance.removePropertyValues(instance.getOntology().getProperty(p));
    	
    	// reset all finding properties, but diagnosis
    	for(String type: concepts.keySet()){
    		if(!type.equals(DISEASES)){
    			// add all concepts thar are not in instance
    			for(ConceptEntry e: concepts.get(type).getValues()){
    				addConceptClass(e);
    			}
    		}
    	}
    	
    	// remove 'stale' Dx
    	for(IClass d :instance.getDirectTypes()){
    		if(OntologyHelper.isDisease(d) && !concepts.get(DISEASES).getKeys().contains(d)){
    			instance.removeType(d);
    		}
    	}
    	
    	
    	// all findings should be synced already
    	// sync deseases
    	for(IClass d: concepts.get(DISEASES).getKeys()){
	    	if(!instance.hasType(d)){
	    		// only add asserted disease
	    		if(concepts.get(DISEASES).get(d).isAsserted())
	    			instance.addType(d);
	    	}
    	}
    	
    	// set report
    	instance.setPropertyValue(o.getProperty(HAS_REPORT),report);
    	if(status != null)
    		instance.setPropertyValue(o.getProperty(HAS_STATUS),status);
    	
    	// set slide names
    	instance.removePropertyValues(o.getProperty(HAS_SLIDE));
    	List<String> slist = new ArrayList<String>();
    	for(SlideEntry e: slides.getValues())
    		slist.add(e.getSlidePath());
    	//instance.setPropertyValues(o.getProperty(HAS_SLIDE),slides.getKeys().toArray());
    	instance.setPropertyValues(o.getProperty(HAS_SLIDE),slist.toArray());
    	modified = false;
    	newcase = false;
    }
    
    /**
     * save case entry as a file
     * @param os
     * @throws IOException
     */
    public void save(OutputStream os) throws IOException{
    	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    	writer.write("[CASE]\nname="+getName()+"\n");
    	if(status != null)
    		writer.write("status="+getStatus()+"\n");
    	if(instance != null){
    		writer.write("domain="+instance.getNameSpace()+"\n");
    		// save meta properties
    		for(Object prop : getProperties().keySet()){
    			writer.write(prop+"="+getProperties().getProperty(""+prop)+"\n");
    		}
    	}
    	writer.write("\n");
    	writer.write("[REPORT]\n"+getReport()+"\n\n");
    	// write out all of the lists
    	writer.write("["+DISEASES+"]\n");
    	for(IClass concept: concepts.get(DISEASES).getKeys())
    		writer.write(concept.getName()+"\n");
    	writer.write("\n");
    	writer.write("["+DIAGNOSTIC_FEATURES+"]\n");
    	for(IClass concept: concepts.get(DIAGNOSTIC_FEATURES).getKeys())
    		writer.write(concept.getName()+"\n");
    	writer.write("\n");
    	writer.write("["+PROGNOSTIC_FEATURES+"]\n");
    	for(IClass concept: concepts.get(PROGNOSTIC_FEATURES).getKeys())
    		writer.write(concept.getName()+"\n");
    	writer.write("\n");
    	writer.write("["+CLINICAL_FEATURES+"]\n");
    	for(IClass concept: concepts.get(CLINICAL_FEATURES).getKeys())
    		writer.write(concept.getName()+"\n");
    	writer.write("\n");
    	writer.write("["+ANCILLARY_STUDIES+"]\n");
    	for(IClass concept: concepts.get(ANCILLARY_STUDIES).getKeys())
    		writer.write(concept.getName()+"\n");
    	writer.write("\n");
    	writer.write("["+RECOMMENDATIONS+"]\n");
    	for(IClass concept: concepts.get(RECOMMENDATIONS).getKeys())
    		writer.write(concept.getName()+"\n");
    	writer.write("\n");
    	
    	writer.write("[SLIDES]\n");
    	for(String slide: slides.getKeys())
    		writer.write(slide+"\n");
    	writer.write("\n");
    	
    	// write entries details (only one though)
    	Set<String> writtenConcepts = new HashSet<String>();
    	for(String key: concepts.keySet()){
    		for(ConceptEntry e: concepts.get(key).getValues()){
    			if(!writtenConcepts.contains(e.getName())){
	    			writer.write("["+e.getName()+"]\n");
	    			writer.flush();
	    			e.getProperties().store(os,null);
	    			writer.write("\n");
	    			writtenConcepts.add(e.getName());
    			}
    		}
    	}
    	writtenConcepts = null;
    	
    	// write out slides
    	// make sure that primary slide is always first
    	boolean primary = true;
    	for(SlideEntry e: slides.getValues()){
    		e.setPrimarySlide(primary);
			
    		writer.write("["+e.getSlideName()+"]\n");
			writer.flush();
			e.getProperties().store(os,null);
			writer.write("\n");
			primary = false;
		}
    	
    	// write out shapes
    	//for(ShapeEntry e: manager.getAnnotations()){
    	for(SlideEntry slide: slides.getValues()){
    		for(ShapeEntry e: slide.getAnnotations()){
				writer.write("["+e.getName()+"]\n");
				writer.flush();
				e.getProperties().store(os,null);
				writer.write("\n");
			}
    	}
    	
    	writer.flush();
    	writer.close();
    }
    
    /**
     * load
     * @param is
     * @param manager
     * @throws IOException
     */
    public void load(InputStream is) throws IOException {
    	load(is,true);
    }
    
    /**
     * load
     * @param is
     * @param manager
     * @throws IOException
     */
    public void load(InputStream is, boolean overwite) throws IOException {
    	loading = true;
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	String line,field = null;
    	warnings = new StringBuffer();
    	StringBuffer buffer = new StringBuffer();
    	Pattern pt = Pattern.compile("\\[([\\w\\.\\-]+)\\]");
    	Map<String,String> map = new HashMap<String,String>();
    	while((line = reader.readLine()) != null){
    		line = line.trim();
    		// skip comments
    		if(line.startsWith("#"))
    			continue;
    		// extract headers
    		Matcher mt = pt.matcher(line);
    		if(mt.matches()){
    			// save previous field
    			if(field != null){
    				map.put(field,buffer.toString());
    				buffer = new StringBuffer();
    			}
    			field = mt.group(1);
    		}else{
    			buffer.append(line+"\n");
    		}
    	}
    	// finish the last item
    	if(field != null && buffer.length() > 0){
    		map.put(field,buffer.toString());
    	}
    	reader.close();
    
    	
     	Set<String> exclude = new HashSet<String>();
     	IOntology ont = null;
     	if(instance != null)
     		ont = instance.getOntology();
     	
     	// figure out institutions
     	String caseLocation = null;
     	String domainLocation = OntologyHelper.getInstitution(ont.getURI());
     	
     	
    	// set case name
    	if(map.containsKey("CASE")){
    		exclude.add("CASE");
    		Properties p = TextHelper.getProperties(map.get("CASE"));
    		String name = p.getProperty("name",NEW_CASE);
    		
    		if(overwite && ont != null && ont.hasResource(name)){
    			IInstance inst = ont.getInstance(name);
    			//remove all properies
    			//inst.removePropertyValues();
    			
    			// remove if not the same instance
    			//if(!inst.equals(instance)){
    			//	inst.delete();
    			//}
    			
    			// reload the instance
    			load(inst);
    		}
    		// rename if necessary
    		setName(name);
    		
    		// set institutions for case
    		if(p.containsKey("domain"))
    			caseLocation = OntologyHelper.getInstitution(URI.create(p.getProperty("domain")));
    		
    		
    		if(p.containsKey("status"))
    			setStatus(p.getProperty("status"));
    		
    		if(p.containsKey("domain") && !instance.getNameSpace().equals(p.getProperty("domain"))){
    			warnings.append(name+":\n");
    			warnings.append("\tcase file seems to belong to a different domain: "+p.getProperty("domain")+"\n");
    			//throw new IOException("Case file seems to belong to a different domain: "+p.getProperty("domain"));
    		}
    		
    		// load meta data
    		for(Object key: p.keySet()){
    			// skip known entries
    			if(!"status".equals(key) && !"domain".equals(key) && !"name".equals(key)){
    				getProperties().setProperty(""+key,p.getProperty(""+key));
    				if(instance != null){
    					IProperty pr = ont.getProperty(""+key);
    					if(pr != null){
    						String [] vals = TextHelper.parseList(p.getProperty(""+key));
    						if(vals.length == 1)
    							instance.setPropertyValue(pr,vals[0]);
    						else
    							instance.setPropertyValues(pr, vals);
    					}
    				}
    			}
    		}
    	}else
    		throw new IOException("Case file is missing a [CASE] header");	
    	
    	// set report
    	if(map.containsKey("REPORT")){
    		exclude.add("REPORT");
    		report = map.get("REPORT").trim();
    	}
    	
    	// load slides 
    	if(map.containsKey("SLIDES")){
    		exclude.add("SLIDES");
    		for(String s: map.get("SLIDES").trim().split("\n")){
    			if(s.length() > 0){
	    			SlideEntry slide = new SlideEntry(s);
	    			exclude.add(s);
	    			slide.setProperties(TextHelper.getProperties(map.get(s)));
	    			
	    			// if different institutions and has location then reset it
	    			if(caseLocation != null && !domainLocation.equals(caseLocation) && OntologyHelper.getInstitutions().contains(caseLocation)){
	    				slide.setConfigurationName(caseLocation);
	    				slide.setConfigurationURL(""+OntologyHelper.getConfigFile(caseLocation));
	    			}
	    			
	    			// remove privious slide
	    			if(hasSlide(s))
	    				removeSlide(getSlide(s));
	    			
	    			addSlide(slide);
    			}
			}
    	}
    	
    	
    	// load concepts into case
    	String [] categories = 
    		new String [] {
    			DISEASES,DIAGNOSTIC_FEATURES,
    			PROGNOSTIC_FEATURES,CLINICAL_FEATURES,ANCILLARY_STUDIES,RECOMMENDATIONS};
    	if(overwite)
    		concepts = initConceptMap();
    	//else
    	//	checkConceptMap();
    	IOntology kb = OntologyHelper.getKnowledgeBase(ont);
    	for(String key: categories ){
    		exclude.add(key);
	    	if(map.containsKey(key)){
	    		for(String name: map.get(key).trim().split("\n")){
	    			if(name.length() > 0){
		    			exclude.add(name);
		    			IClass cls = kb.getClass(name);
		    			// what if class doesn't exists
		    			if(cls == null){
		    				if(overwite){
			    				cls = kb.getClass(key).createSubClass(name);
			    				System.err.println("WARNING: "+name+" concept was not found, creating ..");
			    			}else{
			    				if(warnings.length() == 0){
			    					warnings.append(getName()+":\n");
			    				}
			    				warnings.append("\t"+name+" concept is not defined in domain ontology "+ont+"\n");
			    				continue;
			    				//throw new IOException(name+" concept is not defined in domain ontology "+ont);
			    			}
		    			}
		    			// check if we already have this entry if so remove it
		    			ConceptEntry entry = concepts.get(key).get(cls);
		    			if(entry != null){
		    				removeConceptEntry(key,entry);
		    			}
		    			
		    			// then create new one 
		    			entry = new ConceptEntry(cls);
		    			entry.setProperties(TextHelper.getProperties(map.get(name)));
		    			
		    			// add parts to case
		    			//getParts().addAll(entry.getParts());
		    			
		    			// not really necessary
		    			//if(!concepts.containsKey(key))
		    			//	concepts.put(key,new OrderedMap<IClass,ConceptEntry>());
		    			
		    			//if(addConcept(concepts.get(key),entry))
		    			//	addConceptClass(entry);
		    			addConceptEntry(key,entry);
	    			}
	    		}
	    	}
    	}
    	
    	
    	// load all remaining anntations
    	//int num = 0;
    	//pt = Pattern.compile("[A-Za-z]+(\\d+)");
    	ArrayList<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
    	for(String key: map.keySet()){
    		// if not in exclude list, then it is an annotation
    		if(!exclude.contains(key)){
    			Properties p = TextHelper.getProperties(map.get(key));
    			// further make sure that it is a shape
    			if(p.containsKey("tag")){
    				ShapeEntry entry = new ShapeEntry();
    				entry.setProperties(p);
    				shapes.add(entry);
    				/*
    				Matcher mt = pt.matcher(entry.getName());
    				if(mt.matches()){
    					int x = Integer.parseInt(mt.group(1));
    					if(x > num)
    						num = x;
    				}*/
    			}
    		}
    	}
    	//manager.setAnnotationNumber(num);
    	Collections.sort(shapes);
    	for(ShapeEntry entry: shapes){
    		if(slides.containsKey(entry.getImage())){
				slides.get(entry.getImage()).addAnnotation(entry);
			}
    	}
    	
    	loading = false;
    }
    
    /**
     * add concept entry based on the root
     * @param root
     * @param e
     */
    public void addConceptEntry(String root, ConceptEntry e){
    	// well, if we already have this concept in another category
    	// we should use it, instead of the new one
    	ConceptEntry old = getConceptEntry(e.getName());
    	if(old != null){
    		old.copyFrom(e);
    		e = old;
    	}
    	
    	// now add stuff
    	final String category = root;
    	final ConceptEntry entry = e;
    	
    	// keep track off parts
    	getParts().addAll(e.getParts());
    	
    	
		if(models != null && models.containsKey(category)){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					models.get(category).addElement(entry);
				}
			});
		}else{
			if(addConcept(concepts.get(category),e))
				addConceptClass(e);
		}
    }
    
    
    /**
     * add concept entry based on the root
     * @param root
     * @param e
     */
    public void removeConceptEntry(String root, ConceptEntry e){
    	final String category = root;
    	final ConceptEntry entry = e;
    	if(models != null && models.containsKey(category)){
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				models.get(category).removeElement(entry);
    			}
    		});
    	}else{
    		if(removeConcept(concepts.get(category),e))
    			removeConceptClass(e);
    	}
    }
    

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	
	/**
	 * get concept entry representation of this case
	 * @return
	 */
	public ConceptEntry getConceptEntry() {
		IOntology ont = OntologyHelper.getKnowledgeBase(instance.getOntology());
		return new ConceptEntry(ont.getClass(CASES)){
			private ConceptExpression findings;
			public String getText(){
				return CaseEntry.this.getName();
			}
			public String getName(){
				return CaseEntry.this.getName();
			}
			public int getPatternCount(){
				return 1;
			}
			public boolean isInconsistent() {
				return true;
			}
			public ConceptExpression getFindings() {
				if(findings == null){
					IOntology ont = getConceptClass().getOntology();
					findings = new ConceptExpression(ILogicExpression.AND);
					for(ConceptEntry e: concepts.get(DIAGNOSTIC_FEATURES).values()){
						ConceptEntry entry = new ConceptEntry(ont.getClass(e.getName()));
						entry.setAsserted(true);
						entry.setAbsent(e.isAbsent());
						findings.add(entry);
					}
				}
				return findings;
			}
		};
	}


}
