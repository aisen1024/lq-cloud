package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import io.lettuce.core.ScriptOutputType;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SetOpt extends BaseOpt {
    private LingQueRedis lingQueRedis;

    private static final String SADD_AND_EXPIRE_SCRIPT = 
        "local result = redis.call('SADD', KEYS[1], ARGV[1]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "return result;";

    private static final String BATCH_SADD_AND_EXPIRE_SCRIPT =
        "local count = 0; " +
        "for i = 1, #ARGV - 1 do " +
        "    count = count + redis.call('SADD', KEYS[1], ARGV[i]); " +
        "end; " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String SREM_AND_EXPIRE_SCRIPT =
        "local result = redis.call('SREM', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return result;";

    private static final String SPOP_AND_EXPIRE_SCRIPT =
        "local result = redis.call('SPOP', KEYS[1], ARGV[1]); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
        "end; " +
        "return result;";

    public SetOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    /**
     * 添加set成员
     * @param obj 成员对象
     * @return 是否添加成功
     */
    public boolean addMember(Object obj) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                SADD_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 批量添加set成员
     * @param objs 成员对象列表
     * @return 添加成功的数量
     */
    public long addMembers(Collection<?> objs) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[objs.size() + 1];
            int i = 0;
            for (Object obj : objs) {
                args[i++] = LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj);
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                BATCH_SADD_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 删除set成员
     * @param obj 要删除的成员
     * @return 是否删除成功
     */
    public boolean deleteMember(Object obj) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                SREM_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj),
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 获取set的成员个数
     * @return 成员数量
     */
    public long size() {
        return (long) lingQueRedis.execBase((commands) -> {
            Long result = commands.scard(lingQueRedis.key);
            return result != null ? result : 0L;
        });
    }

    /**
     * 判断是否是set集合中的元素
     * @param obj 要检查的对象
     * @return 是否是成员
     */
    public boolean isMembers(Object obj) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            Boolean result = commands.sismember(lingQueRedis.key, 
                LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj));
            return result != null && result;
        });
    }

    /**
     * 随机获取集合成员
     * @param count 获取数量
     * @return 成员列表
     */
    public List<String> randomMembers(long count) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.srandmember(lingQueRedis.key, count));
    }

    /**
     * 随机弹出集合成员
     * @param count 弹出数量
     * @return 成员列表
     */
    public Set<String> popMembers(long count) {
        return (Set<String>) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                SPOP_AND_EXPIRE_SCRIPT,
                ScriptOutputType.MULTI,
                new String[]{lingQueRedis.key},
                String.valueOf(count),
                String.valueOf(lingQueRedis.ttl)
            );
            if (result instanceof List) {
                return new HashSet<>((List<String>) result);
            }
            return new HashSet<>();
        });
    }

    /**
     * 随机弹出集合成员
     * @param count 弹出数量
     * @return 成员列表
     */
    public <S>Set<S> popMembers(long count, Class<S> targetClass) {
        Set<String> result = popMembers(count);
        if (LQUtil.isEmpty(result)){
            return new HashSet<>();
        }
        Set<S> members = new HashSet<>();
        for (String member : result) {
            members.add(LQUtil.isBasClass(targetClass) ?
                LQUtil.baseClassTran(member, targetClass) :
                JSONUtil.toBean(member, targetClass));
        }
        return members;
    }


    /**
     * 获取所有成员
     * @return 所有成员的列表
     */
    public Set<String> members() {
        return (Set<String>) lingQueRedis.execBase((commands) -> 
            commands.smembers(lingQueRedis.key));
    }

    /**
     * 获取所有成员并转换为指定类型
     * @param targetClass 目标类型
     * @return 转换后的成员列表
     */
    public <T> Set<T> members(Class<T> targetClass) {
        return (Set<T>) lingQueRedis.execBase((commands) -> {
            Set<String> members = commands.smembers(lingQueRedis.key);
            return members.stream()
                .map(member -> LQUtil.isBasClass(targetClass) ?
                    LQUtil.baseClassTran(member, targetClass) : 
                    JSONUtil.toBean(member, targetClass))
                .collect(Collectors.toSet());
        });
    }
}

