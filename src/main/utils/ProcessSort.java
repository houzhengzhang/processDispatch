package main.utils;

import main.process.ProcessData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @Auther: Administrator
 * @Date: 2018/11/2 15:12
 * @Description:
 */
public class ProcessSort {

    public static void sortByPriority(List<ProcessData> processDataList){
        // 根据优先级排序
        Collections.sort(processDataList, new Comparator<ProcessData>() {
            @Override
            public int compare(ProcessData o1, ProcessData o2) {
                // 数大优先级高
                return o2.getPrio() - o1.getPrio();
            }
        });
    }
    public static void sortByArriveTime(List<ProcessData> processDataList){
        // 根据优先级排序
        Collections.sort(processDataList, new Comparator<ProcessData>() {
            @Override
            public int compare(ProcessData o1, ProcessData o2) {
                // 按到底时间升序排列
                return o1.getArriveTime() - o2.getArriveTime();
            }
        });
    }

    public static void main(String[] args) {
        List<ProcessData> processDataList = new ArrayList<>();
        sortByPriority(processDataList);
        System.out.println(processDataList.remove(0));

        for(ProcessData processData:processDataList){
            System.out.println(processData);
        }
    }


}
