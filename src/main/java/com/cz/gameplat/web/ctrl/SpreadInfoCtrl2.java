package com.cz.gameplat.web.ctrl;

import com.cz.gameplat.game.bean.WebSpreadInfoReq;
import com.cz.gameplat.user.service.SpreadInfoService2;
import com.cz.gameplat.web.interceptor.HY;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Validated
@Controller
@RequestMapping({"/api/spreadInfo"})
public class SpreadInfoCtrl2
{
  @Resource
  private SpreadInfoService2 spreadInfoService2;
  
  @RequestMapping(value={"/createByAgentId"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public Long create(@HY Long userId, @Valid WebSpreadInfoReq webSpreadInfoReq) throws Exception
  {
    return this.spreadInfoService2.addByDl(userId, webSpreadInfoReq);
  }
  
  @RequestMapping(value={"/updateByAgentId"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public int update(@HY Long userId, @NotNull Long id, @Valid WebSpreadInfoReq webSpreadInfoReq) throws Exception
  {
    return this.spreadInfoService2.updateByDl(userId, id, webSpreadInfoReq);
  }
}
