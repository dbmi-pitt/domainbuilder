package edu.pitt.dbmi.casetest;


import java.net.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.regex.*;



public class CaseTestHelper {
    private static final String RESOURCES = "/resources/";
    private static final String []  EXCLUDE_DIR = new String [] {"CVS",".svn","examples","source"};
    public static String DISEASES = "DIAGNOSES";
	public static final String NEW_CASE = "NEW_CASE";
    private static final String RECURSE_DIR = "kb";
    private static Map jnlpMap = new HashMap();
    private static Map caseMap = new HashMap();
    private static List<String> domains = new ArrayList<String>();
    private static String protegeDir;
    private static Connection conn;
    private static String url,driver,user,pass;
    private static FileFilter protegeFilter = new FileFilter(){
        public boolean accept(File f){
            return f.getName().matches(".+\\.case");
        }
    };
    
    // constants
    public static final String CASE   = "CASE";
    public static final String SERVER = "SERVER";
    public static final String DOMAIN = "DOMAIN";
    public static final String CONDITION = "CONDITION";
    public static final String USER = "USER";
    public static final String PASS = "PASS";
    public static final String TYPE = "TYPE";
    public static final String SESSION = "SESSION";
    /**
     * Set path of protege directory
     * @param path
     */
    public static void setProtegeDir(String path){
        protegeDir = path;
    }
    
    
    /**
     * Query servlet for object
     * <servlet_url>?action=<action>
     * @param URL servlet URL
     * @param String type of action
     * @return Object object returned by the servlet
     */
    public static Object queryServlet( URL servlet, String action ) {
        return Utils.queryServlet(servlet,action);
    }
    
    
    /**
     * Get WebLauncher from given template name and
     * substitution map
     */
    public static String getWebLuncher(String jnlp, Map sub) throws IOException{
        String text = (String) jnlpMap.get(jnlp);
        if(text == null){
            //read template if for the first time
            try{
                text = Utils.getText(
                CaseTestHelper.class.getResourceAsStream(RESOURCES+jnlp),null);
            }catch(IOException ex){
                return null;
            }
            // save the template
            jnlpMap.put(jnlp,text);
        }
        
        // do substitution
        if(sub != null){
            for(Iterator i=sub.keySet().iterator();i.hasNext();){
                String key = i.next().toString();
                text = text.replaceAll(key,""+sub.get(key));
            }
        }
        return text;
    }
    
    /**
     * Lookup user map for a given project file
     * @param domain
     * @param name of the p
     * @return
     */
    private static Map lookupUserMap(String domain, String name){
        return Utils.getTestUserMap(getConnection(),domain,name);
    }
    
    
    /**
     * Lookup image name in the given project file
     * @param filename of the project
     * @param name of the p
     * @return
     */
    private static String lookupImageName(File file, String name){
        // read in instance file
        String iname = null;
        BufferedReader reader = null;
       
        try{
            File f = new File(file.getParentFile(),name+".pins");
            reader = new BufferedReader(new FileReader(f));
            // iterate through file to find instance
            Pattern pt = Pattern.compile(".*\\bslide_name\\s+\"(.*)\".*");
            for(String line = reader.readLine();line != null;line=reader.readLine()){
                Matcher m = pt.matcher(line);
                if(m.matches()){
                    iname = m.group(1);
                    break;
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ex){}
            }
        }
        // if null, then guess, if no extension append ".tif"
        if(iname == null)
            iname = (name.startsWith("AP"))?name+".svs":name+".tif";
        else if(iname.lastIndexOf('.') < 0)
            iname = iname+".tif";    
        return iname ;
    }
    
    
   
    
    
    /**
     * Read cases from disk
     * @param domain
     * @return
     */
    private static Map readCases(String domain){
        Map map = new HashMap();
        File dir = new File(protegeDir+File.separator+domain);
        File [] files  = dir.listFiles(protegeFilter);
        if(files != null){
            for(int i=0;i<files.length;i++){
                String name = files[i].getName();
                if(name.endsWith(".case"))
                    name = name.substring(0,name.length()-5);
                CaseBean bean = new CaseBean(name);
                bean.setDomain(domain);
                
                try{
	                Map info = readCaseFile(new FileInputStream(files[i]));
	                bean.setDiagnoses((Collection)info.get("diagnosis"));
	                bean.setSlides((Collection)info.get("slides"));
	                bean.setPattern("n/a");
	                bean.setDifficulty(""+info.get("difficulty"));
	                if("complete".equalsIgnoreCase(""+info.get("status")))
	                	bean.setStatus("TEST");
	                else if("tested".equalsIgnoreCase(""+info.get("status")))
	                	bean.setStatus("OK");
	                else
	                	bean.setStatus("AUTH");
	                bean.setTestUserMap(lookupUserMap(domain,name));
                }catch(Exception ex){
                	ex.printStackTrace();
                }
                
                
                // store in the map
                map.put(name,bean);
            }
        }else{
            System.err.println("ERROR: directory "+dir+" could not be listed");
        }
        return map;
    }
    
    /**
     * Do recursive listing of dir
     * @param dir
     * @param list
     */
    private static void recursiveList(File dir,List list,String prefix){
        File [] files  = dir.listFiles();
        // add children
        for(int i=0;i<files.length;i++){
            if(files[i].isDirectory() && !excludeDir(files[i].getName())){
                list.add(prefix+files[i].getName());
                recursiveList(files[i],list,prefix+files[i].getName()+"/");
            }
        }
    }
    
    /**
     * Do recursive listing of dir
     * @param dir
     * @param list
     */
    private static void pruneList(List<String> list,int n){
    	ArrayList<String> torem = new ArrayList<String>();
    	for(String s : list){
    	   if(Utils.getCharCount(s,'/') < n)
    		   torem.add(s);
       }
    	list.removeAll(torem);
    }
    
    /**
     * check if name should be excluded
     * @param name
     * @return
     */
    private static boolean excludeDir(String name){
    	for(int i=0;i<EXCLUDE_DIR.length;i++)
    		if(name.matches(EXCLUDE_DIR[i]))
    			return true;
    	return false;
    }
    
    /**
     * Get list of available domains
     * @return list of domains
     */
    public static List getDomains(){
        if(domains.isEmpty()){
            File dir = new File(protegeDir);
            recursiveList(dir,domains,"");
            pruneList(domains,2);
        }
        return domains;
    }
    
    /**
     * Check if user/pass is legit
     * @param user
     * @param pass
     * @return true/false
     */
    public static boolean isAuthenticated(String user, String pass){
    	/*
    	try{
            return dbTool.isUserValidadated(user,pass);
        }catch(Exception ex){
            return false;
        }
        */
    	return true;
    }
    
    
    /**
     *  Get list of cases, the list is cached
     */
    public static List getCases(String domain){
        Map map = (Map) caseMap.get(domain);
        if(map == null){
            map = readCases(domain);
            caseMap.put(domain,map);
        }
        return new ArrayList(map.values());
    }
    
    /**
     *  Get list of cases, the list is cached
     */
    public static CaseBean getCase(String domain, String name){
        Map map = (Map) caseMap.get(domain);
        if(map != null){
            return (CaseBean) map.get(name);
        }else
            return null;
    }
    
    /**
     * Forget cache
     */
    public static void refresh(){
        caseMap.clear();
        domains.clear();
    }
    
    /**
     * Save given bean in the DB
     */
    public static void saveCase(CaseBean bean){
        Utils.saveTestUserMap(getConnection(),bean.getDomain(),bean.getName(),bean.getTestUserMap());
    }


    /**
     * @param dbTool The dbTool to set.
     */
    public static void setConnection(String dr, String ur, String us, String ps) {
    	driver = dr;
    	url = ur;
    	user = us;
    	pass = ps;
    }
    
    public static Connection getConnection(){
    	if(conn == null){
    		try{
                Class.forName(driver).newInstance();
                conn = DriverManager.getConnection(url, user, pass);
            } catch (Exception e) {
                e.printStackTrace();   
            }
            
            // check tables
            Utils.checkTestCaseTable(conn);
    	}
    	return conn;
    }
    
    
    /**
     * close DB connection
     */
    public static void closeConnection(){
    	if(conn != null){
    		try{
    			conn.close();
    		}catch(Exception ex){
    			ex.printStackTrace();
    		}
    		conn = null;
    	}
    }
    
    
    
    /**
     * Lookup image name in the given project file
     * @param filename of the project
     * @param name of the p
     * @return
     */
    private static String findSlideInstance(File file, String name){
        // read in instance file
        BufferedReader reader = null;
        StringBuffer buf = new StringBuffer();
        try{
            File f = new File(file.getParentFile(),name+".pins");
            if(f.canRead()){
                reader = new BufferedReader(new FileReader(f));
                // iterate through file to find matching instance
                boolean matchFound = false;
                for(String line = reader.readLine();line != null;line=reader.readLine()){
                    // find begining
                    if(matchFound || line.matches("\\(\\[.+\\]\\s+of\\s+SLIDE")){
                        buf.append(line);
                        matchFound = true;
                        //find end
                        if(line.matches(".+\\)\\)") || line.matches("\\s*\\)\\s*")){
                            matchFound = false;
                            break;
                        }
                    }
                }
            }else{
                System.err.println("ERROR: CaseTestHelper.findSlideInstance() file "+f+" does not exist");
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ex){}
            }
        }
        return buf.toString();
    }
   
    /**
     * Extract image name from slide instance
     * @param s slide instance string
     * @return
     */
    private static String lookupImageName(String s){
        Pattern pt = Pattern.compile(".*\\(\\bslide_name\\s+\"(.*)\"\\).*");
        Matcher m = pt.matcher(s);
        if(m.matches()){
            return m.group(1);
        }
        return "";
    }
    
    /**
     * return a list of slide images
     * @param f
     * @return
     */
    private static Map lookupInfoOWL(File f){
    	// read in instance file
        BufferedReader reader = null;
        Set slides = new HashSet();
        Set diagnosis   = new HashSet();
        String difficulty = null;
        try{
            if(f.canRead()){
                reader = new BufferedReader(new FileReader(f));
                // this is what I am looking for <T:SLIDE rdf:ID="AP_926_NEGRAB.svs">
                // iterate through file to find matching instance
                // >normal</T:case_difficulty>
                Pattern pt1 = Pattern.compile("\\s*<T:SLIDE rdf:ID=\"(.*)\">\\s*");
                Pattern pt2 = Pattern.compile("\\s*>(.*)</T:final_diagnosis>\\s*");
                Pattern pt3 = Pattern.compile("\\s*>(.*)</T:case_difficulty>\\s*");
                for(String line = reader.readLine();line != null;line=reader.readLine()){
                	// find begining
                	Matcher m = pt1.matcher(line);
                	if(m.matches()){
                		slides.add(m.group(1));
                	}else{
                		m = pt2.matcher(line);
                		if(m.matches()){
                			diagnosis.add(m.group(1));
                		}else{
                			m = pt3.matcher(line);
                    		if(m.matches()){
                    			difficulty = m.group(1);
                    		}
                		}
                	}
                }
            }else{
                System.err.println("ERROR: CaseTestHelper.findSlideInstance() file "+f+" does not exist");
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            if(reader != null){
                try{
                    reader.close();
                }catch(IOException ex){}
            }
        }
        List list = new ArrayList(diagnosis);
        Collections.sort(list);
        Map map = new HashMap();
        map.put("slides",slides);
        map.put("diagnosis",list);
        map.put("difficulty",difficulty);
        return map;
    }
    
    

    
    /**
     * load
     * @param is
     * @param manager
     * @throws IOException
     */
    private static Map readCaseFile(InputStream is) throws IOException {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    	String line,field = null;
    	StringBuffer buffer = new StringBuffer();
    	Pattern pt = Pattern.compile("\\[([\\w\\.]+)\\]");
    	Map<String,String> map = new HashMap<String,String>();
    	while((line = reader.readLine()) != null){
    		line = line.trim();
    		// skip comments
    		if(line.startsWith("#"))
    			continue;
    		// extract headers
    		Matcher mt = pt.matcher(line);
    		if(mt.matches()){
    			// save previous field
    			if(field != null){
    				map.put(field,buffer.toString());
    				buffer = new StringBuffer();
    			}
    			field = mt.group(1);
    		}else{
    			buffer.append(line+"\n");
    		}
    	}
    	// finish the last item
    	if(field != null && buffer.length() > 0){
    		map.put(field,buffer.toString());
    	}
    	reader.close();
    
    	
     	Set<String> exclude = new HashSet<String>();
     	String name, status = "", report, difficulty = "normal";
     	List<String> slides = new ArrayList<String>();
     	List<String> dx = new ArrayList<String>();
     	
    	// set case name
    	if(map.containsKey("CASE")){
    		exclude.add("CASE");
    		Properties p = Utils.getProperties(map.get("CASE"));
    		name = p.getProperty("name",NEW_CASE);
    		
    		if(p.containsKey("status"))
    			status = p.getProperty("status");
    		
    	}else
    		throw new IOException("Case file is missing a [CASE] header");	
    	
    	// set report
    	if(map.containsKey("REPORT")){
    		exclude.add("REPORT");
    		report = map.get("REPORT").trim();
    	}
    	
    	// load slides 
    	if(map.containsKey("SLIDES")){
    		exclude.add("SLIDES");
    		for(String s: map.get("SLIDES").trim().split("\n")){
    			if(s.length() > 0){
	    			slides.add(s);
    			}
			}
    	}
    	
    	
    	// load concepts into case
    	String [] categories = new String [] {DISEASES};
    	//		,DIAGNOSTIC_FEATURES,
    	//		PROGNOSTIC_FEATURES,CLINICAL_FEATURES,ANCILLARY_STUDIES};
    	for(String key: categories ){
    		exclude.add(key);
	    	if(map.containsKey(key)){
	    		for(String n: map.get(key).trim().split("\n")){
	    			if(n.length() > 0){
		    			exclude.add(n);
		    			dx.add(n);
	    			}
	    		}
	    	}
    	}
    	
    	
        Collections.sort(dx);
        Map rmap = new HashMap();
        rmap.put("slides",slides);
        rmap.put("diagnosis",dx);
        rmap.put("difficulty",difficulty);
        rmap.put("status",status);
        return rmap;
    }
    
    /*
    public static void main(String [] args) throws Exception {
        String slide = findSlideInstance(new File(args[0]),args[1]);
        //System.out.println(slide);
        System.out.println(lookupImageName(slide));
        System.out.println(lookupDiagnosis(slide));
        System.out.println(lookupPatterns(slide));
    }
    */
    
    /**
     * Chop the name of the string to size
     * and append ...
     */
    public static String chopString(String s, int size){
        if(s == null)
        	return "";
    	
        if(s.startsWith("["))
        	s = s.substring(1);
        
        if(s.endsWith("]"))
        	s = s.substring(0,s.length()-1);
        
        
    	if(s.length()<=size)
            return s;
        else
            return s.substring(0,size)+"...";
    }
    
    
    /**
     * Sort list of components, so that the component
     * whose time is more recent is on tip
     * @param list
     */
    public static void sortComponentsByTime(List list){
        Collections.sort(list,new Comparator(){
            public int compare(Object o1, Object o2){
                // make sure we have lists
                if(o1 instanceof List && o2 instanceof List){
                    List l1 = (List) o1;
                    List l2 = (List) o2;
                    // make sure they are the right sizes
                    if(l1.size() >= 2 && l2.size() >=2){
                        //get 2nd entry (time)
                        String s1 = ""+l1.get(1);
                        String s2 = ""+l2.get(1);
                        return s2.compareTo(s1);
                    }
                }
                return 0;
            }
        });
    }
    
    /**
     * create a map out of case list
     * @param list
     * @param size
     * @return
     */
    public static Map createCaseMap(List list, int size){
    	Map map = new HashMap();
    	if(list.isEmpty()){
    		map.put(""+1,Collections.EMPTY_LIST);
    	}else{
    		int page = 1;
    		for(int i=0;i<list.size();i++){
    			Object o = list.get(i);
    			List l = (List) map.get(""+page);
    			if(l == null){
    				l = new ArrayList();
    				map.put(""+page,l);
    			}
    			l.add(o);
    			if(l.size() >= size)
    				page ++;
    		}
    	}
    	return map;
    }
    
    /**
     * iterate over cases and create key/value pair
     * stat summary map
     * @param list
     * @return
     */
    public static Map createSummaryMap(List list){
    	int okcount = 0, testcount = 0, normalcount = 0;
    	int difficultcount = 0;
    	Map diseases = new TreeMap();
    	for(int i=0;i<list.size();i++){
    		CaseBean bean = (CaseBean) list.get(i);
    		// count number of cases to test
			if(!bean.getStatus().equals("AUTH")){
				testcount++;
				
				// count number of tested cases
				if(bean.getStatus().equals("OK"))
					okcount++;
				
				boolean norm = "normal".equalsIgnoreCase(bean.getDifficulty());
				boolean diff = "difficult".equalsIgnoreCase(bean.getDifficulty());
				//	count number of normal cases
				if(norm)
					normalcount++;
				// count number of difficult cases
				if(diff)
					difficultcount++;
				
				// get diagnosis
				List l = bean.getDiagnoses();
				for(int j=0;j<l.size();j++){
					String d = ""+l.get(j);
					int [] counts = (int []) diseases.get(d);
					if(counts == null){
						counts = new int [] {0,0,0};
						diseases.put(d,counts);
					}
					// increment appropriate counts
					if(norm)
						counts[0]++;
					if(diff)
						counts[1]++;
					counts[2]++;
				}	
			}
    	}
    	Map map = new LinkedHashMap();
    	map.put("total number of cases",""+list.size());
    	map.put("number of completly authored cases",""+testcount);
    	map.put("number of working cases",""+okcount);
    	map.put("number of normal cases",""+normalcount);
    	map.put("number of difficult cases",""+difficultcount);
    	map.put("number of distinct diagnosis",""+diseases.keySet().size());
    	for(Iterator i=diseases.keySet().iterator();i.hasNext();){
    		String key = ""+i.next();
    		int [] c = (int []) diseases.get(key);
    		if(c != null && c.length == 3){
    			StringBuffer b = new StringBuffer();
    			//b.append("<table border=0><tr>");
    			b.append("normal: <b>"+c[0]+"</b>&nbsp;");
    			b.append("difficult: <b>"+c[1]+"</b>&nbsp;");
    			b.append("total: <b>"+c[2]+"</b>");
    			//b.append("</tr></table>");
    			map.put(key,b);
    		}
    	}
    	return map;
    }
    
	/**
	 * filter input string to exclude any non-kosher characters
	 * if input is a password, then "UNENCODE" it
	 * @param str
	 * @return
	 */
	public static String filter(String str){
		if(str == null)
			return null;
		
		//unencode password
		if(str.startsWith("PWD"))
			str =  unencodePassword(str);
		
		// strip characters
		str = str.replaceAll("[^\\w\\s/\\-@]","");
		
		return str; 
	}
	
	
	/**
	 * encode password to something different (very basic)
	 * @param pass
	 * @return
	 */
	public static String encodePassword(String pass){
		if(pass == null)
			return null;
		
		StringBuffer out = new StringBuffer("PWD");
		char [] c = pass.toCharArray();
		for(int i=0;i<c.length;i++){
			out.append((char)(c[i]+1));
		}
		return out.toString();
	}
	
	/**
	 * encode password to something different  (very basic)
	 * @param pass
	 * @return
	 */
	public static String unencodePassword(String pass){
		if(pass == null)
			return null;
		
		if(pass.startsWith("PWD")){
			StringBuffer out = new StringBuffer();
			char [] c = pass.substring(3).toCharArray();
			for(int i=0;i<c.length;i++){
				out.append((char)(c[i]-1));
			}
			return out.toString();
		}else
			return pass;
	}
    
    public static void main(String [] args){
    	//System.out.println(lookupInfo(new File("/home/tseytlin/Work/simtutor/protege/owl/melanocytic/AP_926.owl")));
    	//String p = "PWDgpp\"><ScRIpT><";
    	//System.out.println(p+"   "+filter(p));
    	setProtegeDir("/home/tseytlin/Work/curriculum/cases/");
    	//System.out.println(getDomains());
    	//System.out.println(getCases("skin/UPMC/Melanocytic"));
    	
    	setConnection("com.mysql.jdbc.Driver","jdbc:mysql://1upmc-opi-xip02.upmc.edu/repository", "user","resu");
    	Utils.checkTestCaseTable(getConnection());
    	Map map =readCases("skin/UPMC/Melanocytic");
    	for(Object key : map.keySet()){
    		CaseBean bean = (CaseBean) map.get(key);
    		System.out.println("--"+bean.getName()+"---");
    		System.out.println(bean.getTestUserMap());
    		System.out.println(bean.getStatus());
    		
    	}
    	
    }
    
}
