package com.cz.gameplat.web.ctrl;

import com.cz.framework.QRCodeUtil;
import com.cz.framework.StringUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.web.HttpUtil;
import com.cz.gameplat.sys.entity.DomainConfig;
import com.cz.gameplat.sys.entity.WxConfig;
import com.cz.gameplat.sys.service.DomainConfigService;
import com.cz.gameplat.sys.service.WeChatService;
import com.cz.gameplat.user.entity.SpreadInfo;
import com.cz.gameplat.user.entity.SpreadType;
import com.cz.gameplat.user.service.SpreadInfoService;
import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;




@Controller
public class SpreadIndexCtrl
{
  private static final int COOKIE_EXPIRE_TIME = 108000000;
  @Autowired
  SpreadInfoService spreadInfoService;
  @Resource
  WeChatService weChatService;
  @Resource
  DomainConfigService domainConfigService;
  
  @RequestMapping(value={"/{code:[0-9]{1,9}}"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public String index(@PathVariable("code") Integer code, HttpServletRequest request, HttpServletResponse response)
  {
    SpreadInfo spreadInfo = this.spreadInfoService.getById(code);
    if (spreadInfo != null) {
      HttpUtil.setCookie(request, response, "spreadCode", spreadInfo.getCode(), 108000000);
      HttpUtil.setCookie(request, response, "spreadId", String.valueOf(code), 108000000);
    }
    
    this.spreadInfoService.addSpreadCodeVisitCount(code);
    
    String token = HttpUtil.getCookie(request, "token");
    if ((HttpUtil.isWeChatClient(request)) && (StringUtil.isBlank(token)))
    {
      WxConfig wxConfig = this.weChatService.getWxConfig();
      String baseUrl = HttpUtil.getBaseUrl(request);
      if ((wxConfig != null) && (baseUrl.equalsIgnoreCase(wxConfig.getRedirectUri()))) {
        String authorize = this.weChatService.getAuthorize(code + "");
        return "redirect:" + authorize;
      }
    }
    
    int spreadTypeId = spreadInfo != null ? spreadInfo.getSpreadType().intValue() : 0;
    SpreadType spreadType = this.spreadInfoService.getSpreadTypeById(spreadTypeId);
    if (HttpUtil.isMobileClient(request)) {
      return "redirect:" + spreadType.getWapPath();
    }
    try {
      return "redirect:/redirect?path=" + URLEncoder.encode(spreadType.getWebPath(), "utf-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    
    return "redirect:/";
  }
  
  @RequestMapping(value={"/domain_config"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public DomainConfig queryDomainSpreadInfo(HttpServletRequest request) throws BusinessException {
    String host = request.getServerName();
    String[] hostArray = host.split("\\.");
    if (hostArray.length > 2) {
      host = String.format("%s.%s", new Object[] { hostArray[(hostArray.length - 2)], hostArray[(hostArray.length - 1)] });
    }
    return this.domainConfigService.queryByDomain(host);
  }
  
  @RequestMapping(value={"/redirect"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public void redirect(String path, String token, HttpServletRequest request, HttpServletResponse response) {
    try {
      if ((path != null) && (!path.matches("[-/a-zA-Z0-9._#]+(\\?[a-zA-Z0-9%=_]+)*"))) {
        path = "/";
      }
      if (path == null) {
        String spreadId = HttpUtil.getCookie(request, "spreadId");
        if ((spreadId != null) && (spreadId.matches("[0-9]+"))) {
          SpreadInfo spreadInfo = this.spreadInfoService.getById(Integer.valueOf(Integer.parseInt(spreadId)));
          if (spreadInfo != null) {
            SpreadType spreadType = this.spreadInfoService.getSpreadTypeById(spreadInfo.getSpreadType().intValue());
            if (HttpUtil.isMobileClient(request)) {
              path = spreadType.getWapPath();
            } else {
              path = spreadType.getWebPath();
            }
          }
        }
      }
      if (path == null) {
        path = "/";
      }
      if (token != null) {
        HttpUtil.setCookie(request, response, "token", token, 108000000);
      }
      if (HttpUtil.isMobileClient(request)) {
        response.getWriter().println("<!DOCTYPE html><html><head><title></title></head><body><script type=\"text/javascript\">window.location.href=\"" + path + "\";</script></body></html>");
      } else {
        response.getWriter().println("<!DOCTYPE html><html><head><title></title></head><body><script type=\"text/javascript\">window.sessionStorage&&window.sessionStorage.setItem(\"currentUrl\",\"" + path + "\"),window.location.href=\"/\";</script></body></html>");
      }
      response.setContentType("text/html");
      response.flushBuffer();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  

  @RequestMapping(value={"/api/qrcode"}, method={org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
  public void makeQRCode(String content, @RequestParam(value="download", defaultValue="false") boolean download, HttpServletResponse response)
  {
    try
    {
      if (download) {
        response.addHeader("Content-Disposition", "attachment; filename=\"qrcode.jpg\"");
      }
      response.setContentType("image/jpg");
      QRCodeUtil.make(content, 380, 380, response.getOutputStream());
      response.flushBuffer();
    } catch (WriterException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
