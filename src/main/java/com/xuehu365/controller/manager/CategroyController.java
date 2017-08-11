package com.xuehu365.controller.manager;

import com.alibaba.fastjson.JSON;
import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TentCourseCategroy;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.model.UserInfoModel;
import com.xuehu365.service.CategroyService;
import com.xuehu365.service.CourseExtendService;
import com.xuehu365.util.CookieUtils;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Constants;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import javafx.scene.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/6/20.
 */
@Controller
public class CategroyController {


    @Autowired
    CategroyService categroyService;

    @Autowired
    CourseExtendService courseExtendService;

    /**
     * 课程分类
     *
     * @param request
     * @return
     */
    @RequestMapping("/categroy/tree")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result catagory_tree(HttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        List<TentCourseCategroy> list = categroyService.getCategroyTree(customer_id);
        return new Result(list);
    }

    @RequestMapping("/categroy/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result catagory_list(HttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        List<TentCourseCategroy> list = categroyService.getCategroyList(customer_id);
        return new Result(list);
    }


    /**
     * 保存更新
     *
     * @param categroy
     * @return
     */
    @RequestMapping("/categroy/save")

    @ResponseBody
    public Result category_save( @Valid TentCourseCategroy categroy, HttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        Map rq=new HashMap<>();
        rq.put("categroyName",categroy.getCategroyName());
        rq.put("parentId",categroy.getParentId());
        rq.put("customerId",customer_id);

        List<TentCourseCategroy> clist=categroyService.findAll(rq);
        if(clist.size()>0)
        {
            return new Result(-1,"同级分类类名不能相同");
        }

        if (categroy.getCategroyId() != null) categroy.setId(categroy.getCategroyId());
        categroy.setCustomerId(customer_id);
        categroy.setPriory(categroyService.getParentMaxPrioryValue(categroy.getParentId()));
        return categroyService.saveOrUpdate(categroy) > 0 ? new Result(categroy) : new Result(false);
    }

    /**
     * @param request
     * @param target
     * @param position
     * @param parent
     * @return
     */
    @RequestMapping("/categroy/exchange")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result category_exchange(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer target, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer position, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer parent) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        TentCourseCategroy c=categroyService.getById(target);
        if(c.getParentId()!=parent)
        {
            return new Result(-1,"不允许跨级拖动");
        }
        Integer rs = categroyService.category_exchange(customer_id, position, parent, target);
        return new Result(rs);
    }

    @RequestMapping("/categroy/exchange1")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result category_exchangeN(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer from, Integer to, Integer parent) {
        boolean result;
        Integer priory_temp, parent_temp;
        TentCourseCategroy categroy1 = categroyService.getById(from);
        priory_temp = categroy1.getPriory();
        if (to != null) {

            TentCourseCategroy categroy2 = categroyService.getById(to);
            if (categroy1.getParentId() == categroy2.getParentId()) {
                categroy1.setPriory(categroy2.getPriory());
                categroy2.setPriory(priory_temp);
            } else {
                categroy1.setParentId(categroy2.getParentId() == null ? categroy2.getCategroyId() : categroy2.getParentId());
                categroy1.setPriory(categroy2.getPriory());
            }
            result = categroyService.update(categroy1) > 0 && categroyService.update(categroy2) > 0;
        } else {
            categroy1.setParentId(null);
            result = categroyService.update(categroy1) > 0;
        }
        return new Result(result);
    }


    /**
     * 删除分类
     *
     * @param categroyId
     * @return
     */
    @RequestMapping("/categroy/delete")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result cateagory_delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categroyId) {
        Map map = new HashMap<>();
        map.put("parentId", categroyId);
        if (categroyService.findAll(map).size() > 0) return new Result(Error.EXISTING_ASSOCIATION_SUBJECT);
        map = new HashMap<>();
        map.put("relateType", 1);
        map.put("relateId", categroyId);

        if (courseExtendService.findAll(map).size() > 0) return new Result(-1,"已有课程使用该分类，无法删除");

        return new Result(categroyService.deleteById(categroyId) > 0);
    }
}
