package edu.pitt.dbmi.domainbuilder.caseauthor;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.tree.*;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ShapeEntry;
import edu.pitt.dbmi.domainbuilder.beans.SlideEntry;
import edu.pitt.dbmi.domainbuilder.util.*;
import edu.pitt.dbmi.domainbuilder.widgets.AnnotationManager;
import edu.pitt.slideviewer.ViewPosition;
import edu.pitt.slideviewer.markers.Annotation;

public class ShapeSelector extends JPanel 
	implements ActionListener, TreeSelectionListener, AnnotationManager,
	DropTargetListener, ItemListener {
	//, DragGestureListener, DragSourceListener {
	public static String SHAPE_RENAMED = "SHAPE_RENAMED";
	public static String SHAPE_DELETED = "SHAPE_DELETED";
	public static String SHAPE_SELECTED = "SHAPE_SELECTED";
	public static String UNTAGGED = "Untagged";
	private CaseAuthor caseAuthor;
	private Annotation currentTutorMarker;
	private JTree tree;
	private int shapeCount;
	private JComboBox viewList;
	private final String [] VIEW_OPTIONS = new String [] {"all","slide"}; //,"finding"};
	private ShapeNode root,untagged;
	private Map<String,ShapeNode> shapeTable;
	private boolean blockEvent, blockSelectionEvent,readOnly;
	private JToolBar toolbar;
	private JPopupMenu tpopup, spopup, epopup;
	private final boolean SELECT_SHAPE_AFTER_DRAW = false;
	private List<ShapeNode> clipboard;
	
	//private DragSource source;
	/**
	 * create new slide selector
	 */
	public ShapeSelector(CaseAuthor author){
		super();
		this.caseAuthor = author;
		setLayout(new BorderLayout());
		
		shapeTable = new HashMap<String,ShapeNode>();
		
		// create shape tree
		root = new ShapeNode();
		untagged = new ShapeNode(UNTAGGED,true);
		root.add(untagged);
		shapeTable.put(UNTAGGED,untagged);
		tree = new JTree(new DefaultTreeModel(root));
		tree.setRootVisible(false);
		tree.setDragEnabled(true);
		tree.setShowsRootHandles(true);
		tree.addTreeSelectionListener(this);
		tree.setExpandsSelectedPaths(true);
		tree.putClientProperty("JTree.lineStyle", "Horizontal");
		tree.setCellRenderer(new ShapeRenderer(caseAuthor));
		tree.setScrollsOnExpand(true);
		tree.setToggleClickCount(2);
		tree.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3){
					int x = tree.getRowForLocation(e.getX(),e.getY());
					if(x > -1){
						// check if withing selection
						if(!inPreviousSelection(tree.getSelectionRows(),x))
							tree.setSelectionRow(x);
					}else
						tree.clearSelection();
					if(!readOnly)
						getPopupMenu().show(tree,e.getX(),e.getY());
					
				}
			}
		});
		tree.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e) {
				if(e.isControlDown() || e.isMetaDown()){
					if(e.getKeyCode() == KeyEvent.VK_X){
						doCut();
					}else if(e.getKeyCode() == KeyEvent.VK_C){
						doCopy();
					}else if(e.getKeyCode() == KeyEvent.VK_V){
						doPaste();
					}
				}
			}
			
		});
		new DropTarget(tree,this);
		//source = new DragSource();
		//source.createDefaultDragGestureRecognizer(tree,DnDConstants.ACTION_COPY_OR_MOVE,this);
		//slideList.setCellRenderer(new IconListCellRenderer(slideList));
		add(getToolBar(),BorderLayout.NORTH);
		add(new JScrollPane(tree),BorderLayout.CENTER);
		setPreferredSize(new Dimension(120,120));
		
		// watch when tab is displayed
		addComponentListener(new ComponentAdapter(){
			/**
			 * this component is shown
			 */
			public void componentShown(ComponentEvent e) {
				super.componentShown(e);
				// if slide view is shown, redo it
				if(VIEW_OPTIONS[1].equals(viewList.getSelectedItem())){
					filterTree(VIEW_OPTIONS[1]);
				}
			}
		});
		setEnabled(false);
	}
	
	private boolean inPreviousSelection(int [] rows, int x){
		if(rows == null)
			return false;
		for(int i: rows){
			if(i == x)
				return true;
		}
		return false;
	}
	
	/**
	 * reset values
	 */
	public void reset(){
		if(shapeTable != null)
			shapeTable.clear();
		untagged.removeAllChildren();
		root.removeAllChildren();
		root.add(untagged);
		shapeTable.put(UNTAGGED,untagged);
		loadAnnotations();
		setEnabled(true);
	}
	
	
	/**
	 * load already existing annotations
	 */
	private void loadAnnotations(){
		blockSelectionEvent = true;
		shapeCount = 0;
		Pattern pt = Pattern.compile("[A-Za-z]+(\\d+)");
    	for(SlideEntry slide: caseAuthor.getCaseEntry().getSlides()){
			for(ShapeEntry shape: slide.getAnnotations()){
				ShapeNode node = new ShapeNode(shape,false);
				shapeTable.put(shape.getName(),node);
				addTags(node,shape.getTag(),false);	
				
				// keep tabs on shapes
				Matcher mt = pt.matcher(shape.getName());
				if(mt.matches()){
					int x = Integer.parseInt(mt.group(1));
					if(x > shapeCount)
						shapeCount = x;
				}
			}
		}
		reloadTree();
		blockSelectionEvent = false;
	}
	
	
	/**
	 * @return the currentTutorMarker
	 */
	public Annotation getCurrentAnnotation() {
		return currentTutorMarker;
	}

	/**
	 * @param currentTutorMarker the currentTutorMarker to set
	 */
	public void setCurrentAnnotation(Annotation currentTutorMarker) {
		this.currentTutorMarker = currentTutorMarker;
	}
	
	/**
	 * add tutor marker to this list
	 * @param tm
	 */
	public void addAnnotation(Annotation tm){
		TreePath path = tree.getSelectionPath();
		ShapeNode selected = (path != null)?(ShapeNode)path.getLastPathComponent():null;
		// if nothing is selected, then it is untagged
		if(selected == null)
			selected = untagged;
		// if selected node doesn't allow children, then selecte its parent
		else if(!selected.getAllowsChildren())
			selected = (ShapeNode) selected.getParent();
		
		ShapeEntry entry = ShapeEntry.createShapeEntry(tm);
		entry.setSlide(caseAuthor.getCaseEntry().getCurrentSlide());
		
		// add to node
		ShapeNode node = new ShapeNode(entry,false);
		
		// add to slide
		caseAuthor.getCaseEntry().getSlide(SlideEntry.getImageName(tm.getImage())).addAnnotation(entry);
		
		// add to map
		shapeTable.put(tm.getName(),node);
		
		// if Annotation was just drawn and it has Default tag,
		// then add it to highlighted tag, else add tags that it had
		//System.out.println(entry.getName()+" "+entry.getTag());
		if(TextHelper.isEmpty(entry.getTag())){
			entry.setTag(""+selected.getUserObject());
			selected.add(node);
		}else{
			addTags(node,entry.getTag());
		}
		
		// reload tree
		reloadTree();
		//TODO:
		//tree.expandPath(new TreePath(selected.getPath()));
		if(SELECT_SHAPE_AFTER_DRAW)
			setSelectedNode(node);
		
		
		// set modification flag
		if(!blockSelectionEvent){
			caseAuthor.setCaseModified();
		}
	}
	
	
	/**
	 * add tutor marker to this list
	 * @param tm
	 */
	public void removeAnnotation(ShapeEntry entry){
		Annotation tm = entry.getAnnotation();
		// remove from slide entry
		entry.getSlide().removeAnnotation(entry);
		
		// remove if currently displayed
		//System.out.println(entry.getImage()+" "+caseAuthor.getViewer().getImage());
		if(caseAuthor.getViewer().getImage().endsWith(entry.getImage())){
			caseAuthor.getViewer().getAnnotationManager().removeAnnotation(tm);
		}
		
		// remove from table 
		shapeTable.remove(entry.getName());
		
		// set modification flag
		if(!blockSelectionEvent){
			caseAuthor.setCaseModified();
		}
	}
		
	
	
	
	/**
	 * clear tutor marker selection
	 */
	private void clearAnnotationSelection(){
		edu.pitt.slideviewer.AnnotationManager mm = caseAuthor.getViewer().getAnnotationManager();
		for(Object obj : mm.getSelectedAnnotations()){
			Annotation tm = (Annotation) obj;
			tm.setSelected(false);
			tm.setMovable(false);
			tm.removePropertyChangeListener(caseAuthor);
		}
	}
	
	/**
	 * listen to tree selection events
	 */
	public void valueChanged(TreeSelectionEvent e) {
		if(blockSelectionEvent)
			return;
		
		clearAnnotationSelection();
		TreePath [] paths =  tree.getSelectionPaths();
		if(paths == null)
			return;
		for(TreePath path: paths){
			ShapeNode node = (ShapeNode) path.getLastPathComponent();
		    if (node == null) // || node.getUserObject() instanceof String)
		    	continue;
		    
		    boolean movable = !(node.getUserObject() instanceof String);
	
		    Collection<ShapeEntry> shapes = getAnnotations(""+node.getUserObject());
			for(ShapeEntry shape: shapes){
				Annotation tm = shape.getAnnotation(caseAuthor.getViewer().getAnnotationManager());
				if(SlideEntry.getImageName(tm.getImage()).equals(caseAuthor.getCaseEntry().getCurrentSlide().getSlideName())){
					tm.setMovable(movable);
					tm.setSelected(true);
					tm.setTagVisible(true);
					tm.addPropertyChangeListener(caseAuthor);
					if(movable)
						showAnnotation(tm);
				}
			}
			// notify of shape selection
			if(!blockEvent)
				firePropertyChange(SHAPE_SELECTED,null,""+node);
		}
	}

	/**
	 * show annotation on screen and perhaps fix it
	 * @param tm
	 */
	private void showAnnotation(Annotation tm){
		ViewPosition p = tm.getViewPosition();
		// fix broken shapes
		if(p.x < 0 && p.y < 0){
			Dimension d = caseAuthor.getViewer().getSize();
			p = tm.getCenterPosition();
			p.x =  p.x - (int)(d.width/(2*p.scale));
			p.y =  p.y - (int)(d.height/(2*p.scale));
			tm.setViewPosition(p);
		}
		caseAuthor.getViewer().setViewPosition(p);
	}
	
	/**
	 * add new tag
	 */
	private void doAddTag(){
		String name = null;
		ShapeNode [] selected = getSelectedNodes();
		if(selected != null && selected.length > 0 && selected[0].getUserObject() instanceof ShapeEntry){
			name = UIHelper.showComboBoxDialog(this,"enter tag name",null,getTags(),Icons.getIcon(Icons.TAG,24));
		}else{
			name = UIHelper.showInputDialog(this,"enter tag name",Icons.getIcon(Icons.TAG,24));
		}
		// add new tag	
		if(name != null){
			if(selected.length > 0){
				for(ShapeNode n: selected)
					addTags(n,name);
			}else
				addTags(null,name);
		}
	}
	
	/**
	 * get tags
	 * @return
	 */
	private String [] getTags(){
		List<String> tags = new ArrayList<String>();
		for(int i=0;i<root.getChildCount();i++){
			ShapeNode node = (ShapeNode) root.getChildAt(i);
			if(node != untagged && node.getUserObject() instanceof String){
				tags.add(""+node.getUserObject());
			}
		}
		Collections.sort(tags);
		return tags.toArray(new String[0]);
	}
	
	
	
	/**
	 * add tags
	 * @param e
	 * @param name
	 */
	private void addTags(ShapeNode selected, String name){
		addTags(selected,name,true);
		
	}
	
	private boolean tagHasShape(ShapeNode node, ShapeEntry e){
		for(int i=0;i<node.getChildCount();i++){
			ShapeNode en = (ShapeNode) node.getChildAt(i);
			if(e.equals(en.getUserObject())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * add tags
	 * @param e
	 * @param name
	 */
	private void addTags(ShapeNode selected, String name,boolean now){
		for(String tag: name.split("[,;\\|]")){
			tag = tag.trim();
			if(tag.trim().length() == 0)
				tag = UNTAGGED;
			
			ShapeNode node = shapeTable.get(tag);
			if(node == null){
				node = new ShapeNode(tag,true);
				root.insert(node,0);
				shapeTable.put(tag,node);
			}
			if(selected != null && selected.getUserObject() instanceof ShapeEntry){
				ShapeEntry e = (ShapeEntry) selected.getUserObject();
				
				// don't add if in node already
				if(tagHasShape(node,e))
					continue;				
				
				// set tags for this shape
				if(tag.trim().length() == 0 || tag.equals(UNTAGGED))
					e.setTag(UNTAGGED);
				else{
					e.removeTag(UNTAGGED);
					e.addTag(tag);
				}
				if(untagged.isNodeChild(selected) || tag.equals(UNTAGGED)){
					node.add(selected);
				}else{
					ShapeNode ns = (selected.getParent() == null)?selected:(ShapeNode)selected.clone();
					node.add(ns);
					setSelectedNode(ns);
				}
				tree.expandPath(new TreePath(node.getPath()));
			}
		}
		if(now)
			reloadTree();
		
		
		// set modification flag
		if(!blockSelectionEvent){
			caseAuthor.setCaseModified();
		}
	}
	
	/**
	 * reload tree after it was modified
	 * try to keep branch expansion the same
	 * @param selected
	 */
	private void reloadTree(){
		// remember selected nodes
		//int [] rows = tree.getSelectionRows();
		
		// remmember expanded nodes
		ArrayList<Boolean> exp = new ArrayList<Boolean>();
		for(int i=0;i<root.getChildCount();i++){
			TreePath path = new TreePath(((ShapeNode) root.getChildAt(i)).getPath());
			exp.add(new Boolean(tree.isExpanded(path)));
		}
		// reload model
		((DefaultTreeModel)tree.getModel()).reload();
		
		// reset expanded nodes
		for(int i=0;i<root.getChildCount();i++){
			boolean expanded = ((Boolean)exp.get(i)).booleanValue();
			TreePath path = new TreePath(((ShapeNode) root.getChildAt(i)).getPath());
			if(expanded)
				tree.expandPath(path);
		}
		
		// reselect what was selected
		//if(rows != null){
		//	tree.setSelectionRows(rows);	
		//}
		
		//always expand untagged
		tree.expandPath(new TreePath(untagged.getPath()));
	}
	
	/**
	 * remove tag from annotation
	 */
	private void doRemoveTag(ShapeNode node){
		//ShapeNode node = getSelectedNode();
		//DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		if(node != null && node.getUserObject() instanceof ShapeEntry){
			ShapeNode parent = (ShapeNode) node.getParent();
			ShapeEntry shape = (ShapeEntry) node.getUserObject();
			// remove this tag from shape tag
			String tag = shape.getTag().replaceAll("[,\\s]*\\b"+parent.getUserObject()+"\\b","").trim();
			if(tag.startsWith(","))
				tag = tag.substring(1).trim();
			if(tag.length() == 0)
				tag = UNTAGGED;
			shape.setTag(tag);
			
			// remove from parent
			node.removeFromParent();
			//model.removeNodeFromParent(node);	
			
			// maybe add to untagged
			if(tag.equals(UNTAGGED))
				untagged.add(node);
		}
		reloadTree();
	}
	
	
	/**
	 * remove nodes
	 */
	private void doRemove(){
		// get selected nodes
		TreePath [] paths = tree.getSelectionPaths();
		if(paths == null){
			return;
		}
		
		doRemove(paths);
	}
	
	/**
	 * remove nodes
	 */
	public void doRemove(Collection<ShapeEntry> shapes){
		TreePath [] paths = new TreePath [shapes.size()];
		int i = 0;
		for(ShapeEntry e: shapes){
			ShapeNode node = shapeTable.get(e.getName());
			if(node != null && node.getUserObject() instanceof ShapeEntry){
				paths[i] = new TreePath(node.getPath());
			}
			i++;
		}
		
		doRemove(paths);
	}
	
	
	/**
	 * remove nodes
	 */
	private void doRemove(TreePath [] paths){
		// get selected nodes
			
		// iterate over paths
		for(int i=0;i<paths.length;i++){
			ShapeNode node = (ShapeNode) paths[i].getLastPathComponent();
			Object obj = node.getUserObject();
			
			// skip untagged 
			if(node.equals(untagged))
				continue;
			
			// notify
			firePropertyChange(SHAPE_DELETED,null,new String(""+obj));
			
			// if shape entry, remove this shape
			if(obj instanceof ShapeEntry){
				removeAnnotation((ShapeEntry) obj);
			// if tag, reroute all shapes to untagged cattegory	
			}else if(obj instanceof String){
				for(Object e : Collections.list(node.children())){
					ShapeNode n = (ShapeNode) e;
					doRemoveTag(n);
				}
				shapeTable.remove(obj);
			}
			
			
			// now remove this node && its clones
			//DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
			//System.out.println(node.getClones());
			for(ShapeNode clone: node.getClones()){
				//System.out.println(Arrays.toString(clone.getPath()));
				clone.removeFromParent();
			}
				
			node.removeFromParent();
				
		}
		// clear selection
		tree.clearSelection();
		
		// remove nodes
		reloadTree();
	}
	
	/**
	 * edit name 
	 */
	private void doEdit(){
		ShapeNode node = getSelectedNode();
		if(node != null && !node.equals(untagged)){
			String oname = ""+node.getUserObject();
			String name = UIHelper.showInputDialog(this,"enter tag name",oname,Icons.getIcon(Icons.TAG,24));
			if(name != null && !name.equals(oname)){
				node.setUserObject(name);
				// update tags in all of the shapes
				for(int i=0;i<node.getChildCount();i++)
					((ShapeEntry)((ShapeNode)node.getChildAt(i)).getUserObject()).getTag().replaceAll(oname,name);
				tree.repaint();
				
				// search and replace tags
				for(ShapeEntry e: getAnnotations(oname)){
					e.removeTag(oname);
					e.addTag(name);
				}
				
				// rename in table
				shapeTable.put(name,shapeTable.remove(oname));
				
				firePropertyChange(SHAPE_RENAMED,new String(oname),new String(name));
			}
		}
	}
	
	
	private void doLink(){
		ShapeNode node = getSelectedNode();
		if(node != null ){
			JList list = new JList(caseAuthor.getCaseEntry().getConceptEntries());
			JScrollPane scroll = new JScrollPane(list);
			scroll.setPreferredSize(new Dimension(200,300));
			scroll.setBorder(new TitledBorder("Select Findings"));
			int r = JOptionPane.showConfirmDialog(caseAuthor.getFrame(),scroll,"Select Concepts",
					JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
			if(r == JOptionPane.OK_OPTION){
				for(Object obj : list.getSelectedValues()){
					if(obj instanceof ConceptEntry){
						ConceptEntry e = (ConceptEntry) obj;
						e.addLocation(""+node);
						// update concept selectors
						for(ConceptSelector s :caseAuthor.getConceptSelectors(e)){
							s.repaint();
							e.flash(s);
						}
					}
				}
			}
		}
	}
	
	/**
	 * custom renderer for shapes
	 * @author tseytlin
	 */
	public static class ShapeRenderer extends DefaultTreeCellRenderer {
		private CaseAuthor caseAuthor;
		public ShapeRenderer(CaseAuthor a){
			caseAuthor = a;
		}
		/**
	     * get cell render component
	     */
	    public Component getTreeCellRendererComponent(
	                        JTree tree,
	                        Object value,
	                        boolean sel,
	                        boolean expanded,
	                        boolean leaf,
	                        int row,
	                        boolean hasFocus) {
	        super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
	        if(value instanceof ShapeNode){
		        Object user = ((ShapeNode)value).getUserObject();
	        	if (user instanceof ShapeEntry) {
	        		ShapeEntry se = (ShapeEntry) user;
	        		String type = se.getType();
	        		if(type.startsWith("Arrow"))
	        			setIcon(Icons.getIcon(Icons.ARROW,16));
	        		else if(type.startsWith("Rect") || type.startsWith("Para"))
	        			setIcon(Icons.getIcon(Icons.RECTANGLE,16));
	        		else if(type.startsWith("Poly"))
	        			setIcon(Icons.getIcon(Icons.POLYGON,16));
	        		else if(type.startsWith("Circle"))
	        			setIcon(Icons.getIcon(Icons.CIRCLE,16));
	        		else if(type.startsWith("Ruler"))
	        			setIcon(Icons.getIcon(Icons.RULER,16));	
	        		setToolTipText("Click on annotation to select it");
	        		// fade the annotation from different slide
	        		if(caseAuthor != null && caseAuthor.getCaseEntry() != null && caseAuthor.getCaseEntry().getCurrentSlide() != null &&
	        		   !SlideEntry.getImageName(se.getImage()).equals(caseAuthor.getCaseEntry().getCurrentSlide().getSlideName())){
	        			setForeground(Color.lightGray);
	        		}
	        	} else if (user instanceof String){
		        	setIcon(Icons.getIcon(Icons.TAG,16));
		        	setToolTipText(user+" annotation group");
		        } 
	        }
	        return this;
	    }
	}

	/**
	 * Drag Gesture Handler
	 *
	public void dragGestureRecognized(DragGestureEvent dge) {
		TreePath path = tree.getSelectionPath();
		if ((path == null) || (path.getPathCount() <= 1)) {
			// We can't move the root node or an empty selection
			return;
		}
		ShapeNode node = (ShapeNode) path.getLastPathComponent();
		//transferable = new TransferableTreeNode(path);
		source.startDrag(dge,DragSource.DefaultCopyDrop, node, this);
		// If you support dropping the node anywhere, you should probably
		// start with a valid move cursor:
		//source.startDrag(dge, DragSource.DefaultMoveDrop, transferable,
		// this);
	}
	*/
	/**
	 * drag-n-drop support, this is when drop occures
	 * @param dtde
	 */
	public void drop(DropTargetDropEvent dtde){
        if(readOnly)
        	return;
		
		Point loc = dtde.getLocation();
        if(dtde.getSource() instanceof DropTarget){
        	DropTarget droptarget = (DropTarget) dtde.getSource();
        	// if JTree is a target, then
        	if(droptarget.getComponent() instanceof JTree){
        		JTree tree = (JTree) droptarget.getComponent();
        		
        		// get selected row (for target component)
        		int selRow = tree.getRowForLocation(loc.x, loc.y);

        		// if it was selected
                if(selRow != -1) {
                	try{
                		// if drag data is not in table, then nodes are not being dragged
                		String [] data = (""+dtde.getTransferable().getTransferData(DataFlavor.stringFlavor)).split("\n");
                		// is transferable ta
                		if(data.length > 0 && shapeTable.containsKey(data[0])){
	                		// path of component being dragged
		                	TreePath [] path = tree.getSelectionPaths();
		                	ShapeNode [] entry = new ShapeNode [path.length];
		                	ShapeNode target = (ShapeNode) tree.getPathForRow(selRow).getLastPathComponent();
		                    // parent component is either itself or its parent
		                	ShapeNode parent =  (ShapeNode) target.getParent();
		                    int index;
		                    if(parent == null || target.getAllowsChildren()){
		                        index = 0;
		                        parent = target;
		                    }else
		                        index = parent.getIndex(target);
		
		                    for(int i=0;i<path.length;i++){
		                        entry[i] = (ShapeNode) path[i].getLastPathComponent();
		                        //change tag in dragged items
		                        Object obj = entry[i].getUserObject();
		                        if(obj instanceof ShapeEntry){
		                        	parent.insert(entry[i],index);
		                        	ShapeEntry e = (ShapeEntry) obj;
		                        	String otag = ""+((ShapeNode) path[i].getPathComponent(path[i].getPathCount()-2)).getUserObject();
		                        	String tag = ""+parent.getUserObject();
		                        	e.addTag(tag);
		                        	e.removeTag(otag);
		                        }
		                    }
		
		                    reloadTree();
		                    tree.expandPath(new TreePath(target.getPath()));
                		}else{
                			for(String name: data){
                				ConceptEntry e = caseAuthor.getCaseEntry().getConceptEntry(name);
                				if(e != null){
                					TreePath path = tree.getPathForRow(selRow);
                					if(path != null)
                						e.addLocation(""+path.getLastPathComponent());
                					e.flash();
                				}
                				
                			}
                		}
                	}catch(Exception ex){
                		ex.printStackTrace();
                	}
                }
        		
        	}
        }

    }
	// don't need those methods for now
    public void dragEnter(DropTargetDragEvent dtde) {}
    public void dragExit(DropTargetEvent dte) {}
    public void dragOver(DropTargetDragEvent dtde) {}
    public void dropActionChanged(DropTargetDragEvent dtde) {}
    //dragsource events
    /*
    public void dragEnter(DragSourceDragEvent dsde) { }
    public void dragExit(DragSourceEvent dse) {}
    public void dragOver(DragSourceDragEvent dsde) {}
    public void dropActionChanged(DragSourceDragEvent dsde) {}
    public void dragDropEnd(DragSourceDropEvent dsde) {}
    */
    /**
     * wrap default tree node to handle DnD
     * @author tseytlin
     *
     */
    public static class ShapeNode extends DefaultMutableTreeNode {
    	//implements Serializable,Transferable {
    	//private DataFlavor [] flavors;
    	private static boolean filter;
    	private boolean visible = true;
    	private Set<ShapeNode> clones;
    	
    	
    	public ShapeNode(){
    		super();
    		//setupFlavors();
    	}
    	public ShapeNode(Object obj){
    		super(obj);
    		//setupFlavors();
    	}
    	public ShapeNode(Object obj,boolean allowchildren){
    		super(obj,allowchildren);
    		//setupFlavors();
    	}
    	
    	public Object clone(){
    		ShapeNode e = new ShapeNode(getUserObject(),getAllowsChildren());
    		// add clone to this new node
    		addClone(e);
    		e.addClone(this);
    		    		
    		// add all clones to the new value
    		// so that every clone knows about the other one
    		for(ShapeNode c: getClones()){
    			c.addClone(e);
    			e.addClone(c);
    		}
    		return e;
    	}
    	
    	public void addClone(ShapeNode e){
    		if(clones == null)
    			clones = new LinkedHashSet<ShapeNode>();
    		clones.add(e);
    	}
    	
    	public Set<ShapeNode> getClones(){
    		return (clones != null)?clones:Collections.EMPTY_SET;
    	}
    	
    	public String toString(){
    		return ""+getUserObject();
    	}
    	
    	/*
    	private void setupFlavors(){
    		flavors = new DataFlavor [] {
    				new DataFlavor(getClass(),DataFlavor.javaJVMLocalObjectMimeType),
    				DataFlavor.stringFlavor};
    	}
    	public DataFlavor [] getTransferDataFlavors(){
    		return flavors;
    	}
    	public boolean isDataFlavorSupported(DataFlavor flavor) {
    		for(int i=0;i<flavors.length;i++){
    			if(flavor.equals(flavors[i]))
    				return true;
    		}
    		return false;
    	}
    	public Object getTransferData(DataFlavor flavor){
    		if(flavor.equals(flavors[0]))
    			return this;
    		return ""+this;
    	}
    	*/
    	
    	/**
    	 * get child at 
    	 */
    	public TreeNode getChildAt(int index) {
			// default behaviour to speed things up
    		if (!filter)
				return super.getChildAt(index);
			
    		// recalculate indexs
			if(children != null){
				int realIndex = -1;
				int visibleIndex = -1;
				
				for(Object n: children){
					if(n instanceof ShapeNode){
						ShapeNode node = (ShapeNode) n;
						if (node.isVisible()) {
							visibleIndex++;
						}
						realIndex++;
						if (visibleIndex == index) {
							return (TreeNode) children.elementAt(realIndex);
						}
					}
				}
			}
			//throw new ArrayIndexOutOfBoundsException("index unmatched");
			// let the superclass deal w/ it
			return super.getChildAt(index);
		}

    	/**
    	 * get child count
    	 */
		public int getChildCount(){
			// default behaviour to speed things up
			if(!filter) 
				return super.getChildCount();
			
			// no children no problem
			if (children == null) {
				return 0;
			}
			// count visible children
			int count = 0;
			for(Object n: children){
				if(n instanceof ShapeNode){
					ShapeNode node = (ShapeNode) n;
					if(node.isVisible())
						count++;
				}else{
					count ++;
				}
			}
			return count;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		public boolean isVisible() {
			return visible;
		}
		
		public static boolean isFiltered(){
			return filter;
		}
		public static void setFilter(boolean b){
			filter = b;
		}	
    }
    
    
    /**
     * filter tree
     * @param method
     */
    private void filterTree(String method){
    	// if ALL is selected
    	if(method.equals(VIEW_OPTIONS[0])){
    		ShapeNode.setFilter(false);
    		for(ShapeNode node: shapeTable.values()){
    			node.setVisible(true);
    		}
    	// if SLIDE is selected	
    	}else if(method.equals(VIEW_OPTIONS[1])){
    		ShapeNode.setFilter(true);
    		for(ShapeNode node: shapeTable.values()){
    			if(node.getUserObject() instanceof ShapeEntry){
    				ShapeEntry entry = (ShapeEntry) node.getUserObject();
    				node.setVisible(entry.getImage().equals(caseAuthor.getViewer().getImage()));
    			}
    		}
    	// if FINDING is selected	
    	}else if(method.equals(VIEW_OPTIONS[2])){
    		//ShapeNode.setFilter(true);
    		
    	}
    	// update model
    	reloadTree();
    }
    

	/**
	 * @return the root
	 */
	public MutableTreeNode getRoot() {
		return root;
	}

	/**
	 * filter the tree
	 */
	public void itemStateChanged(ItemEvent e) {
		if(e.getSource().equals(viewList)){
			filterTree(""+viewList.getSelectedItem());
		}
	}
	
	/**
	 * select all nodes 
	 * @param shapes
	 */
	public void doSelectShapes(List<String> shapes){
		blockEvent = true;
		List<TreePath> paths = new ArrayList<TreePath>();
		for(String shape: shapes){
			ShapeNode node = shapeTable.get(shape);
			if(node != null){
				paths.add(new TreePath(node.getPath()));
			}
		}
		tree.setSelectionPaths((TreePath [])paths.toArray(new TreePath[0]));	
		blockEvent = false;
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
		tree.setEnabled(b);
	}
	
	
	/**
	 * get selected node
	 * @return
	 */
	public ShapeNode getSelectedNode(){
		TreePath path = tree.getSelectionPath();
		return (path != null)?(ShapeNode)path.getLastPathComponent():null;
	}
	
	/**
	 * get selected node
	 * @return
	 */
	public ShapeNode[] getSelectedNodes(){
		TreePath [] path = tree.getSelectionPaths();
		if(path == null)
			return new ShapeNode [0];
		ShapeNode [] nodes = new ShapeNode [path.length];
		for(int i=0;i<nodes.length;i++)
			nodes[i] = (ShapeNode)path[i].getLastPathComponent();
		return nodes;
	}
	
	public void setSelectedNode(ShapeNode node){
		tree.setSelectionPath(new TreePath(node.getPath()));
	}
	

	public JPopupMenu getPopupMenu(){
		// init popus
		if(spopup == null){
			spopup = new JPopupMenu();
			spopup.add(UIHelper.createMenuItem("Add Tag","Add Tag to Annotation",Icons.TAG,this));
			spopup.add(UIHelper.createMenuItem("Remove Tag","Remove Tag from Annotation",Icons.MINUS,this));
			spopup.add(UIHelper.createMenuItem("Duplicate Shape","Create a Copy of Annotation",Icons.COPY,this));
			spopup.add(UIHelper.createMenuItem("Link","Link Annotation to Concept",Icons.LINK,this));
			spopup.addSeparator();
			spopup.add(UIHelper.createMenuItem("Delete","Delete Annotation",Icons.DELETE,this));
		}
		
		if(tpopup == null){
			tpopup = new JPopupMenu();
			tpopup.add(UIHelper.createMenuItem("Edit Tag","Edit Annotation tag",Icons.TAG,this));
			tpopup.add(UIHelper.createMenuItem("Link","Link Tag to Concept",Icons.LINK,this));
			
			tpopup.addSeparator();
			tpopup.add(UIHelper.createMenuItem("Delete","Delete Tag",Icons.DELETE,this));
		}
		
		// init empty popup
		if(epopup == null){
			epopup = new JPopupMenu();
			epopup.add(UIHelper.createMenuItem("Add Tag","Add Annotation Tag",Icons.TAG,this));
		}
		
		// return the right one
		ShapeNode node = getSelectedNode();
		if(node == null){
			return epopup;
		}else if(node.getUserObject() instanceof ShapeEntry){
			return spopup;
		}else
			return tpopup;
	}

	/**
	 * create tool bar
	 * @return
	 */
	private JToolBar createToolBar(){
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(UIHelper.createButton("Add Tag","Create New Tag or Add Existing Tag to Annotation",Icons.TAG,16,this));
		toolbar.add(UIHelper.createButton("Delete","Delete Tag/Annotation",Icons.DELETE,16,this));
		//toolbar.add(Box.createHorizontalGlue());
		// create view preference combo box
		viewList = new JComboBox(VIEW_OPTIONS);
		viewList.setFont(viewList.getFont().deriveFont(Font.PLAIN));
		viewList.addItemListener(this);
		toolbar.add(viewList);
		return toolbar;
	}
	
	
	/**
	 * get toolbar instance
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
		if(cmd.equals("Add Tag")){
			doAddTag();
		}else if(cmd.equals("Remove Tag")){
			doRemoveTag(getSelectedNode());
		}else if(cmd.equals("Delete")){
			doRemove();
		}else if(cmd.equals("Edit Tag")){
			doEdit();
		}else if(cmd.equals("Link")){
			doLink();
		}else if(cmd.equals("Cut")){
			doCut();
		}else if(cmd.equals("Copy") || cmd.equals("Duplicate Shape")){
			doCopy();
		}else if(cmd.equals("Paste")){
			doPaste();
		}
	}
	
	
	private void doPaste() {
		ShapeNode tag = getSelectedNode();
		if(clipboard == null || clipboard.isEmpty()){
			JOptionPane.showMessageDialog(caseAuthor.getComponent(),"Nothing Selected","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}else if(tag == null){
			JOptionPane.showMessageDialog(caseAuthor.getComponent(),"You must select a tag to paste shapes into","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// switch to parent if necessary
		if(!tag.getAllowsChildren())
			tag = (ShapeNode) tag.getParent();
		
		// assign tags
		for(ShapeNode n : clipboard){
			addTags(n, tag.getUserObject().toString(),true);
		}
		
	}

	private void doCopy() {
		if(getSelectedNodes().length == 0){
			JOptionPane.showMessageDialog(caseAuthor.getComponent(),"Nothing Selected","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		clipboard = new ArrayList<ShapeNode>();	
		for(ShapeNode n : Arrays.asList(getSelectedNodes())){
			if(!n.getAllowsChildren()){
				ShapeEntry entry = (ShapeEntry) n.getUserObject();
				ShapeEntry shape = copy(entry);
				// add annotation 
				Annotation a = shape.getAnnotation(caseAuthor.getViewer().getAnnotationManager());
				caseAuthor.getViewer().getAnnotationManager().addAnnotation(a);
				caseAuthor.addAnnotation(a);
				clipboard.add(shapeTable.get(shape.getName()));
			}
		}
		reloadTree();
	}

	/**
	 * copy shape
	 * @param entry
	 * @return
	 */
	private ShapeEntry copy(ShapeEntry entry) {
		ShapeEntry e = entry.clone();
		
		// reset name to new annotation
		String name = e.getType();
		if (name.startsWith("Para"))
			name = "Rectangle";
		e.setName(name + getAnnotationNumber());
		
		return e;
	}

	private void doCut() {
		if(getSelectedNodes().length == 0){
			JOptionPane.showMessageDialog(caseAuthor.getComponent(),"Nothing Selected","Warning",JOptionPane.WARNING_MESSAGE);
			return;
		}
		clipboard = new ArrayList<ShapeNode>();	
		for(ShapeNode n : getSelectedNodes()){
			if(!n.getAllowsChildren()){
				doRemoveTag(n);
				clipboard.add(n);
			}
		}
	}

	public List<ShapeEntry> getAnnotations(String name) {
		ShapeNode node = shapeTable.get(name);
		if(node != null){
			if(node.getUserObject() instanceof ShapeEntry){
				return Collections.singletonList((ShapeEntry)node.getUserObject());
			}else{
				List<ShapeEntry> list = new ArrayList<ShapeEntry>();
				for(int i=0;i<node.getChildCount();i++){
					ShapeNode n  = (ShapeNode)node.getChildAt(i);
					if(n.getUserObject() instanceof ShapeEntry)
						list.add((ShapeEntry)n.getUserObject());
				}
				return list;
			}
		}
		return Collections.EMPTY_LIST;
	}
	
	public boolean hasAnnotation(String name){
		return shapeTable.containsKey(name);
	}
	
	/**
	 * get all annotations
	 * @return
	 */
	public List<ShapeEntry> getAnnotations() {
		ArrayList<ShapeEntry> list = new ArrayList<ShapeEntry>();
		for(ShapeNode node : shapeTable.values()){
			if(node.getUserObject() instanceof ShapeEntry){
				list.add((ShapeEntry) node.getUserObject());
			}
		}
		return list;
	}
	
	
	/**
	 * get and increment unique annotation number
	 * @return
	 */
	public int getAnnotationNumber(){
		return ++shapeCount;
	}
	
	/**
	 * set a unique annotation number
	 * @return
	 */
	public void setAnnotationNumber(int x){
		shapeCount = x;
	}
	
	/**
	 * set read only status
	 * @param b
	 */
	public void setReadOnly(boolean b){
		readOnly = b;
		
		// enable/disable toolbar buttons
		if(toolbar != null){
			UIHelper.setEnabled(toolbar,new String [0],!b);
		}
	}
}
