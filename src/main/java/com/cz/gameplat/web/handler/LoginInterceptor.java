package com.cz.gameplat.web.handler;

import com.cz.framework.LogUtil;
import com.cz.framework.StringUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.web.HttpUtil;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.util.TokenManager;
import com.cz.gameplat.user.service.UserService;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;








public class LoginInterceptor
  extends HandlerInterceptorAdapter
{
  @Autowired
  private UserService userService;
  private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);
  


  private static Set<String> uncheckUrlSet = new HashSet();
  static { uncheckUrlSet.add("/api/user/token");
    uncheckUrlSet.add("/api/notice/queryPageRoll");
    uncheckUrlSet.add("/api/notice/getNoticeJson");
    uncheckUrlSet.add("/api/userFindPwd/save");
    uncheckUrlSet.add("/api/admin/add/dl");
    uncheckUrlSet.add("/api/admin/checkUnique");
    uncheckUrlSet.add("/api/sports/match");
    uncheckUrlSet.add("/api/sports/matchNum");
    uncheckUrlSet.add("/api/sports/matchNumFu");
    uncheckUrlSet.add("/api/sports/getBetConfig");
    uncheckUrlSet.add("/api/sports/getLowRateConfig");
    uncheckUrlSet.add("/api/wechat/wxoauth");
    uncheckUrlSet.add("/api/wechat/requestLogin");
    uncheckUrlSet.add("/api/live/free");
    uncheckUrlSet.add("/api/live/qst");
    uncheckUrlSet.add("/api/live/verifySessionToken");
    uncheckUrlSet.add("/api/live/redirectPage");
    uncheckUrlSet.add("/v/user/login");
    uncheckUrlSet.add("/api/counter/totalNumber");
    uncheckUrlSet.add("/api/activity/getRedEnvelopeInfo");
    uncheckUrlSet.add("/api/activity/queryWelfareDetailList");
    uncheckUrlSet.add("/api/activity/queryUnrealWelfareDetailList");
    uncheckUrlSet.add("/api/activity/getRedEnvelopeType");
    uncheckUrlSet.add("/api/eSports/queryMatchPage");
    uncheckUrlSet.add("/api/eSports/queryMatchCompetition");
    uncheckUrlSet.add("/api/eSports/queryCompetitionOdds");
    uncheckUrlSet.add("/api/eSports/queryMatchResult");
    uncheckUrlSet.add("/api/eSports/queryMatchDate");
    uncheckUrlSet.add("/api/app/error/upload");
    uncheckUrlSet.add("/api/eSports/getOpenStatus");
  }
  


  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws Exception
  {
    String url = request.getRequestURI();
    if (StringUtil.isBlank(url)) {
      url = request.getRequestURI();
    }
    if (url.startsWith("/v/")) {
      return true;
    }
    




    if (url.startsWith("/api/u/game/")) {
      return true;
    }
    
    if ((!uncheckUrlSet.isEmpty()) && 
      (uncheckUrlSet.contains(url))) {
      return true;
    }
    

    if (url.startsWith("/")) {
      url = url.substring(1, url.length());
    }
    
    String token = TokenManager.getRequestToken(request);
    if (StringUtil.isBlank(token)) {
      logger.info("token is null");
      throw new BusinessException("UC/TOKEN_INVALID", "uc.token_invalid", null);
    }
    String loginIp = HttpUtil.getforwardedForIP(request);
    Long uid = TokenManager.getUidByToken(token);
    TokenInfo tokenInfo = this.userService.getTokenInfo(uid);
    if (tokenInfo == null) {
      logger.info("redis tokenInfo is null token=" + token + ",uid=" + uid + ", ip:" + loginIp);
      HttpUtil.removeCookie(request, response, "token");
      throw new BusinessException("UC/TOKEN_INVALID", "uc.token_invalid", null);
    }
    
    if (!tokenInfo.getToken().equals(token)) {
      logger.info("tokenInfo:" + tokenInfo + ",请求IP:" + loginIp + ",现token=" + token);
      HttpUtil.removeCookie(request, response, "token");
      throw new BusinessException("UC/TOKEN_INVALID", "网络连接超时，请重新登录", null);
    }
    
    return true;
  }
  
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    throws Exception
  {
    super.afterCompletion(request, response, handler, ex);
    LogUtil.debug("清空UserThreadLocal");
  }
}
