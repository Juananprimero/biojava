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
import org.biojava.bio.*;
import org.biojava.bio.symbol.*;

/**
 * The central port-of-call for all information and functionality specific to
 * SymbolLists over the protein alphabet.
 *
 * @author Matthew Pocock
 */
public class ProteinTools {
  private static final FiniteAlphabet proteinAlpha;
  private static final FiniteAlphabet proteinTAlpha;
  
  static {
    try {
      proteinAlpha = (FiniteAlphabet) AlphabetManager.alphabetForName("PROTEIN");
      proteinTAlpha = (FiniteAlphabet) AlphabetManager.alphabetForName("PROTEIN-TERM");
    } catch (Exception e) {
      throw new BioError(e, " Could not initialize ProteinTools");
    }
  }
  /**
  *Gets the protein alphabet
  */
  public static final FiniteAlphabet getAlphabet() {
    return proteinAlpha;
  }
  
  /**
  *Gets the protein alphabet including the translation termination symbols
  */
  public static final FiniteAlphabet getTAlphabet() {
    return proteinTAlpha;
  }
}
