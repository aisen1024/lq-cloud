package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SetOpt<T> {
    private LingQueRedis<T> lingQueRedis;

    public SetOpt(LingQueRedis<T> lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
    }

    /**
     * set添加数据并更新过期时间
     * @param obj 要添加的对象
     */
    public void add(Object obj) {
        String luaScript = 
            "local value = ARGV[1]\n" +
            "local result = redis.call('SADD', KEYS[1], ARGV[1])\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "return result";

        lingQueRedis.execBase((jedis) -> {
            jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return null;
        });
    }
    /**
     * 删除set成员并更新过期时间
     * @param obj 要删除的对象
     * @return 是否删除成功
     */
    public boolean deleteMember(Object obj) {
        String luaScript = 
            "local value = ARGV[1]\n" +
            "local result = redis.call('SREM', KEYS[1], ARGV[1])\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return result";

       return(boolean)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    LQUtil.isBaseValue(obj) ? obj.toString() : JSONUtil.toJsonStr(obj),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 获取set的成员个数
     * @return
     */
    public long size() {
        return(long)lingQueRedis.execBase((jedis) -> {
            return jedis.scard(lingQueRedis.key);
        });
    }

    /**
     * 判断 是否是set集合中的元素
     * @param obj
     */
    public boolean isMembers(Object obj) {
        return(boolean)lingQueRedis.execBase((jedis) -> {
            Boolean member = jedis.sismember(lingQueRedis.key, obj.toString());
            return null != member && member;
        });
    }

    /**
     * 随机抽取集合
     * @param count
     * @return
     */
    public List<T> randomMembers(Class<T> targetClass, Integer count) {
        return(List<T>)lingQueRedis.execBase((jedis) -> {
            List<String> set = jedis.srandmember(lingQueRedis.key, count);
            if (null == set || set.isEmpty()) {
                return Collections.emptyList();
            }
            List<T> list = new ArrayList<>();
            set.forEach(s->{
                list.add(LQUtil.isBasClass(targetClass) ? LQUtil.baseClassTran(s, targetClass) : JSONUtil.toBean(s, targetClass));
            });
            return list;
        });
    }

    /**
     * 随机抽取一个成员
     * @return
     */
    public <T> T randomMember(Class<T> targetClass) {
        return(T)lingQueRedis.execBase((jedis) -> {
            String member = jedis.srandmember(lingQueRedis.key);
            if (LQUtil.isEmpty(member)) {
                return null;
            }
            return LQUtil.isBasClass(targetClass) ? LQUtil.baseClassTran(member, targetClass) : JSONUtil.toBean(member, targetClass);
        });
    }

    /**
     * 获取set集合元素
     * @return
     */
    public List<T> getMembers(Class<T> targetClass) {
        return(List<T>)lingQueRedis.execBase((jedis) -> {
            Set<String> set = jedis.smembers(lingQueRedis.key);
            if (null == set || set.isEmpty()) {
                return Collections.emptyList();
            }
            List<T> list = new ArrayList<>();
            set.forEach(s->{
                list.add(LQUtil.isBasClass(targetClass) ? LQUtil.baseClassTran(s, targetClass) : JSONUtil.toBean(s, targetClass));
            });
            return list;
        });
    }

    /**
     * 随机弹出元素并更新过期时间
     * @return 弹出的元素
     */
    public String pop() {
        String luaScript = 
            "local result = redis.call('SPOP', KEYS[1])\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return result";

        return (String)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Collections.singletonList(String.valueOf(lingQueRedis.ttl))
            );
            return result != null ? result.toString() : null;
        });
    }

    /**
     * 弹出多个元素并更新过期时间
     * @param count 弹出数量
     * @param targetClass 目标类型
     * @return 弹出的元素列表
     */
    public List<T> pops(long count, Class<T> targetClass) {
        String luaScript = 
            "local result = redis.call('SPOP', KEYS[1], ARGV[1])\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return result";

        return (List<T>)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    String.valueOf(count),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            if (result == null) {
                return Collections.emptyList();
            }
            List<Object> list = (List<Object>) result;
            return list.stream().map(r->LQUtil.isBasClass(targetClass)?LQUtil.baseClassTran(r,targetClass) : JSONUtil.toBean(r.toString(),targetClass)).collect(Collectors.toList());
        });
    }


}

