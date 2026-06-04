package com.techmanage.config;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA 路由支持：非 API 请求转发到 index.html（Vue Router history 模式）
 */
@Controller
public class SpaController implements ErrorController {

    /**
     * 当 Spring Boot 找不到匹配的静态资源时，返回 index.html
     * 让 Vue Router 接管前端路由
     */
    @RequestMapping(value = "/error")
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
