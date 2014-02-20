package HadoopTree.apps;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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

public class MainReadLsp 
	extends Configured
    implements Tool
{
public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new MainReadLsp(), args);
    System.exit(res);
}

public int run(String[] args) throws Exception {
    Configuration conf = new Configuration();
    // parse the command line to a hadoop configuration
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 1) {
      System.err.println("use the root directory of the splitbuffs.seq file as argument ");
      System.exit(2);
    }

    Path rootDir = new Path(otherArgs[0]);
    Path seqFile = new Path(rootDir.toString() + "/splitbuffs.seq");

    System.out.println("ARRIVED");
    
    SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(conf), seqFile, conf);
		LabeledSplitPointBuff lsp = new LabeledSplitPointBuff();
		reader.next(new LongWritable(0), lsp);

		int a=999999;
		System.out.println(a);
		a = lsp.data[0];
		System.out.println(a);
		if (a > 31) {
			System.out.println("WRONG LABEL TOO HIGH " + a);
		} else if (a < 0) {
			System.out.println("WRONG LABEL TOO LOW " + a);

		} else if (a != 0) {
			System.out.println("LABEL NOT ZERO " + a);
		}

		// java.util.Random ran = new Random();
		// int t = ran.nextInt(100);

		// if (counter == 1 || t == 7) {
			byte[] data = lsp.data;

			System.out.println("LABEL " + data[0] + ",PADDING " + data[1]);
			String s = "";
			// for (int i = 2; i < data.length; i++) {
			for (int i = 2; i < 101; i++) {
				s += data[i] + " ";
				if (i % 100 == 0) {
					System.out.println(s);
					s = "";
				}

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
