package com.xuehu365.controller.manager;

import com.alibaba.fastjson.JSON;
import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.Tcommunity;
import com.xuehu365.domain.TcommunityMembers;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CommunityMembersService;
import com.xuehu365.service.CommunityService;
import com.xuehu365.util.AppReturnResult;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.WebUtil;
import com.xuehu365.util.constants.Constants;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.Page;
import com.xuehu365.util.pagination.PageRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class CommunityController {

    private Logger log = LoggerFactory.getLogger(CommunityController.class);
    @Autowired
    CommunityService communityService;
    @Autowired
    CommunityMembersService communityMembersService;

    /**
     * 社群列表（搜索：状态，id，关键字）
     *
     * @param request
     * @param state
     * @param content
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/community/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result list(HttpServletRequest request, Integer state, String content, Integer pageNo, Integer pageSize) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.getFilters().put("customerId", customer_id);
        pr.getFilters().put("status", state);
        pr.getFilters().put("content", content);
        pr.setPageSize(pageSize);
        pr.setPageNumber(pageNo);
        pr.setSortColumns(" addDate desc");
        Page<Tcommunity> page = communityService.findByPageRequest(pr);
        return new Result(page);
    }

    /**
     * 编辑社群
     *
     * @param id
     * @return
     */
    @RequestMapping("/community/info")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result info(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(communityService.getById(id));
    }

    /**
     * 保存（新增、修改）
     *
     * @param request
     * @param community
     * @return
     */
    @RequestMapping("/community/save")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result save(HttpServletRequest request, @Valid Tcommunity community) {
        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        if (community.getId() == null) {
            //新增社群时，设置群主
//            community.setOwnerId(user_id);//默认群主
            community.setTotalCount(1);//默认群主已经在群里面了
            community.setStatus(2);//默认为隐藏
            community.setIsOpen(1);
            community.setIsNeedApproval(0);
        }
        Tcommunity result = communityService.createORUpdate(community, cusetomer_id);
        return result == null ? new Result(false) : new Result(result);
    }


    /**
     * 删除社群
     *
     * @param id
     * @return
     */
    @RequestMapping("/community/delete")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(communityService.deleteById(id));
    }

    /**
     * 社群成员列表
     *
     * @param communityId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/community/member/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result member_list(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer communityId, String content, Integer pageNo, Integer pageSize) {
        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        log.info(cusetomer_id + "");
        PageRequest pr = new PageRequest();
        pr.setPageNumber(pageNo);
        pr.setPageSize(pageSize);
        pr.getFilters().put("content", content);
        pr.getFilters().put("customerId", cusetomer_id);
        pr.getFilters().put("communityId", communityId);
        pr.setSortColumns("role_type ASC,id DESC");
        return new Result(communityService.getMemberPage(pr));
    }

    /**
     * 踢出社群成员
     *
     * @param id
     * @return
     */
    @RequestMapping("/community/member/kick")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result kick(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(communityService.kick(id));
    }


    /**
     * 添加社群成员
     *
     * @param communityId
     * @return
     */
    @RequestMapping("/community/member/add")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result addMembers(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer communityId) {
        Pattern p = Pattern.compile("^1[34578]\\d{9}$");
        List<String> mobiles = new ArrayList<>();
        List<String> failds = new ArrayList<>();
        String[] items = request.getParameterValues("items");
        if (items == null) return new Result(-1, "获取的items值为空");
        for (String s : items) {
            if (p.matcher(s).matches()) {
                mobiles.add(s);
            } else {
                failds.add(s);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("communityId", communityId);
        data.put("mobiles", StringUtils.join(mobiles, ","));
        String response = WebUtil.sendPost(
                String.format("http://%sapp.xuehu365.com/intranet/community/batch/join", Constants.ENVIRONMENT),
                data
        );
        AppReturnResult result = JSON.parseObject(response, AppReturnResult.class);
        if ("0".equals(result.getResult())) {
            return new Result(true);
        }
        if (!failds.isEmpty()) {
            return new Result(false);
        }
        return new Result(true);
    }

    @RequestMapping("/community/member/assigned")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result already_add(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer communityId) {
        ArrayList<Integer> list = new ArrayList();
        Map map = new HashMap<>();
        map.put("communityId", communityId);
        for (TcommunityMembers m : communityMembersService.findAll(map)) {
            list.add(m.getUserId());
        }
        return new Result(list);
    }


    @ResponseBody
    @RequestMapping("/community/member/change_role")
    @AgentAuthCheck(authRequired = true)
    public Result change_role(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer communityId,
                              @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer userId,
                              @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer roleType) {
        return communityService.assignRoleToUser(communityId, userId, roleType) > 0 ? new Result(true) : new Result(false);
    }
}
