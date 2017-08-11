package com.xuehu365.interceptor;

import com.xuehu365.model.UserCookieModel;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.exception.PMException;
import com.xuehu365.util.security.MACTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthInterceptor implements HandlerInterceptor {
    Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    public static final String XUEHUAPP_SIGN_SECRET = "abc";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        if (handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            AgentAuthCheck authPassport = ((HandlerMethod) handler).getMethodAnnotation(AgentAuthCheck.class);
            //没有声明需要权限,或者声明不验证权限
            if (authPassport == null) {
                return true;
            } else {
                //在这里实现自己的权限验证逻辑
                if (authPassport.authRequired()) {
                        /*来自网页端*/
                    if (!webLoginAgenAuthCheck(request))
                        throw new PMException(Error.UN_AUTHORIZATION);

                }
            }
        }
        return true;
    }
    public boolean webLoginAgenAuthCheck(HttpServletRequest request) {
        UserCookieModel model = UserLoginInfoUtil.getCurrentUserFromCookie(request);
        if (model == null) {
            throw new PMException(Error.UN_AUTHORIZATION);
        }
        String userId = request.getParameter("LOGINID");
        if (StringUtil.isBlank(userId)) {
            userId = request.getHeader("LOGINID");
        }
        String scope = request.getParameter("SCOPE");
        if (StringUtil.isBlank(scope)) {
            scope = request.getHeader("SCOPE");
        }
        if (userId == null || scope == null) {
            return false;
        }
        if (!userId.equals(model.getUserId().toString()) || !model.getScope().equals(scope)) {
            return false;
        }
        return true;

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }

}