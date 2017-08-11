package com.xuehu365.controller;

import com.xuehu365.controller.base.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 视图控制器,返回jsp视图给前端
 **/
@Controller
@RequestMapping("/page")
public class PageController {
    /**
     * 404页
     */
    @RequestMapping("/404")
    @ResponseBody
    public Result error404() {
        return new Result(404, "请求不存在");
    }

    /**
     * 401页
     */
    @RequestMapping("/401")
    public Result error401() {
        return new Result(401, "未授权");
    }

    /**
     * 500页
     */
    @RequestMapping("/500")
    @ResponseBody
    public Result error500() {
        return new Result(500, "网络错误");
    }

}