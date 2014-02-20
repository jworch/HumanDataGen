package HadoopTree.mapredTasks;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.SplitInfo;
import HadoopTree.SplitPointAccum;
import HadoopTree.mapredTasks.FindBestSplit_sp.Map;
import HadoopTree.mapredTasks.FindBestSplit_sp.Reduce;
import HadoopTree.mapredUtils.ChangeMapRedParam;


// CLASS IN NO LONGER NECESSARY
/**
 * @author Maarten
 */
public class ReadLsp {

	public static class Map
    extends Mapper<LongWritable, LabeledSplitPointBuff, IntWritable, SplitPointAccum>
{
    
		public static enum READ_COUNT{
			NUMBER_LSPS,
			NUMBER_PRINTED,
			WRONG_LABEL_TOO_HIGH,
			WRONG_LABEL_TOO_LOW,
			LABELS_NOT_ZERO
			
		}

		int counter=0;

   

    @Override
    public void map( LongWritable Key, LabeledSplitPointBuff lsp, Context context)
            throws IOException, InterruptedException {
    	
    	counter++;
    	context.getCounter(READ_COUNT.NUMBER_LSPS).increment(1);
    	
    	int a = lsp.data[0];
    	if(a>31) {
    		context.getCounter(READ_COUNT.WRONG_LABEL_TOO_HIGH).increment(1);
    		System.out.println("WRONG LABEL TOO HIGH "+ a);
    	}
    	else if(a<0) {
    		context.getCounter(READ_COUNT.WRONG_LABEL_TOO_LOW).increment(1);
    		System.out.println("WRONG LABEL TOO LOW "+ a);
    		
    	}
    	else if(a!=0) {
    		context.getCounter(READ_COUNT.LABELS_NOT_ZERO).increment(1);
    		System.out.println("LABEL NOT ZERO "+a);
    	}
    	
//    	java.util.Random ran = new Random();
//    	int t = ran.nextInt(100);
    	
			//if (counter == 1 || t == 7) {
    		if(lsp.data[0] != 0 /*&& lsp.data[3] !=21*/){
				context.getCounter(READ_COUNT.NUMBER_PRINTED).increment(1);
				byte[] data = lsp.data;

				System.out.println("LABEL " + data[0] + ",PADDING " + data[1]);
				String s = "";
//				for (int i = 2; i < data.length; i++) {
				for (int i = 2; i < 101; i++) {
					s += data[i] + " ";
					if (i % 100 == 0) {
						System.out.println(s);
						s = "";
					}

				}
			}
    	
    	
    }

  
}

/**
 * Reducer Functor
 */
public static class Reduce
       extends Reducer<IntWritable, SplitPointAccum, IntWritable, SplitInfo>
{
	
   
}
	
	public static Job createJob(Cluster cluster, Configuration baseConf )
	        throws IOException, InterruptedException, ClassNotFoundException
	{
	    Configuration conf = new Configuration(baseConf);
	 
	    // create the job
	    Job job = Job.getInstance(cluster, conf);
	    job.setJobName("read the LabeledSplitPointBuffer");
	    job.setJarByClass(ReadLsp.class);
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
	
}
