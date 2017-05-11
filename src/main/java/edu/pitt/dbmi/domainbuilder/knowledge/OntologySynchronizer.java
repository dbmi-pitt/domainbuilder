package edu.pitt.dbmi.domainbuilder.knowledge;

import java.util.*;

import javax.swing.JProgressBar;


/**
 * syncronized actions for a given ontology
 * @author tseytlin
 */
public class OntologySynchronizer {
	private LinkedList<OntologyAction> queue;
	private static OntologySynchronizer instance;
	
	private OntologySynchronizer(){
		queue = new LinkedList<OntologyAction>();
	}
	
	/**
	 * get instance
	 * @return
	 */
	public static OntologySynchronizer getInstance(){
		if(instance == null){
			instance = new OntologySynchronizer();
		}
		return instance;
	}
	
	/**
	 * does this synchronizer have any actions
	 * @return
	 */
	public boolean hasActions(){
		return !queue.isEmpty();
	}
	
	
	/**
	 * add ontology action
	 * @param a
	 */
	public void addOntologyAction(OntologyAction a){
		queue.add(a);
	}
	
	/**
	 * do undo operation
	 */
	public void undo(){
		if(!queue.isEmpty()){
			OntologyAction a = queue.removeLast();
			a.undo();
			System.out.println("UNDO: "+a);
		}
	}
	
	
	/**
	 * run synchronization
	 * should invoke in separate thread for best results
	 */
	public void run(){
		run(null);
	}
	
	
	/**
	 * run synchronization
	 * should invoke in separate thread for best results
	 * @param progress
	 */
	public void run(JProgressBar progress){
		if(progress != null){
			progress.setString("Synchronizing Ontology ...");
			progress.setMinimum(0);
			progress.setMaximum(queue.size()-1);
			progress.setIndeterminate(false);
		}
		// cycle through all actions 
		int size = queue.size();
		while(!queue.isEmpty()){
			try{
				OntologyAction a = queue.removeFirst();
				a.run();
				System.out.println("RUN: "+a);
			}catch(Exception ex){
				ex.printStackTrace();
			}
			if(progress != null){
				progress.setValue(size-queue.size());
			}
		}
		
		if(progress != null){
			progress.setString(null);
			progress.setIndeterminate(true);
		}
		
	}
	
	/**
	 * clear the queue
	 */
	public void clear(){
		queue.clear();
	}
	
}
