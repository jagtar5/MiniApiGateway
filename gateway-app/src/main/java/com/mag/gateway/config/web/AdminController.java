package com.mag.gateway.config.web;

import com.mag.gateway.config.AdminService;
import com.mag.gateway.config.ConfigSnapshot;
import com.mag.gateway.config.RateLimitRuleConfig;
import com.mag.gateway.config.RouteConfig;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Thin HTTP layer: all logic is in AdminService (already unit-tested). */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/routes")
    public List<RouteConfig> listRoutes() { return service.listRoutes(); }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteConfig createRoute(@RequestBody RouteConfig body) { return service.createRoute(body); }

    @PutMapping("/routes/{id}")
    public RouteConfig updateRoute(@PathVariable String id, @RequestBody RouteConfig body) {
        return service.updateRoute(id, body);
    }

    @DeleteMapping("/routes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable String id) { service.deleteRoute(id); }

    @GetMapping("/rules")
    public List<RateLimitRuleConfig> listRules() { return service.listRules(); }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RateLimitRuleConfig createRule(@RequestBody RateLimitRuleConfig body) { return service.createRule(body); }

    @PutMapping("/rules/{routeId}")
    public RateLimitRuleConfig updateRule(@PathVariable String routeId, @RequestBody RateLimitRuleConfig body) {
        return service.updateRule(routeId, body);
    }

    @DeleteMapping("/rules/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable String routeId) { service.deleteRule(routeId); }

    @PostMapping("/reload")
    public ConfigSnapshot reload() { return service.reload(); }
}
