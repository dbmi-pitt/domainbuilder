package edu.pitt.dbmi.domainbuilder.widgets;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.POntology;


public class ResourcePropertiesPanel extends JPanel {
	private Map<String,ValueContainer> fields;
	
	/**
	 * ontology properties
	 */
	public ResourcePropertiesPanel(){
		this(null);
	}
	
	/**
	 * ontology properties
	 */
	public ResourcePropertiesPanel(IResource r){
		super();
		fields = new LinkedHashMap<String,ValueContainer>();
		
		// create a properies panel
		setLayout(new GridBagLayout());
		setBorder(new TitledBorder("Ontology Properties"));
		// init constraints
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,5,5,5);
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTH;
		
		// fill in the list with names
		addSingleValuePanel(OntologyHelper.TITLE,c);
		addSingleValuePanel("URI",c);
		addSingleValuePanel(OntologyHelper.VERSION,c);
		addSingleValuePanel(OntologyHelper.DESCRIPTION,true,c);
		add(new JLabel(" "),c);
		c.gridy++;
		addSingleValuePanel(OntologyHelper.CREATOR,c);
		addMultipleValuePanel(OntologyHelper.CONTRIBUTOR,c);
		addMultipleValuePanel(OntologyHelper.SOURCE,c);
		add(new JLabel(" "),c);
		c.gridy++;
		addMultipleValuePanel(OntologyHelper.HAS_TAG,c);
		
		
		JTextField uri = (JTextField) fields.get("URI");
		uri.setEditable(false);
		uri.setForeground(Color.blue);
		
		// fill in values
		if(r != null)
			loadProperties(r);
	}
	
	/**
	 * load properties into the the panel
	 * @param r
	 */
	public void loadProperties(IResource r){
		IOntology ont = r.getOntology();
		for(String name: fields.keySet()){
			if(name.equals(OntologyHelper.TITLE)){
				String [] l = r.getLabels();
				if(l.length == 0)
					l = new String [] {r.getName()};
				fields.get(name).setValues(l);
			}else if(name.equals(OntologyHelper.DESCRIPTION)){
				String [] s = r.getComments();
				if(s.length > 0)
					fields.get(name).setValues(new String [] {s[0]});
			}else if(name.equals(OntologyHelper.VERSION)){
				fields.get(name).setValues(new Object [] {r.getVersion()});
			}else if(name.equals("URI")){
				fields.get(name).setValues(new Object [] {r.getURI()});
			}else{
				/*
				List<String> vals = new ArrayList<String>();
				for(String s: r.getComments()){
					if(s.startsWith(name+": ")){
						vals.add(s.substring(name.length()+2));
					}
				}
				fields.get(name).setValues(vals.toArray());
				*/
				IProperty p = ont.getProperty(name);
				if(p != null){
					fields.get(name).setValues(r.getPropertyValues(p));
				}
			}
			
		}
	}
	
	/**
	 * load properties into the the panel
	 * @param r
	 */
	public void saveProperties(IResource r){
		IOntology ont = r.getOntology();
		
		// go over all fields
		for(String name: fields.keySet()){
			if(name.equals(OntologyHelper.TITLE)){
				for(String s: r.getLabels())
					r.removeLabel(s);
				for(String s: fields.get(name).getValues())
					r.addLabel(s);
			}else if(name.equals(OntologyHelper.DESCRIPTION)){
				// remove all comments
				for(String s: r.getComments())
					r.removeComment(s);
				for(String s: fields.get(name).getValues())
					r.addComment(s);
			}else if(name.equals(OntologyHelper.VERSION)){
				r.removeVersion(r.getVersion());
				for(String s: fields.get(name).getValues())
					r.addVersion(s);
			}else if(name.equals("URI")){
				// URI is immutable
			}
			// try to find dublin core properties
			IProperty p = ont.getProperty(name);
			if(p != null){
				//r.removePropertyValues(p);
				r.setPropertyValues(p,fields.get(name).getValues());
				//for(String s: fields.get(name).getValues()){
				//	//r.addComment(name+": "+s);
				//}
			}
		}
		
	}
	
	
	/**
	 * derive pretty name from method name
	 * @param method
	 * @return
	 */
	private String deriveName(String method){
		if(method.length() < 1)
			return method;
		//return method.replaceAll("([a-z])([A-Z])","\\1 \\2").replaceAll("_"," ");
		String title = new String(method);
		title = UIHelper.getPrettyClassName(title);
		
		//if(title.startsWith(prefix))
		//	title = title.substring(prefix.length());
		if(title.matches("[A-Z]+"))
			return title;
		return (""+title.charAt(0)).toUpperCase()+title.substring(1).toLowerCase();
	}
	/**
	 * create value panel
	 * @param str
	 * @return
	 */
	private void addSingleValuePanel(String name, GridBagConstraints c){
		addSingleValuePanel(name,false, c);
	}
	
	/**
	 * create value panel
	 * @param str
	 * @return
	 */
	private void addSingleValuePanel(String name, boolean large, GridBagConstraints c){
		c.gridx = 0;
		c.gridwidth = 1;
		// strip get/set prefix
	
		// get getter and setter methods
		// for everything else label and text box
		JLabel lbl = new JLabel(deriveName(name));
		add(lbl,c);
		c.gridx++;
		ValueContainer txt;
		if(large){
			txt = new ValueTextArea(false);
			add(new JScrollPane((JComponent)txt),c);
		}else{
			txt = new ValueTextField();
			add((JComponent)txt,c);
		}
		// next row please
		c.gridy++;
	
		// save in map
		fields.put(name,txt);
	}
	
	/**
	 * create multiple value panel
	 * @param name
	 * @param c
	 */
	private void addMultipleValuePanel(String name, GridBagConstraints c){
		c.gridx = 0;
		c.gridwidth = 1;
		// strip get/set prefix
	
		// get getter and setter methods
		// for everything else label and text box
		JLabel lbl = new JLabel(deriveName(name));
		add(lbl,c);
		c.gridx++;
		
		
		ValueContainer txt = new ValueTextArea(true);
		JScrollPane scroll = new JScrollPane((JComponent)txt);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll,c);
			
		/*
		// init list
		final ValueList entry = new ValueList();
		entry.setFont(entry.getFont().deriveFont(Font.PLAIN));	
		JScrollPane scroll = new JScrollPane(entry);
		scroll.setPreferredSize(new Dimension(200,75));
		
		// create button panel
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		final JTextField text = new JTextField(25);
		buttons.add(text);
		JButton b = UIHelper.createButton("add","Add Entry",Icons.PLUS,16,new ActionListener(){
			public void actionPerformed(ActionEvent e){
				((DefaultListModel)entry.getModel()).addElement(text.getText());
			}
		});
		b.setPreferredSize(new Dimension(24,24));
		buttons.add(b);
		b = UIHelper.createButton("rem","Remove Entry",Icons.MINUS,16,new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int [] sel = entry.getSelectedIndices();
				for(int i=0;i<sel.length;i++)
					((DefaultListModel)entry.getModel()).removeElementAt(sel[i]);
			}
		});
		b.setPreferredSize(new Dimension(24,24));
		buttons.add(b);
		
		// add to panels
		add(buttons,c);
		c.gridy++;
		add(scroll,c);
		c.gridy++;
		*/
		
		// next row please
		c.gridy++;
	
		fields.put(name,txt);
	}
	
	/**
	 * small interface that contains values
	 * @author tseytlin
	 *
	 */
	private interface ValueContainer {
		public String [] getValues();
		public void setValues(Object [] v);
	}
	
	
	/**
	 * @author tseytlin
	 */
	private class ValueTextField extends JTextField implements ValueContainer {
		public ValueTextField(){
			super(25);
		}
		public String [] getValues(){
			String str = getText().trim();
			return (str.length()> 0)?new String [] {str}:new String [0];
		}
		public void setValues(Object [] obj){
			setText((obj != null && obj.length > 0)?""+obj[0]:"");
		}
	}
	/**
	 * @author tseytlin
	 */
	private class ValueTextArea extends JTextArea implements ValueContainer {
		private boolean multiple;
		public ValueTextArea(boolean m){
			super(5,25);
			multiple = m;
		}
		public String [] getValues(){
			String s = getText().trim();
			if(s.length() == 0)
				return new String [0];
			return (multiple)?getText().split("\n"):new String [] {s};
		}
		public void setValues(Object [] obj){
			if(multiple){
				StringBuffer bf = new StringBuffer();
				for(Object o: obj)
					bf.append(o+"\n");
				setText(bf.toString().trim());
			}else{
				setText((obj != null && obj.length > 0)?""+obj[0]:"");
			}
		}
	}
	
	/**
	 * @author tseytlin
	 *
	private class ValueList extends JList implements ValueContainer {
		public ValueList(){
			super(new DefaultListModel());
		}
		public String [] getValues(){
			ListModel model = getModel();
			String [] vals = new String [model.getSize()];
			for(int i=0;i<vals.length;i++)
				vals[i] = ""+model.getElementAt(i);
			return vals;
		}
		public void setValues(Object [] obj){
			if(obj != null)
				for(Object o: obj)
					((DefaultListModel)getModel()).addElement(o);
		}
	}
	*/
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		/*
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost/repository";
		String user = "user";
		String pass = "resu";
		String table = "repository";
		String dir   = System.getProperty("user.home")+File.separator+".protegeRepository";
		IRepository r = new ProtegeRepository(driver,url,user,pass,table,dir);
		long time = System.currentTimeMillis();
		IOntology kb = r.getOntology("Test.owl");
		kb.load();
		System.out.println("load time "+(System.currentTimeMillis()-time));
		time = System.currentTimeMillis();
		IProperty prop = kb.getProperty("http://dublincore.org/2008/01/14/dcelements.rdf#creator");
		kb.addPropertyValue(prop,"me");
		System.out.println(kb.getPropertyValue(prop)+" "+(System.currentTimeMillis()-time));
		*/
		
		IOntology ont = POntology.loadOntology("/home/tseytlin/Work/curriculum/owl/skin/PITT/VesicularDermatitis.owl");
		IProperty pr = ont.getProperty("dc:creator");
		System.out.println(pr);
		
		ResourcePropertiesPanel p = new ResourcePropertiesPanel(ont);
		JOptionPane.showMessageDialog(null,p,"",JOptionPane.PLAIN_MESSAGE);
	}

}
