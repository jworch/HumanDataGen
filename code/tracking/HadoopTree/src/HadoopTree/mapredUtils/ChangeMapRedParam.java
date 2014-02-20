/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.mapredUtils;



import org.apache.hadoop.conf.Configuration;

public class ChangeMapRedParam {

    public static void changeParam(Configuration conf, String name, int newval) {
        int oldVal = conf.getInt(name, -1);
        conf.setInt(name, newval);
        System.out.println(" ## param change: " + name
                + " from " + Integer.toString(oldVal)
                + " to " +  Integer.toString(newval));
    }

    public static void changeParam(Configuration conf, String name, boolean newval) {
        boolean oldVal = conf.getBoolean(name, false);
        conf.setBoolean(name, newval);
        System.out.println(" ## param change: " + name
                + " from " + Boolean.toString(oldVal)
                + " to " +  Boolean.toString(newval));
    }

    public static void changeParam(Configuration conf, String name, String newval) {
        String oldVal = conf.get(name);
        conf.set(name, newval);
        System.out.println(" ## param change: " + name
                + " from " + oldVal
                + " to " +  newval);
    }

    
    
}
