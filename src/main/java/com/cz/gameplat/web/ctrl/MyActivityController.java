package com.cz.gameplat.web.ctrl;

import org.springframework.beans.factory.annotation.*;
import com.cz.gameplat.activity.service.*;
import com.cz.gameplat.sys.service.*;
import com.cz.gameplat.user.entity.*;
import com.cz.gameplat.web.interceptor.*;
import com.cz.framework.bean.*;
import com.cz.gameplat.sys.entity.*;
import java.time.*;
import com.cz.gameplat.sys.enums.*;
import com.cz.gameplat.sports.util.*;
import com.cz.gameplat.activity.constant.*;
import javax.servlet.http.*;
import com.cz.framework.web.*;
import com.cz.framework.*;
import com.cz.gameplat.activity.util.*;
import javax.validation.*;
import com.cz.gameplat.activity.model.*;
import org.apache.commons.lang.*;
import com.cz.gameplat.activity.enity.*;
import org.springframework.web.bind.annotation.*;
import com.cz.gameplat.activity.bean.*;
import com.alibaba.fastjson.*;
import java.util.*;

@RestController
@RequestMapping({ "/api/activity" })
public class MyActivityController
{
    @Autowired
    private ActivityRecordService recordService;
    @Autowired
    private ActivityQualificationService qualificationService;
    @Autowired
    private ActivityWelfareDetailService welfareDetailService;
    @Autowired
    private ConfigService configService;
    private static Object lockobj;

    @RequestMapping(value = { "/queryMyActivityRecord" }, method = { RequestMethod.GET })
    public PageData<ActivityRecord> queryMyActivityRecord(final PageBean pageBean, @HY final UserInfo user) throws Exception {
        final ActivityRecordQueryBean arb = new ActivityRecordQueryBean();
        arb.setActivityRecordStatus(ActivityStatus.NORMAL.getType());
        arb.setHyLevel(user.getHyLevel());
        return (PageData<ActivityRecord>)this.recordService.slaveGetActivityRecords(pageBean, arb);
    }

    @RequestMapping(value = { "/getActivityRecord" }, method = { RequestMethod.GET })
    public ActivityRecordVO getActivityRecord(@RequestParam("id") final Optional<Long> id) throws Exception {
        return this.recordService.slaveGetActivityRecordVO(id.isPresent() ? id.get() : null);
    }

    @RequestMapping(value = { "/applyActivityRecord" }, method = { RequestMethod.POST })
    public ActivityMessage applyActivityRecord(@RequestParam("id") final Optional<Long> id, @HY final UserInfo user) throws Exception {
        return this.qualificationService.applyActivityRecord(id.isPresent() ? id.get() : null, user);
    }

    @RequestMapping(value = { "/queryMyActivityQualification" }, method = { RequestMethod.GET })
    public PageData<ActivityQualificationVO> queryMyActivityQualification(final PageBean pageBean, ActivityQualificationBean aqb, @HY final UserInfo user) throws Exception {
        if (aqb == null) {
            aqb = new ActivityQualificationBean();
        }
        aqb.setUserId(user.getUserId().toString());
        return (PageData<ActivityQualificationVO>)this.qualificationService.slaveQueryActivityQualificationsInfo(pageBean, aqb, ActivityType.MANPOWER.getType());
    }

    @RequestMapping(value = { "/queryMyActivityLottery" }, method = { RequestMethod.GET })
    public PageData<ActivityQualificationVO> queryMyActivityLottery(final PageBean pageBean, ActivityQualificationBean aqb, @HY final UserInfo user) throws Exception {
        if (aqb == null) {
            aqb = new ActivityQualificationBean();
        }
        aqb.setUserId(user.getUserId().toString());
        return (PageData<ActivityQualificationVO>)this.qualificationService.slaveQueryActivityQualificationsInfo(pageBean, aqb, ActivityType.AUTOMATIC.getType());
    }

    @RequestMapping(value = { "/getRedEnvelopeType" }, method = { RequestMethod.GET })
    public ActivityRecordVO getRedEnvelopeType() throws Exception {
        ActivityRecordVO arv = null;
        try {
            final Config config1 = this.configService.getByKey("redenvelope_id");
            final Config config2 = this.configService.getByKey("weekend_redenvelope_id");
            final Integer activityId1 = new Integer((null == config1) ? "-1" : config1.getConfigValue());
            final Integer activityId2 = new Integer((null == config2) ? "-1" : config2.getConfigValue());
            if ((null == activityId1 && null == activityId2) || (activityId1 == -1L && activityId2 == -1L)) {
                throw new Exception();
            }
            if (null != activityId1 && activityId1 != -1) {
                arv = this.recordService.slaveGetActivityRecordVO((long)activityId1);
                LogUtil.info("\u7ea2\u5305\u96e8" + arv);
                if (null != arv) {
                    arv.setType("1");
                }
            }
            if (null != activityId2 && activityId2 != -1 && null == arv) {
                arv = this.recordService.slaveGetActivityRecordVO((long)activityId2);
                LogUtil.info("\u5468\u672b\u7ea2\u5305" + arv);
                if (null != arv) {
                    arv.setType("0");
                }
                else {
                    new Exception();
                }
            }
        }
        catch (Exception e) {
            LogUtil.error(e.toString());
            e.printStackTrace();
            arv = new ActivityRecordVO();
            arv.setType("1");
            return arv;
        }
        return arv;
    }

    @RequestMapping(value = { "/getRedEnvelopeInfo" }, method = { RequestMethod.GET })
    public RedEnvelopeInfo getRedEnvelopeInfo() throws Exception {
        final RedEnvelopeInfo rei = new RedEnvelopeInfo();
        rei.setC_time(DateUtil.getNowTime());
        final LocalDate l = LocalDate.now();
        final String beginTime = this.recordService.slaveGetActivityConfig("redenvelope_beginTime");
        final String endTime = this.recordService.slaveGetActivityConfig("redenvelope_endTime");
        rei.setStart_time(l + " " + beginTime);
        rei.setEnd_time(l + " " + endTime);
        rei.setMaxhong(ActivityConstant.ACTIVITY_REDENVELOPE_COUNT);
        final Integer activityId = new Integer(this.recordService.slaveGetActivityConfig("redenvelope_id"));
        final ActivityRecordVO arv = this.recordService.slaveGetActivityRecordVO((long)activityId);
        if (arv == null || !ActivityStatus.NORMAL.getType().equals(arv.getActivityRecord().getActivityRecordStatus())) {
            rei.setStat("-404");
            rei.setRain(ActivityConstant.ACTIVITY_REDENVELOPE_NO_RAIN);
        }
        else {
            if (LocalTime.now().isAfter(LocalTime.parse(beginTime)) && LocalTime.now().isBefore(LocalTime.parse(endTime))) {
                rei.setStat("0");
                rei.setRain(ActivityConstant.ACTIVITY_REDENVELOPE_RAIN);
            }
            if (LocalTime.now().isBefore(LocalTime.parse(beginTime))) {
                rei.setStat("-1");
                rei.setRain(ActivityConstant.ACTIVITY_REDENVELOPE_NO_RAIN);
            }
            else if (LocalTime.now().isAfter(LocalTime.parse(endTime))) {
                rei.setStat("-1");
                rei.setStart_time(l.plusDays(1L) + " " + beginTime);
                rei.setEnd_time(l.plusDays(1L) + " " + endTime);
                rei.setRain(ActivityConstant.ACTIVITY_REDENVELOPE_NO_RAIN);
            }
        }
        return rei;
    }

    @RequestMapping(value = { "/getRedEnvelope" }, method = { RequestMethod.GET })
    public ActivityMessage getRedEnvelope(@HY final UserInfo user) throws Exception {
        final ActivityMessage am = new ActivityMessage();
        if (!SysUserTypes.HY.getCode().equals(user.getType())) {
            am.setCode(300);
            am.setMessage("\u8bd5\u73a9\u4f1a\u5458\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String blacks = this.recordService.slaveGetActivityConfig("activity_balcklist");
        if (StringUtil.isNotBlank(blacks) && Helper.checkUserInBlackList(user.getAccount(), blacks)) {
            am.setCode(300);
            am.setMessage("\u4e0d\u7b26\u5408\u8be5\u6d3b\u52a8\u8d44\u683c!");
            return am;
        }
        final String beginTime = this.recordService.slaveGetActivityConfig("redenvelope_beginTime");
        final String endTime = this.recordService.slaveGetActivityConfig("redenvelope_endTime");
        if (!LocalTime.now().isAfter(LocalTime.parse(beginTime))) {
            am.setCode(300);
            am.setMessage("\u4eca\u5929\u7684\u6d3b\u52a8\u8fd8\u672a\u5f00\u59cb!");
            return am;
        }
        if (!LocalTime.now().isBefore(LocalTime.parse(endTime))) {
            am.setCode(300);
            am.setMessage("\u4eca\u5929\u7684\u6d3b\u52a8\u5df2\u7ed3\u675f\uff0c\u6b22\u8fce\u660e\u5929\u5728\u6765!");
            return am;
        }
        final String today = LocalDate.now().toString();
        final Integer activityId = new Integer(this.recordService.slaveGetActivityConfig("redenvelope_id"));
        final ActivityRecordVO arv = this.recordService.slaveGetActivityRecordVO((long)activityId);
        if (arv == null) {
            am.setCode(300);
            am.setMessage("\u65e0\u7ea2\u5305\u96e8\u6d3b\u52a8");
            return am;
        }
        if (!ActivityStatus.NORMAL.getType().equals(arv.getActivityRecord().getActivityRecordStatus())) {
            am.setCode(300);
            am.setMessage("\u7ea2\u5305\u96e8\u6d3b\u52a8\u672a\u5f00\u542f");
            return am;
        }
        final ActivityQualification aq = this.qualificationService.slaveHaveQualification(user.getUserId(), today, activityId);
        if (aq == null) {
            am.setCode(300);
            am.setMessage("\u60a8\u4eca\u5929\u65e0\u62bd\u5956\u8d44\u683c!");
            return am;
        }
        if (QualificationStatus.INVALID.getType().equals(aq.getStatus())) {
            am.setCode(300);
            am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u683c\u65e0\u6548!");
            return am;
        }
        if (QualificationStatus.APPLYING.getType().equals(aq.getStatus())) {
            am.setCode(300);
            am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u683c\u5f85\u5ba1\u6279!");
            return am;
        }
        if (QualificationStatus.REFUSED.getType().equals(aq.getStatus())) {
            am.setCode(300);
            am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u88ab\u62d2\u7edd,\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458!");
            return am;
        }
        if (aq.getCount() <= 0) {
            am.setCode(300);
            am.setMessage("\u60a8\u4eca\u5929\u7684\u62bd\u5956\u673a\u4f1a\u5df2\u7ecf\u7528\u5b8c!");
            return am;
        }
        am.setCode(200);
        am.setCount(aq.getCount() + "");
        return am;
    }

    @RequestMapping(value = { "/drawRedEnvelope" }, method = { RequestMethod.POST })
    public ActivityMessage drawRedEnvelope(@HY final UserInfo user, final HttpServletRequest request) throws Exception {
        final ActivityMessage am = new ActivityMessage();
        if (!SysUserTypes.HY.getCode().equals(user.getType())) {
            am.setCode(300);
            am.setMessage("\u8bd5\u73a9\u4f1a\u5458\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String blackList = this.recordService.slaveGetActivityConfig("activity_balcklist");
        if (StringUtil.isNotBlank(blackList) && Helper.checkUserInBlackList(user.getAccount(), blackList)) {
            am.setCode(300);
            am.setMessage("\u9ed1\u540d\u5355\u7528\u6237\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String beginTime = this.recordService.slaveGetActivityConfig("redenvelope_beginTime");
        final String endTime = this.recordService.slaveGetActivityConfig("redenvelope_endTime");
        if (!LocalTime.now().isAfter(LocalTime.parse(beginTime))) {
            am.setCode(300);
            am.setMessage("\u4eca\u5929\u7684\u6d3b\u52a8\u8fd8\u672a\u5f00\u59cb!");
            return am;
        }
        if (!LocalTime.now().isBefore(LocalTime.parse(endTime))) {
            am.setCode(300);
            am.setMessage("\u4eca\u5929\u7684\u6d3b\u52a8\u5df2\u7ed3\u675f\uff0c\u6b22\u8fce\u660e\u5929\u5728\u6765!");
            return am;
        }
        final String today = LocalDate.now().toString();
        final Integer activityId = new Integer(this.recordService.slaveGetActivityConfig("redenvelope_id"));
        final ActivityRecordVO arv = this.recordService.slaveGetActivityRecordVO((long)activityId);
        if (arv == null) {
            am.setCode(300);
            am.setMessage("\u65e0\u7ea2\u5305\u96e8\u6d3b\u52a8");
            return am;
        }
        if (!ActivityStatus.NORMAL.getType().equals(arv.getActivityRecord().getActivityRecordStatus())) {
            am.setCode(300);
            am.setMessage("\u7ea2\u5305\u96e8\u6d3b\u52a8\u672a\u5f00\u542f");
            return am;
        }
        synchronized (MyActivityController.lockobj) {
            final ActivityQualification aq = this.qualificationService.slaveHaveQualification(user.getUserId(), today, activityId);
            if (aq == null) {
                am.setCode(300);
                am.setMessage("\u60a8\u4eca\u5929\u65e0\u62bd\u5956\u8d44\u683c!");
                return am;
            }
            if (QualificationStatus.INVALID.getType().equals(aq.getStatus())) {
                am.setCode(300);
                am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u683c\u65e0\u6548!");
                return am;
            }
            if (QualificationStatus.APPLYING.getType().equals(aq.getStatus())) {
                am.setCode(300);
                am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u683c\u5f85\u5ba1\u6279!");
                return am;
            }
            if (QualificationStatus.REFUSED.getType().equals(aq.getStatus())) {
                am.setCode(300);
                am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u88ab\u62d2\u7edd,\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458!");
                return am;
            }
            if (QualificationStatus.IPLIMIT.getType().equals(aq.getStatus())) {
                am.setCode(300);
                am.setMessage("IP\u53d7\u9650,\u8bf7\u8054\u7cfb\u5ba2\u670d\u4eba\u5458!");
                return am;
            }
            if (aq.getCount() <= 0) {
                am.setCode(300);
                am.setMessage("\u60a8\u4eca\u5929\u7684\u62bd\u5956\u673a\u4f1a\u5df2\u7ecf\u7528\u5b8c!");
                return am;
            }
            if (null == aq.getExpand() || aq.getExpand() == "") {
                am.setCode(500);
                am.setMessage("\u7f51\u7edc\u5ef6\u8fdf\u83b7\u53d6\u7ea2\u5305\u5931\u8d25!");
                return am;
            }
            final String requestIp = HttpUtil.getforwardedForIP(request);
            if (Boolean.valueOf(this.recordService.slaveGetActivityConfig("redenvelope_is_ip_limit"))) {
                try {
                    final boolean isLimit = this.welfareDetailService.slaveIsLimit(requestIp, activityId, user.getAccount(), user.getUserId(), new Date());
                    if (!isLimit) {
                        am.setCode(200);
                        am.setMessage("\u540cIP\u5176\u4ed6\u4f1a\u5458\u5df2\u9886\u53d6\uff0c\u7cfb\u7edf\u65e0\u6cd5\u91cd\u590d\u6d3e\u53d1\uff01");
                        aq.setStatus(QualificationStatus.IPLIMIT.getType());
                        this.qualificationService.updateActivityQualification(aq);
                        return am;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            final Map<String, Object> obj = (Map<String, Object>)JsonUtil.toMapObject(aq.getExpand());
            Double maxMoney = 0.0;
            Double minMoney = 0.0;
            String bonus = "0.00";
            final Object obMoney = obj.get("money");
            if (obMoney != null) {
                minMoney = Double.parseDouble((null == obj.get("minMoney")) ? "0.0" : obj.get("minMoney").toString());
                maxMoney = Double.parseDouble((null == obj.get("maxMoney")) ? "1.0" : obj.get("maxMoney").toString());
                bonus = ActivityHelper.randomRedEnvelope((int)(Object)minMoney + "", (int)(Object)maxMoney + "");
            }
            final ActivityQualification acqu = this.qualificationService.consumeQualification(user, aq, today, Double.valueOf(bonus), activityId, requestIp);
            if (null == acqu) {
                am.setCode(200);
                am.setMessage("\u672c\u6b21\u62bd\u5956\u65e0\u6548,\u8bf7\u91cd\u65b0\u62bd");
            }
            else {
                am.setCode(200);
                am.setMessage(bonus + "");
            }
            final ActivityQualification aq2 = this.qualificationService.slaveHaveQualification(user.getUserId(), today, activityId);
            am.setCount(aq2.getCount() + "");
        }
        return am;
    }

    @RequestMapping(value = { "/queryMyWelfareDetailList" }, method = { RequestMethod.GET })
    public PageData<ActivityWelfareDetailVO> queryMyWelfareDetailList(final PageBean pageBean, @Valid ActivityWelfareDetailQueryBean awdqb, @HY final UserInfo user) throws Exception {
        if (awdqb == null) {
            awdqb = new ActivityWelfareDetailQueryBean();
        }
        awdqb.setUserId(user.getUserId() + "");
        return (PageData<ActivityWelfareDetailVO>)this.welfareDetailService.slaveQueryActivityWelfareDetailList(pageBean, awdqb);
    }

    @RequestMapping(value = { "/queryWelfareDetailList" }, method = { RequestMethod.GET })
    public List<ActivityWelfareDetailVO> queryWelfareDetailList(final PageBean pageBean) throws Exception {
        final List<ActivityWelfareDetailVO> result = new ArrayList<ActivityWelfareDetailVO>();
        final char[] title = "qwrtypsdfghjklzxcbnm".toCharArray();
        final char[] userNameChars = "qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
        final Random random = new Random();
        pageBean.setRows(110);
        final ActivityWelfareDetailQueryBean awdqb = new ActivityWelfareDetailQueryBean();
        final PageData<ActivityWelfareDetailVO> list = (PageData<ActivityWelfareDetailVO>)this.welfareDetailService.slaveQueryActivityWelfareDetailList(pageBean, awdqb);
        final List<ActivityWelfareDetailVO> da = (List<ActivityWelfareDetailVO>)list.getData();
        List<ActivityWelfareDetailVO> subList = new ArrayList<ActivityWelfareDetailVO>();
        subList = da;
        if (null != da && da.size() >= 100) {
            subList = da.subList(0, 99);
        }
//        final StringBuilder sb = n;
        subList.stream().forEach(e -> {
            if (StringUtils.isNotBlank(e.getAccount())) {
                if (e.getAccount().length() == 1) {
                    e.setAccount(e.getAccount() + "**");
                }
                if (e.getAccount().length() == 2) {
                    e.setAccount(e.getAccount() + "*");
                }
                if (e.getAccount().length() == 3) {
                    e.setAccount(e.getAccount() + "*");
                }
                if (e.getAccount().length() > 3) {
                    StringBuilder sb = new StringBuilder(e.getAccount());
                    e.setAccount(sb.replace(2, sb.toString().length() - 2, "***").toString());
                }
            }
            return;
        });
        result.addAll(subList);
        return result;
    }

    @RequestMapping(value = { "/queryUnrealWelfareDetailList" }, method = { RequestMethod.GET })
    public List<ActivityWelfareDetailVO> queryUnrealWelfareDetailList(final PageBean pageBean) throws Exception {
        final List<ActivityWelfareDetailVO> result = new ArrayList<ActivityWelfareDetailVO>();
        final char[] title = "qwrtypsdfghjklzxcbnm".toCharArray();
        final char[] userNameChars = "qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
        final Random random = new Random();
        for (int i = 0; i < 40; ++i) {
            final ActivityWelfareDetailVO temp = new ActivityWelfareDetailVO();
            temp.setAccount(RandomStringUtils.random(1, title) + RandomStringUtils.random(2, userNameChars) + RandomStringUtils.random(random.nextInt(5) + 2, "****"));
            temp.setMoney(Double.parseDouble(ActivityHelper.randomRedEnvelope("1000", "10000")));
            result.add(temp);
        }
        return result;
    }

    @RequestMapping(value = { "/drawRotaryTable" }, method = { RequestMethod.POST })
    public ActivityMessage drawRotaryTable(@HY final UserInfo user) throws Exception {
        final ActivityMessage am = new ActivityMessage();
        if (!SysUserTypes.HY.getCode().equals(user.getType())) {
            am.setCode(300);
            am.setMessage("\u8bd5\u73a9\u4f1a\u5458\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String blackList = this.recordService.slaveGetActivityConfig("activity_balcklist");
        if (StringUtil.isNotBlank(blackList) && Helper.checkUserInBlackList(user.getAccount(), blackList)) {
            am.setCode(300);
            am.setMessage("\u9ed1\u540d\u5355\u7528\u6237,\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String beginTime = this.recordService.slaveGetActivityConfig("rotary_switch_beginTime");
        final String endTime = this.recordService.slaveGetActivityConfig("rotary_switch_endTime");
        if (!LocalTime.now().isAfter(LocalTime.parse(beginTime))) {
            am.setCode(300);
            am.setMessage("\u4eca\u5929\u7684\u6d3b\u52a8\u8fd8\u672a\u5f00\u59cb!");
            return am;
        }
        if (!LocalTime.now().isBefore(LocalTime.parse(endTime))) {
            am.setCode(300);
            am.setMessage("\u4eca\u5929\u7684\u6d3b\u52a8\u5df2\u7ed3\u675f\uff0c\u6b22\u8fce\u660e\u5929\u5728\u6765!");
            return am;
        }
        final String today = LocalDate.now().toString();
        final Integer activityId = new Integer(this.recordService.slaveGetActivityConfig("rotary_switch_id"));
        final ActivityRecordVO arv = this.recordService.slaveGetActivityRecordVO((long)activityId);
        if (arv == null) {
            am.setCode(300);
            am.setMessage("\u65e0\u8f6c\u76d8\u6d3b\u52a8");
            return am;
        }
        if (!ActivityStatus.NORMAL.getType().equals(arv.getActivityRecord().getActivityRecordStatus())) {
            am.setCode(300);
            am.setMessage("\u8f6c\u76d8\u6d3b\u52a8\u672a\u5f00\u542f");
            return am;
        }
        final ActivityQualification aq = this.qualificationService.slaveHaveQualification(user.getUserId(), today, activityId);
        if (aq == null) {
            am.setCode(300);
            am.setMessage("\u60a8\u4eca\u5929\u65e0\u62bd\u5956\u8d44\u683c!");
            return am;
        }
        if (!QualificationStatus.APPLYED.getType().equals(aq.getStatus())) {
            am.setCode(300);
            am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u683c\u65e0\u6548!");
            return am;
        }
        if (aq.getCount() <= 0) {
            am.setCode(300);
            am.setMessage("\u60a8\u4eca\u5929\u7684\u62bd\u5956\u673a\u4f1a\u5df2\u7ecf\u7528\u5b8c!");
            return am;
        }
        final List<Prize> prizes = (List<Prize>)ActivityHelper.getPrizes(arv.getRuleList().get(0).getBlock());
        final int luckyPrizeId = ActivityHelper.drawRotaryTable((List)prizes);
        final Map<String, Object> r = (Map<String, Object>)ActivityHelper.getRotaryTableResult(luckyPrizeId, (List)prizes);
        this.qualificationService.consumeRotaryQualification(user, aq, today, activityId, luckyPrizeId, (List)prizes, arv);
        final JSONObject result = new JSONObject();
        result.put("angle", r.get("angle"));
        result.put("prize", r.get("prizeName"));
        result.put("money", r.get("money"));
        am.setCode(200);
        am.setMessage(result.toJSONString());
        return am;
    }

    @RequestMapping(value = { "/getWeekendRedEnvelope" }, method = { RequestMethod.POST })
    public ActivityMessage getWeekendRedEnvelope(@HY final UserInfo user, @RequestBody final String parameter) throws Exception {
        final ActivityMessage am = new ActivityMessage();
        if (StringUtil.isBlank(parameter)) {
            am.setCode(300);
            am.setMessage("\u65e0\u53c2");
            return am;
        }
        final Map m = (Map)JsonUtil.toMapObject(parameter);
        final String qujianId = m.get("id") + "";
        if (!SysUserTypes.HY.getCode().equals(user.getType())) {
            am.setCode(300);
            am.setMessage("\u8bd5\u73a9\u4f1a\u5458\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String blacks = this.recordService.slaveGetActivityConfig("activity_balcklist");
        if (StringUtil.isNotBlank(blacks) && Helper.checkUserInBlackList(user.getAccount(), blacks)) {
            am.setCode(300);
            am.setMessage("\u9ed1\u540d\u5355\u7528\u6237\u4e0d\u80fd\u53c2\u52a0\u6d3b\u52a8!");
            return am;
        }
        final String today = LocalDate.now().toString();
        final Integer activityId = new Integer(this.recordService.slaveGetActivityConfig("weekend_redenvelope_id"));
        final ActivityRecordVO arv = this.recordService.slaveGetActivityRecordVO((long)activityId);
        if (arv == null) {
            am.setCode(300);
            am.setMessage("\u65e0\u7ea2\u5305\u6d3b\u52a8");
            return am;
        }
        if (!ActivityStatus.NORMAL.getType().equals(arv.getActivityRecord().getActivityRecordStatus())) {
            am.setCode(300);
            am.setMessage("\u7ea2\u5305\u6d3b\u52a8\u672a\u5f00\u542f");
            return am;
        }
        final ActivityQualification aq = this.qualificationService.slaveHaveQualification(user.getUserId(), today, activityId);
        if (aq == null) {
            am.setCode(300);
            am.setMessage("\u60a8\u4eca\u5929\u65e0\u62bd\u5956\u8d44\u683c!");
            return am;
        }
        if (!QualificationStatus.APPLYED.getType().equals(aq.getStatus())) {
            am.setCode(300);
            am.setMessage("\u60a8\u7684\u62bd\u5956\u8d44\u683c\u65e0\u6548!");
            return am;
        }
        final List<ActivityWeekendRedenvelope> wrList = (List<ActivityWeekendRedenvelope>)JSONArray.parseArray(aq.getExpand(), (Class)ActivityWeekendRedenvelope.class);
        if (wrList == null || wrList.size() == 0) {
            am.setCode(300);
            am.setMessage("\u60a8\u65e0\u62bd\u5956\u8d44\u683c!");
            return am;
        }
        ActivityWeekendRedenvelope myActivityWeekendRedenvelope = null;
        for (final ActivityWeekendRedenvelope a : wrList) {
            final Integer tempId = a.getId() + 1;
            if (qujianId.equals(tempId.toString())) {
                myActivityWeekendRedenvelope = a;
                wrList.remove(a);
                break;
            }
        }
        if (myActivityWeekendRedenvelope == null) {
            am.setCode(300);
            am.setMessage("\u60a8\u65e0\u8be5\u62bd\u5956\u8d44\u683c!");
            return am;
        }
        if ("1".equals(myActivityWeekendRedenvelope.getIsUsed())) {
            am.setCode(300);
            am.setMessage("\u60a8\u5df2\u7ecf\u4f7f\u7528\u8be5\u62bd\u5956\u8d44\u683c!");
            return am;
        }
        final String hongbao = ActivityHelper.randomWeekendRedEnvelope(myActivityWeekendRedenvelope.getMinMoney(), myActivityWeekendRedenvelope.getMaxMoney());
        this.qualificationService.consumeWeekendRedPacketQualification(user, aq, today, Double.valueOf(hongbao), activityId, myActivityWeekendRedenvelope, (List)wrList);
        am.setCode(200);
        am.setCount(aq.getCount() + "");
        am.setMessage(hongbao.toString());
        return am;
    }

    static {
        MyActivityController.lockobj = new Object();
    }
}
