package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * This class was developed by me at my previous job.
 * It is part of the fiswidgets project http://grommit.lrdc.pitt.edu/fiswidgets/
 */
public class Postit extends JPanel implements MouseListener, MouseMotionListener {
	private final int maxOffset = 50; // if component is 'higher' then this, then use this as offset
	private Point m; //mouse location
	private JComponent invoker; //component that the Postit is attached to
	private JToolTip tip; //this is the actual ToolTip
	private String text; //this is the PostIt text;
	private boolean loaded = false; //if the Postit was created before the components were 

	//displayed it has to be loaded when setVisible is invoked.

	/**
	 *  This class is used to display a lightweight PostIt note. 
	 *  It looks like a permanent ToolTip which is invisible by default.
	 *  Use setVisible to togle its visibility status.
	 *  @param JComponent invoker - this is the component that the PostIt is tied to.
	 *  @param String text - this is a text of the Postit.
	 *  Note: Make sure that the invoker component is visible before you
	 *  create an instance of the Postit. Otherwise you will get an Exception.
	 */
	public Postit(JComponent invoker, String text) {
		super();
		this.invoker = invoker;
		this.text = text;
		if (invoker == null || !invoker.isShowing())
			return;
		else
			createPostit();
	}

	/**
	 * format string in nice html way
	 * @param text
	 * @return
	 */
	private String formatText(String text){
		String html = "<table width=300><tr><td>"+text+"</td></tr></table>";
		
		// adjust text to be well-formatted html with picture
		Pattern pt = Pattern.compile("(.*)<example>(.*)</example>.*");
		Matcher mt = pt.matcher(text);
		if(mt.matches()){
			text  = mt.group(1);
			String pic = mt.group(2);
			// load picture to see ration
			int w=0,h=0; 
			try{
				ImageIcon img = new ImageIcon(new URL(pic));
				w = img.getIconWidth();
				h = img.getIconHeight();
			}catch(Exception ex){}
			
			// adjust width/height to fit into limit
			if(w >= h && w > 300){
				h = (h * 300)/ w;
				w = 300;
			}else if( w < h && h > 200){
				w = (w * 200)/ h;
				h = 200;
			}
			int width = (w>=h)?300:350;	
			//reset text
			html = "<table width="+width+"><tr><td valign=top>"+text+"</td>"+
					((w >= h)?"</tr><tr>":"")+
					"<td><a href=\""+pic+"\"><img src=\""+pic+"\" border=1 width="+w+
					" height="+h+" ></a></td></tr></table>";
		}
		return html;
	}
	
	
	/**
	 *  this is the method which creates the PostIt
	 */
	private void createPostit() {
		//create a tooltip
		tip = invoker.createToolTip();
		tip.setLayout(new BorderLayout());
		tip.setTipText("<html>"+formatText(text)+"<br><br><br>");
		
		
		tip.setBackground(Color.white);
		tip.setBorder(new LineBorder(Color.black));
		
		
		// add close button
		JPanel pane = new JPanel();
		pane.setOpaque(false);
		JButton close = new JButton("close");
		close.setFont(close.getFont().deriveFont(Font.PLAIN));
		//close.setPreferredSize(new Dimension(100,30));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				remove();
			}
		});
		pane.add(close);
		tip.add(pane, BorderLayout.SOUTH);
		
		
		//calculate postit location
		//Dimension size = tip.getPreferredSize();
		Point screenLocation = invoker.getLocationOnScreen();
		Point p = new Point();
		int offset = invoker.getPreferredSize().height;
		if (offset > maxOffset)
			offset = maxOffset;
		p.x = screenLocation.x + offset;
		p.y = screenLocation.y + offset;

		//create a tooltip window
		setLayout(new BorderLayout());
		setDoubleBuffered(true);
		this.setOpaque(true);
		add(tip, BorderLayout.CENTER);
		setSize(tip.getPreferredSize());
		//setSize(new Dimension(500,500));
		//validate();
		addMouseListener(this);
		addMouseMotionListener(this);

		//add the component to the GlassPane
		SwingUtilities.convertPointFromScreen(p, invoker.getRootPane().getLayeredPane());
		this.setBounds(p.x, p.y, getSize().width, getSize().height);
		invoker.getRootPane().getLayeredPane().add(this, JLayeredPane.POPUP_LAYER, 0);

		//is invisible by default
		setVisible(false);
		loaded = true;
	}

	/**
	 *  This method shows/hides the PostIt
	 */
	public void display(boolean status) {
		//if it was loaded then simply display, else load it if you can
		if (loaded) {
			setVisible(status);
		} else {
			if (invoker == null || !invoker.isShowing())
				return;
			else {
				createPostit();
				setVisible(status);
			}
		}
	}

	/**
	 * remove itself from the component
	 */
	public void remove() {
		invoker.getRootPane().getLayeredPane().remove(this);
		display(false);
		invoker.getRootPane().getLayeredPane().repaint();
		invoker.repaint();
	}

	/**
	 *  This is method returns a handle to JToolTip.
	 */
	public JToolTip getToolTip() {
		return tip;
	}

	/**
	 *  This is method returns a handle to the parent JComponent
	 */
	public JComponent getComponent() {
		return invoker;
	}

	/**
	 * Convinient method to get text in the Postit.
	 */
	public String getText() {
		return text;
	}

	/**
	 * Convinient method to get text in the Postit.
	 */
	public void setText(String text) {
		if (loaded) {
			this.text = text;
			tip.setTipText(text);
		} else {
			this.text = text;
		}
	}

	public void update(Graphics g) {
		paint(g);
	}

	// do the draging by reseting location of the component
	public void mouseDragged(MouseEvent e) {
		Point pt = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), invoker.getRootPane());
		pt.x = pt.x - m.x;
		pt.y = pt.y - m.y;
		setLocation(pt);
	}

	public void mousePressed(MouseEvent e) {
		m = e.getPoint(); //remember the location of mouse cursor
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
		//if(e.getClickCount() == 2 || e.getButton() == MouseEvent.BUTTON3)
		//	remove();
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

}
