package org.lkg.core.ctl;


import org.lkg.core.ctl.mapper.TaskMapper;
import org.lkg.core.ctl.mapper.TaskSegementMapper;
import org.lkg.po.TaskPo;
import org.lkg.po.TaskSegement;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author likaiguang
 * @date 2023/4/18 11:48 上午
 */
@Service
public class TaskService{

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskSegementMapper taskSegementMapper;

    public TaskPo getWorkingSnapShot(String taskId) {
        return taskMapper.getWorkingSnapShot(taskId);
    }

    public void saveSnapshot(TaskPo taskPo) {
        taskMapper.insert(taskPo);
    }

    public void saveSegment(List<TaskSegement> taskSegmentList) {
        taskSegementMapper.insertTaskSegments(taskSegmentList);
    }

    public void updateTask(TaskPo taskPo) {
        taskMapper.update(taskPo);
    }

    public void updateTaskSegment(TaskSegement taskSegement) {
        taskSegementMapper.updateByTaskId(taskSegement);
    }


    public List<TaskSegement> listSegmentWithOrder(String taskId) {
        List<TaskSegement> taskSegements = taskSegementMapper.listSegmentWithTaskId(taskId);
        if (ObjectUtils.isEmpty(taskSegements)) {
            return null;
        }
        // 获取自己的内容
        return taskSegements;
    }
}
