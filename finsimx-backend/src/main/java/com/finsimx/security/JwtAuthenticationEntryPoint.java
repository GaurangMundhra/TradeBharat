package com.finsimx.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsimx.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            AuthenticationException ex) throws IOException, ServletException {

        log.error("Responding with unauthorized error. Message - {}", ex.getMessage());

        httpServletResponse.setContentType("application/json");
        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final ApiResponse<?> body = ApiResponse.error(
                "Unauthorized: " + ex.getMessage(),
                HttpServletResponse.SC_UNAUTHORIZED);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(httpServletResponse.getOutputStream(), body);
    }
}
