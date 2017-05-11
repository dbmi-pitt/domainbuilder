package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

import edu.pitt.dbmi.domainbuilder.caseauthor.ShapeSelector;


public class ShapeSelectorPanel extends JPanel implements EntryChooser {
	private JTree tree;
	private int mode;
	private Frame frame;
	private boolean ok;
	
	/**
	 * create new shape selection panel
	 * @param root
	 */
	public ShapeSelectorPanel(MutableTreeNode root){
		super();
		setLayout(new BorderLayout());
		
		// create shape tree
		tree = new JTree(new DefaultTreeModel(root));
		tree.setRootVisible(false);
		tree.setDragEnabled(true);
		tree.setShowsRootHandles(true);
		tree.setExpandsSelectedPaths(true);
		tree.putClientProperty("JTree.lineStyle", "Horizontal");
		tree.setCellRenderer(new ShapeSelector.ShapeRenderer(null));
		tree.setScrollsOnExpand(true);
		tree.setToggleClickCount(2);
		
		add(new JScrollPane(tree),BorderLayout.CENTER);
		setPreferredSize(new Dimension(300,400));
	}
	
	/**
	 * get selected objects
	 */
	public Object getSelectedObject() {
		TreePath p = tree.getSelectionPath();
		return (p != null)?""+((DefaultMutableTreeNode)p.getLastPathComponent()).getUserObject():null;
	}

	/**
	 * get selected objects
	 */
	public Object[] getSelectedObjects() {
		TreePath [] path = tree.getSelectionPaths();
		if(path != null){
			String [] obj = new String [path.length];
			for(int i=0;i<obj.length;i++){
				obj[i] = ""+((DefaultMutableTreeNode)path[i].getLastPathComponent()).getUserObject();
			}
			return obj;
		}
		return null;
	}

	public int getSelectionMode() {
		return mode;
	}

	public boolean isSelected() {
		return ok && !tree.isSelectionEmpty();
	}

	public void setOwner(Frame frame) {
		this.frame = frame;

	}

	public void setSelectionMode(int mode) {
		this.mode = mode;
	}

	public void showChooserDialog() {
		int r = JOptionPane.showConfirmDialog(frame,this,"Select Shapes",
				JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		ok = r == JOptionPane.OK_OPTION;
	}

}
