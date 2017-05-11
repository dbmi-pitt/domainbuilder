/**
 * This class draws a JLabel for each concept that it represents
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.Point;
import javax.swing.border.LineBorder;
import java.awt.Color;
import javax.swing.text.Position;
import java.awt.Cursor;
import java.awt.Font;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import edu.pitt.dbmi.domainbuilder.caseauthor.report.ReportPanel;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.terminology.lexicon.Concept;

//import java.io.Serializable;


public class ConceptLabel  implements Comparable{
	//extends JLabel implements Serializable , MouseListener, ActionListener, Comparable {
	//private static Font	textFont;
	//private static Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
	//private static float labelAlignment = .8f;


	//private List values;
	private SimpleAttributeSet attr;
	//private JLabel lbl;
	private String text;
	//private boolean underlined;
	private int offset; // offset within the document
	//private String definition,error;
	private Position pos;
	//private final Color recognizedColor = new Color(0,0,128);
	private transient JPopupMenu conceptMenu;
	private transient JMenuItem glossary,cut,copy,paste;
	private boolean debugStatus;
	private transient ReportPanel reportPanel;
	private Concept concept;
	private boolean deleted;
	
	// this variable controls blinking
	//boolean stopBlinking = true;
	
	/*
	static {
		String os = System.getProperty("os.name");
		//System.out.println("OS is "+os);
		if(os != null){
			if(os.startsWith("Windows")	)
				labelAlignment = .85f;
			else if(os.startsWith("Linux"))
				labelAlignment = .8f;	
		}
	}
	*/


	/**
	 * Create JLabel
	 */
	public ConceptLabel(Concept c){
		this(c.getText(),c.getOffset());
		this.concept = c;
	}
	
	/**
	 * create label from text and offset
	 * @param text
	 * @param offset
	 */
	public ConceptLabel(String text, int offset){
		this.text = text;
		this.offset = offset;
		

		// add create attr set
		attr = new SimpleAttributeSet();
		//StyleConstants.setComponent(attr,this);
		attr.addAttribute("concept",Boolean.TRUE);
		StyleConstants.setForeground(attr,Color.blue);
		attr.addAttribute("object",this);
	}
	
	
	/**
	 * create remove concept attribute
	 * @return
	 */
	public static AttributeSet getRemoveConceptAttribute(){
		SimpleAttributeSet a = new SimpleAttributeSet();
		a.removeAttribute("concept");
		StyleConstants.setForeground(a,Color.black);
		a.removeAttribute("object");
		return a;
	}
	
	
	public Concept getConcept() {
		return concept;
	}
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	/*
	public ConceptLabel(String text){
		this(text,0);	
	}
	*/
	
	/**
	 * Create JLabel
	 *
	public ConceptLabel(String text, int offset){
		super(text);
		this.text = text;
		this.offset = offset;
		
		setFont(textFont);
		setAlignmentY(labelAlignment);
		setCursor(handCursor);
		setMaximumSize(getPreferredSize());
		addMouseListener(this);
		
		//setOpaque(false);
		values = new ArrayList();
		
		// add create attr set
		attr = new SimpleAttributeSet();
		//StyleConstants.setComponent(attr,this);
		attr.addAttribute("concept",Boolean.TRUE);
		StyleConstants.setForeground(attr,Color.orange);
		attr.addAttribute("object",this);
		
	}
	*/

	public void setReportPanel(ReportPanel reportPanel) {
		this.reportPanel = reportPanel;
	}

	
	// make sure we stop the thread before we finish
	protected void finalize(){
		//stopBlinking = true;
	}
	
	
	/**
	 * get attribute set
	 */
	public AttributeSet getAttributeSet(){
		return attr;	 
	}
	
	/**
	 * set label text
	 *
	public void setText(String text){
		this.text = text;
		super.setText((underlined)?"<html><u>"+text+"</u></html>":text);
		/// THIS is a workaround for very stupid bug looking feature in JLabel
		setMaximumSize(getPreferredSize());		
	}
	*/
	
	/**
	 * get label text
	 */
	public String getText(){
		return text;
	}
	
	
	/**
	 * read errors from DataEntry and create a post-it error message
 	 *
	public void updateErrors(){
		if(isVisible()){
			StringBuffer buf = new StringBuffer("<center><b>MISTAKES</b></center>");
			error = null;
			
			for(Iterator k=values.iterator();k.hasNext();){
				DataEntry value = (DataEntry) k.next(); //getDataEntry();
				if(!value.isIgnored() && value.getMessages() != null && !value.getMessages().isEmpty()){
					List messages = value.getMessages(); 
					// this is the new post-it stuff
					for(Iterator i=messages.iterator();i.hasNext();){
						MessageEntry msg = (MessageEntry) i.next();
						// make sure those are error messages :)
						//msg.setHint(false);
						List texts = msg.getBugMessages();
						if(texts != null){
							for(int j=0;j<texts.size();j++){
								Object obj = texts.get(j);
								// display error as a postit
								TextPointer tp;
								if(obj instanceof TextPointer)
									tp = ( TextPointer ) obj;
								else
									tp = new TextPointer( obj.toString());
								
								//String lineBreak = "<hr>";
								//if(i == (messages.size()-1) && (j == (texts.size()-1)))
								//	lineBreak = "";
								buf.append("<hr>"+tp.getText());
							}
						}else{
							System.err.println("Bug messages not set in ConceptLabel");	
						}
					}
					error = "";
					//error =	 new FisPostit(this,Util.formatString(buf.toString()));
					//error = Util.formatString(buf.toString());
				}
				//else error = null;
			}
			
			if(error != null)
				error = Util.formatString(buf.toString());
			
			if(error == null){
				setToolTipText(null);
				// stop blinking
				stopBlinking = true;
			}else{
				setToolTipText(error);
				// start blinking if no other blinker is running
				if(stopBlinking){
					stopBlinking = false;
					if(ReportTutor.getConceptBlinkStatus())
						(new Blinker(this)).start();
				}
			}
		}
	}
	*/
	
	/**
	 * This method creates custom tooltip for errors vs definition
	 *
	public JToolTip createToolTip(){
		JToolTip tip = super.createToolTip();
		// we want a different background color for errors
		if(error != null){
			  tip.setBackground(Color.yellow);
			  tip.setBorder(new LineBorder(Color.black));
		}
		return tip;
	}
	
	*/
	
	
	/**
	 * set DataEntry associated with this label
	 * ConceptLabel can hold multiple DataEntries iff
	 * it is an ALL concept, or DataEntry has siblings
	 *
	public void setDataEntry(ConceptEntry e){
		if(values.size() > 0){
			// if this element is there, no point in doing anything
			if(values.contains(e))
				return;
			
			// get last element
			//ConceptEntry prev = (ConceptEntry) values.get(values.size()-1);
			
			//if previous element is fake that turned good, ex resolved attribute, then
			//forget the fake attribute
			//TODO:
			
		}
		// add new data entry
		values.add(e);
	}
	*/
	
	/**
	 * add DataEntry associated with this label
	 *
	public void addDataEntry(DataEntry e){
		if(!values.contains(e))
			values.add(e);
		//System.out.println("DataEntries in label "+values);
	}
	*/
	/**
	 * add DataEntry associated with this label
	 *
	public void addDataEntries(Collection e){
		values.addAll(e); 
	}
	*/
	/**
	 * set label text
	 */
	public void setColor(Color c){
		//setForeground((UIHelper.getConceptColorStatus())?c:recognizedColor);
		StyleConstants.setForeground(attr,c);
	}
	
	/**
	 * set label text
	 */
	public void setBackgroundColor(Color c){
		//setForeground((UIHelper.getConceptColorStatus())?c:recognizedColor);
		StyleConstants.setBackground(attr,c);
	}
	
	
	/**
	 * get label text
	 */
	public Color getColor(){
		//return getForeground();	
		return StyleConstants.getForeground(attr);
	}
	
	/**
	 * update in document
	 * @param doc
	 */
	public void update(StyledDocument doc){
		// check if this label is deleted, this is when what is in document doesn't match
		// what is in the label
		if(text != null){
			try{
				deleted = !text.equalsIgnoreCase(doc.getText(getOffset(),getLength()));
			}catch(BadLocationException ex){}
		}
		if(!isDeleted())
			doc.setCharacterAttributes(getOffset(),getLength(),attr,true);
	}
	
	/** 
	 * set definition
	 *
	public void setDefinition(String def){
		definition = def;
		//setToolTipText(def);
	}
	*/
	/**
	 * get text font
	 *
	public static Font getTextFont(){
		return textFont;	
	}
	*/
		
	/**
	 * Set text font for all labels
	 *
	public static void setTextFont(Font t){
		textFont = t;
	}
	*/

	
	/**
	 * get concept label offset
	 */
	public int getOffset(){
		if(pos != null)
			return pos.getOffset(); 
		return offset;
	}
	
	/**
	 * set postiont
	 */
	public void setPosition(Position p){
		pos = p;	 
	}

	
	/**
	 * Call attention to message window
	 *
	public void blink() {
		timer = new javax.swing.Timer(800, new ActionListener() {
			public void actionPerformed( ActionEvent evt ) {
			   timer.stop();
			   setForeground(color);
			}
		});
		setForeground(Color.WHITE);
		timer.start();
	}
	*/
	
	/**
	 * get concept label length
	 */
	public int getLength(){
		if(text == null)
			return 0;
		return text.length(); 
	}
	
	// std string method for debuging
	public String toString(){
		return text+" "+getOffset();	
	}
	// labels are equals if the have the same text and offset
	public boolean equals(Object obj){
		if(obj instanceof ConceptLabel){
			ConceptLabel lbl = (ConceptLabel) obj;
			return text.equals(lbl.getText()) && getOffset() == lbl.getOffset();
		}
		return false;
	}
	
	// std hash function
	public int hashCode(){
		String str = text+getOffset();
		return str.hashCode();
	}

	// create popup menu with list of concepts attached to this label
	private JPopupMenu createConceptMenu(){
		JPopupMenu menu = new JPopupMenu();
	
		glossary = new JMenuItem("Glossary",Icons.getIcon(Icons.INFO,16));
		glossary.addActionListener(reportPanel);
		menu.add(glossary);
		
		/*
		icon = new ImageIcon(getClass().getResource(helpIcon16));
		hint = new JMenuItem("Hint",icon);
		hint.addActionListener(this);
		menu.add(hint);
		menu.add(new JSeparator());
		
		icon = new ImageIcon(getClass().getResource(ignoreIcon16));
		ignore = new JCheckBoxMenuItem("Ignore Mistakes",icon,false);
		ignore.addActionListener(this);
		menu.add(ignore);
		*/
		menu.add(new JSeparator());
		
		cut = new JMenuItem("Cut",Icons.getIcon(Icons.CUT,16));
		cut.setActionCommand("cut");
		cut.addActionListener(reportPanel);
		menu.add(cut);
		
		copy = new JMenuItem("Copy",Icons.getIcon(Icons.COPY,16));
		copy.setActionCommand("copy");
		copy.addActionListener(reportPanel);
		menu.add(copy);		
		
		paste = new JMenuItem("Paste",Icons.getIcon(Icons.PASTE,16));
		paste.setActionCommand("paste");
		paste.addActionListener(reportPanel);
		menu.add(paste);
		
		// debug
		debugStatus = UIHelper.isDebug();
		/*
		if(UIHelper.isDebug()){
			menu.add(new JSeparator());
			JMenu concepts = new JMenu("Concepts");
			for(int i=0;i<values.size();i++){
				concepts.add(new JMenuItem(values.get(i).toString()));
			}
			menu.add(concepts);
		}
		*/
		return menu;
	}
	
	// which mode is context menu in?
	private void contextMode(boolean b){
		cut.setEnabled(b);
		copy.setEnabled(b);
		//paste.setEnabled(true);
	}
	
	// get popup menu
	public JPopupMenu getConceptMenu(){
		if(conceptMenu == null || debugStatus != UIHelper.isDebug())
			conceptMenu = createConceptMenu();
		return conceptMenu;
	}
	
	// for glossary
	/*
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == glossary){
			JTextPane edit = reportPanel.getTextPanel();
					
			// create definition window
			Postit post = new Postit(edit,definition);		
			post.getToolTip().setBackground(new Color(250,250,250));
			
			// display popup tip
			Point p = getLocationOnScreen();
			SwingUtilities.convertPointFromScreen(p,edit.getRootPane().getLayeredPane());
			post.setLocation(p.x+10,p.y+20);
			post.display(true);
		}
			
	}
	*/
	
	/* mouse listeners */
	/*
	public void mouseClicked(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}	
	public void mouseExited(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
    public void mousePressed(MouseEvent e){
		if(SwingUtilities.isRightMouseButton(e)){
			// get popup menu if needed
			JPopupMenu menu = getConceptMenu();
			
			// text is selected then do one thing, else
			contextMode(reportPanel.getTextPanel().getSelectedText() != null);
			//hint.setEnabled(error != null);
			//ignore.setEnabled(error != null || ignore.isSelected());
			menu.show(this,e.getX(),e.getY());	
		}else if(SwingUtilities.isLeftMouseButton(e)){
			reportPanel.getTextPanel().setCaretPosition(getOffset()+getLength());
		}
	}
	*/
    /*
	private class Blinker extends Thread {
		ConceptLabel lbl;
		public Blinker(ConceptLabel l){
			super(ReportScanner.getLabelGroup(),"Label-"+l.getOffset());
			lbl = l;	
		}
		public void run(){
			while(!lbl.stopBlinking){
				try{
					if(UIHelper.getConceptBlinkStatus()){
						lbl.setForeground(Color.MAGENTA);
						sleep(800);
						lbl.setForeground(lbl.color);
						sleep(800);
					}else{
						sleep(1600);
					}
				}catch(InterruptedException ex){
						ex.printStackTrace();	
				}
			}
		}
	}
	*/
	
    
    public int compareTo(Object o) {
    	if(o instanceof ConceptLabel){
    		ConceptLabel l = (ConceptLabel) o;
    		return getOffset() - l.getOffset();
    	}
    	return 1;
	}
}	

