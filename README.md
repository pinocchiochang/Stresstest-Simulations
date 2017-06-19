Stress Tester
=========================
This is my summer internship project in Medialets. It utilizes Gatling, Medialets' REST interface, a parser combinator, Json4s, and functional programming to simulate a wide variety of customizable stress tests and log all failed requests on a server (https://github.com/pinocchiochang/Error-Logger).

To run a test, the user first customizes the test scenario to create his/her choice of virtual users who can do any combination of 11 actions. During the simulation, the stress tester will automate the specified actions of these virtual users and perform them on Medialets' platform. The stress tester then generates a report of the simulation (which includes information about the request response times, request failure rate, and other relevant stress statistics) and finally logs the simulation's failed requests on a server.

Certain information had to be withheld from this repo as it is public. Specifically, the internal URLs of Medialets' platform, login information, commit history, and branch/pull request history have been omitted.

Quickstart Guide
=========================

Get the project
---------------

```bash
git clone https://github.com/pinocchiochang/Stresstest-Simulations.git
```

Edit Config Files
-----------------
Edit application-template.conf (in src/test/resources) and userConfig.txt (in the main directory) to customize the test simulation. Then, rename application-template.conf to application.conf.

Run
---------------
* Open terminal and run the "run.sh" script to run the main simulation, or the "runWithLogging.sh" script to both run the main simulation and another simulation which logs all failed requests on an error-logging server (see below). These scripts are located in the bin folder. (If you are receiving a "permission denied" error for a run script, then run the command "chmod +x [run script path]" and try running it again).

  
Check Failed Requests
-------------
Here is the link to an error logging server made to log failed requests from Gatling simulations: https://github.com/pinocchiochang/Error-Logger. It can be used to log errors from this stress tester's simulations. To do this, first make sure the server's "adminKey" field in its application.conf file matches the "errorLoggingKey" field in this stress tester's application.conf file. Then, run the server and make sure the URL it's being run on matches the "errorLoggingURL" field of this stress tester's application.conf file. After this, you can run any simulation on this stress tester with error logging.
