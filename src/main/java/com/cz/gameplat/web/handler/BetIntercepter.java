package com.cz.gameplat.web.handler;

import com.cz.framework.StringUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.entity.AccountIpBlack;
import com.cz.gameplat.sys.enums.SysUserTypes;
import com.cz.gameplat.sys.service.AccountIpBlackService;
import com.cz.gameplat.sys.util.TokenManager;
import com.cz.gameplat.user.service.UserService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class BetIntercepter extends HandlerInterceptorAdapter
{
  private static Map<String, String> urls = new HashMap();
  
  @Autowired
  private UserService userService;
  @Autowired
  private AccountIpBlackService accountIpBlackService;
  
  static
  {
    urls.put("0", "/api/bet,/api/bet/betG,/api/bet/trace");
    urls.put("1", "/api/sports/bet");
    urls.put("2", "/api/live/play");
  }
  
  private Set<String> checkUrlSet(String[] games) {
    Set<String> checkBetUrl = new HashSet();
    if (games != null) {
      for (String game : games) {
        if (urls.containsKey(game)) {
          checkBetUrl.addAll(Arrays.asList(((String)urls.get(game)).split(",")));
        }
      }
    }
    return checkBetUrl;
  }
  
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws Exception
  {
    String token = TokenManager.getRequestToken(request);
    if (StringUtil.isBlank(token)) {
      return true;
    }
    
    Long uid = TokenManager.getUidByToken(token);
    TokenInfo tokenInfo = this.userService.getTokenInfo(uid);
    if (tokenInfo == null) {
      return true;
    }
    
    if (!tokenInfo.getToken().equals(token)) {
      return true;
    }
    
    String loginIp = com.cz.framework.web.HttpUtil.getforwardedForIP(request);
    
    Boolean isBlack = Boolean.valueOf(false);
    List<AccountIpBlack> accountIpBlackList = this.accountIpBlackService.getAll();
    
    if (accountIpBlackList == null) {
      return true;
    }
    

    String url = request.getRequestURI();
    
    for (AccountIpBlack item : accountIpBlackList) {
      if ((item.getIp().equals(loginIp)) && (item.getAccount().equals(tokenInfo.getAccount())) && (checkUrlSet(item.getGames().split(",")).contains(url))) {
        isBlack = Boolean.valueOf(true);
        break;
      }
    }
    if (isBlack.booleanValue()) {
      throw new BusinessException("您的ip不允许下注,请与客服人员联系");
    }
    if ((StringUtil.isNotBlank(url)) && (url.contains("/api/live/play")) && (SysUserTypes.VHY.getCode().equals(tokenInfo.getType()))) {
      throw new BusinessException("您的账号没有权限，请联系您的上级开通");
    }
    return true;
  }
}
