package edu.pitt.dbmi.misc;
import java.net.*;
import java.util.*;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.POntology;



public class KnowledgeBaseTransmutator {
	private IOntology kb;
	
	public KnowledgeBaseTransmutator(String path) throws Exception{
		kb = POntology.loadOntology(URI.create(path));
	}
	
	public void start() throws Exception {
		IClass diagnosticFeatures = kb.getClass("DIAGNOSTIC_FEATURES");
		// create hasFindings subproperties
		IClass [] dchild = diagnosticFeatures.getDirectSubClasses();
		IProperty prop = kb.getProperty("hasFinding");
		for(int i=0;i<dchild.length;i++){
			String name = derivePropertyName(dchild[i].getName());
			System.out.println("Creating property "+name);
			IProperty p = prop.createSubProperty(name);
			p.setRange(new IResource []{dchild[i]});
		}
		
		// modify restrictions in all diseases
		IClass [] disease = kb.getClass("DISEASES").getSubClasses();
		for(int i=0;i<disease.length;i++){
			System.out.println("processing disease: "+disease[i]);
			//specify necessary and sufficient
			specifyExpression(disease[i].getEquivalentRestrictions());
			//specify necessary restrictions
			specifyExpression(disease[i].getNecessaryRestrictions());
		}
		
		kb.save();
		
	}
	
	/**
	 * process restriction
	 * @param ex
	 */
	private void specifyExpression(ILogicExpression ex){
		for(Object e: ex){
			if(e instanceof IRestriction){
				specifyRestriction((IRestriction) e);
			}else if(e instanceof ILogicExpression){
				specifyExpression((ILogicExpression) e);
			}
		}
	}
	
	/**
	 * specify restriction
	 * @param r
	 */
	private void specifyRestriction(IRestriction r){
		IProperty p = r.getProperty();
		// make sure you modify the right restriction
		if(r.getRestrictionType() == IRestriction.SOME_VALUES_FROM
		   && p.equals(kb.getProperty("hasFinding"))){
			//I AM LAZY AND ASSUME THAT EXPRESSION HAS ONLY ONE
			//OPERAND AND IS NOT A COMPLEMENT
			ILogicExpression exp = r.getParameter();
			IClass c = (IClass) exp.getOperand();
			IClass parent = findType(c);
			if(parent != null && kb.hasResource(derivePropertyName(parent.getName()))){
				r.setProperty(kb.getProperty(derivePropertyName(parent.getName())));
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private IClass findType(IClass c){
		if(c.getName().matches("[A-Z_]+"))
			return c;
		IClass [] p = c.getDirectSuperClasses();
		//for(int i=0;i<p.length;i++)
		if(p.length > 0)
			return findType(p[0]);
		return null;
	}
	
	
	/**
	 * derive pretty name from class name
	 * @param name
	 * @return
	 */
	private String derivePropertyName(String name){
		String [] p = name.split("_");
		StringBuffer buf = new StringBuffer("has");
		for(int j=0;j<p.length;j++){
			String s = p[j].toLowerCase();
			buf.append(s.substring(0,1).toUpperCase()+s.substring(1));
		}
		return buf.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		KnowledgeBaseTransmutator kbt = new KnowledgeBaseTransmutator(args[0]);
		kbt.start();

	}

}
