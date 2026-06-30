package com.paypay.lqadmin.controller;

import cn.lingque.mq.LQMQTemplate;
import com.paypay.lqadmin.service.MessageQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 消息队列管理接口
 */
@RestController
@RequestMapping("/api/mq")
@CrossOrigin(origins = "*")
public class MessageQueueController {

    @Autowired
    private MessageQueueService messageQueueService;

    @GetMapping("/queues")
    public List<Map<String, Object>> getQueues() {
        return messageQueueService.getAllQueues();
    }

    @GetMapping("/queue/{queue}")
    public Map<String, Object> getQueueInfo(@PathVariable String queue) {
        return messageQueueService.getQueueInfo(queue);
    }

    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> body) {
        String queue = (String) body.get("queue");
        Object message = body.get("message");
        Integer delay = (Integer) body.getOrDefault("delay", 0);
        
        try {
            if (delay > 0) {
                LQMQTemplate.sendDelayMessage(queue, message, delay.longValue());
            } else {
                LQMQTemplate.sendOrderMessage(queue, message);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "消息发送成功");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "消息发送失败: " + e.getMessage());
            return result;
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return messageQueueService.getMQStats();
    }

    @DeleteMapping("/queue/{queue}")
    public Map<String, Object> clearQueue(@PathVariable String queue) {
        try {
            messageQueueService.clearQueue(queue);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "队列清空成功");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "队列清空失败: " + e.getMessage());
            return result;
        }
    }
}
