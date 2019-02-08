package com.cz.gameplat.web.ctrl;

import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.gameplat.user.entity.SpreadInfo;
import com.cz.gameplat.user.entity.SpreadType;
import com.cz.gameplat.user.service.SpreadInfoService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.List;
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
public class SpreadInfoCtrl
{
  @Resource
  private SpreadInfoService spreadInfoService;
  
  @RequestMapping(value={"/queryPageByAgentId"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<SpreadInfo> queryPageByAgentId(@HY Long userId, PageBean pageBean)
    throws Exception
  {
    return this.spreadInfoService.queryPageByAgentId(userId, pageBean);
  }
  
  @RequestMapping(value={"/spreadTypes"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<SpreadType> spreadTypes() {
    return this.spreadInfoService.spreadTypes();
  }
  
  @RequestMapping(value={"/getByAgentId"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public SpreadInfo get(@HY Long userId, @NotNull Long id) throws Exception {
    return this.spreadInfoService.get(userId, id);
  }
  


























































  @RequestMapping(value={"/removeByAgentId"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public int remove(@HY Long userId, @Valid @NotNull Long id)
    throws Exception
  {
    return this.spreadInfoService.remove(userId, id);
  }
}
