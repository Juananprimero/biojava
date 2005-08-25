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

/*
 * RichLocation.java
 *
 * Created on July 28, 2005, 5:29 PM
 */
package org.biojavax.bio.seq;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.biojava.bio.symbol.FuzzyLocation;
import org.biojava.bio.symbol.FuzzyPointLocation;
import org.biojava.bio.symbol.Location;
import org.biojava.bio.symbol.MergeLocation;
import org.biojava.bio.symbol.PointLocation;
import org.biojava.bio.symbol.RangeLocation;
import org.biojava.utils.ChangeType;
import org.biojava.utils.ChangeVetoException;
import org.biojavax.CrossRef;
import org.biojavax.RichAnnotatable;
import org.biojavax.ontology.ComparableTerm;

/**
 * Holds enough info about locations to keep BioSQL happy if needs be.
 *
 * @author Richard Holland
 */
public interface RichLocation extends Location,RichAnnotatable,Comparable {
    
    public static final ChangeType NOTE = new ChangeType(
            "This location's notes have changed",
            "org.biojavax.bio.seq.RichLocation",
            "NOTE"
            );
    public static final ChangeType TERM = new ChangeType(
            "This location's term has changed",
            "org.biojavax.bio.seq.RichLocation",
            "TERM"
            );
    public static final ChangeType RANK = new ChangeType(
            "This location's rank has changed",
            "org.biojavax.bio.seq.RichLocation",
            "RANK"
            );
    public static final ChangeType CIRCULAR = new ChangeType(
            "This location's circularity has changed",
            "org.biojavax.bio.seq.RichLocation",
            "CIRCULAR"
            );
    
    /**
     * Retrieves the crossref associated with this location.
     * @return the crossref.
     */
    public CrossRef getCrossRef();
    
    /**
     * Retrieves the term associated with this location.
     * @return the term.
     */
    public ComparableTerm getTerm();
    
    /**
     * Sets the term for this location.
     * @param term the term this location should adopt.
     * @throws ChangeVetoException in case of error.
     */
    public void setTerm(ComparableTerm term) throws ChangeVetoException;
    
    /**
     * Retrieves the strand associated with this location.
     * @return the strand.
     */
    public Strand getStrand();
    
    /**
     * Retrieves the rank associated with this location.
     * @return the rank.
     */
    public int getRank();
    
    /**
     * Sets the rank for this location.
     * @param rank the rank this location should adopt.
     * @throws ChangeVetoException in case of error.
     */
    public void setRank(int rank) throws ChangeVetoException;
    
    public Position getMinPosition();
    
    public Position getMaxPosition();
    
    public void setPositionResolver(PositionResolver p);
    
    public int getCircularLength();
    
    public void setCircularLength(int sourceSeqLength) throws ChangeVetoException;
        
    public static final RichLocation EMPTY_LOCATION = new EmptyRichLocation();
    
    public static class Strand implements Comparable {
        public static final Strand POSITIVE_STRAND = new Strand("+",1);
        public static final Strand NEGATIVE_STRAND = new Strand("-",-1);
        public static final Strand UNKNOWN_STRAND = new Strand("?",0);
        public static Strand forValue(int value) {
            switch (value) {
                case 1: return POSITIVE_STRAND;
                case 0: return UNKNOWN_STRAND;
                case -1: return NEGATIVE_STRAND;
                default: throw new IllegalArgumentException("Unknown strand type: "+value);
            }
        }
        public static Strand forName(String name) {
            if (name.equals("+")) return POSITIVE_STRAND;
            else if (name.equals("?")) return UNKNOWN_STRAND;
            else if (name.equals("-")) return NEGATIVE_STRAND;
            else throw new IllegalArgumentException("Unknown strand type: "+name);
        }
        private String name;
        private int value;
        public Strand(String name,int value) { this.name = name; this.value = value; }
        public int intValue() { return this.value; }
        public String toString() { return this.name; }
        public int hashCode() {
            int code = 17;
            code = 31*code + this.name.hashCode();
            code = 31*code + this.value;
            return code;
        }
        public boolean equals(Object o) {
            if (!(o instanceof Strand)) return false;
            if (o==this) return true;
            Strand them = (Strand)o;
            if (!them.toString().equals(this.name)) return false;
            if (them.intValue()!=this.value) return false;
            return true;
        }
        public int compareTo(Object o) {
            Strand fo = (Strand) o;
            if (!this.name.equals(fo.toString())) return this.name.compareTo(fo.toString());
            return this.value-fo.intValue();
        }
    }
    
    public static class Tools {
        
        private Tools() {}
        
        public static RichLocation construct(Collection members) {
            if (members.size()==0) return RichLocation.EMPTY_LOCATION;
            else if (members.size()==1) return ((SimpleRichLocation[])members.toArray(new SimpleRichLocation[0]))[0];
            else return new CompoundRichLocation(members);
        }
        
        public static Collection merge(Collection members) {
            List membersList = new ArrayList(flatten(members));
            // all members are now singles so we can use single vs single union operations
            if (membersList.size()>1) {
                for (int p = 0; p < (membersList.size()-1); p++) {
                    RichLocation parent = (RichLocation)membersList.get(p);
                    for (int c = p+1; c < membersList.size(); c++) {
                        RichLocation child = (RichLocation)membersList.get(c);
                        RichLocation union = (RichLocation)parent.union(child);
                        // if parent can merge with child
                        if (union.isContiguous()) {
                            //      replace p with merged result
                            membersList.set(p,union);
                            //      remove c
                            membersList.remove(c);
                            //      reset c to check all children again
                            c=p;
                        }
                    }
                }
            }
            return membersList;
        }
        
        public static Collection flatten(RichLocation location) {
            List members = new ArrayList();
            for (Iterator i = location.blockIterator(); i.hasNext(); ) members.add(i.next());
            return flatten(members);
        }
        
        public static Collection flatten(Collection members) {
            List flattened = new ArrayList(members);
            for (int i = 0; i < flattened.size(); i++) {
                RichLocation member = (RichLocation)flattened.get(i);
                if (!(member instanceof SimpleRichLocation)) {
                    flattened.remove(i);
                    int insertPos = i;
                    for (Iterator j = member.blockIterator(); j.hasNext(); ) flattened.add(insertPos++,j.next());
                    i--;
                }
            }
            return flattened;
        }
        
        public static int[] modulateCircularLocation(int start, int end, int seqLength) {
            // Dummy case
            if (seqLength==0) return new int[]{start,end};
            // Modulate.
            while (end<start) end+=seqLength;
            int locationLength = end-start;
            while (start>=seqLength) start-=seqLength;
            end = start+locationLength;
            return new int[]{start,end};
        }
        
        public static int[] modulateCircularLocationPair(Location a, Location b, int seqLength) {
            // Dummy case
            if (seqLength==0) return new int[]{a.getMin(),a.getMax(),b.getMin(),b.getMax()};
            // Modulate our start/end to shortest possible equivalent region
            int[] aParts = modulateCircularLocation(a.getMin(), a.getMax(), seqLength);
            int aStart = aParts[0];
            int aEnd = aParts[1];
            // Modulate their start/end to shortest possible equivalent region
            int[] bParts = modulateCircularLocation(b.getMin(), b.getMax(), seqLength);
            int bStart = bParts[0];
            int bEnd = bParts[1];
            // If we wrap and the point we are checking for is before our start, increment it by circularLength length
            if (aEnd>seqLength && bStart<aStart) {
                bStart+=seqLength;
                bEnd+=seqLength;
            }
            return new int[] {aStart,aEnd,bStart,bEnd};
        }
        
        public static int modulateCircularIndex(int index, int seqLength) {
            // Dummy case
            if (seqLength==0) return index;
            // Modulate
            while (index>seqLength) index-=seqLength;
            return index;
        }
        
        public static RichLocation enrich(Location l) {
            if (l instanceof RichLocation) {
                return (RichLocation)l;
            } else if (l instanceof MergeLocation || !l.isContiguous()) {
                List members = new ArrayList();
                for (Iterator i = l.blockIterator(); i.hasNext(); ) {
                    Location member = (Location)i.next();
                    members.add(enrich(member));
                }
                return RichLocation.Tools.construct(RichLocation.Tools.merge(members));
            } else if (l instanceof FuzzyPointLocation) {
                FuzzyPointLocation f = (FuzzyPointLocation)l;
                Position pos = new SimplePosition(f.hasBoundedMin(),f.hasBoundedMax(),f.getMin(),f.getMax(),Position.IN_RANGE);
                return new SimpleRichLocation(pos,0); // 0 for no rank
            } else if (l instanceof FuzzyLocation) {
                FuzzyLocation f = (FuzzyLocation)l;
                Position start = new SimplePosition(f.hasBoundedMin(),false,f.getMin());
                Position end = new SimplePosition(false,f.hasBoundedMax(),f.getMax());
                return new SimpleRichLocation(start,end,0); // 0 for no rank
            } else if (l instanceof RangeLocation) {
                RangeLocation r = (RangeLocation)l;
                Position start = new SimplePosition(false,false,r.getMin());
                Position end = new SimplePosition(false,false,r.getMax());
                return new SimpleRichLocation(start,end,0); // 0 for no rank
            } else if (l instanceof PointLocation) {
                PointLocation p = (PointLocation)l;
                Position pos = new SimplePosition(false,false,p.getMin());
                return new SimpleRichLocation(pos,0); // 0 for no rank
            } else if (l.toString().equals("{}")) {
                return EMPTY_LOCATION;
            } else {
                throw new IllegalArgumentException("Unable to enrich locations of type "+l.getClass().toString());
            }
        }
    }
}
