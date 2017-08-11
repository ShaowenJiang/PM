package com.xuehu365.controller;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TcustomerXuea;
import com.xuehu365.domain.TentDict;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.*;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * <p>
 * 注意：
 *
 * @NotNull 和 @NotEmpty  和@NotBlank 区别
 * @NotEmpty 用在集合类上面
 * @NotBlank 用在String上面
 * @NotNull 用在基本类型上
 **/
@Controller
public class IndexController {

    private Logger log = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private CourseService courseService;
    @Autowired
    private CommunityService communityService;
    @Autowired
    private RoomTopicService roomTopicService;
    @Autowired
    private CustomerXueaService customerXueaService;
    @Autowired
    private EntDictService entDictService;

    /**
     * 首页
     *
     * @return
     */
    @RequestMapping(value = "/index")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result index(HttpServletRequest request) {
        String scope = UserLoginInfoUtil.getCurrentUserFromCookie(request).getScope();
        Map<String, Object> map = courseService.getCustomerIndexCourse(scope);
        Map<String, Object> communityModelList = communityService.getIndexCommunityModelList(scope);
        Map<String, Object> topicModelList = roomTopicService.getIndexTopicModelList(scope);
        map.putAll(communityModelList);
        map.putAll(topicModelList);
        return new Result(map);

    }

    @RequestMapping(value = "/index/banners")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result banners(HttpServletRequest request) {
        String scope = UserLoginInfoUtil.getCurrentUserFromCookie(request).getScope();
        HashMap map = new HashMap();
        map.put("scope", scope);
        List<TcustomerXuea> list = customerXueaService.findAll(map);
        if (list.size() > 0) {
            Integer customerId = list.get(0).getCustomerId();
            map = new HashMap();
            map.put("customerId", customerId);
            map.put("_sort_", " priority desc ");
            map.put("code", Constants.COMPANY_INDEX_BANNER);
            return new Result(entDictService.findAll(map));
        }
        return new Result(false);
    }

    @RequestMapping(value = "/index/content")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result content(HttpServletRequest request) {
        String scope = UserLoginInfoUtil.getCurrentUserFromCookie(request).getScope();
        HashMap map = new HashMap();
        map.put("scope", scope);
        List<TcustomerXuea> list = customerXueaService.findAll(map);
        if (list.size() > 0) {
            Integer customerId = list.get(0).getCustomerId();
            return new Result(entDictService.getIndexNavAndContent(customerId));
        }
        return new Result(false);
    }

}



