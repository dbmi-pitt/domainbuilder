package edu.pitt.dbmi.domainbuilder.util;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.STATUS_COMPLETE;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.STATUS_INCOMPLETE;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.STATUS_TESTED;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.text.html.HTMLDocument;

import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.CaseEntry;
import edu.pitt.dbmi.domainbuilder.widgets.DynamicComboBox;


import java.util.*;
import java.util.List;

/**
 * singleton class with bunch of useful methods
 * @author tseytlin
 *
 */
public class UIHelper {
	// some global UI vars
	private static boolean debug,conceptColorStatus,conceptBlinkStatus;
	public static String UI_CLOSING_EVENT = "UI_CLOSING";
	//public static final String HELP_FILE = "/resources/DomainBuilder.html"; 
	public static final String HELP_FILE = "http://slidetutor.upmc.edu/domainbuilder/manual/index.html"; 
	private static final String[] colors = { "BLACK", "BLUE", "CYAN", "GRAY", "GREEN", "MAGENTA", "ORANGE", "PINK",
		"RED", "WHITE", "YELLOW" };
	
	public static boolean getConceptBlinkStatus() {
		return conceptBlinkStatus;
	}

	public static void setConceptBlinkStatus(boolean conceptBlinkStatus) {
		UIHelper.conceptBlinkStatus = conceptBlinkStatus;
	}


		
	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(boolean debug) {
		UIHelper.debug = debug;
	}

	public static boolean getConceptColorStatus() {
		return conceptColorStatus;
	}

	
	/**
	 * get number of newlines in a string
	 * @param str
	 * @return
	 */
	public static int getNewLineCount(String str){
		int count = 0;
		for(int i = 0;i<str.length();i++){
			if(str.charAt(i) == '\n')
				count ++;
		}
		return count;
	}
	
	/**
	 * get number of tabs that are prefixed in a string
	 * @param s
	 * @return
	 */
	public static int getTabOffset(String str){
		int count = 0;
		for(int i = 0;i<str.length();i++){
			if(str.charAt(i) == '\t')
				count ++;
			else
				break;
		}
		return count;
	}
	
	/**
	 * pretty print description
	 * 
	 * @param text
	 * @return
	 */
	public static String getDescription(String text,int w, int h) {
		if(text == null)
			text = "&nbsp;<br>&nbsp;";
		return "<html><table width="+w+" height="+h+" cellpadding=10 bgcolor=\"#FFFFCC\">" + text + "</table></html>";
	}
	
	/**
	 * This method gets a text file (HTML too) from input stream 
	 * reads it, puts it into string and subbstitutes keys for values in 
	 * from given map
	 * @param InputStream text input
	 * @param Map key/value substitution (used to substitute paths of images for example)
	 * @return String that was produced
	 * @throws IOException if something is wrong
	 * WARNING!!! if you use this to read HTML text and want to put it somewhere
	 * you should delete newlines
	 */
	public static String getText(InputStream in, Map sub) throws IOException {
		StringBuffer strBuf = new StringBuffer();
		BufferedReader buf = new BufferedReader(new InputStreamReader(in));
		try {
			for (String line = buf.readLine(); line != null; line = buf.readLine()) {
				strBuf.append(line.trim() + "\n");
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			buf.close();
		}
		// we have our text
		String text = strBuf.toString();
		// do substitution
		if (sub != null) {
			for (Iterator i = sub.keySet().iterator(); i.hasNext();) {
				String key = i.next().toString();
				text = text.replaceAll(key, "" + sub.get(key));
			}
		}
		return text;
	}
	
	
	/**
	 * Set tab size. I don't know why it has to be so damn complicated in java
	 * this code was shamelessly copied from: 
	 * http://forum.java.sun.com/thread.jspa?threadID=585006&messageID=3002940
	 */
	public static void setTabSize(JTextPane text, int charactersPerTab) {
		Component comp = text;
		FontMetrics fm = comp.getFontMetrics(text.getFont());
		int charWidth = fm.charWidth('w');
		int tabWidth = charWidth * charactersPerTab;

		TabStop[] tabs = new TabStop[20];

		for (int j = 0; j < tabs.length; j++) {
			int tab = j + 1;
			tabs[j] = new TabStop(tab * tabWidth);
		}

		TabSet tabSet = new TabSet(tabs);
		SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setTabSet(attributes, tabSet);
		((StyledDocument)text.getDocument()).
		setParagraphAttributes(0,text.getDocument().getLength(), attributes, false);
	}
	
	
	
	
	public static void setConceptColorStatus(boolean conceptColorStatus) {
		UIHelper.conceptColorStatus = conceptColorStatus;
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, String icon,int size, int m, ActionListener listener){
		return createButton(name,tip,icon,size,m,false,listener);
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, String icon,int size, int m, boolean showname,ActionListener listener){
		JButton n = new JButton();
		if(showname)
			n.setText(name);
		if(icon != null)
			n.setIcon(Icons.getIcon(icon,size));
		else
			n.setText(name);
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		if(m > -1)
			n.setMnemonic(m);
		return n;
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JMenuItem createMenuItem(String name, String tip, String icon, ActionListener listener){
		return createMenuItem(name, tip, icon,-1,listener);
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JMenuItem createMenuItem(String name, String tip, String icon, int m,ActionListener listener){
		JMenuItem n = new JMenuItem(name);
		if(icon != null)
			n.setIcon(Icons.getIcon(icon,16));
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		if(m > -1)
			n.setMnemonic(m);
		return n;
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JCheckBoxMenuItem createCheckboxMenuItem(String name, String tip, String icon, ActionListener listener){
		return createCheckboxMenuItem(name, tip, icon,false, listener);
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JCheckBoxMenuItem createCheckboxMenuItem(String name, String tip, String icon, boolean t, ActionListener listener){
		JCheckBoxMenuItem n = new JCheckBoxMenuItem(name);
		if(icon != null)
			n.setIcon(Icons.getIcon(icon,16));
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		n.setSelected(t);
		return n;
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JRadioButtonMenuItem createRadioMenuItem(String name, String tip, Icon icon, boolean t, ButtonGroup grp, ActionListener listener){
		JRadioButtonMenuItem n = new JRadioButtonMenuItem(name);
		if(icon != null)
			n.setIcon(icon);
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addActionListener(listener);
		n.setSelected(t);
		grp.add(n);
		return n;
	}
	
	
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, String icon, ActionListener listener){
		return createButton(name, tip, icon,24,-1,listener);
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JButton createButton(String name, String tip, String icon,int size, ActionListener listener){
		return createButton(name, tip, icon,size,-1,listener);
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JToggleButton createToggleButton(String name, String tip, String icon,int size,int m,ItemListener listener){
		JToggleButton n = new JToggleButton(Icons.getIcon(icon,size));
		n.setToolTipText(tip);
		n.setActionCommand(name);
		n.addItemListener(listener);
		if(m > -1)
			n.setMnemonic(m);
		return n;
	}
	
	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JToggleButton createToggleButton(String name, String tip, String icon, ItemListener listener){
		return createToggleButton(name, tip, icon,24,-1,listener);
	}

	/**
	 * create button with na
	 * @param name/command
	 * @param tool tip
	 * @param icon
	 * @return
	 */
	public static JToggleButton createToggleButton(String name, String tip, String icon, int size, ItemListener listener){
		return createToggleButton(name, tip, icon,size,-1,listener);
	}
	
	
	/**
	 * create a JPanel that gets/sets objects getter and setter
	 * fields for normal datatypes
	 * @param bean
	 * @return
	 */
	public static JPanel createBeanSetterPanel(Object bean){
		Class cls = bean.getClass();
		Method [] methods = cls.getMethods();
		java.util.List<String> list = new ArrayList<String>();
		for(int i=0;i<methods.length;i++){
			// see if this is a getter
			if(methods[i].getName().startsWith("get")){
				String smethod = "set"+methods[i].getName().substring(3);
				Class rt = methods[i].getReturnType();
				//make sure it has a setter with same parameter
				boolean hasSetter = false;
				try{
					cls.getMethod(smethod, new Class [] {rt});	
					hasSetter = true;
				}catch(Exception ex){}
				// now if setter is available, make sure that type is normal
				if(hasSetter){
					if(rt.isPrimitive() || rt.getName().startsWith("java.lang.")){
						// NOW THIS IS WHAT WE WANT
						list.add(methods[i].getName().substring(3));
					}
				}
			}
		}
		return createBeanSetterPanel(bean, list.toArray(new String[0]));
	}
	
	/**
	 * create a JPanel that gets/sets objects getter and setter
	 * fields for normal datatypes
	 * @param bean
	 * @param method list assumes that methods ARE THERE!!!!
	 * @return
	 */
	public static JPanel createBeanSetterPanel(Object bean, String [] methods){
		final Object target = bean;
		Class cls = bean.getClass();
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setBorder(new TitledBorder(""+bean));
		// init constraints
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,5,5,5);
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 0;
		
		// iterate over method names
		for(int i=0;i<methods.length;i++){
			c.gridx = 0;
			c.gridwidth = 1;
			// strip get/set prefix
			String meth = methods[i];
			String get = "get";
			String set = "set";
			if(meth.startsWith("get") || meth.startsWith("set")){
				meth = meth.substring(3);
			}else if(meth.startsWith("is")){
				meth = meth.substring(2);
				get = "is";
			}
			
			try{
				// get getter and setter methods
				final Method getter = cls.getMethod(get+meth,new Class [0]);
				final Method setter = cls.getMethod(set+meth,new Class []{getter.getReturnType()});	
			
				// create label and text field based on datatype
				
				if(getter.getReturnType().equals(Boolean.TYPE)){
					// for boolean create checkbox
					final JCheckBox check = new JCheckBox(deriveName(meth));
					c.gridwidth = 2;
					panel.add(check);
					
					// set status
					Boolean b = (Boolean) getter.invoke(bean,new Object [0]);
					check.setSelected(b.booleanValue());
					check.addItemListener(new ItemListener(){
						public void itemStateChanged(ItemEvent e){
							try{
								setter.invoke(target,new Object []{new Boolean(check.isSelected())});
							}catch(Exception ex){
								ex.printStackTrace();
							}
						}
					});	
				}else{
					// for everything else label and text box
					JLabel lbl = new JLabel(deriveName(meth));
					panel.add(lbl,c);
					c.gridx++;
					final JTextField txt = new JTextField(15);
					panel.add(txt,c);
				
					
					// set text from object and make sure that answer synchronizes
					// invoke getter to set in text field
					Object v = getter.invoke(bean,new Object [0]);
					txt.setText((v != null)?""+v:"");
					txt.addFocusListener(new FocusListener(){
						public void focusGained(FocusEvent e){}
						public void focusLost(FocusEvent e){
							Object param = "";
							// TODO: handle more types
							if(getter.getReturnType().equals(String.class)){
								param = txt.getText();
							}else if(getter.getReturnType().equals(Integer.TYPE)){
								param = new Integer(txt.getText());
							}
							try{
								setter.invoke(target,new Object []{param});
							}catch(Exception ex){
								ex.printStackTrace();
							}
						}
					});
				}
				// next row please
				c.gridy++;
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
		return panel;
	}
	
	
	/**
	 * derive pretty name from method name
	 * @param method
	 * @return
	 */
	private static String deriveName(String method){
		return method.replaceAll("([a-z])([A-Z])","\\1 \\2").replaceAll("_"," ");
	}
	
	/**
	 * Derive prettier version of a class name
	 * @param name
	 * @return
	 */
	public static String getPrettyClassName(String name){
		// if name is in fact URI, just get a thing after hash
		int i = name.lastIndexOf("#");
		if(i > -1){
			name = name.substring(i+1);
		}
				
		// strip prefix (if available)
		i = name.indexOf(":");
		if(i > -1){
			name = name.substring(i+1);
		}
		
		
		// strip suffix
		//if(name.endsWith(OntologyHelper.WORD))
		//	name  = name.substring(0,name.length()-OntologyHelper.WORD.length());
		
		// possible lowercase values to make things look prettier
		if(!name.matches("[A-Z_\\-\\'0-9 ]+") &&
		   !name.matches("[a-z][A-Z_\\-\\'0-9]+[\\w\\-]*")	)
			name = name.toLowerCase();
			
		// now replace all underscores with spaces
		return name.replaceAll("_"," ");
	}
	
	
	/**
	 * Show OK/Cancel dialog to get user input
	 * @param parent frame
	 * @param message to prompt
	 * @param icon can be null
	 * @return input or null if canceled or empty
	 */
	public static String showInputDialog(Component comp, String message, Icon icon){
		return showInputDialog(comp, message,null,icon);
	}
	
	/**
	 * Show OK/Cancel dialog to get user input
	 * @param parent frame
	 * @param message to prompt
	 * @param icon can be null
	 * @return input or null if canceled or empty
	 */
	public static String showInputDialog(Component comp, String message, String str, Icon icon){
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextField text = new JTextField(20);
		if(str != null)
			text.setText(str);
		p.add(new JLabel(message),BorderLayout.NORTH);
		p.add(text,BorderLayout.CENTER);
		Frame frame = JOptionPane.getFrameForComponent(comp);
		
		// THIS IS A HACK TO GET AROUND SUN JAVA BUG
		(new FocusTimer(text)).start();
		int r = JOptionPane.showConfirmDialog(frame,p,message,
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,icon);
		if(r == JOptionPane.OK_OPTION && text.getText().length() > 0){
			return text.getText();
		}
		return null;
	}
	
	/**
	 * Show OK/Cancel dialog to get user input
	 * @param parent frame
	 * @param message to prompt
	 * @param icon can be null
	 * @return input or null if canceled or empty
	 */
	public static String showComboBoxDialog(Component comp, String message, String str, String [] content, Icon icon){
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JComboBox text = new DynamicComboBox(content);
		text.setEditable(true);
		if(str != null)
			text.setSelectedItem(str);
		else
			text.setSelectedItem("");
		
		
		p.add(new JLabel(message),BorderLayout.NORTH);
		p.add(text,BorderLayout.CENTER);
		Frame frame = JOptionPane.getFrameForComponent(comp);
		
		// THIS IS A HACK TO GET AROUND SUN JAVA BUG
		(new FocusTimer((JComponent) text.getEditor().getEditorComponent())).start();
		int r = JOptionPane.showConfirmDialog(frame,p,message,
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,icon);
		if(r == JOptionPane.OK_OPTION && (""+text.getSelectedItem()).length() > 0){
			return ""+text.getSelectedItem();
		}
		return null;
	}
	
	/**
	 * request timer to focus on given component
	 * can only invoke one instance at a time
	 * @author tseytlin
	 *
	 */
	public static class FocusTimer extends Timer {
		private static FocusTimer instance;
		public FocusTimer(JComponent comp){
			this(createListener(comp));
		}
		public FocusTimer(ActionListener listener){
			super(100,listener);
			setRepeats(true);
			instance = this;
		}
		
		public static ActionListener createListener(JComponent c){
			final JComponent component = c;
			return new ActionListener() {
				public void actionPerformed(ActionEvent e){
		            if(component != null){
						if(component.hasFocus()) {
							if(instance != null)
								instance.setRepeats(false);
			                return;
			            }
						component.requestFocusInWindow();
		            }
		        }
			};
		}
	}
	
	
	
	/**
	 * get window for component (frame/dialog)
	 * @param comp
	 * @return
	 */
	public static Window getWindowForComponent(Component comp){
		if(comp == null)
			return null;
		if(comp instanceof Window)
			return (Window) comp;
		return getWindowForComponent(comp.getParent());
	}
	
	/**
	 * Center child window on top of parent window
	 * @param Component parent window
	 * @param Component child window
	 */
	public static void centerWindow(Component parent, Component child) {
		try{
			Point p = parent.getLocationOnScreen();
			Dimension fs = parent.getSize();
			Dimension ds = child.getSize();
		
			
			if (p != null && fs != null && ds != null) {
				int x = p.x + (fs.width / 2) - (ds.width / 2);
				int y = p.y + (fs.height / 2) - (ds.height / 2);
				child.setLocation(x, y);
			}
		}catch(Exception ex){
			//we don't care enough to center it to crash everything else
		}
	}
	
	/**
	 * convert array of generic objects into array of specific objects
	 * @param cls
	 * @param s
	 * @return
	 */
	public static Object [] convertArray(Class cls, Object [] s){
		Object [] trg = (Object []) Array.newInstance(cls,s.length);
		for(int i=0;i<s.length;i++)
			trg[i] = s[i];
		return trg;
	}
	
	/** 
	 * This seems to be the only way to antialias fonts in labels.
	 */
	public static class Label extends JLabel{
		public Label(String text){
			super(text);	
		}
		public void paintComponent(Graphics g){
			Graphics2D g2 = (Graphics2D)g;
			
			// Enable antialiasing for text
		 	g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
		}
	}
	
	/**
	 * This class is extension of JEditorPane
	 * with 2 changes
	 * 1) it sets content type to HTML
	 * 2) it has good looking anti-aliased fonts
	 * @author tseytlin
	 */
	public static class HTMLPanel extends JEditorPane {
		private HTMLDocument doc;

		public HTMLPanel() {
			super();
			setContentType("text/html; charset=UTF-8");
			doc = (HTMLDocument) getDocument();
			doc.getStyleSheet().addRule("body { font-family: sans-serif;");
		}

		/**
		 * set font size
		 * @param s
		 */
		public void setFontSize(int s) {
			doc.getStyleSheet().addRule("body { font-family: sans-serif; font-size: " + s + ";");
		}

		// make antialiased text
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			// Enable antialiasing for text
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			super.paintComponent(g);
		}

		/**
		 * Append text to panel
		 * @param text
		 */
		public void append(String text) {
			int offset = getDocument().getEndPosition().getOffset();
			try {
				HTMLDocument doc = (HTMLDocument) getDocument();
				Element elem = doc.getCharacterElement(offset);
				doc.insertAfterEnd(elem, text);
				//doc.insertString(offset-1,text,null);
			} catch (BadLocationException ex) {
			} catch (IOException ex) {
			}

		}

		public void setText(String txt){
			super.setText(txt.replaceAll("\n","<br>"));
		}
		
		public void setReport(String txt){
			super.setText(convertToHTML(txt));
		}
	}
	 /**
	  * return an index of an object in any array -1 if object is not there
	  * @param array
	  * @param obj
	  * @return
	  */
	 public static int indexOf(Object [] array, Object obj){
		 for(int i=0;i<array.length;i++)
			 if(array[i].equals(obj))
				 return i;
		 return -1;
	 }
	 
	 /**
	  * sleep for a time
	  * @param time
	  */
	 public static void sleep(long time){
		 try{
			 Thread.sleep(time);
		 }catch(InterruptedException ex){}
	 }
	 
	 
	 /**
	  * get index for given location, -1 if no cell selected
	  * @param list
	  * @param l
	  * @return
	  */
	 public static int getIndexForLocation(JList list, Point l){
		 Rectangle r = list.getCellBounds(0,list.getModel().getSize()-1);
		 if(r != null && r.contains(l))
			 return list.locationToIndex(l);
		 return -1;
	 }
	 
	 /**
	  * convert regular text report to HTML
	  * @param txt
	  * @return
	  */
	 public static String convertToHTML(String txt){
		 return txt.replaceAll("\n","<br>").replaceAll("(^|<br>)([A-Z ]+:)<br>","$1<b>$2</b><br>");
	 }
	 
	 
	 /**
	  * extendable JTable
	  * @author Eugene Tseytlin
	  */
	 public static class DynamicTable extends JTable implements ActionListener {
		 private JPopupMenu menu;
		 private int selectedRow = -666;
		 private boolean blockExpansion;
		 private final KeyStroke tabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		 private final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		 private DefaultCellEditor editor;
		 
		 public DynamicTable(int rows, int cols){
			 super(new DefaultTableModel(rows,cols));
			 setBackground(Color.white);
			 //setSurrendersFocusOnKeystroke(true);
			 setAutoCreateColumnsFromModel(true);
			 setColumnSelectionAllowed(false);
			 setRowSelectionAllowed(false);
			 setTableHeader(null);
						  
			 setPreferredScrollableViewportSize(new Dimension(200,150));
			 
			 addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e) {
					selectedRow = rowAtPoint(e.getPoint());
					if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
						getPopupMenu().show(DynamicTable.this,e.getX(),e.getY());
					}
				}
			 });
			 
			 // make sure that last edited entry is saved
			 editor = new DefaultCellEditor(new JTextField());
			 // editor.setClickCountToStart(1);
			 setDefaultEditor(Object.class,editor);
			 editor.getComponent().addFocusListener(new FocusListener(){
				public void focusGained(FocusEvent e) {	}
				public void focusLost(FocusEvent e) {
					editor.stopCellEditing();
				}
			 });
			 
			 
		 }
		 
		 private JPopupMenu getPopupMenu(){
			 if(menu == null){
				menu = new JPopupMenu();
				menu.add(createMenuItem("Add Row","Add Row",Icons.PLUS, this));
				menu.add(createMenuItem("Remove Row","Remove Row",Icons.MINUS, this));
				menu.add(createMenuItem("Clear Table Content","Clear Cell Content",Icons.DELETE, this));
			 }
			 return menu;
		 }
		 
		 /**
		  * get values in given column
		  * @param col
		  * @return
		  */
		 public List getValues(int col){
			 List list = new ArrayList();
			 for(int i=0;i<getRowCount();i++)
				 list.add(getModel().getValueAt(i,col));
			return list;
		 }
		 
		 public boolean editCellAt(int x, int y){
			 blockExpansion = true;
			 setRowSelectionInterval(x,x);
			 setColumnSelectionInterval(y,y);
			 return super.editCellAt(x,y);
		 }
		 
		 public void setValueAt(Object val, int row, int col){
			 // add missing rows
			 for(int i=0;i<(row -getRowCount()+1);i++)
				((DefaultTableModel) getModel()).addRow(new Object[getColumnCount()]);
			super.setValueAt(val,row, col); 
		 }
		 /**
		  * detect selection
		  */
		 public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
			AWTEvent currentEvent = EventQueue.getCurrentEvent();
			if (currentEvent instanceof KeyEvent) {
				KeyEvent ke = (KeyEvent) currentEvent;
				if (ke.getSource() != this)
					return;
				// focus change with keyboard 
				if (rowIndex == 0 && columnIndex == 0 && 
					(KeyStroke.getKeyStrokeForEvent(ke).equals(tabKeyStroke) ||
					KeyStroke.getKeyStrokeForEvent(ke).equals(enterKeyStroke))) {
					if(!blockExpansion){
						((DefaultTableModel) getModel()).addRow(new Object[getColumnCount()]);
						rowIndex = getRowCount() - 1;
					}else{
						rowIndex ++;
					}
					revalidate();
				}
				blockExpansion = false;
			}
			super.changeSelection(rowIndex, columnIndex, toggle, extend);
		}

		public void clear(){
			for(Object v: ((DefaultTableModel)getModel()).getDataVector()){
				if(v instanceof List){
					List l = (List)v;
					for(int i=0;i<l.size();i++)
						l.set(i,"");
				}
			}
			revalidate();
			repaint();
		}
		 
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if(cmd.equals("Add Row")){
				((DefaultTableModel) getModel()).addRow(new Object[getColumnCount()]);
			}else if(cmd.equals("Remove Row")){
				if(selectedRow >= 0 && selectedRow < getRowCount()){
					((DefaultTableModel) getModel()).removeRow(selectedRow);
					selectedRow = -666;
				}
			}else if(cmd.equals("Clear Table Content")){
				clear();
			}
		} 
	}

	 
	/**
	 * convert color to string
	 * 
	 * @param c
	 * @return
	 */
	public static String getColor(Color c) {
		try {
			for (int i = 0; i < colors.length; i++) {
				Field field = Class.forName("java.awt.Color").getField(colors[i]);
				if (c.equals(field.get(null)))
					return colors[i];
			}
		} catch (Exception ex) {
		}
		return c.getRed() + " " + c.getGreen() + " " + c.getBlue();
	}
	
	/**
	 * get color from string
	 * @param colorName
	 * @return
	 */

	public static Color getColor(String colorName) {
		if (colorName == null)
			return null;
		try {
			// Find the field and value of colorName
			Field field = Class.forName("java.awt.Color").getField(colorName);
			return (Color) field.get(null);
		} catch (Exception e) {
			// attempt to parse RGB if color name not found
			String[] s = colorName.split(" ");
			if (s.length == 3) {
				try{
					return new Color(Integer.parseInt(s[0]),
									 Integer.parseInt(s[1]), 
									 Integer.parseInt(s[2]));
				}catch(NumberFormatException ex){}
			}
			return null;
		}
	}
	
	/**
	 * only allow integer input
	 * @author Eugene Tseytlin
	 */
	public static class IntegerDocument extends DefaultStyledDocument{
		public void insertString(int i, String s, AttributeSet a) throws BadLocationException {
			if(s.matches("[0-9]+"))
				super.insertString(i, s, a);
		}
	}
	
	/**
	 * only allow integer input
	 * @author Eugene Tseytlin
	 */
	public static class DecimalDocument extends DefaultStyledDocument{
		public void insertString(int i, String s, AttributeSet a) throws BadLocationException {
			if(s.matches("[0-9\\.]+"))
				super.insertString(i, s, a);
		}
	}
	
	/**
	 * this class helps in creating a dynamic list based on search
	 * @author tseytlin
	 */
	public static class FilterDocument extends PlainDocument {
		private JTextField textField;
		private JList list;
		private JComboBox box;
		private Collection originalContent;
		private boolean changeColorOnEmpty;
		
	

		/**
		 * create filter document for JList
		 * @param text
		 * @param list
		 * @param vec
		 */
		public FilterDocument(JTextField text, JList list,Collection content){
			this.textField = text;
			this.list = list;
			this.originalContent = content;
		}
		
		/**
		 * create filter document for JComboBox
		 * @param text
		 * @param list
		 * @param vec
		 */
		public FilterDocument(JTextField text, JComboBox box,Collection content){
			this.textField = text;
			this.box = box;
			this.originalContent = content;
		}
		
		public boolean isChangeColorOnEmpty() {
			return changeColorOnEmpty;
		}

		public void setChangeColorOnEmpty(boolean changeColorOnEmpty) {
			this.changeColorOnEmpty = changeColorOnEmpty;
		}
		
		public void setSelectableObjects(Collection obj){
			originalContent = obj;
		}
		
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offs, str, a);
			if(str.length() == 1)
				filter();
 	    }
		public void remove(int offs,int len) throws BadLocationException {
			super.remove(offs,len);
			filter();
		}
		public void replace(int offs,int len,String txt,AttributeSet a) throws BadLocationException{
			super.remove(offs,len);
			insertString(offs,txt,a);
		}
		
		public void filter(){
			textField.setForeground(Color.black);
			String text = textField.getText().toLowerCase();	
			
			// filter content
			Vector v = new Vector();
			for(Object item :originalContent){
				if(item.toString().toLowerCase().contains(text))
					v.add(item);
			}
			
			// change color if nothing is in list
			if(changeColorOnEmpty && v.isEmpty())
				textField.setForeground(Color.red);
			
			// set list data
			if(list != null){
				list.setListData(v);
				if(v.size() == 1)
					list.setSelectedIndex(0);
			}else if(box != null){
				box.removeAllItems();
				for(Object o: v)
					box.addItem(o);
				box.revalidate();
			}
			
		}
	}
	
	
	/**
	 * backup file into given directory
	 * @param file
	 */
	
	public static void backup(File input, File output) throws IOException {
		// save a local copy at location
		copy(input, output);
		
		// do file upload 
		upload(output);
		
		// remove local copy if applicable
		if(DomainBuilder.getRepository() instanceof FileRepository){
			if(((FileRepository)DomainBuilder.getRepository()).isServerMode()){
				delete(output);
			}
		}
	}
	
	/**
	 * upload file
	 * @param output
	 * @throws IOException
	 */
	public static void delete(File file) throws IOException {
		if(Communicator.isConnected())
			file.renameTo(new File(file.getParentFile(),file.getName()+".backup"));
	}
	
	/**
	 * upload file
	 * @param output
	 * @throws IOException
	 */
	public static void upload(File output) throws IOException {
		// do file upload 
		File dir = output.getParentFile();
		String path = dir.getAbsolutePath();
		
		// strip home folder part
		String home = OntologyHelper.getLocalRepositoryFolder();
		if(path.startsWith(home))
			path = path.substring(home.length());
		
		// convert to path
		if(path.contains(File.separator))
			path = path.replace(File.separatorChar,'/');
		
		
		// figure out the root
		if(path.startsWith("/"))
			path = path.substring(1);
		int i = path.indexOf("/");
		
		String root = "";
		
		// this should always be the case
		if(i > -1){
			root = path.substring(0,i);
			path = path.substring(i+1);
		}
		Communicator.upload(output,root,path);
	}
	
	
	
	/**
	 * copy file from one location to another
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 */
	public static void copy(File inputFile,File outputFile) throws IOException {
		Communicator.copy(new FileInputStream(inputFile), new FileOutputStream(outputFile));
	}
	
	 /**
     * create mirrored image
     * @param tc
     * @return
     */
    public static Image getMirrorImage(Image tc ){
    	 BufferedImage bf = new BufferedImage(tc.getWidth(null),tc.getHeight(null),BufferedImage.TYPE_INT_ARGB);
         bf.createGraphics().drawImage(tc,0,0,null);
         // Flip the image horizontally
         AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
         tx.translate(-tc.getWidth(null), 0);
         AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
         bf = op.filter(bf, null);
         return bf;
    }
	
    public static void flashImage(Component c, Image img){
    	 MediaTracker tracker = new MediaTracker(c);
         tracker.addImage(img,0);
         try{
        	 tracker.waitForAll();
         }catch(Exception ex){}
    }
    
    
    /**
     * prompt for login informat
     * @param place - prompt for institution
     * @return
     */
    public static String [] promptLogin(boolean place){
    	JLabel errorLabel = new JLabel("Enter name and password");
    	
    	// fetch institutions
    	Vector<String> places = new Vector<String>();
    	if(place){
    		Map map = new HashMap();
    		map.put("action","list");
    		map.put("root","config");
    		try{
	    		String str = Communicator.doGet(Communicator.getServletURL(),map);
	    		for(String p: str.split("\n")){
	    			if(p.endsWith("/"))
	    				p = p.substring(0,p.length()-1);
	    			places.add(p);
	    		}
	    		OntologyHelper.setInstitutions(places);
    		}catch(Exception ex){
    			errorLabel.setText("<html><font color=red>"+ex.getMessage()+"</font>");
    		}
    	}
    	// create fields
		JTextField usernameField = new JTextField("");
		JTextField passwordField = new JPasswordField("",12);
		JComboBox institutionField = new JComboBox(places);
		institutionField.setPreferredSize(passwordField.getPreferredSize());
		
		// set default
		if(places.contains(OntologyHelper.DEFAULT_INSTITUTION))
			institutionField.setSelectedItem(OntologyHelper.DEFAULT_INSTITUTION);
		
		// create labels
		JLabel  userNameLabel = new JLabel("Name:   ", JLabel.RIGHT);
		JLabel  passwordLabel = new JLabel("Password:   ", JLabel.RIGHT);
		JLabel  institutiondLabel = new JLabel("Institution:   ", JLabel.RIGHT);
		errorLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
		JLabel 	statusLabel = new JLabel("Status:    ", JLabel.RIGHT);
				
		JPanel connectionPanel = new JPanel(false);
		connectionPanel.setLayout(new BoxLayout(connectionPanel,BoxLayout.X_AXIS));
							
		JPanel namePanel = new JPanel(false);
		namePanel.setLayout(new GridLayout(0,1));
		namePanel.add(userNameLabel);
		namePanel.add(passwordLabel);
		if(place)
			namePanel.add(institutiondLabel);
		namePanel.add(statusLabel);
		JPanel fieldPanel = new JPanel(false);
		fieldPanel.setLayout(new GridLayout(0,1));
		fieldPanel.add(usernameField);
		fieldPanel.add(passwordField);
		if(place)
			fieldPanel.add(institutionField);
		fieldPanel.add(errorLabel);
		
		connectionPanel.add(namePanel);
		connectionPanel.add(fieldPanel);
    		
		// return 
    	String [] login = new String [3];
		
    	(new FocusTimer(usernameField)).start();
    	
		// prompt for password
		if(JOptionPane.OK_OPTION == 
		   JOptionPane.showConfirmDialog(null,connectionPanel,"DomainBuilder",
		   JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)){
			login[0] = usernameField.getText();
			login[1] = passwordField.getText();
			login[2] = ""+institutionField.getSelectedItem();
		}else{
			return null;
		}
		return login;
    }
    
    
    /**
     * prompt user to use local copy
     * @return
     */
    public static boolean promptUseLocalCopy(String url){
    	if(Communicator.isConnected()){
    		//TODO: in connected mode, always use server copy
    		/*
    		String msg = "<html>A copy of <a href=\"\">"+url+"</a> has been detected on your local computer.<br>This normally happens " +
    					 "when you didn't export your work after saving it. Would you like to use that local copy?<br><br>" +
    					 "If you choose <font color=green>Yes</font>, you will load a local copy and <font color=red>override</font>" +
    					 " all of the changes on the server once you export your work.<br>" +
    					 "If you choose <font color=green>No</font>, you will load a copy from the server" +
    					 " and <font color=red>overwride</font> any local changes that you've  made.";
    		int r = JOptionPane.showConfirmDialog(DomainBuilder.getInstance().getFrame(),msg,"Use Local?",JOptionPane.YES_NO_OPTION);
    		return r == JOptionPane.YES_OPTION;
    		*/
    		return false;
    	}
    	return true;
    }
    
    
	/**
	 * get list of images on server
	 * @param servlet
	 * @return
	 *
	public static Collection<String> listImages(URL servlet){
		//try{
			String response = Communicator.doGet(new URL(servlet+"?action=list&root=image"));
		
		//}
	}*/
    

    /*
	private static Timer timer;
	public static void main(String [] a){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		final DynamicTable table = new UIHelper.DynamicTable(5,1);
		panel.add(new JLabel("Type one or more concepts below"),BorderLayout.NORTH);
		panel.add(new JScrollPane(table),BorderLayout.CENTER);
		
		timer = new UIHelper.FocusTimer(new ActionListener() {
			public void actionPerformed(ActionEvent e){
            	if(table.hasFocus()) {
					timer.setRepeats(false);
	                return;
	            }
				table.requestFocusInWindow();
				table.editCellAt(0,0);
			}
		});
		timer.start();
		int r = JOptionPane.showConfirmDialog(null,panel,"Create New Concepts ...",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
				Icons.getIcon(Icons.EDIT,24));
		if(r == JOptionPane.OK_OPTION){
			Set<String> values = new LinkedHashSet<String>();
			for(Object obj: table.getValues(0)){
				String s = (String) obj;
				if(s != null && s.length() > 0)
					values.add(s);
			}
			System.out.println(values);
		}
		
	}
	*/
	
	/**
	 * enable/disable abstract buttons inside container unless
	 * they are part of exceptions
	 * @param cont
	 * @param exceptions
	 */
	public static void setEnabled(Container cont, String [] exceptions, boolean flag){
		for(Component c: (cont instanceof JMenu)?((JMenu)cont).getMenuComponents():cont.getComponents()){
			if(flag || !isException(c, exceptions)){
				if(c instanceof JMenu){
					setEnabled((Container)c, exceptions,flag);
				}else{
					c.setEnabled(flag);
				}
			}
		}
	}
	
	/**
	 * is this component an exception
	 * @param c
	 * @param exceptions
	 * @return
	 */
	private static boolean isException(Component c, String [] exceptions){
		String name = null;
		if(c instanceof AbstractButton){
			name = ((AbstractButton)c).getActionCommand();
		}else if(c instanceof JMenu){
			name = ((JMenu)c).getText();
		}
		// if name is null then don't touch this component
		if(name == null)
			return true;
		
		// check all exceptions
		for(String ex: exceptions)
			if(ex.equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	/**
	 * debug messsages
	 * @param obj
	 */
	public static void debug(Object obj){
		System.out.println(obj);
	}
	
	/**
	 * create an icon on the fly that has some color
	 * @param c
	 * @return
	 */
	
	public static Icon createSquareIcon(Color c, int s){
		final Color color = c;
		final int size = s;
		return new Icon(){
			public int getIconHeight() {
				return size;
			}
			public int getIconWidth() {
				return size;
			}
			public void paintIcon(Component component, Graphics g, int i, int j) {
				g.setColor(color);
				g.fillRoundRect(i, j,size,size,3,3);				
			}
		};
	}
	
	/**
	 * does parent contain child
	 * @param parent
	 * @param child
	 * @return
	 */
	public static boolean contains(Container parent, Component child){
		if(child == null || parent == null)
			return false;
		
		if(child.getParent() == parent)
			return true;
		
		return contains(parent,child.getParent());
	}
	
	
	/**
	 * pretty print case status to the buffer
	 * @param output
	 * @param caseEntry
	 */
	public static void printCaseStatus(StringBuffer text, CaseEntry caseEntry){
		// set case information
		if(caseEntry != null){
			text.append("&nbsp;&nbsp;&nbsp;&nbsp;");
			
			// set case color
			String color = "gray";
			if(STATUS_INCOMPLETE.equals(caseEntry.getStatus()))
				color = "red";
			else if(STATUS_COMPLETE.equals(caseEntry.getStatus()))
				color = "blue";
			else if(STATUS_TESTED.equals(caseEntry.getStatus()))
				color = "green";
			
			text.append("case: <font color=\""+color+"\">" + caseEntry.getName()+"</font>");
			if(caseEntry.isModified()){
				text.append(" (modified)");
			}
		}
	}
	
	/**
	 * pretty print case status to the buffer
	 * @param output
	 * @param caseEntry
	 */
	public static void printUserStatus(StringBuffer text){
		// set authentication
		text.append("&nbsp;&nbsp;&nbsp;&nbsp;user: ");
		if(!Communicator.isConnected())
			text.append(" <font color=red>not authenticated </font> " +
						" from <b>"+DomainBuilder.getParameter("repository.institution")+"</b>");		
		else
			text.append(" <font color=green>authenticated</font> as <b>"+DomainBuilder.getParameter("repository.username")+
					    "</b> from <b>"+DomainBuilder.getParameter("repository.institution")+"</b>");
	}	
	
	/**
	 * print pretty printed knowledge status
	 * @param text
	 * @param uri
	 */
	public static void printKnowledgeStatus(StringBuffer text,String uri){
		text.append("domain: <font color=blue><u>" +uri+"</u></font>");
	}
}
