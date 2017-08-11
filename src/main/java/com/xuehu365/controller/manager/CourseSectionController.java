package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TentCourseSection;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CourseLessonService;
import com.xuehu365.service.CourseSectionService;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/section")
public class CourseSectionController {
    @Autowired
    CourseSectionService courseSectionService;
    @Autowired
    CourseLessonService courseLessonService;

    @RequestMapping("/tree")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result section_list(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer courseId) {
        return new Result(courseSectionService.getSectionListByCourseId(courseId));
    }


    @RequestMapping("/info")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result section_info(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer sectionId) {
        return new Result(courseSectionService.getById(sectionId));
    }


    @RequestMapping("/save")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result section_save(@Valid TentCourseSection section) {
        if (section.getSectionId() != null) {
            section.setId(section.getSectionId());
        }
        return new Result(courseSectionService.saveOrUpdate(section) > 0 ? section : false);
    }

    @RequestMapping("/delete")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result section_delete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer sectionId) {
        Map map = new HashMap<>();
        map.put("parentId", sectionId);
        if (courseSectionService.findAll(map).size() > 0) return new Result(Error.EXISTING_ASSOCIATION_SUBJECT);
        map = new HashMap<>();
        map.put("sectionId", sectionId);
        if (courseLessonService.findAll(map).size() > 0) return new Result(Error.EXISTING_ASSOCIATION_SUBJECT);
        return new Result(courseSectionService.deleteById(sectionId) > 0 ? true : false);
    }
}
