package com.example.hotel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Jackson JSON設定クラス 日本語文字化け防止とUTF-8エンコーディング強制設定
 */
@Configuration
public class JacksonConfig implements WebMvcConfigurer {

  /**
   * ObjectMapperのカスタム設定 UTF-8エンコーディングと日本語文字の適切な処理を保証
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Java 8 時間API対応
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // UTF-8エンコーディング関連設定
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    return mapper;
  }

  /**
   * HTTPメッセージコンバーターの設定 JSON出力時のUTF-8エンコーディングを強制
   */
  @Override
  public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    ObjectMapper mapper = objectMapper();
    if (mapper != null) {
      converter.setObjectMapper(mapper);
    }
    converter.setDefaultCharset(StandardCharsets.UTF_8);

    // UTF-8強制設定
    converter.setPrettyPrint(false);

    converters.add(0, converter);
  }
}
