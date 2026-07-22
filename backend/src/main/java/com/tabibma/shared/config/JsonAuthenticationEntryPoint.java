package com.tabibma.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.shared.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/** Handles auth failures raised inside the filter chain (before any controller runs) as JSON, not the servlet container's default HTML error page. */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String requestId = String.valueOf(request.getAttribute("requestId"));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new ErrorResponse(new ErrorResponse.ErrorBody("UNAUTHORIZED", "Authentication is required.", List.of(), requestId));
        objectMapper.writeValue(response.getWriter(), body);
    }
}
