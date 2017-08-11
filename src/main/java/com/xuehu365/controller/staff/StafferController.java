package com.xuehu365.controller.staff;

import com.sun.org.apache.regexp.internal.RE;
import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.*;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.model.CourseModel;
import com.xuehu365.model.SourceStaffModel;
import com.xuehu365.service.*;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.util.*;

/**
 * Created by Administrator on 2017/7/7.
 */
@Controller
@RequestMapping("/staffer")
public class StafferController {
    @Autowired
    StaffCourseService staffCourseService;
    @Autowired
    TsourceStaffService tsourceStaffService;
    @Autowired
    SourceStaffService sourceStaffService;
    @Autowired
    CourseService courseService;
    @Autowired
    CourseExtendService courseExtendService;
    @Autowired
    ProviderService providerService;
    @Autowired
    CategroyService categroyService;
    @Autowired
    CourseLessonService courseLessonService;
    @Autowired
    CourseSectionService courseSectionService;
    @Autowired
    CoursesinfoDatumService coursesinfoDatumService;


    @RequestMapping("/index")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result index(HttpServletRequest request) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        //查询课程数量
        Map map = new HashMap<>();
        //查询课程数量
        map.put("course_count", tsourceStaffService.selectCountByUserId(userId, 0, customerId));
        //查询视频数量
        map.put("video_count", tsourceStaffService.selectCountByUserId(userId, 1, customerId));
        //查询文章数量
        map.put("essay_count", tsourceStaffService.selectCountByUserId(userId, 10, customerId));
        //查询文档数量
        map.put("document_count", tsourceStaffService.selectCountByUserId(userId, 11, customerId));
        //查询作业
        map.put("work_count", tsourceStaffService.selectCountByUserId(userId, 9, customerId));
        //查询调研数量
        map.put("survey_count", tsourceStaffService.selectCountByUserId(userId, 7, customerId));
        //查询考试数量
        map.put("exam_count", tsourceStaffService.selectCountByUserId(userId, 8, customerId));
        map.put("room_count", tsourceStaffService.getRoomsCountByUserId(userId, null));
        map.put("community_count", tsourceStaffService.getCommunityCountByUserId(userId, null));
        return new Result(map);
    }

    @RequestMapping("/source/count")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result count(HttpServletRequest request) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        //查询课程数量
        Map map = new HashMap<>();
        //查询课程数量
        map.put("course_count", tsourceStaffService.selectCountByUserId(userId, 0, customerId));
        //查询视频数量
        map.put("video_count", tsourceStaffService.selectCountByUserId(userId, 1, customerId));
        //查询文章数量
        map.put("essay_count", tsourceStaffService.selectCountByUserId(userId, 10, customerId));
        //查询文档数量
        map.put("document_count", tsourceStaffService.selectCountByUserId(userId, 11, customerId));
        //查询作业
        map.put("work_count", tsourceStaffService.selectCountByUserId(userId, 9, customerId));
        //查询调研数量
        map.put("survey_count", tsourceStaffService.selectCountByUserId(userId, 7, customerId));
        //查询考试数量
        map.put("exam_count", tsourceStaffService.selectCountByUserId(userId, 8, customerId));
        map.put("room_count", tsourceStaffService.getRoomsCountByUserId(userId, customerId));
        map.put("community_count", tsourceStaffService.getCommunityCountByUserId(userId, customerId));
        return new Result(map);
    }

    @RequestMapping("/mycommunity")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result myCommunity(HttpServletRequest request, Integer pageSize, Integer pageNo) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(tsourceStaffService.getCommunityByUserId(userId, pageNo, pageSize, customerId));
    }

    @RequestMapping("/myrooms")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result myRooms(HttpServletRequest request, Integer pageSize, Integer pageNo) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(tsourceStaffService.getRoomListByUserId(userId, pageNo, pageSize, customerId));
    }


    /**
     * 课程列表
     *
     * @param request
     * @return
     */
    @RequestMapping("/course/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_course_list(HttpServletRequest request) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(staffCourseService.getEntCourseList(customerId, userId));
    }

    /**
     * 企业课程详情页
     *
     * @param request
     * @return
     */
    @RequestMapping("/course/detail_page")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_course_detail_page(HttpServletRequest request, Integer beginsId, Integer courseId) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        return new Result(staffCourseService.getCourseDetailModelList(courseId, userId));
    }

    /**
     * 提供给APP调用
     *
     * @param request
     * @return
     */
    @RequestMapping("/source/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_source_list(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer propertyType) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        //课程资源学乎
        List<SourceStaffModel> company_source_list = new ArrayList<>();
        switch (propertyType) {
            case 1://视频
                company_source_list = sourceStaffService.selectVideoByUserId(userId, oldToNewType(propertyType), customerId);
                break;
            case 10://文章
                company_source_list = sourceStaffService.selectEssayByUserId(userId, oldToNewType(propertyType), customerId);
                break;
            default://3-11 文档 9-8考试，7-9 作业 8-7调研
                company_source_list = sourceStaffService.selectDatumLisByUserId(userId, oldToNewType(propertyType), customerId);
        }
        return new Result(company_source_list);

    }

    private int oldToNewType(int old) {
        switch (old) {
            case 1:
                return 1;
            case 2:
                return 10;
            case 3:
                return 11;
            case 4:
                return 6;
            case 5:
                return 6;
            case 7:
                return 9;
            case 8:
                return 7;
            case 9:
                return 8;
            case 10:
                return 12;
            case 11:
                return 5;
            case 12:
                return 13;
        }
        return 6;
    }


    /**
     * 提供给pm调用
     *
     * @param request
     * @return
     */
    @RequestMapping("/source/mylist/old")
    @ResponseBody
    public Result staff_source_mylist(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer propertyType, Integer pageSize, Integer pageNo) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        List<SourceStaffModel> company_source_list = new ArrayList<>();
        switch (propertyType) {
            case 0:
                //课程资源学乎
                List<CourseModel> courseList = staffCourseService.getEntCourseList(customerId, userId);
                return new Result(courseList);
            case 1:
                company_source_list = sourceStaffService.selectVideoByUserId(userId, propertyType, customerId);
                break;
            case 10:
                company_source_list = sourceStaffService.selectEssayByUserId(userId, propertyType, customerId);
                break;
            default:
                company_source_list = sourceStaffService.selectDatumLisByUserId(userId, propertyType, customerId);
        }

        return new Result(company_source_list);

    }


    /**
     * 提供给pm调用
     *
     * @param request
     * @return
     */
    @RequestMapping("/source/mylist")
    @ResponseBody
    public Result staff_source_mylist_page(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType, Integer pageSize, Integer pageNo) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(sourceStaffService.selectSourceListPage(userId, categoryType, pageSize, pageNo, customerId));

    }


    /**
     * 提供给pm调用
     *
     * @param request
     * @return
     */
    @RequestMapping("/source/mylist/course")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result staff_source_mylist_course(HttpServletRequest request, Integer pageSize, Integer pageNo) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(staffCourseService.getEntCourseListPage(customerId, userId, pageSize, pageNo));
        //return new Result(staffCourseService.getEntCourseList(customerId, userId));
    }

    /**
     * 提供给app调用
     *
     * @param id
     * @return
     */
    @RequestMapping("/source/finish")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result study_finish(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(tsourceStaffService.finish(id));
    }

    @RequestMapping("/source_save")
    @ResponseBody
    public Result staff_save(HttpServletRequest request, @Valid TsourceStaff sourceStaff) {
        //由我指派功能先校验是否存在
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        String[] users = request.getParameterValues("users");
        //过滤已经指派的数据
        for (String userId : users) {
            boolean hasApply = tsourceStaffService.checkSourceStaff(sourceStaff.getSourceId(), sourceStaff.getSourceType(), Integer.parseInt(userId), customerId);
            if (!hasApply) {
                sourceStaff.setCustomerId(customerId);
                sourceStaff.setUserId(Integer.parseInt(userId));
                sourceStaff.setCompleteState(0);
                if (sourceStaff.getStudyType() == 0)//随到随学
                {
                    sourceStaff.setStudyBeginTime(null);
                    sourceStaff.setStudyEndTime(null);
                }

                sourceStaff.setId(null);
                tsourceStaffService.saveOrUpdate(sourceStaff);

            }
        }
        return new Result(true);
    }

    @RequestMapping("/source_cancel")
    @ResponseBody
    public Result staff_cancelStudy(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(tsourceStaffService.deleteById(id));
    }

    @RequestMapping("/assigned")
    @ResponseBody
    public Result getAssignedIds(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer sourceId, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer sourceType) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(tsourceStaffService.getAssignedIds(sourceId, sourceType, customerId));
    }
}
