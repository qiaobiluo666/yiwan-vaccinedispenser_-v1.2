package com.yiwan.vaccinedispenser.system.zyc;

import cn.gov.zcy.open.sdk.http.ResponseResult;
import cn.gov.zcy.open.sdk.open.HttpMethod;
import cn.gov.zcy.open.sdk.open.ZcyClient;
import cn.gov.zcy.open.sdk.open.ZcyOpenRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.InventoryReportData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultData;
import com.yiwan.vaccinedispenser.system.sys.data.zyc.SendVaccineResultRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.*;

/**
 * @author slh
 * @version 1.0
 * @desc
 * @date 2024/5/16 9:11
 */
@Slf4j
@Component
@Configuration
public class ZcyRequestBuilder {
    @Value("${app.apiKey}")
    private  String appKey;
    @Value("${app.privateKeyPath}")
    private  String privateKeyPath;
    @Value("${app.apiEnvironment}")
    private  String apiEnvironment;
    @Value("${app.version}")
    private  String version;


    /**
     * 获取疫苗列表
     * @return
     * @throws Exception
     */
    public ResponseResult getVaccineListRequest() throws Exception {
       ZcyOpenRequest request = getRequest("/zcy.vaccine.product.query");

       Map<String, Object> paramMaps = new HashMap<>();
       request.setParamMap(paramMaps);
       ZcyClient zcyClient = new ZcyClient();
       ResponseResult result = zcyClient.sendRequest(request);
       log.info(JSON.toJSONString(result));
       // 发送请求并返回结果
       return result;

    }



    /**
     * 获取扫码后的疫苗信息
     * @return
     * @throws Exception
     */
    public ResponseResult getScanResultRequest(String supervisedCode) throws Exception {

        ZcyOpenRequest request = getRequest("/zcy.vaccine.info.query.for.machine.enter");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("supervisedCode", supervisedCode);

        Map<String, Object> paramMaps = new HashMap<>();
        paramMaps.put("data", jsonObject.toString());
        request.setParamMap(paramMaps);

        ZcyClient zcyClient = new ZcyClient();
        ResponseResult result = zcyClient.sendRequest(request);
        log.info(JSON.toJSONString(result));
        // 发送请求并返回结果
        return result;

    }


    /**
     * 获取发苗处方 (定时器)
     * @return
     * @throws Exception
     */
    public ResponseResult getSendVaccineRequest() throws Exception {

        ZcyOpenRequest request = getRequest("/zcy.vaccine.cmd.for.machine.pick.up");
        Map<String, Object> paramMaps = new HashMap<>();
        request.setParamMap(paramMaps);
        ZcyClient zcyClient = new ZcyClient();
        ResponseResult result = zcyClient.sendRequest(request);

        // 发送请求并返回结果
        return result;

    }



    /**
     * 发苗结果
     * @return
     * @throws Exception
     */
    public ResponseResult sendVaccineEndRequest(SendVaccineResultRequest sendVaccineResultRequest) throws Exception {
        ZcyOpenRequest request = getRequest("/zcy.vaccine.machine.up.pick.result");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("requestNo", sendVaccineResultRequest.getRequestNo());
        jsonObject.put("result", sendVaccineResultRequest.getResult());
        Map<String, Object> paramMaps = new HashMap<>();
        paramMaps.put("data", jsonObject.toString());
        request.setParamMap(paramMaps);
        ZcyClient zcyClient = new ZcyClient();
        ResponseResult result = zcyClient.sendRequest(request);
        log.info(JSON.toJSONString(result));
        // 发送请求并返回结果
        return result;

    }



    /**
     * 库存上报
     * @return
     * @throws Exception
     */
    public ResponseResult getInventoryRequest(List<InventoryReportData> inventoryReportDataList) throws Exception {
        ZcyOpenRequest request = getRequest("/zcy.vaccine.stock.report.for.machine");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("stockList", inventoryReportDataList);
        Map<String, Object> paramMaps = new HashMap<>();
        paramMaps.put("data", jsonObject.toString());
        request.setParamMap(paramMaps);
        ZcyClient zcyClient = new ZcyClient();
        ResponseResult result = zcyClient.sendRequest(request);
        log.info(JSON.toJSONString(result));
        // 发送请求并返回结果
        return result;

    }



    /**
     * 发苗机-当日门诊损耗监管码查询
     * @return
     * @throws Exception
     */
    public ResponseResult getVaccineCodeRequest(LocalDate time) throws Exception {
        ZcyOpenRequest request = getRequest("/zcy.vaccine.machine.consume.supervised.code.query");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("usedDate", time);
        Map<String, Object> paramMaps = new HashMap<>();
        paramMaps.put("data", jsonObject.toString());
        request.setParamMap(paramMaps);
        ZcyClient zcyClient = new ZcyClient();
        ResponseResult result = zcyClient.sendRequest(request);
        log.info(JSON.toJSONString(result));
        // 发送请求并返回结果
        return result;

    }


   /* public static void main(String[] args) {
        InputStream resourceAsStream = ZcyRequestBuilder.class.getResourceAsStream("/private_key.pem");
        String stringFromStream = getStringFromStream(resourceAsStream);
        System.out.println(stringFromStream);
    }*/


    //政采云请求头配置
    public  ZcyOpenRequest getRequest(String url) throws IOException {

        String key = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
//        String key = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
        key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        ZcyOpenRequest request = ZcyOpenRequest.buildJwtRequest(appKey, key, true, apiEnvironment);
        request.setUri(url);
        request.setMethod(HttpMethod.POST);
        // 设置Header
        Map<String, String> jwtHeaders = new HashMap<>();
        jwtHeaders.put("version",version);
        request.setHeaderMap(jwtHeaders);


        return  request;
    }


}
