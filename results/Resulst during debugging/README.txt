
THESE ARE SOME INTERMEDIATE RESULTS DURING DEBUGGING THE PROGRAM WHEN TRYING TO RUN THE PROGRAM ON THE CLUSTER AT KUL.
THESE RESULTS ARE OF LITTLE IMPORTANCE WHEN THE PROGRAM IS RUNNING CORRECTLY, BUT I JUST DID NOT YET DELETED THESE FILES.

File: job_history_count_wrong_labels01.html
This file contains the history of a job that reads all the LabeledSplitPointBuffers (LSP) from the input data and checks whether the LSP.data array has a valid value for the label. The value for the label is the first element in the array (so data[0]) and the value must be in the range [0,31].

In this file especially the counters NUMBER_LSPS and WRONG_LABELS are important. The first one indicates how many LSP's are read from the input (6,000,000) and the second one indicates how many of them are invalid (50,795). So rought 0.1% of the input data is invalid, and now I will try to run the program by discarding this invalid input.

SOLVED: this problem is now solved by using an old splitBuffs.dat file that contains correct input data.
BUT: there is thus a problem when generating that file with the runBVDHtoRGDB.sh script, this problem still needs to be solved.

File: task_output01.html
This file contains the output of a task. The data of one per 100 LSP's is printed, first the LABEL and the PADDING (at data[1]) and then the first 100 elements of the data array.
The strange thing is that many tasks (almost all of them) show the same output. The LABEL often is 0, the PADDING 57 and the contents of the data array 26.

File: reduce_output_names.html
In this file you can see that it looks like the reducer places the samples that are true in files with an odd number and files and the samples that are false in files with an even number. The java program depends on this structure. It is, however, not clear to me why this works. I thought the Key (0 or 1) that belongs to a sample decides whether that sample is true or false..

SOLVED: probably the filename 'part-r-000xx' gets the number of the reducer that is creating that file. Probably the first reducer (with number 0) gets the the records (from the map output) with key=0 and the second reducer (with number 1) gets the records with key=1 and so on. The program depends on this assumption and it seems to be correct. The class MainReadRedOutput can be used to verify this assumption. And in the map() method in the SplitSamples_sp class is written in comment some explanation how the correct reducer gets the correct records


