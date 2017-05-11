package edu.pitt.dbmi.domainbuilder.util;
import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

/**
 * Icon that displays vertical text
 * @author tseytlin
 */
public class VerticalTextIcon implements Icon{
	private String text;
	private Dimension size;
	private Font font;
	private int offs = 7;
	private boolean downup;
	private double angle;
	
	/**
	 * Text rotated 90 clockwise 
	 * @param txt
	 */
	public VerticalTextIcon(String txt){
		this(txt,false);
	}
	
	/**
	 * Text rotated clockwise or counterclockwise 
	 * @param text
	 * @param true - to rotate counter clockwise, false to rotate clockwize
	 */
	public VerticalTextIcon(String txt, boolean downup){
		this.text = txt;
		this.downup = downup;
		
		this.angle = Math.toRadians((downup)?-90:90);
		
		// get bounds
		font = UIManager.getFont("Label.font");
		
		//get bounds
		FontRenderContext frc = new FontRenderContext(null,true,true);
		TextLayout tl = new TextLayout(text,font,frc);
		Rectangle2D r = tl.getBounds();
		size = new Dimension((int)r.getWidth()+offs*2,(int)r.getHeight()+offs*2);
		
		//	rotate text
		AffineTransform fontAT = AffineTransform.getRotateInstance(angle);
		//fontAT.rotate(angle);
		font = font.deriveFont(fontAT);
	}
	
	public int getIconWidth(){
		return 1;
	}
	public int getIconHeight(){
		return size.width;
	}
	public void	paintIcon(Component c, Graphics g, int x, int y){
		Graphics2D g2 = (Graphics2D) g;
		 // Enable antialiasing for text
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    	g2.setFont(font);
    	if(downup)
    		g.drawString(text,x+2,y+getIconHeight()-offs);
    	else
    		g.drawString(text,x-offs,y+offs);
	}
}
