package cn.lingque.redis.exten;

import cn.hutool.json.JSONUtil;
import cn.lingque.exceptions.LQException;
import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author aisen
 * @date 2024/9/25
 * @desc 简单说一下
 **/
public class ListOpt extends BaseOpt{

    private LingQueRedis lingQueRedis;

    public ListOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    // 添加 Lua 脚本常量
    private static final String DELETE_FIRST_SCRIPT =
        "local count = redis.call('LREM', KEYS[1], ARGV[1], ARGV[2]); " +
        "if count > 0 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[3]); " +
        "end; " +
        "return count;";

    /**
     * 追加数据并更新过期时间
     * @param member 成员
     * @return 列表长度
     */
    public long add(Object member) {
        String luaScript = 
            "local len = redis.call('RPUSH', KEYS[1], ARGV[1])\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "return len";

        return (Long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 向指定的位置插入数据并更新过期时间
     * @param member 成员
     * @param index 要在哪个位置下插入
     */
    public String add(Object member, int index) {
        String luaScript = 
            "redis.call('LSET', KEYS[1], ARGV[1], ARGV[2])\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[3])\n" +
            "return 'OK'";

        return (String)lingQueRedis.execBase((jedis) -> {
            return (String) jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(
                    String.valueOf(index),
                    LQUtil.isBaseValue(member) ? member.toString() : JSONUtil.toJsonStr(member),
                    String.valueOf(lingQueRedis.ttl)
                )
            );
        });
    }

    /**
     * 获取集合个数
     */
    public long size() {
        return (long)lingQueRedis.execBase((jedis) -> {
            return jedis.llen(lingQueRedis.key);
        });
    }

    /**
     * 通过下标获取对应的数据
     */
    public String get(int index) {
        return (String)lingQueRedis.execBase((jedis) -> {
            return jedis.lindex(lingQueRedis.key, index);
        });
    }

    /**
     * list左边追加队列并更新过期时间
     * @param members 成员数组
     */
    public long leftPush(String... members) {
        String luaScript = 
            "local len = redis.call('LPUSH', KEYS[1], unpack(ARGV, 1, #ARGV-1))\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV])\n" +
            "return len";

        return (long)lingQueRedis.execBase((jedis) -> {
            List<String> args = new ArrayList<>(members.length+1);
            args.addAll(Arrays.asList(members));
            args.add(String.valueOf(lingQueRedis.ttl));
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * list右边追加队列并更新过期时间
     * @param members 成员数组
     */
    public long rightPush(String... members) {
        String luaScript = 
            "local len = redis.call('RPUSH', KEYS[1], unpack(ARGV, 1, #ARGV-1))\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV])\n" +
            "return len";

        return (long)lingQueRedis.execBase((jedis) -> {
            List<String> args = new ArrayList<>(members.length + 1);
            args.addAll(Arrays.asList(members));
            args.add(String.valueOf(lingQueRedis.ttl));
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 分页获取
     *
     * @param targetClass
     * @param page
     * @param pageSize
     * @return
     */
    public <T> List<T> page(Class<T> targetClass, Integer page, Integer pageSize) {
        page = null == page || page < 1 ? 1 : page;
        pageSize = null == pageSize || pageSize < 1 ? 10 : pageSize;
        Integer offset = (page - 1) * pageSize;
        Integer limit = pageSize + offset - 1;
        return (List<T>)lingQueRedis.execBase((jedis) -> {
            List<String> ls = jedis.lrange(lingQueRedis.key, offset, limit);
            if (null != ls && ls.size() > 0) {
                return ls.stream().map(item -> JSONUtil.toBean(item, targetClass)).collect(Collectors.toList());
            }
            return new ArrayList<>();
        });
    }

    /**
     * 移除所有跟member值一样的元素并更新过期时间
     * @param member 要移除的元素
     * @return 移除的元素数量
     */
    public long deleteAllSame(String member) {
        String luaScript = 
            "local count = redis.call('LREM', KEYS[1], 0, ARGV[1])\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return count";

        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                luaScript,
                Collections.singletonList(lingQueRedis.key),
                Arrays.asList(member, String.valueOf(lingQueRedis.ttl))
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 从左边开始匹配（首部），删除指定数量的匹配元素
     * @param member 要删除的元素值
     * @param count 要删除的数量（必须大于0）
     * @return 实际删除的元素数量
     */
    public long beforeDeleteFirst(String member, int count) {
        if (count == 0) {
            throw new LQException("count 不能为0！");
        }
        count = Math.abs(count);
        
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(count));  // 正数表示从左边开始删除
        params.add(member);
        params.add(String.valueOf(lingQueRedis.getTTL()));
        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    DELETE_FIRST_SCRIPT,
                    Collections.singletonList(lingQueRedis.key),
                    params
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 从右边开始匹配（尾部），删除指定数量的匹配元素
     * @param member 要删除的元素值
     * @param count 要删除的数量（必须大于0）
     * @return 实际删除的元素数量
     */
    public long afterDeleteFirst(String member, int count) {
        if (count == 0) {
            throw new LQException("count 不能为0！");
        }
        count = Math.abs(count) * -1;  // 负数表示从右边开始删除
        
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(count));
        params.add(member);
        params.add(String.valueOf(lingQueRedis.getTTL()));
        return (long)lingQueRedis.execBase((jedis) -> {
            Object result = jedis.eval(
                    DELETE_FIRST_SCRIPT,
                    Collections.singletonList(lingQueRedis.key),
                    params
            );
            return result != null ? Long.parseLong(result.toString()) : 0;
        });
    }

    /**
     * 从右边弹出元素并更新过期时间
     * @param count 元素个数
     * @return 弹出的元素列表
     */
    public List<String> rpops(int count) {
        String luaScript = 
            "local result = redis.call('RPOP', KEYS[1], ARGV[1])\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return result";

        return (List<String>)lingQueRedis.execBase((jedis) -> {
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
            try {
                return JSONUtil.toList(JSONUtil.toJsonStr(result), String.class);
            }catch (Exception e){
                return Collections.emptyList();
            }

        });
    }

    /**
     * 从左边弹出元素并更新过期时间
     * @param count 元素个数
     * @return 弹出的元素列表
     */
    public List<String> lpops(int count) {
        String luaScript = 
            "local result = redis.call('LPOP', KEYS[1], ARGV[1])\n" +
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
            "end\n" +
            "return result";

        return (List<String>)lingQueRedis.execBase((jedis) -> {
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
            try {
                return JSONUtil.toList(result.toString(), String.class);
            }catch (Exception e){
                return Collections.emptyList();
            }
        });
    }

}
