# Milestone 2 - DFAlgorithrm
## Goal

- Create an optimal schedule for small input graphs in reasonable time
- The output file needs to contain a optimal schedule.

## Solution

The solution is based on the DFS branch&bound algorithm, which performs an exhaustive search of all possible solutions. The algorithm uses a recursive backtracking approach to establish the entire searching tree and applying several pruning techniques to reduce the search space. 

### Data Structure

- Instead of using Node object to represent the task from Milestone 1, Milestone 2 uses the node index (Integer) to represent each unique task.
- Use Integer array (int[ ]) to store the start time, processor allocation, duration, critical paths for each task. 
- Use Integer array (int[ ]) to store the finish time of each processor
- Use HashMap<Integer, ArrayList<Integer>> for the adjacent lists, LinkedList<Integer> for storing the tasks that will be exploded.
- Use an Array (Task [ ] )to represent the schedule. The Task class store all relevent information about a task. 
  

### Algorithm 
- Run the list scheduling algorithm and get the finish time and load balance as the initial upper bounds for the optimal solution.

- In the DFAlgorithrm, use the finish time and load balance values as the initial upper bounds.

- Update the liveTaskList before invoking the recursive function. The liveTaskList contains all the available tasks. The recursive function takes the liveTaskList as the parameter. 

- In the recursive function, the base case is when there are no tasks in the liveTaskList. It compares the current completed schedule with the previous best schedule. The best schedule will be updated if the current one is more optimal. 

- The recursive process will use a nest FOR-LOOP to explore all the possible solutions. Then it invokes the recursive function again with a deep copy of the most updated liveTaskList.

- All the heuristic bounds and pruning techniques will apply in this process. 

- The backtracking process will restore the previously scheduled task and the processor's finish time. Restore the removing incoming edges of the previously scheduled task. 

     

### Heuristic & Prunning  

- There are total four heuristic bounds, the algorithrm chooses the maximum of them. 

- ```java
  earliestStartTime + perfectLoadBalance >= bestFinishTime 
  ```

- ```java
  earliestStartTimeLoadBalance + partialBalance > loadBalance 
  ```

- ```java
   earliestStartTime + longestCriticalPath >= bestFinishTime
  ```

- ```java
  (dataReadyTime + tasksDuration[task]) > bestFinishTime 
  ```

- Detect the isomorphic processors. A task will not schedule on different empty processor. 



## Testing

- The methods in CheckValidity class load and parse the output .dot into a graph object and check each node's start time does not breach any of the constraints. 

- Check the states that brute force DFS recursive search can reach.

- Check against all the graphs that we already know the optimal solution. 

  



