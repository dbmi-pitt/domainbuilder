package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptExpression;
import edu.pitt.dbmi.domainbuilder.knowledge.OntologyAction;
import edu.pitt.dbmi.domainbuilder.util.DomainTerminology;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.protege.POntology;
import edu.pitt.terminology.Terminology;


/**
 * trigger expression editor
 * @author tseytlin
 *
 */
public class TriggerEntryPanel extends JPanel implements ActionListener {
	private JTable table;
	private TriggerTableModel model;
	private final int PAD = 8;
	private ConceptExpression triggers;
	private boolean readOnly;
	private ConceptComboBox conceptEditor;
	private IOntology ontology;
	private int defaultWidth = 100;
	private TreeDialog triggerDialog;

	
	public TriggerEntryPanel(){
		super();
		createUI();
		setConceptExpression(new ConceptExpression(ILogicExpression.AND));
	}
	
	private void createUI(){
		// creat toolbar
		final int size = 16;
		JPanel toolbar = this;
		
		// custom layout
		GridBagLayout l = new GridBagLayout();
		toolbar.setLayout(l);
		GridBagConstraints c = 
			new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,new Insets(1,1,1,1),1,1);
		toolbar.setPreferredSize(new Dimension(600,Icons.CONCEPT_ICON_HEIGHT+26));
		
	
		
		//toolbar.setFloatable(false);
		toolbar.setBackground(Color.white);
		toolbar.setBorder(new TitledBorder("Triggers in the Template"));
		model = new TriggerTableModel();
		table = new JTable(model);
		table.setCellSelectionEnabled(true);
		table.setRowHeight(Icons.CONCEPT_ICON_HEIGHT+PAD);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setTableHeader(null);
		table.addKeyListener(new KeyAdapter(){
	    	//private TreeDialog treeDialog;
	    	public void keyPressed(KeyEvent k){
	    		if(k.getKeyCode() == KeyEvent.VK_DELETE){
					doDelete(true);
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
		
		// set column properties
		for(int i=0;i<model.getColumnCount();i++){
			TableColumn col = table.getColumnModel().getColumn(i);
			//col.setResizable(false);
			//col.setMinWidth(Icons.CONCEPT_ICON_WIDTH+16);
			//col.setMaxWidth(Icons.CONCEPT_ICON_WIDTH+16);
			col.setHeaderRenderer(new IconCellRenderer(true));
			col.setCellRenderer(new IconCellRenderer(false));
			defaultWidth = col.getPreferredWidth();
		}

		
		JScrollPane stable = new JScrollPane(table);
		stable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		stable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		//stable.setPreferredSize(new Dimension(500,Icons.CONCEPT_ICON_HEIGHT+26));
		
		// add components
		JComponent comp = UIHelper.createButton("Add Trigger","Add Trigger",Icons.PLUS,size,this);
		l.setConstraints(comp,c);
		toolbar.add(comp);
		
		comp = UIHelper.createButton("Remove Trigger","Remove Trigger",Icons.MINUS,size,this);
		c.gridy++;
		l.setConstraints(comp,c);
		toolbar.add(comp);
				
		c.gridx++;
		c.gridy = 0;
		c.gridheight = 3;
		c.weightx = 1.0;
		l.setConstraints(stable,c);
		toolbar.add(stable);
		
		
		comp = UIHelper.createButton("OR  ","OR/Union/Conjunction selected findings",Icons.LINE,size,-1,true,this);
		c.weightx = 0;
		c.gridx++;
		c.gridheight = 1;
		l.setConstraints(comp,c);
		toolbar.add(comp);
		
		
		comp = UIHelper.createButton("AND ","AND/Intersection/Disjunction selected findings",Icons.LINE,size,-1,true,this);
		c.gridx++;
		l.setConstraints(comp,c);
		toolbar.add(comp);
		
		comp = UIHelper.createButton("NOT ","NOT/Complement selected finding",Icons.LINE,size,-1,true,this);
		c.gridy++;
		c.gridx--;
		l.setConstraints(comp,c);
		toolbar.add(comp);
		
	}
	
	public void reset(){
		setConceptExpression(new ConceptExpression(ILogicExpression.AND));
	}
    
	/**
	 * @param ontology the ontology to set
	 */
	public void setOntology(IOntology ont) {
		ontology = ont;
		
		Terminology term = ont.getRepository().getTerminology(OntologyHelper.ONTOLOGY_TERMINOLOGY);
		if(term == null)
			term = new DomainTerminology(ont);
		
		
		// init concept editor
		conceptEditor = new ConceptComboBox((DomainTerminology) term);
		conceptEditor.addDefaultParent(ontology.getClass(OntologyHelper.CONCEPTS));
		
		// set column properties
		for(int i=0;i<model.getColumnCount();i++){
			TableColumn col = table.getColumnModel().getColumn(i);
			col.setCellEditor(new ConceptCellEditor(conceptEditor));
		}
		
		setReadOnly(OntologyHelper.isReadOnly(ont));
		
	}
	
    /**
	 * set read only flag
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		
		// take care of toolbar
		UIHelper.setEnabled(this,new String [0],!b);
	}
	
	public void setEnabled(boolean b){
		if(b && OntologyHelper.isReadOnly(ontology))
			return;
		
		super.setEnabled(b);
		// take care of toolbar
		UIHelper.setEnabled(this,new String [0],b);
	}
	
	/**
	 * get concept expression
	 * @return
	 */
	public ConceptExpression getConceptExpression(){
		return triggers;
	}
	
	/**
	 * get concept expression
	 * @return
	 */
	public void setConceptExpression(ConceptExpression c){
		triggers = c;
		updateTable();
	}
	
	
	/**
	 * get frame for component
	 * @return
	 */
	public Frame getFrame(){
		return JOptionPane.getFrameForComponent(this);
	}
	
	/**
	 * add schema
	 */
	public void doAddTrigger(){
		if(triggerDialog == null){
			triggerDialog = new TreeDialog(getFrame());
			triggerDialog.setTitle("Select Trigger");
			triggerDialog.setRoot(ontology.getClass(OntologyHelper.CONCEPTS));
		}
		triggerDialog.setVisible(true);
		// add result
		int i=0;
		int col = table.getSelectedColumn();
		if(col < 0){
			// advance to next empty column
			for(col = 0;!(triggers.getEntry(col) instanceof String); col++);
		}
		for(ConceptEntry entry: (ConceptEntry [])triggerDialog.getSelectedObjects()){
			if(!triggers.contains(entry)){
				table.setValueAt(entry,0,col+i++);
			}
		}
	}
	

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand().toLowerCase().trim();
		if(cmd.equals("delete") || cmd.equals("remove trigger")){
			doDelete(true);
		}else if(cmd.equals("not")){
			doComplement();
		}else if(cmd.equals("or")){
			doUnion();
		}else if(cmd.equals("and")){
			doIntersection();
		}else if(cmd.equals("add trigger")){
			doAddTrigger();
		}
	}
	
	
	/**
	 * update table
	 */
	void updateTable(){
		// set row heights
		for(int j=0;j<model.getColumnCount();j++){
			Object obj = model.getValueAt(0,j);
			int width = -1;
			if(obj instanceof Icon){
				Icon icon = (Icon) obj;
				if(width < icon.getIconWidth()+PAD)
					width = icon.getIconWidth()+PAD;
			}
			table.getColumnModel().getColumn(j).setPreferredWidth((width > -1)?width:defaultWidth);
		}
		
		
		// revalidate
		table.revalidate();
		table.repaint();
		
		// fire edit
		firePropertyChange("EDIT_TRIGGER",null,null);
	}
	
	/**
	 * synonym data table
	 */
	private class TriggerTableModel extends DefaultTableModel {
		public TriggerTableModel(){
		}
		public String getColumnName(int col){
			return "";
		}
		public int getColumnCount(){ 
			return 25;
		}
        public int getRowCount() { 
        	return 1;
        }
        public Object getValueAt(int row, int col) { 
        	return triggers.getEntry(col);
        }	
        
        public boolean isCellEditable(int row, int col){ 
        	return true; 
        }
        
        /**
         * set value
         */
        public void setValueAt(Object value, int r, int c) {
        	if(readOnly)
        		return;
        	
        	final int place = c;
    		if(value instanceof ConceptEntry || value instanceof ConceptExpression){
    			if(triggers.contains(value))
    				return;
    			
    			triggers.addEntry(value,place);
    		}else if(value instanceof String && ((String)value).length() == 0){
    			// don't do anything if cell was already blank
    			if(getValueAt(r, c) instanceof String)
    				return;
    			
    			triggers.removeEntry(c);
    		}
        	
        	//table.repaint();
        	updateTable();
        	
        }
    }
	
	public void doDelete(boolean prompt){
		int [] cols = table.getSelectedColumns();
		int [] rows = table.getSelectedRows();
		if(cols.length > 0){
			int r = (prompt)?JOptionPane.showConfirmDialog(this,"Are you sure you want to delete selected values?",
					"Question",JOptionPane.YES_NO_OPTION):JOptionPane.YES_OPTION;
			if(r == JOptionPane.YES_OPTION){
				for(int i=0;i<rows.length;i++)
					for(int j=0;j<cols.length;j++)
						table.setValueAt("",rows[i],cols[j]);
			}
		}
		updateTable();
	}
	
	
	private void doComplement(){
		int [] cols = table.getSelectedColumns();
		if(cols.length > 0){
			StringBuffer problems = new StringBuffer();
			for(int j=0;j<cols.length;j++){
				Object obj = table.getValueAt(0,cols[j]);
				if(obj instanceof ConceptEntry){
					ConceptEntry e = (ConceptEntry) obj;
					//if(OntologyHelper.isFeature(e.getConceptClass())){
					e.setAbsent(!e.isAbsent());
					//}else
					//	problems.append(e.getName()+" ");
				}
			}
			/*
			if(problems.length() > 0){
				JOptionPane.showMessageDialog(this,
						"Cannot negate non diagnostic findings: "+problems,
						"Warning",JOptionPane.WARNING_MESSAGE);
			}*/
			
			table.repaint();
		}
	}
	
	/**
	 * create a union expression
	 */
	private void doUnion(){
		int [] cols = table.getSelectedColumns();
		if(cols.length > 1){
			ConceptExpression exp = new ConceptExpression(ILogicExpression.OR);
			exp.setHorizontalIcon(true);
			
			List toremove = new ArrayList();
			for(int i=0;i<cols.length;i++){
				Object obj = table.getValueAt(0,cols[i]);
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
			triggers.add(exp);
			triggers.removeAll(toremove);
			triggers.setEntry(exp,cols[0]);
			table.clearSelection();
			updateTable();
		}else
			JOptionPane.showMessageDialog(this,"Invalid selection!","Error",JOptionPane.ERROR_MESSAGE);
		
	}
	
	
	/**
	 * break up union expresssion
	 */
	private void doIntersection(){
		int [] cols = table.getSelectedColumns();
		//int [] rows = table.getSelectedRows();
		if(cols.length  > 0){
			List<ConceptExpression> toremove = new ArrayList<ConceptExpression>();
			for(int i=0;i<cols.length;i++){
				Object obj = table.getValueAt(0,cols[i]);
				if(obj instanceof ConceptExpression){
					ConceptExpression e = (ConceptExpression) obj;
					if(e.getExpressionType() == ILogicExpression.OR){
						triggers.addAll(e);
						toremove.add(e);
					}
					
				}
			}
			if(!toremove.isEmpty()){
				triggers.removeAll(toremove);
				updateTable();
			}else{
				JOptionPane.showMessageDialog(this,"Selected values are already disjoint!","Warning",JOptionPane.WARNING_MESSAGE);
			}
		}else
			JOptionPane.showMessageDialog(this,"Invalid selection!","Error",JOptionPane.ERROR_MESSAGE);
	}
	
}
