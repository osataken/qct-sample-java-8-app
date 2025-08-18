package org.springframework.web.filter;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.ContentCachingResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Custom implementation of ShallowEtagHeaderFilter that uses SHA-512 for ETag generation.
 */
public class Sha512ShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {

    /**
     * Custom implementation that uses SHA-512 for ETag generation.
     */
	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        HttpServletResponse responseToUse = response;
        if (!isAsyncDispatch(request) && !response.isCommitted()) {
            responseToUse = new ContentCachingResponseWrapper(response);
        }
        
        filterChain.doFilter(request, responseToUse);
        
        if (!isAsyncStarted(request) && !response.isCommitted() && 
            responseToUse instanceof ContentCachingResponseWrapper wrapper) {
            
            // Only for GET requests as per ShallowEtagHeaderFilter behavior
            if (HttpMethod.GET.matches(request.getMethod())) {
                byte[] body = wrapper.getContentAsByteArray();
                if (body.length > 0) {
                    HashCode hash = Hashing.sha512().hashBytes(body);
                    String etag = "\"" + hash + "\"";
                    response.setHeader("ETag", etag);
                }
            }
            
            wrapper.copyBodyToResponse();
        }
	}
}
