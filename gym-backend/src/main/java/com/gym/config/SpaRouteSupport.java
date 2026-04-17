package com.gym.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

public final class SpaRouteSupport {

    private static final Set<String> API_ONLY_ROOTS = Set.of(
            "api",
            "admin",
            "dict",
            "error",
            "form-config",
            "member",
            "menu",
            "swagger-ui",
            "v3",
            "webjars"
    );

    private static final Set<String> MIXED_ROOTS = Set.of(
            "coach",
            "course",
            "equipment",
            "reservation",
            "user",
            "venue"
    );

    private static final Set<String> STATIC_ROOTS = Set.of(
            "assets",
            "css",
            "fonts",
            "img",
            "images",
            "js"
    );

    private SpaRouteSupport() {
    }

    public static RequestMatcher frontendRouteMatcher() {
        return request -> HttpMethod.GET.matches(request.getMethod())
                && acceptsHtml(request)
                && isFrontendRoute(request);
    }

    public static boolean isFrontendRoute(HttpServletRequest request) {
        return isFrontendRoute(extractPath(request));
    }

    public static boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept == null
                || accept.contains(MediaType.TEXT_HTML_VALUE)
                || accept.contains(MediaType.APPLICATION_XHTML_XML_VALUE);
    }

    public static boolean isFrontendRoute(String rawPath) {
        String path = normalizePath(rawPath);
        if (path == null) {
            return false;
        }

        if ("/".equals(path) || "/index.html".equals(path)) {
            return true;
        }

        String[] segments = path.substring(1).split("/");
        if (segments.length == 0 || segments[0].isBlank()) {
            return false;
        }

        String firstSegment = segments[0];
        String lastSegment = segments[segments.length - 1];
        if (STATIC_ROOTS.contains(firstSegment) || lastSegment.contains(".")) {
            return false;
        }

        if (segments.length == 1) {
            return !API_ONLY_ROOTS.contains(firstSegment);
        }

        return !API_ONLY_ROOTS.contains(firstSegment) && !MIXED_ROOTS.contains(firstSegment);
    }

    private static String extractPath(HttpServletRequest request) {
        Object errorRequestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = errorRequestUri instanceof String ? (String) errorRequestUri : request.getRequestURI();
        return stripContextPath(path, request.getContextPath());
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String stripContextPath(String path, String contextPath) {
        if (path == null || path.isBlank()) {
            return path;
        }
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}
