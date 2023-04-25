package org.lkg.core.ctl;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.lkg.po.enums.DataSourceEnum;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author likaiguang
 * @date 2023/4/12 5:28 下午
 */
@Getter
@Setter
@ToString
@Slf4j
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TaskSnapShot {

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<String> dataList;

    /**
     * 数据库 or redis 下标
     */
    private Object dataIndex;

    private DataSourceEnum ds;

    /**
     * PT10S、PT1h、 P1D
     */
    private Duration period;

    public static  TaskSnapShot newInstance() {
        return new TaskSnapShot();
    }

    public static  TaskSnapShot newInstance(Collection<String> dataList) {
        TaskSnapShot objectBaskTaskPo = new TaskSnapShot();
        objectBaskTaskPo.setDataList(new ArrayList<String>(dataList));
        return objectBaskTaskPo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskSnapShot that = (TaskSnapShot) o;
        return
                Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate)
                && Objects.equals(dataList, that.dataList) && Objects.equals(dataIndex, that.dataIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, dataList, dataIndex);
    }
}
