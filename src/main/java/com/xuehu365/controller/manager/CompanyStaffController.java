package com.xuehu365.controller.manager;

import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.TcompanyStaff;
import com.xuehu365.domain.TcoursesinfoVideo;
import com.xuehu365.domain.Tuser;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.service.CompanyStaffSrevice;
import com.xuehu365.service.DeptService;
import com.xuehu365.service.SourceStaffService;
import com.xuehu365.service.UserService;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.Page;
import com.xuehu365.util.pagination.PageRequest;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.hibernate.validator.constraints.NotBlank;
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
import java.util.*;


/**
 * Created by Administrator on 2017/6/29.
 */
@Controller
public class CompanyStaffController {

    @Autowired
    CompanyStaffSrevice companyStaffSrevice;
    @Autowired
    UserService userService;
    @Autowired
    DeptService deptService;


    @RequestMapping("/staff/list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result list(HttpServletRequest request, Integer entDepartmentId, Integer entDutyId, Integer pageNo, Integer pageSize,String content) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.setPageNumber(pageNo);
        pr.setPageSize(pageSize);
        pr.getFilters().put("enterpriseId", customer_id);
        //pr.getFilters().put("entDepartmentId", entDepartmentId);
        pr.getFilters().put("entDutyId", entDutyId);
        pr.getFilters().put("content", content);
        pr.getFilters().put("status", 0);
        pr.setSortColumns("addDateTime desc");
        if(entDepartmentId!=null&&entDepartmentId>0) {
            ArrayList<Integer> deptIdsList = deptService.getAllDeptByParentId(customer_id, entDepartmentId);
            pr.getFilters().put("deptIds", deptIdsList);
        }

        Page<TcompanyStaff> companyStaff= companyStaffSrevice.findByPageRequest(pr);
        companyStaffSrevice.getAllDeptStr(companyStaff,customer_id);
        return new Result(companyStaff);
    }

    /**
     * 保存学员信息
     *
     * @param request
     * @param staff
     * @param password
     * @return
     */
    @RequestMapping("/staff/save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result save(HttpServletRequest request, @Valid TcompanyStaff staff, String password) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        staff.setEnterpriseId(customer_id);
        //判断当前手机号是否已经存在学员中
        HashMap map = new HashMap();
        map.put("mobile", staff.getMobile());
        map.put("enterpriseId", customer_id);
        if (staff.getEntStafferId() != null && !staff.getEntStafferId().isEmpty()) {
            map.put("entStafferId", staff.getEntStafferId());
        } else {
            staff.setAddDateTime(new Date());
        }
        if (companyStaffSrevice.checkMobile(map) > 0) {
            return new Result(-1, "学员库中已存在相同的手机号");
        }
        Tuser user = userService.savesimple(staff.getUserId(), staff.getMobile(), staff.getEmail(), staff
                .getName(), password);
        if (user != null) {
            if (staff.getEntStafferId() == null) {
                staff.setEntStafferId(UUID.randomUUID().toString());
            } else {
                staff.setId(-1);
            }
            staff.setUserId(user.getUserId());
            if (staff.getStatus() == null) {
                staff.setStatus(0);
            }
            staff.setPersonalId(user.getPersonalId());
            int count = companyStaffSrevice.saveOrUpdate(staff);
            return count > 0 ? new Result(staff) : new Result(false);
        }
        return new Result(false);
    }

    @ResponseBody
    @RequestMapping("/staff/import")
    @AgentAuthCheck(authRequired = true)
    public Result staffimport(MultipartHttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap map = new HashMap();
        MultipartFile file = request.getFile("file");
        if(!StringUtil.isExcelFile(file.getOriginalFilename())) {
            return new Result(-1, "文件格式不正确，请上传“xls”格式文件");
        }
        List<String> failed = new ArrayList<>();
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            InputStream in = file.getInputStream();
            Workbook rwb = Workbook.getWorkbook(in);
            Sheet sheet = rwb.getSheet(0);

            for (int i = 0; i < sheet.getRows(); i++) {//行
                Cell[] cell = sheet.getRow(i);
                if (i < 2 && (cell[0].getContents().equals("姓名") || cell[0].getContents().equals("张三"))) {
                    continue;
                }
                String strErr = companyStaffSrevice.checkResult(cell, i + 1, customer_id);
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


    /***
     * 获取学员详细信息
     */
    @ResponseBody
    @RequestMapping("/staff/info")
    @AgentAuthCheck(authRequired = true)
    public Result info(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String entStafferId) {
        TcompanyStaff staff = companyStaffSrevice.getById(entStafferId);
        if (staff != null) return new Result(staff);
        return new Result(false);
    }

    /***
     * 删除学员-删除学员的学习记录、报名课程
     */
    @ResponseBody
    @RequestMapping("/staff/del")
    @AgentAuthCheck(authRequired = true)
    public Result del(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) String entStafferId,@NotNull(message = "不能为空") Integer userId) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        if (companyStaffSrevice.delStaff(customer_id, entStafferId, userId) > 0) return new Result(true);
        return new Result(false);
    }

    @ResponseBody
    @RequestMapping("/staff/counts")
    @AgentAuthCheck(authRequired = true)
    public Result counts(HttpServletRequest request) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        HashMap<String, Object> hashMapmap = new HashMap<String, Object>();
        PageRequest pr = new PageRequest();
        pr.getFilters().put("enterpriseId", customer_id);
        pr.getFilters().put("status", 0);
        Long staffCount = companyStaffSrevice.getCount(pr);
        hashMapmap.put("staffCount", staffCount);//学员统计
        return new Result(hashMapmap);
    }
}
