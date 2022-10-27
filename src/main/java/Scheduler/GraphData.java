package Scheduler;

import javafx.scene.layout.StackPane;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;

import java.util.*;

/**
 * The GraphData class parse the Graph object into the needed data structure
 * and provide related operation methods.
 */
public class GraphData {
    //combine the queue and nodeReadForPush to one LinkList
    //private Queue<Integer> executeQueue = new LinkedList<>();
    private Graph g;

    private LinkedList<Integer> liveTaskList  = new LinkedList<>();
    private HashMap<Integer , ArrayList<Integer>> dynamicInAdjacentList = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>> fixInAdjacentList = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>> outAdjacentList = new HashMap<>();

    private final int[][] communicationCosts;


    //This empty Arraylist is used to find the node with no parent nodes
    private final ArrayList<Integer> empty = new ArrayList<>();
    private final Task[] allTasks;
    private final int totalNumberOfNodes;


    public GraphData(Graph g){
        this.g = g;
        this.totalNumberOfNodes = g.getNodeCount();
        allTasks = new Task[this.totalNumberOfNodes];
        communicationCosts = new int[this.totalNumberOfNodes][this.totalNumberOfNodes];
        constructFixInAdjacentList(g);
        constructAdjacentList(g,"Incoming");
        constructAdjacentList(g,"Outgoing");
    }

    /**
     * This method displays the graph which is changed dynamically during runtime
     */
    public StackPane displayGraph() {
        g.setAttribute("ui.stylesheet", "url('css/nodeStyling.css')");

        for (Node node : g) {

            node.setAttribute("StartTime",-1);
            node.setAttribute("Processor",-1);
            node.setAttribute("ui.label", "ID: " + node.getId() + "\nStartTime" + node.getAttribute("StartTime") + "\nProcessor" + node.getAttribute("Processor"));
        }

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FxViewer viewer = new FxViewer(g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        FxViewPanel panel = (FxViewPanel) viewer.addView(FxViewer.DEFAULT_VIEW_ID, new FxGraphRenderer());

        StackPane graphPane = new StackPane();
        graphPane.getChildren().addAll(panel); // prevent UI shift issues

        return graphPane;
    }

    public GraphData(LinkedList<Integer> liveTaskList, int[][] communicationCosts, HashMap<Integer, ArrayList<Integer>> outAdjacentList, HashMap<Integer, ArrayList<Integer>> fixInAdjacentList
    , HashMap<Integer,ArrayList<Integer>> dynamicInAdjacentList, Task[] allTasks, int totalNumberOfNodes) {
        this.liveTaskList = liveTaskList;
        this.communicationCosts = communicationCosts;
        this.outAdjacentList = outAdjacentList;
        this.fixInAdjacentList = fixInAdjacentList;
        this.dynamicInAdjacentList = dynamicInAdjacentList;
        this.allTasks = allTasks;
        this.totalNumberOfNodes = totalNumberOfNodes;
    }

    public void constructFixInAdjacentList(Graph g) {
        for (int i = 0; i < totalNumberOfNodes; i++) {
            Node node = g.getNode(i);
            ArrayList<Integer> sourceNodes = new ArrayList<>();
                node.enteringEdges().forEach(e -> sourceNodes.add(e.getSourceNode().getIndex()));

                this.fixInAdjacentList.put(i, sourceNodes);
            }
        }

    /**
     * This method create both incoming/outgoing Adjacent List for each node
     * and store in a hash map.
     * @param g is a Graph object that contains all the information from .dot file
     * @param order is a String that control the if-statement to decide which list to be created .
     */
    public void constructAdjacentList(Graph g, String order){

        for (int i = 0; i < totalNumberOfNodes; i++){

            Node node = g.getNode(i);
            Task task = new Task (node.getIndex(), ((Double) node.getAttribute("Weight")).intValue());
            ArrayList<Integer> sourceNodes = new ArrayList<>();
            if(order.equals("Incoming")) {
                node.enteringEdges().forEach(e -> {
                    sourceNodes.add(e.getSourceNode().getIndex());
                    communicationCosts[e.getSourceNode().getIndex()][e.getTargetNode().getIndex()] = ((Double)e.getAttribute("Weight")).intValue();
                });

                this.dynamicInAdjacentList.put(i, sourceNodes);
                allTasks[i] = task;
            }
            if(order.equals("Outgoing")){
                node.leavingEdges().forEach(e -> sourceNodes.add(e.getTargetNode().getIndex()));
                this.outAdjacentList.put(i, sourceNodes);
            }
        }
    }

    /**
     * This method can find the key in HashMap by using the value.
     * @param map is the target HashMap.
     * @param value is the value in HashMap for the needed key.
     * @return list a List<> with all the keys that match the value.
     */
    public <E> LinkedList<Integer> getKeysByValue(HashMap<Integer, ArrayList<Integer>> map, E value) {
        Set<Integer> keys = new HashSet<>();
        for (Map.Entry<Integer, ArrayList<Integer>> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return new LinkedList<>(keys);
    }

    /**
     * This method remove the incoming degree for nodes in the Incoming AdjacentList.
     * @param task is a Node object that need to be removed.
     */

   public void removeIncomingEdges(Integer task) {
       ArrayList<Integer> reduceList = this.outAdjacentList.get(task);
       for (int t : reduceList) {
           if(dynamicInAdjacentList.containsKey(t)) {
               this.dynamicInAdjacentList.get(t).remove(task);

           }
       }
   }

    public void addIncomingEdges(Integer taskIndex, LinkedList<Integer> liveTaskList) {
        ArrayList<Integer> childrenTasks = this.outAdjacentList.get(taskIndex);

        for(Integer c : childrenTasks){
            if(this.dynamicInAdjacentList.get(c) == null){
                ArrayList<Integer> addList = new ArrayList<>();
                addList.add(taskIndex);
                this.dynamicInAdjacentList.put(c,addList);
                liveTaskList.removeLast();

            }
            else{
                this.dynamicInAdjacentList.get(c).add(taskIndex);

            }
        }
    }

    /**
     * This method push all the nodes form nodeReadForPush list into the executeQueue.
     */
    public void updateLiveTaskList() {
        LinkedList<Integer>newTaskList = getKeysByValue(dynamicInAdjacentList, empty);
       for(int t : newTaskList) {
           this.dynamicInAdjacentList.remove(t);
           this.liveTaskList.add(t);
       }
    }

    /**
     * This method updates the graph to the current best values
     * @param processor array of processors
     * @param startTime array of start times
     */
    public synchronized void updateGraph(int[] processor, int[] startTime) {
        for (int i = 0; i < g.getNodeCount(); i++) {
            Node task = this.g.getNode(i);
            task.setAttribute("StartTime", startTime[i]);
            task.setAttribute("Processor", processor[i]);
            task.setAttribute("ui.label", "ID: " + task.getId() + "\nWeight: " + task.getAttribute("Weight") + "\nStartTime: " + startTime[i] +
                    "\nProcessor: " + processor[i]);
        }
        try {
            Thread.sleep(0, 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public Task getTask(int taskIndex){
        return this.allTasks[taskIndex];
    }

    public HashMap<Integer, ArrayList<Integer>> getDynamicInAdjacentList(){
        return this.dynamicInAdjacentList;
    }

    public HashMap<Integer, ArrayList<Integer>> getFixInAdjacentList(){
        return this.fixInAdjacentList;
    }

    public HashMap<Integer, ArrayList<Integer>> getOutAdjacentList(){
        return this.outAdjacentList;
    }

    public LinkedList<Integer> getLiveTaskList(){
        return this.liveTaskList;
    }
    public Task[] getAllTasks() {return this.allTasks; }

    public int getTotalNumberOfNodes() { return this.totalNumberOfNodes; }

    public int getCommunicationCost(int source, int target){
        return this.communicationCosts[source][target];
    }

    public int[][] getCommunicationArray() {return this.communicationCosts; }

}

