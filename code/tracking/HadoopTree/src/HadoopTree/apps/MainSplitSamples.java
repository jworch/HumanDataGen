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
import HadoopTree.SplitPointAccum;
import HadoopTree.mapredTasks.SplitSamples_sp;

import java.util.Arrays;

public class MainSplitSamples
        extends Configured
        implements Tool
{
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new MainSplitSamples(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {
        Configuration conf = new Configuration();
        // parse the command line to a hadoop configuration
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 3) {
          System.err.println("Usage: SplitSamples <attribId> <threshValue> <inDir>" +
                  "where inDir contains the *.seq file and threshValue needs to be in attribThresh.txt");
          System.exit(2);
        }




        System.out.print("\n\n\n\n\n" +
                "####################\n" +
                "# STARTING SPLITSAMPLES \n" +
                "####################\n");
        
        FileSystem fs = FileSystem.get(conf);

        int[] threshs = SplitPointAccum.loadThreshFile(fs, new Path("attribThresh.txt"));
        int attribId  = Integer.parseInt(otherArgs[0]);
        int threshVal = Integer.parseInt(otherArgs[1]);
        int threshId  = Arrays.binarySearch(threshs, threshVal);
        if( threshId < 0 ) return -1; // crash if the threshold doesnt exist

        Path rootDir    = new Path(otherArgs[2]);
        Path seqFile    = new Path(rootDir.toString() + "/splitbuffs.seq");
        Path tempDir    = new Path(rootDir.toString() + "/temp");
        Path file_false = new Path(rootDir.toString() + "/0/splitbuffs.seq");
        Path file_true  = new Path(rootDir.toString() + "/1/splitbuffs.seq");

        // create a cluster
        Cluster cluster = new Cluster(conf);
        // create a job
        Job job = SplitSamples_sp.createJob( cluster, conf, attribId, threshId,rootDir );
        if ( fs.delete(tempDir, true) )    System.out.println("Deleted " + tempDir);
        if ( fs.delete(file_false, true) ) System.out.println("Deleted " + file_false);
        if ( fs.delete(file_false, true) ) System.out.println("Deleted " + file_true);
        FileInputFormat.addInputPath(job, seqFile);
        FileOutputFormat.setOutputPath(job, tempDir);
        job.setNumReduceTasks(2);
        System.out.println("CAREFULL we are only using two reducers on this test program");

        job.waitForCompletion(true);

        SplitSamples_sp.moveSplits(fs, tempDir, file_false, file_true);
        // ?? moveSplits needs dir_false and dir_true, not files..
        
        return 0;
    }


}
