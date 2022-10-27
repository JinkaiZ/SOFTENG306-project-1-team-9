package Scheduler;

import Controllers.MenuController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.cli.*;
import org.graphstream.graph.Graph;

import java.io.IOException;

public class Main extends Application {

    private static String inputFileName;
    private static String outputFileName;
    private static int proNum;
    private static int coreNum;
    private static Graph g;
    private static Graph gCopy;
    private static GraphData graphOp;
    private static int[] upperBoundAndLoadBalance;
    private static String mode = "d";
    private static boolean visualise = false;
    private static MenuController controller;

    private static String searchCount;
    private static String timeElapsed;
    private static String endTime;

    private static Service<Void> backgroundThread;
    public static Task[] optimalSchedule;

    /**
     * This method is where the algorithm is run and our solution is generated into a .dot file.
     * An example run should be java −jar scheduler.jar INPUT.dot P with the following options
     * -p N, -v, -o OUTPUT
     */
    public static void main(String[] args) throws IOException, ParseException, IllegalArgumentException, InterruptedException {
        System.setProperty("org.graphstream.ui", "javafx");

        if(args.length < 2) {
            throw new IllegalArgumentException("Needs at least 2 arguments provided");
        }

        CommandLine cmd = createOptions(args);

        inputFileName = args[0];

        try {
            proNum = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException e) {
            throw new NumberFormatException("Passed invalid number of processes");
        }

        outputFileName = getOutputFileName(cmd);

        IOParser parser = new IOParser();
        g = parser.readDotFile(inputFileName);
        gCopy = parser.readDotFile(inputFileName);

        GraphData graph = new GraphData(g);
        graphOp = new GraphData(g);
        ListScheduling listScheduling = new ListScheduling();

        upperBoundAndLoadBalance = listScheduling.greedyAlgorithm(graph, proNum);

        // Visualise
        if(cmd.hasOption("v")) {
            visualise = true;
        }

        // Parallel
        if(cmd.hasOption("p")) {
            mode = "p";
            try {
                coreNum = Integer.parseInt(cmd.getOptionValue("p"));
            }
            catch(NumberFormatException e) {
                throw new NumberFormatException("Passed invalid number of cores for allocation");
            }
        }
        // A Star
        else if (cmd.hasOption("a")) {
            mode = "a";
        }
        // Sequential
        else {}

        if (visualise) {
            launch(args);
        } else {
            startScheduler();
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Menu.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);

        primaryStage.setTitle("Scheduler");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void startScheduler() throws IOException {

        IOParser parser = new IOParser();

        // Visualise
        if (visualise) {
            controller.disableGraphButton();
            StackPane pane = graphOp.displayGraph();
            controller.enableGraphButton();
            controller.showMainPane();
            controller.setMainPane(pane);
        }

        backgroundThread = new Service<Void>() {
            @Override
            protected javafx.concurrent.Task<Void> createTask() {
                return new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {

                        // Parallel
                        if(mode == "p") {
                            DFAlgorithmParallel test = new DFAlgorithmParallel(graphOp);
                            optimalSchedule = test.run(coreNum, proNum, upperBoundAndLoadBalance[0], upperBoundAndLoadBalance[1]);
                        }
                        // A Star
                        else if (mode == "a") {
                            AStarAlgorithm test = new AStarAlgorithm();
                            optimalSchedule = test.run(g, gCopy, graphOp, proNum);
                        }
                        // Sequential
                        else {
                            DFAlgorithm test = new DFAlgorithm();
                            optimalSchedule = test.run(graphOp, proNum, upperBoundAndLoadBalance[0], upperBoundAndLoadBalance[1]);
                        }

                        return null;
                    }
                };
            }
        };

        Timeline timeline = new Timeline();

        if (visualise) {
            timeline = new Timeline(
                    new KeyFrame(Duration.seconds(2.0), e -> {
                        controller.setSearchCount(searchCount);
                        controller.setTimeElapsed(timeElapsed);
                        controller.setEndTime(endTime);
                    })
            );
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }

        Timeline finalTimeline = timeline;
        backgroundThread.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                try {
                    parser.writeDotFile(optimalSchedule, outputFileName, g);
                    if (visualise) {
                        controller.finalResult = optimalSchedule;
                        controller.displayChart();
                        finalTimeline.stop();
                        controller.setSearchCount(searchCount);
                        controller.setTimeElapsed(timeElapsed);
                        controller.setEndTime(endTime);
                    } else {
                        System.exit(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        backgroundThread.restart();
    }

    /**
     * This method creates the three options for the command line to use. These three are:
     * -p N: use N cores for execution in parallel (default is sequential)
     * -v : visualize the search
     * -o OUTPUT: output file is named OUTPUT (default is INPUT−output.dot)
     * @param args, the command line arguments
     * @return the command line with the added options if it was specified in args
     */
    public static CommandLine createOptions(String[] args) throws ParseException {
        Options options = new Options();
        Option p = new Option("p", true, "core number");
        p.setRequired(false);
        options.addOption(p);

        Option v = new Option("v", false, "visualize search");
        v.setRequired(false);
        options.addOption(v);

        Option o = new Option("o", true, "output file name");
        o.setRequired(false);
        options.addOption(o);

        Option a = new Option("a", false, "use a star algorithm");
        a.setRequired(false);
        options.addOption(a);

        CommandLineParser parser = new DefaultParser();

        try {
            return parser.parse(options, args);
        }
        catch (ParseException e) {
            throw new ParseException("Could not parse options");
        }
    }

    /**
     * This method gets the output file name
     * @param cmd, the CommandLine object with some parameters from String[] args
     * @return a string which is the outputFileName.
     */
    public static String getOutputFileName(CommandLine cmd) {

        String o = cmd.getOptionValue("o",
                inputFileName.substring(0, inputFileName.length()-4) + "-output.dot");

        return o.endsWith(".dot") ? o : o + ".dot";
    }

    public static int getProNum() {
        return proNum;
    }

    public static Graph getG() {
        return g;
    }

    public static void setSearchCount(String count) {
        searchCount = count;
    }

    public static void setTimeElapsed(String time) {
        timeElapsed = time;
    }

    public static void setEndTime(String time) {
        endTime = time;
    }

    public static String getEndTime() {
        return endTime;
    }
}
