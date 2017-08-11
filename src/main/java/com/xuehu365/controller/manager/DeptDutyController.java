package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TdeptDuty;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.DeptDutyService;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by leven on 2017/6/30.
 */
@Controller
@RequestMapping("/deptduty")
public class DeptDutyController {

    @Autowired
    DeptDutyService deptDutyService;

    @RequestMapping("/list")
    @ResponseBody
    public Result list(HttpServletRequest request, Integer deptId, Integer type) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(deptDutyService.getList(deptId != null ? deptId : 0, customer_id, type));
    }


    @RequestMapping("/save")
    @ResponseBody
    public Result save(HttpServletRequest request, @Valid TdeptDuty deptDuty, Integer customerId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        if (deptDuty.getId() == null) {
            deptDuty.setCreateTime(new Date());
            deptDuty.setCompanyId(customer_id);
        }
        if (deptDutyService.saveOrUpdate(deptDuty) > 0) return new Result(deptDuty);
        return new Result(false);
    }

    @RequestMapping("/del")
    @ResponseBody
    public Result del(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        if (deptDutyService.deleteById(id) > 0) return new Result(true);
        return new Result(false);
    }

    @RequestMapping("/listForTree")
    @ResponseBody
    public Result listForTree(HttpServletRequest request, Integer deptId, Integer type) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(deptDutyService.getListForTree(deptId != null ? deptId : 0, customer_id, type));
    }
}
