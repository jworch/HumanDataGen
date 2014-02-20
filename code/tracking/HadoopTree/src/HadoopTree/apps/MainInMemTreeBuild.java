/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.apps;


import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;


import org.apache.hadoop.fs.Path;

import HadoopTree.Tree;
import HadoopTree.TreeBuilder;
import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.AttribLoc;
import HadoopTree.LabeledFeature;
import HadoopTree.SplitPointAccum;


import java.util.ArrayList;


public class MainInMemTreeBuild {

    public static void main(String[] args) throws Exception {
        assert( args.length == 3);
        Path seqFile = new Path(args[0]);
        Path threshFile = new Path(args[1]);
        Path locFile = new Path(args[2]);


        // load thresh
        FileInputStream fthresh = new FileInputStream(threshFile.toString());
        int[] threshs = SplitPointAccum.loadThreshFile(new DataInputStream(fthresh));
        AttribLoc[] attribLocs = AttribLoc.loadAttribLocs(locFile.toString());


            // load items
        
        FileInputStream fin = new FileInputStream(seqFile.toString());
        BufferedInputStream bfin = new BufferedInputStream(fin,  8192);


        ArrayList<LabeledSplitPointBuff> items = new ArrayList<LabeledSplitPointBuff>();
        // -------------------
        // 1 - copy all the input data
        while(true) {
            // read the feature
            LabeledFeature lf = new LabeledFeature();
            int read = bfin.read(lf.data, 0, LabeledFeature.RECORDSIZE);
            if (read != LabeledFeature.RECORDSIZE) break;
            // build the splitbuff
            items.add(new LabeledSplitPointBuff(lf, threshs));
        }

        LabeledSplitPointBuff[] itemArray = new LabeledSplitPointBuff[items.size()];
        items.toArray(itemArray);


        Tree tree = TreeBuilder.treeBuild(itemArray, threshs, 5);
        tree.saveToFile("/u/ccagniart/Desktop/treetest.txt", attribLocs);


        
        for(int nodeid = 0; nodeid < tree.nodes.length; nodeid++ ) {
            System.err.println(tree.nodes[nodeid]);
        }
    }
}
