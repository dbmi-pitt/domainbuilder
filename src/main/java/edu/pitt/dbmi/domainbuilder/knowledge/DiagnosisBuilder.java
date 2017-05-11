package edu.pitt.dbmi.domainbuilder.knowledge;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.getLocalRepositoryFolder;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.isReadOnly;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.*;
import javax.swing.tree.TreePath;
import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptExpression;
import edu.pitt.dbmi.domainbuilder.util.Communicator;
import edu.pitt.dbmi.domainbuilder.util.DomainTerminology;
import edu.pitt.dbmi.domainbuilder.util.FileRepository;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.dbmi.domainbuilder.widgets.ConceptCellEditor;
import edu.pitt.dbmi.domainbuilder.widgets.ConceptComboBox;
import edu.pitt.dbmi.domainbuilder.widgets.EntryWidgetCellRenderer;
import edu.pitt.dbmi.domainbuilder.widgets.IconCellRenderer;
import edu.pitt.dbmi.domainbuilder.widgets.ResourcePropertiesPanel;
import edu.pitt.dbmi.domainbuilder.widgets.SingleEntryWidget;
import edu.pitt.dbmi.domainbuilder.widgets.TreeDialog;
import edu.pitt.dbmi.domainbuilder.widgets.TreePanel;
import edu.pitt.ontology.*;
import edu.pitt.terminology.Terminology;

/**
 * build expressions that link diagnosis to findings
 * @author tseytlin
 *
 */
public class DiagnosisBuilder extends JPanel implements ActionListener {
	private DefaultTableModel model;
	private JTable table;
	private TreeDialog diagnosisDialog,findingsDialog;
	private IOntology kb;
	private Terminology terminology;
	private OntologySynchronizer synchronizer;
	private JPopupMenu popup;
	private JMenuBar menubar;
	private JToolBar toolbar;
	private JLabel tip;
	private JScrollPane stable;
	private java.util.List<ConceptEntry> diagnoses; 
	private ConceptComboBox conceptEditor;
	private Object [][] buffer;
	private final int PAD = 8;
	private boolean readOnly;
	private JDialog findReplaceDialog;
	private SingleEntryWidget find,replace;
	
	
	
	public DiagnosisBuilder(){
		super();
		setLayout(new BorderLayout());
		diagnoses = new ArrayList<ConceptEntry>();
		synchronizer = OntologySynchronizer.getInstance();
		
		// create JTree
		model = new DiagnosisTableModel();
		table = new JTable(model);
		table.setCellSelectionEnabled(true);
		table.setRowHeight(Icons.CONCEPT_ICON_HEIGHT+8);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		
		//table.setFillsViewportHeight(true);
	    table.addKeyListener(new KeyAdapter(){
	    	//private TreeDialog treeDialog;
	    	public void keyPressed(KeyEvent k){
	    		if(k.getKeyCode() == KeyEvent.VK_DELETE){
					doDelete(true);
				}else if(k.isControlDown() && k.getKeyCode() == KeyEvent.VK_X){
					doCut();
				}else if(k.isControlDown() && k.getKeyCode() == KeyEvent.VK_C){
					doCopy();
				}else if(k.isControlDown() && k.getKeyCode() == KeyEvent.VK_V){
					doPaste();
				}else if(k.isControlDown() && k.getKeyCode() == KeyEvent.VK_Z){
					doUndo();
				}else if(k.getKeyChar() >= 'a' && k.getKeyChar() <= 'z'){
					int r = table.getSelectedRow();
					int c = table.getSelectedColumn();
					if(r > -1 && c > -1){
						if(!table.isEditing()){
							table.editCellAt(r,c);
							conceptEditor.getTextEditor().setText(""+k.getKeyChar());
						}
						conceptEditor.requestFocusInWindow();
					}	
				}
				
	    	}
	    });
	    table.getTableHeader().addMouseListener(new MouseAdapter(){
	    	public void mouseClicked(MouseEvent e){
	    		int i = table.getColumnModel().getColumnIndexAtX(e.getX());
	    	    table.setColumnSelectionInterval(i,i);
	    	    table.setRowSelectionInterval(0,model.getRowCount()-1);

	    	}
	    });
	    ListSelectionListener l = new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if(!e.getValueIsAdjusting())
					table.getTableHeader().repaint();
			}
	    };
	    table.getSelectionModel().addListSelectionListener(l);
	    table.getColumnModel().getSelectionModel().addListSelectionListener(l);
	    table.addMouseListener(new MouseAdapter(){
	    	public void mousePressed(MouseEvent e){
	    		if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
	    			int row = table.rowAtPoint(e.getPoint());
	    			int col = table.columnAtPoint(e.getPoint());
	    			selectLocation(row, col);
	    			getPopupMenu().show(table,e.getX(),e.getY());
	    		}
	    	}
	    });
	    
	    add(getToolBar(),BorderLayout.NORTH);
		stable = new JScrollPane(table);
	    //add(new JScrollPane(table),BorderLayout.CENTER);
	    tip = new JLabel("<html><font size=20 color=gray>Use </font>&nbsp;&nbsp;<img src=\""+
	    		getClass().getResource(Icons.PLUS+24+".gif")+"\"> "+
	    		"&nbsp;&nbsp;<font size=20 color=gray> button to add diagnoses to worksheet</font>");
	    tip.setHorizontalAlignment(JLabel.CENTER);
	    add(tip,BorderLayout.CENTER);
	}
	
	/**
	 * if selection already contains row,col
	 * do nothing, else clear selection and select
	 * at location
	 * @param row
	 * @param col
	 */
	private void selectLocation(int row, int col){
		// check if already in selected range
		int [] rows = table.getSelectedRows();
		int [] cols = table.getSelectedColumns();
		if( Arrays.binarySearch(rows,row) > -1 && 
		    Arrays.binarySearch(cols,col) > -1){
			return;
		}
		// else
		table.clearSelection();
		table.addRowSelectionInterval(row,row);
		table.addColumnSelectionInterval(col,col);
	}
	
	
	private JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = new JToolBar();
			final int size = 24;
			
			if(Communicator.isConnected())
				toolbar.add(UIHelper.createButton("publish","Save and Publish Knowledge Base ",Icons.PUBLISH,this));
			else
				toolbar.add(UIHelper.createButton("save","Save Knowledge Base ",Icons.SAVE,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("import","Import from Spreadsheet",Icons.IMPORT,this));
			toolbar.add(UIHelper.createButton("export","Export to Spreadsheet",Icons.EXPORT,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("add diagnosis","Add Diagnosis Column",Icons.PLUS,size,this));
			toolbar.add(UIHelper.createButton("remove diagnosis","Remove Diagnosis Column",Icons.MINUS,size,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("Align Findings","Align Findings to Compare",Icons.ALIGN,size,-1,true,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("Find/Replace Findings","Find Diagnoses that Contain Finding and Replace with Another Finding",Icons.REPLACE,size,-1,this));
			toolbar.addSeparator();
			
			toolbar.add(UIHelper.createButton("delete","Remove Selected Findings",Icons.DELETE,size,this));
			toolbar.add(UIHelper.createButton("cut","Cut Selected Findings",Icons.CUT,size,this));
			toolbar.add(UIHelper.createButton("copy","Copy Selected Findings",Icons.COPY,size,this));
			toolbar.add(UIHelper.createButton("paste","Paste Findings into Selection",Icons.PASTE,size,this));
			toolbar.addSeparator();
			toolbar.add(UIHelper.createButton("NO ","NO/Complement selected finding",Icons.LINE,size,-1,true,this));
			toolbar.add(UIHelper.createButton("OR  ","OR/Union/Conjunction selected findings",Icons.LINE,size,-1,true,this));
			toolbar.add(UIHelper.createButton("AND ","AND/Intersection/Disjunction selected findings",Icons.LINE,size,-1,true,this));
		}
		return toolbar;
	}
	
	/**
	 * @return the ontology
	 */
	public IOntology getOntology() {
		return kb;
	}

	/**
	 * reset panel
	 */
	public void reset(){
		diagnosisDialog = null;
		// clear everything out
		table.selectAll();
		doRemove(table.getSelectedColumns());
	}
	
	
	/**
	 * get list of current diagnosis in worksheet
	 * @return
	 */
	public List<ConceptEntry> getWorksheetDiagnosisList(){
		return diagnoses;
	}
	
	/**
	 * get list of all diagnosis
	 * @return
	 */
	public List<ConceptEntry> getAllDiagnosisList(){
		List<ConceptEntry> list = new ArrayList<ConceptEntry>();
		for(IClass cls : kb.getClass(OntologyHelper.DISEASES).getSubClasses()){
			if(!OntologyHelper.isSystemClass(cls)){
				ConceptEntry entry = new ConceptEntry(cls);
				list.add(entry);
				
				// check for patterns
				if(entry.getPatternCount() > 1){
					for(int i=1;i<entry.getPatternCount();i++){
						entry = new ConceptEntry(cls);
						entry.setPatternOffset(i);
						list.add(entry);
					}
				}
				
			}
		}
		return list;
	}
	
	
	/**
	 * @param ontology the ontology to set
	 */
	public void setOntology(IOntology ont) {
		this.kb = ont;
		terminology = ont.getRepository().getTerminology(OntologyHelper.ONTOLOGY_TERMINOLOGY);
		conceptEditor = null;
		//if(conceptEditor != null)
		//	conceptEditor.updateWords();
		reset();
		setReadOnly(OntologyHelper.isReadOnly(ont));
	}
	
	// get instance of parent frame
	public JFrame getFrame(){
		//return frame;
		return DomainBuilder.getInstance().getFrame();
	}
	
	/**
	 * invoked when tab is no longer visible, perhaps save the ontology or something
	 */
	public void unselected(){
		if(readOnly)
			return;
		
		if(synchronizer.hasActions()){
			int r = JOptionPane.showConfirmDialog(getFrame(),kb.getName()+" knowledge base has been modified.\n" +
					"Would you like to save the changes?","Save?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
			if(r == JOptionPane.YES_OPTION){
				doSave();
			}
		}
	}
	
	/**
	 * this component is selected
	 */
	public void selected(){
		
	}
	
	
	/**
	 * add new concept
	 *
	 */
	private void doAdd(){
		if(diagnosisDialog == null){
			diagnosisDialog = new TreeDialog(getFrame());
			diagnosisDialog.setTitle("Select Diagnosis");
			diagnosisDialog.getTreePanel().setColorMode(TreePanel.DIAGNOSTIC_RULE_MODE);
			diagnosisDialog.setRoot(kb.getClass(OntologyHelper.DISEASES));
		}
		diagnosisDialog.setVisible(true);
		// add result
		TreePath [] path = diagnosisDialog.getSelectedPaths();
		if(path != null && path.length > 0){
			for(int i=0;i<path.length;i++){
				IClass cls = kb.getClass(""+path[i].getLastPathComponent());
				addDiagnosis(new ConceptEntry(cls));
			}
			updateTable();
			//pcs.firePropertyChange(CONCEPT_ADDED,null,this);
		}
	}
	
	/**
	 * open diagnosis
	 * @param dx
	 */
	public void openDiagnoses(Collection dx){
		if(dx == null || dx.isEmpty())
			return;
		for(Object obj: dx){
			ConceptEntry entry = null;
			
			// init from string or cast an object
			if(obj instanceof String)
				entry = new ConceptEntry(kb.getClass((String)obj));
			else if(obj instanceof ConceptEntry)
				entry = (ConceptEntry) obj;
			
			if(entry != null)
				addDiagnosis(entry);
		}
		updateTable();
	}
	
	
	
	/**
	 * save column content before removing
	 */
	private void doRemove(int [] selection){
		//int [] selection = table.getSelectedColumns();
		
		// remove columns
		List<TableColumn> toremove = new ArrayList<TableColumn>();
		for(int i=0;i<selection.length;i++){
			diagnoses.get(selection[i]).setRemoved(true);
			toremove.add(table.getColumnModel().getColumn(selection[i]));
			//TableColumn c = table.getColumn(selection[i]);
			//toremove.add(e);
			//table.removeColumn(c);
		}
		// remove all columns
		//System.out.println(toremove);
		for(TableColumn c: toremove){
			table.removeColumn(c);
		}
		
		//diagnoses.removeAll(toremove);
		//System.out.println(diagnoses);
		// This should take care of a problem with removing multi-pattern diagnoses
		for(ListIterator<ConceptEntry> i = diagnoses.listIterator();i.hasNext();){
			ConceptEntry e = i.next();
			if(e.isRemoved())
				i.remove();
		}
		//System.out.println(diagnoses);
		
		// sync diagnoses size for renderer
		IconCellRenderer.setNumberOfDiagnoses(diagnoses.size());
		
		syncColumns();
		updateTable();
		//System.out.println("after update");
		table.clearSelection();
		
		// if table has not content, display tip
		/*
		if(table.getColumnCount() == 0){
			remove(stable);
			add(tip,BorderLayout.CENTER);
			revalidate();
		}*/
	}
	
	private void doDelete(boolean prompt){
		int [] cols = table.getSelectedColumns();
		int [] rows = table.getSelectedRows();
		if(cols.length > 0){
			int r = (prompt)?JOptionPane.showConfirmDialog(getFrame(),"Are you sure you want to delete selected values?",
					"Question",JOptionPane.YES_NO_OPTION):JOptionPane.YES_OPTION;
			if(r == JOptionPane.YES_OPTION){
				for(int i=0;i<rows.length;i++)
					for(int j=0;j<cols.length;j++)
						table.setValueAt("",rows[i],cols[j]);
			}
		}
		updateTable();
	}
	
	private void doCut(){
		doCopy();
		doDelete(false);
	}

	
	private void doCopy(){
		int [] cols = table.getSelectedColumns();
		int [] rows = table.getSelectedRows();
		if(cols.length > 0){
			buffer = new Object [rows.length][cols.length];
			for(int i=0;i<rows.length;i++)
				for(int j=0;j<cols.length;j++)
					buffer[i][j] = table.getValueAt(rows[i],cols[j]);
		}
	}
	
	private void doPaste(){
		int [] cols = table.getSelectedColumns();
		int [] rows = table.getSelectedRows();
		if(buffer != null && cols.length > 0 && rows.length > 0){
			// if only one item is in buffer
			if(buffer.length == 1 && buffer[0].length ==1){
				for(int i=0;i<cols.length;i++){
					for(int j=0;j<rows.length;j++){
						table.setValueAt(copy( buffer[0][0]),rows[j],cols[i]);
					}
				}
			}else{
				// paste buffer in upper left corder
				for(int i=0;i<buffer.length;i++){
					for(int j=0;j<buffer[i].length;j++){
						if(rows[0]+i < table.getRowCount() && cols[0]+j < table.getColumnCount())
							table.setValueAt(copy(buffer[i][j]),rows[0]+i,cols[0]+j);
					}
				}
			}
		}
		updateTable();
		table.clearSelection();
	}
	
	/**
	 * copy object
	 * @param obj
	 * @return
	 */
	private Object copy(Object obj){
		if(obj instanceof ConceptEntry){
			obj = ((ConceptEntry)obj).clone();
		}else if(obj instanceof ConceptExpression){
			obj = ((ConceptExpression)obj).clone();
		}
		return obj;
	}
	
	
	private void doComplement(){
		int [] cols = table.getSelectedColumns();
		int [] rows = table.getSelectedRows();
		if(cols.length > 0){
			final Map<ConceptEntry,List<ConceptEntry>> map = new HashMap<ConceptEntry,List<ConceptEntry>>();
			StringBuffer problems = new StringBuffer();
			for(int i=0;i<rows.length;i++){
				for(int j=0;j<cols.length;j++){
					Object obj = table.getValueAt(rows[i],cols[j]);
					if(obj instanceof ConceptEntry){
						ConceptEntry e = (ConceptEntry) obj;
						if(OntologyHelper.isFeature(e.getConceptClass())){
							e.setAbsent(!e.isAbsent());
							
							// save the map
							ConceptEntry d = diagnoses.get(cols[j]);
							if(!map.containsKey(d)){
								map.put(d,new ArrayList<ConceptEntry>());
							}
							map.get(d).add(e);
						}else
							problems.append(e.getName()+" ");
					}
				}
				if(problems.length() > 0){
					JOptionPane.showMessageDialog(getFrame(),
							"Cannot negate non diagnostic findings: "+problems,
							"Warning",JOptionPane.WARNING_MESSAGE);
				}
			}
			table.repaint();
			
			// now do the notification thing
			synchronizer.addOntologyAction(new OntologyAction(){
				public void run(){
					for(ConceptEntry d: map.keySet()){
						OntologyHelper.getConceptHandler(kb).addDiagnosis(d);
					}
				}
				public void undo(){
					for(ConceptEntry d: map.keySet()){
						for(ConceptEntry e: map.get(d)){
							e.setAbsent(!e.isAbsent());
						}
					}
					table.repaint();
				}
				public String toString(){
					return "complement "+map.values();
				}
			});
		}
	}
	
	/**
	 * create a union expression
	 */
	
	private void doUnion(){
		int [] cols = table.getSelectedColumns();
		final int [] rows = table.getSelectedRows();
		if(cols.length == 1 && rows.length > 1){
			final ConceptEntry disease = diagnoses.get(cols[0]);
			
			// if entire column selected, then clone column
			// else create an expression
			if(rows.length == model.getRowCount()){
				// add new disease column
				ConceptEntry entry = new ConceptEntry(disease.getConceptClass());
				entry.addNewPattern(disease);
				addDiagnosisColumn(entry);
				
				/*
				entry.setPatternOffset(entry.createNewPattern());
				addDiagnosisColumn(entry);
				
				// make other siblings aware of this change
				for(ConceptEntry e: diagnoses){
					if(e.equals(entry))
						e.setCompleteFindings(entry.getCompleteFindings());
				}
				*/
			}else{
				final ConceptExpression exp = new ConceptExpression(ILogicExpression.OR);
				final List toremove = new ArrayList();
				for(int i=0;i<rows.length;i++){
					Object obj = table.getValueAt(rows[i],cols[0]);
					if(obj instanceof ConceptEntry){
						exp.add(obj);
					}else if(obj instanceof ConceptExpression){
						ConceptExpression e = (ConceptExpression) obj;
						if(e.getExpressionType() == ILogicExpression.OR){
							exp.addAll(e);
						}else if(e.getExpressionType() == ILogicExpression.AND){
							// this is highly unlikley if even possible
							// but will deal
							exp.add(e);
						}
					}
					toremove.add(obj);
				}
				// add expression
				disease.getFindings().add(exp);
				disease.getFindings().removeAll(toremove);
				disease.getFindings().setEntry(exp,rows[0]);
				table.clearSelection();
				
				// add action
				synchronizer.addOntologyAction(new OntologyAction(){
					public void run(){
						OntologyHelper.getConceptHandler(kb).addDiagnosis(disease);
					}
					public void undo(){
						disease.getFindings().remove(exp);
						for(int i=0;i<toremove.size();i++){
							disease.getFindings().add(toremove.get(i));
							disease.getFindings().setEntry(toremove.get(i),rows[i]);
						}
						updateTable();
					}
					public String toString(){
						return "union "+exp;
					}
				});
				
			}
			updateTable();
		}else{
			JOptionPane.showMessageDialog(getFrame(),"Invalid selection!","Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	/**
	 * break up union expresssion
	 */
	private void doIntersection(){
		int [] cols = table.getSelectedColumns();
		int [] rows = table.getSelectedRows();
		if(cols.length == 1 && rows.length > 0){
			final ConceptEntry disease = diagnoses.get(cols[0]);
			final List<ConceptExpression> toremove = new ArrayList<ConceptExpression>();
			for(int i=0;i<rows.length;i++){
				Object obj = table.getValueAt(rows[i],cols[0]);
				if(obj instanceof ConceptExpression){
					ConceptExpression e = (ConceptExpression) obj;
					if(e.getExpressionType() == ILogicExpression.OR){
						disease.getFindings().addAll(e);
						toremove.add(e);
					}
					
				}
			}
			if(!toremove.isEmpty()){
				disease.getFindings().removeAll(toremove);
				updateTable();
				
				// add action
				synchronizer.addOntologyAction(new OntologyAction(){
					public void run(){
						OntologyHelper.getConceptHandler(kb).addDiagnosis(disease);
					}
					public void undo(){
						for(ConceptExpression exp: toremove){
							disease.getFindings().removeAll(exp);
							disease.getFindings().add(exp);
						}
						updateTable();
						
					}
					public String toString(){
						return "intersection"+toremove;
					}
				});	
			}else{
				JOptionPane.showMessageDialog(getFrame(),"Selected values are already disjoint!","Warning",JOptionPane.WARNING_MESSAGE);
			}
		}else
			JOptionPane.showMessageDialog(getFrame(),"Invalid selection!","Error",JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * add findings
	 * @param row
	 * @param col
	 */
	private void doAddFinding(int row, int col){
		if(findingsDialog == null){
			findingsDialog = new TreeDialog(getFrame());
			findingsDialog.setTitle("Select Diagnositc Findings");
			//findingsDialog.setRoot(kb.getClass(OntologyHelper.DIAGNOSTIC_FEATURES));
			IClass [] roots = new IClass [] {
				kb.getClass(OntologyHelper.DIAGNOSTIC_FEATURES),
				kb.getClass(OntologyHelper.CLINICAL_FEATURES),
				kb.getClass(OntologyHelper.ANCILLARY_STUDIES)
			};
			findingsDialog.setRoots(roots);
			findingsDialog.setSelectionMode(TreeDialog.MULTIPLE_SELECTION);
		}
		findingsDialog.setVisible(true);
		// add multiple objects
		int i=0;
		for(Object obj: findingsDialog.getSelectedObjects()){
			IClass cls = kb.getClass(""+obj);
			ConceptEntry entry = new ConceptEntry(cls);
			ConceptEntry d = diagnoses.get(col);
			if(d != null && !d.getFindings().contains(entry)){
				table.setValueAt(entry,row+i++,col);
			}
		}
	}
	
	
	/**
	 * sync columns to their values
	 */
	private void syncColumns(){
		/*
		for(int i=0;i<diagnoses.size();i++){
			ConceptEntry d = diagnoses.get(i);
			table.getColumn(d).setModelIndex(i);
		}*/
		for(int i=0;i<table.getColumnModel().getColumnCount();i++){
			table.getColumnModel().getColumn(i).setModelIndex(i);
		}
	}
	
	/**
	 * add diagnosis column
	 * @param d
	 */
	void addDiagnosis(ConceptEntry entry){
		// remove if it is there already
		removeDiagnosis(entry);
		
		if(entry.getPatternCount() > 1){
			for(int i=0;i<entry.getPatternCount();i++){
				ConceptEntry e = new ConceptEntry(entry.getConceptClass());
				e.setPatternOffset(i);
				e.setCompleteFindings(entry.getCompleteFindings());
				addDiagnosisColumn(e);
			}
		}else
			addDiagnosisColumn(entry);
		//doOrganizeDiagnoses();
	}
	
	private void removeDiagnosis(ConceptEntry entry){
		// remove previous diagnoss
		List<Integer> selection = new ArrayList<Integer>();
		for(int i=0;i<diagnoses.size();i++){
			if(diagnoses.get(i).equals(entry))
				selection.add(i);
		}
		if(!selection.isEmpty()){
			int [] s = new int [selection.size()];
			for(int i=0;i<s.length;i++)
				s[i] = selection.get(i).intValue();
			doRemove(s);
		}
	}
	
	
	
	
	
	/**
	 * add diagnosis column
	 * @param d
	 */
	void addDiagnosisColumn(ConceptEntry entry){
		if(!table.isShowing()){
			remove(tip);
			add(stable,BorderLayout.CENTER);
			revalidate();
		}
		diagnoses.add(entry);
		TableColumn c = new TableColumn(diagnoses.size()-1);
		c.setResizable(false);
		c.setHeaderValue(entry);
		c.setIdentifier(entry);
		c.setMinWidth(Icons.CONCEPT_ICON_WIDTH+16);
		c.setMaxWidth(Icons.CONCEPT_ICON_WIDTH+16);
		c.setHeaderRenderer(new IconCellRenderer(true));
		c.setCellRenderer(new IconCellRenderer(false));
		//c.setCellEditor(new IconCellEditor());
		if(conceptEditor == null && terminology != null && terminology instanceof DomainTerminology){
			conceptEditor = new ConceptComboBox((DomainTerminology) terminology);
			conceptEditor.addDefaultParent(kb.getClass(OntologyHelper.DIAGNOSTIC_FEATURES));
			conceptEditor.addDefaultParent(kb.getClass(OntologyHelper.CLINICAL_FEATURES));
			conceptEditor.addDefaultParent(kb.getClass(OntologyHelper.ANCILLARY_STUDIES));
		}
		c.setCellEditor(new ConceptCellEditor(conceptEditor));
		table.getColumnModel().addColumn(c);
		
		IconCellRenderer.setNumberOfDiagnoses(diagnoses.size());
	}
	
	
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand().toLowerCase().trim();
		if(cmd.equals("add diagnosis")){
			doAdd();
		}else if(cmd.equals("remove diagnosis")){
			doRemove(table.getSelectedColumns());
		}else if(cmd.equals("save")){
			doSave();
		}else if(cmd.equals("publish")){
			doPublish();
		}else if(cmd.equals("undo")){
			doUndo();
		}else if(cmd.equals("delete")){
			doDelete(true);
		}else if(cmd.equals("cut")){
			doCut();
		}else if(cmd.equals("copy")){
			doCopy();
		}else if(cmd.equals("paste")){
			doPaste();
		}else if(cmd.equals("no")){
			doComplement();
		}else if(cmd.equals("or")){
			doUnion();
		}else if(cmd.equals("and")){
			doIntersection();
		}else if(cmd.equals("align findings")){
			doOrganizeDiagnoses();
		}else if(cmd.equals("find/replace findings")){	
			doFindReplace();
		}else if(cmd.equals("import")){
			doImport();
		}else if(cmd.equals("export")){
			doExport();
		}else if(cmd.equals("add finding")){
			int row = table.getSelectedRow();
			int col = table.getSelectedColumn();
			//if(row > -1 && col > -1)
			//	table.editCellAt(row,col);
			doAddFinding(row, col);
		}else if(cmd.equals("properties")){
			doProperties();
		}else if(cmd.equals("close")){
			if(findReplaceDialog != null)
				findReplaceDialog.dispose();
		}else if(cmd.equals("find")){
			doFind();
		}else if(cmd.equals("replace")){
			doFind();
			doReplace();
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
			file.addSeparator();
			file.add(UIHelper.createMenuItem("Import","Import Diagnoses Spreadsheet",Icons.IMPORT,this));
			file.add(UIHelper.createMenuItem("Export","Export Diagnoses Spreadsheet",Icons.EXPORT,this));
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
					edit.getItem(0).setEnabled(synchronizer.hasActions());
				}
			});
			
			
			JMenu builder = new JMenu("Builder");
			builder.add(UIHelper.createMenuItem("Add Diagnosis","Add Diagnosis Column to Spreadsheet",Icons.PLUS,this));
			builder.add(UIHelper.createMenuItem("Remove Diagnosis","Remove Diagnosis Column from Spreadsheet",Icons.MINUS,this));
			builder.addSeparator();
			builder.add(UIHelper.createMenuItem("Align Findings","Align Diagnostic Findings in Spreadsheet",Icons.ALIGN,this));
			builder.add(UIHelper.createMenuItem("Find/Replace Findings","Find Diagnoses that Contain Finding and Replace with Another Finding",Icons.REPLACE,this));
			builder.addSeparator();
			builder.add(UIHelper.createMenuItem("NO","NO/Complement selected finding",Icons.COMPLEMENT,this));
			builder.add(UIHelper.createMenuItem("OR","OR/Union/Conjunction selected findings",Icons.UNION,this));
			builder.add(UIHelper.createMenuItem("AND","AND/Intersection/Disjunction selected findings",Icons.INTERSECTION,this));
			
			
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
			menubar.add(help);
		}
		return menubar;
	}
	
	
	
	/**
	 * update table
	 */
	void updateTable(){
		// set row heights
		for(int i=0;i<model.getRowCount();i++){
			int height = -1;
			for(int j=0;j<model.getColumnCount();j++){
				Object obj = model.getValueAt(i,j);
				if(obj instanceof Icon){
					Icon icon = (Icon) obj;
					if(height < icon.getIconHeight()+PAD)
						height = icon.getIconHeight()+PAD;
				}
			}
			if(height > -1)
				table.setRowHeight(i,height);
		}
		// revalidate
		table.revalidate();
		table.repaint();
	}
	
	
	void refreshTable(){
		//table.revalidate();
		table.repaint();
	}
	
	
	/**
	 * create new case
	 */
	private void doImport(){
		(new Thread(new Runnable(){
			public void run(){
				JProgressBar p = DomainBuilder.getInstance().getProgressBar();
				p.setString("Importing Spreadsheet ...");
				DomainBuilder.getInstance().setBusy(true);
				SpreadsheetHandler.getInstance().doImportSpreadsheet(DiagnosisBuilder.this);
				DomainBuilder.getInstance().setBusy(false);
				p.setString("");
			}
		})).start();
		
	}
	
	/**
	 * do export to OWL file
	 */
	private void doExport(){
		SpreadsheetHandler.getInstance().doExportSpreadsheet(this);
	}
	
	/**
	 * edit properties
	 */
	private void doProperties(){
		ResourcePropertiesPanel panel = new ResourcePropertiesPanel(kb);
		int r = JOptionPane.showConfirmDialog(getFrame(),panel,"Properties",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			panel.saveProperties(kb);
		}
	}
	
	/**
	 * undo last action
	 */
	private void doUndo(){
		synchronizer.undo();
	}
	
	/**
	 * create new case
	 */
	private void doSave(){
		(new Thread(new Runnable(){
			public void run(){
				KnowledgeAuthor.saveOntology(kb);
			}
		})).start();
		
	}
	
	private void doPublish(){
		if(readOnly)
			return;
		
		(new Thread(new Runnable(){
			public void run(){
				
				// do regular save
				KnowledgeAuthor.saveOntology(kb);
				
				// now do export
				if(Communicator.isConnected()){
					try{
						// don't need to write it, if local copy exists
						// figure out file location and upload it if repository is not database
						File fc = new File(getLocalRepositoryFolder(),kb.getURI().getPath());
						if(DomainBuilder.getRepository() instanceof FileRepository && fc != null && fc.exists()){
							// do file upload operation
							if(!isReadOnly(kb)){
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
	 * rearrange lookup tables for displayed diagnosis so that they would all allign based on
	 * findings
	 */
	private void doOrganizeDiagnoses(){
		// build a set of all concepts in all diagnosis
		List<ConceptEntry> allconcepts = new ArrayList<ConceptEntry>();
		for(ConceptEntry d: diagnoses){
			for(Object o: d.getFindings()){
				if(o instanceof ConceptEntry){
					ConceptEntry f = (ConceptEntry) o;
					int i = allconcepts.indexOf(f);
					if(i > -1){
						ConceptEntry e = allconcepts.get(i);
						e.setCount(e.getCount()+1);
					}else{
						allconcepts.add(f);
					}
				}
			}
		}
		// sort based on frequenc
		Collections.sort(allconcepts,new Comparator<ConceptEntry>(){
			public int compare(ConceptEntry o1, ConceptEntry o2) {
				return o2.getCount()-o1.getCount();
			}
		});
		
	
		
		// now re-organize tables for diagnosis
		int n = 0;
		for(ConceptEntry a: allconcepts){
			for(ConceptEntry d: diagnoses){
				if(a.getCount() > 1){
					int i = d.getFindings().indexOf(a);
					if(i> -1){
						ConceptEntry e = (ConceptEntry) d.getFindings().get(i);
						e.setCount(a.getCount());
						d.getFindings().setEntry(e,n);
					}else
						d.getFindings().setEntry(null,n);
				}
				
			}
			n++;
		}
		updateTable();
	}
	
	/**
	 * synonym data table
	 */
	private class DiagnosisTableModel extends DefaultTableModel {
		public DiagnosisTableModel(){
		}
		public String getColumnName(int col){
			return ""+diagnoses.get(col);
		}
		public int getColumnCount(){ 
			return diagnoses.size();
		}
        public int getRowCount() { 
        	return 25;
        }
        public Object getValueAt(int row, int col) { 
        	if(col >= diagnoses.size()){
        		//System.out.println("error: "+row+","+col+" "+getColumnCount()+" "+diagnoses.size());
        		return "";
        	}
        	ConceptEntry c = diagnoses.get(col);
        	return (c != null)?c.getFindings().getEntry(row):"";
        	//if(row < 3)
        	//	System.out.println(row+","+col+" "+c+" "+c.getPatternOffset()+" "+c.getFindings().getEntry(row)+" "+getColumnCount());
        	//return o;
        }	
        
        public boolean isCellEditable(int row, int col){ 
        	return true; 
        }
        
        /**
         * set value
         */
        public void setValueAt(Object value, int row, int col) {
        	if(readOnly)
        		return;
        	
        	final ConceptEntry c = diagnoses.get(col);
        	final int place = row;
        	if(c != null){
        		if(value instanceof ConceptEntry || value instanceof ConceptExpression){
        			if(c.getFindings().contains(value))
        				return;
        			
        			c.getFindings().addEntry(value,row);
        			
        			// add sync action
        			if(c.isDisease()){
	        			final Object val = value;
	        			synchronizer.addOntologyAction(new OntologyAction(){
	        				public void run(){
	        					OntologyHelper.getConceptHandler(kb).addDiagnosis(c);
	        				}
	        				public void undo(){
	        					c.getFindings().removeEntry(place);
	        					table.repaint();
	        				}
	        				public String toString(){
	        					return "add "+val+" finding to diagnosis "+c;
	        				}
	        			});
        			}
        		}else if(value instanceof String && ((String)value).length() == 0){
        			// don't do anything if cell was already blank
        			if(getValueAt(row, col) instanceof String)
        				return;
        			
        			final Object obj = c.getFindings().removeEntry(row);
        			
        			// add sync action
        			if(c.isDisease()){
	        			synchronizer.addOntologyAction(new OntologyAction(){
	        				public void run(){
	        					OntologyHelper.getConceptHandler(kb).addDiagnosis(c);
	        				}
	        				public void undo(){
	        					c.getFindings().addEntry(obj,place);
	        					updateTable();
	        				}
	        				public String toString(){
	        					return "remove "+obj+" finding from diagnosis "+c;
	        				}
	        			});
        			}
        		}
        	}
        	//table.repaint();
        	updateTable();
        }
    }
	
	
	/**
	 * render icons where available
	 * from: http://exampledepot.com/egs/javax.swing.table/IconHead.html
	 *
	private class IconCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
		private Object selectedObject;
		private JButton button;
		private TreeDialog treeDialog;
		private int currentCol,currentRow;
		protected static final String EDIT = "edit";

		public IconCellEditor() {
			button = new JButton();
			button.setActionCommand(EDIT);
			button.addActionListener(this);
			button.setBorderPainted(false);
		}

		public void actionPerformed(ActionEvent e) {
			if(treeDialog == null){
				treeDialog = new TreeDialog(getFrame());
				treeDialog.setTitle("Select Diagnositc Findings");
				treeDialog.setRoot(kb.getClass(OntologyHelper.DIAGNOSTIC_FEATURES));
				treeDialog.setSelectionMode(TreeDialog.SINGLE_SELECTION);
			}
			treeDialog.setSelectedNode(""+selectedObject);
			treeDialog.setVisible(true);
			// add result
			Object path = treeDialog.getSelectedObject();
			if(path != null){
				IClass cls = kb.getClass(""+path);
				selectedObject = new ConceptEntry(cls);
				
				ConceptEntry d = diagnoses.get(currentCol);
				if(d != null && d.getFindings().contains(selectedObject)){
    				JOptionPane.showMessageDialog(getFrame(),"Cannot add a duplicate finding "+selectedObject,
    							"Error",JOptionPane.ERROR_MESSAGE);
    				selectedObject = null;
    				return; 
    			}
    			//  
				button.setIcon((Icon)selectedObject);
			}
		}

		// Implement the one CellEditor method that AbstractCellEditor doesn't.
		public Object getCellEditorValue() {
			return selectedObject;
		}

		//Implement the one method defined by TableCellEditor.
		public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int column) {
			currentCol = column;
			currentRow = row;
			button.setIcon(null);
			button.setText(null);
			if(value instanceof Icon)
				button.setIcon((Icon)value);
			else
				button.setText(""+value);
			selectedObject = value;
			button.doClick();
			return button;
		}
		
		public boolean isCellEditable(EventObject e){
			if(e == null){
				//editCell is invoked
				return true;
			}
			if(e instanceof MouseEvent){
				return ((MouseEvent)e).getClickCount() > 1;
			}
			return false;
		}
	}
	*/
	
    
    public JPopupMenu getPopupMenu(){
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("Add Finding","Add Finding",Icons.ADD,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Cut","Cut",Icons.CUT,this));
			popup.add(UIHelper.createMenuItem("Copy","Copy",Icons.COPY,this));
			popup.add(UIHelper.createMenuItem("Paste","Paste",Icons.PASTE,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Delete","Delete",Icons.DELETE,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("NO","NO/Complement selected finding",Icons.COMPLEMENT,this));
			popup.add(UIHelper.createMenuItem("OR","OR/Union/Conjunction selected findings",Icons.UNION,this));
			popup.add(UIHelper.createMenuItem("AND","AND/Intersection/Disjunction selected findings",Icons.INTERSECTION,this));
		}
		return popup;
	}
    
    /**
     * get or create a concept class that represents a concept
     * @param str
     * @return
     */
    IClass createConceptClass(String parent,String str){
    	// lets derive a name
    	IClass p = kb.getClass(parent);
    	final ConceptHandler cc =  OntologyHelper.getConceptHandler(kb);
    	final IClass cls = cc.createQuickConceptClass(p,str);
    	
    	// if not, lets quickly create it, but push analysis for later
    	if(cls != null){
	    	synchronizer.addOntologyAction(new OntologyAction(){
	    		public void run(){
	    			// break into words, set semantic type, etc ....
	    			cc.analyzeConceptClass(cls);
	    		}
				public void undo(){
					// NO undo for this action
				}
				public String toString(){
					return "create concept class "+cls;
				}
			});
    	}
		return cls;
    }
    
    
    /**
	 * set read only flag
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		
		// take care of toolbar
		String [] exceptions = new String []
		{"add diagnosis","remove diagnosis","align findings",
		 "export","properties","exit","tools","help"};
		UIHelper.setEnabled(getToolBar(),exceptions,!b);
		UIHelper.setEnabled(getMenuBar(),exceptions,!b);
	}
	
	
	
	/**
	 * do find replace
	 */
	private void doFindReplace(){
		// create find/replaceDialog if necessary
		if(findReplaceDialog == null){
			findReplaceDialog = new JDialog(getFrame(),"Find/Replace Findings");
			findReplaceDialog.setModal(false);
			
			TreeDialog input = new TreeDialog(getFrame());
			input.setTitle("Findings");
			input.setRoot(kb.getClass(OntologyHelper.FEATURES));
			
			find = new SingleEntryWidget("Search for Finding");
			find.setBorder(new EmptyBorder(10,10,5,10));
			find.setEntryChooser(input);
			find.setDynamicTitle(false);
			
			replace = new SingleEntryWidget("Replace with Finding");
			replace.setBorder(new EmptyBorder(10,10,10,10));
			replace.setEntryChooser(input);
			replace.setDynamicTitle(false);
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.setBorder(new CompoundBorder(new EmptyBorder(10,10,10,10),new BevelBorder(BevelBorder.RAISED)));
			p.add(find,BorderLayout.NORTH);
			p.add(replace,BorderLayout.SOUTH);
			
			JPanel b = new JPanel();
			b.setLayout(new FlowLayout(FlowLayout.CENTER));
			b.add(UIHelper.createButton("Find","Find Diagnosis that Contain this Finding in their Diagnostic Rules",null,this));
			b.add(UIHelper.createButton("Replace","Replace all Occurances of this Finding with another Finding in Diagnostic Rules",null,this));
			b.add(UIHelper.createButton("Close","Close Find/Replace Dialog",null,this));
			
			findReplaceDialog.getContentPane().setLayout(new BorderLayout());
			findReplaceDialog.getContentPane().add(p,BorderLayout.CENTER);
			findReplaceDialog.getContentPane().add(b,BorderLayout.SOUTH);
			
			findReplaceDialog.pack();
			UIHelper.centerWindow(getFrame(),findReplaceDialog);
			
		}
		findReplaceDialog.setVisible(true);	
	}

	private void doFind(){
		// find all relevant diagnosis
		ConceptEntry entry = (ConceptEntry) find.getEntry();
		if(entry == null)
			return;
		
		// now search ...
		List<ConceptEntry> found = new ArrayList<ConceptEntry>();
		for(IClass dx: kb.getClass(OntologyHelper.DISEASES).getSubClasses()){
			if(OntologyHelper.isFindingInDiagnosticRule(entry.getConceptClass(),dx.getEquivalentRestrictions())){
				found.add(new ConceptEntry(dx));
			}
		}
		// open all relevant diagnosis
		if(!diagnoses.containsAll(found))
			openDiagnoses(found);
	}
	
	private void doReplace(){
		ConceptEntry fn = (ConceptEntry) find.getEntry();
		ConceptEntry rp = (ConceptEntry) replace.getEntry();
		
		if(fn == null || rp == null)
			return;
		
		// go over all open diagnosis and set the entry
		for(ConceptEntry dx: diagnoses){
			if(!dx.isDisease())
				continue;
			// do replace
			replace(dx,dx.getFindings(),fn,rp);
		}
		refreshTable();
	}
	
	private void replace(ConceptEntry dx, ConceptExpression exp, ConceptEntry fn, ConceptEntry rp){
		//check expression
		for(int i=0;i<exp.size();i++){
			Object obj = exp.getEntry(i);
			if(obj instanceof ConceptExpression){
				replace(dx,(ConceptExpression)obj,fn,rp);
			}else if(obj instanceof ConceptEntry){
				if(fn.equals(obj)){
					// if not already there
					if(!exp.contains(rp)){
        			
						// do replace here
						if(((ConceptEntry)obj).isAbsent()){
							ConceptEntry r = rp.clone();
							r.setAbsent(true);
							exp.addEntry(r,i);
						}else
							exp.addEntry(rp,i);
	        			
						// add sync action
						final Object f = obj;
        				final Object val = rp;
	        			final ConceptEntry d = dx;
	        			final int place = i;
	        			final ConceptExpression e = exp;
	        			
	        			synchronizer.addOntologyAction(new OntologyAction(){
	        				public void run(){
	        					OntologyHelper.getConceptHandler(kb).addDiagnosis(d);
	        				}
	        				public void undo(){
	        					e.addEntry(f,place);
	        					table.repaint();
	        				}
	        				public String toString(){
	        					return "add "+val+" finding to diagnosis "+d;
	        				}
	        			});
	        			
					}
				}
			}
			
		}
	}
}
