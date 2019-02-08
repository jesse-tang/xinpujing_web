package com.cz.gameplate.web.game.ctrl;

import com.cz.framework.DateUtil;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.entity.UserBetQueryReq;
import com.cz.gameplat.game.entity.UserBetRep;
import com.cz.gameplat.game.service.UserBetService;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.web.interceptor.HY;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;









@Controller
@RequestMapping({"/v/records"})
public class TransRecordController
{
  @Resource
  private UserBetService userBetService;
  @Resource
  private UserService userService;
  
  @RequestMapping(value={"/getRecords"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserBetRep> getRecords(@HY UserInfo user, UserBetQueryReq params, PageBean page)
    throws Exception
  {
    if (user == null) {
      return null;
    }
    params.setUserId(user.getUserId());
    if (params.getStartDate() != null) {
      params.setStartDate(DateUtil.getDateStart(params.getStartDate()));
    }
    if (params.getEndDate() != null) {
      params.setEndDate(DateUtil.getDateEnd(params.getEndDate()));
    }
    params.setAccount(null);
    return this.userBetService.slaveQueryUserBetPage(params, page);
  }
  
  @RequestMapping(value={"/queryByAgent"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserBetRep> queryByAgent(@HY UserInfo user, UserBetQueryReq params, String subAccount, PageBean page)
    throws BusinessException
  {
    if (user == null) {
      return null;
    }
    if (StringUtils.isNotBlank(subAccount)) {
      UserInfo userInfo = this.userService.getUserInfo(subAccount);
      if ((userInfo == null) || 
        (!StringUtils.startsWith(userInfo.getSuperPath(), user.getSuperPath()))) {
        return new PageData();
      }
      params.setUserId(userInfo.getUserId());
    } else {
      params.setUserId(user.getUserId());
    }
    if (params.getStartDate() != null) {
      params.setStartDate(DateUtil.getDateStart(params.getStartDate()));
    }
    if (params.getEndDate() != null) {
      params.setEndDate(DateUtil.getDateEnd(params.getEndDate()));
    }
    params.setAccount(null);
    return this.userBetService.slaveQueryUserBetPage(params, page);
  }
}
