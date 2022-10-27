# Milestone 1
## Goal

- Read the input file and number of processors and create an output file.
- The output file needs to contain a valid schedule.

## Solution
### I/O Parsing

- Use [GraphStream](http://graphstream-project.org/) to read and write the .dot files.
- Use [Commons CLI](https://commons.apache.org/proper/commons-cli/) to obtain the command line input arguments.
- After reading the .dot file, all the information is decomposed 
  into needed data structure in `GraphData` class.

### Data Structure

- Use Node object directly in most of operations at this stage. 
- Use HashMap<Node, ArrayList<Node>> for the adjacent lists, Queue<Node> for storing 
  the nodes that will be exploded.
- Use an Array to represent the schedule and a ArrayList<int[]> to represent each processor inside it.
  In a processor, int[0] is the start time for a task, int[1] is the finish time for a task.
  The overall is ArrayList<int[]>[]. 

### Algorithm 
- Implement the list scheduling algorithm.
- Read the .dot file and construct the adjacent lists for the graph. 
- Use the adjacent lists to keep finding the node with in-degree of 0 and 
  push it into the execution queue. 
- Update the adjacent list after a node is pushed into the queue. 
- While the queue is not empty, pull out one node from the queue and get the parent nodes of the node.
  Use a nest for loop to find the earlist start time according to the constrains. 
  
 - for(processor:all processors){
     - for(node : all parent nodes){
     - list1.add(starttime)
     - }
     - find Max(list1)
     - add the Max value to list2
 - }
 - find Min(list2)
  
- Find the node with in-degree of 0 and push it into the queue then update the adjacent list.
- Repeat until the queue is empty.

## Testing
  
- The methods in CheckValidity class load and parse the output .dot into a graph object.
- Use a for loop to check each node's start time does not breach any of the constraints. 
- We used [JUnit](https://junit.org/junit4/) as a testing framework.

