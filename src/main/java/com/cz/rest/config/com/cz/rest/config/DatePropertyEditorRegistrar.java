package com.cz.rest.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.WebRequest;


























public class DatePropertyEditorRegistrar
  implements PropertyEditorRegistrar, WebBindingInitializer
{
  private String format = "yyyy-MM-dd";
  
  public void setFormat(String format) {
    this.format = format;
  }
  

  public void registerCustomEditors(PropertyEditorRegistry registry)
  {
    SimpleDateFormat dateFormat = new SimpleDateFormat(this.format);
    dateFormat.setLenient(false);
    
    registry.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat));
    

    registry.registerCustomEditor(String.class, new StringTrimmerEditor(false));
  }
  


  public void initBinder(WebDataBinder binder, WebRequest request)
  {
    SimpleDateFormat dateFormat = new SimpleDateFormat(this.format);
    dateFormat.setLenient(false);
    
    binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat));
    

    binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
  }
  
  public String getFormat()
  {
    return this.format;
  }
}
