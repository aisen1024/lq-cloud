package com.lingque.redis.exten;

import com.lingque.redis.LingQueRedis;
import lombok.AllArgsConstructor;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class GeoOps<T> {
    private LingQueRedis<T> lingQueRedis;

    /**
     * 添加坐标位置元素
     *
     * @param pointMap
     */
    public long addGeo(Map<String, GeoCoordinate> pointMap) {
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().geoadd(lingQueRedis.key, pointMap));
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
        return lingQueRedis.run(() -> lingQueRedis.getRedisTemplate().geoadd(lingQueRedis.key, longitude, latitude, member));
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
        return lingQueRedis.getRedisTemplate().georadius(lingQueRedis.key, longitude, latitude, radius, unit);
    }


    /**
     * 根据目标位置查询半圆内的所有地理位置
     *
     * @return
     */
    public List<GeoRadiusResponse> geoRadiusByMember(String member, double radius, GeoUnit unit) {
        return lingQueRedis.getRedisTemplate().georadiusByMember(lingQueRedis.key, member, radius, unit);
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
        return lingQueRedis.getRedisTemplate().georadiusByMember(lingQueRedis.key, member, radius, unit, param);

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
        return lingQueRedis.getRedisTemplate().geodist(lingQueRedis.key, source, target, unit);
    }

    /**
     * 批量获取定位
     *
     * @param local
     * @return 返回经纬度坐标
     */
    public List<GeoCoordinate> geoPos(String... local) {
        return lingQueRedis.getRedisTemplate().geopos(lingQueRedis.key, local);
    }

    /**
     * geohash 用一个字符串表示经度和纬度两个坐标。某些情况下无法在两列上同时应用索引 （例如 MySQL 4 之前的版本，Google App Engine 的数据层等），利用 geohash，只需在一列上应用索引即可。
     *
     * @param members
     * @return
     */
    public List<String> geoHash(String... members) {
        return lingQueRedis.getRedisTemplate().geohash(lingQueRedis.key, members);
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
        return lingQueRedis.getRedisTemplate().georadiusStore(lingQueRedis.key, longitude, latitude, radius, unit, param, storeParam);
    }


}