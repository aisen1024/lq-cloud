package com.paypay.lqadmin.controller;

import com.paypay.lqadmin.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dashboard 总览接口
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return dashboardService.getOverview();
    }
}
