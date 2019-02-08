package com.cz.gameplat.web.handler;

import com.cz.framework.AppContext;
import com.cz.framework.PropertyHandler;
import com.cz.framework.StringUtil;
import com.cz.framework.WafProperties;
import com.cz.gameplat.game.mamager.GameRegexpManager;
import com.cz.gameplat.sys.util.QueueManager;
import java.io.PrintStream;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

@Component
public class WebAppService
  implements ApplicationContextAware, ServletContextAware
{
  @Resource
  private QueueManager queueManager;
  
  public WebAppService()
  {
    System.out.println("--------------WebAppService--------------------------------");
  }
  
  public void setApplicationContext(ApplicationContext context) throws BeansException
  {
    System.out.println("--------------setApplicationContext--------------------------------");
  }
  
  public void setServletContext(ServletContext servletContext)
  {
    String realPath = servletContext.getRealPath("");
    realPath = realPath.replace("\\", "/");
    System.out.println("--------------realPath--------------------------------" + realPath);
    AppContext.getInstance().setSysRealPath(realPath);
    String fileName = WafProperties.getProperty("key.file");
    String key = null;
//    try
//    {
//      PropertyHandler hanler = new PropertyHandler(fileName);
//
//      key = hanler.getValue("key.fk");
//    } catch (Exception e) {
//      e.printStackTrace();
//      System.out.println("--------------读取key文件出错--------------");
//      System.exit(0);
//    }
//    if (StringUtil.isBlank(key)) {
//      System.out.println("--------------读取key错误--------------");
//      System.exit(0);
//    }
//    AppContext.getInstance().setAppKey(key);
    try {
      GameRegexpManager.init();
    } catch (Exception e) {
      System.out.println("--------------初使化游戏正则表达式失败--------------");
      System.exit(0);
    }
    
    this.queueManager.startLogUserLoginThread();
    this.queueManager.startLogOperateThread();
    System.out.println("--------------开启日志记录线程--------------");
  }
}
