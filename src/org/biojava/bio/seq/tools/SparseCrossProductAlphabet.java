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


package org.biojava.bio.seq.tools;

import java.util.*;

import org.biojava.bio.seq.*;

/**
 * Cross product of a list of arbitrary alphabets.  This is a memory efficicent
 * implementation of CrossProductAlphabet that instantiates residues as they are
 * needed. This is required as alphabets can get prohibatively large very
 * quickly (e.g. align 200 proteins & you need 20^200 tokens).
 * 
 * @author Matthew Pocock
 */

class SparseCrossProductAlphabet implements FiniteAlphabet, CrossProductAlphabet {
  private final int size;
  private final List alphas;
  private final Map knownResidues;
  private char symbolSeed = 'A';
  
  SparseCrossProductAlphabet(List alphas) {
    this.alphas = alphas;
    knownResidues = new HashMap();
    int size = 1;
    for(Iterator i = alphas.iterator(); i.hasNext(); ) {
      FiniteAlphabet a = (FiniteAlphabet) i.next();
      size *= a.size();
    }
    this.size = size;
  }
  
  public ResidueList residues() {
    return null;
  }
  
  public String getName() {
    StringBuffer name = new StringBuffer("(");
    for (int i = 0; i < alphas.size(); ++i) {
	    Alphabet a = (Alphabet) alphas.get(i);
	    name.append(a.getName());
	    if (i < alphas.size() - 1) {
        name.append(" x ");
      }
    }
    name.append(")");
    return name.toString();
  }

  public int size() {
    return size;
  }
  
  public boolean contains(Residue r) {
    if(! (r instanceof CrossProductResidue)) {
      return false;
    }
    return knownResidues.values().contains(r);
  }

  public void validate(Residue r)
  throws IllegalResidueException {
    if(! (r instanceof CrossProductResidue)) {
	    throw new IllegalResidueException(
        "CrossProductAlphabet " + getName() + " does not accept " + r.getName() +
        " as it is not an instance of CrossProductResidue"
      );
    }
    
    if(!contains(r)) {
      throw new IllegalResidueException(
        r,
        "Residue " + r.getName() + " is not a member of the alphabet " +
        getName()
      );
    }
  }
  
  
  public Annotation getAnnotation() {
    return Annotation.EMPTY_ANNOTATION;
  }

  public List getAlphabets() {
    return alphas;
  }
  
  public Iterator iterator() {
    return knownResidues.values().iterator();
  }
  
  public ResidueParser getParser(String name)
  throws NoSuchElementException, SeqException {
    if(name == "name") {
      return new CrossProductResidueNameParser(this);
    }
    throw new NoSuchElementException(
      "No parser for " + name + " is defined for " + getName()
    );
  }

  private AlphabetManager.ListWrapper gopher =
    new AlphabetManager.ListWrapper();

  public CrossProductResidue getResidue(List rList)
  throws IllegalResidueException {
    if(rList.size() != alphas.size()) {
      throw new IllegalResidueException(
        "List of residues is the wrong length (" + alphas.size() +
        ":" + rList.size() + ")"
      );
    }
    
    CrossProductResidue r;
    synchronized(gopher) {
      gopher.l = rList;
      r = (CrossProductResidue) knownResidues.get(gopher);
    }

    if(r == null) {
      for(Iterator i = rList.iterator(), j = alphas.iterator(); i.hasNext(); ) {
        Residue res = (Residue) i.next();
        Alphabet alp = (Alphabet) j.next();
        try {
          alp.validate(res);
        } catch (IllegalResidueException ire) {
          throw new IllegalResidueException(
            ire,
            "Can't retrieve residue for " +
            new SimpleCrossProductResidue(rList, '?') + " in alphabet " +
            getName()
          );
        }
      }
      List l = new ArrayList(rList);
      r = new SimpleCrossProductResidue(l, symbolSeed++);
      knownResidues.put(new AlphabetManager.ListWrapper(l), r);
    }
    
    return r;
  }
}
