package HadoopTree.apps;


import org.apache.hadoop.util.ProgramDriver;


public class Driver {
 public static void main(String[] argv) throws Exception {

    int exitCode = -1;
    ProgramDriver pgd = new ProgramDriver();
    try {
      pgd.addClass("upload", MainUpload.class,
                   "A program that uploads a .feat file to HDFS as a proper sequence file.");
      pgd.addClass("upload_sp", MainUpload_sp.class,
                   "A program that uploads a .feat file to HDFS as a proper sequence file."); // isn't it a .dat file??
      pgd.addClass("ConvertFeatToSplit", MainConvertFeatToSplit.class,
                   "converting a feature file into a splitPointBuff file");
      pgd.addClass("countLabels", MainCountLabels.class,
                   "Counting label occurences in a sequence file");
      pgd.addClass("findBestSplit", MainFindBestSplit.class,
                   "finding the best Split in a sequence File");
      pgd.addClass("splitSamples", MainSplitSamples.class,
                   "splitting a seq file in two files");
      pgd.addClass("trainTree", MainTrainTree.class,
                   "training a tree recursively");
      
      pgd.addClass("readlsp", MainReadLsp.class,
              "read contents of the LabeledSplitPointBuffer");
      pgd.addClass("readred", MainReadRedOutput.class,
              "read contents of the SplitSample reducer");
      pgd.addClass("uploadreduced_sp", MainUploadReducedData_sp.class,
              "read contents of the SplitSample reducer");
      
      
      // RUN THE THING
      pgd.driver(argv);
      // Success
      exitCode = 0;
    }
    catch(Throwable e){
      e.printStackTrace();
    }

    System.exit(exitCode); 
 }
}

/*
 // #######################
    // Upload a file to the HDFS with proper sequencing
    if( argv[0].equals("upload")) {
        String[] arguments = Arrays.copyOfRange(argv, 1, argv.length);
        int returnCode = ToolUpload.run(arguments);
        System.exit(returnCode);
    }


    // #######################
    // Train the tree
    else if( argv[0].equals("train")) {
    String[] arguments = Arrays.copyOfRange(argv, 1, argv.length);
    int returnCode = ToolTrain.run(arguments);
    System.exit(returnCode);
    }


    // #######################
    // Crash in flames
    else System.out.println("(E) unknown argument " + argv[0]);
    System.exit(0);
  */
 
