/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredUtils;





public interface JobResultListener {
     
    public void stateChanged(JobResultManager.JobStatus status)
            throws java.io.IOException, InterruptedException, ClassNotFoundException, JobResultException;

}