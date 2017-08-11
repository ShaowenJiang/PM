package com.xuehu365.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSON;
import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.Tpersonal;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.model.*;
import com.xuehu365.service.ThemeService;
import com.xuehu365.service.UserService;
import com.xuehu365.util.*;
import com.xuehu365.util.constants.Constants;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.security.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    ThemeService themeService;
    private Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * 用户登录
     *
     * @return
     */
    @RequestMapping(value = "/login")
    @ResponseBody
    public Result login(@NotNull(message = "不能为空") String scope, @NotNull(message = "不能为空") String username, @NotNull(message = "不能为空") String password, HttpServletResponse response) throws InvalidKeySpecException, NoSuchAlgorithmException {
        List<UserInfoModel> list =new ArrayList<>();
        Error error=userService.authentication(username, MD5Util.MD5(password), scope,list);
        if (error!=null)return  new Result(error);
        if (list != null && !list.isEmpty()) {
            List<UserCookieModel> userCookieModels = userService.transUserCookieInfoFromUserInfoModel(list);
            String cookieToSave = UserLoginInfoUtil.getUserInfoEncript(userCookieModels);
            CookieUtils.addCookie(response, Constants.COOKIE_INFO_PM, cookieToSave, null, 18000);
            //TODO 提供个WAP直播间免登陆的cookie
            XueHuUserCookieModel xuehuCookie = new XueHuUserCookieModel();
            xuehuCookie.setUserId(userCookieModels.get(0).getUserId());
            xuehuCookie.setUserGuid(UUID.randomUUID().toString());
            String xuehucookieToSave = JSON.toJSONString(xuehuCookie);
            CookieUtils.addCookie(response, Constants.COOKIE_INFO_XUEHU, xuehucookieToSave, null, 18000);
            return new Result(list);
        }
        return new Result(Error.UNKNOW_EXCEPTION);
    }

    /**
     * 用户登出
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.getCookie(request, response, Constants.COOKIE_INFO_PM);
        CookieUtils.getCookie(request, response, Constants.COOKIE_INFO_XUEHU);
        return new Result(true);
    }

    /**
     * 用户切换
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/user_change")
    @ResponseBody
    public Result logout(@NotNull(message = "不能为空") String userId, HttpServletRequest request, HttpServletResponse response) {
        List<UserCookieModel> list = UserLoginInfoUtil.getUserInfoFromCookiesOrHeader(request);
        if (list != null) {
            list = userService.changeUserRole(list, Integer.parseInt(userId));
            String cookieToSave = UserLoginInfoUtil.getUserInfoEncript(list);
            CookieUtils.addCookie(response, Constants.COOKIE_INFO_PM, cookieToSave, null, 18000);
            return new Result(true);
        }
        return new Result(Error.USERNAME_OR_PASSWORD_ERROR);
    }


    /**
     * 获取用户信息
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/user_info/ent")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result user_Info_ent(HttpServletRequest request) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        return new Result(userService.getEntUserInfo(userId,false));

    }

    @RequestMapping(value = "/user_info/personal")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result user_Info_personal(HttpServletRequest request) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        PersonalUserModel p= userService.getPersonalUserInfo(userId,false);
        if(p.getIsEnterprise()==1)
        {
            EntUserModel ent=  userService.getEntUserInfo(userId,false);
            p.setMobile(ent.getLinkMobile());
        }
        return new Result(p);

    }


    /**
     * 获取用户信息
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/update/EntUser")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result update_entUser(HttpServletRequest request, EntUserModel user) {
        Integer roleType = UserLoginInfoUtil.getCurrentUserFromCookie(request).getRoleType();
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(userService.updateEntInfo(user, customerId));

    }

    /**
     * 获取用户信息
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/update/personal")
    @ResponseBody
    public Result update_personal(HttpServletRequest request, PersonalUserModel user) {
        return new Result(userService.updatePersonalUserInfo(user));
    }


    /**
     * 修改密码
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/user_updatePsw")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result user_updatePsw(HttpServletRequest request, @NotNull(message = "不能为空") String oldPsw, @NotNull(message = "不能为空") String psw1, @NotNull(message = "不能为空") String psw2) {
        oldPsw = MD5Util.MD5(oldPsw);
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        if (!psw1.equals(psw2)) {
            return new Result(-1, "两次密码不一致");
        } else {
            //交易原密码是否正确
            if (userService.checkOldPsw(userId, oldPsw) == 0) {
                return new Result(-1, "原密码不正确");
            }
        }

        return new Result(userService.updateMobileOrPsw(null, MD5Util.MD5(psw1), userId));

    }


    /**
     * 修改手机号——获取短信验证码
     */
    @RequestMapping(value = "/getMsgCode")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result getMsgCode(HttpServletRequest request, @NotNull(message = "不能为空") String mobile, HttpSession session) {
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        if (mobile == null || mobile.trim().isEmpty()) {
            //mobile = userService.getPersonalUserInfo(userId).getMobile();

            PersonalUserModel p= userService.getPersonalUserInfo(userId,false);
            if(p.getIsEnterprise()==1)
            {
                EntUserModel ent=  userService.getEntUserInfo(userId,false);
                p.setMobile(ent.getLinkMobile());
            }
            mobile=p.getMobile();
        }


        if (mobile.trim().isEmpty()) {
            return new Result(-1, "手机号不能为空");
        }
        String ip = IpUtils.getIpAddr(request);
        if (session.getAttribute(ip) != null) {
            Date date = (Date) session.getAttribute(ip);
            long time = new Date().getTime() - date.getTime();
            if (time < 1000 * 60) {
                return new Result(-2, "1分钟内只允许发送一次哦");
            }
        }
        if (session.getAttribute(mobile) != null) {
            Date date = (Date) session.getAttribute(mobile);
            long time = new Date().getTime() - date.getTime();
            if (time < 1000 * 60) {
                return new Result(-3, "1分钟内只允许发送一次哦");
            }
        }

        session.setAttribute(ip, new Date());
        session.setMaxInactiveInterval(60);
        session.setAttribute(mobile, new Date());
        session.setMaxInactiveInterval(60);

        HashMap hs = new HashMap();
        Random random = new Random();
        String msgCode = "";
        for (int i = 0; i < 6; i++) {
            msgCode = msgCode + random.nextInt(10);
        }
        session.setAttribute("msgCode" + mobile, msgCode);
        session.setMaxInactiveInterval(60 * 10);

        String message = "验证码：" + msgCode + "（学乎网手机验证码，请完成验证），如非本人操作，请忽略此短信。";
        Integer aaa = ZJMessageUtils.getInstance().sendMessage(ZJMessageUtils.JIANZHOU_ACCOUNT_V,
                ZJMessageUtils.JIANZHOU_PASSWORD_V, mobile, message + ZJMessageUtils.JIANZHOU_SIGN_XUEHU);
        if (aaa <= 0) {
            new Result(-12, "短信发送失败!");
        }

        hs.put("ip", ip);
        hs.put("res", aaa);
        return new Result(hs);
    }


    /**
     * 开放平台申请试用
     */
    @RequestMapping(value = "/checkMsgCode")
    @ResponseBody
    public Result openTryCustomer_save(HttpSession session, String msgCode, String mobile) {

        if (session.getAttribute("msgCode" + mobile) == null || !session.getAttribute("msgCode" + mobile).equals(msgCode)) {
            return new Result(-3, "短信验证码不正确");
        }
        session.removeAttribute("msgCode"+mobile);
        return new Result(0);

    }


    /**
     * 更改手机号——获取图片验证码
     */
    @RequestMapping(value = "/getImgCode")
    @ResponseBody
    public Result getImgCode(HttpServletResponse response, HttpSession session) throws IOException {
        // 设置响应的类型格式为图片格式
        response.setContentType("image/jpeg");
        //禁止图像缓存。
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        ImgCodeUtil imgCode = new ImgCodeUtil(100, 30, 4, 10);
        session.setAttribute("imgCode" + imgCode.getCode(), imgCode.getCode());
        imgCode.write(response.getOutputStream());
        return new Result(imgCode);
    }


    /**
     * 更改绑定的手机号
     */
    @RequestMapping(value = "/updateMobile")
    @ResponseBody
    public Result updateMobile(HttpServletRequest request, String mobile, String msgCode, String imgCode, HttpSession session) {

        if (session.getAttribute("msgCode" + mobile) == null) {
            return new Result(-3, "短信验证码不正确");
        } else {
            session.removeAttribute("msgCode"+mobile);
        }

//        if (session.getAttribute("imgCode" + imgCode) == null || !session.getAttribute("imgCode" + imgCode).equals(imgCode)) {
//            return new Result(-2, "图片验证码不正确");
//        } else {
//            session.removeAttribute("imgCode"+imgCode);
//        }


        //直接更新t_user 用户手机号码
        Integer userId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getUserId();
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        Integer roleType = UserLoginInfoUtil.getCurrentUserFromCookie(request).getRoleType();

        //判断是否存在管理员身份
        PersonalUserModel pInfo = userService.getPersonalUserInfo(userId,false);

        if (pInfo.getIsEnterprise() == 1) {
            userService.updateCustomerLinkmanXuea(mobile, customerId);
        } else {
            //判断要新手机号是否和当前要修改的手机号一致
            if(pInfo.getMobile().equals(mobile.trim()))
            {
                return new Result(-1, "新手机号和当前绑定的手机号码不能一致");
            }

            //判断当前手机号是否存在用户
            UserInfoModel userInfoModel = userService.selectInfoByMoblie(mobile);
            if (userInfoModel != null) {
                return new Result(-1, "手机号码已占用");
            }
            userService.updateMobileOrPsw(mobile, null, userId);
        }

        return new Result("0");

    }


}
