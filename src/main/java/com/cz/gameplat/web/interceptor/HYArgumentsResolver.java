package com.cz.gameplat.web.interceptor;

import com.cz.framework.LogUtil;
import com.cz.framework.StringUtil;
import com.cz.gameplat.sys.bean.TokenInfo;
import com.cz.gameplat.sys.util.TokenManager;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.UserService;
import java.lang.reflect.Field;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;









public class HYArgumentsResolver
  implements HandlerMethodArgumentResolver
{
  private static Logger logger = LoggerFactory.getLogger(HYArgumentsResolver.class);
  
  @Resource
  private UserService userService;
  

  public boolean supportsParameter(MethodParameter parameter)
  {
    return parameter.getParameterAnnotation(HY.class) != null;
  }
  
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
    throws Exception
  {
    HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest(HttpServletRequest.class);
    HttpServletResponse response = (HttpServletResponse)webRequest.getNativeResponse(HttpServletResponse.class);
    Class<?> paramType = parameter.getParameterType();
    
    HY sessionAnnotation = (HY)parameter.getParameterAnnotation(HY.class);
    if (sessionAnnotation != null) {
      Long uid = getUid(request, response);
      if (uid == null) {
        return null;
      }
      
      UserInfo userInfo = this.userService.getUserInfo(uid);
      if (userInfo == null) {
        return null;
      }
      if (UserInfo.class == paramType) {
        return userInfo;
      }
      String parameterName = sessionAnnotation.value();
      if (StringUtils.isEmpty(parameterName)) {
        parameterName = parameter.getParameterName();
      }
      
      if (parameterName.equals("userId")) {
        return userInfo.getUserId();
      }
      
      Field field = FieldUtils.getDeclaredField(UserInfo.class, parameterName, true);
      if (field != null) {
        return field.get(userInfo);
      }
      logger.error("错误的参数名称：" + parameterName + "，请查看" + UserInfo.class);
    }
    

    return null;
  }
  
  private Long getUid(HttpServletRequest request, HttpServletResponse response) {
    String token = TokenManager.getRequestToken(request);
    
    if (StringUtil.isBlank(token)) {
      return null;
    }
    try
    {
      Long uid = TokenManager.getUidByToken(token);
      if (uid == null) {
        return null;
      }
      TokenInfo tokenInfo = this.userService.getTokenInfo(uid);
      if (tokenInfo == null) {
        return null;
      }
      




      return tokenInfo.getUid();
    }
    catch (Exception e) {
      LogUtil.error("SessionArgumentsResolver, getUid error", e);
    }
    
    return null;
  }
}
