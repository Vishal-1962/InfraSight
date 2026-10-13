package com.infrasight.web;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.infrasight.dto.EndpointForm;
import com.infrasight.dto.TrendPoint;
import com.infrasight.entity.CheckResult;
import com.infrasight.entity.MonitoredEndpoint;
import com.infrasight.service.EndpointService;
import com.infrasight.service.HealthCheckService;
import com.infrasight.service.HistoryService;
import com.infrasight.service.IncidentService;
import com.infrasight.service.TrendService;
import com.infrasight.service.UnsafeUrlException;

import jakarta.validation.Valid;

@Controller
public class EndpointController {

    private final EndpointService endpoints;
    private final HealthCheckService healthChecks;
    private final HistoryService history;
    private final IncidentService incidents;
    private final TrendService trends;

    public EndpointController(EndpointService endpoints, HealthCheckService healthChecks, HistoryService history,
                              IncidentService incidents, TrendService trends) {
        this.endpoints = endpoints;
        this.healthChecks = healthChecks;
        this.history = history;
        this.incidents = incidents;
        this.trends = trends;
    }

    @GetMapping("/endpoints")
    public String list(Model model) {
        model.addAttribute("endpoints", endpoints.findAll());
        model.addAttribute("autoRefresh", true);
        return "endpoints";
    }

    @GetMapping("/endpoints/new")
    public String newForm(Model model) {
        model.addAttribute("form", new EndpointForm());
        model.addAttribute("editing", false);
        return "endpoint-form";
    }

    @PostMapping("/endpoints")
    public String create(@Valid @ModelAttribute("form") EndpointForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                MonitoredEndpoint created = endpoints.create(form);
                redirect.addFlashAttribute("success", "Endpoint added. The first check runs within a few seconds.");
                return "redirect:/endpoints/" + created.getId();
            } catch (UnsafeUrlException ex) {
                errors.rejectValue("url", "url.unsafe", ex.getMessage());
            }
        }
        model.addAttribute("editing", false);
        return "endpoint-form";
    }

    @GetMapping("/endpoints/{id}")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, Model model) {
        MonitoredEndpoint endpoint = endpoints.get(id);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusHours(23).truncatedTo(ChronoUnit.HOURS);
        List<TrendPoint> trend = trends.trend(from, now.plusMinutes(1), id);
        Page<CheckResult> checks = history.forEndpoint(id, page, 20);

        model.addAttribute("endpoint", endpoint);
        model.addAttribute("summary", history.summary(now.minusHours(24), now.plusMinutes(1), id));
        model.addAttribute("trendJson", trends.toJson(trend));
        model.addAttribute("hasTrend", TrendService.hasData(trend));
        model.addAttribute("checks", checks);
        model.addAttribute("incidents", incidents.forEndpoint(id));
        model.addAttribute("autoRefresh", true);
        return "endpoint-detail";
    }

    @GetMapping("/endpoints/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MonitoredEndpoint endpoint = endpoints.get(id);
        model.addAttribute("form", EndpointForm.from(endpoint));
        model.addAttribute("endpoint", endpoint);
        model.addAttribute("editing", true);
        return "endpoint-form";
    }

    @PostMapping("/endpoints/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") EndpointForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                endpoints.update(id, form);
                redirect.addFlashAttribute("success", "Changes saved.");
                return "redirect:/endpoints/" + id;
            } catch (UnsafeUrlException ex) {
                errors.rejectValue("url", "url.unsafe", ex.getMessage());
            }
        }
        model.addAttribute("endpoint", endpoints.get(id));
        model.addAttribute("editing", true);
        return "endpoint-form";
    }

    @PostMapping("/endpoints/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam(defaultValue = "list") String from,
                         RedirectAttributes redirect) {
        MonitoredEndpoint endpoint = endpoints.toggle(id);
        redirect.addFlashAttribute("success",
                endpoint.isEnabled() ? "Monitoring enabled." : "Monitoring disabled. No further checks will run.");
        return "detail".equals(from) ? "redirect:/endpoints/" + id : "redirect:/endpoints";
    }

    @PostMapping("/endpoints/{id}/check")
    public String check(@PathVariable Long id, @RequestParam(defaultValue = "list") String from,
                        RedirectAttributes redirect) {
        CheckResult result = healthChecks.runNow(id);
        if (result == null) {
            redirect.addFlashAttribute("error", "A check for this endpoint is already running.");
        } else if (result.isSuccess()) {
            redirect.addFlashAttribute("success", "Check completed: up (HTTP " + result.getStatusCode()
                    + ", " + result.getResponseTimeMs() + " ms).");
        } else {
            redirect.addFlashAttribute("error", "Check completed: down. " + result.getErrorMessage());
        }
        return "detail".equals(from) ? "redirect:/endpoints/" + id : "redirect:/endpoints";
    }

    @PostMapping("/endpoints/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        endpoints.delete(id);
        redirect.addFlashAttribute("success", "Endpoint deleted along with its check history and incidents.");
        return "redirect:/endpoints";
    }
}
