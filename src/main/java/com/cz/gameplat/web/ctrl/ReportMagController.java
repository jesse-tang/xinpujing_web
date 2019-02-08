package com.cz.gameplat.web.ctrl;

import com.cz.framework.StringUtil;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.entity.UserBillQueryReq;
import com.cz.gameplat.lottery.bean.TranTypeBean;
import com.cz.gameplat.lottery.enums.TranTypes;
import com.cz.gameplat.user.entity.UserBill;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.UserBillService;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;














@Validated
@Controller
@RequestMapping({"/api/reportMag"})
public class ReportMagController
{
  @Resource
  private UserBillService userBillService;
  @Resource
  private UserService userService;
  
  @RequestMapping(value={"/queryPageBill"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserBill> queryPageBill(@HY UserInfo user, UserBillQueryReq userBillReq, PageBean pageBean)
    throws Exception
  {
    if (user == null) {
      return null;
    }
    
    if (userBillReq.getUserId() != null)
    {
      if (!this.userService.checkSubUser(user, userBillReq.getUserId())) {
        throw new BusinessException("未查询到对应下级用户");
      }
      
      user = this.userService.getUserInfo(userBillReq.getUserId());
    }
    

    if (StringUtil.isNotBlank(userBillReq.getAccount())) {
      UserInfo userInfo = this.userService.getUserInfo(userBillReq.getAccount());
      if (userInfo == null)
      {
        return new PageData();
      }
      userBillReq.setUserId(userInfo.getUserId());
      
      userBillReq.setSuperPath(user.getSuperPath());
    }
    else {
      userBillReq.setUserId(user.getUserId());
    }
    
    return this.userBillService.slaveQueryBillReportPage(user, userBillReq, pageBean);
  }
  
  @RequestMapping(value={"/getTransList"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<TranTypeBean> queryTranTypes() {
    return TranTypes.getAllTranList();
  }
  
  @RequestMapping(value={"/getTransMap"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<Integer, String> queryTranTypesMap() {
    return TranTypes.getTransMap();
  }
}
