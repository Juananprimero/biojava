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

/**
 * Cross product of a list of alphabets.  This is provided primarily
 * to assist in the implemention of a `multi-headed' hidden markov
 * model.  For instance, in a pair HMM intended for aligning DNA
 * sequence, the emmision alphabet will be (DNA, gap) x (DNA, gap).
 *
 * To actualy make a CrossProductAlphabet, either roll your own, or use
 * the CrossProductAlphabetFactory object.
 *
 * @author Thomas Down
 */

public interface CrossProductAlphabet extends Alphabet {
    /**
     * Return an ordered List of the alphabets which make up this
     * compound alphabet.  The returned list should be immutable.
     *
     */

    public List getAlphabets();

    /**
     * Get a symbol from the CrossProductAlphabet which corresponds
     * to the specified ordered list of symbols.
     *
     * @param rl A list of symbols.
     * @throws IllegalSymbolException if the members of rl are
     *            not Symbols over the alphabets returned from
     *            <code>getAlphabets</code>
     */

    public CrossProductSymbol getSymbol(List rl) 
	         throws IllegalSymbolException;
}
