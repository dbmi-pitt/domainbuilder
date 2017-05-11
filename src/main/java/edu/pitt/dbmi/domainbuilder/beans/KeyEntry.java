/**
 * This class represents a concept from knowledge base as well as its position in the context.
 * Author: Eugene Tseytlin (University of Pittsburgh)
 */

package edu.pitt.dbmi.domainbuilder.beans;

import java.io.Serializable;

import edu.pitt.dbmi.domainbuilder.widgets.ConceptLabel;
import edu.pitt.terminology.lexicon.Concept;

/**
 * This class holds information about string that was recognized as a concept
 */
public class KeyEntry  implements Serializable {
    private static final long serialVersionUID = -469270744643580113L;
    private String text,key;
	private int offset;
	private Concept term;
	private int age = 3; // key entry "lives" for several iterations, this is used for retained attributes
	private boolean resolved;	
	private transient ConceptLabel conceptLabel;
	
	
	public KeyEntry(String t,String k, int offs, Concept le){
		text=t;
		key=k;
		offset=offs;
		term = le;
	}
	// getter methods
	public String getText(){
		return text;	
	}
	public String getKey(){
		return key;	
	}
	public int getOffset(){
		return offset;	
	}
	public int getLength(){
		return text.length();	
	}
	public Concept getConcept(){
		return term;	
	}
	public void setConcept(Concept e){
		term = e;
	}
	public void setKey(String s){
		key = s;	
	}
	public int getAge(){
		return age;		
	}
	public void decreseAge(){
		age --;
	}
	
	/**
	 * Returns the value of resolved.
	 */
	public boolean isResolved(){
		return resolved;
	}

	/**
	 * Sets the value of resolved.
	 * @param resolved The value to assign resolved.
	 */
	public void setResolved(boolean resolved){
		this.resolved = resolved;
	}

	/**
	 * Returns the value of conceptLabel.
	 */
	public ConceptLabel getConceptLabel()	{
		return conceptLabel;
	}

	/**
	 * Sets the value of conceptLabel.
	 * @param conceptLabel The value to assign conceptLabel.
	 */
	public void setConceptLabel(ConceptLabel conceptLabel)	{
		this.conceptLabel = conceptLabel;
	}
	
	/**
	 * Adjust offset by phrase location
	 */
	public void adjustOffset(int offs){
		offset += offs;	
	}
	
	public String toString(){
		return text+" "+key+" "+offset+" "+age;
	}
}

