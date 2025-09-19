package cn.lingque.base;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import cn.lingque.mq.exten.LQStreamUnifiedQueue;
import cn.lingque.mq.exten.LQUnifiedQueue;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.redis.exten.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Random;

/**
 * <p>
 * 基础缓存key  三要素：key前缀，版本号，过时时间
 * </p>
 *
 * @author zlm
 * @date 2022/8/8
 * @since 1.0
 */
@Slf4j
public class LQKey<T> {
    /**
     * 缓存key前缀
     */
    @Getter
    protected String prefixKey;
    /**
     * 版本
     */
    protected Double version;
    /**
     * 过时时间,-1L代表不过时
     */
    protected Long ttl;


    /**空缓存值*/
    public String NULL_VALUE = "$$NULL$$";

    /**默认内置的成功标记*/
    public String OK = "ok";

    /**默认内置的失败标记*/
    public String FAIL = "fail";

    public static LQKey key(String prefixKey, Double version, Long ttl) {
        LQKey LQRKey = new LQKey();
        LQRKey.prefixKey = prefixKey;
        LQRKey.version = version;
        LQRKey.ttl = ttl;
        return LQRKey;
    }

    public LockOpt ofLock(Object... params){
        return new LockOpt(rd(params));
    }
    public ValueOpt ofV(Object... params) {
        return new ValueOpt(rd(params));
    }

    public SetOpt ofS(Object... params) {
        return new SetOpt(rd(params));
    }

    public SortedSetOpt ofZ(Object... params) {
        return new SortedSetOpt(rd(params));
    }

    public HashOpt ofH(Object... params) {
        return new HashOpt(rd(params));
    }

    public GeoOpt ofG(Object... params) {
        return new GeoOpt(rd(params));
    }

    public ListOpt ofL(Object... params) {
        return new ListOpt(rd(params));
    }


    public LQUnifiedQueue<T> ofUnifiedQueue(Object... params) {
        return new LQUnifiedQueue<>(rd(params));
    }

    public LQStreamUnifiedQueue<T> ofStreamUnifiedQueue(Object... params) {
        return new LQStreamUnifiedQueue<>(rd(params));
    }


    /**
     * 构建一个完整的key
     *
     * @param params
     * @return
     */
    public String buildKey(Object... params) {
        StringBuilder newKey = new StringBuilder(prefixKey);
        for (Object p : params) {
            if (p instanceof Long) {
                newKey.append(":").append(Long.toString((long) p));
            } else {
                newKey.append(":").append(p.toString());
            }
        }
        newKey.append(":").append(version);
        return newKey.toString();
    }

    /**
     * 构建redis
     */
    public LingQueRedis rd(Object... params) {
        return LingQueRedis.ofKey(this, params);
    }

    /**
     * 设置缓存时间
     *
     * @return
     */
    public Long getTtl() {
        return ttl;
    }

    /***
     * ttl标准时间
     */

    public static final Long TEN_SECONDS = 10L;

    public static final Long _30_SECONDS = 30L;

    public static final Long ONE_MINUTE = 60L;

    public static final Long TWO_MINUTE = 120L;

    public static final Long FIVE_MINUTE = 300L;

    public static final Long TEN_MINUTE = 600L;

    public static final Long HALF_HOUR = 1800L;

    public static final Long ONE_HOUR = 3600L;

    public static final Long HALF_DAY = 43200L;

    public static final Long ONE_DAY = 86400L;

    public static final Long ONE_WEEK = 86400 * 7L;

    public static final Long HALF_MONTH = 86400 * 15L;

    public static final Long ONE_MONTH = 86400 * 30L;

    public static final Long FOREVER = DateUtil.offset(new Date(), DateField.YEAR, 100).getTime() / 1000L; //永久

    private static final Random RAND = new Random();

    public static Long random12HTo24H() {
        return (RAND.nextInt(13) + 12) * ONE_HOUR * 1L;
    }
}
