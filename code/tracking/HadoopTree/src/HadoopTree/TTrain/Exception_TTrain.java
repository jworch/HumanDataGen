/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package HadoopTree.TTrain;

import org.apache.hadoop.fs.Path;


public class Exception_TTrain extends Exception {
    public Exception_TTrain(Path nodePath, String message) {
        super("(E) at node "+nodePath.toString() + ": \n"+message);
    }
}
