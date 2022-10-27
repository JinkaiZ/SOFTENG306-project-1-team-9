import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.ArrayList;

public class CheckValidity {


    /**
     * This method checks the validity of the graphs after the schedule has been written to the graph,
     * it checks that all scheduled tasks fulfill their respected start time constraints
     *
     * @param g is a graph object in the state after the created schedule has been written to the graph
     * @return boolean, whether the schedule on the graph is valid or not
     */
    public boolean checkValidity(Graph g) {

        // Loop through all nodes in graph g
        for (Node n : g) {

            ArrayList<Node> sourceNodes = new ArrayList<>();

            // Get all pre-requisite nodes for each node in the graph
            n.enteringEdges().forEach(edge -> {
                sourceNodes.add(edge.getSourceNode());
            });

            // Compare the start time and end time between all nodes and its pre-requisite nodes
            for (Node sourceNode : sourceNodes) {
                // If nodes are on the same processor
                if (sourceNode.getAttribute("Processor") == n.getAttribute("Processor")) {
                    if (((Integer) n.getAttribute("Start")) < (((Integer) sourceNode.getAttribute("Start")) +
                            ((Double) sourceNode.getAttribute("Weight")).intValue())) {
                        return false;
                    }
                }
                // If nodes are not on the same processor
                else {
                    if (((Integer) n.getAttribute("Start")) < (((Integer) sourceNode.getAttribute("Start")) +
                            ((Double) sourceNode.getAttribute("Weight")).intValue() +
                            ((Double) sourceNode.getEdgeBetween(n).getAttribute("Weight")).intValue())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
