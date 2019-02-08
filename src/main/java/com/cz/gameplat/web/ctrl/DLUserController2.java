package com.cz.gameplat.web.ctrl;

import com.cz.framework.exception.BusinessException;
import com.cz.framework.valid.Save;
import com.cz.gameplat.user.bean.UserEquipmentVO;
import com.cz.gameplat.user.bean.UserInfoVO;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.user.service.RebateService;
import com.cz.gameplat.user.service.UserService2;
import com.cz.gameplat.web.interceptor.HY;
import eu.bitwalker.useragentutils.UserAgent;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;



@Validated
@Controller
@RequestMapping({"/api/dl"})
public class DLUserController2
{
  @Resource
  private UserService2 userService2;
  @Resource
  private RebateService rebateService;
  
  @RequestMapping(value={"/add"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void add(@Validated({Save.class}) UserInfoVO po, @HY Long userId, HttpServletRequest request, UserAgent userAgent)
    throws Exception
  {
    this.userService2.addByDl(po.getUserInfo(), userId, UserEquipmentVO.create(null, userAgent, request));
  }
  
  @RequestMapping(value={"/updateRebate"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public void updateRebate(@NotNull Long userId, @NotNull Double rebate, @HY UserInfo dl) throws Exception
  {
    if (userId == dl.getUserId()) {
      throw new BusinessException("不能修改自己的返点！");
    }
    this.userService2.updateRebateByDl(userId, rebate, dl.getUserId());
  }
  
  @RequestMapping(value={"/getDlSubRebateRange"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public double[] getDlSubRebateRange(@HY UserInfo dl) throws Exception {
    return this.rebateService.getDlSubRebateRange(dl.getUserId(), true);
  }
  
  @RequestMapping(value={"/getRebateRange"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public double[] getRebateRange(Long userId) throws Exception {
    return this.rebateService.getHyRebateRange(userId);
  }
}
