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

package org.biojava.bio;

import java.util.*;
import java.io.*;

import org.biojava.utils.*;

/**
 * Annotation implementation which allows new key-value
 * pairs to be layered on top of an underlying Annotation.
 * When <code>getProperty</code> is called, we first check
 * for a value stored in the overlay.  If this fails, the
 * underlying <code>Annotation</code> is checked.  Values
 * passed to <code>setProperty</code> are always stored
 * within the overlay.
 *
 * @author Thomas Down
 * @author Matthew Pocock
 * @author Greg Cox
 */

public class OverlayAnnotation
  extends
    AbstractChangeable
  implements
    Annotation,
    Serializable
{
  private transient ChangeListener propertyForwarder = null;

  private Annotation parent;
  private Map overlay = null;

  protected ChangeSupport getChangeSupport(ChangeType changeType) {
    ChangeSupport changeSupport = super.getChangeSupport(changeType);

    if(
      ((changeType == null) || (changeType.isMatchingType(Annotation.PROPERTY))) &&
      (propertyForwarder == null)
    ) {
      propertyForwarder = new PropertyForwarder(
        OverlayAnnotation.this,
        changeSupport
      );
      parent.addChangeListener(
        propertyForwarder,
        Annotation.PROPERTY
      );
    }
    
    return changeSupport;
  }

    protected Map getOverlay() {
	if (overlay == null)
	    overlay = new HashMap();
	return overlay;
    }

  /**
   * Construct an annotation which can overlay new key-value
   * pairs onto an underlying annotation.
   *
   * @param par The `parent' annotation, on which new
   *            key-value pairs can be layered.
   */

  public OverlayAnnotation(Annotation par) {
    parent = par;
  }

  public void setProperty(Object key, Object value)
    throws ChangeVetoException 
  {
    if(hasListeners()) {
      ChangeSupport changeSupport = getChangeSupport(Annotation.PROPERTY);
      ChangeEvent ce = new ChangeEvent(
        this,
        Annotation.PROPERTY,
        new Object[] {key, value},
        new Object[] {key, getProperty(key)}
      );
      synchronized(changeSupport) {
        changeSupport.firePreChangeEvent(ce);
        getOverlay().put(key, value);
        changeSupport.firePostChangeEvent(ce);
      }
    } else {
      getOverlay().put(key, value);
    }
  }
  
  public void removeProperty(Object key)
    throws ChangeVetoException 
  {
      if (overlay == null || !overlay.containsKey(key)) {
          if (parent.containsProperty(key)) {
              throw new ChangeVetoException("Can't remove properties from the parent annotation");
          } else {
              throw new NoSuchElementException("Property doesn't exist: " + key);
          }
      }
      
    if(hasListeners()) {
      ChangeSupport changeSupport = getChangeSupport(Annotation.PROPERTY);
      ChangeEvent ce = new ChangeEvent(
        this,
        Annotation.PROPERTY,
        new Object[] {key, null},
        new Object[] {key, getProperty(key)}
      );
      synchronized(changeSupport) {
        changeSupport.firePreChangeEvent(ce);
        getOverlay().remove(key);
        changeSupport.firePostChangeEvent(ce);
      }
    } else {
      getOverlay().remove(key);
    }
  }

  public Object getProperty(Object key) {
      Object val = null;
      if (overlay != null)
	  val = overlay.get(key);
      if (val != null) {
	  return val;
      }
      return parent.getProperty(key);
  }

  public boolean containsProperty(Object key) {
     if(
       (overlay != null) &&
       (overlay.containsKey(key))
     ) {
       return true;
     } else {
       return parent.containsProperty(key);
     }
   }

    private Object getPropertySilent(Object key) {
	try {
	    return getProperty(key);
	} catch (NoSuchElementException ex) {
	    return null;
	}
    }

  /**
   * Return a <code>Set</code> containing all key objects
   * visible in this annotation.  The <code>Set</code> is
   * unmodifiable, but will dynamically reflect changes made
   * to the annotation.
   */
  public Set keys() {
    return new OAKeySet();
  }

  /**
   * Return a <code>Map</code> view onto this annotation.
   * The returned <code>Map</code> is unmodifiable, but will
   * dynamically reflect any changes made to this annotation.
   */

  public Map asMap() {
    return new OAMap();
  }

  private class OAKeySet extends AbstractSet {
    private Set parentKeys;

     private OAKeySet() {
       super();
       parentKeys = parent.keys();
     }

     public Iterator iterator() {
       return new Iterator() {
         Iterator oi = (overlay != null) ? overlay.keySet().iterator()
	                                 : Collections.EMPTY_SET.iterator();
         Iterator pi = parentKeys.iterator();
         Object peek = null;

         public boolean hasNext() {
           if (peek == null)
             peek = nextObject();
             return (peek != null);
         }

         public Object next() {
           if (peek == null) {
             peek = nextObject();
           }
           if (peek == null) {
             throw new NoSuchElementException();
           }
           Object o = peek;
           peek = null;
           return o;
         }

         private Object nextObject() {
           if (oi.hasNext()) {
             return oi.next();
           }
           Object po = null;
           while (po == null && pi.hasNext()) {
             po = pi.next();
             if (overlay != null && overlay.containsKey(po)) {
               po = null;
             }
           }
           return po;
         }

         public void remove() {
           throw new UnsupportedOperationException();
         }
       };
     }

     public int size() {
       int i = 0;
       Iterator keys = iterator();
       while(keys.hasNext()) {
         keys.next();
         ++i;
       }
       return i;
     }

     public boolean contains(Object o) {
       return (overlay != null && overlay.containsKey(o)) || parentKeys.contains(o);
     }
  }

  private class OAEntrySet extends AbstractSet {
    OAKeySet ks;

    private OAEntrySet() {
	    super();
	    ks = new OAKeySet();
    }

    public Iterator iterator() {
	    return new Iterator() {
        Iterator ksi = ks.iterator();

        public boolean hasNext() {
          return ksi.hasNext();
        }

        public Object next() {
          Object k = ksi.next();
          Object v = getProperty(k);
          return new OAMapEntry(k, v);
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
	    };
    }

    public int size() {
	    return ks.size();
    }
  }

  private class OAMapEntry implements Map.Entry {
    private Object key;
    private Object value;

    private OAMapEntry(Object key, Object value) {
	    this.key = key;
	    this.value = value;
    }

    public Object getKey() {
	    return key;
    }

    public Object getValue() {
	    return value;
    }

    public Object setValue(Object v) {
	    throw new UnsupportedOperationException();
    }

    public boolean equals(Object o) {
	    if (! (o instanceof Map.Entry)) {
        return false;
      }

	    Map.Entry mo = (Map.Entry) o;
	    return ((key == null ? mo.getKey() == null : key.equals(mo.getKey())) &&
		    (value == null ? mo.getValue() == null : value.equals(mo.getValue())));
    }

    public int hashCode() {
	    return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }
  }

  private class OAMap extends AbstractMap {
    OAEntrySet es;
    OAKeySet ks;

    private OAMap() {
	    super();
	    ks = new OAKeySet();
	    es = new OAEntrySet();
    }

    public Set entrySet() {
	    return es;
    }

    public Set keySet() {
	    return ks;
    }

	  public Object get(Object key) {
	    try {
        return getProperty(key);
	    } catch (NoSuchElementException ex) {
	    }

	    return null;
    }
  }

  protected class PropertyForwarder extends ChangeForwarder {
    public PropertyForwarder(Object source, ChangeSupport cs) {
      super(source, cs);
    }

    public ChangeEvent generateEvent(ChangeEvent ce) {
      ChangeType ct = ce.getType();
      if(ct == Annotation.PROPERTY) {
        Object curVal = ce.getChange();
        if(curVal instanceof Object[]) {
          Object[] cur = (Object []) curVal;
          if(cur.length == 2) {
            Object key = cur[0];
            Object value = cur[0];
            if(getProperty(key) != value) {
              return new ChangeEvent(
                getSource(),
                Annotation.PROPERTY,
                curVal,
                ce.getPrevious(),
                ce
              );
            }
          }
        }
      }
      return null;
    }
  }
}
