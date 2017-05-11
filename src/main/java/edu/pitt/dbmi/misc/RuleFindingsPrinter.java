package edu.pitt.dbmi.misc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;
import edu.pitt.ontology.protege.*;
import edu.pitt.ontology.ui.OntologyExplorer;
import edu.pitt.ontology.*;


public class RuleFindingsPrinter {

	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	private static Set<IResource> getFindings(ILogicExpression exp, IProperty prop, Set<IResource> list,Set<IResource> alist){
		for(Object obj : exp){
			if(obj instanceof IRestriction){
				IRestriction r = (IRestriction) obj;
				IProperty p = r.getProperty();
				getFindings(r.getParameter(),p,list,alist);
			}else if(obj instanceof IClass && prop != null){
				// convert class to a concept entry
				IClass c = (IClass) obj;
				if( prop.getName().contains(HAS_FINDING) || 
					prop.getName().contains(HAS_CLINICAL)){
					list.add(c);
				}else if(prop.getName().contains(HAS_NO_FINDING)){
					alist.add(c);
				}
				/*
				else if(prop.getName().contains(OntologyHelper.HAS_NO_FINDING)){
					entry.setAbsent(true);
					list.add(entry);
				}*/
			}else if(obj instanceof ILogicExpression){
				// recurse into expression
				getFindings((ILogicExpression) obj,prop,list,alist);
			}
		}
		return list;
	}
	
	/**
	 * extract concept entries from expression and put them into a list
	 * @param exp
	 * @return
	 */
	private static Set<IResource> getFindings(IRestriction [] restrictions, Set<IResource> list,Set<IResource> alist){
		for(IRestriction r : restrictions){
			getFindings(r.getParameter(),r.getProperty(),list,alist);
		}
		return list;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String path = "/home/tseytlin/Work/curriculum/owl/skin/PITT/Melanocytic.owl";
		//String path = "/home/tseytlin/Work/curriculum/owl/skin/UPMC/VesicularDermatitis.owl";
		//String path = "/home/tseytlin/VesicularDermatitis.owl";
		//String path = "/home/tseytlin/NodularDiffuseDermatitis.owl";
		//String path = "/home/tseytlin/PerivascularDermatitis.owl";
		/*
		for(File path: (new File("/home/tseytlin")).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".owl");
			}
		})){
		*/
			IOntology ontology = POntology.loadOntology(path);
	
			
			Set<IResource> findings = new TreeSet<IResource>();
			Set<IResource> afindings = new TreeSet<IResource>();
			
			// iterate over all diagnosis
			for(IClass diagnosis : ontology.getClass(DISEASES).getSubClasses()){
				if(diagnosis.getName().equals(DISEASES))
					continue;
				// get necessary and sufficient restrictions to get diagnostic features
				ILogicExpression exp = diagnosis.getEquivalentRestrictions();
				getFindings(exp,null,findings,afindings);
				
				// add stuff that might be inherited
				getFindings(diagnosis.getRestrictions(ontology.getProperty(HAS_FINDING)),findings,afindings);
				getFindings(diagnosis.getRestrictions(ontology.getProperty(HAS_NO_FINDING)),findings,afindings);
				getFindings(diagnosis.getRestrictions(ontology.getProperty(HAS_CLINICAL)),findings,afindings);
			}
		
			System.out.println("--- present findings ---");
			for(IResource r: findings){
				//System.out.println(r.getName());
				if(r instanceof IClass){
					long time = System.currentTimeMillis();
					IClass c = (IClass) r;
					String l = ""+getAttributes(c);
					System.out.println(c.getName()+" | "+getFeature(c)+" | "+l.substring(1,l.length()-1)+"| "+(System.currentTimeMillis()-time));
				}
			}
			System.out.println("--- absent findings ---");
			for(IResource r: afindings){
				//System.out.println(r.getName());
				if(r instanceof IClass){
					long time = System.currentTimeMillis();
					IClass c = (IClass) r;
					String l = ""+getAttributes(c);
					System.out.println(c.getName()+" | "+getFeature(c)+" | "+l.substring(1,l.length()-1)+"| "+(System.currentTimeMillis()-time));
				}
			}
		//}
		/*
		OntologyExplorer explorer = new OntologyExplorer();
		explorer.setRoot(ontology.getRoot());
		JOptionPane.showMessageDialog(null,explorer,"",JOptionPane.PLAIN_MESSAGE);
		*/
	}

}
