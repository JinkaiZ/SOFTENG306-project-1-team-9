package Scheduler;

import java.util.concurrent.ForkJoinPool;

public class DFAlgorithmParallel {


    private final GraphData graphData;

    public static int searchCount = 0;
    public static long startTime = 0;

    public DFAlgorithmParallel(GraphData graphData) {
        this.graphData = graphData;
    }

    /**
     * This method runs the parallel version of the DFS algorithm given a set of predefined threads.
     * @param numOfCores number of cores
     * @param numOfProcessors number of processors
     * @param upperBound known upper bound
     * @param loadBalance perfect load balance
     * @return A Task array with the optimal schedule where every index corresponds to a node
     */
    public Task[] run(int numOfCores,int numOfProcessors, int upperBound, int loadBalance ) {
        startTime = System.currentTimeMillis();

        ForkJoinPool pool = new ForkJoinPool(numOfCores);
        int numOfTasks = graphData.getTotalNumberOfNodes();
        int totalTaskDuration = 0;

        // Track the last finish time of each processor.
        int[] processorsFinishTime = new int[numOfProcessors];
        // Track the processor that each task is scheduled on.
        int[] tasksStartTime = new int [numOfTasks];
        int[] tasksProcessorTracker = new int [numOfTasks];
        int[] tasksDuration = new int[numOfTasks];

        int[] criticalPaths = new int[numOfTasks];
        int remainTaskDuration = 0;

        graphData.updateLiveTaskList();

        for(int i = 0; i < numOfTasks; i++){
            tasksDuration[i] = graphData.getTask(i).getWeight();
            tasksProcessorTracker[i] = -1;
            totalTaskDuration += tasksDuration[i];
            remainTaskDuration += tasksDuration[i];
        }

        for(int i = 0; i < numOfTasks; i++) {
            int[] bottomLevel = new int[numOfTasks];
            criticalPaths[i] = this.calBottomLevel(i, bottomLevel);
        }


        SearchState init = new SearchState(tasksStartTime, tasksProcessorTracker, processorsFinishTime,
                 graphData, remainTaskDuration, loadBalance);

        DFRecursive rec = new DFRecursive(init, numOfProcessors, criticalPaths, tasksDuration, totalTaskDuration);
        rec.setOg(graphData);
        rec.setBestFinishTime(upperBound);
        rec.setTasksEarliestStartTime(new int[numOfTasks]);
        rec.setTasksEarliestProcessorTracker(new int[numOfTasks]);

        pool.invoke(rec);

        Task[] optimalSchedule = new Task[numOfTasks];
        for (int i = 0; i < numOfTasks; i++){
            Task task = new Task(rec.getTasksEarliestProcessorTracker()[i]);
            task.setStartTime(rec.getTasksEarliestStartTime()[i]);
            task.setFinishTime(rec.getTasksEarliestStartTime()[i] + tasksDuration[i]);
            optimalSchedule[i] = task;
        }
        return optimalSchedule;
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
            bottomLevel[task] = graphData.getTask(task).getWeight();
            return bottomLevel[task];
        }

        for(int t : graphData.getOutAdjacentList().get(task)){
            max = Math.max(max, calBottomLevel(t, bottomLevel));
        }

        bottomLevel[task] = max + graphData.getTask(task).getWeight();
        return  bottomLevel[task];
    }
}
