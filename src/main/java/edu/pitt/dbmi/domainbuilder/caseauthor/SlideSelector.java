package edu.pitt.dbmi.domainbuilder.caseauthor;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import edu.pitt.dbmi.domainbuilder.DomainBuilder;
import edu.pitt.dbmi.domainbuilder.beans.*;
import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.dbmi.domainbuilder.widgets.DynamicSelectionPanel;
import edu.pitt.dbmi.domainbuilder.widgets.EntryChooser;
import edu.pitt.slideviewer.ImageProperties;
import edu.pitt.slideviewer.ImageSelectionPanel;
import edu.pitt.slideviewer.ViewerFactory;

public class SlideSelector extends JPanel implements ActionListener, DropTargetListener {
	private JList slideList;
	//private DynamicSelectionPanel selectionPanel;
	private ImageSelectionPanel selectionPanel;
	//private URL listServlet;
	private CaseAuthor caseAuthor;
	private JToolBar toolbar;
	private JPopupMenu popup,epopup;
	private int selectedIndex;
	private boolean readOnly;
	
	/**
	 * create new slide selector
	 */
	public SlideSelector(CaseAuthor author){
		super();
		this.caseAuthor = author;
		setLayout(new BorderLayout());
		slideList = new JList(); //author.getCaseEntry().createSlideListModel()
		slideList.setCellRenderer(new IconListCellRenderer(slideList));
		slideList.setDragEnabled(true);
		slideList.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				selectedIndex = -1;
				if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
					selectedIndex = UIHelper.getIndexForLocation(slideList,e.getPoint());
					getPopupMenu(selectedIndex).show(slideList,e.getX(),e.getY());
				}
			}
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() > 1){
					doProperties();
				}
			}
		});
		new DropTarget(slideList,this);
		add(getToolBar(),BorderLayout.NORTH);
		add(new JScrollPane(slideList),BorderLayout.CENTER);
		setPreferredSize(new Dimension(120,120));
		
		// set servlet
		/*
		Properties p = DomainBuilder.getParameters();
		try{
			
			listServlet = new URL(p.getProperty("image.list.server.url"));
		}catch(Exception ex){
			System.out.println(p.getProperty("image.list.server.url"));
			ex.printStackTrace();
		}*/
		
		// add listener
		slideList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				if(!e.getValueIsAdjusting()){
					SlideEntry slide = (SlideEntry) slideList.getSelectedValue();
					caseAuthor.openSlide(slide);
				}
			}
		});
		setEnabled(false);
	}
	
	public void openSlide(Object obj){
		slideList.setSelectedValue(obj,true);
	}
	
	
	public void openNextSlide(){
		int i = slideList.getSelectedIndex()+1;
		if(i< slideList.getModel().getSize())
			slideList.setSelectedIndex(i);
	}
	
	public void openPreviousSlide(){
		int i = slideList.getSelectedIndex()-1;
		if(i >= 0)
			slideList.setSelectedIndex(i);
	}
	
	/**
	 * set case entry 
	 * @param e
	 */
	public void reset(){
		slideList.setModel(caseAuthor.getCaseEntry().createSlideListModel());
		setEnabled(true);
		
		// load existing slides thumbnails
		for(SlideEntry slide: caseAuthor.getCaseEntry().getSlides()){
			if(slide.getThumbnail() == null){
				try {
					// check viewer settings 
					if(!ViewerFactory.getPropertyLocation().equals(slide.getConfigurationName()) &&
						ViewerFactory.getPropertyLocations().contains(slide.getConfigurationName())){
						ViewerFactory.setPropertyLocation(slide.getConfigurationName());
					}
					
					//ImageProperties im = caseAuthor.getViewer().getImageProperties();
					ImageProperties im = ViewerFactory.getImageProperties(slide.getSlidePath());
					if(im != null && im.getThumbnail() != null){
						slide.setThumbnail(im.getThumbnail());
						slide.setImageSize(im.getImageSize());
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			slideList.repaint();
		}
		slideList.setSelectedIndex(0);
	}

	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(){
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(UIHelper.createButton("Add Slide","Add Whole Slide Image",Icons.PLUS,16,this));
		toolbar.add(UIHelper.createButton("Remove Slide","Remove Whole Slide Image",Icons.MINUS,16,this));
		toolbar.add(Box.createHorizontalGlue());
		toolbar.add(UIHelper.createButton("Properties","Edit Slide Properties",Icons.PROPERTIES,16,this));
		return toolbar;
	}
	
	
	/**
	 * get toolbar
	 * @return
	 */
	private JToolBar getToolBar(){
		if(toolbar == null){
			toolbar = createToolBar();
			setReadOnly(readOnly);
		}
		return toolbar;
	}
	
	/**
	 * actions handler
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equals("Add Slide")){
			doAdd();
		}else if(cmd.equals("Remove Slide")){
			doRemove();
		}else if(cmd.equals("Properties")){
			doProperties();
		}	
	}
	
	private JPopupMenu getPopupMenu(int i){
		if(popup == null){
			popup = new JPopupMenu();
			popup.add(UIHelper.createMenuItem("Add Slide","Add Whole Slide Image",Icons.PLUS,this));
			popup.add(UIHelper.createMenuItem("Remove Slide","Remove Whole Slide Image",Icons.MINUS,this));
			popup.addSeparator();
			popup.add(UIHelper.createMenuItem("Properties","Edit Slide Properties",Icons.PROPERTIES,this));
			setReadOnly(readOnly);
		}
		if(epopup == null){
			epopup = new JPopupMenu();
			epopup.add(UIHelper.createMenuItem("Add Slide","Add Whole Slide Image",Icons.PLUS,this));
			setReadOnly(readOnly);
		}
		return (i > -1)?popup:epopup;
	}
	
	
	/**
	 * add new slide
	 */
	private void doAdd(){
		// create selection panel
		if(selectionPanel == null){
			/*
			selectionPanel = new DynamicSelectionPanel();
			selectionPanel.setSelectionMode(EntryChooser.MULTIPLE_SELECTION);
			selectionPanel.setLabel("Select a Virtual Slide to load");
			*/
			selectionPanel = new ImageSelectionPanel();
		}
		
		// query server for available images
		String [] names = null;
		/*
		List<String> images = ViewerFactory.getImageList();
		Frame frame = JOptionPane.getFrameForComponent(this);
		if(images != null){
			selectionPanel.setSelectableObjects(images);
			selectionPanel.setOwner(frame);
			selectionPanel.showChooserDialog();
			if(selectionPanel.isSelected()){
				names = selectionPanel.getSelectedObjects();
			}else
				return;
		}else{
			names = new String [1];
			names[0] = JOptionPane.showInputDialog(frame, "Enter image name");
		}
		*/
		Frame frame = JOptionPane.getFrameForComponent(this);
		if(selectionPanel.showDialog(frame)){
			names = selectionPanel.getSelectedImages();
		}else{
			return;
		}
		
		//check if such cases are already in DB
		if(caseAuthor.checkCase(names)){
			// add all slides
			final Object [] params = names;
			(new Thread(new Runnable(){
				public void run(){
					addSlides(params);
				}
			})).start();
		}
		
		// set modified flag
		caseAuthor.setCaseModified();
	}
	
	/**
	 * add slides
	 * @param names
	 */
	private void addSlides(Object [] names){
		for(Object name: names){
			// create slide entry  and add it to list
			SlideEntry slide = new SlideEntry(""+name);
			
			// setup configuration values
			String place = selectionPanel.getSelectedLocation();
			slide.setConfigurationName(place);
			slide.setConfigurationURL(""+OntologyHelper.getConfigFile(place));
			
			
			// open it in viewer
			// load image first, so that we can reuse its thumbnail
			caseAuthor.openSlide(slide);
			
			MutableListModel model = (MutableListModel) slideList.getModel();
			if( !model.containsElement(slide)){
				slide.setLoadedSlide(true);
				if(slide.getThumbnail() == null){
					try {
						ImageProperties im = caseAuthor.getViewer().getImageProperties();
						//ViewerFactory.getImageProperties(slide.getSlideName());
						if(im != null && im.getThumbnail() != null){
							slide.setThumbnail(im.getThumbnail());
							slide.setImageSize(im.getImageSize());
						}
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
				model.addElement(slide);
			}
			slideList.setSelectedValue(slide,true);
			slideList.repaint();
		}
	}
	
	
	/**
	 * remove slide
	 */
	public void doRemove(){
		MutableListModel model = (MutableListModel) slideList.getModel();
		SlideEntry entry = getSelectedSlide();
		if(entry != null){
			// prompt user
			int r = JOptionPane.showConfirmDialog(DomainBuilder.getInstance().getFrame(),
					"<html>Are you sure you want to remove <font color=red>"+entry.getName()+"</font> slide along with all associated annotations?",
					"Confirm", JOptionPane.YES_NO_OPTION);
			
			if(JOptionPane.YES_OPTION == r){
				// remove all annotations associated with this slide
				caseAuthor.getShapeSelector().doRemove(entry.getAnnotations());
				
				// remove slide from model
				model.removeElement(entry);
				if(entry.getSlidePath().equals(caseAuthor.getViewer().getImage()))
					caseAuthor.getViewer().closeImage();
				
				// set modified flag
				caseAuthor.setCaseModified();
			}
		}
	}
	
	/**
	 * get selected slide
	 * @return
	 */
	private SlideEntry getSelectedSlide(){
		if(selectedIndex > -1)
			return (SlideEntry) slideList.getModel().getElementAt(selectedIndex);
		return (SlideEntry) slideList.getSelectedValue();
	}
	
	
	/**
	 * open properties dialog
	 */
	public void doProperties(){
		// create setter component
		SlideEntry entry = getSelectedSlide();
		if(entry == null)
			return;
		String [] params = new String [] {"Stain","Part","Block","Level"};
		JComponent comp = UIHelper.createBeanSetterPanel(entry,params);
		Frame frame = JOptionPane.getFrameForComponent(this);
		JOptionPane.showMessageDialog(frame,comp,"Properties",JOptionPane.PLAIN_MESSAGE);
		// redraw text on icon
		entry.setText(null);
		slideList.repaint();
		
		// set modified flag
		caseAuthor.setCaseModified();
	}
	
	/**
	 * set panel to be editable
	 * enable/disable buttons
	 * @param b
	 */
	public void setEnabled(boolean b){
		for(int i=0;i<toolbar.getComponentCount();i++){
			toolbar.getComponent(i).setEnabled(b);
		}
		slideList.setEnabled(b);
	}
	
	
	/**
	 * This is a hack to get Java 1.6 to work
	 * @author tseytlin
	 *
	 */
	private class IconListCellRenderer extends DefaultListCellRenderer.UIResource{
		private JList list;
		public IconListCellRenderer(JList list){
			super();
			this.list = list;
		}
		public Container getParent(){
			return list;
		}
	}


	public void dragEnter(DropTargetDragEvent dtde) {}
	public void dragExit(DropTargetEvent dte) {}
	public void dragOver(DropTargetDragEvent dtde) {}
	public void dropActionChanged(DropTargetDragEvent dtde) {}

	public void drop(DropTargetDropEvent dtde) {
		if(readOnly)
			return;
		
		Object obj = slideList.getSelectedValue();
		if(obj != null && obj instanceof SlideEntry){
			int i = slideList.locationToIndex(dtde.getLocation());
			if(i > -1){
				MutableListModel model = (MutableListModel) slideList.getModel();
				model.removeElement(obj);
				model.insertElementAt(obj,i);
				slideList.revalidate();
				slideList.repaint();
			}
		}
	}

	
	/**
	 * set read only status
	 * @param b
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		String [] ex = new String [] {"properties"};
		// enable/disable toolbar buttons
		if(toolbar != null){
			UIHelper.setEnabled(toolbar,ex,!b);
		}
		// enable/disable popup buttons
		if(popup != null){
			UIHelper.setEnabled(popup,ex,!b);
		}
		if(epopup != null){
			UIHelper.setEnabled(epopup,new String [0],!b);
		}
		slideList.revalidate();
	}


}
