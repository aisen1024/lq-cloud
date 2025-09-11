package cn.lingque.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  工具类  环境判断、字符串、对象转换、文本提取
 * </p>
 *
 * @author zlm
 * @date 2022/8/3
 * @since 1.0
 */
@SuppressWarnings("all")
public class LQUtil<T>{

    private static Logger log = LoggerFactory.getLogger(LQUtil.class);

    /**
     * 将map转bean，参数为null或者map元素个数为0，都返回null，对比BeanUtil，屏蔽了CopyOptions的配置
     * @param source 源头
     * @param targetClass 目标类
     * @param <T> 返回值的泛型
     * @return bean对象
     */
    public static <T>T convertMapToBean(Map<?,?> source,Class<T> targetClass){
        if (null == source || source.size() == 0 || null == targetClass){
            return null;
        }
        CopyOptions copyOptions = CopyOptions.create().setIgnoreError(true);
        return BeanUtil.mapToBean(source,targetClass,true,copyOptions);
    }


    /**
     * 拷贝一个对象到一个新对象上
     * source bean convert to target bean
     * @param source 拷贝资源
     * @param targetClass 拷贝到目标对象的class
     * @return target bean
     */
    public static <T>T beanToBean(Object source,Class<T> targetClass){
        if (null == source || null == targetClass){
            return null;
        }
       return BeanUtil.copyProperties(source,targetClass);
    }


    /**
     * 对象转json
     * @param source
     * @return
     */
    public static String toJson(Object source){
       return JSONUtil.toJsonStr(source);
    }


    /**
     * json转bean
     * @param json
     * @param beanClass
     * @param <T>
     * @return
     */
    public static <T>T jsonToBean(String json,Class<T> beanClass){
        return JSONUtil.toBean(json,beanClass);
    }

    /**
     * 列表转列表
     * @param list
     * @param beanClass
     * @return
     */
    public static List<?> listToList(List<?> list, Class<?> beanClass){
        return JSONUtil.toList(JSONUtil.toJsonStr(list),beanClass);
    }

    /**
     *  bean对象转map对象
     * @param source
     * @return
     */
    public static Map<String,Object> toMap(Object source){
       return BeanUtil.beanToMap(source);
    }

    /**
     * 判断请求参数是否为空，其中 null 、""、空格、undefined都是表示空
     * @param v 值
     * @return
     */
    public static boolean isEmpty(String v){
        return null == v || v.trim().equals("") || "null".equals(v.toLowerCase()) || "undefined".equals(v.toLowerCase());
    }

    public static boolean isEmpty(Collection v){
        return null == v || v.isEmpty();
    }

    public static boolean isEmpty(Map v){
        return null == v || v.isEmpty();
    }

    /**
     * 不为空，其中 null 、""、空格、undefined都是表示空
     * @param v
     * @return
     */
    public static boolean isNotEmpty(String v){
        return !isEmpty(v);
    }



    public static boolean isNotEmpty(Collection v){
        return null != v && !v.isEmpty();
    }

    public static boolean isBasClass(Class<?> c){
        return c != null && (c == String.class || c == Integer.class || c == Long.class || c == Boolean.class || c == Double.class || c == Float.class || c == BigDecimal.class || c == Character.class);
    }

    public static <T>T baseClassTran(Object obj,Class<T> c){
        if ( c == null ){
            return null;
        }
        if (c == String.class){
            return (T) obj.toString();
        }
        if (c == Integer.class){
            return (T) Integer.valueOf(obj.toString());
        }
        if (c == Long.class){
            return (T) Long.valueOf(obj.toString());
        }
        if (c == Boolean.class){
            return (T) Boolean.valueOf(obj.toString());
        }
        if (c == Double.class){
            return (T) Double.valueOf(obj.toString());
        }
        if (c == Float.class){
            return (T) Float.valueOf(obj.toString());
        }
        if (c == BigDecimal.class){
            return (T) BigDecimal.valueOf(Double.valueOf(obj.toString()));
        }
        if (c == Character.class){
            return (T) Character.valueOf(obj.toString().charAt(0));
        }
        return null;
    }

    public static boolean isBaseValue(Object o){
        return o != null && (o instanceof String || o instanceof Integer || o instanceof Long || o instanceof Double || o instanceof Short || o instanceof Boolean || o instanceof Character || o instanceof BigDecimal);
    }
    /**
     * 范围随机数【startNum开始区间～endNum结束区间】+ incr 增量(可选)
     * @param startNum 开始区间
     * @param endNum 结束区间
     * @param incr 增量(可选)
     * @return
     */
    public static Integer randomNum(Integer startNum,Integer endNum, Integer incr){
        if (null == startNum || null == endNum || startNum > endNum){
            return null == incr ? 0 : incr;
        }
        Double value = (Math.random() * (endNum - startNum)) + startNum ;
        if (null != incr){
            value += incr;
        }
        return value.intValue();
    }


    public static String toSqlFeilds(Class entity,String asTableName){
        Field[] fields = entity.getDeclaredFields();
        String as = " ";
        if (isNotEmpty(asTableName)){
            as = asTableName + ".";
        }
        String sql = "";
        for (Field field : fields){
            sql += ", "+as + formatFildsName(field.getName()) +" as "+ field.getName();
        }
        return sql.substring(1);
    }

    private static String formatFildsName(String name){

        String newName = "";
        char[] sourceNames = name.toCharArray();
        char[] lowerNames = name.toLowerCase().toCharArray();
        for (int i = 0;i< sourceNames.length;i++){
            if (sourceNames[i] == lowerNames[i]){
                newName += lowerNames[i] + "";
            }else {
                newName += "_"+lowerNames[i];
            }
        }
        return newName;
    }


    /**
     * url是否包含关键key
     * @param url
     * @param key
     * @return
     */
    public static boolean isUrlContains(String url,String key){
        try {
            if (url.contains(key)){
                return true;
            }
            if (url.contains("url=")){
                String tmp = url.split("url=")[1];
                tmp = URLDecoder.decode(tmp,"UTF-8");
                return tmp.contains(key);
            }
        }catch (Exception e){
            log.error("解码异常 | {} | {} ",url,key,e);
        }
        return false;
    }

    /**
     * 签名算法
     *
     * @param o 要参与签名的数据对象
     * @return 签名
     * @throws IllegalAccessException
     */
    public static String getSign(Object o, String payApiKey) {
        try {
            ArrayList<String> list = new ArrayList<String>();
            Class cls = o.getClass();
            Field[] fields = cls.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                String k = f.getName();
                if ("packageValue".equals(k)){
                    k = "package";
                }
                if (f.get(o) != null && f.get(o) != ""
                        && !"serialVersionUID".equals(f.getName())) {
                    list.add( k + "=" + f.get(o) + "&");
                }
            }
            int size = list.size();
            String[] arrayToSort = list.toArray(new String[size]);
            Arrays.sort(arrayToSort, String.CASE_INSENSITIVE_ORDER);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append(arrayToSort[i]);
            }
            String result = sb.toString();
            result += "key=" + payApiKey;
            result = getMD5(result).toUpperCase();
            return result;
        } catch (Exception e) {
            log.error("签名算法生成签名失败", e);
        }
        return null;
    }

    /**
     * MD5加密
     * @param content
     * @return
     */
    public static String getMD5(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(content.getBytes());
            return getHashString(digest);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getHashString(MessageDigest digest) {
        StringBuilder builder = new StringBuilder(

        );
        for (byte b : digest.digest()) {
            builder.append(Integer.toHexString((b >> 4) & 0xf));
            builder.append(Integer.toHexString(b & 0xf));
        }
        return builder.toString();
    }

    /**
     * 获取一定长度的随机字符串
     *
     * @param length 指定字符串长度
     * @return 一定长度的字符串
     */
    public static String getRandomStringByLength(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    /**
     * Sql的字符串值
     * @param v
     * @return
     */
    public static String sqlStrValue(String v){
        return "'"+v+"'";
    }

    /**
     *  如果非空则返回value，否则返回 defaultValue
     * @param value
     * @param defaultValue
     * @return
     */
    public static String defaultString(String value,String defaultValue){
        return isNotEmpty(value) ? value : defaultValue;
    }

    public static String userNameDesensitized(String userName) {
        return userName.charAt(0) + "**" + userName.charAt(userName.length() - 1);
    }


    /**
     * 是否在列表中
     * @param list
     * @param id
     * @return
     */
    public static boolean isIn(String list,String id){
        if (LQUtil.isEmpty(list) || LQUtil.isEmpty(id)){
            return false;
        }
        String[] ids = list.split(",");
        for (String s : ids) {
            if (id.equalsIgnoreCase(s)){
                return true;
            }
        }
        return false;
    }

    /**
     * 是否在列表中
     * @param list
     * @param id
     * @return
     */
    public static boolean isInBaseList(String source,String ...baseList){
        if (baseList == null || LQUtil.isEmpty(source)){
            return false;
        }
        for (String s : baseList) {
            if (source.equalsIgnoreCase(s)){
                return true;
            }
        }
        return false;
    }

    /**
     * 是否在列表中
     * @param list
     * @param id
     * @return
     */
    public static boolean isInLike(String text,String[] key){
        if (LQUtil.isEmpty(text) || null == key || key.length == 0){
            return false;
        }
        for (String s : key) {
            if (text.contains(s)){
                return true;
            }
        }
        return false;
    }


    /**
     * 定时加载任务
     * @param runnable
     * @param initialDelay
     * @param period
     * @param unit
     */
    public static void execLoadJob(String jobName, Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        ThreadFactory threadFactory = r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("LQ-JOB-" + jobName);
            return thread;
        };
        Executors.newSingleThreadScheduledExecutor(threadFactory).scheduleAtFixedRate(()->{
            //为了避免异常导致任务停止，这个进行统一异常处理
            TryCatch.trying(jobName,()->{
                runnable.run();
            });
        }, initialDelay, period, unit);
    }

    /**
     * 生成随机数
     * @param size
     * @return
     */
    public static int randomInt(int size) {
        return size > 1 ? (int)(Math.random() * size) : 0;
    }

    public static boolean isEmptyOrZero(Long userId) {
        return null == userId || 0 == userId;
    }


    /**
     * 获取map集合的列表，不存在则创建
     * @param map map桶
     * @param key map的key
     * @return
     * @param <K>
     * @param <V>
     */
    public static <K,V> List<V> getMapList(Map<K,List<V>> map, K key) {
        if (map == null) {
            return null;
        }
        List<V> list = map.get(key);
        if (null == list) {
            list = new ArrayList<V>();
            map.put(key, list);
        }
        return list;
    }

    public static String formatTimeUnit(int time) {
        if (time <= 1){
            return "1秒";
        }
        if (time < 60){
            return time +"秒";
        }else if (time < 3600){
            return time/60 +"分钟";
        }else if (time < 3600 * 24){
            return time/3600 +"小时";
        }else {
            int day = time/3600/24;
            return  day > 365 * 2 ?  "永久" : day+"天";
        }
    }


    /**
     * 大于等于
     * @param l
     * @param r
     * @return
     */
    public static boolean gtn(Integer l, Integer r) {
        if (null == l || null == r ){
            return false;
        }
        return l >= r;
    }

    /**
     *  创建可编写文案
     * @return
     */
    public static WriteWord optWord(){
        return WriteWord.opt();
    }

    /**
     * maxTotal 小于 min 返回 def 否则 返回 maxTotal
     * @param maxTotal
     * @param min
     * @param def
     * @return
     */
    public static Integer lt(Integer maxTotal, int min, int def) {
        if (maxTotal == null){
            return def;
        }
        return maxTotal < min ? def : maxTotal;
    }


    /**
     * 文案对象
     */
    public static class WriteWord{
        private StringBuffer word;
        public static WriteWord opt(){
            WriteWord writeWord = new WriteWord();
            writeWord.word = new StringBuffer();
            return writeWord;
        }
        public WriteWord append(String str){
            return append("#080808",str);
        }

        public WriteWord br(){
            word.append("<br/>");
            return this;
        }
        public WriteWord append(String color,String str){
            word.append("<font color='"+color+"'>"+str+"</font>");
            return this;
        }

        public WriteWord appendBlod(String color,String str){
            word.append("<font color='"+color+"'><b>"+str+"</b></font>");
            return this;
        }


        public WriteWord appendLink(String str,String link){
            word.append("<a href='"+link+"'>"+str+"</a>");
            return this;
        }



        public String getWord(){
            return word.toString();
        }

    }

    /**
     * 格式化模版
     * @param target
     * @param params
     * @return
     */
    public static String doFormat(String target,Map<String,String> params){
        if (isEmpty(target) || null == params || params.size() == 0){
            return target;
        }
        for (String key : params.keySet()){
            target = target.replace(key,params.get(key));
        }
        return target;
    }

    public static int equalsIfTrue(String a,String b, int trueValue,int falseValye) {
        return Objects.equals(a, b) ? trueValue : falseValye;
    }


    public static String like(String str){
        return "%" + str + "%";
    }

    public static Class<?> getTypeClass(Class<?> c){
        Type type = c.getGenericSuperclass();
        if (null != type){
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] params = parameterizedType.getActualTypeArguments();
            Class<?> actType = (Class<?>) params[0];
            return actType;
        }
        return String.class;
    }
}
