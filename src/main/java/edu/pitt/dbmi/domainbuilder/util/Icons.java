package edu.pitt.dbmi.domainbuilder.util;

import java.awt.Color;

import javax.swing.*;

/**
 * predefined values
 * @author tseytlin
 */
public class Icons {
	public static final int CONCEPT_ICON_WIDTH = 180;
	public static final int CONCEPT_ICON_HEIGHT = 24;
	public static final Color CONCEPT_SELECTION_COLOR = new Color(200,200,255,175);
	
	
	//misc icons
	public static final String UP  = "/icons/Up";
	public static final String DOWN  = "/icons/Down";
	public static final String ADD  = "/icons/Add";
	public static final String NEW  = "/icons/New";
	public static final String SAVE  = "/icons/Save";
	public static final String PUBLISH  = "/icons/Publish";
	public static final String SAVE_AS  = "/icons/SaveAs";
	public static final String OPEN  = "/icons/Open";
	public static final String PLUS  = "/icons/Plus";
	public static final String MINUS  = "/icons/Minus";
	public static final String ARROW  = "/icons/Arrow";
	public static final String CIRCLE  = "/icons/Circle";
	public static final String RECTANGLE  = "/icons/Rectangle";
	public static final String POLYGON  = "/icons/Polygon";
	public static final String RULER  = "/icons/Ruler";
	public static final String IMAGE  = "/icons/Image";
	public static final String SCREENSHOT  = "/icons/Camera";
	public static final String PROPERTIES  = "/icons/Properties";
	public static final String STATUS  = "/icons/Status";
	public static final String VALIDATE  = "/icons/Validate"; 
	public static final String IMPORT  = "/icons/Import";
	public static final String EXPORT  = "/icons/Export";
	public static final String SEARCH = "/icons/Search";
	public static final String BROWSE = "/icons/Search";
	public static final String EDIT = "/icons/Edit";
	public static final String LOGO = "/icons/SlideTutorLogo64.png";
	public static final String PREVIEW = "/icons/Preview128.png";
	public static final String TAG = "/icons/Tag";
	public static final String EXPAND = "/icons/Expand";
	public static final String COLLAPSE = "/icons/Collapse";
	public static final String CUT = "/icons/Cut";
	public static final String COPY = "/icons/Copy";
	public static final String DELETE = "/icons/Delete";
	public static final String PASTE = "/icons/Paste";
	public static final String UNION = "/icons/Union";
	public static final String INTERSECTION = "/icons/Intersection";
	public static final String COMPLEMENT = "/icons/Complement";
	public static final String ALIGN = "/icons/AlignJustifyHorizontal";
	public static final String REPLACE = "/icons/Replace";
	public static final String INFO = "/icons/Information";
	public static final String RUN = "/icons/Parse";
	public static final String WORKSHEET = "/icons/Preferences";
	public static final String SPELL = "/icons/Spell";
	public static final String HELP = "/icons/Help";
	public static final String ABOUT = "/icons/About";
	public static final String ONTOLOGY = "/icons/Ontology";
	public static final String LINK = "/icons/Link";
	public static final String MINUS_ALL = "/icons/MinusAll";
	public static final String BOOKMARK = "/icons/Bookmarks";
	public static final String GLOSSARY = "/icons/Information";
	public static final String LINE = "/icons/Line";
	public static final String UNDO = "/icons/Undo";
	public static final String FORWARD = "/icons/Forward";
	public static final String HIERARCHY = "/icons/Hierarchy";
	public static final String SORT = "/icons/Sort";
	public static final String REFRESH = "/icons/Refresh";
	public static final String CASE_PARTS = "/icons/CaseParts";
	public static final String SELECTION_CURSOR = "/icons/cursor-selection.gif";
	public static final String INFER_DIAGNOSES  = "/icons/Down";
	public static final String INFER_FINDINGS  = "/icons/Down";
	public static final String FONT  = "/icons/FontSize";
	
	/**
	 * get icon
	 */
	public static Icon getIcon(String text,int size){
		return getIcon(text+size+".gif");
	}
	
	/**
	 * get icon
	 */
	public static Icon getIcon(String text){
		try{
			return new ImageIcon(Icons.class.getResource(text));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return null;
	}
	
}
