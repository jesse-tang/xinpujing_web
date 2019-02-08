package com.cz.gameplate.web.game.ctrl;

import com.cz.framework.DateUtil;
import com.cz.framework.StringUtil;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.entity.UserBet;
import com.cz.gameplat.game.service.UserBetService;
import com.cz.gameplat.report.bean.BetReportVo;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.web.interceptor.HY;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;






@RestController
@RequestMapping({"/api/cp/records"})
public class BetRecordController
{
  @Resource
  private UserBetService userBetService;
  private SimpleDateFormat dateFormat = DateUtil.simpleDateFormat("yyyy-MM-dd");
  





  @RequestMapping(value={"/todayList"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public PageData<UserBet> todayList(@HY UserInfo user, @RequestParam(required=false) Integer status, @RequestParam(required=false) String subAccount, @RequestParam(required=false) Integer model, @RequestParam(required=false) Integer gameId, PageBean pageBean)
  {
    return this.userBetService.slaveQueryTodayBetRecordList(user, gameId, subAccount, model, status, pageBean);
  }
  
  @RequestMapping(value={"historyDayReport"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public List<BetReportVo> historyDayReport(@HY UserInfo user) {
    return this.userBetService.slaveQueryHistoryDayReport(user);
  }
  



  @RequestMapping(value={"/historyList"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public PageData<UserBet> historyList(@HY UserInfo user, String date, @RequestParam(required=false) Integer status, @RequestParam(required=false) String subAccount, @RequestParam(required=false) Integer model, @RequestParam(required=false) Integer gameId, PageBean pageBean)
    throws BusinessException
  {
    try
    {
      if (StringUtil.isBlank(date)) {
        throw new BusinessException("时间不能为空");
      }
      Date startDate = this.dateFormat.parse(date);
      return this.userBetService.slaveQueryHistoryBetRecordList(user, startDate, status, subAccount, gameId, model, pageBean);
    } catch (ParseException e) {
      throw new BusinessException("时间格式错误");
    }
  }
}
