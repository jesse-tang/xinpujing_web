package com.cz.gameplat.web.ctrl;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.cz.framework.*;

@Controller
@RequestMapping({ "/api/counter" })
public class counterController
{
  private static int dayTotalAmount;
  private static int MonthTotalAmount;
  private static int totalAmount;
  private static Double gameAmount;



  @RequestMapping(value = { "/totalNumber" }, method = { RequestMethod.GET })
  @ResponseBody
  public Map totalNumber() throws Exception {
    final Map map = new HashMap();
    map.put("dayTotalAmount", counterController.dayTotalAmount);
    map.put("MonthTotalAmount", counterController.MonthTotalAmount);
    map.put("totalAmount", counterController.totalAmount);
    map.put("gameAmount", String.format("%.2f", counterController.gameAmount));
    return map;
  }

  static {
    counterController.dayTotalAmount = 1036857;
    counterController.MonthTotalAmount = 368596;
    counterController.totalAmount = 23556483;
    counterController.gameAmount = 1549856.12;
    final Timer timer = new Timer();
    final long delay1 = 1000L;
    final long period1 = 1000L;
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        final String day = DateUtil.getDateToString(new Date(), "HH");
        final String month = DateUtil.getDateToString(new Date(), "dd");
        if ("00".equals(day)) {
          counterController.dayTotalAmount = 1036857;
        }
        if ("01".equals(month)) {
          counterController.MonthTotalAmount = 368596;
        }
        if (counterController.gameAmount > 9999999.0) {
          counterController.gameAmount = 1549856.12;
        }
        counterController.dayTotalAmount += (int)(Math.random() * 231.0);
        counterController.MonthTotalAmount += (int)(Math.random() * 231.0);
        counterController.totalAmount += (int)(Math.random() * 231.0);
        counterController.gameAmount += Math.random() * 231.0;
      }
    }, delay1, period1);
  }
}
