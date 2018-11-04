package main.process;

import main.utils.ProcessStatus;

/**
 * @Auther: Administrator
 * @Date: 2018/11/2 14:49
 * @Description:
 */
public class ProcessData {
    private String name;//进程名
    private int ID;//进程标识数
    private int arriveTime;//到达时间
    private int needTime;//服务时间
    private int prio;//进程优先数，数字越大优先级越高
    private int moreTime;    //剩余执行的时间
    private int beginTime;    // 开始时间
    private int finishTime;  // 完成时间
    private ProcessStatus status;  //进程状态   4种状态：执行--0、就绪--1、阻塞--2、完成--3

    // 请求内存大小
    private int memoryReq;
    // 请求打印机数
    private int printerReq;

    public ProcessData(String name, int arriveTime, int needTime, int prio, int memoryReq, int printerReq) {
        this.name = name;
        this.arriveTime = arriveTime;
        this.needTime = needTime;
        this.prio = prio;
        this.memoryReq = memoryReq;
        this.printerReq = printerReq;
        moreTime = needTime;
        // 初始状态
        beginTime = -1;
    }

    public int getPrio() {
        return prio;
    }

    public int getArriveTime() {
        return arriveTime;
    }

    public String getName() {
        return name;
    }

    public int getNeedTime() {
        return needTime;
    }

    public int getMoreTime() {
        return moreTime;
    }

    public int getMemoryReq() {
        return memoryReq;
    }

    public int getPrinterReq() {
        return printerReq;
    }

    public int getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(int beginTime) {
        this.beginTime = beginTime;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public void subMoreTime(){
        moreTime -=1;
    }

    public boolean isFinished(){
        if(moreTime <=0)
            return true;
        else
            return false;
    }

    public String toString(){
        return "name: " + name + " arriveTime: " + arriveTime +  "  needTime: " + needTime + " sortByPriority: " + prio + " moreTime: " + moreTime;
    }
}
