package edu.pitt.dbmi.domainbuilder.servlet;


import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Managers files for Domain Builder
 * @version 1.0
 */
public class FileManagerServlet extends HttpServlet {
	
	// this buffer stores output from CVS commands (should be synchronized)
	private Properties passwords  = new Properties();
	private Properties places  = new Properties();
	private SVNHandler svn;
	
	// map of all roots
	private Map<String,String> roots;
	private Map<String,String> caseListCache;
	
	
	/**
	 *  Initializes the servlet.
	 */
	public void init( ServletConfig config ) throws ServletException {
		super.init( config );
		
		// initialize SVN handler
		svn = new SVNHandler();
		
		// initialize CACHE for case list
		caseListCache = new HashMap<String, String>();
				
		// load init parameter with roots we care to list
		roots = new LinkedHashMap<String, String>();
		for(Enumeration<String> e = config.getInitParameterNames();e.hasMoreElements();){
			String param = e.nextElement();
			if(param.endsWith(".dir")){
				String name = param.substring(0,param.length()-4);
				roots.put(name,config.getInitParameter(param));
			}else if(param.equals("svn.user")){
				svn.setUsername(config.getInitParameter(param));
			}else if(param.equals("svn.pass")){
				svn.setPassword(config.getInitParameter(param));
			}else if(param.equals("password.file")){
				try{
					//passwords.load(new FileInputStream(config.getInitParameter(param)));
					readPasswordFile(config.getInitParameter(param));
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}else if(param.equals("svn.command")){
				svn.setSVNProgramName(config.getInitParameter(param));
			}
		}
	}

	/**
	 * read password file
	 * @param file
	 * @throws Exception
	 */
	private void readPasswordFile(String file) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		for(String line = reader.readLine();line != null; line = reader.readLine()){
			line = line.trim();
			String [] p = line.split("\\|");
			if(p.length == 3){
				passwords.setProperty(p[0].trim(),p[1].trim());
				places.setProperty(p[0].trim(),p[2].trim());
			}
		}
		reader.close();
	}
	
	
	/**
	 * used to load an existing project
	 * @param req
	 * @param res
	 * @throws IOException
	 */
	public void doGet( HttpServletRequest req, HttpServletResponse res ) throws IOException {
		res.setContentType("text/plain");
		
		// get action
		String response = "error";
		String action = ""+req.getParameter( "action" );
		if( action.equals( "list" ) ) {
			String path = req.getParameter("path");
			String root = req.getParameter("root");
			String recurse = req.getParameter("recurse");
			if(root != null && roots.containsKey(root)){
				String file = roots.get(root)+"/"+filter(path);
				response = (Boolean.valueOf(recurse))?listRecursive(file,""):list(file);
			}
		}else if( action.equals( "list-cases" ) ) {
			String path = req.getParameter("path");
			String root = req.getParameter("root");
			String props = req.getParameter("properties");
			if(root != null && roots.containsKey(root)){
				String file = roots.get(root)+"/"+filter(path);
				// check cache first
				if(caseListCache.containsKey(file+"-"+props)){
					response = caseListCache.get(file+"-"+props);
				}else{
					response = listCases(file,"",props);
					caseListCache.put(file+"-"+props,response);
				}
			}
		}else if( action.equals( "update" ) ) {
			//String path = req.getParameter("path");
			String root = req.getParameter("root");
			if(root != null && roots.containsKey(root)){
				//String file = roots.get(root)+"/"+filter(path);
				svn.setWorkingDirectory(roots.get(root));
				response = svn.update();
			}
		}else if( action.equals( "status" ) ) {
			String root = req.getParameter("root");
			if(root != null && roots.containsKey(root)){
				svn.setWorkingDirectory(roots.get(root));
				response = svn.status();
			}
		}else if( action.equals( "commit" ) ) {
			String root = req.getParameter("root");
			if(root != null && roots.containsKey(root)){
				svn.setWorkingDirectory(roots.get(root));
				response = svn.doCommit();
			}
		}
		res.getWriter().write(response);
	}

	/** Manages the client requests.
	* @param req servlet request
	* @param res servlet response
	*/
	public void doPost( HttpServletRequest req, HttpServletResponse res ) throws IOException {
		Object obj = null;
		try {
			ObjectInputStream objIn = new ObjectInputStream( req.getInputStream() );
			obj = objIn.readObject();
			objIn.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		catch ( ClassNotFoundException e ) {
			e.printStackTrace();
		}
			
		res.setContentType("text/plain");
		
		// get action 
		String response = "Error: expecting a Map object";
		
		
		// if we get a specially formated map, then we are in business
		if ( obj instanceof Map ){
			Map map = (Map) obj;
			String action = ""+map.get("action");
			
			if(action.equals("null")){
				response = "Error: action not specified in a map";
			}else if(action.equals("upload")){
				String path = ""+map.get("path");
				String root = ""+map.get("root");
				String user = ""+map.get("user");
				String pass = ""+map.get("pass");
				if(authenticate(user, pass)){
					if(root != null && roots.containsKey(root)){
						// authenticate
						String file = roots.get(root)+"/"+path;
						if(authenticateLocation(user,file)){
							response = file;
							try{
								byte [] data = (byte []) map.get("data");
								// upload file
								upload(new ByteArrayInputStream(data),file);
								
								// commit file to svn
								// set working directory
								svn.setWorkingDirectory(roots.get(root));
								String s = svn.doCommit(path);
								
								//System.err.println(s);
								response = "ok\n"+s;
							}catch(IOException ex){
								ex.printStackTrace(res.getWriter());
							}
						}else{
							response = "Error: user "+user+" is not allowed to upload "+file;
						}
					}else{
						response = "Error: unkown root "+root;
					}
				}else
					response = "Error: authentication failed";
			}
			/*
			else if(action.equals("delete")){
				String path = ""+map.get("path");
				String root = ""+map.get("root");
				String user = ""+map.get("user");
				String pass = ""+map.get("pass");
				if(authenticate(user, pass)){
					if(root != null && roots.containsKey(root)){
						String file = roots.get(root)+"/"+path;
						response = file;
						
						File f = new File(file);
						if(f.exists())
							f.delete();
						
						// commit file to svn
						//String s = commit(file);
						//System.err.println(s);
						
						response = "ok";
						
					}else{
						response = "Error: unkown root "+root;
					}
				}else
					response = "Error: authentication failed";
			}
			*/
			else if(action.equals("authenticate")){
				String user = ""+map.get("user");
				String pass = ""+map.get("pass");
				String place = ""+map.get("place");
				response = (authenticate(user,pass,place))?"ok":"failed";
			}
		}
		res.getWriter().write(response);
	}

	
	/**
	 * 
	 * @param is
	 * @param filename
	 * @throws Exception
	 */
	private void upload(InputStream in, String filename) throws IOException{
		OutputStream out = null;
		try{
			File file = new File(filename);
			if(!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			out = new FileOutputStream(file);
		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0){
		    	out.write(buf,0,len);
		    }
		}catch(IOException ex){
			throw ex;
		}finally{
			if(out != null)
				out.close();
		}
	}
	
	/**
	 * list content of director
	 * @param filename
	 * @return
	 */
	private String list(String filename){
		File file = new File(filename);
		if(file.isDirectory()){
			StringBuffer buffer = new StringBuffer();
			for(File f: file.listFiles()){
				if(!f.isHidden() && !f.getName().startsWith("."))
					buffer.append(f.getName()+((f.isDirectory())?"/":"")+"\n");
			}
			return buffer.toString();
		}
		return "error";
	}
	
	/**
	 * list content of director
	 * @param filename
	 * @return
	 */
	private String listRecursive(String filename, String prefix){
		File file = new File(filename);
		if(file.isDirectory()){
			StringBuffer buffer = new StringBuffer();
			for(File f: file.listFiles()){
				if(!f.isHidden() && !f.getName().startsWith(".")){
					if(f.isDirectory()){
						buffer.append(listRecursive(f.getAbsolutePath(),prefix+f.getName()+"/"));
					}else
						buffer.append(prefix+f.getName()+"\n");
				}
			}
			return buffer.toString();
		}
		return "error";
	}
	
	
	/**
	 * list content of director
	 * @param filename
	 * @return
	 */
	private String listCases(String filename, String prefix, String tags){
		File file = new File(filename);
		if(file.isDirectory()){
			StringBuffer buffer = new StringBuffer();
			for(File f: file.listFiles()){
				if(!f.isHidden() && !f.getName().startsWith(".")){
					if(f.isDirectory()){
						buffer.append(listCases(f.getAbsolutePath(),prefix+f.getName()+"/",tags));
					}else if(f.getName().toLowerCase().endsWith(".case")){
						buffer.append(prefix+f.getName()+getCaseMetaData(f,tags)+"\n");
					}
				}
			}
			return buffer.toString();
		}
		return "error";
	}
	
	/**
	 * get metadata of a given case
	 * @param f
	 * @param tags
	 * @return
	 */
	private String getCaseMetaData(File f, String tags){
		if(tags == null || tags.trim().length() == 0)
			return "";
		
		// get list of properties
		String [] props = tags.split(",");
		
		// read header
		try{ 
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line,field = null;
	    	StringBuffer buffer = new StringBuffer();
	    	Pattern pt = Pattern.compile("\\[([\\w\\.\\-]+)\\]");
	    	while((line = reader.readLine()) != null){
	    		line = line.trim();
	    		// skip comments
	    		if(line.startsWith("#"))
	    			continue;
	    		// extract headers
	    		Matcher mt = pt.matcher(line);
	    		if(mt.matches()){
	    			field = mt.group(1);
	    			
	    			// if field is NOT case, then we are done
	    			if(!"case".equalsIgnoreCase(field)){
	    				break;
	    			}
	    		}else{
	    			buffer.append(line+"\n");
	    		}
	    	}
			// close reader
	    	reader.close();
	    	
	    	// extract properties
	    	Properties meta = new Properties();
	    	meta.load(new ByteArrayInputStream(buffer.toString().getBytes()));
	    	
	    	// now select properties that fit the criteria
	    	StringBuffer opts = new StringBuffer();
	    	for(String key: props){
	    		key = key.trim();
	    		if(meta.containsKey(key)){
		    		String pref = (opts.length() == 0)?"?":"&";
		    		opts.append(pref+key+"="+meta.getProperty(key));
	    		}
	    	}
	    	
	    	return opts.toString(); 	
		}catch(Exception ex){
			//ex.printStackTrace();
		}
		return "";
	}
	
	
	/**
	 * filter input string to exclude any non-kosher characters
	 * if input is a password, then "UNENCODE" it
	 * @param str
	 * @return
	 */
	private String filter(String str){
		if(str == null)
			return "";
		
		// strip characters
		return str.replaceAll("[^\\w\\s/\\-@]","");
	}
	
	/**
	 * authenticate user and password
	 * @param user
	 * @param pass
	 * @return
	 */
	private boolean authenticate(String user, String pass){
		return authenticate(user, pass, null);
	}
	
	/**
	 * authenticate user and password
	 * @param user
	 * @param pass
	 * @return
	 */
	private boolean authenticate(String user, String pass,String place){
		String spass = ""+passwords.getProperty(user,"");
		
		// if from different place
		if(place != null && !places.getProperty(user,"").contains(place))
			return false;
		
		return pass.equals(spass.trim());
	}
	
	/**
	 * authenticate user and password
	 * @param user
	 * @param pass
	 * @return
	 */
	private boolean authenticateLocation(String user, String file){
		String place = ""+places.getProperty(user);
		for(String p: place.split(",")){
			if(file.contains("/"+p.trim()+"/")){
				return true;
			}
		}
		return false;
		//return file.contains("/"+place+"/");
	}
	
	/*
	public static void main(String [] args ){
		FileManagerServlet m = new FileManagerServlet();
		long time = System.currentTimeMillis();
		System.out.println(m.listCases("/home/tseytlin/Work/curriculum/cases/","","status,question.type"));
		System.out.println(System.currentTimeMillis()-time);
	}
	*/
}

