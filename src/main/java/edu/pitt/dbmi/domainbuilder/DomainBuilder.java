package edu.pitt.dbmi.domainbuilder;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.plaf.metal.MetalLookAndFeel;

import edu.pitt.dbmi.domainbuilder.caseauthor.CaseAuthor;
import edu.pitt.dbmi.domainbuilder.knowledge.KnowledgeAuthor;
import edu.pitt.dbmi.domainbuilder.util.Communicator;
import edu.pitt.dbmi.domainbuilder.util.DomainTerminology;
import edu.pitt.dbmi.domainbuilder.util.FileRepository;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.validation.KnowledgeCloud;
import edu.pitt.dbmi.domainbuilder.widgets.DomainSelectorPanel;
import edu.pitt.dbmi.domainbuilder.widgets.FontPanel;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.ProtegeRepository;
import edu.pitt.ontology.ui.RepositoryManager;
import edu.pitt.terminology.RemoteTerminology;
import edu.pitt.terminology.Terminology;



/**
 * this class is the main application class for domain builder
 * application
 * @author tseytlin
 *
 */
public class DomainBuilder implements ActionListener {
	private static DomainBuilder instance;
	private static Properties params = new Properties();
	private JFrame frame;
	//private JToolBar toolbar;
	private JMenuBar menubar;
	private JPanel statusPanel;
	private JLabel knowledgeLabel,statusLabel;
	private Component helpComponent;
	private JProgressBar progress;
	private DomainBuilderComponent caseAuthor,knowledgeAuthor,knowledgeExplorer;
	private DomainSelectorPanel domainManager;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private JTabbedPane tabs;
	private static IRepository repository;
	private static RepositoryManager repositoryManager;
	
	
	// some constunts 
	private final String ABOUT_MESSAGE = "<html><h2>Domain Builder</h2>" +
										"<a href=\"http://slidetutor.upmc.edu/domainbuilder\">http://slidetutor.upmc.edu/domainbuilder</a><br>"+
										"Department of BioMedical Informatics<br>University of Pittsburgh";
	private URI domain  =  URI.create("http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/Melanocytic.owl");
	//private final URI domain  =  URI.create("http://slidetutor.upmc.edu/domainbuilder/owl/skin/UPMC/Subepidermal.owl");

	
	
	
	/**
	 * initialize parameters
	 */
	public DomainBuilder(){
		instance = this;
		try{
    		//String os = System.getProperty("os.name").toLowerCase();
			//if(os.contains("windows"))
			//	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			//else
			UIManager.setLookAndFeel(new MetalLookAndFeel());
    	}catch(Exception ex){}
    	
    	// make sure to release resources
    	Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				dispose();
			}
		});
	}
	
	/**
	 * dispose of resources
	 */
	public void dispose(){
		caseAuthor.dispose();
		knowledgeAuthor.dispose();
		knowledgeExplorer.dispose();
		Terminology r = repository.getTerminology(OntologyHelper.ONTOLOGY_TERMINOLOGY);
		if(r != null)
			repository.removeTerminology(r);
		pcs.firePropertyChange(UIHelper.UI_CLOSING_EVENT,null,null);
	}
	
	
	/**
	 * derive instance domain
	 * @param uri
	 * @return
	 */
	private URI getInstanceDomain(URI uri){
		String owl = OntologyHelper.OWL_SUFFIX;
		String s = ""+uri;
		int i = s.length();
		if(s.endsWith(owl))
			i = i - owl.length();
		String iuri = s.substring(0,i)+OntologyHelper.INSTANCES_ONTOLOGY;
		return URI.create(iuri);
	}
	/**
	 * do load domain
	 * @param domain
	 */
	public void doLoad(URI domain){
		getProgressBar().setString("Loading Knowledge Base, Please Wait ...");
		setBusy(true);
		// load knowedge base
		DomainSelectorPanel.setLockedOntology(domain);
		URI uri = loadKnowledgeBase(domain);
		loadTerminology(domain);
		setBusy(false);
		
		// notify that knowledge base is done
		pcs.firePropertyChange(OntologyHelper.KB_LOADED_EVENT,null,uri);
	}
	
	
	public void firePropertyChange(String p, Object o1, Object o2){
		pcs.firePropertyChange(p,o1,o2);
	}
	
	
	
	/**
	 * load knowledge base
	 * @param domain
	 */
	private URI loadKnowledgeBase(URI domain){
		// clear all caches related to domains
		OntologyHelper.clearCache();
		
		// create new ontology
		URI uri = getInstanceDomain(domain);
		
		long time = System.currentTimeMillis();
		// get ontology
		try{
			IRepository r = DomainBuilder.getRepository();
			// check if ontology exists
			if(!r.hasOntology(""+uri)){
				IOntology ont = r.createOntology(uri);
				IOntology kb = r.getOntology(domain);
				ont.addImportedOntology(kb);
				r.importOntology(ont);
			}
			IOntology kb = DomainBuilder.getRepository().getOntology(uri);
			OntologyHelper.checkLocalOntology(kb);
			kb.load();
			OntologyHelper.checkOntologyVersion(kb);
			
			// set read only flag
			// check if same institution
			String place = getParameter("repository.institution");
			if(!OntologyHelper.isSameInstitution(domain,place))
				OntologyHelper.setReadOnly(r.getOntology(domain),true);
			if(!OntologyHelper.isSameInstitution(uri,place))
				OntologyHelper.setReadOnly(kb,true);
			
			
		}catch(IOntologyException ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame,ex.getMessage(),
					"Error",JOptionPane.ERROR_MESSAGE);
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame,"No KnowledgeBase exists",
					"Error",JOptionPane.ERROR_MESSAGE);
		}
		knowledgeLabel.setText("<html>domain: <font color=blue><u>"+domain+"</u></font>");
		statusPanel.revalidate();
		System.out.println("ontology load time: "+(System.currentTimeMillis()-time)+" ms");
		return uri;
	}
	
	
	/**
	 * load terminology from ontology
	 * @param uri
	 */
	public void loadTerminology(URI uri){
		Terminology term = repository.getTerminology(OntologyHelper.ONTOLOGY_TERMINOLOGY);
		//Terminology term = repository.getTerminology(OntologyHelper.LUCENE_TERMINOLOGY);
		// remove previous lucene terminology
		if(term != null)
			repository.removeTerminology(term);
		
			
		IOntology  ont = repository.getOntology(uri);
		OntologyHelper.checkLocalOntology(ont);
		long time = System.currentTimeMillis();
		try{
			// create new terminology
			//term = ((ProtegeRepository)repository).createTerminology();
			term = new DomainTerminology(ont,ont.getClass(OntologyHelper.CONCEPTS));
			repository.addTerminology(term);
			
			
			// add lexicon classes
			/*
			for(IClass c: ont.getClass(OntologyHelper.LEXICON).getDirectSubClasses()){
				//System.out.println(Arrays.toString(c.getConcept().getSynonyms()));
				Concept concept = c.getConcept();
				concept.setName(UIHelper.getPrettyClassName(c.getName()));
				term.addConcept(concept);
			}*/
			/*
			if(term instanceof LuceneTerminology){
				((LuceneTerminology)term).commit();
			}*/
		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println("terminology load time: "+(System.currentTimeMillis()-time)+" ms");
	}
	
	/**
	 * add single class to terminology
	 * @param cls
	 * @param term
	 *
	private void addClass(IClass cls, Terminology term) throws TerminologyException {
		if(term.addConcept(cls.getConcept()))
			for(IClass c: cls.getDirectSubClasses())
				addClass(c,term);
	}
	*/
	
	/**
	 * prompt for domain
	 * @return
	 */
	private URI promptDomain(){
		IRepository r = getRepository();
		if(r == null){
			System.exit(1);
		}
		if(domainManager == null){
			domainManager = new DomainSelectorPanel(r,true);
			domainManager.setOwner(getFrame());
		}
		domainManager.showChooserDialog();
		if(domainManager.isSelected()){
			return ((IOntology)domainManager.getSelectedObject()).getURI();
		}else{
			System.exit(0);
		}
		return null;
	}
	

	/**
	 * start the main application
	 */
	public void start(){
		getFrame();
		frame.setVisible(true);
	
		// load knowledge base
		(new Thread(new Runnable(){
			public void run(){
				// prompt for domain
				//doLoad(domain);
				doLoad(promptDomain());
			}
		})).start();
		
	}
	
	/**
	 * get status label component
	 * @return
	 */
	public JLabel getStatusLabel(){
		return statusLabel;
	}
	
	/**
	 * get status label component
	 * @return
	 */
	public JLabel getInfoLabel(){
		return knowledgeLabel;
	}
	
	/**
	 * get frame of domain builder
	 * @return
	 */
	public JFrame getFrame(){
		if(frame == null)
			frame = createGUI();
		return frame;
	}
	
	/**
	 * create GUI
	 * @return
	 */
	private JFrame createGUI(){
		JFrame f = new JFrame("DomainBuilder");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.setIconImage(((ImageIcon)Icons.getIcon(Icons.LOGO)).getImage());
		
		// tooltip longer
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
		
		
		// intialize domain builder components
		caseAuthor = new CaseAuthor();
		knowledgeAuthor =  new KnowledgeAuthor();
		knowledgeExplorer = new KnowledgeCloud();
		pcs.addPropertyChangeListener(caseAuthor);
		pcs.addPropertyChangeListener(knowledgeAuthor);
		pcs.addPropertyChangeListener(knowledgeExplorer);
		
		// init other components
		statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout());
		knowledgeLabel = new JLabel(" ");
		knowledgeLabel.setFont(knowledgeLabel.getFont().deriveFont(Font.PLAIN));
		statusLabel = new JLabel(" ");
		statusLabel.setFont(knowledgeLabel.getFont().deriveFont(Font.PLAIN));
		statusPanel.add(knowledgeLabel,BorderLayout.WEST);
		statusPanel.add(statusLabel,BorderLayout.EAST);
		
		// create tabbed component
		tabs = new JTabbedPane();
		tabs.addTab(caseAuthor.getName(),caseAuthor.getIcon(),caseAuthor.getComponent());
		tabs.addTab(knowledgeAuthor.getName(),knowledgeAuthor.getIcon(),knowledgeAuthor.getComponent());
		tabs.addTab(knowledgeExplorer.getName(),knowledgeExplorer.getIcon(),knowledgeExplorer.getComponent());
		tabs.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				JTabbedPane tab = (JTabbedPane) e.getSource();
				Component c = tab.getSelectedComponent();
				if(c instanceof DomainBuilderComponent){
					DomainBuilderComponent dc = (DomainBuilderComponent)c;
					getFrame().setJMenuBar(dc.getMenuBar());
					dc.load();
				}
			}
		});
		
		//f.getContentPane().add(getToolBar(),BorderLayout.NORTH);
		f.getContentPane().add(tabs,BorderLayout.CENTER);
		f.getContentPane().add(statusPanel,BorderLayout.SOUTH);
		f.setJMenuBar(caseAuthor.getMenuBar());
		f.pack();
		
		// progress bar
		progress = new JProgressBar();
		//progress.setString("Loading Knowledge Base, Please Wait ...");
		progress.setStringPainted(true);
		progress.setIndeterminate(true);
		statusPanel.setPreferredSize(progress.getPreferredSize());
		
		
		// resize
    	Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		f.setSize(d.width-50, d.height-50);
     	//f.setSize(1024,768);
     	//f.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		return f;
	}
	
	
	/**
	 * select tab
	 * @param obj
	 */
	public void setSelectedTab(Component c){
		tabs.setSelectedComponent(c);
		
	}
	
	/**
	 * exit application
	 */
	public void exit(){
		dispose();
		System.exit(0);
	}
	
	private void doHelp(){
		JOptionPane op = new JOptionPane(new JScrollPane(getHelpComponent()),JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(frame,"Help");
		d.setModal(false);
		d.setResizable(true);
		d.setVisible(true);
	}
	
	/**
	 * create help panel
	 * @return
	 */
	private UIHelper.HTMLPanel createHelpComponent(){
		UIHelper.HTMLPanel panel = new UIHelper.HTMLPanel();
		panel.setPreferredSize(new Dimension(850,600));
		panel.setEditable(false);
		panel.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e){
				if(e.getEventType() == EventType.ACTIVATED){
					JEditorPane p = (JEditorPane) e.getSource();
					String ref = e.getDescription();
					if(ref.startsWith("#"))
						ref = ref.substring(1);
					p.scrollToReference(ref);
				}
			}
		});
		try{
			if(UIHelper.HELP_FILE.startsWith("http://"))
				panel.setPage(UIHelper.HELP_FILE);
			else
				panel.append(getHelpText());
		}catch(Exception ex){
			ex.printStackTrace();
		}
		panel.setCaretPosition(0);
		return panel;
	}
	
	/**
	 * create help panel
	 * @return
	 */
	private Component getHelpComponent(){
		if(helpComponent == null)
			helpComponent = createHelpComponent();
		return helpComponent;
	}
	
	// construct a help string
	private String getHelpText() {
		String helpText = null;
		String help = UIHelper.HELP_FILE;
		Map map = new HashMap();
		URL url = getClass().getResource(help);
		if (url != null) {
			String s = url.toString();
			map.put("PATH", s.substring(0, s.lastIndexOf("/")));
		}
		try {
			//((interfaceType == ARC) ? archelp : algohelp);
			if(help.startsWith("http://")){
				helpText = UIHelper.getText((new URL(help)).openStream(),map);
			}else{
				helpText = UIHelper.getText(getClass().getResourceAsStream(help), map);
				helpText = helpText.replaceAll("\n", "");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	
		return helpText;
	}
	
	
	/**
	 * get progress bar
	 * @return
	 */
	public JProgressBar getProgressBar(){
		return progress;
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		JComponent c = (JComponent)frame.getContentPane();
		if(busy){
			c.remove(statusPanel);
			c.add(progress,BorderLayout.SOUTH);
		}else{
			progress.setIndeterminate(true);
			progress.setString(null);
			c.remove(progress);
			c.add(statusPanel,BorderLayout.SOUTH);
		}
		c.revalidate();
		c.repaint();
	}
	
	
	/**
	 * get MenuBar
	 * @return
	 */
	public JMenuBar getMenuBar(){
		if(menubar == null)
			menubar = createMenuBar();
		return menubar;
	}
	
	/**
	 * create toolbar
	 */
	private JMenuBar createMenuBar(){
		JMenuBar menubar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenu tools = new JMenu("Tools");
		JMenu help = new JMenu("Help");
		
		JMenuItem exit = new JMenuItem("exit");
		exit.setActionCommand("exit");
		exit.addActionListener(this);
		file.add(exit);
		
		JMenuItem repo = new JMenuItem("Repository Manager ...");
		repo.setActionCommand("repository-manager");
		repo.addActionListener(this);
		tools.add(repo);
		
		JMenuItem protege = new JMenuItem("Protege Editor ...");
		protege.setActionCommand("protege-editor");
		protege.addActionListener(this);
		tools.add(protege);
		
		
		JMenuItem about = new JMenuItem("about");
		about.setActionCommand("about");
		about.addActionListener(this);
		help.add(about);
		
		JMenuItem fontSize = new JMenuItem("Change Font Size ...");
		fontSize.setActionCommand("font-size");
		fontSize.addActionListener(this);
		tools.add(fontSize);
		
		
		menubar.add(file);
		menubar.add(tools);
		menubar.add(help);
		return menubar;
	}
	
	/**
	 * take care of buttons
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand().toLowerCase();
		if(cmd.equals("exit")){
			exit();
		}else if(cmd.equals("about")){
			JOptionPane.showMessageDialog(getFrame(),ABOUT_MESSAGE,"About",
			JOptionPane.PLAIN_MESSAGE,Icons.getIcon(Icons.ONTOLOGY,48));
		}else if(cmd.equals("help")){
			doHelp();
		}else if(cmd.equals("repository manager")){
			doRepositoryManager();
		}else if(cmd.equals("protege-editor")){
			doProtege();
		}else if(cmd.equals("domain manager")){
			JOptionPane op = new JOptionPane(domainManager,JOptionPane.PLAIN_MESSAGE);
			JDialog d = op.createDialog(getFrame(),"Domain Manager");
			d.setModal(false);
			d.setVisible(true);
		}else if(cmd.equals("change font size")){
			FontPanel.getInstance().showDialog(getFrame());
		}
	}
	
	
	private void doRepositoryManager(){
		if(repositoryManager == null){
			repositoryManager = new RepositoryManager();
			repositoryManager.start(getRepository());
		}else{
			repositoryManager.getFrame().setVisible(true);
		}
	}
	
	/**
	 * open protege editor
	 */
	private void doProtege(){
		ProtegeRepository r = (ProtegeRepository) getRepository();
		IOntology ont = r.getOntology(getInstanceDomain(domain));
		r.openProtegeEditor(ont);	
	}
	
	
	/**
	 * get parameters
	 * @return
	 */
	public static Properties getParameters(){
		return params;
	}
	
	/**
	 * get parameter
	 * @param key
	 * @return
	 */
	public static String getParameter(String key){
		return params.getProperty(key,"");
	}
	
	/**
	 * get parameter
	 * @param key
	 * @return
	 */
	public static void setParameter(String key,String val){
		if(params == null)
			params = new Properties();
		params.setProperty(key,val);
	}
	
	
	
	/**
	 * @return the repository
	 */
	public static IRepository getRepository() {
		if(repository == null){
			IRepository rep = null;
			String url    =  params.getProperty("repository.url");
			try{
				/*if(params.containsKey("repository.configuration")){
					// create proptege repository based on config file
					rep = new ProtegeRepository(params.getProperty("repository.configuration"));
				}else */
					
				if(params.containsKey("repository.driver")){
					// create database based repository 
					String driver =  params.getProperty("repository.driver");
					String user   =  params.getProperty("repository.username");
					String pass	  =  params.getProperty("repository.password");
					String table  =  params.getProperty("repository.table");
					String dir    =  params.getProperty("repository.folder");
					File folder = new File(System.getProperty("user.home")+File.separator+dir);
					if(!folder.exists())
						folder.mkdirs();
					rep = new ProtegeRepository(driver,url,user,pass,table,folder.getAbsolutePath());
				}else if(params.containsKey("curriculum.path")){
					// create file based repository when curriculum is in the same location
					rep = new FileRepository(new File(params.getProperty("curriculum.path")));
				}else if(params.containsKey("file.manager.server.url")){
					// create URL based repository given a servlet 
					URL u = new URL(params.getProperty("file.manager.server.url"));
					String dir    =  params.getProperty("repository.folder");
					if(dir != null){
						File proj = new File(System.getProperty("user.home")+File.separator+dir);
						rep = new FileRepository(u,proj);
					}else
						rep = new FileRepository(u);
					
				}
			}catch(Exception ex){
				String u = ex.getMessage();
				JOptionPane.showMessageDialog(null,"<html>"+u.replace(url,"<a href=\"\">"+url+"</a>"),"Error",JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
				return null;
			}
			
			// load lucene terminology
			/*
			if(rep.getTerminology(OntologyHelper.LUCENE_TERMINOLOGY) == null){
				try{
					rep.addTerminology(rep.createTerminology());
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}*/
			JProgressBar progress = getInstance().getProgressBar();
			progress.setString("Please wait ...");
			progress.setIndeterminate(true);
			getInstance().setBusy(true);
			
			// if KowledgeBase.owl is not there import it from URI
			if(!rep.hasOntology(OntologyHelper.KNOWLEDGE_BASE)){
				progress.setString("Importing "+OntologyHelper.KNOWLEDGE_BASE+" for the first time ...");
				try{
					rep.importOntology(URI.create(OntologyHelper.DEFAULT_BASE_URI+OntologyHelper.KNOWLEDGE_BASE));
				}catch(IOntologyException ex){
					ex.printStackTrace();
				}
			}
			
			
			// load remote terminology
			if(rep.getTerminology(OntologyHelper.REMOTE_TERMINOLOGY) == null){
				progress.setString("Adding Controlled Terminology  for the first time ...");
				String u = params.getProperty("terminology.server.url");
				try{
					rep.addTerminology(new RemoteTerminology(new URL(u)));
				}catch(Exception ex){
					JOptionPane.showMessageDialog(null,"Unable to load remote terminology: "+u,"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
			
			// load anatomic ontology
			// if KowledgeBase.owl is not there import it from URI
			if(!rep.hasOntology(OntologyHelper.ANATOMY_ONTOLOGY)){
				progress.setString("Importing "+OntologyHelper.ANATOMY_ONTOLOGY+" for the first time ...");
				try{
					rep.importOntology(URI.create(OntologyHelper.DEFAULT_BASE_URI+OntologyHelper.ANATOMY_ONTOLOGY));
				}catch(IOntologyException ex){
					ex.printStackTrace();
				}
			}
			getInstance().setBusy(false);
			repository = rep;
			
		}
		return repository;
	}

	/**
	 * @param repository the repository to set
	 */
	public static void setRepository(IRepository repository) {
		DomainBuilder.repository = repository;
	}

	
	/**
	 * @return the instance
	 */
	public static DomainBuilder getInstance() {
		return instance;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		final String CONFIG = "DomainBuilder.conf";
		
		// check wheather for local config file is there
		// this file overwrites any sort of server config
		File config = new File(System.getProperty("user.home")+File.separator+"."+CONFIG);
		InputStream in = null;
		String [] login = null;
		if(config.exists() && config.canRead()){
			in = new FileInputStream(config);
		}else if(args.length > 0){
			// check first parameter if it is a file or a url
			String p = args[0];
			if(p.startsWith("http:")){
				OntologyHelper.setDomainBuilderServer(p);
				// if url points directly to config, read it there
				if(p.endsWith(CONFIG)){
					in = (new URL(p)).openConnection().getInputStream();
				}else{
					params.setProperty("domain.builder.server.url",p);
					params.setProperty("file.manager.server.url",p+OntologyHelper.FILE_MANAGER_SERVLET);
					login = UIHelper.promptLogin(true);
					if(login != null &&  login.length == 3){
						in = (new URL(p+"/config/"+login[2]+"/"+CONFIG)).openConnection().getInputStream();
					}
							
				}
			}else{
				config = new File(p);
				if(config.exists() && config.canRead())
					in = new FileInputStream(config);
			}
		}
		
		// load parameters
		if(in != null){
			params.load(in);
		}else{
			System.err.println("Usage: java edu.pitt.dbmi.domainbuilder.DomainBuilder <config file | server url>");
			System.exit(1);	
		}
		
		// prompt for username and password if not present
		if( !params.containsKey("repository.username") || 
		    !params.containsKey("repository.password")){
			if(login != null && login.length == 3){
				params.setProperty("repository.username",login[0]);
				params.setProperty("repository.password",login[1]);
				params.setProperty("repository.institution",login[2]);
			}else{
				login = UIHelper.promptLogin(false);
				if(login != null && login.length == 3){
					params.setProperty("repository.username",login[0]);
					params.setProperty("repository.password",login[1]);
				}else{
					return;
				}
			}
		}
		
		
		// authenticate with server
		if(!Communicator.authenticateDatabase(params)){
			JOptionPane.showMessageDialog(null,"<html>Could not connect to DomainBuilder database at<br>" +
			"<font color=blue><u>"+params.getProperty("repository.url")+"</u></font><br> under usernamer" +
			" <font color=green>"+params.getProperty("repository.username")+"</font>","Error",JOptionPane.ERROR_MESSAGE);
			System.exit(1);	
		}
		
		
		// authenticate with website
		if(!Communicator.authenticateWebsite(getParameters())){
			JOptionPane.showMessageDialog(null,"<html>Could not register with the server " +
					"<font color=blue><u>"+params.getProperty("domain.builder.server.url")+"</u>"+
					"</font><br> under username <font color=green>"+params.getProperty("repository.username")+"</font>.<br>" +
					"You will NOT be able to upload example images, exported cases or ontologies.",
					"Warning",JOptionPane.WARNING_MESSAGE);
		}
		
		
		// reset domain builder server url
		if( params.containsKey("file.manager.server.url") && 
		    !params.containsKey("domain.builder.server.url")){
			String u = params.getProperty("file.manager.server.url");
			params.setProperty("domain.builder.server.url",u.substring(0,u.length()-OntologyHelper.FILE_MANAGER_SERVLET.length()));
		}
		
		
		// now start the program
		//SwingUtilities.invokeLater(new Runnable(){
		//	public void run(){
		DomainBuilder dbuilder = new DomainBuilder();
		dbuilder.start();
		//	}
		//});	
	}
	
	
}
