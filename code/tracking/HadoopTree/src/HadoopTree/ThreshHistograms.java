/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree;

/**
 *
 * @author ccagniart
 */

import org.apache.hadoop.io.Writable;
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;
import org.apache.hadoop.io.IntWritable;

import java.lang.Math;


public class ThreshHistograms implements Writable{

    static final int NUMTHRESH = DataTraits.NUMTHRESH;

    public ThreshHistograms() {
        hps = new HistogramPair[NUMTHRESH];
        for(int ti=0;ti<NUMTHRESH;++ti) hps[ti] = new HistogramPair();
    }


    public void addSample(int label, int splitPoint, int count) {
        for(int ti=0;ti<splitPoint;++ti)
            hps[ti].addSampleTrue(label, count);
        for(int ti=splitPoint;ti<NUMTHRESH;++ti)
            hps[ti].addSampleFalse(label, count);
    }

    // ---------------------------------
    // Finding the best threshold
    // ---------------------------------
    // the best threshold is an index (in hps[]) that refers to the
    // HistogramPair with the highest information gain
    // (and in case of the same information gain the lowest thresh value)
    public int bestSplit(int[] threshs)
    {
        double bestGain = hps[0].information_gain();
        int bestIndex   = 0;
        
        for(int ti=1;ti<NUMTHRESH;++ti) {
            double gain = hps[ti].information_gain();
            if( SplitInfo.less(bestGain, threshs[bestIndex], gain, threshs[ti] ) ) {
                bestIndex = ti;
                bestGain  = gain;
            }
        }
        return bestIndex;
    }

    // ---------------------------------
    // merging representations
    // ---------------------------------
    public void accum( ThreshHistograms ths) {
        for(int ti=0;ti<NUMTHRESH;ti++) {
            hps[ti].accum(ths.hps[ti]);
        }
    }


    // ---------------------------------
    // Implementing the hadoop serialization
    // --------------------------------- 
    public void write(DataOutput out) throws IOException {
        for(int i=0;i<NUMTHRESH;++i){
            hps[i].write(out);
        }
    }

    public void readFields(DataInput in) throws IOException {
         for(int i=0;i<NUMTHRESH;++i){
            hps[i].readFields(in);
        }
    }
    
    public HistogramPair[] hps;
}
