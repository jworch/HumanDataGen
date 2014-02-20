/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree;


import org.apache.hadoop.io.Writable;
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;

/**
 *
 * @author ccagniart
 */
public class SplitInfo implements Writable{

    public SplitInfo() {
        attribId    = -1;
        threshVal   = 0;
        entropyGain = 0;
        hp = new HistogramPair();
    }

    public SplitInfo(SplitInfo B) {
        attribId    = B.attribId;
        threshVal   = B.threshVal;
        entropyGain = B.entropyGain;
        hp = new HistogramPair();
        for(int li=0;li<DataTraits.NUMCLASSES;++li) hp.m_h_false[li] = B.hp.m_h_false[li];
        for(int li=0;li<DataTraits.NUMCLASSES;++li) hp.m_h_true[li]  = B.hp.m_h_true[li];
    }

    public SplitInfo(int attribId_, int threshVal_, HistogramPair hp_) {
        attribId    = attribId_;
        threshVal   = threshVal_;  
        hp          = new HistogramPair();
        hp.copyFrom(hp_);
        entropyGain = hp_.information_gain();
    }



    public static boolean less(double IG1, int threshVal1,
                               double IG2, int threshVal2 )
    {
        return  (IG2 >  IG1)
            || ((IG2 == IG1) && (Math.abs(threshVal1) > Math.abs(threshVal2)));
    }

    // -------------------------
    // Implementing the hadoop serialization
    // -------------------------
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(attribId);
        out.writeInt(threshVal);
        out.writeDouble(entropyGain);
        hp.write(out);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
         attribId = in.readInt();
         threshVal = in.readInt();
         entropyGain = in.readDouble();
         hp.readFields(in);
    }

    @Override
    public String toString(){
        return   Integer.toString(attribId) + " "
               + Integer.toString(threshVal) + " "
               + Double.toString(entropyGain) + "\n"
               + hp.toString();
    }




    public int           attribId;
    public int           threshVal;
    public double        entropyGain;
    public HistogramPair hp;
}
