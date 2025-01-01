package cn.lingque.redis.exten;

import cn.lingque.redis.LingQueRedis;
import lombok.AllArgsConstructor;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class GeoOps<T> {
    private LingQueRedis<T> lingQueRedis;

    private static final String ADD_GEO_SCRIPT = 
        "redis.call('GEOADD', KEYS[1], ARGV[1], ARGV[2], ARGV[3]); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[4]); " +
        "return 1;";

    private static final String BATCH_ADD_GEO_SCRIPT =
        "local count = redis.call('GEOADD', KEYS[1], unpack(ARGV, 1, #ARGV-1)); " +
        "redis.call('EXPIRE', KEYS[1], ARGV[#ARGV]); " +
        "return count;";

    /**
     * 添加坐标位置元素
     *
     * @param pointMap
     */
    public long addGeo(Map<String, GeoCoordinate> pointMap) {
        List<String> paras = new ArrayList<>(pointMap.keySet().size() + 1);
        for (Map.Entry<String, GeoCoordinate> entry : pointMap.entrySet()) {
            GeoCoordinate coord = entry.getValue();
            paras.add(String.valueOf(coord.getLongitude()));
            paras.add(String.valueOf(coord.getLatitude()));
            paras.add(entry.getKey());
        }
        paras.add(String.valueOf(lingQueRedis.getTTL()));
       return (long)lingQueRedis.execBase((jedis) -> {
            return (Long) jedis.eval(
                    BATCH_ADD_GEO_SCRIPT,
                    new ArrayList<String>() {{
                        add(lingQueRedis.key);
                    }},
                    paras
            );
        });
    }

    /**
     * 添加坐标位置元素
     *
     * @param longitude
     * @param latitude
     * @param member
     * @return
     */
    public long addGeo(double longitude, double latitude, String member) {
        return (long)lingQueRedis.execBase((jedis) -> {
            return (Long) jedis.eval(
                ADD_GEO_SCRIPT,
                1,
                lingQueRedis.key,
                String.valueOf(longitude),
                String.valueOf(latitude),
                member,
                String.valueOf(lingQueRedis.getTTL())
            );
        });
    }

    /**
     * 获取半径内的数据
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半圆内
     * @param unit      单位 M米 KM千米 MI英里 FT英尺
     * @return
     */
    public List<GeoRadiusResponse> geoRadius(double longitude, double latitude, double radius,
                                             GeoUnit unit) {
        return (List<GeoRadiusResponse>)lingQueRedis.execBase((jedis) -> {
            return jedis.georadius(lingQueRedis.key, longitude, latitude, radius, unit);
        });
    }


    /**
     * 根据目标位置查询半圆内的所有地理位置
     *
     * @return
     */
    public List<GeoRadiusResponse> geoRadiusByMember(String member, double radius, GeoUnit unit) {
        return (List<GeoRadiusResponse>)lingQueRedis.execBase((jedis) -> {
            return jedis.georadiusByMember(lingQueRedis.key, member, radius, unit);
        });
    }

    /**
     * 根据目标位置查询半圆内的所有地理位置 可筛选
     *
     * @param member 地名
     * @param radius 半圆内
     * @param unit   单位 M米 KM千米 MI英里 FT英尺
     * @param param  可筛选条件
     * @return
     */
    public List<GeoRadiusResponse> geoRadiusByMember(final String member,
                                                     final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        return (List<GeoRadiusResponse>)lingQueRedis.execBase((jedis) -> {
            return jedis.georadiusByMember(lingQueRedis.key, member, radius, unit, param);
        });
    }

    /**
     * 获取两地距离
     *
     * @param source 开始地
     * @param target 结束地
     * @param unit   单位  M米 KM千米 MI英里 FT英尺
     * @return
     */
    public Double geoDist(String source, String target, GeoUnit unit) {
        return (Double)lingQueRedis.execBase((jedis) -> {
            return jedis.geodist(lingQueRedis.key, source, target, unit);
        });
    }

    /**
     * 批量获取定位
     *
     * @param local
     * @return 返回经纬度坐标
     */
    public List<GeoCoordinate> geoPos(String... local) {
        return (List<GeoCoordinate>)lingQueRedis.execBase((jedis) -> {
            return jedis.geopos(lingQueRedis.key, local);
        });
    }

    /**
     * geohash 用一个字符串表示经度和纬度两个坐标。某些情况下无法在两列上同时应用索引 （例如 MySQL 4 之前的版本，Google App Engine 的数据层等），利用 geohash，只需在一列上应用索引即可。
     *
     * @param members
     * @return
     */
    public List<String> geoHash(String... members) {
        return (List<String>)lingQueRedis.execBase((jedis) -> {
            return jedis.geohash(lingQueRedis.key, members);
        });
    }


    /**
     * 统计符合条件的地理个数
     *
     * @param longitude  经度
     * @param latitude   纬度
     * @param radius     半径
     * @param unit       单位
     * @param param      地理位置参数
     * @param storeParam 参数
     * @return
     */
    public long geoRadiusStore(double longitude, double latitude, double radius,
                               GeoUnit unit, GeoRadiusParam param, GeoRadiusStoreParam storeParam) {
        return (long)lingQueRedis.execBase((jedis) -> {
            return jedis.georadiusStore(lingQueRedis.key, longitude, latitude, radius, unit, param, storeParam);
        });
    }


}