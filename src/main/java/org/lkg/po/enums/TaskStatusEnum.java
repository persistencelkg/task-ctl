package org.lkg.po.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author likaiguang
 * @date 2023/4/13 6:33 下午
 */
@AllArgsConstructor
@Getter
public enum TaskStatusEnum {
    /**
     *
     */
    WORKING(1),
    OCCUPY(2),
    FINISHED(3)
        ;
    private final Integer code;
}
