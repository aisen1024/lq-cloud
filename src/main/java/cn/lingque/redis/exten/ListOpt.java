package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.exceptions.LQException;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import io.lettuce.core.ScriptOutputType;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author aisen
 * @date 2024/9/25
 * @desc 简单说一下
 **/
@Data
public class ListOpt extends BaseOpt{

    private LingQueRedis lingQueRedis;

    private static final String LPUSH_AND_EXPIRE_SCRIPT = 
        "local count = redis.call('LPUSH', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return count;";

    private static final String RPUSH_AND_EXPIRE_SCRIPT = 
        "local count = redis.call('RPUSH', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return count;";

    private static final String BATCH_LPUSH_AND_EXPIRE_SCRIPT =
        "local count = redis.call('LPUSH', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String BATCH_RPUSH_AND_EXPIRE_SCRIPT =
        "local count = redis.call('RPUSH', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String LPOP_AND_EXPIRE_SCRIPT =
        "local value = redis.call('LPOP', KEYS[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
        "end; " +
        "return value;";

    private static final String RPOP_AND_EXPIRE_SCRIPT =
        "local value = redis.call('RPOP', KEYS[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
        "end; " +
        "return value;";

    private static final String BATCH_LPOP_AND_EXPIRE_SCRIPT =
        "local values = redis.call('LPOP', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return values;";

    private static final String BATCH_RPOP_AND_EXPIRE_SCRIPT =
        "local values = redis.call('RPOP', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return values;";

    private static final String LREM_AND_EXPIRE_SCRIPT =
        "local count = redis.call('LREM', KEYS[1], ARGV[1], ARGV[2]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return count;";

    private static final String AFTER_DELETE_FIRST_SCRIPT =
        "local index = 0; " +
        "local found = false; " +
        "local values = redis.call('LRANGE', KEYS[1], 0, -1); " +
        "for i, value in ipairs(values) do " +
        "    if value == ARGV[1] then " +
        "        index = i; " +
        "        found = true; " +
        "        break; " +
        "    end; " +
        "end; " +
        "if found and index + ARGV[2] <= #values then " +
        "    redis.call('LSET', KEYS[1], index + ARGV[2] - 1, ''); " +
        "    redis.call('LREM', KEYS[1], 1, ''); " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    private static final String BEFORE_DELETE_FIRST_SCRIPT =
        "local index = 0; " +
        "local found = false; " +
        "local values = redis.call('LRANGE', KEYS[1], 0, -1); " +
        "for i, value in ipairs(values) do " +
        "    if value == ARGV[1] then " +
        "        index = i; " +
        "        found = true; " +
        "        break; " +
        "    end; " +
        "end; " +
        "if found and index - ARGV[2] > 0 then " +
        "    redis.call('LSET', KEYS[1], index - ARGV[2] - 1, ''); " +
        "    redis.call('LREM', KEYS[1], 1, ''); " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    public ListOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    /**
     * 从列表左端插入元素
     * @param value 要插入的值
     * @return 插入后列表的长度
     */
    public long lpush(Object value) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                LPUSH_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 从列表右端插入元素
     * @param value 要插入的值
     * @return 插入后列表的长度
     */
    public long rpush(Object value) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                RPUSH_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 批量从列表左端插入元素
     * @param values 要插入的值列表
     * @return 插入后列表的长度
     */
    public long lpushAll(List<?> values) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[values.size() + 1];
            int i = 0;
            for (Object value : values) {
                args[i++] = LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value);
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                BATCH_LPUSH_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 批量从列表右端插入元素
     * @param values 要插入的值列表
     * @return 插入后列表的长度
     */
    public long rpushAll(List<?> values) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[values.size() + 1];
            int i = 0;
            for (Object value : values) {
                args[i++] = LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value);
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                BATCH_RPUSH_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 从列表左端弹出元素
     * @return 弹出的元素
     */
    public String lpop() {
        return (String) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                LPOP_AND_EXPIRE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                String.valueOf(lingQueRedis.ttl)
            );
            return result;
        });
    }

    /**
     * 从列表右端弹出元素
     * @return 弹出的元素
     */
    public String rpop() {
        return (String) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                RPOP_AND_EXPIRE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                String.valueOf(lingQueRedis.ttl)
            );
            return result;
        });
    }

    /**
     * 从列表左端批量弹出元素
     * @param count 要弹出的元素数量
     * @return 弹出的元素列表
     */
    public List<String> lpop(long count) {
        return (List<String>) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                BATCH_LPOP_AND_EXPIRE_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                String.valueOf(count),
                String.valueOf(lingQueRedis.ttl)
            );
            return result instanceof List ? (List<String>) result : new ArrayList<>();
        });
    }

    /**
     * 从列表右端批量弹出元素
     * @param count 要弹出的元素数量
     * @return 弹出的元素列表
     */
    public List<String> rpop(long count) {
        return (List<String>) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                BATCH_RPOP_AND_EXPIRE_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                String.valueOf(count),
                String.valueOf(lingQueRedis.ttl)
            );
            return result instanceof List ? (List<String>) result : new ArrayList<>();
        });
    }

    /**
     * 从列表中删除指定元素
     * @param count 要删除的数量，0表示删除所有匹配的元素，正数表示从头部开始删除指定数量，负数表示从尾部开始删除指定数量
     * @param value 要删除的元素值
     * @return 实际删除的元素数量
     */
    public long lrem(long count, Object value) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                LREM_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(count),
                LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 获取列表长度
     * @return 列表长度
     */
    public long size() {
        return (long) lingQueRedis.execBase((commands) -> {
            Long result = commands.llen(lingQueRedis.key);
            return result != null ? result : 0L;
        });
    }

    /**
     * 获取列表指定范围的元素
     * @param start 起始位置（从0开始）
     * @param end 结束位置（包含）
     * @return 指定范围的元素列表
     */
    public List<String> range(long start, long end) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.lrange(lingQueRedis.key, start, end));
    }

    /**
     * 获取列表指定范围的元素并转换为指定类型
     * @param start 起始位置（从0开始）
     * @param end 结束位置（包含）
     * @param targetClass 目标类型
     * @return 转换后的元素列表
     */
    public <T> List<T> range(long start, long end, Class<T> targetClass) {
        return (List<T>) lingQueRedis.execBase((commands) -> {
            List<String> values = commands.lrange(lingQueRedis.key, start, end);
            return values.stream()
                .map(val -> LQUtil.isBasClass(targetClass) ?
                    LQUtil.baseClassTran(val, targetClass) : 
                    JSONUtil.toBean(val, targetClass))
                .collect(Collectors.toList());
        });
    }

    /**
     * 删除指定元素后面第n个元素
     * @param value 指定元素
     * @param n 向后偏移量
     * @return 1-删除成功 0-删除失败
     */
    public int afterDeleteFirst(String value, int n) {
        return (int) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                AFTER_DELETE_FIRST_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                value,
                String.valueOf(n),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Integer.parseInt(result.toString()) : 0;
        });
    }

    /**
     * 删除指定元素前面第n个元素
     * @param value 指定元素
     * @param n 向前偏移量
     * @return 1-删除成功 0-删除失败
     */
    public int beforeDeleteFirst(String value, int n) {
        return (int) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                BEFORE_DELETE_FIRST_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                value,
                String.valueOf(n),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Integer.parseInt(result.toString()) : 0;
        });
    }
}
