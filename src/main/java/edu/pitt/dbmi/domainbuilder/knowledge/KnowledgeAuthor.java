package edu.pitt.dbmi.domainbuilder.knowledge;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.pitt.dbmi.domainbuilder.*;
import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.ontology.*;

public class KnowledgeAuthor extends JPanel implements DomainBuilderComponent {
	private KnowledgeTree knowledgeTree;
	private DiagnosisBuilder diagnosisBuilder;
	private ReportBuilder reportBuilder;
	private JTabbedPane tabs;
	private Component lastComponent;
	
	private IOntology kb,instances;
	private boolean loaded;
	private static boolean saving;
	
	public KnowledgeAuthor(){
		super();
		createGUI();
	}
	
	/**
	 * create GUI for case author panel
	 */
	private void createGUI(){
		setLayout(new BorderLayout());
		
		// create knowledge tree
		knowledgeTree = new KnowledgeTree();
		diagnosisBuilder = new DiagnosisBuilder();
		reportBuilder = new ReportBuilder();
		
		
		// create tabbed component
		tabs = new JTabbedPane(SwingConstants.LEFT);
		tabs.addTab(null,new VerticalTextIcon("Hierarchy Builder",true),knowledgeTree);
		tabs.addTab(null,new VerticalTextIcon("Diagnosis Builder",true),diagnosisBuilder);
		tabs.addTab(null,new VerticalTextIcon("Report Builder",true),reportBuilder);
		add(tabs,BorderLayout.CENTER);
		lastComponent = tabs.getSelectedComponent();
		tabs.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				updateTab();
			}
		});
	}
	
	/**
	 * update tab
	 */
	private void updateTab(){
		// cleanup
		if(knowledgeTree.equals(lastComponent))
			knowledgeTree.unselected();
		else if(diagnosisBuilder.equals(lastComponent))
			diagnosisBuilder.unselected();
		else if(reportBuilder.equals(lastComponent))
			reportBuilder.unselected();
		// set menu bar
		DomainBuilder.getInstance().getFrame().setJMenuBar(getMenuBar());
		lastComponent = tabs.getSelectedComponent();
	}
	
	
	
	/**
	 * get component
	 */
	public Component getComponent() {
		return this;
	}

	/**
	 * get icon
	 */
	public Icon getIcon() {
		return Icons.getIcon(Icons.SEARCH,16);
	}

	/**
	 * get Name
	 */
	public String getName() {
		return "Knowledge";
	}
	
	public void dispose(){
		//TODO:
		//knowledgeTree.unselected();
	}
	
	/**
	 * listen for property changes
	 */
	public void propertyChange(PropertyChangeEvent evt){	
		String cmd = evt.getPropertyName();
		if(cmd.equals(OntologyHelper.KB_LOADED_EVENT)){
			URI uri = (URI) evt.getNewValue();
			// load kb
			instances = DomainBuilder.getRepository().getOntology(uri);
			kb = DomainBuilder.getRepository().getOntology(OntologyHelper.getKnowledgeBase(uri));
			knowledgeTree.setOntology(kb);
			diagnosisBuilder.setOntology(kb);
			reportBuilder.setOntology(kb);
			loaded = false;
		}else if(cmd.equals(OntologyHelper.SUGGEST_TERM_EVENT)){
			knowledgeTree.doSuggestTerm(""+evt.getOldValue(),""+evt.getNewValue());
		}else if(cmd.equals(OntologyHelper.SHOW_CONCEPT_EVENT)){
			String c = ""+evt.getNewValue();
			if(c.length() > 0 && !"null".equals(c)){
				DomainBuilder.getInstance().setSelectedTab(this);
				tabs.setSelectedComponent(knowledgeTree);
				knowledgeTree.doSelectConcept(c);
			}
		}else if(cmd.equals(OntologyHelper.OPEN_DIAGNOSIS_EVENT)){
			Collection list = (Collection) evt.getNewValue();
			if(!list.isEmpty()){
				DomainBuilder.getInstance().setSelectedTab(this);
				tabs.setSelectedComponent(diagnosisBuilder);
				diagnosisBuilder.openDiagnoses(list);
			}
		}else if(cmd.equals(OntologyHelper.CLEAR_DIAGNOSIS_EVENT)){
			diagnosisBuilder.reset();
		}else if(cmd.equals(OntologyHelper.RELOAD_UI_EVENT)){
			diagnosisBuilder.reset();
			reportBuilder.reload();
		}
	}
	
	/**
	 * create new case
	 */
	public static void saveOntology(IOntology ontology){
		saving = true;
		DomainBuilder.getInstance().setBusy(true);
		JProgressBar p = DomainBuilder.getInstance().getProgressBar();
		OntologySynchronizer.getInstance().run(p);
		ConceptHandler cc = OntologyHelper.getConceptHandler(ontology);
		p.setString("Saving Diagnostic Rules ...");
		boolean removedClses = cc.hasRemovedClasses();
		cc.flush();
		p.setString("Saving Ontology ...");
		IOntology instances = OntologyHelper.getCaseBase(ontology);
		try{
			// save instances first not to overwrite stuff done in KB
			instances.save();
			ontology.save();
		}catch(IOntologyException ex){
			JOptionPane.showMessageDialog(DomainBuilder.getInstance().getFrame(),
			ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
		
		
		// do automatic upload for file repository
		
		//if(DomainBuilder.getRepository() instanceof FileRepository){
			//JOptionPane.showMessageDialog(DomainBuilder.getInstance().getFrame(),
			//	"Don't forget to publish changes by exporting this domain","Warning",JOptionPane.WARNING_MESSAGE);
			/*
			try{
				
				// figure out file location and upload it if repository is not database
				File f = new File(getLocalRepositoryFolder(),ontology.getURI().getPath());
				
				// do file upload operation
				if(f.exists())
					UIHelper.upload(f);
			}catch(Exception ex){
				ex.printStackTrace();
			}*/
		//}
		
		// update terminology
		p.setString("Updating Terminology ...");
		DomainBuilder.getInstance().loadTerminology(ontology.getURI());
		
		
		// reload case instances db
		p.setString("Reloading Instance Ontology ...");
	
		try{
			
			instances.reload();
		}catch(IOntologyException ex){
			ex.printStackTrace();
		}
		
		// if there were classes removed, then reset report builder and dx builder
		if(removedClses){
			// notify that case was reloaded
			DomainBuilder.getInstance().firePropertyChange(OntologyHelper.RELOAD_UI_EVENT,null,null);
		}
		
		// notify that case was reloaded
		DomainBuilder.getInstance().firePropertyChange(OntologyHelper.CASE_KB_RELOADED_EVENT,null,instances);
		
		DomainBuilder.getInstance().setBusy(false);	
		saving = false;
	}
	
	public static boolean isSaving(){
		return saving;
	}
	

	public JMenuBar getMenuBar(){
		Component c = tabs.getSelectedComponent();
		if(knowledgeTree.equals(c)){
			knowledgeTree.selected();
			return knowledgeTree.getMenuBar();
		}
		if(diagnosisBuilder.equals(c)){
			diagnosisBuilder.selected();
			return diagnosisBuilder.getMenuBar();
		}
		reportBuilder.selected();
		return reportBuilder.getMenuBar();
	}
	
	
	/**
	 * load whatever resources one needs to get this piece working 
	 */
	public void load(){
		setOntologyInfo(knowledgeTree.getOntology());
		if(loaded)
			return;
		
		knowledgeTree.load();
		loaded = true;
	}
	
	/**
	 * set ontology info
	 * @param ont
	 */
	public static void setOntologyInfo(IOntology ont){
		if(ont == null)
			return;
		
		StringBuffer text= new StringBuffer("<html>");
		UIHelper.printKnowledgeStatus(text,""+ont.getURI());
		UIHelper.printUserStatus(text);
		
		JLabel st = DomainBuilder.getInstance().getInfoLabel();
		st.setText(""+text);
	}
}
