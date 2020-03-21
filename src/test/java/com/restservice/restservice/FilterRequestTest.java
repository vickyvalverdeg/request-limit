package com.restservice.restservice;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.restservice.restservice.filter.FilterRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.MockUtil.createMock;

@RunWith(MockitoJUnitRunner.class)
public class FilterRequestTest {

    private static final int MAX_REQUESTS_FOR_A_SPECIFIC_TIME = 5;

    private MockHttpServletRequest mockHttpServletRequest;

    private MockHttpServletResponse mockHttpServletResponse;

    private FilterChain mockFilterChain;

    @Mock
    private LoadingCache<String, Integer> mockRequestCounts;

    @InjectMocks
    private FilterRequest filterRequest;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(filterRequest, "MAX_REQUESTS_PER_HOUR", MAX_REQUESTS_FOR_A_SPECIFIC_TIME);

        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setRequestURI("/anyUri");

        mockHttpServletResponse = new MockHttpServletResponse();

        mockFilterChain = Mockito.mock(FilterChain.class);
    }

    @Test
    public void shouldReturnHttpStatusOKWhenNumberOfReceivedRequestIsLessThatMaxRequestPerHour() throws ServletException, IOException, ExecutionException {
        int numberOfReceivedRequests = 2;

        when(mockRequestCounts.get("requests")).thenReturn(numberOfReceivedRequests);

        filterRequest.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getStatus(), is(HttpStatus.OK.value()));
    }

    @Test
    public void shouldReturnHttpStatusTooManyRequestWhenNumberOfReceivedRequestIsEqualThatMaxRequestPerHour() throws ServletException, IOException, ExecutionException {
        int numberOfReceivedRequests = MAX_REQUESTS_FOR_A_SPECIFIC_TIME;

        when(mockRequestCounts.get("requests")).thenReturn(numberOfReceivedRequests);

        filterRequest.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getStatus(), is(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    public void shouldReturnHttpStatusTooManyRequestWhenNumberOfReceivedRequestIsGreaterThatMaxRequestPerHour() throws IOException, ServletException, ExecutionException {
        int numberOfReceivedRequests = 10;

        when(mockRequestCounts.get("requests")).thenReturn(numberOfReceivedRequests);

        filterRequest.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getStatus(), is(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

}
