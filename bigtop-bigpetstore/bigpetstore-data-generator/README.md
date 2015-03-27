BigPetStore Data Generator
==========================

BigPetStore ...

Data Generator ...

=======
Building and Testing
--------------------
We use the Gradle build system for the BPS data generator so you'll need
to install Gradle on your system.
Once that's done, you can use gradle to run the included unit tests
and build the data generator jar.

To build:
    
    $ gradle build

This will create several directories and a jar located at:
    
    build/libs/bigpetstore-data-generator-0.9.0-SNAPSHOT.jar

Building automatically runs the included unit tests.  If you would prefer
to just run the unit tests, you can do so by:

    $ gradle test


To clean up the build files, run:

    $ gradle clean


Running the Data Generator
--------------------------
The data generator can be used as a library (for incorporating in
Hadoop or Spark applications) or using a command-line interface.
The data generator CLI requires several parameters.  To get 
descriptions:

    $ java -jar build/libs/bigpetstore-data-generator-0.9.0-SNAPSHOT.jar

Here is an example for generating 10 stores, 1000 customers, 100 purchasing models,
and a year of transactions:

    $ java -jar build/libs/bigpetstore-data-generator-0.9.0-SNAPSHOT.jar generatedData/ 10 1000 100 365.0


Groovy Drivers for Scripting
----------------------------
Several Groovy example script drivers are included in the `groovy_example_drivers` directory.
Groovy scripts can be used to easily call and interact with classes in the data generator
jar without having to create separate Java projects or worry about compilation.  I've found
them to be very useful for interactive exploration and validating my implementations
when unit tests alone aren't sufficient.

To use Groovy scripts, you will need to have Groovy installed on your system.  Build the 
data generator as instructed above.  Then run the scripts in the `groovy_example_drivers`
directory as so:

    $ groovy -classpath ../build/libs/bigpetstore-data-generator-0.9.0-SNAPSHOT.jar MonteCarloExponentialSamplingExample.groovy

