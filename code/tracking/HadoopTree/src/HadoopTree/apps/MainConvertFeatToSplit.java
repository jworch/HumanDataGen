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
import HadoopTree.mapredTasks.ConvertFeatToSplit;



public class MainConvertFeatToSplit
        extends Configured
        implements Tool
{
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new MainConvertFeatToSplit(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {
        Configuration conf = new Configuration();
        // parse the command line to a hadoop configuration
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
          System.err.println("Usage: ConvertFeatToSplit <features.feat> <rootDir>" );
          System.exit(2);
        }


        System.out.print("\n\n\n\n\n" +
                "####################\n" +
                "# STARTING FEAT TO SPLIT \n" +
                "####################\n");

        FileSystem fs      = FileSystem.get(conf);
        Path featFile      = new Path(otherArgs[0]);
        Path rootDir       = new Path(otherArgs[1]);
        Path splitBuffFile = new Path(rootDir.toString() + "/splitbuffs.seq");
        
        

        // create a cluster
        Cluster cluster = new Cluster(conf);
        // create a job
        Job job = ConvertFeatToSplit.createJob( cluster, conf  );
        
        // In hadoop the output file must not exist (to prevent the overwriting of results)
        if ( fs.delete(rootDir, true) ) System.out.println("Deleted " + rootDir);
        FileInputFormat.addInputPath(job, featFile);
        FileOutputFormat.setOutputPath(job, splitBuffFile);
        job.waitForCompletion(true);
        System.out.println("THE CONVERTED FEATURES ARE SAVED IN: "+splitBuffFile.toString());
        return 0;
    }


}
