package com.cz.gameplat.web.ctrl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cz.framework.http.HttpClient;
import com.cz.framework.http.HttpRespBean;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.service.WeChatService;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.service.UserService2;
import eu.bitwalker.useragentutils.UserAgent;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;





@Validated
@Controller
@RequestMapping({"/wxmp/api/wechat"})
public class WeChatController2
{
  private String ACCESS_TOKEN_KEY = "access_token_key";
  
  private String OPENID_KEY = "open_id_key";
  
  @Resource
  private WeChatService weChatService;
  
  @Resource
  private UserService2 userService2;
  
  @RequestMapping(value={"/wxoauth"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public String oauth(HttpServletRequest request, String code, String state, UserAgent clientUserAgent)
  {
    String GET_USER_INFO = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
    HttpSession session = request.getSession();
    session.getId();
    String access = (String)session.getAttribute(this.ACCESS_TOKEN_KEY);
    String openId = (String)session.getAttribute(this.OPENID_KEY);
    Long expires_in = (Long)session.getAttribute("expires_in");
    
    Integer spreadInfoId = Integer.valueOf(Integer.parseInt(state));
    UserEquipmentVO clientInfo = UserEquipmentVO.create(null, clientUserAgent, request);
    
    TokenInfo tokenInfo = null;
    if ((StringUtils.isNotEmpty(access)) && (StringUtils.isNotEmpty(openId)) && (expires_in != null) && 
      (System.currentTimeMillis() <= expires_in.longValue())) {
      GET_USER_INFO = GET_USER_INFO.replace("ACCESS_TOKEN", access).replace("OPENID", openId);
      tokenInfo = getUserInfo(GET_USER_INFO, spreadInfoId, clientInfo);
    }
    if (tokenInfo == null) {
      String getAccessTokenUrl = this.weChatService.getAccessToken().replace("CODE", code);
      HttpRespBean tokenResp = HttpClient.build().get(getAccessTokenUrl).execute();
      tokenInfo = getUserInfo(getUserInfoLink(tokenResp, request.getSession()), spreadInfoId, clientInfo);
    }
    try
    {
      return 
        "redirect:/redirect?path=/" + state + "&token=" + URLEncoder.encode(tokenInfo.getToken(), "utf-8");
    } catch (UnsupportedEncodingException e) {}
    return "";
  }
  

  private TokenInfo getUserInfo(String url, Integer spreadInfoId, UserEquipmentVO clientInfo)
  {
    HttpRespBean userResp = HttpClient.build().get(url).execute();
    JSONObject user = JSON.parseObject(userResp.getRespBody());
    if ((user.get("errcode") != null) && (((Integer)user.get("errcode")).intValue() == 40001)) {
      return null;
    }
    String unionid = (String)user.get("unionid");
    String nickName = (String)user.get("nickname");
    return autoLogin(unionid, nickName, spreadInfoId, clientInfo);
  }
  
  private TokenInfo autoLogin(String openId, String nickName, Integer spreadInfoId, UserEquipmentVO clientInfo)
  {
    try
    {
      return this.userService2.wxAutoLogin(openId, nickName, spreadInfoId, clientInfo);
    } catch (Exception e) {
      e.printStackTrace(); }
    return null;
  }
  
  private String getUserInfoLink(HttpRespBean bean, HttpSession session)
  {
    String GET_USER_INFO = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
    JSONObject obj = JSONObject.parseObject(bean.getRespBody());
    String accessToken = (String)obj.get("access_token");
    String openId = (String)obj.get("openid");
    session.setAttribute(this.ACCESS_TOKEN_KEY, accessToken);
    session.setAttribute(this.OPENID_KEY, openId);
    Long currentTime = Long.valueOf(System.currentTimeMillis());
    Long ei = Long.valueOf(((Integer)obj.get("expires_in")).intValue() * 1000L);
    session.setAttribute("expires_in", Long.valueOf(currentTime.longValue() + ei.longValue()));
    GET_USER_INFO = GET_USER_INFO.replace("ACCESS_TOKEN", accessToken).replace("OPENID", openId);
    return GET_USER_INFO;
  }
}
