package edu.pitt.dbmi.misc;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.domainbuilder.beans.CaseEntry;
import edu.pitt.dbmi.domainbuilder.beans.ShapeEntry;
import edu.pitt.dbmi.domainbuilder.beans.SlideEntry;
import edu.pitt.dbmi.domainbuilder.util.OrderedMap;
import edu.pitt.dbmi.domainbuilder.util.TextHelper;
import edu.stanford.smi.protege.model.*;
import edu.stanford.smi.protege.util.URIUtilities;

/**
 * Batch converter of cases 
 * @author tseytlin
 */
public class CaseConverter implements ActionListener{
	private final String prf = "http://www.owl-ontologies.com/SlideTemplate.owl#";
	private JFrame frame;
	private JTextField input,output;
	private JList projectList;
	private JScrollPane scroll;
	private JProgressBar progress;
	private JButton cmd;
	
	/**
	 * present user w/ graphical UI
	 */
	public void buildGUI(){
		frame = new JFrame("CaseConverter");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setResizeWeight(1);
		frame.getContentPane().add(split,BorderLayout.CENTER);
		
		// input panel
		JPanel p1 = new JPanel();
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p1.setLayout(new BorderLayout());
		p1.setBorder(new TitledBorder("Input"));
		p1.setPreferredSize(new Dimension(500,250));
		// input text
		input = new JTextField(20);
		input.setToolTipText("Input directory");
		p.add(input,BorderLayout.CENTER);
		
		// input button
		JButton b1 = new JButton("Browse");
		b1.addActionListener(this);
		b1.setActionCommand("browse_input");
		p.add(b1,BorderLayout.EAST);
		p1.add(p,BorderLayout.NORTH);
		
		// input project list
		projectList = new JList();
		projectList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		projectList.setVisibleRowCount(0);
		scroll = new JScrollPane(projectList);
		p1.add(scroll,BorderLayout.CENTER);
		//frame.getContentPane().add(p1,BorderLayout.NORTH);
		split.setTopComponent(p1);
		
		// output panel
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		p2.setBorder(new TitledBorder("Output"));
		// input text
		output = new JTextField(20);
		output.setToolTipText("Output directory");
		p2.add(output,BorderLayout.CENTER);
		
		// input button
		JButton b2 = new JButton("Browse");
		b2.setActionCommand("browse_output");
		b2.addActionListener(this);
		p2.add(b2,BorderLayout.EAST);
		//frame.getContentPane().add(p2,BorderLayout.CENTER);
		split.setBottomComponent(p2);
		
		// progress panel
		JPanel p3 = new JPanel();
		p3.setLayout(new BorderLayout());
		p3.setBorder(new TitledBorder("Progress"));
		
		// progress bar
		progress = new JProgressBar();
		progress.setMinimum(0);
		progress.setStringPainted(true);
		progress.setPreferredSize(new Dimension(500,30));
		p3.add(progress,BorderLayout.NORTH);
		p3.add(new JLabel(" "),BorderLayout.CENTER);
		cmd = new JButton("Convert");
		cmd.setActionCommand("convert");
		cmd.addActionListener(this);
		p3.add(cmd,BorderLayout.SOUTH);
		frame.getContentPane().add(p3,BorderLayout.SOUTH);
		
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * actions
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("browse_input")){
			JFileChooser fc = new JFileChooser(new File(input.getText()));
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setFileFilter(new FileFilter(){
				public String getDescription(){
					return "Protege Project Files (*.pprj)";
				}
				public boolean accept(File file){
					return file.isDirectory() || file.getName().endsWith(".pprj");
				}
			});
			if(JFileChooser.APPROVE_OPTION == fc.showOpenDialog(frame)){
				File file = fc.getSelectedFile();
				// set text field
				if(file != null && !file.isDirectory()){
					file = file.getParentFile();
				}
				input.setText(file.getAbsolutePath());
				
				// add list of files to the list
				String [] files = file.list(new FilenameFilter(){
					public boolean accept(File f, String name){
						//return name.matches("[A-Za-z]+_\\d+.*\\.pprj");
						return name.matches(".*\\.pprj");
					}
				});
				// sort files
				Vector list = new Vector();
				Collections.addAll(list,files);
				Collections.sort(list);
				projectList.setListData(list);
			}
			
		}else if(cmd.equals("browse_output")){
			JFileChooser fc = new JFileChooser(new File(output.getText()));
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setFileFilter(new FileFilter(){
				public String getDescription(){
					return "Domain Builder Case Files (*.case)";
				}
				public boolean accept(File file){
					return file.isDirectory() || file.getName().endsWith(".case");
				}
			});
			if(JFileChooser.APPROVE_OPTION == fc.showOpenDialog(frame)){
				File file = fc.getSelectedFile();
				// set text field
				if(file != null && !file.isDirectory()){
					file = file.getParentFile();
				}
				output.setText(file.getAbsolutePath());
			}
		}else if(cmd.equals("convert")){
			Object [] selection = projectList.getSelectedValues();
			if(selection != null && selection.length > 0){
				File idir = new File(input.getText());
				File odir = new File(output.getText());
				// checks
				if(idir == null || !idir.isDirectory()){
					JOptionPane.showMessageDialog(frame,"You must enter valid input directory!");
					return;
				}
				if(odir == null || !odir.isDirectory()){
					JOptionPane.showMessageDialog(frame,"You must enter valid output directory!");
					return;
				}
				
				//do conversion
				convertFiles(idir, selection , odir);
				
			}else{
				JOptionPane.showMessageDialog(frame,"No input project files selected!");
			}
		}
	}
	
	/**
	 * Convert files
	 * @param idir
	 * @param names
	 * @param odir
	 */
	public void convertFiles(File idir, Object [] names, File odir){
		final File indir = idir;
		final Object [] fnames = names;
		final File outdir = odir;
		Thread t = new Thread(new Runnable(){
			public void run(){
				// pre op
				cmd.setEnabled(false);
				progress.setMaximum(fnames.length);
				progress.setString("Please Wait ...");
				for(int i=0;i<fnames.length;i++){
					convertFile(indir,(String)fnames[i],outdir);
					//progress bar
					progress.setString((String)fnames[i]);
					progress.setValue(i+1);
				}
				
				//post op
				cmd.setEnabled(true);
				JOptionPane.showMessageDialog(frame,"All Done!");
			}
		});
		t.start();
	}
	
	/**
	 * Convert files
	 * @param idir
	 * @param names
	 * @param odir
	 */
	public void convertFile(File idir, String name, File odir){
		String infile = idir.getAbsolutePath()+File.separator+name;
		// strip suffix
		int x = name.lastIndexOf(".");
		if(x > -1)
			name = name.substring(0,x);
		
		// load input projet
		List errors = new ArrayList();
		Project inProject = Project.loadProjectFromFile(infile, errors);
		if (!errors.isEmpty()) {
			for (Iterator i = errors.iterator(); i.hasNext();)
				System.out.println(i.next().toString());
			//return;
		}
		
		// get KBs
		KnowledgeBase ikb = inProject.getKnowledgeBase();
		
		// out
		System.out.println("Converting file "+name);
		//System.out.println(ikb.getRootCls().getDirectSubclasses());
		
		// get report info
		Instance report = getSingleInstance(ikb.getCls(prf+"REPORT"));
		String preOp   = TextHelper.toString(report.getOwnSlotValue(ikb.getSlot(prf+"pre-op_diagnosis")));
		String postOp  = TextHelper.toString(report.getOwnSlotValue(ikb.getSlot(prf+"post-op_diagnosis")));
		String history = TextHelper.toString( report.getOwnSlotValue(ikb.getSlot(prf+"patient_history")));
		String expertDx  = TextHelper.toString( report.getOwnSlotValue(ikb.getSlot(prf+"expert_diagnosis")));
		String comment = TextHelper.toString(report.getOwnSlotValue(ikb.getSlot(prf+"comment")));
		String exportRt  = TextHelper.toString( report.getOwnSlotValue(ikb.getSlot(prf+"expert_report")));
		String gross   = TextHelper.toString( report.getOwnSlotValue(ikb.getSlot(prf+"gross_description")));
		// build report text
		String reportText = 
			"PATIENT HISTORY:\n"+history+"\n\nFINAL DIAGNOSIS:\n"+expertDx+"\n\n"+
			"COMMENT:\n"+comment+"\n\nGROSS DESCRIPTION:\n"+gross;
		
		// use export report if it looks more complete
		if(exportRt != null && exportRt.length() > 0 && expertDx == null)
			reportText = exportRt;
		
		// get slide information
		OrderedMap<String,SlideEntry> slides = new OrderedMap<String,SlideEntry>();
		for(Instance slide: ikb.getCls(prf+"SLIDE").getDirectInstances()){
			Map info = new HashMap();
			// iterate over fields
			for(Slot s : slide.getOwnSlots()){
				String key = s.getName();
				if(key.startsWith(prf))
					key = key.substring(prf.length());
				Object obj = slide.getOwnSlotValue(s);
				if(obj instanceof Instance)
					obj = ((Instance)obj).getBrowserText();
				info.put(key,obj);
			}
			// create slide entry
			SlideEntry entry = SlideEntry.createSlideEntry(info);
			slides.put(entry.getSlideName(),entry);
		}
		
		// get all shapes
    	final Map<String,ShapeEntry> shapes = new HashMap<String,ShapeEntry>();
		for(Instance shape: ikb.getCls(prf+"SHAPE").getDirectInstances()){
    		// get info from old shape
    		ShapeEntry shapeEntry = createShapeEntry(shape);
    		// add tag 
    		String str = (String) shape.getOwnSlotValue(ikb.getSlot("http://www.w3.org/2000/01/rdf-schema#comment"));
    		shapeEntry.setTag((str != null)?str:"");
    		
    		// add to slides
    		if(slides.containsKey(shapeEntry.getImage())){
    			SlideEntry slide = slides.get(shapeEntry.getImage());
    			shapeEntry.setSlide(slide);
    			slide.addAnnotation(shapeEntry);
    		}
    		shapes.put(shapeEntry.getName(),shapeEntry);
    	}
		
		// add tags to shapes bases on observables
		// transfer LOCATED_OBSERVABLEs
    	String [][] clsNames = {{"LOCATED_DIAGNOSTIC","has_diagnostic"},
    							{"LOCATED_PROGNOSTIC","has_prognostic"}};
    	// iterate over located
    	for(int j=0;j<clsNames.length;j++){
	    	for(Instance inst: ikb.getCls(prf+clsNames[j][0]).getDirectInstances()){
	    		//get info from old observable
	    		Instance obs = (Instance) inst.getOwnSlotValue(ikb.getSlot(prf+clsNames[j][1]));
	    		Instance an = (Instance) inst.getOwnSlotValue(ikb.getSlot(prf+"has_annotation"));
	    		Instance op = (Instance) inst.getOwnSlotValue(ikb.getSlot(prf+"has_optimal_location"));
	    		Collection locations = inst.getOwnSlotValues(ikb.getSlot(prf+"has_location"));
	    		String tag = (obs != null)?obs.getBrowserText():null;
	    		
	    		// add tags
	    		if(tag != null){
	    			if(an != null)
	    				shapes.get(getName(an)).addTag(tag);
	    			if(op != null)
	    				shapes.get(getName(op)).addTag(tag);
	    			for(Object obj: locations){
	    				shapes.get(getName((Instance) obj)).addTag(tag);
	    			}
	    		}
	    	}
    	}
    			
		
		// create case entry
		CaseEntry outCase = new CaseEntry(name);
		outCase.setReport(reportText);
		
		// add slides
	    for(SlideEntry e: slides.getValues())
	    	outCase.addSlide(e);
	    
	    try{
		    FileOutputStream os = new FileOutputStream(new File(odir,name+".case"));
		    outCase.save(os);
		    /*
		    new AnnotationManager(){;
				public List<ShapeEntry> getAnnotations(String name) {
					return Collections.EMPTY_LIST;
				}
				public List<ShapeEntry> getAnnotations() {
					return new ArrayList<ShapeEntry>(shapes.values());
				}
				public void setAnnotationNumber(int x) {
					//NOOP
				}
		    });
		    */
	    }catch(Exception ex){
	    	ex.printStackTrace();
	    }
    	// cleanup
    	inProject.dispose();

	}
	
	private String getName(Instance inst){
		return inst.getBrowserText();
	}
	
	
	/**
	 * Create ShapeEntry from Protege shape instance
	 */
	public ShapeEntry createShapeEntry(Instance inst){
		KnowledgeBase kb = inst.getKnowledgeBase();		
		// add instance to Protege
		ShapeEntry entry = new ShapeEntry();
		
		entry.setName(getName(inst));
		entry.setType((String) inst.getOwnSlotValue(kb.getSlot(prf+"type")));
		entry.setTag((String) inst.getOwnSlotValue(kb.getSlot(prf+"tag")));
		entry.setViewZoom(((Float) inst.getOwnSlotValue(kb.getSlot(prf+"zoom"))).floatValue());
		entry.setViewX(((Integer) inst.getOwnSlotValue(kb.getSlot(prf+"viewx"))).intValue());
		entry.setViewY(((Integer) inst.getOwnSlotValue(kb.getSlot(prf+"viewy"))).intValue());
		entry.setColorName((String)inst.getOwnSlotValue(kb.getSlot(prf+"color")));
		entry.setColor(pickColor(entry.getColorName()));
		
		// get slide name
		Instance slide = (Instance) inst.getOwnSlotValue(kb.getSlot(prf+"has_slide"));
		if(slide != null)
			entry.setImage(""+slide.getOwnSlotValue(kb.getSlot(prf+"slide_name")));
	
		
		int xst=0,yst=0,img_w=0,img_h=0;
		Integer xstart = (Integer)inst.getOwnSlotValue(kb.getSlot(prf+"xstart"));
		Integer ystart = (Integer)inst.getOwnSlotValue(kb.getSlot(prf+"ystart"));
		Integer xend = (Integer)inst.getOwnSlotValue(kb.getSlot(prf+"xend"));
		Integer yend = (Integer)inst.getOwnSlotValue(kb.getSlot(prf+"yend"));
		Integer width = (Integer)inst.getOwnSlotValue(kb.getSlot(prf+"width"));
		Integer height = (Integer)inst.getOwnSlotValue(kb.getSlot(prf+"height"));
		
		if(xstart != null){
			xst = xstart.intValue();
			entry.setXStart(xst);
		}
		if(ystart != null){
			yst = ystart.intValue();
			entry.setYStart(yst);
		}
		if(width != null){
			img_w = width.intValue();
			entry.setWidth(img_w);
		}
		if(height != null){
			img_h = height.intValue();
			entry.setHeight(img_h);
		}
		if(xend != null)
			entry.setXEnd(xend.intValue());
		if(yend != null)
			entry.setYEnd(yend.intValue());

		//if(entry.getType().equalsIgnoreCase("Polygon")){
		String xpoints = (String) inst.getOwnSlotValue(kb.getSlot(prf+"xpoints"));
		String ypoints = (String) inst.getOwnSlotValue(kb.getSlot(prf+"ypoints"));
		if(xpoints != null && ypoints != null){
			setXYPoints(entry,xpoints,ypoints);
		}
		//}
		
		return entry;
	}
	
	private void setXYPoints(ShapeEntry entry,String XPoints, String YPoints){
		ArrayList  xp = new ArrayList();
		ArrayList  yp = new ArrayList();
	
		StringTokenizer xt = new StringTokenizer(XPoints, " ");
		StringTokenizer yt = new StringTokenizer(YPoints, " ");
	
		// get individual numbers
		while(xt.hasMoreTokens()){
			xp.add(xt.nextToken());
			yp.add(yt.nextToken());
		}
		
		// this should not happen
		if(xp.size() != yp.size()){
			System.err.println("ERROR: We have a problem with X and Y points");
			return;
		}
		
		// create int arrays
		int [] xPoints = new int [xp.size()];
		int [] yPoints = new int [yp.size()];
		
		// fill int arrays
		for(int i=0;i<xPoints.length;i++){
			try{
				xPoints[i] = Integer.parseInt((String)xp.get(i));
				yPoints[i] = Integer.parseInt((String)yp.get(i));
			}catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		// clean up
		xp = yp =null;
		
		// set
		entry.setXPoints(xPoints);
		entry.setYPoints(yPoints);
	}
	
	// gets a color object from string representation
	private Color pickColor(String color){
		if(color == null)
			return null;
		
		Color c = null;
		String [] colors = new String []
		{"black","blue","cyan","darkGray","gray","green","lightGray","magenta","orange","pink","red","white","yellow"};	
		
		for(int i=0;i<colors.length;i++){
			if(color.equalsIgnoreCase(colors[i])){
				try{
					c =  (Color) Color.class.getField(colors[i]).get(null);
					break;
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
		return c;
	}	
	
	
	/**
	 * Save local copy of a protege file
	 * @param info
	 */
	private void saveLocalCopy(Project project, File dir, String name){
		// strip stuff from name
		Pattern p = Pattern.compile("([A-Za-z]+_\\d+).*\\.[A-Za-z0-9]+");
		Matcher m = p.matcher(name);
		if(m.matches())
			name = m.group(1);
		
		//set project URI
		URI projectURI = dir.toURI();
		
		// redo includes as a new protege HACK
		//System.out.println(project.getProjectName()+" : "+project.getProjectURI());
		ArrayList includedProjects = new ArrayList();
		Collection c = project.getDirectIncludedProjectURIs();
		for (Iterator i = c.iterator(); i.hasNext();) {
			includedProjects.add(URIUtilities.relativize(projectURI, (URI) i.next()));
		}

		// set project name
		project.setProjectFilePath(dir +File.separator+name+".pprj");
		project.getSources().setString("owl_file_name",name+".owl");
		//project.getSources().setString("classes_file_name",slide.getProjectClassesName());
		//project.getSources().setString("instances_file_name",slide.getProjectInstancesName());
		project.setDirectIncludedProjectURIs(includedProjects);
	}
	
	
	private Instance getSingleInstance(Cls cls){
		for(Instance inst: cls.getDirectInstances())
			return inst;
		return null;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CaseConverter c = new CaseConverter();
		c.buildGUI();
	}

}
