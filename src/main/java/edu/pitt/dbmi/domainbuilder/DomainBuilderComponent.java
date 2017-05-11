package edu.pitt.dbmi.domainbuilder;

import java.awt.Component;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JMenuBar;

/**
 * component that describes a "tab" in domain builder
 * @author tseytlin
 */
public interface DomainBuilderComponent extends PropertyChangeListener {
	/**
	 * get name of the component
	 * @return
	 */
	public String getName();
	
	/**
	 * get icon for the component
	 * @return
	 */
	public Icon getIcon();
	
	
	/**
	 * get component
	 * @return
	 */
	public Component getComponent();
	
	
	/**
	 * get menu bar
	 * @return
	 */
	public JMenuBar getMenuBar();
	
	
	/**
	 * load whatever resources one needs to get this piece working 
	 */
	public void load();
	
	/**
	 * dispose of this component
	 */
	public void dispose();
}
