package com.xuehu365.controller.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.IntegerCodec;
import com.xuehu365.controller.base.Result;
import com.xuehu365.domain.*;
import com.xuehu365.interceptor.AgentAuthCheck;
import com.xuehu365.model.SourceStaffModel;
import com.xuehu365.service.*;
import com.xuehu365.util.DateUtil;
import com.xuehu365.util.QiniuUtil;
import com.xuehu365.util.StringUtil;
import com.xuehu365.util.UserLoginInfoUtil;
import com.xuehu365.util.constants.Error;
import com.xuehu365.util.constants.Message;
import com.xuehu365.util.pagination.Page;
import com.xuehu365.util.pagination.PageRequest;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leven on 2017/6/27.
 */
@Controller
@RequestMapping("/source")
public class SourceController {

    @Autowired
    CoursesinfoVideoService coursesinfoVideoService;
    @Autowired
    CoursesinfoDatumService coursesinfoDatumService;
    @Autowired
    CoursesinfoEssayService coursesinfoEssayService;
    @Autowired
    TcoursesinfoCategroyService ccoursesinfoCategroyService;
    @Autowired
    TsourceStaffService tsourceStaffService;
    @Autowired
    ProviderService providerService;
    @Autowired
    DeptService deptService;
    @Autowired
    DutyService dutyService;
    @Autowired
    CompanyStaffSrevice companyStaffSrevice;


    @RequestMapping("/video_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result video_list(HttpServletRequest request, Integer customerId, Integer categoryPid, String title, Integer pageNo, Integer pageSize) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.setPageNumber(pageNo);
        pr.setPageSize(pageSize);
        pr.getFilters().put("customerId", customer_id);
        if (categoryPid != null) {
            pr.getFilters().put("categoryPid", categoryPid);
        }
        if (StringUtil.isNotBlank(title)) {
            pr.getFilters().put("title", title);
        }
        pr.getFilters().put("status", 0);
        pr.setSortColumns("VideoID desc");
        return new Result(coursesinfoVideoService.findByPageRequest(pr));
    }

    @RequestMapping("/video_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result video_save(HttpServletRequest request, @Valid TcoursesinfoVideo video) {
        try {
            Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
            video.setCustomerId(customer_id);
            boolean newVideo = false;
            if (video.getVideoId() != null) {
                //修改的操作
                video.setId(video.getVideoId());

                TcoursesinfoVideo oldvideo = coursesinfoVideoService.getById(video.getVideoId());
                //如果旧的视频链接和新的视频链接不一致，则认为它已经重新上传了新的视频
                if (!oldvideo.getVideoVid().equals(video.getVideoVid()) && video.getSourceType().equals(4)) {
                    newVideo = true;
                    new QiniuUtil().delete(oldvideo.getVideoKey());
                }
            } else {
                newVideo = true;
                video.setAddDateTime(new Date());
            }
            //如果是新的视频文件，获取新的视频信息
            if (newVideo && video.getSourceType().equals(4)) {
                String obj = QiniuUtil.videofo(video.getVideoVid());
                Map map = JSON.parseObject(obj, Map.class);
                Map map2 = JSON.parseObject(map.get("format").toString());
                if (map2.get("size") != null) {
                    //将字节数转换成单位M进行存储
                    Integer size = Integer.parseInt(map2.get("size").toString());
                    Double d1 = (double) size / (1024 * 1024);
                    DecimalFormat df = new DecimalFormat("#.00");
                    String videoSize = df.format(d1);
                    video.setVideoSize(videoSize + "MB");
                }
                if (map2.get("duration") != null) {
                    Float fsec = Float.parseFloat(map2.get("duration").toString());
                    Integer sec = fsec.intValue();
                    Integer hour = sec / 3600;
                    String strHour = StringUtil.intPadString(hour, 2);
                    Integer minute = sec / 60 % 60;
                    String strMinute = StringUtil.intPadString(minute, 2);
                    Integer second = sec % 60;
                    String strSecond = StringUtil.intPadString(second, 2);
                    String strTime = strHour + ":" + strMinute + ":" + strSecond;
                    video.setVideoTime(strTime);
                }
            }

            if (coursesinfoVideoService.saveOrUpdate(video) > 0) return new Result(video);
            return new Result(false);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Result(false);
        }
    }

    @RequestMapping("/video_del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result video_del(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer videoId) {
        try {

            TcoursesinfoVideo video = coursesinfoVideoService.getById(videoId);
            video.setId(video.getVideoId());
            video.setStatus(1);
            if (coursesinfoVideoService.saveOrUpdate(video) > 0) {
                if (video.getSourceType().equals(4)) {
                    new QiniuUtil().delete(video.getVideoKey());
                }
                return new Result(true);
            }
            return new Result(false);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Result(false);
        }
    }

    @RequestMapping("/video_info")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result video_info(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer videoId) {

        TcoursesinfoVideo video = coursesinfoVideoService.getById(videoId);
        if (video != null) return new Result(video);
        return new Result(false);
    }


    @RequestMapping("/essay_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result essay_list(HttpServletRequest request, Integer customerId, Integer categoryPid, String title, Integer pageNo, Integer pageSize) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        pr.setPageNumber(pageNo);
        pr.setPageSize(pageSize);
        pr.getFilters().put("customerId", customer_id);
        if (categoryPid != null) {
            pr.getFilters().put("categoryPid", categoryPid);
        }
        if (StringUtil.isNotBlank(title)) {
            pr.getFilters().put("title", title);
        }
        pr.getFilters().put("status", 0);
        pr.setSortColumns("EssayID desc");
        return new Result(coursesinfoEssayService.findByPageRequest(pr));
    }

    @RequestMapping("/essay_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result essay_save(HttpServletRequest request, @Valid TcoursesinfoEssay essay) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        essay.setCustomerId(customer_id);
        if (essay.getEssayId() != null) {
            //修改的操作
            essay.setId(essay.getEssayId());
        } else {
            essay.setAddDateTime(new Date());
            essay.setLinkCount(0);
        }

        if (coursesinfoEssayService.saveOrUpdate(essay) > 0) return new Result(essay);
        return new Result(false);

    }

    @RequestMapping("/essay_del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result essay_del(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer essayId) {
        TcoursesinfoEssay essay = new TcoursesinfoEssay();
        essay.setStatus(1);
        essay.setEssayId(essayId);
        if (coursesinfoEssayService.update(essay) > 0) return new Result(true);
        return new Result(false);
    }

    @RequestMapping("/essay_info")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result essay_info(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer essayId) {
        TcoursesinfoEssay essay = coursesinfoEssayService.getById(essayId);
        if (essay != null) return new Result(essay);
        return new Result(false);
    }


    @RequestMapping("/datum_list")
    @ResponseBody
    public Result datum_list(Integer customer_id, HttpServletRequest request, Integer categoryPid, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType, String title, Integer pageNo, Integer pageSize) {
        if (customer_id == null) {
            customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        }
        return new Result(coursesinfoDatumService.getDatumList(customer_id, coursesinfoDatumService.toDatumType(categoryType), categoryPid, title, pageNo, pageSize));
    }

    @RequestMapping("/datum_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result datum_save(HttpServletRequest request, @Valid TcoursesinfoDatum datum, Integer categoryType) {
        Integer customer_id = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        datum.setCustomer_Id(customer_id);
        if (datum.getDatumId() != null) {
            datum.setId(datum.getDatumId());
            datum.setModifyDateTime(new Date());
        }
        datum.setDatumType(coursesinfoDatumService.toDatumType(categoryType));
        coursesinfoDatumService.saveOrUpdate(datum);
        return new Result(datum);
    }

    @RequestMapping("/datum_del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result datum_del(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer datumId) {
        return new Result(coursesinfoDatumService.deleteById(datumId));
    }

    @RequestMapping("/datum_info")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result datum_info(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer datumId) {
        return new Result(coursesinfoDatumService.getById(datumId));
    }


    @RequestMapping("/categroy_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result categroy_list(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType, Integer clazz, String name, Integer customerId) {

        if (customerId == null) {
            customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        }

        return new Result(ccoursesinfoCategroyService.getCategoryList(ccoursesinfoCategroyService.toCategoryType(categoryType), customerId, clazz, name));
    }


    @RequestMapping("/categroy_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result categroy_save(HttpServletRequest request, @Valid TcoursesinfoCategroy categroy) {

        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        Integer pid = categroy.getPid();
        String cName = categroy.getCategroyName();
        Map rq = new HashMap<>();
        rq.put("pid", categroy.getPid());
        rq.put("categroyName", categroy.getCategroyName());
        rq.put("customer_Id", customerId);
        List<TcoursesinfoCategroy> cList = ccoursesinfoCategroyService.findAll(rq);
        if (cList.size() > 0) {
            return new Result(Error.ON_THE_SAME_LEVEL_CAN_NOT_SAMENAME);
        }


        categroy.setCustomer_Id(customerId);
        categroy.setId(categroy.getCategroyId());
        if (categroy.getPid() == null) {
            categroy.setPid(0);
        }
        if (categroy.getCategroyId() == null) {
            //查询当前分类最大的id
            Integer maxid = Integer.parseInt(String.valueOf(ccoursesinfoCategroyService.selectMaxCategoryId(categroy.getPid(), ccoursesinfoCategroyService.toCategoryType(categroy.getCategroyType()), customerId)));

            categroy.setCategroysort(maxid + 1);

        }

        categroy.setCategroyType(ccoursesinfoCategroyService.toCategoryType(categroy.getCategroyType()));
        ccoursesinfoCategroyService.saveOrUpdate(categroy);
        return new Result(categroy);
    }

    @RequestMapping("/categroy_del")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result categroy_del(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryId) {
        TcoursesinfoCategroy info = ccoursesinfoCategroyService.getById(categoryId);
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        boolean candel = true;
        HashMap map = new HashMap();
        map.put("customer_Id", customerId);
        if (info.getPid() == 0) {
            map.put("categoryPid", categoryId);
        } else {
            map.put("categoryId", categoryId);
        }

        if (info.getCategroyType() == 1) {
            if (coursesinfoVideoService.findAll(map).size() > 0) {
                candel = false;
            }
        } else if (info.getCategroyType() == 2) {
            if (coursesinfoEssayService.findAll(map).size() > 0) {
                candel = false;
            }
        } else {

            if (coursesinfoDatumService.findAll(map).size() > 0) {
                candel = false;
            }
        }
        if (!candel) {
            return new Result(Error.EXISTING_ASSOCIATION_SUBJECT);
        }

        Integer rs = ccoursesinfoCategroyService.deleteById(categoryId);

        if (rs > 0) {
            return new Result(true);
        } else {
            return new Result(false);
        }
        //return new Result(ccoursesinfoCategroyService.deleteById(categoryId));
    }

    @RequestMapping("/staff_list")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_list(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id, Integer pageNo, Integer pageSize, String name, Integer complete, String content) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        PageRequest pr = new PageRequest();
        if (!StringUtil.isBlank(content)) {
            pr.getFilters().put("content", content);
        }
        pr.getFilters().put("sourceType", categoryType);
        pr.getFilters().put("sourceId", id);
        pr.getFilters().put("complete", complete);
        pr.getFilters().put("customerId", customerId);
        pr.setPageNumber(pageNo);
        pr.setPageSize(pageSize);
        pr.setSortColumns("applyTime desc");
        Page<TsourceStaff> list = tsourceStaffService.findByPageRequest(pr);
        if (list.getPageSize() > 0) {
            //读取当前企业用户的所有部门
            HashMap map = new HashMap();
            map.put("customerId", customerId);
            List<Tdept> deptList = deptService.findAll(map);
            for (TsourceStaff item : list.getItems()) {
                //获取职位以及部门
                Tduty duty = dutyService.getById(item.getDutyId());
                if (duty != null) {
                    item.setDutyName(duty.getDutyName());
                }
                String deptName = companyStaffSrevice.getDeptStr(item.getDeptId(), deptList);

                if (deptName != null && deptName.length() > 0) {
                    deptName = deptName.substring(0, deptName.length() - 1);
                    item.setDeptName(deptName);
                }
            }
        }

        return new Result(list);
    }


    @RequestMapping("/staff_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_save(HttpServletRequest request, @Valid TsourceStaff sourceStaff) {
        sourceStaff.setCompleteState(0);
        if (sourceStaff.getStudyType() == 0)//随到随学
        {
            sourceStaff.setStudyBeginTime(null);
            sourceStaff.setStudyEndTime(null);
        }
        return new Result(tsourceStaffService.saveOrUpdate(sourceStaff));
    }

    @RequestMapping("/staff_complete")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_complete(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(tsourceStaffService.completeStudy(id));
    }

    @RequestMapping("/staff_cancel")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_cancelStudy(@NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id) {
        return new Result(tsourceStaffService.deleteById(id));
    }


    @RequestMapping("/providerinfo_save")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result providerinfo_save(@Valid Tproviderinfo tproviderinfo) {
        providerService.saveOrUpdate(tproviderinfo);
        return new Result();
    }

    @RequestMapping("/categroy_updatePosition")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result categroy_updatePosition(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryId, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer pid, Integer categorySort, Integer customerId) {
        if (customerId == null) {
            customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        }
        TcoursesinfoCategroy c = ccoursesinfoCategroyService.getById(categoryId);
        if (c.getPid() != pid) {
            return new Result(Error.NOT_ALLOW_SKIP_LEVEL);
        }
        Integer rs = ccoursesinfoCategroyService.updatePosition(categoryType, categoryId, pid, categorySort + 1, customerId);
        return new Result(rs);
    }

    @RequestMapping("/categroy_tree")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result getCategroyTree(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType, Integer customerId) {
        if (customerId == null) {
            customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        }
        return new Result(ccoursesinfoCategroyService.getCategroyTree(ccoursesinfoCategroyService.toCategoryType(categoryType), customerId));
    }

    @RequestMapping("/getcount")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result getCount(HttpServletRequest request) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request
        ).getCustomerId();
        HashMap<String, Object> hashMapmap = new HashMap<String, Object>();

        PageRequest pr = new PageRequest();
        pr.getFilters().put("customerId", customerId);

        pr.getFilters().put("status", 0);
        Long videoCount = coursesinfoVideoService.getCount(pr);
        Long essayCount = coursesinfoEssayService.getCount(pr);
        Long providerCount = providerService.getCount(pr);//师资
        pr.getFilters().put("datumType", 1);
        pr.getFilters().put("customer_Id", customerId);
        Long datuCount = coursesinfoDatumService.getCount(pr);//文档
        pr.getFilters().put("datumType", 3);
        Long taskCount = coursesinfoDatumService.getCount(pr);//作业
        pr.getFilters().put("datumType", 4);
        Long diaoyanCount = coursesinfoDatumService.getCount(pr);//调研
        pr.getFilters().put("datumType", 5);
        Long examinationCount = coursesinfoDatumService.getCount(pr);//考试
        pr.getFilters().put("datumType", 0);
        Long otherCount = coursesinfoDatumService.getCount(pr);//其它
        pr.getFilters().put("datumType", 3);

        hashMapmap.put("datuCount", datuCount);//文档统计
        hashMapmap.put("taskCount", taskCount);//作业统计
        hashMapmap.put("diaoyanCount", diaoyanCount);//调研统计
        hashMapmap.put("examinationCount", examinationCount);//考试统计
        hashMapmap.put("otherCount", otherCount);//其它统计

        hashMapmap.put("providerCount", providerCount);//师资统计
        hashMapmap.put("videoCount", videoCount);//视频统计
        hashMapmap.put("essayCount", essayCount);//文章统计
        return new Result(hashMapmap);
    }

    @RequestMapping("/staff_statistics")
    @ResponseBody
    @AgentAuthCheck(authRequired = true)
    public Result staff_statistics(HttpServletRequest request, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer id, @NotNull(message = Message.PARAMER_CAN_NOT_BLANK) Integer categoryType) {
        Integer customerId = UserLoginInfoUtil.getCurrentUserFromCookie(request).getCustomerId();
        Map<String, Long> rs = tsourceStaffService.staffStatistics(id, customerId, categoryType);
        // double rate=rs.get("completeCount")*1.0/rs.get("totalCount");

        String r = "0";
        if (rs.get("completeCount") > 0) {
            float rate = (float) rs.get("completeCount") / (float) rs.get("totalCount");
            DecimalFormat df = new DecimalFormat("0.00");//格式化小数
            r = df.format(rate * 100);
        }
        Map mp = new HashMap<>();
        mp.put("totalCount", rs.get("totalCount"));
        mp.put("completeCount", rs.get("completeCount"));
        mp.put("rate", r);
        return new Result(mp);
    }
}
