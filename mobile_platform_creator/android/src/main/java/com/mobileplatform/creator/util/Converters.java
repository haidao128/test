package com.mobileplatform.creator.util;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Room 数据库类型转换器
 * 用于转换复杂数据类型与 SQLite 支持的基本数据类型
 */
public class Converters {
    /**
     * 将时间戳（Long）转换为日期（Date）
     * @param value 时间戳
     * @return 日期对象
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * 将日期（Date）转换为时间戳（Long）
     * @param date 日期对象
     * @return 时间戳
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
} 