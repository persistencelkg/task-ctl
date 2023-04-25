package org.lkg.core.ctl;

import org.lkg.util.TaskUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;

public interface TaskCtlService {

    Logger log = LoggerFactory.getLogger(TaskCtlService.class);
    String LINE = "->";

    /**
     * 本脚手架不提供具体实现，因为每个developer都有自己的梦想
     * @return 自定义线程池
     */
    ExecutorService executorService();

    /**
     * 当前是否是混合业务，改值对线程任务数有直接影响
     * @return 是混合业务
     */
    default boolean isMixedBiz() {
        return true;
    }

    /**
     * 根据业务高峰期 & 根据自身服务特征决定，执行任务的线程数
     * @return
     */
    default Integer getThreadCount() {
        return getDynamicConcurrentThreadNum(isMixedBiz());
    }

    /**
     * 定制化业务的高峰期
     * @return
     */
    default String getBizPeek() {
        return String.join(LINE,"6",  "21");
    }

    /**
     * 如果任务纬度TaskDimensionEnum 是时间，则决定快照间隔
     * @return
     */
    default Duration getDuration() {
        return TaskUtil.buildTaskPeriod(getPeriod());
    }

    default String getPeriod(){
        return null;
    }

    /**
     * 批量任务大小
     * @return
     */
    default Integer getBatchSize() {
        return 500;
    }

    /**
     * 任务执行时间
     * @return 默认1秒
     */
    default long getSleepTime() {
        return 1000L;
    }


    boolean isRun();

    /**
     * 获取动态运行的并发线程数
     *
     * @return
     */
    default int getDynamicConcurrentThreadNum(boolean isMixedBiz) {
        int count = Runtime.getRuntime().availableProcessors();
        if (!isMixedBiz) {
            // experience count
            return (int) Math.floor(count * 1.25);
        }
        String bizPeek = getBizPeek();
        int half = count >> 1;
        if (ObjectUtils.isEmpty(bizPeek)) {
            log.warn("job run with out biz peek");
            return half;
        }
        String[] split = bizPeek.split(LINE);
        try {
            int start = Integer.parseInt(split[0]);
            int end = Integer.parseInt(split[1]);
            if (start >= end) {
                return half;
            }
            int hour = LocalTime.now().getHour();
            if (start <= hour && hour <= end) {
                return half;
            }
            // experience count
            return (int) Math.ceil(count * 0.75);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return half;
    }
}
