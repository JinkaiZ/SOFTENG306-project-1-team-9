package Controllers;

import Scheduler.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.graphstream.graph.Graph;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    @FXML
    private Button startButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button graphButton;

    @SuppressWarnings("rawtypes")
    @FXML
    private StackedBarChart stackedBarChart;

    @FXML
    private Label searchCount;
    @FXML
    private Label timeElapsed;
    @FXML
    private Label endTime;
    @FXML
    private BorderPane mainPane;

    @FXML
    public Task[] finalResult;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    @FXML
    private void startScheduler() throws IOException {

        startButton.setDisable(true);
        mainPane.setVisible(false);

        Main.startScheduler();
    }

    public void displayChart() {

        int numOfProcessors = Main.getProNum();

        int[] timeTracker = new int[numOfProcessors];
        for (int i = 0; i < numOfProcessors; i++) {
            timeTracker[i] = 0;
        }

        for (int i = 0; i < finalResult.length; i++) {
            finalResult[i].setId(i);
        }

        Arrays.sort(finalResult, Comparator.comparingInt(Task::getProcessNum).thenComparingInt(Task::getStartTime));

        stackedBarChart.getXAxis().setAnimated(false);
        stackedBarChart.getYAxis().setAnimated(false);
        stackedBarChart.setLegendVisible(false);

        XYChart.Series<String, Number> fillSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> taskSeries = new XYChart.Series<>();

        for (int i = 0; i < finalResult.length; i++) {
            int processorNum = finalResult[i].getProcessNum();

            if (finalResult[i].getStartTime() != timeTracker[processorNum]) {
                //noinspection rawtypes
                fillSeries.getData().add(new XYChart.Data(finalResult[i].getStartTime() - timeTracker[processorNum], Integer.toString(processorNum)));
            }

            Graph g = Main.getG();

            StackPane node = new StackPane();
            Label label = new Label(g.getNode(finalResult[i].getId()).getId());
            label.setStyle("-fx-font-size: 30px; -fx-text-fill: white");
            Group group = new Group(label);
            node.getChildren().add(group);

            //noinspection rawtypes
            XYChart.Data data = new XYChart.Data(finalResult[i].getFinishTime() - finalResult[i].getStartTime(), Integer.toString(finalResult[i].getProcessNum()));
            data.setNode(node);

            taskSeries.getData().add(data);
            timeTracker[processorNum] = finalResult[i].getFinishTime();
        }
        stackedBarChart.getData().addAll(fillSeries, taskSeries);

        for(Node n : stackedBarChart.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: white;");
        }

        for(Node n : stackedBarChart.lookupAll(".default-color1.chart-bar")) {
            n.setStyle("-fx-bar-fill: #2f4b7c;");
        }
    }

    public void setSearchCount(String searchCount) {
        this.searchCount.setText(searchCount);
    }

    public void setTimeElapsed(String timeElapsed) {
        this.timeElapsed.setText(timeElapsed);
    }

    public void setEndTime(String endTime) {
        this.endTime.setText(endTime);
    }

    @FXML
    private void closeMenu() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        System.exit(0);
    }

    public void setMainPane(StackPane pane) {
        this.mainPane.setCenter(pane);
    }

    @FXML
    public void hideMainPane() {
        this.mainPane.setVisible(false);
    }

    @FXML
    public void showMainPane() {
        this.mainPane.setVisible(true);
    }

    public void disableGraphButton() {
        graphButton.setDisable(true);
    }

    public void enableGraphButton() {
        graphButton.setDisable(false);
    }
}
