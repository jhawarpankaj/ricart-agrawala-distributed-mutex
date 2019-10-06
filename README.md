# ricart-agrawala-distributed-mutex
Mutual Exclusion in a distributed system using Ricart Agrawala algorithm with the **Roucairol and Carvalho** optimization.

###Pre-Requisite
1. JAVA 8 or later.
2. Maven

###Steps to build
Go to the root of the project and execute

_mvn clean install -DskipTests_

###Execute the jar
The jar should be executed on all the clients and server as defined in the applicationConfig.json file. Within 60 seconds of time the jar should be executed on all the clients.
In the below command the log.file should be different for each client and server.

**Command**:

_java -jar -Dlog.file=1.txt -Dmutex.config=<path_to_application_config_file> mutex-0.0.1-SNAPSHOT.jar_

_java -jar -Dlog.file=2.txt -Dmutex.config=<path_to_application_config_file> mutex-0.0.1-SNAPSHOT.jar_

_java -jar -Dlog.file=3.txt -Dmutex.config=<path_to_application_config_file> mutex-0.0.1-SNAPSHOT.jar_

...

###Verification
All copies of the file should be updated. 

To check each file go the directory:


_ 0/file1.txt _

_ 1/file2.txt _

_ ... _

To verify logs:

_ logfile/1.txt _

_ logfile/2.txt _

...



