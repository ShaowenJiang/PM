package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TdeptDuty;
import com.xuehu365.domain.Tduty;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CompanyStaffSrevice;
import com.xuehu365.service.DeptDutyService;
import com.xuehu365.service.DutyService;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.PageRequest;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by leven on 2017/7/19.
 */
@Controller
@RequestMapping("/duty")
public class DutyController {

    @Autowired
    DutyService dutyService;
    @Autowired
    CompanyStaffSrevice companyStaffSrevice;

    @RequestMapping("/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result list(HttpServletRequest request, Integer deptId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        map.put("customerId", customer_id);
        if (deptId != null && deptId > 0) {
            map.put("deptId", deptId);
        }
        map.put("_sort_", "createTime desc");
        return new Result(dutyService.findAll(map));
    }


    @RequestMapping("/save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result save(HttpServletRequest request, @Valid Tduty tduty) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        //先判断是否已经存在此职位
        HashMap map = new HashMap();
        map.put("dutyName", tduty.getDutyName());
        map.put("customerId", customer_id);

        if (tduty.getDutyId() == null) {
            tduty.setCreateTime(new Date());
        } else {
            tduty.setId(tduty.getDutyId());
            map.put("dutyId", tduty.getDutyId());
        }
        if (dutyService.checkDuty(map) > 0) {
            return new Result(-1, "已存在相同名称的职位");
        }

        if (tduty.getDeptId() == null) {
            tduty.setDeptId(0);
        }
        tduty.setCustomerId(customer_id);
        if (dutyService.saveOrUpdate(tduty) > 0) return new Result(tduty);

        return new Result(false);
    }

    /**
     * 检查当前职位是否存在学员
     *
     * @param request
     * @param dutyId
     * @return
     */
    @RequestMapping("/checkStaffCount")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result checkStaffCount(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer dutyId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.getFilters().put("entDutyId", dutyId);
        pr.getFilters().put("enterpriseId", customer_id);
        if (companyStaffSrevice.getCount(pr) > 0) {
            return new Result(Error.EXISTING_ASSOCIATED_DATA);
        }
        return new Result(true);
    }

    @RequestMapping("/del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result del(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer dutyId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.getFilters().put("entDutyId", dutyId);
        pr.getFilters().put("enterpriseId", customer_id);
        if (companyStaffSrevice.getCount(pr) > 0) {
            return new Result(Error.EXISTING_ASSOCIATED_DATA);
        }

        if (dutyService.deleteById(dutyId) > 0) return new Result(true);
        return new Result(false);
    }

    @ResponseBody
    @RequestMapping("/import")
    @AgentAuthCheck(authRequired = true)
    public Result dutyImport(MultipartHttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        MultipartFile file = request.getFile("files");
        List<String> failed = new ArrayList<>();
        ArrayList<String> arrayList = new ArrayList<>();
        try {

            InputStream in = file.getInputStream();
            Workbook rwb = Workbook.getWorkbook(in);
            Sheet sheet = rwb.getSheet(0);

            for (int i = 0; i < sheet.getRows(); i++) {//行
                Cell[] cell = sheet.getRow(i);
                if (i < 1 && cell[0].getContents().equals("职位")) {
                    continue;
                }
                String strErr = dutyService.checkImportResult(cell, i + 1, customer_id);
                if (!StringUtil.isBlank(strErr)) {
                    arrayList.add(strErr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false);
        }
        if (!failed.isEmpty()) {
            return new Result(false);
        }
        map.put("info", arrayList);
        return new Result(map);
    }
}
