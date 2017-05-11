package edu.pitt.dbmi.domainbuilder.servlet;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * handles svn operations
 * @author tseytlin
 *
 */

public class SVNHandler {
	private String svnUser, svnPass,svn;
	private String workingDirectory;

	
	/**
	 * initialize SVN handler w/ default values
	 */
	public SVNHandler(){
		// setup defaults
		svn = (isWindows())?"svn.exe":"svn";
		svnUser = svnPass = "";
		setWorkingDirectory(System.getProperty("user.dir"));
		//setUsername("1upmc-opi-cvs01/cvs");
		//setPassword("cvs123");
	}
	
	/**
	 * is this running on windows
	 * @return
	 */
	private boolean isWindows(){
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}
	
	/**
	 * set SVN username
	 * @param u
	 */
	public void setUsername(String u){
		svnUser = "--username="+u;
	}
	
	/**
	 * set SVN username
	 * @param u
	 */
	public void setPassword(String p){
		svnPass= "--password="+p;
	}
	
	
	/**
	 * set working directory
	 * @param s
	 */
	public void setWorkingDirectory(String s){
		workingDirectory = s;
	}
	
	/**
	 * set the name of the SVN program
	 * (use this to specify full path or if svn is called something else)
	 * @param s
	 */
	public void setSVNProgramName(String s){
		svn = s;
	}
	
	/**
	 * trim list of arguments
	 * @param args
	 * @return
	 */
	private String [] trim(String [] args){
		List<String> list = new ArrayList<String>();
		for(String a: args){
			if(a != null && a.length() > 0)
				list.add(a);
		}
		return list.toArray(new String [0]);
	}
	
	
	/**
	 * execute a program w/ arguments 
	 * @param args
	 * @return output of this program
	 */
	private String execute(String ... args){
		// trim list of arguments
		//System.err.println(Arrays.toString(trim(args)));
		
		// create a process builder
		ProcessBuilder pb = new ProcessBuilder(trim(args));
		pb.directory(new File(workingDirectory));
		pb.redirectErrorStream(true);
		
		// start a process
		try{
			Process process = pb.start();
			
			// catch stdout/stderr
			StreamGobbler out = new StreamGobbler(process.getInputStream());
			
			// start output catchers
			out.start();
	
			// wait for process to end
			process.waitFor();
			
			// wait for outputs to catch up
			while(out.isAlive()){
				Thread.sleep(20);
			}
			
			// now return output
			return out.getOutput();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return "";
	}
	
	
	/**
	 * update svn in current working directory
	 * @param filename
	 * @return
	 */
	public String update(){
		return execute(svn,"update",svnUser,svnPass,"--depth=infinity","--non-interactive","--accept=theirs-full");
	}
	
	
	/**
	 * update svn
	 * @param filename relative to working dir
	 * @return
	 */
	public String update(String filename){
		return execute(svn,"update",svnUser,svnPass,"--depth=infinity","--non-interactive","--accept=theirs-full",filename);
	}
	
	/**
	 * cleanup svn (remove lock files)
	 * @param filename
	 * @return
	 */
	public String cleanup(){
		return execute(svn,"cleanup",svnUser,svnPass,"--non-interactive");
	}
	
	/**
	 * get status from working directories
	 * @param filename
	 * @return
	 */
	public String status(String filename){
		return execute(svn,"status",svnUser,svnPass,"--non-interactive",filename);
	}
	
	/**
	 * get status from working directory
	 * @param filename
	 * @return
	 */
	public String status(){
		return execute(svn,"status",svnUser,svnPass,"--non-interactive");
	}
	
	
	/**
	 * commit all of changes
	 * @return
	 */
	public String commit(){
		String msg = "--message="+svnUser+" commited on "+(new Date());
		
		// on windows spaces in message mess things up
		msg = msg.replaceAll(" ",".");
		
		return execute(svn,"commit",svnUser,svnPass,msg,"--non-interactive");
	}
	
	/**
	 * commit changes to a file
	 * @return
	 */
	public String commit(String file){
		String msg = "--message="+svnUser+" commited on "+(new Date());
		
		// on windows spaces in message mess things up
		msg = msg.replaceAll(" ",".");
		
		return execute(svn,"commit",svnUser,svnPass,msg,"--non-interactive",file);
	}
	
	
	/**
	 * update svn
	 * @param filename
	 * @return
	 */
	public String add(String filename){
		return execute(svn,"add",svnUser,svnPass,"--parents","--non-interactive",filename);
	}
	
	
	/**
	 * commit the removal of a file
	 * @param filename
	 * @return
	 */
	public String remove(String filename){
		return execute(svn,"delete",svnUser,svnPass,"--non-interactive",filename);
	}
	
	/**
	 * is copy locked
	 * @param str
	 * @return
	 */
	private boolean isLocked(String str){
		return str.contains("locked");
	}
	
	/**
	 * determine if the file is new, based on the output of status command
	 * @param str
	 * @return
	 */
	private List<String> getNewItems(String str){
		Set<String> files = new HashSet<String>();
		for(String s: str.split("\n")){
			if(s.startsWith("?")){
				files.add(s.substring(1).trim());
			}
		}
		return new ArrayList<String>(files);
	}
	
	/**
	 * get new items
	 * @param str
	 * @return
	 */
	private boolean isNewItem(String str){
		// if parents are A OK
		if(str.startsWith("?"))
			return true; 
		// if one of the parents is missing
		if(str.contains("not a working copy"))
			return true;
		return false;
	}
	
	
	/**
	 * updates the repository, then
	 * run commit of all modified files
	 * this operation will add all of the files that
	 * are not versioned
	 * @param filename
	 */
	public String doCommit(){
		StringBuffer out = new StringBuffer();
		out.append("cd "+workingDirectory+"\n");
		
		// lets do an update first
		String str = update();
		out.append("svn update\n"+str+"\n");
		
		// unlock it if locked
		if(isLocked(str)){
			str = cleanup();
			out.append("svn clean\n"+str+"\n");
			
			// and update again
			str = update();
			out.append("svn update\n"+str+"\n");
		}
		
		// run global status to add new files
		// then commit everything in one swoop
		int attempt = 3;
		List<String> newFiles = null;
		do {
			// get status
			str = status();
			out.append("svn status\n"+str+"\n");
			
			// get list of new items			
			newFiles = getNewItems(str);
			
			// add each new item
			for(String newFile : newFiles){
				str = add(newFile);
				out.append("svn add "+newFile+"\n"+str+"\n");
			}
			
			// commit all modified files
			str = commit();
			out.append("svn commit\n"+str+"\n");
			
			// don't do it infinetley
			attempt --;
		}while(!newFiles.isEmpty() && attempt > 0);
				
		// return output
		return out.toString();
	}
	
	/**
	 * updates the repository, then
	 * run commit of a given file
	 * this operation will add all of the files that
	 * are not versioned
	 * @param filename
	 */
	public String doCommit(String filename){
		StringBuffer out = new StringBuffer();
		out.append("cd "+workingDirectory+"\n");
		
		// lets do an update first
		String str = update(filename);
		out.append("svn update "+filename+"\n"+str+"\n");
		
		// unlock it if locked
		if(isLocked(str)){
			str = cleanup();
			out.append("svn clean\n"+str+"\n");
			
			// and update again
			str = update(filename);
			out.append("svn update "+filename+"\n"+str+"\n");
		}
		
		// get status
		str = status(filename);
		out.append("svn status "+filename+"\n"+str+"\n");
		
		// if new file add it
		if(isNewItem(str)){
			str = add(filename);
			out.append("svn add "+filename+"\n"+str+"\n");
			
			// commit first entry in ADD queue
			if(str.length() > 0 && str.startsWith("A ")){
				filename = str.split("\n")[0].substring(2).trim();
				// fix output of a binary file add
				if(filename.startsWith("(bin)"))
					filename = filename.substring(5).trim();
			}
		}
		
		// commit modified file
		str = commit(filename);
		out.append("svn commit "+filename+"\n"+str+"\n");
			
		
		// return output
		return out.toString();
	}
	
	
	/**
	 * This class reads stdout/stderr from the process
	 * it is taken from JavaWorld article:
	 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps_p.html
	 */
	private class StreamGobbler extends Thread {
		private InputStream is;
		private StringBuffer output;
		
		public StreamGobbler( InputStream is ) {
			this.is = is;
			output = new StringBuffer();
		}
		
		public void run() {
			try	{
				BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
				String line = null;
				while ( ( line = br.readLine() ) != null ) {
					//synchronized ( output ) {
					output.append( line + "\n" );
					//}
				}
				br.close();
			} catch ( IOException ioe ) {
				ioe.printStackTrace();
			}
		}
		
		/**
		 * get output
		 * @return
		 */
		public String getOutput(){
			return output.toString();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//do update on curriculum
		String file = "/home/tseytlin/Work/curriculum";
		
		SVNHandler svn = new SVNHandler();
		svn.setWorkingDirectory(file);
		System.out.println(svn.doCommit(file+"/test/test2/test3/test4.txt"));
		//System.out.println(out);
		

	}

}
