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

package org.biojava.bio.symbol;

import java.util.*;
import java.io.*;

import org.biojava.bio.*;

/**
 * A simple implementation of an Alignment.
 * <p>
 * This is a simple-stupid implementation that is made from a set of same-lengthed
 * SymbolList objects each with an associated label. It does not handle differently
 * lengthed sequences and doesn't contain any gap-editing concepts.
 *
 * @author Matthew Pocock
 * @author Greg Cox
 */
public class SimpleAlignment
extends AbstractSymbolList implements Alignment, Serializable {
  private Map labelToSymbolList;
  private List labels;
  private Alphabet alphabet;
  private int length;

  public int length() {
    return length;
  }

  public Alphabet getAlphabet() {
    return alphabet;
  }

  public Symbol symbolAt(int index) {
    try {
      return alphabet.getSymbol(new ColAsList(index));
    } catch (IllegalSymbolException ire) {
      throw new BioError(
        ire,
        "Somehow my crossproduct alphabet is incompatible with column " + index
      );
    }
  }

  public List getLabels() {
    return labels;
  }

  public Symbol symbolAt(Object label, int column) {
    return symbolListForLabel(label).symbolAt(column);
  }

  public Alignment subAlignment(Set labels, Location loc)
  throws NoSuchElementException {
    Map labelsToResList = new HashMap();
    Iterator i;
    if(labels != null) {
      i = labels.iterator();
    } else {
      i = getLabels().iterator();
    }
    while(i.hasNext()) {
      Object label = i.next();
      SymbolList sym = symbolListForLabel(label);
      if(loc != null) {
        sym = loc.symbols(sym);
      }
      labelsToResList.put(label, sym);
    }
    return new SimpleAlignment(labelsToResList);
  }

  public SymbolList symbolListForLabel(Object label)
  throws NoSuchElementException {
    SymbolList rl = (SymbolList) labelToSymbolList.get(label);
    if(rl == null) {
      throw new NoSuchElementException("No symbol list associated with label " + label);
    }
    return rl;
  }

  /**
   * Generate an alignment from a list of SymbolLists.
   * <p>
   * The SymbolLists must all be of the same length.
   *
   * @param labelToResList  the label-to-symbol list mapping
   * @throws IllegalArgumentException if the SymbolLists are not the same
   *         length
   */
  public SimpleAlignment(Map labelToResList) throws IllegalArgumentException {
    this.labels = Collections.unmodifiableList(new ArrayList(labelToResList.keySet()));
    this.labelToSymbolList = labelToResList;

    int length = -1;
    List alphaList = new ArrayList();
    for(Iterator li = labels.iterator(); li.hasNext(); ) {
      Object label = li.next();
      try {
        SymbolList rl = symbolListForLabel(label);
        alphaList.add(rl.getAlphabet());
        if(length == -1) {
          length = rl.length();
        } else {
          if(rl.length() != length) {
            StringBuffer sb = new StringBuffer();
            for(Iterator labI = labels.iterator(); labI.hasNext(); ) {
              Object lab = labI.next();
              sb.append("\n\t" + lab + " (" + symbolListForLabel(lab).length() + ")");
            }
            throw new IllegalArgumentException(
              "All SymbolLists must be the same length: " + sb.substring(0)
            );
          }
        }
      } catch (NoSuchElementException nsee) {
        if(labelToSymbolList.containsKey(label)) {
          throw new IllegalArgumentException(
            "The symbol list associated with " + label + " is null"
          );
        } else {
          throw new BioError(nsee, "Something is screwey - map is lieing about key/values");
        }
      }
    }

    this.alphabet = AlphabetManager.getCrossProductAlphabet(alphaList);
    this.length = length;
  }

  /**
   * Makes a column of the alignment behave like a list.
   *
   * @author Matthew Pocock
   */
  private final class ColAsList extends AbstractList implements Serializable {
    private final int col;

    public ColAsList(int col) {
      this.col = col;
    }

    protected ColAsList() {
      this.col = 0;
    }

    public Object get(int indx) {
      return symbolAt(labels.get(indx), col);
    }

    public int size() {
      return labels.size();
    }
  }
}
