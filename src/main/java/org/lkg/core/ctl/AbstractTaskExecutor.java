package org.lkg.core.ctl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.extern.slf4j.Slf4j;
import org.lkg.core.service.DingDingService;
import org.lkg.po.TaskPo;
import org.lkg.po.TaskSegement;
import org.lkg.po.enums.TaskDimensionEnum;
import org.lkg.po.enums.TaskStatusEnum;
import org.lkg.util.JacksonUtil;
import org.lkg.util.TaskUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author likaiguang
 * @date 2023/4/11 7:33 下午
 */
@Slf4j
public abstract class AbstractTaskExecutor<T> implements TaskSnapshotProducer<T>, TaskCtlService, DingDingService {

    @Resource
    private TaskService taskService;

    @Value("${spring.application.name:'lkg'}")
    private String applicationName;

    @Value("${spring.profiles.active}")
    private String env;

    public void run() {
        try {
            log.info("ctl start ---->");
            buildTask();
            log.info("ctl finished ---->");
        } catch (Exception e) {
            log.error("ctl error ---->", e);
        }
    }

    @Override
    public String getEnv() {
        return env;
    }

    public boolean isNegative(Object num) {
        if (Objects.isNull(num)) {
            return false;
        }
        if (num instanceof Integer) {
            return (Integer) num > 0;
        }
        if (num instanceof Long) {
            return (Long) num > 0L;
        }
        return false;
    }

    public void prepareBuildTask(TaskDimensionEnum taskDimensionEnum) {
        Assert.notNull(taskDimensionEnum, "you not point at task split diemnsion such as:" + Arrays.toString(TaskDimensionEnum.values()));
        Assert.isTrue(isNegative(this.getSleepTime()), "each split task must have valid milliseconds sleep time");
        Assert.isTrue(isNegative(this.getBatchSize()) && isNegative(this.getThreadCount()), "each split task must have valid batch size");
        if (TaskDimensionEnum.isTimeRange(taskDimensionEnum)) {
            // 校验执行频率
            Assert.notNull(this.getDuration(), "you choose time range to split task， must point at duration ");
        }
    }

    private void buildTask() {
        String simpleName = this.getClass().getSimpleName();
        // 获取进行中的快照
        if (existWorkingSnapshot(simpleName)) {
            return;
        }
        TaskPo taskjob;
        T param = getParam();
        Assert.notNull(param, "not execute param not support save snapshot");
        taskjob = new TaskPo();
        taskjob.setTaskId(simpleName);
        taskjob.setServiceName(applicationName);
        taskjob.setServiceKey("lkg nb");
        taskjob.setTaskStatus(TaskStatusEnum.WORKING.getCode());

        TaskPo.InitialSnapShot taskSnapShot = generateGlobalTask(param);
        taskjob.setInitialSnapShot(JacksonUtil.writeValue(taskSnapShot));
        TaskDimensionEnum instance = TaskDimensionEnum.getInstance(taskSnapShot);
        // 预校验
        prepareBuildTask(instance);
        taskjob.setTaskDimension(instance.getDimension());

        // 保存全局快照
        taskService.saveSnapshot(taskjob);
        log.info("generate global snapshot:{} --->", taskSnapShot);
        // 生产所有子任务
        List<TaskSegement> taskSegementList = buildSegmentTaskWithDimension(simpleName, taskSnapShot, instance);
        taskService.saveSegment(taskSegementList);
        // 执行任务
        doTask(taskSegementList, instance);
        // 尝试完结任务
        tryFinishJob(taskjob, taskSnapShot, instance, taskSegementList.get(taskSegementList.size() - 1));
    }

    private boolean existWorkingSnapshot(String simpleName) {
        TaskPo taskjob = taskService.getWorkingSnapShot(simpleName);
        List<TaskSegement> segmentList = null;
        if (!ObjectUtils.isEmpty(taskjob)) {
            TaskPo.InitialSnapShot initialSnapShot = JacksonUtil.readValue(taskjob.getInitialSnapShot(), TaskPo.InitialSnapShot.class);
            TaskDimensionEnum instance = TaskDimensionEnum.getInstance(initialSnapShot);
            // 获取所有快照
            segmentList = taskService.listSegmentWithOrder(taskjob.getTaskId());
            if (!ObjectUtils.isEmpty(segmentList)) {
                //获取属于自己操作部分
                // XxlJobHelper.getShardIndex(), XxlJobHelper.getShardTotal()
                List<TaskSegement> belongOwn = TaskUtil.list(segmentList, 1, 2);
                if (!ObjectUtils.isEmpty(belongOwn)) {
                    List<TaskSegement> workingTaskSegment = belongOwn.stream().filter(ref -> TaskStatusEnum.WORKING.getCode().equals(ref.getStatus())).collect(Collectors.toList());
                    if (ObjectUtils.isEmpty(workingTaskSegment)) {
                        //获取最后一个
                        TaskSegement taskSegement = belongOwn.get(belongOwn.size() - 1);
                        tryFinishJob(taskjob, initialSnapShot, instance, taskSegement);
                        return true;
                    }
                    dingInfoLog(MessageFormat.format("读取快照个数：{0} 上次执行位置:{1}", workingTaskSegment.size(), workingTaskSegment.get(0)));
                    doTask(workingTaskSegment, instance);
                    return true;
                }
            } else {
                log.warn("you select snapshot but not split task，so do nothing");
                return true;
            }
        }
        return false;
    }

    private void tryFinishJob(TaskPo taskjob, TaskPo.InitialSnapShot initialSnapShot, TaskDimensionEnum instance, TaskSegement taskSegement) {
        boolean flag = judgeTaskFinish(initialSnapShot, instance, taskSegement);
        if (flag) {
            dingErrorLog(MessageFormat.format("job:{0}，global task has finished", taskjob.getTaskId(), XxlJobHelper.getShardIndex()));
            taskjob.setTaskStatus(TaskStatusEnum.FINISHED.getCode());
            taskService.updateTask(taskjob);
        } else {
            log.info("machine {} segment task has finished, global task not finish", XxlJobHelper.getShardIndex());
        }
    }

    private boolean judgeTaskFinish(TaskPo.InitialSnapShot initialSnapShot, TaskDimensionEnum instance, TaskSegement taskSegement) {
        if (!TaskStatusEnum.FINISHED.getCode().equals(taskSegement.getStatus())) {
            return false;
        }
        if (instance == TaskDimensionEnum.BIZ_ID) {
            ArrayList<?> dataList = new ArrayList<>(initialSnapShot.getDataList());
            Object lastElement = dataList.get(dataList.size() - 1);
            return lastElement.equals(dataList.get(taskSegement.getEndIndex()));
        } else if (instance == TaskDimensionEnum.TIME_RANGE) {
            return initialSnapShot.getEndTime().equals(taskSegement.getEndTime());
        } else if (instance == TaskDimensionEnum.TIME_RANGE_WITH_INDEX) {
            return initialSnapShot.getEndTime().equals(taskSegement.getEndTime()) && initialSnapShot.getMaxId().equals(taskSegement.getEndIndex());
        }
        return false;
    }

    private List<TaskSegement> buildSegmentTaskWithDimension(String taskId, TaskPo.InitialSnapShot initialSnapShot, TaskDimensionEnum instance) {
        List<TaskSegement> list = new ArrayList<>();
        if (TaskDimensionEnum.TIME_RANGE == instance) {
            // 根据频率切分
            list = TaskUtil.list(taskId, initialSnapShot.getStartTime(), initialSnapShot.getEndTime(), this.getDuration());
        } else if (TaskDimensionEnum.BIZ_ID == instance) {
            // batchSize 和 线程数 互斥
            Integer batchSize = this.getBatchSize();
            ArrayList<?> dataList = new ArrayList<>(initialSnapShot.getDataList());
            if (Objects.isNull(batchSize) || batchSize <= 0) {
                Integer threadCount = this.getThreadCount();
                batchSize = dataList.size() / threadCount;
            }
            log.info("批量大小：{}", batchSize);
            list = TaskUtil.list(taskId, dataList, batchSize);
        } else if (TaskDimensionEnum.TIME_RANGE_WITH_INDEX == instance) {
            List<TaskUtil.MinStartEnd> timeRangeList = TaskUtil.list(initialSnapShot.getStartTime(), initialSnapShot.getEndTime(), this.getDuration());
            list = new ArrayList<>();
            for (TaskUtil.MinStartEnd val : timeRangeList) {
                List<TaskSegement> tempList = TaskUtil.list(taskId, initialSnapShot.getMinId(), initialSnapShot.getMaxId(), getDynamicConcurrentThreadNum(isMixedBiz()));
                tempList.forEach(temp -> {
                    temp.setStartTime(val.getStart());
                    temp.setEndTime(val.getEnd());
                });
                list.addAll(tempList);
            }
        }
        // 根据机器实例划分
        // XxlJobHelper.getShardIndex(), XxlJobHelper.getShardTotal()
        return TaskUtil.list(list, 0, 2);
    }


    private void doTask(List<TaskSegement> workingTaskSegment, TaskDimensionEnum instance) {
        int concurrentThreadNum = Objects.nonNull(this.getThreadCount()) ? this.getThreadCount() : getDynamicConcurrentThreadNum(isMixedBiz());
        // n个 任务 分配给 m个线程
        int batchCount = workingTaskSegment.size() / concurrentThreadNum;
        if (workingTaskSegment.size() % concurrentThreadNum != 0) {
            batchCount++;
        }
        int threadCount = concurrentThreadNum;
        ExecutorService executorService = executorService();
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < batchCount; i++) {
            if (i == batchCount - 1) {
                threadCount = workingTaskSegment.size() % concurrentThreadNum;
            }
            CountDownLatch countDownLatch = new CountDownLatch(threadCount);

            for (int j = 0; j < threadCount; j++) {
                TaskSegement taskSegement = workingTaskSegment.get(count.getAndIncrement());
                // 停止具体的子任务
                if (!isRun()) {
                    dingErrorLog(MessageFormat.format("快照：{0} 手动停止", taskSegement));
                    return;
                }
                try {
                    executorService.execute(() -> {
                        executeTask(instance, taskSegement);
                        countDownLatch.countDown();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    countDownLatch.countDown();
                    dingErrorLog(MessageFormat.format("快照：{0} 执行出现异常：{1}", taskSegement, e));
                }

            }
            try {
                countDownLatch.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void executeTask(TaskDimensionEnum instance, TaskSegement taskSegement) {
        dingInfoLog(MessageFormat.format("开始执行快照：{0}", taskSegement));
        if (instance == TaskDimensionEnum.BIZ_ID) {
            doAnything(JacksonUtil.readValue(taskSegement.getSnapshotValue(), new TypeReference<Collection<?>>() {
            }));
        } else if (instance == TaskDimensionEnum.TIME_RANGE) {
            doAnything(taskSegement.getStartTime(), taskSegement.getEndTime());
        } else {
            doAnything(taskSegement.getStartTime(), taskSegement.getEndTime(), taskSegement.getStartIndex(), taskSegement.getEndIndex());
        }
        taskSegement.setStatus(TaskStatusEnum.FINISHED.getCode());
        taskService.updateTaskSegment(taskSegement);
        dingInfoLog(MessageFormat.format("快照执行已结束：{0}", taskSegement));
        try {
            TimeUnit.MILLISECONDS.sleep(getSleepTime());
        } catch (InterruptedException i) {
        }
    }


    /**
     * 虽然我们的程序已经提供了 自动控制任务停止的功能
     * 我们仍然希望你在自定义的doAnything 也加上isRun判断以便程序停止看起起来就好像是你操作了开关一样
     */

    public void doAnything(LocalDateTime start, LocalDateTime end){}

    public void doAnything(LocalDateTime start, LocalDateTime end, Integer tableStart, Integer tableEnd){}

    public void doAnything(Collection<?> list){}


    /**
     * 创建初始化快照
     * 具体可以通过 TaskCtlGenerator#getTask实现
     *
     * @param t
     * @return
     */
    protected abstract TaskPo.InitialSnapShot generateGlobalTask(T t);


}
