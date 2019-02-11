package com.cz.gameplat.lottery.manager;

import com.cz.gameplat.game.dao.GameTimeDao;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.*;
import com.cz.gameplat.game.service.*;
import com.cz.gameplat.lottery.service.*;
import com.cz.gameplat.lottery.bean.*;
import com.cz.framework.exception.*;
import com.cz.framework.*;
import java.util.*;
import com.cz.gameplat.game.entity.*;
import com.cz.gameplat.game.enums.*;

@Component
public class OpenInfoManager
{
    @Autowired
    GameTimeDao gameTimeDao;
    @Autowired
    GameService gameService;
    @Autowired
    LotteryService lotteryService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate redisTemplate;

    @Cacheable(value = { "open_info_cur" }, key = "'id_' + #gameId")
    public GameOpenInfo getCurOpenInfoWithCache(final Integer gameId) throws BusinessException {
        return this.getCurOpenInfo(gameId);
    }

    public GameOpenInfo getCurOpenInfo(final Integer gameId) throws BusinessException {
        final Game game = this.gameService.get(gameId);
        if (game == null) {
            throw new BusinessException("GAME/GAME_NOT_EXIT", "game.game_not_exit", (Object[])null);
        }
        final Calendar calendar = Calendar.getInstance();
        final Date date = calendar.getTime();
        GameOpenInfo info = new GameOpenInfo();
        info.setStatus(OpenStatus.CLOSE.getStatus());
        info.setGameId(game.getId());
        if (game.getOpen() == 1) {
            info.setStatus(OpenStatus.STOP.getStatus());
            return info;
        }
        if (game.getIsBan() == 1) {
            return info;
        }
        if (game.getRestStartDate() != null && game.getRestEndDate() != null && DateUtil.dateCompareByYmdhms(date, game.getRestStartDate(), game.getRestEndDate())) {
            return info;
        }
        final List<GameTime> timeList = this.gameTimeDao.getByGameId(game.getId());
        if (timeList == null) {
            return info;
        }
        for (int index = 0; index < timeList.size(); ++index) {
            final GameTime t = timeList.get(index);
            info = this.getTurnInfo(t, game, index);
            if (DateUtil.dateCompareByYmd(date, info.getStartTime(), info.getEndTime())) {
                info.setStatus(OpenStatus.DEFAULT.getStatus());
                return info;
            }
            if (DateUtil.dateCompareByYmdhms(date, info.getStartTime())) {
                info.setStatus(OpenStatus.CLOSE.getStatus());
                return info;
            }
        }
        info.setStatus(OpenStatus.CLOSE.getStatus());
        return info;
    }

    public List<GameOpenInfo> getAllLottery(final Integer gameId) throws BusinessException {
        final List<GameOpenInfo> list = new ArrayList<GameOpenInfo>();
        final Game game = this.gameService.get(gameId);
        if (game == null || game.getOpen() == 1 || game.getIsBan() == 1) {
            return list;
        }
        final Calendar calendar = Calendar.getInstance();
        final Date date = calendar.getTime();
        if (game.getRestStartDate() != null && game.getRestEndDate() != null && DateUtil.dateCompareByYmd(date, game.getRestStartDate(), game.getRestEndDate())) {
            return list;
        }
        final List<GameTime> timeList = this.gameTimeDao.getByGameId(game.getId());
        if (timeList == null) {
            return list;
        }
        final boolean isAdditive = GameLotteryRules.ADDITIVE.getRule().equals(game.getRules());
        Integer preTurnNum = null;
        if (isAdditive) {
            final Lottery lottery = this.lotteryService.slaveGetPreOpenInfo(gameId);
            if (lottery == null) {
                return list;
            }
            preTurnNum = new Integer(lottery.getTurnNum());
        }
        for (int index = 0; index < timeList.size(); ++index) {
            final GameTime t = timeList.get(index);
            final GameOpenInfo info = this.getTurnInfo(t, game, index);
            if (!isAdditive || preTurnNum < new Integer(info.getTurnNum())) {
                if (DateUtil.dateCompareByYmdhms(date, info.getStartTime()) || DateUtil.dateCompareByYmd(date, info.getStartTime(), info.getEndTime())) {
                    info.setStatus(OpenStatus.DEFAULT.getStatus());
                    list.add(info);
                }
            }
        }
        return list;
    }

    public GameOpenInfo getByTurn(final Game game, final String turn) {
        final List<GameTime> timeList = this.gameTimeDao.getByGameId(game.getId());
        if (timeList == null) {
            return null;
        }
        for (int index = 0; index < timeList.size(); ++index) {
            final GameTime t = timeList.get(index);
            final GameOpenInfo info = this.getTurnInfo(t, game, index);
            if (info.getTurnNum().equals(turn)) {
                info.setStatus(OpenStatus.DEFAULT.getStatus());
                return info;
            }
        }
        return null;
    }

    public GameOpenInfo getCurTurn(final Game game, final String turn) {
        final List<GameTime> timeList = this.gameTimeDao.getByGameId(game.getId());
        if (timeList == null) {
            return null;
        }
        GameOpenInfo info = null;
        for (int index = 0; index < timeList.size(); ++index) {
            final GameTime t = timeList.get(index);
            info = this.getTurnInfo(t, game, index);
            String turnNum = info.getTurn();
            if (GameLotteryRules.ADDITIVE.getRule().equals(game.getRules())) {
                turnNum = info.getTurnNum();
            }
            if (turnNum.equals(turn)) {
                return info;
            }
        }
        return null;
    }

    private GameOpenInfo getTurnInfo(final GameTime t, final Game game, final int index) {
        Date betStartTime = null;
        Date betEndTime = null;
        String turnNum = "";
        final Calendar calendar = Calendar.getInstance();
        final Date date = calendar.getTime();
        String statDate = "";
        if (GameLotteryRules.MANUAL.getRule().equals(game.getRules()) || GameLotteryRules.DAY_ADDITIVE.getRule().equals(game.getRules())) {
            betStartTime = DateUtil.strToDate(t.getStartTime(), "yyyy-MM-dd HH:mm:ss");
            betEndTime = DateUtil.strToDate(t.getEndTime(), "yyyy-MM-dd HH:mm:ss");
            statDate = DateUtil.dateToStr(betEndTime, "yyyy-MM-dd");
            turnNum = DateUtil.dateToStr(betEndTime, game.getTurnFormat()) + t.getTurnNum();
        }
        else if (GameLotteryRules.TIME_FORMAT.getRule().equals(game.getRules())) {
            final String sDate = DateUtil.dateToYMD(date);
            betStartTime = DateUtil.strToDate(sDate + " " + t.getStartTime(), "yyyy-MM-dd HH:mm:ss");
            betEndTime = DateUtil.strToDate(sDate + " " + t.getEndTime(), "yyyy-MM-dd HH:mm:ss");
            if (index == 0 && DateUtil.dateCompareByYmdhms(betEndTime, betStartTime)) {
                if (DateUtil.dateCompareByYmdhms(date, betStartTime)) {
                    betStartTime = DateUtil.getDay(betStartTime, -1);
                }
                else {
                    betEndTime = DateUtil.getDay(betEndTime, 1);
                }
            }
            statDate = DateUtil.dateToStr(betEndTime, "yyyy-MM-dd");
            turnNum = DateUtil.dateToStr(betEndTime, game.getTurnFormat()) + t.getTurnNum();
        }
        else if (GameLotteryRules.ADDITIVE.getRule().equals(game.getRules())) {
            final String sDate = DateUtil.dateToYMD(date);
            betStartTime = DateUtil.strToDate(sDate + " " + t.getStartTime(), "yyyy-MM-dd HH:mm:ss");
            betEndTime = DateUtil.strToDate(sDate + " " + t.getEndTime(), "yyyy-MM-dd HH:mm:ss");
            turnNum = Integer.parseInt(game.getCurTurnNum()) + Integer.parseInt(t.getTurnNum()) + "";
            statDate = DateUtil.dateToStr(betEndTime, "yyyy-MM-dd");
        }
        else if (GameLotteryRules.CROSS_DAY.getRule().equals(game.getRules())) {
            final String sDate = DateUtil.dateToStr(DateUtil.strToDate(game.getCurTurnNum(), "yyyyMMdd"), "yyyy-MM-dd");
            betStartTime = DateUtil.strToDate(sDate + " " + t.getStartTime(), "yyyy-MM-dd HH:mm:ss");
            betEndTime = DateUtil.strToDate(sDate + " " + t.getEndTime(), "yyyy-MM-dd HH:mm:ss");
            if (t.getCrossDay() == CrossDayTypes.CROSS_ALL.getValue()) {
                betStartTime = DateUtil.getDay(betStartTime, 1);
                betEndTime = DateUtil.getDay(betEndTime, 1);
            }
            else if (t.getCrossDay() == CrossDayTypes.END_TIME_CROSS.getValue()) {
                betEndTime = DateUtil.getDay(betEndTime, 1);
            }
            statDate = DateUtil.dateToStr(betEndTime, "yyyy-MM-dd");
            turnNum = game.getCurTurnNum() + t.getTurnNum();
        }
        final GameOpenInfo info = new GameOpenInfo();
        info.setEndTime(betEndTime);
        info.setStartTime(betStartTime);
        info.setTurnNum(turnNum);
        info.setTurn(t.getTurnNum());
        info.setStatDate(statDate);
        info.setGameId(game.getId());
        return info;
    }
}
