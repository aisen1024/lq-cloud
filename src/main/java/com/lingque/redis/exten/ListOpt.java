package com.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import com.lingque.exceptions.LQException;
import com.lingque.redis.LingQueRedis;
import com.lingque.util.LQUtil;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author aisen
 * @date 2024/9/25
 * @desc 简单说一下
 **/
@AllArgsConstructor
public class ListOpt<T> {

    private LingQueRedis<T> lingQueRedis;

    /**
     *  追加数据
     * @param member 成员
     */
    public long add(Object member) {
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().rpush(lingQueRedis.key, LQUtil.isBaseValue(member)?member.toString():JSONUtil.toJsonStr(member)));
    }

    /**
     * 向制定的位置插入数据
     * @param member 成员
     * @param index 要在哪个位置下插入
     */
    public String add(Object member,int index) {
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().lset(lingQueRedis.key,index, LQUtil.isBaseValue(member)?member.toString():JSONUtil.toJsonStr(member)));
    }

    /**
     *  获取集合个数
     */
    public long size() {
        return lingQueRedis.getRedisTemplate().llen(lingQueRedis.key);
    }

    /**
     *  通过下标获取对应的数据
     */
    public String get(int index) {
        return lingQueRedis.getRedisTemplate().lindex(lingQueRedis.key,index);
    }

    /**
     * list左边追加队列（允许重复）
     * @param member 成员
     */
    public long leftPush(String ...member) {
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().lpushx(lingQueRedis.key,member));
    }

    /**
     * list右边追加队列（允许重复）
     * @param member 成员
     */
    public long rightPush(String ...member) {
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().rpushx(lingQueRedis.key,member));
    }

    /**
     * 分页获取
     *
     * @param targetClass
     * @param page
     * @param pageSize
     * @return
     */
    public List<T> page(Class<T> targetClass, Integer page, Integer pageSize) {
        page = null == page || page < 1 ? 1 : page;
        pageSize = null == pageSize || pageSize < 1 ? 10 : pageSize;
        Integer offset = (page - 1) * pageSize;
        Integer limit = pageSize + offset - 1;
        List<String> ls = lingQueRedis.getRedisTemplate().lrange(lingQueRedis.key, offset, limit);
        if (null != ls && ls.size() > 0) {
            return ls.stream().map(item -> JSONUtil.toBean(item, targetClass)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    /**
     * 移除所有跟member值一样的元素
     * @param member
     * @return
     */
    public long deleteAllSame(String member){
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().lrem(lingQueRedis.key,0,member));

    }

    /**
     * 从左边开始匹配（首部），删除第一个匹配到的元素
     * @param member
     * @return
     */
    public long beforeDeleteFirst(String member,int count){
        //不能为0
        if (count == 0){
            throw new LQException("count 不能为0！");
        }
        count = Math.abs(count);
        int finalCount = count;
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().lrem(lingQueRedis.key, finalCount,member));

    }

    /**
     * 从右边开始匹配（尾部），删除count个匹配到的元素
     * @param member
     * @param count 要删掉几个元素
     * @return
     */
    public long afterDeleteFirst(String member,int count){
        //不能为0
        if (count == 0){
           throw new LQException("count 不能为0！");
        }
        count = Math.abs(count);
        int finalCount = count * -1;
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().lrem(lingQueRedis.key, finalCount ,member));

    }


    /**
     * 从右边弹出元素
     * @param count 元素个数
     * @return
     */
    public List<String> rpops(int count){
       return lingQueRedis.getRedisTemplate().rpop(lingQueRedis.key,count);
    }


    /**
     * 从左边弹出元素
     * @param count 元素个数
     * @return
     */
    public List<String> lpops(int count){
        return lingQueRedis.getRedisTemplate().lpop(lingQueRedis.key,count);
    }



}
