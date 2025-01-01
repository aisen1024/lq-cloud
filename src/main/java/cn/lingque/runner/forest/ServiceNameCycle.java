package cn.lingque.runner.forest;

import cn.lingque.runner.annon.LqService;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.lifecycles.MethodAnnotationLifeCycle;
import com.dtflys.forest.reflection.ForestMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceNameCycle implements MethodAnnotationLifeCycle<LqService, Object> {


    /**
     * 当方法调用时调用此方法，此时还没有执行请求发送
     * 此方法可以获得请求对应的方法调用信息，以及动态传入的方法调用参数列表
     */
    @Override
    public void onInvokeMethod(ForestRequest request, ForestMethod method, Object[] args) {
        Object serviceName = getAttribute(request, "serviceName");
        log.info("forest serviceName:{}", serviceName);
        request.setHost(serviceName.toString());
    }

    /**
     * 发送请求前执行此方法，同拦截器中的一样
     */
    @Override
    public boolean beforeExecute(ForestRequest request) {
        return true;
    }

    /**
     * 此方法在请求方法初始化的时候被调用
     */
    @Override
    public void onMethodInitialized(ForestMethod method, LqService annotation) {
    }
}
