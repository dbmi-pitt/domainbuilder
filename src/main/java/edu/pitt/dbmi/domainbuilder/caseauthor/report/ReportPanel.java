package edu.pitt.dbmi.domainbuilder.caseauthor.report;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.caseauthor.CaseAuthor;
import edu.pitt.dbmi.domainbuilder.caseauthor.report.process.*;
import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.dbmi.domainbuilder.widgets.TreeDialog;
import edu.pitt.ontology.*;
import edu.pitt.terminology.Terminology;
import edu.pitt.text.tools.TextTools;

/**
 * report panel
 * @author tseytlin
 */
public class ReportPanel extends JPanel implements ActionListener {
	private final String TEMPLATE = "/resources/ReportTemplate.xml";
	private final String WORKSHEET_SECTION = "FINAL DIAGNOSIS";
	private JTextPane textPane;
	private ReportDocument doc;
	private ReportData reportData;
	private ReportSpellChecker spellChecker;
	private ReportScanner reportScanner;
	private JToolBar toolbar;
	private JPopupMenu contextMenu;
	private Terminology terminology;
	//private CaseEntry caseEntry;
	private TreeDialog treeDialog;
	private CaseAuthor caseAuthor;
	private JLabel status;
	private boolean readOnly;
	private JComponent worksheet;
	private List<AbstractButton> worksheetButtons, resetButtons;
	private File lastFile;
	private JTextField [] totalScoreText;

	public ReportPanel(CaseAuthor ca){
		super();
		this.caseAuthor = ca;
		setLayout(new BorderLayout());
		textPane = new JTextPane();
		textPane.setEditorKit(new ReportEditorKit(textPane));
		textPane.addMouseListener(new ReportMouseAdapter());
		textPane.setAutoscrolls(true);
        status = new JLabel(" ");
		doc = new ReportDocument(textPane);
		doc.loadTemplate(getClass().getResourceAsStream(TEMPLATE));
		doc.setStatusLabel(status);
		textPane.setDocument(doc);
		add(getToolBar(),BorderLayout.WEST);
        add(new JScrollPane(textPane),BorderLayout.CENTER);
        //add(status,BorderLayout.SOUTH);
	}
	
	/**
	 * load resources
	 * @param doc
	 */
	public void load(){
		URL server = null;
		String u = DomainBuilder.getParameter("text.tools.server.url");
		try{
			server = new URL(u);
		}catch(MalformedURLException ex){
			JOptionPane.showMessageDialog(textPane,"Invalid TextService URL "+u);
		}
		reportData = new ReportData(caseAuthor);
		
		doc.setTextTools(new TextTools(server));
		doc.setStatusLabel(DomainBuilder.getInstance().getStatusLabel());
		doc.setTerminology(terminology);
		doc.setReportData(reportData);
		
		spellChecker = new ReportSpellChecker(doc);
		reportScanner = new ReportScanner(doc);
		
		doc.addReportProcessor(spellChecker);
		//doc.addReportProcessor(reportScanner);
		
		// notify that case was modified
		doc.addReportProcessor(new ReportProcessor() {
			public void updateOffset(int offset) {}
			public void removeString(int offset, String str, String section) {
				caseAuthor.setCaseModified();
			}
			public void insertString(int offset, String str, String section) {
				caseAuthor.setCaseModified();
			}
			public void finishReport() {};
		});
		
		textPane.addCaretListener(doc);
		//textPane.addContainerListener(reportData);
		
		reloadWorksheet();
	}
	
	/**
	 * reset pannel
	 */
	public void reset(){
		if(treeDialog != null){
			treeDialog.dispose();
			treeDialog = null;
		}
		// empty out the repoprt panel
		doc.clear();
		
		// load report if available
		importReport(caseAuthor.getCaseEntry().getReport());
	}
	
	public void doSuggestTerm(){
		if(treeDialog == null){
			treeDialog = new TreeDialog(caseAuthor.getFrame());
			treeDialog.setTitle("Select Diagnositc Findings");
			treeDialog.setRoot(caseAuthor.getKnowledgeBase().getClass(OntologyHelper.CONCEPTS));
			treeDialog.setSelectionMode(TreeDialog.SINGLE_SELECTION);
		}
		String text = textPane.getSelectedText();
		treeDialog.setSearchText(text);
		treeDialog.setVisible(true);
		Object o = treeDialog.getSelectedObject();
		DomainBuilder.getInstance().firePropertyChange(OntologyHelper.SUGGEST_TERM_EVENT,""+o,text);
	}
	
	
	
	public Terminology getTerminology() {
		return terminology;
	}

	public void setTerminology(Terminology terminology) {
		this.terminology = terminology;
		if(doc != null)
			doc.setTerminology(terminology);
	}
		
	/**
	 * get text component
	 * @return
	 */
	public JTextPane getTextPanel(){
		return textPane;
	}
	
	/**
	 * get report document
	 * @return
	 */
	public ReportDocument getReportDocument() {
		return doc;
	}

	
	/**
	 * get toolbar
	 * @return
	 */
	public JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = createToolBar();
			setReadOnly(readOnly);
		}
		return toolbar;
	}
	
	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(){
		JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
		//toolbar.setBackground(Color.white);
		//toolbar.setFloatable(false);
		
		toolbar.add(UIHelper.createButton("Parse Report","Extract Semantic Concepts from Report",Icons.RUN,24,this));
		toolbar.addSeparator();
		toolbar.add(UIHelper.createButton("Worksheet","Worksheet",Icons.WORKSHEET,24,this));
		toolbar.add(UIHelper.createButton("spell","Spell Check",Icons.SPELL,24,this));
		toolbar.addSeparator();
		toolbar.add(UIHelper.createButton("Add Report","Add Report to Case",Icons.ADD,24,this));
		return toolbar;
	}
	
	/**
	 * get context menu
	 * @return
	 */
	private JPopupMenu getContextMenu(){
		if(contextMenu == null)
			contextMenu = createContextMenu();
		return contextMenu;
	}
	
	private JPopupMenu createContextMenu(){
		// create context menu
		JPopupMenu menu = new JPopupMenu();
		menu.add(UIHelper.createMenuItem("Cut","Cut Selection",Icons.CUT,this));
		menu.add(UIHelper.createMenuItem("Copy","Copy Selection",Icons.COPY,this));
		menu.add(UIHelper.createMenuItem("Paste","Paste Selection",Icons.PASTE,this));
		menu.addSeparator();
		menu.add(UIHelper.createMenuItem("Suggest Term","Suggest a new concept or synonym",Icons.NEW,this));
		
		return menu;
	}
	
	// which mode is context menu in?
	private void contextMode(boolean b){
		getContextMenu().getComponent(0).setEnabled(b);
		getContextMenu().getComponent(1).setEnabled(b);
		getContextMenu().getComponent(4).setEnabled(b);
	}
	
	/**
	 * actions
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("Parse Report")){
			doRun();
		}else if(cmd.equals("Worksheet")){
			doWorksheet();
		}else if(cmd.equals("spell")){
			spellChecker.doSpellCheck();
		}else if(cmd.equals("Add Report")){
			doImport();
		}else if(cmd.equals("Auto Spell Check")){
			if(((AbstractButton)e.getSource()).isSelected()){
				doc.addReportProcessor(spellChecker);				
			}else{
				doc.removeReportProcessor(spellChecker);
			}
		}else if(cmd.equals("Cut")){
			textPane.cut();
		}else if(cmd.equals("Copy")){
			textPane.copy();
		}else if(cmd.equals("Paste")){
			textPane.paste();
		}else if(cmd.equals("Suggest Term")){
			doSuggestTerm();
		}else if(e.getSource() instanceof JRadioButton){
			doTotalScore();
		}
	}
	
	/**
	 * mouse listener
	 * @author tseytlin
	 */
	private class ReportMouseAdapter extends MouseAdapter {
		public void mousePressed(MouseEvent e){
			if(readOnly)
				return;
			
			if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
				// text is selected then do one thing, else
				if(textPane.getSelectedText() != null){
					contextMode(true);
					getContextMenu().show(textPane,e.getX(),e.getY());

				}else{
					// move caret to position where I just clicked with left mouse button, now see if works
					textPane.setCaretPosition(textPane.viewToModel(e.getPoint()));
					// get popup menu if needed
					JPopupMenu menu = spellChecker.getSuggestionList(textPane.getCaretPosition());
					if(menu != null){
						menu.show(textPane,e.getX(),e.getY());
					}else{
						contextMode(false);
						getContextMenu().show(textPane,e.getX(),e.getY());
					}
				}
			}
		}
	}
	
	private void doRun(){
		(new Thread(new Runnable(){
			public void run(){
				reportScanner.scanDocument();
				reportData.processDocument();
				
				// now that new info got added,lets infor
				caseAuthor.doInferTemplate();
			}
		})).start();
	}
	
	private void doWorksheet(){
		JComponent worksheet = getWorksheet();
		if(worksheet instanceof JTabbedPane){
			JTabbedPane t = (JTabbedPane) worksheet;
			if(t.getTabCount() == 0){
				JOptionPane.showMessageDialog(caseAuthor.getFrame(),"Worksheet was not setup for current domain");
				return;
			}
		}
		
		/*
		int r = JOptionPane.showConfirmDialog(caseAuthor.getFrame(),worksheet,
		"Worksheet",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			doc.insertText(getWorksheetString(),WORKSHEET_SECTION,false);
		}
		*/
		
		// non-modal worksheet
		final JOptionPane op = new JOptionPane(worksheet,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
		JDialog d = op.createDialog(caseAuthor.getFrame(),"Worksheet");
		d.setResizable(true);
		d.setModal(false);
		d.addWindowListener(new WindowAdapter(){
			public void windowDeactivated(WindowEvent e) {
				Object o = op.getValue();
				if(o instanceof Integer && ((Integer)o).intValue() == JOptionPane.OK_OPTION)
					doc.insertText(getWorksheetString(),WORKSHEET_SECTION,false);
			}
		});
		d.setVisible(true);
		
	}
	
	private void doImport(){
		JFileChooser chooser = new JFileChooser(lastFile);
		int r = chooser.showOpenDialog(JOptionPane.getFrameForComponent(this));
		if(r == JFileChooser.APPROVE_OPTION){
			lastFile = chooser.getSelectedFile();
			
			if(!lastFile.exists() || !lastFile.canRead()){
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
						"Cannot open file "+lastFile.getAbsolutePath(),"Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			try{
				importReport(alignReport(UIHelper.getText(new FileInputStream(lastFile),null)));
			}catch(IOException ex){
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * give user an option to aligh reports interactively
	 * @param text
	 * @return
	 */
	private String alignReport(String text){
		ReportAligner ra = new ReportAligner();
		ra.setSourceReport(text);
		int r = JOptionPane.showConfirmDialog(DomainBuilder.getInstance().getFrame(),ra.getComponent(),"Allign Report to a Template",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		return (r == JOptionPane.OK_OPTION)?ra.getTargetReport():null;
	}
	
	
	/**
	 * import report
	 * @param text
	 */
	public void importReport(String text){
		if(text == null)
			return;
		
		// normalize section headings
		for(String section: doc.getSections()){
			text = text.replaceFirst("(?i)\\b"+section+":",section+":");
		}
		
		// extract section info
		doc.setDefaultOp(true);
		for(String section: doc.getSections()){
			String txt = TextHelper.getSectionText(section, text);
			if(txt == null || txt.length() == 0)
				txt = "\n\n";
			
			// replace carriege returns w/ newlines
			txt = txt.replaceAll("\r\n","\n");
			
			doc.setText(txt,section);
		}
		doc.setDefaultOp(false);
	}
	
	
	/**
	 * Create worksheet
	 */
	private JComponent createWorksheet() {
		JTabbedPane workPanel = new JTabbedPane();
		workPanel.setPreferredSize(new Dimension(500,500));
		// build worksheet
		worksheetButtons = new ArrayList<AbstractButton>();
		resetButtons = new ArrayList<AbstractButton>();
		
		
		IOntology ont = caseAuthor.getKnowledgeBase();
		IClass w = ont.getClass(OntologyHelper.WORKSHEET);
		IClass [] children = w.getDirectSubClasses();
		totalScoreText = new JTextField [ children.length];
		
		// iterate over worksheets
		for(int tab=0;tab < children.length; tab++){
			IClass work = children[tab];
			JPanel panel = new JPanel();
			// setup fonts
			Font defaultFont = panel.getFont();
			Font head = defaultFont.deriveFont(Font.BOLD);
			Font bold = defaultFont.deriveFont(Font.BOLD);
			Font plain = defaultFont.deriveFont(Font.PLAIN);

			// setup other panel attributes
			panel.setBackground(Color.white);
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			// get components
			IClass totalScore = null;
			
			// iterate over features
			for (IClass feature: work.getDirectSubClasses()) {
				
				// if this feature is a general number panel
				if(OntologyHelper.isNumber(feature)){
					totalScore = work;
				} else {
					// do normal worksheet entry
					JLabel featureLbl = createWorksheetLabel(feature,true);
					featureLbl.setFont(head);
					panel.add(featureLbl);
					ButtonGroup buttons = new ButtonGroup();
					
					// create a reset button that is not displayed
					JRadioButton bt = new JRadioButton("clear");
					buttons.add(bt);
					resetButtons.add(bt);
					
					// get direct children of scores
					IClass [] f = feature.getDirectSubClasses();
					Arrays.sort(f,new Comparator<IClass>(){
						public int compare(IClass o1, IClass o2) {
							return toText(o1).compareTo(toText(o2));
						}
					});
					
					// iterate over childrent
					for (IClass att: f){
						IClass [] a  = att.getDirectSubClasses();
						// iterate over grand children
						if (a.length > 0) {
							// sort
							Arrays.sort(a,new Comparator<IClass>(){
								public int compare(IClass o1, IClass o2) {
									return toText(o1).compareTo(toText(o2));
								}
							});
								
							JLabel attrLbl = createWorksheetLabel(att,false);
							attrLbl.setFont(bold);
							panel.add(attrLbl);
							for(int i=0;i<a.length;i++){
								AbstractButton button = createWorksheetButton(a[i]);
								button.setFont(plain);
								buttons.add(button);
								panel.add(button);
								worksheetButtons.add(button);
							}
						} else {
							AbstractButton button = createWorksheetButton(att);
							button.setFont(plain);
							buttons.add(button);
							panel.add(button);
							worksheetButtons.add(button);
						}
					}
					panel.add(new JLabel("   "));
				}
			}
			
			// add total score 
			if(totalScore != null){
				JLabel featureLbl = createWorksheetLabel(totalScore,true);
				featureLbl.setFont(head);
								
				// totalScore
				totalScoreText[tab] = new JTextField(10);
				totalScoreText[tab].setDocument(new UIHelper.IntegerDocument());
				totalScoreText[tab].setEditable(false);
				totalScoreText[tab].setBackground(Color.white);
				totalScoreText[tab].setMaximumSize(new Dimension(200,25));
				totalScoreText[tab].setHorizontalAlignment(JTextField.CENTER);
				totalScoreText[tab].setAlignmentX(CENTER_ALIGNMENT);
				
				// add panel
				//JPanel p  = new JPanel();
				//p.setLayout(new FlowLayout());
				//p.add(featureLbl);
				//p.add(totalScoreText);
				panel.add(featureLbl);
				panel.add(totalScoreText[tab]);
			}
			
			
			
			JScrollPane scroll = new JScrollPane(panel);
			scroll.setPreferredSize(new Dimension(500,500));
			scroll.getVerticalScrollBar().setUnitIncrement(30);
			workPanel.addTab(UIHelper.getPrettyClassName(work.getName()),scroll);
		}
		
		// put tab component in panel
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(workPanel,BorderLayout.CENTER);
		p.setPreferredSize(new Dimension(500,500));
		
		return p;
	}
	
	/**
	 * calculate sum of scores
	 */
	private void doTotalScore(){
		double n = 0;
		IOntology ont = caseAuthor.getKnowledgeBase();
		JTabbedPane tabPane = (JTabbedPane) getWorksheet();
		Container tab = (Container) tabPane.getSelectedComponent();
		JTextField total = totalScoreText[tabPane.getSelectedIndex()];
		
		if(total != null){
			for(AbstractButton bt: worksheetButtons){
				if(bt.isSelected() && UIHelper.contains(tab,bt)){
					// check if this thing is a number
					IClass cls = ont.getClass(bt.getActionCommand());
					if(cls != null && OntologyHelper.isNumber(cls)){
						double d = TextHelper.parseDecimalValue(toText(cls));
						if(d != OntologyHelper.NO_VALUE){
							n += d;
						}
					}
				}
			}
			
			// set total score
			total.setText(TextHelper.toString(n));
		}
	}
	
	
	/**
	 * convert class to string representation
	 * @param cls
	 * @return
	 */
	private String toText(IClass cls){
		String text = UIHelper.getPrettyClassName(cls.getName());
		// if class is numeric w/ concrete number, then get number
		if(OntologyHelper.isNumber(cls)){
			// extract number
			for(IClass c: cls.getDirectSuperClasses()){
				if(OntologyHelper.isNumber(c)){
					// convert to digits
					double d = TextHelper.parseDecimalValue(c.getName());
					if(d != OntologyHelper.NO_VALUE){
						text = TextHelper.toString(d);
					}
					break;
				}
			}
		}
		return text;
	}
	
	
	/**
	 * create button for worksheet
	 * @param cls
	 * @return
	 */
	private AbstractButton createWorksheetButton(IClass cls){
		// now create button
		JRadioButton button = new JRadioButton(TextHelper.formatString(toText(cls)+": "+cls.getDescription(), 80));
		button.setBackground(Color.white);
		button.setActionCommand(cls.getName());
		button.addActionListener(this);
		button.setAlignmentX(CENTER_ALIGNMENT);
		return button;
	}
	
	private JLabel createWorksheetLabel(IClass cls, boolean f){
		JLabel lbl;
		
		if(f)
			lbl = new UIHelper.Label(
					TextHelper.formatString("<b>"+ 
					UIHelper.getPrettyClassName(cls.getName()).toUpperCase()
					+":</b><br><i>" + cls.getDescription()+ "</i>", 60));
		else
			lbl = new UIHelper.Label(
					TextHelper.formatString("&nbsp;&nbsp;"+
					cls.getName() + ": "
					+ cls.getDescription(),80));
		lbl.setAlignmentX(CENTER_ALIGNMENT);
		return lbl;
	}
	
	/**
	 * get worksheet instance
	 * @return
	 */
	private JComponent getWorksheet(){
		if(worksheet == null)
			worksheet = createWorksheet();
		return worksheet;
	}
	
	public void reloadWorksheet(){
		worksheet = null;
	}
	
	/**
	 * clear worksheet
	 */
	public void clearWorksheetSelection() {
		if (resetButtons != null) {
			for (int i = 0; i < resetButtons.size(); i++) {
				JRadioButton button = (JRadioButton) resetButtons.get(i);
				button.setSelected(true);
			}
		}
	}
	
	/**
	 * get worksheet string
	 * @return
	 */
	public String getWorksheetString() {
		StringBuffer worksheetString = new StringBuffer();
		if (worksheetButtons != null) {
			JTabbedPane tabPane = (JTabbedPane) getWorksheet();
			Container tab = (Container) tabPane.getSelectedComponent();
			int sel = tabPane.getSelectedIndex();
			for (int i = 0; i < worksheetButtons.size(); i++) {
				JRadioButton button = (JRadioButton) worksheetButtons.get(i);
				// if selected tab, has this button
				if(UIHelper.contains(tab,button)){
					if (button.isSelected()) {
						String cui = button.getActionCommand();
						// add string
						if (cui != null) {
							String sep = (worksheetString.length() == 0) ? "" : ",";
							worksheetString.append(sep + UIHelper.getPrettyClassName(cui));
						} else
							System.err.println("ERROR: Could not find Concept for " + cui);
					}
				}
			}
			
			// add total score
			if(totalScoreText[sel] != null){
				worksheetString.append("\n"+tabPane.getTitleAt(sel)+": "+totalScoreText[sel].getText());
			}
		}
		return "\n"+worksheetString.toString();
	}

	
	/**
	 * get report 
	 * @return
	 */
	public String getReport(){
		return textPane.getText();
	}
	
	
	/**
	 * set read-only flag
	 * @param b
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		// disable buttons
		if(toolbar != null){
			UIHelper.setEnabled(toolbar,new String [0],!b);
		}
		// disable report
		doc.setReadOnly(b);
	}
}
