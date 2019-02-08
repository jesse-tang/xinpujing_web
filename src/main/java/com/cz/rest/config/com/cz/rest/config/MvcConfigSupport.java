package com.cz.rest.config;

import com.cz.framework.redis.lock.RedisLock;
import com.cz.framework.redis.lock.RedisLockAspect;
import com.cz.framework.web.RequestLogInterceptor;
import com.cz.framework.web.WebExceptionResolver;
import com.cz.gameplat.web.handler.BetIntercepter;
import com.cz.gameplat.web.handler.LoginInterceptor;
import com.cz.gameplat.web.interceptor.HYArgumentsResolver;
import com.github.theborakompanioni.spring.useragentutils.UserAgentHandlerMethodArgumentResolver;
import com.github.theborakompanioni.spring.useragentutils.UserAgentResolverHandlerInterceptor;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.validator.HibernateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;











@Configuration
@ImportResource({"classpath:spring/web-mvc.xml"})
@EnableWebMvc
@EnableAspectJAutoProxy
@ComponentScan(basePackages={"com.cz.gameplat"}, useDefaultFilters=false, includeFilters={@org.springframework.context.annotation.ComponentScan.Filter(type=org.springframework.context.annotation.FilterType.ANNOTATION, value={org.springframework.stereotype.Controller.class})})
public class MvcConfigSupport
  extends WebMvcConfigurationSupport
{
  private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", MvcConfigSupport.class.getClassLoader());
  
  static {
    if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", MvcConfigSupport.class
      .getClassLoader())) {} }
  private static final boolean jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", MvcConfigSupport.class
    .getClassLoader());
  
  private static final Logger logger = LoggerFactory.getLogger(MvcConfigSupport.class);
  
  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping()
  {
    return super.requestMappingHandlerMapping();
  }
  
  @Bean
  public ContentNegotiationManager mvcContentNegotiationManager() {
    return super.mvcContentNegotiationManager();
  }
  
  @Bean
  public UserAgentResolverHandlerInterceptor userAgentResolverHandlerInterceptor()
  {
    return new UserAgentResolverHandlerInterceptor();
  }
  
  @Bean
  public UserAgentHandlerMethodArgumentResolver userAgentHandlerMethodArgumentResolver() {
    return new UserAgentHandlerMethodArgumentResolver();
  }
  

  public void configureContentNegotiation(ContentNegotiationConfigurer configurer)
  {
    configurer.favorPathExtension(false).favorParameter(false);
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }
  

  @Bean
  public RedisLock redisLock(RedisTemplate<String, Long> redisTemplate)
  {
    return new RedisLock(redisTemplate);
  }
  
  @Bean
  public RedisLockAspect redisLockAspect(RedisLock redisLock) {
    return new RedisLockAspect(redisLock);
  }
  
  @Bean
  public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
    return super.requestMappingHandlerAdapter();
  }
  
  protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    System.out.println("---------===addArgumentResolvers start");
    argumentResolvers.add(argumentsResolver());
    argumentResolvers.add(userAgentHandlerMethodArgumentResolver());
  }
  
  @Bean
  public HYArgumentsResolver argumentsResolver()
  {
    return new HYArgumentsResolver();
  }
  


  public void configureMessageConverters(List<HttpMessageConverter<?>> converters)
  {
    StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
    stringConverter.setWriteAcceptCharset(false);
    
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(stringConverter);
    converters.add(new ResourceHttpMessageConverter());
    converters.add(new SourceHttpMessageConverter());
    converters.add(new AllEncompassingFormHttpMessageConverter());
    if (jaxb2Present) {
      converters.add(new Jaxb2RootElementHttpMessageConverter());
    }
    if (jackson2Present) {
      MappingJackson2HttpMessageConverter convert = new MappingJackson2HttpMessageConverter();
      convert.setObjectMapper(WafJsonMapper.getMapper());
      

      List<MediaType> supportedMediaTypes = new ArrayList();
      supportedMediaTypes.add(MediaType.APPLICATION_JSON);
      convert.setSupportedMediaTypes(supportedMediaTypes);
      
      converters.add(convert);
    }
  }
  

  protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer()
  {
    logger.info("---------===ConfigurableWebBindingInitializer");
    ConfigurableWebBindingInitializer initializer = super.getConfigurableWebBindingInitializer();
    DatePropertyEditorRegistrar register = new DatePropertyEditorRegistrar();
    register.setFormat("yyyy-MM-dd");
    initializer.setPropertyEditorRegistrar(register);
    return initializer;
  }
  

  protected void addInterceptors(InterceptorRegistry registry)
  {
    registry.addInterceptor(betIntercepter()).addPathPatterns(new String[] { "/api/bet", "/api/betG", "/api/bet/trace", "/api/sports/bet", "/api/live/play" });
    
    registry.addInterceptor(requestLogInterceptor()).addPathPatterns(new String[] { "/**" });
    registry.addInterceptor(loginInterceptor()).addPathPatterns(new String[] { "/api/**" })
      .excludePathPatterns(new String[] { "/api/config/getAll/analysis_api_host", "/api/recharge/onlinePayAsyncCallback/**/*", "/api/recharge/onlinePaySyncCallback/**/*", "/api/sports/loadResult", "/api/sports/message", "/api/sports/getNews", "/api/sports/getMaintenanceTime", "/api/recharge/deposit", "/api/payment/onlinePayAsyncCallback/**/*", "/api/sports/time", "/api/sports/getSportOnOff", "/api/fixedAsyncCallback/onlinePayAsyncCallback/**/*" });
    


    registry.addInterceptor(userAgentResolverHandlerInterceptor());
  }
  
  @Bean
  public RequestLogInterceptor requestLogInterceptor() {
    return new RequestLogInterceptor();
  }
  
  @Bean
  public LoginInterceptor loginInterceptor() {
    return new LoginInterceptor();
  }
  
  @Bean
  public BetIntercepter betIntercepter() {
    return new BetIntercepter();
  }
  
  @Bean
  public WebExceptionResolver webExceptionResolver() {
    return new WebExceptionResolver();
  }
  


  @Bean
  public HandlerExceptionResolver handlerExceptionResolver()
  {
    return new HandlerExceptionResolverComposite();
  }
  
  @Bean
  public CookieLocaleResolver localeResolver() {
    return new CookieLocaleResolver();
  }
  
  @Bean
  public ReloadableResourceBundleMessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:bundle/messages");
    source.setDefaultEncoding("UTF-8");
    source.setCacheSeconds(0);
    source.setUseCodeAsDefaultMessage(false);
    return source;
  }
  
  @Bean
  protected Validator getValidator()
  {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.setProviderClass(HibernateValidator.class);
    validator.setValidationMessageSource(messageSource());
    return validator;
  }
  
  @Bean
  public CommonsMultipartResolver multipartResolver() {
    return new CommonsMultipartResolver();
  }
}
