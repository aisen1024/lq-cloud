package com.paypay.lqadmin.controller;

import cn.lingque.bus.enhanced.LQEnhancedBus;
import com.paypay.lqadmin.service.MessageBusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 消息总线管理接口
 */
@RestController
@RequestMapping("/api/bus")
@CrossOrigin(origins = "*")
public class MessageBusController {

    @Autowired
    private MessageBusService messageBusService;

    @GetMapping("/topics")
    public List<Map<String, Object>> getTopics() {
        return messageBusService.getAllTopics();
    }

    @GetMapping("/topic/{topic}/subscribers")
    public List<Map<String, Object>> getSubscribers(@PathVariable String topic) {
        return messageBusService.getTopicSubscribers(topic);
    }

    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestBody Map<String, Object> body) {
        String topic = (String) body.get("topic");
        Object message = body.get("message");
        
        try {
            LQEnhancedBus.publish(topic, message);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "消息发布成功");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "消息发布失败: " + e.getMessage());
            return result;
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return messageBusService.getBusStats();
    }
}
