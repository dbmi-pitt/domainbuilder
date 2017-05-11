/**
 * This class describes case authoring component
 */
package edu.pitt.dbmi.domainbuilder.caseauthor;

import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;
import java.beans.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.domainbuilder.*;
import edu.pitt.dbmi.domainbuilder.beans.*;
import edu.pitt.dbmi.domainbuilder.caseauthor.report.ReportPanel;
import edu.pitt.dbmi.domainbuilder.util.Communicator;
import edu.pitt.dbmi.domainbuilder.util.FileRepository;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.widgets.CaseSelectorPanel;
import edu.pitt.dbmi.domainbuilder.widgets.DomainSelectorPanel;
import edu.pitt.dbmi.domainbuilder.widgets.ResourcePropertiesPanel;
import edu.pitt.ontology.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.RegionShape;
import edu.pitt.slideviewer.markers.Annotation;
import edu.pitt.terminology.Terminology;

/**
 * This class is responsible for case authoring
 * 
 * @author tseytlin
 */
public class CaseAuthor extends JPanel implements DomainBuilderComponent, ActionListener, ItemListener {
	private final Color DRAW_COLOR = Color.green;
	private JFrame viewerFrame;
	private JPanel viewerContainer,addSlideContainer;
	private JToggleButton viewerToggleButton;
	private JTabbedPane slideShapeTab;
	private JSplitPane viewerSplitPanel, mainSplitPanel;
	private SlideSelector slideSelector;
	private ShapeSelector shapeSelector;
	private ReportPanel reportPanel;
	private ConceptSelector diseaseList, diagnosticList, progrnosticList, immunoList, clinicalList, recommendationsList;
	private ConceptSelector[] selectors;
	private Viewer viewer;
	private CaseEntry caseEntry;
	private Frame frame;
	private JToggleButton cur_selection;
	private JToolBar viewerToolBar,toolBar;
	private IOntology knowledgeBase, caseBase;
	private JMenuBar menubar;
	private JMenu partMenu2;
	private File lastDir;
	private CaseLibrary caseLibrary;
	private Terminology terminology;
	private DomainSelectorPanel domainSelector;
	private JPanel exportOption;
	private ButtonGroup exportOptionGroup, caseStatusGroup,caseStatusGroup2,partsGroup1,partsGroup2;
	private boolean readOnly;
	private Color shapeColor;
	private JRadioButton clearStatus,clearStatus2;
	private JPopupMenu statusMenu,partMenu;
	private JToggleButton partsMenuButton;
	private boolean autoInfer,transformPressed;
	private Object cutCopySource;
	
	/**
	 * create new case author panel
	 */
	public CaseAuthor() {
		// initially load dummy case object
		// setCaseEntry(new CaseEntry());
		createGUI();
		try {
			caseLibrary = new CaseLibrary(DomainBuilder.getParameters());
		} catch (Exception ex) {
			// JOptionPane.showMessageDialog(getFrame(),"Problem with Case Library: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	/**
	 * load whatever resources one needs to get this piece working
	 */
	public void load() {
		setInfoPanelText();
	}

	/**
	 * load knowledge base
	 * 
	 * @param uri
	 */
	private void loadKnowledgeBase(URI uri) {
		// load kb
		IRepository repository = DomainBuilder.getRepository();
		caseBase = repository.getOntology(uri);
		knowledgeBase = OntologyHelper.getKnowledgeBase(caseBase);

		// enable concept selectors
		selectors = new ConceptSelector[] { diseaseList, diagnosticList, progrnosticList, clinicalList, immunoList, recommendationsList};
		for (ConceptSelector selector : selectors) {
			selector.setEnabled(true);
		}

		// load report panel
		// reportPanel.setTerminology(repository.getTerminology(OntologyHelper.LUCENE_TERMINOLOGY));
		terminology = null;
		// reportPanel.setTerminology(terminology);
		reportPanel.load();

		// open new case
		setReadOnly(false);
		if(!OntologyHelper.isReadOnly(caseBase)){
			doNew();
		}else{
			setReadOnly(true);
		}		
	}

	public Terminology getTerminology() {
		if (terminology == null) {
			terminology = knowledgeBase.getRepository().getTerminology(OntologyHelper.ONTOLOGY_TERMINOLOGY);
		}
		return terminology;
	}

	/**
	 * get report panel
	 * 
	 * @return
	 */
	public ReportPanel getReportPanel() {
		return reportPanel;
	}

	/**
	 * create GUI for case author panel
	 */
	private void createGUI() {
		setLayout(new BorderLayout());

		// create lists of selectors
		diseaseList = new ConceptSelector(this, OntologyHelper.DISEASES, "Diagnosis");
		diagnosticList = new ConceptSelector(this, OntologyHelper.DIAGNOSTIC_FEATURES, "Findings");
		progrnosticList = new ConceptSelector(this, OntologyHelper.PROGNOSTIC_FEATURES, "Report");
		clinicalList = new ConceptSelector(this, OntologyHelper.CLINICAL_FEATURES, "Clinical");
		immunoList = new ConceptSelector(this, OntologyHelper.ANCILLARY_STUDIES, "Ancillary");
		recommendationsList = new ConceptSelector(this, OntologyHelper.RECOMMENDATIONS, "Assessment");

		selectors = new ConceptSelector[] { diseaseList, diagnosticList, progrnosticList, clinicalList, immunoList,recommendationsList };
		for (ConceptSelector selector : selectors) {
			selector.addPropertyChangeListener(this);
			selector.setEnabled(false);
		}

		// create report panel
		reportPanel = new ReportPanel(this);

		// create concept container
		JPanel conceptContainers = new JPanel();
		conceptContainers.setLayout(new BoxLayout(conceptContainers, BoxLayout.X_AXIS));
		conceptContainers.add(diseaseList);
		conceptContainers.add(diagnosticList);
		conceptContainers.add(progrnosticList);
		conceptContainers.add(clinicalList);
		conceptContainers.add(immunoList);
		conceptContainers.add(recommendationsList);

		JScrollPane scroll = new JScrollPane(conceptContainers);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		Dimension d = diseaseList.getPreferredSize();
		scroll.setPreferredSize(new Dimension(d.width * 3, d.height + 20));
		scroll.getHorizontalScrollBar().setBlockIncrement(d.width);
		scroll.getHorizontalScrollBar().setUnitIncrement(d.width);
		
		// split between report and feature lists
		JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		topSplit.setLeftComponent(scroll);
		topSplit.setRightComponent(reportPanel);
		topSplit.setOneTouchExpandable(true);
		
		// top panel
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		top.add(getToolBar(), BorderLayout.WEST);
		top.add(topSplit, BorderLayout.CENTER);

		// initialize the viewer
		//ViewerFactory.setProperties(DomainBuilder.getParameters());
		
		// add the currently loaded institiution
		String place = getParameter("repository.institution");
		ViewerFactory.addProperties(place,DomainBuilder.getParameters());
		
		// load parameters for other institutions
		for(String p : OntologyHelper.getInstitutions()){
			if(!place.equals(p)){
				URL u = OntologyHelper.getConfigFile(p);
				if(u != null){
					Properties props = new Properties();
					try{
						InputStream is = u.openStream();
						props.load(is);
						is.close();
					}catch(Exception ex){
						continue;
					}
					ViewerFactory.addProperties(p,props);
				}
			}
		}
		//revert back to default location
		ViewerFactory.setPropertyLocation(place);
		
		// setup logo
		//setupLogo();

		// create virtual microscope panel
		Viewer viewer = ViewerFactory.getViewerInstance(getParameter("image.server.type"));
		viewer.setSize(500, 500);
		setViewer(viewer);
		
		viewerContainer = new JPanel();
		viewerContainer.setLayout(new OverlayLayout(viewerContainer));
		viewerContainer.add(viewer.getViewerPanel());
		
		JPanel viewerPanel = new JPanel();
		viewerPanel.setLayout(new BorderLayout());
		viewerPanel.add(getViewerToolBar(), BorderLayout.WEST);
		viewerPanel.add(viewerContainer, BorderLayout.CENTER);
		enableViewerToolBar(false);

		// create side panel
		slideSelector = new SlideSelector(this);
		shapeSelector = new ShapeSelector(this);
		shapeSelector.addPropertyChangeListener(this);
		slideShapeTab = new JTabbedPane();
		slideShapeTab.addTab("slides", slideSelector);
		slideShapeTab.addTab("shapes", shapeSelector);
		slideShapeTab.setMinimumSize(new Dimension(145, 145));
		
		JButton addSlide = new JButton("Add Digital Slide");
		addSlide.setToolTipText("Add Whole Slide Image to a Case");
		addSlide.setIcon(Icons.getIcon(Icons.PREVIEW));
		addSlide.setHorizontalTextPosition(SwingConstants.CENTER);
		addSlide.setVerticalTextPosition(SwingConstants.BOTTOM);
		addSlide.setPreferredSize(new Dimension(200,200));
		addSlide.setActionCommand("Add Slide");
		addSlide.addActionListener(slideSelector);

		addSlideContainer = new JPanel();
		addSlideContainer.setLayout(new GridBagLayout());
		addSlideContainer.setOpaque(false);
		addSlideContainer.add(addSlide, new GridBagConstraints());
		
		// bottom panel
		JSplitPane bottom = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		bottom.setLeftComponent(viewerPanel);
		bottom.setRightComponent(slideShapeTab);
		bottom.setResizeWeight(1);

		// main interface
		JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		main.setTopComponent(top);
		main.setBottomComponent(bottom);
		add(main, BorderLayout.CENTER);

		// disable components
		UIHelper.setEnabled(getToolBar(),new String [0],false);
		UIHelper.setEnabled(reportPanel.getToolBar(),new String [0],false);
		
		// save panels
		viewerSplitPanel = bottom;
		mainSplitPanel = main;
	}

	/**
	 * setup custom logo w/ a hint
	 */
	private void setupLogo() {
		ImageIcon icon = ViewerFactory.getLogoIcon();
		int xo = 10;
		int w = icon.getIconWidth() + xo * 2;
		int h = icon.getIconHeight();
		int o = 60;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.drawImage(icon.getImage(), xo, 0, null);
		g.setColor(new Color(255, 255, 255, 200));
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		g.fillRect(0, h - o - 20, w, 30);
		g.setColor(new Color(50, 50, 50, 200));
		g.drawString("use SLIDES panel on the right to add slides", 0, h - o);
		ViewerFactory.setLogoIcon(new ImageIcon(img, "Logo"));
	}

	/**
	 * get knowledge base
	 * 
	 * @return
	 */
	public IOntology getKnowledgeBase() {
		return knowledgeBase;
	}

	/**
	 * get knowledge base
	 * 
	 * @return
	 */
	public IOntology getCaseBase() {
		return caseBase;
	}

	/**
	 * Sets the viewer as main tutor viewer. Registers all appropriate listeners
	 * 
	 * @param Viewer
	 *            viewer
	 */
	private void setViewer(Viewer v) {
		// remove previous viewer if exists
		if (viewer != null) {
			viewer.removePropertyChangeListener(this);
			viewer.dispose();
			AbstractButton tr = getTransformButton();
			if(tr != null)
				tr.removeActionListener(this);
			viewer = null;
			System.gc();
		}
		viewer = v;
		viewer.addPropertyChangeListener(this);
		viewer.getViewerComponent().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_N){
					slideSelector.openNextSlide();
				}else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_P){
					slideSelector.openPreviousSlide();
				}
			}
		});
		
		// assign action listener to transform button
		AbstractButton tr = getTransformButton();
		if(tr != null)
			tr.addActionListener(this);
	}
	
	private AbstractButton getTransformButton(){
		for(int i=0;i<viewer.getViewerControlPanel().getComponentCount();i++){
			Component c = viewer.getViewerControlPanel().getComponent(i);
			if(c instanceof AbstractButton){
				if("transform".equals(((AbstractButton)c).getActionCommand())){
					return (AbstractButton) c;
				}
			}
		}
		return null;
	}

	
	
	// return appropriate image directory
	private String getImageDir() {
		String type = getParameter("image.server.type");
		String dir = ViewerFactory.getProperties().getProperty(type + ".image.dir");
		return (dir != null)?dir:getParameter("image.dir");
	}

	/**
	 * open slide that is currently loaded into slideMap
	 * 
	 * @param name
	 */
	public void openSlide(String slide) {
		openSlide(caseEntry.getSlide(slide));
	}

	/**
	 * open slide that is currently loaded into slideMap
	 * 
	 * @param name
	 */
	public void openSlide(SlideEntry slide) {
		if (slide == null)
			return;
		
		// default behaviour
		String filename = slide.getSlideName();

		// save location of previes slide
		SlideEntry currentImage = caseEntry.getCurrentSlide();
		if (viewer.hasImage() && currentImage != null && !slide.equals(currentImage)) {
			currentImage.setViewPosition(viewer.getViewPosition());
			viewer.closeImage();
		}

		// remember this slide
		currentImage = slide;
		caseEntry.setCurrentSlide(currentImage);

		// make sure this name is selected
		if (filename.equals(viewer.getImage()))
			return;

		// make sure we load from the right sourrce
		checkViewerSettings(slide);
		
		// check if we need to switch viewer based on image type
		String type = getParameter("image.server.type");
		String rtype = ViewerFactory.recomendViewerType(filename);
		if (rtype == null) {
			JOptionPane.showMessageDialog(frame, filename + " is not of supported type", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (!type.equals(rtype))
			switchViewer(rtype);

		// load image
		try {
			viewer.openImage(getImageDir() + slide.getSlidePath());
		} catch (ViewerException ex) {
			// ex.printStackTrace();
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// load some usefull params
		if (slide.getServer() == null)
			slide.setServer("" + viewer.getServer());
		//if (slide.getPath() == null)
		//	slide.setPath(getImageDir());
		if (slide.getType() == null)
			slide.setType(type);

		// load transforms
		viewer.getImageProperties().setImageTransform(slide.getImageTransform());
		viewer.update();
		
		
		// load annotations
		for (ShapeEntry entry : slide.getAnnotations()) {
			Annotation tm = entry.getAnnotation(viewer.getAnnotationManager());
			tm.setViewer(viewer);
			// tm.addPropertyChangeListener(this);
			// tm.setMovable(true);
			viewer.getAnnotationManager().addAnnotation(tm);
		}

		// play out loaded pointers
		// slide.activatePointers();

		// mark that slide has been opened
		currentImage.setOpened(true);

		// set chooser to what is displayed
		// slideChooser.setSelectedItem(name);
	}
	
	
	/**
	 * check viewer settings based on institution
	 */
	private void checkViewerSettings(SlideEntry e){
		
		// setup viewer instition for the first time
		if(TextHelper.isEmpty(getParameter("viewer.institution"))){
			setParameter("viewer.institution",getParameter("repository.institution"));
		}
		
		// make sure that the settings from correct institution are loaded
		// when opening a case from a different institution
		String place = caseEntry.getInstitution();
		String inst = getParameter("viewer.institution");
		
		
		// if case is from different institution then DomainBuilder AND it is a valid institution 
		//   OR
		// if slide is from different configuration AND it is a valid configuration
		if(!inst.equals(place) && getInstitutions().contains(place) && (e == null || TextHelper.isEmpty(e.getConfigurationName()))){
		  	// set this new institution as default viewer location
			ViewerFactory.setPropertyLocation(place);
			
			// reset viwer type
			setParameter("image.server.type","BLAH_BLAH");
			setParameter("viewer.institution",place);

		}else if( e != null && (!inst.equals(e.getConfigurationName()) && ViewerFactory.getPropertyLocations().contains(e.getConfigurationName()))){
		 	// set this new institution as default viewer location
			ViewerFactory.setPropertyLocation(e.getConfigurationName());
			
			// reset viwer type
			setParameter("image.server.type","BLAH_BLAH");
			setParameter("viewer.institution",e.getConfigurationName());
		}
		
		
		// if no slide add button else remove
		if(e == null){
			viewerContainer.add(addSlideContainer,0);
		}else{
			viewerContainer.remove(addSlideContainer);
		}
		viewerContainer.revalidate();
		viewerContainer.repaint();	
	}

	/**
	 * Replace a current viewer w/ a viewer of different type
	 * 
	 * @param type
	 */

	private void switchViewer(String type) {
		String dir = getParameter(type + ".image.dir");
		setParameter("image.server.type", type);
		if (dir != null)
			setParameter("image.dir", dir);
		else
			System.err.println("Error: image dir " + dir + " not found for type " + type);

		// create virtual microscope panel
		Viewer v = ViewerFactory.getViewerInstance(getParameter("image.server.type"));
		v.setSize(450, 400);
		// remove component
		viewerContainer.remove(this.viewer.getViewerPanel());
		setViewer(v);

		// replace component
		viewerContainer.add(v.getViewerPanel(),0);
		viewerContainer.revalidate();
	}

	// get property
	private String getParameter(String a) {
		String str = DomainBuilder.getParameters().getProperty(a);
		return (str != null) ? str.trim() : "";
	}

	// set parameter
	private void setParameter(String key, String val) {
		if (DomainBuilder.getParameters() != null)
			DomainBuilder.getParameters().setProperty(key, val);
	}

	private JToolBar getToolBar(){
		if(toolBar == null)
			toolBar = createToolBar();
		return toolBar;
	}
	
	/**
	 * create toolbar
	 */
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
		toolbar.add(UIHelper.createButton("new", "Create New Case", Icons.NEW, this));
		toolbar.add(UIHelper.createButton("open", "Open Existing Case", Icons.OPEN, this));
		if(Communicator.isConnected())
			toolbar.add(UIHelper.createButton("publish", "Save and Publish Case", Icons.PUBLISH, this));
		else
			toolbar.add(UIHelper.createButton("save", "Save Current Case", Icons.SAVE, this));
		toolbar.addSeparator();
		toolbar.add(UIHelper.createButton("import", "Import Case File", Icons.IMPORT, this));
		toolbar.add(UIHelper.createButton("export", "Export Case File", Icons.EXPORT, this));
		toolbar.addSeparator();
		
		
		// create status sub menu
		final JToggleButton st = UIHelper.createToggleButton("status","Set Case Status",Icons.STATUS,this);
		
		// case status sub menu
		statusMenu = new JPopupMenu();
		statusMenu.addPopupMenuListener(new PopupMenuListener(){
			public void popupMenuCanceled(PopupMenuEvent e) {}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						st.doClick();
					}
				});
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
			
		});
		
		caseStatusGroup2 = new ButtonGroup();
		statusMenu.add(UIHelper.createRadioMenuItem(STATUS_INCOMPLETE,"Case authoring is not done",
				  UIHelper.createSquareIcon(Color.red,16),false, caseStatusGroup2,this)); 
		statusMenu.add(UIHelper.createRadioMenuItem(STATUS_COMPLETE,"Case authoring is done",
				  UIHelper.createSquareIcon(Color.blue,16),false, caseStatusGroup2,this)); 
		statusMenu.add(UIHelper.createRadioMenuItem(STATUS_TESTED,"Case authoring has been tested",
				  UIHelper.createSquareIcon(Color.green,16),false, caseStatusGroup2,this)); 
		
		
	
		
		// blank status line
		clearStatus2 = new JRadioButton();
		clearStatus2.setActionCommand("none");
		caseStatusGroup2.add(clearStatus2);
	
		String tip = "<html>A case can have multiple pathologies when two or more unrelated diagnoses can be<br>" +
					"observed either on different slides or in different locations on the same slide.<br>" +
					"You can manage case part information from this menu";
		partsMenuButton = UIHelper.createToggleButton("parts",tip,Icons.CASE_PARTS,this);
		
		toolbar.add(st);
		toolbar.add(partsMenuButton);
		toolbar.addSeparator();
		toolbar.add(UIHelper.createButton("validate", "Validate", Icons.VALIDATE, this));
		return toolbar;
	}

	/**
	 * create toolbar
	 */
	private JToolBar createViewerToolBar() {
		JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
		toolbar.setBackground(Color.white);
		toolbar.add(UIHelper.createToggleButton("arrow", "Draw Arrow Annotation [Alt-A]", Icons.ARROW, 24,
				KeyEvent.VK_A, this));
		toolbar.add(UIHelper.createToggleButton("rectangle", "Draw Rectangle Annotation  [Alt-R]", Icons.RECTANGLE, 24,
				KeyEvent.VK_R, this));
		toolbar.add(UIHelper.createToggleButton("circle", "Draw Oval Annotation  [Alt-O]", Icons.CIRCLE, 24,
				KeyEvent.VK_O, this));
		toolbar.add(UIHelper.createToggleButton("polygon", "Draw Free Hand Annotation [Alt-P]", Icons.POLYGON, 24,
				KeyEvent.VK_P, this));
		toolbar.add(UIHelper.createToggleButton("ruler", "Draw Measurement Annotation [Alt-M]", Icons.RULER, 24,
				KeyEvent.VK_M, this));
		toolbar.addSeparator();
		toolbar.add(UIHelper.createToggleButton("screenshot", "Select Region for a Screenshot [Alt-S]",
				Icons.SCREENSHOT, 24, KeyEvent.VK_S, this));
		toolbar.add(Box.createGlue());
		viewerToggleButton = UIHelper.createToggleButton("expand", "Expand Viewer Window  [Alt-W]", Icons.EXPAND, 24,
				KeyEvent.VK_W, this);
		viewerToggleButton.setSelectedIcon(Icons.getIcon(Icons.COLLAPSE, 24));
		toolbar.add(viewerToggleButton);
		return toolbar;
	}

	/**
	 * get part menu
	 * @return
	 */
	private JPopupMenu getPartMenu(){
		if(partMenu == null){
			// case status sub menu
			JPopupMenu menu = new JPopupMenu("Parts");
			menu.addPopupMenuListener(new PopupMenuListener(){
				public void popupMenuCanceled(PopupMenuEvent e) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							partsMenuButton.doClick();
						}
					});
				}
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
				
			});
			
			partsGroup1 = new ButtonGroup();
			
			// add static content
			menu.add(UIHelper.createMenuItem("Add Other Pathology", "Add Other Pathology to a Case", Icons.PLUS, this));
			menu.add(UIHelper.createMenuItem("Remove Pathology", "Remove Pathology from the Case", Icons.MINUS, this));
			menu.addSeparator();
			menu.add(UIHelper.createRadioMenuItem("All Pathologies","Show All Pathologies",null,true, partsGroup1,this)); 
			
			// now add additional parts from case
			if(caseEntry != null){
				for(String p: caseEntry.getParts()){
					menu.add(UIHelper.createRadioMenuItem("Show "+p,"Show Only Concepts from "+p,null,false,partsGroup1,this)); 
				}
			}
			partMenu = menu;
		}
		return partMenu;
	
	}
	
	/**
	 * get instance of viewer toolbar
	 * 
	 * @return
	 */
	private JToolBar getViewerToolBar() {
		if (viewerToolBar == null)
			viewerToolBar = createViewerToolBar();
		return viewerToolBar;
	}

	/**
	 * enable/disable viewer toolbar
	 */
	private void enableViewerToolBar(boolean b) {
		JToolBar toolbar = getViewerToolBar();
		for (int i = 0; i < toolbar.getComponentCount(); i++)
			toolbar.getComponent(i).setEnabled(b);
	}

	/**
	 * take care of buttons
	 * 
	 * @param e
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand().toLowerCase();
		//System.out.println(cmd);
		if (cmd.equals("new")) {
			doNew();
		} else if (cmd.equals("open")) {
			doOpen();
		} else if (cmd.equals("save")) {
			doSave();
		} else if (cmd.equals("publish")) {
			doPublish();
		} else if (cmd.equals("save as")) {
			String s = UIHelper.showInputDialog(this, "Please enter case name?", Icons.getIcon(Icons.SAVE_AS, 24));
			if (s == null) 
				return;
			caseEntry.setName(s);
			doSave();
		} else if (cmd.equals("delete")) {
			doDelete();
		} else if (cmd.equals("validate")) {
			(new Thread(new Runnable() {
				public void run() {
					getProgressBar().setString("Inferring Diagnosis ...");
					setBusy(true);
					doInferDiagnosis(false);
					setBusy(false);
					doValidate();
				}
			})).start();
		} else if (cmd.equals("import")) {
			doImport();
		} else if (cmd.equals("export")) {
			doExport();
		} else if (cmd.equals(STATUS_INCOMPLETE) || cmd.equals(STATUS_COMPLETE) || cmd.equals(STATUS_TESTED)) {
			//doCaseStatus();
			caseEntry.setStatus(cmd);
			setCaseStatus(cmd);
			setCaseModified();
		} else if (cmd.equals("properties")) {
			doProperties();
			setCaseModified();
		} else if (cmd.equals("switch domains")) {
			doSwitchDomain();
		} else if (cmd.equals("auto infer findings")){
			setAutoInfer(((AbstractButton)e.getSource()).isSelected());
		} else if (cmd.equals("add other pathology")){
			doAddPart();
		} else if (cmd.equals("remove pathology")){
			doRemovePart();
		} else if (cmd.equals("all pathologies")){
			doShowPart(null);
		} else if (cmd.startsWith("show ")){			
			doShowPart(e.getActionCommand().substring("show ".length()).trim());
		} else if (cmd.equals("infer findings")){			
			doInferFindings();
		} else if (cmd.equals("infer diagnoses")){	
			doInferDiagnosis(true);
		} else if (cmd.equals("edit diagnoses")){	
			doEditDiagnosis();
		} else if(cmd.equals("cut")){
			if(reportPanel.getTextPanel().getSelectedText() != null){
				reportPanel.actionPerformed(e);
				cutCopySource = reportPanel;
			}else if(shapeSelector.getSelectedNodes().length > 0){
				shapeSelector.actionPerformed(e);
				cutCopySource = shapeSelector;
			}else{
				JOptionPane.showMessageDialog(getComponent(),"Nothing Selected","Warning",JOptionPane.WARNING_MESSAGE);
			}
			
		} else if(cmd.equals("copy")){
			if(reportPanel.getTextPanel().getSelectedText() != null){
				reportPanel.actionPerformed(e);
				cutCopySource = reportPanel;
			}else if(shapeSelector.getSelectedNodes().length > 0){
				shapeSelector.actionPerformed(e);
				cutCopySource = shapeSelector;
			}else{
				JOptionPane.showMessageDialog(getComponent(),"Nothing Selected","Warning",JOptionPane.WARNING_MESSAGE);
			}
		} else if(cmd.equals("paste")){
			if(reportPanel.equals(cutCopySource)){
				reportPanel.actionPerformed(e);
			}else if(shapeSelector.equals(cutCopySource)){
				shapeSelector.actionPerformed(e);
			}else{
				JOptionPane.showMessageDialog(getComponent(),"Clipboard is Empty","Warning",JOptionPane.WARNING_MESSAGE);
			}
		} else if (cmd.equals("transform")){
			transformPressed = true;
		} else {
			// check if there is an appropriate toggle button
			for (Component c : getViewerToolBar().getComponents()) {
				if (c instanceof JToggleButton) {
					JToggleButton bt = (JToggleButton) c;
					if (cmd.equals(bt.getActionCommand()))
						bt.doClick();
				}
			}
		}
	}

	/**
	 * get application frame
	 * 
	 * @return
	 */
	public Frame getFrame() {
		if (frame == null)
			frame = JOptionPane.getFrameForComponent(this);
		return frame;
	}

	/**
	 * drawing buttons
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JToggleButton) {
			JToggleButton tb = (JToggleButton) e.getSource();
			String comm = tb.getActionCommand();

			// there do exist OTHER uses for toggle buttons
			if ("expand".equals(comm)) {
				doExpandViewer(tb.isSelected());
				return;
			}else if("status".equals(comm)){
				if(tb.isSelected()){
					Dimension d = tb.getSize();
					statusMenu.show(tb,d.width,0);
				}
				return;
			}else if("parts".equals(comm)){
				if(tb.isSelected()){
					Dimension d = tb.getSize();
					getPartMenu().show(tb,d.width,0);
				}
				return;
			}

			// if no slide, do nothing
			if (!viewer.hasImage()) {
				tb.setSelected(false);
			}

			// handle drowing shapes

			// stop sketch if different shape is being drawn
			if (cur_selection != null && cur_selection != tb)
				stopSketch();
			cur_selection = tb;
			// if we are drawing, then
			if (cur_selection.isSelected()) {
				if (comm.equalsIgnoreCase("rectangle"))
					sketchShape(AnnotationManager.PARALLELOGRAM_SHAPE);
				else if (comm.equalsIgnoreCase("circle"))
					sketchShape(AnnotationManager.CIRCLE_SHAPE);
				else if (comm.equalsIgnoreCase("arrow"))
					sketchShape(AnnotationManager.ARROW_SHAPE);
				else if (comm.equalsIgnoreCase("polygon"))
					sketchShape(AnnotationManager.POLYGON_SHAPE);
				else if (comm.equalsIgnoreCase("ruler"))
					sketchShape(AnnotationManager.RULER_SHAPE);
				else if (comm.equalsIgnoreCase("screenshot")) {
					// take care of screenshot
					AnnotationManager mm = viewer.getAnnotationManager();
					Color c = new Color(128, 128, 128, 70);
					Annotation tm = mm.createAnnotation(AnnotationManager.REGION_SHAPE, null, c, false);
					tm.setCursor(AnnotationManager.createCursor("selection", Icons.SELECTION_CURSOR, new Point(0, 0)));
					mm.addAnnotation(tm);
					viewer.getAnnotationPanel().sketchAnnotation(tm);
				}
			} else {
				// remove screenshot marker
				Annotation m = viewer.getAnnotationPanel().getCurrentAnnotation();
				if (m != null && m instanceof RegionShape) {
					viewer.getAnnotationManager().removeAnnotation(m);
				}
				stopSketch();
			}
		}
	}

	/**
	 * listen for property changes
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String cmd = evt.getPropertyName();
		if (cmd.equals(OntologyHelper.KB_LOADED_EVENT)) {
			URI uri = (URI) evt.getNewValue();
			loadKnowledgeBase(uri);
			diseaseList.setRoot(OntologyHelper.DISEASES);
		} else if (cmd.equals(UIHelper.UI_CLOSING_EVENT)) {
			// doClose();
		} else if (cmd.equals(Constants.VIEW_CHANGE)) {
			// ViewPosition l = (ViewPosition) evt.getNewValue();
			// updateLocation(l.x,l.y,l.scale);
		} else if (cmd.equals(Constants.SKETCH_DONE)) {
			// finished drawing or took a screenshot
			Annotation marker = (Annotation) evt.getNewValue();
			if(shapeColor != null)
				marker.setColor(shapeColor);
			stopSketch();
			if (marker instanceof RegionShape) {
				doScreenshot(marker);
			} else {
				addAnnotation(marker);
			}
		} else if (cmd.equals(Constants.UPDATE_SHAPE)) {
			// update marker that was moved
			Annotation marker = (Annotation) evt.getNewValue();
			marker.setViewPosition(viewer.getViewPosition());
		} else if (cmd.equals(Constants.IMAGE_CHANGE)) {
			enableViewerToolBar(evt.getNewValue() != null);

			// init view loaction
			SlideEntry slide = caseEntry.getCurrentSlide();
			if (slide != null && slide.getViewPosition() != null) {
				viewer.setViewPosition(slide.getViewPosition());
			} else {
				viewer.getViewerController().resetZoom();
			}
		}else if (cmd.equals(Constants.IMAGE_TRANSFORM)){
			// image was transformed
			doTransform();
		} else if (cmd.equals(ConceptSelector.CONCEPT_ADDED) || cmd.equals(ConceptSelector.CONCEPT_REMOVED)){
			ConceptSelector s = (ConceptSelector) evt.getNewValue();
			if (s != null && isAutoInfer()) {
				doInference(s);
			}
			setCaseModified();
		} else if (cmd.equals(ConceptSelector.INFER_CONCEPTS)){
			ConceptSelector s = (ConceptSelector) evt.getNewValue();
			if (s != null) {
				doInference(s);
				setCaseModified();
			}
		} else if (cmd.equals(ConceptSelector.CONCEPT_NEGATED)) {
			ConceptEntry c = (ConceptEntry)evt.getNewValue();
			// handle negation 
			if(c != null){
				doConceptNegated(c);
				setCaseModified();
			}
			// do auto inference
			if(isAutoInfer()){
				ConceptSelector s = (ConceptSelector) evt.getOldValue();
				if (s != null && s != progrnosticList) {
					doInference(s);
				}
			}
		} else if (cmd.equals(ConceptSelector.CONCEPT_ASSERTED)) {
			ConceptSelector s = (ConceptSelector) evt.getNewValue();
			if (s != null && s == diseaseList && isAutoInfer()) {
				doInferFindings();
			}
			setCaseModified();
		} else if (cmd.equals(ConceptSelector.CONCEPT_SELECTED)) {
			// unselect others
			for (ConceptSelector s : selectors) {
				if (!s.equals(evt.getOldValue()))
					s.clearSelection();
			}
			ConceptEntry s = (ConceptEntry) evt.getNewValue();
			if (s != null)
				doSelectShapes(s);
		} else if (cmd.equals(ConceptSelector.CONCEPT_RECOMMENDATIONS_CHANGED)) {
			ConceptEntry s = (ConceptEntry) evt.getNewValue();
			if(s != null){
				for(ConceptEntry e: s.getRecommendations()){
					e.setAsserted(true);
					recommendationsList.addConceptEntry(e);
				}
			}
			setCaseModified();
		} else if (cmd.equals(ShapeSelector.SHAPE_SELECTED)) {
			// unselect others
			for (ConceptSelector s : selectors) {
				s.clearSelection();
			}
			doSelectConcepts("" + evt.getNewValue());
		} else if (cmd.equals(ShapeSelector.SHAPE_DELETED)) {
			doDeleteShape("" + evt.getNewValue());
		} else if (cmd.equals(ShapeSelector.SHAPE_RENAMED)) {
			doRenameShape("" + evt.getOldValue(), "" + evt.getNewValue());
		} else if (cmd.equals(OntologyHelper.CASE_KB_RELOADED_EVENT)) {
			//caseBase = (IOntology) evt.getNewValue();
			caseEntry.updateReferences(caseBase);
			// refresh reminology
			terminology = null;
			// reportPanel.setTerminology(caseBase.getRepository().getTerminology(OntologyHelper.LUCENE_TERMINOLOGY));
			// reportPanel.setTerminology(caseBase.getRepository().getTerminology(OntologyHelper.ONTOLOGY_TERMINOLOGY));
			reportPanel.reloadWorksheet();
		} else if (cmd.equals(OntologyHelper.CASE_OPEN_EVENT)) {
			String name = "" + evt.getNewValue();
			if (name.length() > 0 && !"null".equals(name)) {
				DomainBuilder.getInstance().setSelectedTab(this);

				// check current case
				if (!doCheckCase())
					return;

				CaseEntry entry = new CaseEntry(caseBase.getInstance(name));
				setCaseEntry(entry);
			}
		}else if(cmd.equals(ConceptSelector.CONCEPT_PART_ADDED)){
			ConceptEntry e = (ConceptEntry) evt.getNewValue();
			// infer parts for all relevant findings
			if(e.isDisease()){
				String part = (String)evt.getOldValue();
				doSetPartFromDisease(e,part,true);
				doFlashPart(part);
			}
		}else if(cmd.equals(ConceptSelector.CONCEPT_PART_REMOVED)){
			ConceptEntry e = (ConceptEntry) evt.getNewValue();
			// infer parts for all relevant findings
			if(e.isDisease()){
				doSetPartFromDisease(e,(String)evt.getOldValue(),false);
			}
		}

	}

	public boolean isAutoInfer() {
		return autoInfer;
	}

	public void setAutoInfer(boolean autoInfer) {
		this.autoInfer = autoInfer;
	}

	
	private void doTransform(){
		if(!transformPressed)
			return;
		
		// if we are modifiying dicom image
		if(viewer.getImage() != null){
			if("DICOM".equals(viewer.getImageMetaData().get("image.format"))){
				// if we have DICOM image then copy/paste brighness and contrast to all other slides
				ImageTransform it = viewer.getImageProperties().getImageTransform();
				// only if image transformation is different from what is in the file
				for(SlideEntry slide: caseEntry.getSlides()){
					if(!viewer.getImage().endsWith(slide.getSlideName())){
						slide.getImageTransform().setBrightness(it.getBrightness());
						slide.getImageTransform().setContrast(it.getContrast());
					}
				}	
			}
		}
		transformPressed = false;
	}
	
	/**
	 * set part info for all relevant findings
	 * @param diagnosis
	 */
	private void doSetPartFromDisease(ConceptEntry diagnosis, String part,boolean add){
		// features
		ILogicExpression exp = diagnosis.getConceptClass().getEquivalentRestrictions();
		Set<ConceptEntry> findings = new HashSet<ConceptEntry>();
		getFindings(exp, null, findings);
		
		// go over all concepts and for the ones that are in the rule
		// set the part information
		for(ConceptSelector s : getConceptSelectors()){
			for(ConceptEntry e: s.getConceptEntries()){
				for(ConceptEntry r: findings){
					if(r.getConceptClass().evaluate(e.getConceptClass())){
						// set part information
						if(add)
							e.addPart(part);
						else
							e.removePart(part);
					}
				}
			}
		}
	}
	
	
	/**
	 * Annotation params will be updated in the TutorImageView during the
	 * sketching and then it should be added to the AnnotationManager
	 * 
	 * @param shape
	 * @param c
	 * @param cur
	 * @param tb
	 */
	private Annotation sketchShape(int shape) {
		AnnotationManager annotationManager = viewer.getAnnotationPanel().getAnnotationManager();
		Annotation tm = annotationManager.createAnnotation(shape, true);
		// reset name
		String name = tm.getType();
		if (name.startsWith("Para"))
			name = "Rectangle";
		tm.setName(name + shapeSelector.getAnnotationNumber());
		shapeColor = tm.getColor();
		tm.setColor(DRAW_COLOR);
		// tm.setTag(caseEntry.getCurrentSlide().getSlideName());
		annotationManager.addAnnotation(tm);
		viewer.getAnnotationPanel().sketchAnnotation(tm);

		// make sure shape selector is visible
		if (shapeSelector.getParent() instanceof JTabbedPane) {
			((JTabbedPane) shapeSelector.getParent()).setSelectedComponent(shapeSelector);
		}

		return tm;
	}

	/**
	 * stop sketching shape This method is called by AnnotationViewPanel
	 */
	private void stopSketch() {
		if (cur_selection != null) {
			cur_selection.setSelected(false);
			cur_selection = null;
			stopSketch();
		}
		viewer.getAnnotationPanel().sketchDone();
	}

	/**
	 * Take a screenshot of selected region if region null, then entire screen
	 */
	public void doScreenshot(Annotation marker) {
		// get region if available
		Rectangle r = null;
		if (marker != null) {
			r = marker.getRelativeBounds();
			// remove marker
			viewer.getAnnotationManager().removeAnnotation(marker);
		}

		// open up dialog box
		ViewerHelper.SnapshotChooserPanel chooserPanel = new ViewerHelper.SnapshotChooserPanel();
		JFileChooser chooser = new JFileChooser(OntologyHelper.getDefaultImageFolder());
		chooser.setFileFilter(new ViewerHelper.JpegFileFilter());
		chooser.setAccessory(chooserPanel);
		chooser.setPreferredSize(new Dimension(550, 350));
		int returnVal = chooser.showSaveDialog(frame);

		// if approved
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			// select mode
			int mode = chooserPanel.getSelectedMode();

			// take snapshot
			Image img = viewer.getSnapshot(mode);

			// now get region
			if (r != null) {
				// get subimage
				if (img instanceof BufferedImage) {
					try {
						img = ((BufferedImage) img).getSubimage(r.x, r.y, r.width, r.height);
					} catch (RasterFormatException ex) {
						ex.printStackTrace();
					}
				}
			}
			try {
				ViewerHelper.writeJpegImage(img, chooser.getSelectedFile());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * This method is called by AnnotationViewPanel to add Annotation to
	 * internal hash table as well as protege instance
	 */
	public void addAnnotation(Annotation tm) {
		// hide markers for previous shape
		Annotation lastMarker = shapeSelector.getCurrentAnnotation();
		if (lastMarker != null && lastMarker.isMovable()) {
			lastMarker.setMovable(false);
		}

		// add listener if shape is movable
		if (tm.isMovable())
			tm.addPropertyChangeListener(this);

		// figure out optimal zoom
		// setOptimalZoom(tm);

		// add instance to Protege
		shapeSelector.addAnnotation(tm);

		// select instance
		// shapeInstanceSelected(inst);
		shapeSelector.setCurrentAnnotation(tm);
	}

	/**
	 * Get component
	 */
	public Component getComponent() {
		return this;
	}

	/**
	 * icon for this case
	 */
	public Icon getIcon() {
		return Icons.getIcon(Icons.NEW, 16);
	}

	/**
	 * get name of this component
	 */
	public String getName() {
		return "Case";
	}

	/**
	 * @return the caseEntry
	 */
	public CaseEntry getCaseEntry() {
		return caseEntry;
	}

	/**
	 * @param caseEntry
	 *            the caseEntry to set
	 */
	public void setCaseEntry(CaseEntry entry) {
		setCaseEntry(entry, true);
	}

	/**
	 * @param caseEntry
	 *            the caseEntry to set
	 */
	public void setCaseEntry(CaseEntry entry, boolean l) {
		final boolean load = l;
		this.caseEntry = entry;
		(new Thread(new Runnable() {
			public void run() {
				DomainBuilder.getInstance().getProgressBar().setString("Loading Case ...");
				setBusy(true);
				slideShapeTab.setSelectedIndex(0);

				// reset viewer
				viewer.closeImage();

				// reset report
				reportPanel.reset();

				// reset selectors
				for (ConceptSelector s : selectors)
					s.reset();

				
				//check viewer settings
				checkViewerSettings(null);
				
				
				// load slide and shape info
				if (load) {
					try {
						caseLibrary.loadCaseEntry(caseEntry);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					}
				}
				
				// add default part
				if(caseEntry.getParts().isEmpty())
					caseEntry.getParts().add(DEFAULT_PART);
				
				// remove last parts
				Component [] parts = partMenu2.getMenuComponents();
				for(int i=parts.length-1;i>=0;i--){
					if(parts[i] instanceof JRadioButtonMenuItem){
						JRadioButtonMenuItem item = (JRadioButtonMenuItem) parts[i];
						if(item.getText().startsWith("All"))
							break;
						else
							partMenu2.remove(item);
					}
				}
				
				// add parts to a menu
				for(String p: caseEntry.getParts()){
					partMenu2.add(UIHelper.createRadioMenuItem("Show "+p,"Show Only Concepts from "+p,null,false,partsGroup2,CaseAuthor.this)); 
				}
				

				// reset other components
				slideSelector.reset();
				shapeSelector.reset();

				// set status 
				setCaseStatus(caseEntry.getStatus());
				
				
				// getFrame().setTitle("DomainBuilder ["+caseEntry.getName()+"]");
				setBusy(false);
				setReadOnly(readOnly);
				
				// update info panel
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						// case is not modified
						caseEntry.setModified(false);
						setInfoPanelText();
					}
				});
				
				// display warnings
				if(caseEntry.hasWarnings()){
					displayWarnings(caseEntry.getWarnings());
				}
				
			
			}
		})).start();
	}

	
	/**
	 * programticly set case status
	 * @param status
	 */
	private void setCaseStatus(String status){
		// clear visible buttons
		clearStatus.setSelected(true);
		clearStatus2.setSelected(true);
		
		//find appropriate button if available
		for(ButtonGroup gr : new ButtonGroup [] {caseStatusGroup,caseStatusGroup2}){
			for(AbstractButton bt: Collections.list(gr.getElements())){
				if(bt.getActionCommand().equals(caseEntry.getStatus())){
					bt.setSelected(true);
					break;
				}
			}
		}
	}
	
	
	/**
	 * set info text
	 * 
	 * @param s
	 *
	private void setInfoPanelText(String s) {
		JLabel st = DomainBuilder.getInstance().getInfoLabel();
		//String color = OntologyHelpercaseEntry.getStatus();
		st.setText("<html>domain: <font color=blue><u>" + OntologyHelper.getKnowledgeBase(caseBase.getURI())
				+ "</u></font>&nbsp;&nbsp;&nbsp;&nbsp;case: <font color=green>" + s + "</font>");
	}
	*/
	
	
	public void setCaseModified(){
		if(caseEntry != null){
			caseEntry.setModified(true);
			setInfoPanelText();
		}
	}
	

	/**
	 * set info text
	 * 
	 * @param s
	 */
	private void setInfoPanelText() {
		StringBuffer text = new StringBuffer("<html>");
		UIHelper.printKnowledgeStatus(text,""+OntologyHelper.getKnowledgeBase(caseBase.getURI()));
		UIHelper.printCaseStatus(text,caseEntry);
		if(readOnly){
			text.append(" (read only)");
		}
		UIHelper.printUserStatus(text);
		
		DomainBuilder.getInstance().getInfoLabel().setText(text.toString());
		DomainBuilder.getInstance().getInfoLabel().repaint();
	}

	/**
	 * @return the viewer
	 */
	public Viewer getViewer() {
		return viewer;
	}

	/**
	 * display progress bar
	 * 
	 * @param b
	 */
	public void setBusy(boolean b) {
		DomainBuilder.getInstance().setBusy(b);
	}

	/**
	 * get progress bar
	 * 
	 * @return
	 */
	public JProgressBar getProgressBar() {
		return DomainBuilder.getInstance().getProgressBar();
	}

	/**
	 * expand viewer to full window
	 * 
	 * @param b
	 */
	private void doExpandViewer(boolean expand) {
		if (expand) {
			JFrame frame = new JFrame("DomainBuilder: Viewer Window");
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					viewerToggleButton.setSelected(false);
				}
			});
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(viewerSplitPanel, BorderLayout.CENTER);
			frame.pack();
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize(d.width - 70, d.height - 70);
			frame.setVisible(true);
			viewerFrame = frame;
		} else {
			mainSplitPanel.setBottomComponent(viewerSplitPanel);
			mainSplitPanel.revalidate();
			viewerFrame.dispose();
		}
	}

	/**
	 * do inference (that is infer either diagnostic findings) or possible
	 * diseases
	 */
	private void doInference(ConceptSelector s) {
		final ConceptSelector selector = s;
		(new Thread(new Runnable() {
			public void run() {
				boolean inferFindings = selector == diseaseList;
				boolean inferDiagnosis = selector == diagnosticList;
				
				// if not auto infer then switch things around
				if(!isAutoInfer()){
					inferDiagnosis = selector == diseaseList;
					inferFindings = selector != diseaseList;
				}
				
				if (inferFindings) {
					getProgressBar().setString("Inferring Findings ...");
					setBusy(true);
					doInferFindings();
					setBusy(false);
				} else if (inferDiagnosis) {
					getProgressBar().setIndeterminate(false);
					getProgressBar().setString("Inferring Diagnosis ...");
					setBusy(true);
					doInferDiagnosis(true);
					setBusy(false);
				}
			}
		})).start();
	}

	/**
	 * infer findings for given set of diseases
	 */
	private void doInferFindings() {
		
		// clear implied concepts
		for(ConceptSelector selector: selectors){
			if(selector != diseaseList)
				diagnosticList.removeImpliedConceptEntries();
		}
		
		// get inferred findings
		for (ConceptEntry diagnosis : diseaseList.getConceptEntries()) {
			if (diagnosis.isAsserted()) {
				// get necessary and sufficient restrictions to get diagnostic
				// features
				ILogicExpression exp = diagnosis.getConceptClass().getEquivalentRestrictions();
				Set<ConceptEntry> findings = new HashSet<ConceptEntry>();
				getFindings(exp, null, findings);
								
				// add stuff that might be inherited
				// TODO: how will this work w/ findings that are just necessary
				ArrayList<IRestriction> r = new ArrayList<IRestriction>();
				Collections.addAll(r, diagnosis.getConceptClass().getRestrictions(
						knowledgeBase.getProperty(OntologyHelper.HAS_FINDING)));
				Collections.addAll(r, diagnosis.getConceptClass().getRestrictions(
						knowledgeBase.getProperty(OntologyHelper.HAS_CLINICAL)));
				Collections.addAll(r, diagnosis.getConceptClass().getRestrictions(
						knowledgeBase.getProperty(OntologyHelper.HAS_ANCILLARY)));
				Collections.addAll(r, diagnosis.getConceptClass().getRestrictions(
						knowledgeBase.getProperty(OntologyHelper.HAS_NO_FINDING)));
				getFindings(r, findings);
				
				//diagnosticList.addConceptEntries((ConceptEntry[]) findings.toArray(new ConceptEntry[0]));
				for(ConceptEntry e : findings){
					// set part information
					e.setParts(diagnosis.getParts());
					
					// add to selectors
					for(ConceptSelector s : getConceptSelectors(e)){
						s.addConceptEntry(e);
					}
				}		
			}
		}
		
		// try to infer template when diagnoses is entered
		doInferTemplate();
	}

	/**
	 * try to invoke template
	 */
	public void doInferTemplate(){
		// get necessary restrictions for prognostic features 
		Set<ConceptEntry> prognostic = new LinkedHashSet<ConceptEntry>();
		IClass template = getMatchingTemplate(caseEntry);
		if(template != null){
			getPrognostic(template.getNecessaryRestrictions(), null, prognostic);
		
			// sort prognostic
			List<ConceptEntry> list = new ArrayList<ConceptEntry>(prognostic);
			Collections.sort(list,OntologyHelper.getOrderComparator(template));
		
			progrnosticList.addConceptEntries(list.toArray(new ConceptEntry[0]));
		}
	}

	/**
	 * get appropriate expression for a given diagnosis
	 * 
	 * @param diagnosis
	 * @return
	 */
	private IClass getMatchingTemplate(CaseEntry cas) {
		
		// get all classes in case
		ConceptEntry [] e = cas.getConceptEntries();
		IClass [] clses = new IClass [e.length];
		for(int i=0;i<clses.length;i++)
			clses[i] = e[i].getConceptClass();
		
		
		// remember last template, to select the closest match
		IClass template  = null; 
		LogicExpression lastExp = null;
		
		// iterate over available schemas
		for (IClass schema : knowledgeBase.getClass(OntologyHelper.SCHEMAS).getDirectSubClasses()) {
			
			// create an expression from a template (all of triggers should be ORed together
			LogicExpression exp = new LogicExpression(ILogicExpression.AND);
			for(IRestriction r : schema.getRestrictions(knowledgeBase.getProperty(OntologyHelper.HAS_TRIGGER))){
				exp.add(r.getParameter());
			}
			
			// if expression is satisfied, AND it has more terms then previous template, then select it
			if (evaluate(exp,clses) >= exp.size() && (lastExp == null || exp.size() > lastExp.size())){
				template = schema;
				lastExp = exp;
			}
		}
		
		// else return empty expression
		return template;
	}
	
	
	
	/**
	 * extract concept entries from expression and put them into a list
	 * 
	 * @param exp
	 * @return
	 */
	private Set<ConceptEntry> getFindings(ILogicExpression exp, IProperty prop, Set<ConceptEntry> list) {
		Set<ConceptEntry> orSet = null;
		if(exp.getExpressionType() == ILogicExpression.OR)
			orSet = new HashSet<ConceptEntry>();
		
		for (Object obj : exp) {
			if (obj instanceof IRestriction) {
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				getFindings(r.getParameter(), p, list);
			} else if (obj instanceof IClass && prop != null) {
				// convert class to a concept entry
				IClass c = (IClass) obj;
				ConceptEntry entry = new ConceptEntry(c);
				if (prop.getName().contains(OntologyHelper.HAS_NO_FINDING)) {
					entry.setAbsent(true);
					list.add(entry);
				}else if (prop.getName().contains(OntologyHelper.HAS_FINDING) || 
					prop.getName().contains(OntologyHelper.HAS_CLINICAL) ||
					prop.getName().contains(OntologyHelper.HAS_ANCILLARY)) {
					list.add(entry);
				}
				if(orSet != null)
					orSet.add(entry);
			} else if (obj instanceof ILogicExpression) {
				// recurse into expression
				getFindings((ILogicExpression) obj, prop, list);
			}
		}
		
		// setup alternatives
		if(orSet != null && !orSet.isEmpty()){
			for(ConceptEntry a: orSet){
				for(ConceptEntry b: orSet){
					if(!a.equals(b))
						a.getAlternativeConcepts().add(b.getText());
				}
			}
		}
		
		return list;
	}
	
	/**
	 * extract concept entries from expression and put them into a list
	 * 
	 * @param exp
	 * @return
	 */
	//private Set<ConceptEntry> getFindings(ILogicExpression exp, IProperty prop, Set<ConceptEntry> list) {
	//	return getFindings(exp,prop,list,null);
	//}

	/**
	 * extract concept entries from expression and put them into a list
	 * 
	 * @param exp
	 * @return
	 */
	private Set<ConceptEntry> getFindings(List<IRestriction> restrictions, Set<ConceptEntry> list) {
		for (IRestriction r : restrictions) {
			getFindings(r.getParameter(), r.getProperty(), list);
		}
		return list;
	}

	/**
	 * extract concept entries from expression and put them into a list
	 * 
	 * @param exp
	 * @return
	 */
	private Set<ConceptEntry> getPrognostic(ILogicExpression exp, IProperty prop, Set<ConceptEntry> list) {
		for (Object obj : exp) {
			if (obj instanceof IRestriction) {
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				getPrognostic(r.getParameter(), p, list);
			} else if (obj instanceof IClass && prop != null) {
				// convert class to a concept entry
				IClass c = (IClass) obj;
				ConceptEntry entry = new ConceptEntry(c);
				if (prop.getName().contains(OntologyHelper.HAS_PROGNOSTIC)
						&& c.hasSuperClass(knowledgeBase.getClass(OntologyHelper.PROGNOSTIC_FEATURES))) {
					list.add(entry);
				}
			} else if (obj instanceof ILogicExpression) {
				// recurse into expression
				getPrognostic((ILogicExpression) obj, prop, list);
			}
		}
		return list;
	}

	private void doEditDiagnosis(){
		Set<ConceptEntry> list = new LinkedHashSet<ConceptEntry>();
		list.add(caseEntry.getConceptEntry());
		for(ConceptEntry dx: caseEntry.getDiagnoses()){
			if(dx.isAsserted()){
				list.add(dx);
				// get parents with rules
				for(IClass p: dx.getConceptClass().getSuperClasses()){
					if(!isSystemClass(p) && isDisease(p) && !p.getEquivalentRestrictions().isEmpty())
						list.add(new ConceptEntry(p));
				}
			}
		}
		DomainBuilder.getInstance().firePropertyChange(OntologyHelper.CLEAR_DIAGNOSIS_EVENT,null,null);
		DomainBuilder.getInstance().firePropertyChange(OntologyHelper.OPEN_DIAGNOSIS_EVENT,null,list);
	}
	
	/**
	 * infer diagnosis for given findings
	 */
	private void doInferDiagnosis(boolean conjunction) {
		// conjunction = false;
		diseaseList.removeImpliedConceptEntries();

		// no need to do anything if we have no findings
		if (diagnosticList.isEmpty())
			return;

		// get list of all diseases
		IInstance inst = caseEntry.getInstance();
		IClass[] diseaseCls = getCaseBase().getClass(OntologyHelper.DISEASES).getSubClasses();
		// setup progress bar
		// getProgressBar().setIndeterminate(false);
		getProgressBar().setMinimum(0);
		getProgressBar().setMaximum(diseaseCls.length - 1);
		// do the work
		Set<ConceptEntry> list = new HashSet<ConceptEntry>();
		for (int i = 0; i < diseaseCls.length; i++) {
			ILogicExpression exp = diseaseCls[i].getEquivalentRestrictions();
			if (exp != null && !exp.isEmpty()) {
				//System.out.println(diseaseCls[i]);
				// set expression as CONJUNCTION so that partial evidence can be
				// evaluated
				if ((conjunction) ? evaluateExpression(exp, inst) : exp.evaluate(inst)) {
					IClass d = getKnowledgeBase().getClass(diseaseCls[i].getName());
					list.add(new ConceptEntry(d));
				}
			}
			getProgressBar().setValue(i);
		}
		// long time = System.currentTimeMillis();
		//diseaseList.addConceptEntries(list.toArray(new ConceptEntry[0]));
		// System.out.println("add diseases "+(System.currentTimeMillis()-time)+" ms");
		
		// if we are validating Dx, then we need to mark diagnosis that do not infer as invalid
		if(!conjunction){
			for(ConceptEntry e: diseaseList.getConceptEntries()){
				// if list doesn't contain concept class, then it is inconsistent
				e.setInconsistent(false);
				if(!list.contains(e)){
					e.setInconsistent(true);
				}
			}
			diseaseList.validate();
		}
		
		// now add implied entries
		for(ConceptEntry dx: list){
			// check if this new DX is subsumed by the old dx
			boolean include = true;
			for(ConceptEntry e: diseaseList.getConceptEntries()){
				if(!e.isInconsistent() && dx.getConceptClass().hasSubClass(e.getConceptClass())){
					include = false;
					break;
				}
			}
			if(include)
				diseaseList.addConceptEntry(dx);
		}
		diseaseList.validate();
		
	}

	/**
	 * evaluate expression
	 * 
	 * @param exp
	 * @param inst
	 * @return
	 */
	private boolean evaluateExpression(ILogicExpression exp, IInstance inst) {
		if (exp.getExpressionType() == ILogicExpression.OR) {
			for (Object o : exp) {
				if (evaluate(evaluate(o, inst), termCount(exp),inst))
					return true;
			}
			return false;
		} else {
			return evaluate(evaluate(exp, inst),termCount(exp), inst);
		}
	}

	/**
	 * count number of terms in expression
	 * @param exp
	 * @return
	 */
	private int termCount(ILogicExpression exp){
		//if we have an ANDed expression then we care about number of terms
		if(exp.getExpressionType() == ILogicExpression.AND){
			return exp.size();
		// if we have the ORed expression then we care about the smallest n
		// of its content
		}else if(exp.getExpressionType() == ILogicExpression.OR){
			int n = 777;
			for(Object o:exp){
				if(o instanceof ILogicExpression){
					int x = termCount((ILogicExpression) o);
					if(x < n)
						n = x;
				}
			}
			return n;
		}
		// default makes no sense
		return 777;
	}
	
	/**
	 * custom expression evaluation (to replace built in mechanism)
	 * 
	 * @param exp
	 * @param inst
	 */
	private int evaluate(Object exp, Object param) {
		int hits = 0;
		if (exp instanceof ILogicExpression) {
			ILogicExpression e = (ILogicExpression) exp;
			// check for not
			if(ILogicExpression.NOT == e.getExpressionType()){
				// invert the result
				//TODO: what is negation in this context?
				if(evaluate(e.getOperand(),param) <= 0){
					hits ++;
				}
			}else{
				// iterate over parameters
				for (Object obj : e) {
					hits += evaluate(obj, param);
				}
			}
			return hits;
		} else if (exp instanceof IRestriction && param instanceof IInstance) {
			IRestriction r = (IRestriction) exp;
			IInstance inst = (IInstance) param;
			Object[] values = inst.getPropertyValues(r.getProperty());
			if (values == null || values.length == 0)
				return 0;
			// if any of values fits, that we are good
			ILogicExpression value = r.getParameter();
			for (int i = 0; i < values.length; i++) {
				if (value.evaluate(values[i]))
					hits++;
			}
			return hits;
		} else if (exp instanceof IClass) {
			if(param instanceof IClass []){
				for(IClass c: (IClass []) param){
					if(((IClass) exp).evaluate(c))
						return 1;
					
				}
				return 0;
			}else
				return (((IClass) exp).evaluate(param)) ? 1 : 0;
		} else {
			return (exp.equals(param)) ? 1 : 0;
		}
	}

	/**
	 * evaluate based on number of hits (as oppose to logical operation)
	 * 
	 * @param hits
	 * @param inst
	 * @return
	 */
	private boolean evaluate(int hits, int expTerms, IInstance inst) {
		
		// we can be here only if we just processed expression
		IProperty p1 = inst.getOntology().getProperty(OntologyHelper.HAS_FINDING);
		IProperty p2 = inst.getOntology().getProperty(OntologyHelper.HAS_NO_FINDING);
		//IProperty p3 = inst.getOntology().getProperty(OntologyHelper.HAS_CLINICAL);
		//IProperty p4 = inst.getOntology().getProperty(OntologyHelper.HAS_ANCILLARY);
		
		// see if this instance has a value that fits this restriction
		Object[] v1 = inst.getPropertyValues(p1);
		Object[] v2 = inst.getPropertyValues(p2);
		//Object[] v3 = inst.getPropertyValues(p3);
		//Object[] v4 = inst.getPropertyValues(p4);
		//if(hits > 0)
		//	System.out.println(hits+" vs "+v1.length+"+"+v2.length+"+"+v3.length);
		// disable checking against clinical, this may cause FP, but won't cause FN
		return hits >= (v1.length + v2.length ) || hits >= expTerms;
	}

	/**
	 * check if case needs to be saved;
	 */
	private boolean doCheckCase() {
		if (caseEntry == null)
			return true;

		int r = JOptionPane.NO_OPTION;

		if (caseEntry.isModified()) {
			r = JOptionPane.showConfirmDialog(getFrame(), "Would you like to save the current case?", "Question",
					JOptionPane.YES_NO_CANCEL_OPTION);
		}

		if (r == JOptionPane.CANCEL_OPTION)
			return false;

		if (r == JOptionPane.YES_OPTION)
			doSave();
		else
			doClose();
			
		return true;
	}

	/**
	 * create new case
	 */
	private void doNew() {
		if (!doCheckCase())
			return;

		// remove previous new case instance
		IInstance inst = caseBase.getInstance(OntologyHelper.NEW_CASE);
		if (inst != null) {
			inst.delete();
			caseBase.flush();
		}
		// create new instace
		inst = caseBase.getClass(OntologyHelper.CASES).createInstance(OntologyHelper.NEW_CASE);
		setCaseEntry(new CaseEntry(inst));
	}

	/**
	 * create new case
	 */
	private void doOpen() {
		if (!doCheckCase())
			return;

		// now open new case
		CaseSelectorPanel selector = new CaseSelectorPanel(caseBase);
		selector.showChooserDialog();
		if (selector.isSelected()) {
			String name = "" + selector.getSelectedObject();
			CaseEntry entry = new CaseEntry(caseBase.getInstance(name));
			setCaseEntry(entry);
		}
	}

	/**
	 * edit properties
	 */
	private void doProperties() {
		ResourcePropertiesPanel panel = new ResourcePropertiesPanel(caseEntry.getInstance());
		int r = JOptionPane.showConfirmDialog(getFrame(), panel, "Properties", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (r == JOptionPane.OK_OPTION) {
			panel.saveProperties(caseEntry.getInstance());
		}
	}
	
	/**
	 * edit case status
	 */
	private void doCaseStatus(){
		JComboBox list = new JComboBox(new String [] {STATUS_INCOMPLETE, STATUS_COMPLETE, STATUS_TESTED});
		list.setSelectedItem(caseEntry.getInstance().getPropertyValue(caseBase.getProperty(HAS_STATUS)));
		list.setBorder(new TitledBorder("Edit Case Status"));
		int r = JOptionPane.showConfirmDialog(getFrame(),list, "Edit Case Status", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (r == JOptionPane.OK_OPTION) {
			String status = ""+list.getSelectedItem();
			//caseEntry.getInstance().setPropertyValue(caseBase.getProperty(HAS_STATUS),status);
			caseEntry.setStatus(status);
		}
	}

	/**
	 * handle negation of a concept
	 * @param e
	 */
	private void doConceptNegated(ConceptEntry e){
		boolean b = e.isAbsent();
		
		// if concept was negated, remove its complement 
		// and add it back as a negated concept
		e.setAbsent(!b);
		caseEntry.removeConceptClass(e);
		e.setAbsent(b);
		caseEntry.addConceptClass(e);
	}
	
	/**
	 * create new case
	 */
	private void doSave() {
		doSave(true);
	}
	/**
	 * create new case
	 */
	private void doSave(boolean interactive) {
		if(readOnly)
			return;
	
		// rename if necessary
		if (OntologyHelper.NEW_CASE.equals(caseEntry.getName())) {
			String s = UIHelper.showInputDialog(this, "Please enter case name?", Icons.getIcon(Icons.SAVE, 24));
			if (s == null) {
				return;
			}
			caseEntry.setName(s);
		}
		// validate
		// doInferDiagnosis(false);

		// sync report, the rest should be synced already
		caseEntry.setReport(reportPanel.getReport());
		caseEntry.save();

		// save annotations
		try {
			caseLibrary.saveCaseEntry(caseEntry);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}

		// getFrame().setTitle("DomainBuilder ["+caseEntry.getName()+"]");
		try {
			caseBase.flush();
			caseBase.save();
			
			// do automatic upload for file repository
			//if(DomainBuilder.getRepository() instanceof FileRepository){
				// figure out file location and upload it if repository is not database
				//File f = new File(getLocalRepositoryFolder(),caseBase.getURI().getPath());
					
				// do file upload operation
				//if(f.exists())
				//	UIHelper.upload(f);
			//	if(interactive)
			//		JOptionPane.showMessageDialog(getFrame(),"Don't forget to publish changes by exporting this case","Warning",JOptionPane.WARNING_MESSAGE);
			//}
			
			
			// JOptionPane.showMessageDialog(getFrame(),caseEntry.getName()+" saved!");
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(getFrame(), "Problem Saving Case", "Error", JOptionPane.ERROR_MESSAGE);
		}
		setInfoPanelText();
		// System.out.println(Arrays.toString(caseBase.getClass(OntologyHelper.CASES).getDirectInstances()));
		
		// notify that case was reloaded
		DomainBuilder.getInstance().firePropertyChange(OntologyHelper.CASE_KB_UPDATED_EVENT,null,caseBase);
	}

	/**
	 * delete this case
	 */
	private void doDelete() {
		int r = JOptionPane.showConfirmDialog(getFrame(),
				"Are you sure you want to delete case " + caseEntry.getName(), "Question", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (r == JOptionPane.YES_OPTION) {
			caseEntry.delete();
			// load slide and shape info
			try {
				caseLibrary.removeCaseEntry(caseEntry);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}

			caseEntry = null;
			doNew();
		}
	}

	/**
	 * create new case
	 */
	private void doImport() {
		final JFileChooser jf = new JFileChooser(lastDir);
		jf.setMultiSelectionEnabled(true);
		jf.setFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(OntologyHelper.CASE_SUFFIX);
			}

			public String getDescription() {
				return "Case Files (*" + OntologyHelper.CASE_SUFFIX + ")";
			}

		});
		if (JFileChooser.APPROVE_OPTION == jf.showOpenDialog(getFrame())) {
			(new Thread(new Runnable() {
				public void run() {
					if (!doCheckCase())
						return;
					File[] files = jf.getSelectedFiles();
					if (files.length == 1) {
						File f = files[0];
						if (f != null && f.canRead()) {
							String name = f.getName();
							if (name.endsWith(OntologyHelper.CASE_SUFFIX)) {
								name = name.substring(0, name.length() - OntologyHelper.CASE_SUFFIX.length());
							}
							int x = JOptionPane.YES_OPTION;
							if (caseBase.hasResource(name)) {
								x = JOptionPane.showOptionDialog(getFrame(), "Case " + name + " already exists in "
										+ caseBase.getName() + " domain\nWould you like to Merge or Overwrite?",
										"Overwrite", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
										null, new String[] { "Merge", "Overwrite", "Cancel" }, "Cancel");
							}
							if (x == JOptionPane.CANCEL_OPTION)
								return;
							boolean overwrite = (x == JOptionPane.NO_OPTION);
							setBusy(true);
							getProgressBar().setString("Importing Case " + f.getName() + ". Please Wait...");
							// remove previous new case instance
							IInstance inst = caseBase.getInstance(OntologyHelper.NEW_CASE);
							if (inst != null) {
								inst.delete();
								caseBase.flush();
							}
							// create new instace
							inst = caseBase.getClass(OntologyHelper.CASES).createInstance(OntologyHelper.NEW_CASE);
							caseEntry = new CaseEntry(inst);

							lastDir = f.getParentFile();
							try {
								// load shapes
								caseLibrary.loadCaseEntry(caseEntry);
							
								// load case info
								caseEntry.load(new FileInputStream(f), overwrite);
								
								// since case is just imported reset status
								caseEntry.setStatus(STATUS_INCOMPLETE);
								
								
								// flash case base 
								caseBase.flush();
							//	System.out.println(Arrays.toString(caseBase.getClass(CASES).getDirectInstances()));
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(getFrame(), "Problem importing case file "
										+ f.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
								ex.printStackTrace();
							}

							// load primary slide
							slideSelector.openSlide(caseEntry.getPrimarySlide());
						} else {
							JOptionPane.showMessageDialog(getFrame(), "File " + f + " cannot be read!", "Error",
									JOptionPane.ERROR_MESSAGE);
						}
						setCaseEntry(caseEntry, false);
						
						// display warnings (SHOULD BE TAKEN CARE OFF EARLIER)
						//if(caseEntry.hasWarnings()){
						//	displayWarnings(caseEntry.getWarnings());
						//}
						
					} else if (files.length > 1) {
						// import multiple cases
						JProgressBar progress = DomainBuilder.getInstance().getProgressBar();
						progress.setMinimum(0);
						progress.setMaximum(files.length);
						progress.setIndeterminate(false);
						setBusy(true);
						StringBuffer warnings = new StringBuffer();
						List<String> problemCases = new ArrayList<String>();
						for (int i = 0; i < files.length; i++) {
							if (files[i] != null && files[i].canRead()) {
								progress.setString("Importing Case " + files[i].getName() + "...");

								// remove previous new case instance
								IInstance inst = caseBase.getInstance(OntologyHelper.NEW_CASE);
								if (inst != null) {
									inst.delete();
								}
								// create new instace
								inst = caseBase.getClass(OntologyHelper.CASES).createInstance(OntologyHelper.NEW_CASE);
								caseEntry = new CaseEntry(inst);
								
								try {
									caseEntry.load(new FileInputStream(files[i]), true);
								} catch (IOException ex) {
									// JOptionPane.showMessageDialog(getFrame(),"Problem importing case file "+f.getAbsolutePath(),"Error",JOptionPane.ERROR_MESSAGE);
									problemCases.add(files[i].getName());
									ex.printStackTrace();
								}
								// append warnings, if any
								warnings.append(caseEntry.getWarnings());
								
								// since case is just imported reset status
								caseEntry.setStatus(STATUS_INCOMPLETE);
								
								// sync report, the rest should be synced
								// already
								caseEntry.save();
								caseBase.flush();
								
								// save annotations
								try {
									caseLibrary.saveCaseEntry(caseEntry);
								} catch (Exception ex) {
									JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Error",
											JOptionPane.ERROR_MESSAGE);
									ex.printStackTrace();
								}
								
								progress.setValue(i + 1);
								
								
							}

						}
						setBusy(false);
						try {
							caseBase.flush();
							caseBase.save();
							// JOptionPane.showMessageDialog(getFrame(),caseEntry.getName()+" saved!");
						} catch (IOntologyException ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(getFrame(), "Problem Saving Case", "Error",
									JOptionPane.ERROR_MESSAGE);
						}
						if(warnings.length() > 0){
							displayWarnings(warnings.toString());
						}else
							JOptionPane.showMessageDialog(getFrame(), "Import of muliple cases complete!");
						
					}
					
					// notify that case was reloaded
					DomainBuilder.getInstance().firePropertyChange(OntologyHelper.CASE_KB_UPDATED_EVENT,null,caseBase);
				}
			})).start();
		}
	}

	private void displayWarnings(String txt){
		JTextArea text = new JTextArea(12,70);
		text.setText(txt);
		text.setEditable(false);
		JOptionPane op = new JOptionPane(new JScrollPane(text),JOptionPane.WARNING_MESSAGE);
		JDialog d = op.createDialog(getFrame(),"Import Warnings");
		d.setModal(false);
		d.setResizable(true);
		d.setVisible(true);
	}
	
	
	/**
	 * get export option panel
	 * 
	 * @return
	 */
	private JComponent getExportOptionPanel() {
		if (exportOption == null) {
			exportOption = new JPanel();
			exportOption.setLayout(new BoxLayout(exportOption, BoxLayout.Y_AXIS));
			JRadioButton b1 = new JRadioButton("all cases", false);
			b1.setActionCommand("all");
			JRadioButton b2 = new JRadioButton("this case", true);
			b2.setActionCommand("this");
			exportOptionGroup = new ButtonGroup();
			exportOptionGroup.add(b1);
			exportOptionGroup.add(b2);
			exportOption.add(b2);
			exportOption.add(b1);
		}
		return exportOption;
	}

	private void doPublish(){
		// first save this file
		doSave(false);
	
		// is it connected
		if(!Communicator.isConnected())
			return;
		
		// do automatic upload for file repository
		if(DomainBuilder.getRepository() instanceof FileRepository){
			// upload the case file
			File f = new File(getLocalCaseFolder(knowledgeBase),caseEntry.getName()+CASE_SUFFIX);
			try{
				if(f.exists() && !isReadOnly(caseBase)){
					UIHelper.upload(f);
					if(((FileRepository)DomainBuilder.getRepository()).isServerMode()){
						UIHelper.delete(f);
					}
				}
			}catch(IOException ex) {
				JOptionPane.showMessageDialog(getFrame(), "Problem exporting case to file "+f.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			
			// figure out file location and upload it if repository is not database
			File fc = new File(getLocalRepositoryFolder(),caseBase.getURI().getPath());
			try{
				// do file upload operation
				if(fc.exists() && !isReadOnly(caseBase)){
					UIHelper.upload(fc);
					if(((FileRepository)DomainBuilder.getRepository()).isServerMode())
						UIHelper.delete(fc);
				}
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(getFrame(), "Problem exporting case to file "+fc.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}	
		
			
	}
	
	/**
	 * create new case
	 */
	private void doExport() {
		doSave(false);
		JFileChooser jf = new JFileChooser();
		jf.setFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(OntologyHelper.CASE_SUFFIX);
			}

			public String getDescription() {
				return "Case Files (*" + OntologyHelper.CASE_SUFFIX + ")";
			}

		});
		jf.setAccessory(getExportOptionPanel());
		if (lastDir == null)
			lastDir = jf.getFileSystemView().getDefaultDirectory();
		jf.setSelectedFile(new File(lastDir, caseEntry.getName() + OntologyHelper.CASE_SUFFIX));

		if (JFileChooser.APPROVE_OPTION == jf.showSaveDialog(getFrame())) {
			File f = jf.getSelectedFile();
			String action = exportOptionGroup.getSelection().getActionCommand();
			if ("this".equals(action)) {
				// current case is being exported
				if (f != null && f.getParentFile().canWrite() && !f.isDirectory()) {
					lastDir = f.getParentFile();
					int r = JOptionPane.YES_OPTION;
					if (f.exists()) {
						r = JOptionPane.showConfirmDialog(getFrame(), "File " + f.getAbsolutePath()
								+ " already exists. Overwrite?", "Question", JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE);
					}
					if (r == JOptionPane.YES_OPTION) {
						try {
							caseEntry.save(new FileOutputStream(f));
							// copy to its own discrete location
							if(!isReadOnly(caseBase))
								UIHelper.backup(f, new File(OntologyHelper.getLocalCaseFolder(knowledgeBase), f.getName()));
							
							// do automatic upload for file repository
							if(DomainBuilder.getRepository() instanceof FileRepository){
								// figure out file location and upload it if repository is not database
								File fc = new File(getLocalRepositoryFolder(),caseBase.getURI().getPath());
								// do file upload operation
								if(fc.exists() && !isReadOnly(caseBase)){
									UIHelper.upload(fc);
									if(((FileRepository)DomainBuilder.getRepository()).isServerMode())
										UIHelper.delete(fc);
								}
							}
							
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(getFrame(), "Problem exporting case to file "
									+ f.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
						}
						
						
						
					}
				} else {
					JOptionPane.showMessageDialog(getFrame(), "Invalid file selected " + f, "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			} else if ("all".equals(action)) {
				final File dir = (f.isDirectory()) ? f : f.getParentFile();

				(new Thread(new Runnable() {
					public void run() {
						IInstance[] cases = caseBase.getClass(OntologyHelper.CASES).getInstances();

						// export multiple cases
						JProgressBar progress = DomainBuilder.getInstance().getProgressBar();
						progress.setMinimum(0);
						progress.setMaximum(cases.length);
						progress.setIndeterminate(false);

						setBusy(true);
						for (int i = 0; i < cases.length; i++) {
							progress.setString("Exporting Case " + cases[i].getName() + " ...");
							CaseEntry e = new CaseEntry(cases[i]);
							try {
								caseLibrary.loadCaseEntry(e);
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Error",
										JOptionPane.ERROR_MESSAGE);
								ex.printStackTrace();
							}

							File f = new File(dir, e.getName() + OntologyHelper.CASE_SUFFIX);
							try {
								e.save(new FileOutputStream(f));
								// copy to its own discrete location
								UIHelper.backup(f,new File(OntologyHelper.getLocalCaseFolder(knowledgeBase),e.getName()+OntologyHelper.CASE_SUFFIX));
							} catch (IOException ex) {
								ex.printStackTrace();
							}
							progress.setValue(i + 1);
						}
						
						// do automatic upload for file repository
						if(DomainBuilder.getRepository() instanceof FileRepository){
							// figure out file location and upload it if repository is not database
							File fc = new File(getLocalRepositoryFolder(),caseBase.getURI().getPath());
							// do file upload operation
							if(fc.exists()){
								try {
									UIHelper.upload(fc);
									if(((FileRepository)DomainBuilder.getRepository()).isServerMode())
										UIHelper.delete(fc);
								}catch(IOException ex){
									ex.printStackTrace();
								}
							}
						}
						
						setBusy(false);
						JOptionPane.showMessageDialog(getFrame(), "Export of muliple cases into " + dir + " complete!");
					}
				})).start();
			}
		}
	}

	public void dispose() {
		doClose();
	}

	private void doClose() {
		// remove all created instances
		if (caseEntry != null && caseEntry.isNewCase()) {
			// delete all for now.
			// if(OntologyHelper.NEW_CASE.equals(caseEntry.getName()) ||
			// JOptionPane.YES_OPTION !=
			// JOptionPane.showConfirmDialog(getFrame(),
			// "Would you like to save case "+caseEntry.getName()+"?",
			// "Question",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)){
			caseEntry.getInstance().delete();
			// }else{
			// doSave();
			// }
		}
	}

	/**
	 * validate varies aspects of the case
	 */
	private void doValidate(){
		if(caseEntry != null){
			// make sure that all concept entries have valid annotations
			StringBuffer problems = new StringBuffer();
			boolean hasDx = false;
			for(ConceptEntry e: caseEntry.getConceptEntries()){
				// get all annotations
				List<String> shapes = new ArrayList<String>();
				shapes.addAll(e.getLocations());
				shapes.addAll(e.getExamples());
				for(String s: shapes){
					if(!shapeSelector.hasAnnotation(s)){
						problems.append("<li><b>"+e.getText()+"</b> points to non-existing annotation <b>"+s+"</b></li>");
					}
				}
				
				// make sure that Dx has rules
				if(e.isDisease()){
					hasDx = true;
					if(e.getFindings().isEmpty())
						problems.append("<li><b>"+e.getText()+"</b> has no diagnostic rules associated with it</li>");
				}
			}
			if(caseEntry.getConceptEntries().length == 0)
				problems.append("<li><b>"+caseEntry.getName()+"</b> does not have any associated concepts</li>");
			else if(!hasDx)
				problems.append("<li><b>"+caseEntry.getName()+"</b> does not have any diagnosis</li>");
			
			// make sure there are implide diagnosis
			List<ConceptEntry> inconsistent = new ArrayList<ConceptEntry>();
			for(ConceptEntry d: diseaseList.getConceptEntries()){
				if(d.isInconsistent()){
					inconsistent.add(d);
				}
			}
			// Dx is inconsistent
			if(!inconsistent.isEmpty()){
				problems.append("<li>The following diagnoses can NOT be implied by the tutor given the evidence authored: " +
						TextHelper.toText(inconsistent)+"</li>");
			}
			
			//  report about problems
			if(problems.length() > 0){
				UIHelper.HTMLPanel infoPanel =  new UIHelper.HTMLPanel();
				infoPanel.setPreferredSize(new Dimension(600, 500));
				infoPanel.setEditable(false);
				infoPanel.setText("<h2>Outstanding Problems:</h2><ul>"+problems+"</ul>");
				infoPanel.setCaretPosition(0);

				// display info in modal dialog
				JOptionPane op = new JOptionPane(new JScrollPane(infoPanel),JOptionPane.PLAIN_MESSAGE);
				JDialog d = op.createDialog(this,"Case Validation");
				d.setResizable(true);
				d.setModal(false);
				d.setVisible(true);
			}
		}
	}
	
	
	/**
	 * create new case
	 */
	private void doSwitchDomain() {
		if (domainSelector == null) {
			domainSelector = new DomainSelectorPanel(knowledgeBase.getRepository());
			domainSelector.setOwner(getFrame());
		}
		domainSelector.showChooserDialog();
		if (domainSelector.isSelected()) {
			if (caseEntry != null && caseEntry.isModified())
				doCheckCase();
			// reset();
			(new Thread(new Runnable() {
				public void run() {
					IOntology ont = (IOntology) domainSelector.getSelectedObject();
					DomainBuilder.getInstance().doLoad(ont.getURI());
				}
			})).start();
		}
	}

	public ConceptSelector getDiseaseList() {
		return diseaseList;
	}

	public ConceptSelector getDiagnosticList() {
		return diagnosticList;
	}

	public ConceptSelector getPrognosticList() {
		return progrnosticList;
	}

	/**
	 * get appropriate concept selector based on concept entry
	 * 
	 * @param e
	 * @return
	 */
	public ConceptSelector[] getConceptSelectors() {
		return selectors;
	}

	/**
	 * get appropriate concept selector based on concept entry
	 * 
	 * @param e
	 * @return
	 */
	public ConceptSelector getConceptSelector(ConceptEntry entry) {
		if (entry.isDisease()) {
			return getDiseaseList();
		} else if (entry.isDiagnosticFeature()) {
			return getDiagnosticList();
		} else if (entry.isPrognosticFeature()) {
			return getPrognosticList();
		} else if (entry.isClinicalFeature()) {
			return clinicalList;
		} else if (entry.isAncillaryStudy()) {
			return immunoList;
		} else if (entry.isRecommendation()) {
			return recommendationsList;
		}
		return null;
	}

	/**
	 * get appropriate concept selector based on concept entry
	 * 
	 * @param e
	 * @return
	 */
	public List<ConceptSelector> getConceptSelectors(ConceptEntry entry) {
		List<ConceptSelector> list = new ArrayList<ConceptSelector>();
		if (entry.isDisease()) {
			list.add(getDiseaseList());
		}
		if (entry.isDiagnosticFeature()) {
			list.add(getDiagnosticList());
		}
		if (entry.isPrognosticFeature()) {
			list.add(getPrognosticList());
		}
		if (entry.isClinicalFeature()) {
			list.add(clinicalList);
		}
		if (entry.isAncillaryStudy()) {
			list.add(immunoList);
		}
		
		if (entry.isRecommendation()) {
			list.add(recommendationsList);
		}
		
		return list;
	}

	/**
	 * select shapes of highlighted concept
	 * 
	 * @param e
	 */
	private void doSelectShapes(ConceptEntry e) {
		ArrayList<String> shapes = new ArrayList<String>();
		shapes.addAll(e.getLocations());
		shapes.addAll(e.getExamples());
		shapeSelector.doSelectShapes(shapes);
	}

	/**
	 * select shapes of highlighted concept
	 * 
	 * @param e
	 */
	private void doSelectConcepts(String shape) {
		for (ConceptSelector selector : selectors) {
			for (ConceptEntry entry : selector.getConceptEntries()) {
				if (entry.hasAnnotation(shape)) {
					selector.selectEntry(entry);
				}
			}
		}
	}

	/**
	 * select shapes of highlighted concept
	 * 
	 * @param e
	 */
	private void doDeleteShape(String shape) {
		for (ConceptSelector selector : selectors) {
			for (ConceptEntry entry : selector.getConceptEntries()) {
				entry.removeAnnotation(shape);
			}
		}
	}

	/**
	 * select shapes of highlighted concept
	 * 
	 * @param e
	 */
	private void doRenameShape(String old, String shape) {
		for (ConceptSelector selector : selectors) {
			for (ConceptEntry entry : selector.getConceptEntries()) {
				entry.replaceAnnotation(old, shape);
			}
		}
	}

	/**
	 * create menubar
	 */
	public JMenuBar getMenuBar() {
		if (menubar == null) {
			menubar = new JMenuBar();
			// file
			JMenu file = new JMenu("File");
			file.add(UIHelper.createMenuItem("New", "New Case", Icons.NEW, this));
			file.add(UIHelper.createMenuItem("Open", "Open Case", Icons.OPEN, this));
			if(Communicator.isConnected()){
				JMenuItem item = UIHelper.createMenuItem("Save", "Save and Publish Case", Icons.PUBLISH, this);
				item.setActionCommand("Publish");
				file.add(item);
			}else
				file.add(UIHelper.createMenuItem("Save", "Save Case", Icons.SAVE, this));
			file.add(UIHelper.createMenuItem("Save As", "Rename Case", Icons.SAVE_AS, this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Delete", "Delete Case", Icons.DELETE, this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Import", "Import Case from File", Icons.IMPORT, this));
			file.add(UIHelper.createMenuItem("Export", "Export Case to File", Icons.EXPORT, this));
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Switch Domains", "Open a new domain", Icons.BROWSE, this));
			file.addSeparator();
			
			// case status sub menu
			JMenu status = new JMenu("Case Status");
			status.setToolTipText("Edit Case Status");
			status.setIcon(Icons.getIcon(Icons.STATUS,16));
			
			caseStatusGroup = new ButtonGroup();
			status.add(UIHelper.createRadioMenuItem(STATUS_INCOMPLETE,"Case authoring is not done",
					  UIHelper.createSquareIcon(Color.red,10),false, caseStatusGroup,this)); 
			status.add(UIHelper.createRadioMenuItem(STATUS_COMPLETE,"Case authoring is done",
					  UIHelper.createSquareIcon(Color.blue,10),false, caseStatusGroup,this)); 
			status.add(UIHelper.createRadioMenuItem(STATUS_TESTED,"Case authoring has been tested",
					  UIHelper.createSquareIcon(Color.green,10),false, caseStatusGroup,this)); 
			
			// blank status line
			clearStatus = new JRadioButton();
			clearStatus.setActionCommand("none");
			caseStatusGroup.add(clearStatus);
			
			//file.add(UIHelper.createMenuItem("Case Status", "Edit Case Status", Icons.STATUS, this));
			//file.add(status);
			//file.add(UIHelper.createMenuItem("Properties", "Edit Case Properties", Icons.PROPERTIES, this));
			//file.addSeparator();
			file.add(UIHelper.createMenuItem("Exit", "Exit Domain Builder", null, DomainBuilder.getInstance()));

			// edit
			JMenu edit = new JMenu("Edit");
			edit.add(UIHelper.createMenuItem("Cut", "Cut", Icons.CUT, this));
			edit.add(UIHelper.createMenuItem("Copy", "Copy", Icons.COPY, this));
			edit.add(UIHelper.createMenuItem("Paste", "Paste", Icons.PASTE, this));
			//for (Component c : edit.getMenuComponents())
			//	c.setEnabled(false);

			// case
			JMenu cas = new JMenu("Case");
			cas.add(UIHelper.createMenuItem("Add Slide", "Add Slide", Icons.PLUS, slideSelector));
			cas.add(UIHelper.createMenuItem("Add Report", "Add Report", Icons.ADD, reportPanel));
			cas.add(UIHelper.createMenuItem("Add Tag", "Add Tag", Icons.TAG, shapeSelector));
			cas.addSeparator();
			cas.add(UIHelper.createMenuItem("Parse Report", "Extract Semantic Concepts from Report", Icons.RUN, reportPanel));
			cas.add(UIHelper.createMenuItem("Worksheet", "Open Worksheet", Icons.WORKSHEET, reportPanel));
			cas.addSeparator();
			cas.add(UIHelper.createMenuItem("Infer Findings", "Get Findings from the Knowledge Base that are Associated with Case Diagnosis", Icons.SEARCH, this));
			cas.add(UIHelper.createMenuItem("Infer Diagnoses","Get Diagnoses from the Knowledge Base that are Implied by Findings in this Case", Icons.SEARCH, this));
			cas.addSeparator();
			cas.add(UIHelper.createMenuItem("Edit Diagnoses", "Edit Case Diagnoses in Diagnosis Builder ", Icons.EDIT, this));
			cas.addSeparator();
			cas.add(status);
			
			partsGroup2 = new ButtonGroup();
			
			partMenu2 = new JMenu("Other Pathology");
			partMenu2.setToolTipText("<html>A case can have multiple pathologies when two or more unrelated diagnoses can be<br>" +
					"observed either on different slides or in different locations on the same slide.<br>" +
					"You can manage case part information from this menu");
			partMenu2.setIcon(Icons.getIcon(Icons.CASE_PARTS,16));
			partMenu2.add(UIHelper.createMenuItem("Add Other Pathology", "Add Other Pathology to a Case", Icons.PLUS, this));
			partMenu2.add(UIHelper.createMenuItem("Remove Pathology", "Remove Pathology from the Case", Icons.MINUS, this));
			partMenu2.addSeparator();
			partMenu2.add(UIHelper.createRadioMenuItem("All Pathologies","Show All Pathologies",null,true, partsGroup2,this)); 
			cas.add(partMenu2);
			
			//cas.add(getPartMenu());
			cas.addSeparator();
			cas.add(UIHelper.createMenuItem("Validate", "Validate Case", Icons.VALIDATE, this));
			cas.add(UIHelper.createMenuItem("Properties", "Edit Case Properties", Icons.PROPERTIES, this));
			//file.addSeparator();
			
			// annotation
			JMenu annotations = new JMenu("Annotations");
			annotations.add(UIHelper.createMenuItem("Arrow", "Draw Arrow Annotation [Alt-A]", Icons.ARROW, this));
			annotations.add(UIHelper.createMenuItem("Rectangle", "Draw Rectangle Annotation  [Alt-R]", Icons.RECTANGLE,
					this));
			annotations.add(UIHelper.createMenuItem("Circle", "Draw Oval Annotation  [Alt-O]", Icons.CIRCLE, this));
			annotations.add(UIHelper
					.createMenuItem("Polygon", "Draw Free Hand Annotation [Alt-P]", Icons.POLYGON, this));
			annotations.add(UIHelper.createMenuItem("Ruler", "Draw Measurement Annotation [Alt-M]", Icons.RULER, this));
			annotations.addSeparator();
			annotations.add(UIHelper.createMenuItem("Screenshot", "Select Region for a Screenshot [Alt-S]",
					Icons.SCREENSHOT, this));

			// tools
			JMenu tools = new JMenu("Tools");
			tools.add(UIHelper.createMenuItem("Domain Manager", "Domain Manager", Icons.ONTOLOGY, DomainBuilder.getInstance()));
			//tools.add(UIHelper.createMenuItem("Repository Manager", "Repository Manager", Icons.BROWSE, DomainBuilder.getInstance()));
			

			JMenu options = new JMenu("Options");
			options.add(UIHelper.createCheckboxMenuItem("Auto Spell Check", "Check spelling as you type", Icons.SPELL,
					true, reportPanel));
			options.add(UIHelper.createCheckboxMenuItem("Auto Infer Findings", "Automaticly Infer Findings and Diagnoses  ", Icons.DOWN,
					false, this));
			options.addSeparator();
			options.add(UIHelper.createMenuItem("Change Font Size", "Allows users to increase or decrease fonts size in all components", Icons.FONT,
					 DomainBuilder.getInstance()));
			
			// help
			JMenu help = new JMenu("Help");
			help.add(UIHelper.createMenuItem("Help", "DomainBuilder Manual", Icons.HELP, DomainBuilder.getInstance()));
			help.add(UIHelper.createMenuItem("About", "About DomainBuilder", Icons.ABOUT, DomainBuilder.getInstance()));

			menubar.add(file);
			menubar.add(edit);
			menubar.add(cas);
			menubar.add(annotations);
			menubar.add(tools);
			menubar.add(options);
			menubar.add(help);
		}
		return menubar;
	}

	/**
	 * @return the shapeSelector
	 */
	public ShapeSelector getShapeSelector() {
		return shapeSelector;
	}

	/**
	 * get slide selector
	 * 
	 * @return
	 */
	public SlideSelector getSlideSelector() {
		return slideSelector;
	}

	public CaseLibrary getCaseLibrary() {
		return caseLibrary;
	}

	/**
	 * check if case exists
	 * 
	 * @param slides
	 * @return
	 */
	public boolean checkCase(Object[] names) {
		final String cs = caseLibrary.getCaseForSlide(names);
		// if case is set and it is not the same case :)
		if (cs != null && !caseEntry.getName().equals(cs)) {
			// check if such instance exists
			IInstance inst = caseBase.getInstance(cs);
			if (inst != null) {
				int r = JOptionPane.showConfirmDialog(frame, "Case " + cs
						+ " associated with selected image already exists." + "\nWould you like to load it?",
						"Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (r == JOptionPane.YES_OPTION) {
					CaseEntry entry = new CaseEntry(inst);
					setCaseEntry(entry);
				}
			} else {
				(new Thread() {
					public void run() {
						setBusy(true);
						caseEntry.setName(cs);
						try {
							caseLibrary.loadCaseEntry(caseEntry);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						// reset other components
						reportPanel.reset();
						slideSelector.reset();
						shapeSelector.reset();
						setBusy(false);
					}
				}).start();

			}
			return false;
		}
		return true;
	}
	
	
	/**
	 * is this case base read-only
	 * @param b
	 */
	private void setReadOnly(boolean b){
		readOnly = b;
		String [] exceptions = new String []
		  {"open","export","properties","screenshots","exit",
		   "switch domains","expand","edit","tools","help"};
		
		// disable toolbar buttons
		UIHelper.setEnabled(getToolBar(),exceptions,!b);
		UIHelper.setEnabled(getViewerToolBar(),exceptions,!b);
		UIHelper.setEnabled(getMenuBar(), exceptions, !b);
		
		// iterate through selectors
		for(ConceptSelector s: selectors)
			s.setReadOnly(b);
		reportPanel.setReadOnly(b);
		slideSelector.setReadOnly(b);
		shapeSelector.setReadOnly(b);
		
	}
	
	
	/**
	 * add case part
	 */
	private void doAddPart(){
		if(caseEntry == null)
			return;
		
		String name = UIHelper.showInputDialog(this,"Add Other Pathology",Icons.getIcon(Icons.CASE_PARTS,24));
		if(name != null){
			caseEntry.getParts().add(name);
			partMenu = null;
			
			// add to menu
			if(getPartMenuItem(name) == null)
				partMenu2.add(UIHelper.createRadioMenuItem("Show "+name,"Show Only Concepts from "+name,null,false,partsGroup2,this));
			
			doShowPart(null);
		}
	}
	
	/**
	 * remove case part
	 */
	private void doRemovePart(){
		if(caseEntry == null)
			return;
		String name = UIHelper.showComboBoxDialog(this,"Remove Part",null,
					caseEntry.getParts().toArray(new String [0]),Icons.getIcon(Icons.CASE_PARTS,24));
		if(name != null){
			caseEntry.getParts().remove(name);
			partMenu = null;
			
			// take care of menu
			JRadioButtonMenuItem item = getPartMenuItem(name);
			if(item != null)
				partMenu2.remove(item);
			
			doShowPart(null);
		}
	}
	
	private JRadioButtonMenuItem getPartMenuItem(String name){
		// take care of menu
		for(int i=0;i<partMenu2.getMenuComponentCount();i++){
			if(partMenu2.getMenuComponent(i) instanceof JRadioButtonMenuItem){
				JRadioButtonMenuItem item = (JRadioButtonMenuItem) partMenu2.getMenuComponent(i);
				if(item.getText().endsWith(name)){
					return item;
				}
			}
		}
		return null;
	}
	
	/**
	 * show only concepts from 
	 */
	private void doFlashPart(String part){
		doShowPart(part);
		Timer t = new Timer(500,new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doShowPart(null);
			}
		});
		t.setRepeats(false);
		t.start();
	}
	
	
	/**
	 * show only concepts from 
	 */
	private void doShowPart(String part){
		if(caseEntry == null)
			return;
		
		// hide all nodes that don't belong
		for(ConceptSelector s: getConceptSelectors()){
			for(ConceptEntry e: s.getConceptEntries()){
				if(part == null){
					e.setHidden(false);
				}else{
					e.setHidden(!e.getParts().contains(part));
				}
			}
			s.repaint();
		}
		
		// make sure all buttons are selected
		for(ButtonGroup grp: Arrays.asList(partsGroup1,partsGroup2)){
			if(grp == null)
				continue;
			Enumeration<AbstractButton> bts = grp.getElements();
			while(bts.hasMoreElements()){
				AbstractButton bt = bts.nextElement();
				if(bt != null){
					if(bt.getText().startsWith("All") && part == null && !bt.isSelected())
						bt.setSelected(true);
					else if(part != null && bt.getText().endsWith(part) && !bt.isSelected())
						bt.setSelected(true);
				}
			}
		}
	}
	
}
