package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.util.DomainTerminology;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.ProtegeRepository;


public class ConceptComboBox extends JComboBox implements DocumentListener {
	private DomainTerminology terminology;
	private IOntology ontology;
	private List<IClass> defaultParents;
	private JTextField text;
	//private StringBuffer s;
	private List<String> words;
	private boolean block,selected;
	
	public ConceptComboBox(){
		this(null);
	}
	
	
	public ConceptComboBox(DomainTerminology term){
		super();
		setEditable(true);
		text = (JTextField) getEditor().getEditorComponent();
		text.getDocument().addDocumentListener(this);
		putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		setPreferredSize(new Dimension(160,25));
		setDomainTerminolgy(term);
		defaultParents = new ArrayList<IClass>();
		
		// "disable" word items
		setRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus){
				String v = ""+value;
				if(v.startsWith(" ")){
					Component c = super.getListCellRendererComponent(list,
							value, index, isSelected, cellHasFocus);
					String t = getTab(v);
					setText(t+UIHelper.getPrettyClassName(v));
					return c;
				}else{
					setText(UIHelper.getPrettyClassName(v));
					setBackground(list.getBackground());
			        setForeground(UIManager.getColor("Label.disabledForeground"));
				}
				return this;
		    }
		});
		
		
		
		// make popup bigger then combobox size
		//http://forums.java.net/jive/message.jspa?messageID=61267
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
		//s = new StringBuffer();
		text.setText("");
	}
	
	/**
	 * get tab
	 * @param s
	 * @return
	 */
	private String getTab(String s){
		StringBuffer b = new StringBuffer("");
		char [] c = s.toCharArray();
		for(int i=0;i<c.length;i++){
			if(c[i] == ' '){
				b.append(c[i]);
			}else{
				return b.toString();
			}
		}
		return "";
	}

	/**
	 * get selected item
	 */
	public Object getSelectedItem() {
		selected = false;
		Object obj = super.getSelectedItem();
		//System.out.println(obj+" "+getSelectedIndex());
		if(obj != null){ 
			// if right kind of object already selected, just return it
			if(obj instanceof ConceptEntry)
				return obj;
			// else create new "right kind of object
			String name = obj.toString();
			// skip empty lines and words
			if(name.length() == 0 || !name.startsWith(" ")){
				return null;
			}
			// create right kind of object ;)
			IClass cls = ontology.getClass(name.trim());
			if(cls != null){
				selected = true;
				return new ConceptEntry(cls);
			}
			return null;
		}
		return obj;
	}
	
	public void updateWords(){
		words = new ArrayList<String>();
		if(terminology != null){
			words.addAll(terminology.getWords());
			/*
			for(IClass c: ontology.getClass(OntologyHelper.LEXICON).
				getDirectSubClasses()){
				words.add(c);
			}
			*/
		}
	}
	
	/**
	 * set ontology
	 * @param ont
	 */
	public void setDomainTerminolgy(DomainTerminology term){
		this.ontology = term.getOntology();
		this.terminology = term;
		setDefaultParent(ontology.getClass(OntologyHelper.CONCEPTS));
		updateWords();
	}
	
	/**
	 * default parent
	 * @param defaultParent
	 */
	public void setDefaultParent(IClass defaultParent) {
		this.defaultParents = Collections.singletonList(defaultParent);
	}
	
	/**
	 * default parent
	 * @param defaultParent
	 */
	public void addDefaultParent(IClass defaultParent) {
		if(defaultParents == null)
			defaultParents = new ArrayList<IClass>();
		defaultParents.add(defaultParent);
	}
	
	/*
	public void keyPressed(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyReleased(KeyEvent evt) {
		// clear buffer if nothing is in
		if(s == null || text.getText().length() == 0)
			s = new StringBuffer();
		// get keycode and append to buffer
		int  c = evt.getKeyCode();
		char h = evt.getKeyChar();
		
		// if special char or word char
		if (c == KeyEvent.VK_BACK_SPACE && s.length() > 0){
			s.replace(s.length()-1,s.length(),"");
			sync(""+s);
		}else if((""+h).matches("[\\w ]")){
			s.append(""+h);
			sync(""+s);
		}
	}*/
	

	//sync combobox w/ what is typed in
	private void sync(String str) {
		//System.out.println(str);
		removeAllItems();
		hidePopup();
		boolean show = false;
		if(str != null && str.length() > 0){
			for(String word: getMatchingWords(str)){
				addWord(word);
				show = true;
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
	private List<String> getMatchingWords(String str){
		str = str.toLowerCase();
		List<String> list = new ArrayList<String>();
		String [] pstr = str.split("\\s+");
		for(String w: words){
			for(String ps: pstr){
				if(w.toLowerCase().startsWith(ps)){
					list.add(w);
				}
			}
		}
		return list;
	}
	
	/**
	 * add individual word to a list
	 * @param cls
	 * @param tab
	 */
	private void addWord(IClass cls, Set<IClass> clist, String tab){
		//filter
		addItem(tab+cls.getName());
		
		// get a sorted set of children
		Set<IClass> children = new TreeSet<IClass>();
		Collections.addAll(children,cls.getDirectSubClasses());
		
		for(IClass c: children){
			if(isLegitWord(c) && !clist.contains(c)){
				clist.add(c);
				addWord(c,clist,tab+"   ");
			}
		}
	}
	
	/**
	 * is legit word
	 * @param c
	 * @return
	 */
	private boolean isLegitWord(IClass c){
		for(IClass parent : defaultParents){
			if(c.hasSuperClass(parent)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * add individual word to a list
	 * @param cls
	 * @param tab
	 */
	private void addWord(String word){
		//filter
		addItem(word);
		if(terminology != null){
			Set<IClass> clist = new HashSet<IClass>();
			for(IClass c: terminology.getWordClasses(word)){
				if(c != null && isLegitWord(c) && !clist.contains(c)){	
					clist.add(c);
					addWord(c,clist,"   ");
				}
			}
		}
	}
	

	
	
	public static void main(String [] args) throws Exception{
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost/repository";
		String user = "user";
		String pass = "resu";
		String table = "repository";
		String dir   = System.getProperty("user.home")+File.separator+".protegeRepository";
		IRepository r = new ProtegeRepository(driver,url,user,pass,table,dir);
		IOntology [] kbs = r.getOntologies("Melanocytic.owl");
		IOntology kb = kbs[0];
		DomainTerminology term = new DomainTerminology(kb);
		ConceptComboBox combo = new ConceptComboBox(term);
		combo.setDefaultParent(kb.getClass(OntologyHelper.DIAGNOSTIC_FEATURES));
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(combo,BorderLayout.CENTER);
		f.pack();
		f.setVisible(true);
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
