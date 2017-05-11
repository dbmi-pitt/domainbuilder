package edu.pitt.dbmi.domainbuilder.util;

import javax.swing.*;


/**
 * list model that can be changed 
 * @author tseytlin
 */
public abstract class MutableListModel extends AbstractListModel {
	public abstract void addElement(Object obj);
	public abstract boolean removeElement(Object obj);
	public abstract void setElementAt(Object obj, int index);
	public abstract void insertElementAt(Object obj, int index);
	public abstract void removeAllElements();
	public abstract boolean containsElement(Object obj);
	public abstract void sort();
}
