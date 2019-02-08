package com.cz.gameplat.web.ctrl;

import com.alibaba.fastjson.JSONObject;
import com.cz.framework.web.HttpUtil;
import com.cz.gameplat.payment.service.RechargeOrderService;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;









@RestController
@RequestMapping({"/api/payment"})
public class ExRechargeController
{
  @Resource
  private RechargeOrderService rechargeOrderService;
  
  @RequestMapping({"onlinePayAsyncCallback/{orderNo}"})
  public String onlinePayAsyncCallback(@PathVariable String orderNo, @RequestBody(required=false) String requestBody, HttpServletRequest request, String userAgentString, UserAgent clientUserAgent)
    throws Exception
  {
    JSONObject headerJson = new JSONObject();
    Enumeration e = request.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = (String)e.nextElement();
      String value = request.getHeader(name);
      headerJson.put("name", value);
    }
    return 
      this.rechargeOrderService.onlinePayAsyncCallback(orderNo, getRequestParameterMap(request), requestBody, 
      HttpUtil.getforwardedForIP(request), "/api/recharge/onlinePayAsyncCallback/${orderNo}", "/api/recharge/onlinePaySyncCallback/${orderNo}", 
      

      UserEquipmentVO.create(userAgentString, clientUserAgent, request), headerJson);
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
}
