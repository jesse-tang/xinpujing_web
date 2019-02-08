package com.cz.gameplat.web.ctrl;

import com.cz.gameplat.report.bean.DayReportVO;
import com.cz.gameplat.report.bean.QueryDayReportReq;
import com.cz.gameplat.report.service.DayReportService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;






@Controller
@RequestMapping({"/api/report"})
public class ReportController
{
  @Resource
  private DayReportService dayReportService;
  
  @RequestMapping(value={"/personalReport"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, DayReportVO> personalReport(@HY Long userId, QueryDayReportReq req)
    throws Exception
  {
    req.setUserId(userId);
    
    return this.dayReportService.slaveQueryDayReport(req);
  }
}
