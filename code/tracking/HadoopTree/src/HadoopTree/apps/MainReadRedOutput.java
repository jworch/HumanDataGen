package HadoopTree.apps;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapred.SequenceFileRecordReader;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.SplitInfo;
import HadoopTree.mapredTasks.FindBestSplit_sp;
import HadoopTree.mapredTasks.ReadLsp;
import HadoopTree.mapredTasks.ReadLsp.Map.READ_COUNT;


/**
 * This class is created to check whether the output of the reducer of SplitSamples_sp
 * is correct. And indeed, the files part-r-000xx that end with an even number contain the
 * keys that are 0 (the samples that belong to these keys are thus false). This is correct
 * since in SplitSample_sp the methods moveSplits() and getSplits() depend on this assumption.
 * 
 * This thus mean that a reducer saves its output in a file with as number its number for the 
 * reduce task (or something like that). I have not yet seen any confirmation of this, but this
 * class proves this assumption is correct!  
 * 
 * Notice that the file name 'part-r-000xx' will have been changed by the program into the 
 * form 'xx.seq' and is in a splitbuffs.seq. The number 'xx' stays the same for both files.
 * 
 * @author Maarten
 *
 */
public class MainReadRedOutput
	extends Configured
    implements Tool
{
public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new MainReadRedOutput(), args);
    System.exit(res);
}

public int run(String[] args) throws Exception {
    Configuration conf = new Configuration();
    // parse the command line to a hadoop configuration
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 1) {
      System.err.println("use the root directory of a reducer output file as argument. Originally" +
      		" such a file gets the name 'part-r-000xx', but the program might already have changed this" +
      		" name. Look in the Hadoop file system for file names in the form of '5.seq' that are in a" +
      		" 'splitbuffs.seq' directory. ");
      System.exit(2);
    }

    Path file = new Path(otherArgs[0]);
    
    SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(conf), file, conf);
    
//    LongWritable key = new LongWritable();
//    
//    for(int i=0;i<10;++i){
//    	reader.next(key);
//    	System.out.println("KEY: " + key);
//    }
    
    IntWritable key = new IntWritable();
    SplitInfo info = new SplitInfo();
    boolean busy = true;
    int i =0;
  	while(busy) {
//    for(int j=0;j<100;j++){
    	busy=reader.next(key);
    	i++;
    	System.out.println(i+" KEY: " + key/*+" GAIN: "+ info.entropyGain*/);
    	    	
    }
		
			
		
    
//    // create a cluster
//    Cluster cluster = new Cluster(conf);
//    // init the filesystem
//    FileSystem fs = FileSystem.get(conf);
//
//
//    // create a job
//    Job job = ReadLsp.createJob( cluster, conf);
//    (FileSystem.get(conf)).delete(new Path("resread"),true);
//    FileOutputFormat.setOutputPath(job, new Path("resread"));
//    
//    FileInputFormat.addInputPath(job, seqFile);
//
//    
//    job.submit();
//    System.out.println("JOBINFO: "+job.toString());
//    job.waitForCompletion(true);
    return 0;
}


}
