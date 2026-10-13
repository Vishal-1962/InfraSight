package com.infrasight.web;

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

import com.infrasight.dto.ResolveForm;
import com.infrasight.entity.Incident;
import com.infrasight.service.EndpointService;
import com.infrasight.service.IncidentService;

import jakarta.validation.Valid;

@Controller
public class IncidentController {

    private final IncidentService incidents;
    private final EndpointService endpoints;

    public IncidentController(IncidentService incidents, EndpointService endpoints) {
        this.incidents = incidents;
        this.endpoints = endpoints;
    }

    @GetMapping("/incidents")
    public String list(@RequestParam(defaultValue = "active") String status,
                       @RequestParam(defaultValue = "0") long endpointId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String filter = switch (status) {
            case "open", "acknowledged", "resolved", "all" -> status;
            default -> "active";
        };
        Page<Incident> result = incidents.search(filter, endpointId, page);
        model.addAttribute("incidents", result);
        model.addAttribute("status", filter);
        model.addAttribute("endpointId", endpointId);
        model.addAttribute("endpoints", endpoints.findAll());
        model.addAttribute("autoRefresh", true);
        return "incidents";
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public String acknowledge(@PathVariable Long id, RedirectAttributes redirect) {
        incidents.acknowledge(id);
        redirect.addFlashAttribute("success", "Incident acknowledged.");
        return "redirect:/incidents";
    }

    @PostMapping("/incidents/{id}/resolve")
    public String resolve(@PathVariable Long id, @Valid @ModelAttribute ResolveForm form,
                          BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", "The resolution note must be 1000 characters or fewer.");
            return "redirect:/incidents";
        }
        incidents.resolve(id, form.getNote());
        redirect.addFlashAttribute("success", "Incident resolved.");
        return "redirect:/incidents?status=all";
    }
}
