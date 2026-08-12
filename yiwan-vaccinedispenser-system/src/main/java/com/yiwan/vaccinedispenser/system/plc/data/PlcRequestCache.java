package com.yiwan.vaccinedispenser.system.plc.data;

import com.yiwan.vaccinedispenser.core.redis.CashService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

@Slf4j
@Component
public class PlcRequestCache {

    private static final String KEY_PREFIX = "plc:req:";
    private static final long TTL_SECONDS = 1800;

    @Autowired
    private CashService cashService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 应用启动时清理上次残留的帧号记录
     * 帧号每次重启从1开始，旧记录已失效
     */
    @PostConstruct
    public void cleanUp() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("[PLC] 已清理 {} 条残留帧号记录", keys.size());
        }
    }

    public void save(int transactionId, int startAddress, int registerCount) {
        String key = buildKey(transactionId);
        String value = startAddress + "," + registerCount;
        cashService.set(key, value, TTL_SECONDS);
    }

    public int[] getAndRemove(int transactionId) {
        String key = buildKey(transactionId);
        Object obj = cashService.get(key);
        if (obj == null) {
            return null;
        }
        cashService.set(key, null, 0);
        String value = obj.toString();
        String[] parts = value.split(",");
        if (parts.length < 2) {
            return null;
        }
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private String buildKey(int transactionId) {
        return KEY_PREFIX + transactionId;
    }
}
