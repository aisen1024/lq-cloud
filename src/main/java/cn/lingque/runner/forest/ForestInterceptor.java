package cn.lingque.runner.forest;

import cn.lingque.cloud.node.bean.LQNodeInfo;
import cn.lingque.runner.config.LqSpringUtil;
import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.interceptor.Interceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;

@Slf4j
@Component
public class ForestInterceptor<T> implements Interceptor<T> {

    /**
     * 该方法在请求发送之前被调用, 若返回false则不会继续发送请求
     * @Param request Forest请求对象
     */
    @Override
    public boolean beforeExecute(ForestRequest req) {
        log.info("invoke Simple beforeExecute");
        URI newUri = getUriByServiceName(req.getHost());
        if(Objects.nonNull(newUri)){
            req.setHost(newUri.getAuthority());
        }
        return true;  // 继续执行请求返回true
    }

    private URI getUriByServiceName(String serviceName){
        LQNodeInfo choose = LqSpringUtil.getBean(LqBalanceService.class).pollingBalance(serviceName);
        if(Objects.nonNull(choose)){
            return choose.getUri("http");
        }
        return null;
    }
}
