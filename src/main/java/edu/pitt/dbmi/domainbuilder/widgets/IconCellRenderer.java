package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;

/**
 * render icons where available
 * from: http://exampledepot.com/egs/javax.swing.table/IconHead.html
 */
public class IconCellRenderer extends DefaultTableCellRenderer {
    private final Color diagnosisColor = new Color(255,255,100);
    private final Color SIMILAR_COLOR = new Color(225,225,240);
    private boolean isHeader;
	private static int numberOfDiagnoses;
	
    public static void setNumberOfDiagnoses(int numberOfDiagnoses) {
		IconCellRenderer.numberOfDiagnoses = numberOfDiagnoses;
	}

	public IconCellRenderer(boolean header){
		isHeader = header;
	}
	
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof Icon){
        	JLabel lbl = new JLabel();
        	lbl.setOpaque(true);
        	// Inherit the colors and font from the header component
            if (table != null && isHeader) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                	int c = table.getSelectedColumn();
                	int [] r = table.getSelectedRows();
                	if(c == column && r.length == table.getRowCount())
                		lbl.setBackground(table.getSelectionBackground());
                	else
                		lbl.setBackground(diagnosisColor);
                }
            }else{
            	boolean highlight = false;
            	if(value instanceof ConceptEntry && numberOfDiagnoses > 2)
            		highlight = ((ConceptEntry)value).getCount() > numberOfDiagnoses/2;
            	lbl.setBackground((isSelected)?table.getSelectionBackground():(highlight)?SIMILAR_COLOR:Color.white);
             	//lbl.setBackground((isSelected)?table.getSelectionBackground():Color.white);
            }
            lbl.setIcon((Icon)value);
            if(isHeader)
            	lbl.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            lbl.setHorizontalAlignment(JLabel.CENTER);
            lbl.setToolTipText(""+value);
            return lbl;
        }
    	return super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);	
    }
};