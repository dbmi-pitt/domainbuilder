package edu.pitt.dbmi.domainbuilder.caseauthor;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.domainbuilder.beans.CaseEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ShapeEntry;
import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.dbmi.domainbuilder.widgets.*;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;

/**
 * this class represents a list of conepts
 * @author tseytlin
 *
 */
public class ConceptSelector extends JPanel implements ActionListener, ListSelectionListener, DropTargetListener {
	public static final String CONCEPT_REMOVED = "CONCEPT_REMOVED";
	public static final String CONCEPT_ADDED = "CONCEPT_ADDED";
	public static final String CONCEPT_RECOMMENDATIONS_CHANGED = "CONCEPT_RECOMMENDATIONS_CHANGED";
	public static final String CONCEPT_ASSERTED = "CONCEPT_ASSERTED";
	public static final String CONCEPT_SELECTED = "CONCEPT_SELECTED";
	public static final String CONCEPT_NEGATED = "CONCEPT_NEGATED";
	public static final String INFER_CONCEPTS = "INFER_CONCEPTS";
	public static final String CONCEPT_PART_ADDED = "CONCEPT_PART_ADDED";
	public static final String CONCEPT_PART_REMOVED = "CONCEPT_PART_REMOVED";
	private JList list;
	private CaseAuthor author;
	private TreeDialog treeDialog;
	private String root,title;
	//private PropertyChangeSupport pcs;
	private JToolBar toolbar;
	private ShapeSelectorPanel shapeChooser;
	private JPopupMenu popup;
	private boolean blockEvent,readOnly;
	//private static boolean autoInfer = false;
	
	/**
	 * create concept selector
	 */
	public ConceptSelector(CaseAuthor author,String root,String title){
		super();
		this.author = author;
		this.root = root;
		this.title = title;
		setLayout(new BorderLayout());
		list = new ConceptList();
		list.setCellRenderer( new EntryWidgetCellRenderer(list));
		list.addListSelectionListener(this);
		list.setDragEnabled(true);
		new DropTarget(list,this);
		toolbar = createToolBar(title);
		add(toolbar,BorderLayout.NORTH);
		add(new JScrollPane(list),BorderLayout.CENTER);
		setPreferredSize(new Dimension(200,300));
		//pcs = new PropertyChangeSupport(this);
		
		list.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					int i = UIHelper.getIndexForLocation(list,e.getPoint());
					if(i > -1)
						doProperties((ConceptEntry)list.getModel().getElementAt(i));
				} 
			}
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
					int x = UIHelper.getIndexForLocation(list,e.getPoint());
					// set selection 
					if(x > -1){
						// if not within selection, then reset selection
						if(!isWithinSelection(x))
							list.setSelectedIndex(x);
					}
					getPopupMenu(x).show(list,e.getX(),e.getY());
					
				}
			}
			private boolean isWithinSelection(int x){
				for(int i:list.getSelectedIndices()){
					if(i == x)
						return true;
				}
				return false;
			}
		});
		setEnabled(false);
	}
	
	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	/**
	 * reset concept selector
	 */
	public void reset(){
		list.setModel(author.getCaseEntry().createConceptListModel(root));
		if(treeDialog != null){
			treeDialog.dispose();
			treeDialog = null;
		}
		setEnabled(true);
	}
	
	/**
	 * get all concept entries
	 * @return
	 */
	public ConceptEntry [] getConceptEntries(){
		ConceptEntry [] c = new ConceptEntry [list.getModel().getSize()];
		for(int i=0;i<c.length;i++)
			c[i] = (ConceptEntry) list.getModel().getElementAt(i);
		return c;
	}
	
	/*
	public static boolean isAutoInfer() {
		return autoInfer;
	}

	public static void setAutoInfer(boolean autoInfer) {
		ConceptSelector.autoInfer = autoInfer;
	}
	*/
	
	/**
	 * add several concept entries
	 * @return
	 */
	public void addConceptEntries(ConceptEntry [] entries){
		for(int i=0;i<entries.length;i++)
			addConceptEntry(entries[i]);
		list.revalidate();
	}
	
	/**
	 * add a concept entry
	 * @return
	 */
	public void addConceptEntry(ConceptEntry entry){
		if(SwingUtilities.isEventDispatchThread()){
			((MutableListModel)list.getModel()).addElement(entry);
		}else{
			final ConceptEntry e = entry;
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					public void run(){
						((MutableListModel)list.getModel()).addElement(e);
					}
				});
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}	
		}
	}
	
	/**
	 * remove a concept entry
	 * @return
	 */
	public void removeConceptEntry(ConceptEntry entry){
		if(SwingUtilities.isEventDispatchThread()){
			((MutableListModel)list.getModel()).removeElement(entry);
		}else{
			final ConceptEntry e = entry;
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					public void run(){
						((MutableListModel)list.getModel()).removeElement(e);
					}
				});
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * remove a concept entry
	 * @return
	 */
	public void removeConceptEntries(){
		if(SwingUtilities.isEventDispatchThread()){
			((MutableListModel)list.getModel()).removeAllElements();
		}else{
			try {
				SwingUtilities.invokeAndWait(new Runnable(){
					public void run(){
						((MutableListModel)list.getModel()).removeAllElements();
					}
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * remove a concept entry
	 * @return
	 */
	public void removeImpliedConceptEntries(){
		// select elements that need to be removed
		ArrayList<ConceptEntry> toremove = new ArrayList<ConceptEntry>();
		for(int i=0;i<list.getModel().getSize();i++){
			Object o = list.getModel().getElementAt(i);
			if(o instanceof ConceptEntry){
				ConceptEntry entry = (ConceptEntry) o;
				if(!entry.isAsserted())
					toremove.add(entry);
			}
		}
		// remove all elements that were selected
		for(ConceptEntry e: toremove){
			removeConceptEntry(e);
		}
		list.validate();
	}
	
	
	/**
	 * add listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener l){
		super.addPropertyChangeListener(l);
	}
	
	/**
	 * remove listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener l){
		super.removePropertyChangeListener(l);
	}
	
	
	/**
	 * forward fire requests
	 */
	public void firePropertyChange(String p, Object o, Object n){
		super.firePropertyChange(p,o,n);
	}

	
	/**
	 * set panel to be editable
	 * enable/disable buttons
	 * @param b
	 */
	public void setEnabled(boolean b){
		for(int i=0;i<toolbar.getComponentCount();i++){
			toolbar.getComponent(i).setEnabled(b);
		}
		list.setEnabled(b);
	}
	
	
	/**
	 * add new concept
	 *
	 */
	public void doAdd(){
		if(treeDialog == null){
			IOntology ont = author.getKnowledgeBase();
			treeDialog = new TreeDialog(author.getFrame());
			treeDialog.setTitle(title);
			treeDialog.setRoot(ont.getClass(root));
		}
		
		// highlight concept that is selected
		Object e = list.getSelectedValue();
		if(e != null){
			treeDialog.setHighlightedNode(""+e);
		}
				
		treeDialog.setVisible(true);
		// add result
		TreePath [] path = treeDialog.getSelectedPaths();
		if(path != null && path.length > 0){
			for(int i=0;i<path.length;i++){
				IClass cls = author.getKnowledgeBase().getClass(""+path[i].getLastPathComponent());
				ConceptEntry entry = new ConceptEntry(cls);
				entry.setAsserted(true);
				addConceptEntry(entry);
			}
			list.revalidate();
			//if(autoInfer)
				firePropertyChange(CONCEPT_ADDED,null,this);
		}
	}
	
	/**
	 * remove concept
	 */
	public void doRemove(){
		Object [] values = list.getSelectedValues();
		for(int i=0;i<values.length;i++){
			removeConceptEntry((ConceptEntry)values[i]);
		}
		if(values.length > 0){
			//if(autoInfer)
				firePropertyChange(CONCEPT_REMOVED,null,this);
			list.revalidate();
		}
	}

	/**
	 * link to annotations
	 */
	
	public void doLink(){
		if(shapeChooser == null){
			shapeChooser = new ShapeSelectorPanel(author.getShapeSelector().getRoot());
			shapeChooser.setOwner(author.getFrame());
			shapeChooser.setSelectionMode(EntryChooser.MULTIPLE_SELECTION);
		}
		shapeChooser.showChooserDialog();
		ConceptEntry entry = (ConceptEntry) list.getSelectedValue();
		if(entry != null && shapeChooser.isSelected()){
			for(Object shape: shapeChooser.getSelectedObjects())
				entry.addLocation(""+shape);
			entry.flash(list);
			entry.setAsserted(true);
		}
	}
	
	/**
	 * perform sort
	 */
	public void doSort(){
		((MutableListModel)list.getModel()).sort();
		list.revalidate();
		list.repaint();
	}
	
	
	/**
	 * select concept entry
	 * @param e
	 */
	public void selectEntry(Object e){
		blockEvent = true;
		list.setSelectedValue(e,true);
		blockEvent = false;
	}
	
	public void clearSelection(){
		blockEvent = true;
		list.clearSelection();
		blockEvent = false;
	}
	
	/**
	 * display properties of concept entry
	 * @param e
	 */
	public void doProperties(ConceptEntry e){
		ConceptEntryPanel.showConceptEntryDialog(e,author);	
		list.repaint();
	}
	
	/**
	 * display properties of concept entry
	 * @param e
	 */
	public void doAllProperties(){
		if(list.getModel().getSize() <= 0)
			return;
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		final ArrayList<ConceptEntryPanel> entries = new ArrayList<ConceptEntryPanel>();
		for(int i=0;i<list.getModel().getSize();i++){
			ConceptEntry entry = (ConceptEntry) list.getModel().getElementAt(i);
			ConceptEntryPanel panel = new ConceptEntryPanel(entry);
			entries.add(panel);
			tabs.addTab(entry.getText(),panel);
		}
		final JOptionPane opt = new JOptionPane(tabs,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
		JDialog d = opt.createDialog(author.getFrame(),"Case Concept Properties");
		d.setModal(false);
		d.setResizable(true);
		d.addWindowListener(new WindowAdapter(){
			public void windowDeactivated(WindowEvent e) {
				Object x = opt.getValue();
				if(x instanceof Integer && ((Integer)x).intValue() == JOptionPane.OK_OPTION){
					for(ConceptEntryPanel panel: entries){
						panel.sync();
						if(panel.isNegated())
							firePropertyChange(CONCEPT_NEGATED,panel,panel.getConceptEntry());
					}
					list.repaint();
				}
				super.windowDeactivated(e);
			}
		});
		d.setVisible(true);
	}
	
	
	/**
	 * add shape node to the concept
	 * @param node
	 */
	private void addShape(ConceptEntry concept, Object node){
		/*
		Object obj = node.getUserObject();
		// if individual shape was dragged
		if(obj instanceof ShapeEntry){
			ShapeEntry shape = (ShapeEntry) obj;
			if(shape.isArrow()){
				concept.setExample(shape);
			}else{
				concept.addLocation(shape);
			}
		// if tag was dragged 
		}else{
			
		}
		*/
		String tag = ""+node;
		if(tag.startsWith("Arrow")){
			concept.setExample(tag);
		}else{
			concept.addLocation(tag);
		
			// maybe it is a tag with arrow if there
			for(ShapeEntry e: author.getShapeSelector().getAnnotations(tag)){
				if(e.getType().equals("Arrow")){
					concept.setExample(e.getName());
					break;
				}
			}
		}
		
		concept.setAsserted(true);
	}
	
	
	/**
	 * add shape node to the concept
	 * @param node
	 */
	private void addSlide(ConceptEntry concept, String slide){
		concept.addLocation(slide);
	}
	
	
	/**
	 * is list empty
	 * @return
	 */
	public boolean isEmpty(){
		return list.getModel().getSize() == 0;
	}
	
	/**
	 * drag-n-drop support, this is when drop occures
	 * @param dtde
	 */
	public void drop(DropTargetDropEvent dtde){
        if(readOnly)
        	return;
		
		Point loc = dtde.getLocation();
        if(dtde.getSource() instanceof DropTarget){
        	DropTarget droptarget = (DropTarget) dtde.getSource();
        	if(droptarget.getComponent() instanceof JList){
        		JList list = (JList) droptarget.getComponent();
        		int indx = list.locationToIndex(loc);
        		if(indx > -1){
        			ConceptEntry entry = (ConceptEntry) list.getModel().getElementAt(indx);
        			try{
        				String data = ""+dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
        				
        				// check if it is a concept itself that is being dragged
        				ConceptEntry source = (ConceptEntry) list.getSelectedValue();
        				if(source != null && source.getName().equals(data)){
        					MutableListModel model = (MutableListModel) list.getModel();
        					model.removeElement(source);
        					model.insertElementAt(source,indx);
        					list.revalidate();
        					list.repaint();
        				}else{
        					// check if this is a shape
	        				for(String str: data.split("\n")){
	        					// make sure it is not another resource like Concept
	        					if(author.getShapeSelector().hasAnnotation(str))
	        						addShape(entry,str);
	        					else if(author.getCaseEntry().hasSlide(str))
	        						addSlide(entry,str);
	        					else if(OntologyHelper.isRecommendation(author.getKnowledgeBase().getClass(str)))
	        						entry.getRecommendations().add(new ConceptEntry(author.getKnowledgeBase().getClass(str)));
	        				}
	        				list.repaint();
	        				entry.flash(list);
        				}
        			}catch(Exception ex){
        				ex.printStackTrace();
        			}
        		}
        	}
        }
    }
	// don't need those methods for now
    public void dragEnter(DropTargetDragEvent dtde) {}
    public void dragExit(DropTargetEvent dte) {}
    public void dragOver(DropTargetDragEvent dtde) {}
    public void dropActionChanged(DropTargetDragEvent dtde) {}
	
	
	/**
	 * to get custom tooltips extend JList
	 */
	private class ConceptList extends JList {
		/**
		 * a way to get tooltip text for individual items
		 * copy/pasted from:
		 * http://blog.codebeach.com/2007/06/tooltips-for-individual-items-in-jlist.html
		 */
		 public String getToolTipText(MouseEvent e) {
			//Get the item in the list box at the mouse location
		    int i = UIHelper.getIndexForLocation(this,e.getPoint());
		 
		    //Get the value of the item in the list
		    String text = null;
		    if(i>-1){
		    	Object obj = getModel().getElementAt(i);
		    	if(obj instanceof ConceptEntry){
		    		ConceptEntry en = (ConceptEntry) obj;
		    		String n = UIHelper.getPrettyClassName(en.getName());
		    		String d = en.getDescription();
		    		if(TextHelper.isEmpty(d)){
		    			d = en.getFeature().getDescription();
		    		}
		    		//text = (d != null && d.length() > 0)?"<html><b>"+n+"</b><hr>"+TextHelper.formatString(d):n;
		    		text = "<html><b>"+n+"</b>";
		    		// add definition
		    		if(d != null && d.length() > 0){
		    			text = text+"<table width=500 border=0><tr><td>"+d+"</td></tr></table>";
		    		}
		    		// add disease rules
		    		if(en.isDisease()){
		    			text = text+"<hr>"+TextHelper.formatExpression(en.getCompleteFindings());
		    		}
		    		
		    		//alternative concepts
		    		if(!en.getAlternativeConcepts().isEmpty()){
		    			text = text+"<hr><table width=500 border=0><tr><td><font color=\"#C35617\"><b>This case may also have:</b></font></td></tr><tr><td>"+
		    			TextHelper.toString(en.getAlternativeConcepts())+"</td></tr></table>";
		    			/*
		    			for(String s: en.getAlternativeConcepts()){
		    				text = text+"<br>OR<br>"+s;
		    			}
		    			text = text+"</center>";
		    			*/
		    		}
		    		
		    	}else
		    		text = ""+obj;
		    			
		    }
		    return text;
		}
		
	}

	/**
	 * content is being browsed
	 */
	public void valueChanged(ListSelectionEvent e) {
		if(!e.getValueIsAdjusting() && !blockEvent){
			firePropertyChange(CONCEPT_SELECTED,this,list.getSelectedValue());
		}
	}
	
	public JPopupMenu getPopupMenu(int x){
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("Link","Link Concept to Annotations",Icons.LINK,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Add","Add Concept to Case",Icons.PLUS,this));
			popup.add(UIHelper.createMenuItem("Remove","Remove Concept from Case",Icons.MINUS,this));
			popup.add(UIHelper.createMenuItem("Remove All","Remove All Concepts from Case",Icons.MINUS_ALL,this));
			popup.addSeparator();
			popup.add(UIHelper.createCheckboxMenuItem("Assert","Assert Concepts in Case",null,this));
			popup.add(UIHelper.createCheckboxMenuItem("Mark Absent","Set as Explicitly Absent Concepts in Case",null,this));
			popup.add("Pathologies");
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Sort","Sort Concepts in Alphabetical Order",Icons.SORT,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Properties","Case Concept Properties",Icons.PROPERTIES,this));
			
			// disable stuff if read-only
			if(readOnly){
				setReadOnly(true);
			}
		}
		if(!readOnly){
			popup.getComponent(0).setEnabled(x > -1);
			popup.getComponent(3).setEnabled(x > -1);
			popup.getComponent(6).setEnabled(x > -1);
			popup.getComponent(7).setEnabled(x > -1);
			popup.getComponent(8).setEnabled(x > -1);
			popup.getComponent(12).setEnabled(x > -1);
			
			if(x > -1){
				//ConceptEntry e = (ConceptEntry) list.getModel().getElementAt(x);
				JCheckBoxMenuItem assertEntry = (JCheckBoxMenuItem)popup.getComponent(6);
				JCheckBoxMenuItem absentEntry = (JCheckBoxMenuItem)popup.getComponent(7);
				
				// set values for an individual entry
				if(list.getSelectedValues().length == 1){
					ConceptEntry e = (ConceptEntry) list.getSelectedValues()[0];
					assertEntry.setSelected(e.isAsserted());
					absentEntry.setSelected(e.isAbsent());
					assertEntry.setText("Assert");
					absentEntry.setText("Mark Absent");
					JMenu m = getPartMenu(e.getParts());
					m.setText("Pathologies");
					popup.remove(popup.getComponent(8));
					popup.add(m,8);
				}else if(list.getSelectedValues().length > 1){
					assertEntry.setSelected(getAssertAggregate());
					absentEntry.setSelected(getAbsentAggregate());
					assertEntry.setText("Assert All");
					absentEntry.setText("Mark All Absent");
					Set<String> parts = new LinkedHashSet<String>();
					for(Object o: list.getSelectedValues())
						parts.addAll(((ConceptEntry)o).getParts());
					JMenu m = getPartMenu(parts);
					m.setText("Set All Pathologies");
					popup.remove(popup.getComponent(8));
					popup.add(m,8);
				}
			}
		}
		return popup;
	}
	
	private boolean getAssertAggregate(){
		boolean r = false;
		for(Object o: list.getSelectedValues()){
			ConceptEntry e = (ConceptEntry) o;
			r |= e.isAsserted();
		}
		return r;
	}
	
	private boolean getAbsentAggregate(){
		boolean r = true;
		for(Object o: list.getSelectedValues()){
			ConceptEntry e = (ConceptEntry) o;
			r &= e.isAbsent();
		}
		return r;
	}
	
	
	/**
	 * get part menu
	 * @return
	 */
	private JMenu getPartMenu(Set<String> parts){
		JMenu menu = new JMenu("Pathologies");
		CaseEntry cas = author.getCaseEntry();
		if(cas != null){
			for(String p: cas.getParts()){
				boolean on = parts != null && parts.contains(p);
				JCheckBoxMenuItem m = UIHelper.createCheckboxMenuItem(p,"Assign Pathology Information to selected Concepts",null,on,this);
				// disable menu from closing on click
				// http://forums.sun.com/thread.jspa?threadID=5366636
				m.setUI(new BasicCheckBoxMenuItemUI(){
					 protected void doClick(MenuSelectionManager msm) {
					      menuItem.doClick(0);
					   }
				});
				menu.add(m);
			}
		}
		return menu;
	}
	
	
	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(String title){
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(Color.white);
		toolbar.add(UIHelper.createButton("Add","Add Concept to Case",Icons.PLUS,16,this));
		toolbar.add(UIHelper.createButton("Remove","Remove Concept from Case",Icons.MINUS,16,this));
		toolbar.addSeparator();
		toolbar.add(new JLabel(title));
		toolbar.add(Box.createHorizontalGlue());
		JButton infer = UIHelper.createButton("Infer","Infer Relevent Concepts",Icons.SEARCH,16,this);
		if("Diagnosis".equalsIgnoreCase(title)){
			infer.setToolTipText("Get Diagnoses from the Knowledge Base that are Implied by Findings in this Case");
			toolbar.add(infer);
		}else if("Findings".equalsIgnoreCase(title)){
			infer.setToolTipText("Get Findings from the Knowledge Base that are Associated with Case Diagnosis");
			toolbar.add(infer);
		}
		
		toolbar.add(UIHelper.createButton("All Properties","Edit All Concept Properties in Case",Icons.PROPERTIES,16,this));
		setReadOnly(readOnly);
		return toolbar;
	}
	
	/**
	 * actions
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if("Add".equals(cmd)){
			doAdd();
		}else if("Remove".equals(cmd)){
			doRemove();
		}else if("Remove All".equals(cmd)){
			list.setSelectionInterval(0,list.getModel().getSize()-1);
			doRemove();
		}else if("Link".equals(cmd)){
			doLink();
		}else if("Properties".equals(cmd)){
			if(list.getSelectedValue() != null)
				doProperties((ConceptEntry)list.getSelectedValue());
		}else if("All Properties".equals(cmd)){
			doAllProperties();
		}else if("Assert".equals(cmd)){
			//if(list.getSelectedValue() != null){
			for(Object o: list.getSelectedValues()){
				((ConceptEntry)o).setAsserted(((AbstractButton)e.getSource()).isSelected());
				list.repaint();
				//if(autoInfer)
					firePropertyChange(CONCEPT_ASSERTED,null,this);
			}
		}else if("Mark Absent".equals(cmd)){
			//if(list.getSelectedValue() != null){
			for(Object o: list.getSelectedValues()){
				((ConceptEntry)o).setAbsent(((AbstractButton)e.getSource()).isSelected());
				list.repaint();
				list.revalidate();
				firePropertyChange(CONCEPT_NEGATED,this,(ConceptEntry)o);
			}
		}else if("Sort".equals(cmd)){
			doSort();
		}else if("Infer".equals(cmd)){
			firePropertyChange(INFER_CONCEPTS,null,this);
		}else if(author.getCaseEntry() != null && author.getCaseEntry().getParts().contains(cmd)){
			// this is a part assignment
			AbstractButton b = (AbstractButton) e.getSource();
			doSetPart(b.isSelected(),cmd);
		}
	}
	
	/**
	 * add/remove part information;
	 * @param add
	 * @param part
	 */
	private void doSetPart(boolean add, String part){
		for(Object o: list.getSelectedValues()){
			ConceptEntry e = (ConceptEntry) o;
			if(add){
				e.addPart(part);
				firePropertyChange(CONCEPT_PART_ADDED,part,e);
			}else{
				e.removePart(part);
				firePropertyChange(CONCEPT_PART_REMOVED,part,e);
			}
		}
	}
	
	/**
	 * set read only
	 * @param b
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		
		if(toolbar != null){
			UIHelper.setEnabled(toolbar, new String [0], !b);
		}
		if(popup != null){
			UIHelper.setEnabled(popup,new String []{"properties"},!b);
		}
	}
	
	
	
	/**
	 * 
	 */
	public String toString(){
		return title+": "+list.getModel();
	}
}
