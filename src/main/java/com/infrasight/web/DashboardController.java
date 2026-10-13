package com.infrasight.web;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.infrasight.dto.TrendPoint;
import com.infrasight.service.DashboardService;
import com.infrasight.service.IncidentService;
import com.infrasight.service.TrendService;

@Controller
public class DashboardController {

    private final DashboardService dashboard;
    private final TrendService trends;
    private final IncidentService incidents;

    public DashboardController(DashboardService dashboard, TrendService trends, IncidentService incidents) {
        this.dashboard = dashboard;
        this.trends = trends;
        this.incidents = incidents;
    }

    @GetMapping("/")
    public String index(Model model) {
        LocalDateTime now = LocalDateTime.now();
        List<TrendPoint> trend = trends.trend(now.minusHours(23).truncatedTo(ChronoUnit.HOURS), now.plusMinutes(1), 0L);

        model.addAttribute("stats", dashboard.stats());
        model.addAttribute("trendJson", trends.toJson(trend));
        model.addAttribute("hasTrend", TrendService.hasData(trend));
        model.addAttribute("recentChecks", dashboard.recentChecks(10));
        model.addAttribute("recentIncidents", incidents.recent(5));
        model.addAttribute("autoRefresh", true);
        return "dashboard";
    }
}
