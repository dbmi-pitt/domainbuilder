package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;

public class ReportEntryPanel extends JPanel implements ActionListener {
	private ConceptEntry concept;
	private SingleEntryWidget feature, action;
	private MultipleEntryWidget attributes;
	private static Frame frame;
	
	public ReportEntryPanel(ConceptEntry e){
		super();
		this.concept = e;
		
		setLayout(new BorderLayout());
		feature = new SingleEntryWidget("Reportable Item");
		feature.setEditable(false);
		add(feature,BorderLayout.NORTH);
		
		attributes = new MultipleEntryWidget("Possible Values");
		attributes.setEntryChooser(getAttributeChooser());
		attributes.setPreferredSize(new Dimension(250,200));
		attributes.setBorder(new EmptyBorder(10,0,10,0));
		attributes.setEditable(false);
		attributes.setDragEnabled(false);
		attributes.setListLayout(JList.VERTICAL);
		add(attributes,BorderLayout.CENTER);
		
		action = new SingleEntryWidget("Required Action");
		action.setEntryChooser(getActionChooser());
		add(action,BorderLayout.SOUTH);
		loadEntry(e);
	}
	
	/**
	 * prompt for attributes
	 * @return
	 */
	private EntryChooser getAttributeChooser(){
		if(concept == null)
			return null;
		IOntology ont = concept.getConceptClass().getOntology();
		TreeDialog attributeChooser = new TreeDialog(frame);
		attributeChooser.setTitle("Attributes");
		attributeChooser.setRoot(ont.getClass(OntologyHelper.ATTRIBUTES));
		return attributeChooser;
	}
	
	/**
	 * prompt for attributes
	 * @return
	 */
	private EntryChooser getActionChooser(){
		if(concept == null)
			return null;
		IOntology ont = concept.getConceptClass().getOntology();
		TreeDialog attributeChooser = new TreeDialog(frame);
		attributeChooser.setTitle("Actions");
		attributeChooser.setRoot(ont.getClass(OntologyHelper.ACTIONS));
		return attributeChooser;
	}
	
	
	/**
	 * load entry 
	 * @param e
	 */
	private void loadEntry(ConceptEntry e){
		if(e == null)
			return;
		feature.setEntry(e.getFeature());
		action.setEntry(e.getAction());
		List<ConceptEntry> values = new ArrayList<ConceptEntry>();
		for(ConceptEntry v: e.getPotentialAttributes()){
			if(!OntologyHelper.isAttributeCategory(v.getConceptClass()))
				values.add(v);
		}
		attributes.addEntries(values);
	}
	
	
	/**
	 * sync to concept
	 */
	public void sync(){
		if(action.getEntry() != null)
			concept.setAction((ConceptEntry)action.getEntry());
		ConceptEntry [] attrs = (ConceptEntry []) UIHelper.convertArray(ConceptEntry.class,attributes.getEntries());
		// this is for display purposes
		concept.setPotentialAttributes(attrs);
		
		// this is to create new findings
		List<ConceptEntry> vals = new ArrayList<ConceptEntry>();
		for(ConceptEntry v: attrs){
			if(OntologyHelper.isAttributeCategory(v.getConceptClass())){
				for(IClass c: v.getConceptClass().getDirectSubClasses()){
					vals.add(new ConceptEntry(c));
				}
			}else{
				vals.add(v);
			}
		}
		
		concept.setAttributes(vals.toArray(new ConceptEntry [0]));
		
	}
	
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String [] args){
		JOptionPane.showMessageDialog(null,new ReportEntryPanel(null),"",JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * show new concept entry dialog
	 * @param entry
	 * @param frame
	 */
	public static void showReportEntryDialog(ConceptEntry entry,JFrame f ){
		frame = f;
		final ReportEntryPanel panel = new ReportEntryPanel(entry);
		/*
		JOptionPane opt = new JOptionPane(panel,JOptionPane.PLAIN_MESSAGE);
		JDialog d = opt.createDialog(frame,"Report Entry Properties");
		d.setModal(true);
		d.setResizable(true);
		d.addWindowListener(new WindowAdapter(){
			public void windowDeactivated(WindowEvent e) {
				super.windowDeactivated(e);
				panel.sync();
			}
		});
		d.setVisible(true);
		*/
		int r = JOptionPane.showConfirmDialog(frame,panel,"Report Entry Properties",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION)
			panel.sync();
	}
}
