package Scheduler;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkDOT;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDOT;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class IOParser {

    /**
     * This method reads the graph in the .dot file format and converts it into a graph data structure
     * @param path is a string that contains the location of the .dot file
     * @return Graph, a data structure defined by graphstream
     */
    public Graph readDotFile(String path) throws IOException {
        Graph g = new MultiGraph("g");
        FileSource fs = new FileSourceDOT();
        fs.addSink(g);

        try {
            fs.readAll(path);
        }
        catch (IOException e) {
            throw new IOException("Error reading the graph file");
        }
        finally {
            fs.removeSink(g);
        }

        return g;
    }

    /**
     * This method writes the graph to a .dot file called solution
     * @param outputFileName, the name of the output .dot file
     * @param g, the graph data structure defined by graph stream
     */
    public void writeDotFile(Task[] optimalSchedule, String outputFileName, Graph g) throws IOException {
        Graph gCopy = Graphs.clone(g);

        gCopy.removeAttribute("ui.stylesheet");

        for(Node n : gCopy) {
            n.removeAttribute("ui.label");
            n.removeAttribute("StartTime");
            n.removeAttribute("Processor");
        }

        for(int i = 0; i < optimalSchedule.length; i++) {
            Node node = gCopy.getNode(i);
            Task task = optimalSchedule[i];

            node.setAttribute("Start", task.getStartTime());
            node.setAttribute("Processor", task.getProcessNum());
        }

        FileSink f = new FileSinkDOT(true);

        try {
            f.writeAll(gCopy, outputFileName);
        }
        catch(IOException e) {
            throw new IOException("Error writing to .dot file");
        }

        Path filePath = Paths.get(outputFileName);
        List<String> lines = Files.readAllLines(filePath);
        lines.set(0, "digraph " + "\"" + outputFileName.substring(0, outputFileName.length()-4) + "\""+ " {");
        Files.write(filePath, lines);
    }
}
