package com.cz.gameplat.web.ctrl;

import org.springframework.stereotype.*;
import org.apache.log4j.*;
import com.cz.gameplat.sys.service.*;
import javax.annotation.*;
import com.cz.rest.config.*;
import com.cz.gameplat.user.service.*;
import org.springframework.web.multipart.*;
import com.cz.framework.exception.*;
import org.springframework.util.*;
import com.cz.framework.*;
import net.coobird.thumbnailator.*;
import org.apache.http.*;
import org.apache.http.message.*;
import com.cz.gameplat.live.core.utils.*;
import java.io.*;
import com.cz.gameplat.sys.entity.*;
import java.awt.image.*;
import com.alibaba.fastjson.*;
import com.cz.gameplat.web.interceptor.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.*;
import com.cz.framework.web.*;
import com.cz.gameplat.sys.util.*;
import com.cz.gameplat.user.entity.*;
import java.util.*;

@Controller
@RequestMapping({ "/api/chat" })
public class ApiChatController
{
  private Logger logger;
  private static final String[] image_type;
  @Resource
  private ConfigService configService;
  @Resource
  private ChatConfig chatConfig;
  @Resource
  private UserService userService;

  public ApiChatController() {
    this.logger = Logger.getLogger((Class)ApiChatController.class);
  }

  @RequestMapping(value = { "/uploadImage" }, method = { RequestMethod.POST })
  @ResponseBody
  public void uploadImage(final HttpServletRequest request, @RequestParam("file") final MultipartFile uploadFile, @RequestParam(required = false) final String messageId, final int imgWidth, final int imgHeight, final int roomId) throws Exception {
    final Config config = this.configService.getByNameAndKey("system_config", "chat_api_url");
    if (config == null) {
      throw new BusinessException("\u672a\u914d\u7f6e\u804a\u5929\u670d\u52a1");
    }
    final String chatServiceUrl = String.format("%s/%s", config.getConfigValue(), "api/user/sendImage");
    try {
      byte[] imageContent = StreamUtils.copyToByteArray(uploadFile.getInputStream());
      final String fileSuffix = FileUtil.getExtension(uploadFile.getOriginalFilename());
      if (!ArrayUtils.isExist(ApiChatController.image_type, fileSuffix)) {
        throw new BusinessException("\u4e0d\u652f\u6301\u7684\u56fe\u7247\u7c7b\u578b " + fileSuffix);
      }
      int newWidth = 0;
      int newHeight = 0;
      if (!"gif".equals(fileSuffix)) {
        final BufferedImage bufferedImage = Thumbnails.of(new InputStream[] { new ByteArrayInputStream(imageContent) }).scale(1.0).asBufferedImage();
        final int width = bufferedImage.getWidth();
        final int height = bufferedImage.getHeight();
        if (width == 0 || height == 0) {
          throw new BusinessException("\u56fe\u7247\u683c\u5f0f\u9519\u8bef");
        }
        newWidth = Math.min(1000, width);
        newHeight = height * newWidth / width;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Thumbnails.of(new InputStream[] { new ByteArrayInputStream(imageContent) }).width(newWidth).height(newHeight).outputFormat(fileSuffix).toOutputStream((OutputStream)bos);
        imageContent = bos.toByteArray();
      }
      else {
        newWidth = imgWidth;
        newHeight = imgHeight;
      }
      if (imageContent.length > 1048576) {
        throw new BusinessException("\u6587\u4ef6\u4e0d\u80fd\u8d85\u8fc7 1M");
      }
      final String imageBase64 = Base64.getEncoder().encodeToString(imageContent);
      final Header[] headers = { new BasicHeader("Cookie", request.getHeader("Cookie")), new BasicHeader("plat_code", this.chatConfig.getCustomCode()) };
      final Map<String, Object> params = new HashMap<String, Object>();
      params.put("imageBase64", "data:image/" + fileSuffix + ";base64, " + imageBase64);
      params.put("width", newWidth);
      params.put("height", newHeight);
      params.put("messageId", messageId);
      params.put("roomId", roomId);
      try {
        HttpClientUtils.doPost(chatServiceUrl, JSON.toJSONString((Object)params), headers);
      }
      catch (BusinessException e) {
        final JSONObject json = JSON.parseObject(e.getMessage());
        throw new BusinessException(json.getString("code"), json.getString("msg"), (Object[])null);
      }
    }
    catch (IOException e2) {
      e2.printStackTrace();
      throw new BusinessException("\u6587\u4ef6\u4e0a\u4f20\u5931\u8d25");
    }
  }

  @RequestMapping(value = { "/post/{apiPath}" }, method = { RequestMethod.POST })
  @ResponseBody
  public String post(@HY final UserInfo user, @PathVariable("apiPath") String apiPath, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final long startMethod = System.currentTimeMillis();
    final Config config = this.configService.getByNameAndKey("system_config", "chat_api_url");
    if (config == null) {
      throw new BusinessException("\u672a\u914d\u7f6e\u804a\u5929\u670d\u52a1");
    }
    apiPath = apiPath.replace("_", "/");
    final String chatServiceUrl = String.format("%s/%s", config.getConfigValue(), apiPath);
    final HashMap<String, String> params = new HashMap<String, String>();
    if ("api/u/init".equals(apiPath)) {
      params.put("userId", String.valueOf(user.getUserId()));
      params.put("account", user.getAccount());
      params.put("orgUrl", HttpUtil.getBaseUrl(request));
      params.put("token", TokenManager.getRequestToken(request));
      params.put("nickName", user.getNickname());
      params.put("platCode", this.chatConfig.getCustomCode());
      params.put("userType", user.getType());
      params.put("level", String.valueOf(this.configService.getChatLevel(user.getHyLevel())));
      final UserExtInfo extInfo = this.userService.getUserExtInfoByCahe(user.getUserId());
      params.put("rechCount", extInfo.getRechCount() + "");
      params.put("rechMoney", extInfo.getRechMoney() + "");
    }
    else {
      final Enumeration<String> names = (Enumeration<String>)request.getParameterNames();
      while (names.hasMoreElements()) {
        final String name = names.nextElement();
        params.put(name, request.getParameter(name));
      }
    }
    final Header[] headers = { new BasicHeader("Cookie", request.getHeader("Cookie")), new BasicHeader("plat_code", this.chatConfig.getCustomCode()) };
    final long startRequest = System.currentTimeMillis();
    try {
      return HttpClientUtils.doPost(chatServiceUrl, JSON.toJSONString((Object)params), headers);
    }
    catch (BusinessException e) {
      final JSONObject json = JSON.parseObject(e.getMessage());
      throw new BusinessException(json.getString("code"), json.getString("msg"), (Object[])null);
    }
    finally {
      final long finishMethod = System.currentTimeMillis();
      this.logger.info((Object)String.format("[PROXY][%s] post cost:%-5d,request cost:%-5d,total:%-5d", apiPath, startRequest - startMethod, finishMethod - startRequest, finishMethod - startMethod));
    }
  }

  static {
    image_type = new String[] { "bmp", "jpg", "jpeg", "png", "gif" };
  }
}
