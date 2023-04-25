package org.lkg.core.ctl;

import lombok.extern.slf4j.Slf4j;
import org.lkg.po.TaskPo;
import org.lkg.po.enums.DataSourceEnum;
import org.lkg.util.DateTimeUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import static org.lkg.po.enums.DataSourceEnum.MYSQL;

/**
 * @author likaiguang
 * @date 2023/4/13 1:55 下午
 */
@Slf4j
public class TaskCtlGenerator{

    public static  TaskPo.InitialSnapShot getTask(String ds, Collection<?> dataList) {
        return getTask(ds, dataList, null, null);
    }

    public static  TaskPo.InitialSnapShot getTask(String ds, String index, Collection<?> dataList) {
        return getTask(ds, dataList, index, null);
    }


    public static  TaskPo.InitialSnapShot getTask(String ds, String index) {
        return getTask(ds, null, index, null);
    }

    public static  TaskPo.InitialSnapShot getTask(String ds, Collection<?> dataList, String index) {
        return getTask(ds, dataList, index, null);
    }

    public static  TaskPo.InitialSnapShot getTask(String ds, Collection<?> dataList, String index, String start, String end) {
        return getTask(ds, dataList, index, String.join(TaskCtlService.LINE, start,end));
    }

    public static TaskPo.InitialSnapShot getTask(String ds, Collection<?> dataList, @Nullable String index, @Nullable String timeRange) {
        DataSourceEnum instance = DataSourceEnum.instance(ds);
        Assert.isTrue(!ObjectUtils.isEmpty(ds) && !ObjectUtils.isEmpty(instance), "invalid data source :" + ds);

        Object indexTypeWithDs = getIndexTypeWithDs(index, instance);
        Assert.isTrue(!ObjectUtils.isEmpty(indexTypeWithDs), MessageFormat.format("ds:{0}not support this index:{1}", ds, index));

        TaskPo.InitialSnapShot baskTaskPo = new TaskPo.InitialSnapShot();
        if (!ObjectUtils.isEmpty(dataList)) {
            baskTaskPo.setDataList(dataList);
        }
        if (ObjectUtils.isEmpty(ds)) {
            baskTaskPo.setDs(MYSQL.getDs());
        } else {
            baskTaskPo.setDs(ds);
        }
        if (!ObjectUtils.isEmpty(index)) {
            baskTaskPo.setIndex(index);
        }
        String line = TaskCtlService.LINE;
        if (!ObjectUtils.isEmpty(timeRange) && timeRange.contains(line)) {
            String[] split = timeRange.split(line);
            baskTaskPo.setStartTime(DateTimeUtil.parse(split[0]));
            baskTaskPo.setEndTime(DateTimeUtil.parse(split[1]));
        }
        return baskTaskPo;
    }

    private static Object getIndexTypeWithDs(String index, DataSourceEnum instance) {
        try {
            if (MYSQL == instance) {
                return Integer.parseInt(index);
            }
            return index;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }

    public static void main(String[] args) {

        ArrayList<Object> objects = new ArrayList<>();

        TaskPo.InitialSnapShot mysql = getTask("es", null, "2022-01-01 00:00:00->2022-03-03 00:00:00");
        System.out.println(mysql);
    }
}
