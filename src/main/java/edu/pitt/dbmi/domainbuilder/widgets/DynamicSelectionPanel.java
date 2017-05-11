package edu.pitt.dbmi.domainbuilder.widgets;

import javax.swing.*;
import javax.swing.Timer;
//import javax.swing.border.TitledBorder;
//import javax.swing.event.*;
//import javax.swing.text.*;

//import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.slideviewer.ViewerHelper;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * This is a panel that allows dynamic selection of a set
 * of objects
 * @author tseytlin
 */
public class DynamicSelectionPanel extends JPanel implements EntryChooser {
	private JList input;
	private Vector selectableObjects;
	//private TitledBorder lbl;
	private Frame frame;
	private JTextField inputText;
	private boolean ok;
	private Timer timer;
	private ViewerHelper.FilterDocument filter; 
	
	public DynamicSelectionPanel(){
		super();
		setLayout(new BorderLayout());
		input = new JList();
		//createSelectionPanel();
		add(ViewerHelper.createDynamicSelectionPanel(input,Collections.EMPTY_LIST),BorderLayout.CENTER);
		
		// find text box
		inputText = getInputText(this);
		filter = (ViewerHelper.FilterDocument) inputText.getDocument();
	}
	
	
	/**
	 * find input text
	 * @return
	 */
	private JTextField getInputText(Component cont){
		if(cont instanceof JTextField){
			return (JTextField) cont;
		}
		JTextField text = null;
		if(cont instanceof Container){
			for(Component comp : ((Container)cont).getComponents()){
				text = getInputText(comp);
				if(text != null)
					return text;
			}
		}
		return text;
	}
	
	
	/**
	 * set objects that can be selected
	 * @param c
	 */
	public void setSelectableObjects(Collection c){
		if(c instanceof Vector){
			selectableObjects = (Vector) c;
		}else{
			selectableObjects = new Vector();
			selectableObjects.addAll(c);
		}
		input.setListData(selectableObjects);
		filter.setSelectableObjects(selectableObjects);
	}
	
	/**
	 * set objects that can be selected
	 * @param c
	 */
	public void setSelectableObjects(Object [] c){
		setSelectableObjects(Arrays.asList(c));
	}
	
	
	/**
	 * get selected value
	 * @return
	 */
	public Object getSelectedValue(){
		return input.getSelectedValue();
	}
	
	/**
	 * set label text
	 * @param txt
	 */
	public void setLabel(String txt){
		//lbl.setTitle(txt);
	}
	
	/**
	 * Create project selection dialog component
	 *
	private void createSelectionPanel(){
		JPanel panel = this;
		panel.setLayout(new BorderLayout());
		
		lbl = new TitledBorder("Select an object you want to load:");
		//panel.add(lbl,BorderLayout.NORTH);
		
		// create list populated w/ available images
		// ???? queryServlet should be moved to AuthorUtils
		input = new JList();
		
		//input.setCellRenderer(new SlideInfoCellRenderer());
		
		JScrollPane scroll = new JScrollPane(input);
		scroll.setPreferredSize(new Dimension(300,200));
		scroll.setBorder(lbl);
		panel.add(scroll,BorderLayout.CENTER);
		
		// create text field
		
		inputText = new JTextField();
		filter = new FilterDocument(inputText,input);
		inputText.setDocument(filter);
		
		//		inputText.addAncestorListener(new AncestorListener() {
		//            public void ancestorAdded(AncestorEvent event) {
		//                // for a simple component: request
		//            	inputText.requestFocusInWindow();
		//                // for a compound: go to next focusable which is the editor
		//            	inputText.transferFocus();
		//            }
		//            public void ancestorMoved(AncestorEvent event) {}
		//            public void ancestorRemoved(AncestorEvent event) { }
		//        });
		 
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(inputText,BorderLayout.CENTER);
		p.setBorder(new TitledBorder("Search"));
		panel.add(p,BorderLayout.SOUTH);
		
		// add listener to list
		input.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(inputText != null && input.getSelectedValue() != null)
					inputText.setText(input.getSelectedValue().toString());
			}
		});
		input.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// iterate through parents
					for(Component c=(Component) e.getSource();c != null;c=c.getParent()){
						if(c instanceof JOptionPane){
							// set OK close option
							((JOptionPane)c).setValue(new Integer(JOptionPane.OK_OPTION));
						}else if(c instanceof Dialog){
							// we found dialog, great, lets close it
							((Dialog) c).dispose();
							break;
						}
					}
				}
			}
		});
	}
	*/

	public Object getSelectedObject() {
		return input.getSelectedValue();
	}

	public Object[] getSelectedObjects() {
		return input.getSelectedValues();
	}

	public int getSelectionMode() {
		if(input.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION){
			return SINGLE_SELECTION;
		}else
			return MULTIPLE_SELECTION;
	}

	public boolean isSelected() {
		return ok;
	}

	public void setSelectionMode(int mode) {
		input.setSelectionMode((mode == SINGLE_SELECTION)?
				ListSelectionModel.SINGLE_SELECTION:
				ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	public void setOwner(Frame f){
		this.frame = f;
	}
	
	/**
	 * show dialog
	 */
	public void showChooserDialog() {
		// THIS IS A HACK TO GET AROUND SUN JAVA BUG
		timer = new Timer(100,new ActionListener() {
			public void actionPerformed(ActionEvent e){
	            if(inputText.hasFocus()) {
	            	timer.setRepeats(false);
	                return;
	            }
	            inputText.requestFocusInWindow();
	        }
	 
	    });
		timer.setRepeats(true);
		timer.start();

		int r = JOptionPane.showConfirmDialog(frame,this,"Open Slide Image",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		ok = r == JOptionPane.OK_OPTION;
	}
}
