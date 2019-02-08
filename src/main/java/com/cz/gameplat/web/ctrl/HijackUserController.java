package com.cz.gameplat.web.ctrl;

import javax.annotation.*;
import com.cz.gameplat.user.service.*;
import org.springframework.web.bind.annotation.*;
import eu.bitwalker.useragentutils.*;
import java.util.*;
import com.cz.gameplat.sys.bean.*;
import com.cz.framework.exception.*;
import com.cz.gameplat.user.bean.*;
import com.cz.gameplat.user.entity.*;
import org.apache.commons.lang3.*;
import javax.servlet.http.*;

@RestController
@RequestMapping({ "/v/h-user" })
public class HijackUserController
{
  @Resource
  private HijackUserService hijackUserService;
  @Resource
  private UserService userService;
  private static final String HIJACK_USER_ID_KEY = "HIJACK_USER_ID";

  @RequestMapping(value = { "verify" }, method = { RequestMethod.GET })
  public int getAccountStatus(@RequestParam final String account) {
    return this.hijackUserService.getAccountStatus(account).getValue();
  }

  @RequestMapping(value = { "authenticate" }, method = { RequestMethod.POST })
  public Map<String, Object> authenticate(@RequestParam final String account, @RequestParam final String loginPassword, final String verifyCode, final HijackUserProfile profile, final HttpServletRequest request, final UserAgent userAgent) throws Exception, TransactionException {
    this.matchVerifyCode(verifyCode, request);
    final HijackUserAccountStatus accountStatus = this.hijackUserService.getAccountStatus(account);
    final Map<String, Object> result = new HashMap<String, Object>();
    switch (accountStatus) {
      case UN_REGISTER:
      case UN_COMPLETE: {
        try {
          final Long id = this.hijackUserService.matchOrCreate(account, loginPassword, profile, this.getClientInfo(request, userAgent));
          this.setSessionHijackUserId(request, id);
          result.put("status", HijackUserAccountStatus.UN_COMPLETE.getValue());
          break;
        }
        catch (HijackUserService.HijackUserCompleteException ex) {}
      }
      case EXISTS: {
        final TokenInfo tokenInfo = this.userService.login(account, loginPassword, this.getClientInfo(request, userAgent), "HY");
        result.put("status", HijackUserAccountStatus.EXISTS.getValue());
        result.put("tokenInfo", tokenInfo);
        break;
      }
      default: {
        throw new BusinessException("\u7f51\u7edc\u94fe\u63a5\u8d85\u65f6\uff01");
      }
    }
    return result;
  }

  @RequestMapping(value = { "register" }, method = { RequestMethod.POST })
  public TokenInfo register(final HijackUserProfile profile, final HttpServletRequest request, final UserAgent userAgent) throws Exception, TransactionException {
    final Long id = this.getSessionHijackUserId(request);
    if (id == null) {
      throw new BusinessException("SESSION_EXPIRED", "\u4f1a\u8bdd\u5931\u6548\uff01", (Object[])null);
    }
    try {
      final UserEquipmentVO clientInfo = this.getClientInfo(request, userAgent);
      final HijackUser hijackUser = this.hijackUserService.register(id, profile, clientInfo);
      this.clearSessionHijackUserId(request);
      return this.userService.login(hijackUser.getAccount(), hijackUser.getLoginPassword(), clientInfo, "HY");
    }
    catch (HijackUserService.HijackUserNotFoundException e) {
      throw new BusinessException("SESSION_EXPIRED", "\u4f1a\u8bdd\u5931\u6548\uff01", (Object[])null);
    }
  }

  private UserEquipmentVO getClientInfo(final HttpServletRequest request, final UserAgent userAgent) {
    return UserEquipmentVO.create("", userAgent, request);
  }

  private void matchVerifyCode(final String verifyCode, final HttpServletRequest request) throws Exception {
    final HttpSession session = request.getSession();
    final String sessionCode = (String)session.getAttribute("checkCode");
    session.removeAttribute("checkCode");
    if (!StringUtils.equalsIgnoreCase((CharSequence)verifyCode, (CharSequence)sessionCode)) {
      throw new BusinessException("\u9a8c\u8bc1\u7801\u9519\u8bef\uff01");
    }
  }

  private void setSessionHijackUserId(final HttpServletRequest request, final Long id) {
    request.getSession().setAttribute("HIJACK_USER_ID", (Object)id);
  }

  private Long getSessionHijackUserId(final HttpServletRequest request) {
    return (Long)request.getSession().getAttribute("HIJACK_USER_ID");
  }

  private void clearSessionHijackUserId(final HttpServletRequest request) {
    request.getSession().removeAttribute("HIJACK_USER_ID");
  }
}
