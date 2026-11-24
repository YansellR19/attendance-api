package com.attendance.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(basePackages = "com.attendance.api.controller.web")
public class GlobalWebExceptionHandler {

    @ExceptionHandler({BusinessRuleException.class, DuplicateResourceException.class, ResourceNotFoundException.class})
    public String onWebDomainError(
            RuntimeException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/dashboard";
    }
}
