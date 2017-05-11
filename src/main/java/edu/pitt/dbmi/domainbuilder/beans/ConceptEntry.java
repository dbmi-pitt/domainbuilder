package edu.pitt.dbmi.domainbuilder.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.awt.*;
import javax.swing.*;

import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.widgets.ConceptLabel;
import edu.pitt.ontology.*;
import edu.pitt.terminology.lexicon.Concept;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;


/**
 * this class is a wrapper for a concept
 * @author tseytlin
 *
 */
public class ConceptEntry implements Serializable, Icon {
	// marker for no value
	private IClass cls;
	private transient Concept concept;
	private boolean absent, asserted, inconsistent,removed,hidden;
	private int count = 1,patternOffset;
	private double numericValue = NO_VALUE;
	private transient ConceptExpression findings;
	private java.util.List<ConceptEntry> attributes,components,children;
	private Set<ConceptEntry> recommendations;
	private SortedSet<ConceptEntry> possibleAttributes;
	private SortedSet<ConceptLabel> labels;
	private java.util.List<String> locations;
	private Set<String> parts;
	private Set<String> alternativeConcepts;
	private String example, resourceLink;
	private transient Color color;
	private transient Component comp;
	// to retire
	private ConceptEntry feature, action;
	
	
	//icon fields
	private Color incompleteColor,impliedColor,inconsistentColor,completeColor,hiddenColor;
	private Stroke stroke = new BasicStroke(2);
	private String text;
	private Point offset = new Point(4,2);
	private Font font = new Font("Dialog",Font.PLAIN,12);
	private boolean trancated;
	
	/**
	 * create new concept entry based on a class
	 * @param conceptClass
	 */
	public ConceptEntry(IClass conceptClass){
		this.cls = conceptClass;
		// setup some things
		//text = UIHelper.getPrettyClassName(cls.getName());
		
		if(isNumber(conceptClass) && !NUMERIC.equals(conceptClass.getName())){
			String str = conceptClass.getName().replaceAll("[^\\d]","");
			numericValue = TextHelper.parseDecimalValue(str);
		}
		
	}
	
	/**
	 * get a list of alternative concepts as far as dx rule is concerned
	 * @return
	 */
	public Set<String> getAlternativeConcepts(){
		if(alternativeConcepts == null)
			alternativeConcepts = new TreeSet<String>();
		return alternativeConcepts;
	}
	
	/**
	 * create new concept entry based on a class
	 * @param conceptClass
	 */
	public ConceptEntry(IInstance inst){
		//this(inst.getDirectTypes()[0]);
		Boolean b = (Boolean) getPropertyValue(inst,IS_ABSENT);
		absent = (b != null)?b.booleanValue():false;
		Float d = (Float) getPropertyValue(inst,HAS_NUMERIC_VALUE);
		numericValue = (d != null)?d.floatValue():NO_VALUE;
		String link = (String) getPropertyValue(inst,HAS_RESOURCE_LINK);
		if(link != null)
			resourceLink = link;
		
		// convert instances based class to knowledge base class
		IClass c = inst.getDirectTypes()[0];
		IOntology o = OntologyHelper.getKnowledgeBase(inst.getOntology());
		this.cls = o.getClass(c.getName());
	}
	
	/**
	 * create new concept entry based on a class
	 * @param conceptClass
	 *
	public ConceptEntry(String name){
		//this.cls = conceptClass;
		// setup some things
		text = name;
	}
	*/
	
	/**
	 * update references
	 */
	public void updateReference(IOntology ont){
		cls = ont.getClass(cls.getName());
		if(cls == null)
			setInconsistent(true);
		
		
		if(feature != null && feature != this)
			feature.updateReference(ont);
		
		if(action != null)
			feature.updateReference(ont);
		
			
		if(possibleAttributes != null)
			for(ConceptEntry e: possibleAttributes)
				e.updateReference(ont);
		
		
		if(attributes != null)
			for(ConceptEntry e: attributes)
				e.updateReference(ont);
		
		if(components != null)
			for(ConceptEntry e: components)
				e.updateReference(ont);
		
		if(children != null)
			for(ConceptEntry e: children)
				e.updateReference(ont);
		
		if(findings != null)
			findings.updateReference(ont);
	}
	
	public ConceptEntry clone(){
		ConceptEntry e = new ConceptEntry(cls);
		e.setAbsent(absent);
		return e;
	}
	
	
	/**
	 * copy values from other entries
	 */
	public void copyFrom(ConceptEntry e){
		// add locations
		getLocations().addAll(e.getLocations());
		
		// copy labels
		addLabels(e.getLabels());
		
		// copy parts
		setParts(e.getParts());
		
		// add example
		setExampleMap(e.getExampleMap());
		
		// add optimal
		if(e.isAbsent())
			setAbsent(e.isAbsent());
		if(!isAsserted())
			setAsserted(e.isAsserted());
		if(!isInconsistent())
			setInconsistent(e.isInconsistent());
		if(e.hasNumericValue())
			setNumericValue(e.getNumericValue());
		if(e.hasResourceLink())
			setResourceLink(e.getResourceLink());
		
	}
	
	/**
	 * get pretty name
	 * @return
	 */
	public String getText(){
		if(cls == null)
			return "";
		
		String n = (isAbsent())?"NO ":"";
		String txt = n+UIHelper.getPrettyClassName(cls.getName());
		if(hasNumericValue()){
			txt = txt.replaceAll(NUMERIC.toLowerCase(),getNumericValueString());
		}
		// if class is anatomic location
		if(OntologyHelper.isAnatomicLocation(cls)){
			// try labels first
			// else use external resource link
			if(getLabels().length > 0){
				txt = getLabels()[0].getConcept().getText().toLowerCase();
				//System.out.println(txt);
			}else if(hasResourceLink()){
				txt = UIHelper.getPrettyClassName(resourceLink);
			}
		}
		
		return txt;
	}
	
	
	String getConceptText(){
		return text;
	}
	
	
	public String getInstanceName(){
		if(cls == null)
			return "";
		
		String name = cls.getName().toLowerCase();
		if(absent)
			name = NO+name;
		else if(hasNumericValue())
			name = name.replaceAll(NUMERIC.toLowerCase(),(""+numericValue).replaceAll("\\.","_"));
		else if(hasResourceLink())
			name = OntologyHelper.getClassName(getText()).toLowerCase();
		return name;
	}
	
	
	/**
	 * format numeric value as string
	 * @return
	 */
	public String getNumericValueString(){
		if(hasNumericValue()){
			return TextHelper.toString(numericValue);
		}
		return "";
	}
	
	
	/**
	 * get  name
	 * @return
	 */
	public String getName(){
		return (cls != null)?cls.getName():"UNKNOWN";
	}
	
	
	/**
	 * rename the actual class
	 * WARNING: Use with caution
	 * @param name
	 */
	public void setName(String name){
		name = getClassName(name);
		IOntology ont = cls.getOntology();
		cls.setName(name);
		cls = ont.getClass(name);
		// setup some things
		text = UIHelper.getPrettyClassName(cls.getName());
		trancated = false;
	}
	
	
	/**
	 * get description
	 * @return
	 */
	public String getDescription(){
		return (cls != null)?cls.getDescription():"Unresolved Class";
	}
	
	/**
	 * get description
	 * @return
	 */
	public void setDescription(String s){
		if(cls != null)
			cls.setDescription(s);
	}
	
	/**
	 * @return the concept
	 */
	public Concept getConcept() {
		if(cls == null)
			return null;
		
		if(concept == null)
			concept = cls.getConcept();
		return concept;
	}
	
	/**
	 * @param concept the concept to set
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	/**
	 * @return the conceptClass
	 */
	public IClass getConceptClass() {
		return cls;
	}

	public IInstance getInstance(){
		// TODO:
		return null;
	}
	
	public int getCount() {
		return count;
	}


	public void setCount(int count) {
		this.count = count;
	}
	
	/**
	 * get children of a concept
	 * @return
	 */
	public ConceptEntry [] getChildren(){
		if(cls == null)
			return new ConceptEntry[0];
		
		if(children == null){
			// get list of children
			IClass [] c = cls.getDirectSubClasses();
			children = new ArrayList<ConceptEntry>();
			
			// get list of parents
			List<IClass> parents = Arrays.asList(cls.getSuperClasses());
			
			// add to children and avoid loops
			for(int i=0;i<c.length;i++){
				if(!parents.contains(c[i]))
					children.add(new ConceptEntry(c[i]));
			}
			
			// sort now
			Collections.sort(children,new Comparator<ConceptEntry>(){
				public int compare(ConceptEntry a1, ConceptEntry a2) {
					return a1.getName().compareTo(a2.getName());
				}
			});
		}
		return children.toArray(new ConceptEntry [0]);
	}
	

	public ConceptEntry getAction() {
		if(action == null && cls != null){
			IOntology o = cls.getOntology();
			IRestriction [] acts = cls.getRestrictions(o.getProperty(HAS_ACTION));
			if(acts.length > 0){
				Object obj = acts[0].getParameter().getOperand();
				// this should always be the case
				if(obj instanceof IClass){
					action = new ConceptEntry((IClass) obj);
				}
			}
		}
		return action;
	}

	public void setAction(ConceptEntry action) {
		this.action = action;
	}


	
	/**
	 * @param conceptClass the conceptClass to set
	 */
	public void setConceptClass(IClass conceptClass) {
		this.cls = conceptClass;
	}
	
	
	/**
	 * display preferred concept name
	 */
	public String toString(){
		//return text; 
		return (cls != null)?cls.getName():null;
	}
	
	/**
	 * check if one entry equals to another
	 */
	public boolean equals(Object obj){
		if(obj instanceof ConceptEntry){
			ConceptEntry e = (ConceptEntry) obj;
			// check if classes are ok
			if(cls != null && obj  != null)
				return cls.equals(e.getConceptClass());
			// if both classes are null check text
			if(cls == null && e.getConceptClass() == null)
				return getConceptText() == e.getConceptText();
		}
		return false;
	}
	
	/**
	 * obligatory hash code business
	 */
	public int hashCode(){
		return (cls != null)?cls.getURI().hashCode():0;
	}
	
	/**
	 * get icon height
	 */
	public int getIconHeight(){
		return Icons.CONCEPT_ICON_HEIGHT;
	}
	/**
	 * get icon width
	 */
	public int getIconWidth(){
		return Icons.CONCEPT_ICON_WIDTH;
	}
	
	/**
	 * get proper color
	 * @return
	 */
	private Color getColor(){
		if(color != null)
			return color;
		
		// setup colors
		if(incompleteColor == null){
			incompleteColor = new Color(255,150,0,50);
		}
		if(completeColor == null){
			completeColor = new Color(100,255,100,50);
		}
		if(impliedColor == null){
			impliedColor = new Color(150,150,150,50);
		}
		if(inconsistentColor == null){
			inconsistentColor = new Color(255,100,100,50);
		}
		if(hiddenColor == null){
			hiddenColor = new Color(200,200,200,50);
		}
		
		
		// return color
		if(isHidden())
			return hiddenColor;
		if(isInconsistent())
			return inconsistentColor;
		if(isComplete())
			return completeColor;
		if(isIncomplete())
			return incompleteColor;
		return impliedColor;
	}
	
	
	/**
	 * get outline color
	 * @return
	 */
	private Color getOutlineColor(){
		if(isHidden())
			return Color.gray;
		//!isInconsistent()
		return (isAsserted())?Color.green:Color.black;
	}
	
	/**
	 * paint graphics
	 */
	public void paintIcon(Component c, Graphics g, int x, int y){
		comp = c;
		int w = getIconWidth()-2*offset.x;
		int h = getIconHeight()-2*offset.y;
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(stroke);
		
		// get textual representation
		if(text == null){
			text = getText();
			trancated = false;
		}
		
		// fill rectangle
		g.setColor(getColor());
		g.fillRect(x+offset.x,y+offset.y,w,h);
		
		// truncate text
		if(!trancated){
			FontMetrics fm = c.getFontMetrics(font);
			int n = fm.stringWidth(text);
			while((n+12) > getIconWidth()){
				text = text.substring(0,text.length()-1);
				n = fm.stringWidth(text);
			}
			trancated = true;
		}
		// draw text
		g.setFont(font);
		g.setColor(isHidden()?Color.lightGray:Color.black);
		g.drawString(text,x+5+offset.x,y+h-7+offset.x);
		// do outline
		g.setColor(getOutlineColor());
		g.drawRect(x+offset.x,y+offset.y,w,h);
	}


	/**
	 * @return the absent
	 */
	public boolean isAbsent() {
		return absent;
	}


	/**
	 * @param absent the absent to set
	 */
	public void setAbsent(boolean absent) {
		this.absent = absent;
		trancated = false;
		text = null;
		/*
		if(absent && !text.toLowerCase().startsWith("no")){
			text = "NO "+text;
		}else if(!absent && text.toLowerCase().startsWith("no ")){		
			text = text.substring(3);
		}
		*/
	}

	public boolean isComplete(){
		//return isAbsent() || 
		return (getLocations().size() > 0 && getExamples().size() > 0);
	}
	
	public boolean isIncomplete(){
		
		//return isAbsent() || 
		return (getLocations().size() > 0 || getExamples().size() > 0);
	}
	

	/**
	 * @return the asserted
	 */
	public boolean isAsserted() {
		return asserted;
	}


	/**
	 * @param asserted the asserted to set
	 */
	public void setAsserted(boolean asserted) {
		this.asserted = asserted;
	}

	public boolean isHidden() {
		return hidden;
	}


	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}



	/**
	 * @return the inconsistent
	 */
	public boolean isInconsistent() {
		return inconsistent;
	}


	/**
	 * @param inconsistent the inconsistent to set
	 */
	public void setInconsistent(boolean inconsistent) {
		this.inconsistent = inconsistent;
	}


	/**
	 * is this entry an attribute
	 * @return
	 */
	public boolean isAttribute(){
		return OntologyHelper.isAttribute(cls);
	}
	
	
	
	/**
	 * check if this entry is a feature NOTE that FAVs are also features
	 * @return
	 */
	public boolean isFeature(){
		return OntologyHelper.isFeature(cls);
	}
	
		
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public boolean isDisease(){
		return OntologyHelper.isDisease(cls);
	}
	
		
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public boolean isDiagnosticFeature(){
		return OntologyHelper.isDiagnosticFeature(cls);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public boolean isPrognosticFeature(){
		return OntologyHelper.isPrognosticFeature(cls);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public boolean isAncillaryStudy(){
		return OntologyHelper.isAncillaryStudy(cls);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public boolean isRecommendation(){
		return OntologyHelper.isRecommendation(cls);
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public boolean isClinicalFeature(){
		return OntologyHelper.isClinicalFeature(cls);
	}
	
	
	
	/**
	 * check if this entry is a feature attribute value
	 * compbination of feature and attribute
	 * @return
	 */
	public boolean isConceptAttribute(){
		/*
		IOntology o = cls.getOntology();
		IProperty p = null;
		if(isFeature())
			p = o.getProperty(HAS_CONCEPT);
		else if(isDisease())
			p = o.getProperty(HAS_DISEASE);
		return p != null && cls.getRestrictions(p).length > 0;
		*/
		return (isFeature() || isDisease()) && isAttribute();
	}
	
	public void setAttributes(ConceptEntry [] e){
		Collections.addAll(attributes,e);
	}
	
	
	
	/**
	 * @return the feature
	 */
	public ConceptEntry getFeature() {
		if(cls == null)
			return null;
		
		if(feature == null){
			//IClass c = findFeature(cls);
			IClass c = OntologyHelper.getFeature(cls);
			if(c != null)
				feature = (c.equals(cls))?this:new ConceptEntry(c);
			
			// default
			if(feature == null)
				feature = this;
		}
		return feature;
	}

	
	/**
	 * @param feature the feature to set
	 */
	public void setFeature(ConceptEntry feature) {
		this.feature = feature;
	}

	
	/**
	 * get attributes from a feature
	 * @return
	 */
	public ConceptEntry [] getAttributes(){
		if(cls == null)
			return new ConceptEntry[0];
		
		if(attributes == null){
			attributes = new ArrayList<ConceptEntry>();
			//for(IClass parent:OntologyHelper.findAttributes(cls,getFeature().getConceptClass())){
			for(IClass parent:OntologyHelper.getAttributes(cls)){	
				attributes.add(new ConceptEntry(parent));
			}
			
		}
		return attributes.toArray(new ConceptEntry[0]);
	}
	
	/**
	 * @return the attributes
	 */
	public ConceptEntry[] getPotentialAttributes() {
		if(cls == null)
			return new ConceptEntry[0];
		
		if(possibleAttributes == null){
			possibleAttributes = new TreeSet<ConceptEntry>(new Comparator<ConceptEntry>(){
				public int compare(ConceptEntry a1, ConceptEntry a2) {
					return a1.getName().compareTo(a2.getName());
				}
			});
			for(IClass parent:OntologyHelper.getPotentialAttributes(cls)){	
				possibleAttributes.add(new ConceptEntry(parent));
			}
			
			/*
			// unique set of attributes
			possibleAttributes = new TreeSet<ConceptEntry>(new Comparator<ConceptEntry>(){
				public int compare(ConceptEntry a1, ConceptEntry a2) {
					return a1.getName().compareTo(a2.getName());
				}
			});
			// check if direct children are in fact findings (FAVS)
			boolean isFAV = false;
			for(IClass child: getFeature().getConceptClass().getDirectSubClasses()){
				if(OntologyHelper.isAttribute(child)){
					isFAV = true;
					break;
				}
			}
			
			// get list of all subclasses
			if(isFAV){
				for(IClass child: getFeature().getConceptClass().getSubClasses()){
					for(IClass parent:child.getDirectSuperClasses()){
						if(OntologyHelper.isAttribute(parent) && (!isSystemClass(parent) || isValue(parent)) && 
						   (!(OntologyHelper.isFeature(parent) || OntologyHelper.isDisease(parent)) || isLocation(parent))){
							//don't add upper-cased attributes, cause they are not values but categories
							//if(!isAttributeCategory(parent))
							possibleAttributes.add(new ConceptEntry(parent));
						}
					}
				}
			}else{
				//get list of all children
				for(IClass child: getFeature().getConceptClass().getSubClasses()){
					possibleAttributes.add(new ConceptEntry(child));
				}
			}
			*/
		}
		return possibleAttributes.toArray(new ConceptEntry [0]);
	}
	
	/**
	 * set potential attributes
	 * @param e
	 */
	public void setPotentialAttributes(ConceptEntry [] e){
		possibleAttributes = new TreeSet<ConceptEntry>(new Comparator<ConceptEntry>(){
			public int compare(ConceptEntry a1, ConceptEntry a2) {
				return a1.getName().compareTo(a2.getName());
			}
		});
		possibleAttributes.addAll(Arrays.asList(e));
	}
	
	
	/**
	 * @param attributes c.setFeature(this);
							the attributes to set
	 */
	public void addAttribute(ConceptEntry  attr) {
		if(attributes == null)
			attributes = new ArrayList<ConceptEntry>();
		attributes.add(attr);
	}
	
	
	/**
	 * @param attributes the attributes to set
	 */
	public void removeAttribute(ConceptEntry  attr) {
		if(attributes != null)
			attributes.remove(attr);
	}
	
	

	/**
	 * @return the locationStrings
	 */
	public List<String> getLocations() {
		if(locations == null){
			// make it a set
			locations = new ArrayList<String>(){
				public boolean add(String e) {
					if(!contains(e))
						return super.add(e);
					return false;
				}
				public boolean addAll(Collection<? extends String> c) {
					for(String x: c)
						add(x);
					return true;
				}
				
				
			};
		}
		return locations;
	}

	/**
	 * @param locations the locationStrings to set
	 */
	public void setLocations(List<String> loc) {
		locations = loc;
	}

	/**
	 * @param locations the locationStrings to set
	 */
	public void addLocation(String str) {
		getLocations().add(str);
		asserted = true;
	}

	/**
	 * @param locations the locationStrings to set
	 */
	public void removeLocation(String str) {
		if(locations != null)
			locations.remove(str);
	}
	
	
	/**
	 * is there an annotation
	 * @param name
	 * @return
	 */
	public boolean hasAnnotation(String name){
		if(getLocations().contains(name))
			return true;
		if(getExamples().contains(name))
			return true;
		return false;
	}
	
	/**
	 * remove annotation
	 * @param name
	 */
	public void removeAnnotation(String name){
		getLocations().remove(name);
		if((""+example).equals(name))
			example = null;
		if(feature != null && (""+feature.getExample()).equals(name)){
			feature.setExample(null);
		}
		if(attributes != null){
			for(ConceptEntry e: attributes){
				if((""+e.getExample()).equals(name)){
					e.setExample(null);
				}
			}
		}
	}
	
	/**
	 * remove annotation
	 * @param name
	 */
	public void replaceAnnotation(String oname, String name){
		if(getLocations().remove(oname)){
			getLocations().add(name);
		}
		if((""+example).equals(oname))
			example = name;
		if(feature != null && (""+feature.getExample()).equals(oname)){
			feature.setExample(name);
		}
		if(attributes != null){
			for(ConceptEntry e: attributes){
				if((""+e.getExample()).equals(oname)){
					e.setExample(name);
				}
			}
		}
	}
	
	
	/**
	 * @return the exampleString
	 */
	public String getExample(ConceptEntry e){
		if(e.equals(this))
			return example;
		else if(e.equals(getFeature()))
			return feature.getExample(e);
		else{
			for(ConceptEntry a: getAttributes()){
				if(a.equals(e))
					return a.getExample(e);
			}
		}
		return null;
	}
	
	
	/**
	 * get mappings from examples
	 * @return
	 */
	public Map<String,String> getExampleMap(){
		Map<String,String> map = new HashMap<String,String>();
		ConceptEntry f = getFeature();
		if(f.getExample() != null)
			map.put(f.getName(),f.getExample());
		for(ConceptEntry a: getAttributes()){
			if(a.getExample() != null)
				map.put(a.getName(),a.getExample());
		}
		return map;
	}
	
	public void setExampleMap(Map<String,String> map){
		ConceptEntry f = getFeature();
		ConceptEntry [] as = getAttributes();
		for(String key: map.keySet()){
			if(f != null && f.getName().equals(key))
				f.setExample(map.get(key));
			for(ConceptEntry a: as){
				if(a.getName().equals(key))
					a.setExample(map.get(key));
			}
		}
	}
	
	
	
	/**
	 * @return the exampleString
	 */
	public String getExample() {
		return example;
	}
	
	/**
	 * @return the exampleString
	 */
	public List<String> getExamples() {
		if(isConceptAttribute()){
			ArrayList<String> list = new ArrayList<String>();
			String ex = getFeature().getExample();
			if(ex != null)
				list.add(ex);
			for(ConceptEntry e: getAttributes()){
				list.addAll(e.getExamples());
			}
			return list;
		}else if(example != null){
			return Collections.singletonList(example);
		}else{
			return Collections.emptyList();
		}
	}
	
	


	/**
	 * @param exampleString the exampleString to set
	 */
	public void setExample(String s) {
		if(s == null || s.length() == 0 || s.equals("null"))
			s = null;
		if(isConceptAttribute() && !getFeature().equals(this)){
			getFeature().setExample(s);
			for(ConceptEntry a: getAttributes())
				a.setExample(s);
		}else{
			this.example = s;
		}
		asserted = true;
	}
	
	
	public int getPatternOffset() {
		return patternOffset;
	}

	public void setPatternOffset(int patternOffset) {
		this.patternOffset = patternOffset;
	}


	public int getPatternCount(){
		if(!isDisease())
			return 0;
		// init findings
		if(findings == null)
			getFindings(); 
		return (findings.getExpressionType() == ILogicExpression.OR)?findings.size():1;
	}
	
	/**
	 * creates new pattern
	 * @return pattern offset
	 * @return
	 *
	public int createNewPattern(){
		int n = 0;
		getFindings();
		if(findings.getExpressionType() == ILogicExpression.OR){
			findings.add(new ConceptExpression(ILogicExpression.AND));
		}else{
			ConceptExpression exp = new ConceptExpression(ILogicExpression.OR);
			exp.add(findings);
			exp.add(new ConceptExpression(ILogicExpression.AND));
			findings = exp;
		}
		// assign last pattern
		n = findings.size()-1;
		return n;
	}
	*/
	
	/**
	 * add new disease pattern
	 * @param sibling
	 */
	public void addNewPattern(ConceptEntry sibling){
		ConceptExpression all = sibling.getCompleteFindings();
		if(all.getExpressionType() == ILogicExpression.OR){
			all.add(new ConceptExpression(ILogicExpression.AND));
		}else{
			ConceptExpression exp = new ConceptExpression(ILogicExpression.OR);
			exp.add(all);
			exp.add(new ConceptExpression(ILogicExpression.AND));
			sibling.setCompleteFindings(exp);
			all = exp;
		}
		setCompleteFindings(all);
		// assign last pattern
		patternOffset = all.size()-1;
	}
	
	
	
	/**
	 * get findings expression for a diagnosis
	 * @return
	 */
	
	public void setCompleteFindings(ConceptExpression exp){
		findings = exp;
	}
	
	/**
	 * get compete findings
	 * @return
	 */
	public ConceptExpression getCompleteFindings(){
		getFindings();
		return findings;
	}
	
	
	/**
	 * get findings expression for a diagnosis
	 * @return
	 */
	public ConceptExpression getFindings() {
		//System.out.println(this);
		if(cls == null || !isDisease())
			return new ConceptExpression(ILogicExpression.EMPTY);
		
		// create new expression
		if(findings == null){
			ILogicExpression s = cls.getEquivalentRestrictions();
			//System.out.println(cls+" eq rest: "+s);
			int type = (s.isEmpty())?ILogicExpression.AND:s.getExpressionType();
			findings =  getFindingExpression(s,null,new ConceptExpression(type));
		}
		//System.out.println(findings+"\n");
		
		// check if this is a multi-pattern diagnosis
		if(findings.getExpressionType() == ILogicExpression.OR){
			ConceptExpression exp = new ConceptExpression(ILogicExpression.AND);
			if(patternOffset < findings.size()){
				Object obj = findings.get(patternOffset);
				if(obj instanceof ConceptExpression){
					exp = (ConceptExpression) obj;
				}else{
					exp.add(obj);
					// replace
					findings.set(patternOffset,exp);
				}	
			}
			return exp;
		}else
			return findings;
	}

	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	private ConceptExpression getFindingExpression(ILogicExpression source, IProperty prop, ConceptExpression target){
		//target.setExpressionType(source.getExpressionType());
		for(Object obj : source){
			if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				// if it there is more then one thing as parameter, nest the expression
				// so that when we recurse, the internal expression is preserved
				ILogicExpression param = (r.getParameter().size() == 1)?
				r.getParameter():new LogicExpression(r.getParameter());
				getFindingExpression(param,p,target);
			}else if(obj instanceof IClass && prop != null){
				// convert class to a concept entry
				IClass c = (IClass) obj;
				ConceptEntry entry = new ConceptEntry(c);
				if(prop.getName().contains(HAS_NO_FINDING)){
					entry.setAbsent(true);
					target.add(entry);
				}else if(prop.getName().contains(HAS_FINDING)  || 
				   prop.getName().contains(HAS_CLINICAL) || 
				   prop.getName().contains(HAS_ANCILLARY)){
					target.add(entry);
				}
			}else if(obj instanceof ILogicExpression){
				// recurse into expression
				ILogicExpression src = (ILogicExpression) obj;
				target.add(getFindingExpression(src,prop,
						  new ConceptExpression(src.getExpressionType())));
			}
		}
		return target;
	}
	
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void addLabel(ConceptLabel lbl){
		if(labels == null){
			labels = new TreeSet<ConceptLabel>();
		}
		labels.add(lbl);
	}
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void addLabels(ConceptLabel [] lbl){
		if(labels == null){
			labels = new TreeSet<ConceptLabel>();
		}
		for(int i=0;i<lbl.length;i++)
			labels.add(lbl[i]);
	}
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void addLabels(Collection<ConceptLabel> lbl){
		if(labels == null){
			labels = new TreeSet<ConceptLabel>();
		}
		labels.addAll(lbl);
	}
	
	
	/**
	 * add concept label
	 * @param lbl
	 */
	public void removeLabel(ConceptLabel lbl){
		if(labels != null)
			labels.remove(lbl);
	}
	
	/**
	 * get all labels
	 * @return
	 */
	public ConceptLabel [] getLabels(){
		// filter deleted labels
		ConceptLabel [] list = new ConceptLabel [0];
		if(labels != null){
			list = new ConceptLabel [labels.size()];
			ArrayList<ConceptLabel> toremove = new ArrayList<ConceptLabel>();
			int i = 0;
			for(ConceptLabel lbl:labels){
				if(lbl.isDeleted())
					toremove.add(lbl);
				else
					list[i++] = lbl;
			}
			if(!toremove.isEmpty()){
				for(ConceptLabel lbl: toremove){
					removeLabel(lbl);
				}
				list = labels.toArray(new ConceptLabel [0]);
			}
		}
		//return (labels == null)?new ConceptLabel[0]:labels.toArray(new ConceptLabel [0]);
		return list;
	}
	
	/**
	 * remove deleted labels
	 */
	public void removeDeletedLabels(){
		
	}
	
	
	/**
	 * if concept in text get its offset
	 * @return
	 */
	public int getOffset(){
		if(labels != null && !labels.isEmpty()){
			return labels.first().getOffset();
		}
		return 0;
	}
	
	public int getLength(){
		if(concept != null)
			return concept.getText().length();
		else if(labels != null){
			int n = 0;
			for(ConceptLabel l: labels){
				n+=l.getLength();
			}
			return (labels.size()> 1)?n-1:n;
		}
		return 0;
	}
	
	public void addComponent(ConceptEntry e){
		if(components == null)
			components = new ArrayList<ConceptEntry>();
		components.add(e);
	}
	
	public java.util.List<ConceptEntry> getComponents(){
		if(components != null)
			return components;
		return Collections.emptyList();
	}
	
	/**
	 * flash a concept
	 */
	public void flash(Component c){
		final Component comp = c;
		color = Color.YELLOW;
		comp.repaint();
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				UIHelper.sleep(500);
				color = null;
				comp.repaint();
			}
		});
		
	}
	
	/**
	 * flash a concept
	 */
	public void flash(){
		Component c;
		for(c = comp; c != null && !(c instanceof JList) ; c = c.getParent());
		if(c != null)
			flash(c);
	}
	
	
	
	/**
	 * get properties representation of this object
	 * @return
	 */
	public Properties getProperties(){
		Properties p = new Properties();
		p.setProperty("name",getName());
		p.setProperty("is.absent",""+absent);
		p.setProperty("is.asserted",""+asserted);
		p.setProperty("parts",""+getParts());
		if(hasResourceLink())
			p.setProperty("resource.link",resourceLink);
		if(hasNumericValue())
			p.setProperty("numeric.value",getNumericValueString());
		p.setProperty("locations",""+getLocations());
		ConceptEntry f = getFeature();
		if(f != null)
			p.setProperty(f.getName()+".example",TextHelper.toString(f.getExample()));
		for(ConceptEntry a: getAttributes()){
			p.setProperty(a.getName()+".example",TextHelper.toString(a.getExample()));
		}
		p.setProperty("recommendations",""+getRecommendations());
		return p;
	}
	
	/**
	 * mark that this feature belongs to a distinct part of a case
	 * @param str
	 */
	public void addPart(String str){
		if(parts == null){
			parts = new LinkedHashSet<String>();
		}
		parts.add(str);
	}
	
	/**
	 * mark that this feature belongs to a distinct part of a case
	 * @param str
	 */
	public void removePart(String str){
		if(parts != null){
			parts.remove(str);
		}	
	}
	
	/**
	 * get all parts (bins) that this concept belongs to
	 * if the set is empty, the concepts should belong to all parts
	 * @return
	 */
	public Set<String> getParts(){
		if(parts == null)
			parts = new LinkedHashSet<String>();
		return parts;
	}
	
	/**
	 * set parts for a concept entry
	 * @param p
	 */
	public void setParts(Set<String> p){
		getParts().addAll(p);
	}
	
	/**
	 * get link to externa resource
	 * @return
	 */
	public String getResourceLink() {
		return resourceLink;
	}

	public boolean hasResourceLink(){
		return resourceLink != null;
	}

	/**
	 * set link to external resource
	 * @param resourceLink
	 */
	public void setResourceLink(String resourceLink) {
		this.resourceLink = resourceLink;
	}


	/**
	 * get properties representation of this object
	 * @return
	 */
	public void setProperties(Properties p){
		p.setProperty("name",getName());
		absent = Boolean.parseBoolean(p.getProperty("is.absent",""+absent));
		asserted = Boolean.parseBoolean(p.getProperty("is.asserted",""+asserted));
		numericValue = Double.parseDouble(p.getProperty("numeric.value",""+NO_VALUE));
		resourceLink = p.getProperty("resource.link");
		
		// add locations
		for(String s: TextHelper.parseList(p.getProperty("locations",""+getLocations())))
			addLocation(s);
		
		// add parts
		for(String s : TextHelper.parseList(p.getProperty("parts",""))){
			addPart(s);
		}
		
		//add example to feature and attributes
		ArrayList<ConceptEntry> list = new ArrayList<ConceptEntry>();
		list.add(getFeature());
		Collections.addAll(list,getAttributes());
		Set<String> keys = new HashSet<String>();
		for(ConceptEntry f: list){
			keys.add(f.getName()+".example");
			String [] ex = TextHelper.parseList(p.getProperty(f.getName()+".example"));
			if(ex.length > 0)
				f.setExample(ex[0]);
		}
		
		// see if there are any stray examples
		for(Object key : p.keySet()){
			if(!keys.contains(key) && (""+key).endsWith(".example")){
				if(getFeature().getExample() == null){
					String [] ex = TextHelper.parseList(p.getProperty(""+key));
					if(ex.length > 0)
						getFeature().setExample(ex[0]);
				}
			}
		}
		
		// set recommendations
		for(String s: TextHelper.parseList(p.getProperty("recommendations",""))){
			IClass c = cls.getOntology().getClass(s.trim());
			if(OntologyHelper.isRecommendation(c)){
				getRecommendations().add(new ConceptEntry(c));
			}
		}
		
	}

	
	/**
	 * has numeric value
	 * @return
	 */
	public boolean hasNumericValue(){
		return numericValue != NO_VALUE;
	}

	public double getNumericValue() {
		return numericValue;
	}


	public void setNumericValue(double numericValue) {
		this.numericValue = numericValue;
		text = null;
	}
	
	
	/**
	 * is this entry marked for removal???
	 * NOTE: no classes get deleted when this method is used
	 * @return
	 */
	public boolean isRemoved() {
		return removed;
	}

	/**
	 * mark entry for removal, if anyone cares
	 * doesn't do any removal itself
	 * NOTE: no classes get deleted when this method is used
	 * @param removed
	 */
	public void setRemoved(boolean removed) {
		this.removed = removed;
	}

	public Set<ConceptEntry> getRecommendations() {
		if(recommendations == null)
			recommendations = new LinkedHashSet<ConceptEntry>();
		return recommendations;
	}


	public void setRecommendations(Collection<ConceptEntry> recommendations) {
		getRecommendations().clear();
		getRecommendations().addAll(recommendations);
	}



}
