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

package org.biojava.bio.gui.sequence;

import java.io.Serializable;
import java.util.*;

import java.awt.*;
import java.awt.geom.*;

import org.biojava.utils.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.symbol.*;

import java.util.List;

public class SequenceRendererWrapper
extends AbstractChangeable
implements SequenceRenderer, Serializable {
  public static ChangeType RENDERER = new ChangeType(
    "The renderer used to render the filtered features has changed",
    "org.biojava.bio.gui.sequence.FilteringRenderer",
    "RENDERER",
    SequenceRenderContext.LAYOUT
  );
  
  private SequenceRenderer renderer;
  private transient ChangeForwarder rendForwarder;
  
  public SequenceRendererWrapper() {}
  public SequenceRendererWrapper(SequenceRenderer renderer) {
    this.renderer = renderer;
  }
  
  protected ChangeSupport getChangeSupport(ChangeType ct) {
    ChangeSupport cs = super.getChangeSupport(ct);
    
    if(rendForwarder == null) {
      rendForwarder = new SequenceRenderer.RendererForwarder(this, cs);
      if((renderer != null) && (renderer instanceof Changeable)) {
        Changeable c = (Changeable) this.renderer;
        c.addChangeListener(
          rendForwarder,
          SequenceRenderContext.REPAINT
        );
      }
    }
    
    return cs;
  }
  
  public void setRenderer(SequenceRenderer renderer)
  throws ChangeVetoException {
    if(hasListeners()) {
      ChangeEvent ce = new ChangeEvent(
        this, RENDERER,
        renderer, this.renderer
      );
      ChangeSupport cs = getChangeSupport(RENDERER);
      synchronized(cs) {
        cs.firePreChangeEvent(ce);
        if((renderer != null) && (renderer instanceof Changeable)) {
          Changeable c = (Changeable) this.renderer;
          c.removeChangeListener(rendForwarder);
        }
        this.renderer = renderer;
        if(renderer instanceof Changeable) {
          Changeable c = (Changeable) renderer;
          c.removeChangeListener(rendForwarder);
        }
        cs.firePostChangeEvent(ce);
      }
    } else {
      this.renderer = renderer;
    }
  }
  
  public SequenceRenderer getRenderer() {
    return this.renderer;
  }
  
  public double getDepth(SequenceRenderContext src, int min, int max) {
    return getRenderer().getDepth(src, min, max);
  }
  
  public double getMinimumLeader(SequenceRenderContext src) {
    return getRenderer().getMinimumLeader(src);
  }
  
  public double getMinimumTrailer(SequenceRenderContext src) {
    return getRenderer().getMinimumTrailer(src);
  }
  
  public void paint(
    Graphics2D g,
    SequenceRenderContext src,
    int min, int max
  ) {
    getRenderer().paint(g, src, min, max);
  }
}

