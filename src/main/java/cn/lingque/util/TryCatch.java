package cn.lingque.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;


/**
 * <p>
 * 拦截异常
 * </p>
 *
 * @author zlm
 * @date 2023/3/21
 * @since v1.0
 */
@Slf4j
public class TryCatch<R> {


    /**
     * 简化try-catch
     *
     * @param remark 任务标题
     * @param exec
     */
    public static void trying(String remark, TrySomething exec) {
        long start = System.currentTimeMillis();
        try {
            log.debug(" [开始] ｜ {}", remark);
            exec.trying();
            log.debug(" [结束] : {}ms ｜ {} ", System.currentTimeMillis() - start, remark);
        } catch (Exception e) {
            log.error("TryCatch 异常 | {}", remark, e);
        }
    }

    public static void trying(TrySomething exec) {
        try {
            exec.trying();
        } catch (Exception e) {
            log.error("TryCatch 异常 ", e);
        }
    }

    public static void tryingIgnoreError(TrySomething exec) {
        try {
            exec.trying();
        } catch (Exception e) {
            log.debug("TryCatch 异常 ", e);
        }
    }

    /**
     * 简化try-catch
     *
     * @param remark 任务标题
     * @param exec
     */
    public static void trying(TrySomething exec, String remark, Object... logParams) {
        long start = System.currentTimeMillis();
        try {
            remark = logs(remark, logParams);
            log.debug(" [开始] ｜ {}", remark);
            exec.trying();
            log.debug(" [结束] : {}ms ｜ {} ", System.currentTimeMillis() - start, remark);
        } catch (Exception e) {
            log.error("TryCatch 异常 | {}", remark, e);
        }
    }


    /**
     * 简化try-catch
     */
    public static <R> R supplier(Supplier<R> supplier, R defaultResult) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("supplier 异常", e);
        }
        return defaultResult;
    }


    /**
     * 简化try-catch
     *
     * @param remark 任务标题
     * @param exec
     */
    public static Object tryResult(TryResult exec, Object defaultResult, String remark, Object... logParams) {
        long start = System.currentTimeMillis();
        try {
            remark = logs(remark, logParams);
            log.debug(" [开始] ｜ {}", remark);
            return exec.trying();
        } catch (Exception e) {
            log.error("TryCatch 异常 | {}", remark, e);
        } finally {
            log.debug(" [结束] : {}ms ｜ {} ", System.currentTimeMillis() - start, remark);
        }
        return defaultResult;
    }

    /**
     * 简化try-catch
     *
     * @param exec
     */
    public static <R>R tryResult(TryResult<R> exec, R defaultResult) {
        long start = System.currentTimeMillis();
        try {
            return exec.trying();
        } catch (Exception e) {
            log.debug("TryCatch 异常 ", e);
        } finally {
            log.debug(" [结束] : {}ms  ", System.currentTimeMillis() - start);
        }
        return defaultResult;
    }

    /**
     * 日志拼接
     *
     * @param remark
     * @param params
     * @return
     */
    public static String logs(String remark, Object... params) {
        String[] kds = remark.split("\\{\\}");
        int index = 0;
        int startDiff = 0;
        for (Object p : params) {
            if (index + startDiff >= params.length) {
                break;
            }
            String pStr = p instanceof Long ? Long.toString((long) p) : p.toString();
            kds[index + startDiff] += pStr;
            index++;
        }
        return String.join("", kds);
    }


    public interface TrySomething {
        void trying() throws Exception;
    }

    public interface TryResult<E> {
        E trying() throws Exception;
    }
}
