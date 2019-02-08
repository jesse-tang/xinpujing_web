package com.cz.gameplate.web.game.ctrl;

import org.springframework.stereotype.*;
import com.cz.gameplat.lottery.service.*;
import javax.annotation.*;
import com.cz.gameplat.game.service.*;
import org.springframework.data.redis.core.*;
import com.cz.gameplat.lottery.manager.*;
import com.cz.gameplat.sys.service.*;
import com.cz.framework.exception.*;
import org.springframework.web.bind.annotation.*;
import com.cz.gameplat.game.enums.*;
import org.apache.commons.lang.*;
import com.cz.gameplat.lottery.bean.*;
import com.cz.gameplat.game.entity.*;
import java.util.concurrent.*;
import com.cz.gameplat.sys.entity.*;
import com.cz.gameplat.game.bean.*;
import com.cz.framework.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

@Controller
@RequestMapping({ "/v/lottery" })
public class LotteryController
{
    private static final String ALL_OPEN_INFO_OLD_CACHE_KEY = "all_open_info_old";
    @Resource
    LotteryService lotteryService;
    @Resource
    GameService gameService;
    @Resource
    PlayLmclService lmcService;
    @Resource
    PlayClService clService;
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Resource
    private OpenInfoManager openInfoManager;
    @Resource
    ConfigService configService;

    @RequestMapping(value = { "/getResultsList" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Lottery> getHistoryList(final Lottery lottery) throws BusinessException {
        int num = 70;
        if (lottery.getGameId() == 70) {
            num = 10;
        }
        else if (lottery.getGameId() == 10 || lottery.getGameId() == 11) {
            num = 20;
        }
        final List<Lottery> p = (List<Lottery>)this.lotteryService.queryAll(lottery, num);
        return p;
    }

    @RequestMapping(value = { "/getTopResults" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Lottery> getHistoryList(final Integer gameId, final Integer topNums) throws BusinessException {
        final Lottery lottery = new Lottery();
        lottery.setGameId(gameId);
        final List<Lottery> p = (List<Lottery>)this.lotteryService.queryAll(lottery, (int)topNums);
        return p;
    }

    @RequestMapping(value = { "/todayOpened" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Lottery> getTodayOpenedData(@RequestParam(value = "gameId", required = true) final Integer gameId) {
        return (List<Lottery>)this.lotteryService.getTodayOpenedData(gameId);
    }

    @RequestMapping(value = { "/todayCl" }, method = { RequestMethod.GET })
    @ResponseBody
    public Map<String, List<String>> todayChangeLong(@RequestParam(value = "gameId", required = true) final Integer gameId) {
        return (Map<String, List<String>>)this.clService.getGameCl(gameId);
    }

    @RequestMapping(value = { "/luzhi" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<LuZhiInfo> luZhiInfoList(final Integer gameId) throws BusinessException {
        return (List<LuZhiInfo>)this.clService.getGameLuzhi(gameId);
    }

    @RequestMapping(value = { "/getOpenInfo" }, method = { RequestMethod.GET })
    @ResponseBody
    @Deprecated
    public Map<String, Object> getGameOpenInfo(final Integer gameId) throws BusinessException {
        final GameOpenInfo cur = this.lotteryService.getCurLottery(gameId);
        Lottery pre = this.lotteryService.slaveGetPreOpenInfo(gameId);
        if (pre == null) {
            pre = new Lottery();
        }
        pre.setStatus(OpenStatus.DEFAULT.getStatus());
        if (StringUtils.isNotEmpty(cur.getTurn()) && StringUtils.isNotEmpty(pre.getTurn())) {
            final long len = Long.parseLong(cur.getTurn()) - Long.parseLong(pre.getTurn());
            if (len > 1L) {
                pre.setStatus(OpenStatus.OPEN.getStatus());
            }
        }
        if (OpenStatus.DEFAULT.getStatus() != cur.getStatus()) {
            cur.setTurnNum((String)null);
            cur.setTurn((String)null);
            cur.setStartTime((Date)null);
            cur.setEndTime((Date)null);
            cur.setCloseTime(0L);
            cur.setServerTime(0L);
        }
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put("cur", cur);
        result.put("pre", pre);
        return result;
    }

    @RequestMapping(value = { "/openInfo" }, method = { RequestMethod.GET })
    @ResponseBody
    public Map<String, Object> gameOpenInfo(final Integer gameId) throws BusinessException {
        final Map<String, Object> openInfo = (Map<String, Object>)this.lotteryService.getGameOpenInfo(gameId);
        openInfo.put("serverTime", System.currentTimeMillis());
        return openInfo;
    }

    @RequestMapping(value = { "/allOpenInfo" }, method = { RequestMethod.GET })
    @ResponseBody
    public Map<String, Object> allGameOpenInfo() throws BusinessException {
        final Map<String, Object> result = (Map<String, Object>)this.lotteryService.getAllGameOpenInfo();
        result.put("serverTime", System.currentTimeMillis());
        return result;
    }

    @RequestMapping(value = { "/getAllLottery" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<GameOpenInfo> getAllLottery(final Integer gameId) throws BusinessException {
        return (List<GameOpenInfo>)this.openInfoManager.getAllLottery(gameId);
    }

    @RequestMapping(value = { "/getAllOpenInfo" }, method = { RequestMethod.GET })
    @ResponseBody
    @Deprecated
    public List<Map<String, Object>> getAllGameOpenInfo() throws BusinessException {
        final List<Map<String, Object>> cacheData = (List<Map<String, Object>>)this.redisTemplate.opsForValue().get((Object)"all_open_info_old");
        if (cacheData != null) {
            return cacheData;
        }
        final Config config = this.configService.getByNameAndKey("system_config", "play_type_config");
        String value = "";
        if (config != null) {
            value = config.getConfigValue();
        }
        final List<Game> games = (List<Game>)this.gameService.queryAll();
        final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (final Game game : games) {
            final Map<String, Object> map = this.getGameOpenInfo(game.getId());
            map.put("game", game);
            map.put("config", value);
            result.add(map);
        }
        this.redisTemplate.opsForValue().set("all_open_info_old", (Object)result, 3L, TimeUnit.SECONDS);
        return result;
    }

    @RequestMapping(value = { "getLmcl" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<PlayLmclBean> getGameLmcl(final Integer gameId) {
        return (List<PlayLmclBean>)this.lmcService.getLmcl(gameId);
    }

    @RequestMapping(value = { "/all_game_lmcl" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Map<String, Object>> getAllGameLmcl() {
        return (List<Map<String, Object>>)this.lmcService.getAllGameLmcl();
    }

    @RequestMapping(value = { "/game_lmcl" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Map<String, Object>> getGameLmcl(final String gameIds) throws BusinessException {
        if (StringUtil.isBlank(gameIds)) {
            return Collections.emptyList();
        }
        if (!gameIds.matches("[0-9,]{0,100}")) {
            throw new BusinessException("\u6e38\u620fID\u683c\u5f0f\u9519\u8bef");
        }
        final List<Integer> gameList = Stream.of(gameIds.split(",")).filter(str -> str.length() > 0).map(Integer::parseInt).collect(Collectors.toList());
        return (List<Map<String, Object>>)this.lmcService.getGameLmcl((List)gameList);
    }

    @RequestMapping(value = { "/lmcl_rank" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<PlayLmclBean> getLmclRank() {
        return (List<PlayLmclBean>)this.lmcService.getLmclRank();
    }

    @RequestMapping(value = { "getSysTime" }, method = { RequestMethod.GET })
    @ResponseBody
    public Map<String, Long> getSysTime() {
        final Map<String, Long> map = new HashMap<String, Long>();
        map.put("serverTime", new Date().getTime());
        return map;
    }

    @RequestMapping(value = { "/getNoticeWinMessage" }, method = { RequestMethod.GET })
    @ResponseBody
    public List<Map<String, String>> getNoticeWinMessage() {
        final String cacheKey = "notice_win_message_te";
        List<Map<String, String>> result;
        if (!this.redisTemplate.hasKey(cacheKey)) {
            result = (List<Map<String, String>>)this.lotteryService.makeNoticeWinMessage();
            this.redisTemplate.opsForValue().set(cacheKey, (Object)result, 1L, TimeUnit.MINUTES);
        }
        else {
            result = (List<Map<String, String>>)this.redisTemplate.opsForValue().get((Object)cacheKey);
        }
        return result;
    }
}
