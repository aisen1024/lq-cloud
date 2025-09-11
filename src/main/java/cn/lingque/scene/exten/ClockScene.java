package cn.lingque.scene.exten;

import cn.lingque.redis.LingQueRedis;
import cn.lingque.util.LQUtil;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * @author aisen
 * @date 2024/9/27
 * @desc 打卡场景
 **/
@AllArgsConstructor
public class ClockScene<T> {

    private LingQueRedis redis;

    /**
     * 是否完全完成了，【全部打满】
     *
     * @return true-是， false-否
     */
    public boolean isFullBit() {
        String bitArray = (String) redis.ofValue().get(String.class);
        return LQUtil.isNotEmpty(bitArray) && bitArray.indexOf("0") < 0;
    }

    /**
     * 是否打满x天
     *
     * @param len
     * @return
     */
    public boolean isFinishPartBit(Integer len) {
        return getBitFullLen() >= len;
    }

    /**
     * 适用场景 连续打卡 -- 创建bit数组
     * 位结构，利用日期 endDate - startDate 计算日差，比如日差是7天，则结构为 1000000 , 如果第二天继续位移 1100000
     * @param day
     */
    public void ifNullCreateClock(int day) {
        String bitArray = (String) redis.ofValue().get(String.class);
        if (LQUtil.isEmpty(bitArray)) {
            //bit长度
            redis.ofValue().setNx(initBit(day));
        }
    }

    /**
     * 适用场景 连续打卡 -- 创建bit数组
     * 位结构，利用日期 endDate - startDate 计算日差，比如日差是7天，则结构为 1000000 , 如果第二天继续位移 1100000
     * @param startDate
     * @param endDate
     */
    public void ifNullCreateClock(Long startDate, Long endDate) {

        if (startDate > endDate || startDate < 0 || endDate < 0) {
            throw new RuntimeException("startDate或endDate 不符合规范");
        }
        int len = getDayDiff(startDate, endDate);

    }

    /**
     * 根据结束和开始时间，计算时间差
     * @param startDate
     * @param endDate
     * @return
     */
    private int getDayDiff(Long startDate, Long endDate) {
        if (startDate > endDate || startDate < 0 || endDate < 0){
            return 1;
        }
        return BigDecimal.valueOf(endDate).subtract(BigDecimal.valueOf(startDate)).divide(BigDecimal.valueOf(1000L * 3600L * 24),0,BigDecimal.ROUND_UP).intValue();
    }


    /**
     * 适用场景 连续打卡
     * 位结构，利用日期 endDate - startDate 计算日差，比如日差是7天，则结构为 1000000 , 如果第二天继续位移 1100000
     *
     * @param startDate
     * @param endDate
     */
    public void moveBit(Long startDate, Long endDate) {
        //目前位移 天数差
        int nowIndex = getDayDiff(startDate, System.currentTimeMillis());
        //bit长度
        int len = getDayDiff(startDate, endDate);
        //未开始、已结束、长度小于0
        if (nowIndex < 0 || nowIndex + 1 > len || len < 0) {
            return;
        }

        String bitArray = (String) redis.ofValue().get(String.class);
        if (LQUtil.isEmpty(bitArray)) {
            throw new RuntimeException("bit数组不存在，请使用ifNullCreateBit创建！");
        }

        boolean isFinish = bitArray.charAt(nowIndex) == '1';
        if (!isFinish) {
            bitArray = replaceBit(bitArray, nowIndex);
            Long t =  redis.getTTL();
            t = t < 0 ?  redis.ttl : t;
            //设置后，更新ttl时间
            redis.ofValue().set(bitArray);
            redis.resetTTL(t);
        }
    }


    /**
     * 获取打卡天数
     *
     * @return
     */
    public Integer getBitFullLen() {
        String bitArray = (String) redis.ofValue().get(String.class);
        if (LQUtil.isEmpty(bitArray)) {
            return 0;
        }
        char[] bitChar = bitArray.toCharArray();
        int bitFullLen = 0;
        for (char b : bitChar) {
            if (b == '1') {
                bitFullLen++;
            }
        }
        return bitFullLen;
    }

    /**
     * 初始化bit位
     *
     * @param len
     * @return
     */
    private String initBit(Integer len) {
        StringBuilder bit = new StringBuilder();
        for (int i = 0; i < len; i++) {
            bit.append("0");
        }
        return bit.toString();
    }

    /**
     * 替换bit位
     *
     * @param s
     * @param index
     * @return
     */
    private String replaceBit(String s, Integer index) {
        String b = "1";
        if (s.length() == 1) {
            return b;
        }
        if (s.length() < index) {
            return s;
        }
        char[] bitArray = s.toCharArray();
        bitArray[index] = '1';
        return new String(bitArray);
    }
}
