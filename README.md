# project-1-project-1-team-9

## Background
This project aims to create an optimal schedule given an acyclic and directed graph consisting of nodes and edges which represents tasks and the order in which each task needs to be done respectively.

Our solution produces an optimal schedule of tasks using DFS branch and bound as well as the A* algorithm. It also has parallelization and visualization implemented as well.

## How to Run the Project

To run the jar, use the following command:
```
java -jar scheduler.jar INPUT.dot P [OPTION]
```

Required Arguments
- `[INPUT.dot]` - A task graph with integer weights in dot format
- `[P]` - The number of processors

Optional Arguments

- `-o OUTPUT` - Output file is named OUTPUT (default is INPUT-output.dot)
- `-v` - Visualizes the search
- `-p N` - use N cores for execution in parallel (default is sequential). Only uses dfs.
- `-a` - uses the A* algorithm (default is dfs branch and bound). Note that this is only for sequential.  

## Required Enviroment

- Java 8 openjdk version 1.8.0_302 
- Windows or Linux OS

## Team Members

Jiaqi Li 

Jimmy Wang

Jinkai Zhang

Lance Delos Reyes

Samuel Liu

## AcknowLedgements
* [GraphStream](http://graphstream-project.org/)
* [Commons CLI](https://commons.apache.org/proper/commons-cli/)
* [JUnit 4](https://junit.org/junit4/)
* [JitPack](https://jitpack.io/)
