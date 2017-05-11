package edu.pitt.dbmi.misc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;


import edu.pitt.dbmi.domainbuilder.beans.*;
import edu.pitt.slideviewer.*;
import edu.pitt.slideviewer.markers.Annotation;

public class FlipCaseAnnotations {
	private CaseEntry caseEntry;
	private static String config = "http://slidetutor.upmc.edu/viewer/SlideViewer.conf";
	private static Viewer viewer;
	private static JFrame frame;
	
	public static void initialize() throws Exception{
		Properties p = new Properties();
		p.load((new URL(config)).openStream());
		ViewerFactory.setProperties(p);
		
		//open viewer
		viewer = ViewerFactory.getViewerInstance("qview");
		viewer.setSize(600,600);
		
		
		// add to viewer
		//viewerPanel.add(viewer.getViewerPanel(),BorderLayout.CENTER);
		//viewerPanel.revalidate();
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		//frame.getContentPane().add(flip,BorderLayout.NORTH);
		frame.getContentPane().add(viewer.getViewerPanel(),BorderLayout.CENTER);
		//frame.getContentPane().add(save,BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
	}
	
	
	public FlipCaseAnnotations(File f) throws Exception{
		caseEntry = new CaseEntry("TEMP");
		caseEntry.load(new FileInputStream(f));
	
	}
	
	
	
	/**
	 * display case
	 * @param c
	 *
	public void displayCase(){
		String name = caseEntry.getName();
		
		// query servlet
		//Map map = (Map) Utils.queryServlet(servlet,"get-case-info&domain="+domain+"&case="+name);
		
		// get list of slides
		String info = "No Report Available";
		String [] slides = new String [caseEntry.getSlides().size()];
		for(SlideEntry 
		
		
		Object [] s =  caseEntry.getSlides()
		String t = (String) inst.getPropertyValue(ontology.getProperty(OntologyHelper.HAS_REPORT));
		if(s != null && s.length > 0){
			slides = (String [])s;
		}
	
		info = UIHelper.convertToHTML(caseEntry.getReport());
		
		
		// init viewer
		String type = (slides.length>0)?ViewerFactory.recomendViewerType(slides[0]):"qview";
		final String dir = ViewerFactory.getProperties().getProperty(type+".image.dir","");
		final Viewer viewer = ViewerFactory.getViewerInstance(type);
		viewer.setSize(new Dimension(500,500));
		
		JButton screen = new JButton(Icons.getIcon(Icons.SCREENSHOT,24));
		screen.setToolTipText("Capture slide snapshot");
		screen.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					JFileChooser chooser = new JFileChooser();
					chooser.setFileFilter(new ViewerHelper.JpegFileFilter());
					chooser.setPreferredSize(new Dimension(550, 350));
					int returnVal = chooser.showSaveDialog(null);
					
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						// select mode
						try {
							ViewerHelper.writeJpegImage(viewer.getSnapshot(), chooser.getSelectedFile());
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				} catch (java.security.AccessControlException ex) {
					JOptionPane.showMessageDialog(null, 
							"You do not have permission to save screenshots on local disk.",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		viewer.getViewerControlPanel().add(Box.createHorizontalGlue());
		viewer.getViewerControlPanel().add(screen);
		
		
		// init buttons
		JToolBar toolbar = new JToolBar();
		toolbar.setMinimumSize(new Dimension(0,0));
		ButtonGroup grp = new ButtonGroup();
		AbstractButton selected = null;
		for(int i=0;i<slides.length;i++){
			final String image = slides[i];
			String text = slides[i];
			// strip suffic and prefix
			if(slides.length > 1 && text.startsWith(name))
				text = text.substring(name.length()+1);
			if(text.lastIndexOf(".") > -1)
				text = text.substring(0,text.lastIndexOf("."));
			// create buttons
			AbstractButton bt = new JToggleButton(text);
			bt.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					JToggleButton bt = (JToggleButton) e.getSource();
					if(bt.isSelected()){
						try{
							viewer.openImage(dir+image);
						}catch(ViewerException ex){
							ex.printStackTrace();
						}
					}
				}
			});
			grp.add(bt);
			toolbar.add(bt);
			
			// select entry
			if(selected == null && (text.contains("HE") || slides.length == 1))
				selected = bt;
		}
		
		// create gui
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(toolbar,BorderLayout.NORTH);
		panel.add(viewer.getViewerPanel(),BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(500,600));
		
		edu.pitt.dbmi.domainbuilder.util.UIHelper.HTMLPanel text = new UIHelper.HTMLPanel();
		text.setEditable(false);
		//text.setPreferredSize(new Dimension(350,600));
		text.append(info);
		text.setCaretPosition(0);
		JScrollPane scroll = new JScrollPane(text);
		scroll.setPreferredSize(new Dimension(350,600));
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,panel,scroll);
		split.setResizeWeight(1);
		
		// create dialog
		JOptionPane op = new JOptionPane(split,JOptionPane.PLAIN_MESSAGE);
		JDialog d = op.createDialog(null,name);
		d.setModal(false);
		d.setResizable(true);
		d.pack();
		d.setVisible(true);
		
		// load image
		if(selected != null)
			selected.doClick();
		
		//JOptionPane.showMessageDialog(this,"Not Implemented");
	}
	*/
	
	
	/**
	 * rotate all markers for current image
	 *
	 */
	public void doShow() throws Exception {
		// iterate over slides
		for(SlideEntry e: caseEntry.getSlides()){
			viewer.openImage(e.getSlideName());
			
			// iterate over shapes
			for(ShapeEntry s: e.getAnnotations()){
				Annotation tm = s.getAnnotation(viewer.getAnnotationManager());
				viewer.getAnnotationManager().addAnnotation(tm);
			}
		}
	}
	
	/**
	 * rotate all markers for current image
	 *
	 */
	public void doRotate() throws Exception {
		// iterate over slides
		for(SlideEntry e: caseEntry.getSlides()){
			viewer.openImage(e.getSlideName());
			Dimension is = viewer.getImageSize();
			Rectangle vr = viewer.getViewRectangle();
			
			
			// iterate over shapes
			for(ShapeEntry s: e.getAnnotations()){
				Annotation tm = s.getAnnotation(viewer.getAnnotationManager());
				viewer.getAnnotationManager().addAnnotation(tm);
				
				ViewPosition vp = tm.getViewPosition();
				Rectangle r = tm.getBounds();
				int x = is.width - r.width - r.x;
				int y = is.height - r.height - r.y;
				// move shape to correct location
				tm.translate(x-r.x,y-r.y);
				r = tm.getBounds();
				
				// rotate shape
				AffineTransform af = new AffineTransform();
				af.rotate(Math.toRadians(180),r.getCenterX(),r.getCenterY());
				tm.transform(af);
							
				// transfer view coordinates
				x = is.width - vr.width - vp.x;
				y = is.height - vr.height - vp.y;
				tm.setViewPosition(new ViewPosition(x,y,vp.scale));
				
				// sync shapes back to instances
				//tm.notifyBoundsChange();
				
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		initialize();
		File dir = new File("/home/tseytlin/Output/SimTutor Cases/melanocytic/");
		//String [] files = new String [] {"AP_1018.case"};
		for(File f: dir.listFiles()){
			FlipCaseAnnotations flip = new FlipCaseAnnotations(f);
			flip.doShow();
			int r = JOptionPane.showConfirmDialog(frame,"Flip Case?");
			if(JOptionPane.YES_OPTION == r){
				flip.doRotate();
				Thread.sleep(2000);
			}else if(JOptionPane.CANCEL_OPTION == r)
				break;
		}

	}

}
