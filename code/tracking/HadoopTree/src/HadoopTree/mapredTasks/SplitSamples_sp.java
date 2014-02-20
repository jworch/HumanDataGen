/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredTasks;

import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.mapredUtils.ChangeMapRedParam;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;

import org.apache.hadoop.io.LongWritable;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.util.ArrayList;
import java.io.IOException;
/**
 *
 * @author ccagniart
 */
public class SplitSamples_sp {

    
public static class Map
    extends Mapper<LongWritable, LabeledSplitPointBuff,
                   LongWritable, LabeledSplitPointBuff> {
    int attribId;
    int thresholdId;
    int numReduces_halved;
    Counter counter;
    LongWritable KeyWritable;

    @Override
    public void setup(Context context)
            throws IOException, InterruptedException {
        // open the file
        Configuration conf  = context.getConfiguration();
        
        String path = conf.get("myparams.path", "no path found");
        
        // this is to check whether there are still SplitSamples_sp
        // jobs performed on nodes a level above the leaf nodes
        System.out.println("Splitting path: "+path);
        
        // we want this to crash in flames if it fails.
        attribId = conf.getInt("myparams.attribId", -1);
        thresholdId = conf.getInt("myparams.threshId", java.lang.Integer.MAX_VALUE);
        // these to parameters are determined by a FindBestSplit job
        

        if( attribId == -1 || thresholdId == java.lang.Integer.MAX_VALUE )
            throw ( new IOException("could not get the config properly") );
        
        numReduces_halved = context.getNumReduceTasks() / 2;
        // the number of reduce tasks is set in the setup_SplitSamples job
        // in the TreeTrainer_sp class
        if(numReduces_halved == 0 ) numReduces_halved = 1;
        // numReduces_halved is used in the map() method, see there for further explanation
        
        counter = new Counter("mycount", "mycount", 0);
        KeyWritable = new LongWritable(0);
    }

    @Override
    public void map( LongWritable Key, LabeledSplitPointBuff lsp, Context context)
        throws IOException, InterruptedException {
            int splitPointId = lsp.getSplitPointId(attribId);
            //careful this is a deprecated mapred enum
            //Counter counter = context.getCounter(org.apache.hadoop.mapred.Task.Counter.MAP_INPUT_RECORDS);
            
            long cval = counter.getValue() % numReduces_halved;
            // cval will get a value in the rance [0, numReduced_halved-1 ]
            
            counter.increment(1);

            // The value of cval will be doubled (and in the second case be 
            // incremented by 1 afterwards)
            // In this way the KeyWritable gets a value in the range of
            // [0, 2*(numReduced_halved-1)+1] = [0, numReducetasks-1]	(because numReduced_halved=getNumReduceTasks()/2
            // And so every reducer will get records with the same key
            if (splitPointId <= thresholdId) KeyWritable.set(2*cval);
            else                             KeyWritable.set(2*cval+1);

            context.write(KeyWritable, lsp);
    }
}




public static class Reduce
            extends Reducer<LongWritable, LabeledSplitPointBuff,
                            LongWritable, LabeledSplitPointBuff > {
    @Override
    public void reduce(LongWritable bla, Iterable<LabeledSplitPointBuff> values, Context context )
        throws IOException, InterruptedException {
        LongWritable key = new LongWritable(bla.get() %2);
        for( LabeledSplitPointBuff val : values ) context.write( key, val );
    }
}









// the nodePath as argument is actually only used to be printed
//this is to check which node is being split by each job
public static Job createJob(Cluster cluster,
            Configuration originalconf,
            int attribId, int threshId, Path nodePath )
        throws IOException, InterruptedException, ClassNotFoundException
{
    Configuration conf = new Configuration(originalconf);
    conf.setInt("myparams.attribId", attribId);
    conf.setInt("myparams.threshId", threshId);
    conf.set("myparams.path", nodePath.toString());

    ChangeMapRedParam.changeParam(conf, "mapreduce.tasktracker.map.tasks.maximum", 8);
    ChangeMapRedParam.changeParam(conf, "mapreduce.task.io.sort.mb", 256);
    
    // give the mapred task 512MB of memory
    ChangeMapRedParam.changeParam(conf, "mapred.child.java.opts", "-Xmx512m" );
    ChangeMapRedParam.changeParam(conf, "mapreduce.task.timeout", 3600000); //upgrade from 10min to 1h

    // #################
    // create the job
    Job job = Job.getInstance(cluster, conf);
    job.setJobName("SplitSamples_sp");
    job.setJarByClass(SplitSamples_sp.class);

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
    job.setReducerClass(Reduce.class);

    // #################
    // output
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(LabeledSplitPointBuff.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);


    return job;
}

/**
 * get a list of the files in the results of the reduce operations that contain
 * the samples that tested false (Resp.true)
 */
public static Path[] getSplits(FileSystem fs, Path splitDir, boolean side)
    throws IOException
{
    FileStatus[] contents = fs.listStatus(splitDir);
    
    //System.out.println("NUMBER OF FILES IN THE SPLITDIR: "+contents.length+" in side "+side);
    
    ArrayList<Path> args = new ArrayList<Path>();

    int sideID = 0;
    if(side == true ) sideID = 1;

    for (int i = 0; i < contents.length; i++) {
        if( contents[i].isFile() ) {
        	//System.out.println(contents[i].getPath().toString()+ " is  a file");
        	// TODO try catch outside for loop (rule 49)
        	// but will this be a long for loop? 
        	// contents.lenght will not be that large I guess
            try {
            Path path = contents[i].getPath();
            String[] x = path.toString().split("-");
            // because the output file has the form part-r-00005
            // the number of that file name decides whether the samples in that file are true or false.
            // even numbers contain false samples, odd numbers true samples.
            // I don't know exactly why, but it seems that the Reducer of SplitSamples_sp saves its output
            // in a file with name 'part-r-000xx' and the number of that name is equal
            String trailDigits = x[x.length-1];
            int u = Integer.parseInt(trailDigits);
            if(u%2 == sideID) args.add(path);
            } catch(NumberFormatException e){}
        }
        else {
        	System.out.println(contents[i].getPath().toString()+ " is not a file");
        }
    }

    Path[] aargs = new Path[args.size()];
    args.toArray(aargs);

    return aargs;
}


/**
 * move the files in the result of the reduce operations to the dir_false and
 * dir_true directories.
 */
public static void moveSplits(FileSystem fs, Path splitDir,
        Path dir_false, Path dir_true)
    throws IOException
{
	// this method is calle by the callback_SplitSamples_done() method in TreeTrainer_sp
	// that method passes the following arguments
	// splitDir will have the form: rootpath/0/0/1/0/temp
	// dir_false will have the form: rootpath/0/0/1/0/0/splitbuffs.seq
	// dir_true will have the form: rootpath/0/0/1/0/1/splitbuffs.seq
	// notice that splitbuffs.seq is indeed a directory which is a bit strange
	// considering it has the .seq extension, but the program works so I leave it like that
	
    fs.mkdirs(dir_false);
    fs.mkdirs(dir_true);

    Path[] files_false = getSplits(fs, splitDir, false);
    Path[] files_true  = getSplits(fs, splitDir, true);

    for(int fi=0;fi<files_false.length;++fi) {
        Path file_old = files_false[fi];
        Path file_new = new Path( dir_false.toString() + "/"
                +Integer.toString(fi)+".seq");
        fs.rename(file_old, file_new);
    }

    for(int fi=0;fi<files_true.length;++fi) {
        Path file_old = files_true[fi];
        Path file_new = new Path( dir_true.toString() + "/"
                +Integer.toString(fi)+".seq");
        fs.rename(file_old, file_new);
    }
}

}
