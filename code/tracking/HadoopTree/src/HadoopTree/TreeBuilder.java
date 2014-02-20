package HadoopTree;



import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Arrays;



public class TreeBuilder {

public static SplitInfo findBestSplit( int[]                     threshs,
                                       LabeledSplitPointBuff[]   lsplits,
                                       int                       begin,
                                       int                       end )
{
    int[] finalAccum = new int[SplitPointAccum.size];
    
    SplitPointAccum accum = new SplitPointAccum();
    // ----------------
    // 1 - accumulate all
    for(int fi=begin;fi<end;++fi) {
        accum.accumSplitPoints(lsplits[fi]);
        if( fi % (int)(java.lang.Short.MAX_VALUE) == 0) {
            SplitPointAccum.accumOnInts(accum,finalAccum);
            accum = new SplitPointAccum(); // reset the accumulator
        }
    }

    // --------------
    // 2 - find best split
    SplitPointAccum.accumOnInts(accum,finalAccum);
    return SplitPointAccum.findBestSplit(finalAccum, threshs);
}


/**
 * This method has the same result as the findBestSplit() method 
 * 
 * @param threshs
 * @param lsplits
 * @param begin
 * @param end
 * @return
 */
public static SplitInfo findBestSplit_small( int[]                     threshs,
                                             LabeledSplitPointBuff[]   lsplits,
                                             int                       begin,
                                             int                       end )
{
    // ----------------
    // 1 - accumulate all
    SplitInfo bestSplit = new SplitInfo();
    bestSplit.entropyGain = -1.;
    bestSplit.attribId    = -1; // just add this so that any subsequent
    bestSplit.threshVal   = java.lang.Integer.MAX_VALUE;

    for(int ai=0;ai<DataTraits.NUMATTRIBS;++ai)
    {
        ThreshHistograms ths = new ThreshHistograms();

        // fi is an index in the lsplits array that is an array of input samples
        for(int fi=begin;fi<end;++fi) {
            ths.addSample(lsplits[fi].getLabel(), lsplits[fi].getSplitPointId(ai), 1);
        }

        int bestThreshId_along_ai   = ths.bestSplit(threshs);
        int bestThreshVal_along_ai  = threshs[bestThreshId_along_ai];
        double bestIG_along_ai      = ths.hps[bestThreshId_along_ai].information_gain();

        if(SplitInfo.less(bestSplit.entropyGain, bestSplit.threshVal,
                          bestIG_along_ai,       bestThreshVal_along_ai) ){
            bestSplit.attribId    = ai;
            bestSplit.threshVal   = bestThreshVal_along_ai;
            bestSplit.entropyGain = bestIG_along_ai;
            bestSplit.hp.copyFrom(ths.hps[bestThreshId_along_ai]);
        }
    }

    return bestSplit;
}


/**
 * reorganizes the samples so that all that test false are in the first half
 * of the subarray and all that test true in the second half. The subarray is
 * a part of the LabeledSplitPointBuff array that is defined by the indexes
 * begin and end
 * 
 * @param lsplits  The input samples of a node that will be split in memory
 * @param begin    The first index of the subarray that needs to be rearranged
 * @param end      The last index of the subarray that needs to be rearranged
 * 
 */
public static void splitSamplesInPlace( int                     attribId,
                                        int                     thresholdId,
                                        HistogramPair           hp,
                                        LabeledSplitPointBuff[] lsplits,
                                        int                     begin,
                                        int                     end )
{
    // we know how big each half is going to be
    int itr_false   = begin;
    int end_false   = itr_false + (int)(hp.getNfalse());
    int itr_true    = end_false;
    int end_true    = itr_true + (int)(hp.getNtrue());
    if(end_true != end) { // small consistency check
        throw new IndexOutOfBoundsException("The splitInfo is incoherent" +
                " with the data " + end_true + " " + end + "\n"
                + attribId + " " + thresholdId +"\n" + hp + "\n");
    }

    while( itr_false != end_false )
    {
        // find a guy in the first half that returns true
        while(  itr_false < end_false
                && lsplits[itr_false].getSplitPointId(attribId) <= thresholdId  ) itr_false++;

        if( itr_false  == end_false ) break; // no swap left to make.. we're good

        // find a guy in the second half that returns false
        while( itr_true < end_true
                && lsplits[itr_true].getSplitPointId(attribId) > thresholdId  ) itr_true++;

        assert(itr_true != end_true);

        // swap the two
        LabeledSplitPointBuff swap  = lsplits[itr_false];
        lsplits[itr_false]          = lsplits[itr_true];
        lsplits[itr_true]           = swap;

        // these guys are now good... continue
        itr_false++;
        itr_true++;
    }

}




/**
 * This method builds a subtree of a certain depth with the given input samples. The 
 * input samples are provided in an array and this array will be rearranged by the method 
 * splitSamplesInPlace().
 * 
 * @param lsplits
 * @param threshs
 * @param maxDepth
 * @return
 */
public static Tree treeBuild( LabeledSplitPointBuff[] lsplits, int[] threshs, int maxDepth )
{
	
	// the next two classes are necessary to organize the queue
	// that will be used in this method
    class SplitTask {
        SplitTask(int n, int b, int e) { node = n; begin = b; end = e; }
        int node;
        // begin and end are indexes for the array lsplits
        int begin;
        int end;
    }

    class SplitTask_Comparator implements Comparator<SplitTask>{
        @Override
        public int compare( SplitTask a, SplitTask b) {
            if( a.node < b.node )  return -1;
            if( a.node == b.node ) return  0;
            else                   return  1;
        }
    }

     Tree tree = new Tree(maxDepth);

    // initialize the queue
    Queue<SplitTask> filesToSplitQueue
            = new PriorityQueue<SplitTask>(10,new SplitTask_Comparator());
    filesToSplitQueue.add(new SplitTask(0,0,lsplits.length));

    // process the queue
    while( filesToSplitQueue.peek() != null)
    {
        // pop the queue
        final SplitTask element = filesToSplitQueue.remove();
        final int       nid     = element.node;
        final int       depth   = Tree.getDepth(nid);

        // find the split
        SplitInfo info = null;
        if((element.end - element.begin) < 4000)
            info = findBestSplit_small(threshs, lsplits, element.begin, element.end);
        else
            info = findBestSplit(threshs, lsplits, element.begin, element.end);
        // write the split
        tree.nodes[nid] = info;
        // perform the split
        int threshId = Arrays.binarySearch(threshs, info.threshVal);
        if(threshId < 0) throw new IndexOutOfBoundsException("aaaaah");
        
        splitSamplesInPlace(info.attribId,
                            threshId,
                            info.hp,
                            lsplits,
                            element.begin,
                            element.end);


        // add the sons
        if( depth < maxDepth-1 ) {
            int splitPoint = element.begin + (int) ( info.hp.getNfalse() );

            if(HistogramPair.numRepresentedClasses(info.hp.m_h_false) > 1 ){
                filesToSplitQueue.add(new SplitTask(2*nid+1, element.begin, splitPoint) );
            }
            if(HistogramPair.numRepresentedClasses(info.hp.m_h_true) > 1 ) {
                filesToSplitQueue.add(new SplitTask(2*nid+2, splitPoint, element.end) );
            }
        }
    }
    return tree;
}


}
