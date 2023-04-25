package org.lkg.po.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author likaiguang
 * @date 2023/4/13 1:51 下午
 */
@AllArgsConstructor
@Getter
public enum DataSourceEnum {

    /**
     *
     */
    MYSQL("mysql"),
    REDIS("redis"),
    ES("es");

    private final String ds;


    private static HashMap<String, DataSourceEnum> MAP = null;

    private static void init() {
        if (Objects.isNull(MAP)) {
            MAP = new HashMap<>();
        }
        DataSourceEnum[] values = DataSourceEnum.values();
        for (DataSourceEnum value : values) {
            MAP.put(value.getDs(), value);
        }
    }


    public static DataSourceEnum instance(String ds) {
        init();
        return MAP.get(ds);
    }
}
