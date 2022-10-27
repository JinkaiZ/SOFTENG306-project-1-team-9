package Scheduler;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.*;
import java.util.stream.Collectors;

public class DFAlgorithm {

    private GraphData graphData;
    private int numOfProcessors;

    private int[] tasksDuration; // record the duration of each task
    private int[] criticalPaths; // record the critical path to exit for each task
    private int totalTaskDuration = 0; // sum of all task's durations

    private int[] tasksStartTime; // record the start time of each task
    private int[] tasksProcessorTracker; //record the allocated processor of each task
    private int[] processorsFinishTime; //record the finish time of each processor
    private int remainTaskDuration = 0; // sum of unscheduled task's durations

    // only update when find a better completed schedule
    private int bestFinishTime;
    private int loadBalance;
    private int[] tasksEarliestStartTime;
    private int[] tasksEarliestProcessorTracker;

    private long startTime = 0;

    private int stateCount = 0; // the state counter

    public Task[] run(GraphData graphData, int numOfProcessors, int upperBound, int greedyLoadBalance) {
        startTime = System.currentTimeMillis();

        this.graphData = graphData;
        this.numOfProcessors = numOfProcessors;
        int numOfTasks = graphData.getTotalNumberOfNodes();
        this.loadBalance = greedyLoadBalance;
        this.processorsFinishTime = new int[numOfProcessors];
        this.bestFinishTime = upperBound;


        Main.setEndTime("Finish Time: " + bestFinishTime);


        // Track the processor that each task is scheduled on.
        this.tasksStartTime = new int [numOfTasks];
        this.tasksEarliestStartTime = new int[numOfTasks];

        this.tasksProcessorTracker = new int [numOfTasks];
        this.tasksEarliestProcessorTracker = new int[numOfTasks];
        this.tasksDuration = new int[numOfTasks];
        this.criticalPaths = new int[numOfTasks];

        for(int i = 0; i < numOfTasks; i++){
            this.tasksDuration[i] = graphData.getTask(i).getWeight();
            this.tasksProcessorTracker[i] = -1;
            this.remainTaskDuration = this.remainTaskDuration + this.tasksDuration[i];
            this.totalTaskDuration = this.totalTaskDuration + this.tasksDuration[i];
        }

        for(int i = 0; i < numOfTasks; i++) {
            int[] bottomLevel = new int[numOfTasks];
            this.criticalPaths[i] = this.calBottomLevel(i, bottomLevel);
        }

        // put all task with zero incoming edges into liveTaskList
        graphData.updateLiveTaskList();

        // Start search the optimal algorithm.
        recursiveDFS();

        // get the schedule back here
        Task[] optimalSchedule = new Task[numOfTasks];

        for (int i = 0; i < numOfTasks; i++){
            Task task = new Task(this.tasksEarliestProcessorTracker[i]);
            task.setStartTime(this.tasksEarliestStartTime[i]);
            task.setFinishTime(this.tasksEarliestStartTime[i] + this.tasksDuration[i]);
            optimalSchedule[i] = task;
        }

        Main.setSearchCount("Search Count: " + stateCount);
        Main.setTimeElapsed("Time Elapsed(ms): " + (System.currentTimeMillis() - startTime));
        Main.setEndTime("Finish Time: " + bestFinishTime);

        return optimalSchedule;
    }

    /**
     * This method try all possible schedule recursively.
     */
    public void recursiveDFS(){

        /*
          Define the base case.
         */

        stateCount++;

        Main.setSearchCount("Search Count: " + stateCount);
        Main.setTimeElapsed("Time Elapsed(ms): " + (System.currentTimeMillis() - startTime));
        
        if(graphData.getLiveTaskList().isEmpty()){


            List<Integer> listForSort= Arrays.stream(this.processorsFinishTime)
                    .boxed()
                    .collect(Collectors.toList());
            // get the latest finish time for this iteration.
            int currentFinishTime =  Collections.max(listForSort);
            // get the load balance for this iteration.
            int currentLoadBalance = this.calLoadBalance("total");

            // update the upper bound and load balance
            if (currentFinishTime <= this.bestFinishTime ) {
                this.bestFinishTime = currentFinishTime;
//                endTime.set("Finish Time: " + bestFinishTime);
                this.loadBalance = currentLoadBalance;
                // update the schedule.
                for (int i = 0; i < this.tasksEarliestStartTime.length; i++) {
                    this.tasksEarliestProcessorTracker[i] = tasksProcessorTracker[i];
                    this.tasksEarliestStartTime[i] = this.tasksStartTime[i];
                }
                graphData.updateGraph(tasksEarliestProcessorTracker, tasksEarliestStartTime);
            }

            return;
        }

        // prepare the heuristic bound

        ArrayList<Integer> criticalTaskLists= new ArrayList<>();

        for (int t : graphData.getLiveTaskList()){
            criticalTaskLists.add(criticalPaths[t]);
        }
        // get the max critical path form the tasks in liveTaskList
        int longestCriticalPath = Collections.max(criticalTaskLists);

        List<Integer> listForSort= Arrays.stream(this.processorsFinishTime)
                .boxed()
                .collect(Collectors.toList());
        // get the earliest processor finish time
        int earliestStartTime = Collections.min(listForSort);
        // divide the time by the number of processors, using in load balance underestimation.
        int earliestStartTimeLoadBalance = (int) Math.ceil(earliestStartTime/ (double)this.numOfProcessors);
       // calculate the load balance which assume there is no idle time in the unscheduled task.
        int partialBalance = this.calLoadBalance("remain");
       // calculate the idea load balance which assume there is no idle time in the schedule.
        int perfectLoadBalance = (int) Math.ceil(this.remainTaskDuration/ (double) this.numOfProcessors);

       // start the recursive search process.
        for (int i = 0; i < graphData.getLiveTaskList().size(); i++) {

            int task = graphData.getLiveTaskList().remove();

            int dataReadyTime = this.findDataReadyTime(task);

            // use a combination of 4 underestimation to prune the search space.
            if( earliestStartTime + perfectLoadBalance >= bestFinishTime ||
                earliestStartTimeLoadBalance + partialBalance > loadBalance ||
                earliestStartTime + longestCriticalPath >= bestFinishTime ||
                (dataReadyTime + tasksDuration[task]) > bestFinishTime) {

                graphData.getLiveTaskList().add(task);

                continue;
            }

            // update the remaining task duration
            this.remainTaskDuration -= this.tasksDuration[task];
            // removing the incoming edges for all the children task of the current task.
            graphData.removeIncomingEdges(task);
            graphData.updateLiveTaskList();


            // find the parent tasks of the current task.
            ArrayList<Integer> parentTasks = graphData.getFixInAdjacentList().get(task);

            boolean scheduledOnEmptyProcessor = false;

            for (int processorIndex = 0; processorIndex < this.numOfProcessors; processorIndex++) {

                //detect the isomorphic processors
                if(this.processorsFinishTime[processorIndex] == 0 ){
                    if(scheduledOnEmptyProcessor) {
                        continue;
                    }
                    else{
                        scheduledOnEmptyProcessor = true;
                    }
                }

                // find the earliest start time on this processor
                int startTime = this.findEarliestStartTime(task, processorIndex, parentTasks);

                // save the current processor's finish time.
                int previousFinishTime = this.processorsFinishTime[processorIndex];

                // update the processor's finish time after schedule the task on this processor.
                this.processorsFinishTime[processorIndex] = startTime + this.tasksDuration[task];

                // update the task's start time.
                this.tasksStartTime[task] = startTime;

                // update the task processor tracker.
                this.tasksProcessorTracker[task] = processorIndex;

                // start recursive search again.
                recursiveDFS();

                // start backtrack and reload the previous finish
                this.processorsFinishTime[processorIndex] = previousFinishTime;

            }
            // start backtrack and add the previous removed incoming edges back
            this.tasksProcessorTracker[task] = -1;
            graphData.addIncomingEdges(task, graphData.getLiveTaskList());
            graphData.getLiveTaskList().add(task);
            // add the task duration back
            this.remainTaskDuration += this.tasksDuration[task];

        }
    }


    /**
     * This method finds the earliest possible start time for a node that is to be scheduled,
     * keeping all constraints on the node into account
     * @param taskIndex is an Integer that represent the task index.
     * @param processorIndex is an Integer that represent the processor.
     * @param parentTasks is an ArrayList that store all the parent tasks of the current task.
     */
    public int findEarliestStartTime(int taskIndex,int processorIndex, ArrayList<Integer> parentTasks ) {
        int startTime;
        // if the task has no parent tasks, just start at the processor's finish time.
        if(parentTasks.isEmpty()){
            startTime = this.processorsFinishTime[processorIndex];
            return startTime;
        }

        ArrayList<Integer> startTimeList = new ArrayList<>();
        // loops every parent task
        for (int t : parentTasks) {

            int parentTaskProcessor =  this.tasksProcessorTracker[t];
            int parentTaskFinishTime = this.tasksStartTime[t] + this.tasksDuration[t];
            int earliestStartTimeCommunicationDelay = graphData.getCommunicationCost(t, taskIndex) + parentTaskFinishTime;

            if (processorIndex != parentTaskProcessor) {
                startTimeList.add(earliestStartTimeCommunicationDelay);
            }
            startTimeList.add(this.processorsFinishTime[processorIndex]);
        }
        startTime = Collections.max(startTimeList);
        return startTime;
    }


    /**
     * This method calculate the bottom level for a task.
     * @param task is an Integer that represent the task index.
     * @param bottomLevel is an Integer array that save the possible maximum exit length.
     */
    private int calBottomLevel(int task, int[] bottomLevel){

        if(bottomLevel[task] != 0){
            return bottomLevel[task];
        }

        int max = 0;

        if(graphData.getOutAdjacentList().get(task).isEmpty()){
            bottomLevel[task] = this.tasksDuration[task];
            return bottomLevel[task];
        }

        for(int t : graphData.getOutAdjacentList().get(task)){
            max = Math.max(max, calBottomLevel(t, bottomLevel));
        }

        bottomLevel[task] = max + this.tasksDuration[task];
        return  bottomLevel[task];
    }




    public int findDataReadyTime(int task) {

        int dataReadyTime;

        ArrayList<Integer> startList = new ArrayList<>();

        ArrayList<Integer> parentTaskList = graphData.getFixInAdjacentList().get(task);

        // support multi nodes that are with no source nodes.
        // loop through the schedule, find the processor with the earliest start time to schedule.
        if(parentTaskList.isEmpty()){
            for(int i = 0; i < this.numOfProcessors; i++){
                startList.add(processorsFinishTime[i]);
            }

        }
        else {
            //loops through each processor
            for (int i = 0; i < this.numOfProcessors; i++) {
                int earliestStartTimeNoCollision = processorsFinishTime[i];
                ArrayList<Integer> minStartEachProcess = new ArrayList<>();
                // loops every parent node
                for (int t : parentTaskList) {

                    int parentTaskProcessor = this.tasksProcessorTracker[t];
                    int parentTaskFinishTime = this.tasksStartTime[t] + this.tasksDuration[t];
                    int earliestStartTimeCommunicationDelay = graphData.getCommunicationCost(t, task) + parentTaskFinishTime;

                    if (i != parentTaskProcessor) {
                        minStartEachProcess.add(earliestStartTimeCommunicationDelay);
                    }
                    minStartEachProcess.add(earliestStartTimeNoCollision); // satisfies communication delay constraint for dependency nodes

                }
                // finds the earliest possible start time when considering both constraints for each processor
                startList.add(Collections.max(minStartEachProcess));
            }
            // finds the earliest amongst the valid start times

        }
        dataReadyTime = Collections.min(startList);
        return dataReadyTime;
    }


    /**
     * This method calculate the load balance of the schedule
     * ad balance = (sum of duration + sum of idle time) / number of processors
     * @param req is a string that use as an identifier to decide which value should return
     */
    public int calLoadBalance(String req){
        int totalIdleTime = 0;
        for(int i = 0; i < processorsFinishTime.length; i++){
            int idleTime = processorsFinishTime[i];
            for(int j = 0; j < tasksProcessorTracker.length; j++){

                if(tasksProcessorTracker[j] != -1 && tasksProcessorTracker[j] == i){
                    idleTime = idleTime - this.tasksDuration[j];
                }
            }
            totalIdleTime = totalIdleTime + idleTime;
        }

        if(req.equals("total")){
            return (int)Math.ceil((this.totalTaskDuration + totalIdleTime) /(double) this.numOfProcessors);
        }
        else{
            return (int)Math.ceil((this.remainTaskDuration + totalIdleTime) /(double) this.numOfProcessors);
        }

    }

}
