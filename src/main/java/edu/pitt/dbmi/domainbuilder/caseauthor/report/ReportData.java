/**
 * This is the object representation of concepts that are in report
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.domainbuilder.caseauthor.report;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;
import javax.swing.*;
import edu.pitt.dbmi.domainbuilder.beans.*;
import edu.pitt.dbmi.domainbuilder.caseauthor.*;
import edu.pitt.dbmi.domainbuilder.util.Eggs;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.widgets.ConceptLabel;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.lexicon.Concept;
import edu.pitt.terminology.lexicon.Source;
import edu.pitt.terminology.util.TerminologyException;
import edu.pitt.text.tools.NegEx;
import edu.pitt.text.tools.TextTools;
import gov.nih.nlm.nls.nlp.textfeatures.Sentence;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;


/**
 * This class represents information about kb
 */
public class ReportData implements  ActionListener, PropertyChangeListener {
	private final String MERGED_TERMS = "/resources/MergedTerms.lst";
	private PropertyChangeSupport pcs;
	private CaseAuthor caseAuthor;
	private NegEx negex;
	
	// this is a list of tokens that was parsed in the document
	//private SortedSet<ConceptLabel> tokens;
	private SortedSet<ConceptEntry> concepts;
	private List<ConceptEntry> negatedConcepts;
	private Comparator<ConceptEntry> conceptComparator;
	private List<String> mergedTerms;
	//private SortedMap<ConceptLabel,ConceptEntry> tokenTable;
	////
	/*
	private static Map<String,List<Concept>> expressionList;
	
	static {
		expressionList = new LinkedHashMap<String,List<Concept>>();
		expressionList.put(".*\\b(\\d+\\.\\d+)\\b.*",Collections.singletonList(new Concept(NUMERIC)));
		expressionList.put(".*\\b(\\d+)\\b.*",Collections.singletonList(new Concept(NUMERIC)));
	}*/
	
	
	/**
	 * constructor
	 */
	public ReportData(CaseAuthor ca) {
		this.caseAuthor = ca;
		pcs = new PropertyChangeSupport(this);
		pcs.addPropertyChangeListener(ca);
		
		// listen to concept selections
		for(ConceptSelector selector: caseAuthor.getConceptSelectors())
			selector.addPropertyChangeListener(this);
		
		
		
		// init list of tokens sorted by offset
		//tokens = new TreeSet<ConceptLabel>();
		//concepts  =  new LinkedList<ConceptEntry>();
		conceptComparator = new Comparator<ConceptEntry>(){
			public int compare(ConceptEntry o1, ConceptEntry o2) {
				return o1.getOffset() - o2.getOffset();
			}
		};
		//tokenTable = new TreeMap<ConceptLabel,ConceptEntry>();
		concepts  =  new TreeSet<ConceptEntry>(conceptComparator);
		negatedConcepts = new ArrayList<ConceptEntry>();
		
		negex = new NegEx();
		

		// initiate merged terms list
		mergedTerms = readMergedTerms();
	}

	
	private List<String> readMergedTerms() {
		List<String> lines = new ArrayList<String>();
		try {
			InputStream in = getClass().getResourceAsStream(MERGED_TERMS);
			if (in == null)
				return lines;

			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				line = line.trim();
				if (line.length() > 0 && !line.startsWith("#"))
					lines.add(line);
			}
			reader.close();
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return lines;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////

	
	/**
	 * Get case info
	 * @param field
	 */
	public String getCaseInfo(String field) {
		return caseAuthor.getCaseEntry().getReportSection(field);
	}
	
	/**
	 * Register recognized noun-phrase
	 * @param  list of keys (ConceptLabel) usually generated by Lexicon
	 * @param  list for error messages
	 * @return list of ConceptLabels
	 *
	public Collection<ConceptLabel> processPhrase(List<Concept> concepts, List messages) {
		List labels = new ArrayList();
		for(Concept concept: concepts){
			// create new labels
			ConceptLabel lbl = new ConceptLabel(concept);
			lbl.setReportPanel(caseAuthor.getReportPanel());
			lbl.setColor(Color.blue);
			labels.add(lbl);
						
			// add to tokens
			tokens.add(lbl);
		}
		return labels;
	}
	*/
		
	/**
	 * Register recognized negations
	 * @param NegEx object that already parsed a sentence
	 * @return list of ConceptLabels
	 */
	public Collection<ConceptLabel> processNegation(NegEx negex, List messages) {
		if(!negex.isTriggered())
			return Collections.EMPTY_LIST;
		
		List labels = new ArrayList<ConceptLabel>();
		
		IOntology ont = caseAuthor.getKnowledgeBase();
		// create initial list of concepts
		List<ConceptEntry> clist = new ArrayList<ConceptEntry>();
		for(Concept c: negex.getNegatedConcepts()){
			clist.add(new ConceptEntry(ont.getClass(c.getCode())));
		}
		
		// compact concepts
		processConcepts(clist);
		negatedConcepts.addAll(clist);
		
		// get negated
		for(Concept concept: negex.getNegations()){
			// create new labels
			ConceptLabel lbl = new ConceptLabel(concept);
			lbl.setReportPanel(caseAuthor.getReportPanel());
			lbl.setColor(Color.blue);
			labels.add(lbl);
			// add to tokens
			for(ConceptEntry e: clist){
				e.addLabel(lbl);
			}
		}
		return labels;
	}
	


	/**
	 * Register recognized sentence tokens
	 * @param  list of keys (KeyEntry) usually generated by Lexicon
	 * @param  list for error messages
	 * @return list of ConceptLabels
	 */
	public Collection<ConceptLabel> processSentence(Sentence sentence, List messages) {
		// search in lexicon
		List<Concept> keys = lookupConcepts(sentence);
		
		// filter out overlapping numbers
		filterNumbers(keys);
		filterOverlap(keys);
		
		// process phrase
		List<ConceptLabel> labels = new ArrayList<ConceptLabel>();
		for(Concept concept: keys){
			
			// create new labels
			Collection<ConceptLabel> lbl = createConceptLabels(concept,sentence.getCharOffset());
			labels.addAll(lbl);
						
			// add to tokens
			//tokens.addAll(lbl);
			ConceptEntry entry = createConceptEntry(concept);
			entry.addLabels(lbl);
			entry.setAsserted(true);
			concepts.add(entry);
			
			// process numbers
			for(ConceptLabel l: lbl)
				processNumericValues(entry,l);
		}
			
		// take care of negation
		negex.clear();
		negex.process(sentence,keys);
		labels.addAll(processNegation(negex, messages));
		
		// check eggs
		Eggs.processText(sentence.getTrimmedString());
		
		return labels;
	}

	/**
	 * create new concept entry
	 * @param concept
	 * @return
	 */
	private ConceptEntry createConceptEntry(Concept concept){
		IClass cls = concept.getConceptClass();
		String link = null;
		// make sure concept exists
		if(cls == null){
			cls = caseAuthor.getKnowledgeBase().getClass(concept.getCode());
		}
		// if concept came from external source, then
		Map map = concept.getCodes();
		if(map != null && !map.isEmpty()){
			link = ""+map.values().iterator().next();
		}
		
		// create concept entry
		ConceptEntry entry = new ConceptEntry(cls);
		if(link != null)
			entry.setResourceLink(link);
		return entry;
	}
	
	/**
	 * create one or more concept labels for a given concept
	 * @param concept
	 * @return
	 */
	private Collection<ConceptLabel> createConceptLabels(Concept concept,int offset){
		List<ConceptLabel> labels = new ArrayList<ConceptLabel>();
		
		// split concept into matched text
		// unless it is a number 
		String text = concept.getSearchString();
		//String text = concept.getText();
		if(text != null && !NUMERIC.equals(concept.getConceptClass().getName())){
			//int offset  = concept.getOffset();
			int [] map  = concept.getWordMap();
			String [] words = TextTools.getWords(text);
			//String [] words = text.split(" +");
			//System.out.println(Arrays.toString(map)+" "+Arrays.toString(words));
			
			// for every word create a separate label
			if(words.length == map.length){
				for(int i=0,j=0;i<words.length;i++){
					if(map[i] > 0){
						ConceptLabel lbl = new ConceptLabel(words[i],offset+text.indexOf(words[i],j));
						//ConceptLabel lbl = new ConceptLabel(words[i],concept.getOffset()+text.indexOf(words[i]));
						lbl.setConcept(concept);
						lbl.setReportPanel(caseAuthor.getReportPanel());
						lbl.setColor(Color.blue);
						labels.add(lbl);
					}
					j += words[i].length();
				}
			}
		}
		
		// if something went wrong, lets do a backup
		if(labels.isEmpty()){
			ConceptLabel lbl = new ConceptLabel(concept);
			lbl.setReportPanel(caseAuthor.getReportPanel());
			lbl.setColor(Color.blue);
			labels.add(lbl);
		}
		
		return labels;
	}
	
	/**
	 * filter out duplicate numbers Ex: 1.3 might produce Number, One and Three
	 * concepts
	 * 
	 * @param text
	 * @return
	 */
	private void filterNumbers(List<Concept> concepts) {
		// make sure there are no overlaps in numbers
		Concept num = null;

		// find a general number
		for (Concept c : concepts) {
			if (c.getConceptClass() != null && NUMERIC.equals(c.getConceptClass().getName())) {
				num = c;
				break;
			}
		}

		// if there is a number
		if (num != null) {
			// get number offsets
			int num_st = num.getOffset();
			int num_en = num_st + num.getText().length();

			// search all concepts for specific numbers
			List<Concept> torem = new ArrayList<Concept>();
			for (Concept c : concepts) {
				IClass cls = c.getConceptClass();
				// if found a specific number, then
				if (cls != null && cls != num.getConceptClass() && isNumber(cls) && c.getText() != null) {
					int st = c.getOffset();
					int en = st + c.getText().length();

					// if this number is within bounds of general number, then
					// it is a repeat
					if (num_st <= st && en <= num_en) {
						torem.add(c);
						// else if the other number is within bounds of previous
						// number, then discard that
					} else if (st <= num_st && num_en <= en) {
						torem.add(num);
						num = c;
						num_st = st;
						num_en = en;
					}
				}

				// if concept text is "number", it is too general to use
				if (NUMERIC.toLowerCase().equals(c.getText()))
					torem.add(c);
			}

			// remove whatever
			for (Concept r : torem) {
				for (ListIterator<Concept> it = concepts.listIterator(); it.hasNext();) {
					Concept c = it.next();
					if (c.equals(r) && c.getText().equals(r.getText())) {
						it.remove();
					}
				}
			}

		}
	}
	
	
	/**
	 * filter out duplicate numbers Ex: 1.3 might produce Number, One and Three concepts
	 * @param text
	 * @return
	 */
	private void filterOverlap(List<Concept> concepts){
		List<Concept> torem = new ArrayList<Concept>();
		
		// compare a word map of each concept w/ the rest of the concepts
		for(int i=0;i<concepts.size();i++){
			Concept a = concepts.get(i);
			int [] amap = a.getWordMap();
			for(int j=0;j<concepts.size();j++){
				Concept b = concepts.get(j);
				// text is compared 
				if(i != j){
					int [] bmap = b.getWordMap();
					
					// amap is fully contained in bmap, then remove a
					if(contains(bmap,amap)){
						torem.add(a);
					}
				}
			}
			
		}
		
		// remove filtered concepts
		concepts.removeAll(torem);
	}
	
	
	/**
	 * is one list more general
	 * @param outer
	 * @param inner
	 * @return
	 */
	private boolean contains(int [] outer, int [] inner){
		if(outer.length != inner.length)
			return false;
		
		// go over each slot
		for(int i=0;i<outer.length;i++){
			// inner covers a word, but not outer
			if(inner[i] > 0 && outer[i] == 0)
				return false;
		}
		
		return true;
	}
	
	
	/**
	 * lookup concepts
	 * @param text
	 * @return
	 */
	private List<Concept> lookupConcepts(Sentence text){
		// search in lexicon
		List<Concept> keys = new ArrayList<Concept>();
		try{
			// check out results
			for(Concept c: caseAuthor.getTerminology().search(text.getOriginalString())){
				//System.out.println(text.getOriginalString()+"|"+c.getName()+" "+c.getCode()+" "+c.getText()+" "+c.getOffset()+" "+Arrays.toString(c.getSynonyms()));
				c.getText(); // trigger offset calculation
				c.setOffset(c.getOffset()+text.getCharOffset());
				keys.add(c);
				//keys.add(new KeyEntry(c.getText(),c.getCode(),c.getOffset()+text.getCharOffset(),c));
			}
		
			//TODO: don't search stuff already found
			// add expression lookup
			//keys.addAll(lookupExpressions(text));
			// search for stuff in anatomic site terminology
			Terminology aterm = OntologyHelper.getAnatomicTerminology();
			Source src = new Source(aterm.getName(),aterm.getDescription(),""+aterm.getURI());
			for(Concept c: aterm.search(text.getOriginalString())){
				// pick a best match for hierarchy
				Concept d = new Concept(OntologyHelper.getAnatomicalClass(caseAuthor.getCaseBase(),c));
				d.setText(c.getText());
				d.setOffset(c.getOffset()+text.getCharOffset());
				d.addCode(c.getCode(),src);
				keys.add(d);
			}
			
			// add expression lookup
			keys.addAll(lookupExpressions(text));
			
			// sort by offsets
			Collections.sort(keys,new Comparator<Concept>() {
				public int compare(Concept o1, Concept o2) {
					return o1.getOffset() - o2.getOffset();
				}
				
			});
			
			
		}catch(TerminologyException ex){
			ex.printStackTrace();
		}
		return keys;
	}
	
	/**
	 * lookup concepts
	 * 
	 * @param text
	 * @return
	 */
	private List<Concept> lookupExpressions(Sentence text) {
		List<Concept> keys = new ArrayList<Concept>();

		// check a set of hard-coded, but common expressions
		for (String expr : mergedTerms) {
			Pattern pt = Pattern.compile(expr);
			Matcher mt = pt.matcher(text.getOriginalString());
			while (mt.find()) {
				for (int i = 1; i <= mt.groupCount(); i++) {
					try {
						String txt = mt.group(i);
						for (Concept c : caseAuthor.getTerminology().search(txt)) {
							c.getText(); // trigger offset calculation
							c.setOffset(c.getOffset() + text.getCharOffset() + mt.start(i));
							keys.add(c);
						}
					} catch (TerminologyException ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		return keys;
	}
	
	
	// search blacklist that has REs and find all matches
	/*
	private List<Concept> lookupExpressions(Sentence phrase ) {
		int offset = phrase.getSpan().getBeginCharacter();
		String term = phrase.getOriginalString();
		// iterate over expression
		for(String re: expressionList.keySet()){
			// match regexp from file to
			Pattern p = Pattern.compile(re,Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher( term );
			if ( m.matches() ){
				List<Concept> concepts = expressionList.get(re);
				System.out.println(concepts);
				for(int i=0;i<concepts.size();i++){
					String txt = m.group(i+1);    // THIS BETTER BE THERE,
					Concept c = new Concept(concepts.get(i));
					c.setSearchString(phrase.getOriginalString());
					c.setText(txt);
					c.setOffset(offset+term.indexOf(txt));
				}
				return concepts;
			}
		}
		return Collections.EMPTY_LIST;
	}
	*/
	
	
	
	/**
	 * concept label is being deleted
	 * @param lbl
	 */
	public void removeConceptLabel(ConceptLabel lbl){
		lbl.setDeleted(true);
		//tokens.remove(lbl);
		//System.out.println("- "+tokens);
	}

	/**
	 * give pointer to data
	 */
	public CaseEntry getCaseEntry() {
		return caseAuthor.getCaseEntry();
	}


	// protocol button presses for worksheet
	public void actionPerformed(ActionEvent e) {
		JRadioButton button = (JRadioButton) e.getSource();
		String name = button.getText();
		int i = name.indexOf(":");
		if (i > -1) {
			if (name.startsWith("<html>"))
				name = name.substring(6, i).trim();
			else
				name = name.substring(0, i).trim();
		}
	}


	/**
	 * process numeric values in concept
	 * @param entry
	 * @param lbl
	 */
	private void processNumericValues(ConceptEntry entry, ConceptLabel lbl){
		if(OntologyHelper.isNumber(entry.getConceptClass()) && !entry.hasNumericValue()){
			entry.setNumericValue(TextHelper.parseDecimalValue(lbl.getText()));
		}
	}
   
	/**
	 * clear previousle parsed concepts
	 */
	public void clear(){
		
		negatedConcepts.clear();
		negex.clear();
		concepts.clear();
	}
	
	
	/**
	 * process all tokens that are in the document
	 */
	public void processDocument(){
		// compact concepts
		long time = System.currentTimeMillis();
		UIHelper.debug("--- report analysis ---\nconcepts before: "+concepts);
		processConcepts(concepts);
		UIHelper.debug("concepts after:  "+concepts);
		UIHelper.debug("negated concepts: "+negatedConcepts);
		UIHelper.debug("time: "+(System.currentTimeMillis()-time)+" ms\n------");
		// negate concepts and proces numbers
		for(ConceptEntry e: concepts){
			int i = negatedConcepts.indexOf(e);
			if(i > -1){
				e.setAbsent(true);
				e.addLabels(negatedConcepts.get(i).getLabels());
			}
		}
		
		
		// clear tables
		//caseAuthor.getDiseaseList().removeConceptEntries();
		//caseAuthor.getDiagnosticList().removeConceptEntries();
		//caseAuthor.getPrognosticList().removeConceptEntries();
		
	
		// now re-add everything back
		boolean change = false;
		for(ConceptEntry entry: concepts){
			for(ConceptSelector selector: caseAuthor.getConceptSelectors(entry)){
				//if(selector != null){
				if(!(entry.isDisease() && entry.isAbsent()))
					selector.addConceptEntry(entry);
				if(entry.isDisease() && !entry.isAbsent()){
					pcs.firePropertyChange(ConceptSelector.CONCEPT_ADDED,null,caseAuthor.getDiseaseList());
					
				}
				//}
			}
		}
		
		// remove diagnosis that ain't there
		ArrayList<ConceptEntry> list = new ArrayList<ConceptEntry>(caseAuthor.getCaseEntry().getDiagnoses());
		for(ConceptEntry e: list){
			if(!concepts.contains(e) && !e.isAsserted()){
				caseAuthor.getDiseaseList().removeConceptEntry(e);
				change = true;
			}
		}
		
		if(change){
			pcs.firePropertyChange(ConceptSelector.CONCEPT_REMOVED,null,caseAuthor.getDiseaseList());
		}
	}
	

	/**
	 * process tokens and convert them to a set of concepts
	 */
	private void processConcepts(Collection<ConceptEntry> concepts){
		// compact concept list until it stops changing size
		int previousSize = 0;
		while(concepts.size() != previousSize){
			previousSize = concepts.size();
			ConceptEntry previous = null;
			for(ConceptEntry entry: new ArrayList<ConceptEntry>(concepts)){
				// check if you can merge concepts
				if(previous != null){
					//check if there is a common higher level concept
					ConceptEntry common = mergeConcepts(previous, entry,concepts);
					if(common == null){
						//if not,check if there is another higher level concept that can be 
						//constructed with components of current concept
						/*
						// match one way
						for(ConceptEntry comp: entry.getComponents()){
							mergeConcepts(previous,comp,concepts);
						}
						// match different way
						for(ConceptEntry comp: previous.getComponents()){
							mergeConcepts(entry,comp,concepts);
						}
						*/
					}else{
						entry = common;
					}
				}
				previous = entry;
			}
		}	
	}
		
	
	/**
	 * attempt to merge two concepts
	 * @param previous
	 * @param entry
	 * @return
	 */
	private ConceptEntry mergeConcepts(ConceptEntry previous, ConceptEntry entry, Collection<ConceptEntry> concepts){
		IClass pc = previous.getConceptClass();
		IClass ec = entry.getConceptClass();
		IClass common = getDirectCommonChild(pc,ec);
		
		// make sure that common ground is valid
		if(common != null){
			// we can't have two attributes s.a. 3 mm inferring a finding
			if(isFeature(common) && !isFeature(pc) && !isFeature(ec))
				common = null;
			
			// if previous concept is in fact more specific then current concept
			/*
			if(pc.hasSuperClass(ec)){
				int stp = previous.getOffset();
				int enp = stp+previous.getLength();
				int stc = entry.getOffset();
				int enc = stc+entry.getLength();
				// we only accept common if more specific encompas the more general
				if(!(stp <= stc && enc <= enp)){
					common = null;
				}
			}else if(pc.hasSubClass(ec)){
				int stp = pc.getConcept().getOffset();
				int enp = stp+pc.getConcept().getText().length();
				int stc = ec.getConcept().getOffset();
				int enc = stc+ec.getConcept().getText().length();
				// we only accept common if more specific encompas the more general
				if(!(stc <= stp && enp <= enc)){
					common = null;
				}
			}
			*/
		}
		
		
		
		
		if(common != null){
			ConceptEntry ne = new ConceptEntry(common);
			ne.setAsserted(true);
			ne.addLabels(previous.getLabels());
			ne.addLabels(entry.getLabels());
			ne.addComponent(previous);
			ne.addComponent(entry);
			
			//CORRECTION: previous should take precedence, MAX was arbitrary
			if(previous.hasNumericValue())
				ne.setNumericValue(previous.getNumericValue());
			else if(entry.hasNumericValue())
				ne.setNumericValue(entry.getNumericValue());
			
			
			if(previous.hasResourceLink())
				ne.setResourceLink(previous.getResourceLink());
			else if(entry.hasResourceLink())
				ne.setResourceLink(entry.getResourceLink());
			
			// update list
			concepts.remove(entry);
			concepts.remove(previous);
			concepts.add(ne);
			return ne;
		}
		return null;
	}
	
	
	/**
	 * get common parent of two classes
	 * @param c1
	 * @param c2
	 * @return
	 */
	private IClass getDirectCommonChild(IClass c1, IClass c2){
		// take care of base conditions
		if(c1.equals(c2))
			return c1;
		if(c1.hasDirectSubClass(c2))
			return c2;
		if(c2.hasDirectSubClass(c1))
			return c1;
		
		// check direct children
		/*
		for(IClass c: c1.getDirectSubClasses()){
			if(c.hasDirectSuperClass(c2))
				return c;
		}
		*/
		List<IClass> c1c = getChildren(c1);
		List<IClass> c2c = getChildren(c2);
		for(IClass c: c1c){
			if(c2c.contains(c))
				return c;
		}
		return null;
	}

	
	/**
	 * clever way to get children of a class
	 * @param cls
	 * @return
	 */
	private List<IClass> getChildren(IClass cls){
		List<IClass> subclasses = new ArrayList<IClass>();
		Collections.addAll(subclasses,cls.getDirectSubClasses());
		
		// add to the list classes that are children of identical WORD class
		/*
		String cname = cls.getName();
		if(cname.endsWith(WORD)){
			IOntology o = cls.getOntology();
			IClass cc1 = o.getClass(cname.substring(0,cname.length()-WORD.length()));
			if(cc1 != null && subclasses.contains(cc1)){
				Collections.addAll(subclasses,cc1.getDirectSubClasses());
			}
		}*/
		return subclasses;
	}
	
	
	
	
	/**
	 * highlight tokens of selected entries
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if(prop.equals(ConceptSelector.CONCEPT_SELECTED) && evt.getNewValue() != null){
			ReportDocument doc = caseAuthor.getReportPanel().getReportDocument();
			doc.clearBackground();
			ConceptEntry entry = (ConceptEntry) evt.getNewValue();
			for(ConceptLabel lbl: entry.getLabels()){
				lbl.setBackgroundColor(Color.yellow);
				lbl.update(doc);
			}
		}
		
	}	
}
