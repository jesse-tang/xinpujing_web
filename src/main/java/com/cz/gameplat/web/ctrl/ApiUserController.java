package com.cz.gameplat.web.ctrl;

import com.cz.framework.StringUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.exception.TransactionException;
import com.cz.framework.web.HttpUtil;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.cache.AdminCache;
import com.cz.gameplat.sys.entity.Config;
import com.cz.gameplat.sys.enums.LimitEnums;
import com.cz.gameplat.sys.limit.LoginLimit;
import com.cz.gameplat.sys.limit.RegisterLimit;
import com.cz.gameplat.sys.limit.enums.VCodeLimit;
import com.cz.gameplat.sys.service.ConfigService;
import com.cz.gameplat.sys.service.LimitInfoService;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.web.interceptor.HY;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;





@Validated
@Controller
@RequestMapping({"/v/user"})
public class ApiUserController
{
  @Resource
  private UserService userService;
  @Resource
  private LimitInfoService limitInfoService;
  @Resource
  private ConfigService configService;
  @Resource
  private AdminCache adminCache;
  @Resource
  private RedisTemplate redisTemplate;
  private static final Logger logger = LoggerFactory.getLogger(ApiUserController.class);
  
  @RequestMapping(value={"/onlineCount"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> onlineCount() {
    String cacheKey = "online_count";
    Integer onlineNum = (Integer)this.redisTemplate.opsForValue().get(cacheKey);
    HashMap<String, Object> result = new HashMap();
    if (onlineNum == null) {
      try
      {
        Config onlineFormulaConfig = this.configService.getByNameAndKey("system_config", "online_num_formula");
        String formula = onlineFormulaConfig.getConfigValue();
        Random random = new Random();
        
        formula = formula.replace("N", String.valueOf(this.adminCache.getOnlineCount())).replace("R", String.valueOf(random.nextFloat()));
        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(formula);
        onlineNum = (Integer)expression.getValue(Integer.class);
        this.redisTemplate.opsForValue().set(cacheKey, onlineNum, 1L, TimeUnit.MINUTES);
      } catch (Throwable e) {
        e.printStackTrace();
        onlineNum = Integer.valueOf(888);
      }
    }
    result.put("count", onlineNum);
    return result;
  }
  



































  @RequestMapping(value={"/login"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public TokenInfo login(String account, String password, String valiCode, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request, HttpServletResponse response)
    throws Exception, TransactionException
  {
    LoginLimit limit = (LoginLimit)this.limitInfoService.get(LimitEnums.userLoginLimit.getName(), LoginLimit.class);
    if ((limit != null) && (VCodeLimit.YES.getValue() == limit.getvCode())) {
      HttpSession session = request.getSession();
      String checkCode = (String)session.getAttribute("checkCode");
      session.removeAttribute("checkCode");
      if ((StringUtil.isBlank(valiCode)) || (!valiCode.equals(checkCode))) {
        throw new BusinessException("UC/VALICODE_ERROR", "uc.login_valiCode_error", null);
      }
    }
    
    if (StringUtil.isBlank(account))
    {
      throw new BusinessException("UC/USER_ACCOUNT_ERROR", "账号不能为空", null);
    }
    
    if (StringUtil.isBlank(password)) {
      throw new BusinessException("UC/USER_PASSWORD_ERROR", "密码不能为空", null);
    }
    
    UserEquipmentVO equipment = UserEquipmentVO.create(userAgentString, clientUserAgent, request);
    TokenInfo token = this.userService.login(account, password, equipment, "HY");
    
    HttpUtil.setSessionCookie(request, response, "token", token.getToken());
    return token;
  }
  


  @RequestMapping({"/logout"})
  @ResponseBody
  public void logout(@HY Long userId, HttpServletRequest request, HttpServletResponse response)
    throws Exception
  {
    if (userId != null) {
      TokenInfo info = this.userService.getTokenInfo(userId);
      logger.info("logout " + info);
      this.userService.logout(userId);
      HttpUtil.removeCookie(request, response, "token");
    }
  }
  
































































































































































  @RequestMapping(value={"/getRegLimit"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public RegisterLimit getRegisterLimit()
    throws Exception
  {
    return (RegisterLimit)this.limitInfoService.get(LimitEnums.registerLimit.getName(), RegisterLimit.class);
  }
  


  @RequestMapping(value={"/getLoginLimit"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public LoginLimit getLoginLimit()
    throws Exception
  {
    return (LoginLimit)this.limitInfoService.get(LimitEnums.userLoginLimit.getName(), LoginLimit.class);
  }
  


  @RequestMapping(value={"/checkUnique"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public boolean checkAccountUnique(String val, int type)
    throws Exception
  {
    switch (type) {
    case 0: 
      return this.userService.checkAccount(val);
    


    case 1: 
    case 2: 
      return false;
    }
    return this.userService.checkAccount(val);
  }
  





  @RequestMapping(value={"/checkUnique2"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public boolean checkAccountUniqueReturnTrueIfValid(String val, int type)
    throws Exception
  {
    switch (type) {
    case 0: 
      return !this.userService.checkAccount(val);
    
    case 1: 
      return true;
    }
    return !this.userService.checkAccount(val);
  }
}
