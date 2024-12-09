package com.lingque.redis.exten;

import com.lingque.redis.LingQueRedis;
import com.lingque.redis.bean.RedisRank;
import com.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
public class ZSetOpt<T> {
    private LingQueRedis<T> lingQueRedis;

    /**
     * zset是否存在该成员
     * @return
     */
    public Boolean isExistMember(String member) {
        return lingQueRedis.getRedisTemplate().zscore(lingQueRedis.key, member) != null;
    }


    /**
     * 获取zset的成员个数
     * @return
     */
    public long size() {
        return size(Double.MIN_VALUE, Double.MAX_VALUE);
    }


    /**
     * 获取zset的成员个数
     * @return
     */
    public long size(double min,double max) {
        return lingQueRedis.getRedisTemplate().zcount(lingQueRedis.key, min,max);
    }
    /**
     * 设置zSet的Score缓存
     * @param member 成员
     * @param value  值
     */
    public long setScore(String member, Double value) {
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().zadd(lingQueRedis.key, value, member));
    }

    /**
     * 设置zSet的Score缓存
     * @param member 成员
     * @param value  值
     */
    public long add(String member, Double value) {
        return setScore(member,value);
    }

    /**
     * 批量设置zSet的Score缓存
     * @param members map集合
     * @param split 分包，避免过大，建议200
     */
    public void addScoreBatch(Map<String, Double> members,int split) {
      setScoreBatch(members,split);
    }

    /**
     * 批量设置zSet的Score缓存
     * @param members map集合
     * @param split 分包，避免过大，建议200
     */
    public void setScoreBatch(Map<String, Double> members,int split) {
         lingQueRedis.run(() -> {
            if (null != members && members.size() > 0) {
                Map<String, Double> newMap = new HashMap<>(split);
                for (String key : members.keySet()) {
                    newMap.put(key, members.get(key));
                    if (newMap.size() == split) {
                        lingQueRedis.getRedisTemplate().zadd(lingQueRedis.key, newMap);
                        newMap.clear();
                    }
                }
                if (newMap.size() > 0) {
                    lingQueRedis.getRedisTemplate().zadd(lingQueRedis.key, newMap);
                    newMap.clear();
                }
            }
            return true;
        });
    }

    /**
     * zset分数自增
     * @param member 成员
     * @param score  自增分数
     * @return
     */
    public Double incrScore(String member, double score) {
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().zincrby(lingQueRedis.key, score, member));
    }

    /**
     * 如果存在则自增
     * @return 自增成功则返回对应的数，key不存在则返回-1
     */
    public Double incrScoreIfExist(String member) {
        return incrScoreIfExist(member, 1D);
    }

    /**
     * 如果存在则自增
     * @param num 自增数
     * @return 自增成功则返回对应的数，key不存在则返回-1
     */
    public Double incrScoreIfExist(String member, Double num) {
        return getScore(member) > 0 ? lingQueRedis.run(() -> incrScore(member, num)) : -1D;
    }


    /**
     * 根据分数获取范围列表
     * @param min
     * @param max
     * @return
     */
    public List<String> getByScore(Double min, Double max) {
        List<String> list = lingQueRedis.getRedisTemplate().zrangeByScore(lingQueRedis.key,min,max);
        return LQUtil.isEmpty(list) ? new ArrayList<>() : list;
    }

    /**
     * 获取分数
     */
    public Double getScore(String memberId) {
        Double score = lingQueRedis.getRedisTemplate().zscore(lingQueRedis.key, memberId);
        return score == null ? 0 : score;
    }

    /**
     * 获取排名
     * @return
     */
    public List<RedisRank> pageRankLimit(Integer offset, Integer limit) {
        List<String> rankSet = lingQueRedis.getRedisTemplate().zrevrange(lingQueRedis.key, offset, limit);
        if (null == rankSet || rankSet.size() == 0) {
            return Collections.emptyList();
        }
        List<RedisRank> rl = new ArrayList<>();
        rankSet.stream().forEach(id -> rl.add(getRankBean(id)));
        return rl;
    }
    /**
     * 获取名次
     * @param memberId 成员
     * @return
     */
    public Long getRank(String memberId) {
        Long getRank = lingQueRedis.getRedisTemplate().zrank(lingQueRedis.key, memberId);
        return null != getRank ? getRank + 1L : null;
    }

    /**
     * 获取名次bean
     * @param memberId
     * @return
     */
    public RedisRank getRankBean(String memberId) {
        Double score = getScore(memberId);
        return new RedisRank(memberId, score, getRank(memberId));
    }

    /**
     * 移除key
     * @param members
     * @return
     */
    public boolean delete(String ...members){
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().zrem(lingQueRedis.key,members)) > 0;
    }

    /**
     * 根据名次移除
     * @param members
     * @return
     */
    public boolean deleteByRank(long starRank,long stopRank){
        return lingQueRedis.run(()->lingQueRedis.getRedisTemplate().zremrangeByRank(lingQueRedis.key,starRank,stopRank)) > 0;
    }

}