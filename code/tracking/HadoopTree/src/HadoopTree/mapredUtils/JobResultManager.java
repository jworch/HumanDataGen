/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredUtils;


import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;


public class JobResultManager {
    
    public enum JobStatus { SUCCESSFUL, FAILED};

    Configuration                 conf;
    FileSystem                    fs;
    Cluster                       cluster;
    Map<JobID, JobResultListener> monitoredJobs;


    

    public JobResultManager(Configuration originalConfiguration)
        throws IOException {
        conf          = originalConfiguration;
        fs            = FileSystem.get(conf); // we need to do this for the configuration
        // to load dfs.block.size properties etc... if not these properties remain undefined
        cluster       = new Cluster(conf);
        monitoredJobs = new HashMap<JobID, JobResultListener>();
    }


    public Cluster       getCluster() { return cluster; }
    public Configuration getConfig()  { return conf; }
    public FileSystem    getFs()      { return fs; }
    
    public void addJob(Job job, JobResultListener listener)
        throws IOException, InterruptedException, ClassNotFoundException  {
        job.submit();
        JobID jid = job.getJobID();
        monitoredJobs.put(jid, listener);
        
    }

    /**
     * @return the number of running jobs
     * @throws IOException
     * @throws InterruptedException
     */
    public void poll()
            throws IOException, InterruptedException, ClassNotFoundException, JobResultException {
        // no idea if remove invalidates iterators so i ll play it safe
        ArrayList<JobID> toUpdate = new ArrayList<JobID>();
        for(JobID jid: monitoredJobs.keySet()) {
            Job j = cluster.getJob(jid);
            assert(j != null);
            if( j.isComplete() ) toUpdate.add(jid);
        }
        // toUpdate contains the completed jobs (they may be successful or not)
        
        for (JobID jid: toUpdate) {
            JobResultListener listener = monitoredJobs.get(jid);
            Job j = cluster.getJob(jid);
            if( j.isSuccessful() ) listener.stateChanged(JobStatus.SUCCESSFUL);
            else                   listener.stateChanged(JobStatus.FAILED);
            monitoredJobs.remove(jid);
        }

        
    }

    public int numJobs() {
        return monitoredJobs.size();
    }



}
