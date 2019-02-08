package com.cz.gameplat.web.ctrl;

import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.gameplat.user.bean.QueryPushMessage;
import com.cz.gameplat.user.entity.PushMessage;
import com.cz.gameplat.user.enums.MessageEnum;
import com.cz.gameplat.user.enums.ReadStatus;
import com.cz.gameplat.user.service.PushMessageService;
import com.cz.gameplat.web.interceptor.HY;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


















@Validated
@Controller
@RequestMapping({"/api/pushMessage"})
public class PushMessageWebController
{
  @Resource
  private PushMessageService pushMessageService;
  
  @RequestMapping(value={"/modifyReadStatus"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Integer modifyReadStatus(@NotNull(message="{NoNull}") Long id, @HY Long userId)
    throws Exception
  {
    this.pushMessageService.modifyReadStatus(id);
    
    QueryPushMessage queryPushMessage = new QueryPushMessage();
    queryPushMessage.setUserId(userId);
    queryPushMessage.setReadStatus(ReadStatus.READ_STATUS_NO.getValue());
    queryPushMessage.setAcceptRemoveFlag(MessageEnum.MESSAGE_REMOVE_NO.getValue());
    return Integer.valueOf(this.pushMessageService.slaveQueryNotReadCount(queryPushMessage));
  }
  



  @RequestMapping(value={"/remove"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public void remove(@NotNull(message="{NoNull}") Long id)
    throws Exception
  {
    this.pushMessageService.removeWeb(id);
  }
  



  @RequestMapping(value={"/queryPage"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<PushMessage> queryPage(@HY Long userId, PageBean pageBean)
    throws Exception
  {
    return this.pushMessageService.slaveQueryPageWeb(userId, pageBean);
  }
  



  @RequestMapping(value={"/queryAll"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<PushMessage> queryPage(@HY Long userId)
    throws Exception
  {
    return this.pushMessageService.slaveQueryAllWeb(userId);
  }
  



  @RequestMapping(value={"/queryPop"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PushMessage queryPop(@HY Long userId)
    throws Exception
  {
    return this.pushMessageService.queryPop(userId);
  }
}
