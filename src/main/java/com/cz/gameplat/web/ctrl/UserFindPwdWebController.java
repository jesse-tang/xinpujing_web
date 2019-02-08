package com.cz.gameplat.web.ctrl;

import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.service.UserFindPwdService;
import eu.bitwalker.useragentutils.UserAgent;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;






















@Validated
@Controller
@RequestMapping({"/api/userFindPwd"})
public class UserFindPwdWebController
{
  @Resource
  private UserFindPwdService userFindPwdService;
  
  @RequestMapping(value={"/save"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void save(@NotNull(message="{NoNull}") String userAccount, @NotNull(message="{NoNull}") String cashPassword, @NotNull(message="{NoNull}") String yzmNum, @RequestParam(value="userAgent", required=false) String userAgentString, UserAgent clientUserAgent, HttpServletRequest request)
    throws Exception
  {
    this.userFindPwdService.save(userAccount, cashPassword, yzmNum, 
      UserEquipmentVO.create(userAgentString, clientUserAgent, request), request);
  }
}
