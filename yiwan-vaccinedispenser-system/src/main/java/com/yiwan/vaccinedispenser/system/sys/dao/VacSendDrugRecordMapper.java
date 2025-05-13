package com.yiwan.vaccinedispenser.system.sys.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacDrugRecord;
import com.yiwan.vaccinedispenser.system.domain.model.vac.VacSendDrugRecord;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.SendDrugRecordRequest;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 疫苗上药信息 Mapper 接口
 * </p>
 *
 * @author vicente
 * @since 2023-05-09
 */
public interface VacSendDrugRecordMapper extends BaseMapper<VacSendDrugRecord> {

    @Select("SELECT DATE_FORMAT(a.date, '%m-%d') AS date, IFNULL(b.sum, 0) AS sum " +
            "FROM " +
            "( " +
            "    SELECT DATE_SUB(CURDATE(), INTERVAL (6 - n) DAY) AS date " +
            "    FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 " +
            "          UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) AS days " +
            ") a " +
            "LEFT JOIN " +
            "( " +
            "    SELECT COUNT(*) AS sum, DATE(create_time) AS date " +
            "    FROM vac_send_drug_record " +
            "    WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "    GROUP BY DATE(create_time) " +
            ") b " +
            "ON a.date = b.date "+
            "ORDER BY a.date ASC")
    List<Map<String, Object>> getWeeklyCountForType0();



    @Select("SELECT DATE_FORMAT(a.date, '%m-%d') AS date, IFNULL(b.sum, 0) AS sum " +
            "FROM " +
            "( " +
            "    SELECT DATE_SUB(CURDATE(), INTERVAL (29 - n) DAY) AS date " +
            "    FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 " +
            "          UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 " +
            "          UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 " +
            "          UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 " +
            "          UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 " +
            "          UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 " +
            "          UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 " +
            "          UNION ALL SELECT 28 UNION ALL SELECT 29) AS days " +
            ") a " +
            "LEFT JOIN " +
            "( " +
            "    SELECT COUNT(*) AS sum, DATE(create_time) AS date " +
            "    FROM vac_send_drug_record " +
            "    WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 29 DAY) " +
            "    GROUP BY DATE(create_time) " +
            ") b " +
            "ON a.date = b.date " +
            "ORDER BY a.date ASC")
    List<Map<String, Object>> getDailyCountForType1();

    @Select("SELECT DATE_FORMAT(a.date, '%Y-%m') AS date, IFNULL(b.sum, 0) AS sum " +
            "FROM " +
            "( " +
            "    SELECT DATE_SUB(CURDATE(), INTERVAL (11 - n) MONTH) AS date " +
            "    FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 " +
            "          UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 " +
            "          UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11) AS months " +
            ") a " +
            "LEFT JOIN " +
            "( " +
            "    SELECT COUNT(*) AS sum, DATE_FORMAT(create_time, '%Y-%m') AS date " +
            "    FROM vac_send_drug_record " +
            "    WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH) " +
            "    GROUP BY DATE_FORMAT(create_time, '%Y-%m') " +
            ") b " +
            "ON a.date = b.date " +
            "ORDER BY a.date ASC")
    List<Map<String, Object>> getMonthlyCountForType2();






    @Select("SELECT w.workbench_name as name, IFNULL(COUNT(r.id), 0) AS counts " +
            "FROM vac_workbench w " +
            "LEFT JOIN vac_send_drug_record r ON w.workbench_no = r.workbench_no " +
            "AND DATE(r.create_time) = CURDATE() AND r.deleted = 0 " +
            "WHERE   w.deleted = 0 "+
            "GROUP BY w.workbench_name " +
            "ORDER BY w.workbench_name")
    List<Map<String, Object>> getSendWorkNum();


    @Select("SELECT COALESCE(COUNT(id), 0) AS total_records " +
            "FROM  vac_send_drug_record " +
            "WHERE DATE(create_time) = CURDATE() AND deleted = 0")
    List<Map<String, Object>>  todaySendDrug();


    @Select("SELECT DATE(create_time) AS date, COALESCE(COUNT(id), 0) AS count " +
            "FROM vac_send_drug_record " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 13 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY DATE(create_time) ASC")
    List<Map<String, Object>> weekSendDrug();



    List<SendDrugRecordRequest> countGroupedByProductName(
            @Param("createTimeStart") Date createTimeStart,
            @Param("createTimeEnd") Date createTimeEnd,
            @Param("workbenchName") String workbenchName
    );

}
