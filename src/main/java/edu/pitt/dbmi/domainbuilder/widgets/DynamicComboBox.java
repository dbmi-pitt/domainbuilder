package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;



public class DynamicComboBox extends JComboBox implements DocumentListener {
	private JTextField text;
	private Object [] content;
	private boolean block,selected;
	
	public DynamicComboBox(){
		this(new Object [0]);
	}
	
	public DynamicComboBox(Collection content){
		this(content.toArray());
	}
	
	public DynamicComboBox(Object [] content){
		super(content);
		this.content = content;
		setEditable(true);
		text = (JTextField) getEditor().getEditorComponent();
		text.getDocument().addDocumentListener(this);
		putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		//setPreferredSize(new Dimension(160,25));
		
		// make popup bigger then combobox size
		//http://forums.java.net/jive/message.jspa?messageID=61267
		/*
		addPopupMenuListener(new PopupMenuListener(){
			 public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                JComboBox box = (JComboBox) e.getSource();
                Object comp = box.getUI().getAccessibleChild(box, 0);
                if (!(comp instanceof JPopupMenu)) 
                	return;
                JComponent scrollPane = (JComponent) ((JPopupMenu) comp).getComponent(0);
                int width  = box.getPreferredSize().width;
                int height = scrollPane.getPreferredSize().height;
                if(scrollPane instanceof JScrollPane){
                	Component c = ((JScrollPane)scrollPane).getViewport().getView();
                	if(c != null){
                		int w = c.getPreferredSize().width;
                		if(w > width)
                			width = w;
                	}
                }
                Dimension size = new Dimension(width,height);
                scrollPane.setPreferredSize(size);
                //  following line for Tiger
                scrollPane.setMaximumSize(size);
            }
			public void popupMenuCanceled(PopupMenuEvent e) {}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
		});
		*/
	}
	
	/**
	 * get text editor
	 */
	public JTextField getTextEditor(){
		return text;
	}
	
	/**
	 * notify that we are done
	 */
	protected void fireActionEvent() {
		//disable firing during sync
		//System.out.println(block+" "+selected);
		if(!block || selected){ 
			super.fireActionEvent();
		}
	}

	public void clear(){
		removeAllItems();
		text.setText("");
	}
	
	
	//sync combobox w/ what is typed in
	private void sync(String str) {
		removeAllItems();
		hidePopup();
		boolean show = false;
		if(str != null && str.length() > 0){
			for(Object word: getMatchingObjects(str)){
				addItem(word);
				show = true;
			}
		}else{
			for(Object word: content){
				addItem(word);
			}
		}
		revalidate();
		if(show && isShowing())
			showPopup();
		text.setText(str);
		
	}

	
	private synchronized void sync(){
		block = true;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				sync(text.getText());
				block = false;
			}
		});
	}
	
	/**
	 * 
	 * @param str
	 * @return
	 */
	private List getMatchingObjects(String str){
		str = str.toLowerCase();
		List list = new ArrayList();
		for(Object w: content){
			if(w.toString().toLowerCase().startsWith(str)){
				list.add(w);
			}
		}
		return list;
	}
	
	
	public void changedUpdate(DocumentEvent arg0) {
		if(!block)
			sync();
	}

	public void insertUpdate(DocumentEvent arg0) {
		if(!block)
			sync();
	}

	public void removeUpdate(DocumentEvent arg0) {
		if(!block)
			sync();
	}
}
