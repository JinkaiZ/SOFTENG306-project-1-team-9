package Scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class SearchState {

    protected int[] tasksStartTime;
    protected int[] tasksProcessorTracker;
    protected int[] processorsFinishTime;
    protected GraphData graphData;
    protected int remainTaskDuration;
    protected int loadBalance;

    public SearchState( int[] tasksStartTime, int[] tasksProcessorTracker,
                       int[] processorsFinishTime, GraphData graphData, int remainTaskDuration, int loadBalance) {
        this.tasksStartTime = tasksStartTime;
        this.tasksProcessorTracker = tasksProcessorTracker;
        this.processorsFinishTime = processorsFinishTime;
        this.graphData = graphData;
        this.remainTaskDuration = remainTaskDuration;
        this.loadBalance = loadBalance;
    }

    // Make a deep copy / clone of the Search state
    public SearchState clone() {

        int nOfTasks = tasksStartTime.length;
        int nOfP = processorsFinishTime.length;

        int[] tasksStartTimeClone = new int[nOfTasks];
        int[] tasksProcessorTrackerClone = new int[nOfTasks];
        int[] processorsFinishTimeClone = new int[nOfP];

        for (int i = 0; i < nOfTasks; i++) {
            tasksStartTimeClone[i] = tasksStartTime[i];
            tasksProcessorTrackerClone[i] = tasksProcessorTracker[i];
        }

        System.arraycopy(processorsFinishTime, 0, processorsFinishTimeClone, 0, nOfP);
        HashMap<Integer , ArrayList<Integer>> dynamicInAdjacentListClone = new HashMap<>();
        for(int key : graphData.getDynamicInAdjacentList().keySet()) {
            dynamicInAdjacentListClone.put(key, new ArrayList<>(graphData.getDynamicInAdjacentList().get(key)));
        }

        LinkedList<Integer> liveTaskListClone = new LinkedList<>(this.graphData.getLiveTaskList());

        GraphData graphDataClone = new GraphData(liveTaskListClone, this.graphData.getCommunicationArray(), this.graphData.getOutAdjacentList(),
                this.graphData.getFixInAdjacentList(), dynamicInAdjacentListClone,
                this.graphData.getAllTasks(), this.graphData.getTotalNumberOfNodes());

        return new SearchState(tasksStartTimeClone, tasksProcessorTrackerClone, processorsFinishTimeClone,
                 graphDataClone, this.remainTaskDuration, this.loadBalance);
    }
}
