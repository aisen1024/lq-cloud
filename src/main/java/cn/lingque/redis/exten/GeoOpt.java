package cn.lingque.redis.exten;

import cn.lingque.redis.LingQueRedis;
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoCoordinates;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.ScriptOutputType;
import lombok.Data;

import java.util.*;

@Data
public class GeoOpt extends BaseOpt {
    private LingQueRedis lingQueRedis;

    private static final String GEOADD_AND_EXPIRE_SCRIPT = 
        "local result = redis.call('GEOADD', KEYS[1], ARGV[1], ARGV[2], ARGV[3]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[4]); " +
        "return result;";

    private static final String BATCH_GEOADD_AND_EXPIRE_SCRIPT =
        "local count = redis.call('GEOADD', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    private static final String ZREM_AND_EXPIRE_SCRIPT =
        "local result = redis.call('ZREM', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "    redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "end; " +
        "return result;";

    public GeoOpt(LingQueRedis lingQueRedis) {
        this.lingQueRedis = lingQueRedis;
        this.key = lingQueRedis.key;
        this.ttl = lingQueRedis.ttl;
    }

    /**
     * 添加坐标位置元素
     * @param pointMap 坐标点映射
     * @return 添加的数量
     */
    public long addGeo(Map<String, GeoCoordinates> pointMap) {
        return (long) lingQueRedis.execBase((commands) -> {
            String[] args = new String[pointMap.size() * 3 + 1];
            int i = 0;
            for (Map.Entry<String, GeoCoordinates> entry : pointMap.entrySet()) {
                GeoCoordinates coord = entry.getValue();
                args[i++] = String.valueOf(coord.getX().doubleValue());
                args[i++] = String.valueOf(coord.getY().doubleValue());
                args[i++] = entry.getKey();
            }
            args[i] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                BATCH_GEOADD_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }

    /**
     * 添加单个坐标位置元素
     * @param longitude 经度
     * @param latitude 纬度
     * @param member 成员名
     * @return 是否添加成功
     */
    public boolean addGeo(double longitude, double latitude, String member) {
        return (boolean) lingQueRedis.execBase((commands) -> {
            Object result = commands.eval(
                GEOADD_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                String.valueOf(longitude),
                String.valueOf(latitude),
                member,
                String.valueOf(lingQueRedis.ttl)
            );
            return result != null && Long.parseLong(result.toString()) > 0;
        });
    }

    /**
     * 根据给定地点查询半径内的其他地点
     * @param longitude 经度
     * @param latitude 纬度
     * @param radius 半径
     * @param unit 单位
     * @return 符合条件的地点列表
     */
    public List<GeoWithin<String>> geoRadius(double longitude, double latitude, double radius, GeoArgs.Unit unit) {
        return (List<GeoWithin<String>>) lingQueRedis.execBase((commands) -> {
            GeoArgs args = new GeoArgs()
                .withDistance()
                .withHash()
                .withCoordinates();
            return commands.georadius(lingQueRedis.key, longitude, latitude, radius, unit, args);
        });
    }

    /**
     * 根据成员查询半径内的其他地点
     * @param member 成员名
     * @param radius 半径
     * @param unit 单位
     * @return 符合条件的地点列表
     */
    public List<GeoWithin<String>> geoRadiusByMember(String member, double radius, GeoArgs.Unit unit) {
        return (List<GeoWithin<String>>) lingQueRedis.execBase((commands) -> {
            GeoArgs args = new GeoArgs()
                .withDistance()
                .withHash()
                .withCoordinates();
            return commands.georadiusbymember(lingQueRedis.key, member, radius, unit, args);
        });
    }

    /**
     * 获取两地距离
     * @param source 开始地
     * @param target 结束地
     * @param unit 单位
     * @return 距离值
     */
    public Double geoDist(String source, String target, GeoArgs.Unit unit) {
        return (Double) lingQueRedis.execBase((commands) -> 
            commands.geodist(lingQueRedis.key, source, target, unit));
    }

    /**
     * 批量获取定位
     * @param members 成员列表
     * @return 返回经纬度坐标
     */
    public List<GeoCoordinates> geoPos(String... members) {
        return (List<GeoCoordinates>) lingQueRedis.execBase((commands) -> 
            commands.geopos(lingQueRedis.key, members));
    }

    /**
     * 获取地理位置的geohash值
     * @param members 成员列表
     * @return geohash字符串列表
     */
    public List<String> geoHash(String... members) {
        return (List<String>) lingQueRedis.execBase((commands) -> 
            commands.geohash(lingQueRedis.key, members));
    }

    /**
     * 删除地理位置信息
     * @param members 要删除的成员
     * @return 删除的数量
     */
    public Long removeGeo(String... members) {
        return (Long) lingQueRedis.execBase((commands) -> {
            String[] args = Arrays.copyOf(members, members.length + 1);
            args[members.length] = String.valueOf(lingQueRedis.ttl);

            Object result = commands.eval(
                ZREM_AND_EXPIRE_SCRIPT,
                ScriptOutputType.INTEGER,
                new String[]{lingQueRedis.key},
                args
            );
            return result != null ? Long.parseLong(result.toString()) : 0L;
        });
    }
}