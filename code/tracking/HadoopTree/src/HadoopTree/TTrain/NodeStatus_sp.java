package HadoopTree.TTrain;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;
import java.util.Scanner;

public class NodeStatus_sp {

    public enum Status {
        TO_SPLIT,
        TO_SPLIT_AS_SUBTREE,
        TO_SPLIT_AS_SUBTREE_WITH_NEW_DEPTH,
        TO_WRITE_AS_EMPTY,
        SPLIT,
        SPLIT_AS_SUBTREE,
        EMPTY
    }

    // ###############################
    // ###############################
    // nodequery operation
    // ###############################
    // ###############################
    static Status getNodeStatus(FileSystem fs, Path rootPath, int nid, int maxDepth )
        throws IOException, Exception_TTrain {
        Path nodePath = buildNodePath(rootPath, nid);
        // nodePath is in the form: rootPath/0/1/1/0
        
        int nodeDepth = buildNodeDepth(nid);

        Path seqFilePath   = FILE_Seq(nodePath);
        Path splitInfoPath = FILE_Info(nodePath);
        Path EmptyFlag     = FILE_EmptyFlag(nodePath);


        // Empty node ( no samples in there )
        if( fs.exists(EmptyFlag) )                   return Status.EMPTY;
        // Split Node
        else if( fs.exists(splitInfoPath) )          return Status.SPLIT;

        // check if this is too long to be in mem
        long fileSize  = NodeStatus_sp.getInputLength(fs, seqFilePath);
        //System.out.println(nodePath.toString() + "has input of lenth " + fileSize/1024 +"KB" );
        if(fileSize == 0)                            return Status.TO_WRITE_AS_EMPTY;
        
        // TODO try the training with a higher block size to see whether it can improve the efficiency
        if( fileSize > fs.getDefaultBlockSize() )    return Status.TO_SPLIT;
        
        // check if this is already correctly split in mem
        int[] subTreeFiles = getSubTreeFiles(fs, nodePath);
        if( subTreeFiles.length == 0 )               return Status.TO_SPLIT_AS_SUBTREE;
        if( subTreeFiles.length == 1
         && subTreeFiles[0] == maxDepth - nodeDepth) return Status.SPLIT_AS_SUBTREE;
        else                                         return Status.TO_SPLIT_AS_SUBTREE_WITH_NEW_DEPTH;

    }




    // ###############################
    // ###############################
    // Path operations ( playing with path names
    // ###############################
    // ###############################
    static Path FILE_Seq(Path nodePath)       { return new Path(nodePath.toString() + "/splitbuffs.seq"); }
    static Path DIR_bestSplit(Path nodePath)  { return new Path(nodePath.toString() + "/bestSplit"); }
    static Path FILE_Info(Path nodePath)      { return new Path(nodePath.toString() + "/splitInfo.seq"); }
    static Path FILE_EmptyFlag(Path nodePath) { return new Path(nodePath.toString() + "/EMPTY"); }
    static Path DIR_Temp(Path nodePath)       { return new Path(nodePath.toString() + "/temp"); }
    static Path DIR_false(Path nodePath)      { return new Path(nodePath.toString() + "/0"); }
    static Path DIR_true(Path nodePath)       { return new Path(nodePath.toString() + "/1"); }
    static Path FILE_false(Path nodePath)     { return new Path(nodePath.toString() + "/0/splitbuffs.seq"); }
    static Path FILE_true(Path nodePath)      { return new Path(nodePath.toString() + "/1/splitbuffs.seq"); }

    // the path of a subtree file that indicates its depth in its name
    static Path FILE_SubTree(Path nodePath, int depth)  { return new Path(nodePath.toString()
                                                                 + "/subtree_"
                                                                 + Integer.toString(depth)
                                                                 +".seq"); }

    /**
     * 
     * @param nodeId
     * @return depth	2^depth - 1 <= nodeId < 2^(depth-1) - 1
     */
    // roughly log_2(nodId), truncated after the comma
    static int  buildNodeDepth(int nodeId) {
        int depth = 0;
        { // awfull, but i prefer not risking float imprecision with Math.pow
            int bla = nodeId;
            while(bla>0) { bla=(bla-1)/2;/*parent address*/ depth++;}
        }
        return depth;
        
        // TODO could this be more efficient?
        // double i = Math.log10(nodeId+1)/Math.log10(2)
        // and then truncate i after comma for the depth
    }

    static Path buildNodePath(Path rootPath, int nodeId ) {
    	// depth not used
        //int depth = buildNodeDepth(nodeId);
        String s = "";
        int node = nodeId;
        while(node > 0) {
            if (node%2 == 0) s = "/1"+ s; // we are the 2x+2 node
            else             s = "/0"+ s; // we are the 2x+1 node
            node = (node-1)/2; // we are now our parent
        }
        return new Path(rootPath.toString() + s);
    }







    // ###############################
    // ###############################
    // FileSystem operations ( that actually look at the file)
    // ###############################
    // ###############################

    /**
     * @return -1 if there is no subtree file in there, or if two subtree files are in there
     *         the depth of the subtree if there is
     */
    static int[] getSubTreeFiles(FileSystem fs, Path nodePath)
            throws Exception_TTrain, IOException{
//    	System.out.println("LOOKING IN NODEPATH: "+nodePath.toString());

    	//System.out.println("CHECK THE FILES IN THIS FOLDER");
//    	FileStatus[] files = fs.listStatus(nodePath);
//    	for(int i=0;i<files.length;++i) {
//    		System.out.println("NAME "+ files[i].getPath().toString());
//    	}
//    	if(files.length==0)
//    		System.out.println("NO FILES IN THIS NODEPATH");
    	
    	
    	
    	
        //this needs a * (and not a + sign)
    	Path pathPattern = new Path(nodePath.toString() + "/subtree_[0-9]*.seq");
//    	System.out.println(pathPattern);
        FileStatus[] matches = fs.globStatus(pathPattern);

        int[] res = new int[matches.length];
//        if(matches.length==0)
//        	System.out.println("NO MATCHES IN: "+nodePath);

        for(int i=0;i<matches.length;++i) {
            // parse the filename for a number
        	// this gets the last number before the ".seq"
        	//	/mydir/subtree_5.seq  gives 5
//        	/mydir/subtree_15.seq  gives 15
        	
            // do it in several time cause I m too lazy to learn regex
        	// TODO regex more efficient?
            Scanner scanner = new Scanner(matches[0].getPath().toString());
            
            //   \\.seq to indicate that we want to find a dot
            //   in regex a dot (without backslash) means that any character may be on the position of the dot
            //   --> e.g. in my case I had the path: correctseqfile/0/1/0/0/0/0/subtree_14.seq
            //   and the String numAsStringSeq was: tseq   (and that is unwanted of course) 
            //String numAsStringSeq = scanner.findInLine("[0-9]*.seq");    // this may give errors
            String numAsStringSeq = scanner.findInLine("[0-9]*\\.seq");
            scanner = new Scanner(numAsStringSeq);
            //String numAsString = scanner.findInLine("[0-9]*");  //the * seems not to work in a test
            String numAsString = scanner.findInLine("[0-9]+"); // replace with + sign
//            System.out.println("NUMASSTRINGSEQ: " + numAsStringSeq + "\n"+ 
//            		"GETSUBTREEFILES FOUND: "+ numAsString);
            res[i] = Integer.parseInt(numAsString);
        }

        return res;
    }

    /**
     * @param a sequenceFile or a directory containing sequence files
     * @return the length of the file if its a file, or the added depth of
     * enclosed files if its a directory
     */
    static long getInputLength(FileSystem fs, Path seqFile)
        throws IOException
    {
        if( fs.isDirectory(seqFile) ) {
        	//System.out.println(seqFile.toString() + " IS NOW A FOLDER");
            long res = 0;
            FileStatus[] status = fs.globStatus(new Path ( seqFile.toString()+"/*.seq"));
            for( FileStatus s : status) res += s.getLen();
            // Kind of a HACK... in case these are the result of a map
            status = fs.globStatus(new Path ( seqFile.toString()+"/part-m-*"));
            for( FileStatus s : status) res += s.getLen();
            return res;
        }
        else {
            return fs.getFileStatus(seqFile).getLen();
        }
    }




}

