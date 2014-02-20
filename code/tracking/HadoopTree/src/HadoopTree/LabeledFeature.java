/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree;

import org.apache.hadoop.io.Writable;
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;

import java.util.Arrays;



public class LabeledFeature implements Writable{
    public static final int NUMATTRIBS = DataTraits.NUMATTRIBS;
    public static final int RECORDSIZE = 1+1+2*NUMATTRIBS; // label, padding, 2 by short

    public LabeledFeature() {
        data = new byte[RECORDSIZE];
    }

    public LabeledFeature(LabeledFeature B) {
        data = Arrays.copyOf(B.data, RECORDSIZE);
    }


    /*Implementing the hadoop serialization*/
    @Override
    public void write(DataOutput out) throws IOException {
        out.write(data,0, RECORDSIZE);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
         in.readFully(data,0,RECORDSIZE);
    }

   /*@Override
    public String toString() {
        Integer u = new Integer(label);
        return u.toString() + " || " + Arrays.toString(attribs);
    }*/


    public byte[] data;
}
