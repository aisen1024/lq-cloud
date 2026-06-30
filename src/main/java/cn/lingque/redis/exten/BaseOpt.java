package cn.lingque.redis.exten;

import cn.lingque.redis.JedisProxy;

import java.util.Objects;

public class BaseOpt {
    /**本对象的操作key*/
    public String key;
    /**缓存时间，单位秒*/
    public Long ttl;
    /**空缓存值*/
    protected String NULL_VALUE = "$$NULL$$";
    /**默认内置的成功标记*/
    public String OK = "ok";
    /**默认内置的失败标记*/
    public String FAIL = "fail";
    
    /**
     * 是否存在
     */
    public Boolean isExistKey() {
        return JedisProxy.execBaseWithRetry((commands) -> commands.exists(key) > 0, 10);
    }

    /**
     * 设置过期时间
     */
    public void resetTTL() {
        resetTTL(ttl);
    }

    /**
     * 获取过期时间
     */
    public Long getTTL() {
        return JedisProxy.execBaseWithRetry((commands) -> commands.ttl(key), 10);
    }

    /**
     * 删除key
     */
    public boolean delete() {
        return JedisProxy.execBaseWithRetry((commands) -> commands.del(key) > 0, 10);
    }

    /**
     * 是否为空缓存
     */
    public boolean isNullCache(Object value) {
        return null != value && Objects.equals(NULL_VALUE, value.toString());
    }

    /**
     * 重置ttl的时间
     * @param t 时间 单位秒
     */
    public void resetTTL(Long t) {
        if (t > 0) {
            JedisProxy.execBaseWithRetry((commands) -> commands.expire(key, t), 10);
        }
    }

    public String getKey(){
        return key;
    }
}
