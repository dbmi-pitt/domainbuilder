package edu.pitt.dbmi.domainbuilder.widgets;

import java.util.List;

import edu.pitt.dbmi.domainbuilder.beans.ShapeEntry;

/**
 * defines behavior of annotation manager
 * @author Eugene Tseytlin
 *
 */
public interface AnnotationManager {
	/**
	 * get annotations that are associated with given name
	 * @param name of annotation or a tag
	 * @return
	 */
	public List<ShapeEntry> getAnnotations(String name);
	
	/**
	 * get all annotations
	 * @return
	 */
	public List<ShapeEntry> getAnnotations();
	
	
	/**
	 * set unique anntation number like an offset
	 * @param x
	 */
	public void setAnnotationNumber(int x);
}
