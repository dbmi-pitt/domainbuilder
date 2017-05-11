package edu.pitt.dbmi.domainbuilder.knowledge;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.dbmi.domainbuilder.widgets.*;
import edu.pitt.dbmi.domainbuilder.widgets.TreePanel.TextViewNode;
import edu.pitt.ontology.*;
import edu.pitt.ontology.bioportal.BioPortalRepository;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.ontology.ui.OntologyImporter;
import edu.pitt.ontology.ui.QueryTool;
import edu.pitt.terminology.Terminology;
import edu.pitt.terminology.lexicon.Concept;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;

/**
 * create a tree representation of knowledge
 * @author tseytlin
 *
 */
public class KnowledgeTree extends JPanel implements ActionListener, TreeSelectionListener, PropertyChangeListener {
	private TreePanel tree;
	private IOntology ontology;
	private Terminology terminology;
	private OntologySynchronizer synchronizer; 
	
	// info panel fields
	private JTextComponent name,cui,definition;
	private UIHelper.DynamicTable synonyms;
	private ArrayList<Component> infoList;
	private QueryTool query;
	private JList  examples;
	private JComboBox power;
	private DomainSelectorPanel selector;
	private JMenuBar menubar;
	private JToolBar toolbar;
	// temp fields
	private TreePanel.TextViewNode currentNode;
	private boolean readOnly,organizeDiagnoses;
	private File lastFile;
	
	public KnowledgeTree(){
		super();
		setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setLeftComponent(getTreePanel());
		split.setRightComponent(getInfoPanel());
		split.setResizeWeight(1);
		split.setDividerLocation(getSize().width-150);
		add(split,BorderLayout.CENTER);
		
		// add tree selection event
		tree.addTreeSelectionListener(this);
		tree.addPropertyChangeListener(this);
		
		setInfoPanelEnabled(false);
		
		synchronizer = OntologySynchronizer.getInstance();
	}
	
	private JComponent getInfoPanel(){
		return createInfoPanel();
	}
	
	private JComponent getTreePanel(){
		if(tree == null){
			tree = new TreePanel(){
				public void doUndo(){
					KnowledgeTree.this.doUndo();
				}
			};
			tree.setEditable(true);
			tree.setColorMode(TreePanel.DEFINITION_MODE);
			tree.getToolBar().add(getToolBar(),0);
			
			// init lexicon
			/*
			lexicon = new JList(new DefaultListModel());
			lexicon.addListSelectionListener(this);
			JScrollPane scroll = new JScrollPane(lexicon);
			scroll.setBorder(new TitledBorder("Lexicon"));
			scroll.setBackground(Color.white);
			tree.add(scroll,BorderLayout.WEST);
			*/
			
		}
		return tree;
	}
	
	/**
	 * create toolbar
	 * @return
	 */
	private JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new JToolBar();
			toolbar.add(UIHelper.createButton("new","Create New Knowledge Base",Icons.NEW,this));
			toolbar.add(UIHelper.createButton("open","Open Knowledge Base",Icons.OPEN,this));
			if(Communicator.isConnected())
				toolbar.add(UIHelper.createButton("publish","Save and Publish Knowledge Base",Icons.PUBLISH,this));
			else
				toolbar.add(UIHelper.createButton("save","Save Knowledge Base",Icons.SAVE,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("import","Import From BioPortal or OWL File",Icons.IMPORT,this));
			toolbar.add(UIHelper.createButton("export","Export to OWL File",Icons.EXPORT,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("organize","Organize Hierarchy",Icons.HIERARCHY,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("delete","Remove Selected Findings",Icons.DELETE,this));
			toolbar.add(UIHelper.createButton("cut","Cut Selected Findings",Icons.CUT,this));
			toolbar.add(UIHelper.createButton("copy","Copy Selected Findings",Icons.COPY,this));
			toolbar.add(UIHelper.createButton("paste","Paste Findings into Selection",Icons.PASTE,this));
			toolbar.addSeparator();
		}
		return toolbar;
	}
	

	/**
	 * load lexicon concepts
	 * @param parent
	 *
	private void loadLexicon(IClass parent){
		IClass [] children = parent.getDirectSubClasses();
		Arrays.sort(children,new Comparator<IClass>(){
			public int compare(IClass a, IClass b) {
				return a.getName().compareTo(b.getName());
			}
			
		});
		for(IClass cls: children){
			((DefaultListModel) lexicon.getModel()).addElement(new ConceptEntry(cls));
		}
		lexicon.revalidate();
	}
	*/
	
	/**
	 * @return the ontology
	 */
	public IOntology getOntology() {
		return ontology;
	}

	/**
	 * @param ontology the ontology to set
	 */
	public void setOntology(IOntology ont) {
		this.ontology = ont;
		// load if showing
		if(isShowing())
			load();
	}
	
	/**
	 * load knowledge tree
	 */
	private void loadKnowledgeTree(){
		DomainBuilder.getInstance().setBusy(true);
		DomainBuilder.getInstance().getProgressBar().setString("Loading Knowledge Hierarchy ...");
		DomainBuilder.getInstance().getProgressBar().setIndeterminate(true);
		
		while(ontology == null){
			UIHelper.sleep(200);
		}
		
		tree.setRoot(ontology.getClass(OntologyHelper.CONCEPTS));
		setInfoPanelEnabled(false);
	}
	
	
	
	/**
	 * load tree
	 */
	public void load(){
		// set root
		//tree.setBusy(true);
		(new Thread(new Runnable(){
			public void run(){
				System.out.println("loading ontology "+ontology+" ...");
				
				// load knowledge tree
				loadKnowledgeTree();
				
				//tree.setBusy(false);
				//DomainBuilder.getInstance().setBusy(false);
				//getToolBar().getComponentAtIndex(2).setEnabled(false);
				KnowledgeAuthor.setOntologyInfo(ontology);
				
				// reset read only flag
				setReadOnly(OntologyHelper.isReadOnly(ontology));
				
				// check for suggested tersm
				if(!readOnly)
					doCheckSuggestedTerms();
			}
		})).start();	
	}
	
	
	/**
	 * invoked when tab is no longer visible, perhaps save the ontology or something
	 */
	public void unselected(){
		if(readOnly)
			return;
		
		if(synchronizer.hasActions()){
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
	 * this component is selected
	 */
	public void selected(){
		if(tree.isDirty()){
			(new Thread(new Runnable(){
				public void run(){
					DomainBuilder.getInstance().setBusy(true);
					reloadTree();
					DomainBuilder.getInstance().setBusy(false);
				}
			})).start();
		}
	}
	
	
	/**
	 * reload tree
	 */
	private void reloadTree(){
		tree.reloadTree();
		currentNode = null;
		setInfoPanelEnabled(false);
	}
	
	/**
	 * create example buttons
	 * @return
	 */
	private JComponent createExampleButtons(){
		JToolBar panel = new JToolBar();
		panel.setFloatable(false);
		panel.add(new JLabel("Example Picture"));
		panel.add(Box.createHorizontalGlue());
		JButton p = UIHelper.createButton("add_example","add example image",Icons.PLUS,16,this);
		JButton m = UIHelper.createButton("remove_example","remove example image",Icons.MINUS,16,this);
		p.setPreferredSize(new Dimension(30,20));
		m.setPreferredSize(p.getPreferredSize());
		panel.add(p);
		panel.add(m);
		return panel;
	}
	
	/**
	 * create example buttons
	 * @return
	 */
	private JComponent createSynonymButtons(){
		JToolBar panel = new JToolBar();
		panel.setFloatable(false);
		panel.add(new JLabel("Synonyms"));
		panel.add(Box.createHorizontalGlue());
		JButton p = UIHelper.createButton("Add Row","Add Row",Icons.PLUS,16,synonyms);
		JButton m = UIHelper.createButton("Remove Row","Remove Row",Icons.MINUS,16,synonyms);
		p.setPreferredSize(new Dimension(30,20));
		m.setPreferredSize(p.getPreferredSize());
		panel.add(p);
		panel.add(m);
		return panel;
	}
	
	
	/**
	 * enable/disable info panel
	 * @param b
	 */
	private void setInfoPanelEnabled(boolean b){
		if(!b){
			name.setText("");
			cui.setText("");
			definition.setText("");
			synonyms.clear();
			power.setSelectedItem("");
		}
		
		definition.setEditable(b);
		for(Component c: infoList)
			c.setEnabled(b);
		definition.setBackground((b)?Color.white:name.getBackground());
		synonyms.setBackground((b)?Color.white:name.getBackground());
		examples.setBackground((b)?Color.white:name.getBackground());
		power.setEnabled(b);
	}
	
	/**
	 * create information panel
	 * @return
	 */
	private JPanel createInfoPanel(){
		infoList = new ArrayList<Component>();
		JPanel panel = new JPanel();
		//panel.setBackground(Color.white);
		//panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,0,
		GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,new Insets(2,2,2,2),0,0);
		
		// name
		name = new JTextField();
		name.setEditable(false);
		//name.setBorder(new TitledBorder("Name"));
		panel.add(new JLabel("Name"),c);
		c.gridy++;
		panel.add(name,c);
		c.gridy++;
		
		// lookup button
		JButton lookup = new JButton("Import from Terminology");
		lookup.setActionCommand("lookup");
		lookup.addActionListener(this);
		panel.add(lookup,c);
		c.gridy++;
		infoList.add(lookup);
		
		// cuid
		cui = new JTextField();
		cui.setEditable(false);
		//cui.setBorder(new TitledBorder("Concept Code"));
		panel.add(new JLabel("Concept Code"),c);
		c.gridy++;
		panel.add(cui,c);
		c.gridy++;
		
		// definition 
		definition = new JTextArea(6,20);
		//definition.setPreferredSize(new Dimension(200,100));
		((JTextArea)definition).setLineWrap(true);
		JScrollPane scroll = new JScrollPane(definition);
		scroll.setMinimumSize(new Dimension(200,100));
		//scroll.setBackground(Color.white);
		
		panel.add(new JLabel("Definition"),c);
		c.gridy++;
		panel.add(scroll,c);
		c.gridy++;
		
		//synonyms
		synonyms = new UIHelper.DynamicTable(5,1);
		scroll = new JScrollPane(synonyms);
		//scroll.getViewport().setBackground(Color.white);
		scroll.setMinimumSize(new Dimension(200,100));
		//scroll.setBorder(new TitledBorder("Synonyms"));
		JComponent bts = createSynonymButtons();
		panel.add(bts,c);
		c.gridy++;
		panel.add(scroll,c);
		c.gridy++;
		for(Component comp: bts.getComponents())
			if(comp instanceof JButton)
				infoList.add(comp);
		
		// add power
		power = new JComboBox(new String[] {"",POWER_LOW,POWER_MEDIUM,POWER_HIGH});
		panel.add(new JLabel("Observable Power"),c);
		c.gridy++;
		panel.add(power,c);
		c.gridy++;
		
		// example picture
		examples = new JList(new DefaultListModel());
		examples.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					Object obj = examples.getSelectedValue();
					if(obj != null){
						if(obj instanceof ExampleImage){
							ExampleImage img = (ExampleImage) obj;
							JOptionPane.showMessageDialog(getFrame(),
							img.getImage(),"Example",JOptionPane.PLAIN_MESSAGE);
						}
					}
				}
			}
		});
		scroll = new JScrollPane(examples);
		scroll.setPreferredSize(new Dimension(200,150));
		scroll.setMinimumSize(scroll.getPreferredSize());
		scroll.setMaximumSize(scroll.getPreferredSize());
		//scroll.setBackground(Color.white);
		//scroll.setBorder(new TitledBorder("Example Pictures"));
		bts = createExampleButtons();
		panel.add(bts,c);
		c.gridy++;
		panel.add(scroll,c);
		c.gridy++;
		for(Component comp: bts.getComponents())
			if(comp instanceof JButton)
				infoList.add(comp);
		// separator
		c.weighty = 1;
		JPanel o = new JPanel();
		o.setOpaque(false);
		panel.add(o,c);
		c.gridy++;
		c.weighty = 0;	
		
		infoList.add(synonyms);
		infoList.add(examples);
					
		return panel;
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand().toLowerCase();
		if(cmd.equals("lookup")){
			doLookup();
		}else if(cmd.equals("new")){
			doNew();
		}else if(cmd.equals("open")){
			doOpen();
		}else if(cmd.equals("save")){
			doSave();
		}else if(cmd.equals("publish")){
			doPublish();
		}else if(cmd.equals("import")){
			doImport();
		}else if(cmd.equals("export")){
			doExport();
		}else if(cmd.equals("undo")){
			doUndo();
		}else if(cmd.equals("delete")){
			tree.doDelete();
		}else if(cmd.equals("cut")){
			tree.doCut();
		}else if(cmd.equals("copy")){
			tree.doCopy();
		}else if(cmd.equals("paste")){
			tree.doPaste();
		}else if(cmd.equals("add_example")){
			doAddExample();
		}else if(cmd.equals("remove_example")){
			doRemoveExamples();
		}else if(cmd.equals("add concept")){
			doAddConcept();
		}else if(cmd.equals("edit concept")){
			tree.doEdit();
		}else if(cmd.equals("organize")){
			doOrganize();
		}else if(cmd.equals("properties")){
			doProperties();
		}else if(cmd.equals("refresh hierarchy")){
			loadKnowledgeTree();
		}else if(cmd.equals("organize diagnoses")){
			organizeDiagnoses = ((AbstractButton)e.getSource()).isSelected();
		}
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
	
	
	private JFrame getFrame(){
		return DomainBuilder.getInstance().getFrame();
	}
	
	/**
	 * create new case
	 */
	private void doNew(){
		if(selector == null){
			selector = new DomainSelectorPanel(ontology.getRepository());
			selector.setOwner(getFrame());
		}
		// prompt for new domain
		if(selector.showNewDomainDialog()){
			while(!selector.isSelected()){
				UIHelper.sleep(250);
			}
			doCheckSave();
			//reset();
			(new Thread(new Runnable(){
				public void run(){
					// wait while saving is done
					while(KnowledgeAuthor.isSaving()){
						UIHelper.sleep(200);
					}
					
					IOntology ont = (IOntology) selector.getSelectedObject();
					DomainBuilder.getInstance().doLoad(ont.getURI());
				}
			})).start();
		}
	}
	/**
	 * create new case
	 */
	private void doOpen(){
		if(selector == null){
			selector = new DomainSelectorPanel(ontology.getRepository());
			selector.setOwner(getFrame());
		}
		selector.showChooserDialog();
		if(selector.isSelected()){
			doCheckSave();
			//reset();
			(new Thread(new Runnable(){
				public void run(){
					// wait while saving is done
					while(KnowledgeAuthor.isSaving()){
						UIHelper.sleep(200);
					}
					
					IOntology ont = (IOntology) selector.getSelectedObject();
					DomainBuilder.getInstance().doLoad(ont.getURI());
				}
			})).start();
		}
	}
	
	
	/**
	 * create new case
	 */
	private void doSave(){
		if(readOnly)
			return;
		
		(new Thread(new Runnable(){
			public void run(){
				setReadOnly(false);
				saveConceptInfo(currentNode);
				KnowledgeAuthor.saveOntology(ontology);
				reloadTree();
				// reset read only flag
				setReadOnly(OntologyHelper.isReadOnly(ontology));
			}
		})).start();
		
	}
	
	/**
	 * 
	 * @return
	 */
	private JComponent createImportPanel(ButtonGroup group,IClass cls){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
		JPanel p1 = new JPanel();
		p1.setLayout(new BorderLayout());
		p1.setBorder(new TitledBorder("Import Source"));
		
		JRadioButton b1 = new JRadioButton("BioPortal Repository",true);
		b1.setActionCommand("bioportal");
		JRadioButton b2 = new JRadioButton("Local Repository");
		b2.setActionCommand("local");
		JRadioButton b3 = new JRadioButton("Local OWL File");
		b3.setActionCommand("file");
		p1.add(b1,BorderLayout.NORTH);
		p1.add(b2,BorderLayout.CENTER);
		p1.add(b3,BorderLayout.SOUTH);
		group.add(b1);
		group.add(b2);
		group.add(b3);
		
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		p2.setBorder(new TitledBorder("Import Destination"));
		
		JLabel lbl = new JLabel(new ConceptEntry(cls));
		lbl.setBackground(Color.white);
		p2.add(lbl,BorderLayout.CENTER);
				
		panel.add(p1);
		panel.add(p2);
		
		return panel;
	}
	
	
	/**
	 * create new case
	 */
	private void doImport(){
		//JOptionPane.showMessageDialog(getFrame(),"Not Implemented!");
		(new Thread(new Runnable(){
			public void run(){
				TreePanel.TextViewNode node = tree.getSelectedNode();
				final IClass target = (node != null)?node.getConceptClass():ontology.getClass(OntologyHelper.CONCEPTS);
				
				// create a selector panel that gives you a choice to what we can import
				ButtonGroup group = new ButtonGroup();
				int r = JOptionPane.showConfirmDialog(null,createImportPanel(group,target),"Import",
						JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,Icons.getIcon(Icons.IMPORT,128));
				if(r == JOptionPane.YES_OPTION){
					String cmd = group.getSelection().getActionCommand();
					IRepository repository = null;
					// prompt for file
					if(cmd.equals("file")){
						JFileChooser jf = new JFileChooser();
						jf.setFileFilter(new FileFilter(){
							public boolean accept(File f) {
								return f.isDirectory() || f.getName().endsWith(OntologyHelper.OWL_SUFFIX);
							}
							public String getDescription() {
								return "OWL Files (*"+OntologyHelper.OWL_SUFFIX+")";
							}
						});
						if(JFileChooser.APPROVE_OPTION == jf.showOpenDialog(getFrame())){
							File file = jf.getSelectedFile();
							// import into local repository first
							try{
								DomainBuilder b = DomainBuilder.getInstance();
								b.setBusy(true);
								b.getProgressBar().setString("Loading OWL File "+file.getName()+" ...");
								//IOntology ont = repository.createOntology(file.toURI());
								//WTF??? Why was I creating an ontology?????
								IOntology ont = POntology.loadOntology(file);
								//repository.addOntology(ont);
								repository = ont.getRepository();
								b.setBusy(false);
							}catch(IOntologyException ex){
								JOptionPane.showMessageDialog(getFrame(),"Problem loading on "+file.getName()+" ontology file",
										"Error",JOptionPane.ERROR_MESSAGE);
								ex.printStackTrace();
								return;
							}
						}else
							return;
						
					}else if(cmd.equals("local")){
						repository = ontology.getRepository();
					}else if(cmd.equals("bioportal")){
						repository = new BioPortalRepository();
					}
					
					// THIS SHOULD NEVER HAPPEN
					if(repository == null)
						return;
					
					final OntologyImporter importer = new OntologyImporter(repository);
					importer.setDeepCopy(true);
					importer.setPreLoad(false);
					importer.addPropertyChangeListener(KnowledgeTree.this);
					JDialog d = importer.showImportWizard(getFrame());
					while(d.isShowing()){
						try{
							Thread.sleep(500);
						}catch(Exception ex){}
					}
					if(importer.isSelected()){
						//IOntology source = importer.getSelectedOntology();
						(new Thread(new Runnable(){
							public void run(){
								IClass [] scls = importer.getSelectedClasses();
								DomainBuilder b = DomainBuilder.getInstance();
								b.setBusy(true);
								importer.copyClasses(scls,target);
								importer.copyValues(importer.getSelectedOntology(),ontology);
								reloadTree();
								importer.removePropertyChangeListener(KnowledgeTree.this);
								b.setBusy(false);
							}
						})).start();
					}
				}
			}
		})).start();
		
	}
	
	private void doPublish(){
		if(readOnly)
			return;
		
		(new Thread(new Runnable(){
			public void run(){
				
				// do regular save
				setReadOnly(false);
				saveConceptInfo(currentNode);
				KnowledgeAuthor.saveOntology(ontology);
				reloadTree();
							
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
	 * add example
	 */
	private void doAddExample(){
		JFileChooser chooser = new JFileChooser(OntologyHelper.getDefaultImageFolder());
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				String n = f.getName().toLowerCase();
				return f.isDirectory() || n.endsWith(".jpg") ||
						n.endsWith(".gif") || n.endsWith(".png") || n.endsWith("jpeg");
			}
			public String getDescription() {
				return "Image Files (JPG, GIF, PNG)";
			}
			
		});
		if(chooser.showOpenDialog(getFrame()) == JFileChooser.APPROVE_OPTION){
			File f = chooser.getSelectedFile();
			if(f.exists()){
				// create a copy of this file in better location
				ExampleImage img = new ExampleImage(OntologyHelper.saveExampleImage(f,currentNode.getConceptClass()));
				((DefaultListModel)examples.getModel()).addElement(img);
			}
			examples.revalidate();
			examples.repaint();
		}
	}
	
	
	/**
	 * remove examples
	 */
	private void doRemoveExamples(){
		int [] n = examples.getSelectedIndices();
		for(int i=0;i<n.length;i++){
			OntologyHelper.removeExampleImage((ExampleImage)examples.getModel().getElementAt(n[i]));
			((DefaultListModel)examples.getModel()).removeElementAt(n[i]);
		}
		examples.revalidate();
		examples.repaint();
	}
	
	/**
	 * perform lookup
	 *
	 */
	private void doLookup(){
		// get default terminology
		if(terminology == null ){
			terminology = DomainBuilder.getRepository().getTerminology(OntologyHelper.REMOTE_TERMINOLOGY);
			//terminology = DomainBuilder.getRepository().getTerminologies()[0];
		}
		
		// swtich terminology
		Terminology term = terminology;
		
		// if something is anatomic location use anatomical site terminology
		if(currentNode != null && OntologyHelper.isAnatomicLocation(currentNode.getConceptClass())){
			term = OntologyHelper.getAnatomicTerminology();
		}
		
		
		if(term == null){
			JOptionPane.showMessageDialog(this,"Could not find terminology ",
					"Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		// prompt terminology
		if(name.getText() != null){
			// init query tool
			if(query == null){
				query = new QueryTool();
			}
			query.setTerminology(term);
			
			// set search term
			query.setSearchTerm(name.getText());
			query.setBusy(true);
			(new Thread(new Runnable(){
				public void run(){
					query.doSearch();
					query.setBusy(false);
				}
			})).start();
			int r = JOptionPane.showConfirmDialog(this,query,"Lookup in Terminology",
					JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
			if(r == JOptionPane.OK_OPTION){
				Concept c = query.getSelectedConcept();
				setConceptInfo(c);
			}
		}
	}
	
	/**
	 * undo last action
	 */
	private void doUndo(){
		synchronizer.undo();
	}
	
	
	/**
	 * suggest a term for concept
	 * @param concept
	 * @param text
	 */
	public void doSuggestTerm(String concept, String text){
		IClass cls = ontology.getClass(concept);
		if(cls != null){
			OntologyHelper.getConceptHandler(ontology).addConceptSynonym(cls,text);
			OntologyHelper.getConceptHandler(ontology).analyzeConceptClass(cls);
			OntologyHelper.setDirty();
		}
	}
	
	
	/**
	 * select concept in a tree
	 * @param c
	 */
	public void doSelectConcept(String c){
		tree.setSelectedNode(c);
	}
	
	
	/**
	 * fill in content from class
	 */
	private void setConceptInfo(TreePanel.TextViewNode node){
		// save privous concept info
		saveConceptInfo(currentNode);
		
		if(OntologyHelper.isSystemClass(node.getConceptClass())){
			setInfoPanelEnabled(false);
			currentNode = null;
			
			return;
		}
		setInfoPanelEnabled(true);
		
		// save node
		currentNode = node;
		
		// add name 
		name.setText(UIHelper.getPrettyClassName(node.toString()));
		
		// add code
		cui.setText(node.getCode());
	
		// add definition
		definition.setText(node.getDefinition());
		
		// add synonyms
		synonyms.clear();
		String [] s = node.getSynonyms();
		if(s.length > 0){
			for(int i=0;i<s.length;i++){
				synonyms.setValueAt(s[i],i,0);
			}
		}else
			synonyms.setValueAt(name.getText().toLowerCase(),0,0);
		synonyms.repaint();
		
		
		
		// add examples
		((DefaultListModel)examples.getModel()).removeAllElements();
		for(Object o: currentNode.getExampleImages())
			((DefaultListModel)examples.getModel()).addElement(o);
		examples.validate();
		
		
		// save power
		IClass c = node.getConceptClass();
		if(c!= null && OntologyHelper.isFeature(c)){
			power.setEnabled(true);
			String pow = node.getPower();
			if(TextHelper.isEmpty(pow)){
				if(OntologyHelper.isArchitecturalFeature(c))
					pow = OntologyHelper.POWER_LOW;
				else if(OntologyHelper.isCytologicFeature(c))
					pow = OntologyHelper.POWER_HIGH;
			}
			power.setSelectedItem(pow);
		}else{
			power.setSelectedItem("");
			power.setEnabled(false);
		}
		
		// save hash
		node.setInfoHash(getInfoHash());
	}
	
	/**
	 * calculate hash code for components
	 * @return
	 */
	private int getInfoHash(){
		//return name.getText().hashCode()+definition.getText().hashCode()+synonyms.getValues(0).hashCode()+
		//		((DefaultListModel)examples.getModel()).elements().hashCode()+power.getSelectedItem().hashCode();
		int x = 0;
		for(int i=0;i<examples.getModel().getSize();i++)
			x += examples.getModel().getElementAt(i).hashCode();
		return name.getText().hashCode()+definition.getText().hashCode()+synonyms.getValues(0).hashCode()+
			   x+power.getSelectedItem().hashCode();
	}
	
	
	/**
	 * fill in content from class
	 */
	private void setConceptInfo(Concept c){
		if(currentNode != null && c != null){
			// add code
			cui.setText(c.getCode());
			
			// add definition
			if(!TextHelper.isEmpty(c.getDefinition()))
				definition.setText(c.getDefinition());
		
			// add synonyms
			if(c.getSynonyms().length > 0){
				//ArrayList slist = new ArrayList();
				Set<String> values = TextHelper.getValues(synonyms.getValues(0));
				for(String s: c.getSynonyms()){
					// cleanup: remove paranthesized comment and lower case
					s = s.replaceAll("\\s*\\(.+\\)\\s*","").toLowerCase();
					//if(!slist.contains(s))
					//	slist.add(s);
					values.add(s);
				}
				//synonyms.clear();
				int i = 0;
				for(String s: values)
					synonyms.setValueAt(s,i++,0);
				//for(int i=values.size();i<slist.size();i++)
				//	synonyms.setValueAt(slist.get(i),i,0);
			}else
				synonyms.setValueAt(name.getText().toLowerCase(),0,0);
			synonyms.repaint();
		
		}
	}
	
	
	/**
	 * fill in content from class
	 */
	private void saveConceptInfo(TreePanel.TextViewNode n){
		if(n != null && n.getInfoHash() != getInfoHash()){
			// sync stuff to node
			final String prevPower = n.getPower();
			n.setCode(cui.getText());
			n.setDefinition(definition.getText());
			n.setSynonyms((String [])synonyms.getValues(0).toArray(new String[0]));
			n.setExampleImages(((DefaultListModel)examples.getModel()).elements());
			n.setPower(""+power.getSelectedItem());
			
			// init action
			final TreePanel.TextViewNode node = n;
			OntologyAction action = new OntologyAction(){
				public void run(){
					node.setClassProperties();
					OntologyHelper.getConceptHandler(getOntology()).
					analyzeConceptClass(node.getConceptClass());
					
					// recursively assign power
					if(!(""+prevPower).equals(""+node.getPower())){
						IProperty p = getOntology().getProperty(OntologyHelper.HAS_POWER);
						OntologyHelper.getConceptHandler(getOntology()).
						setSubtreePropertyValue(node.getConceptClass(),p,node.getPower());
					}
					// TODO: upload new files
				}
				public void undo(){
					node.getClassProperties();
					tree.setSelectedNode(node);
					setConceptInfo(node);
				}
				public String toString(){
					return "set "+node+" properties";
				}
			};
			// add to syncronizer
			synchronizer.addOntologyAction(action);
			//getToolBar().getComponentAtIndex(2).setEnabled(true);
		}
	}
	
	/**
	 * selection detected
	 */
	public void valueChanged(TreeSelectionEvent e) {
		setConceptInfo((TreePanel.TextViewNode) e.getPath().getLastPathComponent());
	}

	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if(prop.equals(OntologyHelper.ONTOLOGY_ACTION)){
			synchronizer.addOntologyAction((OntologyAction) evt.getNewValue());
			//getToolBar().getComponentAtIndex(2).setEnabled(true);
		}else if(prop.equals(OntologyHelper.TREE_LOADED_EVENT)){
			DomainBuilder.getInstance().setBusy(false);
		}else if(prop.equals(OntologyImporter.PROPERTY_PROGRESS_MSG)){
			DomainBuilder.getInstance().getProgressBar().setString(""+evt.getNewValue());
		}
	}
	
	/**
	 * reset space tree
	 */
	private void reset(){
		tree.clear();
		tree.reset();
		//TODO: 
	}

	/**
	 * create menubar
	 */
	public JMenuBar getMenuBar() {
		if(menubar == null){
			menubar = new JMenuBar();
			// file
			JMenu file = new JMenu("File");
			file.add(UIHelper.createMenuItem("New","New Domain Knowledge Base",Icons.NEW,this));
			file.add(UIHelper.createMenuItem("Open","Open Domain Knowledge Base",Icons.OPEN,this));
			if(Communicator.isConnected()){
				JMenuItem item = UIHelper.createMenuItem("Save","Save and Publish Domain Knowledge Base",Icons.PUBLISH,this);
				item.setActionCommand("Publish");
				file.add(item);
			}else
				file.add(UIHelper.createMenuItem("Save","Save Domain Knowledge Base",Icons.SAVE,this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Import","Import Ontology from BioPortal",Icons.IMPORT,this));
			file.add(UIHelper.createMenuItem("Export","Export Domain Knowledge as OWL File",Icons.EXPORT,this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Properties","Edit Knowledge Base Properties",Icons.PROPERTIES,this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Exit","Exit Domain Builder",null,DomainBuilder.getInstance()));
			
			// edit
			final JMenu edit = new JMenu("Edit");
			edit.add(UIHelper.createMenuItem("Undo","Undo",Icons.UNDO,this));
			edit.addSeparator();
			edit.add(UIHelper.createMenuItem("Cut","Cut",Icons.CUT,this));
			edit.add(UIHelper.createMenuItem("Copy","Copy",Icons.COPY,this));
			edit.add(UIHelper.createMenuItem("Paste","Paste",Icons.PASTE,this));
			edit.addSeparator();
			edit.add(UIHelper.createMenuItem("Delete","Delete",Icons.DELETE,this));
			edit.addMenuListener(new MenuListener(){
				public void menuCanceled(MenuEvent e) {}
				public void menuDeselected(MenuEvent e) {}
				public void menuSelected(MenuEvent e) {
					if(readOnly)
						return;
					
					edit.getItem(0).setEnabled(synchronizer.hasActions());
					
					// disable items for system nodes
					boolean system = tree.isSystemClass(tree.getSelectedNode());
					
					// disable system  options
					edit.getItem(2).setEnabled(!system);
					edit.getItem(3).setEnabled(!system);
					edit.getItem(4).setEnabled(!system);
					edit.getItem(6).setEnabled(!system);
				}
			});
			
			
			JMenu builder = new JMenu("Builder");
			builder.add(UIHelper.createMenuItem("Add Concept","Create New Concept(s)",Icons.ADD,KeyEvent.VK_A,this));
			builder.add(UIHelper.createMenuItem("Edit Concept","Edit selected Concept(s)",Icons.EDIT,KeyEvent.VK_E,this));
			builder.addSeparator();
			builder.add(UIHelper.createMenuItem("Refresh Hierarchy","Refresh Concept Hierarchy",Icons.REFRESH,this));
			
			
			JMenu tools = new JMenu("Tools");
			tools.add(UIHelper.createMenuItem("Domain Manager","Domain Manager",Icons.ONTOLOGY,DomainBuilder.getInstance()));
			
			JMenu options = new JMenu("Options");
			options.add(UIHelper.createCheckboxMenuItem("Organize Diagnoses","When checked, Organized Hierarchy " +
									" button will try to infer diagnoses hierarchy.",Icons.HIERARCHY,false,this));
			
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
	 * create new concept
	 */
	private void doAddConcept(){
		if(tree.getSelectedNode() != null){
			TreePanel.doCreateConcept(tree,tree.getSelectedNode());
		}else{
			JOptionPane.showMessageDialog(getFrame(),"You must select a parent concept first.");
		}
	}
	
	/**
	 * classify heirchy
	 */
	private void doOrganize(){
		(new Thread(new Runnable(){
			public void run(){
				// save case before hand
				saveConceptInfo(currentNode);
				KnowledgeAuthor.saveOntology(ontology);
				reloadTree();
				DomainBuilder.getInstance().getProgressBar().setString("Organizing Class Heirarchy ...");
				DomainBuilder.getInstance().setBusy(true);
				OntologyHelper.getConceptHandler(ontology).inferFindingHierarchy();
				if(organizeDiagnoses)
					OntologyHelper.getConceptHandler(ontology).inferDiseaseHierarchy();
				reloadTree();
				DomainBuilder.getInstance().setBusy(false);
			}
		})).start();
		
	}
	
	/**
	 * selection of entries
	 *
	public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting())
			return;
		
		if(cls != null)
			setContent(cls);
		String node = ""+lexicon.getSelectedValue();
		getContent(node);
		tree.setSelectedNode(node);
		
	}
	*/
	
	/**
	 * set read only flag
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		
		// take care of toolbar
		String [] exceptions = new String []
		{"open","export","properties","exit","tools","help"};
		UIHelper.setEnabled(getToolBar(),exceptions,!b);
		UIHelper.setEnabled(getMenuBar(),exceptions,!b);
		tree.setEditable(!b);
	}
	
	
	
	/**
	 * this table represents the suggested terms
	 * @author tseytlin
	 *
	 */
	private static class TermsTableModel extends AbstractTableModel{
		private OrderedMap<String,ConceptEntry> terms;
		private boolean [] include;
		public TermsTableModel(OrderedMap<String,ConceptEntry> map){
			terms = map;
			include = new boolean [map.size()];
		}
		public int getColumnCount() {
			return 3;
		}
		public int getRowCount() {
			return  (terms == null)?0:terms.size();
		}
		public Object getValueAt(int row, int column) {
			if(column == 0)
				return include[row];
			else if(column == 1)
				return terms.getKeys().get(row);
			return terms.getValues().get(row);
		}
		public boolean isCellEditable(int row, int column) {
			return column == 0;
		}
		public void setValueAt(Object aValue, int row, int column) {
			include[row] = (Boolean)aValue;
		}
	    public String getColumnName(int column){
	    	switch(column){
	    	case 0: return "Include";
	    	case 1: return "Suggested Synonym";
	    	default: return "Domain Concept";
	    	}
	    }
	    public Class getColumnClass(int c) {
	        return getValueAt(0, c).getClass();
	    }
        public Map<String,ConceptEntry> getIncludedTerms(){
        	Map<String,ConceptEntry> map = new LinkedHashMap<String, ConceptEntry>();
        	for(int i=0;i<getRowCount();i++){
        		if(include[i]){
        			String key = terms.getKeys().get(i);
        			map.put(key,terms.get(key));
        		}
        	}
        	return map;
        }
	}
	
	/**
	 * check if new terms have been suggested
	 */
	private void doCheckSuggestedTerms(){
		OrderedMap<String,ConceptEntry> map = getSuggestedTerms();
		if(map.isEmpty())
			return;
		String tag = 
			"It appears that tutor users have suggested additional synonyms for concepts that are already in this domain ontology. " +
			"Please check off each term you would like to add to your domain and click <b>OK</b>. All terms that are not selected will be discarded. " +
			"If you click <b>Cancel</b> you will be prompted again the next time you open this domain.";
		final TermsTableModel model = new TermsTableModel(map);
		final JTable table = new JTable();
		table.setModel(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setShowVerticalLines(false);
		table.setRowHeight(Icons.CONCEPT_ICON_HEIGHT+2);
		table.setFillsViewportHeight(true);
		table.setDefaultRenderer(ConceptEntry.class,new IconCellRenderer(false));
		table.getColumnModel().getColumn(0).setMaxWidth(50);
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setMaxWidth(Icons.CONCEPT_ICON_WIDTH+8);
		table.getColumnModel().getColumn(2).setPreferredWidth(Icons.CONCEPT_ICON_WIDTH+8);
		
		JPanel panel = new JPanel();
		JLabel lbl = new JLabel(UIHelper.getDescription(tag,500,200));
		JCheckBox all = new JCheckBox("Add all suggested terms to the domain ontology");
		all.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JCheckBox b = (JCheckBox) e.getSource();
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						for(int i=0;i<model.getRowCount();i++)
							model.setValueAt(b.isSelected(),i,0);
						table.repaint();
					}
				});
			}
		});
		lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
		panel.setLayout(new BorderLayout());
		panel.add(lbl,BorderLayout.NORTH);
		panel.add(new JScrollPane(table),BorderLayout.CENTER);
		panel.add(all,BorderLayout.SOUTH);
		int r = JOptionPane.showConfirmDialog(null,panel,"Add Suggested Terms",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			Map<String,ConceptEntry> imap = model.getIncludedTerms();
			for(String key: imap.keySet()){
				ConceptEntry e = map.get(key);
				IClass c = ontology.getClass(e.getName());
				if(!Arrays.asList(c.getLabels()).contains(key)){
					c.addLabel(key);
					TextViewNode n = tree.getNode(c.getName());
					if(n != null){
						n.getClassProperties();
					}
				}
			}
			// now remove terms file
			if(Communicator.isConnected() && !readOnly && !imap.isEmpty()){
				synchronizer.addOntologyAction(new OntologyAction(){
					public void run() {
						try{
							// figure out local term name
							File dir = OntologyHelper.getLocalOntologyFolder(ontology);
							String o = ontology.getName();
							if(o.endsWith(OWL_SUFFIX))
								o = o.substring(0,o.length()-OWL_SUFFIX.length());
							o = o+TERMS_SUFFIX;
							
							// create an empty terms file in local space
							File f = new File(dir,o);
							if(f.getParentFile().exists()){
								FileWriter writer = new FileWriter(f);
								writer.write("");
								writer.close();
								
								// upload
								UIHelper.upload(f);
							}
						}catch(Exception ex){
							ex.printStackTrace();
							JOptionPane.showMessageDialog(getFrame(),ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);							
						}
						
					}
					public void undo() {
					}
					public String toString(){return "Remove Terms File";}
				});
			};
			
			
		}
	}
	
	/**
	 * load suggested terms
	 * @return
	 */
	private OrderedMap<String, ConceptEntry> getSuggestedTerms() {
		OrderedMap<String,ConceptEntry> map = new OrderedMap<String, ConceptEntry>();
		
		// we don't care what terms are if in read only mode
		if(!readOnly){
			// see if terms file is saved on the server
			try{
				InputStream in = null;
				String o = ontology.getURI().toASCIIString();
				if(o.endsWith(OWL_SUFFIX))
					o = o.substring(0,o.length()-OWL_SUFFIX.length());
				o = o+TERMS_SUFFIX;
				in = (new URL(o)).openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				for(String line = reader.readLine();line != null; line = reader.readLine()){
					String [] p = line.split("\\|");
					if(p.length == 2){
						IClass cls = ontology.getClass(p[1].trim());
						if(cls != null)
							map.put(p[0].trim(),new ConceptEntry(cls));
					}
				}
				reader.close();
			}catch(Exception ex){
				//it's ok to have an exception, we don't care
			}
		}
		return map;
	}
	
}
