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
package seq.db;

import java.io.*;
import java.util.*;

import org.biojava.bio.*;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.seq.io.*;
import org.biojava.bio.seq.db.*;

/**
 * This demo file is a simple implementation of pairwise-alignment.
 *
 * @author Matthew Pocock
 */

public class CreateIndex {
  public static void main(String[] args) {
    try {
      if(args.length != 3) {
        throw new Exception("Use: indexName format alphabet");
      }
      String indexName = args[0];
      File indexFile = new File(indexName+".index");
      String formatName = args[1];
      String alphaName = args[2];
      Alphabet alpha = resolveAlphabet(alphaName);
      SymbolParser sParser = alpha.getParser("token");
      
      SequenceFormat sFormat = null;
      SequenceBuilderFactory sFact = null;
      if(formatName.equals("fasta")) {
	  sFormat = new FastaFormat();
	  sFact = new FastaDescriptionLineParser.Factory(SimpleSequenceBuilder.FACTORY);
      } else if(formatName.equals("embl")) {
	  sFormat = new EmblLikeFormat();
	  sFact = new EmblProcessor.Factory(SimpleSequenceBuilder.FACTORY);
      } else {
	  throw new Exception("Format must be one of {embl, fasta}");
      }
      
      IndexedSequenceDB seqDB = IndexedSequenceDB.createDB(
        indexName,
        indexFile,
        sFormat,
        sFact,
        sParser,
        IDMaker.byName
      );
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }
  
  private static Alphabet resolveAlphabet(String alphaName)
  throws IllegalArgumentException {
    alphaName = alphaName.toLowerCase();
    if(alphaName.equals("dna")) {
      return DNATools.getDNA();
    } else if(alphaName.equals("protein")) {
      return ProteinTools.getAlphabet();
    } else {
      throw new IllegalArgumentException("Could not find alphabet for " + alphaName);
    }
  }
}

