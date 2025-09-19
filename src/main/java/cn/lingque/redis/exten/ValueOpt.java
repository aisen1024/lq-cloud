package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import io.lettuce.core.ScriptOutputType;
import lombok.Data;

import java.util.List;
import java.util.function.Supplier;

@Data
public class ValueOpt extends BaseOpt {
    private LingQueRedis lingQueRedis;

    private static final String SETNX_AND_EXPIRE_SCRIPT = 
        "local result = redis.call('SETNX', KEYS[1], ARGV[1]); " +
        "if result == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return result;";

    private static final String SET_AND_EXPIRE_SCRIPT =
        "redis.call('SET', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return 1;";

    private static final String INCR_AND_EXPIRE_SCRIPT =
        "local value = redis.call('INCRBY', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return value;";

    private static final String INCR_IF_EXIST_SCRIPT =
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    local value = redis.call('INCRBY', KEYS[1], ARGV[1]); " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "    return value; " +
        "end; " +
        "return nil;";

    public ValueOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    /**
     * 设置缓存，不存在时设置
     * @param value 要设置的值
     * @return 是否设置成功
     */
    public boolean setNx(Object value) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                SETNX_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 设置缓存
     * @param value 要设置的值
     */
    public void set(Object value) {
        set(value, getLingQueRedis().ttl);
    }

    /**
     * 设置缓存
     * @param value 要设置的值
     * @param ttl 过期时间（秒）
     */
    public void set(Object value, long ttl) {
        lingQueRedis.execBase((commands) -> {
            String strValue = LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value);
            if (ttl > 0) {
                commands.eval(
                    SET_AND_EXPIRE_SCRIPT,
                    ScriptOutputType.INTEGER,
                    new String[]{lingQueRedis.key},
                    strValue,
                    String.valueOf(ttl)
                );
            } else {
                commands.set(lingQueRedis.key, strValue);
            }
            return null;
        });
    }

    /**
     * 设置空缓存
     */
    public void setNull() {
        set(NULL_VALUE);
    }

    /**
     * key自增 默认 +1
     * @return 自增后的值
     */
    public long incr() {
        return incr(1L);
    }

    /**
     * key自增指定值并更新过期时间
     * @param num 增加的值
     * @return 自增后的值
     */
    public long incr(long num) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                INCR_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(num),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 如果存在则自增
     * @return 自增后的值，如果key不存在则返回null
     */
    public Long incrIfExist() {
        return incrIfExist(1L);
    }

    /**
     * 如果存在则自增指定值
     * @param num 增加的值
     * @return 自增后的值，如果key不存在则返回null
     */
    public Long incrIfExist(Long num) {
        return (Long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                INCR_IF_EXIST_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(num),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : null;
        });
    }

    /**
     * 获取字符串值
     * @return 字符串值
     */
    public String get() {
        return (String) lingQueRedis.execBase((commands) -> commands.get(lingQueRedis.key));
    }

    /**
     * 获取值并转换为指定类型
     * @param targetClass 目标类型
     * @return 转换后的值
     */
    public <T> T get(Class<T> targetClass) {
        return (T) lingQueRedis.execBase((commands) -> {
            String val = commands.get(lingQueRedis.key);
            if (LQUtil.isEmpty(val)) {
                return null;
            }
            if (isNullCache(val)) {
                return null;
            }
            return LQUtil.isBasClass(targetClass) ?
                LQUtil.baseClassTran(val, targetClass) : 
                JSONUtil.toBean(val, targetClass);
        });
    }

    /**
     * 获取值并转换为List类型
     * @param targetClass List元素的类型
     * @return 转换后的List
     */
    public <T> List<T> getList(Class<T> targetClass) {
        return (List<T>) lingQueRedis.execBase((commands) -> {
            String val = commands.get(lingQueRedis.key);
            if (LQUtil.isEmpty(val)) {
                return null;
            }
            if (isNullCache(val)) {
                return null;
            }
            return JSONUtil.toList(val, targetClass);
        });
    }

    /**
     * 删除key
     * @return 是否删除成功
     */
    @Override
    public boolean delete() {
        return (boolean) lingQueRedis.execBase((commands) -> 
            commands.del(lingQueRedis.key) > 0);
    }

    /**--------------------------------------------高级函授编程---------------------------------------*/


    /**
     * 获取缓存对象
     */
    public <S> S execBeanPlus(Class<S> targetClass, Boolean isSetNull, Supplier<S> function) {
        S value = get(targetClass);
        if (null == value) {
            value = function.get();
            if (null != value) {
                final S finalValue = value;
                set(finalValue);
            } else if (isSetNull) {
                setNull();
            }
        }
        return value;
    }

    /**
     * 获取缓存列表
     */
    public <T> List<T> execListPlus(Class<T> targetClass, Boolean isSetNull, Supplier<List<T>> function) {
        List<T> value = getList(targetClass);
        if (null == value) {
            value = function.get();
            if (null != value) {
                final List<T> finalValue = value;
                set(finalValue);
            } else if (isSetNull) {
                setNull();
            }
        }
        return value;
    }

    /**
     * 延迟加载
     */
    public <T> List<T> execListPlusLazyLoad(Class<T> targetClass, Boolean isSetNull, Supplier<List<T>> function) {
        List<T> value = getList(targetClass);
        if (null == value) {
            value = function.get();
            if (null != value) {
                final List<T> finalValue = value;
                lingQueRedis.execBase((commands) -> {
                    commands.set(lingQueRedis.key, JSONUtil.toJsonStr(finalValue));
                    if (lingQueRedis.ttl > 0) {
                        commands.expire(lingQueRedis.key, lingQueRedis.ttl);
                    }
                    return null;
                });
            } else if (isSetNull) {
                setNull();
            }
        }
        return value;
    }

    /**
     * 缓存时间
     */
    @Data
    public static class CacheBean {
        private Long lastTime;
        private Object data;

        public CacheBean(Long ttl, Object data) {
            this.lastTime = System.currentTimeMillis() + (ttl * 1000);
            this.data = data;
        }

        public boolean isOutTime() {
            return System.currentTimeMillis() >= this.lastTime;
        }
    }


}