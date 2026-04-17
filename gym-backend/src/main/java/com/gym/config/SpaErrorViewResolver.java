package com.gym.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.Map;

@Component
public class SpaErrorViewResolver implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if ((status == HttpStatus.NOT_FOUND || status == HttpStatus.METHOD_NOT_ALLOWED)
                && SpaRouteSupport.acceptsHtml(request)
                && SpaRouteSupport.isFrontendRoute(request)) {
            return new ModelAndView("forward:/index.html", Collections.emptyMap(), HttpStatus.OK);
        }
        return null;
    }
}
