package com.lingque.scene.exten;

import com.lingque.redis.LingQueRedis;

/**
 * @author aisen
 * @date 2024/10/8
 * @desc ID场景
 **/
public class SIdScene<T> {
    private final static long startId = 100000L;

    /**
     * 构建分布式ID
     * @param redis
     * @return
     */
    public static Long getID(LingQueRedis redis){
       return getID(redis,startId);
    }

    /**
     * 构建分布式ID
     * @param redis
     * @param startId 如果不存在则从这个ID开始
     * @return
     */
    public static Long getID(LingQueRedis redis,long startId){
        long sid = redis.ofValue().incrIfExist();
        if (sid < startId){
            redis.ofValue().setNx(startId);
            sid = redis.ofValue().incr();
        }
        return sid;
    }


    /**
     * 构建推广员ID 比如sx01fd
     * @param redis
     * @return
     */
    public static String getShareId(LingQueRedis redis){
        long sid = getID(redis,202410L);
        String v = Long.toString(sid);
        StringBuilder shortKey = new StringBuilder();
            int ruleIndex = 0;
            while (v.length() > 0) {
                int len = rule[ruleIndex];
                if (v.length() > len) {
                    shortKey.append(charArray[shortIndex(v.substring(0, len))]);
                    v = v.substring(len);
                } else {
                    shortKey.append(charArray[shortIndex(v)]);
                    break;
                }
                ruleIndex++;
            }
        return shortKey.toString();
    }

    private static final String[] charArray = new String[]{"k", "f", "2", "3", "9", "X", "S", "R", "t", "G", "W", "0", "s", "q", "g", "r", "n", "d", "F", "B", "7", "L", "C", "T", "4", "j", "M", "1", "l", "D", "6", "K", "a", "A", "8", "Q", "E", "5", "b", "x"};
    private static Integer[] rule = new Integer[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    /**
     * 计算短链下标
     */
    private static Integer shortIndex(String v) {
        char[] vs = v.toCharArray();
        Integer index = 0;
        for (char c : vs) {
            index += Integer.parseInt(c + "");
        }
        return index;
    }
}
