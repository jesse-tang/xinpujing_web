package com.cz.gameplat.web.ctrl;

import com.cz.framework.StringUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.service.UserBetReportService;
import com.cz.gameplat.report.service.ReportService;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.service.ConfigService;
import com.cz.gameplat.user.bean.QueryPushMessage;
import com.cz.gameplat.user.bean.RegUserBank;
import com.cz.gameplat.user.bean.UserInfoVO;
import com.cz.gameplat.user.entity.UserBank;
import com.cz.gameplat.user.entity.UserExtInfo;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.enums.MessageEnum;
import com.cz.gameplat.user.enums.ReadStatus;
import com.cz.gameplat.user.service.PushMessageService;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;









@Validated
@Controller
@RequestMapping({"/api/user"})
public class UserInfoController
{
  @Resource
  private ReportService reportService;
  @Resource
  private UserBetReportService betReportService;
  @Resource
  private UserService userService;
  @Resource
  private PushMessageService pushMessageService;
  @Resource
  private ConfigService configService;
  
  @RequestMapping(value={"/info"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public UserInfoVO info(@HY Long userId)
    throws Exception
  {
    if (userId == null) {
      return null;
    }
    UserInfoVO userInfoVO = this.userService.get(userId);
    if (userInfoVO.getUserBank() != null) {
      String cardNo = userInfoVO.getUserBank().getCardNo();
      if (StringUtil.isNotBlank(cardNo)) {
        String s = cardNo.replaceAll("(?<=\\d{4})\\d(?=\\d{4})", "*");
        userInfoVO.getUserBank().setCardNo(s);
      }
    }
    
    QueryPushMessage queryPushMessage = new QueryPushMessage();
    queryPushMessage.setUserId(userId);
    queryPushMessage.setReadStatus(ReadStatus.READ_STATUS_NO.getValue());
    queryPushMessage.setAcceptRemoveFlag(MessageEnum.MESSAGE_REMOVE_NO.getValue());
    userInfoVO.setUnReadCount(Integer.valueOf(this.pushMessageService.slaveQueryNotReadCount(queryPushMessage)));
    
    return userInfoVO;
  }
  
  @RequestMapping(value={"/status"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> status(@HY Long userId) {
    Map<String, Object> result = new HashMap();
    double money = 0.0D;
    if (userId != null) {
      UserExtInfo extInfo = this.userService.getUserExtInfo(userId);
      if (extInfo != null) {
        money = extInfo.getMoney().doubleValue();
      }
    }
    
    QueryPushMessage queryPushMessage = new QueryPushMessage();
    queryPushMessage.setUserId(userId);
    queryPushMessage.setReadStatus(ReadStatus.READ_STATUS_NO.getValue());
    queryPushMessage.setAcceptRemoveFlag(MessageEnum.MESSAGE_REMOVE_NO.getValue());
    int unReadCount = this.pushMessageService.slaveQueryNotReadCount(queryPushMessage);
    
    result.put("message", Integer.valueOf(unReadCount));
    result.put("money", Double.valueOf(money));
    return result;
  }
  

  @RequestMapping(value={"/getToken"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public TokenInfo getToken(@HY Long userId)
    throws Exception
  {
    if (userId == null) {
      return null;
    }
    try {
      return this.userService.getTokenInfo(userId);
    }
    catch (Exception localException) {}
    
    return null;
  }
  

  @RequestMapping(value={"/relieve"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void relieveLimited(Long uid)
    throws Exception
  {
    this.userService.relieveLimited(uid);
  }
  


  @RequestMapping(value={"/bindUserBank"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void bindUserBank(@HY Long userId, RegUserBank regUserBank)
    throws Exception
  {
    this.userService.createUserBank(userId, regUserBank, null);
  }
  


  @RequestMapping({"/modifyUserInfo"})
  @ResponseBody
  public void modifyUserInfo(@HY Long userId, UserInfo userInfo, RegUserBank regUserBank)
    throws Exception
  {
    userInfo.setUserMemo(null);
    this.userService.update(userInfo, regUserBank, userId);
  }
  

  @RequestMapping(value={"/updateFundPwd"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void updateFundPwd(@HY Long userId, String oldPassword, @NotBlank(message="user.fundPwd.notNull") String newPassword)
    throws BusinessException
  {
    this.userService.updateFundPwd(userId, oldPassword, newPassword, true);
  }
  




  @RequestMapping(value={"/updatePassword"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void updatePassword(@HY Long userId, @NotBlank(message="user.password.notNull") String oldPassword, @NotBlank(message="user.password.notNull") String newPassword)
    throws BusinessException
  {
    this.userService.updatePassword(userId, oldPassword, newPassword, null, true);
  }
  
  @RequestMapping(value={"/saveFavoriteLmclGameIds"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void saveUserFavoriteLmclGameIds(@HY Long userId, String gameIds) throws BusinessException {
    this.userService.saveUserFavoriteLmclGameIds(userId, gameIds);
  }
  
  @RequestMapping(value={"/getUserFavoriteLmclGameIds"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public String[] getUserFavoriteLmclGameIds(@HY Long userId) {
    return this.userService.getUserFavoriteLmclGameIds(userId);
  }
}
