package Scheduler;

import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

public class DFRecursive extends RecursiveAction {


    private final SearchState state;
    private final int numOfProcessors;

    private static volatile int bestFinishTime;

    private final int[] criticalPaths;
    private final int[] tasksDuration;
    private final int totalTaskDuration;

    private static volatile int[] tasksEarliestStartTime;
    private static volatile int[] tasksEarliestProcessorTracker;
    private static volatile GraphData og;

    public DFRecursive(SearchState state, int numOfProcessors, int[] criticalPaths, int[] tasksDuration, int totalTaskDuration) {
        this.state = state;
        this.numOfProcessors = numOfProcessors;
        this.criticalPaths = criticalPaths;
        this.tasksDuration = tasksDuration;
        this.totalTaskDuration = totalTaskDuration;
    }

    public void setBestFinishTime(int bft) { bestFinishTime = bft; }

    public void setTasksEarliestStartTime(int[] est) {
        tasksEarliestStartTime = est;
    }

    public int[] getTasksEarliestStartTime() {
        return tasksEarliestStartTime;
    }

    public void setTasksEarliestProcessorTracker(int[] ept) {
        tasksEarliestProcessorTracker = ept;
    }

    public int[] getTasksEarliestProcessorTracker() {
        return tasksEarliestProcessorTracker;
    }

    public void setOg(GraphData graphData) {
        og = graphData;
    }

    /**
     * This is the DFS branch and bound algorithm where new tasks are passed into the fork join pool
     * and allocated to a thread.
     */
    @Override
    protected void compute() {

        if (state.graphData.getLiveTaskList().isEmpty()) {
            List<Integer> listForSort = Arrays.stream(state.processorsFinishTime)
                    .boxed()
                    .collect(Collectors.toList());
            // get the finish time for this iteration.
            int currentFinishTime = Collections.max(listForSort);

            this.calLoadBalance("total");

            // update the upper bound
            updateFinishTimeAndSchedule(currentFinishTime);

            return;
        }

        DFAlgorithmParallel.searchCount++;
        Main.setSearchCount("Search Count: " + DFAlgorithmParallel.searchCount);
        Main.setTimeElapsed("Time Elapsed(ms): " + (System.currentTimeMillis() - DFAlgorithmParallel.startTime));
        Main.setEndTime("Finish Time: " + bestFinishTime);

        ArrayList<Integer> criticalTaskLists= new ArrayList<>();

        for (int t : state.graphData.getLiveTaskList()){
            criticalTaskLists.add(this.criticalPaths[t]);
        }

        int longestCriticalPath = Collections.max(criticalTaskLists);

        // check the critical path of the node at the earliest start time, if it > best time then trim.
        List<Integer> listForSort= Arrays.stream(state.processorsFinishTime)
                .boxed()
                .collect(Collectors.toList());

        int earliestStartTime = Collections.min(listForSort);

        int earliestStartTimeLoadBalance = (int) Math.ceil(earliestStartTime/ (double)this.numOfProcessors);

        int partialBalance = this.calLoadBalance("remain");

        int perfectLoadBalance = (int) Math.ceil(state.remainTaskDuration/ (double) this.numOfProcessors);



        for (int i = 0; i < state.graphData.getLiveTaskList().size(); i++) {

            // pull out the task.
            int task = state.graphData.getLiveTaskList().remove();

            int dataReadyTime = this.findDataReadyTime(task);


            // need to change. the bestFinishTime.
            if( earliestStartTime + perfectLoadBalance >= bestFinishTime || earliestStartTimeLoadBalance + partialBalance > state.loadBalance || earliestStartTime + longestCriticalPath >= bestFinishTime || (dataReadyTime + this.tasksDuration[task]) > bestFinishTime) {

                state.graphData.getLiveTaskList().add(task);
                continue;
            }

            state.remainTaskDuration -= this.tasksDuration[task];

            state.graphData.removeIncomingEdges(task);
            state.graphData.updateLiveTaskList();

            // find the parent tasks of the current task
            ArrayList<Integer> parentTasks = state.graphData.getFixInAdjacentList().get(task);

            // make a deep copy for next round recursion
            LinkedList<DFRecursive> nextRoundLiveTaskList = new LinkedList<>();

            boolean scheduledOnEmptyProcessor = false;

            for (int processorIndex = 0; processorIndex < this.numOfProcessors; processorIndex++) {

                //detect the isomorphic processors
                if(state.processorsFinishTime[processorIndex] == 0 ){
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
                int previousFinishTime = state.processorsFinishTime[processorIndex];

                // after schedule the task on this processor, update the processor's finish time.
                state.processorsFinishTime[processorIndex] = startTime + this.tasksDuration[task];

                // update the task's start time.
                state.tasksStartTime[task] = startTime;

                // update the task processor tracker.
                state.tasksProcessorTracker[task] = processorIndex;


                // add next recursive call to list
                DFRecursive rec = new DFRecursive(state.clone(), this.numOfProcessors, this.criticalPaths, this.tasksDuration,
                        this.totalTaskDuration);
                nextRoundLiveTaskList.add(rec);

                // reload the previous finish
                state.processorsFinishTime[processorIndex] = previousFinishTime;

            }

            state.tasksProcessorTracker[task] = -1;
            state.graphData.addIncomingEdges(task, state.graphData.getLiveTaskList());
            state.graphData.getLiveTaskList().add(task);

            state.remainTaskDuration += state.remainTaskDuration;

            invokeAll(nextRoundLiveTaskList);
        }

    }

    /**
     * This method updates the global variables that are used to create the optimum schedule. Hence, needs
     * to be synchronized.
     * @param currentFinishTime the current best finish time
     */
    public synchronized void updateFinishTimeAndSchedule(int currentFinishTime) {

        if (currentFinishTime <= bestFinishTime) {
            bestFinishTime = currentFinishTime;

            // System.out.println("Best finish time: " + bestFinishTime);
            // update the schedule.
            for (int i = 0; i < tasksEarliestStartTime.length; i++) {
                tasksEarliestProcessorTracker[i] = state.tasksProcessorTracker[i];
                tasksEarliestStartTime[i] = state.tasksStartTime[i];
            }
            og.updateGraph(tasksEarliestProcessorTracker, tasksEarliestStartTime);
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

            startTime = state.processorsFinishTime[processorIndex];

            return startTime;
        }

        ArrayList<Integer> startTimeList = new ArrayList<>();
        // loops every parent task
        for (int t : parentTasks) {

            int parentTaskProcessor =  state.tasksProcessorTracker[t];
            int parentTaskFinishTime = state.tasksStartTime[t] + this.tasksDuration[t];
            int earliestStartTimeCommunicationDelay = state.graphData.getCommunicationCost(t, taskIndex) + parentTaskFinishTime;

            if (processorIndex != parentTaskProcessor) {
                startTimeList.add(earliestStartTimeCommunicationDelay);
            }
            startTimeList.add(state.processorsFinishTime[processorIndex]);
        }
        startTime = Collections.max(startTimeList);
        return startTime;
    }


    public int findDataReadyTime(int task) {

        int dataReadyTime;

        ArrayList<Integer> startList = new ArrayList<>();

        ArrayList<Integer> parentTaskList = state.graphData.getFixInAdjacentList().get(task);


        // support multi nodes that are with no source nodes.
        // loop through the schedule, find the processor with the earliest start time to schedule.
        if(parentTaskList.isEmpty()){
            for(int i = 0; i < this.numOfProcessors; i++){
                startList.add(state.processorsFinishTime[i]);
            }

        }
        else {
            //loops through each processor
            for (int i = 0; i < this.numOfProcessors; i++) {
                int earliestStartTimeNoCollision = state.processorsFinishTime[i];
                ArrayList<Integer> minStartEachProcess = new ArrayList<>();
                // loops every parent node
                for (int t : parentTaskList) {

                    int parentTaskProcessor = state.tasksProcessorTracker[t];
                    int parentTaskFinishTime = state.tasksStartTime[t] + this.tasksDuration[t];
                    int earliestStartTimeCommunicationDelay = state.graphData.getCommunicationCost(t, task) + parentTaskFinishTime;

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
        for(int i = 0; i < state.processorsFinishTime.length; i++){
            int idleTime = state.processorsFinishTime[i];
            for(int j = 0; j < state.tasksProcessorTracker.length; j++){

                if(state.tasksProcessorTracker[j] != -1 && state.tasksProcessorTracker[j] == i){
                    idleTime = idleTime - this.tasksDuration[j];
                }
            }
            totalIdleTime = totalIdleTime + idleTime;
        }

        if(req.equals("total")){
            return (int)Math.ceil((this.totalTaskDuration + totalIdleTime) /(double) this.numOfProcessors);
        }
        else{
            return (int)Math.ceil((state.remainTaskDuration + totalIdleTime) /(double) this.numOfProcessors);
        }

    }

}
