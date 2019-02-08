package com.cz.gameplat.web.ctrl;

import com.cz.gameplat.sys.entity.Admin;
import com.cz.gameplat.sys.service.AdminService;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;









@Controller
@RequestMapping({"/api/admin"})
public class AdminController
{
  @Resource
  private AdminService adminService;
  
  @RequestMapping(value={"/checkUnique"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public boolean checkAccountUnique(String account)
    throws Exception
  {
    Admin admin = this.adminService.get(account);
    if (admin == null) {
      return true;
    }
    return false;
  }
}
