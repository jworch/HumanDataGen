package HadoopTree.mapredTasks;

import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.mapredUtils.ReadMapRedResult;


import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;

// hadoop mapreduce
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;



import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FSDataInputStream;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


import java.util.Arrays;
import java.util.Scanner;




public class CountLabels
{


public static class LabelAccum  implements Writable{
    
    int[] labels;

    LabelAccum() {
        labels = new int[32];
        for(int i=0;i<32;++i) {labels[i] = 0;}      }

    void accum( LabelAccum B) {
        for(int i=0;i<32;++i) labels[i] += B.labels[i];
    }
    
    @Override
    public void write(DataOutput out) throws IOException {
        for(int i=0;i<32;++i) out.writeInt(labels[i]);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
         for(int i=0;i<32;++i) labels[i] = in.readInt();
    }

    @Override
    public String toString() {
        return Arrays.toString(labels);
    }
}


public static class Map
    extends Mapper<LongWritable, LabeledSplitPointBuff, IntWritable, LabelAccum> {
    LabelAccum accum;

    @Override
    public void setup(Context context ) {
        accum  = new LabelAccum();
    }



    @Override
    public void map( LongWritable Key, LabeledSplitPointBuff value, Context context)
            throws IOException, InterruptedException {
                int label = value.data[0]; //casting from byte to label
                accum.labels[label] += 1;
    }

    @Override
    public void cleanup(Context context)
        throws IOException,  InterruptedException{
        IntWritable key = new IntWritable(0);
        context.write(key, accum);
    }
}


public static class Reduce
       extends Reducer<IntWritable, LabelAccum, IntWritable, LabelAccum>{

   @Override
    public void reduce(IntWritable label, Iterable<LabelAccum> values, Context context )
            throws IOException, InterruptedException {

        LabelAccum res = new LabelAccum();
        
        for(LabelAccum itr : values) {
        	// accumulate the count for a certain label
            res.accum(itr);
        }

        context.write(new IntWritable(0), res);
    }
}



public static void countLabels(Configuration conf, FileSystem fs,  
        Path seqFile, Path outDir)
        throws IOException, InterruptedException, ClassNotFoundException
{
    // create the job

    Cluster cluster = new Cluster(conf);
    Job job = Job.getInstance(cluster, conf);
    job.setJobName("countLabels");

    // 0.20.203 Job job = new Job(conf, "countLabels2");
    job.setJarByClass(CountLabels.class);


    System.out.println("ffile is: " + seqFile);
    System.out.println("rfile is: " + outDir);

    // In hadoop the output file must not exist (to prevent the overwriting of results)
    if ( fs.delete(outDir, true) ) {
        System.out.println("Deleted former result File " + outDir);
    }

    FileInputFormat.addInputPath(job, seqFile);
    FileOutputFormat.setOutputPath(job, outDir);



    // #################
    // input
    job.setInputFormatClass(SequenceFileInputFormat.class);

    // #################
    // mapper
    job.setMapperClass(CountLabels.Map.class);
    job.setMapOutputKeyClass(IntWritable.class);  
    job.setMapOutputValueClass(LabelAccum.class); 


    // #################
    // reducer
    job.setReducerClass(CountLabels.Reduce.class);
    job.setNumReduceTasks(1);
    // #################
    // output
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(CountLabels.LabelAccum.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);



    boolean success = job.waitForCompletion(true);

    ReadMapRedResult<IntWritable, LabelAccum> printer
            = new ReadMapRedResult<IntWritable, LabelAccum>();
    printer.printResult(conf,fs,outDir,new IntWritable(), new LabelAccum());

}



}
