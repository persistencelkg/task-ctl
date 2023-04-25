package org.lkg.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lkg.po.TaskSegement;
import org.lkg.po.enums.TaskStatusEnum;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class TaskUtil {

    public static Duration buildTaskPeriod(String period) {
        if (ObjectUtils.isEmpty(period)) {
            return Duration.ofDays(1);
        }
        try {
            return Duration.parse(period);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Duration.ofDays(1);
    }

    public static List<Integer> list(int end) {
        return list(0, end);
    }

    /**
     * 左闭右开
     *
     * @param start
     * @param end
     * @return
     */
    public static List<Integer> list(int start, int end) {
        if (start >= end) {
            return null;
        }
        ArrayList<Integer> arrayList = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            arrayList.add(i);
        }
        return arrayList;
    }


    public static List<TaskSegement> list(String taskId, Integer start, Integer end, Integer spiltCount) {
        Integer tempEnd = 0;
        ArrayList<TaskSegement> taskSegements = new ArrayList<>();
        int i = 0;
        while (tempEnd < end) {
            tempEnd = start + spiltCount;
            if (tempEnd > end) {
                tempEnd = end;
            }

            taskSegements.add(
                    TaskSegement.builder()
                            .taskId(taskId)
                            .segmentId(i + 1)
                            .startIndex(start)
                            .endIndex(tempEnd)
                            .build()

            );
            start = tempEnd + 1;
        }

        return taskSegements;
    }


    /**
     * 每个机器属于自己执行的部分
     *
     * @param list
     * @param index
     * @param totalCount
     * @param <T>
     * @return
     */
    public static <T> List<T> list(List<T> list, Integer index, Integer totalCount) {
        // 任务分发
        int total = list.size();
        // 防止单机情况
        if (list.size() < totalCount || totalCount < 2) {
            return list;
        }
        int shardIndex = index;
        // 批次
        int batchSize = total / totalCount;
        int leaveSize = (total & (totalCount - 1));
        int from = shardIndex * batchSize;
        if (leaveSize != 0 && index == totalCount - 1) {
            return list.subList(from, total);
        }
        return list.subList(from, from + batchSize);
    }

    public static List<TaskSegement> list(String taskId, List<?> dataList, Integer batchSize) {
        int totalCount = dataList.size() / batchSize;
        if (totalCount == 0) {
            ArrayList<TaskSegement> taskSegements = new ArrayList<>();
            taskSegements.add(TaskSegement.builder()
                    .taskId(taskId)
                    .segmentId(0)
                    .status(TaskStatusEnum.WORKING.getCode())
                    .snapshotValue(JacksonUtil.writeValue(dataList))
                    .build()
            );
            return taskSegements;
        }
        int leaveSize = dataList.size() % batchSize;
        if (leaveSize != 0) {
            totalCount++;
        }
        ArrayList<TaskSegement> objects = new ArrayList<>();
        for (int i = 0; i < totalCount; i++) {
            int from = i * batchSize;
            int to = i * batchSize + batchSize;
            if (i == totalCount - 1) {
                to = dataList.size();
            }
            List<?> subList = dataList.subList(from, to);
            TaskSegement build = TaskSegement.builder()
                    .taskId(taskId)
                    .segmentId(i + 1)
                    .startIndex(from)
                    .endIndex(to)
                    .status(TaskStatusEnum.WORKING.getCode())
                    .snapshotValue(JacksonUtil.writeValue(subList))
                    .build();
            objects.add(build);
        }
        return objects;
    }


    public static List<TaskSegement> list(String taskId, LocalDateTime start, LocalDateTime end, Duration duration) {
        ArrayList<TaskSegement> objects = new ArrayList<>();
        LocalDateTime tempStart = start;
        LocalDateTime tempEnd = start;
        int i = 0;
        while (true) {
            tempEnd = tempStart.plus(duration);
            if (tempStart.plusNanos(1).isAfter(end)) {
                break;
            }
            TaskSegement build = TaskSegement.builder()
                    .taskId(taskId)
                    .segmentId(i + 1)
                    .status(TaskStatusEnum.WORKING.getCode())
                    .startTime(tempStart)
                    .endTime(tempEnd)
                    .build();

            objects.add(build);
            tempStart = tempEnd;
            i++;
        }
        return objects;
    }

    public static List<MinStartEnd> list(LocalDateTime start, LocalDateTime end, Duration duration) {
        ArrayList<MinStartEnd> objects = new ArrayList<>();
        LocalDateTime tempStart = start;
        LocalDateTime tempEnd = start;
        while (true) {
            tempEnd = tempStart.plus(duration);
            if (tempStart.plusNanos(1).isAfter(end)) {
                break;
            }

            objects.add(new MinStartEnd(tempStart, tempEnd));
            tempStart = tempEnd;
        }
        return objects;
    }


    public static synchronized String getTaskId() {
        return getMd5(UUID.randomUUID().toString() + System.currentTimeMillis() + IpUtil.getIp());
    }

    public static String getMd5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString().toUpperCase();
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(list(3));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MinStartEnd {
        LocalDateTime start;
        LocalDateTime end;
    }
}
