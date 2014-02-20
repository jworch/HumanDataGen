package HadoopTree;

import java.util.Arrays;


import org.apache.hadoop.io.Writable;
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;

import java.nio.ByteBuffer;

/**
 * A LabeledSplitPointBuff mirrors a LabeledFeature (vector of attributes),
 * but instead of storing the attributes values, it stores for each attribute
 * the index of first element >= than the attribute in a given threshs[] array.
 *
 * If the attribute is smaller than all the elements in threshs[] it returns
 * threshs.length()
 * 
 * @author ccagniart
 */
public class LabeledSplitPointBuff implements Writable{
    public static final int NUMATTRIBS = DataTraits.NUMATTRIBS;
    public static final int RECORDSIZE = 1+1+NUMATTRIBS; //label, padding, splitpoints

    
    public LabeledSplitPointBuff() {
        data = new byte[RECORDSIZE];
        data[0] = DataTraits.NOCLASS;
    }

    // screw clone... 
    public LabeledSplitPointBuff(LabeledSplitPointBuff B) {
        data = B.data.clone(); // byte is not an object so byte[].clone should be deep
    }

    /**
     * This constructor won't be used anymore when using a .dat file as 
     * input data.
     * 
     * @deprecated
     */
    public LabeledSplitPointBuff(LabeledFeature lf, int[] threshs) {
        data = new byte[RECORDSIZE];
        computeSplitPoints(lf, threshs);
    }

    public byte getLabel()                           { return data[0]; }
    public void setLabel(byte l)                     { data[0] = l; }
    public byte getSplitPointId(int ai)              { return data[2+ai];} // the index in the thresh array
    public void setSplitPointId(int ai, byte value ) { data[2+ai] = value; }
    
    public byte[] data;


    /**
     * This method won't be used anymore when using a .dat file as 
     * input data.
     * 
     * @deprecated
     */
    public void computeSplitPoints(LabeledFeature lf, int[] threshs) {
        // copy the label
        data[0] = lf.data[0];

        // wrap the buffer
        ByteBuffer buffer = ByteBuffer.wrap(lf.data);
        buffer.order(java.nio.ByteOrder.nativeOrder()); // we tell that the array is native
        buffer.getShort(); // move the reader 2 bytes forward

        // compute the thresh
        for(int ai=0;ai<DataTraits.NUMATTRIBS;++ai) {
            data[ai+2] = findSplitPoint(buffer.getShort(), threshs);
        }
        
    }
    
    /**
     * This method won't be used anymore when using a .dat file as 
     * input data.
     * 
     * @deprecated
     */
    // #######################
    // Accumulating
    public static byte findSplitPoint(int val, int[] threshs) {
        // we look for the index of the first element >= val ( the attrib)
        // then we will accum false for all the smaller and true for all the others
        int splitPoint = Arrays.binarySearch(threshs, val);
        // if we have [0,1,2,3,4,5,6,7] as thresh array
        // # 4 as incoming sample
        //   we get 4 as split point									(because 4 is in thresh)
        //   then we accum true on [0,1,2,3]
        //                false on [4,5,6,7]
        // # 3.5 as incoming sample
        //   we get -5 as split point ( -4 (insertion point) - 1 )		(because 3.5 is not in thresh)
        //   we transform it to 4 because we want the same behaviour
        if( splitPoint  < 0 ) {
            splitPoint = -splitPoint-1;
        }
        // # 10 as incoming sample
        // we got -8-1 (-list size-1) as split point
        // we transform it to 8
        if(splitPoint > threshs.length) return (byte)( threshs.length);
        else                            return (byte) splitPoint;
    }

    // #######################
    // Implementing the hadoop serialization
    @Override
    public void write(DataOutput out) throws IOException {
        out.write(data,0, RECORDSIZE);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
         in.readFully(data,0,RECORDSIZE);
    }
    
  
}


