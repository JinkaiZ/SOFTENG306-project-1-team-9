package Scheduler;

import java.util.*;
import java.util.stream.Collectors;

public class ListScheduling {

    private GraphData graphData;
    private int numberOfProcessor;
    private int totalTaskDuration;
    private int[] tasksStartTime;
    private int[] tasksProcessorTracker;
    private int[] tasksDuration;
    private int[] processorsFinishTime;


    /**
     * This method creates a valid schedule given the specified graph and number of processors
     * @param graphData is a class that contains the graph object and additional related information
     * @param numberOfProcessors is an Integer specifying the number of processors to use
     * @return Schedule is a class which contains the start and end time of tasks on each processor
     */
    public int[] greedyAlgorithm(GraphData graphData, int numberOfProcessors) {

        this.graphData = graphData;
        int numberOfTasks = graphData.getTotalNumberOfNodes();
        this.numberOfProcessor = numberOfProcessors;
        this.tasksProcessorTracker = new int[numberOfTasks];
        this.tasksStartTime = new int[numberOfTasks];
        this.processorsFinishTime = new int[numberOfProcessors];
        this.tasksDuration = new int[numberOfTasks];
        this.totalTaskDuration = 0;


        // get the duration of each task and put into tasksDuration array.
        // get the sum of all task's durations
        for (int i = 0; i <numberOfTasks ; i++) {
            this.tasksDuration[i] = graphData.getTask(i).getWeight();
            this.totalTaskDuration += this.tasksDuration[i];
        }

        // initialise the return value which is an array of size 2.
        // upperBoundAndLoadBalance[0] is the finish time
        // upperBoundAndLoadBalance[1] is the load balance of the final schedule.
        int[] upperBoundAndLoadBalance = new int[2];

        // the liveTaskList contains the tasks that has zero incoming edges.
        this.graphData.updateLiveTaskList();

        // continues until all nodes exhausted
        while (!graphData.getLiveTaskList().isEmpty()) {

            //pop the task out of the queue
            int task = graphData.getLiveTaskList().remove();

            // remove the incoming edges in the dynamic adjacent list.
            graphData.removeIncomingEdges(task);

            findEarliestStartTime(task);

            graphData.updateLiveTaskList();

        }

        List<Integer> listForSort= Arrays.stream(this.processorsFinishTime)
                .boxed()
                .collect(Collectors.toList());

        // find the max number in the processorsFinishTime array.
        // get the finish time for this iteration.
        int upperBound =  Collections.max(listForSort);

        // get the load balance of the finish schedule.
        int loadBalance = this.calLoadBalance(processorsFinishTime, tasksProcessorTracker);

        upperBoundAndLoadBalance[0] = upperBound;
        upperBoundAndLoadBalance[1] = loadBalance;

        return upperBoundAndLoadBalance;
    }

    /**
     * This method finds the earliest possible start time for a node that is to be scheduled,
     * keeping all constraints on the node into account
     * @param task is an Integer that represent the task index.
     */
    public void  findEarliestStartTime(int task) {

        ArrayList<Integer> startList = new ArrayList<>();
        ArrayList<Integer> parentTaskList = graphData.getFixInAdjacentList().get(task);

        // support multi nodes that are with no source nodes.
        // loop through the schedule, find the processor with the earliest start time to schedule.
        if(parentTaskList.isEmpty()){
            for(int i = 0; i < this.numberOfProcessor; i++){
                startList.add(processorsFinishTime[i]);
            }

        }
        else {
            //loops through each processor
            for (int i = 0; i < this.numberOfProcessor; i++) {
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
        this.tasksProcessorTracker[task] = startList.indexOf(Collections.min(startList));
        this.tasksStartTime[task] = Collections.min(startList);
        this.processorsFinishTime[this.tasksProcessorTracker[task]] = this.tasksStartTime[task] + this.tasksDuration[task];
    }

    /**
     * This method calculate the load balance of the schedule
     * ad balance = (sum of duration + sum of idle time) / number of processors
     * @param processorsFinishTime is an array that save the latest finish time for each processor.
     * @param tasksProcessorTracker is an array that record the allocated processor of each task
     */
    public int calLoadBalance(int[] processorsFinishTime, int[] tasksProcessorTracker){
        int totalIdleTime = 0;
        for(int i = 0; i < processorsFinishTime.length; i++){
            int idleTime = processorsFinishTime[i];
            for(int j = 0; j < tasksProcessorTracker.length; j++){
                if(tasksProcessorTracker[j] == i){
                    idleTime = idleTime - this.tasksDuration[j];
                }
            }
            totalIdleTime = totalIdleTime + idleTime;
        }

        return (int)Math.ceil((totalTaskDuration + totalIdleTime) /(double) this.numberOfProcessor);
    }
}


