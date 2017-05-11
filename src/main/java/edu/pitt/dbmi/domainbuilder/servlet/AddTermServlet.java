package edu.pitt.dbmi.domainbuilder.servlet;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;


public class AddTermServlet extends HttpServlet {
	private String curriculumDir;
	
	/**
	 *  Initializes the servlet.
	 */
	public void init( ServletConfig config ) throws ServletException {
		super.init(config);
		
		String dir = config.getInitParameter("curriculum.dir");
		if(dir != null)
			curriculumDir = dir;
	}

		
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
    
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
	
	private void processRequest(HttpServletRequest req,HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/plain");
		String response = "error";
		String term = req.getParameter("term");
		String url  = req.getParameter("url");
		if(term != null && url != null){
			if(addTerm(filter(term),filter(url)))
				response = "ok";
		}
		res.getWriter().write(response);
	}
	
	/**
	 * add term
	 * @param term
	 * @param url
	 */
	private boolean addTerm(String term, String url){
		return appendToFile(getTermFile(url),term+" | "+url);
	}


	/**
	 * append to File
	 * @param f
	 * @param string
	 */
	private boolean appendToFile(File f, String txt) {
		if(f == null)
			return false;
		try{
			FileWriter fw = new FileWriter(f,true);
			fw.write(txt+"\n");
			fw.close();
		}catch(Exception ex){
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * get term file 
	 * @param url
	 * @return
	 */
	private File getTermFile(String url) {
		String prefix = OntologyHelper.DEFAULT_HOST_URL+"/curriculum/";
		if(url.startsWith(prefix) && url.contains(".owl#")){
			int x = url.indexOf(".owl#");
			String file = url.substring(prefix.length(),x)+OntologyHelper.TERMS_SUFFIX;
			return new File(curriculumDir+File.separator+file.replace('/',File.separatorChar));
		}
		return null;
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
		return str.replaceAll("[^\\w\\s/\\-\\.&\\?:=@#]","").trim();
	}
}
