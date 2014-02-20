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

// HadoopTree
import HadoopTree.LabeledFeature;
import HadoopTree.mapredTasks.CountLabels;



public class MainCountLabels
        extends Configured
        implements Tool
{
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new MainCountLabels(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {
        Configuration conf = new Configuration();
        // parse the command line to a hadoop configuration
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
          System.err.println("Usage: countLabels <rootDir> <outDirectory> "
                          + "where <rootDir> contains the splitbuffs.seq file");
          System.exit(2);
        }

        System.out.print("\n\n\n\n\n" +
                "####################\n" +
                "# STARTING COUNTLABELS \n" +
                "####################\n");

        FileSystem fs = FileSystem.get(conf);
        Path seqFile = new Path(otherArgs[0] + "/splitbuffs.seq");
        Path outDir = new Path(otherArgs[1]);
        
        CountLabels.countLabels( conf, fs, seqFile, outDir);
        return 0;
    }


}
