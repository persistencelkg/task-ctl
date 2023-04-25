package org.lkg.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * @author likaiguang
 * @date 2023/4/11 5:22 下午
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskPo {

    private Long id;

    private String taskId;

    /**
     * 扩展 跨业务平台
     */
    private String serviceName;
    /**
     * 扩展
     */
    private String serviceKey;

    /**
     * 任务整体运行状态
     */
    private Integer taskStatus;

    /**
     * 任务执行纬度
     */
    private Integer taskDimension;
    /**
     * segment 总信息
     */
    private String initialSnapShot;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    /**
     * 静态初始快照，一旦确定 一定得所有的子任务完成才能更改
     */
    @Data
    public static class InitialSnapShot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Collection<?> dataList;
        private Integer minId;
        private Integer maxId;
        /**
         * DS为es index为具体的索引名   ds为数据库时 为分表起始id
         */
        private Object index;
        private String ds;
    }


}
