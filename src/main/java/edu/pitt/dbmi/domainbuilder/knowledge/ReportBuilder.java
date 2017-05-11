package edu.pitt.dbmi.domainbuilder.knowledge;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptExpression;
import edu.pitt.dbmi.domainbuilder.caseauthor.ConceptSelector;

import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.dbmi.domainbuilder.widgets.*;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.IRestriction;



public class ReportBuilder extends JPanel implements ActionListener, ListSelectionListener,ContainerListener, PropertyChangeListener {
	private final int TRIGGERS = 0;
	private final int FINDINGS = 1;
	
	private JList schemas; //, triggers;
	private JPanel goalPanel;
	private JComponent addFindingPanel;//,triggerPanel;
	private TreeDialog findingDialog; //triggerDialog,;
	private IOntology ontology;
	private JMenuBar menubar;
	private Frame frame; 
	private JPopupMenu popup;
	private JToolBar toolbar,schemaToolBar;//,triggerToolBar;
	private JButton addFinding;
	private OntologySynchronizer sync;
	private IClass currentSchema;
	private boolean modified,readOnly,showSummary = true;
	private Map<IClass,ConceptExpression[]> templateMap;
	private TriggerEntryPanel triggerEditor;
	private File lastFile;
	
	public ReportBuilder(){
		buildGUI();
		sync = OntologySynchronizer.getInstance();
		templateMap = new LinkedHashMap<IClass, ConceptExpression[]>();
	}
	private void buildGUI(){
		setLayout(new BorderLayout());
		goalPanel = new JPanel();
		goalPanel.setBackground(Color.white);
		goalPanel.setOpaque(true);
		goalPanel.setLayout(new BoxLayout(goalPanel,BoxLayout.Y_AXIS));
		goalPanel.add(getAddFindingPanel());
		goalPanel.addContainerListener(this);
		add(createSchemaSelector(),BorderLayout.WEST);
		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		panel.setBackground(Color.white);
		//panel.setLayout(new BorderLayout());
		//panel.add(getTriggerPanel(),BorderLayout.NORTH);
		panel.setTopComponent(getTriggerPanel());
		JScrollPane scroll = new JScrollPane(goalPanel);
		scroll.setOpaque(false);
		scroll.setBorder(new TitledBorder("Reportable Items in the Template"));
		scroll.getVerticalScrollBar().setUnitIncrement(50);
		
		//panel.add(scroll,BorderLayout.CENTER);
		panel.setBottomComponent(scroll);
		add(getToolBar(),BorderLayout.NORTH);
		add(panel,BorderLayout.CENTER);
		setEditableReport(false);
	}
	
	public Frame getFrame(){
		if(frame == null)
			frame = JOptionPane.getFrameForComponent(this);
		return frame;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public boolean isModified(){
		return modified;
	}
	
	private JComponent createSchemaSelector(){
		JPanel comp = new JPanel();
		comp.setLayout(new BorderLayout());
		schemas = new JList(new DefaultListModel());
		schemas.addListSelectionListener(this);
		schemas.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				if(readOnly)
					return;
				
				if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
					int x = UIHelper.getIndexForLocation(schemas,e.getPoint());
					if(x > -1)
						schemas.setSelectedIndex(x);
					getPopupMenu().show(schemas,e.getX(),e.getY());
				}
			}
		});
		comp.add(getSchemaToolBar("Templates"),BorderLayout.NORTH);
		comp.add(new JScrollPane(schemas),BorderLayout.CENTER);
		comp.setPreferredSize(new Dimension(200,300));
		comp.setBorder(new EmptyBorder(0,0,0,5));
		return comp;
	}


	public JPopupMenu getPopupMenu(){
		// init popus
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("Edit","Edit Template Name",Icons.EDIT,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Add Template","Add Report Template",Icons.PLUS,this));
			popup.add(UIHelper.createMenuItem("Remove Template","Remove Report Template",Icons.MINUS,this));
		}
		return popup;
	}
	
	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar getSchemaToolBar(String title){
		if(schemaToolBar == null){
			JToolBar toolbar = new JToolBar();
			final int size = 16;
			toolbar.setFloatable(false);
			toolbar.setBackground(Color.white);
			toolbar.add(UIHelper.createButton("Add Template","Add Template",Icons.PLUS,size,this));
			toolbar.add(UIHelper.createButton("Remove Template","Remove Template",Icons.MINUS,size,this));
			toolbar.addSeparator();
			toolbar.add(new JLabel(title));
			schemaToolBar = toolbar;
		}
		return schemaToolBar;
	}
	
	private JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new JToolBar();
			if(Communicator.isConnected())
				toolbar.add(UIHelper.createButton("Publish","Save and Publish Knowledge Base",Icons.PUBLISH,24,this));
			else
				toolbar.add(UIHelper.createButton("Save","Save to Knowledge Base",Icons.SAVE,24,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("Import","Import Template from caDSR",Icons.IMPORT,24,this));
			toolbar.add(UIHelper.createButton("Export","Export to OWL File",Icons.EXPORT,this));
			toolbar.addSeparator();
			addFinding = UIHelper.createButton("Add Reportable Items","Add Reportable Item",Icons.ADD,24,-1,true,this);
			toolbar.add(addFinding);
			//getToolBar().getComponentAtIndex(0).setEnabled(false);
		}
		return toolbar;
	}
	
	
	/**
	 * clear everything
	 */
	private void reset(){
		// clear dialogs
		if(triggerEditor != null)
			triggerEditor.reset();
		//triggerDialog = null;
		findingDialog = null;
		((DefaultListModel)schemas.getModel()).removeAllElements();
		setEditableReport(false);
	}
	
	/**
	 * invoked when tab is no longer visible, perhaps save the ontology or something
	 */
	public void unselected(){
		if(readOnly)
			return;
		
		if(isModified()){
			doCheckSave();
		}
	}
	
	/**
	 * check and save if needed
	 */
	private void doCheckSave(){
		int r = JOptionPane.showConfirmDialog(getFrame(),ontology.getName()+" knowledge base has been modified.\n" +
				"Would you like to save the changes?","Save?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if(r == JOptionPane.YES_OPTION){
			doSave();
		}
	}
	
	/**
	 * invoked when tab is is longer visible, perhaps save the ontology or something
	 */
	public void selected(){
		
	}
	
	public void reload(){
		setOntology(ontology);
	}
	
	
	/**
	 * @param ontology the ontology to set
	 */
	public void setOntology(IOntology ont) {
		this.ontology = ont;
		reset();
		
		// reset editor
		if(triggerEditor != null)
			triggerEditor.setOntology(ontology);
	
		
		// clear goal panel
		goalPanel.removeAll();
		goalPanel.add(getAddFindingPanel());
		goalPanel.revalidate();
		
		OntologyHelper.getConceptHandler(ont).setReportBuilder(this);
		modified = false;
		
		setReadOnly(OntologyHelper.isReadOnly(ont));
		setEditableReport(false);
		
		// load scheams
		loadSchemas();
	}
	
	/**
	 * create new case
	 */
	private void doSave(){
		(new Thread(new Runnable(){
			public void run(){
				KnowledgeAuthor.saveOntology(ontology);
				modified = false;
				//getToolBar().getComponentAtIndex(0).setEnabled(false);
			}
		})).start();
		
	}
	
	/**
	 * undo last action
	 */
	private void doUndo(){
		sync.undo();
	}
	
	/**
	 * edit properties
	 */
	private void doProperties(){
		ResourcePropertiesPanel panel = new ResourcePropertiesPanel(ontology);
		int r = JOptionPane.showConfirmDialog(getFrame(),panel,"Properties",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			panel.saveProperties(ontology);
		}
	}
	
	/**
	 * add schema
	 */
	private void doAddSchema(){
		String name = UIHelper.showInputDialog(this,"enter template name",Icons.getIcon(Icons.TAG,24));
		if(name != null){
			addSchema(name);
			//getToolBar().getComponentAtIndex(0).setEnabled(true);
		}
	}
	
	/**
	 * add schema
	 */
	private void doRemoveSchema(){
		Object [] obj = schemas.getSelectedValues();
		if(obj.length > 0){
			int r = JOptionPane.showConfirmDialog(getFrame(),"Are you sure you want to remove selected template(s)?",
													"Question",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
			if(r == JOptionPane.YES_OPTION){
				for(int i=0;i<obj.length;i++){
					removeSchema(obj[i]);
				}
				//getToolBar().getComponentAtIndex(0).setEnabled(true);
			}
		}
	}
	
	
	private void doEditTemplate(){
		Object obj = schemas.getSelectedValue();
		if(obj != null && obj instanceof ConceptEntry){
			ConceptEntry e = (ConceptEntry) obj;
			String name = UIHelper.showInputDialog(this,"enter template name",e.getName(),Icons.getIcon(Icons.TAG,24));
			if(name != null && !e.getName().equals(name)){
				if(!name.endsWith(OntologyHelper.SCHEMA))
					name = name+OntologyHelper.SCHEMA;
				e.setName(name);
				schemas.repaint();
				//getToolBar().getComponentAtIndex(0).setEnabled(true);
			}
		}
	}
	
	/**
	 * add schema
	 *
	private void doAddTrigger(){
		if(triggerDialog == null){
			triggerDialog = new TreeDialog(getFrame());
			triggerDialog.setTitle("Select Trigger");
			triggerDialog.setRoot(ontology.getClass(OntologyHelper.CONCEPTS));
		}
		triggerDialog.setVisible(true);
		// add result
		for(ConceptEntry entry: (ConceptEntry [])triggerDialog.getSelectedObjects()){
			addTrigger(entry);
			if(templateMap.containsKey(currentSchema)){
				// should be there now
				templateMap.get(currentSchema)[TRIGGERS].add(entry.getConceptClass());
			}	
			final ConceptEntry e = entry;
			sync.addOntologyAction(new OntologyAction(){
				public void run(){}
				public void undo(){
					removeTrigger(e);
				}
			});
		}
		//getToolBar().getComponentAtIndex(0).setEnabled(true);
		schemas.revalidate();
	}
	*/
	
	/**
	 * add schema
	 */
	private void doAddFinding(){
		if(findingDialog == null){
			findingDialog = new TreeDialog(getFrame());
			findingDialog.setTitle("Add Reportable Findings");
			IClass [] roots = new IClass [] {
					ontology.getClass(OntologyHelper.DISEASES),
					ontology.getClass(OntologyHelper.PROGNOSTIC_FEATURES)};
			//TODO: exclude the diseases?
			findingDialog.setRoots(roots);
		}
		findingDialog.setVisible(true);
		// add result
		for(ConceptEntry entry: (ConceptEntry [])findingDialog.getSelectedObjects()){
			addFinding(entry);
			final ConceptEntry e = entry;
			sync.addOntologyAction(new OntologyAction(){
				public void run(){}
				public void undo(){
					removeFinding(e);
				}
			});
		}
		//getToolBar().getComponentAtIndex(0).setEnabled(true);
		goalPanel.revalidate();
	}
	
	/**
	 * get schema panel
	 * @return
	 */
	private JComponent getTriggerPanel(){
		if(triggerEditor == null){
			// creat toolbar
			/*
			final int size = 16;
			JToolBar toolbar = new JToolBar();
			toolbar.setFloatable(false);
			toolbar.setBackground(Color.white);
			toolbar.setBorder(new TitledBorder("Triggers in the Template"));
			//toolbar.add(new JLabel("Triggers"));
			//toolbar.addSeparator();
			triggers = new JList(new DefaultListModel());
			triggers.setVisibleRowCount(1);
			triggers.setLayoutOrientation(JList.HORIZONTAL_WRAP);
			triggers.addMouseListener(new MouseAdapter(){
				public void mouseClicked(MouseEvent e){
					if(triggers.isSelectionEmpty() && e.getClickCount() == 2){
						doAddTrigger();
					}
				}
			});
			toolbar.add(UIHelper.createButton("Add Trigger","Add Trigger",Icons.PLUS,size,this));
			toolbar.add(UIHelper.createButton("Remove Trigger","Remove Trigger",Icons.MINUS,size,this));
			toolbar.add(new JScrollPane(triggers));
			triggerPanel = toolbar;
			*/
			triggerEditor = new TriggerEntryPanel();
			triggerEditor.addPropertyChangeListener(this);
			//triggerEditor.setOntology(ontology);
			
		}
		return triggerEditor;
	}
	
	
	/**
	 * create add finding panel
	 * @return
	 */
	private JComponent getAddFindingPanel(){
		if(addFindingPanel == null){
			JPanel panel = new JPanel();
			panel.setOpaque(false);
			panel.setLayout(new GridBagLayout());
			JButton bt = new JButton("Add Reportable Items",Icons.getIcon(Icons.ADD,24));
			bt.setActionCommand("Add Reportable Items");
			bt.addActionListener(this);
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.CENTER;
			panel.add(bt,c);
			addFindingPanel = panel;
		}
		return addFindingPanel;
	}
	
	

	
	/**
	 * add schema
	 *
	private void doRemoveTrigger(){
		Object [] sel = triggers.getSelectedValues();
		for(int i=0;i<sel.length;i++){
			if(sel[i] instanceof ConceptEntry){
				final ConceptEntry e = (ConceptEntry) sel[i];
				removeTrigger(e);
				if(templateMap.containsKey(currentSchema)){
					// should be there now
					templateMap.get(currentSchema)[TRIGGERS].remove(e.getConceptClass());
				}
				sync.addOntologyAction(new OntologyAction(){
					public void run(){}
					public void undo(){
						addTrigger(e);
					}
				});
				//getToolBar().getComponentAtIndex(0).setEnabled(true);
			}
		}
	}
	*/
	
	/**
	 * add schema
	 */
	private void doImport(){
		//TODO
		JOptionPane.showMessageDialog(this,"Not Implemented!");
	}
	
	private void doPublish(){
		if(readOnly)
			return;
		
		(new Thread(new Runnable(){
			public void run(){
				
				// do regular save
				KnowledgeAuthor.saveOntology(ontology);
				modified = false;
							
				// reset read only flag
				setReadOnly(OntologyHelper.isReadOnly(ontology));
				
				// now do export
				if(Communicator.isConnected()){
					try{
						// don't need to write it, if local copy exists
						// figure out file location and upload it if repository is not database
						File fc = new File(getLocalRepositoryFolder(),ontology.getURI().getPath());
						if(DomainBuilder.getRepository() instanceof FileRepository && fc != null && fc.exists()){
							// do file upload operation
							if(!isReadOnly(ontology)){
								UIHelper.upload(fc);
								if(((FileRepository)DomainBuilder.getRepository()).isServerMode())
									UIHelper.delete(fc);
							}
						}
					}catch(Exception ex){
						ex.printStackTrace();
						JOptionPane.showMessageDialog(getFrame(),ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);							
					}
					DomainBuilder.getInstance().setBusy(false);
				}
			}
		})).start();
	}
	
	
	/**
	 * do export to OWL file
	 */
	private void doExport(){
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".owl");
			}
			public String getDescription() {
				return "OWL File";
			}
			
		});
		if(lastFile != null)
			chooser.setSelectedFile(new File(lastFile.getParentFile(),ontology.getName()));
		else
			chooser.setSelectedFile(new File(ontology.getName()));
		if(chooser.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION){
			lastFile = chooser.getSelectedFile();
			DomainBuilder.getInstance().setBusy(true);
			DomainBuilder.getInstance().getProgressBar().setIndeterminate(true);
			(new Thread(new Runnable(){
				public void run(){
					try{
						// don't need to write it, if local copy exists
						// figure out file location and upload it if repository is not database
						File fc = new File(getLocalRepositoryFolder(),ontology.getURI().getPath());
						if(DomainBuilder.getRepository() instanceof FileRepository && fc != null && fc.exists()){
							// do file upload operation
							UIHelper.copy(fc,lastFile);
							if(!isReadOnly(ontology)){
								UIHelper.upload(fc);
								if(((FileRepository)DomainBuilder.getRepository()).isServerMode())
									UIHelper.delete(fc);
							}
						}else{
							ontology.write(new FileOutputStream(lastFile),IOntology.OWL_FORMAT);
							// copy to its own discrete location
							if(!isReadOnly(ontology))
								UIHelper.backup(lastFile,new File(OntologyHelper.getLocalOntologyFolder(ontology),lastFile.getName()));
						}
					
						JOptionPane.showMessageDialog(getFrame(),ontology.getName()+" saved as "+lastFile.getAbsolutePath());
					}catch(Exception ex){
						ex.printStackTrace();
						JOptionPane.showMessageDialog(getFrame(),ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);							
					}
					DomainBuilder.getInstance().setBusy(false);
				}
			})).start();
		}
	}
	
	/**
	 * do add trigger
	 * @param e
	 *
	private void addTrigger(ConceptEntry e){
		
		//if(model.getSize() > 0)
		//	model.addElement("and");
		final ConceptEntry entry = e;
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				DefaultListModel model = (DefaultListModel)triggers.getModel();
				model.addElement(entry);
			}
			
		});
	
	}
	*/
	/**
	 * do add trigger
	 * @param e
	 *
	private void removeTrigger(ConceptEntry e){
		final ConceptEntry entry = e;
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				DefaultListModel model = (DefaultListModel)triggers.getModel();
				model.removeElement(entry);
			}
			
		});
	}
	*/
	
	/**
	 * do add trigger
	 * @param e
	 */
	private void addSchema(Object e){
		IClass cls = ontology.getClass(OntologyHelper.SCHEMAS);
		String suggestion = ""+e;
		if(!suggestion.endsWith(OntologyHelper.SCHEMA))
			suggestion = suggestion+OntologyHelper.SCHEMA;
		String name = OntologyHelper.getClassName(suggestion);
		IClass c = cls.createSubClass(name);
		templateMap.put(c,new ConceptExpression [2]);
		final ConceptEntry entry = new ConceptEntry(c);
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				((DefaultListModel)schemas.getModel()).addElement(entry);
				schemas.setSelectedValue(entry,true);
			}
		});
		sync.addOntologyAction(new OntologyAction(){
			public void run(){}
			public void undo(){
				((DefaultListModel)schemas.getModel()).removeElement(entry);
				entry.getConceptClass().delete();
				templateMap.remove(entry.getConceptClass());
			}
		});
		
	}
	
	/**
	 * do add trigger
	 * @param e
	 */
	private void removeSchema(Object e){
		if(e instanceof ConceptEntry){
			final ConceptEntry entry = (ConceptEntry) e;
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					((DefaultListModel)schemas.getModel()).removeElement(entry);
				}
			});
			//entry.getConceptClass().delete();
			templateMap.remove(entry.getConceptClass());
			setEditableReport(false);
			sync.addOntologyAction(new OntologyAction(){
				public void run(){
					entry.getConceptClass().delete();
				}
				public void undo(){
					((DefaultListModel)schemas.getModel()).addElement(entry);
					templateMap.put(entry.getConceptClass(),new ConceptExpression [2]);
				}
			});
		}
	}
	
	
	/**
	 * do add trigger
	 * @param e
	 */
	private void addFinding(ConceptEntry e){
		ReportEntryWidget r = new ReportEntryWidget(e);
		r.addPropertyChangeListener(this);
		r.setReadOnly(readOnly);
		r.setShowSummary(showSummary);
		r.load();
		r.setOrderNumber(goalPanel.getComponentCount());
		goalPanel.add(r,goalPanel.getComponentCount()-1);
		goalPanel.revalidate();
		modified = true;
	}
	
	/**
	 * do add trigger
	 * @param e
	 */
	private void removeFinding(ConceptEntry e){
		ReportEntryWidget r = findReportEntry(e);
		if(r != null){
			r.removePropertyChangeListener(this);
			goalPanel.remove(r);
			goalPanel.revalidate();
		}
	}
	
	/**
	 * resync display based on options
	 */
	private void doChangeSummaryOption(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				for(int i=0;i<goalPanel.getComponentCount();i++){
					if(goalPanel.getComponent(i) instanceof ReportEntryWidget){
						ReportEntryWidget r = (ReportEntryWidget) goalPanel.getComponent(i);
						r.setShowSummary(showSummary);
						r.load();
					}
				}
				goalPanel.revalidate();
				goalPanel.repaint();
			}
		});
		
	}
	
	/**
	 * sync findings with report builder tool
	 * @param cls
	 */
	private void syncFindings(IClass cls){
		if(!templateMap.containsKey(cls))
			return;
		templateMap.get(cls)[FINDINGS] = new ConceptExpression();
		for(int i=0;i<goalPanel.getComponentCount();i++){
			if(goalPanel.getComponent(i) instanceof ReportEntryWidget){
				ReportEntryWidget w = (ReportEntryWidget) goalPanel.getComponent(i);
				templateMap.get(cls)[FINDINGS].add(w.getConceptEntry());
			}
		}
	}
	
	/**
	 * sync findings with report builder tool
	 * @param cls
	 */
	private void syncTriggers(IClass cls){
		if(!templateMap.containsKey(cls))
			return;
		templateMap.get(cls)[TRIGGERS] = triggerEditor.getConceptExpression();
	}
	
	public void syncFindings(){
		if(currentSchema != null)
			syncFindings(currentSchema);
	}
	
	public void syncTriggers(){
		if(currentSchema != null)
			syncTriggers(currentSchema);
	}
	
	
	/**
	 * find report entry widget
	 * @param e
	 * @return
	 */
	private ReportEntryWidget findReportEntry(ConceptEntry e){
		for(int i=0;i<goalPanel.getComponentCount();i++){
			if(goalPanel.getComponent(i) instanceof ReportEntryWidget){
				ReportEntryWidget w = (ReportEntryWidget) goalPanel.getComponent(i);
				if(w.getConceptEntry().equals(e)){
					return w;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * is editable report
	 * @param b
	 */
	private void setEditableReport(boolean b){
		if(!readOnly){
			setEnableComponent(getAddFindingPanel(),b);
			setEnableComponent(getTriggerPanel(), b);
			addFinding.setEnabled(b);
		}
		if(!b){
			schemas.clearSelection();
			currentSchema = null;
		}
	}
	
	private void setEnableComponent(JComponent c, boolean b){
		c.setEnabled(b);
		for(int i=0;i<c.getComponentCount();i++){
			if(c.getComponent(i) instanceof JComponent){
				setEnableComponent((JComponent) c.getComponent(i),b);
			}
		}
	}
	
	/**
	 * load schema name
	 * @param name
	 */

	private void loadSchemas(){
		templateMap.clear();
		for(IClass cls : getTemplates()){
			((DefaultListModel)schemas.getModel()).addElement(new ConceptEntry(cls));
		}
		schemas.revalidate();
	}
	
	/**
	 * load schema name
	 * @param name
	 */

	private void loadSchema(String name){
		final String nm = name;
		(new Thread(new Runnable(){
			public void run(){
				DomainBuilder.getInstance().getProgressBar().setString("Loading Schema ...");
				DomainBuilder.getInstance().setBusy(true);
				// sync previous schema
				if(currentSchema != null){
					syncFindings(currentSchema);
					syncTriggers(currentSchema);
				}
				
				// clear goal panel
				goalPanel.removeAll();
				goalPanel.add(getAddFindingPanel());
				goalPanel.revalidate();
				// clear trigger panel
				//((DefaultListModel)triggers.getModel()).removeAllElements();
				IClass cls = ontology.getClass(nm);
				currentSchema = cls;
				if(cls != null){
					setEditableReport(true);
					//long time = System.currentTimeMillis();
					// now load stuff associated with it
					// add triggers
					/*
					for(IClass c : getTriggers(cls)){
						addTrigger(new ConceptEntry(c));
					}
					*/
					triggerEditor.setConceptExpression(getTriggers(cls));
					
					// add goals
					//for(IClass c : getFindings(cls)){
					//	addFinding(new ConceptEntry(c));
					//}
					for(Object c: getFindings(cls)){
						if(c instanceof ConceptEntry)
							addFinding((ConceptEntry) c);
					}
					//System.out.println("load schema "+(System.currentTimeMillis()-time));
				}
				DomainBuilder.getInstance().setBusy(false);
				modified = false;
			}
		})).start();
	}
	
	public void componentAdded(ContainerEvent e) {
		if(e.getChild() instanceof ReportEntryWidget){
			((ReportEntryWidget)e.getChild()).addPropertyChangeListener(this);
		}
	}

	public void componentRemoved(ContainerEvent e) {
		if(e.getChild() instanceof ReportEntryWidget){
			((ReportEntryWidget)e.getChild()).removePropertyChangeListener(this);
		}
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		String cmd = evt.getPropertyName();
		if(cmd.equals("REMOVE")){
			//ConceptEntry e = (ConceptEntry) evt.getNewValue();
			getToolBar().getComponentAtIndex(0).setEnabled(true);
			//goalPanel.revalidate();
			modified = true;
		}else if(cmd.equals("UP") || cmd.equals("DOWN") || cmd.equals("EDIT") || cmd.equals("EDIT_TRIGGER")){
			//ConceptEntry e = (ConceptEntry) evt.getNewValue();
			//getToolBar().getComponentAtIndex(0).setEnabled(true);
			modified = true;
		}
	}
	
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("Add Template")){
			doAddSchema();
		}else if(cmd.equals("Remove Template")){
			doRemoveSchema();
		}else if(cmd.equals("Import")){
			doImport();
		}else if(cmd.equals("Export")){
			doExport();
		}else if(cmd.equals("Add Trigger")){
			if(triggerEditor.isEnabled())
				triggerEditor.doAddTrigger();
		}else if(cmd.equals("Remove Trigger")){
			if(triggerEditor.isEnabled())
				triggerEditor.doDelete(true);
		}else if(cmd.equals("Add Reportable Items")){
			doAddFinding();
		}else if(cmd.equals("Edit")){
			doEditTemplate();
		}else if(cmd.equals("Save")){
			doSave();
		}else if(cmd.equals("Publish")){
			doPublish();
		}else if(cmd.equals("Undo")){
			doUndo();
		}else if(cmd.equals("Properties")){
			doProperties();
		}else if(cmd.equals("Show Reportable Item Summary")){
			showSummary = ((AbstractButton)e.getSource()).isSelected();
			doChangeSummaryOption();
		}
	}
	
	/**
	 * create menubar
	 */
	public JMenuBar getMenuBar() {
		if(menubar == null){
			menubar = new JMenuBar();
			// file
			JMenu file = new JMenu("File");
			if(Communicator.isConnected()){
				JMenuItem item = UIHelper.createMenuItem("Save","Save and Publish Domain Knowledge Base",Icons.PUBLISH,this);
				item.setActionCommand("Publish");
				file.add(item);
			}else
				file.add(UIHelper.createMenuItem("Save","Save to Knowledge Base",Icons.SAVE,this));
			file.add(UIHelper.createMenuItem("Import","Import Template from caDSR",Icons.IMPORT,this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Properties","Edit Knowledge Base Properties",Icons.PROPERTIES,this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Exit","Exit Domain Builder",null,DomainBuilder.getInstance()));
			
			// edit
			final JMenu edit = new JMenu("Edit");
			edit.add(UIHelper.createMenuItem("Undo","Undo",Icons.UNDO,this));
			edit.addMenuListener(new MenuListener(){
				public void menuCanceled(MenuEvent e) {}
				public void menuDeselected(MenuEvent e) {}
				public void menuSelected(MenuEvent e) {
					edit.getItem(0).setEnabled(sync.hasActions());
				}
			});	
			
			JMenu builder = new JMenu("Builder");
			builder.add(UIHelper.createMenuItem("Add Template","Add Report Template",Icons.PLUS,this));
			builder.add(UIHelper.createMenuItem("Remove Template","Remove Report Template",Icons.MINUS,this));
			builder.addSeparator();
			builder.add(UIHelper.createMenuItem("Add Trigger","Add Template Trigger",Icons.PLUS,this));
			builder.add(UIHelper.createMenuItem("Remove Trigger","Remove Template Trigger",Icons.MINUS,this));
			builder.addSeparator();
			builder.add(UIHelper.createMenuItem("Add Findings","Add Reportable Findings",Icons.ADD,this));
			
			JMenu options = new JMenu("Options");
			options.add(UIHelper.createCheckboxMenuItem("Show Reportable Item Summary","Show summary for each reportable item",null,true,this));
			
			
			JMenu tools = new JMenu("Tools");
			tools.add(UIHelper.createMenuItem("Domain Manager","Domain Manager",Icons.ONTOLOGY,DomainBuilder.getInstance()));
			
			
			
			// help
			JMenu help = new JMenu("Help");
			help.add(UIHelper.createMenuItem("Help","DomainBuilder Manual",Icons.HELP,DomainBuilder.getInstance()));
			help.add(UIHelper.createMenuItem("About","About DomainBuilder",Icons.ABOUT,DomainBuilder.getInstance()));
			
			menubar.add(file);
			menubar.add(edit);
			menubar.add(builder);
			menubar.add(tools);
			menubar.add(options);
			menubar.add(help);
		}
		return menubar;
	}
	
	
	
	/**
	 * value changed
	 */
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			loadSchema(""+schemas.getSelectedValue());
		}
	}
	
	/**
	 * get all templates
	 * @return
	 */
	public Collection<IClass> getTemplates(){
		IClass schemaCls = ontology.getClass(OntologyHelper.SCHEMAS);
		for(IClass cls : schemaCls.getDirectSubClasses()){
			if(!templateMap.containsKey(cls)){
				templateMap.put(cls,new ConceptExpression [2]);
			}
		}
		return new ArrayList(templateMap.keySet());
	}

	
	/**
	 * get schema parameters
	 * @param schema
	 * @param property
	 * @param offset
	 * @return
	 */
	private ConceptExpression getParameters(IClass schema, String property, int offset){
		if(!templateMap.containsKey(schema))
			return new ConceptExpression();
		ConceptExpression [] templateSpec = templateMap.get(schema); 
		//System.out.println(schema+" "+property+" "+templateSpec[offset]);
		if(templateSpec[offset] == null || templateSpec[offset].isEmpty()){
			templateSpec[offset] = new ConceptExpression();
			IProperty prop = ontology.getProperty(property);
			
			/*
			for(IClass cls: OntologyHelper.getPropetyValues(schema.
				getNecessaryRestrictions(), prop,new LinkedHashSet<IClass>())){
				templateSpec[offset].add(cls);
			}
			*/
			
			// pull info from expressions
			for(IRestriction r: schema.getRestrictions(prop)){
				ConceptExpression exp = ConceptExpression.toConceptExpression(r.getParameter());
				exp.setHorizontalIcon(true);
				if(exp.isSingleton()){
					templateSpec[offset].add(exp.getOperand());
				}else{
					templateSpec[offset].add(exp);
				}
			}
			
			// re-order the list
			Collections.sort(templateSpec[offset],OntologyHelper.getOrderComparator(schema));
			
		}
		return templateSpec[offset];
	}
	
	/**
	 * get all triggers
	 * @return
	 */
	public ConceptExpression getTriggers(IClass schema){
		return getParameters(schema,OntologyHelper.HAS_TRIGGER,TRIGGERS);
	}
	
	/**
	 * get all findings
	 * @return
	 */
	public ConceptExpression getFindings(IClass schema){
		return getParameters(schema,OntologyHelper.HAS_PROGNOSTIC,FINDINGS);
	}
	

	/**
	 * is this case base read-only
	 * @param b
	 */
	private void setReadOnly(boolean b){
		readOnly = b;
		String [] exceptions = new String []
		  {"properties","exit","tools","help"};
		
		// disable toolbar buttons
		UIHelper.setEnabled(getToolBar(),exceptions,!b);
		UIHelper.setEnabled(getMenuBar(), exceptions, !b);
		UIHelper.setEnabled(getSchemaToolBar("Templates"),exceptions,!b);
		UIHelper.setEnabled(getTriggerPanel(),exceptions,!b);
		
	}
}
