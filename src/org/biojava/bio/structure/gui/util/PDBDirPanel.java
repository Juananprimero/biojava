/*
 *                  BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 * 
 * Created on Feb 9, 2007
 *
 */
package org.biojava.bio.structure.gui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureImpl;
import org.biojava.bio.structure.io.PDBFileReader;

/** A class to define where a structure for the alignment is coming from
 * 
 * @author Andreas Prlic
 * @since 1.7
 * @version %I% %G%
 */
public class PDBDirPanel 
extends JPanel 
implements StructurePairSelector{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5682120627824627408L;

	JTextField pdbDir;
	JTextField f1;
	JTextField f2;
	JTextField c1;
	JTextField c2;

	/** the system property PDB_DIR can be used to configure the 
	 * default location for PDB files.
	 */
	public static final String PDB_DIR = "PDB_DIR";


	public static Logger logger =  Logger.getLogger("org.biojava");

	/** load the PDB files from a local directory
	 * 
	 */
	public PDBDirPanel() {
		
		Box vBox = Box.createVerticalBox();

		pdbDir = new JTextField(20);
		
		String conf = System.getProperty(PDB_DIR);
		if ( conf != null){
			pdbDir.setText(conf);
		}
		JPanel dir = getPDBDirPanel(pdbDir);
		vBox.add(dir);
		
		
		int pdbfSize = 4;

		f1 = new JTextField(pdbfSize);
		c1 = new JTextField(1);
		JPanel p1 = getPDBFilePanel(1,f1,c1);       
		vBox.add(p1);

		f2 = new JTextField(pdbfSize);
		c2 = new JTextField(1);
		JPanel p2 = getPDBFilePanel(2, f2,c2);       
		vBox.add(p2);


		this.add(vBox);

	}


	private Structure fromPDB(JTextField f, JTextField c) throws StructureException{
		String pdb1 = f.getText();


		if ( pdb1.length() < 4) {
			f.setText("!!!");
			return null;
		}

		String chain1 = c.getText();

		//System.out.print("file :" + pdb1 + " " +  chain1);
		/// prepare structures

		// load them from the file system

		PDBFileReader reader = new PDBFileReader();
		
		
		String dir = pdbDir.getText();
		if ( dir != null){
			System.setProperty(PDB_DIR, dir);
		}
		reader.setPath(dir);
		
		System.out.println("dir: " + dir);
		
		Structure tmp1 = new StructureImpl();

		try {
			Structure structure1 = reader.getStructureById(pdb1);

			// no chain has been specified
			// return whole structure
			if (( chain1 != null) || (chain1.length()==0)){
				return structure1;
			}

			Chain c1 = structure1.findChain(chain1);
			tmp1.setPDBCode(structure1.getPDBCode());
			tmp1.addChain(c1);

		} catch (IOException e){
			logger.warning(e.getMessage());
			throw new StructureException(e);
		}
		return tmp1;	
	}



	public Structure getStructure1() throws StructureException{
		return fromPDB(f1,c1);
	}

	public Structure getStructure2() throws StructureException{    
		return fromPDB(f2,c2);
	}


	private JPanel getPDBDirPanel(JTextField f){
		 JPanel panel = new JPanel();
		 panel.setBorder(BorderFactory.createLineBorder(Color.black));
	        
		
		JLabel l01 = new JLabel("Select PDB directory");
		panel.add(l01);
	
		panel.add(f);
		
		Action action = new ChooseDirAction(pdbDir);
		
		JButton chooser = new JButton(action);
		panel.add(chooser);
		return panel;
	}

	private JPanel getPDBFilePanel(int pos ,JTextField f, JTextField c){

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createLineBorder(Color.black));

		

		JLabel l01 = new JLabel("PDB code ");

		panel.add(l01);
		Box hBox11 = Box.createHorizontalBox();

		JLabel l11 = new JLabel(pos + ":");

		

		f.setMaximumSize(new Dimension(Short.MAX_VALUE,30));


		hBox11.add(l11);
		hBox11.add(Box.createVerticalGlue());
		hBox11.add(f, BorderLayout.CENTER);
		hBox11.add(Box.createVerticalGlue());

		panel.add(hBox11);

		Box hBox21 = Box.createHorizontalBox();
		JLabel l21 = new JLabel("Chain" + pos + ":");
		
		c.setMaximumSize(new Dimension(Short.MAX_VALUE,30));
		hBox21.add(l21);
		hBox21.add(Box.createGlue());
		hBox21.add(c, BorderLayout.CENTER);
		hBox21.add(Box.createGlue());

		panel.add(hBox21);

		return panel;
	}

}

class ChooseDirAction extends AbstractAction{
	
	JTextField textField;
	public ChooseDirAction (JTextField textField){
		super("Choose");
		this.textField = textField;
	}
	public static final long serialVersionUID = 0l;
	// This method is called when the button is pressed
	public void actionPerformed(ActionEvent evt) {
		// Perform action...
		JFileChooser chooser = new JFileChooser();
		String txt = textField.getText();
		if ( txt != null){
			chooser.setCurrentDirectory(new java.io.File(txt));
		} 
	    chooser.setDialogTitle("Choose directory that contains your PDB files");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    //
	    // disable the "All files" option.
	    //
	    chooser.setAcceptAllFileFilterUsed(false);
	    //    
	    
	
		
//		In response to a button click:
		int returnVal = chooser.showOpenDialog(null);
		if ( returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			textField.setText(file.getAbsolutePath());
			textField.repaint();
		}

	}
}

