package edu.pitt.dbmi.misc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;

import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.CaseEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ShapeEntry;
import edu.pitt.dbmi.domainbuilder.beans.SlideEntry;
import edu.pitt.dbmi.domainbuilder.knowledge.OntologySynchronizer;
import edu.pitt.dbmi.domainbuilder.util.Communicator;
import edu.pitt.dbmi.domainbuilder.util.FileRepository;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;
import edu.pitt.dbmi.domainbuilder.widgets.DomainSelectorPanel;
import edu.pitt.ontology.*;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.ViewerHelper;
import edu.pitt.slideviewer.markers.Annotation;



/**
 * create example images for a given case
 * @author tseytlin
 *
 */
public class ExampleImageExtractor {
	private final Dimension size = new Dimension(500,500);
	private IOntology ontology;
	private JPanel viewerContainer;
	private Properties params;
	private Viewer viewer;
	private File imageDir;
	private Map<String,List<ShapeEntry>> annotationMap;
	private Set<String> visitedExample;
	
	public ExampleImageExtractor(IOntology ont) throws Exception {
		this.ontology = ont;
		
		imageDir = new File(System.getProperty("user.home")+"/Output","exampleImages");
		if(!imageDir.exists())
			imageDir.mkdirs();
		
		visitedExample = new HashSet<String>();
		
		params = new Properties();
		params.load(new URL("http://slidetutor.upmc.edu/domainbuilder/config/PITT/DomainBuilder.conf").openStream());
		ViewerFactory.setProperties(params);
		
		// create viewer
		viewerContainer = new JPanel();
		viewerContainer.setLayout(new BorderLayout());
		
		switchViewer("aperio");
		
		// display viewer
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(viewerContainer);
		frame.pack();
		frame.setVisible(true);
		
	}
	
	/**
	 * Replace a current viewer w/ a viewer of different type
	 * 
	 * @param type
	 */

	private void switchViewer(String type) {
		String dir = params.getProperty(type + ".image.dir");
		params.setProperty("image.server.type", type);
		if (dir != null)
			params.setProperty("image.dir", dir);
		else
			System.err.println("Error: image dir " + dir + " not found for type " + type);

		// create virtual microscope panel
		Viewer v = ViewerFactory.getViewerInstance(params.getProperty("image.server.type"));
		v.setSize(size.width, size.height);
		// remove component
		if(viewer != null)
			viewerContainer.remove(this.viewer.getViewerPanel());
		viewer = v;

		// replace component
		viewerContainer.add(v.getViewerPanel(),0);
		viewerContainer.revalidate();
	}
	
	
	
	public void process() throws Exception{
		// get case base ontology
		IOntology instancesOntology = getCaseBase(ontology);
		// go over individual cases
		for(IInstance inst: instancesOntology.getClass(CASES).getDirectInstances()){
			// skip questions
			if(inst.getName().matches(".*P[FLD]_?\\d+"))
				continue;
			
			// process each case
			processCase(inst);
		}
		
		// upload all example images
		System.out.println("Uploading all example images ..");
		OntologySynchronizer.getInstance().run();
		
		// saving ontology
		System.out.println("Saving ontology "+ontology.getName()+" ...");
		ontology.save();
		
		// publishing ontology
		File fc = new File(getLocalRepositoryFolder(),ontology.getURI().getPath());
		System.out.println("Publishing ontology "+ontology.getName()+" ...");
		UIHelper.upload(fc);
		
		System.out.println("all done");
		System.exit(0);
	}
	
	/**
	 * process each individual case
	 * @param inst
	 */
	private void processCase(IInstance inst)  throws Exception{
		// clear stuff
		annotationMap = null;
		
		// load case entry
		CaseEntry caseEntry = new CaseEntry(inst);
		URL u = new URL(DEFAULT_HOST_URL+getCasePath(ontology)+"/"+caseEntry.getName()+CASE_SUFFIX);
		caseEntry.load(u.openStream(),false);
		
		SlideEntry slide = caseEntry.getSlides().iterator().next();
		if(slide == null)
			return;
		
		// open case
		String type = params.getProperty("image.server.type");
		String rtype = ViewerFactory.recomendViewerType(slide.getSlideName());
		if (rtype == null) {
			JOptionPane.showMessageDialog(null, slide.getSlidePath() + " is not of supported type", "Error",JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (!type.equals(rtype))
			switchViewer(rtype);
		
		// look at tested cases and complete cases
		if(STATUS_TESTED.equals(caseEntry.getStatus()) || STATUS_COMPLETE.equals(caseEntry.getStatus())){
			// go over findings
			for(ConceptEntry entry: caseEntry.getConceptEntries(DIAGNOSTIC_FEATURES)){
				IClass fn = getFeature(entry.getConceptClass());
				if(fn.getPropertyValues(ontology.getProperty(HAS_EXAMPLE)).length == 0){
					try{
						// load image
						viewer.openImage(getImageDir() + slide.getSlidePath());
						
						// process example
						processExample(caseEntry,entry,fn);
					}catch (ViewerException ex){
						ex.printStackTrace();
					}
				}
			}
		}
	}
	private String getImageDir() {
		String type = params.getProperty("image.server.type");
		if (params.getProperty(type + ".image.dir") != null)
			return params.getProperty(type + ".image.dir");
		return params.getProperty("image.dir");
	}
		
	/**
	 * process example
	 * @param entry
	 */
	private void processExample(CaseEntry casetEntry,ConceptEntry entry,IClass feature) throws Exception {
		if(entry.isAbsent() || visitedExample.contains(feature.getName()))
			return;
		
		ShapeEntry [] examples = getExamples(casetEntry, entry, null);
		System.out.println("Processing example image "+feature.getName()+" ("+entry.getName()+") from "+casetEntry.getName()+".. "+Arrays.toString(examples));
		if(examples.length == 0)
			return;
		
		// add example image
		ShapeEntry s = examples[0];
		Annotation a = s.getAnnotation(viewer.getAnnotationManager());
		a.setViewer(viewer);
		viewer.getAnnotationManager().addAnnotation(a);
		viewer.setCenterPosition(getCenterPosition(s,viewer));
		
		// wait
		UIHelper.sleep(3000);
		
		// take a photo
		Image img = viewer.getSnapshot(Viewer.IMG_ALL_MARKERS);
		File file = new File(imageDir,feature.getName()+".jpg");
		ViewerHelper.writeJpegImage(img, file);
		
		// now do a transfer of image and set the property
		File f = saveExampleImage(file,feature);
		
		// add as a property
		feature.addPropertyValue(ontology.getProperty(HAS_EXAMPLE),f.getName());
		
		// 
		viewer.getAnnotationManager().removeAnnotation(a);
		visitedExample.add(feature.getName());
	}
	
    /**
     * get annotation map for this case
     * @return
     */
    private Map<String,List<ShapeEntry>> getAnnotationMap(CaseEntry entry){
    	if(annotationMap == null){
    		annotationMap = new HashMap<String, List<ShapeEntry>>();
    		for(SlideEntry slide: entry.getSlides()){
    			for(ShapeEntry shape: slide.getAnnotations()){
    				annotationMap.put(shape.getName(),Collections.singletonList(shape));
    				for(String tag : shape.getTags()){
    					List<ShapeEntry> list = annotationMap.get(tag);
    					if(list == null){
    						list = new ArrayList<ShapeEntry>();
    						annotationMap.put(tag,list);
    					}
    					list.add(shape);
    				}
    			}
    		}
    	}
    	return annotationMap;
    }
	
    /**
	 * return a center position of a shape
	 * if shape is a polygon this will use 
	 * polygon's centroid
	 * the scale is shape's scale factor
	 * @return
	 */
	public ViewPosition getCenterPosition(ShapeEntry s,Viewer viewer){
		ViewPosition p = s.getCenterPosition();
		
		boolean arrow = s.getType().equalsIgnoreCase("arrow");	
		
		// calculate the best zoom
		Dimension d = viewer.getSize();
		Rectangle r = s.getShape().getBounds();
		
		
		// dow something special for arrows
		if(arrow){
			p.x = s.getXStart();
			p.y = s.getYStart();
			r.width  = r.width *2;
			r.height = r.height *2;
		}
		
		double zw = d.getWidth()/r.width;
		double zh = d.getHeight()/r.height;
		double z = Math.min(zw,zh);
		
		// adjust based on what is valid
		if(viewer.getScalePolicy() != null){
			z = viewer.getScalePolicy().getValidScale(z);
		}
		
		p.scale = z;
		
	
		
		return p;
	}
	
    /**
     * get all location from a concept entry that is closest to a 
     * given rectangle
     * @param concept
     * @param r
     * @return
     */
    public ShapeEntry [] getExamples(CaseEntry caseEntry, ConceptEntry concept, String image){
    	if(concept == null)
    		return new ShapeEntry [0];
    	
    	List<ShapeEntry> shapes = new ArrayList<ShapeEntry>();
    	Map<String,List<ShapeEntry>> map = getAnnotationMap(caseEntry);
    	
    	
     	// get list of locations, if
    	Collection<String> locations = concept.getExamples();
    	/*
    	if(locations.isEmpty()){
    		locations = new ArrayList<String>();
    		// try to find equivalent concept in case
    		for(ConceptEntry e: getMatchingFindings(this,concept)){
    			locations.addAll(e.getExamples());
    		}
    	}
    	*/
    	
    	// go over all locations
    	for(String loc: locations){
    		if(map.containsKey(loc)){
    			for(ShapeEntry e : map.get(loc)){
    				if(image == null || image.contains(e.getImage()))
    					shapes.add(e);
    			}
    		}
    	}
    	
    	return shapes.toArray(new ShapeEntry [0]);
    }
    

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		URL u = new URL("http://slidetutor.upmc.edu/domainbuilder/servlet/FileManagerServlet");
		IRepository r = new FileRepository(u);
	
		// init parameters
		DomainBuilder.setParameter("file.manager.server.url",u.toString());
		DomainBuilder.setParameter("repository.folder",".ontologyRepository");
		DomainBuilder.setParameter("repository.username","eugene");
		DomainBuilder.setParameter("repository.password","eugene");
		DomainBuilder.setParameter("repository.institution","PITT");
		
		// prompt for domain
		DomainSelectorPanel domainManager = new DomainSelectorPanel(r,true);
		domainManager.showChooserDialog();
		if(domainManager.isSelected()){
			
			//authenticate w/ domainbuilder
			if(!Communicator.authenticateWebsite(DomainBuilder.getParameters())){
				System.err.println("Authentication Failed!");
			}
			
			// do update
			ExampleImageExtractor eie = new ExampleImageExtractor((IOntology)domainManager.getSelectedObject());
			eie.process();
		}
	}
}
