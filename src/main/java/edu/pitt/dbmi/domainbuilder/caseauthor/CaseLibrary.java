package edu.pitt.dbmi.domainbuilder.caseauthor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import edu.pitt.dbmi.domainbuilder.beans.*;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IOntology;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;

public class CaseLibrary {
	private final int LARGE_VARCHAR = 2048;
	//types
	private String prefix = "tutor_";
	private String BOOL_TYPE   = "BOOLEAN";
	private String CHAR_TYPE   = "VARCHAR";
	private String INT_TYPE    = "INTEGER";
	private String FLOAT_TYPE  = "FLOAT";
	private String TEXT_TYPE   = "TEXT";
	private String AUTO_INCREMENT = "AUTO_INCREMENT";
	
	// fields
	private Connection conn;
	private Properties props;
	private boolean databaseMode;
	/**
	 * init case library
	 * @param p
	 */
	public CaseLibrary(Properties p) throws Exception{
		this.props = p;
		databaseMode = p.containsKey("repository.driver");
	
		// create tables if not exist (first time)
		if(databaseMode)
			createSQLTables();
	}
	
	/**
	 * create SQL tables if not exist
	 */
	private void createSQLTables() throws Exception{
		// table definitions
		String CASE_SQL = 
			   "CREATE TABLE IF NOT EXISTS "+prefix+"Case (" +
				"case_id   "+INT_TYPE+" PRIMARY KEY NOT NULL "+AUTO_INCREMENT+", "+
				"name      "+CHAR_TYPE+"(128) UNIQUE NOT NULL, "+
				"source    "+CHAR_TYPE+"(512), "+
				"organ     "+CHAR_TYPE+"(128), "+
				"diagnosis "+CHAR_TYPE+"(512), "+
				"report    "+TEXT_TYPE+", "+
				"notes     "+TEXT_TYPE+")";
		String IMAGE_SQL = 
			   "CREATE TABLE IF NOT EXISTS "+prefix+"Image (" +
				"image_id  "+INT_TYPE+" PRIMARY KEY NOT NULL "+AUTO_INCREMENT+", "+
				"name      "+CHAR_TYPE+"(128) UNIQUE NOT NULL,"+
				"server    "+CHAR_TYPE+"(256), "+
				"path      "+CHAR_TYPE+"(512), "+
				"type      "+CHAR_TYPE+"(64), "+
				"block     "+CHAR_TYPE+"(32), "+
				"stain     "+CHAR_TYPE+"(128), "+
				"part      "+CHAR_TYPE+"(32), "+
				"level     "+CHAR_TYPE+"(32), "+
				"case_id   "+INT_TYPE+" REFERENCES "+prefix+"Case)";
		String  ANNOTATION_SQL = 
				"CREATE TABLE IF NOT EXISTS "+prefix+"Annotation ("+
			    "annotation_id "+INT_TYPE+" PRIMARY KEY NOT NULL "+AUTO_INCREMENT+", "+
			    "name     "+CHAR_TYPE+"(64) NOT NULL,"+
			    "tag      "+CHAR_TYPE+"("+LARGE_VARCHAR+"), "+
			    "type     "+CHAR_TYPE+"(32), "+
			    "color    "+CHAR_TYPE+"(32), "+
			    "image    "+CHAR_TYPE+"(64), "+
			    "viewx    "+INT_TYPE+", "+
			    "viewy    "+INT_TYPE+", "+
			    "zoom     "+FLOAT_TYPE+", "+
			    "xstart   "+INT_TYPE+", "+
			    "ystart   "+INT_TYPE+", "+
			    "xend     "+INT_TYPE+", "+
			    "yend     "+INT_TYPE+", "+
			    "width    "+INT_TYPE+", "+
			    "height   "+INT_TYPE+", "+
			    "xpoints  "+TEXT_TYPE+", "+
			    "ypoints  "+TEXT_TYPE+", "+
			    "image_id "+INT_TYPE+" REFERENCES "+prefix+"Image)";
		
		String FINDING_SQL = 
				"CREATE TABLE IF NOT EXISTS "+prefix+"Finding (" +
				"finding_id "+INT_TYPE+" PRIMARY KEY NOT NULL "+AUTO_INCREMENT+", "+
				"name "+CHAR_TYPE+"(512) NOT NULL, "+
				"domain "+CHAR_TYPE+"(256) , "+
				"asserted "+BOOL_TYPE+", "+
				"examples  "+CHAR_TYPE+"("+LARGE_VARCHAR+"), "+
				"locations "+CHAR_TYPE+"("+LARGE_VARCHAR+"), "+
				"case_id   "+INT_TYPE+" REFERENCES "+prefix+"Case)";
		
		
		//System.out.println(CASE_SQL);
		//System.out.println(IMAGE_SQL);
		//System.out.println(ANNOTATION_SQL);
		// now insert tables
		Connection conn = getConnection();	
		Statement st = conn.createStatement();
		st.execute(CASE_SQL);
		st.close();
		st = conn.createStatement();
		st.execute(IMAGE_SQL);
		st.close();
		st = conn.createStatement();
		st.execute(ANNOTATION_SQL);
		st.close();
		st = conn.createStatement();
		st.execute(FINDING_SQL);
		st.close();
	}
	
	protected void finalize() throws Exception {
		dispose();
	}
	
	/**
	 * dispose of resource
	 */
	public void dispose(){
		try{
			if(conn != null)
				conn.close();
			conn = null;
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * get sql connection
	 * @return
	 */
	private Connection getConnection() throws Exception{
		if(conn == null){
			String driver = props.getProperty("repository.driver");
			String url    = props.getProperty("repository.url");
			String user   = props.getProperty("repository.username");
			String pass   = props.getProperty("repository.password");
			Class.forName(driver).newInstance();
		    conn = DriverManager.getConnection(url,user,pass);
		}
		return conn;
	}
	
	/**
	 * get case it return -1 if not exists
	 * @param conn
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private int getEntryId(Connection conn,String table, String name) throws SQLException{
		int id = -1;
		PreparedStatement st = null;
		ResultSet result = null;
		try{
			// check if such case exists
			st = conn.prepareStatement("SELECT "+table.toLowerCase()+"_id FROM "+prefix+table+" WHERE name = ?");
			st.setString(1,name);
			result = st.executeQuery();
			if(result.next())
				id = result.getInt(1);
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return id;
	}
	
	
	/**
	 * save case entry in database
	 * @param entry
	 */
	public void saveCaseEntry(CaseEntry entry) throws Exception{
		if(databaseMode){
			Connection conn = getConnection();
			// check if such case exists
			int case_id = insertSQLCase(conn,entry);
			
			if(case_id < 0)
				return;
			
			// insert/update slides
			for(SlideEntry slide: entry.getSlides()){
				int slide_id = insertSQLImage(conn, slide, case_id);
				
				// now take care of annotations
				if(slide_id > -1){
					// nuke all shapes, cause it is easier to manage that way
					deleteSQLAnnotations(conn, slide_id);
					
					// re-insert all shapes
					for(ShapeEntry shape: slide.getAnnotations()){
						insertSQLAnnotation(conn, shape, slide_id);
					}
				}
			}
			
			// update findings
			deleteSQLFindings(conn,case_id);
			for(ConceptEntry concept: entry.getConceptEntries()){
				insertSQLFinding(conn, concept, case_id);
			}
		}else{
			IOntology ont = getKnowledgeBase(entry.getOntology());
			
			// copy into its own appropriate location
			File f = new File(getLocalCaseFolder(ont),entry.getName()+CASE_SUFFIX);
			
			// save into this file
			entry.save(new FileOutputStream(f));
				
			// do file upload operation
			// UIHelper.upload(f);
		}
	}

	/**
	 * save case entry in database
	 * @param entry
	 */
	public void removeCaseEntry(CaseEntry entry) throws Exception{
		if(databaseMode){
			Connection conn = getConnection();
			
			// insert/update slides
			for(SlideEntry slide: entry.getSlides()){
				int slide_id = getEntryId(conn,"Image",slide.getName());
				
				// now take care of annotations
				if(slide_id > -1){
					// nuke all shapes, cause it is easier to manage that way
					deleteSQLAnnotations(conn, slide_id);
				}
			}
		}else{
			IOntology ont = getKnowledgeBase(entry.getOntology());
			
			// copy into its own appropriate location
			File f = new File(getLocalCaseFolder(ont),entry.getName()+CASE_SUFFIX);
			
			// remove local file
			if(f.exists())
				f.delete();
			
		}
	}
	
	
	/**
	 * save case entry in database
	 * @param entry
	 */
	public void loadCaseEntry(CaseEntry entry) throws Exception{
		if(databaseMode){
			Connection conn = getConnection();
			int case_id = getEntryId(conn,"Case",entry.getName());
			if(case_id < 0)
				return;
			// load report from db if not in instance
			String r = entry.getReport();
			if(r == null || r.length() == 0){
				entry.setReport(loadSQLReport(conn,case_id));
			}
			
			// load slides use stuff from case base first,
			// else use what's in DB
			if(entry.getSlides().isEmpty()){
				for(SlideEntry slide: loadSQLImages(conn,case_id)){
					entry.addSlide(slide);
					for(ShapeEntry shape: loadSQLAnnotations(conn,slide.getId()))
						slide.addAnnotation(shape);
				}
			}else{
				for(SlideEntry slide: entry.getSlides()){
					int slide_id = loadSQLImage(conn, slide, case_id);
					// load annotations
					if(slide_id > -1){
						for(ShapeEntry shape: loadSQLAnnotations(conn, slide_id))
							slide.addAnnotation(shape);
					}
				}
			}
			
			// load findings
			for(ConceptEntry concept: entry.getConceptEntries()){
				loadSQLFinding(conn,concept,case_id);
			}
		}else if(!entry.isNewCase()){
			// look at case in local folder
			// copy into its own appropriate location
			IOntology ont = getKnowledgeBase(entry.getOntology());
			File f = new File(getLocalCaseFolder(ont),entry.getName()+CASE_SUFFIX);
			
			// load from file or url
			if(f.exists() && UIHelper.promptUseLocalCopy(entry.getName()+CASE_SUFFIX)){
				entry.load(new FileInputStream(f),false);
			}else{
				try{
					URL u = new URL(DEFAULT_HOST_URL+getCasePath(ont)+"/"+entry.getName()+CASE_SUFFIX);
					entry.load(u.openStream(),false);
				}catch(Exception ex){
					// if we fail we fail
					ex.printStackTrace();
				}
			}
			
		}
	}
	
	/**
	 * delete all sql annotations
	 * @param conn
	 * @param slide_id
	 * @throws SQLException
	 */
	private void deleteSQLAnnotations(Connection conn,int slide_id) throws SQLException{
		PreparedStatement st = null;
		try{
			// nuke all shapes, cause it is easier to manage that way
			st = conn.prepareStatement("DELETE FROM "+prefix+"Annotation WHERE image_id = ?");
			st.setInt(1,slide_id);
			st.executeUpdate();
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
		}
	}
	
	/**
	 * delete all sql annotations
	 * @param conn
	 * @param slide_id
	 * @throws SQLException
	 */
	private void deleteSQLFindings(Connection conn,int case_id) throws SQLException{
		PreparedStatement st = null;
		try{
			// nuke all shapes, cause it is easier to manage that way
			st = conn.prepareStatement("DELETE FROM "+prefix+"Finding WHERE case_id = ?");
			st.setInt(1,case_id);
			st.executeUpdate();
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
		}
	}
	
	
	
	/**
	 * insert/update SQL case
	 * @param entry
	 * @return
	 * @throws Exception
	 */
	private int insertSQLCase(Connection conn,CaseEntry entry) throws SQLException {
		PreparedStatement st = null;
		ResultSet result = null;
		// check if such case exists
		int case_id = getEntryId(conn,"Case",entry.getName());
		try{
			if(case_id > -1){
				// update case ???
				st = conn.prepareStatement("UPDATE "+prefix+"Case SET diagnosis=?, report = ? WHERE case_id=?");
				st.setString(1,""+entry.getDiagnoses());
				st.setString(2,entry.getReport());
				st.setInt(3,case_id);
				st.executeUpdate();
			}else{
				st = conn.prepareStatement("INSERT INTO "+prefix+"Case (name,organ,source,diagnosis,report) VALUES (?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
				st.setString(1,entry.getName());
				st.setString(2,entry.getOrgan());
				st.setString(3,entry.getInstitution());
				st.setString(4,""+entry.getDiagnoses());
				st.setString(5,entry.getReport());
				st.executeUpdate();
				result = st.getGeneratedKeys();
				if(result.next())
					case_id = result.getInt(1);
			}
		}catch(SQLException ex){
			throw ex;
		}finally {
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return case_id;
	}
	
	/**
	 * insert or update SQL image
	 * @param slide
	 * @param case_id
	 * @return
	 * @throws Exception
	 */
	private int insertSQLImage(Connection conn,SlideEntry slide, int case_id) throws SQLException {
		PreparedStatement st = null;
		ResultSet result = null;
		int slide_id = getEntryId(conn,"Image",slide.getSlideName());
		try{
			// insert or update
			if(slide_id > -1){
				st = conn.prepareStatement("UPDATE "+prefix+"Image SET part=?, block= ?,level=?,stain=? WHERE image_id=?");
				st.setString(1,slide.getPart());
				st.setString(2,slide.getBlock());
				st.setString(3,slide.getLevel());
				st.setString(4,slide.getStain());
				st.setInt(5,slide_id);
				st.executeUpdate();
			}else{
				st = conn.prepareStatement("INSERT INTO "+prefix+"Image (name,server,path,type,part,block,level,stain,case_id) " +
										   "VALUES (?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
				st.setString(1,slide.getSlideName());
				st.setString(2,slide.getServer());
				st.setString(3,slide.getPath());
				st.setString(4,slide.getType());
				st.setString(5,slide.getPart());
				st.setString(6,slide.getBlock());
				st.setString(7,slide.getLevel());
				st.setString(8,slide.getStain());
				st.setInt(9,case_id);
				st.executeUpdate();
				result = st.getGeneratedKeys();
				if(result.next())
					slide_id = result.getInt(1);
			}
		}catch(SQLException ex){
			throw ex;
		}finally {
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return slide_id;
	}
	
	
	/**
	 * insert or update SQL image
	 * @param slide
	 * @param case_id
	 * @return
	 * @throws Exception
	 */
	private void insertSQLFinding(Connection conn,ConceptEntry concept, int case_id) throws SQLException {
		PreparedStatement st = null;
		try{
			String examples  = ""+concept.getExampleMap();
			String locations = ""+concept.getLocations();
			
			if(examples.length() > LARGE_VARCHAR){
				examples = examples.substring(0,LARGE_VARCHAR-1);
				System.err.println("WARNING: trancating example at "+LARGE_VARCHAR+" for concept "+concept.getName());
			}
			if(locations.length() > LARGE_VARCHAR){
				locations = locations.substring(0,LARGE_VARCHAR-1);
				System.err.println("WARNING: trancating locations at "+LARGE_VARCHAR+" for concept "+concept.getName());
			}
			
			st = conn.prepareStatement("INSERT INTO "+prefix+"Finding (name,domain,asserted,examples,locations,case_id) VALUES (?,?,?,?,?,?)");
			st.setString(1,concept.getName());
			st.setString(2,""+concept.getConceptClass().getOntology().getURI());
			st.setBoolean(3,concept.isAsserted());
			st.setString(4,examples);
			st.setString(5,locations);
			st.setInt(6,case_id);
			
			st.executeUpdate();
			
		}catch(SQLException ex){
			throw ex;
		}finally {
			if(st != null)
				st.close();
		}
	}
	
	
	/**
	 * insert or update SQL image
	 * @param slide
	 * @param case_id
	 * @return
	 * @throws Exception
	 */
	private void loadSQLFinding(Connection conn,ConceptEntry concept, int case_id) throws SQLException {
		PreparedStatement st = null;
		ResultSet result = null;
		try{
			st = conn.prepareStatement("SELECT * FROM "+prefix+"Finding WHERE name = ? AND case_id = ?");
			st.setString(1,concept.getName());
			st.setInt(2,case_id);
			result = st.executeQuery();
			if(result.next()){
				concept.setAsserted(result.getBoolean("asserted"));
				concept.setExampleMap(TextHelper.parseMap(result.getString("examples")));
				ArrayList<String> list = new ArrayList<String>();
				Collections.addAll(list,TextHelper.parseList(result.getString("locations")));
				concept.setLocations(list);
			}
		}catch(SQLException ex){
			throw ex;
		}finally {
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
	}
	
	
	
	
	/**
	 * load SQL Image into entry
	 * @param conn
	 * @param slide
	 * @return
	 */
	private int loadSQLImage(Connection conn,SlideEntry slide, int case_id) throws SQLException{
		PreparedStatement st = null;
		ResultSet result = null;
		int slide_id = -1;
		try{
			st = conn.prepareStatement("SELECT * FROM "+prefix+"Image WHERE case_id = ? AND name = ?");
			st.setInt(1,case_id);
			st.setString(2,slide.getName());
			result = st.executeQuery();
			if(result.next()){
				slide_id = result.getInt("image_id");
				slide.setBlock(result.getString("block"));
				slide.setPart(result.getString("part"));
				slide.setLevel(result.getString("level"));
				slide.setStain(result.getString("stain"));
			}
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return slide_id;
	}
	
	/**
	 * load SQL Image into entry
	 * @param conn
	 * @param slide
	 * @return
	 */
	private String loadSQLReport(Connection conn, int case_id) throws SQLException{
		String report = null;
		PreparedStatement st = null;
		ResultSet result = null;
		try{
			st = conn.prepareStatement("SELECT report FROM "+prefix+"Case WHERE case_id = ?");
			st.setInt(1,case_id);
			result = st.executeQuery();
			if(result.next()){
				report = result.getString("report");
			}
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return report;
	}
	
	
	/**
	 * load SQL Image into entry
	 * @param conn
	 * @param slide
	 * @return
	 */
	private List<SlideEntry> loadSQLImages(Connection conn,int case_id) throws SQLException{
		List<SlideEntry> slides = new ArrayList<SlideEntry>();
		PreparedStatement st = null;
		ResultSet result = null;
		try{
			st = conn.prepareStatement("SELECT * FROM "+prefix+"Image WHERE case_id = ?");
			st.setInt(1,case_id);
			result = st.executeQuery();
			while(result.next()){
				SlideEntry slide = new SlideEntry(result.getString("name"));
				slide.setId(result.getInt("image_id"));
				slide.setBlock(result.getString("block"));
				slide.setPart(result.getString("part"));
				slide.setLevel(result.getString("level"));
				slide.setStain(result.getString("stain"));
				slides.add(slide);
			}
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return slides;
	}
	
	/**
	 * load SQL annotation
	 * @param conn
	 * @param shape
	 * @param slide_id
	 * @throws Exception
	 */
	private List<ShapeEntry> loadSQLAnnotations(Connection conn,int slide_id) throws  SQLException {
		PreparedStatement st = null;
		ResultSet result = null;
		List<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
		try{
			st = conn.prepareStatement("SELECT * FROM "+prefix+"Annotation WHERE image_id = ?");
			st.setInt(1,slide_id);
			result = st.executeQuery();
			while(result.next()){
				ShapeEntry shape = new ShapeEntry();
				shape.setName(result.getString("name"));
				shape.setTag(result.getString("tag"));
				shape.setType(result.getString("type"));
				shape.setImage(result.getString("image"));
				shape.setColorName(result.getString("color"));
				shape.setViewX(result.getInt("viewx"));
				shape.setViewY(result.getInt("viewy"));
				shape.setViewZoom(result.getFloat("zoom"));
				shape.setXStart(result.getInt("xstart"));
				shape.setYStart(result.getInt("ystart"));
				shape.setXEnd(result.getInt("xend"));
				shape.setYEnd(result.getInt("yend"));
				shape.setWidth(result.getInt("height"));
				shape.setHeight(result.getInt("width"));
				shape.setXPoints(result.getString("xpoints"));
				shape.setYPoints(result.getString("ypoints"));
				
				shapes.add(shape);
			}
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
			if(result != null)
				result.close();
		}
		return shapes;
	}
	
	/**
	 * insert SQL annotation
	 * @param conn
	 * @param shape
	 * @param slide_id
	 * @throws Exception
	 */
	private void insertSQLAnnotation(Connection conn, ShapeEntry shape, int slide_id) throws SQLException{
		PreparedStatement st = null;
		try{
			st = conn.prepareStatement("INSERT INTO "+prefix+"Annotation " +
				 "(name,tag,type,color,image,viewx,viewy,zoom,xstart,ystart,xend,yend,width,height," +
				 "xpoints,ypoints,image_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			shape.sync();
			
			String tag = shape.getTag();
			if(tag.length() > LARGE_VARCHAR){
				tag = tag.substring(0,LARGE_VARCHAR-1);
				System.err.println("WARNING: trancating tag at "+LARGE_VARCHAR+" in shape "+
								    shape.getName()+" for image "+shape.getImage());
			}
			st.setString(1,shape.getName());
			st.setString(2,shape.getTag());
			st.setString(3,shape.getType());
			st.setString(4,shape.getColorName());
			st.setString(5,shape.getImage());
			st.setInt(6,shape.getViewX());
			st.setInt(7,shape.getViewY());
			st.setFloat(8,shape.getViewZoom());
			st.setInt(9,shape.getXStart());
			st.setInt(10,shape.getYStart());
			st.setInt(11,shape.getXEnd());
			st.setInt(12,shape.getYEnd());
			st.setInt(13,shape.getWidth());
			st.setInt(14,shape.getHeight());
			st.setString(15,Arrays.toString(shape.getXPoints()));
			st.setString(16,Arrays.toString(shape.getYPoints()));
			st.setInt(17,slide_id);
			st.executeUpdate();
		}catch(SQLException ex){
			throw ex;
		}finally{
			if(st != null)
				st.close();
		}
	}
	
	/**
	 * get array
	 * @param length
	 * @return
	 */
	private String getArray(int length){
		StringBuffer b = new StringBuffer();
		for(int i=0;i<length;i++)
			b.append("?,");
		String s = b.toString();
		if(s.length() > 0)
			s = s.substring(0,s.length()-1);
		return "("+s+")";
	}
	
	/**
	 * find case for slide name
	 * @param slide
	 * @return case name or null
	 */
	String getCaseForSlide(Object [] slides){
		//this only works in database mode
		if(!databaseMode)
			return null;
		
		if(slides == null || slides.length == 0)
			return null;		
		String name = null;
		PreparedStatement st = null;
		ResultSet result = null;
		try{
			Connection conn = getConnection();
			st = conn.prepareStatement(
			"SELECT c.name FROM "+prefix+"Case c, "+prefix+"Image i "+
			"WHERE i.name IN "+getArray(slides.length)+
			" AND c.case_id=i.case_id");
			for(int i=0;i<slides.length;i++)
				st.setString(i+1,""+slides[i]);
			result = st.executeQuery();
			if(result.next()){
				name = result.getString(1);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			try{
				if(st != null)
					st.close();
				if(result != null)
					result.close();
			}catch(Exception ex){}
		}
		return name;
	}
}
