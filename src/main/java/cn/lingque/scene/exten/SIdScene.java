package cn.lingque.scene.exten;

import cn.lingque.redis.LingQueRedis;
import lombok.AllArgsConstructor;

/**
 * @author aisen
 * @date 2024/10/8
 * @desc ID场景
 **/
@AllArgsConstructor
public class SIdScene<T> {
    private final static long startId = 100000L;
    private LingQueRedis redis;
    /**
     * 构建分布式ID
     * @return
     */
    public Long getID(){
       return getID(startId);
    }

    /**
     * 构建分布式ID
     * @param startId 如果不存在则从这个ID开始
     * @return
     */
    public Long getID(long startId){
        long sid = redis.ofValue().incrIfExist();
        if (sid < startId){
            redis.ofValue().setNx(startId);
            sid = redis.ofValue().incr();
        }
        return sid;
    }


    /**
     * 构建推广员ID 比如sx01fd
     * @return
     */
    public String getShareId(){
        long sid = getID(202410L);
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
