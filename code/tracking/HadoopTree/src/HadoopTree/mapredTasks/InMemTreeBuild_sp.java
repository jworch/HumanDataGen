package HadoopTree.mapredTasks;

import HadoopTree.LabeledSplitPointBuff;
import HadoopTree.SplitPointAccum;
import HadoopTree.Tree;
import HadoopTree.TreeBuilder;

import HadoopTree.mapredUtils.ChangeMapRedParam;
import HadoopTree.mapredUtils.ReadMapRedResult;


import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

//import com.javamex.classmexer.MemoryUtil;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;



public class InMemTreeBuild_sp {



public static class Map
    extends Mapper<LongWritable, LabeledSplitPointBuff, LongWritable, LabeledSplitPointBuff>
{
}

public static class Reduce
        extends Reducer<LongWritable, LabeledSplitPointBuff, IntWritable, Tree>
{
     /// the threshs vector contains the thresholds that we are considering
    int              maxDepth;
    int[]            threshs;

    @Override
    public void setup(Context context ) throws IOException {
        Configuration conf = context.getConfiguration();
        
        String path = conf.get("myparams.path", "no path found");
        System.out.println("Splitting path: "+path);
        // load maxDepth
        maxDepth =  conf.getInt("myparams.maxDepth", -1);
         // load thresh file
        Path filename = new Path(conf.get("myparams.threshfile"));
        FileSystem fs = FileSystem.get(conf);
        if( fs.getConf() == null)
            throw new IOException("Could not initialize the filesystem locally");
        if( !fs.exists(filename))
            throw new IOException("Could not find the thresh file on hdfs");
        threshs            = SplitPointAccum.loadThreshFile(fs, filename);
    }


    @Override
    public void reduce(LongWritable Key, Iterable<LabeledSplitPointBuff> values, Context context )
        throws IOException, InterruptedException {
        ArrayList<LabeledSplitPointBuff> items = new ArrayList<LabeledSplitPointBuff>();
        // -------------------
        // 1 - copy all the input data
        for(LabeledSplitPointBuff lsp: values ) {
            items.add(new LabeledSplitPointBuff(lsp));
        }
        // isn't this overkill? try new Array[values.size]
        // no: values.size() does not exist
        for(LabeledSplitPointBuff lsp: values ) {
            items.add(new LabeledSplitPointBuff(lsp));
        }
        LabeledSplitPointBuff[] itemArray = new LabeledSplitPointBuff[items.size()];
        items.toArray(itemArray);

        // -------------------
        // 1 - build the tree
        Tree tree = TreeBuilder.treeBuild( itemArray,   threshs,  maxDepth );
        
        // write the tree to disk
        
        // this was done because with the wrong input data, the Java heap space was out of memory
        // with the correct input data there wasn't this problem anymore, so this code may be removed
        /*System.out.println("CHECKING MEMORY");
        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        
//        sb.append("CHECK MEM WITH CLASSMEX");
//        long memTree = MemoryUtil.memoryUsageOf(tree);
//        sb.append("memTree in long: " + memTree + "<br/>");
//        sb.append("memTree formatted in KB " + format.format(memTree / 1024) + "<br/>");
//        long memTreeDeep = MemoryUtil.deepMemoryUsageOf(tree);
//        sb.append("memTreeDeep in long: " + memTreeDeep + "<br/>");
//        sb.append("memTreeDeep formatted in KB " + format.format(memTreeDeep / 1024) + "<br/>");
        sb.append("\n\n Runtime memory info\n");
        
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("free memory: " + format.format(freeMemory / 1024) + "<br/>");
        sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
        sb.append("max memory: " + format.format(maxMemory / 1024) + "<br/>");
        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "<br/>");
        		
        
        System.out.println(sb);
        */
        
        // TODO use a NullWritable to save bandwidth
        context.write(new IntWritable(0), tree);
    }
}



//TODO variable rootPath may be deleted
public static Job createJob( Cluster cluster, Configuration baseConf, 
		Path AttribTreshFile, Path rootPath, int maxDepth)
        throws IOException, InterruptedException, ClassNotFoundException
{
    Configuration conf = new Configuration(baseConf);
    ChangeMapRedParam.changeParam(conf, "mapreduce.child.java.opts", "-Xmx2048m -Xms2048m" );
    ChangeMapRedParam.changeParam(conf, "mapreduce.task.timeout", 3600000); //upgrade from 10min to 1h
    Job job = Job.getInstance(cluster, conf);
    job.setJobName("InMem_sp");

    job.setJarByClass(InMemTreeBuild_sp.class);
    job.getConfiguration().set("myparams.threshfile", AttribTreshFile.toString());
    job.getConfiguration().setInt("myparams.maxDepth", maxDepth);
    conf.set("myparams.pathinmem", rootPath.toString());


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
    job.setNumReduceTasks(1);
    // #################
    // output
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(Tree.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);

    return job;
}

public static Tree readResult(Configuration conf, Path outDir,
        int nodeDepth, int maxDepth)
    throws IOException {
    FileSystem fs = FileSystem.get(conf);
    ReadMapRedResult<IntWritable, Tree> printer
            = new ReadMapRedResult<IntWritable, Tree>();
    Path resPath = new Path(outDir + "/part-r-00000");
    Tree res = new Tree(maxDepth-nodeDepth);
    printer.readFirstResult(conf,fs, resPath, new IntWritable(), res);
    return res;
}


}
