package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.FormView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.knowledge.OntologyAction;
import edu.pitt.dbmi.domainbuilder.knowledge.OntologySynchronizer;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;

/**
 * widget that wraps ConceptEntry and presents a panel 
 * @author tseytlin
 *
 */
public class ReportEntryWidget extends JEditorPane implements HyperlinkListener, ActionListener{
	private ConceptEntry entry;
	private int tableWidth ;
	private final String UP_STR = "up", DOWN_STR = "down";
	private final int POSSIBLE_VALUES_LINE_LIMIT = 5;
	private String childrenList = "";
	private boolean readOnly,showSummary = true;
	private JTextField counterField;
	
	//private final String UP_STR = "&uarr;", DOWN_STR = "&darr;";
	
	/**
	 * create new panel
	 */
	public ReportEntryWidget(ConceptEntry e){
		super();
		this.entry = e;
		setContentType("text/html; charset=UTF-8");
		HTMLDocument doc = (HTMLDocument) getDocument();
		doc.getStyleSheet().addRule("body { font-family: sans-serif;");
		setEditable(false);
		addHyperlinkListener(this);
		Dimension size = getToolkit().getScreenSize();
		tableWidth = (size.width <= 1024)?700:900;
		//loadEntry(e);
		
		// wana grab text field
		setEditorKit(new HTMLEditorKit(){
			private HTMLFactory factory = new HTMLFactory(){
				public View create(Element elem) {
					Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
					if (o instanceof HTML.Tag) {
						HTML.Tag kind = (HTML.Tag) o;
						if(kind == HTML.Tag.INPUT){
							return new FormView(elem){
								protected Component createComponent() {
									Component c = super.createComponent();
									if(c instanceof JTextField)
										setCounterField((JTextField) c);
									return c;
								}
							};
						}
					}
					return super.create(elem);
				}
			};
			public ViewFactory getViewFactory() {
				return factory;
			}
		});
	}
	
	/**
	 * reload the displayed entry
	 */
	public void load(){
		loadEntry(entry);
	}
	
	
	/**
	 * get concept entry
	 * @return
	 */
	public ConceptEntry getConceptEntry(){
		return entry;
	}
	
	/**
	 * load entry 
	 * @param e
	 */
	private void loadEntry(ConceptEntry e){
		//long time = System.currentTimeMillis();
		//ConceptEntry f = e.getFeature();
		//TODO: what to do when FAV is selected?
		String ctext  = null;
		if(counterField != null)
			ctext = counterField.getText();
		
		ConceptEntry f = e;
		StringBuffer b = new StringBuffer();
		b.append("<table border=0 width=\""+tableWidth+"\"><tr>");
		b.append("<td valign=top align=left width=\"20\"><form action=\"offset\" method=\"get\">");
		b.append("<input type=\"text\" size=\"2\"/></form></td>");
		b.append("<td valign=top align=left width=\"96%\"><b><font size=5>"+f.getText()+"</font></b>");
		b.append("  [<a href=\"edit\">properties</a>] [<a href=\"remove\">remove</a>]");
		b.append("&nbsp;&nbsp;&nbsp;&nbsp;[<a href=\"up\">"+UP_STR+"</a>][<a href=\"down\">"+DOWN_STR+"</a>]<p></td></tr>");
		// </td><td valign=top align=right>
		if(showSummary){
			b.append("<tr><td colspan=2>"+f.getDescription()+"<br>");
			String children = loadChildren(e);
			if(children.length() > 0){
				// now make sure that the list is not to long
				String str = "</li>";
				int count = 0, offset = -1;
				for(int i = children.indexOf(str);i > -1;i = children.indexOf(str,i+1)){
					count ++;
					if(count <= POSSIBLE_VALUES_LINE_LIMIT)
						offset = i;
				}
				//System.out.println(e.getName()+" "+count+" "+offset+"\n"+children);
				// count is more then 5 lines
				if(count > POSSIBLE_VALUES_LINE_LIMIT){
					String text  = children.substring(0,offset+str.length());
					String ul = "";
					for(int i=0;i<TextHelper.getSequenceCount(text,"<ul>")-1;i++)
						ul += "</ul>";
					b.append(text+"<li>...</li>"+ul+
					"<li><a href=\"list\">click here to see af full list of possible values ..</a></li></ul>");
					childrenList = children;
				}else{
					b.append(children);
				}
			}else{
				b.append("Possible Values:<ul><li>present</li><li>absent</li></ul>");
			}
			if(e.getAction() != null)
				b.append("required action: <b>"+e.getAction().getText()+"</b> location(s) on the slide");
			else
				b.append("<font color=\"#FF9900\"><b>WARNING:</b> required action not specified</font>");
		}
		b.append("</td></tr></table>");
		setText(""+b);
		//System.out.println(e+" "+(System.currentTimeMillis()-time));
		repaint();
		
		// format the counter field if it was detected (it should)
		if(counterField != null && ctext != null){
			counterField.setText(ctext);
		}
	}
	
	/**
	 * set counter field
	 * @param textField
	 */
	private void setCounterField(JTextField textField){
		counterField = textField;
		counterField.addActionListener(this);
		counterField.setDocument(new UIHelper.IntegerDocument());
		counterField.setHorizontalAlignment(JTextField.CENTER);
		counterField.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				doUpdateLocation();
			}
			public void focusGained(FocusEvent e) {}
		});
	}
	
	
	/**
	 * set the order number of a component
	 * @param n
	 */
	public void setOrderNumber(int n){
		final int number = n;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(counterField != null)
					counterField.setText(""+number);
			}
		});
	
	}
	
	
	/**
	 * load children
	 * @param c
	 * @param b
	 */
	private String loadChildren(ConceptEntry entry){
		return loadChildren(entry,false);
	}

	/**
	 * load children
	 * @param c
	 * @param b
	 */
	private String loadChildren(ConceptEntry entry, boolean noAttributes){
		StringBuffer b = new StringBuffer();
		
		// try to get possible attributes first
		ConceptEntry [] attributes = (noAttributes)?new ConceptEntry [0]:entry.getPotentialAttributes();
		if(attributes.length > 0){
			b.append("Possible Values:<ul>");
			// sort into categories
			ArrayList<ConceptEntry> categories = new ArrayList<ConceptEntry>();
			ArrayList<ConceptEntry> values = new ArrayList<ConceptEntry>();
			for(ConceptEntry a: attributes){
				if(OntologyHelper.isAttributeCategory(a.getConceptClass()))
					categories.add(a);
				else
					values.add(a);
			}
			// go over categories
			for(ConceptEntry a: categories){
				b.append(a.getName()+":<ul>");
				for(ConceptEntry c: a.getChildren()){
					b.append("<li>"+getText(c)+"</li>");
					values.remove(c);
				}
				b.append("</ul>");
			}
			// cover remaining values
			for(ConceptEntry a: values){
				b.append("<li>"+getText(a)+"</li>");
			}
			b.append("</ul>");
		}else if(entry.getChildren().length > 0){
			b.append("<ul>"); //Possible Values
			for(ConceptEntry c: entry.getChildren()){
				b.append("<li>"+getText(c)+"</li>");
				b.append(loadChildren(c,true));
			}
			b.append("</ul>");
		}
		return b.toString();
	}
	
	/**
	 * return text for concept entry
	 * @param e
	 * @return
	 */
	private String getText(ConceptEntry e){
		StringBuffer b = new StringBuffer(e.getText());
		if(e.getDescription().length() > 0)
			b.append(" - "+e.getDescription());
		return b.toString();
	}
	
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if(readOnly)
			return;
		
		if(e.getEventType() == EventType.ACTIVATED){
			String cmd = e.getDescription();
			if(cmd.equals("edit")){
				doEdit();
			}else if(cmd.equals("remove")){
				doRemove();
			}else if(cmd.equals("up")){
				doUp();
			}else if(cmd.equals("down")){
				doDown();
			}else if(cmd.equals("list")){
				doList();
			}
		}
		
	}
	
	private void doList(){
		JEditorPane htmlPanel = new JEditorPane();
		htmlPanel.setContentType("text/html; charset=UTF-8");
		HTMLDocument doc = (HTMLDocument) htmlPanel.getDocument();
		doc.getStyleSheet().addRule("body { font-family: sans-serif;");
		htmlPanel.setEditable(false);
		htmlPanel.setText(childrenList);
		JScrollPane scroll = new JScrollPane(htmlPanel);
		scroll.setPreferredSize(new Dimension(500,500));
		JOptionPane.showMessageDialog(DomainBuilder.getInstance().getFrame(),scroll,
				entry.getName()+" Possible Values",JOptionPane.PLAIN_MESSAGE);
	}
	
	
	/**
	 * edit this element
	 */
	private void doEdit(){
		final ConceptEntry action = entry.getAction();
		int oact = getHash(action);
		final ConceptEntry [] attr = entry.getAttributes();
		int oatt = getHash(attr);
		
		// prompt user
		ReportEntryPanel.showReportEntryDialog(entry,DomainBuilder.getInstance().getFrame());
		loadEntry(entry);
		
		// send events now
		int nact = getHash(entry.getAction());
		int natt = getHash(entry.getAttributes());
		
		// action changes
		if(oact != nact){
			OntologySynchronizer.getInstance().addOntologyAction(new OntologyAction(){
				public void run(){
					IClass cls = entry.getConceptClass();
					ConceptEntry a = entry.getAction();
					IOntology ont = cls.getOntology();
					OntologyHelper.getConceptHandler(ont).addConceptAction(cls,(a!= null)?a.getConceptClass():null);
				}
				public void undo(){
					entry.setAction(action);
					loadEntry(entry);
				}
			});
		}
		// attribute changes
		if(oatt != natt){
			OntologySynchronizer.getInstance().addOntologyAction(new OntologyAction(){
				public void run(){
					IClass cls = entry.getConceptClass();
					ConceptEntry [] a = entry.getAttributes();
					IOntology ont = cls.getOntology();
					OntologyHelper.getConceptHandler(ont).createConceptAttributes(cls,a);
				}
				public void undo(){
					entry.setAttributes(attr);
					loadEntry(entry);
				}
			});
		}
		if(oact != nact || natt != natt)
			firePropertyChange("EDIT",null,entry);
	}
	
	
	/**
	 * get unique number to detect
	 * @param e
	 * @return
	 */
	private int getHash(Object o){
		return (o != null)?o.hashCode():0;
	}
	
	/**
	 * remove element
	 */
	private void doRemove(){
		int r = JOptionPane.showConfirmDialog(this,"Are you sure you want to remove this element?",
						"Question",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if(r == JOptionPane.YES_OPTION){
			Container cont = getParent();
			if(cont != null){
				firePropertyChange("REMOVE",null,entry);
				cont.remove(this);
				cont.validate();
				cont.repaint();
			}
		}
	}
	
	/**
	 * move component up
	 */
	private void doUp(){
		int n = Integer.parseInt(counterField.getText());
		if(n > 1){
			counterField.setText(""+(n-1));
			doUpdateLocation();
		}
		/*
		Container cont = getParent();
		if(cont != null){
			int i  = UIHelper.indexOf(cont.getComponents(),this);
			if(i> 0){
				cont.add(this,i-1);
				cont.validate();
				cont.repaint();
				firePropertyChange("UP",null,entry);
			}
		}*/
	}
	/**
	 * move component down
	 */
	private void doDown(){
		Container cont = getParent();
		if(cont != null){
			int n = Integer.parseInt(counterField.getText());
			if(n < cont.getComponentCount()-1){
				counterField.setText(""+(n+1));
				doUpdateLocation();
			}
			/*
			int i  = UIHelper.indexOf(cont.getComponents(),this);
			if(i < cont.getComponentCount() - 2){
				cont.add(this,i+1);
				cont.validate();
				cont.repaint();
				firePropertyChange("DOWN",null,entry);
				counterField.setText(""+(i+2));
			}
			*/
		}
	}
	
	public void setReadOnly(boolean b){
		readOnly = b;
	}


	public boolean isShowSummary() {
		return showSummary;
	}


	public void setShowSummary(boolean showSummary) {
		this.showSummary = showSummary;
	}

	private void doUpdateLocation(){
		int n = Integer.parseInt(counterField.getText())-1;
		Container cont = getParent();
		// if there is a parent and n is within range
		if(cont != null){
			// find current location
			int i  = UIHelper.indexOf(cont.getComponents(),this);
			if(i >= 0){
				// if new location is legit
				if(n >= 0 && n < cont.getComponentCount()-1){
					// switch components
					//Component old = cont.getComponent(n);
					cont.add(this,n);
					//cont.add(old,i);
									
					cont.validate();
					cont.repaint();
					
					// update all counts
					for(int x=0;x<cont.getComponentCount()-1;x++){
						if(cont.getComponent(x) instanceof ReportEntryWidget){
							((ReportEntryWidget)cont.getComponent(x)).setOrderNumber(x+1);
						}
					}
					firePropertyChange("UP",null,entry);
				}else{
					setOrderNumber(i+1);
				}
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == counterField){
			doUpdateLocation();
		}
		
	}
}
