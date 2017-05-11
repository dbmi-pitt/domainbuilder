package edu.pitt.dbmi.casetest;
import java.text.DateFormat;
import java.util.*;
import java.io.Serializable;

/**
 * This class contains all relevant information 
 * about a case
 * @author tseytlin
 */
public class CaseBean implements Serializable{
    private String name;
    private List imageName;
    private List diagnosis;
    private String domain;
    private String pattern;
    private String difficulty;
    private String status;
   
	private boolean authored;
    private Map testUsers;
   
    public CaseBean(){}
    
    public CaseBean(String name){
        this.name = name;
    }
    
    
    /**
     * Was case authored
     * @return Returns the authored.
     */
    public boolean isAuthored() {
        return authored;
    }
    /**
     * Set case authore status
     * @param authored The authored to set.
     */
    public void setAuthored(boolean authored) {
        this.authored = authored;
    }
    /**
     * Get case name
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * Set case name
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Get the map of test users.
     * key   = username
     * value = java.util.List of tested components
     * @return Returns the testUsers.
     */
    public Map getTestUserMap() {
        return testUsers;
    }
   
    /**
     * Set the map of test users
     * key   = username
     * value = java.util.List of tested components
     * @param testUsers The testUsers to set.
     */
    public void setTestUserMap(Map testUsers) {
        this.testUsers = testUsers;
    }
    
    /**
     * Add user to the map of test users
     * @param testUsers The testUsers to set.
     */
    public void addTestUser(String user){
        //create map if necessary
        if(testUsers == null)
            testUsers = new HashMap();
        
        //create username only if necessary
        if(!testUsers.containsKey(user))
            testUsers.put(user,new ArrayList());
    }
    
    
    /**
     * Set the map of test users
     * key   = username
     * value = java.util.List of tested components
     * @param testUsers The testUsers to set.
     */
    public void addTestedComponent(String user, Object component){
        // if user is alreader there 
        // this call is a noop
        addTestUser(user);
        
        //add to list
        ((List) testUsers.get(user)).add(component);
    }

    /**
     * @return Returns the diagnosis.
     */
    public String getDiagnosis() {
        if(!diagnosis.isEmpty())
        	return ""+diagnosis.get(0);
        else
        	return "";
    }
    
    /**
     * @param diagnosis The diagnosis to set.
     */
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = new ArrayList();
        this.diagnosis.add(diagnosis);
    }

    /**
     * @return Returns the diagnosis.
     */
    public List getDiagnoses() {
        return diagnosis;
    }
    
    /**
     * @param diagnosis The diagnosis to set.
     */
    public void setDiagnoses(Collection diagnosis) {
        this.diagnosis = new ArrayList(diagnosis);
    }
    
    
    /**
     * @return Returns the imageName.
     */
    public String getImageName() {
    	if(!imageName.isEmpty())
    		return ""+imageName.get(0);
    	else
    		return null;
    }

    /**
     * @param imageName The imageName to set.
     */
    public void setImageName(String imageName) {
        this.imageName = new ArrayList();
        this.imageName.add(imageName);
    }

    
    /**
     * @return Returns the imageName.
     */
    public List getSlides() {
        return imageName;
    }

    /**
     * @param imageName The imageName to set.
     */
    public void setSlides(Collection imageName) {
        this.imageName = new ArrayList(imageName);
    }
    
    
    
    /**
     * Find the latest user entry
     * @param list
     * @return
     */
    private List getLatestEntry(List list){
        Date date = null;
        List ret = null;
        for(Iterator i=list.iterator();i.hasNext();){
            List comp = (List) i.next();
            if(comp.size() >= 2){
                String st = ""+comp.get(0);
                Date dt = null;
                try{
                    dt = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,DateFormat.SHORT).parse(""+comp.get(1));
                }catch(Exception ex){
                    //ex.printStackTrace();
                }
                
                //get the latest entry
                if(dt != null){
                    if(date == null || date.compareTo(dt) < 0){
                        date = dt;
                        ret = comp;
                    }
                }
            }
        }
        return ret;
    }
    
    public void setStatus(String status) {
		this.status = status;
	}

    
    /**
     * @return Returns the status.
     */
    public String getStatus() {
        if(testUsers == null)
            return status;
        else{
            // iterate over users
            String status = "AUTH";
            Date date = null;
            for(Iterator i=testUsers.keySet().iterator();i.hasNext();){
                List list = (List) testUsers.get(""+i.next());
                // get last entry
                //List comp = (List) list.get(list.size()-1);
                List comp = getLatestEntry(list);
                if(comp != null && comp.size() >= 2){
                    String st = ""+comp.get(0);
                    Date dt = null;
                    try{
                        dt = DateFormat.getDateTimeInstance(
                        DateFormat.SHORT,DateFormat.SHORT).parse(""+comp.get(1));
                    }catch(Exception ex){
                        //ex.printStackTrace();
                    }
                    
                    //get the latest entry
                    if(dt != null){
                        if(date == null || date.compareTo(dt) < 0){
                            date = dt;
                            status = (st.equals("FIX"))?"TEST":st;
                        }
                    }
                }
            }
            return status;
        }
    }
    
    /**
     * @return Returns the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain The domain to set.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * @return Returns the pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern The pattern to set.
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
 
    /**
     * Comparators
     */
    public static class NameComparator implements Comparator {
        public int compare(Object a, Object b){
            if(a instanceof CaseBean && b instanceof CaseBean){
                CaseBean a1 = (CaseBean) a;
                CaseBean b1 = (CaseBean) b;
                return a1.getName().compareTo(b1.getName());
            }else            
                return 0;
        }
    }
    public static class StatusComparator implements Comparator {
        public int compare(Object a, Object b){
            if(a instanceof CaseBean && b instanceof CaseBean){
                CaseBean a1 = (CaseBean) a;
                CaseBean b1 = (CaseBean) b;
                int x = a1.getStatus().compareTo(b1.getStatus());
                if(x != 0)
                    return x;
                else
                    return a1.getName().compareTo(b1.getName());        
            }else            
                return 0;
        }
    }
    public static class DiagnosisComparator implements Comparator {
        public int compare(Object a, Object b){
            if(a instanceof CaseBean && b instanceof CaseBean){
                CaseBean a1 = (CaseBean) a;
                CaseBean b1 = (CaseBean) b;
                int x = a1.getDiagnosis().compareTo(b1.getDiagnosis());
                if(x != 0)
                    return x;
                else
                    return a1.getName().compareTo(b1.getName());        
            }else            
                return 0;
        }
    }
    public static class DifficultyComparator implements Comparator {
        public int compare(Object a, Object b){
            if(a instanceof CaseBean && b instanceof CaseBean){
                CaseBean a1 = (CaseBean) a;
                CaseBean b1 = (CaseBean) b;
                int x = a1.getDifficulty().compareTo(b1.getDifficulty());
                if(x != 0)
                    return x;
                else
                    return a1.getName().compareTo(b1.getName());        
            }else            
                return 0;
        }
    }
    
    public static class PatternComparator implements Comparator {
        public int compare(Object a, Object b){
            if(a instanceof CaseBean && b instanceof CaseBean){
                CaseBean a1 = (CaseBean) a;
                CaseBean b1 = (CaseBean) b;
                int x = a1.getPattern().compareTo(b1.getPattern());
                if(x != 0)
                    return x;
                else
                    return a1.getName().compareTo(b1.getName());        
            }else            
                return 0;
        }
    }
	/**
	 * @return the difficulty
	 */
	public String getDifficulty() {
		return difficulty;
	}

	/**
	 * @param difficulty the difficulty to set
	 */
	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}
  
}
