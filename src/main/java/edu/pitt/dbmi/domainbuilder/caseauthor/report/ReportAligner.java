package edu.pitt.dbmi.domainbuilder.caseauthor.report;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;



/**
 * align reports that are being imported
 * @author tseytlin
 *
 */
public class ReportAligner implements ActionListener {
	private final String TEMPLATE = "/resources/ReportTemplate.xml";
	private Component component;
	private JTextPane sourceReport,destinationReport;
	private ReportDocument sourceDoc,destinationDoc;
	private JPopupMenu popup;
	
	// help text
	private final String HELP_TEXT = "<html><table width=750 border=0><tr><td>" +
		"Align sections from the imported (source) report to the target report template by either-" +
		"<br>1.Copying/pasting text from source report into relevant sections "+
		"of the target report. <br>2.Highlighting relevant text in the source report, right clicking on the highlighted text "+
		"and selecting an apporpriate section of the target report."+
		"</td></tr></table>&nbsp;";
		
	
	/**
	 * create UI
	 * @return
	 */
	public Component getComponent(){
		if(component == null){
			// create source report
			sourceReport = new JTextPane();
			sourceReport.setAutoscrolls(true);
	        sourceDoc = new ReportDocument(sourceReport);
	        //sourceDoc.loadTemplate(getClass().getResourceAsStream(TEMPLATE));
	        sourceReport.setDocument(sourceDoc);
			sourceReport.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e) {
					if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
						getPopupMenu().show(sourceReport,e.getX(),e.getY());
					}
				}
				
			});
	        
	        JPanel sPanel = new JPanel();
	        sPanel.setLayout(new BorderLayout());
	        sPanel.setPreferredSize(new Dimension(400,600));
	        JLabel stitle = new UIHelper.Label("<html><h3>Source Report</h3>");
	        stitle.setHorizontalAlignment(JLabel.CENTER);
	        sPanel.add(stitle,BorderLayout.NORTH);
	        sPanel.add(new JScrollPane(sourceReport),BorderLayout.CENTER);
	       
			
			// create source report
			destinationReport = new JTextPane();
			destinationReport.setAutoscrolls(true);
			destinationDoc = new ReportDocument(destinationReport);
			destinationDoc.loadTemplate(getClass().getResourceAsStream(TEMPLATE));
	        destinationReport.setDocument(destinationDoc);
			
			JPanel dPanel = new JPanel();
			dPanel.setLayout(new BorderLayout());
			dPanel.setPreferredSize(new Dimension(400,600));
	        JLabel dtitle = new UIHelper.Label("<html><h3>Target Report</h3>");
	        dtitle.setHorizontalAlignment(JLabel.CENTER);
	        dPanel.add(dtitle,BorderLayout.NORTH);
	        dPanel.add(new JScrollPane(destinationReport),BorderLayout.CENTER);
			
			
			// put everything together
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			split.setLeftComponent(sPanel);
			split.setRightComponent(dPanel);
			split.setResizeWeight(0.5);
			
			// create header
			JPanel header = new JPanel();
			header.setLayout(new BorderLayout());
			JLabel l = new UIHelper.Label("<html><h1>Align Report to a Template</h1></html>");
			l.setHorizontalAlignment(JLabel.CENTER);
			header.add(l,BorderLayout.NORTH);
			JLabel text = new UIHelper.Label(HELP_TEXT);
			text.setHorizontalAlignment(JLabel.CENTER);
			header.add(text,BorderLayout.CENTER);
			
			
			// create main panel
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(header,BorderLayout.NORTH);
			panel.add(split,BorderLayout.CENTER);
			component = panel;
			
		}
		return component;
	}
	
	/**
	 * get context menu
	 * @return
	 */
	private JPopupMenu getPopupMenu(){
		if(popup == null){
			popup = new JPopupMenu();
			// create sections
			JMenuItem heading = UIHelper.createMenuItem("REPORT SECTIONS:","",null,null);
			heading.setEnabled(false);
			popup.add(heading);
			popup.addSeparator();
			for(String s : destinationDoc.getSections()){
				popup.add(UIHelper.createMenuItem(s,"Insert selected text into "+s,null,this));
			}
			
		}
		return popup;
	}
	
	/**
	 * insert selected text into appropriate heading
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		String text = sourceReport.getSelectedText();
		if(text != null && text.length() > 0){
			destinationDoc.insertText("\n"+text,cmd,false);
		}
		
	}
	
	/**
	 * set source report
	 * @param text
	 */
	public void setSourceReport(String text){
		getComponent();
		//sourceReport.setText(text);
		try{
			// insert all text as normal
			sourceDoc.setDefaultOp(true);
			sourceDoc.insertString(0,text,ReportDocument.getNormalTextStyle());
			sourceDoc.setDefaultOp(false);
			//sourceDoc.appendText(text);
			// guess headings
			Pattern pt = Pattern.compile("^([A-Za-z ]+:|\\[[A-Za-z ]+\\])$",Pattern.MULTILINE);
			Matcher mt = pt.matcher(text);
			while(mt.find()){
				String heading = mt.group();
				int offs = mt.start();
				sourceDoc.setCharacterAttributes(offs,heading.length(),ReportDocument.getHeaderTextStyle(),true);
			}
			
		}catch(Exception ex){}
		importReport(destinationDoc,text);
	}
	
	/**
	 * import report
	 * @param text
	 */
	private void importReport(ReportDocument doc, String text){
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
			doc.setText(txt,section);
		}
		doc.setDefaultOp(false);
	}
	
	/**
	 * get resulted text
	 * @return
	 */
	public String getTargetReport(){
		getComponent();
		return destinationReport.getText();
	}
	

}
