package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TentCourseLesson;
import com.xuehu365.domain.TsourceStaff;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CourseLessonService;
import com.xuehu365.service.CourseService;
import com.xuehu365.service.TsourceStaffService;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Created by Administrator on 2017/6/21.
 */
@Controller
@RequestMapping("/lesson")
public class CourseLessonController {

    @Autowired
    CourseLessonService courseLessonService;
    @Autowired
    TsourceStaffService tsourceStaffService;
    @Autowired
    CourseService courseService;

    /**
     * 课时列表
     *
     * @return
     */
    @RequestMapping("/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result lesson_list(Integer sectionId, Integer courseId) {
        Map map = new HashMap<>();
        map.put("sectionId", sectionId);
        map.put("courseId", courseId);
        List<TentCourseLesson> list = courseLessonService.findAll(map);
        return new Result(list);
    }

    /**
     * 保存课时
     *
     * @param lesson
     * @return
     */
    @RequestMapping("/save")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result lesson_save(HttpServletRequest request, @Valid TentCourseLesson lesson) {
        Integer cusetomer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        boolean is_update = false;
        if (lesson.getLessonId() != null) {
            lesson.setId(lesson.getLessonId());
            is_update = true;
        }
        Integer result = courseLessonService.saveOrUpdate(lesson);
        if (is_update && result > 0) {
            //更新t_source_staff
            List<TsourceStaff> source_staffs = tsourceStaffService.getAllSameLessonId(lesson.getLessonId());
            for (TsourceStaff t : source_staffs) {
                t.setSourceType(lesson.getLessonType());
                tsourceStaffService.update(t);
            }
        }
        //TODO 更新t_source_staffer
        if (!is_update && result > 0) {
            Integer courseId = lesson.getCourseId();
            List<Integer> list;
            if ((list = getSourceStaffUserByCourseId(courseId)).size() > 0) {
                for (Integer user_id : list) {
                    TsourceStaff tsourceStaff = new TsourceStaff();
                    tsourceStaff.setUserId(user_id);
                    tsourceStaff.setSourceType(2);
                    tsourceStaff.setCompleteState(0);
                    tsourceStaff.setSourceId(lesson.getLessonId());
                    tsourceStaff.setApplyTime(new Date());
                    tsourceStaff.setApplyType(2);
                    tsourceStaff.setSourceType(lesson.getLessonType());
                    tsourceStaff.setCustomerId(cusetomer_id);
                    tsourceStaffService.save(tsourceStaff);
                }
            }
        }
        return new Result(result > 0 ? lesson : false);
    }


    /**
     * 获取某一门课程下报名的学员
     * @param courseId
     * @return
     */
    private List<Integer> getSourceStaffUserByCourseId(Integer courseId) {
        List<Integer> list = new ArrayList<>();
        Map map = new HashMap();
        map.put("sourceId", courseId);
        map.put("sourceType", 0);
        for (TsourceStaff s : tsourceStaffService.findAll(map)) {
            list.add(s.getUserId());
        }
        return list;
    }

    /**
     * 删除课时
     *
     * @param lessonId
     * @return
     */
    @RequestMapping("/delete")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result lesson_delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer lessonId) {
        return new Result(courseLessonService.deleteById(lessonId) > 0 ? true : false);
    }

}
