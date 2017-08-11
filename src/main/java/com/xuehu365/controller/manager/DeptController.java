package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.Tdept;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CompanyStaffSrevice;
import com.xuehu365.service.DeptService;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
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
@RequestMapping("/dept")
public class DeptController {

    @Autowired
    DeptService deptService;
    @Autowired
    CompanyStaffSrevice companyStaffSrevice;

    @RequestMapping("/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result list(HttpServletRequest request, Integer parentDeptId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        map.put("customerId", customer_id);
        if (parentDeptId != null && parentDeptId >= 0) {
            map.put("parentDeptId", parentDeptId);
        }
        map.put("_sort_","createTime desc");
        return new Result(deptService.findAll(map));
    }


    @RequestMapping("/save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result save(HttpServletRequest request, @Valid Tdept tdept) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        //判断是否已经存在此部门
        HashMap map = new HashMap();
        map.put("deptName", tdept.getDeptName());
        map.put("parentDeptId", tdept.getParentDeptId());
        map.put("customerId", customer_id);

        if (tdept.getDeptId() == null) {
            tdept.setCreateTime(new Date());
        } else {
            tdept.setId(tdept.getDeptId());
            map.put("deptId", tdept.getDeptId());
        }
        if (deptService.checkDept(map) > 0) {
            return new Result(-1, "已存在相同名称的部门");
        }

        if (tdept.getParentDeptId() == null) {
            tdept.setParentDeptId(0);
        }
        if(tdept.children==null ){

        }

        tdept.setCustomerId(customer_id);
        if (deptService.saveOrUpdate(tdept) > 0) return new Result(tdept);
        return new Result(false);
    }

    @RequestMapping("/del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result del(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer deptId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        //判断是否存在子部门
        HashMap map = new HashMap();
        map.put("parentDeptId", deptId);
        if(deptService.getCount(map)>0){
            return new Result(-1,"请先删除该部门下的子部门");
        }
        //判断是否已存在学员
        PageRequest pr = new PageRequest();
        pr.getFilters().put("entDepartmentId", deptId);
        pr.getFilters().put("enterpriseId", customer_id);
        if(companyStaffSrevice.getCount(pr)>0){
            return new Result(-2,"该部门下已有学员，无法删除");
        }

        if (deptService.deleteById(deptId) > 0) return new Result(true);
        return new Result(false);
    }


    @RequestMapping("/listfortree")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result listForTree(HttpServletRequest request, Integer parentDeptId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        return new Result(deptService.getListForTree(parentDeptId != null ? parentDeptId : 0, customer_id));
    }

    /**
     * 导入部门
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/import")
    @AgentAuthCheck(authRequired = true)
    public Result deptImport(MultipartHttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        MultipartFile file = request.getFile("file");
        List<String> failed = new ArrayList<>();
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            InputStream in = file.getInputStream();
            Workbook rwb = Workbook.getWorkbook(in);
            Sheet sheet = rwb.getSheet(0);

            for (int i = 0; i < sheet.getRows(); i++) {//行
                Cell[] cell = sheet.getRow(i);
                if (i < 1 && cell[0].getContents().equals("部门")) {
                    continue;
                }
                String strErr = deptService.checkImportResult(cell, i + 1, customer_id);
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
