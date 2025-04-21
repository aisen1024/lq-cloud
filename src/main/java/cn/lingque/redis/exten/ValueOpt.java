package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.Arrays;
import redis.clients.jedis.Jedis;

@Data
@AllArgsConstructor
public class ValueOpt extends BaseOpt{
    private LingQueRedis lingQueRedis;

    /**
     * 设置缓存，不存在时设置
     *
     * @param value 值
     */
    public boolean setNx(Object value) {
        String luaScript = 
            "local result = redis.call('SETNX', KEYS[1], ARGV[1])\n" +
            "if result == 1 and tonumber(ARGV[2]) > 0 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return result";

        return (boolean)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 设置缓存
     *
     * @param value 值
     */
    public void set(Object value) {
        set(value,getLingQueRedis().ttl);
    }

    /**
     * 设置缓存
     *
     * @param value 值
     */
    public void set(Object value,long ttl) {
        lingQueRedis.execBaseWithRetry((jedis) -> {
            if (ttl == -1L) {
                jedis.set(lingQueRedis.key, LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value));
            } else {
                jedis.setex(lingQueRedis.key, ttl, LQUtil.isBaseValue(value) ? value.toString() : JSONUtil.toJsonStr(value));
            }
            return null;
        });
    }

    /**
     * 设置空缓存
     */
    public void setNull() {
        set(lingQueRedis.NULL_VALUE);
    }

    /**
     * key自增 默认 +1
     * @return 自增后的值
     */
    public long incr() {
        return incr(1);
    }

    /**
     * key自增指定值并更新过期时间
     * @param num 增加的值
     * @return 自增后的值
     */
    public long incr(long num) {
        String luaScript = 
            "local value = redis.call('INCRBY', KEYS[1], ARGV[1])\n" +
            "if value then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return value";

        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    String.valueOf(num),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 如果存在则自增
     * @return 自增成功则返回对应的数，key不存在则返回-1
     */
    public long incrIfExist() {
        return incrIfExist(1L);
    }

    /**
     * 如果存在则自增指定值
     * @param num 自增数
     * @return 自增成功则返回对应的数，key不存在则返回-1
     */
    public long incrIfExist(Long num) {
        String luaScript = 
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    local value = redis.call('INCRBY', KEYS[1], ARGV[1])\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "    return value\n" +
            "else\n" +
            "    return -1\n" +
            "end";

        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    String.valueOf(num),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null ? Long.parseLong(result.toString()) : -1;
        });
    }

    /**
     * 自减,默认-1
     * @return 自减后的值
     */
    public long decr() {
        return decr(1L);
    }

    /**
     * 自减指定值并更新过期时间
     * @param num 自减数（必须为正整数）
     * @return 自减后的值
     */
    public long decr(Long num) {
        if (num < 0) {
            throw new RuntimeException("num 必须为正整数！");
        }

        String luaScript = 
            "local value = redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
            "if value then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return value";

        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    String.valueOf(num),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 如果存在则自减
     * @return 自减成功则返回对应的数，key不存在则返回-1
     */
    public Long decrIfExist() {
        return decrIfExist(1L);
    }

    /**
     * 如果存在则自减指定值
     * @param num 自减数
     * @return 自减成功则返回对应的数，key不存在则返回-1
     */
    public Long decrIfExist(Long num) {
        String luaScript = 
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    local value = redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "    return value\n" +
            "else\n" +
            "    return -1\n" +
            "end";

        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    luaScript,
                    Collections.singletonList(lingQueRedis.key),
                    Arrays.asList(
                            String.valueOf(num),
                            String.valueOf(lingQueRedis.ttl)
                    )
            );
            return result != null ? Long.parseLong(result.toString()) : -1;
        });
    }

    /**
     * 获取缓存的值,支持基础类型和对象bean
     *
     * @param targetClass 需要转换的类型
     * @param <T>         返回值的类型
     * @return 缓存值
     */
    public <T> T getValue(Class<T> targetClass) {
        return (T)lingQueRedis.execBaseWithRetry((jedis) -> {
            String obj = jedis.get(lingQueRedis.key);
            if (lingQueRedis.isNullCache(obj)) {
                return null;
            }
            if (null != obj) {
                if (LQUtil.isBasClass(targetClass)) {
                    return (T) LQUtil.baseClassTran(obj, targetClass);
                }
                return LQUtil.jsonToBean(obj.toString(), targetClass);
            }
            return null;
        });
    }

    /**
     * 获取缓存的值,支持基础类型和对象bean，空返回默认值
     * @param targetClass
     * @param defaultValue
     * @return
     * @param <T>
     */
    public <T> T getValue(Class<T> targetClass,T defaultValue) {
        T value = getValue(targetClass);
        return null == value ? defaultValue : value;
    }

    /**
     * 获取集合对象
     *
     * @param targetClass
     * @return
     */
    public <T> List<T> getListValue(Class<T> targetClass) {
        String json = getValue(String.class);
        if (LQUtil.isNotEmpty(json)) {
            return JSONUtil.toList(json, targetClass);
        }
        //防止直接操作list出现异常，正常new一个集合
        return new ArrayList<>();
    }

    /**--------------------------------------------高级函授编程---------------------------------------*/


    /**
     * 获取缓存对象
     *
     * @param function    查询mysql的函数
     * @param targetClass 返回目标对象的class
     * @param isSetNull   是否空的时候插入空对象，防止缓存穿透
     * @param <S>
     * @return
     */
    public <S> S execBeanPlus(Class<S> targetClass, Boolean isSetNull, Supplier<S> function) {
        Object value = getValue(targetClass);
        if (getLingQueRedis().isNullCache(value)) {
            return null;
        }
        if (null == value) {
            S s = function.get();
            if (null != s) {
                set(s);
            } else if (isSetNull) {
                setNull();
            }
            return s;
        }
        return targetClass == String.class ? (S) value.toString() : LQUtil.jsonToBean(value.toString(), targetClass);
    }


    /**
     * 集合对象
     *
     * @param targetClass
     * @return
     */
    public <T> List<T> execListPlus(Class<T> targetClass, Boolean isSetNull, Supplier<List<T>> function) {
        Object result = getValue(targetClass);
        if (getLingQueRedis().isNullCache(result)) {
            return Collections.emptyList();
        }
        if (null == result) {
            List<T> tl = function.get();
            if (null != tl) {
               set(JSONUtil.toJsonStr(tl));
            } else if (isSetNull) {
               setNull();
            }
            return tl;
        }
        return JSONUtil.toList(result.toString(), targetClass);
    }

    /**
     * 延迟加载
     * @param targetClass
     * @param isSetNull
     * @param function
     * @return
     */
    public <T> List<T> execListPlusLazyLoad(Class<T> targetClass, Boolean isSetNull, Supplier<List<T>> function) {
        Object result = getValue(String.class);
        if (getLingQueRedis().isNullCache(result)) {
            return Collections.emptyList();
        }
        if (null == result) {
            List<T> tl = function.get();
            if (null != tl) {
                if (lingQueRedis.ttl > 0) {
                    set(new CacheBean(lingQueRedis.ttl, tl), lingQueRedis.ttl * 2);
                } else {
                    set(JSONUtil.toJsonStr(new CacheBean(lingQueRedis.ttl, tl)));
                }
            } else if (isSetNull) {
                setNull();
            }
            return tl;
        }
        CacheBean cacheBean = JSONUtil.toBean(result.toString(), CacheBean.class);
        //过时了，试着去更新
        if (cacheBean.isOutTime()) {
            try {
                    CacheBean updateBean = (CacheBean) getLingQueRedis().ofLock().<CacheBean>lockFuture(() -> {
                    List<T> tl = function.get();
                    CacheBean cache = new CacheBean(getLingQueRedis().ttl, tl);
                    if (null != tl) {
                        if (getLingQueRedis().ttl > 0) {
                            set(JSONUtil.toJsonStr(cache), getLingQueRedis().ttl * 2);
                        } else {
                            set(JSONUtil.toJsonStr(cache));
                        }
                        return cache;
                    } else if (isSetNull) {
                        setNull();
                    }
                    return cache;
                });
                if (null != updateBean) {
                    cacheBean = updateBean;
                }
            } catch (Exception e) {
                //nothing to do
            }
        }

        return JSONUtil.toList(JSONUtil.toJsonStr(cacheBean.getData()), targetClass);
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