package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import lombok.Data;

import java.util.*;

@Data
public class HashOpt extends BaseOpt{
    private LingQueRedis lingQueRedis;

    public HashOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    /**
     * 设置哈希缓存并更新过期时间
     * @param member 成员
     * @param value 值
     * @return 操作结果
     */
    public long set(String member, String value) {
        String luaScript = 
            "local result = redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[3])\n" +
            "return result";
        return (Long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    member,
                    value,
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 批量添加或更新hash的值并更新过期时间
     * @param map 要设置的键值对
     * @return 操作结果
     */
    public long setMap(Map<String, Object> map) {
        String luaScript = 
            "local result = 0\n" +
            "for i = 1, #ARGV - 1, 2 do\n" +
            "    result = result + redis.call('HSET', KEYS[1], ARGV[i], ARGV[i+1])\n" +
            "end\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV])\n" +
            "return result";

        return (Long)lingQueRedis.execBase((jedis) -> {
            List<String> args = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                args.add(entry.getKey());
                args.add(LQUtil.isBaseValue(entry.getValue()) ? 
                    entry.getValue().toString() : 
                    JSONUtil.toJsonStr(entry.getValue()));
            }
            args.add(String.valueOf(lingQueRedis.ttl));

            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 根据hk删除哈希的值并更新过期时间
     * @param hk 要删除的字段
     * @return 删除的字段数量
     */
    public Long deleteField(String... hk) {
        String luaScript = 
            "local result = redis.call('HDEL', KEYS[1], unpack(ARGV, 1, #ARGV-1))\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[#ARGV])\n" +
            "end\n" +
            "return result";

        return (Long)lingQueRedis.execBase((jedis) -> {
            List<String> args = new ArrayList<>(Arrays.asList(hk));
            args.add(String.valueOf(lingQueRedis.ttl));

            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 自增哈希的值并更新过期时间
     * @param memberId 成员ID
     * @param num 增加的值
     * @return 增加后的值
     */
    public Long incrHashValue(String memberId, long num) {
        String luaScript = 
            "local value = redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[3])\n" +
            "return value";
        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    memberId,
                    String.valueOf(num),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 自增哈希的值 +1 并更新过期时间
     * @param memberId 成员ID
     * @return 增加后的值
     */
    public Long incrHashValue(String memberId) {
        return incrHashValue(memberId, 1L);
    }

    // 以下是查询方法，不需要更新过期时间

    /**
     * 获取哈希缓存
     */
    public <T> T getValue(String member, Class<T> targetClass) {
        return (T)lingQueRedis.execBase((jedis) -> {
            String val = jedis.hget(lingQueRedis.key, member);
            if (LQUtil.isEmpty(val)) {
                return null;
            }
            return LQUtil.isBaseValue(targetClass) ? LQUtil.baseClassTran(member, targetClass) : JSONUtil.toBean(val, targetClass);
        });
    }

    /**
     * 获取哈希缓存返回list数据
     */
    public <T> List<T> getFieldValueToList(String member, Class<T> targetClass) {
        return (List<T>)lingQueRedis.execBase((jedis) -> {
            String val = jedis.hget(lingQueRedis.key, member);
            if (LQUtil.isEmpty(val)) {
                return null;
            }
            return JSONUtil.toList(val, targetClass);
        });
    }

    /**
     * 获取哈希的属性个数
     */
    public int count() {
        return (int)lingQueRedis.execBase((jedis) -> {
            Long value = jedis.hlen(lingQueRedis.key);
            return null == value ? 0 : value.intValue();
        });
    }

    /**
     * 是否有指定的键
     */
    public Boolean isExist(String memberId) {
        return (Boolean)lingQueRedis.execBase((jedis) -> {
            return jedis.hexists(lingQueRedis.key, memberId);
        });
    }

    /**
     * 列出哈希的键值对
     */
    public <T> Map<String, T> entriesHashValue(Class<T> targetClass) {
        return (Map<String, T>)lingQueRedis.execBase((jedis) -> {
            Map<String, T> resultMap = new HashMap<>();
            Map<String, String> entries = jedis.hgetAll(lingQueRedis.key);
            entries.forEach((k, v) -> {
                T value = LQUtil.isBaseValue(targetClass) ? LQUtil.baseClassTran(v, targetClass) : JSONUtil.toBean(v, targetClass);
                resultMap.put(k, value);
            });
            return resultMap;
        });
    }
}
