package edu.pitt.dbmi.domainbuilder.beans;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.*;

import javax.swing.Icon;

import edu.pitt.dbmi.domainbuilder.util.Icons;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.ILogicExpression;
import edu.pitt.ontology.IOntology;
import edu.pitt.ontology.IProperty;
import edu.pitt.ontology.IRestriction;
import edu.pitt.ontology.LogicExpression;

/**
 * this is an expression of concepts
 * @author tseytlin
 */
public class ConceptExpression extends LogicExpression implements Icon{
	//private SortedMap<Integer,Integer> vtable;
	//private int elementCount;
	//private Set<ConceptEntry> elementList;
	private final int PAD = 8;
	private int iconHeight = -1;
	private final Stroke orStroke = new BasicStroke(3);
	private boolean horizontalIcon;
	
	public ConceptExpression(int type){
		super(type);
	}
	
	public ConceptExpression(){
		super(AND);
	}
	
	public String getExpressionTypAsString(){
		if(getExpressionType() == AND)
			return "AND";
		else if(getExpressionType() == OR)
			return "OR";
		else if(getExpressionType() == NOT)
			return "NOT";
		return "";
	}
	

	public void setHorizontalIcon(boolean horizontalIcon) {
		this.horizontalIcon = horizontalIcon;
	}

	
	/**
	 * recreate trnaslation table
	 *
	public void recreateTranslationTable(){
		vtable = createTranslationTable();
	}
	*/
		
	/**
	 * create translation table for this expression
	 * @return
	 *
	private  SortedMap<Integer,Integer> createTranslationTable(){
		SortedMap<Integer,Integer> table = new TreeMap<Integer,Integer>();
		for(int i=0;i<size();i++)
			table.put(i,i);
		return table;
	}
	*/
	
	/**
	 * set entry to be displayed at given position
	 * (given that this entry is available)
	 * @param e
	 */
	public Object removeEntry(int n){
		/*
		Object obj = null;
		if(vtable == null)
			vtable = createTranslationTable();
		if(vtable.containsKey(n)){
			int i = vtable.get(n);
			if(i < size()){
				obj = remove(i);
			}
			vtable.remove(n);	
		}
		return obj;
		*/
		Object obj = get(n);
		if(n < size())
			set(n,"");
		return obj;
		//return (n < size())?remove(n):null;
	}
	
	
	/**
	 * add new entry to be displayed at given position
	 * (given that this entry is available)
	 * @param e
	 */
	public boolean addEntry(Object e, int n){
		/*
		if(vtable == null)
			vtable = createTranslationTable();
		boolean b = add(e);
		vtable.put(n,indexOf(e));
		return b;
		*/
		if(n >= size()){
			for(int i=size();i<n;i++){
				add("");
			}
		}else if(n<size()){
			set(n,e);
			return true;
		}
		add(n,e);
		return true;
	}
	
	
	/**
	 * add new entry to be displayed at given position
	 * (given that this entry is available)
	 * @param e
	 */
	public boolean insertEntry(Object e, int n){
		if(n >= size()){
			for(int i=size();i<n;i++){
				add("");
			}
		}
		add(n,e);
		return true;
	}
	
	
	/**
	 * get virtual index given real one
	 * (very inefficient)
	 * @param real
	 * @return
	 *
	private int getVirtualIndex(int real){
		for(int x: vtable.keySet()){
			if(real == vtable.get(x))
				return x;
		}
		return 0;
	}
	*/
	/**
	 * set entry to be displayed at given position
	 * (given that this entry is available)
	 * @param e
	 */
	public void setEntry(Object e, int n){
		/*
		if(vtable == null)
			vtable = createTranslationTable();
		
		// check for empty inserts
		if(e == null || e.equals("")){
			// see if there is any object that is
			// at target index
			if(vtable.containsKey(i)){
				int realo = vtable.get(i);
				vtable.put(vtable.lastKey()+1,realo);
				vtable.remove(i);
			}
		}else{
			// find this object in list
			int reali = indexOf(e);
			
			// see if there is any object that is
			// at target index
			if(vtable.containsKey(i)){
				int realo = vtable.get(i);
				vtable.put(getVirtualIndex(reali),realo);
			}
			// if real index doesn't exists, reroute the table
			if(reali < 0){
				add(e);
				reali = indexOf(e);
			}
			vtable.put(i,reali);
		}
		*/
		int x = indexOf(e);
		if(x > -1){
			Object old = get(x);
			remove(old);
			insertEntry(old,n);
		}
	}
	
	
	/**
	 *  get all ellements
	 * @return
	 *
	public Set<ConceptEntry> getAllElements(){
		if(elementList == null){
			elementList = new HashSet<ConceptEntry>();
			if(vtable == null)
				vtable = createTranslationTable();
			for(int i=0;i<elementCount;i++)
				elementList.add((ConceptEntry)getEntry(i));
		}
		return elementList;
	}*/
	
	/**
	 * get concept entry from "virtual" address
	 */
	public Object getEntry(int n){
		/*
		if(vtable == null)
			vtable = createTranslationTable();
		
		if(vtable.containsKey(n)){
			int i = vtable.get(n);
			return (i < size())?get(i):"";
		}*/
		return (n>=0 && n < size())?get(n):"";
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		int w = getIconWidth();
		int h = getIconHeight();
	
		// which expression
		String txt = (getExpressionType() == ILogicExpression.AND)?"and":"or";
		g.setColor(Color.black);
		((Graphics2D)g).setStroke(orStroke);
		
		
		if(horizontalIcon){
			int o = (getExpressionType() == ILogicExpression.AND)?-3:2;
			// draw brackets around expression
			for(int i=0,ix=0;i<size();i++){
				if(get(i) instanceof Icon){
					Icon icon = (Icon) get(i);
					int iw = icon.getIconWidth();
					
					icon.paintIcon(c,g,x+ix,y);
					ix += iw+PAD*2;
					
					// draw divider
					if(i< size()-1){
						g.drawLine(x+ix-PAD,y,x+ix-PAD,y+h/4);
						g.drawLine(x+ix-PAD,y+3*h/4,x+ix-PAD,y+h);
						g.drawString(txt,x+ix-PAD*2+o,y+h/2+4);
					}	
					
				}
			}
		}else{
			int o = (getExpressionType() == ILogicExpression.AND)?10:6;
			// draw brackets around expression
			for(int i=0,iy=0;i<size();i++){
				if(get(i) instanceof Icon){
					Icon icon = (Icon) get(i);
					int ih = icon.getIconHeight();
					//int iy = i* (ih+PAD);
					icon.paintIcon(c,g,x,y+iy);
					// draw divider
					if(i< size()-1){
						g.drawLine(x+w/4,y+iy+ih+PAD/2,x+w/2-15,y+iy+ih+PAD/2);
						g.drawLine(x+w/2+15,y+iy+ih+PAD/2,x+3*w/4,y+iy+ih+PAD/2);
						g.drawString(txt,x+w/2-o,y+iy+ih+PAD/2+4);
					}	
					iy += ih+PAD;
				}
			}
		}
	}

	public int getIconWidth() {
		if(!horizontalIcon)
			return Icons.CONCEPT_ICON_WIDTH;
		
		// calculate icon with for horizontal icons
		if(iconHeight < 0){
			iconHeight = 0;
			// iterate over all values
			for(int i=0;i<size();i++){
				if(get(i) instanceof Icon){
					Icon icon = (Icon) get(i);
					iconHeight = iconHeight + icon.getIconWidth()+PAD*2;
					if(i == (size()-1))
						iconHeight -= PAD*2;
				}
			}
			//iconHeight += PAD;
		}
		
		return iconHeight;
	}

	public int getIconHeight() {
		if(horizontalIcon)
			return Icons.CONCEPT_ICON_HEIGHT;
		
		if(iconHeight < 0){
			iconHeight = 0;
			// iterate over all values
			for(int i=0;i<size();i++){
				if(get(i) instanceof Icon){
					Icon icon = (Icon) get(i);
					iconHeight = iconHeight + icon.getIconHeight()+PAD;
					if(i == (size()-1))
						iconHeight -= PAD;
				}
			}
			//iconHeight += PAD;
		}
		return iconHeight;
	}

	
	/**
	 * get appropriate property based on concept entry
	 * @param e
	 * @return
	 */
	private IProperty getProperty(IOntology o, ConceptEntry e){
		IClass cls = e.getConceptClass();
		if(OntologyHelper.isAncillaryStudy(cls))
			return o.getProperty(HAS_ANCILLARY);
		else if(OntologyHelper.isClinicalFeature(cls))
			return o.getProperty((e.isAbsent())?HAS_NO_FINDING:HAS_CLINICAL);
		// else assume diagnostic
		return o.getProperty((e.isAbsent())?HAS_NO_FINDING:HAS_FINDING);
	}
	
	/**
	 * get ontology action that represents changes to this expression
	 * @return
	 */
	public ILogicExpression toOntologyExpression(IOntology o){
		// check if this
		ILogicExpression exp = new LogicExpression(getExpressionType());
		for(Object e: this){
			if(e instanceof ConceptEntry){
				ConceptEntry param = (ConceptEntry) e;
				IRestriction r = o.createRestriction(IRestriction.SOME_VALUES_FROM);
				r.setProperty(getProperty(o,param));
				r.setParameter(param.getConceptClass().getLogicExpression());
				exp.add(r);
			}else if(e instanceof ConceptExpression){
				ConceptExpression c = (ConceptExpression) e;
				IRestriction r = c.toRestriction(o);
				if(r != null){
					exp.add(r);
				}else{
					ILogicExpression p = c.toOntologyExpression(o);
					if(!p.isEmpty())
						exp.add(p);
				}
			}
		}
		return exp;
		//return toOntologyExpression(o,true);
	}
	
	
	
	/**
	 * convert to logical expression with IClass objects, but no restrictions
	 * @return
	 */
	public ILogicExpression toLogicExpression(){
		// check if this
		ILogicExpression exp = new LogicExpression(getExpressionType());
		for(Object e: this){
			if(e instanceof ConceptEntry){
				ConceptEntry param = (ConceptEntry) e;
				IClass c = param.getConceptClass();
				if(param.isAbsent()){
					exp.add(c.getOntology().createLogicExpression(ILogicExpression.NOT,c));
				}else{
					exp.add(c);
				}
			}else if(e instanceof ConceptExpression){
				exp.add(((ConceptExpression)e).toLogicExpression());
			}
		}
		return exp;
		//return toOntologyExpression(o,true);
	}
	
	
	/**
	 * update references
	 */
	public void updateReference(IOntology ont){
		for(Object o: this){
			if(o instanceof ConceptEntry){
				((ConceptEntry)o).updateReference(ont);
			}else if(o instanceof ConceptExpression){
				((ConceptExpression)o).updateReference(ont);
			}
		}
	}
	
	
	/**
	 * attempt to convert an expression to single restriction
	 * return null, if it cannot be done
	 * @param o
	 * @return
	 */
	private IRestriction toRestriction(IOntology o){
		if(getExpressionType() == ILogicExpression.OR){
			ILogicExpression param = new LogicExpression(ILogicExpression.OR);
			boolean different = false;
			Boolean absent = null;
			for(Object obj: this){
				if(obj instanceof ConceptEntry){
					ConceptEntry e = (ConceptEntry) obj;
					
					// determine abasence and uniformaty
					if(absent == null)
						absent = e.isAbsent();
					
					if(!absent.equals(e.isAbsent()))
						different = true;
					param.add(e.getConceptClass());
				}
			}
			if(!different){
				IRestriction r = o.createRestriction(IRestriction.SOME_VALUES_FROM);
				r.setProperty(o.getProperty((absent)?HAS_NO_FINDING:HAS_FINDING));
				r.setParameter(param);
				return r;
			}
		}
		return null;
	}
	
	
	/**
	 * get ontology action that represents changes to this expression
	 * @return
	 *
	public ILogicExpression toOntologyExpression(IOntology o, boolean shortcut){
		// see if we can make a shortcut expression
		if(shortcut && getExpressionType() == ILogicExpression.OR){
			ILogicExpression param = new LogicExpression(ILogicExpression.OR);
			boolean different = false;
			Boolean absent = null;
			for(Object obj: this){
				if(obj instanceof ConceptEntry){
					ConceptEntry e = (ConceptEntry) obj;
					
					// determine abasence and uniformaty
					if(absent == null)
						absent = e.isAbsent();
					
					if(!absent.equals(e.isAbsent()))
						different = true;
					param.add(e.getConceptClass());
				}
			}
			if(!different){
				IRestriction r = o.createRestriction(IRestriction.SOME_VALUES_FROM);
				r.setProperty(o.getProperty((absent)?HAS_NO_FINDING:HAS_FINDING));
				r.setParameter(param);
				return o.createLogicExpression(ILogicExpression.AND,r);
			}
		}
		
		
		// check if this
		ILogicExpression exp = new LogicExpression(getExpressionType());
		for(Object e: this){
			if(e instanceof ConceptEntry){
				ConceptEntry param = (ConceptEntry) e;
				IRestriction r = o.createRestriction(IRestriction.SOME_VALUES_FROM);
				r.setProperty(o.getProperty((param.isAbsent())?HAS_NO_FINDING:HAS_FINDING));
				r.setParameter(param.getConceptClass().getLogicExpression());
				exp.add(r);
			}else if(e instanceof ConceptExpression){
				exp.add(((ConceptExpression)e).toOntologyExpression(o));
			}
		}
		return exp;
	}
	*/
	
	/**
	 * get text representation of this expression
	 * @return
	 */
	public String getText(){
		String s = (getExpressionType() == AND)?" AND ":" OR ";
		StringBuffer buffer = new StringBuffer();
		for(Object o: this){
			if(o instanceof ConceptEntry){
				buffer.append(((ConceptEntry)o).getText()+s);
			}else if(o instanceof ConceptExpression){
				buffer.append(((ConceptExpression)o).getText()+s);
			}
		}
		// remove last separator
		buffer.delete(buffer.length()-s.length(),buffer.length());
		return buffer.toString();
	}
	
	/**
	 * return concept expression copy
	 */
	public ConceptExpression clone(){
		ConceptExpression exp = new ConceptExpression(getExpressionType());
		for(Object o: this){
			if(o instanceof ConceptEntry){
				exp.add(((ConceptEntry)o).clone());
			}else if (o instanceof ConceptExpression){
				exp.add(((ConceptExpression)o).clone());
			}
		}
		return exp;
	}
	
	
	/**
	 * convert ILogicExpression to ConceptExpression
	 * @param exp
	 * @return
	 */
	public static ConceptExpression toConceptExpression(ILogicExpression exp){
		ConceptExpression e = new ConceptExpression(exp.getExpressionType());
		
		// take care of negation
		boolean absent = (e.getExpressionType() == ILogicExpression.NOT);
		
		
		for(Object o: exp){
			if(o instanceof ILogicExpression){
				ConceptExpression e1 = toConceptExpression((ILogicExpression) o);
				if(e1.getExpressionType() == ILogicExpression.NOT){
					e.add(e1.getOperand());
				}else{
					e.add(e1);
				}
			}else if (o instanceof IRestriction){
				e.add(toConceptExpression(((IRestriction)o).getParameter()));
			}else if (o instanceof IClass){
				ConceptEntry c =  new ConceptEntry((IClass) o);
				c.setAbsent(absent);
				e.add(c);
			}
		}
		return e;
	}
	
}
