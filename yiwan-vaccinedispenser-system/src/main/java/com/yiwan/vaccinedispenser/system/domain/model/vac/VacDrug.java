package com.yiwan.vaccinedispenser.system.domain.model.vac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/5 18:45
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class VacDrug extends Model<VacDrug> {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;




    /**
     * 产品编码
     */

    private String productNo;

    /**
     * 产品名称
     */

    private String productName;

    /**
     * 疫苗名称编码
     */

    private String vaccineMinorCode;

    /**
     * 疫苗名称
     */

    private String vaccineMinorName;

    /**
     * 疫苗属性
     */

    private String vaccineTypeCode;

    /**
     * 生产企业编码
     */

    private String manufacturerCode;


    /**
     * 生产企业名称
     */

    private String manufacturerName;


    /**
     * 剂型  02:注射剂；11:吸入制剂；12:鼻用制剂；23:口服剂；99:皮上划痕
     */

    private String doseTypeCode;



    /**
     * 制剂规格  最小包装疫苗规格；举例：复溶后0.5ml/瓶
     */
    private String strength;



    /**
     * 最小包装转换比
     */
    private Integer pkgCratio;

    /**
     * 最小包装单位  支/瓶
     */
    private String pkgUnit;


    /**
     * 疫苗长度
     */

    private Integer vaccineLong;


    /**
     * 疫苗宽度
     */

    private Integer vaccineWide;


    /**
     * 疫苗高度
     */

    private Integer vaccineHigh;

    /**
     * 疫苗重量
     */

    private Integer vaccineWeight;

    /**
     * 统一编码
     */

    private String vaccineCode;



    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;

    /**
     * 是否删除  true/已删除  false/未删除
     */
    private int deleted;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
