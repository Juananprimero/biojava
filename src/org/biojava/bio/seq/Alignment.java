/*
 *                    BioJava development code
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
 */


package org.biojava.bio.seq;

import java.util.*;

import org.biojava.bio.seq.*;

/**
 * An alignment containing multiple <span class="type">ResidueList</span>s.
 * <P>
 * The alignment can be thought of a rectangular array of
 * <span class="type">Residue</span>s. Each
 * row is indexed by a label and each column is indexed by offset (counting from
 * 0).
 * <P>
 * Alternatively, it can be thought of as a <span class="type">ResidueList</span>
 * where each <span class="type">Residue</span> is
 * a list of <span class="type">Residue</span>s in that column.
 * <P>
 * To create gapped alignments, use <span class="type">ResidueList</span>s with
 * gaps. The most flexible way to do this will be to leverage
 * <span class="type">GappedResidueList</span> objects.
 */
public interface Alignment extends ResidueList {
  /**
   * The list of ResidueLists in the alignment.
   * <P>
   * The index in the list is the same as the index in the alignment.
   * Each ResidueList object will only be in the alignment once. However, a
   * single underlying ResidueList may have more than one view within an
   * alignment, each represented by a different GappedResidueList.
   *
   * @return  the List of all ResidueLists in the alignment
   */
  List getLabels();
  
  /**
   * Retrieve a residue by label and column.
   *
   * @param res the ResidueList to retrieve from
   * @param column  the index of the column to retrieve
   * @return  the residue in the residue list associated with the label at the given column
   * @throws NoSuchElementException if there is no row for 'label'
   */
  Residue residueAt(Object label, int column)
  throws NoSuchElementException;
  
  /**
   * Retrieve a single row of the alignment by label.
   *
   * @return  a ResidueList that contains each token in a row of the alignment
   * @throws NoSuchElementException if there is no row for 'label'
   */
  ResidueList residueListForLabel(Object label)
  throws NoSuchElementException;
  
  /**
   * Make a view onto this alignment.
   * <P>
   * If labels is null, then each label will be kept. Otherwise, only those in
   * labels will be kept.
   * If loc is null, then the entire length of the alignment will be kept.
   * If loc is not null, then only the columns within the location will be kept.
   *
   * @param labels the Set of sequences to include by label
   * @param loc the Location to include
   * @return  a sub Alignment
   * @throws  NoSuchElementException if labels contains any item that is not a label
   */
  Alignment subAlignment(Set labels, Location loc)
  throws NoSuchElementException;
}

