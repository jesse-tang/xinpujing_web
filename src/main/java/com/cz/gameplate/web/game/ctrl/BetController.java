package com.cz.gameplate.web.game.ctrl;

import com.cz.framework.StringUtil;
import com.cz.framework.exception.BusinessException;
import com.cz.framework.redis.lock.Lock;
import com.cz.gameplat.game.bean.HeelBetBean;
import com.cz.gameplat.game.entity.Game;
import com.cz.gameplat.game.entity.PlayCate;
import com.cz.gameplat.game.entity.UserBet;
import com.cz.gameplat.game.odds.GameOddsManager;
import com.cz.gameplat.game.service.BetService;
import com.cz.gameplat.game.service.GameService;
import com.cz.gameplat.game.service.PlayCateService;
import com.cz.gameplat.game.service.UserBetService;
import com.cz.gameplat.lottery.bean.BetContent;
import com.cz.gameplat.lottery.bean.LotteryBetBean;
import com.cz.gameplat.lottery.bean.TraceBetBean;
import com.cz.gameplat.lottery.enums.GameModel;
import com.cz.gameplat.sys.entity.Config;
import com.cz.gameplat.sys.service.ConfigService;
import com.cz.gameplat.user.entity.UserInfo;
import com.cz.gameplat.web.interceptor.HY;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;












@Controller
@RequestMapping({"/api/bet"})
public class BetController
{
  @Resource
  private BetService betService;
  @Resource
  private UserBetService userBetService;
  @Resource
  private GameService gameService;
  @Resource
  private ConfigService configService;
  @Autowired
  private GameOddsManager gameOddsManager;
  @Resource
  private PlayCateService playCateService;
  
  @RequestMapping(method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void bet(@Valid @RequestBody LotteryBetBean betBean, @HY UserInfo user, HttpServletRequest request)
    throws Exception
  {
    betBean.setBetSrc(StringUtil.JudgeIsMoblie(request));
    this.betService.betG(betBean, user, GameModel.CREDIT.getValue());
  }
  






  @RequestMapping(value={"/heelBet"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void heelBet(@Valid @RequestBody HeelBetBean betBean, @HY UserInfo user, HttpServletRequest request)
    throws Exception
  {
    PlayCate play = this.playCateService.get(betBean.getPlayCode());
    if (play == null) {
      throw new BusinessException("玩法不存");
    }
    List<UserBet> list = this.betService.getheelBet(betBean);
    if ((list == null) || (list.isEmpty())) {
      throw new BusinessException("订单不存在");
    }
    Integer gameModel = null;
    LotteryBetBean lotteryBetBean = null;
    List<BetContent> contentList = new ArrayList();
    
    for (UserBet bet : list) {
      if (lotteryBetBean == null) {
        lotteryBetBean = new LotteryBetBean();
        lotteryBetBean.setGameId(bet.getGameId());
        lotteryBetBean.setTurnNum(bet.getTurnNum());
        lotteryBetBean.setBetSrc(StringUtil.JudgeIsMoblie(request));
        gameModel = bet.getModel();
      }
      BetContent bcon = new BetContent();
      bcon.setCode(bet.getCateCode());
      bcon.setCateName(bet.getCateName());
      bcon.setBetInfo(bet.getBetInfo());
      bcon.setBetModel(bet.getBetModel());
      bcon.setMoney(bet.getMoney());
      bcon.setMultiple(bet.getMultiple());
      bcon.setTotalMoney(bet.getTotalMoney());
      bcon.setTotalNums(bet.getTotalNums());
      String[] odds = this.gameOddsManager.odds(play, user.getRebate().doubleValue() / 100.0D, 0.0D, bet.getBetModel(), bet
        .getBetInfo());
      bcon.setOdds(odds[0]);
      bcon.setRebate(Double.valueOf(0.0D));
      contentList.add(bcon);
    }
    if ((lotteryBetBean == null) || (contentList.isEmpty()) || (gameModel == null)) {
      throw new BusinessException("请求参数不正确");
    }
    lotteryBetBean.setContent(contentList);
    this.betService.betG(lotteryBetBean, user, gameModel.intValue());
  }
  






  @RequestMapping(value={"/betG"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void betG(@Valid @RequestBody LotteryBetBean betBean, @HY UserInfo user, HttpServletRequest request)
    throws Exception
  {
    betBean.setBetSrc(StringUtil.JudgeIsMoblie(request));
    this.betService.betG(betBean, user, GameModel.OFFCIAL.getValue());
  }
  
  @RequestMapping(value={"/trace"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  public void trace(@Valid @RequestBody TraceBetBean bean, @HY UserInfo user, HttpServletRequest request) throws BusinessException
  {
    bean.setBetSrc(StringUtil.JudgeIsMoblie(request));
    this.betService.trace(bean, user);
  }
  





  @RequestMapping(value={"/queryTotal"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public List<Map<String, Object>> queryTotal(@HY Long userId)
  {
    List<Game> gameList = this.gameService.queryAll();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> result = new HashMap();
    for (Game g : gameList) {
      UserBet bet = this.userBetService.queryTotal(g.getId(), userId);
      result.put("gameName", g.getName());
      result.put("gameId", g.getId());
      if (bet != null)
      {
        result.put("totalMoney", bet.getTotalMoney());
        result.put("totalNums", bet.getTotalNums());
      }
      list.add(result);
    }
    return list;
  }
  





  @RequestMapping(value={"/cancel"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  @ResponseBody
  @Lock(value="revocation", key="#user.userId")
  public void revocation(String orderNo, @HY UserInfo user)
    throws BusinessException
  {
    if (StringUtil.isBlank(orderNo)) {
      throw new BusinessException("参数不正确");
    }
    Config byNameAndKey = this.configService.getByNameAndKey("system_config", "hy_is_cancel");
    if ((byNameAndKey.getConfigValue() != null) && (byNameAndKey.getConfigValue().equals("1"))) {
      String[] nos = orderNo.split(",");
      this.betService.revocation(nos, user);
    } else {
      throw new BusinessException("会员不允许撤单");
    }
  }
}
