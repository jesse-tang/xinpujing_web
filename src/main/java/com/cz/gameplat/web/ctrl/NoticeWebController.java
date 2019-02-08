package com.cz.gameplat.web.ctrl;

import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.gameplat.sys.util.CreateJsonUtil;
import com.cz.gameplat.user.bean.QueryNotice;
import com.cz.gameplat.user.entity.Notice;
import com.cz.gameplat.user.service.NoticeService;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

















@Validated
@Controller
@RequestMapping({"/api/notice"})
public class NoticeWebController
{
  @Resource
  private NoticeService noticeService;
  @Resource
  private CreateJsonUtil createJsonUtil;
  
  @RequestMapping(value={"/queryPage"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<Notice> queryPage(QueryNotice queryNotice, PageBean pageBean)
    throws Exception
  {
    return this.noticeService.queryPageWeb(queryNotice, pageBean);
  }
  




  @RequestMapping(value={"/queryPageRoll"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public PageData<Notice> queryPageRoll(QueryNotice queryNotice, PageBean pageBean)
    throws Exception
  {
    return this.noticeService.queryPageWeb(queryNotice, pageBean);
  }
  





  @RequestMapping(value={"/queryAll"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<Notice> queryAll(QueryNotice queryNotice)
    throws Exception
  {
    return this.noticeService.queryAll(queryNotice);
  }
  





  @RequestMapping(value={"/getNoticeJson"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> getNoticeJson()
    throws Exception
  {
    this.createJsonUtil.isExistNoticeJsonFile();
    return this.noticeService.getNoticeJsonWeb();
  }
  




  @RequestMapping(value={"/getAllNoticeJson"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public Map<String, Object> getAllNoticeJson()
    throws Exception
  {
    this.createJsonUtil.isExistNoticeJsonFile();
    Map<String, Object> data = this.noticeService.getNoticeListJsonWeb();
    return data;
  }
}
