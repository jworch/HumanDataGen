/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredTasks;

import HadoopTree.DataTraits;
import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.SplitPointAccum;
import HadoopTree.SplitInfo;
import HadoopTree.mapredUtils.ReadMapRedResult;
import HadoopTree.mapredUtils.ChangeMapRedParam;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.io.IOException;
import java.text.NumberFormat;

public class FindBestSplit_sp {

	enum FindBestSplit_COUNTERS {
		SPLIT_INFOS_WRITTEN,
		
	}

/**
 * Mapper Functor
 */
public static class Map
    extends Mapper<LongWritable, LabeledSplitPointBuff, IntWritable, SplitPointAccum>
{
    /// all the samples going through that mapper accumulate on this thing,
    /// which only gets emitted on cleanup. This is a hack and a twist on the
    /// idea of Mapreduce but can be considered a combined Mapper/Combiner
    SplitPointAccum accum;

    /// a counter that prevents us from accumulating too much on a buffer that only
    /// has shorts
    int counter;


    @Override
    public void setup(Context context ) throws IOException {
        // load thresh file
        Configuration conf = context.getConfiguration();
        
        //TODO it is useless to load the threshfile in the Mapper already
        // this is only necessary in the Reducer
        Path filename = new Path(conf.get("myparams.threshfile"));
        
        FileSystem fs = FileSystem.get(conf);
        if( fs.getConf() == null) throw new IOException("Could not initialize the filesystem locally");
        if( !fs.exists(filename)) throw new IOException("Could not find the thresh file on hdfs");
        // allocate
        accum              = new SplitPointAccum();
        counter            = 0;

        //This was done to check how much memory was allocated to the datanodes
        //Currently they get almost 2GB, but this is actually too much for the datanodes
        //Only the namenode should have this amount of memory, but right now I have not
        //succeeded yet to give only the namenode extra memory..
//        System.out.println("CHECKING MEMORY");
//        Runtime runtime = Runtime.getRuntime();
//
//        NumberFormat format = NumberFormat.getInstance();
//
//        StringBuilder sb = new StringBuilder();
//        
//        sb.append("\n\n Runtime memory info\n");
//        
//        long maxMemory = runtime.maxMemory();
//        long allocatedMemory = runtime.totalMemory();
//        long freeMemory = runtime.freeMemory();
//
//        sb.append("free memory: " + format.format(freeMemory / 1048576) + "MB\n");
//        sb.append("allocated memory: " + format.format(allocatedMemory / 1048576) + "MB\n");
//        sb.append("max memory: " + format.format(maxMemory / 1048576) + "MB\n");
//        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1048576) + "MB\n");
//        		
//        System.out.println(sb);
    }

    @Override
    public void map( LongWritable Key, LabeledSplitPointBuff lsp, Context context)
            throws IOException, InterruptedException {
    	// accum contains an array of shorts
    	// after each invocation of accum.accumSplitPoints(lsp) elements could have been incremented by 1,
    	// the maximum value an short element can contain is java.lang.Short.MAX_VALUE
    	accum.accumSplitPoints(lsp);
        counter ++;        
        if( counter == java.lang.Short.MAX_VALUE ) {
        	//TODO the key is of no use and may be replaced by a NullWritable to save bandwidth
            context.write(new IntWritable(0), accum);
            accum = new SplitPointAccum();
            counter = 0;
        }
    }

    @Override
    public void cleanup(Context context)
        throws IOException,  InterruptedException{
        if(counter != 0 ) context.write(new IntWritable(0), accum);
    }
}

/**
 * Reducer Functor
 */
public static class Reduce
       extends Reducer<IntWritable, SplitPointAccum, IntWritable, SplitInfo>
{
    int threshs[];

    @Override
    public void setup(Context context ) throws IOException {
        // load thresh file
        Configuration conf = context.getConfiguration();
        Path filename = new Path(conf.get("myparams.threshfile"));
        FileSystem fs = FileSystem.get(conf);
        if( fs.getConf() == null) throw new IOException("Could not initialize the filesystem locally");
        if( !fs.exists(filename)) throw new IOException("Could not find the thresh file on hdfs");
        threshs            = SplitPointAccum.loadThreshFile(fs, filename);
     }

    @Override
    public void reduce(IntWritable attribId, Iterable<SplitPointAccum> values, Context context )
            throws IOException, InterruptedException
   {
        //trust java will init to 0
        int[] finalAccum = new int[SplitPointAccum.size];

        // accumulate all the buffers onto one unique vote buffer
        for(SplitPointAccum itr : values) {
            SplitPointAccum.accumOnInts(itr,finalAccum);
            // the SplitPointAccums objects contain arrays of short
            // now an array of integer is used (to store higher values)
        }

        // get the best split
        SplitInfo bestSplit = SplitPointAccum.findBestSplit(finalAccum, threshs);

        // return the best split
        // TODO use NullWritable as key to save bandwidth
        context.write(new IntWritable(0), bestSplit);
        context.getCounter(FindBestSplit_COUNTERS.SPLIT_INFOS_WRITTEN).increment(1);
        // this counter is just a check to see whether there is indeed only one SplitInfo written.
        // And this turns out to be true
    }
}







public static Job createJob(Cluster cluster, Configuration baseConf )
        throws IOException, InterruptedException, ClassNotFoundException
{
    Configuration conf = new Configuration(baseConf);
 
    // create the job
    Job job = Job.getInstance(cluster, conf);
    job.setJobName("findBestSplit_sp");
    job.setJarByClass(FindBestSplit_sp.class);
    
    // give this file as a parameter in the configuration file
    // such that the mappers and reducers that run on the nodes
    // can get that file from the Hadoop file system
    job.getConfiguration().set("myparams.threshfile", "attribThresh.txt");
    
    // give the mapper and the reducer more memory (2048MB)
    ChangeMapRedParam.changeParam(conf, "mapred.reduce.child.java.opts", "-Xmx2048m" );
    //max # of streams used at once for sorting
    ChangeMapRedParam.changeParam(conf, "mapreduce.task.io.sort.factor", 10 ); 
    ChangeMapRedParam.changeParam(conf, "mapreduce.task.io.sort.mb", 512 );
//    ChangeMapRedParam.changeParam(conf, "mapreduce.map.output.compress", true );
//    ChangeMapRedParam.changeParam(conf, "mapreduce.map.output.compress.codec", "org.apache.hadoop.io.compress.GzipCodec" );
    ChangeMapRedParam.changeParam(conf, "mapreduce.task.timeout", 3600000); //upgrade from 10min to 1h
    // #################
    // input
    job.setInputFormatClass(SequenceFileInputFormat.class);
    // #################
    // mapper
    job.setMapperClass(Map.class);
    job.setMapOutputKeyClass(IntWritable.class);
    job.setMapOutputValueClass(SplitPointAccum.class);
    // #################
    // reducer
    job.setReducerClass(Reduce.class);
    job.setNumReduceTasks(1);
    // #################
    // output
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(SplitInfo.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);

    return job;
}

public static SplitInfo readResult(Configuration conf, Path outDir)
    throws IOException {
    FileSystem fs = FileSystem.get(conf);
    ReadMapRedResult<IntWritable, SplitInfo> printer
            = new ReadMapRedResult<IntWritable, SplitInfo>();
    Path resPath = new Path(outDir + "/part-r-00000");
    SplitInfo res = new SplitInfo();
    printer.readFirstResult(conf,fs, resPath, new IntWritable(), res); // reads first result from 'outfile' into res
    return res;
}


}
