package com.restservice.restservice.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class FilterRequest implements Filter {

    private final int MAX_REQUESTS_PER_HOUR = 3;
    private static final String KEY_NAME = "requests";
    private static final int ZERO_REQUESTS = 0;
    final static Logger logger = Logger.getLogger(FilterRequest.class);

    private LoadingCache<String, Integer> requestCounts;

    public FilterRequest(){
        super();

        requestCounts = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(
                        new CacheLoader<String, Integer>() {
                            public Integer load(String key) {
                                return 0;
                            }
                        }
                );
    }

    @Override
    public void init(FilterConfig filterConfig)  {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if(isMaximumRequestsPerHourExceeded()){
            httpServletResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            httpServletResponse.getWriter().write("Rate limit exceeded. Try again in 10 seconds.");
            resetRequestCountAfterAmountOfSeconds();
            httpServletResponse.getWriter().flush();
        }

        loggerInfo(httpServletRequest, httpServletResponse);

        chain.doFilter(request, response);
    }

    private boolean isMaximumRequestsPerHourExceeded(){
        boolean isExcededRequest;
        try {
            int requests = requestCounts.get(KEY_NAME);

            isExcededRequest = (requests >= MAX_REQUESTS_PER_HOUR);

            requests++;

            requestCounts.put(KEY_NAME, requests);

        } catch (ExecutionException e) {
            isExcededRequest = false;
        }

        return isExcededRequest;
    }

    private void resetRequestCountAfterAmountOfSeconds() {
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        requestCounts.put(KEY_NAME, ZERO_REQUESTS);
                    }
                }, 10000
        );
    }

    private void loggerInfo(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        logger.info(String.format("Logging Request Method %s", httpServletRequest.getMethod()));
        logger.info(String.format("Logging Request URI %s", httpServletRequest.getRequestURI()));
        logger.info(String.format("Logging Response Status Code %s", httpServletResponse.getStatus()));

    }
}
