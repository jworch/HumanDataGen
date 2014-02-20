package HadoopTree.apps;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;

import java.nio.ByteBuffer;

import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.LongWritable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.GenericOptionsParser;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

// HadoopTree
import HadoopTree.SplitInfo;
import HadoopTree.LabeledFeature;
import HadoopTree.mapredTasks.FindBestSplit_sp;


public class MainFindBestSplit
        extends Configured
        implements Tool
{
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new MainFindBestSplit(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {
        Configuration conf = new Configuration();
        // parse the command line to a hadoop configuration
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 1) {
          System.err.println("Usage: findBestSplit <inDir> where inDir " +
                  "contains the splitbuffs.seq file ");
          System.exit(2);
        }

        
        
        System.out.print("\n\n\n\n\n" +
                "####################\n" +
                "# STARTING FINDBESTSPLIT \n" +
                "####################\n");

        
        Path rootDir = new Path(otherArgs[0]);
        Path seqFile = new Path(rootDir.toString() + "/splitbuffs.seq");
        Path outDir  = new Path(rootDir.toString() + "/bestSplit");

        // create a cluster
        Cluster cluster = new Cluster(conf);
        // init the filesystem
        FileSystem fs = FileSystem.get(conf);


        // create a job
        Job job = FindBestSplit_sp.createJob( cluster, conf);
        if ( fs.delete(outDir, true) ) System.out.println("Deleted " + outDir);
        FileInputFormat.addInputPath(job, seqFile);
        FileOutputFormat.setOutputPath(job, outDir);

        boolean success = job.waitForCompletion(true);

        // read back the information
        SplitInfo info = FindBestSplit_sp.readResult(conf, outDir);
        System.out.println(info);
        
        return 0;
    }


}
