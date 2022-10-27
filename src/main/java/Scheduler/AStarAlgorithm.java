package Scheduler;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.*;

public class AStarAlgorithm {

    private Graph g;
    private Graph gCopy;
    private GraphData graphData;
    private int numOfProcessors;
    private int numOfTasks = 0;
    private int[] tasksDuration;
    private int totalTaskDuration = 0;

    private long startTime = 0;

    //This empty Arraylist is used to find the node with no parent nodes
    private ArrayList<Integer> empty = new ArrayList<>();

    public Task[] run(Graph gCopy, Graph g, GraphData graphData, int numOfProcessors) {
        startTime = System.currentTimeMillis();

        this.g = g;
        this.gCopy = gCopy;
        this.graphData = graphData;
        this.numOfProcessors = numOfProcessors;
        numOfTasks = graphData.getTotalNumberOfNodes();

        int remainTaskDuration = 0;

        // Track the last finish time of each processor.
        int[] processorsFinishTime = new int[numOfProcessors];
        // Track the processor that each task is scheduled on.
        int[] tasksStartTime = new int [numOfTasks];
        int[] tasksProcessorTracker = new int [numOfTasks];
        tasksDuration = new int[numOfTasks];

        for(int i = 0; i < numOfTasks; i++){
            tasksDuration[i] = graphData.getTask(i).getWeight();
            tasksProcessorTracker[i] = -1;
            totalTaskDuration += tasksDuration[i];
            remainTaskDuration += tasksDuration[i];
        }

        Task[] optimalSchedule = new Task[numOfTasks];

        SimpleState initState = new SimpleState(tasksStartTime, tasksProcessorTracker, processorsFinishTime, graphData.getDynamicInAdjacentList(), remainTaskDuration, 0);

        Set<SimpleState> searchedStates = new HashSet<>();
        Set<SimpleState> unSearchedStates = new HashSet<>();

        unSearchedStates.add(initState);

        while (unSearchedStates.size() != 0) {
            SimpleState currentState = getNextBestState(unSearchedStates);
            int[] sortedFinishTime = currentState.clone().processorsFinishTime;
            Arrays.sort(sortedFinishTime);
            Main.setEndTime("Finish Time: " + sortedFinishTime[sortedFinishTime.length - 1]);
            Main.setSearchCount("Search Count: " + (searchedStates.size()));
            Main.setTimeElapsed("Time Elapsed(ms): " + (System.currentTimeMillis() - startTime));

            for (int i = 0; i < numOfTasks; i++) {
                    Node task = this.gCopy.getNode(i);
                    task.setAttribute("StartTime", currentState.tasksStartTime[i]);
                    task.setAttribute("Processor", currentState.tasksProcessorTracker[i]);
                    task.setAttribute("ui.label", "ID: " + task.getId() + "\nWeight: " + task.getAttribute("Weight") + "\nStartTime: " + currentState.tasksStartTime[i] +
                            "\nProcessor: " + currentState.tasksProcessorTracker[i]);
                try {
                    Thread.sleep(0, 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            unSearchedStates.remove(currentState);

            if (currentState.remainTaskDuration == 0) {
                for (int i = 0; i < numOfTasks; i++) {
                    Task task = new Task(i, this.tasksDuration[i]);
                    task.setStartTime(currentState.tasksStartTime[i]);
                    task.setFinishTime(currentState.tasksStartTime[i] + ((Double) g.getNode(i).getAttribute("Weight")).intValue());
                    task.setProcessNum(currentState.tasksProcessorTracker[i]);
                    optimalSchedule[i] = task;
                }
                return optimalSchedule;
            }

            LinkedList<Integer> zeroInDegreeNodes = getZeroInDegreeNodes(currentState);
            for (Integer nodeID : zeroInDegreeNodes) {
                if (searchedStates.size() == 0) {
                    SimpleState childState = currentState.clone();
                    scheduleTask(nodeID, 0, childState);

                    Set<Integer> bottomLevels = new HashSet<>();
                    for (int i = 0; i < numOfTasks; i++) {
                        if (childState.tasksProcessorTracker[i] != -1) {
                            bottomLevels.add(calLoadBalance(i, childState) + childState.tasksStartTime[i]);
                        }
                    }
                    childState.setLoadBalance(Collections.max(bottomLevels));

                    unSearchedStates.add(childState);
                } else {
                    for (int i = 0; i < numOfProcessors; i++) {
                        SimpleState childState = currentState.clone();
                        scheduleTask(nodeID, i, childState);

                        Set<Integer> bottomLevels = new HashSet<>();
                        for (int j = 0; j < numOfTasks; j++) {
                            if (childState.tasksProcessorTracker[j] != -1) {
                                bottomLevels.add(calLoadBalance(j, childState) + childState.tasksStartTime[j]);
                            }
                        }
                        childState.setLoadBalance(Collections.max(bottomLevels));

                        if (!ifContains(searchedStates, childState)) {
                            unSearchedStates.add(childState);
                        }
                    }
                }
            }
            searchedStates.add(currentState);
        }
        return null;
    }

    public SimpleState getNextBestState(Set<SimpleState> unSearchedStates) {
        SimpleState nextBestState = null;
        int lowestLoadBalance = Integer.MAX_VALUE;
        for (SimpleState state : unSearchedStates) {
            int stateLoadBalance = state.getLoadBalance();
            if (stateLoadBalance < lowestLoadBalance) {
                lowestLoadBalance = stateLoadBalance;
                nextBestState = state;
            }
        }
        return nextBestState;
    }

    public LinkedList<Integer> getZeroInDegreeNodes(SimpleState state) {
        return graphData.getKeysByValue(state.getDynamicInAdjacentList(), empty);
    }

    public int calLoadBalance(int nodeID, SimpleState state){

        for (int i = 0; i < numOfTasks; i++) {
            g.getNode(i).setAttribute("Distance", -1);
        }

        Node n = g.getNode(nodeID);
        n.setAttribute("Distance", 0);

        Set<Node> searchedNodes = new HashSet<>();
        Set<Node> unSearchedNodes = new HashSet<>();

        unSearchedNodes.add(n);

        while (unSearchedNodes.size() != 0) {
            Node currentNode = getFarthestNode(unSearchedNodes);
            unSearchedNodes.remove(currentNode);

            List<Node> adjacentNodes = new ArrayList<>();
            currentNode.leavingEdges().forEach(e -> adjacentNodes.add(e.getTargetNode()));

            for (Node adjacentNode : adjacentNodes) {
                if (!searchedNodes.contains(adjacentNode) && state.tasksProcessorTracker[adjacentNode.getIndex()] == -1) {
                    calculateMaxDistance(adjacentNode, currentNode);
                    unSearchedNodes.add(adjacentNode);
                }
            }
            searchedNodes.add(currentNode);
        }

        int bottomLevel = -1;

        for (Node node : searchedNodes) {
            int distance = (Integer) node.getAttribute("Distance");
            if (distance > bottomLevel) {
                bottomLevel = distance;
            }
        }

        return bottomLevel + ((Double) n.getAttribute("Weight")).intValue();
    }

    public void scheduleTask(int taskId, int processorNum, SimpleState state) {
        ArrayList<Integer> parentTasks = new ArrayList<>();
        Node node = g.getNode(taskId);
        node.enteringEdges().forEach(edge -> {
            parentTasks.add(edge.getSourceNode().getIndex());
        });
        int nodeWeight = ((Double) node.getAttribute("Weight")).intValue();
        int startTime = findEarliestStartTime(taskId, processorNum, parentTasks, state);
        state.setTasksStartTime(taskId, startTime);
        state.setTasksProcessorTracker(taskId, processorNum);
        state.setProcessorsFinishTime(processorNum, startTime + nodeWeight);
        state.subtractRemainTaskDuration(nodeWeight);
        removeIncomingEdges(taskId, state);
    }

    public int findEarliestStartTime(int taskIndex,int processorIndex, ArrayList<Integer> parentTasks, SimpleState state) {
        int startTime = 0;
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
            int earliestStartTimeCommunicationDelay = graphData.getCommunicationCost(t, taskIndex) + parentTaskFinishTime;

            if (processorIndex != parentTaskProcessor) {
                startTimeList.add(earliestStartTimeCommunicationDelay);
            }
            startTimeList.add(state.processorsFinishTime[processorIndex]);
        }
        startTime = Collections.max(startTimeList);
        return startTime;
    }

    public void removeIncomingEdges(Integer task, SimpleState state) {
        ArrayList<Integer> reduceList = graphData.getOutAdjacentList().get(task);
        for (int t : reduceList) {
            if(state.dynamicInAdjacentList.containsKey(t)) {
                state.dynamicInAdjacentList.get(t).remove(Integer.valueOf(task));
            }
        }
        state.dynamicInAdjacentList.remove(task);
    }

    public boolean ifContains(Set<SimpleState> searchedStates, SimpleState state) {
        boolean contains = true;
        for (SimpleState searchedState : searchedStates) {
            for (int i = 0; i < numOfTasks; i++) {
                if (searchedState.tasksStartTime[i] != state.tasksStartTime[i]) {
                    contains = false;
                }
                if (searchedState.tasksProcessorTracker[i] != state.tasksProcessorTracker[i]) {
                    contains = false;
                }
            }
        }
        return contains;
    }

    private Node getFarthestNode(Set<Node> unSearchedNodes) {

        Node farthestNode = null;
        int highestDistance = -1;
        for (Node n : unSearchedNodes) {
            int nodeDistance = (Integer) n.getAttribute("Distance");
            if (nodeDistance > highestDistance) {
                highestDistance = nodeDistance;
                farthestNode = n;
            }
        }
        if (farthestNode == null) {
            System.out.println("null");
        }

        return farthestNode;
    }

    private void calculateMaxDistance(Node adjacentNode, Node sourceNode) {

        Integer sourceDistance = (Integer) sourceNode.getAttribute("Distance");
        Integer adjacentWeight = ((Double) adjacentNode.getAttribute("Weight")).intValue();

        if (sourceDistance + adjacentWeight > (Integer) adjacentNode.getAttribute("Distance")) {
            adjacentNode.setAttribute("Distance", sourceDistance + adjacentWeight);
        }
    }

}
