package com.tabibma.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.shared.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/** Handles authorization failures (valid principal, wrong role/ownership) as JSON — the 403 counterpart to JsonAuthenticationEntryPoint's 401. */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        String requestId = String.valueOf(request.getAttribute("requestId"));
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new ErrorResponse(new ErrorResponse.ErrorBody("FORBIDDEN", "You do not have permission to access this resource.", List.of(), requestId));
        objectMapper.writeValue(response.getWriter(), body);
    }
}
