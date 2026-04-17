package com.gym.config;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaRootForwardController {

    @GetMapping(
            value = {"/coach", "/course", "/equipment", "/reservation", "/user", "/venue"},
            produces = MediaType.TEXT_HTML_VALUE
    )
    public String forwardSpaRoot() {
        return "forward:/index.html";
    }
}
