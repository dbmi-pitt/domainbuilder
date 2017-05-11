package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IOntologyException;
import edu.pitt.ontology.IRepository;
import edu.pitt.ontology.protege.ProtegeRepository;
import edu.pitt.ontology.ui.OntologyExplorer;


/**
 * domain selector
 * @author tseytlin
 */

public class DomainSelectorPanel extends JPanel 
	implements EntryChooser, ListSelectionListener, ActionListener, ItemListener {
	private IRepository repository;
	private JList organs,domains;
	private JComboBox institution;
	private JTextField preview;
	private JProgressBar progress;
	private JButton okButton,cancelButton;
	private JDialog dialog;
	private JOptionPane op;
	private Frame frame;
	private boolean ok;
	private Map<String,Vector<IOntology>> ontologyMap;
	private Vector<String> organList,institutionList;
	private String baseURL = OntologyHelper.DEFAULT_BASE_URI;
	private static URI lockedOntology;
	private File dir;
	private IOntology selectedOntology;
	
	public DomainSelectorPanel(IRepository repository){
		this(repository,false);
	}
	
	public DomainSelectorPanel(IRepository repository, boolean manage){
		super();
		this.repository = repository;
		createInterface(manage);
		load();
	}
	
	/**
	 * create UI
	 */
	private void createInterface(boolean manage){
		setLayout(new BorderLayout());
		
		institution = new JComboBox(new DefaultComboBoxModel());
		institution.setBorder(new TitledBorder("Institiution"));
		institution.addItemListener(this);
		
		organs = new JList(new DefaultListModel());
		organs.addListSelectionListener(this);
		organs.setVisibleRowCount(15);
		domains = new JList();
		domains.setVisibleRowCount(15);
		domains.addListSelectionListener(this);
		
		preview = new JTextField();
		preview.setEditable(false);
		preview.setForeground(Color.blue);
		JScrollPane s1 = new JScrollPane(organs);
		s1.setPreferredSize(new Dimension(200,200));
		s1.setBorder(new TitledBorder("Organ"));
		JScrollPane s2 = new JScrollPane(domains);
		s2.setBorder(new TitledBorder("Domain"));
		s2.setPreferredSize(new Dimension(300,200));
		add(institution,BorderLayout.NORTH);
		add(s1,BorderLayout.WEST);
		add(s2,BorderLayout.CENTER);
		if(manage)
			add(createToolBar(),BorderLayout.EAST);
		add(preview,BorderLayout.SOUTH);
		
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		
		
		// create popup
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		okButton.setEnabled(false);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		op = new JOptionPane(this,JOptionPane.PLAIN_MESSAGE,
		JOptionPane.OK_CANCEL_OPTION,null,new Object []{okButton,cancelButton});
		
		// add dobulic click listener
		domains.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(domains.getSelectedValue() != null && e.getClickCount() == 2)
					okButton.doClick();
			}
		});
			
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		JComponent c = this;
		if(busy){
			c.remove(preview);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			progress.setString(null);
			progress.setIndeterminate(true);
			c.remove(progress);
			c.add(preview,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	private JToolBar createToolBar(){
		JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
		toolbar.add(UIHelper.createButton("New",
				"Create New Domain Ontology",Icons.NEW,this));
		toolbar.add(UIHelper.createButton("Browse",
				"Browse Domain Ontology",Icons.BROWSE,this));
		toolbar.add(UIHelper.createButton("Import",
				"Import Domain Ontology from OWL file",Icons.IMPORT,this));
		toolbar.add(UIHelper.createButton("Export",
				"Export Domain Ontology to OWL file",Icons.EXPORT,this));
		toolbar.add(UIHelper.createButton("Delete",
				"Remove Domain Ontology",Icons.DELETE,this));
		return toolbar;
	}
	
	/**
	 * load ontology data
	 */
	public void load(){
		load(null);
	}
	
	
	/**
	 * load ontology data
	 */
	public void load(IOntology selected){
		final IOntology selectedOnt = selected;
		// clear everything
		organList = new Vector<String>();
		institutionList = new Vector<String>();
		
		//organs.setListData(organList);
		domains.setListData(new Vector());
		
		//institution.removeAllItems();
		//preview.setText("");
		
		// iterate over ontologies and create a map
		ontologyMap = new HashMap<String,Vector<IOntology>>();
		Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
		for(IOntology ont: repository.getOntologies()){
			// skip KnowledgeBase and instance ontologies
			String url = ""+ont.getURI();
			if( url.endsWith(OntologyHelper.KNOWLEDGE_BASE) || 
			    url.endsWith(OntologyHelper.INSTANCES_ONTOLOGY)){
				continue;
			}
			// now parse url to extract relevant info
			// (it better be formated right)
			Matcher m = pt.matcher(url);
			if(m.matches()){
				baseURL = m.group(1);
				String organ = m.group(2);
				String inst  = m.group(3);
				// add to models
				if(!organList.contains(organ))
					organList.add(organ);
				if(!institutionList.contains(inst)){
					//institution.addItem(inst);
					institutionList.add(inst);
				}
				// put into hashmap
				Vector<IOntology> list = ontologyMap.get(organ+"/"+inst);
				if(list == null){
					list = new Vector<IOntology>();
					ontologyMap.put(organ+"/"+inst,list);
				}
				list.add(ont);
			}
		}
		Collections.sort(institutionList);
		Collections.sort(organList);
		
		// load new data
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				//organs.setListData(organList);
				//domains.setListData(new Vector());
				institution.removeAllItems();
				preview.setText("");
				
				// add stuff to lists
				for(String inst: institutionList)
					institution.addItem(inst);
				organs.setListData(organList);
				
				// set default
				if(institutionList.contains(OntologyHelper.DEFAULT_INSTITUTION))
					institution.setSelectedItem(OntologyHelper.DEFAULT_INSTITUTION);
				
				
				//setup defaults
				if(selectedOnt != null){
					Pattern pt = Pattern.compile("(http://.+/owl/)(.+)/(.+)/.+");
					Matcher m = pt.matcher(""+selectedOnt.getURI());
					if(m.matches()){
						String organ = m.group(2);
						String inst  = m.group(3);
						
						// set defaults
						institution.setSelectedItem(inst);
						organs.setSelectedValue(organ,true);
						domains.setSelectedValue(selectedOnt,true);
					}
				}else
					organs.setSelectedIndex(0);
			}
		});
	}
	
	
	/**
	 * import ontology
	 * @param u
	 */
	private void importOntology(URI u){
		final URI uri = u;
		setBusy(true);
		(new Thread(new Runnable(){
			public void run(){
				try{
					repository.importOntology(uri);
				}catch(Exception ex){
					ex.printStackTrace();
					JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
				}
				load();
				setBusy(false);
			}
		})).start();
	}
	
	
	public Object getSelectedObject() {
		return (domains.isSelectionEmpty())?selectedOntology:domains.getSelectedValue();
	}

	public Object[] getSelectedObjects() {
		return new Object [] {getSelectedObject()};
	}

	public int getSelectionMode() {
		return EntryChooser.SINGLE_SELECTION;
	}

	public boolean isSelected() {
		return ok && (!domains.isSelectionEmpty() || selectedOntology != null);
	}

	public void setOwner(Frame frame) {
		this.frame = frame;

	}

	public void setSelectionMode(int mode) {
		//do nothing
	}

	public void showChooserDialog() {
		load();
		dialog = op.createDialog(frame,"Open Domain");
		dialog.setVisible(true);
		//int r = JOptionPane.showOptionDialog(frame,this,"Open Domain",
		//		JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		//ok = r == JOptionPane.OK_OPTION;
	}
		
	/**
	 * show new domain dialog
	 */
	public boolean showNewDomainDialog(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		JComboBox org = new JComboBox(organList);
		org.setEditable(true);
		org.setBorder(new TitledBorder("Organ"));
		org.setPreferredSize(new Dimension(100,45));
		JComboBox ins = new JComboBox(institutionList);
		ins.setEditable(true);
		ins.setBorder(new TitledBorder("Institution"));
		ins.setPreferredSize(new Dimension(100,45));
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextField name = new JTextField();
		p.setBorder(new TitledBorder("Domain"));
		p.setPreferredSize(new Dimension(200,45));
		p.add(name,BorderLayout.CENTER);
		panel.add(org);
		panel.add(ins);
		panel.add(p);
		
		// set defaults
		if(organs != null){
			Object obj  = organs.getSelectedValue();
			if(obj != null)
				org.setSelectedItem(obj);
		}
		if(institution != null){
			Object obj = institution.getSelectedItem();
			if(obj != null)
				ins.setSelectedItem(obj);
		}
		
		
		int r = JOptionPane.showConfirmDialog(frame,panel,"Create Domain",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		ok = r == JOptionPane.OK_OPTION && name.getText().length() > 0;
		if(ok){
			createNewDoman(""+org.getSelectedItem(),""+ins.getSelectedItem(),name.getText());
		}
		return ok;
	}
	
	/**
	 * create new domain
	 * @param org
	 * @param ins
	 * @param name
	 */
	private void createNewDoman(String org,String ins,String n){
		final String organ = org;
		final String source = ins;
		final String name = n;
		setBusy(true);
		(new Thread(new Runnable(){
			public void run(){
				try{
					String nm = name.trim().replaceAll("\\W","_");
					URI uri = new URI(baseURL+organ+"/"+source+"/"+nm+".owl");
				
					// make sure such ontology doesn't exist yet
					if(repository.getOntology(uri) != null){
						JOptionPane.showMessageDialog(frame,
								"Domain Ontology "+uri+" already exists!",
								"Error",JOptionPane.ERROR_MESSAGE);
						setBusy(false);
						return;
					}
					selectedOntology = null;
					IOntology ont = repository.createOntology(uri);
					ont.addImportedOntology(repository.getOntology(OntologyHelper.KNOWLEDGE_BASE_URI));
					repository.importOntology(ont);
					load(ont);
					selectedOntology = ont;
					
					// show create ontology
					institution.setSelectedItem(source);
					organs.setSelectedValue(organ,true);
					domains.setSelectedValue(ont,true);
				}catch(Exception ex){
					JOptionPane.showMessageDialog(frame,
							"Problem Creating New Domain Ontology!",
							"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
				setBusy(false);
			}
		})).start();
	}
	
	/**
	 * load domains
	 * @param organ
	 */
	private void loadDomains(String organ,String inst){
		Vector<IOntology> list = ontologyMap.get(organ+"/"+inst);
		if(list == null)
			list = new Vector();
		Collections.sort(list);
		domains.setListData(list);
		preview.setText("");
		//domains.setSelectedIndex(0);
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting()){
			if(e.getSource().equals(organs)){
				loadDomains(""+organs.getSelectedValue(),""+institution.getSelectedItem());
			}else if(e.getSource().equals(domains)){
				Object obj = domains.getSelectedValue();
				if(obj instanceof IOntology){
					preview.setText(""+((IOntology)obj).getURI());
				}
				okButton.setEnabled(obj != null);
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource().equals(institution)){
			loadDomains(""+organs.getSelectedValue(),""+institution.getSelectedItem());
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(e.getSource().equals(okButton)){
			ok = true;
			if(dialog != null)
				dialog.dispose();
		}else if(e.getSource().equals(cancelButton)){
			ok = false;
			if(dialog != null)
				dialog.dispose();
		}else if("New".equals(cmd)){
			showNewDomainDialog();
		}else if("Import".equals(cmd)){
			doImport();
		}else if("Export".equals(cmd)){
			doExport();
		}else if("Delete".equals(cmd)){
			doRemove();
		}else if("Browse".equals(cmd)){
			doBrowse();
		}
	}
	
	/**
	 * remove ontology
	 */
	private void doRemove(){
		IOntology ont = (IOntology) getSelectedObject();
		if(ont != null){
			// check if locked
			if(isLocked(ont)){
				JOptionPane.showMessageDialog(frame,"Cannot delete "+ont+
						" because it is currently open",
						"Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			// ask for permission
			int r = JOptionPane.showConfirmDialog(frame,"<html>Are you sure you want to delete  <b>"+
					ont.getName()+"</b> domain ontology?","Confirm",
					JOptionPane.OK_CANCEL_OPTION);
			// if not canceled, remove entry
			if(r != JOptionPane.CANCEL_OPTION){
				IOntology inst = OntologyHelper.getCaseBase(ont);
				if(inst != null){
					// remove entry
					repository.removeOntology(inst);
					// remove data
					inst.delete();
				}
				// remove entry
				repository.removeOntology(ont);
				// remove data
				ont.delete();
				load();
			}
		}
	}
	
	
	/**
	 * is this ontology locked
	 * @param ont
	 * @return
	 */
	private boolean isLocked(IOntology ont){
		return ont.getURI().equals(lockedOntology);
	}
	
	
	/**
	 * set ontology as locked
	 * @param o
	 */
	public static void setLockedOntology(URI o){
		lockedOntology = o;
	}
	
	/**
	 * import ontology from file
	 */
	private void doImport(){
		JFileChooser chooser = new JFileChooser(dir);
		//if(dir != null)
		//	chooser.setSelectedFile(dir);
		if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
			File f = chooser.getSelectedFile();
			if(f != null && f.canRead()){
				importOntology(f.toURI());
				dir = f.getParentFile();
			}
		}
	}
	
	
	
	
	/**
	 * export ontology
	 */
	private void doExport(){
		final Object value = getSelectedObject();
		if(value != null && value instanceof IOntology){
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileFilter(){
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".owl");
				}
				public String getDescription() {
					return "OWL File";
				}
				
			});
			if(dir == null)
				dir = chooser.getFileSystemView().getDefaultDirectory();
			chooser.setSelectedFile(new File(dir,value.toString()));
			if(chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION){
				final File f = chooser.getSelectedFile();
				dir = f.getParentFile();
				setBusy(true);
				(new Thread(new Runnable(){
					public void run(){
						IOntology ont = (IOntology) value;
						try{
							ont.load();
							ont.write(new FileOutputStream(f),IOntology.OWL_FORMAT);
						}catch(Exception ex){
							ex.printStackTrace();
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);							
						}
						setBusy(false);
					}
				})).start();
			}
			
		}
	}
	
	/**
	 * browse ontology
	 */
	private void doBrowse(){
		final OntologyExplorer explorer = new OntologyExplorer();
		JDialog f = new JDialog(dialog);
		f.setTitle("Ontology Explorer");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(explorer);
		f.setResizable(true);
		f.pack();
		f.setVisible(true);
		// set root
		Object value = getSelectedObject();
		if(value instanceof IOntology){
			IOntology ont = OntologyHelper.getCaseBase((IOntology) value);
			final IOntology ontology = (ont == null)?(IOntology)value:ont;
			explorer.setBusy(true);
			(new Thread(new Runnable(){
				public void run(){
					try{
						ontology.load();
					}catch(IOntologyException ex){
						ex.printStackTrace();
					}
					explorer.setRoot(ontology.getRootClasses());
					explorer.setBusy(false);
				}
			})).start();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost/repository";
		String user = "user";
		String pass = "resu";
		String table = "repository";
		String dir   = System.getProperty("user.home")+File.separator+".protegeRepository";
		IRepository r = new ProtegeRepository(driver,url,user,pass,table,dir);
		DomainSelectorPanel selector = new DomainSelectorPanel(r,true);
		selector.showChooserDialog();
		/*
		if(selector.showNewDomainDialog()){
			while(!selector.isSelected()){
				UIHelper.sleep(250);
			}
			System.out.println(selector.getSelectedObject());
		}
		*/
	}


}
