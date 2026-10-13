package com.infrasight.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.infrasight.service.NotFoundException;

import jakarta.servlet.http.HttpServletResponse;

/** Shows a plain error page. Stack traces and internal messages are never sent to the browser. */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handle(Exception ex, HttpServletResponse response, Model model) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Something went wrong. The details are in the application log.";

        if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = ex.getMessage();
        } else if (ex instanceof TypeMismatchException) {
            status = HttpStatus.BAD_REQUEST;
            message = "One of the values in the request is not valid.";
        } else if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus resolved = HttpStatus.resolve(errorResponse.getStatusCode().value());
            status = resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
            if (status == HttpStatus.NOT_FOUND) {
                message = "The page you requested does not exist.";
            } else if (status.is4xxClientError()) {
                message = "The request could not be completed.";
            } else {
                log.error("Request failed", ex);
            }
        } else {
            log.error("Unhandled error", ex);
        }

        response.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("message", message);
        return "error";
    }
}
