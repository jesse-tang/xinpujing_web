package com.cz.gameplat.web.ctrl;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.cz.framework.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;




@Controller
public class ErrorController
{
  private Logger logger = Logger.getLogger(ErrorController.class);
  
  @RequestMapping(value={"/api/app/error/upload"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
  public void saveError(HttpServletRequest request, HttpServletResponse response) throws BusinessException, IOException {
    List<NameValuePair> nameValuePairs = new ArrayList();
    Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      nameValuePairs.add(new BasicNameValuePair(name, request.getParameter(name)));
    }
    
    HttpClient httpClient = null;
    HttpResponse yiboResponse = null;
    try {
      HttpPost httpPost = new HttpPost("http://appadmin.yibofafa666.com/api/app/error/upload");
      String postPrams = URLEncodedUtils.format(nameValuePairs, Charset.forName("utf-8"));
      httpPost.setEntity(new StringEntity(postPrams, ContentType.create("application/x-www-form-urlencoded", "utf-8")));
      httpClient = HttpClients.createDefault();
      yiboResponse = httpClient.execute(httpPost);
      if (yiboResponse.getStatusLine().getStatusCode() == 200) {
        StreamUtils.copy(yiboResponse.getEntity().getContent(), response.getOutputStream());
      } else {
        String error = StreamUtils.copyToString(yiboResponse.getEntity().getContent(), Charset.forName("utf-8"));
        try {
          JSONObject jsonObject = JSONObject.parseObject(error);
          throw new BusinessException(jsonObject.getString("msg"));
        } catch (JSONException e) {
          this.logger.error("yibofafa666 error:" + error);
          throw new BusinessException();
        }
      }
    } finally {
      HttpClientUtils.closeQuietly(yiboResponse);
      HttpClientUtils.closeQuietly(httpClient);
    }
  }
}
