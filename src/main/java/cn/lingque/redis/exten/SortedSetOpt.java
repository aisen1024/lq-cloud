package cn.lingque.redis.exten;

import cn.lingque.redis.LingQueRedis;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
public class SortedSetOpt extends BaseOpt{
    private LingQueRedis lingQueRedis;

    public SortedSetOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    // Lua 脚本常量
    private static final String ZADD_SCRIPT = 
        "local res = redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return res;";

    private static final String ZADD_BATCH_SCRIPT = 
        "local count = 0; " +
        "for i = 1, #ARGV-1, 2 do " +
        "    count = count + redis.call('ZADD', KEYS[1], ARGV[i], ARGV[i+1]); " +
        "end; " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String ZINCRBY_SCRIPT = 
        "local score = redis.call('ZINCRBY', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return score;";

    private static final String ZREM_SCRIPT = 
        "local res = redis.call('ZREM', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "if res > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "end; " +
        "return res;";

    private static final String ZREMRANGEBYRANK_SCRIPT = 
        "local res = redis.call('ZREMRANGEBYRANK', KEYS[1], ARGV[1], ARGV[2]); " +
        "if res > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return res;";

    private static final String PAGE_RANK_LIMIT_SCRIPT = 
        "local result = {}; " +
        "local members = redis.call('ZREVRANGE', KEYS[1], ARGV[1], ARGV[2]); " +
        "for i, member in ipairs(members) do " +
        "    local score = redis.call('ZSCORE', KEYS[1], member); " +
        "    local rank = redis.call('ZREVRANK', KEYS[1], member); " +
        "    table.insert(result, member); " +
        "    table.insert(result, score); " +
        "    table.insert(result, rank + 1); " +
        "end; " +
        "return result;";

    private static final String GET_RANK_BEAN_SCRIPT = 
        "local result = {}; " +
        "local score = redis.call('ZSCORE', KEYS[1], ARGV[1]); " +
        "if score then " +
        "    local rank = redis.call('ZREVRANK', KEYS[1], ARGV[1]); " +
        "    table.insert(result, ARGV[1]); " +  // member
        "    table.insert(result, score); " +     // score
        "    table.insert(result, rank + 1); " +  // rank
        "end; " +
        "return result;";

    private static final String GET_BY_SCORE_RANGE_SCRIPT = 
        "local result = {}; " +
        "local members = redis.call('ZRANGEBYSCORE', KEYS[1], ARGV[1], ARGV[2], 'LIMIT', 0, ARGV[3]); " +
        "for i, member in ipairs(members) do " +
        "    local score = redis.call('ZSCORE', KEYS[1], member); " +
        "    local rank = redis.call('ZREVRANK', KEYS[1], member); " +
        "    table.insert(result, member); " +
        "    table.insert(result, score); " +
        "    table.insert(result, rank + 1); " +
        "end; " +
        "return result;";

    private static final String IS_EXIST_MEMBER_SCRIPT = 
        "local score = redis.call('ZSCORE', KEYS[1], ARGV[1]); " +
        "return score ~= false;";

    private static final String SIZE_SCRIPT = 
        "return redis.call('ZCOUNT', KEYS[1], ARGV[1], ARGV[2]);";

    private static final String GET_SCORE_SCRIPT = 
        "local score = redis.call('ZSCORE', KEYS[1], ARGV[1]); " +
        "return score or 0;";

    private static final String GET_RANK_SCRIPT = 
        "local rank = redis.call('ZRANK', KEYS[1], ARGV[1]); " +
        "return rank and (rank + 1) or nil;";

    /**
     * 检查有序集合中是否存在指定成员
     * @param member 成员标识
     * @return true-存在 false-不存在
     */
    public Boolean isExistMember(String member) {
        return (Boolean)lingQueRedis.execBase((jedis) -> {
            List<String> params = new ArrayList<>();
            params.add(member);
            Object result = jedis.eval(
                    IS_EXIST_MEMBER_SCRIPT,
                    getKey(),
                    params
            );
            return result != null && (Long) result > 0;
        });
    }

    /**
     * 获取有序集合中的成员总数
     * @return 成员总数
     */
    public long size() {
        return (long)lingQueRedis.execBase((jedis) -> {
            return size(Double.MIN_VALUE, Double.MAX_VALUE);
        });
    }

    /**
     * 获取指定分数范围内的成员数量
     * @param min 最小分数
     * @param max 最大分数
     * @return 指定范围内的成员数量
     */
    public long size(double min, double max) {
        return (long)lingQueRedis.execBase((jedis) -> {
            List<String> params = new ArrayList<>();
            params.add(Double.toString(min));
            params.add(Double.toString(max));

            Object result = jedis.eval(
                    SIZE_SCRIPT,
                    getKey(),
                    params
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 添加或更新成员分数
     * @param member 成员标识
     * @param score 分数
     * @return 成功添加的成员数量（新成员为1，已存在成员更新分数为0）
     */
    public long setScore(String member, Double score) {
        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    ZADD_SCRIPT,
                    getKey(),
                    Arrays.asList(
                            Double.toString(score),
                            member,
                            Long.toString(lingQueRedis.ttl)
                    )
            );
            return TryCatch.tryResult(()->(long) result,-1L);
        });
        
    }
    private List<String> getKey(){
        List<String> keys = new ArrayList<>();
        keys.add(lingQueRedis.key);
        return keys;
    }

    /**
     * 批量添加或更新成员分数
     * @param members 成员分数映射
     * @param split 分批大小（建议200）
     */
    public void setScoreBatch(Map<String, Double> members, int split) {
            if (LQUtil.isEmpty(members)) {
                return ;
            }
            List<Map<String, Double>> batches = new ArrayList<>();
            Map<String, Double> batch = new HashMap<>(split);

            for (Map.Entry<String, Double> entry : members.entrySet()) {
                batch.put(entry.getKey(), entry.getValue());
                if (batch.size() >= split) {
                    batches.add(new HashMap<>(batch));
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                batches.add(batch);
            }
        lingQueRedis.execBase((jedis) -> {
            for (Map<String, Double> batchMap : batches) {
                List<String> args = new ArrayList<>();
                for (Map.Entry<String, Double> entry : batchMap.entrySet()) {
                    args.add(String.valueOf(entry.getValue()));
                    args.add(entry.getKey());
                }
                args.add(String.valueOf(lingQueRedis.getTTL()));
                jedis.eval(
                        ZADD_BATCH_SCRIPT,
                        getKey(),
                        args
                );
            }
            return null;
        });
       
    }

    /**
     * 增加成员分数
     * @param member 成员标识
     * @param score 增加的分数值
     * @return 增加后的最终分数
     */
    public Double incrScore(String member, double score) {
        return (Double)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    ZINCRBY_SCRIPT,
                    getKey(),
                    Arrays.asList(
                            Double.toString(score),
                            member,
                            Long.toString(lingQueRedis.ttl)
                    )
            );
            return result == null ? 0 : Double.valueOf(result.toString());
        });
    }

    /**
     * 删除指定成员
     * @param members 要删除的成员数组
     * @return true-删除成功 false-删除失败
     */
    public boolean delete(String... members) {
        if (members == null || members.length == 0){
            return true;
        }
        List<String> args =new ArrayList<>();
        args.addAll(Arrays.asList(members));
        args.add(lingQueRedis.getTTL().toString());
        return (boolean)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    ZREM_SCRIPT,
                    getKey(),
                    args
            );
            return TryCatch.tryResult(()->(long) result,-1L) > 0;
        });
    }

    /**
     * 获取成员分数
     * @param memberId 成员ID
     * @return 成员分数，不存在返回0
     */
    public Double getScore(String memberId) {
        List<String> params = new ArrayList<>();
        params.add(memberId);
        return (double)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    GET_SCORE_SCRIPT,
                    getKey(),
                    params
            );
            return result != null ? Double.valueOf(result.toString()) : 0D;
        });
    }

    /**
     * 获取指定范围的排名列表，包含成员信息、分数和排名
     * @param offset 起始位置（从0开始）
     * @param limit 结束位置
     * @return 排名列表
     */
    public List<RedisRank> pageRankLimit(Integer offset, Integer limit) {
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(offset));
        params.add(String.valueOf(limit));

        Object result = lingQueRedis.execBase((jedis) -> {
            return jedis.eval(
                    PAGE_RANK_LIMIT_SCRIPT,
                    getKey(),
                    params
            );
        });
        List<RedisRank> rankList = new ArrayList<>();
        try {
            List<Object> resultList = (List<Object>) result;
            // 每三个元素组成一个 RedisRank 对象
            for (int i = 0; i < resultList.size(); i += 3) {
                String member = resultList.get(i).toString();
                Double score = Double.valueOf(resultList.get(i + 1).toString());
                Long rank = Long.valueOf(resultList.get(i + 2).toString());
                rankList.add(new RedisRank(member, score, rank));
            }
        }catch (Exception e){
            //忽略
        }
        return rankList;
    }
    /**
     * 获取成员排名
     * @param memberId 成员ID
     * @return 成员排名（从1开始），不存在返回null
     */
    public Long getRank(String memberId) {
        return (Long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    GET_RANK_SCRIPT,
                    getKey(),
                    Arrays.asList(memberId)
            );
            return result != null ? Long.valueOf(result.toString()) : null;
        });
    }

    /**
     * 获取成员的排名信息
     * @param memberId 成员ID
     * @return RedisRank对象，包含成员ID、分数和排名信息
     */
    public RedisRank getRankBean(String memberId) {
        List<String> params = new ArrayList<>();
        params.add(memberId);

        Object result = lingQueRedis.execBase((jedis) -> {
           return jedis.eval(
                    GET_RANK_BEAN_SCRIPT,
                    getKey(),
                    params
            );
        });
        if (result == null) {
            return new RedisRank(memberId, 0D, null);
        }
        try {
            List<Object> resultList = (List<Object>) result;
            if (resultList.isEmpty()) {
                return new RedisRank(memberId, 0D, null);
            }

            return new RedisRank(
                    resultList.get(0).toString(),
                    Double.valueOf(resultList.get(1).toString()),
                    Long.valueOf(resultList.get(2).toString())
            );
        }catch (Exception e){
            return  new RedisRank(memberId, 0D, null);
        }

    }

    /**
     * 根据排名范围删除成员
     * @param startRank 起始排名（从0开始）
     * @param stopRank 结束排名
     * @return true-删除成功 false-删除失败
     */
    public boolean deleteByRank(long startRank, long stopRank) {
        return (boolean)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    ZREMRANGEBYRANK_SCRIPT,
                    getKey(),
                    Arrays.asList(Long.toString(startRank), Long.toString(stopRank),Long.toString(lingQueRedis.getTTL()))
            );
            return (Long) result > 0;
        });
    }

    /**
     * 根据分数范围获取成员信息列表
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param limit 返回的最大数量
     * @return 成员信息列表，包含成员ID、分数和排名
     */
    public List<RedisRank> getByScoreRange(double minScore, double maxScore, int limit) {
      return getByScoreRange(Double.toString(minScore), Double.toString(maxScore), limit);
    }

    /**
     * 根据分数范围获取成员信息列表
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param limit 返回的最大数量
     * @return 成员信息列表，包含成员ID、分数和排名
     */
    public List<RedisRank> getByScoreRange(long minScore, long maxScore, int limit) {
        return getByScoreRange(Long.toString(minScore), Long.toString(maxScore), limit);
    }

    private List<RedisRank> getByScoreRange(String minScore, String maxScore, int limit) {
        return (List<RedisRank>)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    GET_BY_SCORE_RANGE_SCRIPT,
                    getKey(),
                    Arrays.asList(minScore,maxScore,Integer.toString(limit))
            );

            if (result == null) {
                return new ArrayList<>();
            }
            List<RedisRank> rankList = new ArrayList<>();
            try {
                List<Object> resultList = (List<Object>) result;
                // 每三个元素组成一个 RedisRank 对象
                for (int i = 0; i < resultList.size(); i += 3) {
                    String member = resultList.get(i).toString();
                    Double score = Double.valueOf(resultList.get(i + 1).toString());
                    Long rank = Long.valueOf(resultList.get(i + 2).toString());
                    rankList.add(new RedisRank(member, score, rank));
                }
            }catch (Exception e){
                //忽略
            }

            return rankList;
        });

    }

    public List<String> getMembers(double minScore, double maxScore){
        return (List<String>)lingQueRedis.execBase((jedis) -> {
         return   jedis.zrangeByScore(lingQueRedis.key,minScore,maxScore);
        });
    }


}