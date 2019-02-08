package com.cz.gameplate.web.game.ctrl;

import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.entity.UserBet;
import com.cz.gameplat.game.entity.UserBetHisRep;
import com.cz.gameplat.game.service.UserBetReportService;
import com.cz.gameplat.game.service.UserBetService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;





















@Controller
@RequestMapping({"/v/report"})
public class UserReportController
{
  @Resource
  private UserBetReportService userBetReportService;
  @Resource
  private UserBetService userBetService;
  
  @RequestMapping(method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<UserBetHisRep> getReport(@HY Long userId)
  {
    if (userId == null) {
      return null;
    }
    return this.userBetReportService.queryReportByUserId(userId);
  }
  






  @RequestMapping(value={"/day"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<UserBetHisRep> getReport(Date startDate, @HY Long userId)
    throws BusinessException
  {
    if (userId == null) {
      return null;
    }
    if (startDate == null) {
      throw new BusinessException("请选择时间");
    }
    return this.userBetReportService.queryReportByUserId(userId, startDate);
  }
  



  @RequestMapping(value={"/getLatelyBetInfo/{gameId}"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<UserBet> getLatelyBetInfo(@PathVariable Integer gameId, @HY Long userId, Integer model)
  {
    if ((userId == null) || (gameId == null) || (model == null)) {
      return Collections.emptyList();
    }
    return this.userBetService.getLatelyBetInfo(gameId, userId, model, 10);
  }
}
