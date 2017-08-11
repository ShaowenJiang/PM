package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TroomTopic;
import com.xuehu365.domain.TroomTopicGuest;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.*;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Created by Administrator on 2017/6/7.
 */
@Controller
public class RoomTopicController {

    @Autowired
    RoomTopicService roomTopicService;
    @Autowired
    ProviderService providerService;
    @Autowired
    RoomChatLogService roomChatLogService;
    @Autowired
    RoomQuestLogService roomQuestLogService;
    @Autowired
    RoomLiveLogService roomLiveLogService;
    @Autowired
    RoomTopicGuestService roomTopicGuestService;

    /**
     * 直播间列表
     *
     * @param request
     * @param state
     * @param content
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/roomtopic/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result list(HttpServletRequest request, Integer state, String content, Integer pageNo, Integer pageSize,Integer isShow) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.setPageNumber(pageNo == null ? 1 : pageNo);
        pr.setPageSize(pageSize == null ? 10 : pageSize);
        pr.getFilters().put("customerId", customer_id);
        pr.getFilters().put("state", state);
        pr.getFilters().put("content", content);
        pr.getFilters().put("isShow", isShow);
        pr.setSortColumns("id desc");
        return new Result(roomTopicService.findByPageRequest(pr));
    }

    @RequestMapping("/roomtopic/info")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result info(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(roomTopicService.getById(id));
    }

    /**
     * 保存、更新直播间
     *
     * @param roomTopic
     * @return
     */
    @RequestMapping("/roomtopic/save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result save(HttpServletRequest request, @Valid TroomTopic roomTopic) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        roomTopic.setCustomerId(customer_id);
        if (roomTopicService.saveOrUpdate(roomTopic) > 0) return new Result(roomTopic);
        return new Result(false);
    }

    /**
     * 删除嘉宾
     *
     * @param topicId
     * @return
     */
    @RequestMapping("/roomtopic/delete")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer topicId) {
        if (roomTopicService.deleteById(topicId) > 0) return new Result(true);
        return new Result(false);
    }

    /**
     * 嘉宾列表
     *
     * @param topicId
     * @return
     */
    @RequestMapping("/roomtopic/guest/my_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result member_list(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer topicId) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(roomTopicService.getRoomTopicGuest(topicId, customerId));
    }

    /**
     * 嘉宾信息
     *
     * @param id
     * @return
     */
    @RequestMapping("/roomtopic/guest/info")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result member_info(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(roomTopicService.getRoomTopicGuestInfo(id, customerId));
    }

    /**
     * 保存嘉宾
     *
     * @param model
     * @return
     */
    @RequestMapping("/roomtopic/guest/save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result member_add(@Valid TroomTopicGuest model) {
        model.setUserPictrue(model.getHeadUrl());
        TroomTopicGuest guest = roomTopicService.saveRoomGuest(model);
        return guest == null ? new Result(false) : new Result(guest);
    }

    /**
     * 删除嘉宾
     *
     * @param id
     * @return
     */
    @RequestMapping("/roomtopic/guest/delete")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result member_delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(roomTopicService.deleteGuest(id));
    }

    /**
     * 聊天室聊天内容列表
     *
     * @param topicId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/roomtopic/chat/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result chat_list(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer topicId, Integer pageNo, Integer pageSize, String name) {
        PageRequest pr = new PageRequest();
        pr.getFilters().put("topicId", topicId);
        pr.setSortColumns("sendTime desc");
        pr.setPageNumber(pageNo);
        pr.setPageSize(pageSize);
        if (!StringUtil.isEmpty(name)) {
            pr.getFilters().put("userName", name);
        }
        return new Result(roomChatLogService.findByPageRequest(pr));
    }

    /**
     * 直播间问题列表
     *
     * @param topicId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/roomtopic/quest/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result quest_list(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer topicId, Integer pageNo, Integer pageSize, String name) {
        PageRequest pr = new PageRequest();
        pr.setPageSize(pageSize);
        pr.setPageNumber(pageNo);
        pr.setSortColumns("sendTime desc");
        pr.getFilters().put("topicId", topicId);
        if (!StringUtil.isEmpty(name)) {
            pr.getFilters().put("userName", name);
        }
        return new Result(roomQuestLogService.findByPageRequest(pr));
    }

    /**
     * 直播间消息
     *
     * @param topicId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/roomtopic/live/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result live_list(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer topicId, Integer pageNo, Integer pageSize, String name) {
        PageRequest pr = new PageRequest();
        pr.setPageSize(pageSize);
        pr.setPageNumber(pageNo);
        pr.setSortColumns("sendTime desc");
        pr.getFilters().put("topicId", topicId);
        if (!StringUtil.isEmpty(name)) {
            pr.getFilters().put("userName", name);
        }
        return new Result(roomLiveLogService.findByPageRequest(pr));
    }

    @RequestMapping("/roomtopic/live/delete")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete_live_batch(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(roomLiveLogService.deleteById(id));
    }

    @RequestMapping("/roomtopic/live/deleteLiveBatch")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete_live_batch(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String ids) {
        Set set = new HashSet(Arrays.asList(ids.split(",")));
        for (Iterator it = set.iterator(); it.hasNext(); ) {
            roomLiveLogService.deleteById(Integer.parseInt(it.next().toString()));
        }
        return new Result(true);
    }


    @RequestMapping("/roomtopic/quest/delete")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete_quest(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(roomQuestLogService.deleteById(id));
    }

    @RequestMapping("/roomtopic/quest/deleteQuestBatch")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete_quest_batch(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String ids) {
        Set set = new HashSet(Arrays.asList(ids.split(",")));

        for (Iterator it = set.iterator(); it.hasNext(); ) {
            roomQuestLogService.deleteById(Integer.parseInt(it.next().toString()));
        }
        return new Result(true);
    }

    @RequestMapping("/roomtopic/chat/delete")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete_chat(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(roomChatLogService.deleteById(id));
    }

    @RequestMapping("/roomtopic/chat/deleteChatBatch")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result delete_chat_batch(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String ids) {
        Set set = new HashSet(Arrays.asList(ids.split(",")));
        for (Iterator it = set.iterator(); it.hasNext(); ) {
            roomChatLogService.deleteById(Integer.parseInt(it.next().toString()));
        }
        return new Result(true);
    }


    @RequestMapping("/roomtopic/updateState")
    @ResponseBody
    public Result updateState(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer topicId, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer state) {
        //判断直播间是否指定了嘉宾
        Map m = new HashMap<>();
        m.put("topicId", topicId);
        if (roomTopicGuestService.findAll(m).size() == 0) {
            return new Result(Error.UNSPECIFIED_GUEST);
        }
        return new Result(roomTopicService.updateState(topicId, state));
    }
}
