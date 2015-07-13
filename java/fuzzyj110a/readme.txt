Modified September 5, 2006
------------------------

The contents of the fuzzyJ110a.zip file are:

fuzzyJ110a.jar - the FuzzyJ Toolkit classes (version 1.10a)

FuzzyJDocs110a.zip - FuzzyJ 1.10a User Guide

FuzzyShowerJess.clp - the Jess program used with the 
           FuzzyShowerJess example
symbeans.jar - needed for the FuzzyShower example (from Symantec 
           Visual Cafe version 4.x  -- very old)
sfc.jar    - may no longer be needed (also from Symantec)
FuzzyCompiler - source files in this directory for the Fuzzy Compiler
           example
FuzzyShower - source and support files in this directory for the 
           Fuzzy Shower example (no Jess code)
FuzzyShowerJess - source and support files in this directory for the 
           Fuzzy Shower example (with Jess code)
FuzzyTruckSwing - source and support files in this directory for the 
           Fuzzy Truck example -- this is an applet -- however, there 
           is a main method, so it can be run as an application also.
	   Requires the JFC/Swing classes.
FuzzyPendulum - source and support files in this directory for the
           FuzzyPendulum example -- can be run as an applet or an
           application. The file FuzzyPendulumUtil.jar contains some
           classes necessary to run the example (for the graphs).
           Requires the JFC/Swing classes. 
simpleRule - source file for the example program shown in the FuzzyJ 
           User Guide
           
The FuzzyJess extensions require that Jess version 7.x or later be used.
Also Jess 7.x must be compiled with Java 2 (i.e. 1.2.x or later) and
the FuzzyJ Toolkit and FuzzyJess packages have been compiled with 
Java 2 (1.3.x or later).

The files should be extracted to an appropriate place. The CLASSPATH 
variable must include the FuzzyJ Toolkit classes jar file. All of the
java files for the demos that are to be run should be compiled.

To compile the demos use commands like:

   All demos
        javac -classpath %classpath%;...   examples\fuzzyshower\*.java

To run the demos use commands like:

   FuzzyCompiler demo 
	java -classpath %classpath%;... examples.fuzzycompiler.Frame1

   FuzzyShower demo
	java -classpath %classpath%;... examples.fuzzyshower.ShowerFrame

   FuzzyShowerJess demo
	java -classpath %classpath%;... examples.fuzzyshowerjess.ShowerFrameJess

   FuzzyTruck demo
	java -classpath %classpath%;... examples.fuzzytruckswing.FuzzyTruckJApplet

   FuzzyPendulum demo
	java -classpath %classpath%;... examples.fuzzypendulum.FuzzyPendulumJApplet


Explicit examples of compiling and running the FuzzyTruck demo (adjust
the paths to suit the directory locations of the java jdk, swing files
etc.):

1. to compile

   c:\jdk1.3\bin\javac -classpath c:\FuzzyJ\fuzzyJ110a.jar;
                                  c:\FuzzyJ\sfc.jar 
                                  examples/fuzzytruckswing/*.java

2. to run the demo program

   c:\jdk1.3\jre\bin\java -classpath c:\FuzzyJ\fuzzyJ110a.jar;
                                     c:\FuzzyJ\sfc.jar 
                                     examples.fuzzytruckswing.FuzzyTruckJApplet

Note that the above commands are single line commands (formatting is for 
display purposes only) like the following:

c:\jdk1.3\jre\bin\java -classpath c:\FuzzyJ\fuzzyJ110a.jar;c:\FuzzyJ\sfc.jar examples.fuzzytruckswing.FuzzyTruckJApplet

If the jar files and zip files and paths are in an environment 
variable such as CLASSPATH and your PATH variable points to the
location of the java and javac programs you can shorten the 
commands to look like:

    java -classpath %CLASSPATH%;. FuzzyTruckSwing




