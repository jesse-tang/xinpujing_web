package com.cz.gameplat.web.ctrl;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.user.service.*;
import com.cz.gameplat.esports.service.*;
import com.cz.gameplat.sys.service.*;
import com.cz.gameplat.user.entity.*;
import com.cz.gameplat.web.interceptor.*;
import com.cz.framework.bean.*;
import com.cz.gameplat.esports.constants.*;
import java.util.function.*;
import java.util.stream.*;
import java.util.*;
import com.cz.framework.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.gameplat.sys.entity.*;
import com.cz.gameplat.esports.bean.*;
import javax.validation.*;
import org.springframework.web.bind.annotation.*;
import com.cz.framework.exception.*;
import com.cz.gameplat.user.bean.*;
import com.cz.gameplat.esports.entity.*;

@Controller
@RequestMapping({ "/api/eSports" })
public class ESportsCtrl
{
    @Resource
    EsportsMatchService esportsMatchService;
    @Resource
    EsportsCompetitionService competitionService;
    @Resource
    UserService userService;
    @Resource
    EsportsMatchTypeService matchTypeService;
    @Resource
    EsportsOrdersService ordersService;
    @Resource
    EsportsMatchResultService matchResultService;
    @Resource
    EsportsCompetitionTypeService competitionTypeService;
    @Resource
    private ConfigService configService;

    @RequestMapping(value = { "queryCompetitionOdds" }, method = { RequestMethod.GET })
    @ResponseBody
    public Map<Integer, EsportsCompetition> queryCompetitionOdds(final String ids) {
        return (Map<Integer, EsportsCompetition>)CollectionUtils.changeListToMap(this.competitionService.queryCompetitionByIds((List)JsonUtil.toObject(ids, (Class)List.class)), "id");
    }

    @RequestMapping(value = { "queryMatchType" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<EsportsMatchType> queryMatchType() {
        return (List<EsportsMatchType>)this.matchTypeService.getAll();
    }

    @RequestMapping(value = { "queryMatchDate" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Date> queryMatchDate() {
        return (List<Date>)this.esportsMatchService.queryValidMatchDate();
    }

    @RequestMapping(value = { "queryOrderPage" }, method = { RequestMethod.GET })
    @ResponseBody
    public PageData<EsportsOrders> queryOrderPage(@HY final UserInfo userInfo, final EsportsOrderQueryVO order, final PageBean pageBean) throws Exception {
        if (userInfo == null || userInfo.getUserId() == null) {
            return null;
        }
        order.setUserId(userInfo.getUserId());
        if (order.getQueryType() == null) {
            if (CollectionUtils.isEmpty((Collection)order.getStatusIn())) {
                return null;
            }
        }
        else if (order.getQueryType() == 0) {
            order.setStatusIn(JsonUtil.toJson((Object)Stream.of(new Long[] { EsportsOrderStatusEnum.NORMAL.getValue(), EsportsOrderStatusEnum.SETTLING.getValue(), EsportsOrderStatusEnum.RESET_SETTLED.getValue() }).toArray()));
        }
        else if (order.getQueryType() == 1) {
            order.setStatusIn(JsonUtil.toJson((Object)Stream.of(new Long[] { EsportsOrderStatusEnum.CANCEL.getValue(), EsportsOrderStatusEnum.SETTLED.getValue() }).toArray()));
        }
        if (order.getCreateDateEnd() != null) {
            order.setCreateDateEnd(DateUtil.getDateEnd(order.getCreateDateEnd()));
        }
        else if (order.getCreateDateStart() != null) {
            order.setCreateDateEnd(DateUtil.getDateStart(order.getCreateDateStart()));
        }
        final PageData<EsportsOrders> pageData = (PageData<EsportsOrders>)this.ordersService.slaveQueryOrderDetail(pageBean, order);
        final Map<String, Object> other = new HashMap<String, Object>();
        final Map<String, String> sMap = new HashMap<String, String>();
//        final String s = null;
//        Arrays.stream(EsportsOrderStatusEnum.values()).forEach(e -> s = sMap.put(String.valueOf(e.getValue()), e.getName()));
        other.put("status", sMap);
        pageData.setOtherData((Map)other);
        return pageData;
    }

    @RequestMapping(value = { "queryMatchPage" }, method = { RequestMethod.GET })
    @ResponseBody
    public PageData<EsportsMatch> queryPage(final EsportsMatchQueryVO vo, final PageBean pageBean) throws Exception {
        final PageData<EsportsMatch> matchs = (PageData<EsportsMatch>)this.esportsMatchService.queryWithPage(vo, pageBean);
        if (CollectionUtils.isNotEmpty((Collection)matchs.getData())) {
            final Set<Integer> matchTypeIds = (Set<Integer>)matchs.getData().stream().map(EsportsMatch::getMatchTypeId).collect(Collectors.toSet());
            final Map<Integer, EsportsMatchType> emt = (Map<Integer, EsportsMatchType>)CollectionUtils.changeListToMap((List)this.matchTypeService.getAll().stream().filter(e -> matchTypeIds.contains(e.getId())).collect(Collectors.toList()), "id");
            final Map<String, Object> otherData = new HashMap<String, Object>();
            otherData.put("matchTypes", emt);
            matchs.setOtherData((Map)otherData);
        }
        return matchs;
    }

    @RequestMapping(value = { "queryMatchCompetition" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<EsportsCompetition> queryPageCompetition(final Long matchId, final Long competitionTypeId) throws Exception {
        final List<EsportsCompetition> data = (List<EsportsCompetition>)this.competitionService.queryCompetition(matchId, competitionTypeId);
        final Set<Integer> typeIds = new HashSet<Integer>();
        final Set<Integer> set = new HashSet<>();
        if (this.competitionTypeService.getAll().stream().anyMatch(type -> Boolean.TRUE.equals(type.getAvaiable()) && !set.add(type.getType()))) {
            LogUtil.warn("==============================\u738b\u8005\u8363\u8000competitionType\u91cd\u590d!!!!!==========================");
            final Set<Integer> ids = new HashSet<Integer>();
            return data.stream().filter(ec -> ids.add(ec.getId())).collect(Collectors.toList());
        }
        return data;
    }

    @RequestMapping({ "getOpenStatus" })
    @ResponseBody
    public Map<String, String> getOpenStatus() throws Exception {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("status", "0");
        final Config liveIsOpen = this.configService.getByNameAndKey("live_config", "liveIsOpen");
        final Map<String, String> liveMap = new HashMap<String, String>();
        if (liveIsOpen != null && StringUtil.isNotBlank(liveIsOpen.getEnlargeMemo())) {
            liveMap.putAll((Map<? extends String, ? extends String>)JsonUtil.toObject(liveIsOpen.getEnlargeMemo(), (Class)HashMap.class));
        }
        if ("1".equals(liveMap.get(LiveGame.WZRY.getCode()))) {
            result.put("status", "1");
            result.put("msg", "\u6e38\u620f\u6682\u672a\u5f00\u653e,\u656c\u8bf7\u671f\u5f85...");
        }
        return result;
    }

    @RequestMapping(value = { "bet" }, method = { RequestMethod.POST })
    @ResponseBody
    public Map<String, Object> bet(@HY final UserInfo userInfo, @Valid @RequestBody final EsportsBetParams betParams) throws Exception {
        final Config liveIsOpen = this.configService.getByNameAndKey("live_config", "liveIsOpen");
        final Map<String, String> liveMap = new HashMap<String, String>();
        if (liveIsOpen != null && StringUtil.isNotBlank(liveIsOpen.getEnlargeMemo())) {
            liveMap.putAll((Map<? extends String, ? extends String>)JsonUtil.toObject(liveIsOpen.getEnlargeMemo(), (Class)HashMap.class));
        }
        if ("1".equals(liveMap.get(LiveGame.WZRY.getCode()))) {
            throw new BusinessException("LOGIN/1", "\u6e38\u620f\u6682\u672a\u5f00\u653e,\u656c\u8bf7\u671f\u5f85...", (Object[])null);
        }
        final UserInfoVO infoVO = this.userService.get(userInfo.getUserId());
        if (infoVO == null || infoVO.getExtInfo() == null) {
            throw new BusinessException("UC/USER_NOT_EXIST", "uc.user_not_exist", (Object[])null);
        }
        final Map<String, Object> responseData = new HashMap<String, Object>();
        final String statusKey = "Status";
        responseData.putAll(this.competitionService.bet(infoVO, betParams));
        responseData.put(statusKey, "S");
        return responseData;
    }

    @RequestMapping(value = { "queryMatchResult" }, method = { RequestMethod.GET })
    @ResponseBody
    public Map<String, Object> queryMatchResult(final Integer matchId) throws Exception {
        if (matchId == null) {
            return null;
        }
        final EsportsMatch em = this.esportsMatchService.get(matchId);
        final EsportsMatch esportsMatch = new EsportsMatch();
        final EsportsMatchType emt = (EsportsMatchType)this.matchTypeService.getAll().stream().filter(e -> e.getId() != null && e.getId().equals(esportsMatch.getMatchTypeId())).findFirst().orElse(null);
        final List<Map<String, Object>> types = (List<Map<String, Object>>)this.competitionTypeService.queryEnable(matchId);
        final EsportsMatchResult emr = new EsportsMatchResult();
        emr.setMatchId(matchId);
        emr.setEndStatus(true);
        final List<EsportsMatchResult> resultList = (List<EsportsMatchResult>)this.matchResultService.get(emr);
        final Map<String, Object> response = new HashMap<String, Object>();
        response.put("matchResult", resultList);
        response.put("matchType", emt);
        response.put("match", em);
        response.put("types", types);
        return response;
    }
}
