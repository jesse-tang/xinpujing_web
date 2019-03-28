package com.cz.gameplat.user.service;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.user.dao.*;
import com.cz.gameplat.sys.service.*;
import com.cz.gameplat.payment.service.*;
import com.cz.gameplat.game.cache.*;
import com.cz.gameplat.sys.*;
import com.cz.gameplat.user.bean.*;
import com.cz.gameplat.sys.limit.*;
import com.cz.framework.exception.*;
import org.apache.commons.lang3.*;
import com.cz.gameplat.user.entity.*;
import com.cz.gameplat.sys.bean.*;
import org.springframework.cache.annotation.*;
import com.cz.framework.web.*;
import com.cz.gameplat.sys.util.*;
import java.util.*;
import com.cz.gameplat.user.enums.*;
import com.cz.gameplat.payment.constant.*;
import com.cz.gameplat.lottery.enums.*;
import com.cz.framework.*;
import com.cz.gameplat.sys.enums.*;
import com.cz.gameplat.sys.entity.*;
import org.slf4j.*;

@Service
public class UserService2
{
  private static final Logger LOGGER;
  private static final String FORCE_HY_LEVEL = "1";
  private static final double DEFAULT_BALANCE_TEST = 2000.0;
  private static final String DEFAULT_PASSWORD_TEST = "a123456";
  private static final String DEFAULT_PASSWORD_WX;
  private static final String DEFAULT_FUND_PWD_WX = "8888";
  @Resource
  private UserInfoDao userInfoDao;
  @Resource
  private UserExtInfoDao userExtInfoDao;
  @Resource
  private LimitInfoService limitInfoService;
  @Resource
  private DLService dlService;
  @Resource
  private SpreadInfoService spreadInfoService;
  @Resource
  private RebateService rebateService;
  @Resource
  private DomainConfigService domainConfigService;
  @Resource
  private ConfigService configService;
  @Resource
  private RechargeOrderService rechargeOrderService;
  @Resource
  private DLRebateCache dlRebateCache;
  @Resource
  private PlatConfig platConfig;

  public UserInfo register(final RegUserInfo regInfo, final String domain, final UserEquipmentVO clientInfo, final RegisterLimit registerLimit) throws Exception {
    final UserInfo userInfo = new UserInfo();
    try {
      BeanUtils.copyProperties((Object)userInfo, (Object)regInfo);
    }
    catch (Exception e) {
      UserService2.LOGGER.error("\u6ce8\u518c\u4fe1\u606f\u5f02\u5e38 => {}", (Object)JsonUtil.toJson((Object)regInfo), (Object)e);
      throw new BusinessException("\u6ce8\u518c\u4fe1\u606f\u5f02\u5e38\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb\uff01");
    }
    this.validateRegisterIp(registerLimit, clientInfo.getIpAddress());
    final SpreadInfo spreadInfo = this.validateSpreadInfo(domain, regInfo.getIntrCode(), clientInfo.isMobileDevice(), clientInfo.getUserAgent());
    userInfo.setRegWay(RegisterTypes.ONLINE.getValue());
    this.setClientInfo(userInfo, clientInfo);
    this.createUserInfo(userInfo, spreadInfo, true);
    final UserExtInfo userExtInfo = this.createUserExtInfo(userInfo, regInfo.getFundPwd(), 0.0);
    final Double discountAmount = registerLimit.getSendMenoy();
    if (discountAmount != null && discountAmount > 0.0) {
      this.registerDiscount(userInfo, userExtInfo, discountAmount, registerLimit.getDmlMenoy(), "\u6ce8\u518c\u9001\u5f69\u91d1");
    }
    this.flushDlRebateCache(userInfo);
    return userInfo;
  }

  public UserInfo registerForTest(final String password, final UserEquipmentVO clientInfo, final RegisterLimit registerLimit) throws Exception {
    final SpreadInfo spreadInfo = this.validateSpreadInfo("T", null, this.configService.getPlayIntrCode(), clientInfo.isMobileDevice());
    final UserInfo userInfo = new UserInfo();
    userInfo.setType(SysUserTypes.TEST.getCode());
    userInfo.setRegWay(RegisterTypes.ONLINE.getValue());
    this.setClientInfo(userInfo, clientInfo);
    userInfo.setAccount(this.generateTestAccount());
    userInfo.setPassword(StringUtils.isNotBlank((CharSequence)password) ? password : "a123456");
    userInfo.setFullName("\u8bd5\u73a9\u73a9\u5bb6");
    userInfo.setNickname("\u8bd5\u73a9\u73a9\u5bb6");
    this.createUserInfo(userInfo, spreadInfo, false);
    final Double initBalance = registerLimit.getFreeAccountBalance();
    this.createUserExtInfo(userInfo, null, (initBalance != null) ? ((double)initBalance) : 2000.0);
    this.flushDlRebateCache(userInfo);
    return userInfo;
  }

  public UserInfo registerForWx(final String openId, final String nickName, final Integer spreadInfoId, final UserEquipmentVO clientInfo) throws Exception {
    final SpreadInfo spreadInfo = this.spreadInfoService.getById(spreadInfoId);
    if (spreadInfo == null) {
      UserService2.LOGGER.error("\u5fae\u4fe1\u63a8\u5e7f\u7801\u5f02\u5e38 => {}", (Object)spreadInfoId);
      throw new BusinessException("\u5fae\u4fe1\u63a8\u5e7f\u7801\u5f02\u5e38\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb\uff01");
    }
    final UserInfo userInfo = new UserInfo();
    userInfo.setRegWay(RegisterTypes.EXTENSION.getValue());
    this.setClientInfo(userInfo, clientInfo);
    userInfo.setWxUnionid(openId);
    userInfo.setAccount(this.generateWxAccount());
    userInfo.setPassword(UserService2.DEFAULT_PASSWORD_WX);
    userInfo.setNickname(nickName);
    this.createUserInfo(userInfo, spreadInfo, false);
    final UserExtInfo userExtInfo = this.createUserExtInfo(userInfo, "8888", 0.0);
    final RegisterLimit registerLimit = this.limitInfoService.get(LimitEnums.registerLimit.getName(), RegisterLimit.class);
    final Double discountAmount = registerLimit.getWechatBonus();
    if (discountAmount != null && discountAmount > 0.0) {
      this.registerDiscount(userInfo, userExtInfo, discountAmount, registerLimit.getWechatBonusDml(), "\u5fae\u4fe1\u6ce8\u518c\u9001\u5f69\u91d1");
    }
    this.flushDlRebateCache(userInfo);
    return userInfo;
  }

  public UserInfo registerForHijack(final HijackUser hijackUser) throws Exception {
    final UserInfo userInfo = new UserInfo();
    userInfo.setAccount(hijackUser.getAccount());
    userInfo.setPassword(SecurityUtil.crtyMd5(new String[] { hijackUser.getLoginPassword() }));
    userInfo.setNickname(hijackUser.getNickname());
    userInfo.setFullName(hijackUser.getFullName());
    userInfo.setPhone(hijackUser.getPhone());
    userInfo.setWxUnionid(hijackUser.getWechatUnionId());
    userInfo.setWeixin(hijackUser.getWechat());
    userInfo.setQq(hijackUser.getQq());
    userInfo.setEmail(hijackUser.getEmail());
    final SpreadInfo spreadInfo = this.validateSpreadInfo(null, hijackUser.getSrcCode(), hijackUser.getMobileOrTablet(), hijackUser.getUserAgent());
    userInfo.setRegWay(RegisterTypes.HIJACK.getValue());
    userInfo.setRegIp(hijackUser.getClientIp());
    userInfo.setBrowser(hijackUser.getClientBrowser());
    userInfo.setOperatingSystem(hijackUser.getClientOs());
    userInfo.setUserAgent(hijackUser.getUserAgent());
    this.createUserInfo(userInfo, spreadInfo, true);
    final UserExtInfo userExtInfo = this.createUserExtInfo(userInfo, hijackUser.getWithdrawPassword(), 0.0);
    final RegisterLimit registerLimit = this.limitInfoService.get(LimitEnums.registerLimit.getName(), RegisterLimit.class);
    final Double discountAmount = registerLimit.getSendMenoy();
    if (discountAmount != null && discountAmount > 0.0) {
      this.registerDiscount(userInfo, userExtInfo, discountAmount, registerLimit.getDmlMenoy(), "\u5e73\u53f0\u5347\u7ea7\u9001\u5f69\u91d1");
    }
    this.flushDlRebateCache(userInfo);
    return userInfo;
  }

  @Caching(put = { @CachePut(value = { "tokenInfo" }, key = "'user_'+#result.uid", condition = "#result != null"), @CachePut(value = { "online" }, key = "'user_'+#result.uid", condition = "#result != null") })
  public TokenInfo wxAutoLogin(final String openId, final String nickName, final Integer spreadInfoId, final UserEquipmentVO clientInfo) throws Exception {
    UserInfo userInfo = this.userInfoDao.getByWxUnicode(openId);
    if (userInfo == null) {
      userInfo = this.registerForWx(openId, nickName, spreadInfoId, clientInfo);
    }
    return this.wxLogin(userInfo, clientInfo);
  }

  @Caching(put = { @CachePut(value = { "tokenInfo" }, key = "'user_'+#result.uid", condition = "#result != null"), @CachePut(value = { "online" }, key = "'user_'+#result.uid", condition = "#result != null") })
  public TokenInfo wxLogin(final UserInfo userInfo, final UserEquipmentVO clientInfo) throws Exception {
    final Date now = new Date();
    this.userExtInfoDao.updateLoginInfo(clientInfo.getIpAddress(), now, userInfo.getUserId());
    final String token = TokenManager.createToken(userInfo.getUserId(), this.platConfig.getCustomCode(), now, clientInfo.getIpAddress());
    final TokenInfo tokenInfo = new TokenInfo();
    tokenInfo.setUid(userInfo.getUserId());
    tokenInfo.setLoginDate(now);
    tokenInfo.setExpiresIn(1800000);
    tokenInfo.setToken(token);
    tokenInfo.setDeviceType(HttpUtil.parseDeviceType(clientInfo.getUserAgent()));
    tokenInfo.setLoginIp(clientInfo.getIpAddress());
    tokenInfo.setAccount(userInfo.getAccount());
    tokenInfo.setName(StringUtil.isBlank(userInfo.getFullName()) ? userInfo.getNickname() : userInfo.getFullName());
    if (userInfo.getIsDl()) {
      tokenInfo.setType(SysUserTypes.DL.getCode());
    }
    else {
      tokenInfo.setType(userInfo.getType());
    }
    QueueManager.createQueue(clientInfo, userInfo);
    return tokenInfo;
  }

  public void add(final UserInfo userInfo, final UserEquipmentVO clientInfo) throws Exception {
    userInfo.setRegWay(RegisterTypes.ADMIN_ADD.getValue());
    this.setClientInfo(userInfo, clientInfo);
    this.createUserInfo(userInfo, null, true);
    final UserExtInfo userExtInfo = this.createUserExtInfo(userInfo, null, 0.0);
    final RegisterLimit registerLimit = this.limitInfoService.get(LimitEnums.registerLimit.getName(), RegisterLimit.class);
    final Double discountAmount = registerLimit.getSendMenoy();
    if (discountAmount != null && discountAmount > 0.0) {
      this.registerDiscount(userInfo, userExtInfo, discountAmount, registerLimit.getDmlMenoy(), "\u6ce8\u518c\u9001\u5f69\u91d1");
    }
    this.flushDlRebateCache(userInfo);
  }

  public void addByDl(final UserInfo userInfo, final Long dlId, final UserEquipmentVO clientInfo) throws Exception {
    userInfo.setUserId(null);
    userInfo.setSuperId(dlId);
    final RegisterLimit registerLimit = this.limitInfoService.get(LimitEnums.registerLimit.getName(), RegisterLimit.class);
    this.validateUniqueFields(registerLimit, userInfo);
    userInfo.setRegWay(RegisterTypes.DL_ADD.getValue());
    this.setClientInfo(userInfo, clientInfo);
    this.createUserInfo(userInfo, null, true);
    final UserExtInfo userExtInfo = this.createUserExtInfo(userInfo, null, 0.0);
    final Double discountAmount = registerLimit.getSendMenoy();
    if (discountAmount != null && discountAmount > 0.0) {
      this.registerDiscount(userInfo, userExtInfo, discountAmount, registerLimit.getDmlMenoy(), "\u6ce8\u518c\u9001\u5f69\u91d1");
    }
    this.flushDlRebateCache(userInfo);
  }

  public void updateRebate(final Long userId, final Double rebate) throws Exception {
    if (SiteRebateModel.fixed.getValue().equals(this.configService.getsiteRebateModel())) {
      throw new BusinessException("\u8fd4\u70b9\u4e0d\u80fd\u4fee\u6539\uff01");
    }
    this.rebateService.validateHyRebate(userId, rebate);
    this.doUpdateRebate(userId, rebate);
  }

  private void doUpdateRebate(final Long userId, final Double rebate) throws Exception {
    final UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    userInfo.setRebate(rebate);
    this.userInfoDao.updateObject(userInfo);
    this.flushDlRebateCache(userId);
  }

  public void batchUpdateRebate(final Long dlId, final Double rebate) throws Exception {
    if (SiteRebateModel.fixed.getValue().equals(this.configService.getsiteRebateModel())) {
      throw new BusinessException("\u8fd4\u70b9\u4e0d\u80fd\u4fee\u6539\uff01");
    }
    final UserInfo dl = this.userInfoDao.get(dlId);
    if (dl == null || !Boolean.TRUE.equals(dl.getIsDl())) {
      UserService2.LOGGER.error("\u4ee3\u7406ID\u5f02\u5e38 => {}", (Object)dlId);
      throw new BusinessException("\u65e0\u6548\u7684\u4ee3\u7406\uff01");
    }
    this.rebateService.validateHyRebate(dl, rebate);
    final List<UserInfo> subs = this.userInfoDao.getBySuperPath(dl.getSuperPath());
    for (final UserInfo userInfo : subs) {
      this.doUpdateRebate(userInfo.getUserId(), rebate);
    }
  }

  public void updateRebateByDl(final Long userId, final Double rebate, final Long dlId) throws Exception {
    final UserInfo userInfo = this.userInfoDao.get(userId);
    final UserInfo dl = this.userInfoDao.get(dlId);
    if (!StringUtils.startsWith((CharSequence)userInfo.getSuperPath(), (CharSequence)dl.getSuperPath())) {
      UserService2.LOGGER.error("\u4ee3\u7406\u8def\u5f84\u5f02\u5e38 => {}, {}", (Object)userInfo.getSuperPath(), (Object)dl.getSuperPath());
      throw new BusinessException("\u53ea\u80fd\u4fee\u6539\u4e0b\u7ea7\u4f1a\u5458\u8fd4\u70b9\uff01");
    }
    this.rebateService.validateHyRebate(userInfo, rebate);
    this.doUpdateRebate(userId, rebate);
  }

  public void flushDlRebateCache(final Long userId) {
  }

  public void flushDlRebateCache(final UserInfo userInfo) {
  }

  private void createUserInfo(final UserInfo userInfo, final SpreadInfo spreadInfo, final boolean validateAccount) throws Exception {
    if (validateAccount && this.userInfoDao.get(userInfo.getAccount()) != null) {
      throw new BusinessException("UC/ACCOUNT_IS_EXIST", "uc.account_is_exist", (Object[])null);
    }
    if (spreadInfo != null) {
      userInfo.setIntrCode(spreadInfo.getCode());
      userInfo.setSuperId(spreadInfo.getAgentId());
      userInfo.setRebate(spreadInfo.getRebate());
      if (SysUserTypes.TEST.getCode().equals(userInfo.getType())) {
        userInfo.setIsDl(false);
      }
      else {
        userInfo.setIsDl(spreadInfo.getUserType() == 1);
      }
      if (spreadInfo.getUserLevel() != null) {
        userInfo.setHyLevel(String.valueOf(spreadInfo.getUserLevel()));
      }
      this.spreadInfoService.increaseRegistCount(spreadInfo.getCode());
    }
    if (StringUtils.isBlank((CharSequence)userInfo.getHyLevel())) {
      final String hyLevel = this.configService.getDefaultUserLevel();
      userInfo.setHyLevel(StringUtils.isBlank((CharSequence)hyLevel) ? "1" : hyLevel);
    }
    if (SysUserTypes.get(userInfo.getType()).equals(SysUserTypes.Unknown)) {
      userInfo.setType(SysUserTypes.HY.getCode());
    }
    if (userInfo.getIsDl() == null) {
      userInfo.setIsDl(false);
    }
    if (userInfo.getSuperId() == null) {
      throw new BusinessException("\u672a\u8bbe\u7f6e\u4e0a\u7ea7\u4ee3\u7406\uff01");
    }
    final UserInfo dl = this.userInfoDao.get(userInfo.getSuperId());
    if (dl == null || !Boolean.TRUE.equals(dl.getIsDl())) {
      UserService2.LOGGER.error("\u4ee3\u7406ID\u5f02\u5e38 => {}", (Object)userInfo.getSuperId());
      if (spreadInfo != null) {
        UserService2.LOGGER.info("\u63a8\u5e7f\u7801 => {}", (Object)JsonUtil.toJson((Object)spreadInfo));
      }
      throw new BusinessException("\u65e0\u6548\u7684\u4ee3\u7406\uff01");
    }
    this.checkDlNum(dl, userInfo);
    if (SiteRebateModel.fixed.getValue().equals(this.configService.getsiteRebateModel())) {
      userInfo.setRebate(dl.getRebate());
    }
    else {
      this.rebateService.validateDlSubRebate(dl, userInfo.getRebate(), null, spreadInfo == null);
    }
    userInfo.setSuperName(dl.getAccount());
    userInfo.setSuperPath(String.format("%s%s/", dl.getSuperPath(), userInfo.getAccount()));
    userInfo.setDlLevel(dl.getDlLevel() + 1);
    this.dlService.updateDLNum(dl);
    if (userInfo.getFullName() == null) {
      userInfo.setFullName("");
    }
    userInfo.setPassword(this.encryptPassword(userInfo));
    userInfo.setLimitBet(BetLimit.DEFAULT.getValue());
    userInfo.setState(UserStates.DEFAULT.getValue());
    userInfo.setFk(this.generateFk(userInfo));
    userInfo.setAddTime(new Date());
    if (!Integer.valueOf(1).equals(userInfo.getIsDayWage())) {
      userInfo.setIsDayWage(0);
    }
    final Long userId = this.userInfoDao.save(userInfo);
    this.dlRebateCache.put(userInfo.getAccount(), userId);
  }

  private UserExtInfo createUserExtInfo(final UserInfo userInfo, final String fundPwd, final Double initBalance) throws Exception {
    final UserExtInfo userExtInfo = new UserExtInfo();
    try {
      BeanUtils.copyProperties((Object)userExtInfo, (Object)userInfo);
    }
    catch (Exception e) {
      UserService2.LOGGER.error("\u521b\u5efaUserExtInfo\u5f02\u5e38", (Throwable)e);
      throw new BusinessException("\u6ce8\u518c\u4fe1\u606f\u5f02\u5e38\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb\uff01");
    }
    if (StringUtils.isNotBlank((CharSequence)fundPwd)) {
      userExtInfo.setFundPwd(SecurityUtil.crtyMd5(new String[] { fundPwd }));
    }
    userExtInfo.setMoney((initBalance != null && initBalance >= 0.0) ? ((double)initBalance) : 0.0);
    userExtInfo.setPoints(0.0);
    userExtInfo.setRechCount(0);
    userExtInfo.setRechMoney(0.0);
    userExtInfo.setUwCount(0);
    userExtInfo.setUwMoney(0.0);
    this.userExtInfoDao.save(userExtInfo);
    return userExtInfo;
  }

  private void registerDiscount(final UserInfo userInfo, final UserExtInfo userExtInfo, final Double discountAmount, final Double discountDml, final String desc) throws Exception {
    if (discountAmount != null && discountAmount > 0.0) {
      try {
        this.rechargeOrderService.discount(userInfo, userExtInfo, BuildInDiscountType.REGISTER, discountAmount, discountDml, TranTypes.REGISTER, desc, null);
      }
      catch (Exception e) {
        UserService2.LOGGER.error("注册送彩金异常 => \u91d1\u989d: {}, 打码量: {}", new Object[] { discountAmount, discountDml, e });
        throw new BusinessException();
      }
    }
  }

  private void validateRegisterIp(final RegisterLimit limit, final String registerIp) throws Exception {
    if (limit.getIpCount() != null && limit.getIpCount() > 0 && this.userInfoDao.countByRegIpAndRegWay(registerIp, RegisterTypes.ONLINE.getValue()) >= limit.getIpCount()) {
      UserService2.LOGGER.error("IP\u8fbe\u5230\u6ce8\u518c\u4f1a\u5458\u6570\u4e0a\u9650 => {}", (Object)registerIp);
      throw new BusinessException("\u60a8\u5df2\u6ce8\u518c\u591a\u4e2a\u5e10\u53f7\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb\uff01");
    }
  }

  private void validateUniqueFields(final RegisterLimit limit, final UserInfo userInfo) throws Exception {
    if (limit.getOnlyfullName() != null && limit.getOnlyfullName() == 1 && StringUtils.isNotBlank((CharSequence)userInfo.getFullName()) && this.userInfoDao.countByUserIdNotAndFieldEq(userInfo.getUserId(), "full_name", userInfo.getFullName()) > 0) {
      throw new BusinessException("\u59d3\u540d\u5df2\u88ab\u6ce8\u518c\uff01");
    }
    if (limit.getOnlyfullPhone() != null && limit.getOnlyfullPhone() == 1 && StringUtils.isNotBlank((CharSequence)userInfo.getPhone()) && this.userInfoDao.countByUserIdNotAndFieldEq(userInfo.getUserId(), "phone", userInfo.getPhone()) > 0) {
      throw new BusinessException("\u7535\u8bdd\u53f7\u7801\u5df2\u88ab\u6ce8\u518c\uff01");
    }
  }

  private SpreadInfo validateSpreadInfo(final String domain, final String code, final boolean isMobileOrTablet, final String userAgent) throws Exception {
    SpreadInfo spreadInfo = null;
    if (StringUtils.isNotBlank((CharSequence)code)) {
      spreadInfo = this.validateSpreadInfo("I", domain, code, isMobileOrTablet);
    }
    else {
      final String domainCode = this.domainConfigService.getSpreadCode(domain);
      if (StringUtils.isNotBlank((CharSequence)domainCode)) {
        spreadInfo = this.validateSpreadInfo("D", domain, domainCode, isMobileOrTablet);
      }
    }
    if (spreadInfo == null) {
      String defaultCode;
      if (this.VerificationAppType(userAgent)) {
        defaultCode = this.configService.getPhoneInitialCode();
      }
      else if (isMobileOrTablet) {
        defaultCode = this.configService.getPhoneIntrCode();
      }
      else {
        defaultCode = this.configService.getDefaultIntrCode();
      }
      spreadInfo = this.validateSpreadInfo("S", domain, defaultCode, isMobileOrTablet);
    }
    return spreadInfo;
  }

  private SpreadInfo validateSpreadInfo(final String identify, final String domain, final String code, final boolean isMobileOrTablet) throws Exception {
    final SpreadInfo spreadInfo = this.spreadInfoService.getByCode(code);
    if (spreadInfo == null) {
      UserService2.LOGGER.error("\u63a8\u5e7f\u7801\u5f02\u5e38 => \u7c7b\u578b: {}, \u57df\u540d: {}, \u63a8\u5e7f\u7801: {}, \u79fb\u52a8\u8bbe\u5907: {}", new Object[] { identify, domain, code, isMobileOrTablet });
      throw new BusinessException(String.format("%s\u63a8\u5e7f\u7801\u5f02\u5e38\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb\uff01", identify));
    }
    return spreadInfo;
  }

  private void setClientInfo(final UserInfo userInfo, final UserEquipmentVO clientInfo) {
    userInfo.setRegIp(clientInfo.getIpAddress());
    userInfo.setBrowser(clientInfo.getBrowserMemo());
    userInfo.setOperatingSystem(clientInfo.getMacOs());
    userInfo.setUserAgent(clientInfo.getUserAgent());
  }

  private String encryptPassword(final UserInfo userInfo) {
    return SecurityUtil.crtyMd5(new String[] { userInfo.getPassword() + "@" + userInfo.getAccount() });
  }

  private String generateFk(final UserInfo userInfo) {
    return SecurityUtil.crtyMd5(false, new String[] { userInfo.getAccount() + userInfo.getFullName() + AppContext.getInstance().getAppKey() });
  }

  private String generateTestAccount() {
    String account;
    do {
      account = String.format("test" + StringUtil.getRandomStr(0, 6), new Object[0]);
    } while (this.userInfoDao.countByUserIdNotAndFieldEq(null, "account", account) > 0);
    return account;
  }

  private String generateWxAccount() {
    String account;
    do {
      account = StringUtil.getRandomStr(1, 7);
    } while (this.userInfoDao.countByUserIdNotAndFieldEq(null, "account", account) > 0);
    return account;
  }

  private boolean VerificationAppType(final String userAgent) {
    return userAgent != null && userAgent != "" && (userAgent.toLowerCase().contains("native_android") || userAgent.toLowerCase().contains("native_ios"));
  }

  public void checkDlNum(final UserInfo dl, final UserInfo userInfo) throws BusinessException {
    final Config configData = this.configService.getByNameAndKey(ConfigEnums.DlMaxNumber.getName(), ConfigEnums.DlMaxNumber.getKey());
    if (null != configData) {
      final int maxNumber = Integer.parseInt(configData.getConfigValue());
      if (maxNumber > 0) {
        if (dl.getDlLevel() == maxNumber && userInfo.getIsDl()) {
          throw new BusinessException("\u5f53\u524d\u4ee3\u7406\u4e0d\u80fd\u6dfb\u52a0\u4e0b\u7ea7\u4ee3\u7406");
        }
        if (dl.getDlLevel() > maxNumber) {
          throw new BusinessException("\u5f53\u524d\u4ee3\u7406\u4e0d\u80fd\u6dfb\u52a0\u4e0b\u7ea7");
        }
      }
    }
  }

  static {
    LOGGER = LoggerFactory.getLogger((Class)UserService2.class);
    DEFAULT_PASSWORD_WX = SecurityUtil.crtyMd5(new String[] { "88888888" });
  }
}
