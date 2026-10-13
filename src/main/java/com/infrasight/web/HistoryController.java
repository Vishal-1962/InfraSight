package com.infrasight.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.infrasight.dto.TrendPoint;
import com.infrasight.entity.CheckResult;
import com.infrasight.service.EndpointService;
import com.infrasight.service.HistoryService;
import com.infrasight.service.TrendService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class HistoryController {

    private static final int EXPORT_PAGE_SIZE = 1000;
    private static final int EXPORT_MAX_ROWS = 100_000;
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final HistoryService history;
    private final EndpointService endpoints;
    private final TrendService trends;

    public HistoryController(HistoryService history, EndpointService endpoints, TrendService trends) {
        this.history = history;
        this.endpoints = endpoints;
        this.trends = trends;
    }

    @GetMapping("/history")
    public String history(@RequestParam(defaultValue = "0") long endpointId,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(defaultValue = "all") String result,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        LocalDate[] range = Ranges.resolve(from, to, 7);
        int mode = HistoryService.mode(result);
        Page<CheckResult> checks = history.search(range[0], range[1], endpointId, mode,
                PageRequest.of(Math.max(page, 0), HistoryService.PAGE_SIZE));
        List<TrendPoint> trend = trends.trend(range[0].atStartOfDay(), range[1].plusDays(1).atStartOfDay(), endpointId);

        model.addAttribute("endpoints", endpoints.findAll());
        model.addAttribute("endpointId", endpointId);
        model.addAttribute("from", range[0]);
        model.addAttribute("to", range[1]);
        model.addAttribute("result", mode == 1 ? "success" : mode == 2 ? "failed" : "all");
        model.addAttribute("checks", checks);
        model.addAttribute("summary", history.summary(range[0], range[1], endpointId));
        model.addAttribute("trendJson", trends.toJson(trend));
        model.addAttribute("hasTrend", TrendService.hasData(trend));
        return "history";
    }

    @GetMapping("/history/export")
    public void export(@RequestParam(defaultValue = "0") long endpointId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "all") String result,
                       HttpServletResponse response) throws IOException {
        LocalDate[] range = Ranges.resolve(from, to, 7);
        int mode = HistoryService.mode(result);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"infrasight-checks-" + LocalDateTime.now().format(FILE_TIME) + ".csv\"");
        response.setHeader("Cache-Control", "no-store");

        PrintWriter out = response.getWriter();
        out.write('\uFEFF'); // lets Excel detect UTF-8
        out.print("checked_at,endpoint,url,result,status_code,response_time_ms,error\r\n");

        int written = 0;
        int pageNumber = 0;
        while (written < EXPORT_MAX_ROWS) {
            Page<CheckResult> chunk = history.search(range[0], range[1], endpointId, mode,
                    PageRequest.of(pageNumber, EXPORT_PAGE_SIZE));
            for (CheckResult c : chunk.getContent()) {
                out.print(c.getCheckedAt().format(CSV_TIME));
                out.print(',');
                out.print(text(c.getEndpoint().getName()));
                out.print(',');
                out.print(text(c.getEndpoint().getUrl()));
                out.print(',');
                out.print(c.isSuccess() ? "success" : "failed");
                out.print(',');
                out.print(c.getStatusCode() == null ? "" : c.getStatusCode().toString());
                out.print(',');
                out.print(c.getResponseTimeMs() == null ? "" : c.getResponseTimeMs().toString());
                out.print(',');
                out.print(text(c.getErrorMessage()));
                out.print("\r\n");
                written++;
            }
            if (!chunk.hasNext()) {
                break;
            }
            pageNumber++;
        }
        out.flush();
    }

    /** Quotes a CSV text cell and neutralises spreadsheet formulas (CSV injection). */
    private static String text(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String cell = value;
        char first = cell.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            cell = "'" + cell;
        }
        return "\"" + cell.replace("\"", "\"\"") + "\"";
    }
}
