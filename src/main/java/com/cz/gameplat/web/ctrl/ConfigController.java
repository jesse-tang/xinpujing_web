package com.cz.gameplat.web.ctrl;

import com.cz.gameplat.sys.bean.RegConfig;
import com.cz.gameplat.sys.entity.Config;
import com.cz.gameplat.sys.service.ConfigService;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


















@Validated
@Controller
@RequestMapping({"/api/config"})
public class ConfigController
{
  @Resource
  private ConfigService configService;
  
  @RequestMapping({"/save"})
  @ResponseBody
  public void save(RegConfig regConfig)
    throws Exception
  {
    this.configService.save(regConfig);
  }
  



  @RequestMapping({"/modify"})
  @ResponseBody
  public void modify(RegConfig regConfig)
    throws Exception
  {
    this.configService.modify(regConfig);
  }
  




  @RequestMapping({"/enable"})
  @ResponseBody
  public void modifyEnable(@NotNull(message="{NoNull}") Long configId, @NotNull(message="{NoNull}") Integer enableStatus)
    throws Exception
  {
    this.configService.modifyEnable(configId, enableStatus);
  }
  



  @RequestMapping({"/remove"})
  @ResponseBody
  public void remove(@NotNull(message="{NoNull}") Long configId)
    throws Exception
  {
    this.configService.remove(configId);
  }
  



  @RequestMapping({"/levelManager"})
  @ResponseBody
  public void levelManager(@NotNull(message="{NoNull}") Integer level)
    throws Exception
  {
    this.configService.levelManager(level);
  }
  



  @RequestMapping({"/queryAll/{configName}"})
  @ResponseBody
  public List<Config> queryAll(@PathVariable String configName)
    throws Exception
  {
    return this.configService.queryAll(configName);
  }
  



  @RequestMapping({"/getAll/{configName}"})
  @ResponseBody
  public List<Config> getAll(@PathVariable String configName)
    throws Exception
  {
    return this.configService.getList(configName);
  }
  



  @RequestMapping({"/getRechType/{configName}"})
  @ResponseBody
  public List<Config> getRechType(@PathVariable String configName)
    throws Exception
  {
    return this.configService.getRechType(configName);
  }
  




  @RequestMapping(value={"/getAll"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, List<Config>> getAll()
    throws Exception
  {
    return this.configService.getAll();
  }
  






  @RequestMapping(value={"/getOne"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Config getOne(String configName, String configKey)
  {
    return this.configService.getByNameAndKey(configName, configKey);
  }
}
