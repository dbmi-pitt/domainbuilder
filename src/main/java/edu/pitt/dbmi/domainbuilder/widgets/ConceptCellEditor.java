package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

import edu.pitt.ontology.ILogicExpression;

/**
 * cell renderer
 * @author Eugene Tseytlin
 *
 */
public class ConceptCellEditor extends DefaultCellEditor{
	private ConceptComboBox box;
	public ConceptCellEditor(ConceptComboBox box){
		super(box);
		this.box = box;
		setClickCountToStart(2);
	}
	//Implement the one method defined by TableCellEditor.
	public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int column) {
		if(value instanceof ILogicExpression)
			return null;
		Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		box.clear();
		box.setSelectedItem(value);
		return c;
	}
}