package edu.pitt.dbmi.casetest;

import javax.servlet.http.*;
import javax.servlet.*;
import javax.swing.Timer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.Connection;
import java.text.DateFormat;
import java.util.*;



public class CaseTestServlet extends HttpServlet {
    private final String CASE_TEST = "/CaseTest";
    private final String CASE_SUMMARY = "/CaseSummary";
    private Timer timer;
    
    /**
     * Initializes the servlet and starts an engine for KB.
     */
    public void init(ServletConfig conf) throws ServletException {
        super.init( conf );
        // configure database
        String dbDriver = conf.getInitParameter( "db.driver" );
        String dbURL    = conf.getInitParameter( "db.url" );
        String dbUser   = conf.getInitParameter( "db.user" );
        String dbPassword = conf.getInitParameter("db.passwd" );
        
        // get instance of DB tool
        CaseTestHelper.setConnection(dbDriver,dbURL,dbUser,dbPassword);
        // init directory
        String protegeDir = conf.getInitParameter("project.dir");
        CaseTestHelper.setProtegeDir(protegeDir);
        
        
        // init DB connect close periodicly
        // initialize trash compactor
		timer = new Timer(60*60*1000,new ActionListener(){
			public void actionPerformed(ActionEvent e) {
			 	CaseTestHelper.closeConnection();
			}
		});
		timer.setRepeats(true);
		timer.start();
        
    }

    
    
    protected void finalize() throws Throwable {
    	try {
    		timer.stop();
          	CaseTestHelper.closeConnection();
        } finally {
            super.finalize();
        }
    }

    
    
    //  do get
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        //System.err.println("Processing GET request");
        processRequest(req, res);
    }
    // do post
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        //System.err.println("Processing POST request");
        processRequest(req, res);
    }
    
    /**
     * This is used for normal get requests s.a.  answer submits and authentication.
     */ 
    private void processRequest(HttpServletRequest req,HttpServletResponse res)
        throws ServletException, IOException {

        String dispatch = "";
        String action = CaseTestHelper.filter(req.getParameter("action"));
        Object tosend = null;
        RequestDispatcher dispatcher;
        String serverURL = "http://"+req.getServerName()+":"+req.getServerPort()+
                            req.getContextPath()+"/";
        
        // perform appropriate action       
        if (action == null) {
            // if action is not defined then we prcocess it as an object
            // don't do anything (no response)
            res.setContentType("text/html;");
            res.getWriter().println("<center><b>Error:</b> No action given</center>");
            res.getWriter().close(); 
            dispatch = null; tosend = null;
        } else if (action.equals("get_case_list")) {
            String domain = CaseTestHelper.filter(req.getParameter("domain"));
            String sort   = CaseTestHelper.filter(req.getParameter("sort"));
            List list = CaseTestHelper.getCases(domain);
            Comparator c;
            if(sort == null)
                sort = "name";
            // chose comparator
            if(sort.equals("status"))
                c = new CaseBean.StatusComparator();
            else if(sort.equals("pattern"))
                c = new CaseBean.PatternComparator();
            else if(sort.equals("diagnosis"))
                c = new CaseBean.DiagnosisComparator();
            else if(sort.equals("difficulty"))
                c = new CaseBean.DifficultyComparator();
            else 
                c = new CaseBean.NameComparator();
            Collections.sort(list,c);
            tosend = CaseTestHelper.createCaseMap(list,50);
        } else if(action.equals("get_domain_list")){
            List list = CaseTestHelper.getDomains();
            tosend = list;
        } else if(action.equals("sort")){
            String meth = CaseTestHelper.filter(req.getParameter("sort"));
            String user = CaseTestHelper.filter(req.getParameter("user"));
            String pass = CaseTestHelper.filter(req.getParameter("pass"));
            String domain   = CaseTestHelper.filter(req.getParameter("domain"));
            String condition= CaseTestHelper.filter(req.getParameter("condition"));
            String page= CaseTestHelper.filter(req.getParameter("page"));
            dispatch = CASE_TEST;
            Map map = new HashMap();
            map.put("username",user);
            map.put("password",pass);
            map.put("domain",domain);
            map.put("condition",condition);
            map.put("sort",meth);
            map.put("page",page);
            req.setAttribute("info", map);
        } else if(action.equals("summary")){
            String domain   = CaseTestHelper.filter(req.getParameter("domain"));
            dispatch = CASE_SUMMARY;
            List list = CaseTestHelper.getCases(domain);
            Map map = CaseTestHelper.createSummaryMap(list);
            req.setAttribute("summary", map);
        } else if (action.equals("login")) {
            //String session =  req.getParameter("session");
            String username = CaseTestHelper.filter(req.getParameter("username"));
            String password = CaseTestHelper.filter(req.getParameter("password"));
            String domain   = CaseTestHelper.filter(req.getParameter("domain"));
            String condition= CaseTestHelper.filter(req.getParameter("condition"));
            
            if(CaseTestHelper.isAuthenticated(username,password)){
                dispatch = CASE_TEST;
                Map map = new HashMap();
                map.put("username",username);
                map.put("password",password);
                map.put("domain",domain);
                map.put("condition",condition);
                req.setAttribute("info", map);
            } else {
                res.setContentType("html/text;");
                res.getWriter().println("Authentication error");
                res.getWriter().close(); 
                dispatch = null; tosend = null;    
            }
        } else if (action.equals("refresh")) {
            String domain = CaseTestHelper.filter(req.getParameter("domain"));
            String user = CaseTestHelper.filter(req.getParameter("user"));
            String pass = CaseTestHelper.filter(req.getParameter("pass"));
            String condition= CaseTestHelper.filter(req.getParameter("condition"));
            String page= CaseTestHelper.filter(req.getParameter("page"));
            CaseTestHelper.refresh();
            dispatch = CASE_TEST;
            Map map = new HashMap();
            map.put("username",user);
            map.put("password",pass);
            map.put("domain",domain);
            map.put("condition",condition);
            map.put("page",page);
            req.setAttribute("info", map);
        }else if(action.equals("launch_jnlp")){
            String app    = CaseTestHelper.filter(req.getParameter("jnlp"));
            String problem   = CaseTestHelper.filter(req.getParameter("problem"));
            String domain = CaseTestHelper.filter(req.getParameter("domain"));
            String condition = CaseTestHelper.filter(req.getParameter("condition"));
            String user = CaseTestHelper.filter(req.getParameter("user"));
            String pass = CaseTestHelper.filter(req.getParameter("pass"));
            String type = CaseTestHelper.filter(req.getParameter("type"));
            String session = CaseTestHelper.filter(req.getParameter("session"));
            // create a map 
            Map map = new HashMap();
            map.put(CaseTestHelper.CASE,problem);
            map.put(CaseTestHelper.SERVER,serverURL);
            if(domain != null)
                map.put(CaseTestHelper.DOMAIN,domain);
            if(condition != null)
                map.put(CaseTestHelper.CONDITION,condition);
            if(user != null)
                map.put(CaseTestHelper.USER,user);
            if(pass != null)
                map.put(CaseTestHelper.PASS,pass);
            if(type != null)
                map.put(CaseTestHelper.TYPE,type);
            if(session != null)
            	map.put(CaseTestHelper.SESSION,session);
            res.setContentType("application/x-java-jnlp-file;");
            res.getWriter().println(CaseTestHelper.getWebLuncher(app+".jnlp",map));
            res.getWriter().close(); 
            dispatch = null; tosend = null;   
        }else if(action.equals("tested")){
            //String  comp   = CaseTestHelper.filter(req.getParameter("component"));
            String  status  = CaseTestHelper.filter(req.getParameter("status"));
            String  user    = CaseTestHelper.filter(req.getParameter("user"));
            String  pass    = CaseTestHelper.filter(req.getParameter("pass"));
            String  caseName = CaseTestHelper.filter(req.getParameter("case"));
            String  domain  = CaseTestHelper.filter(req.getParameter("domain"));
            String condition= CaseTestHelper.filter(req.getParameter("condition"));
            String page = CaseTestHelper.filter(req.getParameter("page"));
            
            // build component
            List list = new ArrayList();
            list.add(status);
            Date dt = new Date(System.currentTimeMillis());
            list.add(DateFormat.getDateTimeInstance(
                     DateFormat.SHORT,DateFormat.SHORT).format(dt));
            
            // change case
            CaseBean cb = CaseTestHelper.getCase(domain,caseName);
            if(cb != null){
                cb.addTestedComponent(user,list);
                CaseTestHelper.saveCase(cb);
            }
            
            // forward back
            dispatch = CASE_TEST;
            Map map = new HashMap();
            map.put("username",user);
            map.put("password",pass);
            map.put("domain",domain);
            map.put("condition",condition);
            map.put("page",page);
            req.setAttribute("info", map);
        }
        
        
        //  Do a dispatch if possible, otherwise stream the object back (if possible)
        if(dispatch != null && dispatch.length() > 0){  
            dispatcher = req.getRequestDispatcher(dispatch);
            dispatcher.forward(req, res);
        }else if(tosend !=null){
            // output serialized object
            ObjectOutputStream out = new ObjectOutputStream(res.getOutputStream());
            out.writeObject(tosend);
            out.flush();
            out.close();
            tosend = null;
        }
    }
}
