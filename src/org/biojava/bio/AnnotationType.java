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

import org.biojava.utils.*;

/**
 * <p>A type to constrain annotation bundles.</p>
 *
 * <p><code>AnnotationType</code> instances can be used to validate an
 * <code>Annotation</code> to check that it has the appropriate
 * properties and that they are of the right type.</p>
 *
 * @since 1.3
 * @author Matthew Pocock
 * @author Keith James (docs).
 */
public interface AnnotationType {
    /**
     * The type that accepts all annotations and is the supertype of all
     * other annotations. Only an empty annotation is an exact instance of
     * this type.
     */
    public static final AnnotationType ANY = new AnyOfType(
      PropertyConstraint.ANY,
      CardinalityConstraint.ANY
    );

    /**
     * Validate an Annotation against this AnnotationType.
     *
     * @param ann the Annotation to validate.
     * @return true if ann conforms to this type and false if it doesn't.
     */
    public boolean instanceOf(Annotation ann);
    
    /**
     * Validate an Annotation to an exact type. Exact matches will also
     * return true for instanceOf but will have no properties not found
     * in the type.
     *
     * @param ann the Annotation to validate.
     * @return true if ann is an instance of exactly this type
     */
     public boolean exactInstanceOf(Annotation ann);

    /**
     * <p>See if an AnnotationType is a specialisation of this type.</p>
     *
     * <p>An AnnotationType is a sub-type if it restricts each of the
     * properties of the super-type to a type that can be cast to the
     * type in the super-type. Note that this is not always a cast in
     * the pure Java sense; it may include checks on the number and
     * type of members in collections or other criteria.</p>
     *
     * @param subType an AnnotationType to check.
     * @return true if subType is a sub-type of this type.
     */
    public boolean subTypeOf(AnnotationType subType);

    /**
     * <p>Retrieve the constraint that will be applied to all
     * properties with a given key.</p>
     *
     * <p>For an <code>Annotation</code> to be accepted, each key in
     * getProperties() must be present in the annotation and each of the
     * values associated with those properties must match the
     * constraint.</p>
     *
     * @param key the property to be validated.
     * @return PropertyConstraint the constraint by which the values
     * must be accepted.
     */
    public PropertyConstraint getPropertyConstraint(Object key);
    
    /**
     * <p>Retrieve the cardinality constraint associated with properties.</p>
     *
     * <p>For an annotation to be acceptable, the property must have a number
     * of values that matches the cardinality constraint. Common values are
     * represented by static fields of CardinalityConstraint.</p>
     *
     * @param key the property to be validated
     * @return a CardinalityConstraint giving the number of values assocated
     *         with the property
     */
    public CardinalityConstraint getCardinalityConstraint(Object key);

    /**
     * Set the constraints associated with a property.
     *
     * @param key  the name of the property to constrain
     * @param con  the PropertyConstraint to enforce
     * @param card the CardinalityCnstraint to enforce
     */
    public void setConstraints(
      Object key,
      PropertyConstraint con,
      CardinalityConstraint card
    );
    
    /**
     * <p>Retrieve the complete set of properties that must be present for
     * an <code>Annotation</code> to be accepted by this
     * <code>AnnotationType</code>.</p>
     *
     * @return the Set of properties to validate.
     */
    public Set getProperties();

    public void setProperty(Annotation ann, Object property, Object value)
        throws ChangeVetoException;

    /**
     * <p>An implementation of <code>AnnotationType</code>.</p>
     *
     * <p>To build an instance of <code>AnnotationType.Impl</code>,
     * first invoke the no-args constructor, and then use the
     * setPropertyConstraint method to build the property->constraint
     * mapping.</p>
     * 
     * @since 1.3
     * @author Matthew Pocock
     */
    public class Impl implements AnnotationType {
        private Map cons;
        private Map cards;
    
        /**
         * Create a new Impl with no constraints.
         */
        public Impl() {
            cons = new SmallMap();
            cards = new SmallMap();
        }

        public PropertyConstraint getPropertyConstraint(Object key) {
            PropertyConstraint pc = (PropertyConstraint) cons.get(key);
            if (pc == null) {
                pc = PropertyConstraint.ANY;
            }
            return pc;
        }

        public CardinalityConstraint getCardinalityConstraint(Object key) {
            CardinalityConstraint card = (CardinalityConstraint) cards.get(key);
            if (card == null) {
                card = CardinalityConstraint.ANY;
            }
            return card;
        }
        
        public void setConstraints(
          Object key,
          PropertyConstraint con,
          CardinalityConstraint card
        ) {
            cons.put(key, con);
            cards.put(key, card);
        }

        public Set getProperties() {
            return cons.keySet();
        }

        public boolean instanceOf(Annotation ann) {
          for (Iterator i = getProperties().iterator(); i.hasNext();) {
            Object key = i.next();
            PropertyConstraint con = getPropertyConstraint(key);
            CardinalityConstraint card = getCardinalityConstraint(key);
            
            if(!validate(ann, key, con, card)) {
              return false;
            }
          }

          return true;
        }
        
        private boolean validate(
          Annotation ann,
          Object key,
          PropertyConstraint con,
          CardinalityConstraint card
        ) {
          if(
            CardinalityConstraint.ZERO.equals(card) &&
            !ann.containsProperty(key)
          ) {
            return true;
          } else if(
            CardinalityConstraint.ONE.equals(card) &&
            ann.containsProperty(key) &&
            con.accept(ann.getProperty(key))
          ) {
            return true;
          } else if(
            CardinalityConstraint.ZERO_OR_ONE.equals(card) && (
              !ann.containsProperty(key) ||
              con.accept(ann.getProperty(key))
            )
          ) {
            return true;
          } else {
            if(!ann.containsProperty(key)) {
              return card.accept(0);
            } else {
              Object val = ann.getProperty(key);
              if(val instanceof Collection) {
                Collection vals = (Collection) val;
                if(!card.accept(vals.size())) {
                  return false;
                }
                for(Iterator i = vals.iterator(); i.hasNext(); ) {
                  if(!con.accept(i.next())) {
                    return false;
                  }
                }
                return true;
              } else {
                return false;
              }
            }
          }
        }
        
        public boolean exactInstanceOf(Annotation ann) {
          Set keys = new HashSet(ann.keys());
          keys.removeAll(cons.keySet());
          
          if(keys.isEmpty()) {
            return instanceOf(ann);
          } else {
            return false;
          }
        }
    
        public boolean subTypeOf(AnnotationType subType) {
            for (Iterator i = cons.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                PropertyConstraint thisPropertyConstraint = getPropertyConstraint(key);
                PropertyConstraint subPropertyConstraint = subType.getPropertyConstraint(key);
                if (! thisPropertyConstraint.subConstraintOf(subPropertyConstraint)) {
                    return false;
                }
            }

            return true;
        }

        public void setProperty(Annotation ann, Object property, Object value)
        throws ChangeVetoException {
          try {
            PropertyConstraint prop = getPropertyConstraint(property);
            CardinalityConstraint card = getCardinalityConstraint(property);
            if(card.getMax() > 1) {
              Collection vals = null;
              if(ann.containsProperty(property)) {
                vals = (Collection) ann.getProperty(property);
              } else {
                vals = new ArrayList();
                ann.setProperty(property, vals);
              }
              prop.addValue(vals, value);
            } else {
              prop.setProperty(ann, property, value);
            }
          } catch (ChangeVetoException cve) {
            throw new ChangeVetoException(cve, "Failed to change property " + property);
          }
        }
    }
    /**
     * This, like any, will accept empty annotations. If keys do exist, it will
     * expect all values to conform to a single type and cardinality.
     *
     * @author Matthew Pocock`
     */
    public class AnyOfType
    implements AnnotationType {
        private PropertyConstraint constraint;
        private CardinalityConstraint cardinality;
    
        /**
         * Create a new Impl with no constraints.
         */
        public AnyOfType(PropertyConstraint constraint, CardinalityConstraint cardinality) {
            this.constraint = constraint;
            this.cardinality = cardinality;
        }

        public PropertyConstraint getPropertyConstraint(Object key) {
            return constraint;
        }
        
        public CardinalityConstraint getCardinalityConstraint(Object key) {
          return cardinality;
        }
        

        public void setConstraints(
          Object key,
          PropertyConstraint con,
          CardinalityConstraint card
        ) {
          constraint = con;
          cardinality = card;
        }

        public Set getProperties() {
            return Collections.EMPTY_SET;
        }

        public boolean instanceOf(Annotation ann) {
            for (Iterator i = ann.asMap().values().iterator(); i.hasNext();) {
                Object val = i.next();
                if(!constraint.accept(val)) {
                    return false;
                }
            }

            return true;
        }
        
        public boolean exactInstanceOf(Annotation ann) {
          return ann.keys().isEmpty();
        }
    
        public boolean subTypeOf(AnnotationType subType) {
          for(Iterator pi = subType.getProperties().iterator(); pi.hasNext(); ) {
            if(!constraint.subConstraintOf(subType.getPropertyConstraint(pi.next()))) {
              return false;
            }
          }
          
          return true;
        }

        public void setProperty(Annotation ann, Object property, Object value)
        throws ChangeVetoException {
          try {
            getPropertyConstraint(property).setProperty(ann, property, value);
          } catch (ChangeVetoException cve) {
            throw new ChangeVetoException(cve, "Failed to change property " + property);
          }
        }
    }
}

