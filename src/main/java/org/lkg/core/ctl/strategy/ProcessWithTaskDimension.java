package org.lkg.core.ctl.strategy;


import org.lkg.po.TaskPo;
import org.lkg.po.TaskSegement;

import java.util.List;

public interface ProcessWithTaskDimension {

    boolean isFinish(TaskSegement lastSegment, TaskPo.InitialSnapShot initialSnapShot);


    List<TaskSegement> splitTask(String taskId, TaskPo.InitialSnapShot initialSnapShot);


    void doTask(String taskId, TaskPo.InitialSnapShot initialSnapShot);

}
