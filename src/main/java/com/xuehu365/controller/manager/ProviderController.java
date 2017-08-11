package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.Tpersonal;
import com.xuehu365.domain.Tproviderinfo;
import com.xuehu365.domain.Tuser;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.model.UserInfoModel;
import com.xuehu365.service.CourseService;
import com.xuehu365.service.ProviderService;
import com.xuehu365.service.UserService;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.Page;
import com.xuehu365.util.security.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;

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
@RequestMapping("/provider")
public class ProviderController {

    private Logger log = LoggerFactory.getLogger(ProviderController.class);
    @Autowired
    private ProviderService providerService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private UserService userService;


    @RequestMapping(value = "/info")
    @ResponseBody
    public Result getProviderInfo(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer providerId) {

        //获取供应商信息
        Tproviderinfo info = providerService.getById(providerId);
        //获取讲师课程列表
        //List<CourseInfoModel> list=courseService.getTeacherCourseList(providerId);
        //HashMap rs = new HashMap();
        //rs.put("list",list);
        //服务客户
//        rs.put("serviceCustomer", info.getServiceCustomer());
//        //主要课程
//        rs.put("cooperationCourses", info.getCooperationCourses());
//        //专业背景
//        rs.put("professionalBackground", info.getProfessionalBackground());
//        //实战经验
//        rs.put("actualExperience", info.getActualExperience());
//        //授课风格
//        rs.put("teachingStyle", info.getTeachingStyle());
//        //讲师风采
//        rs.put("pictureUrls", info.getPictureUrls());
//        //讲师姓名
//        rs.put("pname", info.getPname());
//        //讲师头像
//        rs.put("imgUrl", info.getImgUrl());
//        //讲师介绍
//        rs.put("introduce", info.getIntroduce());
//        //常驻地
//        rs.put("cityName", info.getCityName());


        return new Result(info);
    }

    @RequestMapping(value = "/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result getProviderList(HttpServletRequest request, Integer status, String content, Integer pType, Integer pageNo, Integer pageSize, Integer customerId) {

        if (pType == null) {
            pType = 1;
        }


        customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        Page<Tproviderinfo> list = providerService.getProviderList(status, content, pType, pageNo, pageSize, customerId,null);
        return new Result(list);
    }

    @RequestMapping(value = "/teacher_course")
    @ResponseBody
    public Result getTeacherCourseList(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer providerId) {
        HashMap rs = new HashMap();
        rs.put("list", courseService.getTeacherCourseList(providerId));
        return new Result(rs);
    }

    @RequestMapping(value = "/save")
    @ResponseBody
    public Result provider_save(HttpServletRequest request, @Valid Tproviderinfo providerinfo) {

        Integer userId = 0;
        Integer customer_id = 0;

        if (providerinfo.getPtype() == null) {
            providerinfo.setPtype(1);
        }
        if (providerinfo.getCustomerId() != null) {
            customer_id = providerinfo.getCustomerId();
        } else {
            customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        }

        providerinfo.setCustomerId(customer_id);
        if (providerinfo.getProviderId() == null)//新增
        {
            providerinfo.setAddDateTime(new Date());
            providerinfo.setStatus(0);
            //先检查手机号对应的个人用户是否存在
            UserInfoModel model = userService.selectInfoByMoblie(providerinfo.getMobile());
            if (model == null) {

                //创建user表用户
            /*创建一个user对象存储t_user表需要的信息*/
                Tuser user = new Tuser();
                user.setPassword(MD5Util.MD5("123456"));
                user.setIsEnterprise(0);
                user.setUserSourceType(7);
                user.setTel(providerinfo.getMobile());
                user.setAddDatetime(new Date());
                user.setAccount("");

                //创建T_personal表
                Tpersonal personl = new Tpersonal();
                personl.setName(providerinfo.getMobile());
                personl.setMobile(providerinfo.getMobile());
                personl.setPicture(providerinfo.getImgUrl());
                personl.setGender(providerinfo.getGender());
                personl.setEmail(providerinfo.getEmail());
                userId = userService.createUser(user, personl);

                if (userId > 0) {

                    providerinfo.setBinDing(userId);
                }
            } else//用户存在校验是否该企业下的讲师已经绑定了该用户
            {
                providerinfo.setBinDing(model.getUserId());
                List<Tproviderinfo> infos = providerService.getInfoByBingdings(Arrays.asList(new Integer[]{model.getUserId()}), customer_id);
                if (infos != null && infos.size() > 0) {
                    return new Result(Error.HAS_THE_SAME_PROVIDER_BY_MOBILE);
                }
            }
        } else {

            providerinfo.setId(providerinfo.getProviderId());
            providerinfo.setModifyDateTime(new Date());

        }

        providerService.saveOrUpdate(providerinfo);
        return new Result(providerinfo);
    }

    @RequestMapping(value = "/del")
    @ResponseBody
    public Result provider_del( @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer providerId) {
        return new Result(providerService.deleteById(providerId));
    }
}




