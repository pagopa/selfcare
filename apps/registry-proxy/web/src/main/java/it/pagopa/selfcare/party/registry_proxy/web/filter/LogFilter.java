package it.pagopa.selfcare.party.registry_proxy.web.filter;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class LogFilter implements Filter {

    private static final int MAX_LENGTH_CONTENT = 150;
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        final String httpUri = httpServletRequest.getRequestURI();

        if (httpUri.startsWith("/actuator/health")) {
            log.trace("request to health-check actuator");
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        
        final String httpMethod = httpServletRequest.getMethod();
        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper requestCacheWrapperObject = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper  responseCacheWrapperObject = new ContentCachingResponseWrapper(httpServletResponse);

        chain.doFilter(requestCacheWrapperObject, responseCacheWrapperObject);

        Long endTime = System.currentTimeMillis() - startTime;
        String requestBody = this.getContentAsString(requestCacheWrapperObject.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = this.getContentAsString(responseCacheWrapperObject.getContentAsByteArray(), response.getCharacterEncoding());
        String queryParams = requestCacheWrapperObject.getQueryString() != null ? requestCacheWrapperObject.getQueryString() : "";
        log.info("Request from URI : {} - method: {} - timelapse: {}ms - Query params {} - Request body {} - Response body {}", httpUri, httpMethod, endTime, MaskDataUtils.maskInformation(queryParams), MaskDataUtils.maskInformation(requestBody), MaskDataUtils.maskInformation(responseBody));
        responseCacheWrapperObject.copyBodyToResponse();
    }

    private String getContentAsString(byte[] buf, String charsetName) {
        if (buf == null || buf.length == 0) {
            return "";
        }

        try {
            String content = new String(buf, charsetName);
            return content.substring(0, Math.min(MAX_LENGTH_CONTENT, content.length()));
        } catch (UnsupportedEncodingException ex) {
            return "Unsupported Encoding";
        }
    }

}
