package org.lkg.core.ctl.mapper;


import org.lkg.po.TaskSegement;

import java.util.List;

public interface TaskSegementMapper {

    List<TaskSegement> listSegmentWithTaskId(String taskId);

    int updateByTaskId(TaskSegement record);

    int insertTaskSegments(List<TaskSegement> list);
}