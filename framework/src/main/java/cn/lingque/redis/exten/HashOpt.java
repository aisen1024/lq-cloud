package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import io.lettuce.core.ScriptOutputType;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class HashOpt extends BaseOpt {
    private LingQueRedis lingQueRedis;

    private static final String HSET_AND_EXPIRE_SCRIPT = 
        "redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return 1;";

    private static final String BATCH_HSET_AND_EXPIRE_SCRIPT =
        "local count = 0; " +
        "for i = 1, #ARGV - 1, 2 do " +
        "    count = count + redis.call('HSET', KEYS[1], ARGV[i], ARGV[i+1]); " +
        "end; " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String HDEL_AND_EXPIRE_SCRIPT =
        "local count = redis.call('HDEL', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "end; " +
        "return count;";

    private static final String HINCRBY_AND_EXPIRE_SCRIPT =
        "local value = redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return value;";

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
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                HSET_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                member, value, String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 批量添加或更新hash的值并更新过期时间
     * @param map 要设置的键值对
     * @return 添加的字段数量
     */
    public long setMap(Map<String, Object> map) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[map.size() * 2 + 1];
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                args[i++] = entry.getKey();
                args[i++] = LQUtil.isBaseValue(entry.getValue()) ? 
                    entry.getValue().toString() : 
                    JSONUtil.toJsonStr(entry.getValue());
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                BATCH_HSET_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 根据hk删除哈希的值并更新过期时间
     * @param hk 要删除的字段
     * @return 删除的字段数量
     */
    public Long deleteField(String... hk) {
        return (Long) lingQueRedis.execBase((commands) -> {
            String[] args = Arrays.copyOf(hk, hk.length + 1);
            args[hk.length] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                HDEL_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 自增哈希的值并更新过期时间
     */
    public Long incrHashValue(String memberId, long num) {
        return (Long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                HINCRBY_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                memberId, String.valueOf(num), String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 自增哈希的值 +1 并更新过期时间
     */
    public Long incrHashValue(String memberId) {
        return incrHashValue(memberId, 1L);
    }

    /**
     * 获取哈希缓存
     */
    public <T> T getValue(String member, Class<T> targetClass) {
        return (T) lingQueRedis.execBase((commands) -> {
            String val = commands.hget(lingQueRedis.key, member);
            if (LQUtil.isEmpty(val)) {
                return null;
            }
            return LQUtil.isBasClass(targetClass) ?
                LQUtil.baseClassTran(val, targetClass) : 
                JSONUtil.toBean(val, targetClass);
        });
    }

    /**
     * 获取哈希缓存返回list数据
     */
    public <T> List<T> getListValue(String member, Class<T> targetClass) {
        return (List<T>) lingQueRedis.execBase((commands) -> {
            String val = commands.hget(lingQueRedis.key, member);
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
        return (int) lingQueRedis.execBase((commands) -> {
            Long value = commands.hlen(lingQueRedis.key);
            return value != null ? value.intValue() : 0;
        });
    }

    /**
     * 是否有指定的键
     */
    public Boolean isExist(String memberId) {
        return (Boolean) lingQueRedis.execBase((commands) -> 
            commands.hexists(lingQueRedis.key, memberId));
    }

    /**
     * 列出哈希的键值对
     */
    public <T> Map<String, T> entriesHashValue(Class<T> targetClass) {
        return (Map<String, T>) lingQueRedis.execBase((commands) -> {
            Map<String, String> entries = commands.hgetall(lingQueRedis.key);
            Map<String, T> resultMap = new HashMap<>();
            entries.forEach((k, v) -> {
                T value = LQUtil.isBasClass(targetClass) ?
                    LQUtil.baseClassTran(v, targetClass) : 
                    JSONUtil.toBean(v, targetClass);
                resultMap.put(k, value);
            });
            return resultMap;
        });
    }

    /**
     * 设置哈希缓存
     */
    public void setValue(String member, Object value) {
        lingQueRedis.execBase((commands) -> {
            commands.hset(lingQueRedis.key, member, 
                LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value));
            if (lingQueRedis.ttl > 0) {
                commands.expire(lingQueRedis.key, lingQueRedis.ttl);
            }
            return null;
        });
    }

    /**
     * 删除哈希缓存
     */
    public void delValue(String member) {
        lingQueRedis.execBase((commands) -> {
            commands.hdel(lingQueRedis.key, member);
            if (lingQueRedis.ttl > 0) {
                commands.expire(lingQueRedis.key, lingQueRedis.ttl);
            }
            return null;
        });
    }

    /**
     * 获取所有哈希缓存
     */
    public <T> Map<String, T> getAllValue(Class<T> targetClass) {
        return (Map<String, T>) lingQueRedis.execBase((commands) -> {
            Map<String, String> map = commands.hgetall(lingQueRedis.key);
            if (map == null || map.isEmpty()) {
                return null;
            }
            return map.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> LQUtil.isBasClass(targetClass) ?
                        LQUtil.baseClassTran(e.getValue(), targetClass) : 
                        JSONUtil.toBean(e.getValue(), targetClass)
                ));
        });
    }

    /**
     * 获取所有哈希缓存的值
     */
    public <T> List<T> getAllValues(Class<T> targetClass) {
        return (List<T>) lingQueRedis.execBase((commands) -> {
            List<String> values = commands.hvals(lingQueRedis.key);
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.stream()
                .map(val -> LQUtil.isBasClass(targetClass) ?
                    LQUtil.baseClassTran(val, targetClass) : 
                    JSONUtil.toBean(val, targetClass))
                .collect(Collectors.toList());
        });
    }

    /**
     * 获取所有哈希缓存的键
     */
    public List<String> getAllKeys() {
        return (List<String>) lingQueRedis.execBase((commands) -> commands.hkeys(lingQueRedis.key));
    }

    /**
     * 判断哈希缓存是否存在
     */
    public boolean exists(String member) {
        return (boolean) lingQueRedis.execBase((commands) -> commands.hexists(lingQueRedis.key, member));
    }

    /**
     * 获取哈希缓存的长度
     */
    public long size() {
        return (long) lingQueRedis.execBase((commands) -> commands.hlen(lingQueRedis.key));
    }
}
