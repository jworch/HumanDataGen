package HadoopTree.apps;

import java.io.File;
import java.text.NumberFormat;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.GenericOptionsParser;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configured;

// HadoopTree
import HadoopTree.SplitInfo;
import HadoopTree.Tree;
import HadoopTree.AttribLoc;
import HadoopTree.TTrain.TreeTrainer_sp;

public class MainTrainTree
        extends Configured
        implements Tool
{
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new MainTrainTree(), args);
    System.exit(res);
  }

  public int run(String[] args) throws Exception {
    Configuration conf = new Configuration();
    // parse the command line to a hadoop configuration
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 3) {
      System.err.println("Usage: trainTree <path> <maxDepth> <outpath> where path" +
              " contains a splitbuffs.seq file and make sure the HadoopTree.jar file and the attribLoc.txt file" +
              " are in the same directory and that you run the program from this directory!");
      System.exit(2);
    }

    Path rootPath = new Path(otherArgs[0]);
    int maxDepth = Integer.parseInt(otherArgs[1]);
    Path outPath  = new Path(otherArgs[2]);
    //Path attribPath = new Path(otherArgs[3]);

 // DONE: REMOVE THIS HARDCODED PATH!
    //AttribLoc[] locs = AttribLoc.loadAttribLocs("/home/cedric/data/configFiles/attribLoc.txt");
    //System.out.println("attribPath is: "+attribPath.toString());
    
    //it was kind of a mess to give the attribPath with the arguments in the command line 
//    AttribLoc[] locs = AttribLoc.loadAttribLocs(attribPath.toString() + "/attribLoc.txt");
    
    //this makes it easier
    //but take care the HadoopTree.jar file and the attribLoc.txt file are in the same directory
    //AND make sure you run this program in the command line from this directory, because the next 
    //line of code will look for the file in your current working directory!!
    AttribLoc[] locs = AttribLoc.loadAttribLocs("attribLoc.txt");
    // TODO this actually still is a hard coded path, try to give this path with an argument in the command
    // Maybe this file can be put to the Hadoop file system to avoid this kind of
    // path that depends on the local file system.
    
    // create the developing structure
    TreeTrainer_sp trainer = new TreeTrainer_sp(conf, rootPath, maxDepth);
    trainer.developTree(200); // polling period of 200ms
    Tree res = trainer.collectTree();

    
    // save main result
    File outdir = new File(outPath.toString());
    if(!outdir.exists())
    	outdir.mkdir();
    
    
    res.saveToFile(outPath.toString() +"/tree_" + Integer.toString(maxDepth) +".txt", locs);
    res.saveSplitInfosToFile(outPath.toString()+"/splitInfos.txt");

    // save intermediate trees
    for(int i=1;i<maxDepth;++i) 
    {
    	//Runtime runtime = Runtime.getRuntime();

    	// check the memory because at this point in the code there have been OutOfMemory errors
    	// when I used a full data set of 20GB, I ran out of memory when writing Tree 19
    	// The solution was to allocate 2GB of memory to the namenode (instead of 1GB)
    	// Right now there is still the problem that the datanodes get 2GB too (which is not necessary)
        /*NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        
        sb.append("\n\n Runtime memory info\n");
        
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("free memory: " + format.format(freeMemory / 1048576) + "MB\n");
        sb.append("allocated memory: " + format.format(allocatedMemory / 1048576) + "MB\n");
        sb.append("max memory: " + format.format(maxMemory / 1048576) + "MB\n");
        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1048576) + "MB\n");
        		
        System.out.println(sb);*/
        res.getSubTree(i).saveToFile(outPath.toString() 
                +"/tree_" + Integer.toString(i) +".txt", locs);
    }
    return 0;
  }
}

/*   // ---------------
    // create the queue and initiate it with the first file
    Queue<SplitTask> filesToSplitQueue =
            new PriorityQueue<SplitTask>(10,new SplitTask_Comparator());
    filesToSplitQueue.add(new SplitTask(0,new Path(args[0])));

    // ---------------
    // here we go
    while( filesToSplitQueue.peek() != null)
    {
        // pop the queue
        SplitTask element =  filesToSplitQueue.remove();
        int currentNodeId = element.node;
        int depth         = 0;
        { // awfull, but i prefer not risking float imprecision
            int bla = element.node;
            while(bla>0) { bla=(bla-1)/2; depth++;}
        }

        // tell the world
        System.out.println("\n\n\n\n####################\n" +
                           "# PROCESSING :" + element.path.toString() +
                           " at depth "     + depth + "\n" +
                           "####################\n");

        // create the filenames
        Path featureFile       = new Path(element.path.toString() + "/features.seq");
        Path dir_temp          = new Path(element.path.toString() + "/temp");
        Path splitFile         = new Path(element.path.toString() + "/splitInfo.seq");
        Path dir_false         = new Path(element.path.toString() + "/0" );
        Path featureFile_false = new Path(dir_false.toString() + "/features.seq");
        Path dir_true          = new Path(element.path.toString() + "/1" );
        Path featureFile_true  = new Path(dir_true.toString() + "/features.seq");

        // ---------------
        // Here we basically skip jobs that are already performed
        // if this wasn't split, split it !
        if( fs.exists(splitFile) ) {
            nodes[currentNodeId] = readSplitInfo(conf, fs, splitFile);
            // go down the tree
            if( depth < 15 ){
                filesToSplitQueue.add( new SplitTask(2*currentNodeId+1, dir_false));
                filesToSplitQueue.add( new SplitTask(2*currentNodeId+2, dir_true) );
            }
        }
        else {
            // find the split
            nodes[currentNodeId] = null;
            FindBestSplit.findBestSplit(conf,
                                                               fs,
                                                               featureFile,
                                                               dir_temp);
            // perform the split
            SplitSamples.splitSamples(  conf,
                                        fs,
                                        nodes[currentNodeId].attribId,
                                        nodes[currentNodeId].threshVal,
                                        featureFile,
                                        dir_temp,
                                        featureFile_false,
                                        featureFile_true );
             // write to file
             writeSplitInfo(conf, fs, splitFile , nodes[currentNodeId]);
             // go down the tree
             if( depth < 15 ){
                filesToSplitQueue.add( new SplitTask(2*currentNodeId+1, dir_false));
                filesToSplitQueue.add( new SplitTask(2*currentNodeId+2, dir_true) );
            }
        }


        // ---------------
        // introduce the sons into the queue
    }
*/
