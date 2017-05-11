package edu.pitt.dbmi.domainbuilder.widgets;

import java.awt.datatransfer.*;
import java.io.Serializable;

/**
 * wrap default tree node to handle DnD
 * @author tseytlin
 *
 */
public class EntryTransferable implements Serializable, Transferable {
	private DataFlavor [] flavors;
	private Object object;
	
	/**
	 * create new transferable
	 * @param object
	 */
	public EntryTransferable(Object object){
		this.object = object;
	}
	
	
	/**
	 * setup flavors
	 */
	private DataFlavor [] setupFlavors(){
		return new DataFlavor [] {
				new DataFlavor(object.getClass(),DataFlavor.javaJVMLocalObjectMimeType),
					DataFlavor.stringFlavor};
	}
	
	/**
	 * get data flavors
	 */
	public DataFlavor [] getTransferDataFlavors(){
		if(flavors == null)
			flavors = setupFlavors();
		return flavors;
	}
	
	/**
	 * check support
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for(int i=0;i<flavors.length;i++){
			if(flavor.equals(flavors[i]))
				return true;
		}
		return false;
	}
	
	/**
	 * get data
	 */
	public Object getTransferData(DataFlavor flavor){
		if(flavor.equals(flavors[0]))
			return object;
		return ""+object;
	}
}