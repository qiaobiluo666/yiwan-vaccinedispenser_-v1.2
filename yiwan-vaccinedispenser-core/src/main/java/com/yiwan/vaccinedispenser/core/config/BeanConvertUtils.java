package com.yiwan.vaccinedispenser.core.config;

import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 转换对象工具
 *
 * @author bugpool
 */
public class BeanConvertUtils extends BeanUtils {

    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        List<T> targetList = new ArrayList<>();

        for (S sourceObject : sourceList) {
            try {
                T targetObject = targetClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(sourceObject, targetObject);
                targetList.add(targetObject);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                // 处理异常情况
                e.printStackTrace();
            }
        }

        return targetList;
    }
}