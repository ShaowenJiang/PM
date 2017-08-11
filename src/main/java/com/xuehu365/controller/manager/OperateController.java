package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TentDict;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.EntDictService;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Constants;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by leven on 2017/7/21.
 */
@Controller
@RequestMapping("/operate")
public class OperateController {

    @Autowired
    EntDictService entDictService;

    @RequestMapping("/banner_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result banner_list(HttpServletRequest request, Integer status) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        map.put("customerId", customerId);
        if (status != null && status > 0) {
            map.put("status", status);
        }
        map.put("code", Constants.COMPANY_INDEX_BANNER);
        map.put("_sort_", " priority desc");
        return new Result(entDictService.findAll(map));
    }

    @RequestMapping("/banner_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result banner_save(HttpServletRequest request, @NotBlank(message = Message.PLEASE_ENTER_TITLE) @Length(max = 32, message = Message.PLEASE_ENTER_THE_SECOND_CORSS) String key,
                              @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String value, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String comment, Integer status, Integer id) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        TentDict tentDict = new TentDict();
        tentDict.setStatus(status == null ? 1 : status);
        tentDict.setKey(key);
        tentDict.setValue(value);
        tentDict.setComment(comment);
        tentDict.setCustomerId(customerId);
        tentDict.setCode(Constants.COMPANY_INDEX_BANNER);
        tentDict.setDescription("企业首页轮播图");
        if (id != null && id > 0) {
            tentDict.setId(id);
        }
        if (id == null) {
            //获取当前企业的banner图排序
            HashMap map = new HashMap();
            map.put("customerId", customerId);
            map.put("code", Constants.COMPANY_INDEX_BANNER);
            Long maxPriority = entDictService.getMaxPriority(map);
            if (maxPriority == null) {
                maxPriority = 0l;
            }
            tentDict.setPriority(maxPriority.intValue() + 1);
        }
        if (entDictService.saveOrUpdate(tentDict) > 0) return new Result(tentDict);
        return new Result(false);
    }

    @RequestMapping("/banner_del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result banner_del(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        if (entDictService.deleteById(id) > 0) return new Result(true);
        return new Result(false);
    }

    /**
     * 轮播图排序
     *
     * @param request
     * @param from
     * @param to
     * @return
     */
    @RequestMapping("/banner_swap")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result banner_swap(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer from, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer to) {
        return new Result(entDictService.bannerSwap(from, to));
    }


    /**
     * 首页导航列表
     *
     * @param request
     * @param status
     * @return
     */
    @RequestMapping("/nav_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result nav_list(HttpServletRequest request, Integer status) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        map.put("customerId", customerId);
        if (status != null && status > 0) {
            map.put("status", status);
        }
        map.put("code", Constants.COMPANY_INDEX_NAV);
        return new Result(entDictService.findAll(map));
    }

    /**
     * 首页导航编辑
     *
     * @param request
     * @param key
     * @param status
     * @param id
     * @return
     */
    @RequestMapping("/nav_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result nav_save(HttpServletRequest request, @NotBlank(message = Message.PLEASE_ENTER_TITLE) String key,
                           Integer status, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        TentDict tentDict = new TentDict();
        if (id != null && id > 0) {
            tentDict.setId(id);
            tentDict = entDictService.getById(id);
        }
        tentDict.setStatus(status == null ? 1 : status);
        tentDict.setKey(key);
        tentDict.setCustomerId(customerId);
        tentDict.setCode(Constants.COMPANY_INDEX_NAV);
        tentDict.setDescription("企业首页导航");
        if (entDictService.update(tentDict) > 0) return new Result(tentDict);
        return new Result(false);
    }

//    @RequestMapping("/nav_del")
//    @ResponseBody
//    @AgentAuthCheck(authRequired = true)
//    public Result nav_del(HttpServletRequest request,@NotNull(message = "id不能为空") Integer id) {
//        if (entDictService.deleteById(id) > 0) return new Result(true);
//        return new Result(false);
//    }

    /**
     * 内容列表
     *
     * @param request
     * @param id      导航id作为父级id
     * @return
     */
    @RequestMapping("/content_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result content_list(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        map.put("customerId", customerId);
        map.put("key", id);
        List<TentDict> list = entDictService.findAll(map);
        ArrayList arrayList = new ArrayList();
        for (TentDict tentDict : list
                ) {
            arrayList.add(tentDict.getValue());
        }
        return new Result(arrayList);
    }

    /**
     * 添加导航内容
     *
     * @param request
     * @param id      导航id作为父级id
     * @param value   course、room、community 的区分
     * @param ids     id的集合
     * @return
     */
    @RequestMapping("/content_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result content_save(HttpServletRequest request, @NotBlank(message = Message.PARAMER_CAN_NOT_BLANK) String id, String value, String[] ids) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        TentDict dict = new TentDict();
        dict.setComment(value);
        dict.setKey(id);
        dict.setCustomerId(customerId);
        dict.setCode(Constants.COMPANY_INDEX_NAV_CONTENT);
        dict.setDescription("企业首页导航内容");
        dict.setStatus(1);
        //String[] arrIds = ids.split(",");
        //删除原数据
        HashMap map = new HashMap();
        map.put("customerId", customerId);
        map.put("key", id);
        entDictService.deleteNavContent(map);


        Integer res = 0;
        for (int i = 0; i < ids.length; i++) {
            dict.setId(null);
            dict.setValue(ids[i]);
            dict.setPriority(i);
            res += entDictService.save(dict);
        }
        if (res <= 0) {
            return new Result(false);
        } else if (res == ids.length) {
            return new Result(true);
        } else {
            return new Result(Error.PARTAIL_DATA_STORAGE_FAILS);
        }
    }
}
