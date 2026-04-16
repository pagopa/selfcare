package it.pagopa.selfcare.mscore.web.filter;

import it.pagopa.selfcare.mscore.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class LogFilter implements Filter {

    private static final int MAX_LENGTH_CONTENT = 500;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
        String requestBody = getContentAsString(requestCacheWrapperObject.getContentAsByteArray(), request.getCharacterEncoding(), false);
        log.info("Request from URI : {} - method: {} - Request body: {}", httpUri, httpMethod, requestBody);

        Long endTime = System.currentTimeMillis() - startTime;
        String responseBody = getContentAsString(responseCacheWrapperObject.getContentAsByteArray(), response.getCharacterEncoding(), true);
        log.info("Response from URI : {} - method: {} - status: {} - timelapse: {}ms - Response body: {}", httpUri, httpMethod, httpServletResponse.getStatus(), endTime, responseBody);
        responseCacheWrapperObject.copyBodyToResponse();
    }

    private String getContentAsString(byte[] buf, String charsetName, boolean isResponse) {
        if (buf == null || buf.length == 0) {
            return "empty";
        }
        try {
            String content = new String(buf, charsetName);
            String maskedContent = MaskDataUtils.maskInformation(content);
            if(isResponse) {
                return maskedContent.substring(0, Math.min(MAX_LENGTH_CONTENT, maskedContent.length()));
            }
            return maskedContent;
        } catch (UnsupportedEncodingException ex) {
            return "Unsupported Encoding";
        }
    }

}
