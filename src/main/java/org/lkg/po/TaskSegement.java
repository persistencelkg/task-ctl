package org.lkg.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSegement {
    private Integer id;

    private String taskId;

    private Integer segmentId;

    private String snapshotValue;

    private Integer status;

    /**
     * biz id  子任务正在执行的起点
     */
    private Integer startIndex;

    private Integer endIndex;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}