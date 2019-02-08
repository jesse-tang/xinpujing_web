package com.cz.gameplat.web.ctrl;

import com.cz.framework.DateUtil;
import com.cz.framework.StringUtil;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.gameplat.user.bean.QueryUserCash;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.entity.UserBank;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.entity.UserWithdraw;
import com.cz.gameplat.user.entity.UserWithdrawHistory;
import com.cz.gameplat.user.entity.ValidWithdraw;
import com.cz.gameplat.user.service.UserWithdrawService;
import com.cz.gameplat.user.service.ValidWithdrawService;
import com.cz.gameplat.web.interceptor.HY;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.Date;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

















@Validated
@Controller
@RequestMapping({"/api/userWithdraw"})
public class UserWithdrawWebController
{
  @Resource
  ValidWithdrawService validWithdrawService;
  @Resource
  private UserWithdrawService userWithdrawService;
  
  @RequestMapping(value={"/save"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void save(@HY Long userId, @NotNull(message="{NoNull}") String cashPassword, UserWithdraw userWithdraw, String yzmNum, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request)
    throws Exception
  {
    this.userWithdrawService.save(userId, cashPassword, userWithdraw, yzmNum, 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request), request);
  }
  





  @RequestMapping(value={"/queryOutMoneyIndex"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> queryOutMoneyIndex(@HY Long userId)
    throws Exception
  {
    Map<String, Object> result = this.userWithdrawService.queryUserCashInfo(userId);
    Object objBank = result.get("userBank");
    if ((objBank != null) && ((objBank instanceof UserBank))) {
      UserBank userBank = (UserBank)objBank;
      String cardNo = userBank.getCardNo();
      if (StringUtil.isNotBlank(cardNo)) {
        String s = cardNo.replaceAll("(?<=\\d{4})\\d(?=\\d{4})", "*");
        userBank.setCardNo(s);
        result.put("userBank", userBank);
      }
    }
    return result;
  }
  




  @RequestMapping(value={"/queryPage"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserWithdraw> queryPage(@HY UserInfo userInfo, QueryUserCash queryUserCash, PageBean pageBean)
    throws Exception
  {
    queryUserCash.setBeginDatetime(DateUtil.getDateStart(queryUserCash.getBeginDatetime()));
    queryUserCash.setEndDatetime(DateUtil.getDateEnd(queryUserCash.getEndDatetime()));
    queryUserCash.setUserType(userInfo.getType());
    return this.userWithdrawService.queryPageWeb(userInfo.getUserId(), queryUserCash, pageBean);
  }
  



  @RequestMapping(value={"/queryPageHistory"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<UserWithdrawHistory> queryPageHistory(@HY Long userId, QueryUserCash queryUserCash, PageBean pageBean)
    throws Exception
  {
    return this.userWithdrawService.slaveQueryPageHistoryWeb(userId, queryUserCash, pageBean);
  }
  





  @RequestMapping(value={"/validWithdraw/queryAll"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> getAll(@HY Long userId)
    throws Exception
  {
    ValidWithdraw validWithdraw = new ValidWithdraw();
    validWithdraw.setUserId(userId);
    validWithdraw.setStatus(Integer.valueOf(0));
    return this.validWithdrawService.queryAll(validWithdraw, new Date());
  }
}
