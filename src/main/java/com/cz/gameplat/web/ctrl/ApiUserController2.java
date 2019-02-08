package com.cz.gameplat.web.ctrl;

import com.cz.framework.HmacMD5Signer;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.exception.TransactionException;
import com.cz.framework.web.HttpUtil;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.enums.LimitEnums;
import com.cz.gameplat.sys.limit.RegisterLimit;
import com.cz.gameplat.sys.limit.enums.Switch;
import com.cz.gameplat.sys.service.LimitInfoService;
import com.cz.gameplat.user.bean.RegUserInfo;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.user.service.UserService2;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.HashMap;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;









@Validated
@Controller
@RequestMapping({"/v/user"})
public class ApiUserController2
{
  @Resource
  private UserService userService;
  @Resource
  private UserService2 userService2;
  @Resource
  private LimitInfoService limitInfoService;
  
  @RequestMapping(value={"/reg"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void register(@Valid RegUserInfo regInfo, String vCode, HttpServletRequest request, HttpServletResponse response, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent)
    throws Exception, TransactionException
  {
    RegisterLimit registerLimit = getRegisterLimit();
    if ((registerLimit.getvCode() != null) && (registerLimit.getvCode().intValue() == 1)) {
      validateYzm(vCode, request);
    }
    
    UserEquipmentVO clientInfo = UserEquipmentVO.create(userAgentString, clientUserAgent, request);
    

    UserInfo userInfo = this.userService2.register(regInfo, getDomain(request), clientInfo, registerLimit);
    

    TokenInfo token = this.userService.login(userInfo.getAccount(), regInfo.getPassword(), clientInfo, "HY");
    HttpUtil.removeCookie(request, response, "token");
    HttpUtil.setSessionCookieNotDomain(request, response, "token", token
      .getToken());
  }
  




  @RequestMapping(value={"/regTest"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public UserInfo register(HttpServletRequest request, HttpServletResponse response, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent)
    throws Exception, TransactionException
  {
    UserInfo info = this.userService2.registerForTest(null, UserEquipmentVO.create(userAgentString, clientUserAgent, request), 
      getRegisterLimit());
    
    TokenInfo token = this.userService.login(info.getAccount(), null, 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request), "test");
    HttpUtil.removeCookie(request, response, "token");
    HttpUtil.setSessionCookieNotDomain(request, response, "token", token
      .getToken());
    return info;
  }
  





  @RequestMapping(value={"/registerTrailUser"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public UserInfo registerTrailUser(HttpServletRequest request, HttpServletResponse response, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, String password, String valiCode)
    throws Exception, TransactionException
  {
    validateYzm(valiCode, request);
    
    UserInfo info = this.userService2.registerForTest(password, 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request), getRegisterLimit());
    
    TokenInfo token = this.userService.login(info.getAccount(), password, 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request), "test");
    HttpUtil.removeCookie(request, response, "token");
    HttpUtil.setSessionCookieNotDomain(request, response, "token", token
      .getToken());
    return info;
  }
  

  @RequestMapping(value={"/wxLogin"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public String wxLogin(String unionid, String nickname, String spreadCode, long time, String sign, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request, HttpServletResponse response)
    throws BusinessException
  {
    if (System.currentTimeMillis() - time > 300000L) {
      throw new BusinessException("登录已过期！");
    }
    HashMap<String, Object> params = new HashMap();
    params.put("UNIONID", unionid);
    params.put("NICKNAME", nickname);
    params.put("SPREADCODE", spreadCode);
    params.put("TIME", Long.valueOf(time));
    String mysign = HmacMD5Signer.sign(params, "QA%qQ$xgANN@fLi0");
    if (!mysign.equals(sign)) {
      throw new BusinessException("未授权访问！");
    }
    
    UserEquipmentVO clientInfo = UserEquipmentVO.create(userAgentString, clientUserAgent, request);
    try
    {
      TokenInfo token = this.userService2.wxAutoLogin(unionid, nickname, Integer.valueOf(Integer.parseInt(spreadCode)), clientInfo);
      HttpUtil.setSessionCookie(request, response, "token", token.getToken());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "redirect:/" + spreadCode;
  }
  
  private RegisterLimit getRegisterLimit() throws Exception
  {
    RegisterLimit registerLimit = (RegisterLimit)this.limitInfoService.get(LimitEnums.registerLimit.getName(), RegisterLimit.class);
    if ((registerLimit == null) || (registerLimit.getRegister() == Switch.NO.getValue())) {
      throw new BusinessException("未开放注册功能，请与客服人员联系！");
    }
    return registerLimit;
  }
  
  private void validateYzm(String yzm, HttpServletRequest request) throws Exception {
    HttpSession session = request.getSession();
    String checkCode = (String)session.getAttribute("checkCode");
    session.removeAttribute("checkCode");
    if (!StringUtils.equalsIgnoreCase(yzm, checkCode)) {
      throw new BusinessException("验证码错误！");
    }
  }
  
  private String getDomain(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String url = request.getRequestURL().toString();
    return url.replace(uri, "").replaceAll("http://|https://|www.", "");
  }
}
