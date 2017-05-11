package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import edu.pitt.dbmi.domainbuilder.beans.*;
import edu.pitt.dbmi.domainbuilder.caseauthor.CaseAuthor;
import edu.pitt.dbmi.domainbuilder.caseauthor.ConceptSelector;
import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;


/**
 * creates a panel that displays attributes of a given ConceptEntry
 * @author tseytlin
 *
 */
public class ConceptEntryPanel extends JPanel implements ActionListener {
	private ConceptEntry concept;
	private final Color BORDER_COLOR = new Color(184,207,229);
	private JPanel attributePanel;
	private JToggleButton assertConceptButton;
	private EntryChooser attributeChooser, shapeChooser,recommendationsChooser;
	private static MutableTreeNode shapeRoot;
	private int attributeCount;
	private boolean negated;
	private static Frame frame;
	private JButton rem;
	private JCheckBox assertConcept,absentConcept;
	private JTextField floatField, resourceLink;
	private GridLayout panelLayout;
	private ArrayList<Component> componentList = new ArrayList<Component>();
	private MultipleEntryWidget locations,recommendations;
	private Map<ConceptEntry,SingleEntryWidget> examples;
	
	/**
	 * create conept entry panel 
	 * @param entry
	 */
	public ConceptEntryPanel(ConceptEntry entry){
		super();
		this.concept = entry;
		examples = new HashMap<ConceptEntry,SingleEntryWidget>();
		createGUI();
	}
	
	
	public ConceptEntry getConceptEntry(){
		return concept;
	}
	
	/**
	 * create GUI
	 */
	private void createGUI(){
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		// name of FAV
		JPanel bp = new JPanel();
		bp.setLayout(new BorderLayout());
		bp.setMaximumSize(new Dimension(500,36));
		
		// asserted
		assertConceptButton = new JToggleButton(UIHelper.getPrettyClassName(""+concept));
		assertConceptButton.setFont(assertConceptButton.getFont().deriveFont(Font.PLAIN));
		assertConceptButton.setToolTipText("Assert "+UIHelper.getPrettyClassName(""+concept));
		assertConceptButton.setSelected(concept.isAsserted());
		assertConceptButton.addActionListener(this);
		assertConceptButton.setActionCommand("assert");
		bp.add(assertConceptButton,BorderLayout.CENTER);
		
		
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.CENTER));
		assertConcept = new JCheckBox("Assert Concept in Case",concept.isAsserted());
		assertConcept.setActionCommand("assert");
		assertConcept.addActionListener(this);
		p.add(assertConcept);
		p.add(new JLabel("    "));
		absentConcept = new JCheckBox("Mark as Explicitly Absent Concept in Case",concept.isAbsent());
		absentConcept.setActionCommand("absent");
		absentConcept.addActionListener(this);
		p.add(absentConcept);
		add(bp);
		add(p);
		
		// get feature
		/*
		JPanel fp = new JPanel();
		fp.setLayout(new BorderLayout());
		fp.setBorder(new TitledBorder(new LineBorder(BORDER_COLOR),"Feature"));
		SingleEntryWidget feature = new SingleEntryWidget(null);
		feature.setEntry(concept.getFeature());
		feature.setEditable(false);
		fp.add(feature,BorderLayout.CENTER);
		add(fp);
		*/
		
		//  attributes panel 
		attributePanel = new JPanel();
		attributePanel.setLayout(new BoxLayout(attributePanel,BoxLayout.Y_AXIS));
		// add feature panel
		attributePanel.add(createConceptPanel(concept.getFeature()));
		
		
		// add attributes
		ConceptEntry [] attr = concept.getAttributes();
		for(int i=0;i<attr.length;i++){
			attributePanel.add(createConceptPanel(attr[i]));
		}
		
		// add attribute panel
		add(attributePanel);
		

		// add buttons
		//add(createButtonPanel());
		add(new JLabel(" "));
		
		// add location panel
		add(createLocationPanel(concept));
		
		// add recomendation panel
		//add(createRecommendationPanel(concept));
		
		// add glue
		add(Box.createVerticalGlue());
		
		setLocationsEnabled(!concept.isAbsent());
		
	}
	
	/**
	 * create add/remove buttons
	 * @return
	 */
	private JPanel createButtonPanel(){
		JPanel arp = new JPanel();
		arp.setBorder(new LineBorder(BORDER_COLOR));
		arp.setLayout(new FlowLayout());
		JButton add = new JButton("add",Icons.getIcon(Icons.PLUS,16));
		rem = new JButton("remove",Icons.getIcon(Icons.MINUS,16));
		add.setFont(add.getFont().deriveFont(Font.PLAIN));
		rem.setFont(add.getFont());
		add.setPreferredSize((rem.getPreferredSize()));
		add.setToolTipText("Add New Attribute");
		rem.setToolTipText("Remove Last Attribute");
		rem.setEnabled(false);
		arp.add(add);
		arp.add(rem);
		add.addActionListener(this);
		add.setActionCommand("add");
		rem.addActionListener(this);
		rem.setActionCommand("rem");
		return arp;
	}
	
	
	/**
	 * was concept negated
	 * @return
	 */
	public boolean isNegated(){
		return negated;
	}
	
	/**
	 * get title
	 * @param attr
	 * @return
	 */
	private String getTitle(ConceptEntry attr){
		String title = "Concept";
		if(attr == null)
			return title;
		
		if(attr.isFeature())
			title = "Feature";
		else if(attr.isAttribute())
			title = "Attribute";
		else if(attr.isDisease())
			title = "Disease";
		return title;
	}
	
	
	/**
	 * create attribute panel
	 *
	 */
	private JPanel createConceptPanel(ConceptEntry attr){
		// create panel
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
		//p.setLayout(new FlowLayout(FlowLayout.CENTER));
		p.setBorder(new TitledBorder(new LineBorder(BORDER_COLOR),getTitle(attr)));
		
		// add attribute field
		String parent = "                 ";
		/*
		if(attr != null){
			IClass [] par = attr.getConceptClass().getDirectSuperClasses();
			parent = (par.length > 0)?UIHelper.getPrettyClassName(""+par[0]):"            ";
		}*/
		SingleEntryWidget attribute = new SingleEntryWidget(parent);
		if(attr != null)
			attribute.setEntry(attr);
		attribute.setEditable(attr == null);
		attribute.setEntryChooser(getAttributeChooser());
		attribute.setBorder(new EmptyBorder(0,0,0,20));
		p.add(attribute);
		
		// add example field
		if(OntologyHelper.NUMERIC.equals(attr.getName())){
			floatField = new JTextField();
			floatField.setDocument(new UIHelper.DecimalDocument());
			floatField.setText(concept.getNumericValueString());
			p.add(createNumberPanel(floatField,"Numeric Value in Case"));
		}else if(OntologyHelper.isAnatomicLocation(attr.getConceptClass())){
			resourceLink = new JTextField();
			resourceLink.setText(concept.getResourceLink());
			resourceLink.setForeground(Color.blue);
			resourceLink.setEditable(false);
			p.add(createNumberPanel(resourceLink,"Anatomic Location in Case"));
		}else{ 
			// example widget
			SingleEntryWidget example = new SingleEntryWidget("Prime Example in Case");
			example.setEntryChooser(getShapeChooser());
			if(attr != null && attr.getExample() != null){
				example.setEntry(attr.getExample());
			}
			p.add(example);
			
			componentList.add(example);
			examples.put(attr,example);
		}
		
		// add to panel
		//ap.add(p,ap.getComponentCount()-1);
		return p;
	}
	
	private JPanel createNumberPanel(JTextField text,String title){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		text.setHorizontalAlignment(JTextField.RIGHT);
		text.setPreferredSize(new Dimension(232,28));
		panel.add(new JLabel(title),BorderLayout.NORTH);
		panel.add(text,BorderLayout.SOUTH);
		return panel;
	}
	
	/**
	 * create location panel
	 * @return
	 */
	private JPanel createLocationPanel(ConceptEntry attr){
		locations = new MultipleEntryWidget("  All Locations where Case Concept can be observed");
		locations.setEntryChooser(getShapeChooser());
		if(attr != null)
			locations.addEntries(attr.getLocations());
		componentList.add(locations);
		return locations;
	}
	
	/**
	 * create location panel
	 * @return
	 */
	private JPanel createRecommendationPanel(ConceptEntry e){
		recommendations = new MultipleEntryWidget("  All Recommendations/Advice associated with this concept");
		recommendations.setEntryChooser(getRecommendationsChooser());
		recommendations.setPreferredSize(new Dimension(150,60));
		recommendations.setVisibleRowCount(1);
		componentList.add(recommendations);
		
		if(e != null){
			recommendations.addEntries(e.getRecommendations());
		}
		return recommendations;
	}
	
	
	/**
	 * prompt for attributes
	 * @return
	 */
	private EntryChooser getAttributeChooser(){
		IOntology ont = concept.getConceptClass().getOntology();
		if(attributeChooser == null){
			attributeChooser = new TreeDialog(frame);
			((TreeDialog)attributeChooser).setTitle("Attributes");
			((TreeDialog)attributeChooser).setRoot(ont.getClass(OntologyHelper.ATTRIBUTES));
		}
		return attributeChooser;
	}
	
	/**
	 * prompt for attributes
	 * @return
	 */
	private EntryChooser getRecommendationsChooser(){
		IOntology ont = concept.getConceptClass().getOntology();
		if(recommendationsChooser == null){
			recommendationsChooser = new TreeDialog(frame);
			((TreeDialog)recommendationsChooser).setTitle("Recommendations");
			((TreeDialog)recommendationsChooser).setRoot(ont.getClass(OntologyHelper.RECOMMENDATIONS));
		}
		return recommendationsChooser;
	}
	
	
	/**
	 * prompt for attributes
	 * @return
	 */
	private EntryChooser getShapeChooser(){
		if(shapeChooser == null){
			shapeChooser = new ShapeSelectorPanel(shapeRoot);
			shapeChooser.setOwner(frame);
		}
		return shapeChooser;
	}
	
	
	/**
	 * perform action
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("add")){
			// add panel 
			attributePanel.add(createConceptPanel(null));
			attributePanel.revalidate();
			UIHelper.getWindowForComponent(attributePanel).pack();
			attributeCount ++;
			rem.setEnabled(true);
		}else if(cmd.equals("rem")){
			// remove last attribute panel (last component)
			attributePanel.remove(attributePanel.getComponentCount()-1);
			
			// revalidate
			attributePanel.validate();
			attributePanel.repaint();
			attributeCount --;
			if(attributeCount <= 0)
				rem.setEnabled(false);
			
			//	perhaps change layout
			if(attributePanel.getComponentCount()<=1){
				panelLayout.setColumns(1);
			}
			
			UIHelper.getWindowForComponent(attributePanel).pack();
		}else if(cmd.equals("assert")){
			boolean b = ((AbstractButton)e.getSource()).isSelected();
			//concept.setAsserted(b);
			assertConcept.setSelected(b);
			assertConceptButton.setSelected(b);
		}else if(cmd.equals("absent")){
			boolean b = ((AbstractButton)e.getSource()).isSelected();
			setLocationsEnabled(!b);
		}
	}
	
	private void setLocationsEnabled(boolean b){
		/*
		for(Component c: componentList)
			c.setEnabled(b);
		if(floatField != null)
			floatField.setEditable(b);
			*/
		//if(resourceLink != null)
		//	resourceLink.setEditable(b);
	}
	
	/**
	 * sync GUI changes with the concept
	 */
	public void sync(){
		// concept is negated if absent state changed
		negated = concept.isAbsent() ^ absentConcept.isSelected();
		
		// set absent and assert
		concept.setAbsent(absentConcept.isSelected());
		concept.setAsserted(assertConcept.isSelected());
		
		// load locations
		ArrayList<String> locs = new ArrayList<String>();
		for(Object o: locations.getEntries())
			locs.add(""+o);
		concept.setLocations(locs);
		
		// load examples
		ConceptEntry e = concept.getFeature();
		if(examples.containsKey(e))
			e.setExample(""+examples.get(e).getEntry());
		for(ConceptEntry a: concept.getAttributes()){
			if(examples.containsKey(a))
				a.setExample(""+examples.get(a).getEntry());
		}
		
		//set value
		if(floatField != null ){
			concept.setNumericValue(Double.parseDouble(floatField.getText()));
		}
		
		// set external resource
		if(resourceLink != null){
			concept.setResourceLink(resourceLink.getText());
		}
		
		// set a list of recommendations
		concept.getRecommendations().clear();
		for(Object obj: recommendations.getEntries())
			concept.getRecommendations().add((ConceptEntry )obj);
	}


	/**
	 * show new concept entry dialog
	 * @param entry
	 * @param frame
	 */
	public static void showConceptEntryDialog(ConceptEntry entry, CaseAuthor author){
		frame = author.getFrame();
		shapeRoot = author.getShapeSelector().getRoot();
		final java.util.List<ConceptEntry> rlist = new ArrayList<ConceptEntry>(entry.getRecommendations());
		final ConceptEntryPanel panel = new ConceptEntryPanel(entry);
		final ConceptSelector selector = author.getConceptSelector(entry);
		final JOptionPane opt = new JOptionPane(panel,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
		JDialog d = opt.createDialog(frame,"Case Concept Properties");
		d.setModal(false);
		d.setResizable(true);
		d.addWindowListener(new WindowAdapter(){
			public void windowDeactivated(WindowEvent e) {
				super.windowDeactivated(e);
				Object x = opt.getValue();
				if(x instanceof Integer && ((Integer)x).intValue() == JOptionPane.OK_OPTION){
					panel.sync();
					if(panel.isNegated())
						selector.firePropertyChange(ConceptSelector.CONCEPT_NEGATED,panel,panel.getConceptEntry());
					if(selector != null)
						selector.repaint();
					if(!rlist.equals(panel.getConceptEntry().getRecommendations()))
						selector.firePropertyChange(ConceptSelector.CONCEPT_RECOMMENDATIONS_CHANGED,panel,panel.getConceptEntry());
				}
			}
		});
		d.setVisible(true);
	}


}
