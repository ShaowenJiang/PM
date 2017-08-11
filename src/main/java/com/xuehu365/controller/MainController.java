package com.xuehu365.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import com.qiniu.http.Response;
import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TcompanyStaff;
import com.xuehu365.domain.Tproviderinfo;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CityService;
import com.xuehu365.service.CompanyStaffSrevice;
import com.xuehu365.service.ProviderService;
import com.xuehu365.util.*;
import com.xuehu365.util.constants.Constants;
import com.xuehu365.util.constants.Error;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 公共视图控制器
 **/
@Controller
@RequestMapping("/common")
public class MainController {

    @Autowired
    CompanyStaffSrevice companyStaffSrevice;
    @Autowired
    ProviderService providerService;
    @Autowired
    CityService cityService;
    private QiniuUtil qiniuUtil = QiniuUtil.getInstance(QiniuUtil.Namespace.ADMIN);

    /**
     * 七牛上传
     *
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/upload")
    public Result upload(MultipartHttpServletRequest request) {
        List<MultipartFile> filelist = request.getFiles("files");
        List<String> failed = new ArrayList<>();
        try {
            for (MultipartFile file : filelist) {
                String folder = "PM/" + file.getContentType().split("/")[0] + "/";
                String key = qiniuUtil.buildKey(folder, file.getOriginalFilename());
                Response res = qiniuUtil.upload(file.getBytes(), key);
                if (!res.isOK()) {
                    failed.add(file.getOriginalFilename());
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false);
        }
        if (!failed.isEmpty()) {
            return new Result(false);
        }
        return new Result(true);
    }


    @ResponseBody
    @RequestMapping("uptoken")
    public Object uptoken() {
        HashMap<String, String> json = new HashMap<>();
        json.put("uptoken", qiniuUtil.getUpToken());
        json.put("code", "0");
        return json;
    }

    /**
     * 城市列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/city/list")
    public Result getCityList() {
        return new Result(cityService.getCityList());
    }


    /**
     * 下拉框
     *
     * @param request
     * @param content
     * @return
     */
    @RequestMapping("/usermodel/list")
    @AgentAuthCheck(authRequired = true)
    @ResponseBody
    public Result usermodel_list(HttpServletRequest request,
                                 String content,
                                 @RequestParam(value = "keys[]", required = false) String[] keys, Integer type) {
        if (type == null) {
            type = 1;
        }
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        List<UserModel> result = new ArrayList<>();

        if (type == 0) {
            //学员
            for (TcompanyStaff staff : companyStaffSrevice.getListByCustomerIdOrId(Arrays.asList(keys), content, customer_id)) {
                UserModel model = new UserModel();
                model.trafromByStaff(staff);
                result.add(model);
            }

        } else {
            //讲师
            List<Tproviderinfo> list = new ArrayList<>();
            if (keys != null) {
                List<Integer> key_int = new ArrayList<>();
                for (String k : keys) {
                    key_int.add(Integer.parseInt(k));
                }
                list = providerService.getInfoByBingdings(key_int, customer_id);
            } else {
                list = providerService.getProviderList(0, content, 1, 1, 20, customer_id, null).getItems();
            }
            if (list != null) {
                for (Tproviderinfo providerinfo : list) {
                    UserModel model = new UserModel();
                    model.trafromByProvider(providerinfo);
                    result.add(model);
                }
            }
        }
        return new Result(result);
    }

    /**
     * 发送短信消息
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/sendMessage")
    public Result sendMessage(@NotNull(message = "不能为空") String message, @NotNull(message = "不能为空") String mobile) {
        Integer count = ZJMessageUtils.getInstance().sendMessage(ZJMessageUtils.JIANZHOU_ACCOUNT_V,
                ZJMessageUtils.JIANZHOU_PASSWORD_V, mobile, message + ZJMessageUtils.JIANZHOU_SIGN_XUEHU);
        if (count <= 0) {
            return new Result(Error.SEND_MESSAGE_FAILD, mobile);
        }
        return new Result(true);
    }

    @RequestMapping(value = "/testAPP")
    @ResponseBody
    public Result content(HttpServletRequest request) {
        return new Result(Constants.ENVIRONMENT);
    }
}