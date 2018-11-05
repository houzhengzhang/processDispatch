package main.controller;

import main.model.BlockProcessTableModel;
import main.model.CurProcessTableModel;
import main.model.InputProcessTableModel;
import main.model.OutputProcessTableModel;
import main.model.ReadyProcessTableModel;
import main.process.ProcessData;
import main.utils.ProcessSort;
import main.utils.ProcessStatus;
import main.view.MainFrame;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: Administrator
 * @Date: 2018/11/2 13:42
 * @Description:
 */
public class MainFrameController {
    private MainFrame mainFrame;
    private JTabbedPane mainTabbedPane;
    private JButton initialBtn;
    private JTextField memoryJFT;
    private JTextField printerJFT;
    private JTextField processNameJFT;
    private JTextField arriveTimeJFT;
    private JTextField serveTimeJFT;
    private JTextField memoryReqJFT;
    private JTextField printerReqJFT;
    private JTextField priorityJFT;
    private JLabel currentTimeJFT;
    private JButton newProcessBtn;
    private JRadioButton selectGrab;
    private JRadioButton selectNoGrab;
    private JButton startBtn;
    private JTable currentProcessTable;
    private JButton blockBtn;
    private JButton pauseBtn;
    private JButton continueBtn;
    private JButton wakeBtn;
    private JButton resetBtn;
    private JButton exitBtn;
    private JTable readyProcessTable;
    private JTable blockProcessTable;
    private JTable inputProcessTable;
    private JTable outputProcessTable;
    private JMenuItem loadMenuItem;
    private JLabel avgTurnTime;
    private JLabel avgWeightTurnTime;
    private JLabel currentMemoryJFT;
    private JLabel currentPrinterJFT;

    // 定义model
    private CurProcessTableModel curProcessTableModel = new CurProcessTableModel();
    private ReadyProcessTableModel readyProcessTableModel = new ReadyProcessTableModel();
    private BlockProcessTableModel blockProcessTableModel = new BlockProcessTableModel();
    private InputProcessTableModel inputProcessTableModel = new InputProcessTableModel();
    private OutputProcessTableModel outputProcessTableModel = new OutputProcessTableModel();

    // 当前时间
    private int currentTime = -1;
    // 就绪队列
    private List<ProcessData> readyProcessQueue = new ArrayList<>();
    // 阻塞队列
    private List<ProcessData> blockProcessQueue = new ArrayList<>();
    // 创建进程暂存队列
    private List<ProcessData> tempProcessQueue = new ArrayList<>();
    // 记录已完成进程
    private List<ProcessData> finishProcessList = new ArrayList<>();

    // 当前执行进程
    private ProcessData currentProcess = null;
    // 内存大小
    private int memorySize = 0;
    // 打印机数目
    private int printerNum = 0;
    // 时钟
    private Timer timer;

    public MainFrameController() {
        mainFrame = new MainFrame();
        initCompoents();
        initListeners();

        // 定义时钟
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 定时器加1
                currentTime += 1;

                currentTimeJFT.setText(String.valueOf(currentTime));
                // 处理到达的进程
                while (tempProcessQueue.size() > 0) {
                    if (tempProcessQueue.get(0).getArriveTime() <= currentTime) {
                        if (selectNoGrab.isSelected()) {
                            // 非抢占式优先级调度
                            ProcessData process = tempProcessQueue.remove(0);
                            noGrabDispatch(process);
                        } else if (selectGrab.isSelected()) {
                            // 抢占式优先级调度
                            ProcessData process = tempProcessQueue.remove(0);
                            grabDispatch(process);
                        }
                    } else break;
                }
                // 当前cpu空闲直接取就绪队列队首进程
                if (currentProcess == null) {
                    if (readyProcessQueue.size() > 0) {
                        dispatchQueueFirst();
                    }
                } else {
                    // 进程执行完毕
                    if (currentProcess.isFinished()) {
                        // 将状态置为完成
                        currentProcess.setStatus(ProcessStatus.FINISH);
                        // 设置完成时间
                        currentProcess.setFinishTime(currentTime);
                        // 记录已完成进程
                        finishProcessList.add(currentProcess);
                        // 归还资源
                        memorySize += currentProcess.getMemoryReq();
                        printerNum += currentProcess.getPrinterReq();

                        currentProcess = null;
                        // 有进程执行完毕 进行进程调度 --》 调度就绪队列队首进程
                        if (readyProcessQueue.size() > 0) {
                            dispatchQueueFirst();
                        }
                    }
                }
                // 当前进程剩余执行时间减一
                if (currentProcess != null)
                    currentProcess.subMoreTime();

                currentMemoryJFT.setText(String.valueOf(memorySize));
                currentPrinterJFT.setText(String.valueOf(printerNum));

                // 更新table
                readyProcessTableModel.setProcessQueue(readyProcessQueue);
                curProcessTableModel.setCurrentProcess(currentProcess);
                currentProcessTable.updateUI();
                readyProcessTable.updateUI();

                // 判断执行完毕
                if (readyProcessQueue.size() == 0 && blockProcessQueue.size() == 0 && currentProcess == null && tempProcessQueue.size() == 0) {
                    JOptionPane.showMessageDialog(null, "所有进程已执行完毕！");
                    timer.stop();
                }
            }
        });
    }

    /**
     * 调度就绪队列队首进程
     *
     * @param
     */
    private void dispatchQueueFirst() {
        if (readyProcessQueue.size() > 0) {
            currentProcess = readyProcessQueue.remove(0);
            // 将状态置为执行
            currentProcess.setStatus(ProcessStatus.RUNNING);
            // 记录开始执行时间
            if (currentProcess.getBeginTime() == -1)
                currentProcess.setBeginTime(currentTime);

        }
    }

    /**
     * 非抢占式优先级进程调度
     *
     * @param process
     */
    private void noGrabDispatch(ProcessData process) {
        // 置为就绪状态
        process.setStatus(ProcessStatus.READY);
        readyProcessQueue.add(process);
        ProcessSort.sortByPriority(readyProcessQueue);

    }

    /**
     * 抢占式优先级进程调度
     *
     * @param process
     */
    private void grabDispatch(ProcessData process) {
        // 比较与现行进程优先级关系
        if (currentProcess != null) {
            // 优先级高于现行进程
            if (process.getPrio() > currentProcess.getPrio()) {
                // 现行进程 -》 就绪队列
                currentProcess.setStatus(ProcessStatus.READY);
                readyProcessQueue.add(currentProcess);
                ProcessSort.sortByPriority(readyProcessQueue);
                // 该进程 抢占cpu
                currentProcess = process;
                // 记录开始时间
                if (currentProcess.getBeginTime() == -1) {
                    currentProcess.setBeginTime(currentTime);
                }
                // 置为执行状态
                currentProcess.setStatus(ProcessStatus.RUNNING);
            } else {
                // 优先级低于现行进程
                // -》 就绪队列
                // 置为就绪状态
                process.setStatus(ProcessStatus.READY);
                readyProcessQueue.add(process);
                ProcessSort.sortByPriority(readyProcessQueue);
            }
        } else {
            currentProcess = process;
            currentProcess.setStatus(ProcessStatus.RUNNING);
            // 记录开始执行时间
            if (currentProcess.getBeginTime() == -1)
                currentProcess.setBeginTime(currentTime);
        }
    }

    private void initListeners() {
        // 创建进程
        newProcessBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String processName = processNameJFT.getText();
                int arrive_time = Integer.parseInt(arriveTimeJFT.getText());
                int serve_time = Integer.parseInt(serveTimeJFT.getText());
                int priority = Integer.parseInt(priorityJFT.getText());
                int memoryReq = Integer.parseInt(memoryReqJFT.getText());
                int printerReq = Integer.parseInt(printerReqJFT.getText());

                if (memoryReq > memorySize) {
                    JOptionPane.showMessageDialog(null, "当前内存空间不足，创建进程失败!");
                    return;
                } else if (printerReq > printerNum) {
                    JOptionPane.showMessageDialog(null, "当前打印机数不足，创建进程失败!");
                    return;
                } else if (arrive_time < currentTime) {
                    JOptionPane.showMessageDialog(null, "到达时间需大于等于当前时间，创建进程失败!");
                    return;
                }

                // 分配资源
                memorySize -= memoryReq;
                printerNum -= printerReq;
                // 创建进程对象
                ProcessData processData = new ProcessData(processName, arrive_time, serve_time, priority, memoryReq, printerReq);
                tempProcessQueue.add(processData);
                // 到达时间排序
                ProcessSort.sortByArriveTime(tempProcessQueue);
                // 添加到输入table
                inputProcessTableModel.addProcess(processData);

                // 创建成功
                JOptionPane.showMessageDialog(null, "创建进程成功!");
                processNameJFT.setText("");
                arriveTimeJFT.setText("");
                serveTimeJFT.setText("");
                memoryReqJFT.setText("");
                printerReqJFT.setText("");
                priorityJFT.setText("");
                currentMemoryJFT.setText(String.valueOf(memorySize));
                currentPrinterJFT.setText(String.valueOf(printerNum));
            }
        });

        // 初始化内存与打印机
        initialBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                memorySize = Integer.parseInt(memoryJFT.getText());
                printerNum = Integer.parseInt(printerJFT.getText());
                currentMemoryJFT.setText(String.valueOf(memorySize));
                currentPrinterJFT.setText(String.valueOf(printerNum));
            }
        });

        // 开始模拟
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.start();
            }
        });

        // 暂停
        pauseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
            }
        });
        // 继续
        continueBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.restart();
            }
        });

        // 阻塞
        blockBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentProcess != null){
                    // 将状态置为阻塞
                    currentProcess.setStatus(ProcessStatus.BLOCKED);
                    blockProcessQueue.add(currentProcess);
                    blockProcessTableModel.setProcessQueue(blockProcessQueue);
                    blockProcessTable.updateUI();
                    currentProcess = null;
                    // 阻塞后当前进程为空 -- 调度队首进程
                    dispatchQueueFirst();
                    curProcessTableModel.setCurrentProcess(currentProcess);
                    currentProcessTable.updateUI();
                }
            }
        });
        wakeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 获取阻塞队列队首进程
                ProcessData process = blockProcessQueue.remove(0);
                if (selectNoGrab.isSelected()) {
                    // 非抢占式优先级调度
                    noGrabDispatch(process);
                } else if (selectGrab.isSelected()) {
                    // 抢占式优先级
                    grabDispatch(process);
                }
                // 重绘
                blockProcessTableModel.setProcessQueue(blockProcessQueue);
                blockProcessTable.updateUI();
            }
        });

        // 重置按钮
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTime = -1;
                readyProcessQueue.clear();
                blockProcessQueue.clear();
                tempProcessQueue.clear();
                finishProcessList.clear();
                currentProcess = null;
                memorySize = 0;
                printerNum = 0;

                memoryJFT.setText("");
                printerJFT.setText("");
                processNameJFT.setText("");
                arriveTimeJFT.setText("");
                serveTimeJFT.setText("");
                memoryReqJFT.setText("");
                printerReqJFT.setText("");
                priorityJFT.setText("");
                currentTimeJFT.setText("0");

                curProcessTableModel.setCurrentProcess(currentProcess);
                readyProcessTableModel.setProcessQueue(readyProcessQueue);
                blockProcessTableModel.setProcessQueue(blockProcessQueue);
                inputProcessTableModel.clearProcess();
                outputProcessTableModel.setProcessQueue(finishProcessList);

                currentMemoryJFT.setText(String.valueOf(memorySize));
                currentPrinterJFT.setText(String.valueOf(printerNum));

                mainFrame.validate();
                mainFrame.repaint();

                // 停止定时器
                timer.stop();

            }
        });
        // 选项卡切换监听事件
        mainTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane tabbedPane = (JTabbedPane) e.getSource();
                int selectedIndex = tabbedPane.getSelectedIndex();
                // 选择结果panel
                if (selectedIndex == 1) {
                    outputProcessTableModel.setProcessQueue(finishProcessList);
                    // 重绘
                    inputProcessTable.updateUI();
                    outputProcessTable.updateUI();
                    // 周转时间
                    double tunrTime = 0;
                    // 带权周转时间
                    double weightTurnTime = 0;
                    for (ProcessData p : finishProcessList) {
                        tunrTime += p.getFinishTime() - p.getArriveTime();
                        weightTurnTime += (p.getFinishTime() - p.getArriveTime()) * 1.0 / p.getNeedTime();
                    }
                    if (finishProcessList.size() > 0) {
                        tunrTime /= finishProcessList.size();
                        weightTurnTime /= finishProcessList.size();
                    }

                    avgTurnTime.setText(String.valueOf(Math.round(tunrTime * 100) / 100.0));
                    avgWeightTurnTime.setText(String.valueOf(Math.round(weightTurnTime * 100) / 100.0));
                }
            }
        });
        loadMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.setDialogTitle("打开");

                FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files ", "txt");
                fileChooser.setFileFilter(filter);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);

                if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                    File path = fileChooser.getSelectedFile();
                    BufferedReader bufferedReader = null;
                    try {
                        bufferedReader = new BufferedReader(new FileReader(path));
                        String tempString;
                        while ((tempString = bufferedReader.readLine()) != null) {
                            String[] processMsg = tempString.split(" ");
                            // 创建进程对象
                            ProcessData processData = new ProcessData(processMsg[0], Integer.parseInt(processMsg[1]),
                                    Integer.parseInt(processMsg[2]), Integer.parseInt(processMsg[3]), Integer.parseInt(processMsg[4]), Integer.parseInt(processMsg[5]));
                            tempProcessQueue.add(processData);
                            memorySize -= Integer.parseInt(processMsg[4]);
                            printerNum -= Integer.parseInt(processMsg[5]);
                            // 到达时间排序
                            ProcessSort.sortByArriveTime(tempProcessQueue);
                            // 添加到输入table
                            inputProcessTableModel.addProcess(processData);

                            currentMemoryJFT.setText(String.valueOf(memorySize));
                            currentPrinterJFT.setText(String.valueOf(printerNum));

                        }
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
            }
        });

        exitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.dispose();
                System.exit(0);
            }
        });
    }

    private void initCompoents() {
        mainTabbedPane = mainFrame.getMainTabbedPane();
        initialBtn = mainFrame.getInitialBtn();
        memoryJFT = mainFrame.getMemoryJFT();
        printerJFT = mainFrame.getPrinterJFT();
        processNameJFT = mainFrame.getProcessNameJFT();
        arriveTimeJFT = mainFrame.getSubmitTimeJFT();
        serveTimeJFT = mainFrame.getServeTimeJFT();
        memoryReqJFT = mainFrame.getMemoryReqJFT();
        printerReqJFT = mainFrame.getPrinterReqJFT();
        newProcessBtn = mainFrame.getNewProcessBtn();
        selectGrab = mainFrame.getGrabRadioBtnButton2();
        selectNoGrab = mainFrame.getNoGrabRadioBtn();
        startBtn = mainFrame.getStartBtn();
        currentProcessTable = mainFrame.getCurrentProcessTable();
        blockBtn = mainFrame.getBlockBtn();
        pauseBtn = mainFrame.getPauseBtn();
        continueBtn = mainFrame.getContinueBtn();
        readyProcessTable = mainFrame.getReadyProcessTable();
        blockProcessTable = mainFrame.getBlockProcessTable();
        wakeBtn = mainFrame.getWakeBtn();
        resetBtn = mainFrame.getResetBtn();
        exitBtn = mainFrame.getExitBtn();
        priorityJFT = mainFrame.getPriorityJFT();
        currentTimeJFT = mainFrame.getCurrentTimeJFT();
        inputProcessTable = mainFrame.getInputProcessTable();
        outputProcessTable = mainFrame.getOutputProcessTable();
        loadMenuItem = mainFrame.getLoadMenuItem();
        avgTurnTime = mainFrame.getAvgTurnTime();
        avgWeightTurnTime = mainFrame.getAvgWeightTurnTime();
        currentMemoryJFT = mainFrame.getCurrentMemoryJFT();
        currentPrinterJFT = mainFrame.getCurrentPrinterJFT();

        // 设置 当前进程 model
        setTableModel(currentProcessTable, curProcessTableModel);

        // 设置就绪队列 table model
        setTableModel(readyProcessTable, readyProcessTableModel);

        // 设置阻塞队列 table model
        setTableModel(blockProcessTable, blockProcessTableModel);

        // 设置输入队列 table model
        setTableModel(inputProcessTable, inputProcessTableModel);

        // 设置输出队列 table model
        setTableModel(outputProcessTable, outputProcessTableModel);
    }

    public void showMainFrameWindow() {
        mainFrame.setVisible(true);
    }

    private void setTableModel(JTable table, TableModel tableModel) {
        // 设置model
        table.setModel(tableModel);
        // 设置居中显示
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();//单元格渲染器
        tcr.setHorizontalAlignment(JLabel.CENTER);//居中显示
        table.setDefaultRenderer(Object.class, tcr);//设置渲染器
        // 设置table行宽
        table.setRowHeight(17);
        // 设置JTable的内容的字体大小
        table.setFont(new Font("Menu.font", Font.PLAIN, 15));
    }


}
