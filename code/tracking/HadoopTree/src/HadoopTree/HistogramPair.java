/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree;

/**
 * @author ccagniart
 */

import org.apache.hadoop.io.Writable;
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;

import java.util.Arrays;

public class HistogramPair implements Writable
{
    static final int NUMCLASSES = DataTraits.NUMCLASSES;
    public HistogramPair() 
    {
        m_h_false = new long[NUMCLASSES];
        m_h_true = new long[NUMCLASSES];
        for(int i=0;i<NUMCLASSES;++i) m_h_false[i] = 0;  // TODO is this necessary?
        for(int i=0;i<NUMCLASSES;++i) m_h_true[i] = 0;
    }

    public void copyFrom(HistogramPair B) 
    {
        for(int i=0;i<NUMCLASSES;++i) m_h_false[i] = B.m_h_false[i];
        for(int i=0;i<NUMCLASSES;++i) m_h_true[i] = B.m_h_true[i];
    }

    // -------------------------
    // Accumulation
    // -------------------------
    void addSampleTrue(int label, int count) {
        m_h_true[label] += count;
    }
    void addSampleFalse(int label, int count) {
        m_h_false[label] += count;
    }
    void accum(HistogramPair hp) {
        for(int i=0;i<NUMCLASSES;++i) m_h_false[i] += hp.m_h_false[i];
        for(int i=0;i<NUMCLASSES;++i) m_h_true[i]  += hp.m_h_true[i];
    }

    // -------------------------
    // Utilities
    // -------------------------
    public long getNtrue() {
        return sum(m_h_true);
    }

    public long getNfalse() {
        return sum(m_h_false);
    }

    // the amount of labels that are chosen at least once
    public static int numRepresentedClasses(long[] h) {
        int count = 0;
        for(int li=0;li<DataTraits.NUMCLASSES;++li) {
            if(h[li] != 0) count++;
        }
        return count;
    }

    // the label with the highest count 
    public static int majorityClass(long[] h) {
        int maxId    = 0;
        for(int li=0;li<DataTraits.NUMCLASSES;++li) {
            if(h[li] > h[maxId]) maxId = li;
        }
        return maxId;
    }


    // -------------------------
    // Entropy calculations
    // -------------------------

    /// computes the entropy of one histogram
    static public double entropy( long[] h ) 
    {
        double Ntotal = sum(h);
        double entropy = 0.;
        for(int li=0;li<NUMCLASSES;++li) 
        {
            if( h[li] != 0 ) {
                double p = ((double)(h[li])) / Ntotal;
                entropy -= p*Math.log(p);
        }
      }
      return entropy;
    }

    static long sum(long[] h) {
        long s = 0;
        for(int i=0;i<h.length;++i) s+=h[i];
        return s;
    }

    public double entropy_merged() {
        double Ntotal = sum(m_h_false) + sum(m_h_true);

        double entropy = 0.;
        for(int li=0;li<NUMCLASSES;++li) {
            long Ni = m_h_true[li] + m_h_false[li];
            if( Ni!= 0) {
                double p = ((double)Ni) / Ntotal;
                entropy -= p*Math.log(p);
            }
        }
        return entropy;
    }

    public double information_gain() {
        double e0     = entropy_merged();
        double etrue  = entropy(m_h_true);
        double efalse = entropy(m_h_false);
        double Ntrue  = sum(m_h_true);
        double Nfalse = sum(m_h_false);
        double Ntotal = Ntrue + Nfalse;

        if( Ntotal == 0.) return 0.;
        return e0 - (Ntrue/Ntotal)*etrue - (Nfalse/Ntotal)*efalse;
    }

    // -------------------------
    // Implementing the hadoop serialization
    // -------------------------
    @Override
    public void write(DataOutput out) throws IOException {
        for(int i=0;i<NUMCLASSES;++i) out.writeLong(m_h_false[i]);
        for(int i=0;i<NUMCLASSES;++i) out.writeLong(m_h_true[i]);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
         for(int i=0;i<NUMCLASSES;++i) m_h_false[i] = in.readLong();
         for(int i=0;i<NUMCLASSES;++i) m_h_true[i] = in.readLong();
    }

    @Override
    public String toString() {
        return Arrays.toString(m_h_false) + "\n"
             + Arrays.toString(m_h_true) + "\n";
    }

    public long[] m_h_false;
    public long[] m_h_true;
}
