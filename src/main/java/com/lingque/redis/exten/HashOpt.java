package com.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import com.lingque.redis.LingQueRedis;
import com.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class HashOpt<T> {
    private LingQueRedis<T> lingQueRedis;
    /**
     * 设置哈希缓存
     *
     * @param member 成员
     * @param value  值
     */
    public long set(String member, String value) {
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().hset(lingQueRedis.key, member, value));
    }

    /**
     * 批量添加或更新hash的值
     */
    public long setMap(Map<String, T> map) {
        Map<String, String> data = new HashMap<>();
        for (String key : map.keySet()) {
            T val = map.get(key);
            data.put(key, LQUtil.isBaseValue(val) ? val.toString() : JSONUtil.toJsonStr(val));
        }
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().hset(lingQueRedis.key, data));
    }

    /**
     * 获取哈希缓存 返回基础数据或者bean对象
     *
     * @param member 成员
     */
    public <T> T getValue(String member, Class<T> targetClass) {
        String val = lingQueRedis.getRedisTemplate().hget(lingQueRedis.key, member);
        if (LQUtil.isEmpty(val)) {
            return null;
        }
        return LQUtil.isBaseValue(targetClass) ? LQUtil.baseClassTran(member, targetClass) : JSONUtil.toBean(val, targetClass);
    }

    /**
     * 获取哈希缓存 返回list数据
     *
     * @param member 成员
     */
    public List<T> getFieldValueToList(String member, Class<T> targetClass) {
        String val = lingQueRedis.getRedisTemplate().hget(lingQueRedis.key, member);
        if (LQUtil.isEmpty(val)) {
            return null;
        }
        return JSONUtil.toList(val, targetClass);
    }

    /**
     * 获取哈希的属性个数
     *
     * @return
     */
    public int count() {
        Long value = lingQueRedis.getRedisTemplate().hlen(lingQueRedis.key);
        return null == value ? 0 : value.intValue();
    }

    /**
     * 是否有指定的键
     *
     * @param memberId
     * @return
     */
    public Boolean isExist(String memberId) {
        return lingQueRedis.getRedisTemplate().hexists(lingQueRedis.key, memberId);
    }

    /**
     * 列出哈希的键值对
     *
     * @return
     */
    public Map<String, T> entriesHashValue(Class<T> targetClass) {
        Map<String, T> resultMap = new HashMap<>();
        java.util.Map<String, String> entries = lingQueRedis.getRedisTemplate().hgetAll(lingQueRedis.key);
        entries.forEach((k, v) -> {
            T value = LQUtil.isBaseValue(targetClass) ? LQUtil.baseClassTran(v, targetClass) : JSONUtil.toBean(v, targetClass);
            resultMap.put(k, value);
        });
        return resultMap;
    }

    /**
     * 根据hk 删除哈希的值
     *
     * @param hk
     * @return
     */
    public Long deleteField(String... hk) {
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().hdel(lingQueRedis.key, hk));
    }

    /**
     * 自增哈希的值 +1
     *
     * @param memberId
     * @return
     */
    public Long incrHashValue(String memberId) {
        return incrHashValue(memberId, 1L);
    }

    /**
     * 自增哈希的值 + num
     *
     * @param memberId
     * @return
     */
    public Long incrHashValue(String memberId, long num) {
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().hincrBy(lingQueRedis.key, memberId, num));
    }


}
