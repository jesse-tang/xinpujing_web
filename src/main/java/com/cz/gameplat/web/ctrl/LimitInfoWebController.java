package com.cz.gameplat.web.ctrl;

import com.cz.gameplat.sys.entity.LimitInfo;
import com.cz.gameplat.sys.service.LimitInfoService;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;













@Validated
@Controller
@RequestMapping({"/api/limit"})
public class LimitInfoWebController
{
  @Resource
  private LimitInfoService limitInfoService;
  
  @RequestMapping({"/get"})
  @ResponseBody
  public LimitInfo<?> get(String name)
    throws Exception
  {
    LimitInfo<?> info = this.limitInfoService.get(name);
    return info;
  }
}
