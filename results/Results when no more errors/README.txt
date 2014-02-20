
This directory contains some result after running the program with the 20GB splitBuffs.dat as input data. 

1.
-------------------------------------------------------------------------------------------------------------------------------------------------
The results of two runs of the training program itself (the output 'tree_xx.txt' files) are still saved on the namenode 'pma-robot-cutting' in the directory: /home/hadoop/localfiles/correctTreeFiles

2.
-------------------------------------------------------------------------------------------------------------------------------------------------
The files 'leaf node with little data.html' and 'leaf node with much data.html' contain the information of two directories after running the training program. Each directory is a leaf node and they have the same parent node. One directory contains the true samples, the other one the false samples.
Notice that one node has very little data and the other one has much data. This seems strange because I thought the goal was to split the data equally in half by finding a BestSplit.
I don't know what has caused this. Maybe the input data of 20GB is to little to perform equal splits, but this is only a guess..

3.
-------------------------------------------------------------------------------------------------------------------------------------------------
The files 'top node data samples true.html' and 'top node data samples false.html' contain the information of the first split that has been done on the input data. 
At the top node it looks like the data is split more or less in half (11 files of 675MB in the true node, and 11 files of 1GB in the false node). 
