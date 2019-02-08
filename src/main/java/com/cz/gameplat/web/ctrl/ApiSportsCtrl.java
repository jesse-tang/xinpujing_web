package com.cz.gameplat.web.ctrl;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.sports.manager.*;
import org.apache.http.util.*;
import com.cz.framework.exception.*;
import java.text.*;
import org.springframework.web.bind.annotation.*;
import com.cz.gameplat.web.interceptor.*;
import com.cz.gameplat.user.entity.*;
import com.cz.gameplat.sports.constants.*;
import com.cz.framework.*;
import com.cz.gameplat.sys.entity.*;
import com.cz.gameplat.sports.util.*;
import java.util.*;
import com.cz.gameplat.sports.entity.*;
import com.cz.gameplat.sports.bean.*;
import com.cz.framework.bean.*;

@Controller
@RequestMapping({ "/api/sports" })
public class ApiSportsCtrl
{
    @Resource
    BetManager betManager;
    @Resource
    SportWebManager sportWebManager;

    @RequestMapping(value = { "/time" }, method = { RequestMethod.GET })
    @ResponseBody
    public HashMap<String, Object> time(final String type) {
        final HashMap<String, Object> result = new HashMap<String, Object>();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(11, calendar.get(11) - 12);
        result.put("timestamp", calendar.getTimeInMillis());
        return result;
    }

    @RequestMapping(value = { "/match" }, method = { RequestMethod.GET })
    @ResponseBody
    public MatchBean matchList(final String type, @RequestParam(required = false) final String date, @RequestParam(required = false, defaultValue = "-1") final int selection, @RequestParam(required = false, defaultValue = "1") int page, @RequestParam(required = false, defaultValue = "60") int size, @RequestParam(required = false, defaultValue = "") final String legName) throws BusinessException {
        page = Math.max(1, page);
        size = Math.max(20, size);
        size = Math.min(100, size);
        try {
            if (!TextUtils.isEmpty((CharSequence)date)) {
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateFormat.parse(date);
            }
        }
        catch (ParseException e) {
            throw new BusinessException("\u65f6\u95f4\u683c\u5f0f\u9519\u8bef");
        }
        return this.sportWebManager.slaveMatchList(type, date, page, size, selection, legName);
    }

    @RequestMapping(value = { "/matchNum" }, method = { RequestMethod.GET })
    @ResponseBody
    public MatchNum matchNum() throws BusinessException {
        final MatchNum matchNum = new MatchNum();
        matchNum.setFtnum(this.sportWebManager.slaveQueryMatchNum("ft_ft_r"));
        matchNum.setBknum(this.sportWebManager.slaveQueryMatchNum("bk_ft_all"));
        if (!this.sportWebManager.validateSportIsCollectRbRe()) {
            matchNum.setFtpnum(0);
            matchNum.setFtp_num(0);
            matchNum.setBkpnum(0);
            matchNum.setBkp_num(0);
        }
        else {
            matchNum.setFtpnum(this.sportWebManager.slaveQueryMatchNum("ft_rb_re"));
            matchNum.setFtp_num(matchNum.getFtpnum());
            matchNum.setBkpnum(this.sportWebManager.slaveQueryMatchNum("bk_rb_re"));
            matchNum.setBkp_num(matchNum.getBkpnum());
        }
        return matchNum;
    }

    @RequestMapping(value = { "/matchNumFu" }, method = { RequestMethod.GET })
    @ResponseBody
    public MatchFuNum matchNumFu() throws BusinessException {
        final MatchFuNum matchFuNum = new MatchFuNum();
        matchFuNum.setFtmnum(this.sportWebManager.slaveQueryMatchNum("ft_fu_r"));
        matchFuNum.setBkmnum(this.sportWebManager.slaveQueryMatchNum("bk_fu_all"));
        return matchFuNum;
    }

    @RequestMapping(value = { "/getMatch" }, method = { RequestMethod.POST })
    @ResponseBody
    public Map<String, List<SportEntityBase>> getMatch(@RequestBody final GetMatchParamAry data, @HY final String account) throws BusinessException {
        final GetMatchParam[] ary = data.getList();
        if (ary == null || ary.length == 0) {
            throw new BusinessException("\u53c2\u6570\u5f02\u5e38,\u8bf7\u5237\u65b0\u9875\u9762\u91cd\u8bd5");
        }
        final Map resultMap = new HashMap();
        final List<SportEntityBase> result = new ArrayList<SportEntityBase>();
        final List<Long> noAllowBetIds = new ArrayList<Long>();
        for (final GetMatchParam param : ary) {
            final SportEntityBase match = this.sportWebManager.slaveGetMatch(param.getSportType(), param.getGid());
            if (match != null) {
                result.add(match);
            }
            else {
                noAllowBetIds.add(param.getGid());
            }
        }
        if (result.size() == 0) {
            throw new BusinessException("\u8d5b\u4e8b\u5f02\u5e38\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u91cd\u8bd5");
        }
        final SportTypeEnum type = SportTypeEnum.getByName(ary[0].getSportType());
        final Map<String, Float> minMaxConfig = (Map<String, Float>)this.betManager.getMinMaxConfig(type, (SportEntityBase)result.get(0), account);
        resultMap.put("min", minMaxConfig.get("min"));
        resultMap.put("max", minMaxConfig.get("max"));
        resultMap.put("list", result);
        resultMap.put("ctrlStatus", "true");
        resultMap.put("ctrlItem", null);
        resultMap.put("noAllowBetIds", new ArrayList());
        return (Map<String, List<SportEntityBase>>)resultMap;
    }

    @RequestMapping(value = { "/bet" }, method = { RequestMethod.POST })
    @ResponseBody
    public BetSaveRespInfo bet(@RequestBody final BetParam param, @HY final UserInfo user) throws Exception {
        final Orders e = this.betManager.bet(param, user);
        final BetSaveRespInfo respInfo = new BetSaveRespInfo();
        respInfo.setO(e);
        final LastOrderInfo info = new LastOrderInfo();
        String sportTypeName = "";
        final String betTypeName = BetTypeEnum.getCategory(e.getBetType());
        if (e.getSportsType() > 5) {
            sportTypeName = "\u8db3\u7403" + SportTypeEnum.getDesc(e.getSportsType());
        }
        else {
            sportTypeName = "\u7bee\u7403" + SportTypeEnum.getDesc(e.getSportsType());
        }
        info.setSportTypeName(sportTypeName);
        if (e.getStrong()) {
            info.setRatioTeam(e.getTeam_h());
        }
        else {
            info.setRatioTeam(e.getTeam_c());
        }
        if (e.getBetType() == 0 || e.getBetType() == 9 || e.getBetType() == 12 || e.getBetType() == 15) {
            info.setRatioTeam(e.getTeam_h());
        }
        else if (e.getBetType() == 1 || e.getBetType() == 10 || e.getBetType() == 13 || e.getBetType() == 16) {
            info.setRatioTeam(e.getTeam_c());
        }
        info.setTeam_h(e.getTeam_h());
        info.setTeam_c(e.getTeam_c());
        info.setRatio(e.getRatio());
        info.setOdds(e.getOdds());
        info.setBetTypeName(betTypeName);
        if (e.getStart() != null) {
            info.setDatetime(DateUtil.getFormatDate(e.getStart()));
        }
        info.setGnumh((long)e.getGnum_h());
        info.setGnumc((long)e.getGnum_c());
        info.setLeague(e.getLeague());
        info.setMoney((double)e.getMoney());
        respInfo.setBet(info);
        respInfo.setSuccess(true);
        return respInfo;
    }

    @RequestMapping(value = { "/queryBet" }, method = { RequestMethod.GET })
    @ResponseBody
    public BetInfoBean queryBet(@HY final UserInfo userInfo, @RequestParam(required = false, defaultValue = "1") final int page, @RequestParam(required = false, defaultValue = "20") final int size, @RequestParam(required = false, defaultValue = "0") final Integer timeType) throws BusinessException {
        return this.sportWebManager.slaveQueryOrders(userInfo, page, size, timeType);
    }

    @RequestMapping(value = { "/queryAllBet" }, method = { RequestMethod.GET })
    @ResponseBody
    public BetInfoBean queryBet(@HY final UserInfo userInfo, @RequestParam(required = false, defaultValue = "1") final int page, @RequestParam(required = false, defaultValue = "20") final int size, final String beginDate, final String endDate, final int status, final String type, @RequestParam(required = false, defaultValue = "0") final Integer timeType) throws BusinessException {
        return this.sportWebManager.slaveQueryOrders(userInfo, page, size, beginDate, endDate, status, type, timeType);
    }

    @RequestMapping({ "getBetConfig" })
    @ResponseBody
    public SportsBetConfigs getBetConfig() {
        return this.sportWebManager.slaveGetBetConfig();
    }

    @RequestMapping({ "getMaintenanceTime" })
    @ResponseBody
    public Config getMaintenanceTime() {
        return this.sportWebManager.slaveGetMaintenanceTime();
    }

    @RequestMapping({ "getSportOnOff" })
    @ResponseBody
    public Map<String, String> getSportOnOff() {
        return (Map<String, String>)this.sportWebManager.slaveGetSportOnOff();
    }

    @RequestMapping({ "getSingleUserBetConfig" })
    @ResponseBody
    public SportsBetConfigs getSingleUserBetConfig(@HY final UserInfo userInfo) throws BusinessException {
        return this.sportWebManager.slaveGetSingleUserBetConfig(userInfo.getAccount());
    }

    @RequestMapping({ "message" })
    @ResponseBody
    public SportMessage getMessage() {
        return this.sportWebManager.slaveGetLastMessage();
    }

    @RequestMapping({ "queryBetByNotSettled" })
    @ResponseBody
    public List<LastOrderInfo> queryBetByNotSettled(@HY final UserInfo userInfo) throws BusinessException {
        final List<LastOrderInfo> list = new ArrayList<LastOrderInfo>();
        final List<Orders> ordersList = (List<Orders>)this.sportWebManager.slaveQueryBetByNotSettled((long)userInfo.getUserId());
        if (ordersList == null || ordersList.size() == 0) {
            return list;
        }
        for (final Orders e : ordersList) {
            final LastOrderInfo info = this.sportWebManager.setBeanLastOrderInfo((OrdersBase)e);
            final List<LastOrderInfo> chuans = new ArrayList<LastOrderInfo>();
            if (Helper.checkComprehensive(e.getSportsType())) {
                final List<OrdersChuan> chuanList = (List<OrdersChuan>)this.sportWebManager.slaveQueryOrdersChuanById((long)e.getOrderID());
                chuanList.forEach(chuan -> chuans.add(this.sportWebManager.setBeanLastOrderInfo(chuan)));
            }
            info.setChuans((List)chuans);
            if (info != null) {
                list.add(info);
            }
        }
        return list;
    }

    @RequestMapping({ "loadAccountHistory" })
    @ResponseBody
    public List<AccountHistory> loadAccountHistory(@HY final UserInfo userInfo, final int sportType) throws BusinessException {
        return (List<AccountHistory>)this.sportWebManager.slaveLoadAccountHistory(userInfo, sportType);
    }

    @RequestMapping({ "loadAccountHistoryDetail" })
    @ResponseBody
    public List<Orders> loadAccountHistoryDetail(@HY final UserInfo userInfo, final String date, final int sportType) throws BusinessException {
        return (List<Orders>)this.sportWebManager.slaveLoadAccountHistoryDetail(userInfo, date, sportType);
    }

    @RequestMapping({ "loadResult" })
    @ResponseBody
    public List loadResult(final LoadResultParam param) throws BusinessException {
        final boolean isFoot = param.getSt().equals("footBall");
        final String date = param.getStart();
        return this.sportWebManager.slaveLoadResult(isFoot, date);
    }

    @RequestMapping({ "validateGrounderConfig" })
    @ResponseBody
    public boolean validateGrounderConfig(final String sportType, final String betType, final Long gid) throws BusinessException {
        return this.sportWebManager.slaveValidateGrounderConfig(sportType, betType, gid);
    }

    @RequestMapping({ "getNews" })
    @ResponseBody
    public List<SportMessage> getNews(final int type) throws BusinessException {
        return (List<SportMessage>)this.sportWebManager.slaveGetNews(type);
    }

    @RequestMapping({ "/queryBetForUserCenter" })
    @ResponseBody
    public PageData<Orders> queryBetForUserCenter(@HY final UserInfo userInfo, final PageBean bean, final QueryOrderForUserCenterParam param) {
        return (PageData<Orders>)this.sportWebManager.slaveQueryBetForUserCenterPageData(bean, userInfo, param);
    }

    @RequestMapping({ "getLowRateConfig" })
    @ResponseBody
    public String getLowRateConfig() {
        return this.sportWebManager.slaveGetLowRateConfig();
    }
}
