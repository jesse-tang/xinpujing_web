package com.cz.gameplat.web.ctrl;

import com.cz.framework.ValidImgUtil;
import com.cz.framework.ValidateVoBean;
import com.cz.gameplat.game.mamager.LhcManager;
import com.cz.gameplat.sys.entity.Config;
import com.cz.gameplat.sys.service.ConfigService;
import com.cz.gameplat.verification.Kcaptcha;
import com.cz.gameplat.web.interceptor.HY;
import com.google.code.kaptcha.Producer;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;















@Validated
@Controller
@RequestMapping({"/v"})
public class ApiWebController
{
  @Resource
  ConfigService configService;
  @Resource
  LhcManager lhcManager;
  
  @RequestMapping(value={"/vCode"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public void getValidateCode(HttpServletRequest request, HttpServletResponse response, HttpSession session)
    throws Exception
  {
    Config vcodeStyle = this.configService.getByKey("vcode_style");
    boolean isSample = true;
    if ((vcodeStyle != null) && (vcodeStyle.getConfigValue() != null)) {
      isSample = !vcodeStyle.getConfigValue().equals("1");
    }
    Kcaptcha kcaptcha = new Kcaptcha(isSample);
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0L);
    response.setContentType("image/jpeg");
    

    Cookie cookie = new Cookie("JSESSIONID", session.getId());
    cookie.setPath("/");
    cookie.setSecure(false);
    cookie.setHttpOnly(false);
    response.addCookie(cookie);
    String capText = kcaptcha.getKaptchaProducer().createText();
    BufferedImage bi = kcaptcha.getKaptchaProducer().createImage(capText);
    ServletOutputStream out = response.getOutputStream();
    try {
      request.getSession().setAttribute(kcaptcha.getSessionKeyValue(), capText);
      request.getSession().setAttribute(kcaptcha.getSessionKeyDateValue(), new Date());
    } catch (Exception e) {
      e.printStackTrace();
    }
    ImageIO.write(bi, "jpeg", out);
  }
  







  @RequestMapping(value={"/vUserCode"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public void getValidateCode(@HY Long userId, HttpSession session, HttpServletResponse response)
    throws Exception
  {
    ValidateVoBean validateVoBean = ValidImgUtil.getValidateCode();
    String key = "checkCode_" + userId;
    session.setAttribute(key, validateVoBean.getValidateCode());
    

    Cookie cookie = new Cookie("JSESSIONID", session.getId());
    cookie.setPath("/");
    cookie.setSecure(false);
    cookie.setHttpOnly(false);
    response.addCookie(cookie);
    
    OutputStream os = response.getOutputStream();
    ImageIO.write(validateVoBean.getImage(), "JPEG", os);
    os.close();
  }
  

















  @RequestMapping(value={"/lhc/info"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> getLhcInfo()
  {
    Map<String, Object> result = new HashMap();
    result.putAll(this.lhcManager.getInfo());
    
    return result;
  }
  





  @RequestMapping({"/config/maintain"})
  @ResponseBody
  public List<Config> queryMaintainConfig()
  {
    return this.configService.querySlaveMaintainConfig();
  }
}
