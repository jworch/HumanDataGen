package HadoopTree;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.net.URI;



public class AttribLoc {
    int u1,v1,u2,v2;
    
    public AttribLoc(int u1_, int v1_, int u2_, int v2_) {
        u1 = u1_;
        v1 = v1_;
        u2 = u2_;
        v2 = v2_;
    }



    public static AttribLoc[] loadAttribLocs(String filename)
        throws FileNotFoundException {
    	
    	FileInputStream fin = new FileInputStream(filename);
        
        Scanner scan = new Scanner(fin);
        // read the threshold count
        int numAttribs = scan.nextInt();
        System.out.println("LOADING THRESH, numattribs: "+ numAttribs);
        assert( numAttribs == DataTraits.NUMATTRIBS);
        
        // alloc
        AttribLoc[] attribLocs  = new AttribLoc[numAttribs];
        // fill thresh buff
        for(int ai=0;ai<numAttribs;++ai) {
            int u1_ = scan.nextInt();
            int v1_ = scan.nextInt();
            int u2_ = scan.nextInt();
            int v2_ = scan.nextInt();
            attribLocs[ai] = new AttribLoc(u1_,v1_,u2_,v2_);
        }
       
        return attribLocs;
    }
}
