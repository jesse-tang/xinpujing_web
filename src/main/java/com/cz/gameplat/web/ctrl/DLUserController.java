package com.cz.gameplat.web.ctrl;

import com.cz.framework.StringUtil;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.report.bean.QueryCompanyReportReq;
import com.cz.gameplat.report.bean.TeamReportVO;
import com.cz.gameplat.report.service.ReportService;
import com.cz.gameplat.sys.enums.SysUserTypes;
import com.cz.gameplat.user.bean.UserInfoRep;
import com.cz.gameplat.user.bean.UserInfoReq;
import com.cz.gameplat.user.bean.UserInfoVO;
import com.cz.gameplat.user.entity.UserExtInfo;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;










@Validated
@Controller
@RequestMapping({"/api/dl"})
public class DLUserController
{
  @Resource
  private UserService userService;
  @Resource
  private ReportService reportService;
  
  @RequestMapping(value={"/queryUsers"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserInfoRep> queryUsers(UserInfoReq info, PageBean pageBean, @HY Long userId)
    throws BusinessException
  {
    UserInfo agent = this.userService.getUserInfo(userId);
    if ((agent == null) || (!agent.getIsDl().booleanValue())) {
      return new PageData(null, 0);
    }
    

    List<String> accountList = new ArrayList();
    if (StringUtil.isNotBlank(info.getAccount())) {
      String[] list = info.getAccount().replace("，", ",").replace(" ", "").split(",");
      if (list.length > 0) {
        accountList = Arrays.asList(list);
      }
    }
    info.setAccoutList(accountList);
    info.setSuperPath(agent.getSuperPath());
    info.setSuperId(agent.getUserId());
    
    return this.userService.slaveQuery(info, pageBean);
  }
  




























































  @RequestMapping(value={"/querySubUsers"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserInfoRep> querySubUsers(@HY UserInfo user, UserInfoReq info, PageBean pageBean)
    throws Exception
  {
    if (StringUtil.isNotBlank(info.getSubAccount())) {
      user = this.userService.getUserInfo(info.getSubAccount());
    }
    

    if (StringUtil.isBlank(info.getAccount())) {
      info.setAccount(null);
      info.setMinDlLevel(user.getDlLevel());
      info.setMaxDlLevel(Integer.valueOf(user.getDlLevel().intValue() + 1));
    }
    
    info.setUserId(user.getUserId());
    info.setSuperPath(user.getSuperPath());
    
    return this.userService.slaveQuerySubUsers(info, pageBean);
  }
  




  @RequestMapping(value={"/queryUser"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public UserInfoVO queryInfoByUserId(@HY UserInfo info, UserInfoReq infoReq)
    throws BusinessException
  {
    if (!this.userService.checkSubUser(info, infoReq.getUserId())) {
      throw new BusinessException("未查询到对应下级用户");
    }
    UserInfo userInfo = this.userService.getUserInfo(infoReq.getUserId());
    UserInfoVO vo = new UserInfoVO();
    vo.setUserInfo(userInfo);
    UserExtInfo extInfo = this.userService.getUserExtInfoByCahe(infoReq.getUserId());
    vo.setExtInfo(extInfo);
    return vo;
  }
  

























































































































  @RequestMapping(value={"/queryTeamRws"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public TeamReportVO queryTeamRws(@HY UserInfo user, QueryCompanyReportReq infoReq)
    throws Exception
  {
    UserInfo info = null;
    if (infoReq.getUserId() != null) {
      info = this.userService.getUserInfo(infoReq.getUserId());
    } else if (StringUtils.isNotBlank(infoReq.getAccount())) {
      info = this.userService.getUserInfo(infoReq.getAccount());
    }
    infoReq.setSuperPath(user.getSuperPath());
    
    if (info != null) {
      if (!info.getSuperPath().startsWith(user.getSuperPath())) {
        throw new BusinessException("USER/DL_ERROR", "未查询到对应下级用户", null);
      }
      infoReq.setAccount(null);
      infoReq.setUserId(null);
      infoReq.setSuperPath(info.getSuperPath());
    }
    
    return this.reportService.slaveQqueryTeamReport(infoReq);
  }
  






  @RequestMapping(value={"/queryPersonalRws"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public TeamReportVO queryPersonalRws(@HY UserInfo user, QueryCompanyReportReq infoReq)
    throws Exception
  {
    infoReq.setUserId(user.getUserId());
    if (user.getType().equals(SysUserTypes.VHY.getCode())) {
      return this.reportService.slaveQueryPesonalReportVHY(infoReq);
    }
    return this.reportService.slaveQqueryPersonalReport(infoReq);
  }
}
