package com.cz.gameplate.web.game.ctrl;

import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.entity.TraceOrderInfo;
import com.cz.gameplat.game.entity.TraceOrderQueryReq;
import com.cz.gameplat.game.entity.TraceTurnInfo;
import com.cz.gameplat.game.service.TraceOrderInfoService;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.web.interceptor.HY;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
















@Controller
@RequestMapping({"/api/trace"})
public class TraceController
{
  @Resource
  private TraceOrderInfoService traceOrderInfoService;
  
  @RequestMapping(value={"/query"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<TraceOrderInfo> query(TraceOrderQueryReq po, boolean joinSubs, String subAccount, PageBean pageBean, @HY UserInfo user)
    throws Exception
  {
    if (user == null) {
      return null;
    }
    po.setDoSum(true);
    return this.traceOrderInfoService.slaveQuery(user, po, joinSubs, subAccount, pageBean);
  }
  
  @RequestMapping(value={"/querySingle"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public TraceOrderInfo querySingle(@HY UserInfo user, String orderNo) throws Exception {
    if (user == null) {
      return null;
    }
    return this.traceOrderInfoService.queryByOrderNo(orderNo);
  }
  
  @RequestMapping(value={"/turn"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<TraceTurnInfo> turn(String traceOrderNo) throws BusinessException {
    if (StringUtils.isEmpty(traceOrderNo)) {
      throw new BusinessException("PARAMS/ERROR", "参数不正确", null);
    }
    return this.traceOrderInfoService.query(traceOrderNo);
  }
}
