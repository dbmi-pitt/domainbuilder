package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

import static edu.pitt.dbmi.domainbuilder.util.OntologyHelper.*;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.*;
import edu.pitt.ontology.protege.ProtegeRepository;
import edu.pitt.slideviewer.Viewer;
import edu.pitt.slideviewer.ViewerException;
import edu.pitt.slideviewer.ViewerFactory;
import edu.pitt.slideviewer.ViewerHelper;

public class CaseSelectorPanel extends JPanel implements EntryChooser, ListSelectionListener  {
	private final String ALL = "ALL DIAGNOSIS ...";
	private Frame frame;
	private boolean ok;
	private JList diagnoses, cases;
	private UIHelper.HTMLPanel report;
	private JPanel previewPanel;
	private JTextField input;
	private ViewerHelper.FilterDocument filter;
	private IOntology o;
	private JToolBar slideBar;
	private boolean block;
	
	public CaseSelectorPanel(IOntology ont){
		super();
		o = ont;
		setLayout(new BorderLayout());
		diagnoses = new JList();
		diagnoses.addListSelectionListener(this);
		diagnoses.setVisibleRowCount(15);
		cases = new JList();
		cases.setVisibleRowCount(15);
		cases.addListSelectionListener(this);
		cases.setCellRenderer(new DefaultListCellRenderer(){
			private final Color green = Color.GREEN.darker().darker();
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean s,boolean f) {
				JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, s,f);
				if(value instanceof IInstance){
					IInstance i = (IInstance) value;
					String status = ""+i.getPropertyValue(i.getOntology().getProperty(HAS_STATUS));
					if(STATUS_INCOMPLETE.equals(status))
						c.setForeground(Color.RED);
					else if(STATUS_COMPLETE.equals(status))
						c.setForeground(Color.BLUE);
					else if(STATUS_TESTED.equals(status))
						c.setForeground(green);
				}
				return c;
			}
			
		});
		previewPanel = new JPanel();
		previewPanel.setLayout(new BorderLayout());
		previewPanel.setBackground(Color.white);
		report = new UIHelper.HTMLPanel();
		report.setEditable(false);
		report.setFontSize(11);
		//report.setFont(report.getFont().deriveFont(11.0f));
		input = new JTextField();
		input.grabFocus();
		input.setBorder(new TitledBorder("Search by Case Name"));
		filter =  new ViewerHelper.FilterDocument(input,cases,Collections.EMPTY_LIST);
		input.setDocument(filter);
		JScrollPane s1 = new JScrollPane(diagnoses);
		s1.setPreferredSize(new Dimension(250,300));
		s1.setBorder(new TitledBorder("Diagnoses"));
		JScrollPane s2 = new JScrollPane(cases);
		s2.setBorder(new TitledBorder("Cases"));
		s2.setPreferredSize(new Dimension(200,300));
		JScrollPane s3 = new JScrollPane(report);
		s3.setBorder(new TitledBorder("Report"));
		s3.setPreferredSize(new Dimension(300,300));
		previewPanel.add(s3,BorderLayout.CENTER);
		JLabel lbl = new JLabel("<html><font color=blue><u>"+OntologyHelper.getKnowledgeBase(o.getURI())+"</u></font>");
		lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
		lbl.setBorder(new TitledBorder("Domain"));
		add(lbl,BorderLayout.NORTH);
		add(s1,BorderLayout.WEST);
		add(s2,BorderLayout.CENTER);
		add(previewPanel,BorderLayout.EAST);
		//add(input,BorderLayout.SOUTH);
		
		
		final JProgressBar progress = new JProgressBar();
		progress.setPreferredSize(input.getPreferredSize());
		progress.setIndeterminate(true);
		progress.setStringPainted(true);
		progress.setString("loading cases (please wait) ...");
		add(progress,BorderLayout.SOUTH);
		
		// load in sepearate thread
		(new Thread(new Runnable(){
			public void run(){
				load();
				
				// restore interface
				remove(progress);
				add(input,BorderLayout.SOUTH);
				validate();
				repaint();
			}
		})).start();
	}
	
	


	public void valueChanged(ListSelectionEvent e) {
		if(block)
			return;
		
		if(!e.getValueIsAdjusting()){
			if(e.getSource().equals(diagnoses)){
				block = true;
				loadCases(diagnoses.getSelectedValue());
				block = false;
			}else if(e.getSource().equals(cases)){
				block = true;
				Object name = cases.getSelectedValue();
				IInstance inst = o.getInstance(""+name);
				if(inst == null)
					return;
				// load case info
				input.setText(inst.getName());
				loadReport(name);
				loadSlides(inst);
				// highlight appropriate diagnosis
				List<Integer> selection = new ArrayList<Integer>();
				for(IClass cls : inst.getDirectTypes()){
					if(cls.hasSuperClass(o.getClass(OntologyHelper.DISEASES))){
						diagnoses.setSelectedValue(cls,true);
						selection.add(diagnoses.getSelectedIndex());
					}
				}
				diagnoses.clearSelection();		
				for(Integer i: selection){
					diagnoses.addSelectionInterval(i.intValue(),i.intValue());
				}
				block = false;
			}
		}
		
	}
	
	
	/**
	 * load stuff into browser
	 */
	private void load(){
		List<IInstance> caseList = new ArrayList<IInstance>();
		Set<IClass> diagList = new TreeSet<IClass>();
		for(IInstance inst: o.getClass(OntologyHelper.CASES).getDirectInstances()){
			// skip new case
			if(OntologyHelper.NEW_CASE.equals(inst.getName()))
				continue;
			
			caseList.add(inst);
			for(IClass c: inst.getTypes()){
				if(c.hasSuperClass(o.getClass(OntologyHelper.DISEASES)))
					diagList.add(c);
			}
		}
		Collections.sort(caseList,new InstanceComparator());
		ArrayList dlist = new ArrayList();
		dlist.add(ALL);
		dlist.addAll(diagList);
		diagnoses.setListData(dlist.toArray());
		cases.setListData(caseList.toArray());
		filter.setSelectableObjects(caseList);
	}
	
	
	/**
	 * load stuff into browser
	 */
	private void loadReport(Object name){
		if(name instanceof IInstance){
			report.setReport(""+((IInstance)name).getPropertyValue(o.getProperty(OntologyHelper.HAS_REPORT)));
			report.setCaretPosition(0);
		}
	}
	
	/**
	 * load stuff into browser
	 */
	private void loadSlides(IInstance inst){
		Object [] slides = inst.getPropertyValues(o.getProperty(OntologyHelper.HAS_SLIDE));
		if(slides != null && slides.length > 0){
			if(slideBar != null)
				previewPanel.remove(slideBar);
			slideBar = createSlideToolbar(inst.getName(),slides);
			previewPanel.add(slideBar,BorderLayout.SOUTH);
			previewPanel.revalidate();
		}
	}
	
	/**
	 * load stuff into browser
	 */
	private void loadCases(Object name){
		if(name instanceof IClass){
			// use set, to make sure we don't have duplicat cases listed for some reason
			Set<IInstance> ins = new HashSet<IInstance>();
			Collections.addAll(ins,((IClass)name).getDirectInstances());
			Vector v = new Vector(ins);
			Collections.sort(v,new InstanceComparator());
			cases.setListData(v);
			filter.setSelectableObjects(v);
		}else if(ALL.equals(name)){
			loadCases(o.getClass(OntologyHelper.CASES));
		}
	}
	
	
	public Object getSelectedObject() {
		return cases.getSelectedValue();
	}

	public Object[] getSelectedObjects() {
		return new Object [] {getSelectedObject()};
	}

	public int getSelectionMode() {
		return EntryChooser.SINGLE_SELECTION;
	}

	public boolean isSelected() {
		return ok && !cases.isSelectionEmpty();
	}

	public void setOwner(Frame frame) {
		this.frame = frame;

	}

	public void setSelectionMode(int mode) {
		//do nothing
	}

	public void showChooserDialog() {
		int r = JOptionPane.showConfirmDialog(frame,this,"Select Case",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		ok = r == JOptionPane.OK_OPTION;
	}

	
	/**
	 * create slide buttons for a given set of slides
	 */
	private JToolBar createSlideToolbar(String name, Object [] slides){
		// init viewer if required
		String type = (slides.length>0)?ViewerFactory.recomendViewerType(""+slides[0]):"qview";
		final String dir = ViewerFactory.getProperties().getProperty(type+".image.dir","");
		final Viewer viewer = ViewerFactory.getViewerInstance(type);
		viewer.setSize(new Dimension(500,500));
		
		// init buttons
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setBackground(Color.white);
		toolbar.setBorder(new TitledBorder("Slides"));
		toolbar.setMinimumSize(new Dimension(0,0));
		//ButtonGroup grp = new ButtonGroup();
		//AbstractButton selected = null;
		for(int i=0;i<slides.length;i++){
			final String image = ""+slides[i];
			String text = ""+slides[i];
			// strip suffic and prefix
			int x = text.lastIndexOf("/");
			if(x > -1)
				text = text.substring(x+1);
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
							JOptionPane.showMessageDialog(CaseSelectorPanel.this,"Unable to load image "+dir+image,"Error",JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();
							bt.setSelected(false);
							return;
						}
						JOptionPane.showMessageDialog(CaseSelectorPanel.this,viewer.getViewerPanel(),image,JOptionPane.PLAIN_MESSAGE);
						bt.setSelected(false);
					}
				}
			});
			//grp.add(bt);
			toolbar.add(bt);
			
			// select entry
			//if(selected == null && (text.contains("HE") || slides.length == 1))
			//	selected = bt;
		}
		
		// do click
		/*
		if(selected != null){
			selected.addHierarchyListener(new HierarchyListener() {
				public void hierarchyChanged(HierarchyEvent e) {
					if((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) !=0 && e.getComponent().isShowing()) {
						((AbstractButton)e.getComponent()).doClick();
						e.getComponent().removeHierarchyListener(this);
					}
				}
			});
		}*/
		
		
		return toolbar;
	}
	
	
	/**
	 * compare two instances
	 * @author tseytlin
	 *
	 */
	private class InstanceComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof IInstance && o2 instanceof IInstance){
				IInstance i1 = (IInstance) o1;
				IInstance i2 = (IInstance) o2;
				String s1 = (String) i1.getPropertyValue(i1.getOntology().getProperty(HAS_STATUS));
				String s2 = (String) i2.getPropertyValue(i1.getOntology().getProperty(HAS_STATUS));
				
				// sort by status
				if(s1 != null || s2 != null){
					// take care of nulls
					if(s1 == null)
						return 1;
					if(s2 == null)
						return -1;
					
					// if status is set, but not equal, then
					if(!s1.equals(s2)){
						if(STATUS_TESTED.equals(s1))
							return -1;
						if(STATUS_TESTED.equals(s2))
							return 1;
						if(STATUS_COMPLETE.equals(s1))
							return -1;
						if(STATUS_COMPLETE.equals(s2))
							return 1;
						if(STATUS_INCOMPLETE.equals(s1))
							return -1;
						if(STATUS_INCOMPLETE.equals(s2))
							return 1;
					}
				}
				
				// compare names
				if(i1.getName() == null)
					return 1;
				if(i2.getName() == null)
					return -1;
				return i1.getName().compareTo(i2.getName());
			}
			return o1.toString().compareTo(o2.toString());
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		String driver = "com.mysql.jdbc.Driver";
		String url = "jdbc:mysql://localhost/repository";
		String user = "user";
		String pass = "resu";
		String table = "repository";
		String dir   = System.getProperty("user.home")+File.separator+".protegeRepository";
		IRepository r = new ProtegeRepository(driver,url,user,pass,table,dir);
		IOntology [] o = r.getOntologies("SubepidermalInstances.owl");
		o[0].load();
		OntologyHelper.checkOntologyVersion(o[0]);
		CaseSelectorPanel selector = new CaseSelectorPanel(o[0]);
		selector.showChooserDialog();

	}


}
