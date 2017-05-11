package edu.pitt.dbmi.domainbuilder.knowledge;

import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import edu.pitt.dbmi.domainbuilder.beans.ConceptEntry;
import edu.pitt.dbmi.domainbuilder.beans.ConceptExpression;
import edu.pitt.dbmi.domainbuilder.util.CSVReader;
import edu.pitt.dbmi.domainbuilder.util.OntologyHelper;
import edu.pitt.dbmi.domainbuilder.util.UIHelper;
import edu.pitt.ontology.IClass;
import edu.pitt.ontology.ILogicExpression;


public class SpreadsheetHandler {
	private static SpreadsheetHandler instance;
	private JComponent exportOption;
	private ButtonGroup exportOptionGroup;
	private SpreadsheetHandler(){}
	private File dir;
	
	/**
	 * get instance of it
	 * @return
	 */
	public static SpreadsheetHandler getInstance(){
		if(instance == null)
			instance = new SpreadsheetHandler();
		return instance;
	}
	
	
	/**
	 * get export option panel
	 * @return
	 */
	private JComponent getExportOptionPanel(){
		if(exportOption == null){
			exportOption = new JPanel();
			exportOption.setLayout(new BoxLayout(exportOption,BoxLayout.Y_AXIS));
			JRadioButton b1 = new JRadioButton("all diagnoses",true);
			b1.setActionCommand("all");
			JRadioButton b2 = new JRadioButton("diagnoses in worksheet",false);
			b2.setActionCommand("worksheet");
			exportOptionGroup = new ButtonGroup();
			exportOptionGroup.add(b1);
			exportOptionGroup.add(b2);
			exportOption.add(b1);	
			exportOption.add(b2);
			
		}
		return exportOption;
	}
	
	/**
	 * start export process
	 */
	public void doExportSpreadsheet(DiagnosisBuilder builder){
		JFileChooser chooser = new JFileChooser();
		chooser.setAccessory(getExportOptionPanel());
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".csv") || f.getName().endsWith(".txt");
			}
			public String getDescription() {
				return "Comma Separated Values File (*.csv *.txt)";
			}
			
		});
		String name = builder.getOntology().getName();
		if(name.endsWith(".owl"))
			name = name.substring(0,name.length()-4);
		// init default directory (sort of remember the last location)
		if(dir == null)
			dir = chooser.getFileSystemView().getDefaultDirectory();
		chooser.setSelectedFile(new File(dir,name+".csv"));
		
		// show dialog
		if(chooser.showSaveDialog(builder.getFrame()) == JFileChooser.APPROVE_OPTION){
			// check if file exists
			File file = chooser.getSelectedFile();
			if( file.exists() && JOptionPane.CANCEL_OPTION == JOptionPane.showConfirmDialog(
				builder.getFrame(),file+" already exists. Overwrite?",
				"Warning",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE)){
				return;
			}
			dir = file.getParentFile();
			// do export
			String cmd = exportOptionGroup.getSelection().getActionCommand();
			List<ConceptEntry> list = null;
			if("all".equals(cmd)){
				list = builder.getAllDiagnosisList();
			}else if("worksheet".equals(cmd)){
				list = builder.getWorksheetDiagnosisList();
			}
			// should never happen
			if(list != null){
				try{
					writeSpreadsheet(file,list);
				}catch(IOException ex){
					JOptionPane.showMessageDialog(builder.getFrame(),ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
				JOptionPane.showMessageDialog(builder.getFrame(),"Diagnoses saved in "+file.getAbsolutePath());
				
				// backup
				if("all".equals(cmd) && !OntologyHelper.isReadOnly(builder.getOntology())){
					try{
						UIHelper.backup(file,new File(OntologyHelper.getLocalSpreadsheetFolder(builder.getOntology()),file.getName()));
					}catch(IOException ex){
						JOptionPane.showMessageDialog(builder.getFrame(),ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	
	/**
	 * write spreadsheet 
	 * @param file
	 * @param list
	 */
	private void writeSpreadsheet(File file, List<ConceptEntry> list) throws IOException{
		final String separator = ",";
		// go row by row
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(file));
			int index = -1;
			while(true){
				StringBuffer buffer = new StringBuffer();
				// for each disease
				for(ConceptEntry e: list){
					if(index < 0){
						buffer.append(e.getText());
					}else{
						Object o = e.getFindings().getEntry(index);
						if(o instanceof ConceptEntry){
							buffer.append(((ConceptEntry)o).getText());
						}else if(o instanceof ConceptExpression){
							buffer.append(((ConceptExpression)o).getText());
						}
					}
					buffer.append(separator);
				}
				// on first empty line, we are done
				if(buffer.length() <= separator.length()*list.size())
					break;
				// remove last comma
				buffer.delete(buffer.length()-separator.length(),buffer.length());
				// write it out
				writer.write(buffer+"\n");
				index ++;
			}
		}catch(IOException ex){
			throw ex;
		}finally{
			if(writer != null)
				writer.close();
		}
	}
	
	/**
	 * start export process
	 */
	public void doImportSpreadsheet(DiagnosisBuilder builder){
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".csv") || f.getName().endsWith(".txt");
			}
			public String getDescription() {
				return "Comma Separated Values File (*.csv *.txt)";
			}
			
		});
		
		// start in default directory
		if(dir == null)
			dir = chooser.getFileSystemView().getDefaultDirectory();
		chooser.setCurrentDirectory(dir);
		
		// show dialog
		if(chooser.showOpenDialog(builder.getFrame()) == JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			dir = file.getParentFile();
			try{
				readSpreadsheet(file,builder);
			}catch(IOException ex){
				JOptionPane.showMessageDialog(builder.getFrame(),ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * read in spreadsheet
	 * @param file
	 * @param builder
	 */
	private void readSpreadsheet(File file, DiagnosisBuilder builder) throws IOException{
		// go row by row
		CSVReader reader = null;
		try{
			reader = new CSVReader(new FileReader(file));
			ConceptEntry [] diagnoses = null;
			// go row by row
			List<ConceptEntry> dlist = new ArrayList<ConceptEntry>();
			for(Object tokens : reader.readAll()){
				if(tokens instanceof String []){
					String [] a = (String []) tokens;
					// init diagnosis
					if(diagnoses == null)
						diagnoses = new ConceptEntry [a.length];
					
					// iterate over each column item
					for(int i=0;i<a.length;i++){
						// we have a new diagnosis
						if(diagnoses[i] == null){
							diagnoses[i] = (ConceptEntry) readCell(builder,OntologyHelper.DISEASES,a[i]);
							if(diagnoses[i] == null)
								continue;
							
							// keep track of multiple patterns
							int x = dlist.indexOf(diagnoses[i]);
							if(x > -1){
								diagnoses[i].addNewPattern(dlist.get(x));
							}else{
								dlist.add(diagnoses[i]);
							}
							diagnoses[i].getFindings().clear();
							
							// add columns AND mind the rest							
							builder.addDiagnosisColumn(diagnoses[i]);
							OntologyHelper.getConceptHandler(builder.getOntology()).addDiagnosis(diagnoses[i]);
						}else if(diagnoses[i] != null){
							Object o = readCell(builder,OntologyHelper.DIAGNOSTIC_FEATURES,a[i]);
							if(o != null){
								diagnoses[i].getFindings().add(o);
								//diagnoses[i].getFindings().recreateTranslationTable();
							}
						}
						
					}
					builder.refreshTable();
				}
			}
			builder.updateTable();
		}catch(IOException ex){
			throw ex;
		}finally{
			if(reader != null)
				reader.close();
		}
	}
	
	/**
	 * read content of a cell
	 * @param str
	 * @return
	 */
	private Object readCell(DiagnosisBuilder builder,String parent,String str){
		// check if this is an expression
		String [] p = str.split("\\s+(OR|AND)\\s+");
		if(!parent.equals(OntologyHelper.DISEASES) && p.length > 1){
			// deal with expressions
			int t = (str.matches("\\s+AND\\s+"))?ILogicExpression.AND:ILogicExpression.OR;
			ConceptExpression exp = new ConceptExpression(t);
			for(String s: p){
				exp.add(readCell(builder,OntologyHelper.DIAGNOSTIC_FEATURES,s));
			}
			return exp;
		}else{
			// deal with individual concept
			// check if it is MAY
			str = str.replaceAll("[^\\w\\-]+"," ").trim();
			boolean absent = false;
			if(str.startsWith("MAY ")){
				str = str.substring(4).trim();
				// this is optional parameter, lets add it to KB, but not link 
				// it to disease
				builder.createConceptClass(parent,str);
				return null;
			}else if(str.startsWith("NO ")){
				str = str.substring(3).trim();
				absent = true;
			}
			if(str.length() == 0)
				return null;
			IClass c = builder.createConceptClass(parent, str);
			if(c != null){
				ConceptEntry entry = new ConceptEntry(c);
				entry.setAbsent(absent);
				return entry;
			}
		}
		return null;
	}
}
