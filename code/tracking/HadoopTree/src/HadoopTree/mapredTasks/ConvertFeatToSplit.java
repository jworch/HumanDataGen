/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredTasks;



import HadoopTree.LabeledFeature;
import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.SplitPointAccum;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.io.LongWritable;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;


public class ConvertFeatToSplit {

    
public static class Map
    extends Mapper<LongWritable, LabeledFeature, LongWritable, LabeledSplitPointBuff>
{

    /// the threshs vector contains the thresholds that we are considering
    int[]                      threshs;

    /// the splitPointsBuffer has dimension [NUMATTRIBS] and gets filled
    /// with the result of getSplitPoint for each attribute
    LabeledSplitPointBuff      lsp;


    @Override
    public void setup(Context context ) throws IOException {
    	//load thresh file
        Configuration conf = context.getConfiguration();
        Path filename = new Path(conf.get("myparams.threshfile"));
        FileSystem fs = FileSystem.get(conf);
        int j=0;
        if( fs.getConf() == null) throw new IOException("Could not initialize the filesystem locally");
        if( !fs.exists(filename)) throw new IOException("Could not find the thresh file on hdfs");
        threshs            = SplitPointAccum.loadThreshFile(fs, filename);
        // allocate
        lsp                = new LabeledSplitPointBuff();
        
    }

    @Override
    public void map( LongWritable Key, LabeledFeature lf, Context context)
            throws IOException, InterruptedException {
        lsp.computeSplitPoints(lf, threshs);
        context.write(Key, lsp);
    }
}



public static Job createJob(Cluster cluster, Configuration baseConf )
        throws IOException, InterruptedException, ClassNotFoundException
{
	
    Configuration conf = new Configuration(baseConf);
    
    // create the job
    Job job = Job.getInstance(cluster, conf);
    job.setJobName("LabeledFeatToLabeledSplitPointBuff");
    job.setJarByClass(ConvertFeatToSplit.class);
    job.getConfiguration().set("myparams.threshfile", "attribThresh.txt");


    // #################
    // input
    job.setInputFormatClass(SequenceFileInputFormat.class);
    // #################
    // mapper
    job.setMapperClass(Map.class);
    job.setMapOutputKeyClass(LongWritable.class);			 
    job.setMapOutputValueClass(LabeledSplitPointBuff.class); 
    // #################
    // reducer
    job.setNumReduceTasks(0);
    // #################
    // output
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(LabeledSplitPointBuff.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    return job;
}



}
