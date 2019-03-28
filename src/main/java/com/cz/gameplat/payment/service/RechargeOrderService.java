package com.cz.gameplat.payment.service;

import com.cz.framework.DateUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.*;
import com.cz.gameplat.activity.manager.*;
import javax.annotation.*;
import com.cz.gameplat.payment.dao.*;
import com.cz.gameplat.sys.service.*;
import com.cz.gameplat.report.service.*;
import com.cz.gameplat.user.service.*;
import com.cz.gameplat.sys.entity.*;
import org.apache.commons.lang3.*;
import com.cz.framework.exception.*;
import com.cz.gameplat.user.bean.*;
import com.cz.gameplat.user.entity.*;
import com.cz.gameplat.sys.limit.*;
import com.cz.gameplat.sys.enums.*;
import com.cz.gameplat.payment.bo.*;
import com.cz.gameplat.lottery.enums.*;
import org.apache.commons.beanutils.*;
import com.cz.framework.redis.lock.*;
import com.alibaba.fastjson.*;
import com.cz.gameplat.payment.thirdparty.*;
import java.util.stream.*;
import java.text.*;
import com.cz.gameplat.payment.bean.*;
import com.cz.gameplat.user.enums.*;
import com.cz.gameplat.payment.constant.*;
import java.math.*;
import com.cz.framework.bean.*;
import com.cz.gameplat.payment.query.*;
import com.cz.gameplat.payment.vo.*;
import com.cz.gameplat.payment.entity.*;
import com.cz.gameplat.thirdparty.util.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.hssf.usermodel.*;
import java.io.*;
import com.cz.framework.*;
import org.apache.poi.openxml4j.exceptions.*;
import org.springframework.transaction.annotation.*;
import java.beans.*;
import org.apache.poi.ss.usermodel.*;
import java.lang.reflect.*;
import org.slf4j.*;

@Service
public class RechargeOrderService
{
    private static final Logger LOGGER;
    @Resource
    private ActivityManager aj;
    @Resource
    private RechargeOrderDao rechargeOrderDao;
    @Resource
    private RechargeOrderHistoryDao rechargeOrderHistoryDao;
    @Resource
    private RechargeConfigService rechargeConfigService;
    @Resource
    private DiscountTypeService discountTypeService;
    @Resource
    private UserService userService;
    @Resource
    private LimitInfoService limitInfoService;
    @Resource
    private BlacklistService blacklistService;
    @Resource
    private UserRWReportService userRWReportService;
    @Resource
    private ValidWithdrawService validWithdrawService;
    @Resource
    private UserBillService userBillService;
    @Resource
    private PushMessageService pushMessageService;
    @Resource
    private PayAccountService payAccountService;
    @Resource
    private PayTypeService payTypeService;
    @Resource
    private TpPayChannelService tpPayChannelService;
    @Resource
    private TpMerchantService tpMerchantService;
    @Resource
    private TpPayTypeService tpPayTypeService;
    @Resource
    private TpInterfaceService tpInterfaceService;
    @Resource
    private ThirdPartyDispatcherManager thirdPartyDispatcherManager;
    @Resource
    private OfficialAccountsConfigService officialAccountsConfigService;
    
    public RechargeOrder getByOrderNo(final String orderNo) {
        return (RechargeOrder)this.rechargeOrderDao.getObjectByFieldName("order_no", (Object)orderNo);
    }
    
    public void transfer(final TransferRechargeOrderBo transferRechargeOrderBo, final UserEquipmentVO userEquipmentVO) throws Exception {
        final RechargeOrder rechargeOrder = this.buildTransferRechargeOrder(transferRechargeOrderBo, userEquipmentVO);
        this.rechargeOrderDao.save(rechargeOrder);
    }
    
    public ReturnMessage queryMerchantOrder(final RechargeOrder query) throws Exception {
        final TpInterface tpInterface = this.tpInterfaceService.get(query.getTpInterface());
        this.verifyQueryTpInterface(tpInterface);
        final TpMerchant tpMerchant = this.tpMerchantService.get(query.getTpMerchantId());
        final TpPayChannel tpPayChannel = this.tpPayChannelService.getVO(query.getTpPayChannelId());
        if (tpPayChannel == null) {
            throw new Exception("\u6b64\u901a\u9053\u6ca1\u6709\u5f00\u901a");
        }
        if (tpMerchant == null) {
            throw new Exception("\u7b2c\u4e09\u65b9\u5546\u6237\u5df2\u5173\u95ed");
        }
        final ThirdPartyDispatchContext context = new ThirdPartyDispatchContext();
        context.setName(tpInterface.getName());
        context.setOrderNo(query.getOrderNo());
        context.setOrderTime(query.getAddTime());
        context.setAmount(query.getAmount());
        context.setPayType(tpPayChannel.getTpPayType());
        context.setVersion(tpInterface.getOrderQueryVersion());
        context.setCharset(tpInterface.getCharset());
        context.setDispatchUrl(tpInterface.getOrderQueryUrl());
        context.setDispatchMethod(tpInterface.getOrderQueryMethod());
        context.setMerchantParameters((Map<String, String>)JsonUtil.toMapObject(tpMerchant.getParameters()));
        return this.thirdPartyDispatcherManager.queryOnlinePay(tpInterface.getCode(), context);
    }
    
    public void onlinePayForTrial(final Long userId, final String account, final Double amount, final Admin admin) throws Exception {
        final UserInfo userInfo = this.userService.getUserInfo(userId);
        final UserExtInfo userExtInfo = this.userService.getUserExtInfo(userId);
        if (userInfo == null || userExtInfo == null || !StringUtils.equalsIgnoreCase((CharSequence)account, (CharSequence)userInfo.getAccount())) {
            throw new BusinessException("UC/USER_NOT_EXIST", "uc.user_not_exist", (Object[])null);
        }
        if (amount == null) {
            throw new BusinessException("\u5145\u503c\u91d1\u989d\u65e0\u6548\u3002");
        }
        if (!SysUserTypes.TEST.getCode().equals(userInfo.getType())) {
            throw new BusinessException(String.format("%s\u975e\u8bd5\u73a9\u8d26\u53f7!", userInfo.getAccount()));
        }
        final Double balance = (userExtInfo.getMoney() == null) ? 0.0 : userExtInfo.getMoney();
        final UserMoneyBean userMoney = new UserMoneyBean(userInfo.getUserId());
        userMoney.setMoney(amount);
        userMoney.setOrigMoney(balance);
        userExtInfo.setMoney(balance + amount);
        this.userService.updateMoney(userMoney);
    }
    
    public String onlinePay(final Long userId, final Long payChannelId, final String bankCode, Double amount, final String asyncCallbackUrl, final String syncCallbackUrl, final UserEquipmentVO userEquipmentVO) throws Exception {
        final RechargeOrder rechargeOrder = new RechargeOrder();
        final UserInfo userInfo = this.userService.getUserInfo(userId);
        final UserExtInfo userExtInfo = this.userService.getUserExtInfo(userId);
        this.verifyUser(userInfo, userExtInfo, false);
        this.fillUserInfo(rechargeOrder, userInfo, userExtInfo);
        final TpPayChannelVO tpPayChannel = this.tpPayChannelService.getVO(payChannelId);
        this.verifyTpPayChannel(tpPayChannel, userInfo.getHyLevel());
        this.fillTpPayChannelInfo(rechargeOrder, tpPayChannel);
        final TpMerchant tpMerchant = this.tpMerchantService.get(tpPayChannel.getMerchantId());
        this.verifyTpMerchant(tpMerchant);
        this.fillTpMerchantInfo(rechargeOrder, tpMerchant);
        final TpPayType tpPayType = this.tpPayTypeService.get(tpMerchant.getTpInterface(), tpPayChannel.getTpPayType());
        final TpInterface tpInterface = this.tpInterfaceService.get(tpMerchant.getTpInterface());
        this.verifyTpInterface(tpInterface, tpPayType, bankCode);
        this.fillTpInterfaceInfo(rechargeOrder, tpInterface);
        final PayType payType = this.payTypeService.get(tpPayType.getPayType());
        this.verifyPayType(payType);
        this.fillPayTypeInfo(rechargeOrder, payType);
        final RechargeConfig rechargeConfig = this.rechargeConfigService.getByModeAndPayType(RechargeMode.ONLINE_PAY.getValue(), payType.getCode(), userInfo);
        this.verifyRechargeConfig(rechargeConfig);
        this.verifyAmount(amount, rechargeConfig);
        amount = this.addRiskMoney(amount, rechargeConfig, tpPayType, tpInterface);
        amount = this.splitMoney(amount);
        rechargeOrder.setAmount(amount);
        this.verifyPeriod(userInfo, payType, rechargeConfig);
        this.calculateDiscountAmount(rechargeOrder, rechargeConfig, userExtInfo.getRechCount() == 0);
        rechargeOrder.setTotalAmount(amount + rechargeOrder.getDiscountAmount());
        rechargeOrder.setDmlFlag(TrueFalse.TRUE.getValue());
        this.calculateDml(rechargeOrder, rechargeConfig);
        this.fillClientInfo(rechargeOrder, userEquipmentVO);
        rechargeOrder.setPointFlag(TrueFalse.TRUE.getValue());
        rechargeOrder.setMode(RechargeMode.ONLINE_PAY.getValue());
        rechargeOrder.setStatus(RechargeStatus.UNHANDLED.getValue());
        rechargeOrder.setOrderNo(NumUtil.getOrderNo());
        rechargeOrder.setAddTime(new Date());
        rechargeOrder.setDomain(userEquipmentVO.getBaseURL());
        this.fillFk(rechargeOrder);
        this.rechargeOrderDao.save(rechargeOrder);
        final ThirdPartyDispatchContext context = new ThirdPartyDispatchContext();
        this.fillThirdPartyDispatchContext(context, tpInterface, tpPayType);
        context.setTransportRequired(tpPayType.getTransportRequired() == TrueFalse.TRUE.getValue());
        context.setTransportUrl(tpPayChannel.getTransportUrl());
        final RechargeLimit rechargeLimit = this.limitInfoService.get(LimitEnums.RECHARGE_LIMIT.getName(), RechargeLimit.class);
        String callbackDomain = "";
        if (rechargeLimit != null) {
            callbackDomain = rechargeLimit.getCallbackDomain();
        }
        final String domain = StringUtils.isNotEmpty((CharSequence)callbackDomain) ? callbackDomain : userEquipmentVO.getBaseURL();
        context.setAsyncCallbackUrl(StringUtils.replace(domain + asyncCallbackUrl, "${orderNo}", rechargeOrder.getOrderNo()));
        context.setSyncCallbackUrl(StringUtils.replace(userEquipmentVO.getBaseURL() + syncCallbackUrl, "${orderNo}", rechargeOrder.getOrderNo()));
        context.setPayType(tpPayType.getCode());
        context.setBankCode(StringUtils.isEmpty((CharSequence)bankCode) ? "" : bankCode);
        context.setOrderNo(rechargeOrder.getOrderNo());
        context.setOrderName(rechargeOrder.getOrderNo());
        context.setAmount(amount);
        context.setOrderTime(rechargeOrder.getAddTime());
        context.setUserAccount(userInfo.getAccount());
        context.setUserFullName(userInfo.getFullName());
        context.setUserIpAddress(userEquipmentVO.getIpAddress());
        context.setMobile(userEquipmentVO.isMobileDevice());
        context.setViewType(tpPayType.getViewType());
        context.setMacOs(userEquipmentVO.getMacOs());
        context.setMerchantParameters((Map<String, String>)JsonUtil.toMapObject(tpMerchant.getParameters()));
        return this.thirdPartyDispatcherManager.dispatch(tpInterface.getCode(), context);
    }
    
    public void manual(final ManualRechargeOrderBo manualRechargeOrderBo, final String operator) throws Exception {
        final RechargeOrder rechargeOrder = this.buildManualRechargeOrder(manualRechargeOrderBo);
        this.fillFk(rechargeOrder);
        this.rechargeOrderDao.save(rechargeOrder);
        if (manualRechargeOrderBo.isSkipAuditing()) {
            this.accept(rechargeOrder, "\u76f4\u63a5\u5165\u6b3e", operator);
        }
    }
    
    public void discount(final Long userId, final BuildInDiscountType discountType, final Double amount, final Double dml, final TranTypes tranTypes, final String remarks, final String operator) throws Exception {
        final UserInfo userInfo = this.userService.getUserInfo(userId);
        final UserExtInfo userExtInfo = this.userService.getUserExtInfo(userId);
        this.discount(userInfo, userExtInfo, discountType, amount, dml, tranTypes, remarks, operator);
    }
    
    public void discount(final UserInfo userInfo, final UserExtInfo userExtInfo, final BuildInDiscountType discountType, final Double amount, final Double dml, final TranTypes tranTypes, final String remarks, final String operator) throws Exception {
        final RechargeOrder rechargeOrder = new RechargeOrder();
        this.fillUserInfo(rechargeOrder, userInfo, userExtInfo);
        rechargeOrder.setAmount(0.0);
        rechargeOrder.setDiscountRechargeFlag(discountType.getRechargeFlag());
        rechargeOrder.setDiscountType(discountType.getValue());
        rechargeOrder.setDiscountAmount(amount);
        rechargeOrder.setTotalAmount(amount);
        rechargeOrder.setPointFlag(TrueFalse.TRUE.getValue());
        rechargeOrder.setDmlFlag(TrueFalse.TRUE.getValue());
        rechargeOrder.setNormalDml(0.0);
        rechargeOrder.setDiscountDml(dml);
        rechargeOrder.setMode(RechargeMode.MANUAL.getValue());
        rechargeOrder.setStatus(RechargeStatus.SUCCESS.getValue());
        rechargeOrder.setOrderNo(NumUtil.getOrderNo());
        rechargeOrder.setAddTime(new Date());
        rechargeOrder.setAuditTime(new Date());
        rechargeOrder.setAuditorAccount(operator);
        rechargeOrder.setUpdateTime(rechargeOrder.getAddTime());
        rechargeOrder.setRemarks(remarks);
        this.fillFk(rechargeOrder);
        this.rechargeOrderDao.save(rechargeOrder);
        final RechargeOrderHistory rechargeOrderHistory = new RechargeOrderHistory();
        BeanUtils.copyProperties(rechargeOrderHistory, rechargeOrder);
        this.rechargeOrderHistoryDao.save(rechargeOrderHistory);
        if (SysUserTypes.HY.getCode().equals(userInfo.getType())) {
            this.updateRWReport(rechargeOrder, userInfo, userExtInfo);
        }
        this.validWithdrawService.addRechargeOrder(rechargeOrder);
        this.userBillService.add(userInfo, userExtInfo.getMoney(), rechargeOrder.getOrderNo(), tranTypes.getValue(), rechargeOrder.getTotalAmount(), tranTypes.getDesc() + remarks, operator);
        this.updateUserMoney(rechargeOrder);
    }
    
    public void batchManual(final ManualRechargeOrderBo manualRechargeOrderBo, final List<Long> userIds, final String hyLevels, final String operator) throws Exception {
        if (StringUtils.isNotEmpty((CharSequence)hyLevels)) {
            this.userService.getUserIdsByLevelForRecharge(hyLevels).forEach(id -> userIds.add(id));
        }
        if (CollectionUtils.isEmpty((Collection)userIds)) {
            return;
        }
        for (final Long userId : userIds) {
            manualRechargeOrderBo.setUserId(userId);
            this.manual(manualRechargeOrderBo, operator);
        }
    }
    
    public void handle(final Long id, final String operator) throws Exception {
        if (this.rechargeOrderDao.updateStatus(id, RechargeStatus.HANDLED.getValue(), RechargeStatus.UNHANDLED.getValue(), new Date(), operator) != 1) {
            throw new BusinessException("\u5145\u503c\u8ba2\u5355\u5df2\u5904\u7406\u3002");
        }
    }
    
    public void accept(final Long id, final String auditRemarks, final String operator) throws Exception {
        this.accept((RechargeOrder)this.rechargeOrderDao.get(id), auditRemarks, operator);
    }
    
    public void accept(final Long id, final String auditRemarks, final String operator, final boolean hasTpAcceptPermission) throws Exception {
        this.accept((RechargeOrder)this.rechargeOrderDao.get(id), auditRemarks, operator, hasTpAcceptPermission);
    }
    
    public void cancel(final Long id, final String auditRemarks, final String operator) throws Exception {
        this.cancel((RechargeOrder)this.rechargeOrderDao.get(id), auditRemarks, operator);
    }
    
    public void recreate(final Long id, final String account) throws Exception {
        final RechargeOrderHistory rechargeOrderHistory = (RechargeOrderHistory)this.rechargeOrderHistoryDao.get(id);
        if (rechargeOrderHistory == null) {
            throw new BusinessException("\u65e0\u6548\u7684\u5145\u503c\u8ba2\u5355\u3002");
        }
        final RechargeOrder rechargeOrder = new RechargeOrder();
        BeanUtils.copyProperties(rechargeOrder, rechargeOrderHistory);
        rechargeOrder.setId(null);
        rechargeOrder.setOrderNo(NumUtil.getOrderNo());
        rechargeOrder.setAddTime(new Date());
        rechargeOrder.setRemarks("\u540e\u53f0\u8865\u5355");
        rechargeOrder.setStatus(RechargeStatus.HANDLED.getValue());
        rechargeOrder.setUpdateTime(null);
        rechargeOrder.setAuditorAccount(account);
        rechargeOrder.setAuditTime(null);
        rechargeOrder.setAuditRemarks(null);
        this.rechargeOrderDao.save(rechargeOrder);
        this.rechargeOrderHistoryDao.delete(id);
    }
    
    public void batchHandle(final List<Long> ids, final String operator) throws Exception {
        if (ids == null) {
            return;
        }
        for (final Long id : ids) {
            this.handle(id, operator);
        }
    }
    
    public void batchAccept(final List<Long> ids, final String auditRemarks, final String operator, final boolean hasTpAcceptPermission) throws Exception {
        if (ids == null) {
            return;
        }
        for (final Long id : ids) {
            this.accept(id, auditRemarks, operator, hasTpAcceptPermission);
        }
    }
    
    @Lock(key = "recharge_accept", value = "#rechargeOrder.id")
    public void accept(final RechargeOrder rechargeOrder, final String auditRemarks, final String operator, final boolean hasTpAcceptPermission) throws Exception {
        if (rechargeOrder.getMode() == 2 && !hasTpAcceptPermission) {
            throw new BusinessException("\u5b50\u8d26\u53f7\u65e0\u7b2c\u4e09\u65b9\u5165\u6b3e\u6743\u9650\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458");
        }
        this.accept(rechargeOrder, auditRemarks, operator);
    }
    
    public void batchCancel(final List<Long> ids, final String auditRemarks, final String operator) throws Exception {
        if (ids == null) {
            return;
        }
        for (final Long id : ids) {
            this.cancel(id, auditRemarks, operator);
        }
    }
    
    @Lock(key = "recharge_accept", value = "#rechargeOrder.id")
    public void accept(final RechargeOrder rechargeOrder, final String auditRemarks, final String operator) throws Exception {
        this.verifyRechargeOrderForAuditing(rechargeOrder);
        final UserInfo userInfo = this.userService.getUserInfo(rechargeOrder.getUserId());
        final UserExtInfo userExtInfo = this.userService.getUserExtInfo(rechargeOrder.getUserId());
        this.verifyUser(userInfo, userExtInfo, rechargeOrder.getMode() == RechargeMode.TRANSFER.getValue());
        this.recalculateDiscountAmount(rechargeOrder, userExtInfo.getRechCount() > 0);
        this.updateRechargeOrder(rechargeOrder, auditRemarks, operator, RechargeStatus.SUCCESS.getValue(), userInfo);
        this.updateRWReport(rechargeOrder, userInfo, userExtInfo);
        if (rechargeOrder.getDmlFlag() == TrueFalse.TRUE.getValue()) {
            this.updateDml(rechargeOrder, userExtInfo);
        }
        this.addUserBill(rechargeOrder, userInfo, userExtInfo, operator);
        this.updateRechargeMoney(rechargeOrder);
        this.updateUserMoney(rechargeOrder);
        final String content = this.getContentFromRechargeOrder(rechargeOrder);
        if (StringUtil.isNotBlank(content)) {
            this.pushMessageService.save(rechargeOrder.getUserId(), content);
        }
        this.aj.doWeekendRedPacketJob(rechargeOrder.getAmount(), userInfo);
    }
    
    private String getContentFromRechargeOrder(final RechargeOrder rechargeOrder) {
        if (rechargeOrder == null) {
            return "";
        }
        String content = "";
        if (rechargeOrder.getAmount() > 0.0 && rechargeOrder.getDiscountAmount() == 0.0) {
            content = String.format("\u60a8\u4e8e%s\u63d0\u4ea4\u7684%.3f\u5143\u5145\u503c\u8ba2\u5355\u5df2\u6210\u529f\u5165\u6b3e\uff0c\u8ba2\u5355\u7f16\u53f7\uff1a%s\u3002", DateUtil.dateToYMDHMS(rechargeOrder.getAddTime()), rechargeOrder.getAmount(), rechargeOrder.getOrderNo());
        }
        if (rechargeOrder.getAmount() > 0.0 && rechargeOrder.getDiscountAmount() > 0.0) {
            content = String.format("\u60a8\u4e8e%s\u63d0\u4ea4\u7684%.3f\u5143\u5145\u503c\u8ba2\u5355\u5df2\u6210\u529f\u5165\u6b3e\uff0c\u4f18\u60e0\u91d1\u989d\u4e3a%.3f\u5143\uff0c\u8ba2\u5355\u7f16\u53f7\uff1a%s\u3002", DateUtil.dateToYMDHMS(rechargeOrder.getAddTime()), rechargeOrder.getAmount(), rechargeOrder.getDiscountAmount(), rechargeOrder.getOrderNo());
        }
        if (rechargeOrder.getAmount() == 0.0 && rechargeOrder.getDiscountAmount() > 0.0) {
            String discountTypeStr = "";
            final List<DiscountType> list = this.discountTypeService.queryAll();
            if (list != null) {
                for (int i = 0; i < list.size(); ++i) {
                    if (list.get(i).getId().equals(rechargeOrder.getDiscountType())) {
                        discountTypeStr = list.get(i).getName();
                        break;
                    }
                }
            }
            content = String.format("\u60a8\u4e8e%s\u6709\u4e00\u7b14%s\u4f18\u60e0\u91d1\u989d\u5165\u6b3e\uff0c\u91d1\u989d\u4e3a%.3f\u5143\u8ba2\u5355\u7f16\u53f7\uff1a%s\u3002", DateUtil.dateToYMDHMS(rechargeOrder.getAddTime()), discountTypeStr, rechargeOrder.getDiscountAmount(), rechargeOrder.getOrderNo());
        }
        return content;
    }
    
    public void cancel(final RechargeOrder rechargeOrder, final String auditRemarks, final String operator) throws Exception {
        this.verifyRechargeOrderForAuditing(rechargeOrder);
        this.updateRechargeOrder(rechargeOrder, auditRemarks, operator, RechargeStatus.CANCELLED.getValue(), null);
        String message = String.format("\u60a8\u4e8e%s\u63d0\u4ea4\u7684%.3f\u5143\u5145\u503c\u8ba2\u5355\u5df2\u88ab\u53d6\u6d88\uff0c\u8ba2\u5355\u7f16\u53f7\uff1a%s\u3002", DateUtil.dateToYMDHMS(rechargeOrder.getAddTime()), rechargeOrder.getAmount(), rechargeOrder.getOrderNo());
        if (StringUtils.isNotEmpty((CharSequence)auditRemarks)) {
            message += String.format("\u53d6\u6d88\u539f\u56e0\uff1a%s\u3002", auditRemarks);
        }
        this.pushMessageService.save(rechargeOrder.getUserId(), message);
        if (rechargeOrder.getMode() == RechargeMode.TRANSFER.getValue()) {
            final RechargeConfig rechargeConfig = this.rechargeConfigService.getByModeAndPayType(rechargeOrder.getMode(), rechargeOrder.getPayType());
            if (rechargeConfig.getDisableAfterCancelled() == TrueFalse.TRUE.getValue()) {
                this.userService.updateLimitRech(rechargeOrder.getUserId(), TrueFalse.TRUE.getValue());
            }
        }
    }
    
    public String onlinePayAsyncCallback(final String orderNo, final Map<String, String> callbackParameters, final String requestBody, final String callbackIp, final String asyncCallbackUrl, final String syncCallbackUrl, final UserEquipmentVO userEquipmentVO, final JSONObject headerJson) throws Exception {
        final RechargeOrder rechargeOrder = this.getByOrderNo(orderNo);
        final String beanName = this.getTpInterfaceCodeByRechargeOrder(rechargeOrder);
        final ThirdPartyCallbackContext context = this.getThirdPartyCallbackContextByRechargeOrder(rechargeOrder, callbackIp, asyncCallbackUrl, syncCallbackUrl, userEquipmentVO);
        context.setDomain(rechargeOrder.getDomain());
        context.setHeaders(JSON.toJSONString((Object)headerJson));
        final ThirdPartyCallBackResult result = this.thirdPartyDispatcherManager.asyncCallback(beanName, context, callbackParameters, requestBody);
        if (!result.isSuccess()) {
            RechargeOrderService.LOGGER.info("\u5145\u503c\u8ba2\u5355 {} \u5145\u503c\u5931\u8d25\uff01{}", rechargeOrder.getOrderNo(), (Object)result.getErrorMessage());
            return result.getAsyncResponse();
        }
        if (result.getAmount() != (double)rechargeOrder.getAmount()) {
            RechargeOrderService.LOGGER.error("\u5145\u503c\u8ba2\u5355 {} \u652f\u4ed8\u91d1\u989d\u9519\u8bef\uff01\u8ba2\u5355\u91d1\u989d {} \u652f\u4ed8\u91d1\u989d {}", new Object[] { rechargeOrder.getOrderNo(), rechargeOrder.getAmount(), result.getAmount() });
            return result.getAsyncResponse();
        }
        rechargeOrder.setTpOrderNo(result.getTpOrderNo());
        if (rechargeOrder.getStatus() != RechargeStatus.UNHANDLED.getValue() && rechargeOrder.getStatus() != RechargeStatus.HANDLED.getValue()) {
            RechargeOrderService.LOGGER.info("\u5145\u503c\u8ba2\u5355 {} \u5df2\u5904\u7406,\u76f4\u63a5\u8fd4\u56de {}", rechargeOrder.getOrderNo(), (Object)result.getAsyncResponse());
            return result.getAsyncResponse();
        }
        try {
            this.accept(rechargeOrder, "\u5145\u503c\u6210\u529f", null);
            RechargeOrderService.LOGGER.info("\u5145\u503c\u8ba2\u5355 {} \u5165\u6b3e\u6210\u529f\uff01", rechargeOrder.getOrderNo());
        }
        catch (Exception e) {
            RechargeOrderService.LOGGER.error("\u5145\u503c\u8ba2\u5355 {} \u5165\u6b3e\u5931\u8d25\uff01", rechargeOrder.getOrderNo());
            throw e;
        }
        RechargeOrderService.LOGGER.info("\u6536\u5230\u5f02\u6b65\u56de\u8c03\u540e,\u8fd4\u56de\u4fe1\u606f\uff1a{}", (Object)result.getAsyncResponse());
        return result.getAsyncResponse();
    }
    
    public ThirdPartyCallBackResult onlinePaySyncCallback(final String orderNo, final Map<String, String> callbackParameters, final String requestBody, final String callbackIp, final String asyncCallbackUrl, final String syncCallbackUrl, final UserEquipmentVO userEquipmentVO) throws Exception {
        final RechargeOrder rechargeOrder = this.getByOrderNo(orderNo);
        final String beanName = this.getTpInterfaceCodeByRechargeOrder(rechargeOrder);
        final ThirdPartyCallbackContext context = this.getThirdPartyCallbackContextByRechargeOrder(rechargeOrder, callbackIp, asyncCallbackUrl, syncCallbackUrl, userEquipmentVO);
        return this.thirdPartyDispatcherManager.syncCallback(beanName, context, callbackParameters, requestBody);
    }
    
    public boolean containsOnlinePay(final Long[] ids) {
        return ids != null && ids.length != 0 && this.rechargeOrderDao.countByModeAndIdsIn(RechargeMode.ONLINE_PAY.getValue(), (List<Long>)Stream.of(ids).collect(Collectors.toList())) > 0;
    }
    
    private TpInterface getTpInterfaceByRechargeOrder(final RechargeOrder rechargeOrder) {
        if (rechargeOrder == null || StringUtils.isEmpty((CharSequence)rechargeOrder.getTpInterface())) {
            return null;
        }
        return this.tpInterfaceService.get(rechargeOrder.getTpInterface());
    }
    
    private void fillUserInfo(final RechargeOrder rechargeOrder, final UserInfo userInfo, final UserExtInfo userExtInfo) {
        rechargeOrder.setUserId(userInfo.getUserId());
        rechargeOrder.setUserAccount(userInfo.getAccount());
        rechargeOrder.setUserNickname(userInfo.getFullName());
        rechargeOrder.setUserLevel(userInfo.getHyLevel());
        rechargeOrder.setUserColor(userInfo.getUserColor());
        rechargeOrder.setSuperId(userInfo.getSuperId());
        rechargeOrder.setSuperAccount(userInfo.getSuperName());
        rechargeOrder.setSuperPath(userInfo.getSuperPath());
        rechargeOrder.setUserBalance(userExtInfo.getMoney());
        rechargeOrder.setUserType(userInfo.getType());
    }
    
    private void fillPayAccountInfo(final RechargeOrder rechargeOrder, final PayAccount payAccount) {
        rechargeOrder.setPayAccountId(payAccount.getId());
        rechargeOrder.setPayAccountAccount(payAccount.getAccount());
        rechargeOrder.setPayAccountOwner(payAccount.getOwner());
        rechargeOrder.setPayAccountBankName(payAccount.getBankName());
        rechargeOrder.setSubPayType(payAccount.getSubPayType());
    }
    
    private void fillPayTypeInfo(final RechargeOrder rechargeOrder, final PayType payType) {
        rechargeOrder.setPayType(payType.getCode());
        rechargeOrder.setPayTypeName(payType.getName());
    }
    
    private void fillTpInterfaceInfo(final RechargeOrder rechargeOrder, final TpInterface tpInterface) {
        rechargeOrder.setTpInterface(tpInterface.getCode());
        rechargeOrder.setTpInterfaceName(tpInterface.getName());
    }
    
    private void fillTpMerchantInfo(final RechargeOrder rechargeOrder, final TpMerchant tpMerchant) {
        rechargeOrder.setTpMerchantId(tpMerchant.getId());
        rechargeOrder.setTpMerchantName(tpMerchant.getName());
    }
    
    private void fillTpPayChannelInfo(final RechargeOrder rechargeOrder, final TpPayChannel tpPayChannel) {
        rechargeOrder.setTpPayChannelId(tpPayChannel.getId());
        rechargeOrder.setTpPayChannelName(tpPayChannel.getName());
    }
    
    private void fillClientInfo(final RechargeOrder rechargeOrder, final UserEquipmentVO userEquipmentVO) {
        rechargeOrder.setBrowser(userEquipmentVO.getBrowserMemo());
        rechargeOrder.setOs(userEquipmentVO.getMacOs());
        rechargeOrder.setIpAddress(userEquipmentVO.getIpAddress());
        rechargeOrder.setUserAgent(userEquipmentVO.getUserAgent());
    }
    
    private void fillThirdPartyDispatchContext(final ThirdPartyDispatchContext context, final TpInterface tpInterface, final TpPayType tpPayType) {
        final String version = StringUtils.isNotEmpty((CharSequence)tpPayType.getVersion()) ? tpPayType.getVersion() : tpInterface.getVersion();
        final String charset = StringUtils.isNotEmpty((CharSequence)tpPayType.getCharset()) ? tpPayType.getCharset() : tpInterface.getCharset();
        final Integer dispatchType = (tpPayType.getDispatchType() != null) ? tpPayType.getDispatchType() : tpInterface.getDispatchType();
        final String dispatchUrl = StringUtils.isNotEmpty((CharSequence)tpPayType.getDispatchUrl()) ? tpPayType.getDispatchUrl() : tpInterface.getDispatchUrl();
        final String dispatchMethod = StringUtils.isNotEmpty((CharSequence)tpPayType.getDispatchMethod()) ? tpPayType.getDispatchMethod() : tpInterface.getDispatchMethod();
        context.setName(tpInterface.getName());
        context.setVersion(version);
        context.setCharset(charset);
        context.setDispatchType(dispatchType);
        context.setDispatchUrl(dispatchUrl);
        context.setDispatchMethod(dispatchMethod);
    }
    
    private void calculateDiscountAmount(final RechargeOrder rechargeOrder, final RechargeConfig rechargeConfig, final boolean isFirstRecharge) {
        rechargeOrder.setDiscountAmount(0.0);
        if (this.blacklistService.isBlack(rechargeOrder.getUserAccount(), BlackEnum.BLACK_TYPE_YOUHUI.getValue())) {
            rechargeOrder.setRemarks("\u4f1a\u5458\u5df2\u5217\u5165\u4f18\u60e0\u9ed1\u540d\u5355\uff0c\u65e0\u6cd5\u4eab\u53d7\u4f18\u60e0\u653f\u7b56\u3002");
        }
        else if (rechargeConfig.getRechargeDiscountTrigger() == RechargeDiscountTrigger.ALWAYS.getValue()) {
            rechargeOrder.setDiscountRechargeFlag(BuildInDiscountType.RECHARGE.getRechargeFlag());
            rechargeOrder.setDiscountType(BuildInDiscountType.RECHARGE.getValue());
            rechargeOrder.setDiscountAmount(this.calculateDiscountAmount(rechargeOrder.getAmount(), rechargeConfig.getRechargeDiscountMode(), rechargeConfig.getRechargeDiscountValue(), rechargeConfig.getRechargeDiscountMaxAmount()));
        }
        else if (rechargeConfig.getRechargeDiscountTrigger() == RechargeDiscountTrigger.FIRST_RECHARGE.getValue() && isFirstRecharge) {
            rechargeOrder.setDiscountRechargeFlag(BuildInDiscountType.FIRST_RECHARGE.getRechargeFlag());
            rechargeOrder.setDiscountType(BuildInDiscountType.FIRST_RECHARGE.getValue());
            rechargeOrder.setDiscountAmount(this.calculateDiscountAmount(rechargeOrder.getAmount(), rechargeConfig.getRechargeDiscountMode(), rechargeConfig.getRechargeDiscountValue(), rechargeConfig.getRechargeDiscountMaxAmount()));
        }
    }
    
    private Double calculateDiscountAmount(final Double amount, final Integer mode, final Double value, final Double maxDiscountAmount) {
        Double discountAmount = 0.0;
        if (mode == RechargeDiscountMode.PERCENT.getValue()) {
            discountAmount = amount * value / 100.0;
        }
        else if (mode == RechargeDiscountMode.RAW_VALUE.getValue()) {
            discountAmount = value;
        }
        return (maxDiscountAmount != null && maxDiscountAmount > 0.0) ? Math.min(discountAmount, maxDiscountAmount) : discountAmount;
    }
    
    private void recalculateDiscountAmount(final RechargeOrder rechargeOrder, final boolean isFirstRecharge) {
        if (rechargeOrder.getMode() != RechargeMode.MANUAL.getValue() && rechargeOrder.getDiscountAmount() > 0.0 && rechargeOrder.getDiscountRechargeFlag() == BuildInDiscountType.FIRST_RECHARGE.getRechargeFlag() && rechargeOrder.getDiscountType() == BuildInDiscountType.FIRST_RECHARGE.getValue() && isFirstRecharge) {
            rechargeOrder.setDiscountAmount(0.0);
            rechargeOrder.setDiscountDml(0.0);
            rechargeOrder.setDiscountRechargeFlag(null);
            rechargeOrder.setDiscountType(null);
            rechargeOrder.setTotalAmount(rechargeOrder.getAmount());
        }
    }
    
    private void calculateDml(final RechargeOrder rechargeOrder, final RechargeConfig rechargeConfig) {
        rechargeOrder.setNormalDml(rechargeOrder.getAmount() * rechargeConfig.getNormalDmlRate() / 100.0);
        if (rechargeOrder.getDiscountAmount() > 0.0) {
            rechargeOrder.setDiscountDml(rechargeOrder.getDiscountAmount() * rechargeConfig.getDiscountDmlMultiple());
        }
        else {
            rechargeOrder.setDiscountDml(0.0);
        }
    }
    
    private void updateRechargeOrder(final RechargeOrder rechargeOrder, final String auditRemarks, final String operator, final Integer status, final UserInfo userInfo) throws Exception {
        final Integer curStatus = rechargeOrder.getStatus();
        final Date now = new Date();
        rechargeOrder.setStatus(status);
        rechargeOrder.setUpdateTime(now);
        rechargeOrder.setAuditTime(now);
        rechargeOrder.setAuditorAccount(operator);
        rechargeOrder.setAuditRemarks(auditRemarks);
        if (null != userInfo) {
            rechargeOrder.setUserColor(userInfo.getUserColor());
        }
        if (this.rechargeOrderDao.updateStatus(rechargeOrder.getId(), status, curStatus, now, operator, auditRemarks, rechargeOrder.getDiscountAmount(), rechargeOrder.getDiscountDml(), rechargeOrder.getTotalAmount(), rechargeOrder.getDiscountRechargeFlag(), rechargeOrder.getDiscountType()) != 1) {
            throw new BusinessException("\u8ba2\u5355\u72b6\u6001\u5f02\u5e38");
        }
        final RechargeOrderHistory rechargeOrderHistory = new RechargeOrderHistory();
        BeanUtils.copyProperties(rechargeOrderHistory, rechargeOrder);
        this.rechargeOrderHistoryDao.save(rechargeOrderHistory);
    }
    
    private void updateRWReport(final RechargeOrder rechargeOrder, final UserInfo userInfo, final UserExtInfo userExtInfo) throws Exception {
        if (!SysUserTypes.VHY.getCode().equals(userInfo.getType())) {
            this.userRWReportService.addRecharge(userInfo, userExtInfo.getRechCount(), rechargeOrder);
        }
    }
    
    private void updateDml(final RechargeOrder rechargeOrder, final UserExtInfo userExtInfo) throws Exception {
        this.validWithdrawService.addRechargeOrderWithBalanceChecking(rechargeOrder, userExtInfo.getMoney());
    }
    
    private void addUserBill(final RechargeOrder rechargeOrder, final UserInfo userInfo, final UserExtInfo userExtInfo, final String operator) {
        final StringBuilder sb = new StringBuilder();
        final DecimalFormat df = new DecimalFormat("0.00");
        sb.append("\u8ba2\u5355\u7f16\u53f7 ").append(rechargeOrder.getOrderNo()).append("\uff0c\u5145\u503c\u91d1\u989d ").append(rechargeOrder.getAmount()).append("\uff0c\u4f18\u60e0\u91d1\u989d ").append(rechargeOrder.getDiscountAmount()).append("\uff0c\u4f59\u989d ").append(df.format(userExtInfo.getMoney() + rechargeOrder.getTotalAmount()));
        if (StringUtils.isNotEmpty((CharSequence)rechargeOrder.getPayType())) {
            sb.append("\uff0c\u652f\u4ed8\u7c7b\u578b ").append(rechargeOrder.getPayTypeName());
        }
        if (rechargeOrder.getPayAccountId() != null) {
            sb.append("\uff0c\u6536\u6b3e\u8d26\u53f7 ").append(rechargeOrder.getPayAccountAccount()).append("\uff0c\u6536\u6b3e\u4eba ").append(rechargeOrder.getPayAccountOwner());
            if (StringUtils.isNotEmpty((CharSequence)rechargeOrder.getPayAccountBankName())) {
                sb.append("\uff0c\u6536\u6b3e\u94f6\u884c ").append(rechargeOrder.getPayAccountBankName());
            }
        }
        sb.append("\u3002");
        if (StringUtils.isNotEmpty((CharSequence)rechargeOrder.getTpInterface())) {
            sb.append("\u7b2c\u4e09\u65b9\u63a5\u53e3 ").append(rechargeOrder.getTpInterfaceName());
        }
        this.userBillService.add(userInfo, userExtInfo.getMoney(), rechargeOrder.getOrderNo(), RechargeMode.getTranType(rechargeOrder.getMode()), rechargeOrder.getTotalAmount(), sb.toString(), operator);
    }
    
    private void updateRechargeMoney(final RechargeOrder rechargeOrder) {
        final RechargeBean rechargeBean = new RechargeBean();
        rechargeBean.setRechargeTimes(1);
        rechargeBean.setRechargeAmount(rechargeOrder.getAmount());
        if (rechargeOrder.getPayAccountId() != null) {
            this.payAccountService.updateRechargeMoney(rechargeOrder.getPayAccountId(), rechargeBean);
        }
        if (rechargeOrder.getTpMerchantId() != null) {
            this.tpMerchantService.updateRechargeMoney(rechargeOrder.getTpMerchantId(), rechargeBean);
        }
        if (rechargeOrder.getTpPayChannelId() != null) {
            this.tpPayChannelService.updateRechargeMoney(rechargeOrder.getTpPayChannelId(), rechargeBean);
        }
    }
    
    private void updateUserMoney(final RechargeOrder rechargeOrder) throws Exception {
        final UserMoneyBean userMoneyBean = new UserMoneyBean(rechargeOrder.getUserId());
        if (rechargeOrder.getPointFlag() == TrueFalse.TRUE.getValue()) {
            userMoneyBean.setPoints(rechargeOrder.getAmount());
        }
        if (rechargeOrder.getPointFlag() == TrueFalse.TRUE.getValue() && rechargeOrder.getAmount() > 0.0) {
            userMoneyBean.setRechCount(1);
            userMoneyBean.setRechMoney(rechargeOrder.getAmount());
            userMoneyBean.setRechTime(new Date());
        }
        userMoneyBean.setMoney(rechargeOrder.getTotalAmount());
        this.userService.updateMoney(userMoneyBean);
    }
    
    private void fillFk(final RechargeOrder rechargeOrder) {
        rechargeOrder.setFk(null);
    }
    
    private String getTpInterfaceCodeByRechargeOrder(final RechargeOrder rechargeOrder) throws Exception {
        final TpInterface tpInterface = this.getTpInterfaceByRechargeOrder(rechargeOrder);
        if (tpInterface == null || StringUtils.isEmpty((CharSequence)tpInterface.getCode())) {
            throw new Exception("\u7b2c\u4e09\u65b9\u63a5\u53e3\u5df2\u5173\u95ed");
        }
        return tpInterface.getCode();
    }
    
    private ThirdPartyCallbackContext getThirdPartyCallbackContextByRechargeOrder(final RechargeOrder rechargeOrder, final String callbackIp, final String asyncCallbackUrl, final String syncCallbackUrl, final UserEquipmentVO userEquipmentVO) throws Exception {
        final TpPayChannelVO tpPayChannel = this.tpPayChannelService.getVO(rechargeOrder.getTpPayChannelId());
        if (tpPayChannel == null) {
            throw new Exception("\u7b2c\u4e09\u65b9\u901a\u9053\u5df2\u5173\u95ed");
        }
        final TpMerchant tpMerchant = this.tpMerchantService.get(rechargeOrder.getTpMerchantId());
        if (tpMerchant == null) {
            throw new Exception("\u7b2c\u4e09\u65b9\u5546\u6237\u5df2\u5173\u95ed");
        }
        final TpInterface tpInterface = this.tpInterfaceService.get(tpMerchant.getTpInterface());
        if (tpInterface == null) {
            throw new Exception("\u7b2c\u4e09\u65b9\u63a5\u53e3\u5df2\u5173\u95ed");
        }
        final TpPayType tpPayType = this.tpPayTypeService.get(tpMerchant.getTpInterface(), tpPayChannel.getTpPayType());
        if (tpPayType == null) {
            throw new Exception("\u7b2c\u4e09\u65b9\u63a5\u53e3\u5df2\u5173\u95ed");
        }
        if (tpInterface.getIpFlg() != null && tpInterface.getIpFlg() == TrueFalse.TRUE.getValue() && StringUtils.isNotBlank((CharSequence)tpInterface.getIpWhiteList())) {
            if (StringUtils.isBlank((CharSequence)callbackIp)) {
                throw new Exception(String.format("\u56de\u8c03IP\u5f02\u5e38: %s", callbackIp));
            }
            final Set ipSet = Stream.of(tpInterface.getIpWhiteList().split(",")).filter(ip -> StringUtils.isNotBlank((CharSequence)ip)).collect(Collectors.toSet());
            if (!ipSet.isEmpty() && !ipSet.contains(callbackIp)) {
                throw new Exception(String.format("\u56de\u8c03IP\u5f02\u5e38: %s, \u767d\u540d\u5355: %s", callbackIp, tpInterface.getIpWhiteList()));
            }
        }
        final ThirdPartyCallbackContext context = new ThirdPartyCallbackContext();
        this.fillThirdPartyCallbackContext(context, tpInterface, tpPayType);
        context.setName(tpInterface.getName());
        context.setOrderNo(rechargeOrder.getOrderNo());
        context.setOrderName(rechargeOrder.getOrderNo());
        context.setOrderTime(rechargeOrder.getAddTime());
        context.setAmount(rechargeOrder.getAmount());
        context.setMerchantParameters((Map<String, String>)JsonUtil.toMapObject(tpMerchant.getParameters()));
        context.setCallbackIp(callbackIp);
        final RechargeLimit rechargeLimit = this.limitInfoService.get(LimitEnums.RECHARGE_LIMIT.getName(), RechargeLimit.class);
        String callbackDomain = "";
        if (rechargeLimit != null) {
            callbackDomain = rechargeLimit.getCallbackDomain();
        }
        final String domain = StringUtils.isNotEmpty((CharSequence)callbackDomain) ? callbackDomain : userEquipmentVO.getBaseURL();
        context.setAsyncCallbackUrl(StringUtils.replace(domain + asyncCallbackUrl, "${orderNo}", rechargeOrder.getOrderNo()));
        context.setSyncCallbackUrl(StringUtils.replace(userEquipmentVO.getBaseURL() + syncCallbackUrl, "${orderNo}", rechargeOrder.getOrderNo()));
        return context;
    }
    
    private void fillThirdPartyCallbackContext(final ThirdPartyCallbackContext context, final TpInterface tpInterface, final TpPayType tpPayType) {
        context.setVersion(StringUtils.isNotEmpty((CharSequence)tpPayType.getVersion()) ? tpPayType.getVersion() : tpInterface.getVersion());
        context.setCharset(StringUtils.isNotEmpty((CharSequence)tpPayType.getCharset()) ? tpPayType.getCharset() : tpInterface.getCharset());
    }
    
    private void verifyUser(final UserInfo userInfo, final UserExtInfo userExtInfo, final boolean checkLimitRech) throws Exception {
        if (userInfo == null) {
            throw new BusinessException("UC/USER_NOT_EXIST", "uc.user_not_exist", (Object[])null);
        }
        if (userExtInfo == null) {
            throw new BusinessException("UC/USER_NOT_EXIST", "uc.user_not_exist", (Object[])null);
        }
        if (userInfo.getState() != UserStates.DEFAULT.getValue()) {
            throw new BusinessException("\u4f1a\u5458\u8d26\u53f7\u5df2\u51bb\u7ed3\u3002");
        }
        if (SysUserTypes.TEST.getCode().equals(userInfo.getType())) {
            throw new BusinessException("\u8bd5\u73a9\u4f1a\u5458\u65e0\u6cd5\u5145\u503c\u3002");
        }
        if (checkLimitRech && userInfo.getLimitRech() == TrueFalse.TRUE.getValue()) {
            throw new BusinessException("\u5165\u6b3e\u88ab\u9650\u5236\uff0c\u8bf7\u8054\u7cfb\u5ba2\u670d\u4eba\u5458\u3002");
        }
    }
    
    private void verifyRechargeLimit(final RechargeOrder rechargeOrder, final RechargeLimit rechargeLimit) throws Exception {
        final boolean disableTransferIfHasUnhandled = rechargeOrder.getMode() == RechargeMode.TRANSFER.getValue() && (rechargeLimit == null || rechargeLimit.getDisableTransferIfHasUnhandled() == null || rechargeLimit.getDisableTransferIfHasUnhandled() != TrueFalse.FALSE.getValue());
        if (disableTransferIfHasUnhandled && this.rechargeOrderDao.countByModeAndPayAccountIdAndUserIdAndAmountAndStatusIn(RechargeMode.TRANSFER.getValue(), rechargeOrder.getPayAccountId(), rechargeOrder.getUserId(), rechargeOrder.getAmount(), (List<Integer>)Stream.of(new Integer[] { RechargeStatus.UNHANDLED.getValue(), RechargeStatus.HANDLED.getValue() }).collect(Collectors.toList())) > 0) {
            throw new BusinessException("\u8ba2\u5355\u53d7\u7406\u4e2d\uff0c\u8bf7\u52ff\u91cd\u590d\u63d0\u4ea4\u3002");
        }
    }
    
    private void verifyPayAccount(final PayAccount payAccount, final String hyLevel) throws Exception {
        if (payAccount == null) {
            throw new BusinessException("\u6536\u6b3e\u8d26\u53f7\u5df2\u5173\u95ed\u3002");
        }
        if (payAccount.getStatus() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u6536\u6b3e\u8d26\u53f7\u5df2\u5173\u95ed\u3002");
        }
        if (StringUtils.isNotBlank((CharSequence)payAccount.getUserLevels()) && StringUtils.isNotBlank((CharSequence)hyLevel) && !StringUtils.contains((CharSequence)("," + payAccount.getUserLevels()), (CharSequence)("," + hyLevel + ","))) {
            throw new BusinessException("\u6536\u6b3e\u8d26\u53f7\u5df2\u5173\u95ed\u3002");
        }
        if (null != payAccount.getLimitInfo()) {
            final JSONObject json = JSON.parseObject(payAccount.getLimitInfo());
            final String limitStatus = json.getString("limitStatus");
            if (null != limitStatus && !"".equals(limitStatus) && limitStatus.equals("1") && json.getDouble("limitAmount") <= payAccount.getRechargeAmount()) {
                throw new BusinessException("\u6536\u6b3e\u8d26\u53f7\u5df2\u5173\u95ed\u3002");
            }
        }
    }
    
    private void verifyPayType(final PayType payType) throws Exception {
        if (payType == null) {
            throw new BusinessException("\u652f\u4ed8\u65b9\u5f0f\u5df2\u5173\u95ed\u3002");
        }
        if (payType.getStatus() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u652f\u4ed8\u65b9\u5f0f\u5df2\u5173\u95ed\u3002");
        }
    }
    
    private void verifyRechargeConfig(final RechargeConfig rechargeConfig) throws Exception {
        if (rechargeConfig == null) {
            throw new BusinessException("\u652f\u4ed8\u65b9\u5f0f\u5df2\u5173\u95ed\u3002");
        }
    }
    
    private void verifyTpPayChannel(final TpPayChannel tpPayChannel, final String hyLevel) throws Exception {
        if (tpPayChannel == null) {
            throw new BusinessException("\u652f\u4ed8\u901a\u9053\u5df2\u5173\u95ed\u3002");
        }
        if (tpPayChannel.getStatus() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u652f\u4ed8\u901a\u9053\u5df2\u5173\u95ed\u3002");
        }
        if (StringUtils.isNotBlank((CharSequence)tpPayChannel.getUserLevels()) && StringUtils.isNotBlank((CharSequence)hyLevel) && !StringUtils.contains((CharSequence)("," + tpPayChannel.getUserLevels()), (CharSequence)("," + hyLevel + ","))) {
            throw new BusinessException("\u652f\u4ed8\u901a\u9053\u5df2\u5173\u95ed\u3002");
        }
        if (null != tpPayChannel.getLimitInfo()) {
            final JSONObject json = JSON.parseObject(tpPayChannel.getLimitInfo());
            final String limitStatus = json.getString("limitStatus");
            if (null != limitStatus && !"".equals(limitStatus) && limitStatus.equals("1") && json.getDouble("limitAmount") <= tpPayChannel.getRechargeAmount()) {
                throw new BusinessException("\u652f\u4ed8\u901a\u9053\u5df2\u5173\u95ed\u3002");
            }
        }
    }
    
    private void verifyTpMerchant(final TpMerchant tpMerchant) throws Exception {
        if (tpMerchant == null) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u5546\u6237\u5df2\u5173\u95ed\u3002");
        }
        if (tpMerchant.getStatus() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u5546\u6237\u5df2\u5173\u95ed\u3002");
        }
    }
    
    private void verifyTpInterface(final TpInterface tpInterface, final TpPayType tpPayType, final String bankCode) throws Exception {
        if (tpInterface == null) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u63a5\u53e3\u5df2\u5173\u95ed\u3002");
        }
        if (tpInterface.getStatus() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u63a5\u53e3\u5df2\u5173\u95ed\u3002");
        }
        if (StringUtils.isNotBlank((CharSequence)bankCode) && (StringUtils.isEmpty((CharSequence)tpPayType.getBanks()) || !((Map)JsonUtil.toMapObject(tpPayType.getBanks())).containsKey(bankCode))) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u63a5\u53e3\u5df2\u5173\u95ed\u3002");
        }
    }
    
    private void verifyQueryTpInterface(final TpInterface tpInterface) throws Exception {
        if (tpInterface == null) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u67e5\u8be2\u63a5\u53e3\u5df2\u5173\u95ed\u3002");
        }
        if (tpInterface.getOrderQueryEnabled() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u7b2c\u4e09\u65b9\u67e5\u8be2\u63a5\u53e3\u5df2\u5173\u95ed\u3002");
        }
    }
    
    private void verifyPeriod(final UserInfo userInfo, final PayType payType, final RechargeConfig rechargeConfig) throws Exception {
        final Date date = new Date();
        date.setTime(date.getTime() - rechargeConfig.getMinPeriod() * 1000L);
        final RechargeOrder rechargeOrder = this.rechargeOrderDao.getByUserIdAndPayTypeAndAddTimeGt(userInfo.getUserId(), payType.getCode(), date);
        if (rechargeOrder != null) {
            throw new BusinessException("\u64cd\u4f5c\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002");
        }
    }
    
    private void verifyAmount(final Double amount, final RechargeConfig rechargeConfig) throws Exception {
        if (amount < 0.0) {
            throw new BusinessException("\u65e0\u6548\u7684\u5145\u503c\u91d1\u989d\u3002");
        }
        if (rechargeConfig.getMaxAmount() != null && rechargeConfig.getMaxAmount() > 0.0 && amount > rechargeConfig.getMaxAmount()) {
            throw new BusinessException("\u5145\u503c\u91d1\u989d\u8d85\u8fc7\u4e0a\u9650\u3002");
        }
        if (rechargeConfig.getMinAmount() != null && rechargeConfig.getMinAmount() > 0.0 && amount < rechargeConfig.getMinAmount()) {
            throw new BusinessException("\u5145\u503c\u91d1\u989d\u4f4e\u4e8e\u4e0b\u9650\u3002");
        }
    }
    
    private double addRiskMoney(final Double amount, final RechargeConfig rechargeConfig, final TpPayType tpPayType, final TpInterface tpInterface) throws Exception {
        if (null != rechargeConfig.getRiskMoney() && !"".equals(rechargeConfig.getRiskMoney())) {
            final String[] str = rechargeConfig.getRiskMoney().split(",");
            final int index = (int)(Math.random() * str.length);
            final String riskMoney = str[index];
            final BigDecimal bigDecimal1 = new BigDecimal(riskMoney);
            final BigDecimal bigDecimal2 = new BigDecimal(amount.toString());
            return bigDecimal1.add(bigDecimal2).doubleValue();
        }
        if (null != tpPayType.getRiskMoney() && !"".equals(tpPayType.getRiskMoney())) {
            final String[] str = tpPayType.getRiskMoney().split(",");
            final int index = (int)(Math.random() * str.length);
            final String riskMoney = str[index];
            final BigDecimal bigDecimal1 = new BigDecimal(riskMoney);
            final BigDecimal bigDecimal2 = new BigDecimal(amount.toString());
            return bigDecimal1.add(bigDecimal2).doubleValue();
        }
        if (null != tpInterface.getRiskMoney() && !"".equals(tpInterface.getRiskMoney())) {
            final String[] str = tpInterface.getRiskMoney().split(",");
            final int index = (int)(Math.random() * str.length);
            final String riskMoney = str[index];
            final BigDecimal bigDecimal1 = new BigDecimal(riskMoney);
            final BigDecimal bigDecimal2 = new BigDecimal(amount.toString());
            return bigDecimal1.add(bigDecimal2).doubleValue();
        }
        return amount;
    }
    
    private double splitMoney(final Double amount) {
        final DecimalFormat decimalFormat = new DecimalFormat("###################.###########");
        final String money = decimalFormat.format(amount);
        if (money.contains(".")) {
            final String[] amounts = money.split("\\.");
            if (amounts[1].length() > 2) {
                final String returnMoney = amounts[0] + "." + amounts[1].substring(0, 2);
                return Double.valueOf(returnMoney);
            }
        }
        return amount;
    }
    
    private void verifyAmount(final Double amount, final Double discountAmount) throws Exception {
        if (amount == null || amount < 0.0) {
            throw new BusinessException("\u65e0\u6548\u7684\u5145\u503c\u91d1\u989d\u3002");
        }
        if (amount == 0.0 && (discountAmount == null || discountAmount <= 0.0)) {
            throw new BusinessException("\u65e0\u6548\u7684\u5145\u503c\u91d1\u989d\u3002");
        }
    }
    
    private void verifyDiscountType(final DiscountType discountType) throws Exception {
        if (discountType == null) {
            throw new BusinessException("\u4f18\u60e0\u7c7b\u578b\u5df2\u7981\u7528\u3002");
        }
        if (discountType.getStatus() != SwitchStatus.ENABLED.getValue()) {
            throw new BusinessException("\u4f18\u60e0\u7c7b\u578b\u5df2\u7981\u7528\u3002");
        }
    }
    
    private void verifyRechargeOrderForAuditing(final RechargeOrder rechargeOrder) throws Exception {
        if (rechargeOrder == null) {
            throw new BusinessException("\u65e0\u6548\u7684\u5145\u503c\u8ba2\u5355\u3002");
        }
        if (rechargeOrder.getAmount() < 0.0 || rechargeOrder.getTotalAmount() < 0.0) {
            throw new BusinessException("\u65e0\u6548\u7684\u5145\u503c\u91d1\u989d\u3002");
        }
        if (rechargeOrder.getStatus() != RechargeStatus.UNHANDLED.getValue() && rechargeOrder.getStatus() != RechargeStatus.HANDLED.getValue()) {
            throw new BusinessException("\u8ba2\u5355\u5df2\u5904\u7406\u3002");
        }
    }
    
    public PageData<RechargeOrder> masterQueryPage(final RechargeOrderQuery query, final PageBean pageBean) {
        return this.queryPage(query, pageBean);
    }
    
    private PageData<RechargeOrder> queryPage(final RechargeOrderQuery query, final PageBean pageBean) {
        return this.rechargeOrderDao.queryPage(query, pageBean);
    }
    
    public PageData<RechargeOrder> slaveQueryPage(final RechargeOrderQuery query, final PageBean pageBean) {
        return this.queryPage(query, pageBean);
    }
    
    public int masterCountUnhandled(final List<String> userLevels) {
        return this.countUnhandled(userLevels);
    }
    
    private int countUnhandled(final List<String> userLevels) {
        return this.rechargeOrderDao.countByStatusAndModeInAndUserLevelIn(RechargeStatus.UNHANDLED.getValue(), aquireTransferAndManualMode(), userLevels);
    }
    
    public int slaveCountUnhandled(final List<String> userLevels) {
        return this.countUnhandled(userLevels);
    }
    
    public Map<String, Object> masterCountUnhandledAmount(final List<String> userLevels) {
        return this.countUnhandledAmount(userLevels);
    }
    
    private Map<String, Object> countUnhandledAmount(final List<String> userLevels) {
        final Map<String, Object> otherDataMap = new HashMap<String, Object>();
        final Double allUnhandledSum = this.rechargeOrderDao.countAmountByStatusAndModeInAndUserLevelIn(RechargeStatus.UNHANDLED.getValue(), aquireTransferAndManualMode(), userLevels);
        final Double allHandledSum = this.rechargeOrderDao.countAmountByStatusAndModeInAndUserLevelIn(RechargeStatus.HANDLED.getValue(), aquireTransferAndManualMode(), userLevels);
        otherDataMap.put("allUnhandledSum", allUnhandledSum);
        otherDataMap.put("allHandledSum", allHandledSum);
        return otherDataMap;
    }
    
    public Map<String, Object> slaveCountUnhandledAmount(final List<String> userLevels) {
        return this.countUnhandledAmount(userLevels);
    }
    
    private static final List<Integer> aquireTransferAndManualMode() {
        return Stream.of(new Integer[] { RechargeMode.TRANSFER.getValue(), RechargeMode.MANUAL.getValue() }).collect(Collectors.toList());
    }
    
    public PageData<RechargeOrderHistory> slaveQueryHistoryPage(final RechargeOrderHistoryQuery query, final PageBean pageBean) {
        return this.rechargeOrderHistoryDao.queryPage(query, pageBean);
    }
    
    public RechargeOrderHistory queryAllSum(final RechargeOrderHistoryQuery query) throws Exception {
        return this.rechargeOrderHistoryDao.queryAllSum(query);
    }
    
    public List<RechargeOrderHistory> queryAllHistory(final RechargeOrderHistoryQuery query) throws Exception {
        return this.rechargeOrderHistoryDao.queryAll(query);
    }
    
    public List<RechargeOrderHistoryVO> queryGroupByAccountId(final RechargeOrderHistoryQuery query) throws Exception {
        return this.rechargeOrderHistoryDao.queryGroupByAccountId(query);
    }
    
    public List<DayRechargeOrderVO> slaveQueryDayRechargeOrderVO(final RechargeOrderHistoryQuery query) throws Exception {
        return this.rechargeOrderHistoryDao.queryDayRechargeOrderVO(query);
    }
    
    public List<RechargeOrderHistory> queryGroupByMerchantId(final RechargeOrderHistoryQuery query) throws Exception {
        return this.rechargeOrderHistoryDao.queryGroupByMerchantId(query);
    }
    
    public void cleanData(final Date addTime) {
        this.rechargeOrderDao.clean(addTime);
    }
    
    public void cleanHistoryData(final Date addTime) {
        this.rechargeOrderHistoryDao.clean(addTime);
    }
    
    public void activityDiscount(final Long userId, final Double amount, final Double dml, final String remarks, final String operator) throws Exception {
        this.discount(userId, BuildInDiscountType.REDENVELOPE, amount, dml, TranTypes.ACTIVITY_DISCOUNT, remarks, operator);
    }
    
    private RechargeOrder buildManualRechargeOrder(final ManualRechargeOrderBo manualRechargeOrderBo) throws Exception {
        final RechargeOrder rechargeOrder = new RechargeOrder();
        final UserInfo userInfo = this.userService.getUserInfo(manualRechargeOrderBo.getUserId());
        final UserExtInfo userExtInfo = this.userService.getUserExtInfo(manualRechargeOrderBo.getUserId());
        this.verifyUser(userInfo, userExtInfo, false);
        this.fillUserInfo(rechargeOrder, userInfo, userExtInfo);
        this.verifyAmount(manualRechargeOrderBo.getAmount(), manualRechargeOrderBo.getDiscountAmount());
        rechargeOrder.setAmount(manualRechargeOrderBo.getAmount());
        if (manualRechargeOrderBo.getPointFlag() == TrueFalse.TRUE.getValue() && manualRechargeOrderBo.getDiscountType() != null) {
            final DiscountType discountType = this.discountTypeService.getByValue(manualRechargeOrderBo.getDiscountType());
            this.verifyDiscountType(discountType);
            rechargeOrder.setDiscountRechargeFlag(discountType.getRechargeFlag());
            rechargeOrder.setDiscountType(discountType.getValue());
            rechargeOrder.setDiscountAmount(manualRechargeOrderBo.getDiscountAmount());
            rechargeOrder.setDiscountDml(manualRechargeOrderBo.getDiscountDml());
        }
        else {
            final RechargeLimit rechargeLimit = this.limitInfoService.get("rechargeLimit", RechargeLimit.class);
            if (rechargeLimit.getArtificialRechargeDiscount() == null || rechargeLimit.getArtificialRechargeDiscount() == 0) {
                rechargeOrder.setDiscountAmount(0.0);
                rechargeOrder.setDiscountDml(0.0);
            }
            else {
                rechargeOrder.setDiscountRechargeFlag(0);
                rechargeOrder.setDiscountType(0);
                if (rechargeLimit.getArtificialRechargeDiscount() == 1) {
                    final double discount = manualRechargeOrderBo.getAmount() * (rechargeLimit.getArtificialRechargeDiscountValue() / 100.0);
                    rechargeOrder.setDiscountAmount(discount);
                    rechargeOrder.setDiscountDml(discount);
                }
                else {
                    final double discount = rechargeLimit.getArtificialRechargeDiscountValue();
                    rechargeOrder.setDiscountAmount(discount);
                    rechargeOrder.setDiscountDml(discount);
                }
            }
        }
        rechargeOrder.setTotalAmount(manualRechargeOrderBo.getAmount() + rechargeOrder.getDiscountAmount());
        rechargeOrder.setDmlFlag(manualRechargeOrderBo.getDmlFlag());
        rechargeOrder.setNormalDml(manualRechargeOrderBo.getNormalDml());
        rechargeOrder.setPointFlag(manualRechargeOrderBo.getPointFlag());
        rechargeOrder.setRemarks(manualRechargeOrderBo.getRemarks());
        rechargeOrder.setMode(RechargeMode.MANUAL.getValue());
        rechargeOrder.setStatus(RechargeStatus.UNHANDLED.getValue());
        rechargeOrder.setOrderNo(NumUtil.getOrderNo());
        rechargeOrder.setAddTime(new Date());
        rechargeOrder.setUserType(userInfo.getType());
        this.fillFk(rechargeOrder);
        return rechargeOrder;
    }
    
    private RechargeOrder buildTransferRechargeOrder(final TransferRechargeOrderBo transferRechargeOrderBo, final UserEquipmentVO userEquipmentVO) throws Exception {
        final RechargeOrder rechargeOrder = new RechargeOrder();
        UserInfo userInfo = null;
        PayAccount payAccount = null;
        UserExtInfo userExtInfo = null;
        if (StringUtils.isBlank((CharSequence)transferRechargeOrderBo.getUserAccount())) {
            userInfo = this.userService.getUserInfo(transferRechargeOrderBo.getUserId());
            userExtInfo = this.userService.getUserExtInfo(transferRechargeOrderBo.getUserId());
            this.verifyUser(userInfo, userExtInfo, true);
            this.fillUserInfo(rechargeOrder, userInfo, userExtInfo);
            payAccount = this.payAccountService.get(transferRechargeOrderBo.getPayAccountId());
            this.verifyPayAccount(payAccount, userInfo.getHyLevel());
            this.fillPayAccountInfo(rechargeOrder, payAccount);
            this.fillClientInfo(rechargeOrder, userEquipmentVO);
        }
        else {
            userInfo = this.userService.getUserInfo(transferRechargeOrderBo.getUserAccount());
            userExtInfo = this.userService.getUserExtInfo(userInfo.getUserId());
            this.verifyUser(userInfo, userExtInfo, true);
            this.fillUserInfo(rechargeOrder, userInfo, userExtInfo);
            payAccount = this.payAccountService.querySubAccounts(transferRechargeOrderBo.getAccount()).get(0);
            this.verifyPayAccount(payAccount, userInfo.getHyLevel());
            this.fillPayAccountInfo(rechargeOrder, payAccount);
            transferRechargeOrderBo.setRechargeTime(DateUtil.strToDate(transferRechargeOrderBo.getOrderTime(), "yyyyMMddHHmmss"));
            rechargeOrder.setTpOrderNo(transferRechargeOrderBo.getTpOrderNo());
        }
        rechargeOrder.setUserType(userInfo.getType());
        final PayType payType = this.payTypeService.get(payAccount.getPayType());
        this.verifyPayType(payType);
        this.fillPayTypeInfo(rechargeOrder, payType);
        final RechargeConfig rechargeConfig = this.rechargeConfigService.getByModeAndPayType(RechargeMode.TRANSFER.getValue(), payType.getCode(), userInfo);
        this.verifyRechargeConfig(rechargeConfig);
        this.verifyAmount(transferRechargeOrderBo.getAmount(), rechargeConfig);
        rechargeOrder.setAmount(transferRechargeOrderBo.getAmount());
        this.verifyPeriod(userInfo, payType, rechargeConfig);
        this.calculateDiscountAmount(rechargeOrder, rechargeConfig, userExtInfo.getRechCount() == 0);
        rechargeOrder.setTotalAmount(transferRechargeOrderBo.getAmount() + rechargeOrder.getDiscountAmount());
        rechargeOrder.setDmlFlag(TrueFalse.TRUE.getValue());
        this.calculateDml(rechargeOrder, rechargeConfig);
        rechargeOrder.setPointFlag(TrueFalse.TRUE.getValue());
        rechargeOrder.setMode(RechargeMode.TRANSFER.getValue());
        rechargeOrder.setStatus(RechargeStatus.UNHANDLED.getValue());
        rechargeOrder.setOrderNo(NumUtil.getOrderNo());
        rechargeOrder.setAddTime(new Date());
        rechargeOrder.setRechargePerson(transferRechargeOrderBo.getRechargePerson());
        rechargeOrder.setRechargeTime(transferRechargeOrderBo.getRechargeTime());
        this.fillFk(rechargeOrder);
        final RechargeLimit rechargeLimit = this.limitInfoService.get("rechargeLimit", RechargeLimit.class);
        this.verifyRechargeLimit(rechargeOrder, rechargeLimit);
        final StringBuffer buffer = new StringBuffer();
        if (StringUtils.isNotBlank((CharSequence)transferRechargeOrderBo.getFriendAccount())) {
            buffer.append("\u8f6c\u8d26\u5907\u6ce8: ").append(transferRechargeOrderBo.getFriendAccount() + ".");
        }
        if (StringUtils.isNotBlank((CharSequence)transferRechargeOrderBo.getRemark())) {
            buffer.append(" \u4f1a\u5458\u5907\u6ce8\uff1a").append(transferRechargeOrderBo.getRemark());
        }
        rechargeOrder.setRemarks(buffer.toString());
        return rechargeOrder;
    }
    
    public String orderDeposit(final TransferRechargeOrderBo transferRechargeOrderBo) throws Exception {
        try {
            RechargeOrderService.LOGGER.info("\u516c\u4f17\u53f7\u5f02\u6b65\u56de\u8c03\u63a5\u6536\u62a5\u6587\u4e3a:{}", (Object)transferRechargeOrderBo.toString());
            final OfficialAccountsConfig config = this.officialAccountsConfigService.queryConfig();
            if (!this.verifyMd5Sign(transferRechargeOrderBo, config)) {
                throw new Exception("\u7b7e\u540d\u9519\u8bef,\u5165\u6b3e\u5931\u8d25\uff01");
            }
            final RechargeOrder rechargeOrder = this.buildTransferRechargeOrder(transferRechargeOrderBo, null);
            this.rechargeOrderDao.save(rechargeOrder);
            if (TrueFalse.TRUE.getValue() == config.getSkipAuditing()) {
                this.accept(rechargeOrder, "\u76f4\u63a5\u5165\u6b3e", null);
            }
            return "sucess";
        }
        catch (Exception e) {
            RechargeOrderService.LOGGER.error("deal order deposit fail ! the errMsg is:{}", (Throwable)e);
            return "fail";
        }
    }
    
    private boolean verifyMd5Sign(final TransferRechargeOrderBo transferRechargeOrderBo, final OfficialAccountsConfig config) throws Exception {
        final StringBuilder signBuilder = new StringBuilder();
        signBuilder.append(transferRechargeOrderBo.getUserAccount()).append(transferRechargeOrderBo.getRechargePerson()).append(transferRechargeOrderBo.getAccount()).append(MoneyUtils.toCentStr(transferRechargeOrderBo.getAmount())).append(transferRechargeOrderBo.getTpOrderNo()).append(transferRechargeOrderBo.getOrderTime()).append(config.getSignKey());
        final String sign = SecurityUtil.getMD5(signBuilder.toString(), config.getCharset()).toLowerCase();
        return StringUtils.equals((CharSequence)sign, (CharSequence)transferRechargeOrderBo.getSign());
    }
    
    @Transactional
    public String batchManualUpload(final String fileName, final boolean skipAuditing, final String operator) throws InvalidFormatException, IOException {
        final JSONObject result = new JSONObject();
        final List<String> notExist = new ArrayList<String>();
        final List<String> testUsers = new ArrayList<String>();
        final List<String> notNormal = new ArrayList<String>();
        final List<String> noLevel = new ArrayList<String>();
        final List<String> normalUser = new ArrayList<String>();
        try {
            Workbook workbook = null;
            if (fileName.contains(".xlsx")) {
                workbook = (Workbook)new XSSFWorkbook((InputStream)new FileInputStream(new File(fileName)));
            }
            else {
                workbook = (Workbook)new HSSFWorkbook((InputStream)new FileInputStream(new File(fileName)));
            }
            final Sheet sheet = workbook.getSheetAt(0);
            final List<Object> results = new ArrayList<Object>();
            final Map<Integer, String> beanpros = new HashMap<Integer, String>();
            beanpros.put(0, "account");
            beanpros.put(1, "amount");
            beanpros.put(2, "normalDml");
            beanpros.put(3, "discountAmount");
            beanpros.put(4, "discountDml");
            beanpros.put(5, "pointFlag");
            beanpros.put(6, "dmlFlag");
            beanpros.put(7, "discountType");
            beanpros.put(8, "remarks");
            final String s = this.readData(1, beanpros, "com.cz.gameplat.payment.bo.ManualRechargeOrderBo", results, sheet);
            if (s == null) {
                for (final Object obj : results) {
                    final ManualRechargeOrderBo bean = (ManualRechargeOrderBo)obj;
                    if (StringUtils.isBlank((CharSequence)bean.getAccount())) {
                        continue;
                    }
                    bean.setSkipAuditing(skipAuditing);
                    final UserInfo userInfo = this.userService.getUserInfo(bean.getAccount());
                    if (userInfo != null) {
                        if (SysUserTypes.TEST.equals(userInfo.getType())) {
                            testUsers.add(bean.getAccount());
                        }
                        else if (!UserStates.DEFAULT.getValue().equals(userInfo.getState())) {
                            notNormal.add(bean.getAccount());
                        }
                        else if (userInfo.getDlLevel() == null || userInfo.getHyLevel() == null) {
                            noLevel.add(bean.getAccount());
                        }
                        else {
                            normalUser.add(bean.getAccount());
                            bean.setUserId(userInfo.getUserId());
                            final RechargeOrder rechargeOrder = this.buildManualRechargeOrder(bean);
                            this.fillFk(rechargeOrder);
                            this.rechargeOrderDao.save(rechargeOrder);
                            if (!bean.isSkipAuditing()) {
                                continue;
                            }
                            this.accept(rechargeOrder, "\u76f4\u63a5\u5165\u6b3e", operator);
                        }
                    }
                    else {
                        notExist.add(bean.getAccount());
                    }
                }
            }
            result.put("notExist", (Object)notExist);
            result.put("testUsers", (Object)testUsers);
            result.put("notNormal", (Object)notNormal);
            result.put("noLevel", (Object)noLevel);
            result.put("normalUser", (Object)normalUser);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
        catch (Exception e3) {
            e3.printStackTrace();
        }
        LogUtil.info(JSON.toJSONString((Object)result));
        return JSON.toJSONString((Object)result);
    }
    
    public String readData(int beginRow, final Map<Integer, String> beanpros, final String classPathName, final List<Object> results, final Sheet sheet) {
        try {
            final Class clazz = Class.forName(classPathName);
            final Set<Integer> set = beanpros.keySet();
            for (Row row = sheet.getRow(beginRow); row != null; row = sheet.getRow(++beginRow)) {
                final Object obj = clazz.newInstance();
                for (final Integer key : set) {
                    Object value = null;
                    if (row != null) {
                        final Cell cell = row.getCell((int)key);
                        if (cell != null) {
                            final int type = cell.getCellType();
                            if (type == 1) {
                                value = cell.getStringCellValue();
                            }
                            else if (type == 0 || type == 2) {
                                value = cell.getNumericCellValue();
                                if (key > 4) {
                                    final DecimalFormat df = new DecimalFormat("#");
                                    value = Integer.parseInt(df.format(value));
                                }
                                if (key == 0) {
                                    cell.setCellType(1);
                                    value = cell.getStringCellValue();
                                }
                            }
                        }
                    }
                    final PropertyDescriptor pd = new PropertyDescriptor(beanpros.get(key), clazz);
                    final Method wM = pd.getWriteMethod();
                    wM.invoke(obj, value);
                }
                results.add(obj);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return "\u7528\u4e8e\u63a5\u53d7\u7ed3\u679c\u7684bean\u4e0d\u5b58\u5728";
        }
        return null;
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)RechargeOrderService.class);
    }
}
