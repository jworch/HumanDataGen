/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredUtils;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;

import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;






public class ReadMapRedResult <K extends Writable, V extends Writable> {


    public void readFirstResult(Configuration conf, FileSystem fs, Path outFile,
            K k_buffer, V v_buffer)
        throws IOException {
            SequenceFile.Reader reader = new SequenceFile.Reader(fs, outFile, conf);
            boolean res = reader.next(k_buffer, v_buffer);
            if( !res ) throw( new IOException("(E) could not read a result from " + outFile.toString()) );
            reader.close();
    }


    public void printResult(Configuration conf, FileSystem fs, Path outDir,
            K k_buffer, V v_buffer)
            throws IOException
    {
        FileStatus contents[] = fs.listStatus(outDir);
        for (int i = 0; i < contents.length; i++) {
            // 0.20.203 >> if (!contents[i].isDir()) {
            if( contents[i].isFile() ) {
                Path path = contents[i].getPath();
                System.err.println(path);
                try {
                    SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf );
                    while(reader.next(k_buffer, v_buffer)) {
                       System.out.println(v_buffer);
                    }
                    reader.close();
                } catch ( Exception e) {}
            }
        }
    }

}
