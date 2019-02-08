package com.cz.rest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;



@Component
@PropertySource({"classpath:config.properties"})
public class ChatConfig
{
  private String customCode;
  
  public String getCustomCode()
  {
    return this.customCode;
  }
  
  @Autowired
  public ChatConfig(Environment env) {
    String siteIdPrefix = env.getProperty("managerApi.siteIdPrefix");
    this.customCode = siteIdPrefix.split("_")[0];
  }
}
