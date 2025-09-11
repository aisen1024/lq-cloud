package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.redis.bean.RedisRank;
import cn.lingque.util.LQUtil;
import cn.lingque.util.TryCatch;
import io.lettuce.core.Range;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.ScoredValue;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SortedSetOpt extends BaseOpt {
    private LingQueRedis lingQueRedis;

    private static final String ZADD_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return count;";

    private static final String BATCH_ZADD_AND_EXPIRE_SCRIPT =
        "local count = 0; " +
        "for i = 1, #ARGV-1, 2 do " +
        "    count = count + redis.call('ZADD', KEYS[1], ARGV[i], ARGV[i+1]); " +
        "end; " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String ZREM_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZREM', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return count;";

    private static final String BATCH_ZREM_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZREM', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "end; " +
        "return count;";

    private static final String ZINCRBY_AND_EXPIRE_SCRIPT =
        "local score = redis.call('ZINCRBY', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return score;";

    private static final String ZPOPMIN_AND_EXPIRE_SCRIPT =
        "local result = redis.call('ZPOPMIN', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return result;";

    private static final String ZPOPMAX_AND_EXPIRE_SCRIPT =
        "local result = redis.call('ZPOPMAX', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return result;";

    private static final String ZREMRANGEBYRANK_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZREMRANGEBYRANK', KEYS[1], ARGV[1], ARGV[2]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return count;";

    private static final String ZREMRANGEBYSCORE_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZREMRANGEBYSCORE', KEYS[1], ARGV[1], ARGV[2]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return count;";

    private static final String ZREMRANGEBYLEX_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZREMRANGEBYLEX', KEYS[1], ARGV[1], ARGV[2]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return count;";

    private static final String ZCOUNT_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZCOUNT', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return count;";

    private static final String ZLEXCOUNT_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZLEXCOUNT', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return count;";

    private static final String ZSCORE_AND_EXPIRE_SCRIPT =
        "local score = redis.call('ZSCORE', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return score;";

    private static final String ZRANK_AND_EXPIRE_SCRIPT =
        "local rank = redis.call('ZRANK', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return rank;";

    private static final String ZREVRANK_AND_EXPIRE_SCRIPT =
        "local rank = redis.call('ZREVRANK', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return rank;";

    private static final String ZCARD_AND_EXPIRE_SCRIPT =
        "local count = redis.call('ZCARD', KEYS[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
        "return count;";

    private static final String IS_EXIST_MEMBER_SCRIPT =
        "local exists = redis.call('ZSCORE', KEYS[1], ARGV[1]); " +
        "if exists then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "    return 1; " +
        "end; " +
        "return 0;";

    private static final String SIZE_SCRIPT =
        "local size = redis.call('ZCARD', KEYS[1]); " +
        "if size > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
        "end; " +
        "return size;";

    private static final String GET_SCORE_SCRIPT =
        "local score = redis.call('ZSCORE', KEYS[1], ARGV[1]); " +
        "if score then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return score;";

    private static final String PAGE_RANK_LIMIT_SCRIPT =
        "local total = redis.call('ZCARD', KEYS[1]); " +
        "local members = redis.call('ZRANGE', KEYS[1], ARGV[1], ARGV[2], 'WITHSCORES'); " +
        "if total > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return {total, members};";

    private static final String GET_RANK_SCRIPT =
        "local rank = redis.call('ZRANK', KEYS[1], ARGV[1]); " +
        "if rank then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return rank;";

    private static final String GET_RANK_BEAN_SCRIPT =
        "local result = {}; " +
        "for i = 1, #ARGV-1 do " +
        "    local rank = redis.call('ZRANK', KEYS[1], ARGV[i]); " +
        "    local score = redis.call('ZSCORE', KEYS[1], ARGV[i]); " +
        "    if rank and score then " +
        "        table.insert(result, ARGV[i]); " +
        "        table.insert(result, rank); " +
        "        table.insert(result, score); " +
        "    end; " +
        "end; " +
        "if #result > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "end; " +
        "return result;";

    private static final String ZADD_SCRIPT =
        "local count = redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "return count;";

    private static final String ZADD_BATCH_SCRIPT =
        "local count = 0; " +
        "for i = 1, #ARGV-1, 2 do " +
        "    count = count + redis.call('ZADD', KEYS[1], ARGV[i], ARGV[i+1]); " +
        "end; " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String GET_BY_SCORE_RANGE_SCRIPT =
        "local members = redis.call('ZRANGEBYSCORE', KEYS[1], ARGV[1], ARGV[2], 'WITHSCORES', 'LIMIT', 0, ARGV[3]); " +
        "if #members > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[4]); " +
        "end; " +
        "local result = {}; " +
        "for i = 1, #members, 2 do " +
        "    local member = members[i]; " +
        "    local score = members[i + 1]; " +
        "    local rank = redis.call('ZRANK', KEYS[1], member); " +
        "    table.insert(result, member); " +
        "    table.insert(result, score); " +
        "    table.insert(result, rank); " +
        "end; " +
        "return result;";

    public SortedSetOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    /**
     * 添加一个成员到有序集合
     * @param score 分数
     * @param member 成员
     * @return 是否成功添加（1表示新增，0表示更新）
     */
    public long zadd(double score, Object member) {
        return (long) lingQueRedis.execBase((commands) -> {
            String memberStr = LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member);
            Object result = commands.eval(
                ZADD_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(score),
                memberStr,
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 批量添加成员到有序集合
     * @param scoreMembers Map<成员, 分数>
     * @return 成功添加的新成员数量
     */
    public long zaddAll(Map<Object, Double> scoreMembers) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[scoreMembers.size() * 2 + 1];
            int i = 0;
            for (Map.Entry<Object, Double> entry : scoreMembers.entrySet()) {
                args[i++] = String.valueOf(entry.getValue());
                args[i++] = LQUtil.isBaseValue(entry.getKey()) ? 
                    entry.getKey().toString() : 
                    JSONUtil.toJsonStr(entry.getKey());
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                ZADD_BATCH_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 从有序集合中删除指定成员
     * @param member 要删除的成员
     * @return 成功删除的成员数量
     */
    public long zrem(Object member) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZREM_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 批量删除有序集合中的成员
     * @param members 要删除的成员列表
     * @return 成功删除的成员数量
     */
    public long zremAll(Collection<?> members) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[members.size() + 1];
            int i = 0;
            for (Object member : members) {
                args[i++] = LQUtil.isBaseValue(member) ? 
                    member.toString() : 
                    JSONUtil.toJsonStr(member);
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                BATCH_ZREM_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 增加成员的分数
     * @param increment 增加的分数
     * @param member 成员
     * @return 增加后的分数
     */
    public double zincrby(double increment, Object member) {
        return (double) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZINCRBY_AND_EXPIRE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                String.valueOf(increment),
                LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Double.parseDouble(result.toString()) : 0.0;
        });
    }

    /**
     * 获取有序集合的大小
     * @return 集合大小
     */
    public long size() {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                SIZE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 获取成员的分数
     * @param member 成员
     * @return 分数，如果成员不存在则返回null
     */
    public Double zscore(Object member) {
        return (Double) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZSCORE_AND_EXPIRE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Double.parseDouble(result.toString()) : null;
        });
    }

    /**
     * 获取成员的排名（从小到大，0开始）
     * @param member 成员
     * @return 排名，如果成员不存在则返回null
     */
    public Long zrank(Object member) {
        return (Long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZRANK_AND_EXPIRE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : null;
        });
    }

    /**
     * 获取成员的排名（从大到小，0开始）
     * @param member 成员
     * @return 排名，如果成员不存在则返回null
     */
    public Long zrevrank(Object member) {
        return (Long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZREVRANK_AND_EXPIRE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : null;
        });
    }

    /**
     * 弹出分数最小的成员
     * @param count 要弹出的成员数量
     * @return 成员和分数的列表
     */
    public List<ScoredValue<String>> zpopmin(long count) {
        return (List<ScoredValue<String>>) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZPOPMIN_AND_EXPIRE_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                String.valueOf(count),
                String.valueOf(lingQueRedis.ttl)
            );
            if (!(result instanceof List)) {
                return Collections.emptyList();
            }
            List<Object> list = (List<Object>) result;
            List<ScoredValue<String>> scoredValues = new ArrayList<>();
            for (int i = 0; i < list.size(); i += 2) {
                String member = list.get(i).toString();
                double score = Double.parseDouble(list.get(i + 1).toString());
                scoredValues.add(ScoredValue.just(score, member));
            }
            return scoredValues;
        });
    }

    /**
     * 弹出分数最大的成员
     * @param count 要弹出的成员数量
     * @return 成员和分数的列表
     */
    public List<ScoredValue<String>> zpopmax(long count) {
        return (List<ScoredValue<String>>) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZPOPMAX_AND_EXPIRE_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                String.valueOf(count),
                String.valueOf(lingQueRedis.ttl)
            );
            if (!(result instanceof List)) {
                return Collections.emptyList();
            }
            List<Object> list = (List<Object>) result;
            List<ScoredValue<String>> scoredValues = new ArrayList<>();
            for (int i = 0; i < list.size(); i += 2) {
                String member = list.get(i).toString();
                double score = Double.parseDouble(list.get(i + 1).toString());
                scoredValues.add(ScoredValue.just(score, member));
            }
            return scoredValues;
        });
    }

    /**
     * 获取指定排名范围的成员（从小到大）
     * @param start 起始排名（从0开始）
     * @param stop 结束排名（包含）
     * @return 成员列表
     */
    public List<String> zrange(long start, long stop) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.zrange(lingQueRedis.key, start, stop));
    }

    /**
     * 获取指定排名范围的成员（从大到小）
     * @param start 起始排名（从0开始）
     * @param stop 结束排名（包含）
     * @return 成员列表
     */
    public List<String> zrevrange(long start, long stop) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.zrevrange(lingQueRedis.key, start, stop));
    }

    /**
     * 获取指定分数范围的成员（从小到大）
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员列表
     */
    public List<String> zrangebyscore(double min, double max) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.zrangebyscore(lingQueRedis.key, min, max));
    }

    /**
     * 获取指定分数范围的成员（从大到小）
     * @param max 最大分数
     * @param min 最小分数
     * @return 成员列表
     */
    public List<String> zrevrangebyscore(double max, double min) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.zrevrangebyscore(lingQueRedis.key, max, min));
    }

    /**
     * 获取指定排名范围的成员和分数（从小到大）
     * @param start 起始排名（从0开始）
     * @param stop 结束排名（包含）
     * @return 成员和分数的列表
     */
    public List<ScoredValue<String>> zrangeWithScores(long start, long stop) {
        return (List<ScoredValue<String>>) lingQueRedis.execBase((commands) -> {
            List<ScoredValue<String>> result = commands.zrangeWithScores(lingQueRedis.key, start, stop);
            return result != null ? result : Collections.emptyList();
        });
    }

    /**
     * 获取指定排名范围的成员和分数（从大到小）
     * @param start 起始排名（从0开始）
     * @param stop 结束排名（包含）
     * @return 成员和分数的列表
     */
    public List<ScoredValue<String>> zrevrangeWithScores(long start, long stop) {
        return (List<ScoredValue<String>>) lingQueRedis.execBase((commands) -> {
            List<ScoredValue<String>> result = commands.zrevrangeWithScores(lingQueRedis.key, start, stop);
            return result != null ? result : Collections.emptyList();
        });
    }

    /**
     * 获取指定分数范围的成员和分数（从小到大）
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员和分数的列表
     */
    public List<ScoredValue<String>> zrangebyscoreWithScores(double min, double max) {
        return (List<ScoredValue<String>>) lingQueRedis.execBase((commands) -> {
            List<ScoredValue<String>> result = commands.zrangebyscoreWithScores(lingQueRedis.key, min, max);
            return result != null ? result : Collections.emptyList();
        });
    }

    /**
     * 获取指定分数范围的成员和分数（从大到小）
     * @param max 最大分数
     * @param min 最小分数
     * @return 成员和分数的列表
     */
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(double max, double min) {
        return (List<ScoredValue<String>>) lingQueRedis.execBase((commands) -> {
            List<ScoredValue<String>> result = commands.zrevrangebyscoreWithScores(lingQueRedis.key, max, min);
            return result != null ? result : Collections.emptyList();
        });
    }

    /**
     * 统计指定分数范围内的成员数量
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员数量
     */
    public long zcount(double min, double max) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZCOUNT_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(min),
                String.valueOf(max),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 检查成员是否存在
     * @param member 要检查的成员
     * @return 是否存在
     */
    public boolean exists(Object member) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            String memberStr = LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member);
            Object result = commands.eval(
                IS_EXIST_MEMBER_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                memberStr,
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null && Long.parseLong(result.toString()) == 1;
        });
    }

    /**
     * 获取有序集合中的成员总数
     * @return 成员总数
     */
    public long size(double min, double max) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                SIZE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(min),
                String.valueOf(max),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 添加或更新成员分数
     * @param member 成员标识
     * @param score 分数
     * @return 成功添加的成员数量（新成员为1，已存在成员更新分数为0）
     */
    public long setScore(String member, Double score) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZADD_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(score),
                member,
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 批量添加或更新成员分数
     * @param members 成员分数映射
     * @param split 分批大小（建议200）
     */
    public void setScoreBatch(Map<String, Double> members, int split) {
        if (LQUtil.isEmpty(members)) {
            return;
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

        lingQueRedis.execBase((commands) -> {
            for (Map<String, Double> batchMap : batches) {
                String[] args = new String[batchMap.size() * 2 + 1];
                int i = 0;
                for (Map.Entry<String, Double> entry : batchMap.entrySet()) {
                    args[i++] = String.valueOf(entry.getValue());
                    args[i++] = entry.getKey();
                }
                args[i] = String.valueOf(lingQueRedis.ttl);

                commands.eval(
                    ZADD_BATCH_SCRIPT,
                    ScriptOutputType.INTEGER,
                    new String[]{lingQueRedis.key},
                    args
                );
            }
            return null;
        });
    }

    /**
     * 获取成员分数
     * @param memberId 成员ID
     * @return 成员分数，不存在返回0
     */
    public Double getScore(String memberId) {
        return (Double) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                GET_SCORE_SCRIPT,
                ScriptOutputType.VALUE,
                new String[]{lingQueRedis.key},
                memberId,
                String.valueOf(lingQueRedis.ttl)
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
        Object result = lingQueRedis.execBase((commands) -> {
            return commands.eval(
                PAGE_RANK_LIMIT_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                String.valueOf(offset),
                String.valueOf(limit),
                String.valueOf(lingQueRedis.ttl)
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
        } catch (Exception e) {
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
        return (Long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                GET_RANK_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                memberId,
                String.valueOf(lingQueRedis.ttl)
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
        Object result = lingQueRedis.execBase((commands) -> {
            return commands.eval(
                GET_RANK_BEAN_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                memberId,
                String.valueOf(lingQueRedis.ttl)
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
        } catch (Exception e) {
            return new RedisRank(memberId, 0D, null);
        }
    }

    /**
     * 根据排名范围删除成员
     * @param startRank 起始排名（从0开始）
     * @param stopRank 结束排名
     * @return true-删除成功 false-删除失败
     */
    public boolean deleteByRank(long startRank, long stopRank) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZREMRANGEBYRANK_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(startRank),
                String.valueOf(stopRank),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 根据分数范围获取成员列表
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param limit 返回数量限制
     * @return 成员列表
     */
    public List<RedisRank> getByScoreRange(double minScore, double maxScore, int limit) {
        return getByScoreRange(String.valueOf(minScore), String.valueOf(maxScore), limit);
    }

    /**
     * 根据分数范围获取成员列表
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param limit 返回数量限制
     * @return 成员列表
     */
    public List<RedisRank> getByScoreRange(long minScore, long maxScore, int limit) {
        return getByScoreRange(String.valueOf(minScore), String.valueOf(maxScore), limit);
    }

    private List<RedisRank> getByScoreRange(String minScore, String maxScore, int limit) {
        Object result = lingQueRedis.execBase((commands) -> {
            return commands.eval(
                GET_BY_SCORE_RANGE_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                minScore,
                maxScore,
                String.valueOf(limit),
                String.valueOf(lingQueRedis.ttl)
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
        } catch (Exception e) {
            //忽略
        }
        return rankList;
    }

    /**
     * 获取指定分数范围内的成员列表
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @return 成员列表
     */
    public List<String> getMembers(double minScore, double maxScore) {
        return (List<String>) lingQueRedis.execBase((commands) -> {
            return commands.zrangebyscore(lingQueRedis.key, Range.from(Range.Boundary.including(minScore), Range.Boundary.including(maxScore)));
        });
    }

    /**
     * 删除指定排名范围内的成员
     * @param start 起始排名（从0开始）
     * @param stop 结束排名（包含）
     * @return 删除的成员数量
     */
    public long zremrangebyrank(long start, long stop) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZREMRANGEBYRANK_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(start),
                String.valueOf(stop),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 删除指定分数范围内的成员
     * @param min 最小分数
     * @param max 最大分数
     * @return 删除的成员数量
     */
    public long zremrangebyscore(double min, double max) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZREMRANGEBYSCORE_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(min),
                String.valueOf(max),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 删除指定字典区间内的成员
     * @param min 最小字典值（必须以"["或"("开头）
     * @param max 最大字典值（必须以"]"或")"结尾）
     * @return 删除的成员数量
     */
    public long zremrangebylex(String min, String max) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZREMRANGEBYLEX_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                min,
                max,
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 统计指定字典区间内的成员数量
     * @param min 最小字典值（必须以"["或"("开头）
     * @param max 最大字典值（必须以"]"或")"结尾）
     * @return 成员数量
     */
    public long zlexcount(String min, String max) {
        return (long) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                ZLEXCOUNT_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                min,
                max,
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }
}