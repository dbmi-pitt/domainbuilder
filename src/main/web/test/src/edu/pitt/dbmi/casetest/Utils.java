package edu.pitt.dbmi.casetest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Utils {
	private  static final int LARGE_VARCHAR = 2048;
	//types
	private static  String BOOL_TYPE   = "BOOLEAN";
	private static  String CHAR_TYPE   = "VARCHAR";
	private static  String INT_TYPE    = "INTEGER";
	private static  String FLOAT_TYPE  = "FLOAT";
	private static  String TEXT_TYPE   = "TEXT";
	private static  String TIMESTAMP   = "TIMESTAMP";
	private static  String AUTO_INCREMENT = "AUTO_INCREMENT";
	
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
			str = unencodePassword(str);
		
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
	/**
	 * This method gets a text file (HTML too) from input stream 
	 * reads it, puts it into string and subbstitutes keys for values in 
	 * from given map
	 * @param InputStream text input
	 * @param Map key/value substitution (used to substitute paths of images for example)
	 * @return String that was produced
	 * @throws IOException if something is wrong
	 * WARNING!!! if you use this to read HTML text and want to put it somewhere
	 * you should delete newlines
	 */
	public static String getText(InputStream in, Map sub) throws IOException {
		StringBuffer strBuf = new StringBuffer();
		BufferedReader buf = new BufferedReader(new InputStreamReader(in));
		try {
			for (String line = buf.readLine(); line != null; line = buf.readLine()) {
				strBuf.append(line.trim() + "\n");
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			buf.close();
		}
		// we have our text
		String text = strBuf.toString();
		// do substitution
		if (sub != null) {
			for (Iterator i = sub.keySet().iterator(); i.hasNext();) {
				String key = i.next().toString();
				text = text.replaceAll(key, "" + sub.get(key));
			}
		}
		return text;
	}
	
	/**
	 * Query servlet for object
	 * <servlet_url>?action=<action>
	 * @param URL servlet URL
	 * @param String type of action
	 * @return Object object returned by the servlet
	 */
	public static Object queryServlet(URL servlet, String action) {
		Object recievedObj = null;
		try {
			URL url = new URL(servlet.toString() + "?action=" + action);
			//System.out.println("Connection: "+url);
			URLConnection conn = url.openConnection();

			// Turn off caching
			conn.setUseCaches(false);

			//recieve object
			ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream());
			recievedObj = objIn.readObject();
			objIn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return recievedObj;
	}

	/**
	 * Query servlet for object
	 * <servlet_url>?action=<action>
	 * @param URL servlet URL
	 * @param String type of action
	 * @return Object object returned by the servlet
	 */
	public static String queryServlet(String param) {
		StringBuffer retStr = new StringBuffer();
		try {
			URL url = new URL(param);
			//System.out.println("Connection: "+url);
			URLConnection conn = url.openConnection();

			// Turn off caching
			conn.setUseCaches(false);

			// read output as string
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			for (String str = in.readLine(); str != null; str = in.readLine()) {
				retStr.append(str);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retStr.toString();
	}

	/**
	 * Send some object to a servlet
	 *@param URL servlet URL
	 * @param Object that is being sent
	 */
	public static void sendObject(URL servlet, Object obj) throws Exception {
		URLConnection conn = servlet.openConnection();
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		// send an object through serialization
		ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream());
		objOut.writeObject(obj);
		objOut.flush();
		objOut.close();

		// get a reply back (which is null of course)
		InputStream stream = conn.getInputStream();
	}
	

	/**
	 * get properties from string
	 * @param text
	 * @return
	 */
	public static Properties getProperties(String text){
		Properties p = new Properties();
		if(text != null){
			try{
				p.load(new ByteArrayInputStream(text.getBytes()));
			}catch(IOException ex){
				ex.printStackTrace();
			}
		}
		return p;
	}
	
	
	/**
	 * get character count
	 * @param s
	 * @param c
	 * @return
	 */
	public static int getCharCount(String s, char c){
		int n =0;
		for(int i=0;i<s.length();i++){
			if(s.charAt(i) == c)
				n ++;
		}
		return n;
	}
	
	
	/**
     * Get an object representing a set of users
     * that tested a set of components
     * @param domain
     * @param case_name
     * @return
     */
    public static void checkTestCaseTable(Connection conn){
    	// table definitions
		String SQL = 
			   "CREATE TABLE IF NOT EXISTS test_Case (" +
				"id "+INT_TYPE+" PRIMARY KEY NOT NULL "+AUTO_INCREMENT+", "+
				"problem   "+CHAR_TYPE+"(128), "+
				"user   "+CHAR_TYPE+"(128), "+
				"domain "+CHAR_TYPE+"(512), "+
				"status    "+CHAR_TYPE+"(64), "+
				"time      "+TIMESTAMP+")";
		Statement st = null;
		try {
			st = conn.createStatement();
			st.execute(SQL);
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if(st != null)
				try {
					st.close();
				} catch (SQLException e) {}
		}
	
    }
	
	
	/**
     * Get an object representing a set of users
     * that tested a set of components
     * @param domain
     * @param case_name
     * @return
     */
    public static Map<String,List<List<String>>> getTestUserMap(Connection conn, String domain, String case_name){
        String QUERY = 
            "SELECT user,status,time "+
            "FROM test_Case "+
            "WHERE problem=? AND domain=?";
        
        Map<String,List<List<String>>> map = null;
        PreparedStatement psmt=null;
        ResultSet result=null;
        try{
            // construct the prepared statement with the given username 
            psmt = conn.prepareStatement(QUERY);
            psmt.setString(1, case_name);
            psmt.setString(2, domain);
            result = psmt.executeQuery();
           
            // iterate over results
            while(result.next()){
                // create map
                if(map == null)
                    map = new HashMap<String,List<List<String>>>();
                
                // create list
                List<String> list = new ArrayList<String>();
                
                // get result
                String user = result.getString(1);
                String stat = result.getString(2);
                Timestamp t = result.getTimestamp(3);
                // convert to string
                String time = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,DateFormat.SHORT).format(t);
                
                // add to list
                list.add(stat);
                list.add(time);
                // indicator that entry is in DB already
                list.add(""+Boolean.TRUE); 
                
                // add to map
                if(!map.containsKey(user))
                    map.put(user,new ArrayList<List<String>>());
                map.get(user).add(list);
            }
        }catch(SQLException ex){
            ex.printStackTrace();
        }finally{
            try{
                if(psmt !=null)
                    psmt.close();
                if(result != null)
                    result.close();
            }catch(SQLException ex){
            }   
        }
        return map;
    }
    
    /**
     * Get an object representing a set of users
     * that tested a set of components
     * @param domain
     * @param case_name
     * @return
     */
    public static void saveTestUserMap(Connection conn, String domain,String case_name,Map map){
    	PreparedStatement psmt = null;;
        // NOW scan map and insert tutor_case_test_list entries
        for(Object key: map.keySet()){
        	String user = (String) key;
    
        	// get list of entries
            List list = (List) map.get(user);
            for(Object val: list){
                List cl = (List) val;
                // check if last entry in the list is Boolean
                // if it is, then this entry is already in the database
                // and we can skip it
                if(cl.size() >= 2 && !((""+Boolean.TRUE).equals(cl.get(cl.size()-1)))){
                    String status = (String) cl.get(0);
                    String time   = (String) cl.get(1);
                    
                    // convert to timestamp
                    DateFormat f = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
                    java.util.Date t = null;
                    try{
                        t = f.parse(time);
                    }catch(ParseException ex){};
                    Timestamp ts = new Timestamp(t.getTime());
                    
                    // now do an insert
                    // we need tutor case id first
                    String INSERT_TEST = 
                        "INSERT INTO test_Case (problem,user,domain,status,time) "+
                        "VALUES (?,?,?,?,?)";
                  
                    // get tutor_case id
                    try{
                        // construct the prepared statement with the given username 
                        psmt = conn.prepareStatement(INSERT_TEST);
                        psmt.setString(1,case_name);
                        psmt.setString(2,user);
                        psmt.setString(3,domain);
                        psmt.setString(4,status);
                        psmt.setTimestamp(5,ts);
                        psmt.executeUpdate();
                    }catch(SQLException ex){
                        ex.printStackTrace();
                    }finally{
                        try{
                            if(psmt !=null)
                                psmt.close();
                        }catch(SQLException ex){
                        }   
                    }       
                }   
            }
        }
    }
}
