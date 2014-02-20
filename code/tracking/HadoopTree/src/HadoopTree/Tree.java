/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

// test comment to check whether the .java files are synchronized with the git repository
// test succesful

package HadoopTree;

import org.apache.hadoop.io.Writable;
import java.io.IOException;
import java.io.DataOutput;
import java.io.DataInput;

import java.io.FileOutputStream;
import java.util.Formatter;
import java.io.FileWriter;
import java.io.File;

/**
 * A Tree of maxDepth has (2^maxDepth)-1 nodes and 2^maxDepth leaves.
 * So maxDepth 1 -> 1 node and 2 leaves
 * @author ccagniart
 */
public class Tree
    implements Writable {

public int         depth;
public SplitInfo[] nodes;

public Tree(int maxDepth) {
	// maxDepth is the depth of the leaf nodes that would be returned
	// by the method buildNodeDepth() from the class NodeStatus_sp. 
	
	// this is the number of internal nodes
    int numNodes = (int)( java.lang.Math.pow(2, maxDepth) -1);
    
    // the number of leaf nodes is 2^maxDepth
    
    depth = maxDepth;
    nodes = new SplitInfo[numNodes];
    for(int ni=0;ni<numNodes;++ni) nodes[ni] = new SplitInfo();
}

// awfull, but i prefer not risking float imprecision with Math.pow
static int getDepth(int nodeId) {
    int depth = 0;
    // TODO depth=log_10(nodeID+1)/log_10(2)   (but I don't know whether this can give float imprecision)
    // or NodeStatus_sp.buildNodeDepth(nodeId)
    while(nodeId>0) { nodeId=(nodeId-1)/2;/*parent address*/ depth++;}
    return depth;
}

// -------------------------
// Implementing the hadoop serialization
// -------------------------
@Override
public void write(DataOutput out) throws IOException {
    out.writeInt(depth);
    for(int ni=0;ni<nodes.length;++ni) {
        nodes[ni].write(out);
    }
}

@Override
public void readFields(DataInput in) throws IOException {
    int d = in.readInt();
    if( d != depth) throw new IOException("(E) the treeinfofile is  of depth " +
            Integer.toString(d) + "and you are trying to deserialize it to a tree" +
            "of depth " + Integer.toString(depth));
    for(int ni=0;ni<nodes.length;++ni) {
        nodes[ni].readFields(in);
    }
}

/**
 * Get a new Tree with only a number of 'top levels' of this (original) Tree.
 * 
 * @param subdepth  determines the number of top levels of the original Tree
 * @return
 */
public Tree getSubTree(int subdepth) {
    assert(subdepth<depth);
    Tree sub = new Tree(subdepth);
    
    // number of subNodes
    int numsub = (int)( java.lang.Math.pow(2, subdepth) -1); 

    for(int i=0;i<numsub;++i) sub.nodes[i] = nodes[i];

    return sub;
}

/**
 * Insert the subtree in this Tree at the given nodeId. At the nodeId will come the
 * top node of the subtree, and the deeper nodes of the subtree are inserted as well of course
 * @param nodeId
 * @param subtree
 */
public void insertSubTree(int nodeId, Tree subtree ) 
{
    int[] temp = new int[this.depth];

    // example.. tree = subtree
    // nodeId 5
    // the first part will get
    //  temp = [1,2] and nodedepth = 2
    // ninew is initialized as 0
    // it becomes 2*0+temp[1] = 2
    // it then becomes 2*2+temp[0] = 5
    for(int ni=0;ni<subtree.nodes.length;++ni) {
        int nisub = ni;
        int nodedepth = 0;
        while(nisub != 0) {
            if( nisub %2 == 0 ) temp[nodedepth] = 2;
            else                temp[nodedepth] = 1;
            nisub = (nisub-1)/2; // we become our parent
            nodedepth++;
        }
        int ninew = nodeId;
        for(int d=1;d<nodedepth+1;++d) {
            ninew = 2*ninew+temp[nodedepth-d];
        }
        nodes[ninew] = subtree.nodes[ni];
    }
}

public String toString() {
    String s = new String("");
    for(int ni=0;ni<nodes.length;++ni) {
        s += nodes[ni];
    }
    return s;
}

public static int majorityClassBeforeSplit(HistogramPair hp) {
    int maxId   = 0;
    long Nmax   = 0;
    for(int li=0;li<DataTraits.NUMCLASSES;++li) {
        long N = hp.m_h_false[li] +  hp.m_h_true[li];
        if(N > Nmax) { Nmax = N; maxId = li; }
    }
    return maxId;
}

public static int findLastMajorityClassInAncester(SplitInfo[] nodes, int nid) {
    while( nid != 0) {
        HistogramPair hp =  nodes[nid].hp;
        if(hp.getNfalse() + hp.getNtrue() != 0 ) {
            return majorityClassBeforeSplit(hp);
        }
        nid = (nid-1)/2;
    }
    return 31; // unknown class
}


public void saveToFile(String filename, AttribLoc[] locs)
    throws IOException {
    FileOutputStream fout = new FileOutputStream(filename);
    Formatter formatter = new Formatter(fout);

    System.out.println("(HACK) still accepting attribIds  that are -1"); //??
    formatter.format("%d\n", depth);

    AttribLoc zeroLoc = new AttribLoc(0, 0, 0, 0);

    for(int ni=0;ni<nodes.length;++ni){
        SplitInfo info  = nodes[ni];
        //System.out.println(info);
        // if 
        AttribLoc loc   = zeroLoc;
        int threshValue = -1;
        if( info.attribId >= 0 ) { 
            loc         = locs[info.attribId]; // one of the 2000 attributes
            threshValue = info.threshVal;
            // now there is a combination of an attribute (via loc) and a thresh value
        }

        // 5: minimum 5 characters need to be written, why?
        // d: The result is formatted as a decimal integer
        formatter.format("%5d %5d %5d %5d %5d\n",
                loc.u1, loc.v1, loc.u2, loc.v2, threshValue);
        // this writes a line of 5 values in the file:
        // 4 values define an attribute and the 5th value is the threshold value
    }

    // if depth is 2 we want to iterate on lvl 1 
    int penultimateLevel_Begin = (int)( java.lang.Math.pow(2, depth-1) -1);
    int penultimateLevel_End   = nodes.length;
    // these numbers form the range of the indexes of the (internal) nodes of the lowest
    // level of the tree

    // Find the prevalent classes in leaves...
    for(int ni=penultimateLevel_Begin;ni<penultimateLevel_End;++ni){
        int maxIndex_false = 0; 
        int maxIndex_true  = 0;
        HistogramPair hp = nodes[ni].hp;

        if( nodes[ni].attribId >= 0 ) {
            maxIndex_false = HistogramPair.majorityClass(hp.m_h_false);// the label that has the highest count in the 'false' histogram
            maxIndex_true = HistogramPair.majorityClass(hp.m_h_true);
        }
        else {
            maxIndex_false = findLastMajorityClassInAncester(nodes, ni);
            maxIndex_true = maxIndex_false;
        }

        formatter.format("%d\n", maxIndex_false);
        formatter.format("%d\n", maxIndex_true);
    }

    formatter.close();

    System.out.println("Printed the tree to " + filename);
}


public void saveSplitInfosToFile( String filename )
        throws IOException {
    FileWriter fout = new FileWriter(new File(filename));
    for(int ni=0;ni<nodes.length;++ni){
        fout.write( nodes[ni].toString() );
        fout.write("\n");
    }
    fout.close();
}

}
