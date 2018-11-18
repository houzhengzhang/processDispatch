processDispatch

支持多个进程并发运行的进程调度模拟系统 - 基于Swing实现

- 1．设计目的
  - 设计一个模拟进程调度的系统。
  - 理解进程控制块的结构。
  - 理解各种进程调度算法。

- 2．设计要求
  - 进程控制块
      每个进程有一个进程控制块PCB，内容包括：  
      private String name;          //进程名  
      private int id=0;             //进程标识数  
      private int arriveTime;       //到达时间  
      private int needTime;         //服务时间  
      private int prio;             //进程优先数，数字越大优先级越高  
      private int moreTime;         //剩余执行的时间  
      private int beginTime;        // 开始时间  
      private int finishTime;       // 完成时间  
      private ProcessStatus status; //进程状态   4种状态：执行--0、就绪--1、阻塞--2、完成--3  
  - 进程状态  
      每个进程有4种状态：RUNNING,READY,BLOCKED,FINISH
  - 进程队列  
      运行队列、就绪队列、阻塞队列和完成队列。
  - 调度算法  
      非抢占式优先级调度算法和抢占式优先级调度算法
  - 输入输出界面  
![run.png](https://github.com/houzhengzhang/processDispatch/blob/master/img/run.png)
![result.png](https://github.com/houzhengzhang/processDispatch/blob/master/img/result.png)
