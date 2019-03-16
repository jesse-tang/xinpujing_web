package com.cz.gameplat.lottery.service;

import com.cz.framework.*;
import com.cz.framework.bean.PageBean;
import com.cz.framework.bean.PageData;
import com.cz.framework.exception.BusinessException;
import com.cz.gameplat.game.bean.*;
import com.cz.gameplat.game.dao.LotteryDao;
import com.cz.gameplat.game.entity.*;
import com.cz.gameplat.game.enums.*;
import com.cz.gameplat.game.mamager.IPlayWinManager;
import com.cz.gameplat.game.service.*;
import com.cz.gameplat.js.manager.JSManager;
import com.cz.gameplat.lottery.bean.GameOpenInfo;
import com.cz.gameplat.lottery.manager.LotteryManager;
import com.cz.gameplat.lottery.manager.OpenInfoManager;
import com.cz.gameplat.sys.entity.Admin;
import com.cz.gameplat.sys.entity.Config;
import com.cz.gameplat.sys.service.ConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LotteryService
{
    private static final String ALL_OPEN_INFO_CACHE_KEY = "all_game_open_info";
    private static final Logger logger;
    private static final Executor lmclExecutor;
    @Resource
    ApplicationContext applicationContext;
    @Resource
    private GameService gameService;
    @Resource
    private LotteryDao lotteryDao;
    @Resource
    private OpenInfoManager openInfoManager;
    @Resource
    private PlayClService playClService;
    @Resource
    private PlayLmclService playLmclService;
    @Resource
    private PlayCateService playCateService;
    @Resource
    private ConfigService configService;
    @Resource
    private LotteryManager lotteryManager;
    @Resource
    private JSManager jsManager;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private LotteryService lotteryService;
    @Resource
    private GameTimeService gameTimeService;
    
    public void clear(final Date endTime, final Integer gameId) {
        this.lotteryDao.clear(endTime, gameId);
    }
    
    public PageData<Lottery> query(final Lottery lottery, final PageBean pageBean) throws BusinessException {
        return this.lotteryDao.query(lottery, pageBean);
    }
    
    public Integer getMaxLottery(final Integer gameId) {
        return this.lotteryDao.getMaxLottery(gameId);
    }
    
    public List<Lottery> queryAll(final Lottery lottery, final int num) throws BusinessException {
        return this.lotteryDao.queryAll(lottery, num);
    }
    
    public List<Lottery> queryAll(final Lottery lottery) throws BusinessException {
        return this.lotteryDao.queryAll(lottery);
    }
    
    private void checkIsLegalChangeLottery(final Game game, final Admin admin) throws BusinessException {
        if (game.getJsType() == GameJsTypes.JS.getValue()) {
            if (game.getCollectType() != GameCollectTypes.MANUAL.getValue()) {
                throw new BusinessException("GAME/JS_TYPE_ERROR", "\u6781\u901f\u5f69\u5fc5\u987b\u4e3a\u624b\u52a8\u5f00\u5956\u6a21\u5f0f\u624d\u53ef\u4ee5\u6dfb\u52a0\u5f00\u5956\u7ed3\u679c", (Object[])null);
            }
        }
        else if (game.getId() != 70 && !"OPEN".equals(admin.getType())) {
            throw new BusinessException("\u6dfb\u52a0\u6216\u4fee\u6539\u5f00\u5956\u7ed3\u679c\u8bf7\u8054\u7cfb\u5ba2\u670d");
        }
    }
    
    public Long save(final Lottery po, final Admin admin) throws BusinessException {
        final Game game = this.gameService.get(po.getGameId());
        if (game == null) {
            throw new BusinessException("GAME/GAME_NOT_EXIT", "game.game_not_exit", (Object[])null);
        }
        this.checkIsLegalChangeLottery(game, admin);
        String turnNum = po.getTurn();
        if (!game.getTurnLength().equals(turnNum.length())) {
            throw new BusinessException("\u60a8\u8f93\u5165\u7684\u671f\u53f7\u683c\u5f0f\u4e0d\u6b63\u786e\uff0c\u8bf7\u8f93\u5165" + game.getTurnLength() + "\u4f4d\u7684\u671f\u53f7");
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(game.getTurnFormat())) {
            turnNum = DateUtil.dateToStr(po.getOpenTime(), game.getTurnFormat()) + po.getTurn();
        }
        po.setTurnNum(turnNum);
        final Long id = Long.parseLong(po.getGameId() + po.getTurnNum());
        po.setId(id);
        final Long result = this.lotteryDao.save(po);
        LotteryService.lmclExecutor.execute(() -> this.onLotteryChange(po, game));
        return result;
    }
    
    public void add(final Lottery po) throws BusinessException {
        final Game game = this.gameService.get(po.getGameId());
        if (game == null) {
            throw new BusinessException("GAME/GAME_NOT_EXIT", "game.game_not_exit", (Object[])null);
        }
        if (po.getStatus() == null) {
            po.setStatus(LotteryStatus.NOT_LOTTERY.getStatus());
        }
        final Long id = Long.parseLong(po.getGameId() + po.getTurnNum());
        po.setId(id);
        final Lottery lottery = this.lotteryDao.get(id);
        if (lottery == null) {
            this.lotteryDao.save(po);
            LotteryService.lmclExecutor.execute(() -> this.onLotteryChange(po, game));
        }
    }
    
    public int update(final Lottery po, final Admin admin) throws BusinessException {
        final Game game = this.gameService.get(po.getGameId());
        if (game == null) {
            throw new BusinessException("GAME/GAME_NOT_EXIT", "game.game_not_exit", (Object[])null);
        }
        this.checkIsLegalChangeLottery(game, admin);
        if (!game.getTurnLength().equals(po.getTurn().length())) {
            throw new BusinessException("您输入的期号格式不正确，请输入" + game.getTurnLength() + "\u4f4d\u7684\u671f\u53f7");
        }
        final int result = this.lotteryDao.update(po);
        final Lottery lottery = this.lotteryDao.get(po.getId());
        LotteryService.lmclExecutor.execute(() -> this.onLotteryChange(lottery, game));
        return result;
    }
    
    public void updateObject(final Lottery t) throws BusinessException {
        try {
            this.lotteryDao.updateObject(t);
        }
        catch (SQLException e) {
            throw new BusinessException();
        }
    }
    
    public int updateStatus(final Lottery t, final Integer orgStatus) throws BusinessException {
        return this.lotteryDao.updateStatus(t, orgStatus);
    }
    
    public void makeLuzhiData(final Game game) {
        try {
            final String beanName = "playWinManager" + game.getType();
            final IPlayWinManager playWinManager = (IPlayWinManager)this.applicationContext.getBean(beanName, (Class)IPlayWinManager.class);
            if (playWinManager != null) {
                this.playClService.countCl(playWinManager, game);
                this.playLmclService.recountLMCL(playWinManager, game.getId());
            }
        }
        catch (Exception ex) {
            LotteryService.logger.error("两面长龙异常：", (Throwable)ex);
        }
    }
    
    private void onLotteryChange(final Lottery lottery, final Game game) {
        try {
            final String beanName = "playWinManager" + game.getType();
            final IPlayWinManager playWinManager = (IPlayWinManager)this.applicationContext.getBean(beanName, (Class)IPlayWinManager.class);
            if (playWinManager != null) {
                this.playLmclService.countLmcl(playWinManager, lottery);
                this.playClService.countCl(playWinManager, game);
                this.caculateLotteryPlayResult(playWinManager, lottery);
            }
        }
        catch (Exception ex) {
            LotteryService.logger.error("两面长龙异常：", (Throwable)ex);
        }
    }
    
    private void caculateLotteryPlayResult(final IPlayWinManager playWinManager, final Lottery lottery) throws BusinessException {
        final long start = System.currentTimeMillis();
        if (LotteryService.logger.isInfoEnabled()) {
            LotteryService.logger.info("start 开奖结果统计值: game=" + lottery.getGameId() + ",LotteryBean=" + lottery);
        }
        final List<PlayCate> pcList = this.playCateService.queryByRecordInLottery(lottery.getGameId());
        final Map<String, String> lotteryPlayResult = new HashMap<String, String>();
        for (final PlayCate pc : pcList) {
            final PlayWinBean b = playWinManager.handle(pc, lottery.getOpenNum());
            if (b == null) {
                LotteryService.logger.error("玩法未定义配置:" + pc);
                throw new BusinessException();
            }
            lotteryPlayResult.put(pc.getCode(), b.getValue());
        }
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            final String jsonStr = objectMapper.writeValueAsString(lotteryPlayResult);
            final Lottery lo = new Lottery();
            lo.setId(lottery.getId());
            lo.setResult(jsonStr);
            this.updateObject(lo);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (LotteryService.logger.isInfoEnabled()) {
            LotteryService.logger.info("end 开奖结果统计值: game=" + lottery.getGameId() + ",LotteryBean=" + lottery + ",time=" + (System.currentTimeMillis() - start));
        }
    }
    
    public Lottery get(final Long id) {
        return this.lotteryDao.get(id);
    }
    
    public Map<String, Object> getGameOpenInfo(final Integer gameId) throws BusinessException {
        final GameOpenInfo cur = this.openInfoManager.getCurOpenInfoWithCache(gameId);
        if (cur.getEndTime() != null) {
            cur.setCloseTime(cur.getEndTime().getTime());
        }
        Lottery pre = this.lotteryService.slaveGetPreOpenInfo(gameId);
        if (pre == null) {
            pre = new Lottery();
        }
        pre.setStatus(OpenStatus.DEFAULT.getStatus());
        if (OpenStatus.DEFAULT.getStatus() != cur.getStatus()) {
            cur.setTurnNum(null);
            cur.setTurn(null);
            cur.setCloseTime(0L);
        }
        final Map<String, Object> result = new HashMap<String, Object>();
        result.put("cur", new SimpleOpenInfo(cur));
        result.put("pre", new SimpleLottery(pre));
        return result;
    }
    
    public Map<String, Object> getAllGameOpenInfo() throws BusinessException {
        final Map<String, Object> cacheData = (Map<String, Object>)this.redisTemplate.opsForValue().get("all_game_open_info");
        if (cacheData != null) {
            return cacheData;
        }
        final List<Game> games = this.gameService.queryAll();
        final Map<String, Object> result = new HashMap<String, Object>();
        for (final Game game : games) {
            result.put(String.valueOf(game.getId()), this.getGameOpenInfo(game.getId()));
        }
        this.redisTemplate.opsForValue().set("all_game_open_info", result, 10L, TimeUnit.SECONDS);
        return result;
    }
    
    public void makeAllGameCurOpenInfoCache() {
        final List<Game> games = this.gameService.queryAll();
//        GameOpenInfo cur;
//        String cacheKey;
        games.stream().parallel().forEach(game -> {
            try {
                GameOpenInfo cur = this.openInfoManager.getCurOpenInfo(game.getId());
                String cacheKey = String.format("open_info_cur_id_%d", game.getId());
                this.redisTemplate.opsForValue().set(cacheKey, cur, 20L, TimeUnit.SECONDS);
            }
            catch (BusinessException e) {
                e.printStackTrace();
            }
        });
    }
    
    public GameOpenInfo getCurLottery(final Integer gameId) throws BusinessException {
        final GameOpenInfo cur = this.openInfoManager.getCurOpenInfoWithCache(gameId);
        if (cur.getEndTime() != null) {
            cur.setCloseTime(cur.getEndTime().getTime());
        }
        cur.setServerTime(new java.util.Date().getTime());
        return cur;
    }
    
    @Cacheable(value = { "open_info_pre" }, key = "'id_'+#gameId")
    public Lottery slaveGetPreOpenInfo(final Integer gameId) throws BusinessException {
        final Lottery lo = this.lotteryDao.getPerLottery(gameId);
        return lo;
    }
    
    public List<Lottery> getTodayOpenedData(final Integer gameId) {
        return this.lotteryDao.getTodayOpenedData(gameId);
    }
    
    public PageData<UnsettledLottery> queryUnsettled(final Integer gameId, final String turnNum, final Date dateFrom, final Date dateTo, final PageBean pageBean) throws Exception {
        return this.lotteryDao.queryUnsettled(gameId, turnNum, dateFrom, dateTo, pageBean);
    }
    
    public void regather(final Integer gameId, final String turnNums) throws Exception {
        final String apiUrl = this.getApiUrl();
        if (org.apache.commons.lang3.StringUtils.isEmpty(apiUrl)) {
            throw new BusinessException("\u91c7\u96c6\u63a5\u53e3\u672a\u914d\u7f6e");
        }
        this.handlerRegatherResults(this.lotteryManager.collect(apiUrl, gameId, turnNums.split(",")));
    }
    
    public void jsRegather(final Integer gameId, final int nums) throws Exception {
        final Game game = this.gameService.get(gameId);
        if (game.getJsType() != GameJsTypes.JS.getValue()) {
            throw new BusinessException();
        }
        final Date curDate = new java.util.Date();
        final int minte = DateUtil.betweenMinute(DateUtil.getDateStart(curDate), curDate);
        final int cs = minte % game.getInterval();
        Date curOpenTime = DateUtil.getMinute(curDate, -cs);
        final Calendar cal = Calendar.getInstance();
        cal.setTime(curOpenTime);
        cal.set(13, 0);
        curOpenTime = cal.getTime();
        for (int i = nums; i > 0; --i) {
            final Date openTime = DateUtil.getMinute(curOpenTime, -i * game.getInterval());
            final CollectLottery cl = this.jsManager.process(game, openTime);
            Lottery lo = this.lotteryDao.get(cl.getId());
            if (lo == null) {
                lo = new Lottery();
                lo.setId(cl.getId());
                lo.setOpenNum(cl.getOpenNum());
                lo.setOpenTime(cl.getOpenTime());
                lo.setGameId(cl.getGameId());
                lo.setAddTime(new java.util.Date());
                lo.setTurn(cl.getTurn());
                lo.setTurnNum(cl.getTurnNum());
                lo.setCollectStatus(CollectStatus.COLLECT.getStatus());
                this.add(lo);
            }
        }
    }
    
    public List<GatherLottery> slaveQueryByCode(final String code, final int limit) {
        final Game game = this.gameService.getCode(code);
        final List<GatherLottery> list = this.lotteryDao.queryByCode(game.getId(), limit);
        if (list != null && !list.isEmpty()) {
            for (final GatherLottery lo : list) {
                lo.setCode(game.getCode());
            }
        }
        return list;
    }
    
    public Lottery getNextLottery(final Integer gameId) throws Exception {
        final Game game = this.gameService.get(gameId);
        if (GameJsTypes.JS.getValue() != game.getJsType()) {
            throw new BusinessException("GAME/JS_ERROR", "不是极速彩不能进行此操作", (Object[])null);
        }
        final Lottery lo = this.slaveGetPreOpenInfo(gameId);
        final Date openTime = DateUtil.getMinute(lo.getOpenTime(), (int)game.getInterval());
        final CollectLottery info = this.jsManager.process(game, openTime);
        final Lottery result = new Lottery();
        result.setOpenNum(info.getOpenNum());
        result.setOpenTime(info.getOpenTime());
        result.setGameId(game.getId());
        result.setTurn(info.getTurn());
        result.setTurnNum(info.getTurnNum());
        return result;
    }
    
    private void handlerRegatherResults(final List<CollectLottery> results) throws Exception {
        if (CollectionUtils.isEmpty((Collection)results)) {
            return;
        }
        for (final CollectLottery result : results) {
            this.handlerRegatherResult(result);
        }
    }
    
    private void handlerRegatherResult(final CollectLottery result) throws Exception {
        Lottery lottery = this.lotteryDao.get(result.getGameId(), result.getTurnNum());
        if (lottery != null) {
            LogUtil.info("已有开奖结果，忽略补采: gameId=" + lottery.getGameId() + ", turnNum=" + lottery.getTurnNum());
            return;
        }
        lottery = new Lottery();
        BeanUtils.copyProperties(lottery, result);
        lottery.setStatus(LotteryStatus.NOT_LOTTERY.getStatus());
        lottery.setCollectStatus(CollectStatus.COLLECT.getStatus());
        lottery.setRemark("\u8865\u91c7");
        lottery.setAddTime(new java.util.Date());
        lottery.setUpdateTime(null);
        this.lotteryDao.save(lottery);
        final Lottery po = lottery;
        final Game game = this.gameService.get(result.getGameId());
        LotteryService.lmclExecutor.execute(() -> this.onLotteryChange(po, game));
    }
    
    private String getApiUrl() {
        final Config config = this.configService.getByNameAndKey("system_config", "collect_config");
        final String value = config.getConfigValue();
        final String[] vs = value.split(";");
        return (vs != null && vs.length != 0) ? vs[0] : null;
    }
    
    public List<Map<String, String>> makeNoticeWinMessage() {
        final Config config = this.configService.getByNameAndKey("system_config", "win_money_formula");
        boolean isNeedSort = true;
        String moneyFormula = "100 + (#T + #R1) * 500 + #R2 * 5000";
        if (config != null) {
            final String configValue = config.getConfigValue();
            if (StringUtil.isNotBlank(configValue)) {
                isNeedSort = configValue.startsWith("sort:");
                moneyFormula = configValue.replaceAll("sort\\:\\s*", "");
                moneyFormula = moneyFormula.replace("T", "#T").replace("R1", "#R1").replace("R2", "#R2");
            }
        }
        List<Game> gameList = this.gameService.queryAll();
        gameList = gameList.stream().filter(game -> game.getOpen() == 0 && game.getIsBan() == 0).collect(Collectors.toList());
        final List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        final ExpressionParser parser = (ExpressionParser)new SpelExpressionParser();
        final Expression expression = parser.parseExpression(moneyFormula);
        final char[] title = "qwrtypsdfghjklzxcbnm".toCharArray();
        final char[] userNameChars = "qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
        final int[] moneyRatio = { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 5, 7, 7, 8, 9, 10 };
        final Random random = new Random();
        final StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < 100; ++i) {
            context.setVariable("T", moneyRatio[random.nextInt(moneyRatio.length)]);
            context.setVariable("R1", Float.parseFloat(String.format("%.4f", random.nextFloat())));
            context.setVariable("R2", Float.parseFloat(String.format("%.4f", random.nextFloat())));
            final float winMoney = (float)expression.getValue((EvaluationContext)context, (Class)Float.class);
            final Map<String, String> winMessage = new HashMap<String, String>();
            winMessage.put("name", RandomStringUtils.random(1, title) + RandomStringUtils.random(2, userNameChars) + RandomStringUtils.random(random.nextInt(5) + 2, "****"));
            winMessage.put("winMoney", String.format("%.2f", winMoney));
            winMessage.put("gameName", gameList.get(random.nextInt(gameList.size())).getName());
            result.add(winMessage);
        }
        if (isNeedSort) {
            result.sort((param1, param2) -> new Double(param2.get("winMoney")).compareTo(Double.valueOf(Double.parseDouble(param1.get("winMoney")))));
        }
        return result;
    }
    
    public void checkCanOperateTime(final Lottery lo, final Game game) throws Exception {
        final int turnNumber = Integer.valueOf(lo.getTurn());
        if (game.getRules().equals(GameLotteryRules.DAY_ADDITIVE.getRule()) || game.getRules().equals(GameLotteryRules.MANUAL.getRule())) {
            GameTime gameTime = this.gameTimeService.getByGameIdAndTurn(lo.getGameId(), lo.getTurn());
            if (gameTime == null) {
                gameTime = this.gameTimeService.getOneByGameId(lo.getGameId());
            }
            final int gameTurnNumber = Integer.valueOf(gameTime.getTurnNum());
            if (!DateUtil.dateTimeBijiao(DateUtil.getNowTime(), gameTime.getEndTime()) && turnNumber >= gameTurnNumber) {
                throw new BusinessException("未封盘不能结算。");
            }
        }
        if (game.getRules().equals(GameLotteryRules.ADDITIVE.getRule())) {
            final int gameTurnNumber2 = Integer.valueOf(game.getCurTurnNum());
            final int turn = turnNumber - gameTurnNumber2;
            if (turn > 0) {
                final int dateCompareResult = DateUtil.strToDate(DateUtil.dateToStr(lo.getAddTime(), "yyyy-MM-dd")).compareTo(DateUtil.strToDate(DateUtil.getNowDate()));
                if (dateCompareResult == 1) {
                    throw new BusinessException("未封盘不能结算.");
                }
                if (dateCompareResult == 0) {
                    final GameTime gameTime2 = this.gameTimeService.getByGameIdAndTurn(lo.getGameId(), String.valueOf(turn));
                    if (!DateUtil.dateTimeBijiao(DateUtil.getNowTime(), DateUtil.getNowDate() + " " + gameTime2.getEndTime())) {
                        throw new BusinessException("未封盘不能结算.");
                    }
                }
            }
        }
        if (game.getRules().equals(GameLotteryRules.TIME_FORMAT.getRule()) || game.getRules().equals(GameLotteryRules.CROSS_DAY.getRule())) {
            final GameTime gameTime = this.gameTimeService.getByGameIdAndTurn(lo.getGameId(), lo.getTurn());
            final Date date = new java.util.Date();
            final SimpleDateFormat sdf = DateUtil.simpleDateFormat("yyyyMMdd");
            final String time = sdf.format(date);
            final String todayNum = time + lo.getTurn();
            final Long todayNumber = Long.valueOf(todayNum);
            final Long lotteryTurnNumber = Long.valueOf(lo.getTurnNum());
            final Long num = todayNumber - lotteryTurnNumber;
            if (num < 0L) {
                throw new BusinessException("未封盘不能结算!");
            }
            if (!DateUtil.dateTimeBijiao(DateUtil.getNowTime(), DateUtil.getNowDate() + " " + gameTime.getEndTime()) && num == 0L) {
                throw new BusinessException("未封盘不能结算!");
            }
        }
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)LotteryService.class);
        lmclExecutor = Executors.newSingleThreadExecutor();
    }
}
