package com.cz.gameplat.web.ctrl;

import com.cz.framework.HmacMD5Signer;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.sys.entity.WxConfig;
import com.cz.gameplat.sys.service.WXConfigService;
import com.cz.gameplat.user.dao.UserInfoDao;
import com.cz.gameplat.user.entity.UserInfo;
import java.util.HashMap;
import javax.annotation.Resource;
import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;






@Validated
@Controller
@RequestMapping({"/wxmp/api/wechat"})
public class WeChatController
{
  @Autowired
  private RedisTemplate<String, String> redisTemplate;
  @Resource
  UserInfoDao userInfoDao;
  @Resource
  WXConfigService wxConfigService;
  
  @RequestMapping(value={"/config"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public WxConfig config(long time, String sign)
    throws BusinessException
  {
    if (System.currentTimeMillis() - time > 300000L) {
      throw new BusinessException("授权已过期");
    }
    HashMap<String, Object> params = new HashMap();
    params.put("TIME", String.valueOf(time));
    String mySign = HmacMD5Signer.sign(params, "QA%qQ$xgANN@fLi0");
    if (!mySign.equals(sign)) {
      throw new BusinessException("未授权访问");
    }
    return this.wxConfigService.queryOne();
  }
  






































































  @RequestMapping(value={"/requestLogin"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  public String checkIsRequestLogin(String mark)
  {
    String s = (String)this.redisTemplate.opsForValue().get(mark);
    if (!TextUtils.isEmpty(s)) {
      UserInfo userInfo = this.userInfoDao.get(s);
      if (userInfo != null) {
        return "";
      }
    }
    return "/wap/index.html";
  }
}
