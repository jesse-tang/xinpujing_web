package com.cz.rest.config;

import org.springframework.stereotype.*;
import org.apache.log4j.*;
import java.util.*;
import com.cz.framework.*;
import org.apache.commons.io.*;
import java.io.*;
import org.springframework.scheduling.annotation.*;

@Component
@EnableScheduling
public class WebTaskConfig
{
    private Logger logger;
    
    public WebTaskConfig() {
        this.logger = Logger.getLogger((Class)WebTaskConfig.class);
    }
    
    @Scheduled(cron = "0 0 5 * * ?")
    public void deleteChatImage() {
        this.logger.info((Object)"start deleteChatImage");
        final String today = DateUtil.dateToStr(new Date(), "yyyyMMdd");
        final File chatTmpDir = new File(AppContext.getInstance().getSysRealPath(), "/chat_image/tmp/");
        final File[] dayDirs = chatTmpDir.listFiles((dir, name) -> !today.equals(name));
        if (dayDirs != null) {
            for (final File file : dayDirs) {
                try {
                    if (file.isDirectory()) {
                        FileUtils.deleteDirectory(file);
                        this.logger.info((Object)String.format("delete dir: %s", file.getPath()));
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.logger.info((Object)"finish deleteChatImage");
    }
}
