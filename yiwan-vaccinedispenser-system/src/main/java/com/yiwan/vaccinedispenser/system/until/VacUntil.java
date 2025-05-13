package com.yiwan.vaccinedispenser.system.until;

import com.yiwan.vaccinedispenser.system.sys.data.DistanceServoData;
import lombok.extern.slf4j.Slf4j;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/7 11:29
 */

@Slf4j
public class VacUntil {

    public static String boxNoToCode(int lineNum, int positionNum) {
        // 将 LineNum 和 PositionNum 转换成对应的字符串格式
        String lineCode = String.format("%02d", lineNum);
        String positionCode = String.format("%02d", positionNum);

        // 拼接成最终的格式，如 A0108
        return "A" + lineCode + positionCode;
    }


    public static void sleep(long mill){
        try {
            Thread.sleep(mill);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    
    public static DistanceServoData findRectangleCenterX(double lengthAB, double lengthBC, double angleDegrees, double bx, double by) {

        //三角坐标在第一象限数据
        // 将角度从度转换为弧度
        double angleRadians = Math.toRadians(angleDegrees);
        // 计算A点的坐标（假设AC水平，我们需要用角度来计算实际的位置）
        double ax = bx + lengthAB*100 * Math.cos(angleRadians);
        double ay =by - lengthAB*100 * Math.sin(angleRadians);
        // 计算B点的坐标（B在C的垂直方向上，由于AC已经使用了角度，BC也将依赖于这个角度）
        double cx = bx - lengthBC*100 * Math.sin(angleRadians);
        double cy =by - lengthBC*100 * Math.cos(angleRadians);


        // 中心点X是A和C，B和D的中点
        double centerX = (ax + cx) / 2;
        double centerY = (ay + cy) / 2;
        DistanceServoData data = new DistanceServoData();
        data.setServoX((int)centerX);
        data.setServoY((int)centerY);

        return data;
    }


    public static DistanceServoData findRectangleCenterY(double lengthAB, double lengthBC, double angleDegrees, double bx, double by) {

        //三角坐标在第一象限数据
        // 将角度从度转换为弧度
        double angleRadians = Math.toRadians(angleDegrees);
        // 计算A点的坐标（假设AC水平，我们需要用角度来计算实际的位置）left
        double ax = bx - lengthAB*100 * Math.cos(angleRadians);
        double ay =by - lengthAB*100 * Math.sin(angleRadians);
        // 计算B点的坐标（B在C的垂直方向上，由于AC已经使用了角度，BC也将依赖于这个角度） right
        double cx = bx + lengthBC*100 * Math.sin(angleRadians);
        double cy =by - lengthBC*100 * Math.cos(angleRadians);


        // 中心点X是A和C，B和D的中点
        double centerX = (ax + cx) / 2;
        double centerY = (ay + cy) / 2;
        DistanceServoData data = new DistanceServoData();
        data.setServoX((int)centerX);
        data.setServoY((int)centerY);

        return data;
    }


}
