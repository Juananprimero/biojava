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


package org.biojava.bio.dp;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import org.biojava.utils.*;
import org.biojava.bio.*;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.io.*;
import org.biojava.bio.dist.*;

public class XmlMarkovModel {
  public static WeightMatrix readMatrix(Element root)
  throws IllegalSymbolException, IllegalAlphabetException, BioException {
    Element alphaE = (Element) root.getElementsByTagName("alphabet").item(0);
    Alphabet sa = AlphabetManager.alphabetForName(
      alphaE.getAttribute("name"));
    if(! (sa instanceof FiniteAlphabet)) {
      throw new IllegalAlphabetException(
        "Can't read WeightMatrix over infinite alphabet " +
        sa.getName() + " of type " + sa.getClass()
      );
    }
    FiniteAlphabet seqAlpha = (FiniteAlphabet) sa;
    SymbolParser symParser = seqAlpha.getParser("token");
    SymbolParser nameParser = seqAlpha.getParser("name");
    
    int columns = 0;
    NodeList colL = root.getElementsByTagName("col");
    for(int i = 0; i < colL.getLength(); i++) {
      int indx = Integer.parseInt(((Element) colL.item(i)).getAttribute("indx"));
      columns = Math.max(columns, indx);
    }
    
    WeightMatrix wm = new SimpleWeightMatrix(seqAlpha, columns, DistributionFactory.DEFAULT);

    colL = root.getElementsByTagName("col");
    for(int i = 0; i < colL.getLength(); i++) {
      Element colE = (Element) colL.item(i);
      int indx = Integer.parseInt(colE.getAttribute("indx")) - 1;
      NodeList weights = colE.getElementsByTagName("weight");
      for(int j = 0; j < weights.getLength(); j++) {
        Element weightE = (Element) weights.item(j);
        String symName = weightE.getAttribute("res");
        Symbol sym;
        if(symName.length() > 1) {
          sym = nameParser.parseToken(symName);
        } else {
          sym = symParser.parseToken(symName);
        }
        try {
          wm.getColumn(indx).setWeight(sym, Double.parseDouble(weightE.getAttribute("prob")));
        } catch (ChangeVetoException cve) {
          throw new BioError("Assertion failure: Should be able to set the weights");
        }
      }      
    }
    
    return wm;
  }
  
  public static MarkovModel readModel(Element root)
  throws BioException, IllegalSymbolException, IllegalAlphabetException {
    if(root.getTagName().equals("WeightMatrix")) {
      return new WMAsMM(readMatrix(root));
    }
    
    int heads = Integer.parseInt(root.getAttribute("heads"));
    Element alphaE = (Element) root.getElementsByTagName("alphabet").item(0);
    Alphabet seqAlpha = AlphabetManager.alphabetForName(
      alphaE.getAttribute("name")
    );
    SimpleMarkovModel model = new SimpleMarkovModel(heads, seqAlpha);
    int [] advance = new int[heads];
    for(int i = 0; i < heads; i++) {
      advance[i] = 1;
    }
      
    SymbolParser nameParser = null;
    SymbolParser symbolParser = null;
    
    try {
      nameParser = seqAlpha.getParser("name");
    } catch (NoSuchElementException nsee) {
    }
    
    try {
      symbolParser = seqAlpha.getParser("token");
    } catch (NoSuchElementException nsee) {
    }
    
    if(nameParser == null && symbolParser == null) {
      throw new BioException(
        "Couldn't find a parser for alphabet " +
        seqAlpha.getName()
      );
    }
    
    Map nameToState = new HashMap();
    nameToState.put("_start_", model.magicalState());
    nameToState.put("_end_", model.magicalState());
    nameToState.put("_START_", model.magicalState());
    nameToState.put("_END_", model.magicalState());
    NodeList states = root.getElementsByTagName("state");
    DistributionFactory dFact;
    if( (seqAlpha.getAlphabets().size() > 1) &&
        seqAlpha.getAlphabets().equals(
          Collections.nCopies(
            seqAlpha.getAlphabets().size(),
            seqAlpha.getAlphabets().get(0)
          )
        )
    ) {
      dFact = OrderNDistributionFactory.DEFAULT;
    } else {
      dFact = DistributionFactory.DEFAULT;
    }
    for(int i = 0; i < states.getLength(); i++) {
      Element stateE = (Element) states.item(i);
      String name = stateE.getAttribute("name");
      Distribution dis = dFact.createDistribution(seqAlpha);
      EmissionState state = new SimpleEmissionState(
        name, Annotation.EMPTY_ANNOTATION, advance, dis
      );
      
      nameToState.put(name, state);
      NodeList weights = stateE.getElementsByTagName("weight");
      for(int j = 0; j < weights.getLength(); j++) {
        Element weightE = (Element) weights.item(j);
        String symName = weightE.getAttribute("res");
        if(symName == null || "".equals(symName)) {
          symName = weightE.getAttribute("sym");
        }
        Symbol sym;
        if(symName.length() == 1) {
          if(symbolParser != null) {
            sym = symbolParser.parseToken(symName);
          } else {
            sym = nameParser.parseToken(symName);
          }
        } else {
          try {
            if(nameParser != null) {
              sym = nameParser.parseToken(symName);
            } else {
              sym = symbolParser.parseToken(symName);
            }
          } catch (IllegalSymbolException ise) {
            throw new BioException(ise, "Can't extract symbol from " + weightE + " in " + stateE);
          }
        }
        try {
          dis.setWeight(sym, Double.parseDouble(weightE.getAttribute("prob")));
        } catch (ChangeVetoException cve) {
          throw new BioError(
            cve, "Assertion failure: Should be able to edit distribution"
          );
        }
      }
      
      try {
        model.addState(state);
      } catch (ChangeVetoException cve) {
        throw new BioError(
          cve, "Assertion failure: Should be able to add states to model"
        );
      }
    }

    NodeList transitions = root.getElementsByTagName("transition");
    for(int i = 0; i < transitions.getLength(); i++) {
      Element transitionE = (Element) transitions.item(i);
      State from = (State) nameToState.get(transitionE.getAttribute("from"));
      State to = (State) nameToState.get(transitionE.getAttribute("to"));
      double prob = Double.parseDouble(transitionE.getAttribute("prob"));
      try {
        model.createTransition(from, to);
      } catch (IllegalSymbolException ite) {
        throw new BioError(
          ite, 
          "We should have unlimited write-access to this model. " +
          "Something is very wrong."
        );
      } catch (ChangeVetoException cve) {
        throw new BioError(
          cve, 
          "We should have unlimited write-access to this model. " +
          "Something is very wrong."
        );
      }
    }
    
	for(int i = 0; i < transitions.getLength(); i++) {
      Element transitionE = (Element) transitions.item(i);
      State from = (State) nameToState.get(transitionE.getAttribute("from"));
      State to = (State) nameToState.get(transitionE.getAttribute("to"));
      double prob = Double.parseDouble(transitionE.getAttribute("prob"));
      try {
        model.getWeights(from).setWeight(to, prob);
      } catch (IllegalSymbolException ite) {
        throw new BioError(
          ite, 
          "We should have unlimited write-access to this model. " +
          "Something is very wrong."
        );
      } catch (ChangeVetoException cve) {
        throw new BioError(
          cve, 
          "We should have unlimited write-access to this model. " +
          "Something is very wrong."
        );
      }
    }
    return model;
  }
 
  public static void writeMatrix(WeightMatrix matrix, PrintStream out) throws Exception {
    FiniteAlphabet symA = (FiniteAlphabet) matrix.getAlphabet();
    
    out.println("<MarkovModel>\n  <alphabet name=\"" + symA.getName() + "\"/>");
    
    for(int i = 0; i < matrix.columns(); i++) {
      out.println("  <col indx=\"" + (i+1) + "\">");
      for(Iterator si = symA.iterator(); si.hasNext(); ) {
        Symbol s = (Symbol) si.next();
        out.println("    <weight sym=\"" + s.getName() +
                             "\" prob=\"" + matrix.getColumn(i).getWeight(s) + "\"/>");
        }
      out.println("  </col>");
    }
    
    out.println("</MarkovModel>");
  }
  
  public static void writeModel(MarkovModel model, PrintStream out)
  throws Exception {
    model = DP.flatView(model);
    FiniteAlphabet stateA = model.stateAlphabet();
    FiniteAlphabet symA = (FiniteAlphabet) model.emissionAlphabet();
    SymbolList stateR = stateA.symbols();
    List stateL = stateR.toList();
    SymbolList symR = symA.symbols();
    
    out.println("<MarkovModel heads=\"" + model.heads() + "\">");
    out.println("<alphabet name=\"" + symA.getName() + "\"/>");
    
    // print out states & scores
    for(Iterator stateI = stateL.iterator(); stateI.hasNext(); ) {
      State s = (State) stateI.next();
      if(! (s instanceof MagicalState)) {
        out.println("  <state name=\"" + s.getName() + "\">");
        if(s instanceof EmissionState) {
          EmissionState es = (EmissionState) s;
          Distribution dis = es.getDistribution();
          for(Iterator symI = symR.iterator(); symI.hasNext(); ) {
            Symbol sym = (Symbol) symI.next();
            out.println("    <weight sym=\"" + sym.getName() +
                        "\" prob=\"" + dis.getWeight(sym) + "\"/>");
          }
        }
        out.println("  </state>");
      }
    }

    // print out transitions
    for(Iterator i = stateL.iterator(); i.hasNext(); ) {
      State from = (State) i.next();
      printTransitions(model, from, out);
    }
    
    out.println("</MarkovModel>");
  }
  
  static private void printTransitions(MarkovModel model, State from, PrintStream out) throws IllegalSymbolException {
    for(Iterator i = model.transitionsFrom(from).iterator(); i.hasNext(); ) {
      State to = (State) i.next();
      try {
      out.println("  <transition from=\"" + ((from instanceof MagicalState) ? "_start_" : from.getName()) +
                             "\" to=\"" + ((to instanceof MagicalState) ? "_end_" : to.getName()) +
                             "\" prob=\"" + model.getWeights(from).getWeight(to) + "\"/>");
      } catch (IllegalSymbolException ite) {
        throw new BioError(ite, "Transition listed in transitionsFrom(" +
                           from.getName() + ") has dissapeared");
      }
    }
  }
}
