package com.cz.rest.config;

import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.*;
import java.util.EnumSet;











@Order(1)
public class WebApplicationInitializer
  extends AbstractAnnotationConfigDispatcherServletInitializer
{
  protected String[] getServletMappings()
  {
    return new String[] { "/" };
  }
  



  protected Class[] getServletConfigClasses()
  {
    return new Class[] { MvcConfigSupport.class };
  }
  






  protected void customizeRegistration(ServletRegistration.Dynamic registration) {}
  





  public void onStartup(ServletContext servletContext)
    throws ServletException
  {
    super.onStartup(servletContext);
    initFilters(servletContext);
    registerFilters(servletContext);
  }
  

  private void initFilters(ServletContext servletContext)
  {
    initCharacterEncodingFilter(servletContext);
  }
  





  protected void registerFilters(ServletContext servletContext) {}
  





  protected void initCharacterEncodingFilter(ServletContext servletContext)
  {
    CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
    
    characterEncodingFilter.setEncoding("UTF-8");
    addFilter(servletContext, "characterEncodingFilter", characterEncodingFilter);
  }
  
  protected void addFilter(ServletContext servletContext, String filterName, Filter filter) {
    FilterRegistration.Dynamic filterRegistration = servletContext.addFilter(filterName, filter);
    filterRegistration.setAsyncSupported(isAsyncSupported());
    filterRegistration.addMappingForUrlPatterns(getDispatcherTypes(), false, new String[] { "/*" });
  }
  
  protected EnumSet<DispatcherType> getDispatcherTypes() {
    return isAsyncSupported() ? 
      EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) : 
      EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE);
  }
  





  protected Class<?>[] getRootConfigClasses()
  {
    return new Class[0];
  }
}
