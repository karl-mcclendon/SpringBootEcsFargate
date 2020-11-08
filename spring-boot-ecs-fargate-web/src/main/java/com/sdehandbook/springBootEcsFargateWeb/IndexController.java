package com.sdehandbook.springBootEcsFargateWeb;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public final class IndexController {

    @GetMapping(value="/")
    @ResponseBody
    public String index() {
        return "<h1>Hello!</h1>";
    }
}
