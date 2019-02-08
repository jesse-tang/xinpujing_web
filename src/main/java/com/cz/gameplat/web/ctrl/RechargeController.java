package com.cz.gameplat.web.ctrl;

import com.alibaba.fastjson.JSONObject;
import com.cz.framework.DateUtil;
import com.cz.framework.LogUtil;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.redis.lock.Lock;
import com.cz.framework.web.HttpUtil;
import com.cz.gameplat.payment.bo.TransferRechargeOrderBo;
import com.cz.gameplat.payment.constant.EquipmentEnum;
import com.cz.gameplat.payment.constant.RechargeMode;
import com.cz.gameplat.payment.constant.TrueFalse;
import com.cz.gameplat.payment.entity.DiscountType;
import com.cz.gameplat.payment.entity.PayAccount;
import com.cz.gameplat.payment.entity.PayType;
import com.cz.gameplat.payment.entity.RechargeConfig;
import com.cz.gameplat.payment.entity.RechargeOrder;
import com.cz.gameplat.payment.query.RechargeOrderQuery;
import com.cz.gameplat.payment.service.DiscountTypeService;
import com.cz.gameplat.payment.service.PayAccountService;
import com.cz.gameplat.payment.service.PayTypeService;
import com.cz.gameplat.payment.service.RechargeConfigService;
import com.cz.gameplat.payment.service.RechargeOrderService;
import com.cz.gameplat.payment.service.TpPayChannelService;
import com.cz.gameplat.payment.thirdparty.ThirdPartyCallBackResult;
import com.cz.gameplat.payment.vo.PayAccountVO;
import com.cz.gameplat.payment.vo.TpPayChannelVO;
import com.cz.gameplat.sys.enums.LimitEnums;
import com.cz.gameplat.sys.limit.RechargeLimit;
import com.cz.gameplat.sys.service.LimitInfoService;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.web.interceptor.HY;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping({"/api/recharge"})
public class RechargeController
{
  @Resource
  private PayTypeService payTypeService;
  @Resource
  private PayAccountService payAccountService;
  @Resource
  private RechargeConfigService rechargeConfigService;
  @Resource
  private TpPayChannelService tpPayChannelService;
  @Resource
  private RechargeOrderService rechargeOrderService;
  @Resource
  private DiscountTypeService discountTypeService;
  @Resource
  private LimitInfoService limitInfoService;
  
  @RequestMapping(value={"getPayTypes"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public List<PayType> getPayTypes()
  {
    return this.payTypeService.queryEnabledList();
  }
  
  @RequestMapping(value={"getRechargeConfig"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public RechargeConfig getRechargeConfig(@HY UserInfo user, Integer mode, String payType) {
    return this.rechargeConfigService.getByModeAndPayType(mode, payType, user);
  }
  

  @RequestMapping(value={"getPayAccounts"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public List<PayAccountVO> getPayAccounts(@HY UserInfo userInfo, String payType, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request)
    throws Exception
  {
    DeviceType deviceType = clientUserAgent.getOperatingSystem().getDeviceType();
    LogUtil.info("deviceType:" + deviceType.getName());
    



    UserEquipmentVO userEquipmentVO = UserEquipmentVO.create(userAgentString, clientUserAgent, request);
    Integer code = convert2DeviceCode(userEquipmentVO.getMacOs());
    
    LogUtil.info("用户设备名称为：" + userEquipmentVO.getMacOs() + "      对应的Code为：" + code);
    



    int hour = Calendar.getInstance().get(11);
    List<PayAccountVO> accountVOS = this.payAccountService.queryEnabledList(payType, userInfo.getHyLevel());
    Iterator<PayAccountVO> iter = accountVOS.iterator();
    while (iter.hasNext()) {
      PayAccountVO payAccountVO = (PayAccountVO)iter.next();
      



      if ((null != code) && (StringUtils.isNotBlank(payAccountVO.getChannelShows()))) {
        if (!StringUtils.contains(payAccountVO.getChannelShows(), String.format("%s" + code + "%s", new Object[] { ",", "," }))) {
          LogUtil.info("支付通道被过滤，支付方式在次设备不展示。收款账号为：" + payAccountVO.getAccount() + "  用户登录设备为：" + userEquipmentVO.getMacOs());
          iter.remove();
          continue;
        }
      }
      



      if ((TrueFalse.FALSE.getValue() == payAccountVO.getChannelTimeStatus().intValue()) && (
        (hour < payAccountVO.getChannelTimeStart().intValue()) || (hour > payAccountVO.getChannelTimeEnd().intValue()))) {
        LogUtil.info("支付通道被过滤，当前时间不在通道展示时间内，收款账号为：" + payAccountVO.getAccount() + "  展示时间为：" + payAccountVO.getChannelTimeStart() + "点-" + payAccountVO.getChannelTimeEnd() + "点");
        iter.remove();
      }
    }
    




    RechargeLimit rechargeLimit = (RechargeLimit)this.limitInfoService.get(LimitEnums.RECHARGE_LIMIT.getName(), RechargeLimit.class);
    if ((TrueFalse.TRUE.getValue() == rechargeLimit.getScramblePayAccount().intValue()) && 
      (CollectionUtils.isNotEmpty(accountVOS))) {
      Collections.shuffle(accountVOS);
    }
    return accountVOS;
  }
  
  private Integer convert2DeviceCode(String macName)
  {
    if (StringUtils.containsIgnoreCase(macName, EquipmentEnum.EQU_ANDROID.getName()))
      return EquipmentEnum.EQU_ANDROID.getCode();
    if ((StringUtils.containsIgnoreCase(macName, EquipmentEnum.EQU_IOS.getName())) || 
      (StringUtils.containsIgnoreCase(macName, "iPad"))) {
      return EquipmentEnum.EQU_IOS.getCode();
    }
    return EquipmentEnum.EQU_WINDOWS.getCode();
  }
  





  @RequestMapping(value={"getTpPayChannels"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public List<TpPayChannelVO> getTpPayChannels(@HY UserInfo userInfo, String payType, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request)
  {
    UserEquipmentVO userEquipmentVO = UserEquipmentVO.create(userAgentString, clientUserAgent, request);
    Integer code = convert2DeviceCode(userEquipmentVO.getMacOs());
    
    LogUtil.info("用户设备名称为：" + userEquipmentVO.getMacOs() + "      对应的Code为：" + code);
    



    int hour = Calendar.getInstance().get(11);
    
    List<TpPayChannelVO> channelVOS = this.tpPayChannelService.queryEnabledList(payType, userInfo.getHyLevel(), 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request).isMobileDevice());
    Iterator<TpPayChannelVO> it = channelVOS.iterator();
    while (it.hasNext()) {
      TpPayChannelVO channelVO = (TpPayChannelVO)it.next();
      



      if ((null != code) && (StringUtils.isNotBlank(channelVO.getChannelShows()))) {
        if (!StringUtils.contains(channelVO.getChannelShows(), String.format("%s" + code + "%s", new Object[] { ",", "," }))) {
          LogUtil.info("支付通道被过滤，支付方式在次设备不展示。支付渠道为：" + channelVO.getName() + "  用户登录设备为：" + userEquipmentVO.getMacOs());
          it.remove();
          continue;
        }
      }
      



      if ((TrueFalse.FALSE.getValue() == channelVO.getChannelTimeStatus().intValue()) && (
        (hour < channelVO.getChannelTimeStart().intValue()) || (hour > channelVO.getChannelTimeEnd().intValue()))) {
        LogUtil.info("支付通道被过滤，当前时间不在通道展示时间内，支付渠道为：" + channelVO.getName() + "  展示时间为：" + channelVO.getChannelTimeStart() + "点-" + channelVO.getChannelTimeEnd() + "点");
        it.remove();
      }
    }
    

    return channelVOS;
  }
  



  @Lock(value="web_recharge", key="#userId")
  @RequestMapping(value={"transfer"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  public void transfer(@HY Long userId, TransferRechargeOrderBo transferRechargeOrderBo, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request)
    throws Exception
  {
    if (StringUtils.isNotEmpty(transferRechargeOrderBo.getRechargePerson())) {
      boolean isMatch = Pattern.matches("^(?!_)(?!.*?_$)[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", transferRechargeOrderBo
        .getRechargePerson());
      if (!isMatch) {
        throw new BusinessException("存款人不能包含特殊符号");
      }
    }
    

    RechargeConfig rechargeConfig = this.rechargeConfigService.getByModeAndPayType(Integer.valueOf(RechargeMode.TRANSFER.getValue()), this.payAccountService
      .get(transferRechargeOrderBo.getPayAccountId()).getPayType());
    if (transferRechargeOrderBo.getYzmNum() != "") {
      verifyValidateCode(userId, transferRechargeOrderBo.getYzmNum(), rechargeConfig, request);
    }
    transferRechargeOrderBo.setUserId(userId);
    this.rechargeOrderService.transfer(transferRechargeOrderBo, 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request));
  }
  
  @Lock(value="web_recharge", key="#userId")
  @RequestMapping(value={"onlinePay"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public String onlinePay(@HY Long userId, Long payChannelId, String bankCode, Double amount, String yzmNum, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request)
    throws Exception
  {
    TpPayChannelVO tpPayChannelVO = this.tpPayChannelService.getVO(payChannelId);
    
    RechargeConfig rechargeConfig = this.rechargeConfigService.getByModeAndPayType(Integer.valueOf(RechargeMode.ONLINE_PAY.getValue()), tpPayChannelVO.getPayType());
    verifyValidateCode(userId, yzmNum, rechargeConfig, request);
    return this.rechargeOrderService.onlinePay(userId, payChannelId, bankCode, amount, "/api/recharge/onlinePayAsyncCallback/${orderNo}", "/api/recharge/onlinePaySyncCallback/${orderNo}", 
    

      UserEquipmentVO.create(userAgentString, clientUserAgent, request));
  }
  

  @RequestMapping({"onlinePayAsyncCallback/{orderNo}"})
  public String onlinePayAsyncCallback(@PathVariable String orderNo, @RequestBody(required=false) String requestBody, HttpServletRequest request, String userAgentString, UserAgent clientUserAgent)
    throws Exception
  {
    JSONObject headerJson = new JSONObject();
    Enumeration e = request.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = (String)e.nextElement();
      String value = request.getHeader(name);
      headerJson.put(name, value);
    }
    return 
      this.rechargeOrderService.onlinePayAsyncCallback(orderNo, getRequestParameterMap(request), requestBody, 
      HttpUtil.getforwardedForIP(request), "/api/recharge/onlinePayAsyncCallback/${orderNo}", "/api/recharge/onlinePaySyncCallback/${orderNo}", 
      

      UserEquipmentVO.create(userAgentString, clientUserAgent, request), headerJson);
  }
  

  @RequestMapping({"onlinePaySyncCallback/{orderNo}"})
  public String onlinePaySyncCallback(@PathVariable String orderNo, @RequestBody(required=false) String requestBody, HttpServletRequest request, String userAgentString, UserAgent clientUserAgent)
    throws Exception
  {
    ThirdPartyCallBackResult result = this.rechargeOrderService.onlinePaySyncCallback(orderNo, getRequestParameterMap(request), requestBody, 
      HttpUtil.getforwardedForIP(request), "/api/recharge/onlinePayAsyncCallback/${orderNo}", "/api/recharge/onlinePaySyncCallback/${orderNo}", 
      

      UserEquipmentVO.create(userAgentString, clientUserAgent, request));
    return "<html><head><meta charset=\"UTF-8\"><meta http-equiv=\"Content-Type\" content=\"text/html;\"><title>充值失败</title></head><body>充值失败！" + result
    

      .getErrorMessage() + "</body></html>";
  }
  
  private Map<String, String> getRequestParameterMap(HttpServletRequest request) {
    Map<String, String> parameters = new HashMap();
    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String parameterName = (String)parameterNames.nextElement();
      parameters.put(parameterName, request.getParameter(parameterName));
    }
    return parameters;
  }
  
  private void verifyValidateCode(Long userId, String yzmNum, RechargeConfig rechargeConfig, HttpServletRequest request) throws Exception
  {
    if ((rechargeConfig != null) && 
      (rechargeConfig.getValidateCodeEnabled().intValue() == TrueFalse.TRUE.getValue())) {
      HttpSession session = request.getSession();
      String checkCode = (String)session.getAttribute("checkCode_" + userId);
      session.removeAttribute("checkCode_" + userId);
      if (!StringUtils.equalsIgnoreCase(yzmNum, checkCode)) {
        throw new BusinessException("验证码错误");
      }
    }
  }
  



  @RequestMapping(value={"/personalRechargeOrder"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<RechargeOrder> personalReport(@HY UserInfo userInfo, @NotNull Date dateFrom, @NotNull Date dateTo, Integer status, Integer mode, PageBean pageBean)
  {
    RechargeOrderQuery rechargeOrderQuery = new RechargeOrderQuery();
    rechargeOrderQuery.setUserId(userInfo.getUserId());
    rechargeOrderQuery.setAddTimeFrom(DateUtil.getDateFirst(dateFrom));
    rechargeOrderQuery.setAddTimeTo(DateUtil.getDateLast(dateTo));
    rechargeOrderQuery.setMode(mode);
    rechargeOrderQuery.setStatus(status);
    rechargeOrderQuery.setUserType(userInfo.getType());
    return this.rechargeOrderService.masterQueryPage(rechargeOrderQuery, pageBean);
  }
  
  @RequestMapping(value={"/queryDiscountTypeList"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<DiscountType> queryDiscountTypeList() {
    return this.discountTypeService.queryAll();
  }
  

  @RequestMapping(value={"/deposit"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public String deposit(TransferRechargeOrderBo transferRechargeOrderBo)
    throws Exception
  {
    return this.rechargeOrderService.orderDeposit(transferRechargeOrderBo);
  }
}
