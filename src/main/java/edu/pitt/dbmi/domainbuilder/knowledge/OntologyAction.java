package edu.pitt.dbmi.domainbuilder.knowledge;


/**
 * undoable ontology action
 * @author tseytlin
 */
public interface OntologyAction extends  Runnable {
	/**
	 * undo action
	 */
	public void undo();
}
