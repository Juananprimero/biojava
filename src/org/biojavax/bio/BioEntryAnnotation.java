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

package org.biojavax.bio;

import java.util.NoSuchElementException;
import org.biojava.bio.Annotation;
import org.biojava.utils.ChangeVetoException;
import org.biojavax.ontology.ComparableTerm;

/**
 *
 * @author Richard Holland
 */
public interface BioEntryAnnotation extends Annotation {
    
    public void clear();
    
}
