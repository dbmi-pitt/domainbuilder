package edu.pitt.dbmi.domainbuilder.knowledge;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.TitledBorder;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptExpression;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.widgets.NewConceptPanel;

import edu.pitt.ontology.*;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.util.TerminologyException;
import edu.pitt.text.tools.TextTools;



/**
 * this class deals with concept creation duties
 * @author tseytlin
 *
 */
public class ConceptHandler {
	private IOntology ont;
	private Terminology terminology;
	private IReasoner reasoner;
	//private UIHelper.DynamicTable table;
	private Timer timer;
	private List<IClass> classCache = new ArrayList<IClass>();
	private List<ConceptEntry> diagnosis = new ArrayList<ConceptEntry>();
	private Set<IClass> removedClasses = new LinkedHashSet<IClass>();
	private ReportBuilder reportBuilder;
	
	/**
	 * create new instance of concept creator
	 * @param o
	 */
	public ConceptHandler(IOntology o){
		this.ont = o;
		terminology = o.getRepository().getTerminology(ONTOLOGY_TERMINOLOGY);
	}
	
	/**
	 * set report builder instance
	 * @param rb
	 */
	public void setReportBuilder(ReportBuilder rb){
		reportBuilder = rb;
	}
	
	/**
	 * create class with a  given parent (can be null) and a potential class 
	 * name, if such class exists, return it
	 * @param parent
	 * @param name
	 * @return class that was created
	 */
	public IClass createConceptClass(String name){
		return createConceptClass(null,name);
	}
	
	/**
	 * create class with a  given parent (can be null) and a potential class 
	 * name, if such class exists, return it
	 * @param parent
	 * @param name
	 * @return class that was created
	 */
	public IClass renameConceptClass(IClass cls, String name){
		name = createConceptName(cls,name);
		
		if(name != null && name.length() > 0){
		
			// rename class
			String oldName = cls.getName();
			cls.setName(name);
			cls =  ont.getClass(name);
			
			// rename instances in caseBase
			IOntology iont = getCaseBase(ont);
			IClass icls = iont.getClass(oldName);
			if(icls != null){
				for(IInstance i: icls.getInstances()){
					String on = i.getName();
					i.setName(on.replaceAll(oldName.toLowerCase(),name.toLowerCase()));
				}
				icls.setName(name);
			}
			
			
			// notify that class was renamed
			//DomainBuilder.getInstance().firePropertyChange(CLASS_RENAME_EVENT,oldName,name);
		}
		return cls;
	}
	
	/**
	 * remove concept class, perhaps just from parent,
	 */
	public void removeConceptClass(IClass parent, IClass child){
		if(parent != null)
			parent.removeSubClass(child);
		removedClasses.add(child);
	}
	
	/**
	 * add concept class to parent parent,
	 */
	public void addConceptClass(IClass parent, IClass child){
		if(parent != null){
			parent.addSubClass(child);
			if(removedClasses.contains(child))
				removedClasses.remove(child);
		}else {
			System.err.println("Error: problem adding concept "+child+" to a null parent");
		}
	}
	
	
	/**
	 * add concept synonym
	 * @param cls
	 * @param text
	 */
	public void addConceptSynonym(IClass cls, String text){
		if(cls != null){
			// make sure that the name is a preferred synonym as well
			String name = UIHelper.getPrettyClassName(cls.getName());
			if(!hasConceptSynonym(cls, name)){
				cls.addLabel(name);
			}
			
			text = text.trim().toLowerCase(); 
			
			cls.addLabel(text);
		}
	}
	
	/**
	 * does concept have this synonym
	 * @param cls
	 * @param name
	 * @return
	 */
	private boolean hasConceptSynonym(IClass cls, String name){
		if(cls != null){
			for(String s: cls.getLabels()){
				if(s.equals(name))
					return true;
			}
		}
		return false;
	}
	
	
	/**
	 * analyze concept class, that is check synonyms and such
	 * @param cls
	 */
	public void analyzeConceptClass(IClass cls){
		if(cls == null)
			return;
		
		// make sure that name is also a synonym
		String name = UIHelper.getPrettyClassName(cls.getName());
		if(!hasConceptSynonym(cls, name)){
			cls.addLabel(name);
		}
				
		// analyze synonyms
		if(terminology != null){
			try{
				terminology.removeConcept(cls.getConcept());
				terminology.addConcept(cls.getConcept());
			}catch(TerminologyException ex){
				ex.printStackTrace();
			}
		}
		
		/*
		String [] labels = cls.getLabels();
		if(labels.length == 0)
			labels = new String [] {cls.getName()};
		*/
		// remove old isConceptOf definitions
		//removeEquivalentRestrictions(cls,HAS_CONCEPT);
		
		// add all words
		/*
		for(String name: labels){
			createConceptWords(cls,getClassName(name));
		}
	
		// add semantic type if not there
		addSemanticType(cls);
		*/		
	}
	
	/**
	 * recursively set a property value to a class and its children
	 * @param cls
	 * @param p
	 * @param value
	 */
	public void setSubtreePropertyValue(IClass cls, IProperty prop, Object value){
		setSubtreePropertyValue(cls, prop, value,true);
	}
	
	/**
	 * recursively set a property value to a class and its children
	 * @param cls
	 * @param p
	 * @param value
	 */
	public void setSubtreePropertyValue(IClass cls, IProperty prop, Object value, boolean force){
		if(cls == null)
			return;
		
		cls.setPropertyValue(prop, value);
		
		// get ALL subclasses and set property there
		for(IClass c: cls.getSubClasses()){
			if(force || c.getPropertyValue(prop) == null)
				c.setPropertyValue(prop, value);
		}
	}
	
	/**
	 * add diagnoses to the queue to save later
	 * @param d
	 */
	public void addDiagnosis(ConceptEntry d){
		//int i = diagnosis.indexOf(d);
		// if diagnosis already in the list, it can be an updated
		// diagnosis
		/*
		if(i > -1){
			ConceptEntry o = diagnosis.get(i);
			// check if diagnosis in the list is the same or is
			// it multi-pattern disiease
			if(o != d ){
				if(d.getPatternCount() > o.getPatternCount())
					o.setCompleteFindings(d.getCompleteFindings());
				else if (d.getPatternCount() < o.getPatternCount()){
					d.setCompleteFindings(o.getCompleteFindings());
					diagnosis.set(i,d);
				}
			}
		}else
		*/
		int i = diagnosis.indexOf(d);
		if(i > -1)
			diagnosis.set(i,d);
		else
			diagnosis.add(d);
	}
	
	
	/**
	 * create class with a  given parent (can be null) and a potential class 
	 * name, if such class exists, return it
	 * @param parent
	 * @param name
	 * @return class that was created
	 */
	public IClass createConceptClass(IClass parent, String name){
		IClass cls = createQuickConceptClass(parent, name);
		if(cls != null){
			// add words as equivelent restrictions
			//createConceptWords(cls, cls.getName());
			// add semantic type based on parent
			//addSemanticType(cls);
		}
		return cls;
	}
	
	/**
	 * create class with a  given parent (can be null) and a potential class 
	 * name, if such class exists, return it
	 * @param parent
	 * @param name
	 * @return class that was created
	 */
	public IClass createQuickConceptClass(IClass parent, String name){
		if(parent == null)
			parent = ont.getClass(CONCEPTS);
		// create appropriate name
		name = createConceptName(parent,name);
		
		if(name == null || name.length() == 0)
			return null;		
		//System.out.println("name "+(System.currentTimeMillis()-time));
		//time = System.currentTimeMillis();
		// see if class is already there
		IClass cls = ont.getClass(name);
		if(cls == null){
			// create a concept
			cls = parent.createSubClass(name);
		}else if(!cls.hasSuperClass(parent)){
			parent.addSubClass(cls);
		}
		
		// quickly add to terminology
		analyzeConceptClass(cls);
		
		return cls;
	}
	
	
	
	
	/**
	 * create words for given class name
	 * @param c
	 * @param name
	 *
	private void addSemanticType(IClass cls){
		// add semantic type based on parent if no symantec type is defined
		IProperty prop = ont.getProperty(HAS_SEMANTIC_TYPE);
		if(cls.getRestrictions(prop).length == 0){
			IRestriction r = ont.createRestriction(IRestriction.SOME_VALUES_FROM);
			r.setProperty(prop);
			if(cls.hasSuperClass(ont.getClass(DISEASES))){
				r.setParameter(ont.getClass(SEMANTIC_TYPE_DISEASE).getLogicExpression());
			}else if(cls.hasSuperClass(ont.getClass(DIAGNOSTIC_FEATURES))){
				r.setParameter(ont.getClass(SEMANTIC_TYPE_DIAGNOSTIC).getLogicExpression());
			}else if(cls.hasSuperClass(ont.getClass(PROGNOSTIC_FEATURES))){
				r.setParameter(ont.getClass(SEMANTIC_TYPE_PROGNOSTIC).getLogicExpression());
			}else if(cls.hasSuperClass(ont.getClass(CLINICAL_FEATURES))){
				r.setParameter(ont.getClass(SEMANTIC_TYPE_CLINICAL).getLogicExpression());
			}else if(cls.hasSuperClass(ont.getClass(ATTRIBUTES))){
				r.setParameter(ont.getClass(SEMANTIC_TYPE_MODIFIER).getLogicExpression());
			}else
				r = null;
			if(r != null)
				cls.addNecessaryRestriction(r);
		}
		// add to cache
		classCache.add(cls);
	}
	*/
	
	/**
	 * save all diagnoses rules to the ontology
	 */
	public void saveDiagnosticRules(){
		for(ConceptEntry d: diagnosis){
			IClass cls = d.getConceptClass();
			ConceptExpression exp = d.getCompleteFindings();
			
			// remove old equivalent  restrictions
			for(IClass c: cls.getEquivalentClasses()){
				if(c.isAnonymous()){
					cls.removeEquivalentClass(c);
					c.delete();
				}
			}
			//removeEquivalentRestrictions(cls,HAS_FINDING);
			//removeEquivalentRestrictions(cls,HAS_NO_FINDING);
			
			
			// add new rules
			ILogicExpression newRules = exp.toOntologyExpression(ont);
				
			//if(newRules.getExpressionType())
			if(!newRules.isEmpty()){
				IClass c = ont.createClass(newRules);
				cls.addEquivalentClass(c);
				//System.out.println(cls+"\nconcept exp: "+exp+"\nlogic exp:"+newRules+"\nclass: "+c+"\nequiv: "+cls.getEquivalentRestrictions());
			}
		}
		diagnosis.clear();
	}
	
	
	/**
	 * save all prognostic rules
	 */
	public void savePrognosticRules(){
		if(reportBuilder != null && reportBuilder.isModified()){
			reportBuilder.syncFindings();
			reportBuilder.syncTriggers();
			for(IClass schema: reportBuilder.getTemplates()){
				//System.out.println(schema+" "+schema.getNecessaryRestrictions());
				ConceptExpression triggerList = reportBuilder.getTriggers(schema);
				ConceptExpression findingList = reportBuilder.getFindings(schema);
				
				// remove previous rules && orders
				IProperty order = ont.getProperty(HAS_ORDER);
				removeNecessaryRestrictions(schema,HAS_TRIGGER);
				removeNecessaryRestrictions(schema,HAS_PROGNOSTIC);
				schema.removePropertyValues(order);
				
				// add triggers
				for(Object o : triggerList.toLogicExpression()){
					//System.out.println("\tt "+trigger);
					ILogicExpression exp = null;
					if(o instanceof ILogicExpression)
						exp = (ILogicExpression) o;
					else if(o instanceof IClass)
						exp = ((IClass)o).getLogicExpression(); 
					if(exp != null)
						addNecessaryRestriction(schema,HAS_TRIGGER,exp);	
				}
				
				// add findings
				int i = 1;
				for(Object o: findingList){
					//System.out.println("\tf "+finding);
					if(o instanceof ConceptEntry){
						// add restriction
						IClass finding = ((ConceptEntry)o).getConceptClass();
						addNecessaryRestriction(schema,HAS_PROGNOSTIC,finding);
						
						// add order
						schema.addPropertyValue(order,finding.getName()+" : "+(i++));
					}
				}
				//System.out.println(schema+" "+schema.getNecessaryRestrictions());
			}
		}
	}
		
	/**
	 * add finding to KB
	 * @param cls
	 */
	private void addNecessaryRestriction(IClass schema, String restriction, IClass cls){
		addNecessaryRestriction(schema, restriction, cls.getLogicExpression());
	}
	
	/**
	 * add finding to KB
	 * @param cls
	 */
	private void addNecessaryRestriction(IClass schema, String restriction, ILogicExpression exp){
		IRestriction r = ont.createRestriction(IRestriction.SOME_VALUES_FROM);
		r.setProperty(ont.getProperty(restriction));
		r.setParameter(exp);
		schema.addNecessaryRestriction(r);
	}
	
	private void setEquivalentRestriction(IClass cls, ILogicExpression exp){
		// remove old equivalent  restrictions
		removeEquivalentRestrictions(cls);
		//if(newRules.getExpressionType())
		if(exp != null && !exp.isEmpty()){
			cls.addEquivalentClass(ont.createClass(exp));
		}
	}
	
	
	/**
	 * add finding to KB
	 * @param cls
	 */
	private void removeNecessaryRestrictions(IClass schema, String restriction){
		IProperty prop = ont.getProperty(restriction);
		if(prop != null){
			for(IRestriction r: schema.getRestrictions(prop)){
				schema.removeNecessaryRestriction(r);
				r.delete();
			}
		}
	}
	
	/**
	 * add finding to KB
	 * @param cls
	 */
	private void removeEquivalentRestrictions(IClass schema, String restriction){
		IProperty prop = ont.getProperty(restriction);
		if(prop != null){
			for(IRestriction r: schema.getRestrictions(prop)){
				schema.removeEquivalentRestriction(r);
				r.delete();
			}
		}
	}
	
	/**
	 * add finding to KB
	 * @param cls
	 */
	private void removeEquivalentRestrictions(IClass cls){
		// remove old equivalent  restrictions
		for(IClass c: cls.getEquivalentClasses()){
			if(c.isAnonymous()){
				cls.removeEquivalentClass(c);
				c.delete();
			}
		}
	}
	
	/**
	 * add concept action
	 * @param cls
	 * @param action
	 */
	public void addConceptAction(IClass cls, IClass action){
		// remove previous rules
		removeNecessaryRestrictions(cls,HAS_ACTION);
						
		// add triggers
		addNecessaryRestriction(cls,HAS_ACTION,action);	
		
	}
	
	
	/**
	 * create concept attributes (create FAVs)
	 * @param cls
	 * @param attr
	 */
	public void createConceptAttributes(IClass feature, ConceptEntry [] attributes){
		//TODO:!!!! THis is not correct
		System.out.println("Disabled create attributes!");
		/*
		for(ConceptEntry at : attributes){
			IClass attribute = at.getConceptClass();
			// if there is a class that has this attribute and feature as direct parent
			// then this attribute probably exists and you don't want to create a new one
						
			
			// create a finding name
			String finding_name;
			if(attribute.hasSuperClass(ont.getClass(LOCATIONS))){
				finding_name = feature.getName()+"_"+IN+"_"+attribute.getName();
			}else{
				finding_name = feature.getName()+"_"+attribute.getName();
			}
			
			// check if such finding exists
			IClass finding = ont.getClass(finding_name);
			if(finding == null){
				finding = feature.createSubClass(finding_name);
			}
			if(!finding.hasSuperClass(attribute)){
				finding.addSuperClass(attribute);
			}
		}
		*/
	}
	
	/**
	 * create attribute with given name for a concept
	 * @param finding
	 * @param name
	 */
	private void createConceptAttribute(IClass finding, String name){
		IClass parent = ont.getClass(MODIFIERS);
		// check if it is a location
		String prefix = OntologyHelper.getLocationPrefix(name);
		if(prefix != null){
			name = name.substring(prefix.length()+1).trim();
			parent = ont.getClass(LOCATIONS);
		}
				
		// check if its a number (0 means it is not a number)
		int num = TextHelper.parseIntegerValue(name);
		if(num !=  OntologyHelper.NO_VALUE){
			parent = ont.getClass(NUMERIC);
		}
		
		// make appropriate name
		name = getClassName(name);
		
		// create attribute class
		IClass attr = ont.getClass(name);
		if(attr == null){
			attr = parent.createSubClass(getClassName(name));
	
			// if number add restriction to enforce value
			if(num > 0){
				// add number label
				if(!name.equals(""+num))
					attr.addLabel(""+num);
				
				// create restriction to force value
				IRestriction r = ont.createRestriction(IRestriction.HAS_VALUE);
				r.setProperty(ont.getProperty(HAS_NUMERIC_VALUE));
				r.setParameter(new LogicExpression(new Float(num)));
				attr.addEquivalentRestriction(r);
			}	
		}else if(!parent.hasSubClass(attr)){
			parent.addSubClass(attr);
		}
		
		
		
		//add finding as its subclass
		finding.addSuperClass(attr);
	}
	
	
	/**
	 * infer hierarchy of findings based on their linguistic description
	 * (organize feature - attribute -values )
	 */
	
	public void inferFindingHierarchy(){
		// iterate through all features and 
		IClass features = ont.getClass(FEATURES);
		for(IClass cls: features.getSubClasses()){
			inferFindingHeiarchy(features,cls);
		}
		// iterate through ancilary studies
		IClass ancilary = ont.getClass(ANCILLARY_STUDIES);
		for(IClass cls: ancilary.getSubClasses()){
			inferFindingHeiarchy(ancilary,cls);
		}
	}
	
	
	/**
	 * is class a a substring of class b
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean isSubstring(IClass a,IClass b){
		String sa = a.getName().replaceAll("_"," ");
		String ba = b.getName().replaceAll("_"," ");
		return ba.matches(".*\\b"+sa+"\\b.*");
	}
	
	/**
	 * infer concept attribuets of a single class
	 * @param cls
	 * @throws Exception
	 */
	private void inferFindingHeiarchy(IClass parent,IClass cls) {
		if(!isSystemClass(cls)){
			List<IClass> list = new ArrayList<IClass>();
			// get all matching resources
			for(Iterator i = ont.getMatchingResources(cls.getName());i.hasNext();){
				Object obj = i.next();
				if(obj instanceof IClass){
					// don't add stuff from different parent
					// nor do you want to add stuff that are from the same word
					IClass c = (IClass)obj;
					if(c.hasSuperClass(parent) && isSubstring(cls,c))
						list.add(c);
				}
			}
			// remove self
			list.remove(cls);
			
			// if list not empty
			if(!list.isEmpty()){
				//System.out.println(cls+" -> "+list);
				for(IClass child: list){
					if(!child.hasSuperClass(cls)){
						// remove all feature superclasses
						for(IClass p: child.getDirectSuperClasses()){
							if(isFeature(p))
								child.removeSuperClass(p);
						}
						// add superclass
						child.addSuperClass(cls);
					}
				}
			}
		}
	}
	
	/**
	 * infer attributes from given concepts
	 */
	public void inferConceptAttributes(){
		// iterate through all features and 
		for(IClass cls: ont.getClass(FEATURES).getDirectSubClasses()){
			inferConceptAttributes(cls);
		}
		
		// iterate through all ancillary and 
		inferConceptAttributes(ont.getClass(ANCILLARY_STUDIES));
		
		
		// try to find patterns and break up attributes
		// EXAMPLE: if there is a 'moderate' attribute and 'moderate wedge shaped" attribute,
		// then this will break up 'moderate wedge shaped' into 2 parts.
		// go through all direct attributes
		for(IClass cls:  ont.getClass(MODIFIERS).getSubClasses()){
			splitAttribute(cls);	
		}
		
		for(IClass cls:  ont.getClass(LOCATIONS).getDirectSubClasses()){
			// only try to split location attributes that are multi-word
			// this should NOT break up things like reticular_dermis, by selecting dermis
			if(cls.getName().split("_").length > 1)
				splitAttribute(cls);	
		}
		
		// remove stop words in attributes
		for(String type: new String [] {MODIFIERS,LOCATIONS}){
			for(IClass cls:  ont.getClass(type).getDirectSubClasses()){
				removeStopWords(cls);	
			}	
		}
		
	}
	
	/**
	 * remove stop words from attributes
	 * @param cls
	 */
	private void removeStopWords(IClass cls){
		String name = cls.getName();
		//String suffix = null;
		
		// check if this attribute has stop words, if so, then split it
		for(String sw: TextTools.getStopWords()){
			
			// remove stop word from the begining
			if(name.startsWith(sw+"_")){
				name = name.substring(sw.length()+1);
			}
			
			// remove stop word from the end
			if(name.endsWith("_"+sw)){
				name = name.substring(0,name.length()-sw.length()-1);
			}
			
			// does the stop word split the attribute
			/*
			int x = name.indexOf("_"+sw+"_");
			if(x > -1){
				suffix = name.substring(x+sw.length()+2);
				name = name.substring(0,x);
				break;
			}
			*/
		}
		
		// now rename class if necessary
		if(!name.equals(cls.getName())){
			/*
			if(suffix != null){
				IOntology o = cls.getOntology();
				IClass d = o.getClass(suffix);
				if(d == null){
					IClass parent = o.getClass(MODIFIERS);
					for(IClass p: cls.getDirectSuperClasses())
						if(isSystemClass(p))
							parent = p;
					d = parent.createSubClass(suffix);
				}	
				// if there is an existing class merge
				for(IClass c: cls.getDirectSubClasses())
					d.addSubClass(c);
			}
			*/
			// now rename the original
			renameAttribute(cls,name);
		}
	}
	
	/**
	 * rename attribute
	 * @param cls
	 * @param name
	 */
	private void renameAttribute(IClass cls,String name){
		IOntology o = cls.getOntology();
		IClass d = o.getClass(name);
		if(d != null){
			// if there is an existing class merge and remove
			for(IClass c: cls.getDirectSubClasses())
				d.addSubClass(c);
			cls.delete();
		}else{
			// if there is no existing class, then just rename
			cls.setName(name);
		}
	}
	
	/**
	 * if this attribute is composit, split it
	 * @param cls
	 */
	private void splitAttribute(IClass cls){
		// get all matching resources that are naked attributes
		List<IClass> list = new ArrayList<IClass>();
		if(isAttribute(cls) && !isFeature(cls) && !isDisease(cls) && !isAncillaryStudy(cls)){
			for(Iterator i = ont.getMatchingResources(cls.getName());i.hasNext();){
				Object obj = i.next();
				if(obj instanceof IClass){
					// don't add stuff that is a FINDING or DISEASES
					// we just need naked attributes
					IClass c = (IClass)obj;
					if(isAttribute(c) && !isFeature(c) && !isDisease(c) && !isAncillaryStudy(c))
						list.add(c);
				}
			}
		}
		// remove self
		list.remove(cls);
		
		// if list not empty
		if(!list.isEmpty()){
			//System.out.println(cls+" -> "+list);
			for(IClass child: list){
				splitAttribute(cls,child);
			}
		}
	}
	
	/**
	 * if inner attribute is contained in outer attribute, we
	 * need to break outer attribute apart
	 * @param inner
	 * @param outer
	 */
	private void splitAttribute(IClass inner, IClass outer){
		if(outer.getName().contains(inner.getName())){
			String finding_name = outer.getName().replaceAll("_"," ");
			String feature_name = inner.getName().replaceAll("_"," ");
			if(isSubstring(inner,outer)){
				int i = finding_name.indexOf(feature_name);
				
				// I should always be greater then -1 cause it is FAV
				if(i > -1){
					// find prefix and suffix findings
					String prefix = finding_name.substring(0,i).trim();
					String suffix = finding_name.substring(i+feature_name.length()).trim();
					
					// now reshuffle all attributes
					// for all findings of outer attribute
					// create sub attributes as well as reasign global attribute
					for(IClass finding: outer.getDirectSubClasses()){
						// create new sub attributes and add finding to them
						for(String attr: new String [] {prefix,suffix}){
							attr = TextHelper.filterStopWords(attr);
							if(attr.length() > 0)
								createConceptAttribute(finding,attr);
						}
						// add finding to more atomic attribute
						finding.addSuperClass(inner);
					}
					// now remove the original outer finding
					outer.delete();
				}
			}
		}
	}
	
	
	
	/**
	 * infer attributes from given concepts
	 */
	private void inferConceptAttributes(IClass feature){
		List<IClass> parents = new ArrayList<IClass>();
		IClass [] children = feature.getDirectSubClasses();
		Collections.addAll(parents,feature.getSuperClasses());
		

		// now recurse into children
		for(IClass child: children){
			// skip children that are parents
			// (avoid cycles)
			if(parents.contains(child))
				continue;
			
			//if is feature attribute value, then create an attribute
			//isFAV
			//if(child.getName().contains(feature.getName())){
			//if(isGeneralForm(feature,child)){
			if(getFeature(feature).equals(getFeature(child))){	
				String finding_name = child.getName().replaceAll("_"," ");
				String feature_name = feature.getName().replaceAll("_"," ");
				
				// assure that feature is contained
				if(!finding_name.contains(feature_name)){
					feature_name = getGeneralForm(feature, child);
					if(feature_name != null)
						feature_name = feature_name.replaceAll("_"," ");
				}
				
				// now split out attributes
				if(finding_name.matches(".*\\b"+feature_name+"\\b.*")){
					int i = finding_name.indexOf(feature_name);
					// I should always be greater then -1 cause it is FAV
					// make sure that there is either space before it or after it
					if(i > -1){
						String prefix = finding_name.substring(0,i).trim();
						String suffix = finding_name.substring(i+feature_name.length()).trim();
						if(prefix.length() > 0)
							createConceptAttribute(child,prefix);
						if(suffix.length() > 0)
							createConceptAttribute(child,suffix);
					}
				}
			}
			//recurse unless it is a trick
			inferConceptAttributes(child);
		}
	}
	
	
	/**
	 * infer hierarchy of diseases based on 
	 * diagnostic findings.
	 */
	public void inferDiseaseHierarchy(){
		long time = System.currentTimeMillis();
		if(reasoner == null){
			reasoner = ont.getRepository().getReasoner(ont);
			try{
				reasoner.initialize();
			}catch(IOntologyException ex){
				ex.printStackTrace();
			}
		}
		// try to find a more specific class that can be
		// classify using reasoner
		//for(IClass cls: classCache){
		for(IReasoner.IResult result: reasoner.computeInferredHierarchy()){
			result.assertResult();
		}
		//}
		classCache.clear();
		System.out.println("classification "+(System.currentTimeMillis()-time)+" ms");
	}
	
	
	/**
	 * flush all of the operations
	 */
	public void flush(){
		savePrognosticRules();
		saveDiagnosticRules();
		removeConceptClasses();
		inferConceptAttributes();
		//inferConceptHierarchy();
		saveDefaultPower();
		
		setDirty();
	}
	
	/**
	 * assign default power value if not present
	 */
	public void saveDefaultPower(){
		setSubtreePropertyValue(ont.getClass(ARCHITECTURAL_FEATURES),ont.getProperty(HAS_POWER),POWER_LOW,false);
		setSubtreePropertyValue(ont.getClass(CYTOLOGIC_FEATURES),ont.getProperty(HAS_POWER),POWER_HIGH,false);
	}
	
	
	/**
	 * delete class
	 * @param cls
	 */
	private void deleteClass(IClass cls){
		IOntology iont = getCaseBase(ont);
		// remove possible instances (just not for dx since those are cases)
		IClass icls = iont.getClass(cls.getName());
		if(icls != null){
			for(IInstance i: icls.getInstances()){
				if(i.hasType(iont.getClass(CASES)))
					i.removeType(icls);
				else
					i.delete();
			}
		}
		//long time = System.currentTimeMillis();
		
		// check diagnostic rules first (they are the most important)
		for(IClass dx: ont.getClass(DISEASES).getSubClasses()){
			// check neccessary and sufficient
			if(!dx.getEquivalentRestrictions().isEmpty()){
				if(isFindingInDiagnosticRule(cls,dx.getEquivalentRestrictions())){
					setEquivalentRestriction(dx, deleteClassFromExpression(cls,copy(dx.getEquivalentRestrictions(),ont)));
				}
			}
			// check necessary restrictions	
			if(!dx.getNecessaryRestrictions().isEmpty()){
				if(isFindingInDiagnosticRule(cls,dx.getNecessaryRestrictions())){
					for(Object o: dx.getNecessaryRestrictions()){
						if(o instanceof IRestriction){
							IRestriction r = (IRestriction) o;
							// if more then one entry AND finding is there
							// if only one entry, restriction will get deleted automatically
							if(r.getParameter().size() > 1 && isFindingInDiagnosticRule(cls,r.getParameter())){
								r.setParameter(deleteClassFromExpression(cls,copy(r.getParameter(),ont)));
							}
						}
					}
				}
			}
		}
		
		
		// now check templates
		for(IClass tmp: ont.getClass(SCHEMAS).getSubClasses()){
			// now, protege will automatically remove necessary restrictions that 
			// are by itself for hasPrognostic so the only thing we have to worry about
			// is order and complex triggers
			for(IRestriction r : tmp.getRestrictions(ont.getProperty(HAS_TRIGGER))){
				if(r.getParameter().size() > 1 && isFindingInDiagnosticRule(cls,r.getParameter())){
					ILogicExpression nexp = deleteClassFromExpression(cls,copy(r.getParameter(),ont));
					if(nexp.size() == 1 && nexp.get(0) instanceof IClass)
						nexp = ((IClass)nexp.get(0)).getLogicExpression();
					r.setParameter(nexp);
				}
			}
			
			// now check for ordering
			for(Object o : tmp.getPropertyValues(ont.getProperty(HAS_ORDER))){
				if(o.toString().startsWith(cls.getName()+" : ")){
					tmp.removePropertyValue(ont.getProperty(HAS_ORDER),o);
					break;
				}
			}
		}
		//System.out.println("delete took "+(System.currentTimeMillis()-time));
		
		
		// now remove the fucking class
		cls.delete();	
	}
	
	/**
	 * now lets remove this expression
	 * @param cls
	 * @param equivalentRestrictions
	 * @return
	 */
	private ILogicExpression deleteClassFromExpression(IClass cls, ILogicExpression exp) {
		//List toadd = new ArrayList();
		for(ListIterator it = exp.listIterator();it.hasNext();){
			Object o = it.next();
			if(o != null){
				if(o instanceof ILogicExpression){
					ILogicExpression e = deleteClassFromExpression(cls,(ILogicExpression)o);
					// if removed parameter remove entire restriction
					if(e.isEmpty())
						it.remove();
				}else if(o instanceof IRestriction){
					IRestriction r = (IRestriction)o;
					int n = r.getParameter().size();
					ILogicExpression e = deleteClassFromExpression(cls,r.getParameter());
					
					// if parameters changed remove entire restriction
					if(e.size() != n){
						it.remove();
						
						// if not empty re-add new restriction
						if(!e.isEmpty()){
							IOntology on = cls.getOntology();
							IRestriction nr = on.createRestriction(r.getRestrictionType());
							nr.setProperty(r.getProperty());
							nr.setParameter((e.size() == 1)?((IClass)e.getOperand()).getLogicExpression():e);
							
							//toadd.add(nr);
							it.add(nr);
						}
					}
				}else if(o instanceof IClass){
					// if class is equals to parameter/ remove
					if(cls.equals(o))
						it.remove();
				}
			}
		}
		// add stuff that was re-added
		//exp.addAll(toadd);
		return exp;
	}

	
	/**
	 * remove stale concept classes on flush
	 */
	private void removeConceptClasses(){
		//TODO: Protege is nice enough to remove ALL fucking references
		// to deleted class, that is if you delete a Finding, all expressions
		// that dared to contain that finding will be wiped. How nice?
		// If you remove 1 finding, all disease specification is wiped.
		// need to fix it somehow.
		for(IClass cls: removedClasses){
			// only remove concept if it only parents are Thing and Words
			if(isOrphan(cls)){
				deleteClass(cls);
			}
		}
		removedClasses.clear();
		
		// remove orgphaned classes from before
		// if you have root level class that is now known
		// just get rid of it
		for(IClass cls : ont.getRoot().getDirectSubClasses()){
			if(!isSystemClass(cls)){
				// now remove
				deleteClass(cls);
			}
		}
	}
	
	public boolean hasRemovedClasses(){
		return !removedClasses.isEmpty();
	}
	
	/**
	 * is this class an orphan and should be deleted?
	 * @param cls
	 * @return
	 */
	private boolean isOrphan(IClass cls){
		if(cls == null)
			return false;
		boolean remove = true;
		for(IClass parent: cls.getSuperClasses()){
			if( !( parent.isSystem() || parent.isAnonymous() || parent.equals(cls))){
				remove = false;
				break;
			}
		}
		return remove;
	}
	
	
	
	/**
	 * get list of orphaned words
	 * @param cls
	 * @return
	 *
	private List<IClass> getWords(IClass cls){
		List<IClass> words = new ArrayList<IClass>();
		for(IClass w: cls.getSuperClasses()){
			if(w.getName().endsWith(WORD)){
				words.add(w);
			}
		}
		return words;
	}
	*/
	
	
	/**
	 * creates words for a given class
	 * @param cls
	 * @param name
	 *
	public void createConceptWords(IClass cls, String name){
		// break concept into words
		IClass lexicon = ont.getClass(LEXICON);
		String [] words = name.toLowerCase().split("_");
		List<IClass> wcls = new ArrayList<IClass>();
		for(String word: words){
			// check if word class exists
			String wname = word+WORD;
			IClass wc = ont.getClass(wname);
			if(wc == null && !TextTools.isStopWord(word)){
				wc = lexicon.createSubClass(wname);
			}
			// create for non-stop words
			if(wc != null)
				wcls.add(wc);
		}
		
		
		// add words as equivelent restrictions (but not for diseases)
		for(IClass p: wcls){
			cls.addSuperClass(p);
			if(!cls.hasSuperClass(ont.getClass(DISEASES))){
				IRestriction r = ont.createRestriction(IRestriction.SOME_VALUES_FROM);
				r.setProperty(ont.getProperty(HAS_CONCEPT));
				r.setParameter(p.getLogicExpression());
				cls.addEquivalentRestriction(r);
			}
		}
	}
	*/
	
	
	
	/**
	 * create appropriate concept name
	 * @param parent
	 * @return
	 */
	public String createConceptName(IClass parent, String name){
		boolean upperCase = isUpperCaseConceptName(parent,name);
		//System.out.println(parent+" "+name+" "+name.matches("[A-Z ]+"));
		// upper-case modifiers that are already upper cased
		//if(parent != null && parent.equals(ont.getClass(MODIFIERS)) && name.matches("[A-Z ]+"))
		//	upperCase = true;
		return createConceptName(upperCase,name);
	}
	
	
	
	/**
	 * determine if new name should be upper case based on parent
	 * @param parent
	 * @return
	 */
	
	public boolean isUpperCaseConceptName(IClass parent){
		return isUpperCaseConceptName(parent,null);
	}
	
	/**
	 * determine if new name should be upper case based on parent
	 * @param parent
	 * @return
	 */
	
	public boolean isUpperCaseConceptName(IClass parent,String name){
		// upper-case diseases
		IClass d = ont.getClass(DISEASES);
		boolean upperCase = parent != null && (parent.equals(d) || parent.hasSuperClass(d));
		
		// upper-case modifiers that are already upper cased
		if(parent != null && name != null && isModifier(parent) && !isFeature(parent) && name.matches("[A-Z_ ]+"))
			upperCase = true;
		
		// check if it is a stain
		d = ont.getClass(ANCILLARY_STUDIES);
		if(parent != null && (parent.equals(d) || parent.hasSuperClass(d)))
			upperCase = true;
		
		return upperCase;
	}
	
	
	/**
	 * create appropriate concept name
	 * @param parent
	 * @return
	 */
	public String createConceptName(boolean diagnosis, String name){
		// create appropriate class name
		name = getClassName(name);
		if(name.length() == 0)
			return null;
		
		
		if(diagnosis){
			name = name.toUpperCase();
		}else{
			// if single-word and mixed case, then leave it be
			// this excludes weird names like pT2a or pMX
			if(!name.matches("[a-z]+[A-Z]+[a-z0-9]*")){
				// do camelBack notation
				StringBuffer nm = new StringBuffer();
				for(String n: name.toLowerCase().split("_")){
					String w = (TextTools.isStopWord(n))?n:(""+n.charAt(0)).toUpperCase()+n.substring(1);
					nm.append(w+"_");
				}
				name = nm.toString().substring(0,nm.length()-1);
			}
		}
		return name;
	}
	
	/**
	 * 
	 * @return
	 *
	private JPanel createConceptPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		table = new UIHelper.DynamicTable(5,1);
		panel.add(new JLabel("Type one or more concepts below"),BorderLayout.NORTH);
		panel.add(new JScrollPane(table),BorderLayout.CENTER);
		return panel;
	}
	*/
	
	/**
	 * prompt user for concept names
	 * @return
	 */
	public String [] promptConceptNames(Component parent){
		//JPanel p = createConceptPanel();
		final NewConceptPanel p = new NewConceptPanel();
		timer = new UIHelper.FocusTimer(new ActionListener() {
			public void actionPerformed(ActionEvent e){
            	/*
				if(table.hasFocus()) {
					timer.setRepeats(false);
	                return;
	            }
				table.requestFocusInWindow();
				table.editCellAt(0,0);
				*/
				if(p.getTextPanel().hasFocus()){
					timer.setRepeats(false);
	                return;
				}
				p.getTextPanel().requestFocusInWindow();
			}
		});
		timer.start();
		int r = JOptionPane.showConfirmDialog(parent,p,"Create New Concepts ...",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
				Icons.getIcon(Icons.EDIT,48));
		if(r == JOptionPane.OK_OPTION){
			return p.getEntries();
			//return TextHelper.getValues(table.getValues(0)).toArray(new String [0]);
		}
		return new String [0];
	}
	
	
	/**
	 * prompt user for concept names
	 * @return
	 */
	public String promptConceptName(Component parent,Icon parentIcon, String suggestion){
		// create new panel
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		if(parentIcon != null){
			JLabel lbl = new JLabel(parentIcon);
			lbl.setBackground(Color.white);
			lbl.setBorder(new TitledBorder("Parent"));
			panel.add(lbl,BorderLayout.WEST);
		}
		
		JPanel pn = new JPanel();
		pn.setLayout(new BorderLayout());
		final JTextField text = new JTextField(30);
		if(suggestion != null)
			text.setText(suggestion);
		pn.setBorder(new TitledBorder("Concept Name"));
		pn.add(text,BorderLayout.CENTER);
		panel.add(pn,BorderLayout.CENTER);
		
		timer = new UIHelper.FocusTimer(new ActionListener() {
			public void actionPerformed(ActionEvent e){
            	if(text.hasFocus()) {
					timer.setRepeats(false);
	                return;
	            }
				text.requestFocusInWindow();
			}
		});
		// WORK around for mac
		if(!System.getProperty("os.name").toLowerCase().contains("mac"))
			timer.start();
		
		int r = JOptionPane.showConfirmDialog(parent,panel,"Create New Concept ...",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
				Icons.getIcon(Icons.EDIT,24));
		if(r == JOptionPane.OK_OPTION){
			return text.getText();
		}
		return null;
	}
	
	
	/**
	 * @param args
	 *
	public static void main(String[] args) throws Exception {
		
		DomainBuilder.setParameter("text.tools.server.url","http://slidetutor.upmc.edu/term/servlet/TextToolsServlet");
		
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost/repository";
		String user = "user";
		String pass = "resu";
		String table = "repository";
		String dir   = System.getProperty("user.home")+File.separator+".protegeRepository";
		IRepository r = new ProtegeRepository(driver,url,user,pass,table,dir);
		//IOntology ont = r.getOntology("Melanocytic.owl");
		
		File file = new File("/home/tseytlin/Melanocytic.owl");
		IOntology ont = POntology.loadOntology(file.toURI());
		ont.setRepository(r);
	
		
		// do various operations
		ConceptHandler cc = new ConceptHandler(ont);
	
		// flush everything
		//cc.flush();
		//cc.inferFindingHierarchy();
		//cc.inferDiseaseHierarchy();
		cc.inferConceptAttributes();
		//cc.inferConceptAttributes(ont.getClass("Random_Cytologic_Atypia"));
		
		showOntology(ont);
		
	}
	*/
	/*
	private static void showOntology(IOntology ont){
		OntologyExplorer e = new OntologyExplorer();
		JFrame frame = new JFrame("Ontology Explorer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(e);
		frame.pack();
		frame.setVisible(true);
		e.setRoot(ont.getRoot());
	}*/

}
