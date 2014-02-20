/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.TTrain;

import HadoopTree.Tree;
import HadoopTree.SplitInfo;
import HadoopTree.SplitPointAccum;
import HadoopTree.mapredUtils.JobResultManager;
import HadoopTree.mapredUtils.ChangeMapRedParam;

import HadoopTree.mapredTasks.FindBestSplit_sp;
import HadoopTree.mapredTasks.InMemTreeBuild_sp;
import HadoopTree.mapredTasks.SplitSamples_sp;

import HadoopTree.mapredUtils.JobResultManager;
import HadoopTree.mapredUtils.JobResultListener;
import HadoopTree.mapredUtils.JobResultException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.util.Scanner;
import org.apache.hadoop.hdfs.server.namenode.FSClusterStats;

import sun.awt.SubRegionShowable;


public class TreeTrainer_sp {

    JobResultManager JMgr;
    int              maxDepth;
    Path             rootPath;
    Queue<Integer>   nodeToSplitQueue;
    int[]            threshs;
    PrintWriter 	 pw=null;


    public TreeTrainer_sp(Configuration originalConfiguration, Path rPath, int  maximumDepth)
        throws IOException {
        JMgr             = new JobResultManager(originalConfiguration);
        rootPath         = rPath;
        maxDepth         = maximumDepth;
        nodeToSplitQueue = new PriorityQueue<Integer>();
        threshs          = SplitPointAccum.loadThreshFile(JMgr.getFs(),
                                                  new Path("attribThresh.txt"));
        //TODO this was for debugging, not necessary anymore (for now)
        //createLogFile();
    }

    // this method was for debugging, not necessary anymore (for now)
    private void createLogFile() {
    	
    	// each log file is now for only one run of the program 
    	// and will be overwritten in a consecutive run
    	try {
			//File file = new File(rootPath.toString()+"/logs_node.txt");
			String path = System.getProperty("user.dir");
			System.out.println(path);
			System.out.println(rootPath.toString());
			File dir = new File(new URI("file:///"+path + "/logs"));
			if(!dir.exists())
				dir.mkdir();
			System.out.println(dir.toURI());
			File file = new File(new URI(dir.toURI()+"logs_node.txt"));
			
			if(file.exists()) {
				file.delete();
				System.out.println("deleted file: " + rootPath.toString()+"/logs_node.txt");			
			}
			else
				file.createNewFile();
			pw = new PrintWriter( new BufferedWriter( 
					new FileWriter(file)));
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private int counter=0;
    
    public void pushOnSplitQueue(int nodeId) {
        nodeToSplitQueue.add(new Integer(nodeId));
        
        
        // write in a file wich nodes are pushed to check there sequence
        // it may be time consuming, so comment this code when errors are solved
        
        // this was for debugging, not necessary anymore (for now)
//        pw.print(Integer.toString(nodeId)+"  ");
//        if(counter%20==0)
//        	pw.print("\n");
//        
//        if(counter%100==0) {
//        	pw.flush();
//        	counter=0;
//        }
//        counter++;
    }


    // #################################################
    // #################################################
    // Helper class to manage the IO
    // #################################################
    // #################################################
    /**
     * Return the SplitInfo object from the sequence file that is 
     * saved on the Hadoop file system.
     */
    public static SplitInfo readSplitInfo(Configuration conf, Path file)
        throws IOException {
    	// the file is a splitInfo.seq file
        FileSystem fs = FileSystem.get(conf);
        SplitInfo info = new SplitInfo();
        IntWritable key = new IntWritable();
        SequenceFile.Reader reader = new SequenceFile.Reader(fs, file, conf);
        boolean res = reader.next(key, info);
        if( !res ) throw( new IOException("(E) could not read a result from " + file.toString()) );
        reader.close();
        return info;
    }

    /**
     * Read the sequence file of the subtree and fill in that information
     * in the tree that is passed as an argument.
     * 
     */
    public static void readTree(Configuration conf, Path file, Tree t)
        throws IOException {
        FileSystem fs = FileSystem.get(conf);
        IntWritable key = new IntWritable();
        SequenceFile.Reader reader = new SequenceFile.Reader(fs, file, conf);
        boolean res = reader.next(key, t); //will throw
        if( !res ) throw( new IOException("(E) could not read a result from " + file.toString()) );
        reader.close();
    }

    /**
     * Write the information of a SplitInfo object in a sequence file
     * that is saved on the Hadoop file system.
     */
    public static void writeSplitInfo(Configuration conf, Path file, SplitInfo info)
            throws IOException {
        FileSystem fs = FileSystem.get(conf);
        SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
                file,
                IntWritable.class,
                SplitInfo.class,
                SequenceFile.CompressionType.NONE);
        writer.append(new IntWritable(0), info);
        writer.close();
    }

    public static void writeEmptyFile(FileSystem fs, Path file)
            throws IOException {
        fs.create(file);
    }







    // #################################################
    // #################################################
    // Helper classes to setup Jobs
    // #################################################
    // #################################################
    private Job setupJob_FindBestSplit( Path nodepath )
        throws IOException, InterruptedException, ClassNotFoundException
    {
        // paths
        Path DIR_bestSplit = NodeStatus_sp.DIR_bestSplit(nodepath);
        Path FILE_Seq      = NodeStatus_sp.FILE_Seq(nodepath);
        
        // create job
        Job job = FindBestSplit_sp.createJob( JMgr.getCluster(), JMgr.getConfig());
        FileSystem fs = JMgr.getFs();
        // setup input-output paths
        if ( fs.delete(DIR_bestSplit, true) ) System.out.println("Deleted " + DIR_bestSplit);
        
        // the input path may be a file, or it may be a directory
        // when it is a directory all the files (except .SUCCESS) in that directory will be
        // considered as input for the job
        FileInputFormat.addInputPath(job, FILE_Seq);
        
        FileOutputFormat.setOutputPath(job, DIR_bestSplit);
        return job;
    }

    private Job setupJob_SplitSamples( Path nodePath, int attribId, int thresh)
        throws IOException, InterruptedException, ClassNotFoundException
    {
        // paths
        Path FILE_Seq  = NodeStatus_sp.FILE_Seq(nodePath);
        Path DIR_Temp  = NodeStatus_sp.DIR_Temp(nodePath);
        Path DIR_false = NodeStatus_sp.DIR_false(nodePath);
        Path DIR_true  = NodeStatus_sp.DIR_true(nodePath);
        // create job
        Job job = SplitSamples_sp.createJob( JMgr.getCluster(), JMgr.getConfig(), attribId, thresh,nodePath);
        FileSystem fs = JMgr.getFs();
        if ( fs.delete(DIR_Temp, true) )  System.out.println("Deleted " + DIR_Temp);
        if ( fs.delete(DIR_false, true) ) System.out.println("Deleted " + DIR_false);
        if ( fs.delete(DIR_true, true) )  System.out.println("Deleted " + DIR_true);
        FileInputFormat.addInputPath(job, FILE_Seq);
        FileOutputFormat.setOutputPath(job, DIR_Temp);
        // setup input-output paths
        
        
        long inputLength = NodeStatus_sp.getInputLength(fs, FILE_Seq);
        long numBlocks = inputLength / fs.getDefaultBlockSize();
        long numNodes = JMgr.getCluster().getClusterStatus().getTaskTrackerCount();
        if( numBlocks >  2*numNodes) {
        	job.setNumReduceTasks((int)(2*numNodes));
//        	System.out.println("NUMBER REDUCE TASKS set to "+ 2*numNodes + " (twice the number of nodes," +
//        			" because " + numBlocks + " blocks)");
        }
        else {
        	job.setNumReduceTasks((int)(2*((numBlocks+1)/2))); // trying to keep it even
//        	System.out.println("NUMBER REDUCE TASKS set to "+ 2*((numBlocks+1)/2) + " because " + numBlocks + " blocks");
        }
        return job;
    }

    private Job setupJob_InMemTreeBuild( Path nodePath,  int subTreeDepth )
        throws IOException, InterruptedException, ClassNotFoundException
    {
        // paths
        Path FILE_Seq  = NodeStatus_sp.FILE_Seq(nodePath);
        Path DIR_Temp  = NodeStatus_sp.DIR_Temp(nodePath);
        Path DIR_false = NodeStatus_sp.DIR_false(nodePath);
        Path DIR_true  = NodeStatus_sp.DIR_true(nodePath);
        // create job
        Job job =  InMemTreeBuild_sp.createJob(JMgr.getCluster(), JMgr.getConfig(),
                new Path("attribThresh.txt"), nodePath, subTreeDepth);
        FileSystem fs = JMgr.getFs();
        // delete previous results
        if ( fs.delete(DIR_Temp, true) )  System.out.println("Deleted " + DIR_Temp);
        if ( fs.delete(DIR_false, true) ) System.out.println("Deleted " + DIR_false);
        if ( fs.delete(DIR_true, true) )  System.out.println("Deleted " + DIR_true);
        FileStatus[] fileList
                = fs.globStatus(new Path( nodePath.toString() + "/subtree*.seq" ));
        
        for(FileStatus s: fileList) {
            fs.delete(s.getPath(), false);
            System.out.println("Deleted wrongly developped subtree :"
                    + s.getPath().toString());
            
        }

        // setup input-output paths
        FileInputFormat.addInputPath(job, FILE_Seq);
        FileOutputFormat.setOutputPath(job, DIR_Temp);

        return job;
    }



    // #################################################
    // #################################################
    // Helper class to manage the callbacks
    // #################################################
    // #################################################
    private static class FindBestSplitListener
            implements JobResultListener {

        TreeTrainer_sp  trainer;
        int             nodeId;

        public FindBestSplitListener( TreeTrainer_sp trainer_, int nodeId_ ) {
            trainer = trainer_;
            nodeId = nodeId_;
        }

        @Override
        public void stateChanged(JobResultManager.JobStatus status)
            throws IOException, InterruptedException, ClassNotFoundException, JobResultException {
            if( status != JobResultManager.JobStatus.SUCCESSFUL)
                throw new JobResultException(NodeStatus_sp.buildNodePath(new Path("/"), nodeId).toString()
                    + " could not be properly developed by findBestSplit\n" +
                    "nodeId is: "+nodeId);
            trainer.callback_FindBestSplit_done(status, nodeId);
        }
    }

    private static class SplitSamplesListener
            implements JobResultListener {

        TreeTrainer_sp  trainer;
        int             nodeId;
        SplitInfo       info;

        public SplitSamplesListener(TreeTrainer_sp trainer_, int nodeId_, SplitInfo info_ ) {
            trainer = trainer_;
            nodeId  = nodeId_;
            info    = info_;
        }

        @Override
        public void stateChanged(JobResultManager.JobStatus status)
            throws IOException, InterruptedException, ClassNotFoundException, JobResultException {
            if( status != JobResultManager.JobStatus.SUCCESSFUL)
                throw new JobResultException(NodeStatus_sp.buildNodePath(new Path("/"), nodeId).toString()
                    + " could not be properly divided by SplitSamples");
            trainer.callback_SplitSamples_done(status, nodeId, info);
        }
    }

    private static class InMemTreeBuildListener
            implements JobResultListener {

        TreeTrainer_sp  trainer;
        int             nodeId;
        int             subTreeDepth;

        public InMemTreeBuildListener(TreeTrainer_sp trainer_, int nodeId_, int subTreeDepth_ ) {
            trainer      = trainer_;
            nodeId       = nodeId_;
            subTreeDepth = subTreeDepth_;
        }

        @Override
        public void stateChanged(JobResultManager.JobStatus status)
            throws IOException, InterruptedException, ClassNotFoundException, JobResultException{
            if( status != JobResultManager.JobStatus.SUCCESSFUL)
                throw new JobResultException(NodeStatus_sp.buildNodePath(new Path("/"), nodeId).toString()
                   + " could not be properly developped by InMemTreeBuild");
            trainer.callback_InMemTreeBuild_done(status, nodeId, subTreeDepth);
        }
    }







    // #################################################
    // #################################################
    // Callbacks
    // #################################################
    // #################################################
    /**
     * read the split back and launch a splitSample job
     * @param nodeId
     */
    public void callback_FindBestSplit_done(JobResultManager.JobStatus status, int nodeId)
        throws IOException, InterruptedException, ClassNotFoundException
    {
        Path nodePath  = NodeStatus_sp.buildNodePath(rootPath, nodeId);
        Path DIR_bestSplit = NodeStatus_sp.DIR_bestSplit(nodePath);

        // read the split back
        SplitInfo info = FindBestSplit_sp.readResult(JMgr.getConfig(), DIR_bestSplit);
       //System.out.println("CALLBACK FindBestSpit: "+info);

        // get the index of the threshold value in the threshold array
        int threshId = Arrays.binarySearch(threshs, info.threshVal);
        if(threshId < 0) throw new IOException("this should not happen");
        // create next job
        Job job = setupJob_SplitSamples(nodePath, info.attribId, threshId);
        SplitSamplesListener listener = new SplitSamplesListener( this, nodeId, info);
        JMgr.addJob(job, listener);
    }

    /**
     * moves the resulting files to child *.seq directories
     * writes the split Info
     * signals that we are ready for the next
     * @param nodeId
     * @param info
     */
    public void callback_SplitSamples_done(JobResultManager.JobStatus status, int nodeId, SplitInfo info)
        throws IOException
    {
        Path nodePath   = NodeStatus_sp.buildNodePath(rootPath, nodeId);
        Path DIR_Temp   = NodeStatus_sp.DIR_Temp(nodePath);
        
        Path FILE_Info  = NodeStatus_sp.FILE_Info(nodePath);
       
       

        FileSystem fs = JMgr.getFs();
        // first we clean up the result of splitSamples to two big files
        // doesn't have to come here DIR_false and DIR_true???
        // --> No, maybe this is not working, SEE THE getInputLength(FileSystem fs, Path seqFile) METHOD IN NodeStatus_sp

//        Path DIR_false = NodeStatus_sp.DIR_false(nodePath);
//        Path DIR_true  = NodeStatus_sp.DIR_true(nodePath);
//        SplitSamples_sp.moveSplits(fs, DIR_Temp, DIR_false, DIR_true );
        
        Path FILE_false = NodeStatus_sp.FILE_false(nodePath);
        Path FILE_true  = NodeStatus_sp.FILE_true(nodePath);
        SplitSamples_sp.moveSplits(fs, DIR_Temp, FILE_false, FILE_true );
        
        // now that the data has been split, we write the result
        writeSplitInfo(JMgr.getConfig(), FILE_Info, info);

        // recurse on false child only if necessary
        // 2*nodeId+1 is a node down to the left
        if(info.hp.getNfalse() == 0 ) {
            Path p = NodeStatus_sp.buildNodePath(rootPath,2*nodeId+1);
            writeEmptyFile(fs, NodeStatus_sp.FILE_EmptyFlag(p));
        }
        else {
            pushOnSplitQueue(2*nodeId+1);
        }

        // recurse on true child if necessary
        if(info.hp.getNtrue() == 0 ){
            Path p = NodeStatus_sp.buildNodePath(rootPath,2*nodeId+2);
            writeEmptyFile(fs, NodeStatus_sp.FILE_EmptyFlag(p));
        }
        else {
        	//System.out.println("PUSHED NODE "+(2*nodeId+2));
            pushOnSplitQueue(2*nodeId+2);
        }
    }

    /**
     * copies the reduce result (which is a tree in a seq file) to its proper location
     * @param nodeId
     */
    public void callback_InMemTreeBuild_done(JobResultManager.JobStatus status, int nodeId, int subtreeDepth)
        throws IOException
    {
        Path nodePath      = NodeStatus_sp.buildNodePath(rootPath, nodeId);
        Path FILE_SubTree  = NodeStatus_sp.FILE_SubTree(nodePath, subtreeDepth);
        Path DIR_Temp      = NodeStatus_sp.DIR_Temp(nodePath);

        FileSystem fs     = JMgr.getFs();
        Path resultPath   = new Path(DIR_Temp.toString() + "/part-r-00000");

        if( !fs.exists(resultPath)) throw new IOException("(E) could not find " + resultPath.toString());
        boolean success = fs.rename(resultPath, FILE_SubTree);
        if( !success) throw new IOException("(E) could not rename "
                + resultPath.toString() + " to "
                + FILE_SubTree.toString());
    }





    // #################################################
    // #################################################
    // Training
    // #################################################
    // #################################################

    public void SplitNode(int nodeId )
            throws IOException, InterruptedException, ClassNotFoundException, Exception_TTrain
    {
        int  nodeDepth = NodeStatus_sp.buildNodeDepth(nodeId);
        Path nodePath  = NodeStatus_sp.buildNodePath(rootPath, nodeId);
        
        if (nodeDepth >= maxDepth) return; // STOOP !
        FileSystem fs = JMgr.getFs();


        System.out.println("\n\n##############\n Splitting "
                + nodePath.toString() + "\n####\n");
        Job job = setupJob_FindBestSplit(nodePath);
        FindBestSplitListener listener = new FindBestSplitListener(this, nodeId);
        JMgr.addJob(job, listener); // launching job
    }


    public void SplitNode_inmem(int nodeId)
        throws IOException, InterruptedException, ClassNotFoundException, Exception_TTrain
    {
        int  nodeDepth = NodeStatus_sp.buildNodeDepth(nodeId);
        Path nodePath  = NodeStatus_sp.buildNodePath(rootPath, nodeId);

        if (nodeDepth >= maxDepth) return; // STOOP !
        int subTreeDepth = maxDepth - nodeDepth;
        System.out.println("\n\n##############\n Splitting in mem "
                + nodePath.toString() +
                "\n with " + Integer.toString(subTreeDepth)
                + ((subTreeDepth==1)? " level":" levels") +" remaining\n####\n");
        Job job = setupJob_InMemTreeBuild(nodePath, subTreeDepth);
        
        InMemTreeBuildListener listener
                = new InMemTreeBuildListener(this, nodeId, subTreeDepth);
        JMgr.addJob(job, listener);
    }


    public void developTree(int pollingPeriod_ms)
        throws IOException, InterruptedException, ClassNotFoundException,
        Exception_TTrain, JobResultException {

        FileSystem fs = JMgr.getFs();
        pushOnSplitQueue(0);

        // pop jobs from the queue to be split
        while( !( nodeToSplitQueue.isEmpty() && JMgr.numJobs() == 0) ) {

            // empty our queue in the Hadoop JMgr...
            // can be made smarter in case this stresses him
            while( !nodeToSplitQueue.isEmpty() && JMgr.numJobs() < 25) {
                int nodeId    = nodeToSplitQueue.remove().intValue();
                
                int nodeDepth = NodeStatus_sp.buildNodeDepth(nodeId);

                NodeStatus_sp.Status status
                        = NodeStatus_sp.getNodeStatus(fs, rootPath, nodeId, maxDepth);

               System.out.println("node "+Integer.toString(nodeId) +
                       " at depth "+ Integer.toString(nodeDepth) +" has status ");
               System.out.println(status);


                switch( status ) {
                    case TO_SPLIT : {
                         SplitNode(nodeId);
                         break;
                    }
                    case TO_SPLIT_AS_SUBTREE : {
                        SplitNode_inmem(nodeId);
                        break;
                    }
                    case TO_SPLIT_AS_SUBTREE_WITH_NEW_DEPTH : {
                        SplitNode_inmem(nodeId);
                        break;
                    }
                    case TO_WRITE_AS_EMPTY : {
                        Path nodePath = NodeStatus_sp.buildNodePath(rootPath, nodeId);
                        writeEmptyFile(fs, NodeStatus_sp.FILE_EmptyFlag(nodePath));
                        //TODO break ??
                    }
                    case SPLIT : {
                        if(nodeDepth < maxDepth) {
                            pushOnSplitQueue(2*nodeId+1);
                            pushOnSplitQueue(2*nodeId+2);
                        }
                        break;
                    }
                    case SPLIT_AS_SUBTREE : break;
                    case EMPTY : break;
                }
            }

            Thread.sleep(pollingPeriod_ms);
            JMgr.poll();
        }
        // queue is empty, so no more nodes will be written
//		if(pw!=null)
//			pw.close(); // this was for debugging
		
		System.out.println("NUMBER JOBS: "+ JMgr.numJobs());
	

    }




    // #################################################
    // #################################################
    // Final Tree Collection
    // #################################################
    // #################################################
    public Tree collectTree()
        throws IOException, InterruptedException, ClassNotFoundException, Exception_TTrain
    {
        FileSystem fs = JMgr.getFs();

        Tree t = new Tree(maxDepth);
        Queue<Integer> queue = new PriorityQueue<Integer>();
        queue.add(0);

        List<Path> treeFilesToDeleteList = new LinkedList<Path>();


        // pop jobs from the queue to be split
        while( ! queue.isEmpty() ) {
            int nodeId     = queue.remove().intValue();
            int nodeDepth  = NodeStatus_sp.buildNodeDepth(nodeId);
            Path nodePath  = NodeStatus_sp.buildNodePath(rootPath, nodeId);
            Path FILE_Info = NodeStatus_sp.FILE_Info(nodePath); // a splitInfo.seq file

            NodeStatus_sp.Status status
                    = NodeStatus_sp.getNodeStatus(fs, rootPath, nodeId, maxDepth);

            System.out.println("in COLLECTTREE: node "+Integer.toString(nodeId) +
                       " at depth "+ Integer.toString(nodeDepth) +" has status ");
               System.out.println(status);
            switch( status ) {
                case TO_SPLIT : {
                     throw new Exception_TTrain(nodePath, "should be split before we run collectTree");
                }
                case TO_SPLIT_AS_SUBTREE : {
                     throw new Exception_TTrain(nodePath, "should be split before we run collectTree");
                }
                case TO_SPLIT_AS_SUBTREE_WITH_NEW_DEPTH : {
                     throw new Exception_TTrain(nodePath, "should be split before we run collectTree");
                }
                case TO_WRITE_AS_EMPTY : {
                    throw new Exception_TTrain(nodePath, "should have been flagged as EMPTY before we run CollectTree");
                }
                case SPLIT : {
                    t.nodes[nodeId] = readSplitInfo(JMgr.getConfig(), FILE_Info);
                    if(nodeDepth < maxDepth-1) {
                            queue.add(new Integer(2*nodeId+1));
                            queue.add(new Integer(2*nodeId+2));
                    }
                    break;
                }
                case SPLIT_AS_SUBTREE : {
                    Path FILE_SubTree = NodeStatus_sp.FILE_SubTree(nodePath, maxDepth-nodeDepth);
                    Tree subtree = new Tree(maxDepth-nodeDepth);
                    try {
                        readTree(JMgr.getConfig(), FILE_SubTree, subtree );
                        t.insertSubTree(nodeId, subtree);
                    } catch(Exception e) {
                        // the tree was not the correct length
                        System.out.println(e);
                        treeFilesToDeleteList.add(FILE_SubTree);
                    }
                    break;
                }
                case EMPTY : break;
            }
        }

        if( !treeFilesToDeleteList.isEmpty() ) {
            for(Path p : treeFilesToDeleteList) {
                JMgr.getFs().delete(p, false);
                System.out.println("Deleted wrongly developped subtree :" + p.toString());
            }
            throw new Exception_TTrain(rootPath, "There were some wrongly developped subtrees... please rerun");
        }

        return t;
    }


}

