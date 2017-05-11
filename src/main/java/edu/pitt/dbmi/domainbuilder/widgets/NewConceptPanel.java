package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;

/**
 * text panel for entering new concepts
 * @author tseytlin
 */
public class NewConceptPanel extends JPanel {
	private JTextPane textPanel;
	private JPanel headerPanel;
	
	public NewConceptPanel(){
		super();
		setLayout(new BorderLayout());
		headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel,BoxLayout.Y_AXIS));
		headerPanel.add(new JLabel("  1"));
		textPanel = new JTextPane(){
			// disable line wrapping
			public boolean getScrollableTracksViewportWidth(){
				if(getDocument().getLength() < 50)
					return true;
				return false;
			}
		};
		
		//textPanel.setToolTipText("Enter each new concept on a new line, use tabs to indicate hierarchy");
		UIHelper.setTabSize(textPanel,4);
		textPanel.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e) {}
			public void insertUpdate(DocumentEvent e) {
				try{
					String s = e.getDocument().getText(e.getOffset(),e.getLength());
					if(s.contains("\n"))
						update();
				}catch(BadLocationException ex){}
			}
			public void removeUpdate(DocumentEvent e) {
				try{
					if(e.getLength() > 1){
						update();
					}else{
						String s = e.getDocument().getText(e.getOffset(),e.getLength());
						if(s.contains("\n"))
							update();
					}
				}catch(BadLocationException ex){
					ex.printStackTrace();
				}
			}
			private void update(){
				// if more newlines, simply add
				int n = UIHelper.getNewLineCount(textPanel.getText())+1;
				int c = headerPanel.getComponentCount();
				if(n > c){
					for(int i=c;i< n;i++){
						String s = (i<9)?"  ":"";
						headerPanel.add(new JLabel(s+(i+1)));
					}
				}else if(n < c){
					for(int i=c-1;i>=n;i--){
						headerPanel.remove(i);
					}
				}
				headerPanel.validate();
				headerPanel.repaint();
			}
			
		});
		JScrollPane scroll = new JScrollPane(textPanel);
		scroll.setBorder(new TitledBorder("Enter a List of Concepts"));
		scroll.setPreferredSize(new Dimension(400,200));
		scroll.setRowHeaderView(headerPanel);
		scroll.getViewport().setBackground(Color.white);

		add(scroll,BorderLayout.CENTER);
		JLabel hint = new JLabel("HINT: enter each concept on a new line, use tabs to indicate hierarchy");
		hint.setFont(hint.getFont().deriveFont(Font.PLAIN,11));
		hint.setHorizontalAlignment(JLabel.CENTER);
		add(hint,BorderLayout.SOUTH);
	}
	
	/**
	 * get text panel
	 * @return
	 */
	public JTextPane getTextPanel(){
		return textPanel;
	}
	
	/**
	 * get entries 
	 * @return
	 */
	public String [] getEntries(){
		String str = textPanel.getText();
		ArrayList<String> list = new ArrayList<String>();
		for(String s: str.split("\n")){
			if(s.trim().length() > 0)
				list.add(s);
		}
		return list.toArray(new String [0]);
	}
	

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NewConceptPanel ncp = new NewConceptPanel();
		JOptionPane.showMessageDialog(null,ncp,"",JOptionPane.PLAIN_MESSAGE);
		System.out.println(Arrays.toString(ncp.getEntries()));
	}

}
