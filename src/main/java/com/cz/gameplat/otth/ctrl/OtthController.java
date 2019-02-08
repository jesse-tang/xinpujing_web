package com.cz.gameplat.otth.ctrl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cz.framework.JsonUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.http.HttpClient;
import com.cz.framework.http.HttpRespBean;
import com.cz.gameplat.live.core.utils.HttpClientUtils;
import com.cz.gameplat.otth.OtthBusTypes;
import com.cz.gameplat.otth.bean.Otth;
import com.cz.gameplat.otth.service.OtthService;
import com.cz.gameplat.payment.dispatcher.util.EncryptUtil;
import com.cz.gameplat.payment.dispatcher.util.RSAUtils;
import com.cz.gameplat.sys.PlatConfig;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.entity.Config;
import com.cz.gameplat.sys.service.ConfigService;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.UserService;
import com.cz.gameplat.web.interceptor.HY;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping({"/api/otth"})
public class OtthController
{
  private static final Logger logger = LoggerFactory.getLogger(OtthController.class);
  

  @Resource
  private OtthService otthService;
  
  @Resource
  private UserService userService;
  
  @Resource
  private PlatConfig platConfig;
  
  @Resource
  private ConfigService configService;
  

  @RequestMapping(value={"/accept"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void accept(@Valid @RequestBody Otth otth, @HY UserInfo userInfo)
    throws BusinessException
  {
    OtthBusTypes types = OtthBusTypes.get(otth.getBusType());
    if (types == null) {
      throw new BusinessException("OTTH/BUS_TYPE", "业务未接入", null);
    }
    this.otthService.accept(otth, userInfo);
  }
  
  @RequestMapping({"/lucky/login"})
  @ResponseBody
  public String login(@RequestParam(required=false) Integer gameId, @HY Long userId, HttpServletRequest request) throws Exception
  {
    UserInfo info = this.userService.getUserInfo(userId);
    TokenInfo tokenInfo = this.userService.getTokenInfo(userId);
    
    JSONObject params = new JSONObject();
    params.put("userId", info.getUserId());
    params.put("account", info.getAccount());
    params.put("userPaths", info.getSuperPath());
    params.put("token", tokenInfo.getToken());
    params.put("nickName", info.getNickname());
    params.put("code", this.platConfig.getCustomCode());
    params.put("gameId", gameId);
    
    String url = getApiUrl("lucky", "lucky/login");
    String requestBody = params.toString();
    

    Header[] headers = { new BasicHeader("Cookie", request.getHeader("Cookie")), new BasicHeader("plat_code", this.platConfig.getCustomCode()) };
    

    logger.info("Lucky login.<url: {}> <body: {}>", url, requestBody);
    String response = HttpClientUtils.doPost(url, requestBody, headers);
    logger.info("Lucky login response.<body: {}>", response);
    return response;
  }
  

  @RequestMapping(value={"/{server}/{url}"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  public Object post(@PathVariable String server, @PathVariable String url, @RequestBody String body, HttpServletRequest request, HttpServletResponse response)
    throws Exception
  {
    String apiUrl = getApiUrl(server, url);
    
    logger.info("API=" + apiUrl + ":" + body);
    


    Header[] header = { new BasicHeader("Cookie", request.getHeader("Cookie")), new BasicHeader("plat_code", this.platConfig.getCustomCode()) };
    
    return HttpClientUtils.doPost(apiUrl, body, header);
  }
  
  @RequestMapping(value={"/{server}/{url}"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public void get(@PathVariable String url, @PathVariable String server, HttpServletRequest request, HttpServletResponse response)
    throws Exception
  {
    String apiUrl = getApiUrl(server, url);
    Enumeration<String> names = request.getParameterNames();
    Map<String, String> params = new HashMap();
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      params.put(name, request.getParameter(name));
    }
    HttpClient httpClient = HttpClient.build().get(apiUrl);
    httpClient.setPara(params);
    httpClient.addHead("plat_code", this.platConfig.getCustomCode());
    
    HttpRespBean respBean = httpClient.execute();
    handleResponse(respBean, response);
  }
  
  private String getApiUrl(String server, String url) throws BusinessException {
    Config config = this.configService.getByNameAndKey("system_config", server + "_api_url");
    if (config == null) {
      throw new BusinessException("未配服务");
    }
    url = config.getConfigValue() + "/" + url.replace("_", "/");
    return url;
  }
  
  private void handleResponse(HttpRespBean respBean, HttpServletResponse response) throws IOException, BusinessException
  {
    if (respBean.getStatus().intValue() == 200) {
      response.getOutputStream().write(respBean.getRespBody().getBytes("utf-8"));
      response.flushBuffer();
    } else {
      try {
        JSONObject jsonObject = JSON.parseObject(respBean.getRespBody());
        if ((jsonObject.containsKey("code")) && (jsonObject.containsKey("msg"))) {
          throw new BusinessException(jsonObject.getString("code"), jsonObject.getString("msg"), null);
        }
      }
      catch (Throwable e) {
        throw new BusinessException(e.getMessage());
      }
      throw new BusinessException(String.valueOf(respBean.getStatus()), respBean.getRespBody(), null);
    }
  }
  

  @RequestMapping(value={"/acceptTest"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void acceptWithEncryption(HttpServletRequest request, @Valid String p, @HY UserInfo userInfo)
    throws BusinessException
  {
    String tgKey = request.getHeader("tg-key");
    
    String decryptAesKey = RSAUtils.decrypt(tgKey);
    
    String decryptOtthStr = EncryptUtil.decrypt(decryptAesKey, p);
    Otth otth = (Otth)JsonUtil.toObject(decryptOtthStr, Otth.class);
    OtthBusTypes types = OtthBusTypes.get(otth.getBusType());
    if (types == null) {
      throw new BusinessException("OTTH/BUS_TYPE", "业务未接入", null);
    }
    this.otthService.accept(otth, userInfo);
  }
}
