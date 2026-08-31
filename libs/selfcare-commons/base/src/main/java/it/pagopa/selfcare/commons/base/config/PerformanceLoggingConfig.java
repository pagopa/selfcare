package it.pagopa.selfcare.commons.base.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/performance.properties")
@ConditionalOnProperty(value = "performance.logger.enabled", havingValue = "true")
@EnableAspectJAutoProxy
@Aspect
@Slf4j
public class PerformanceLoggingConfig {

    /**
     * Pointcut that matches all repositories, services and Web REST endpoints.
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)" +
            " || within(@org.springframework.data.repository.NoRepositoryBean *)" +
            " || within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.web.bind.annotation.RestController *)")
    public void springBeanPointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advices.
    }

    @Bean
    public Advisor performanceMonitorAdvisor() {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Creating Bean PerformanceLoggingConfig.performanceMonitorAdvisor of type %s", Advisor.class.getName()));
        }
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("it.gov.pagopa.bpd.common.config.PerformanceLoggingConfig.springBeanPointcut()");
        return new DefaultPointcutAdvisor(pointcut, performanceMonitorInterceptor());
    }

    @Bean
    public PerformanceMonitorInterceptor performanceMonitorInterceptor() {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Creating Bean PerformanceLoggingConfig.performanceMonitorInterceptor of type %s", PerformanceMonitorInterceptor.class.getName()));
        }
        return new PerformanceMonitorInterceptor(false);
    }

}
