package edu.pitt.dbmi.domainbuilder.util;

import java.util.*;

import edu.pitt.ontology.IClass;
import edu.pitt.ontology.IOntology;
import edu.pitt.terminology.client.OntologyTerminology;

public class DomainTerminology extends OntologyTerminology {
	private SortedMap<String,List<String>> fullWordMap;
	
	public DomainTerminology(IOntology ont){
		super(ont);
	}
	
	public DomainTerminology(IOntology ont, IClass cls){
		super(ont,cls);
	}
	
	/**
	 * loads ontology into terminology
	 */
	public void load(){
		fullWordMap = new TreeMap<String,List<String>>();
		super.load();
	}
	
	/**
	 * load individual class
	 * @param cls
	 */
	protected void loadClass(IClass cls){
		// don't go into classes that we already visited
		if(termMap.containsValue(cls.getName()))
			return;
		
		// get full word list
		Set<String> terms = getTerms(cls,false);
		for(String term: terms){
			for(String word: term.split(" ")){
				List<String> list = fullWordMap.get(word);
				if(list == null){
					list = new ArrayList<String>();
					fullWordMap.put(word,list);
				}
				list.add(cls.getName());
			}
		}
		
		// do the default thing
		super.loadClass(cls);
	}
	
	/**
	 * get all words
	 * @return
	 */
	public Set<String> getWords(){
		return fullWordMap.keySet();
	}
	
	/**
	 * get classes associated with given word
	 * @param word
	 * @return
	 */
	
	public IClass [] getWordClasses(String word){
		List<String> list = fullWordMap.get(word);
		if(list != null){
			IClass [] cls = new IClass [list.size()];
			for(int i=0;i<cls.length;i++)
				cls[i] = getOntology().getClass(list.get(i));
			return cls;
		}
		return new IClass [0];
	}
	
	
}
