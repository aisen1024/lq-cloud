package com.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import com.lingque.redis.LingQueRedis;
import com.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class SetOpt<T> {
    private LingQueRedis<T> lingQueRedis;
    /**
     * set添加数据
     * @param obj
     */
    public void add(Object obj) {
        lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().sadd(lingQueRedis.key, LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj.toString())));
    }
    /**
     * set添加数据
     * @param obj
     */
    public boolean deleteMember(Object obj) {
       return lingQueRedis.getRedisTemplate().srem(lingQueRedis.key, LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj.toString())) > 0;
    }

    /**
     * 获取set的成员个数
     * @return
     */
    public long size() {
        return lingQueRedis.getRedisTemplate().scard(lingQueRedis.key);
    }

    /**
     * 判断 是否是set集合中的元素
     * @param obj
     */
    public boolean isMembers(Object obj) {
        Boolean member = lingQueRedis.getRedisTemplate().sismember(lingQueRedis.key, obj.toString());
        return null != member && member;
    }

    /**
     * 随机抽取集合
     * @param count
     * @return
     */
    public List<T> randomMembers(Class<T> targetClass, Integer count) {
        List<String> list = lingQueRedis.getRedisTemplate().srandmember(lingQueRedis.key, count);
        if (null == list || list.isEmpty()) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(JSONUtil.toJsonStr(list), targetClass);
    }

    /**
     * 随机抽取一个成员
     * @return
     */
    public <T> T randomMember(Class<T> targetClass) {
        String member = lingQueRedis.getRedisTemplate().srandmember(lingQueRedis.key);
        if (LQUtil.isEmpty(member)) {
            return null;
        }
        return LQUtil.isBasClass(targetClass) ? LQUtil.baseClassTran(member, targetClass) : JSONUtil.toBean(member, targetClass);
    }

    /**
     * 获取set集合元素
     * @return
     */
    public List<T> getMembers(Class<T> targetClass) {
        Set<String> set = lingQueRedis.getRedisTemplate().smembers(lingQueRedis.key);
        if (null == set || set.isEmpty()) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(JSONUtil.toJsonStr(set), targetClass);
    }

    /**
     * 弹出一个元素
     *
     * @return
     */
    public String pop() {
        return lingQueRedis.run(()-> lingQueRedis.getRedisTemplate().spop(lingQueRedis.key));
    }

    /**
     * 弹出多个元素
     *
     * @return
     */
    public List<T> pops(long count, Class<T> targetClass) {
        return lingQueRedis.run(()-> JSONUtil.toList(JSONUtil.toJsonStr(lingQueRedis.getRedisTemplate().spop(lingQueRedis.key,count)),targetClass));
    }


}

