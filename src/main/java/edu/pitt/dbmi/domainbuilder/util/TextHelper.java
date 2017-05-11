package edu.pitt.dbmi.domainbuilder.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptExpression;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.text.tools.TextTools;


public class TextHelper {
	/**
	 * get pretty printed HTML string for tooltips and stuff
	 * @param string to be formated
	 */
	 public static String formatString(String text){
		 return formatString(text,40);
	 }
	
	/**
	 * get pretty printed HTML string for tooltips and stuff
	 * @param string to be formated
	 * @param length of single line
	 */
	 public static String formatString(String text,int limit){
		 int charLimit = limit;
		 StringBuffer def = new StringBuffer("<html>");
		 //format definition itself
		 char [] str = text.toCharArray();
		 boolean insideTag = false;
		 String tag = "";
		 for(int i=0,k=0;i<str.length;i++){
			def.append(str[i]);
			if(str[i] == '\n' || (k > charLimit   && str[i] == ' ')){
				def.append("<br>");
				k = 0;
			}
			// increment char limit counter
			// skip HTML tags (assume that < is always closed by >
			if(str[i] == '<'){
				insideTag = true;
			}else if (str[i] == '>'){
				insideTag = false;
				k --;
				// check tag, if it is a newline tag
				// then reset the counter
				if( tag.equalsIgnoreCase("<hr") || tag.equalsIgnoreCase("<br") || tag.equalsIgnoreCase("<p"))
					k = 0;
				tag = "";
			}
			
			if(!insideTag)
				k++;
			else
				tag = tag+str[i];
		 }
		 return def.toString();
	 }
	 
	 /**
	 * This method gets a text file (HTML too) from input stream 
	 * reads it, puts it into string and substitutes keys for values in 
	 * from given map
	 * @param InputStream text input
	 * @return String that was produced
	 * @throws IOException if something is wrong
	 * WARNING!!! if you use this to read HTML text and want to put it somewhere
	 * you should delete newlines
	 */
	public static String getText(InputStream in) throws IOException {
		return getText(in,null);
	}
	
	/**
	 * This method gets a text file (HTML too) from input stream 
	 * reads it, puts it into string and substitutes keys for values in 
	 * from given map
	 * @param InputStream text input
	 * @param Map key/value substitution (used to substitute paths of images for example)
	 * @return String that was produced
	 * @throws IOException if something is wrong
	 * WARNING!!! if you use this to read HTML text and want to put it somewhere
	 * you should delete newlines
	 */
	public static String getText(InputStream in, Map<String,String> sub) throws IOException {
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
			for (String key : sub.keySet()){
				text = text.replaceAll(key,sub.get(key));
			}
		}
		return text;
	}
	
	/**
	 * extract section text from full report
	 * @param section
	 * @param entire report text
	 * @return
	 */
	public static String getSectionText(String section, String text){
		String str = "";
		Pattern pt = Pattern.compile("^"+section+":$(.*?)^[A-Z ]+:$",
					 Pattern.MULTILINE|Pattern.DOTALL);
		Matcher mt = pt.matcher(text);
		if(mt.find()){
			str = mt.group(1);
		}else{
			pt = Pattern.compile("^"+section+":$(.*)",
				 Pattern.MULTILINE|Pattern.DOTALL);
			mt = pt.matcher(text);
			if(mt.find())
				str = mt.group(1);
		}
		
		// if we fail here maybe sections are marked differently in this text
		if(str.length() == 0){
			pt = Pattern.compile("^\\[" + section + "\\]$(.*?)^\\[[A-Z ]+\\]$", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
			mt = pt.matcher(text);
			if (mt.find()) {
				str = mt.group(1);
			} else {
				pt = Pattern.compile("^\\["+ section + "\\]$(.*)", Pattern.MULTILINE | Pattern.DOTALL  | Pattern.CASE_INSENSITIVE);
				mt = pt.matcher(text);
				if (mt.find())
					str = mt.group(1);
			}
		}
		
		
		// strip last end-of-line
		if(str.endsWith("\n"))
			str = str.substring(0,str.length()-1);
		
		return str;
	}
	
	/**
	 * get character count from string
	 * @param str
	 * @return
	 */
	public static int getSequenceCount(String text,String str){
		int count = 0;
		for(int i = text.indexOf(str);i > -1;i = text.indexOf(str,i+1)){
			count ++;
		}
		return count;
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
	 * parse list that is represented, by java list dump
	 * @param str
	 * @return
	 */
	public static String [] parseList(String str){
		if(str == null)
			return new String [0];
		str = str.trim();
		if(str.startsWith("["))
			str = str.substring(1);
		if(str.endsWith("]"))
			str = str.substring(0,str.length()-1);
		if(str.length() == 0 || str.equals("null"))
			return new String [0];
		String [] slist = str.split(",");
		for(int i=0; i< slist.length; i++)
			slist[i] = slist[i].trim();
		return slist;
	}
	
	/**
	 * parse list that is represented, by java list dump
	 * @param str
	 * @return
	 */
	public static Map<String,String> parseMap(String str){
		Map<String,String> map = new HashMap<String,String>();
		
		if(str == null)
			return map;
		str = str.trim();
		if(str.startsWith("{"))
			str = str.substring(1);
		if(str.endsWith("}"))
			str = str.substring(0,str.length()-1);
		if(str.length() == 0 || str.equals("null"))
			return map;
		for(String s: str.split(",")){
			String [] p = s.split("=");
			if(p.length == 2){
				String key = p[0].trim();
				String val = p[1].trim();
				if(!key.equals("null") && !val.equals("null"))
					map.put(key,val);
			}
		}
		return map;
	}
	
	/**
	 * get input stream for object 
	 * @param text
	 * @return
	 */
	public static InputStream toInputStream(Object text){
		return new ByteArrayInputStream(text.toString().getBytes());
	}
	
	
	/**
	 * convert object to string
	 * @param obj
	 * @return
	 *
	public static String toString(Object obj){
		return (obj == null)?"":obj.toString();
	}*/
	
	/**
	 * convert object to string
	 * 
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj) {
		if (obj == null)
			return "";

		if (obj instanceof ConceptEntry)
			return ((ConceptEntry) obj).getText();

		// display map or collection
		if (obj instanceof Collection || obj instanceof Map) {
			String s = "" + obj;
			s = s.substring(1, s.length() - 1);

			// remove nulls from string
			s = s.replaceAll("\\bnull\\b", "");

			return s;
		}

		return obj.toString();
	}

	/**
	 * convert a list of concept entries to text
	 * 
	 * @param e
	 * @return
	 */
	public static String toText(List<ConceptEntry> e) {
		List<String> s = new ArrayList<String>();
		for (ConceptEntry c : e)
			s.add(toString(c));
		return toString(s);
	}
	
	
	/**
	 * return a unique set of string values from given list
	 * @param list
	 * @return
	 */
	public static Set<String> getValues(List list){
		Set<String> values = new LinkedHashSet<String>();
		for(Object obj: list){
			String s = (String) obj;
			if(s != null && s.length() > 0)
				values.add(s);
		}
		return values;
	}
	
	
	/**
	 * filter out stop words from text
	 * @param text
	 * @return
	 */
	public static String filterStopWords(String text){
		for(String word: TextTools.getStopWords()){
			if(text.matches(".*\\b"+word+"\\b.*")){
				text = text.replaceAll("\\b"+word+"\\b","");
			}
		}
		text = text.replaceAll("\\s+"," ").trim();
		return text;
	}
	
	
	/**
	 * create a pretty expression for diagnostic rules
	 * @param exp
	 * @return
	 */
	public static String formatExpression(ConceptExpression exp){
		StringBuffer buffer = new StringBuffer();
		int count = 1;
		if(exp.getExpressionType() == ILogicExpression.OR)
			count = exp.size();
		buffer.append("<table width=500 border=1><tr><th colspan="+count+">Diagnostic Rules:</th></tr><tr>");
		for(int i=0;i<count;i++){
			ConceptExpression e = (count > 1)?(ConceptExpression)exp.get(i):exp;
			buffer.append("<td valign=top>");
			if(!e.isEmpty()){
				for(Object o: e){
					buffer.append(formatParameter(o)+"<br><br>");
				}
				// strip last newline
				buffer.replace(buffer.length()-8,buffer.length(),"");
			}
			buffer.append("</td>");
		}
		buffer.append("</tr></table>");
		return buffer.toString();
	}
	
	/**
	 * create a pretty expression for diagnostic rules
	 * @param exp
	 * @return
	 */
	private static String formatParameter(Object obj){
		if(obj instanceof ConceptEntry){
			return ((ConceptEntry)obj).getText();
		}else if(obj instanceof ConceptExpression){
			StringBuffer b = new StringBuffer();
			ConceptExpression exp = (ConceptExpression) obj;
			String type = exp.getExpressionTypAsString();
			for(Object p: exp){
				b.append(formatParameter(p)+" "+type+" ");
			}
			String text = b.toString();
			return (text.endsWith(type+" "))?text.substring(0,text.length()-type.length()-2):text;
		}
		return ""+obj;
	}
	
	/**
	 * This function attempts to convert vaires types of input into numerical equivalent
	 */
	public static double parseDecimalValue(String text) {
		double value = 0;
		// check if this is a float
		if(text.matches("\\d+\\.\\d+")){
			// try to parse regular number
			try {
				value = Double.parseDouble(text);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}else{
			value = parseIntegerValue(text);
		}
		return value;
	}

	/**
	 * This function attempts to convert vaires types of input into numerical equivalent
	 */
	public static int parseIntegerValue(String text) {
		int value = 0;

		// try to parse roman numerals
		if (text.matches("[IiVvXx]+")) {
			boolean oneLess = false;
			for (int i = 0; i < text.length(); i++) {
				switch (text.charAt(i)) {
					case 'i':
					case 'I':
						value++;
						oneLess = true;
						break;
					case 'v':
					case 'V':
						value += ((oneLess) ? 3 : 5);
						oneLess = false;
						break;
					case 'x':
					case 'X':
						value += ((oneLess) ? 8 : 10);
						oneLess = false;
						break;
				}
			}

			return value;
		}
		// try to parse words
		if (text.matches("[a-zA-Z]+")) {
			if (text.equalsIgnoreCase("zero"))
				value = 0;
			else if (text.equalsIgnoreCase("one"))
				value = 1;
			else if (text.equalsIgnoreCase("two"))
				value = 2;
			else if (text.equalsIgnoreCase("three"))
				value = 3;
			else if (text.equalsIgnoreCase("four"))
				value = 4;
			else if (text.equalsIgnoreCase("five"))
				value = 5;
			else if (text.equalsIgnoreCase("six"))
				value = 6;
			else if (text.equalsIgnoreCase("seven"))
				value = 7;
			else if (text.equalsIgnoreCase("eight"))
				value = 8;
			else if (text.equalsIgnoreCase("nine"))
				value = 9;
			else if (text.equalsIgnoreCase("ten"))
				value = 10;
			else if (text.equalsIgnoreCase("eleven"))
				value = 11;
			else if (text.equalsIgnoreCase("twelve"))
				value = 12;
			else
				value = (int) OntologyHelper.NO_VALUE;

			return value;
		}

		// try to parse regular number
		try {
			value = Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			//ex.printStackTrace();
			return (int) OntologyHelper.NO_VALUE;
		}
		return value;
	}
	
	
	/**
	 * is string a number
	 * @param text
	 * @return
	 */
	public static boolean isNumber(String text){
		return text.matches("\\d+(\\.\\d+)?");
	}
	
	
	/**
	 * pretty print number as integer or 2 precision float
	 * format numeric value as string
	 * @return
	 */
	public static String toString(double numericValue){
		Formatter f = new Formatter();
		if((numericValue*10)%10 == 0)
			f.format("%d",(int)numericValue);
		else
			f.format("%.2f",numericValue);
		return ""+f.out();
	}
	
	/**
	 * check if string is empty (take care of null)
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str){
		return str == null || str.length() == 0 || "null".equals(str);
	}
	
	/**
	 * create URI from string
	 * @param s
	 * @return
	 */
	
	public static URI toURI(String s){
		try {
			return new URI(s.replaceAll("\\s","%20"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * create URI from string
	 * @param s
	 * @return
	 */
	
	public static URL toURL(String s){
		try {
			return new URL(s.replaceAll("\\s","%20"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * create URI from string
	 * @param s
	 * @return
	 */
	
	public static URL toURL(URI s){
		try {
			return s.toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
