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

/**
 * A no-frills implementation of ReversibleTranslationTable that uses two Maps
 * to map between symbols in a finite source alphabet into a finite target
 * alphabet.
 *
 * @author Matthew Pocock
 */
public class SimpleReversibleTranslationTable
extends SimpleTranslationTable
implements ReversibleTranslationTable, Serializable {
  /**
   * Map from targets to source symbols
   */
  private Map revMap;

  public void setTranslation(Symbol from, Symbol to)
  throws IllegalSymbolException {
    super.setTranslation(from, to);
    revMap.put(to, from);
  }
  
  public Symbol untranslate(Symbol res)
  throws IllegalSymbolException {
    Symbol r = (Symbol) revMap.get(res);
    if(r == null) {
      getTargetAlphabet().validate(res);
      throw new IllegalSymbolException(
        "Unable to map " + res.getName()
      );
    }
    return r;
  }

  /**
   * Construct a new translation table.
   *
   * @param source  the source FiniteAlphabet
   * @param target the target FiniteAlphabet
   * @throws IllegalAlphabetException if the alphabets are of different sizes
   */
  public SimpleReversibleTranslationTable(
    FiniteAlphabet source, FiniteAlphabet target
  ) throws IllegalAlphabetException {
    super(source, target);
    if(source.size() != target.size()) {
      throw new IllegalAlphabetException(
        "Couldn't create translation table as " +
        "the alphabets were different sizes: " +
        source.size() + ":" + source.getName() +
        target.size() + ":" + target.getName()
      );
    }

    this.revMap = new HashMap();
  }
}
