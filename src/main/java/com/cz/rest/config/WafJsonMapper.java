package com.cz.rest.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.springframework.util.Assert;












public class WafJsonMapper
{
  private static ObjectMapper mapper = new ObjectMapper();
  

  static
  {
    mapper.setTimeZone(TimeZone.getDefault());
    
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
    mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    
    mapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, false);
    


    mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
    
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
  }
  
  public static ObjectMapper getMapper() {
    return mapper;
  }
  
  public static void setMapper(ObjectMapper mapper) {
    mapper = mapper;
  }
  
  public static <T> T parse(String json, Class<T> objectType) throws IOException {
    if (json == null)
      return null;
    Assert.notNull(objectType, "objectType cannot be null.");
    return (T)mapper.readValue(json, objectType);
  }
  
  public static <T> T parse(InputStream stream, Class<T> objectType) throws IOException {
    Assert.notNull(stream, "stream cannot be null.");
    Assert.notNull(objectType, "objectType cannot be null.");
    return (T)mapper.readValue(stream, objectType);
  }
  
  public static String toJson(Object obj) throws IOException
  {
    return mapper.writeValueAsString(obj);
  }
}
