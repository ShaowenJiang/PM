package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.dictionary.StudyForm;
import com.xuehu365.domain.*;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.*;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.Page;
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
 * Created by Administrator on 2017/6/16.
 */
@Controller
@RequestMapping("/course")
public class CourseController {

    @Autowired
    CourseService courseService;
    @Autowired
    CourseExtendService courseExtendService;
    @Autowired
    ProviderService providerService;
    @Autowired
    CategroyService categroyService;
    @Autowired
    TsourceStaffService tsourceStaffService;
    @Autowired
    StaffCourseService staffCourseService;

    @RequestMapping("/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result list(HttpServletRequest request, String type, Integer state, Integer category, String content, Integer pageNo, Integer pageSize) {
        PageRequest pr = new PageRequest();
        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        pr.getFilters().put("customerId", cusetomer_id);
        if (StringUtil.isNotBlank(type)) {
            String[] type_array = type.split("\\|");
            pr.getFilters().put("studyForm", Integer.parseInt(type_array[0]));
            if (type_array.length > 1) {
                pr.getFilters().put("studyFormSecond", Integer.parseInt(type_array[1]));
            }
        }
        pr.getFilters().put("state", state);
        if (category != null) {
            Map map = new HashMap<>();
            map.put("parentId", category);
            List<Integer> catagorys = new ArrayList<Integer>();
            for (TentCourseCategroy c : categroyService.findAll(map)) {
                catagorys.add(c.getCategroyId());
            }
            catagorys.add(category);
            pr.getFilters().put("catagorys", catagorys);
        }
        pr.getFilters().put("content", content);
        pr.setSortColumns("create_time DESC");
        pr.setPageSize(pageSize == null ? 20 : pageSize);
        pr.setPageNumber(pageNo == null ? 1 : pageNo);
        Page<TentCourse> page = courseService.findByPageRequest(pr);
        for (TentCourse c : page.getItems()) {
            List<String> speaker_names = new ArrayList<>();
            List<String> locations = new ArrayList<>();
            List<Tproviderinfo> speakers = providerService.getInfoByBingdings(courseExtendService.getCourseSpeakerIds(c.getCourseId()), c.getCustomerId());
            if (speakers != null) {
                for (Tproviderinfo p : speakers) {
                    speaker_names.add(speaker_names.size(), p.getPname());
                    locations.add(locations.size(), p.getLocation());
                }
                c.setSpeakerNames(speaker_names);
                c.setLocation(locations);
            }
        }
        return new Result(page);
    }


    /**
     * 课程详情
     *
     * @param courseId
     * @return
     */
    @RequestMapping("/info")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result info(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer courseId) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        TentCourse course = courseService.getById(courseId);
        course.setStudyFormName(staffCourseService.getStudyFormName(course.getStudyForm()));
//        if(course.getStudyFormSecond()!=null)
//        {
//            course.setStudyFormSecondName(staffCourseService.getStudyFormSecondName(course.getStudyForm(),course.getStudyFormSecond()));
//        }

        //TODO 获取课程的主讲
        StringBuffer speaker = new StringBuffer();
        List<Integer> speakers = courseExtendService.getCourseSpeakerIds(courseId);
        if (speakers != null) {
            for (Integer p : speakers) {
                speaker.append(",").append(p);
            }
            course.setSpeakers(speaker.toString().substring(1, speaker.toString().length()));
        }
        if (course.getSpeakers() != null) {
            List<String> spIds = Arrays.asList(course.getSpeakers().split(","));
            List<Tproviderinfo> speakList = providerService.getInfoByBinDingids(spIds, customerId);
            course.speakersList = speakList;
        }
        Map map = new HashMap<>();
        map.put("courseId", course.getCourseId());
        map.put("relateType", 1);
        List<TentCourseExtend> categroy_list = courseExtendService.findAll(map);
        if (categroy_list != null && categroy_list.size() > 0) course.setCategroyId(categroy_list.get(0).getRelateId());
        return new Result(course);
    }


    /**
     * 课程详情
     *
     * @param courseId
     * @return
     */
    @RequestMapping("/lesson_list")
    @ResponseBody
    public Result lesson_list(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer courseId) {
        return new Result(courseService.getLessonListByCourseId(courseId));
    }


    /**
     * 保存更新
     *
     * @param course
     * @return
     */
    @RequestMapping("/save")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result course_save(HttpServletRequest request, @Valid TentCourse course, Integer categroyId, String speakers) {
        boolean result = false;
        if (course.getCourseId() != null) {
            course.setId(course.getCourseId());
        } else {
            course.setState(0);
        }
        //TODO 20170810 修改 当0 无限制，1 确定时间
        if (course.getCourseDateType() != 1) {
            course.setStartTime(null);
            course.setEndTime(null);
        }
        //更新课程数据
        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        course.setCustomerId(cusetomer_id);
        course.setCreateId(UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId());

        return courseService.saveOrupdateCourse(course, categroyId, speakers) ? new Result(course) : new Result(false);
    }


    @RequestMapping("/delete")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result course_delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer courseId) {
        return new Result(courseService.deleteById(courseId) > 0);
    }


    /**
     * 学习形式
     *
     * @return
     */
    @RequestMapping("/study_form/list")
    @ResponseBody
    public Result study_form_list() {
        return new Result(new StudyForm[]{
//                new StudyForm(1, "在线学习", new StudyForm[]{new StudyForm(1, "直播课", null), new StudyForm(5, "轻课", null), new StudyForm(4, "微课", null), new StudyForm(2, "录播", null), new StudyForm(2, "在线混合", null)}),
//                new StudyForm(2, "面授学习", new StudyForm[]{new StudyForm(1, "小课", null), new StudyForm(2, "大课", null), new StudyForm(3, "内训课", null)}),
//                new StudyForm(4, "考察", new StudyForm[]{new StudyForm(1, "公开考察", null), new StudyForm(2, "定制考察", null)}),
//                new StudyForm(6, "体验式学习", new StudyForm[]{new StudyForm(5, "沙龙", null)}),

//                new StudyForm(1, "在线学习", new StudyForm[]{new StudyForm(1, "直播课", null), new StudyForm(4, "微课", null), new StudyForm(6, "其他", null)}),
//                new StudyForm(2, "面授学习",null),
//                new StudyForm(3, "混合式学习",null),
//                new StudyForm(6, "活动",null),
//                new StudyForm(7, "其他",null),


                new StudyForm(1, "直播课", null),
                new StudyForm(2, "面授+", null),
                new StudyForm(3, "混学课", null),
                new StudyForm(4, "轻学课", null),
                new StudyForm(5, "微课件", null),
                new StudyForm(6, "活动", null),
                new StudyForm(7, "其他", null),

        });
    }

    /**
     * 指派（针对管理员操作）
     *
     * @param request
     * @param courseId
     * @return
     */
    @RequestMapping("/assign")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result assign(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer courseId,
                         @NotNull(message = "不能为空(多个用，分隔)；") String userIds) {
        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(courseService.assign(courseId, cusetomer_id, userIds));
    }

    /**
     * 报名（针对学员）
     *
     * @param request
     * @param courseId
     * @return
     */
    @RequestMapping("/enroll")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result enroll(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer courseId) {


        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Map m = new HashMap<>();
        m.put("sourceId", courseId);
        m.put("customerId", cusetomer_id);
        m.put("userId", userId);
        m.put("sourceType", 0);
        if (tsourceStaffService.findAll(m).size() > 0) {
            return new Result(-1, "不允许重复报名");
        }
        return new Result(courseService.assign(courseId, cusetomer_id, userId.toString()));
    }

    /**
     * 发布课程
     *
     * @param courseId
     * @param state
     * @return
     */
    @RequestMapping("/publish")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result publiced(
            @NotNull(message = Message.PARAMER_CAN_NOT_BLANK)
                    Integer courseId, Integer
                    state) {
        TentCourse course = courseService.getById(courseId);
        course.setState(state == null ? 1 : state);
        return new Result(courseService.update(course) > 0 ? true : false);
    }

}
