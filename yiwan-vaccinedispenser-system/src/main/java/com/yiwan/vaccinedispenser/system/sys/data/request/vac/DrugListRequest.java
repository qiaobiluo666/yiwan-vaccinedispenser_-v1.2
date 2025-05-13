package com.yiwan.vaccinedispenser.system.sys.data.request.vac;

import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/3/6 9:01
 */
@Data
public class DrugListRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = 5605713320595496734L;


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



    /**
     * 当前页
     */
    @Min(1)
    private Integer page;

    /**
     * 每页大小
     */
    @Min(1)
    private Integer size;



}
