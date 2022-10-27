package Scheduler;

public class Task {

    private int id;

    private int startTime;
    private int finishTime;
    private int processNum = -1;
    private int weight;
    public boolean isScheduled;

    public Task(int index, int weight) {
        this.weight = weight;
    }

    public Task(int processNum){
        this.processNum = processNum;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getStartTime() {
        return this.startTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public int getFinishTime() {
        return this.finishTime;
    }

    public int getProcessNum() {
        return this.processNum;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setProcessNum(int num) {
        processNum = num;
    }

    public boolean getIsScheduled() {
        return this.isScheduled;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
