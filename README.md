/*
# -----------------------------------------------------------------
#
# Project:  ProvErr: System Level Fault Diagnosis Tool
# File:  README
# Description:  instructions for building and running the tool.
#
# -----------------------------------------------------------------
*/

Table of Contents
=======================
I.	Software Dependencies
II.	Running ProvErr

I. Software Dependencies
========================
 
ProvErr has been built in Eclipse (v4.3 or compatible) IDE with the following software packages on which it has a dependency.  These packages will need to be installed separately:
 
1) Hadoop v1.2.1
	http://hadoop.apache.org/
  
2) xmlbeans-2.6.0
   	http://xmlbeans.apache.org/
        
3) Java Development Kit (JDK) v6 or later
	http://java.sun.com

4) ANT v1.8 or later
	http://ant.apache.org/


II. Running ProvErr
========================

2.1 Information Sources

1) Output from Pig command “pig -e 'explain -script *.pig' &>*.explain.txt”.

2) Output from executing Pig Latin script using command “pig *.pig &>*.out.txt”.

3) Hadoop JobTracker.

2.2 Building Dependency Models

For each Pig run, run java class ‘edu.iu.d2i.proverr.capture.DependencyModelBuilder.java’ to build the dependency model in XML format.

2.3 Diagnosing Faults

Invoke the method ‘diagnose(double)’ in class ‘edu.iu.d2i.proverr.diagnosis.CauseDetermination’. You can start with the sample usage in package ‘edu.iu.d2i.proverr.evaluation’.

1) Output

For each Pig run labeled as a fault, ProvErr output the top cause candidate as well as a list of redundant candidates and co-cause candidates (if any).

2) Performance Evaluation

Directory ‘evaulation/error-code’ contains UDFs with software bugs, and you can build them with command ‘ant’. 

Directory ‘evaluation/Gropu1’ contains a group of Pig runs that have induced software bugs. Run ‘edu.iu.d2i.proverr.evaluation.SoftwareBugEval.java’ to evaluate ProvErr with these software bugs.

Directory ‘evaluation/Gropu2’ contains a group of Pig runs that have real hardware problems (lacking of computing resource in PlanetLab nodes). Run ‘edu.iu.d2i.proverr.evaluation.HardwareProblemEval.java’ to evaluate ProvErr with these hardware problems.

Directory ‘evaluation/Gropu3’ contains a group of Pig runs that have real hardware problems as well as induced software problems. Run ‘edu.iu.d2i.proverr.evaluation.SoftwareWithHardwareProblemEval.java’ to evaluate ProvErr with these hardware problems.



