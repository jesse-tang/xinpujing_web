package com.cz.gameplat.live.web.ctrl;

import org.springframework.stereotype.*;
import javax.annotation.*;
import com.cz.gameplat.live.core.service.*;
import com.cz.gameplat.sys.service.*;
import com.cz.gameplat.user.service.*;
import com.cz.gameplat.live.core.config.*;
import com.cz.gameplat.user.entity.*;
import com.cz.gameplat.web.interceptor.*;
import com.cz.gameplat.sys.enums.*;
import com.cz.framework.exception.*;
import com.cz.framework.*;
import com.cz.gameplat.sys.entity.*;
import com.cz.gameplat.live.core.constants.*;
import com.cz.framework.bean.*;
import org.springframework.web.bind.annotation.*;
import com.cz.framework.web.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.text.*;
import com.cz.gameplat.live.core.bean.*;
import com.cz.gameplat.live.core.entity.*;
import java.net.*;
import javax.servlet.http.*;
import org.slf4j.*;

@Controller
@RequestMapping({ "/api/live" })
public class LiveController
{
    private static final Logger logger;
    private SimpleDateFormat dateFormat;
    @Resource
    private LiveService liveService;
    @Resource
    private QueryBetRecordService queryBetRecordService;
    @Resource
    private LiveBlacklistService liveBlacklistService;
    @Resource
    private LiveUserDayReportService liveUserDayReportService;
    @Resource
    private LiveGameKindService liveGameKindService;
    @Resource
    private ConfigService configService;
    @Resource
    private UserService userService;
    @Resource
    private SbConfig sbConfig;

    public LiveController() {
        this.dateFormat = DateUtil.simpleDateFormat("yyyy-MM-dd");
    }

    @RequestMapping(value = { "/getBalance" }, method = { RequestMethod.GET })
    @ResponseBody
    public Double getBalance(final String liveCode, @HY final UserInfo userInfo) throws Exception {
        if (SysUserTypes.TEST.getCode().equals(userInfo.getType())) {
            throw new BusinessException("LIVE/TEST", "\u8bd5\u73a9\u7528\u6237\u4e0d\u80fd\u4f59\u989d\u67e5\u8be2", (Object[])null);
        }
        if (SysUserTypes.VHY.getCode().equals(userInfo.getType())) {
            throw new BusinessException("LIVE/TEST", "\u60a8\u7684\u8d26\u53f7\u6ca1\u6709\u6743\u9650\uff0c\u8bf7\u8054\u7cfb\u60a8\u7684\u4e0a\u7ea7\u5f00\u901a", (Object[])null);
        }
        final Config liveIsOpen = this.configService.getByNameAndKey("live_config", "liveIsOpen");
        final Map<String, String> liveMap = new HashMap<String, String>();
        if (liveIsOpen != null && StringUtil.isNotBlank(liveIsOpen.getEnlargeMemo())) {
            liveMap.putAll((Map<? extends String, ? extends String>)JsonUtil.toObject(liveIsOpen.getEnlargeMemo(), (Class)HashMap.class));
        }
        if ("1".equals(liveMap.get(liveCode) + "")) {
            throw new BusinessException("LOGIN/1", "\u771f\u4eba\u6e38\u620f\u672a\u4e0a\u7ebf,\u656c\u8bf7\u671f\u5f85...", (Object[])null);
        }
        return this.liveService.getBalance(liveCode, userInfo);
    }

    @RequestMapping(value = { "/transfer" }, method = { RequestMethod.GET })
    @ResponseBody
    public void transfer(final String from, final String to, final Double amount, @HY final UserInfo userInfo) throws Exception {
        if (amount <= 0.0) {
            throw new BusinessException("\u9519\u8bef\u7684\u91d1\u989d");
        }
        if (SysUserTypes.TEST.getCode().equals(userInfo.getType())) {
            throw new BusinessException("LIVE/TEST", "\u8bd5\u73a9\u7528\u6237\u4e0d\u80fd\u989d\u5ea6\u8f6c\u6362", (Object[])null);
        }
        if (SysUserTypes.VHY.getCode().equals(userInfo.getType())) {
            throw new BusinessException("LIVE/TEST", "\u60a8\u7684\u8d26\u53f7\u6ca1\u6709\u6743\u9650\uff0c\u8bf7\u8054\u7cfb\u60a8\u7684\u4e0a\u7ea7\u5f00\u901a", (Object[])null);
        }
        final Config liveIsOpen = this.configService.getByNameAndKey("live_config", "liveIsOpen");
        final Map<String, String> liveMap = new HashMap<String, String>();
        if (liveIsOpen != null && StringUtil.isNotBlank(liveIsOpen.getEnlargeMemo())) {
            liveMap.putAll((Map<? extends String, ? extends String>)JsonUtil.toObject(liveIsOpen.getEnlargeMemo(), (Class)HashMap.class));
        }
        if ("1".equals(liveMap.get(TransferTypes.SELF.getCode().equals(from) ? to : from) + "")) {
            throw new BusinessException("LOGIN/1", "\u771f\u4eba\u6e38\u620f\u672a\u4e0a\u7ebf,\u656c\u8bf7\u671f\u5f85...", (Object[])null);
        }
        final PageData list = this.liveBlacklistService.slaveLiveBlacklist(new LiveBlackReq(userInfo.getAccount(), TransferTypes.SELF.getCode().equals(from) ? to : from, "1", (String)null, (String)null), new PageBean());
        if (list.getTotalCount() > 0) {
            throw new BusinessException("LIVE/BLACK_USER", "\u60a8\u7684\u8d26\u6237\u4e0d\u80fd\u989d\u5ea6\u8f6c\u6362\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb", (Object[])null);
        }
        this.liveService.transfer(from, to, amount, userInfo);
    }

    @RequestMapping(value = { "/play" }, method = { RequestMethod.GET })
    public void play(final String liveCode, final String gameType, @HY final UserInfo userInfo, final HttpServletRequest request, final HttpServletResponse response, @RequestParam(defaultValue = "true") final Boolean isMobile) throws Exception {
        if (SysUserTypes.TEST.getCode().equals(userInfo.getType())) {
            throw new BusinessException("LOGIN/1", "\u8bd5\u73a9\u7528\u6237\u4e0d\u80fd\u8fdb\u5165\u771f\u4eba\u6e38\u620f", (Object[])null);
        }
        if (SysUserTypes.VHY.getCode().equals(userInfo.getType())) {
            throw new BusinessException("LOGIN/1", "\u60a8\u7684\u8d26\u53f7\u6ca1\u6709\u6743\u9650\uff0c\u8bf7\u8054\u7cfb\u60a8\u7684\u4e0a\u7ea7\u5f00\u901a", (Object[])null);
        }
        final Config liveIsOpen = this.configService.getByNameAndKey("live_config", "liveIsOpen");
        final Map<String, String> liveMap = new HashMap<String, String>();
        if (liveIsOpen != null && StringUtil.isNotBlank(liveIsOpen.getEnlargeMemo())) {
            liveMap.putAll((Map<? extends String, ? extends String>)JsonUtil.toObject(liveIsOpen.getEnlargeMemo(), (Class)HashMap.class));
        }
        if ("1".equals(liveMap.get(liveCode) + "")) {
            throw new BusinessException("LOGIN/1", "\u771f\u4eba\u6e38\u620f\u672a\u4e0a\u7ebf,\u656c\u8bf7\u671f\u5f85...", (Object[])null);
        }
        final PageData list = this.liveBlacklistService.slaveLiveBlacklist(new LiveBlackReq(userInfo.getAccount(), liveCode, "2", (String)null, (String)null), new PageBean());
        if (list.getTotalCount() > 0) {
            throw new BusinessException("LOGIN/2", "\u60a8\u7684\u8d26\u6237\u4e0d\u80fd\u8fdb\u5165\u771f\u4eba\u6e38\u620f\uff0c\u8bf7\u4e0e\u5ba2\u670d\u4eba\u5458\u8054\u7cfb", (Object[])null);
        }
        String gameUrl = this.liveService.play(liveCode, gameType, userInfo.getAccount(), HttpUtil.getforwardedForIP(request), isMobile, request.getServerName());
        LiveController.logger.info("\u767b\u5f55\u6e38\u620furl:" + gameUrl);
        if ("sb".equalsIgnoreCase(liveCode)) {
            if (!this.sbConfig.getGameUrl().equals(this.sbConfig.getMobileGameUrl())) {
                gameUrl = this.sbConfig.getGameUrl() + "/page/sb.html?token=" + this.sbConfig.getAccount(userInfo.getAccount()) + "&isMobile=" + isMobile;
            }
            else {
                gameUrl = this.sbConfig.getGameUrl() + "/api/live/play?account=" + this.sbConfig.getAccount(userInfo.getAccount()) + "&baseUrl=" + this.sbConfig.getGameUrl() + "&isMobile=" + isMobile;
            }
        }
        response.getWriter().write(gameUrl);
    }

    @RequestMapping(value = { "/free" }, method = { RequestMethod.GET })
    @ResponseBody
    public String free(final String liveCode) throws Exception {
        return this.liveService.free(liveCode);
    }

    @RequestMapping(value = { "/qst" }, method = { RequestMethod.GET })
    @ResponseBody
    public PageData<GameDataDict> getSlotResources(final GameDataDict po, final PageBean pageBean) throws Exception {
        return (PageData<GameDataDict>)this.liveService.slaveSlotResources(po, pageBean);
    }

    @RequestMapping(value = { "/br" }, method = { RequestMethod.GET })
    @ResponseBody
    public PageData<GameBetRecord> betRecord(@HY final String account, final QueryBetRecordParam params, final PageBean bean) throws Exception {
        params.setAccount(account);
        return (PageData<GameBetRecord>)this.queryBetRecordService.queryPageBetRecord(params, bean);
    }

    @RequestMapping(value = { "/hbr" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<LiveUserDayReport> historyBetRecord(@HY final String account, final QueryBetRecordParam params) throws Exception {
        final Date date = new Date();
        params.setAccount(account);
        params.setBetEndDate(DateUtil.getDateEnd(date));
        params.setBetStartDate(DateUtil.getDateStart(DateUtil.addDate(date, -6)));
        return (List<LiveUserDayReport>)this.liveUserDayReportService.queryDayReport(params);
    }

    @RequestMapping(value = { "/gameReportList" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<LiveGameReport> gameReportList(@HY final Long userId, final String gameCodes, final String startDate, final String endDate) throws BusinessException {
        if (StringUtil.isBlank(gameCodes) || StringUtil.isBlank(startDate) || StringUtil.isBlank(endDate)) {
            return Collections.emptyList();
        }
        final List<String> gameCodeList = Stream.of(gameCodes.split(",")).filter(StringUtil::isNotBlank).distinct().collect(Collectors.toList());
        try {
            final Date _startDate = this.dateFormat.parse(startDate);
            final Date _endDate = this.dateFormat.parse(endDate);
            final Date maxStartDate = new Date(System.currentTimeMillis() - 777600000L);
            if (_startDate.before(maxStartDate)) {
                throw new BusinessException("\u6700\u591a\u53ea\u80fd\u67e5\u8be28\u5929\u524d\u8bb0\u5f55");
            }
        }
        catch (ParseException e) {
            throw new BusinessException("\u65e5\u671f\u683c\u5f0f\u9519\u8bef");
        }
        return (List<LiveGameReport>)this.liveUserDayReportService.slaveQueryGameReport(userId, (List)gameCodeList, startDate, endDate);
    }

    @RequestMapping(value = { "/allLiveGames" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<LiveGameBean> allLiveGames() throws Exception {
        return (List<LiveGameBean>)this.configService.getLiveOpenConfig(true);
    }

    @RequestMapping(value = { "/allLiveGameKinds" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<LiveGameKind> allLiveGameKinds() throws Exception {
        return (List<LiveGameKind>)this.liveGameKindService.slaveAll();
    }

    @RequestMapping({ "/verifySessionToken" })
    public void verifySessionToken(final String secret_key, final String session_token, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Map respMap = new HashMap();
        respMap.put("status_code", 0);
        if (StringUtil.isBlank(secret_key) || !secret_key.equals(this.sbConfig.getSecretKey())) {
            respMap.put("status_code", 1);
        }
        response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <authenticate version=\"2.0\"> <vendor_member_id>" + session_token + "</vendor_member_id> <status_code>" + respMap.get("status_code") + "</status_code> <message>OK</message> </authenticate>");
    }

    @RequestMapping(value = { "/redirectPage" }, method = { RequestMethod.GET })
    public void redirectPage(final String account, final Boolean isMobile, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final String gameUrl = this.sbConfig.getGameUrl();
        final URL url = new URL(gameUrl);
        String host = url.getHost();
        final String[] hostAry = host.split("\\.");
        if (hostAry.length > 2) {
            host = hostAry[hostAry.length - 2] + "." + hostAry[hostAry.length - 1];
        }
        String urlRedict = "";
        if (!isMobile) {
            final Cookie cookie = new Cookie("g", account);
            cookie.setDomain("." + host);
            cookie.setMaxAge(100);
            cookie.setPath("/");
            response.addCookie(cookie);
            urlRedict = this.sbConfig.getMobileGameUrl();
        }
        if (isMobile) {
            urlRedict = this.sbConfig.getMobileGameUrl().replace("mkt", "ismart") + "&st=" + account;
        }
        response.sendRedirect(urlRedict);
    }

    static {
        logger = LoggerFactory.getLogger((Class)LiveController.class);
    }
}
