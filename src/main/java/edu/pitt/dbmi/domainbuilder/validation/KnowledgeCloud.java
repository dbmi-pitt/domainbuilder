package edu.pitt.dbmi.domainbuilder.validation;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

//import edu.stanford.smi.protege.model.*;
import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.DomainBuilderComponent;


import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.ScrollableFlowLayout;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.widgets.DomainSelectorPanel;
import edu.pitt.ontology.*;
//import edu.pitt.pathtutor.util.Utils;
import edu.pitt.slideviewer.*;
import edu.pitt.text.tools.TextTools;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;



public class KnowledgeCloud extends JPanel implements ActionListener, DomainBuilderComponent {
		
	// fields
	private JPanel cloudPanel;
	private JPanel featurePanel,diagnosisPanel,casesPanel;
	private JMenuItem close, legend, refresh,glossary, bookmark, clearBookmarks;
	private JCheckBoxMenuItem showParameters;
	private JTextField search;
	private JLabel status;
	private JProgressBar progress;
	private JMenuBar menu;
	private JToolBar toolbar;
	private JPopupMenu popup;
	private ConceptListener listener;
	private DomainSelectorPanel domainSelector;
	// temp maps
	private Map<String,Concept> featureMap,diagnosisMap,casesMap;
	private Map<String,String> parameterMap;
	private IOntology ontology;
	
	
	//private URL server, servlet;
	//private String domain = "melanocytic";
	//private Map learnMap,observeMap;
	//private String observableType;
	private boolean loaded;
	private static KnowledgeCloud instance;
	//private static Map learnedParam, observedParam;
	private static boolean isStandAlone;
	
	private String legendText = "<html><table border=1>"+
		"<tr><td>Cases:</td><td> bold -> difficult, <font color='red'>red</font> -> not ready</td></tr>"+
		"<tr><td>Diagnosis:</td><td> label size -> frequency</td></tr>" +
		"<tr><td>Findings/<br>Diagnosis:</td><td><font color='red'>red</font>" +
		" -> no definition or example<br>" +
		"<font color='green'>green</font> -> example and definition</td></tr>";
		
	
	/**
	 * get icon
	 */
	public Icon getIcon() {
		return Icons.getIcon(Icons.PROPERTIES,16);
	}

	/**
	 * get name
	 */
	public String getName() {
		return "Validation";
	}

	/**
	 * listen for property changes
	 */
	public void propertyChange(PropertyChangeEvent evt){
		String cmd = evt.getPropertyName();
		if(cmd.equals(OntologyHelper.KB_LOADED_EVENT)){
			URI uri = (URI) evt.getNewValue();
			// load kb
			ontology = DomainBuilder.getRepository().getOntology(uri);	
			loaded = false;
			// load if currently showing
			if(isShowing())
				load();
		}else if(CASE_KB_RELOADED_EVENT.equals(cmd) || CASE_KB_UPDATED_EVENT.equals(cmd)){
			// load if currently showing
			loaded = false;
			//ontology  = (IOntology) evt.getNewValue();
			if(isShowing())
				load();
        }
	}	
	
	
	
	/**
	 * load parameters
	 *
	private static void loadParameters(KnowledgeCloud kc){
		// compute "learned" values by number of authored diagnosis
		long time = System.currentTimeMillis();
		ArrayList diagnosisList = new ArrayList(kc.getDiagnosisMap().values());
		Collections.sort(diagnosisList,new Comparator(){
			public int compare(Object o1, Object o2){
				Concept c1 = (Concept) o1;
				Concept c2 = (Concept) o2;
				return c2.getReferenceCount(Concept.CASE) - c1.getReferenceCount(Concept.CASE);
			}
		});
		Map learned = new HashMap();
		int max = ((Concept)diagnosisList.get(0)).getReferenceCount(Concept.CASE);
		for(int i=0;i<diagnosisList.size();i++){
			Concept c = (Concept) diagnosisList.get(i);
			int count = c.getReferenceCount(Concept.CASE);
			double size = ((double)count)/max;
			learned.put(""+c,""+size);
			if(count == 0)
				break;
		}
		//kc.setLearnedValues(learned);
		//System.out.println("learn map time "+(System.currentTimeMillis()-time)+" ms");
		
		// compute "observed" value by case difficulty
		//time = System.currentTimeMillis();
		ArrayList casesList = new ArrayList(kc.getCasesMap().values());
		Map observed = new HashMap();
		for(Iterator i=casesList.iterator();i.hasNext();){
			Concept c = (Concept) i.next();
			String df = c.getProperties().getProperty("difficulty","normal");
			String b =  c.getProperties().getProperty("ready","false");
			//System.out.println(c+" "+df+" "+b);
			if(df.trim().equalsIgnoreCase("difficult"))
				learned.put(""+c,""+Concept.BOLD_VALUE);
				//observed.put(""+c,"1");
			if(!b.equalsIgnoreCase("true"))
				observed.put(""+c,"1");
				//learned.put(""+c,""+Concept.BOLD_VALUE);
			
		}
		// assign color values based on defenitions
		ArrayList list = new ArrayList();
		list.addAll(kc.getFeatureMap().values());
		list.addAll(kc.getDiagnosisMap().values());
		for(Iterator i=list.iterator();i.hasNext();){
			Concept c = (Concept) i.next();
			String df =  c.getProperties().getProperty("definition");
			String ex = c.getProperties().getProperty("examples");
			
			//System.out.println(c+" "+def+" "+examples);
			// mark concepts that don't have definitions and exampes as red
			if((df == null || df.length() == 0) && (ex == null || ex.length() == 0))
				observed.put(""+c,"1");
			else if(df != null && df.length() > 0 && ex != null && ex.length() > 0)
				observed.put(""+c,"-1");
		}
		
		
		//System.out.println(observed);
		learnedParam = learned;
		observedParam = observed;
		//kc.setLearnedValues(learned);
		//kc.setObservedValues(observed);
		System.out.println("observe and learn masp time "+(System.currentTimeMillis()-time)+" ms");
		
	}
	*/
	
	
	
	/**
	 * Create instance of KnowledgeCloud
	 */
	public KnowledgeCloud(){
		//this.observableType = observable;
		//this.domain = domain;
		
		// init listener
		listener = new ConceptListener();
	
		// create interface  
		cloudPanel = createInterface();
		
		// save instance
		instance = this;
	}
	
	public void dispose(){
		//TODO:
	}
	
	/**
	 * misc action events
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == close){
			System.exit(0);
		}else if(e.getSource() == refresh){
			/*
			try{
				Utils.queryServlet(servlet,"clear");
				featurePanel.removeAll();
				diagnosisPanel.removeAll();
				casesPanel.removeAll();
				cloudPanel.remove(status);
				cloudPanel.add(progress,BorderLayout.SOUTH);
				cloudPanel.revalidate();
				//load(server);
			}catch(IOException ex){
				ex.printStackTrace();
			}*/
		}else if(e.getSource() == showParameters){
			if(showParameters.isSelected()){
				setConceptParameters(parameterMap);
			}else{
				setConceptParameters(null);
			}
			//JOptionPane.showMessageDialog(this,"Not Implemented");
		}else if(e.getSource() == legend){
			JOptionPane.showMessageDialog(cloudPanel,legendText,"Legend",JOptionPane.PLAIN_MESSAGE);
		}else if(e.getSource() == glossary){
			Concept c = (Concept) getPopupMenu().getInvoker();
			displayInfo(c);
		}else if(e.getSource() == bookmark){
			Concept c = (Concept) getPopupMenu().getInvoker();
			if(c != null){
				boolean b = !c.isBookmared();
				c.setBookmark(b);
				c.bookmarkRelatedConcept(b);
			}
		}else if(e.getSource() == clearBookmarks){
			for(Iterator i=getAllConcepts().iterator();i.hasNext();){
				((Concept)i.next()).setBookmark(false);
			}
		}else if(e.getActionCommand().startsWith("Open")){
			gotoConcept((Concept) getPopupMenu().getInvoker());
		}else if("Switch Domains".equals(e.getActionCommand())){
			doSwitchDomain();
		}else if("Summary".equals(e.getActionCommand())){
			doSummary();
		}else if("Validate".equals(e.getActionCommand())){
			doValidate();
		}
		
	}
	/**
	 * create new case
	 */
	private void doSwitchDomain(){
		if(domainSelector == null){
			domainSelector = new DomainSelectorPanel(ontology.getRepository());
			domainSelector.setOwner(DomainBuilder.getInstance().getFrame());
		}
		domainSelector.showChooserDialog();
		if(domainSelector.isSelected()){
			//reset();
			(new Thread(new Runnable(){
				public void run(){
					IOntology ont = (IOntology) domainSelector.getSelectedObject();
					DomainBuilder.getInstance().doLoad(ont.getURI());
				}
			})).start();
		}
	}
	
	/**
	 * run case validation and print results to string buffer
	 * @param output
	 */
	private void doValidateCases(StringBuffer output){
		IInstance [] ilist = ontology.getClass(OntologyHelper.CASES).getDirectInstances();
		getProgressBar().setString("Validating Case Instances ...");
		getProgressBar().setMinimum(0);
		getProgressBar().setMaximum(ilist.length);
		getProgressBar().setValue(0);
		getProgressBar().setIndeterminate(false);
		
		// header
		output.append("<center><h2>Inconsistent Case Diagnoses</h2></center><hr>");
		output.append("<table border=0 bgcolor=\"#FFF8C6\"><tr><td>");
		output.append("<p><font color=red>invalid diagnoses</font> are diagnoses that can NOT be implied by the tutor given the evidence authored. " +
					  "In order to correct this error, please check the authored findings for the case in the CaseTab OR " +
				       "re-evaluate the diagnostic rules using DiagnosisBuilder Tab.</p>");
		output.append("</td></tr><tr><td>");
		output.append("<p><font color=red>missing diagnoses</font> are diagnoses that are implied as correct diagnoses by the tutor given the evidence authored. " +
					  "In order to correct this error, please add the missing diagnosis to the case in CaseTab OR check the authored " +
					  "findings for the case in CaseTab OR re-evaluate the diagnostic rules using DomainBuilder Tab.</p>");
		output.append("</td></tr></table><br>");
		// iterate over all cases
		int i = 0;
		int stComplete = 0, stTested = 0;
		for(IInstance inst: ilist ){
			// skip new case
			if(OntologyHelper.NEW_CASE.equals(inst.getName()))
				continue;
			
			// do this only for complete and tested cases
			StringBuffer buf = new StringBuffer();
			String st = (String) inst.getPropertyValue(ontology.getProperty(HAS_STATUS));
			if(st != null && (st.equals(STATUS_COMPLETE) || st.equals(STATUS_TESTED))){
				// check status
				if(st.equals(STATUS_COMPLETE))
					stComplete++;
				else if(st.equals(STATUS_TESTED))
					stTested ++;
				
				// validate this instance against all diagnosis
				List<IClass> dlist = new ArrayList<IClass>();
				for(IClass d : ontology.getClass(OntologyHelper.DISEASES).getSubClasses()){
					ILogicExpression exp = d.getEquivalentRestrictions();
					if(!exp.isEmpty() && exp.evaluate(inst)){
						dlist.add(d);
					}
				}
				
				// compare lists
				List<IClass> common = new ArrayList<IClass>();
				//find if authored Dx is not inferred
				for(IClass d: inst.getDirectTypes()){
					if(isDisease(d)){
						if(dlist.contains(d)){
							common.add(d);
						}else{
							buf.append("<font color=red>invalid diagnosis</font> "+UIHelper.getPrettyClassName(d.getName())+"<br>");
						}
					}
				}
				// find if infered Dx is not authored ""
				for(IClass d : dlist){
					if(!common.contains(d)){
						boolean missingDx = true;
						
						// check if this DX is perhaps more general then asserted diagnosis
						for(IClass cd: common){
							if(cd.hasSuperClass(d)){
								missingDx = false;
								break;
							}
						}
						
						if(missingDx)
							buf.append("<font color=red>missing diagnosis</font> "+UIHelper.getPrettyClassName(d.getName())+"<br>");
					}
				}
			}else if(st == null){
				buf.append("case status has not been set");
			}
			
			// add to buffer
			if(buf.length() > 0){
				output.append("<b>"+inst.getName()+"</b>");
				output.append("<blockquote>"+buf+"</blockquote>");
			}
			
			// update progressbar
			getProgressBar().setValue(++i);
		}
		
		// addreport on number of tested or complete cases
		if(stComplete == 0)
			output.append("WARNING: There are no <font color=blue>complete</font> cases<br>");
		if(stTested == 0)
			output.append("WARNING: There are no <font color=green>tested</font> cases");
		
		
	}
	
	/**
	 * validate diagnoses
	 * @param output
	 */
	private void doValidateTerms(StringBuffer output){
		IClass [] allconcepts = ontology.getClass(OntologyHelper.CONCEPTS).getSubClasses();
		
		getProgressBar().setString("Validating Term Overlap ...");
		getProgressBar().setMinimum(0);
		getProgressBar().setMaximum(allconcepts.length);
		getProgressBar().setValue(0);
		getProgressBar().setIndeterminate(false);
		
		output.append("<center><h2>Overlapping Terms</h2></center><hr>");
		output.append("<table border=0 bgcolor=\"#FFF8C6\"><tr><td>");
		output.append("<p>The following terms or synonyms are assigned to multiple concepts. " +
				     "This may cause confusion during text parsing and generally should be avoided. " +
				     "You can fix this by removing a problem term from one of the concepts in the knowledge hierarchy tab.</p>");
		output.append("</td></tr></table><br>");
		int count = 0;
		Map<String,IClass> termMap = new HashMap<String,IClass>();
		for(IClass cls: allconcepts){
			for(String lbl: cls.getLabels()){
				if(termMap.containsKey(lbl)){
					output.append("term <i>"+lbl+"</i> is shared by <b>"+cls.getName()+"</b> and <b>"+termMap.get(lbl).getName()+"</b><br>");
				}else{
					termMap.put(lbl,cls);
				}
			}
			getProgressBar().setValue(count++);
		}
		
	}
	
	/**
	 * validate diagnoses
	 * @param output
	 */
	private void doValidateUnusedFindings(StringBuffer output){
		IClass [] allconcepts = ontology.getClass(OntologyHelper.DIAGNOSTIC_FEATURES).getSubClasses();
		
		getProgressBar().setString("Finding Unused Findings ...");
		getProgressBar().setMinimum(0);
		getProgressBar().setMaximum(allconcepts.length);
		getProgressBar().setValue(0);
		getProgressBar().setIndeterminate(false);
		
		output.append("<center><h2>Unused Findings</h2></center><hr>");
		output.append("<table border=0 bgcolor=\"#FFF8C6\"><tr><td>");
		output.append("<p>The following findings are not used in any diagnostic rules. This is not necessary a problem.</p>");
		output.append("</td></tr></table><br>");
		int count = 0;
		for(IClass cls: allconcepts){
			if(!isSystemClass(cls)){
				boolean used = false;
				for(IClass dx: ontology.getClass(DISEASES).getSubClasses()){
					if(isFindingInDiagnosticRule(cls,dx.getEquivalentRestrictions())){
						used = true;
						break;
					}
				}
				
				if(!used)
					output.append("unused finding: <i>"+cls.getName()+"</i> <br>");
			}
			getProgressBar().setValue(count++);
		}
		
	}
	
	/**
	 * validate similar findings
	 * @param output
	 */
	private void doValidateSimilarFindings(StringBuffer output){
		IClass [] allconcepts = ontology.getClass(OntologyHelper.CONCEPTS).getSubClasses();
		
		getProgressBar().setString("Finding Similar Sounding Findings ...");
		getProgressBar().setMinimum(0);
		getProgressBar().setMaximum(allconcepts.length);
		getProgressBar().setValue(0);
		getProgressBar().setIndeterminate(false);
		
		output.append("<center><h2>Similar Concepts</h2></center><hr>");
		output.append("<table border=0 bgcolor=\"#FFF8C6\"><tr><td>");
		output.append("<p>The following concepts have similar sounding names and may cause confusion during authoring. Some differences may be intentional," +
						  " while others can be typos in concept names.</p>");
		output.append("</td></tr></table><br>");
		int count = 0;
		List<IClass> used = new ArrayList<IClass>();
		for(IClass c1: allconcepts){
			if(!isSystemClass(c1)){
				for(IClass c2: allconcepts){
					if(!isSystemClass(c2)){
						if(!c1.equals(c2) && TextTools.similar(c1.getName(),c2.getName()) && !used.contains(c2)){
							output.append("concept <i>"+c1.getName()+"</i> is similar to <i>"+c2+"</i><br>");
							used.add(c1);
							break;
						}
					}
				}
				getProgressBar().setValue(count++);
			}
		}
	}
	
	/**
	 * validate diagnoses
	 * @param output
	 */
	private void doValidateDiagnoses(StringBuffer output){
		IClass [] dlist = ontology.getClass(OntologyHelper.DISEASES).getSubClasses();
		getProgressBar().setString("Validating Diagnoses Similarity ...");
		getProgressBar().setMinimum(0);
		getProgressBar().setMaximum(dlist.length);
		getProgressBar().setValue(0);
		getProgressBar().setIndeterminate(false);
		
		output.append("<center><h2>Questionable Diagnostic Rules</h2></center><hr>");
		output.append("<table border=0 bgcolor=\"#FFF8C6\"><tr><td>");
		output.append("<p>The following is a list of diagnoses that may have potential problems with their associated" +
				     " diagnostic rules. Please note that two or more diseases are allowed to have similar diagnostic rules. However, the tutor" +
				     " will allow both diseases to be valid diagnoses for a case, even though only one of those diagnoses is authored for that case.</p>");
		output.append("</td></tr></table><br>");
		
		// sort
		int p = 0;
		Arrays.sort(dlist,new Comparator<IClass>() {
			public int compare(IClass a, IClass b) {
				return a.getName().compareTo(b.getName());
			}
		
		});
		// go over each diagnoses
		for(IClass dx: dlist){
			String dname = "<b>"+UIHelper.getPrettyClassName(dx.getName())+"</b>";
			ILogicExpression exp = dx.getEquivalentRestrictions();
			
			if(exp.isEmpty()){
				output.append(dname+" does not have any diagnostic rules.<br>");
			}else if(exp.getExpressionType() == ILogicExpression.OR){
				for(int i=0;i<exp.size();i++){
					if(exp.get(i) instanceof ILogicExpression){
						List<IClass> conflict = new ArrayList<IClass>();
						IInstance inst = createTempInstance((ILogicExpression) exp.get(i));
						for(IClass d: dlist){
							if(!d.equals(dx) && d.evaluate(inst)){
								conflict.add(d);
							}
						}
						inst.delete();
						if(!conflict.isEmpty()){
							output.append(dname+" ["+(i+1)+"] has similar diagnostic rules as <blockquote>");
							for(int j=0;i<conflict.size();j++){
								output.append(UIHelper.getPrettyClassName(conflict.get(j).getName()));
								if(j < conflict.size()-1)
									output.append(", ");
							}
							output.append("</blockquote>");
									
						}
					}
				}
			}else{
				IInstance inst = createTempInstance(exp);
				List<IClass> conflict = new ArrayList<IClass>();
				for(IClass d: dlist){
					ILogicExpression de = d.getEquivalentRestrictions();
					if(!d.equals(dx) && !de.isEmpty() && de.evaluate(inst)){
						conflict.add(d);
					}
				}
				inst.delete();
				
				if(!conflict.isEmpty()){
					output.append(dname+" has similar diagnostic rules as <blockquote>");
					for(int i=0;i<conflict.size();i++){
						output.append(UIHelper.getPrettyClassName(conflict.get(i).getName()));
						if(i < conflict.size()-1)
							output.append(", ");
					}
					output.append("</blockquote>");
							
				}
				
			}
			
			getProgressBar().setValue(++p);
		}
	}
	
	/**
	 * create temporary instance from expression
	 * @param exp
	 * @return
	 */
	private IInstance createTempInstance(ILogicExpression exp){
		// if temp instance exists, remove it
		IInstance inst = ontology.getInstance("temporaryInstance");
		if(inst != null)
			inst.delete();
		
		// now create a new one
		inst = ontology.getClass(CASES).createInstance("temporaryInstance");
		for(String p: new String [] {HAS_FINDING,HAS_NO_FINDING,HAS_CLINICAL}){
			IProperty prop = ontology.getProperty(p);
			inst.setPropertyValues(prop,getInstanceValues(exp,prop).toArray());
		}
		
		return inst;
	}
	
	/**
	 * get a list of instance values from expression, given a property
	 * @param exp
	 * @param prop
	 * @return
	 */
	private Set<IInstance> getInstanceValues(ILogicExpression exp, IProperty prop){
		Set<IInstance> values = new LinkedHashSet<IInstance>();
		for(Object o: exp){
			if(o instanceof IRestriction){
				IRestriction r = (IRestriction) o;
				if(r.getProperty().equals(prop)){
					values.addAll(getInstanceValues(r.getParameter(),prop));
				}
			}else if(o instanceof ILogicExpression){
				values.addAll(getInstanceValues((ILogicExpression)o,prop));
			}else if(o instanceof IClass){
				IClass c = (IClass) o;
				IInstance in = null;
				IInstance [] din = c.getInstances();
				if(din.length > 0){
					in = din[0];
				}
				if(in == null)
					in = c.createInstance(c.getName().toLowerCase());
				
				// we have the instance now
				values.add(in);
			}
		}
		
		return values;
	}
	
	
	/**
	 * create userfull summary
	 */
	private void doValidate(){
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				StringBuffer output = new StringBuffer();
				
				// do validation
				doValidateCases(output);
				doValidateDiagnoses(output);
				doValidateTerms(output);
				doValidateUnusedFindings(output);
				doValidateSimilarFindings(output);
				
				// add to summary window
				UIHelper.HTMLPanel infoPanel =  new UIHelper.HTMLPanel();
				infoPanel.setPreferredSize(new Dimension(600, 500));
				infoPanel.setEditable(false);
				infoPanel.setText(""+output);
				infoPanel.setCaretPosition(0);

				setBusy(false);
				// display info in modal dialog
				JOptionPane op = new JOptionPane(new JScrollPane(infoPanel),JOptionPane.PLAIN_MESSAGE);
				JDialog d = op.createDialog(KnowledgeCloud.this,"Validation Summary");
				d.setResizable(true);
				d.setModal(false);
				d.setVisible(true);	
			}
		})).start();
		
	}
	
	
	/**
	 * create userfull summary
	 */
	private void doSummary(){
		// go over cases and create a case based table
		Map<String,Set<String>> dxToCase = new HashMap<String, Set<String>>();
		Map<String,Set<String>> tagToCase = new HashMap<String, Set<String>>();
		StringBuffer caseSummary = new StringBuffer();
		List<String> cases = new ArrayList<String>(casesMap.keySet());
		Collections.sort(cases);
		
		// iterate over cases, to get case based stats
		int statusIncomplete = 0, statusComplete = 0,statusTested = 0,total = 0;
		caseSummary.append("<table>");
		caseSummary.append("<tr><th>CASE</th><th>STATUS</th><th>DIAGNOSES</th></tr>");
		for(String name: cases){
			IInstance inst =  ontology.getInstance(name);
			
			// get status
			String st = (String) inst.getPropertyValue(ontology.getProperty(OntologyHelper.HAS_STATUS));
			if(st != null){
				if(STATUS_INCOMPLETE.equals(st))
					statusIncomplete ++;
				else if(STATUS_COMPLETE.equals(st))
					statusComplete ++;
				else if(STATUS_TESTED.equals(st))
					statusTested ++;
			}
			total ++;
			
			// get tags
			for(Object obj: inst.getPropertyValues(ontology.getProperty(OntologyHelper.HAS_TAG))){
				String [] s = (""+obj).split("=");
				String tag = s[0].trim();
				// get value of key = value pair
				if(s.length > 1)
					tag = s[1].trim();
				
				Set<String> tags = tagToCase.get(tag);
				if(tags == null){
					tags = new TreeSet<String>();
					tagToCase.put(tag,tags);
				}
				tags.add(name);
			}
			
			
			// count diseases
			List<String> dx = new ArrayList<String>();
			for(IClass cls : inst.getDirectTypes()){
				if(OntologyHelper.isDisease(cls)){
					String dxName = cls.getName();
					
					// check for multi-pattern
					ILogicExpression exp = cls.getEquivalentRestrictions();
					if(exp.getExpressionType() == ILogicExpression.OR){
						for(int i=0;i<exp.size();i++){
							if(exp.get(i) instanceof ILogicExpression){
								ILogicExpression e = (ILogicExpression) exp.get(i);
								// if this pattern matches
								if(e.evaluate(inst)){
									dxName = cls.getName()+" ("+(i+1)+")";
									if(!dx.contains(dxName))
										dx.add(dxName);

									// set-up dx to case map
									Set<String> list = dxToCase.get(dxName);
									if(list == null){
										list = new LinkedHashSet<String>();
										dxToCase.put(dxName,list);
									}
									list.add(name);
								}
							}
						}
						
						// if dxName == cls.getName(), we could not find pattern
						// but it is a multipattern dx, so add general dx
						if(dxName.equals(cls.getName())){
							if(!dx.contains(dxName))
								dx.add(dxName);
						}	
					}else{
						// now create a list of diagnosis
						if(!dx.contains(dxName))
							dx.add(dxName);
					}
					
				
					
					
					// set-up dx to case map
					Set<String> list = dxToCase.get(dxName);
					if(list == null){
						list = new LinkedHashSet<String>();
						dxToCase.put(dxName,list);
					}
					list.add(name);
				}
			}
			Collections.sort(dx);
			String d = ""+dx;
			d = d.substring(1,d.length()-1).replaceAll("_"," ");
			// add to output
			caseSummary.append("<tr><td>"+name+"</td><td>"+TextHelper.toString(st)+"</td><td>"+d+"</td></tr>");
		}
		caseSummary.append("</table>");
		
		StringBuffer out = new StringBuffer("<html>");
		out.append("<center><h2>Summary</h2></center><hr>");
		out.append("<b>"+total+"</b> total cases<br>");
		out.append("<font color=blue><b>"+statusComplete+"</b></font> cases authored<br>");
		out.append("<font color=green><b>"+statusTested+"</b></font> cases tested<br>");
		out.append("<font color=red><b>"+statusIncomplete+"</b></font> cases pending completion<br><br>");
		
		
		// create tag summary
		StringBuffer tagSummary = new StringBuffer();
		tagSummary.append("<table><tr>");
		int rowCount = 0;
		List<List<String>> tagList = new ArrayList<List<String>>();
		// report on tags
		for(String tag: new TreeSet<String>(tagToCase.keySet())){
			int count = tagToCase.get(tag).size();
			String adj = (count > 1)?"are":"is";
			out.append("<b>"+count+"</b> cases "+adj+" tagged as <b>"+tag+"</b><br>");
			tagSummary.append("<th><b>"+tag+"</b></th>");
			if(count > rowCount)
				rowCount = count;
			// fill out tag list
			tagList.add(new ArrayList<String>(tagToCase.get(tag)));
		}
		tagSummary.append("</tr>");
		
		out.append("<br><center><h2>Diagnosis Summary</h2></center><hr>");
		
		List<String> dlist = new ArrayList<String>(dxToCase.keySet());
		Collections.sort(dlist);
		for(String dx: dlist){
			out.append("<b>"+dxToCase.get(dx).size()+"</b> cases of "+dx.replaceAll("_"," ")+"<br>");
		}
		out.append("<br><center><h2>Case Summary</h2></center><hr>");
		out.append(caseSummary);
		
		
		// do vertical laout for tag summary
		if(!tagToCase.isEmpty()){
			for(int i=0;i<rowCount;i++){
				tagSummary.append("<tr>");
				for(int j=0;j<tagList.size();j++){
					String name = (tagList.get(j).size() > i)?tagList.get(j).get(i):"";
					tagSummary.append("<td> "+name+" </td>");
				}
				tagSummary.append("</tr>");
			}
			tagSummary.append("</table>");
			
			out.append("<br><center><h2>Tag Summary</h2></center><hr>");
			out.append(tagSummary);
		}
		
		// add to summary window
		UIHelper.HTMLPanel infoPanel =  new UIHelper.HTMLPanel();
		infoPanel.setPreferredSize(new Dimension(600, 500));
		infoPanel.setEditable(false);
		infoPanel.setText(out.toString());
		infoPanel.setCaretPosition(0);

		// display info in modal dialog
		JOptionPane op = new JOptionPane(new JScrollPane(infoPanel),JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(this,"Domain Summary");
		d.setResizable(true);
		d.setModal(false);
		d.setVisible(true);	
	}
	
	
	/**
	 * goto specific concept in the tool
	 * @param c
	 */
	private void gotoConcept(Concept c){
		if(c.getType() == Concept.CASE){
			DomainBuilder.getInstance().firePropertyChange(OntologyHelper.CASE_OPEN_EVENT,null,c.toString());
		}else if(c.getType() == Concept.DIAGNOSIS){
			List<String> list = new ArrayList<String>();
			if(c.isBookmared()){
				for(Concept a: diagnosisMap.values()){
					if(a.isBookmared()){
						list.add(a.toString());
					}
				}
			}else{
				list.add(c.toString());
			}
			DomainBuilder.getInstance().firePropertyChange(OntologyHelper.OPEN_DIAGNOSIS_EVENT,null,list);
		}else{
			DomainBuilder.getInstance().firePropertyChange(OntologyHelper.SHOW_CONCEPT_EVENT,null,c.toString());
		}
	}
	
	
	/**
	 * get menu bar
	 * @return
	 */
	public JMenuBar getMenuBar(){
		if(menu == null)
			menu = createMenuBar();
		return menu;
	}
	
	
	/**
	 * create menubar
	 * @return
	 */
	public JMenuBar createMenuBar(){
		JMenuBar menubar = new JMenuBar();
	
		// file
		JMenu file = new JMenu("File");
		
		file.add(UIHelper.createMenuItem("Switch Domains","Open a new domain",Icons.BROWSE,this));
		file.addSeparator();
		file.add(UIHelper.createMenuItem("Exit","Exit Domain Builder",null,DomainBuilder.getInstance()));
		
		JMenu options = new JMenu("Options");
		showParameters = new JCheckBoxMenuItem("Show Parameters");
		showParameters.addActionListener(this);
		options.add(showParameters);
		
		clearBookmarks = new JMenuItem("Clear Bookmarks");
		clearBookmarks.addActionListener(this);
		options.add(clearBookmarks);
		
		// help
		JMenu help = new JMenu("Help");
		legend = new JMenuItem("Legend");
		legend.addActionListener(this);
		//help.add(legend);	
		help.add(UIHelper.createMenuItem("Help","DomainBuilder Manual",Icons.HELP,DomainBuilder.getInstance()));
		help.add(UIHelper.createMenuItem("About","About DomainBuilder",Icons.ABOUT,DomainBuilder.getInstance()));
		
		
		menubar.add(file);
		menubar.add(options);
		menubar.add(help);
		
		return menubar;
	}

	
	/**
	 * create user interface
	 * @return
	 */
	private JPanel createInterface(){
		// init components
		featurePanel   = createPanel();
		diagnosisPanel = createPanel();
		
		search = new JTextField();
		search.setDocument(new SearchDocument(this));
		search.setBorder(new TitledBorder("Search"));
		// create cloud panel
		JSplitPane fd = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		fd.setTopComponent(createScrollPanel("Findings",featurePanel));
		fd.setBottomComponent(createScrollPanel("Diagnosis",diagnosisPanel));
		fd.setResizeWeight(.5);
		fd.setDividerLocation(500);
		casesPanel = createPanel();
		JScrollPane caseScroll = createScrollPanel("Cases",casesPanel);
		caseScroll.setPreferredSize(new Dimension(255,300));
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setTopComponent(fd);
		split.setRightComponent(caseScroll);
		split.setResizeWeight(1);
		
		
		// status label
		status = new JLabel(" ");
		status.setFont(status.getFont().deriveFont(Font.PLAIN));
		
		// create progress bar
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setString("Loading Knowledge Base, Please wait ...");
		progress.setStringPainted(true);
		
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		top.add(getToolBar(),BorderLayout.WEST);
		top.add(search,BorderLayout.CENTER);
		
		// create search pane
		JPanel cloudPanel = this;
		cloudPanel.setLayout(new BorderLayout());
		cloudPanel.add(top,BorderLayout.NORTH);
		cloudPanel.add(split,BorderLayout.CENTER);
		//cloudPanel.add(status,BorderLayout.SOUTH);
		return cloudPanel;
	}
	
	/** 
	 * reset interface for new kb
	 */
	private void reset(){
		search.setText("");
		featurePanel.removeAll();
		diagnosisPanel.removeAll();
		casesPanel.removeAll();
	}
	
	
	/**
	 * set busy
	 * @param b
	 */
	private void setBusy(boolean busy){
		if(isStandAlone){
			if(busy){
				cloudPanel.remove(status);
				cloudPanel.add(progress,BorderLayout.SOUTH);
				cloudPanel.revalidate();
			}else{
				cloudPanel.remove(progress);
				cloudPanel.add(status,BorderLayout.SOUTH);
				cloudPanel.revalidate();
			}
		}else{
			JProgressBar progress = DomainBuilder.getInstance().getProgressBar();
			progress.setIndeterminate(true);
			progress.setString("Loading Knowledge Base, Please wait ...");
			DomainBuilder.getInstance().setBusy(busy);
		}
	}
	private JProgressBar getProgressBar(){
		return isStandAlone?progress:DomainBuilder.getInstance().getProgressBar();
	}
	
	
	
	public JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new JToolBar();
			toolbar.add(UIHelper.createButton("Summary","Display Summary",Icons.PROPERTIES, this));
			toolbar.add(UIHelper.createButton("Validate","Run Validation Suite",Icons.VALIDATE, this));
			
			
		}
		return toolbar;
	}
	
	
	/**
	 * set status line
	 * @param s
	 */
	public void setStatus(String s){
		status.setText(s);
	}
	
	
	/**
	 * Get panel that contains this cloud
	 * @return
	 */
	public JComponent getComponent(){
		return cloudPanel;
	}
	
	/**
	 * Get recent instance of KnowledgeCloud
	 * @return
	 */
	public static KnowledgeCloud getInstance(){
		return instance;
	}
	
	/**
	 * Create panel for individual component
	 * @param title
	 * @return
	 */
	private JPanel createPanel(){
		JPanel panel = new JPanel();
		//if(title != null)
		//	panel.setBorder(new TitledBorder(title));
		panel.setLayout(new ScrollableFlowLayout(FlowLayout.LEFT));
		panel.setBackground(Color.white);
		//panel.setPreferredSize(new Dimension(750,350));
		panel.setMinimumSize(new Dimension(0,0));
		return panel;
	}
	
	
	/**
	 * create scroll panel
	 * @param title
	 * @param panel
	 * @return
	 */
	private JScrollPane createScrollPanel(String title,JPanel panel){
		JScrollPane scroll = new JScrollPane(panel);
		scroll.setBackground(Color.white);
		scroll.setPreferredSize(new Dimension(600,300));
		
		if(title != null)
			scroll.setBorder(new TitledBorder(title));
		
		return scroll;
	}
	
	
	//	 wrapper to load projects from given URI
	/*
	private Project loadProjectFromURI(URI uri){
		Collection errors = new ArrayList();
		Project project = Project.loadProjectFromURI(uri,errors);
		if (!errors.isEmpty()) {
			//System.out.print("Error: ");
			for (Iterator i = errors.iterator(); i.hasNext();)
				System.out.println(i.next().toString());
			project = null;
		}
		return project;
	}
	
	//	 wrapper to load projects from given URI
	private Project loadProjectFromFile(File file){
		Collection errors = new ArrayList();
		Project project = Project.loadProjectFromFile(file.getAbsolutePath(),errors);
		if (!errors.isEmpty()) {
			//System.out.print("Error: ");
			for (Iterator i = errors.iterator(); i.hasNext();)
				System.out.println(i.next().toString());
			project = null;
		}
		return project;
	}
	
	*/
	/**
	 * Get concept (either feature or diagnosis)
	 * @param name
	 * @return
	 */
	private Concept getConcept(String name){
		Concept c = (Concept) featureMap.get(name);
		if(c == null)
			c = (Concept) diagnosisMap.get(name);
		if(c == null)
			c = (Concept) casesMap.get(name);
		return c;
	}
	
	/**
	 * get list of all concepts
	 * @return
	 */
	private java.util.List<Concept> getAllConcepts(){
		ArrayList<Concept> list = new ArrayList<Concept>();
		list.addAll(featureMap.values());
		list.addAll(diagnosisMap.values());
		list.addAll(casesMap.values());
		return list;
	}
	
    /**
     * set concept parameters to a content of a map
     * Map is a key=value pair where value can be a number or a color
     * @param map
     */
    public void setConceptParameters(Map<String,String> map){
    	setConceptParameters(map,loaded);
    }
    
    
    /**
     * Set learned values for given objects
     * @param map list of key=value pairs
     */
    public void setConceptParameters(Map<String,String> map, boolean now){
    	if(map != null)
    		parameterMap = map;
    	if(now){
    		if(map == null){
    			boolean stripe = true;
    			for(Concept c : getAllConcepts()){
    				c.setSizeValue(0);
    				c.setColorValue(0);
    				//c.setConceptColor(c.getConceptColor());boolean stripe = true;
    				c.setConceptColor((stripe)?Color.black:Color.gray);
    				stripe = stripe ^ true;
    			}
    		}else{
    			// iterate over keys
	            for(String key : map.keySet()){
	                String val  = map.get(key);
	                // find corresponding concept
	                Concept concept = getConcept(key);
	                if(concept != null){
		                // check if input is number
	                	if(TextHelper.isNumber(val)){
	                		 concept.setSizeValue(TextHelper.parseDecimalValue(val));
	                	}else{
	                		Color color = UIHelper.getColor(val);
	                		if(color != null){
	                			concept.setColorValue(color);
	                		}
	                	}
	                }
	            }
    		}
    	}
    }
    
    
    /**
     * perform search
     */
    public void doSearch(){
    	if(!loaded)
    		return;
    	
    	// get search term
    	String txt = search.getText().toLowerCase();
    	
    	Map [] map = new Map [] {featureMap,diagnosisMap,casesMap};
    	// iterate over maps
    	for(int i=0;i<map.length;i++){
    		// iterate over keys in map
    		for(Iterator j=map[i].values().iterator();j.hasNext();){
    			Concept c = (Concept) j.next();
    			c.setHighlight(c.getText().toLowerCase().contains(txt) && txt.length() > 1);
    		}
    	}
    }
    
    /**
     * Stip prefix
     * @param name
     * @return
     
    private String stripPrefix(String name){
    	return (name.startsWith(prf))?name.substring(prf.length()):name;
    }
    */
    
    
    /**
     * parse list presented as a string
     * @param list
     * @return
     */
    private static String [] parseList(String exmpl){
    	if(exmpl != null){
    		if(exmpl.startsWith("[") && exmpl.endsWith("]")){
				exmpl = exmpl.substring(1,exmpl.length()-1);
				return exmpl.split(",");
    		}
    		return exmpl.split(";");
		}
    	return null;
    }
    
    
    /**
     * Extract values from concept Cls and assign them to a concept
     * @param c
     * @param cls
     *
    private void populateConcept(Concept c, Cls cls){
    	KnowledgeBase kb = cls.getKnowledgeBase();
    	URI uri = kb.getProject().getProjectURI();
    	String name = stripPrefix(cls.getName());
    	// add cuid and other usefull fields
		String cui = (String) cls.getOwnSlotValue(kb.getSlot(prf+"hasCUI"));
		if(cui != null)
			c.getProperties().setProperty("code",cui);
		
		
		// add definition and other usefull fields
		String def = (String) cls.getOwnSlotValue(kb.getSlot(prf+"hasDefinition"));
		if(def != null)
			c.getProperties().setProperty("definition",def);
    	
		
		String [] examples = null;
		if(kb.getSlot(prf+"hasExamples") != null){
			String exmpl = (String )cls.getOwnSlotValue(kb.getSlot(prf+"hasExamples"));
			examples = parseList(exmpl);
		}
		
		
		// see if there are any example pictures
		int indx = uri.toString().lastIndexOf("/");
		String exampleURI = uri.toString().substring(0,indx)+"/examples/"+name;
		ArrayList list = new ArrayList();
		
		// if examples are not defined (scan)
		if(examples != null){
			for(int i=0;i<examples.length;i++){
				String s = examples[i].trim();
				if(s.length() > 0)
					list.add(exampleURI+"/"+s);
			}
		}
		if(!list.isEmpty())
			c.getProperties().setProperty("examples",""+list);
    }
    */
    
    /**
     * create concept from map
     * @param map
     * @return
     */
    private Concept createConcept(IResource r, Collection<IResource> related){
    	return createConcept(null,r,related);
    }
    
    /**
     * create concept from map
     * @param map
     * @return
     */
    private Concept createConcept(IResource r, Collection<IResource> related, Collection<IResource> arelated){
    	return createConcept(null,r,related,arelated);
    }
    
    /**
     * create concept from map
     * @param map
     * @return
     */
    private Concept createConcept(String name, IResource r, Collection<IResource> related){
    	return createConcept(name, r,related,Collections.EMPTY_LIST);
    }
    /**
     * create concept from map
     * @param map
     * @return
     */
    private Concept createConcept(String name, IResource r, Collection<IResource> related,Collection<IResource> arelated){
		IOntology ont = r.getOntology();
    	Concept concept = null;
    	int cType = Concept.FEATURE;
		Map cMap = null;
		IResource res = null;
		
		if(r instanceof IInstance){
			IInstance inst = (IInstance) r;
			if(inst.hasType(ont.getClass(OntologyHelper.CASES))){
				cType = Concept.CASE;
				cMap = casesMap;
				if(name == null)
					name = inst.getName();
			}else if(inst.hasType(ont.getClass(OntologyHelper.DISEASES))){
				cType = Concept.DIAGNOSIS;
				cMap = diagnosisMap;
				if(name == null)
					name = inst.getDirectTypes()[0].getName();
			}else if(inst.hasType(ont.getClass(OntologyHelper.FEATURES))){
				cType = Concept.FEATURE;
				cMap = featureMap;
				if(name == null)
					name = inst.getDirectTypes()[0].getName();
			}
			res = inst;
		}else if(r instanceof IClass){
			IClass cls = (IClass) r;
			if(name == null)
				name = cls.getName();
			if(cls.hasSuperClass(ont.getClass(OntologyHelper.DISEASES))){
				cType = Concept.DIAGNOSIS;
				cMap = diagnosisMap;
			}else if(cls.hasSuperClass(ont.getClass(OntologyHelper.FEATURES))){
				cType = Concept.FEATURE;
				cMap = featureMap;
			}
			res = cls;
		}
		
		// should never happen
		if(cMap == null || name == null)
			return null;
		
		// init or fetch new concept
		if(cMap.containsKey(name)){
			concept = (Concept) cMap.get(name);
		}else{
			// init concepts
			concept = new Concept(name,cType);
			concept.getComponent().addMouseListener(listener);
			populateConcept(concept,res);
			cMap.put(name,concept);
		}
		
		// attach all related concepts
		for(IResource rcls:  related){
			//ArrayList<IResource> list = new ArrayList<IResource>();
			//if(cType==Concept.DIAGNOSIS || cType == Concept.CASE)
			//	list.add(r);
			Concept c = createConcept(rcls,Collections.EMPTY_LIST);
			if(c != null){
				concept.addRelatedConcept(c);
				c.addRelatedConcept(concept);
			}
		}
		
		// attach all related absent concepts
		for(IResource rcls:  arelated){
			//ArrayList<IResource> list = new ArrayList<IResource>();
			//if(cType==Concept.DIAGNOSIS || cType == Concept.CASE)
			//	list.add(r);
			Concept c = createConcept(rcls,Collections.EMPTY_LIST);
			if(c != null){
				concept.addRelatedAbsentConcept(c);
				c.addRelatedAbsentConcept(concept);
			}
		}
		
		return concept;
    }
    
    
    
    
    /**
     * Extract values from concept Cls and assign them to a concept
     * @param c
     * @param cls
     */
    private void populateConcept(Concept c, IResource res){
    	if(res == null)
    		return;
    	
    	//String name = res.getName();
    	// add cuid and other usefull fields
    	IProperty prop = ontology.getProperty(OntologyHelper.HAS_CONCEPT_CODE);
		if(prop != null){
			String cui = (String) res.getPropertyValue(prop);
			if(cui != null)
				c.getProperties().setProperty("code",cui);
		}
		
		// add definition and other usefull fields
		String def = (String) res.getDescription();
		if(def != null)
			c.getProperties().setProperty("definition",def);
    	
		
		// pull all properties
		for(IProperty p : res.getProperties()){
			if(p.isAnnotationProperty())
				c.getProperties().setProperty(p.getName(),Arrays.toString(res.getPropertyValues(p)));
		}
		
		
		Object [] ex = res.getPropertyValues(ontology.getProperty(HAS_EXAMPLE));
		if(ex != null && ex.length > 0){
			List<String> examples = new ArrayList<String>();
			URL url = getExampleURL(getKnowledgeBase(ontology));
			for(Object e: ex){
				try{
					examples.add(""+new URL(url+"/"+e));
				}catch(Exception er){}
			}
			c.getProperties().setProperty("examples",""+examples);
		}
	
		// check if we have difficulty and ready
		/*
		if(map.containsKey("difficulty"))
			c.getProperties().setProperty("difficulty",""+map.get("difficulty"));
		if(map.containsKey("ready"))
			c.getProperties().setProperty("ready",""+map.get("ready"));	
		*/
    }
    
	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	private Set<IResource> getFindings(ILogicExpression exp, IProperty prop, Set<IResource> list,Set<IResource> alist){
		for(Object obj : exp){
			if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				getFindings(r.getParameter(),p,list,alist);
			}else if(obj instanceof IClass && prop != null){
				// convert class to a concept entry
				IClass c = (IClass) obj;
				if( prop.getName().contains(OntologyHelper.HAS_FINDING) || 
					prop.getName().contains(OntologyHelper.HAS_CLINICAL)){
					list.add(c);
				}else if(prop.getName().contains(OntologyHelper.HAS_NO_FINDING)){
					alist.add(c);
				}
				/*
				else if(prop.getName().contains(OntologyHelper.HAS_NO_FINDING)){
					entry.setAbsent(true);
					list.add(entry);
				}*/
			}else if(obj instanceof ILogicExpression){
				// recurse into expression
				getFindings((ILogicExpression) obj,prop,list,alist);
			}
		}
		return list;
	}
	
	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	private Set<IResource> getFindings(IRestriction [] restrictions, Set<IResource> list,Set<IResource> alist){
		for(IRestriction r : restrictions){
			getFindings(r.getParameter(),r.getProperty(),list,alist);
		}
		return list;
	}
	
	
	
	public void load(){
		setOntologyInfo();
		
		//We want to reload all of the time, since each time something new can be added in the tabs
		//if(loaded)
		//	return;
		
		// wait for ontology to load
		while(ontology == null){
			try{
				Thread.sleep(500);
			}catch(InterruptedException ex){
				ex.printStackTrace();
			}
		}
		
		// load ontology
		try{
			load(ontology);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private void setOntologyInfo(){
		if(ontology != null){
			StringBuffer text= new StringBuffer("<html>");
			UIHelper.printKnowledgeStatus(text,""+OntologyHelper.getKnowledgeBase(ontology).getURI());
			UIHelper.printUserStatus(text);
			
			JLabel st = DomainBuilder.getInstance().getInfoLabel();
			st.setText(""+text);
		}
	}
	/**
	 * load concepts and their definitions
	 */
	public void load(IOntology ont) throws IOException{
		//	init viewer params
		reset();
		final IOntology ontology = ont;
		setBusy(true);
		// now, load from server
		(new Thread(new Runnable(){
			public void run(){
				//component maps
				featureMap = new HashMap<String,Concept>();
				diagnosisMap = new HashMap<String,Concept>();
				casesMap = new HashMap<String,Concept>();
				Map<IClass,List<Concept>> dxPatterns = new HashMap<IClass, List<Concept>>();
				
				// iterate over all diagnosis
				for(IClass diagnosis : ontology.getClass(OntologyHelper.DISEASES).getSubClasses()){
					if(diagnosis.getName().equals(OntologyHelper.DISEASES))
						continue;
					
					Set<IResource> findings = new HashSet<IResource>();
					Set<IResource> afindings = new HashSet<IResource>();
					
					// get necessary and sufficient restrictions to get diagnostic features
					ILogicExpression exp = diagnosis.getEquivalentRestrictions();
					getFindings(exp,null,findings,afindings);
					
					// add stuff that might be inherited
					getFindings(diagnosis.getRestrictions(ontology.getProperty(OntologyHelper.HAS_FINDING)),findings,afindings);
					getFindings(diagnosis.getRestrictions(ontology.getProperty(OntologyHelper.HAS_NO_FINDING)),findings,afindings);
					getFindings(diagnosis.getRestrictions(ontology.getProperty(OntologyHelper.HAS_CLINICAL)),findings,afindings);
					
					createConcept(diagnosis,findings,afindings);
					
					// check for multi-pattern
					if(exp.getExpressionType() == ILogicExpression.OR){
						// save multi-pattern dx in a list
						List<Concept> list = dxPatterns.get(diagnosis);
						if(list == null){
							list = new ArrayList<Concept>();
							dxPatterns.put(diagnosis,list);
						}
						
						for(int i=0;i<exp.size();i++){
							if(exp.get(i) instanceof ILogicExpression){
								Set<IResource> ab = new HashSet<IResource>();
								Set<IResource> pt = getFindings((ILogicExpression)exp.get(i),null,new HashSet<IResource>(),ab);
								list.add(createConcept(diagnosis+" "+(i+1),diagnosis,pt,ab));
							}
						}
					}
				}
				
				// iterate over all cases
				for(IInstance inst: ontology.getClass(OntologyHelper.CASES).getDirectInstances()){
					// skip new case
					if(OntologyHelper.NEW_CASE.equals(inst.getName()))
						continue;
					
					ArrayList<IResource> list = new ArrayList<IResource>();
					ArrayList<IResource> alist = new ArrayList<IResource>();
					// add findings
					for(Object f: inst.getPropertyValues(ontology.getProperty(OntologyHelper.HAS_FINDING))){
						if(f instanceof IResource)
							list.add((IResource)f);
					}
					
					// add findings
					for(Object f: inst.getPropertyValues(ontology.getProperty(OntologyHelper.HAS_NO_FINDING))){
						if(f instanceof IResource)
							alist.add((IResource)f);
					}
					
					// add diseases
					//inst.getDirectTypes()
					// extra concepts to add
					List<Concept> extra = new ArrayList<Concept>();
					for(IClass type:  inst.getTypes()){ 
						if(type.hasSuperClass(ontology.getClass(OntologyHelper.DISEASES))){
							list.add(type);
						
							// check for multi-pattern dx
							List<Concept> l = dxPatterns.get(type);
							if(l != null){
								extra.addAll(getMatchingPatterns(type,inst,l));	
							}
						}
					}
					
					// create case instance
					Concept c = createConcept(inst,list,alist);
					
					// add other related concepts
					for(Concept r : extra){
						c.addRelatedConcept(r);
						r.addRelatedConcept(c);
					}
				}
				
				// TODO: add findings that are not in diseases or cases
				
				// now sort Concepts
				ArrayList<Concept> featureList = new ArrayList<Concept>(featureMap.values());
				ArrayList<Concept> diagnosisList = new ArrayList<Concept>(diagnosisMap.values());
				ArrayList<Concept> casesList = new ArrayList<Concept>(casesMap.values());
				
				// sort values
				Collections.sort(featureList);
				Collections.sort(diagnosisList);
				Collections.sort(casesList);
				
				// add concepts to panels
				boolean stripe = true;
				for(Concept c: featureList){
					c.setConceptColor((stripe)?Color.black:Color.gray);
					featurePanel.add(c.getComponent());
					stripe = stripe ^ true;
				}
				featurePanel.validate();
				//featurePanel.getParent().validate();
				// compress concepts with offset
				Concept last = null;
				for(Concept c: diagnosisList){
					c.setConceptColor((stripe)?Color.black:Color.gray);
					String txt = c.getText().trim();
					if(last != null && txt.matches("[A-Za-z\\- ]+\\s*[0-9]") && txt.startsWith(last.getText().trim())){
						c.setText(txt.substring(last.getText().trim().length()+1));
					}else{
						last = c;
					}
					diagnosisPanel.add(c.getComponent());
					stripe = stripe ^ true;
				}
				diagnosisPanel.validate();
				//diagnosisPanel.getParent().validate();
			
				
				// add cases if allowed
				for(Concept c : casesList){
					c.setConceptColor((stripe)?Color.black:Color.gray);
					casesPanel.add(c.getComponent());
					stripe = stripe ^ true;
				}
				casesPanel.validate();
				
				//casesPanel.getParent().validate();
				loaded = true;
				//System.out.println("setup time "+(System.currentTimeMillis()-time)+" ms");
				
				// load parameters
				loadParameters(ontology);
				
				// remove progress bar
				//getPanel().remove(progress);
				//progress = null;
				setBusy(false);
				
				// update filter
				//getPanel().revalidate();
				//loadParameters(kc);
				setOntologyInfo();
			}
		})).start();
	}
	
	
	/**
	 * find out which pattern does this case match
	 * @param evidence
	 * @param dpatterns
	 * @return
	 */
	private List<Concept> getMatchingPatterns(IClass dx, IInstance inst, List<Concept> dpatterns){
		List<Concept> result = new ArrayList<Concept>();
		// check for multi-pattern
		ILogicExpression exp = dx.getEquivalentRestrictions();
		if(exp.getExpressionType() == ILogicExpression.OR){
			for(int i=0;i<exp.size();i++){
				if(exp.get(i) instanceof ILogicExpression){
					ILogicExpression e = (ILogicExpression) exp.get(i);
					// if this pattern matches
					if(e.evaluate(inst)){
						result.add(dpatterns.get(i));
					}
				}
			}
		}
		return result;
	}
	
	
	/**
	 * load misc relevant paramters for an ontology
	 * @param ont
	 */
	public void loadParameters(IOntology ont){
		Map<String,String> params = new HashMap<String, String>();
		Map<String,Integer> countsF = new HashMap<String, Integer>();
		Map<String,Integer> countsD = new HashMap<String, Integer>();
		// color cases
		int maxD = 0,maxF = 0;
		for(IInstance inst: ont.getClass(OntologyHelper.CASES).getDirectInstances()){
			// skip new case
			if(OntologyHelper.NEW_CASE.equals(inst.getName()))
				continue;
			
			// get status
			String st = (String) inst.getPropertyValue(ont.getProperty(OntologyHelper.HAS_STATUS));
			if(st != null){
				if(STATUS_INCOMPLETE.equals(st))
					params.put(inst.getName(),"red");
				else if(STATUS_COMPLETE.equals(st))
					params.put(inst.getName(),"blue");
				else if(STATUS_TESTED.equals(st))
					params.put(inst.getName(),"0 150 0"); //"green"
			}
			
			// count diseases
			for(IClass cls : inst.getDirectTypes()){
				if(OntologyHelper.isDisease(cls)){
					int i = (countsD.containsKey(cls.getName()))?countsD.get(cls.getName()):0;
					i ++;
					countsD.put(cls.getName(),i);
					if(i > maxD)
						maxD = i;
				}
			}
			
			// count findings
			for(Object o : inst.getPropertyValues(ont.getProperty(OntologyHelper.HAS_FINDING))){
				if(o instanceof IInstance){
					IInstance fn = (IInstance) o;
					for(IClass cls : fn.getDirectTypes()){
						if(OntologyHelper.isFeature(cls)){
							int i = (countsF.containsKey(cls.getName()))?countsF.get(cls.getName()):0;
							i ++;
							countsF.put(cls.getName(),i);
							if(i > maxF)
								maxF = i;
						}
					}
				}
			}
		}
		
		// compute prevelance of diseases and findings
		for(String key: countsD.keySet()){
			params.put(key,""+((double)countsD.get(key)/maxD));
		}
		for(String key: countsF.keySet()){
			params.put(key,""+((double)countsF.get(key)/maxF));
		}
		
		// select parameters option
		showParameters.setSelected(true);
		
		// now set parameters
		setConceptParameters(params);
	}
	
 	
	/**
	 * do some default action
	 */
	public void doAction(Concept c){
		if(c.getType() == Concept.CASE){
			displayCase(c);
		}else{
			displayInfo(c);
		}
	}
	
	
	/**
	 * display status
	 * @param c
	 */
	public void displayStatus(Concept c){
		int [] counts = new int [3];
		for(Iterator i=c.getRelatedConcepts().iterator();i.hasNext();){
			Concept rc = (Concept) i.next();
			counts[rc.getType()-1]++;
		}
		StringBuffer b = new StringBuffer("selected: "+c);
		if(counts[Concept.FEATURE-1] > 0)
			b.append(" features: "+counts[Concept.FEATURE-1]);
		if(counts[Concept.DIAGNOSIS-1] > 0)
			b.append(" diagnoses: "+counts[Concept.DIAGNOSIS-1]);
		if(counts[Concept.CASE-1] > 0)
			b.append(" cases: "+counts[Concept.CASE-1]);
		KnowledgeCloud.getInstance().setStatus(b.toString());
	}
	
	
	/**
	 * Display information
	 * @param c
	 */
	public void displayInfo(Concept c){
		//System.out.println("get codes");
		String name = c.getText().replaceAll("_"," ");
		String cui = c.getProperties().getProperty("code","");
		String def = c.getProperties().getProperty("definition","");
		String examples = c.getProperties().getProperty("examples");
		String [] files = KnowledgeCloud.parseList(examples);
		
		if(c.getType() == Concept.CASE){
			cui = def = "";
			//def = "difficulty: <b>"+c.getProperties().getProperty("difficulty")+"</b><br>"+
			//	  "ready: <b>"+c.getProperties().getProperty("ready")+"</b>";
			for(Object val: c.getProperties().keySet()){
				def += val+": <b>"+c.getProperties().getProperty(""+val)+"</b><br>";
			}
			
		}else if(c.getType() == Concept.DIAGNOSIS){
			cui = "";
		}
		
		//String img = "no example";
		//if(files.length > 0){
		//	img = "<img src=\""+files[0]+"\">";
		//}
		if(cui != null && cui.length() > 10)
			cui = cui.substring(0,10)+"..";
		// text panel
		//System.out.println("create components");
		UIHelper.HTMLPanel pnl = new UIHelper.HTMLPanel();
		pnl.setEditable(false);
		pnl.setBorder(new TitledBorder("Definition"));
		pnl.setPreferredSize(new Dimension(300,200));
		pnl.append("<b>"+name+"</b>"+cui+" <hr>");
		pnl.append(def);
		JScrollPane scroll = new JScrollPane(pnl);
		scroll.setPreferredSize(pnl.getPreferredSize());
		
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setLeftComponent(scroll);
		split.setResizeWeight(1.0);
		
		//images
		if(files != null && files.length > 0){
			JList images = new JList();
			images.setVisibleRowCount(1);
			Vector data = new Vector();
			int w = 0, h = 0;
			//System.out.println("load images");
		
			for(int i=0;i<files.length;i++){
				try{
					// create icon
					//System.out.println("\t"+files[i]);
					ImageIcon icon = new ImageIcon(new URL(files[i]));
					data.add(icon);
					// find out the size of largest icon
					if(icon.getIconWidth() > w)
						w = icon.getIconWidth();
					if(icon.getIconHeight() > h)
						h = icon.getIconHeight();
					//System.out.println(files[i]);
					
				}catch(MalformedURLException ex){
					//ex.printStackTrace();
					//System.err.println("malformed exception "+files[i]);
				}
			}
		
			//System.out.println("set images");
			// set cell width
			if(w >0 && h > 0){
				images.setFixedCellHeight(h);
				images.setFixedCellWidth(w);
			}
			images.setListData(data);
			//System.out.println("show dialog");
		
			// add component
			split.setRightComponent(new JScrollPane(images));
			split.setResizeWeight(.5);
			
		}
		notifyConcept(c,"definition");
		JComponent cmp = KnowledgeCloud.getInstance().getComponent();
		
		JOptionPane op = new JOptionPane(split,JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(cmp,"info");
		d.setModal(false);
		d.setVisible(true);
	}
	
	
	
	/**
	 * display case
	 * @param c
	 */
	public void displayCase(Concept c){
		String name = c.toString();
		
		// query servlet
		//Map map = (Map) Utils.queryServlet(servlet,"get-case-info&domain="+domain+"&case="+name);
		
		// get list of slides
		String info = "No Report Available";
		String [] slides = new String [0];

		IInstance inst = ontology.getInstance(name);
		if(inst != null){
			Object [] s = inst.getPropertyValues(ontology.getProperty(OntologyHelper.HAS_SLIDE));
			String t = (String) inst.getPropertyValue(ontology.getProperty(OntologyHelper.HAS_REPORT));
			if(s != null && s.length > 0){
				slides = (String [])s;
			}
			if(t != null)
				info = UIHelper.convertToHTML(t);
		}
		
		final JPanel viewerContainer = new JPanel();
		viewerContainer.setLayout(new BorderLayout());
		viewerContainer.setBackground(Color.white);
		viewerContainer.setOpaque(true);
		viewerContainer.setPreferredSize(new Dimension(500,500));
			
		
		// init buttons
		JToolBar toolbar = new JToolBar();
		toolbar.setMinimumSize(new Dimension(0,0));
		ButtonGroup grp = new ButtonGroup();
		AbstractButton selected = null;
		for(int i=0;i<slides.length;i++){
			final String image = slides[i];
			String text = slides[i];
			// strip foler
			int x = image.lastIndexOf("/");
			if(x > -1)
				text = text.substring(x+1);
			
			// strip suffic and prefix
			if(slides.length > 1 && text.startsWith(name))
				text = text.substring(name.length()+1);
			if(text.lastIndexOf(".") > -1)
				text = text.substring(0,text.lastIndexOf("."));
			// create buttons
			AbstractButton bt = new JToggleButton(text);
			bt.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					JToggleButton bt = (JToggleButton) e.getSource();
					if(bt.isSelected()){
						
						// make sure we load from the right sourrce
						ViewerFactory.setPropertyLocation(OntologyHelper.getInstitution(ontology.getURI()));
						
						// check if we need to switch viewer based on image type
						String type = ViewerFactory.recomendViewerType(image);
						
						final Viewer viewer = ViewerFactory.getViewerInstance(type);
						String dir = ViewerFactory.getProperties().getProperty(type+".image.dir","");
						viewer.setSize(new Dimension(500,500));
						
						
						
						// add screenshot button
						JButton screen = new JButton(Icons.getIcon(Icons.SCREENSHOT,24));
						screen.setToolTipText("Capture slide snapshot");
						screen.addActionListener(new ActionListener(){
							public void actionPerformed(ActionEvent e){
								try {
									JFileChooser chooser = new JFileChooser();
									chooser.setFileFilter(new ViewerHelper.JpegFileFilter());
									chooser.setPreferredSize(new Dimension(550, 350));
									int returnVal = chooser.showSaveDialog(cloudPanel);
									
									if (returnVal == JFileChooser.APPROVE_OPTION) {
										// select mode
										try {
											ViewerHelper.writeJpegImage(viewer.getSnapshot(), chooser.getSelectedFile());
										} catch (IOException ex) {
											ex.printStackTrace();
										}
									}
								} catch (java.security.AccessControlException ex) {
									JOptionPane.showMessageDialog(cloudPanel, 
											"You do not have permission to save screenshots on local disk.",
											"Error", JOptionPane.ERROR_MESSAGE);
								}
							}
						});
						
						viewer.getViewerControlPanel().add(Box.createHorizontalGlue());
						viewer.getViewerControlPanel().add(screen);
						
						
						// update viewer container
						viewerContainer.removeAll();
						viewerContainer.add(viewer.getViewerPanel(),BorderLayout.CENTER);
						viewerContainer.validate();
						viewerContainer.repaint();
						
						// open image
						try{
							viewer.openImage(dir+image);
						}catch(ViewerException ex){
							ex.printStackTrace();
						}
					}
				}
			});
			grp.add(bt);
			toolbar.add(bt);
			
			// select entry
			if(selected == null && (text.contains("HE") || slides.length == 1))
				selected = bt;
		}
		
		// create gui
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(toolbar,BorderLayout.NORTH);
		panel.add(viewerContainer,BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(500,600));
		
		UIHelper.HTMLPanel text = new UIHelper.HTMLPanel();
		text.setEditable(false);
		//text.setPreferredSize(new Dimension(350,600));
		text.append(info);
		text.setCaretPosition(0);
		JScrollPane scroll = new JScrollPane(text);
		scroll.setPreferredSize(new Dimension(350,600));
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,panel,scroll);
		split.setResizeWeight(1);
		
		// create dialog
		JOptionPane op = new JOptionPane(split,JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(cloudPanel,c.getText());
		d.setModal(false);
		d.setResizable(true);
		d.pack();
		d.setVisible(true);
		
		// load image
		if(selected != null)
			selected.doClick();
		
		//JOptionPane.showMessageDialog(this,"Not Implemented");
	}
	

	/**
	 * Concept selected/diselected
	 * @param c
	 * @param selected
	 */
	public void notifyConcept(Concept c, String action){
		//TODO: notify that concept has been selected
	}
	
	/**
	 * create popup menu
	 * @return
	 */
	public JPopupMenu createPopupMenu(){
		JPopupMenu menu = new JPopupMenu();
		
		glossary = new JMenuItem("Glossary",Icons.getIcon(Icons.GLOSSARY,16));
		glossary.addActionListener(this);
		menu.add(glossary);
		
		bookmark = new JMenuItem("Bookmark",Icons.getIcon(Icons.BOOKMARK,16));
		bookmark.addActionListener(this);
		menu.add(bookmark);
		
		menu.addSeparator();
		menu.add(UIHelper.createMenuItem("Open","Open Finding/Diagnosis/Case",Icons.OPEN,this));
		
		return menu;
	}
	
	/**
	 * get Popup menu
	 * @return
	 */
	public JPopupMenu getPopupMenu(){
		if(popup == null){
			popup = createPopupMenu();
		}
		return popup;
	}
	
	
	/**
	 * This class represents concepts that are put into 
	 * Knowledge cloud
	 * @author tseytlin
	 */
	private static class Concept extends UIHelper.Label implements Serializable, Comparable {
		public static double BOLD_VALUE = 0.00000001;
		public static final int FEATURE = 1, DIAGNOSIS = 2, CASE = 3;
		public static final Color BOOKMARK_COLOR = new Color(0,125,0);
		private final float SIZE_INC = 10;
		private Color background = Color.white,foreground = Color.black, inverse = Color.white;
		private Color color = foreground;
		private Set relatedConcepts,relatedAbsentConcepts;
		private transient JLabel label;
		private String text;
		private float fontSize;
		private double svalue,cvalue;
		private int type;
		private Properties properties;
		private Font font;
		
		/**
		 * create new concept
		 */
		public Concept(String name){
			this(name,0);
		}
		
		/**
		 * create new concept
		 */
		public Concept(String name, int type){
			super(UIHelper.getPrettyClassName(name)+"  ");
			this.type = type;
			setOpaque(true);
			setBackground(background);
			setForeground(foreground);
			//Font font = getFont();
			font = new Font("Arial",Font.PLAIN,12);
			fontSize = font.getSize2D();
			setFont(font);
			this.text = name;
			this.label = this;
		}
		
		/**
		 * Set concept color
		 * @param c
		 */
		public void setConceptColor(Color c){
			color = c;
			setForeground(c);
			foreground = c;
		}
		
		/**
		 * set concept color
		 */
		public Color getConceptColor(){
			return color;
		}
		/**
		 * Get label component
		 * @return
		 */
		public JComponent getComponent(){
			/*
			if(label == null){
				label = new JLabel(text.toLowerCase());
				label.setOpaque(true);
				label.setBackground(background);
				label.setForeground(foreground);
			}*/
			return label;
		}
		
		/**
		 * Set value for this concept
		 * float 0-1.0
		 * @param val
		 */
		public void setSizeValue(double val){
			this.svalue = val;
			int style = (val == BOLD_VALUE)?Font.BOLD:Font.PLAIN;
			if(label != null){
				float size = fontSize+(float)(SIZE_INC*val);
				Font font = label.getFont();
				label.setFont(font.deriveFont(style,size));
				if(label.getParent() != null)
					label.getParent().validate();
			}
		}
		
		/**
		 * Set value for this concept
		 * float 0-1.0
		 * @param val
		 */
		public void setColorValue(double val){
			this.cvalue = val;
			if(label != null){
				foreground = (val >= 0)?new Color((int)(255*val),0,0):new Color(0,(int)(128*-val),0);
				//inverse = Color.black;
				label.setForeground(foreground);
				//if(label.getParent() != null)
				//	label.getParent().validate();
			}
		}
		
		/**
		 * Set value for this concept
		 * float 0-1.0
		 * @param val
		 */
		public void setColorValue(Color color){
			if(label != null){
				foreground = color;
				//inverse = Color.black;
				label.setForeground(foreground);
			}
		}
		
		
		/**
		 * How many concepts link to this Concept
		 * This is the same as number of related concepts
		 * @return
		 */
		public int getReferenceCount(){
			return relatedConcepts.size();
		}
		
		
		/**
		 * How many concepts link to this Concept of type
		 * This is the same as number of related concepts
		 * @return
		 */
		public int getReferenceCount(int type){
			int n = 0;
			for(Iterator i = relatedConcepts.iterator();i.hasNext();){
				Concept c = (Concept) i.next();
				if(c.getType() == type)
					n ++;
			}
			return n;
		}
		
		
		/**
		 * Highlight this concept for search purposes
		 * @param b
		 */
		public void setHighlight(boolean b){
			// make sure component is there
			if(label == null )
				return;
			//don't do anything if value is the same
			if(isHighlighted() == b)
				return;
			background = (b)?Color.yellow:Color.white;
			label.setBackground(background);
		}
		
		/**
		 * Is highlighted?
		 */
		public boolean isHighlighted(){
			return background.equals(Color.yellow);
		}
		
		/**
		 * Select current concept
		 * @param b
		 */
		public void setSelected(boolean b){
			if(label == null)
				return;
			label.setBackground((b)?Color.red:background);
			label.setForeground((b)?inverse:foreground);
			//label.setFont((b)?font.deriveFont(Font.BOLD):font.deriveFont(Font.PLAIN));
			if(label.getParent() != null)
				label.getParent().repaint();
		}
		
		/**
		 * Select current concept
		 * @param b
		 */
		public void setAbsentSelected(boolean b){
			if(label == null)
				return;
			label.setBackground((b)?Color.lightGray:background);
			label.setForeground((b)?Color.red:foreground);
			//label.setFont((b)?font.deriveFont(Font.BOLD):font.deriveFont(Font.PLAIN));
			if(label.getParent() != null)
				label.getParent().repaint();
		}
		
		/**
		 * Select related concepts
		 * @param b
		 */
		public void selectRelatedConcept(boolean b){
			if(relatedConcepts != null){
				for(Iterator i=relatedConcepts.iterator();i.hasNext();){
					((Concept) i.next()).setSelected(b);
				}
			}
			
			if(relatedAbsentConcepts != null){
				for(Iterator i=relatedAbsentConcepts.iterator();i.hasNext();){
					((Concept) i.next()).setAbsentSelected(b);
				}
			}
		}
		
		/**
		 * is concept bookmarked
		 * @return
		 */
		public boolean isBookmared(){
			return background.equals(BOOKMARK_COLOR); 
		}
		
		
		/**
		 * Select current concept
		 * @param b
		 */
		public void setBookmark(boolean b){
			if(label == null)
				return;
			
			background = (b)?BOOKMARK_COLOR:Color.white;
			foreground = (b)?inverse:color;
			
			label.setBackground(background);
			label.setForeground(foreground);
			//label.setFont((b)?font.deriveFont(Font.BOLD):font.deriveFont(Font.PLAIN));
			if(label.getParent() != null)
				label.getParent().repaint();
		}
		
		/**
		 * Select current concept
		 * @param b
		 */
		public void setBookmarkAbsent(boolean b){
			if(label == null)
				return;
			
			background = (b)?Color.lightGray:Color.white;
			foreground = (b)?BOOKMARK_COLOR:color;
			
			label.setBackground(background);
			label.setForeground(foreground);
			//label.setFont((b)?font.deriveFont(Font.BOLD):font.deriveFont(Font.PLAIN));
			if(label.getParent() != null)
				label.getParent().repaint();
		}
		
		
		/**
		 * Select related concepts
		 * @param b
		 */
		public void bookmarkRelatedConcept(boolean b){
			if(relatedConcepts != null){
				for(Iterator i=relatedConcepts.iterator();i.hasNext();){
					((Concept) i.next()).setBookmark(b);
				}
			}
			if(relatedAbsentConcepts != null){
				for(Iterator i=relatedAbsentConcepts.iterator();i.hasNext();){
					((Concept) i.next()).setBookmarkAbsent(b);
				}
			}
		}
		
		/**
		 * @return the relatedConcepts
		 */
		public Set getRelatedConcepts() {
			if(relatedConcepts == null)
				relatedConcepts = new TreeSet();
			return relatedConcepts;
		}

		/**
		 * @param relatedConcepts the relatedConcepts to set
		 */
		public void setRelatedConcepts(Set relatedConcepts) {
			this.relatedConcepts = relatedConcepts;
		}
		
		/**
		 * Add related concept
		 * @param c
		 */
		public void addRelatedConcept(Concept c){
			getRelatedConcepts().add(c);
		}
		
		/**
		 * @return the relatedConcepts
		 */
		public Set getRelatedAbsentConcepts() {
			if(relatedAbsentConcepts == null)
				relatedAbsentConcepts = new TreeSet();
			return relatedAbsentConcepts;
		}

		/**
		 * @param relatedConcepts the relatedConcepts to set
		 */
		public void setRelatedAbsentConcepts(Set relatedConcepts) {
			this.relatedAbsentConcepts = relatedConcepts;
		}
		
		/**
		 * Add related concept
		 * @param c
		 */
		public void addRelatedAbsentConcept(Concept c){
			getRelatedAbsentConcepts().add(c);
		}
		
		
		/**
		 * @return the properties
		 */
		public Properties getProperties() {
			if(properties == null)
				properties = new Properties();
			return properties;
		}
		
		/**
		 * return string representation of this object
		 */
		public String toString(){
			return text;
		}
		
		/**
		 * Compare 2 Concepts
		 * @param obj
		 * @return
		 */
		public int compareTo(Object obj){
			return toString().compareTo(obj.toString());
		}

		/**
		 * @return the cvalue
		 */
		public double getColorValue() {
			return cvalue;
		}

		/**
		 * @return the svalue
		 */
		public double getSizeValue() {
			return svalue;
		}

		/**
		 * @return the type
		 */
		public int getType() {
			return type;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(int type) {
			this.type = type;
		}
	}
	
	/**
	 * This class listends to mouse movements that envolve concepts 
	 * Knowledge cloud
	 * @author tseytlin
	 */
	private static class ConceptListener extends MouseAdapter {
		public void mouseEntered(MouseEvent e){
			if(e.getSource() instanceof Concept){
				Concept c = (Concept) e.getSource();
				c.setSelected(true);
				c.selectRelatedConcept(true);
				instance.notifyConcept(c,"selected");
			}
		}
		public void mouseExited(MouseEvent e){
			if(e.getSource() instanceof Concept){
				Concept c = (Concept) e.getSource();
				c.setSelected(false);
				c.selectRelatedConcept(false);
				instance.notifyConcept(c,"unselected");
			}
		}
		public void mouseClicked(MouseEvent e){
			if(e.getSource() instanceof Concept){
				Concept c = (Concept) e.getSource();
				instance.displayStatus(c);
				if(e.getClickCount() == 2){
					instance.doAction(c);
				}
				
			}
		}
		
		// show context menu
		public void mousePressed(MouseEvent e){
			if(e.getSource() instanceof Concept){
				if(e.isPopupTrigger()){
					Concept c = (Concept) e.getSource();
					JPopupMenu menu = instance.getPopupMenu();
					modifyPopup(menu,c);
					instance.bookmark.setText((c.isBookmared())?"UnBookmark":"Bookmark");
					menu.show(c,e.getX(),e.getY());
				}
			}
		}
		
		//	show context menu
		public void mouseReleased(MouseEvent e){
			if(e.getSource() instanceof Concept){
				if(e.isPopupTrigger()){
					Concept c = (Concept) e.getSource();
					JPopupMenu menu = instance.getPopupMenu();
					instance.bookmark.setText((c.isBookmared())?"UnBookmark":"Bookmark");
					menu.show(c,e.getX(),e.getY());
				}
			}
		}
	}
	
	private static void modifyPopup(JPopupMenu menu, Concept c){
		JMenuItem m = (JMenuItem) menu.getComponent(menu.getComponentCount()-1);
		switch(c.getType()){
		case Concept.CASE: 		m.setText("Open Case"); m.setToolTipText("Open Case in Case Tab"); break;
		case Concept.DIAGNOSIS: m.setText("Open Diagnosis"); m.setToolTipText("Open Diagnosis in Diagnosis Builder"); break;
		case Concept.FEATURE:   m.setText("Open Finding"); m.setToolTipText("Open Finding in Hierarchy Builder"); break;
		}
	}
	
	/**
	 * Search Document Listener
	 */
	private static class SearchDocument extends DefaultStyledDocument {
		private KnowledgeCloud cloud;
		public SearchDocument(KnowledgeCloud kc){
			this.cloud = kc;
		}
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException{
			super.insertString(offset, str, a);
			cloud.doSearch();
		}
		public void remove(int offs, int len) throws BadLocationException{
			super.remove(offs,len);
			cloud.doSearch();
		}
	}

	/**
	 * @return the casesMap
	 */
	public Map getCasesMap() {
		return casesMap;
	}



	/**
	 * @return the diagnosisMap
	 */
	public Map getDiagnosisMap() {
		return diagnosisMap;
	}



	/**
	 * @return the featureMap
	 */
	public Map getFeatureMap() {
		return featureMap;
	}
}
