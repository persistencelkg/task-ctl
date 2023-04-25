package org.lkg.core.ctl.mapper;


import org.apache.ibatis.annotations.*;
import org.lkg.po.TaskPo;

/**
 * @author likaiguang
 * @date 2023/4/11 8:22 下午
 */

public interface TaskMapper {

    String TABLE_NAME = "task";

    String INSERT_KEY = "task_id,service_name,service_key,task_status,task_dimension,initial_snap_shot";

    String INSERT_VALUE = "#{snapshot.taskId},#{snapshot.serviceName},#{snapshot.serviceKey},#{snapshot.taskStatus},#{snapshot.taskDimension},#{snapshot.initialSnapShot}";

    @Insert("insert into " + TABLE_NAME + "(" + INSERT_KEY + ") values(" + INSERT_VALUE + ")")
    int insert(@Param("snapshot") TaskPo taskSnapshotPo);

    @Update("update task set " +
            "task_status=#{snapshot.taskStatus} where task_id =#{snapshot.taskId}")
    int update(@Param("snapshot") TaskPo taskSnapshotPo);


    @Select("select * from task where task_id =#{taskId} and task_status =1")
    TaskPo getWorkingSnapShot(@Param("taskId")String taskId);

    @Delete("delete from task where task_id=#{taskId} and status=2")
    int delete(@Param("taskId") String taskId);
}
