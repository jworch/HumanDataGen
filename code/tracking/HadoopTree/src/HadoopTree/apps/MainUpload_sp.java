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
import HadoopTree.LabeledSplitPointBuff;


/**
 * This will read a file from the local file system
 * Then rewrite it as a sequencefile to hdfs
 */
public class MainUpload_sp 
        extends Configured
        implements Tool
{


    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new MainUpload_sp(), args);
        System.exit(res);
    }

    public int run(String[] args) throws Exception {

        System.out.println("UPLOADING Program");

        // -------------------
        // parse the arguments
        if( args.length != 2 ) {
            System.err.println("usage: hadoop xxx.jar upload_sp <localfilein.dat> <hdfsfileout.seq>");
            return -1;
        }
        String filename_in  = args[0];
        String filename_out = args[1];


        // -------------------
        // We implement configured which takes care of this
        Configuration conf = getConf();

        // -------------------
        // The input file, and a bufferedreader to read significant chunks
        // at a time
        FileInputStream fin = new FileInputStream(filename_in);
        BufferedInputStream bfin = new BufferedInputStream(fin,  8192); 

        // -------------------
        // the input file system is local
        conf.set("fs.file.impl","org.apache.hadoop.fs.LocalFileSystem");
        FileSystem fs = FileSystem.get(conf);
        if( fs.getConf() == null) {
            System.err.println("Could not initialize the local filesystem");
            return -1;
        }

        // -------------------
        // the sequence writer on the output file
        java.io.File fileout = new java.io.File(filename_out);
        //Path path_out = new Path(fileout.getAbsolutePath());
        Path path_out = new Path(fileout.getPath());
        SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
                path_out,
                LongWritable.class,
                LabeledSplitPointBuff.class,
                SequenceFile.CompressionType.NONE);


        // -------------------
        // iterate through the input file and write to output
        long numRecords;

        try {
            numRecords = 0;
            // TODO use a NullWritable as key to save bandwidth
            LongWritable Key          = new LongWritable(0);
            LabeledSplitPointBuff lsp = new LabeledSplitPointBuff();
            while(true) {
                // data from input file is read and written into lsp.data
                int read = bfin.read(lsp.data, 0, LabeledSplitPointBuff.RECORDSIZE);
                if (read != LabeledSplitPointBuff.RECORDSIZE) break;
                writer.append(Key, lsp);
                numRecords += 1;
            }
        } finally {
            writer.close();
        }

        System.out.println("sucessfully (or) read " + Long.toString(numRecords)
                    + " from " + filename_in );


        return 0;
    } 

}
