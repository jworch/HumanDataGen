package HadoopTree;


import HadoopTree.DataTraits;
import HadoopTree.HistogramPair;
import HadoopTree.LabeledFeature;
import HadoopTree.SplitInfo;
import HadoopTree.ThreshHistograms;
import HadoopTree.mapredUtils.ReadMapRedResult;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;

// hadoop mapreduce
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;


import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.DataInputStream;


import java.util.Arrays;
import java.util.Scanner;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import java.lang.Math; // for Math.abs
import java.lang.IndexOutOfBoundsException;

/**
 * A Large Buffer that will contain the splitting points
 */
public class SplitPointAccum
        implements Writable
{
    // the accumulator is organized as [label][attrib][threshold]
    public static final int size = DataTraits.NUMCLASSES
                          * DataTraits.NUMATTRIBS
                          * (DataTraits.NUMTHRESH + 1);
    // +1 because if the feature value of a sample is greater than every threshold
    // value in the threshold array, the index that belongs to that feature value
    // will be thresh[].length (and will thus be 50)
    // (see the method findSplitPoint() in the LabeledSplitPointBuff class)
    // And the index 50 is actually the 51st index in an array.. 
    public static final int attribOffset = DataTraits.NUMTHRESH + 1;
    public static final int labelOffset  = DataTraits.NUMATTRIBS * attribOffset;
 

    // careful short will only work as long as
    // input splits are less than 64 MB i. ( technically ~120 MB is the max.
    public short[] threshAccums;




    public SplitPointAccum() {
        threshAccums = new short[size];
    }

    

    public void accumSplitPoints( int label, int[] splitPoints) {
    	// each label has its own splitPoints array (and each label will have many splitPoints arrays(otherwise
    	// you would not want to accumulate them))
    	// in splitPoints is the threshold value 
    	// that belongs to a certain attribute(=offset)
        assert(splitPoints.length == DataTraits.NUMATTRIBS);

        int baseOffset = label*labelOffset;
        for(int ai=0;ai<DataTraits.NUMATTRIBS;++ai){
            int offset = baseOffset
                         + ai*attribOffset
                         + splitPoints[ai]; // ?splitPoints[ai] is the index in thresh[] for attribute ai (is this correct?)
            threshAccums[offset] += 1;
            // so for each label is counted how many times for each attribute
            // a certain thresh value appears in the splitPoints array
        }
    }
/*
    public void accumSplitPoints(int label, byte[] splitPoints) {
        int baseOffset = label*labelOffset;
        for(int ai=0;ai<splitPoints.length;++ai){
            int offset = baseOffset
                         + ai*attribOffset
                         + splitPoints[ai];
            threshAccums[offset] += 1;
        }
    }*/

    // the same as accumSplitPoints( int label, int[] splitPoints)
    // but now the thresh value is stored as its index in the thresh array
    public void accumSplitPoints( LabeledSplitPointBuff splitPoints) {
    	// the LabeledSplitPointBuff is a sample and it contains an array of 2000 threshold values (via the index
    	// in the threshold array), one threshold value for each attribute.
    	// The sample belongs to a certain pixel of a training image 
    	// and it is thus known which label belongs to that sample
        byte[] data = splitPoints.data;
        
        int label = (int) data[0];
		int baseOffset = label * labelOffset;

		for (int ai = 0; ai < DataTraits.NUMATTRIBS; ++ai) {
			int offset = baseOffset + ai * attribOffset + data[ai + 2]; 
			// data[ai+2] contains the index in thresh[] for attribute ai (which is called the splitPointId)
			threshAccums[offset] += 1;
			// so for each label is counted how many times for each attribute
            // a certain thresh value appears in the splitPoints array
		}

	}

  



	// #######################
    // Implementing Hadoop Writable
    @Override
    public void write(DataOutput out) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(threshAccums.length*2);
        ShortBuffer shortBuffer = buffer.asShortBuffer();
        shortBuffer.put(threshAccums);
        out.write(buffer.array(), 0, buffer.array().length );
//        for(int i=0;i<threshAccums.length;++i) out.writeShort(threshAccums[i]);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
         ByteBuffer buffer = ByteBuffer.allocate(threshAccums.length*2);
         in.readFully( buffer.array(), 0, threshAccums.length*2 );
         ShortBuffer shortBuffer = buffer.asShortBuffer();
         shortBuffer.get(threshAccums);
//         for(int i=0;i<threshAccums.length;++i) threshAccums[i] = in.readShort();
    }






    /**
     * Helper function to load
     * @param conf Configuration file
     * @return the threshold array
     * @throws IOException
     */
    public static int[] loadThreshFile(FileSystem fs, Path path)
            throws IOException {
        FSDataInputStream fin = fs.open(path);
        int[] threshs =  loadThreshFile(fin);
        fin.close();      
        return threshs;
    }

    public static int[] loadThreshFile(DataInputStream stream )
        throws IOException {
        Scanner scan = new Scanner(stream);
        // read the threshold count
        int numThresh = scan.nextInt();
        assert( numThresh == DataTraits.NUMTHRESH);
        // alloc
        int[] threshs           = new int[numThresh];
        // fill thresh buff
        for(int ti=0;ti<numThresh;++ti) threshs[ti] = scan.nextInt();
        // make sure it is sorted
        for(int ti=0;ti<numThresh-1;++ti) {
            if(threshs[ti+1] <= threshs[ti] )
                throw (new IOException("(E) the threshfile must be strictly sorted"));
        }
        return threshs;
    }



    
    // the following two methods overcome the maximum values that can be stored in an array
    // of shorts by using an array of integers or of longs
    /**
     * This method will accumulate the contents of a SplitPointAccum object 
     * in the array bigBuffer. In this way the contents of many SplitPointAccum objects
     * can be accumulated on the same array.
     *  
     * @param a
     * @param bigBuffer
     */
    public static void accumOnInts(SplitPointAccum a, int[] bigBuffer) {
        if( bigBuffer.length != size) throw new IndexOutOfBoundsException("");
       
        for(int i=0;i<size;++i) {
        	bigBuffer[i] += a.threshAccums[i];
        }
    }

    public static void accumOnLongs(SplitPointAccum a, long[] bigBuffer) {
        if( bigBuffer.length != size) throw new IndexOutOfBoundsException("");
        for(int i=0;i<size;++i) bigBuffer[i] += a.threshAccums[i];
    }

    // a SplitInfo object contains a combination of an attribute ID and a thresh value and its entropy gain
    // (or information gain) and the HistogramPair that belongs to the combination 
    // of that attribute and the thresh value
    public static SplitInfo findBestSplit(int[]  bigBuffer,
                                          int[]  threshs) {

        SplitInfo bestSplit   = new SplitInfo();
        bestSplit.entropyGain = -1;
        bestSplit.attribId    = -1; //this will create throws down the line if we can't find better     
        bestSplit.threshVal   = java.lang.Integer.MAX_VALUE;
        // ----------------------------
        // convert the buffer to histograms for all dimensions
        for(int ai=0;ai<DataTraits.NUMATTRIBS;++ai) {
            ThreshHistograms ths = new ThreshHistograms();
            for(int li=0;li<DataTraits.NUMCLASSES;++li) {
                int baseOffset = li*SplitPointAccum.labelOffset
                               + ai*SplitPointAccum.attribOffset;
                // now a loop over the 50 thresh values (+1) that belong to a combination
                // of label and an attribute
                for(int ti=0;ti<DataTraits.NUMTHRESH+1;++ti) {
                	// ti as splitPoint to decide whether the value in 
                	// bigBuffer[baseOffset+ti] is added by the true
                	// array or the false array of the 50 HistogramPairs
                	// that each belong to a certain thresh value
                    ths.addSample(li, ti, bigBuffer[baseOffset+ti]);
                   
                    // the bigBuffer is the finalAccum array provided by the class FindBestSplit_sp or TreeBuilder,
                    // the value in bigBuffer[baseOffset+ti] belongs to a certain combination of an
                    // attribute and a thresh value
                }
            }

            int bestThreshId_along_ai   = ths.bestSplit(threshs); // for each attribute the best thresh value is selected
            													  // so bestThreshId_along_ai is the index in thresh[] for a certain attribute
            int bestThreshVal_along_ai  = threshs[bestThreshId_along_ai];
            double bestIG_along_ai      = ths.hps[bestThreshId_along_ai].information_gain();

             // here the best (with highest information gain) attribute is selected
             if( SplitInfo.less(bestSplit.entropyGain, bestSplit.threshVal,
                                bestIG_along_ai,       bestThreshVal_along_ai) ){
                bestSplit.attribId    = ai;
                bestSplit.threshVal   = bestThreshVal_along_ai;
                bestSplit.entropyGain = bestIG_along_ai;
                bestSplit.hp.copyFrom(ths.hps[bestThreshId_along_ai]);
            }
        }

        return bestSplit;
    }


    
}
