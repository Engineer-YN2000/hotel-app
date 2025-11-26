package com.example.hotel.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

/**
 * アプリケーション全体の設定クラス
 */
@Configuration
@EnableConfigurationProperties(PriceProperties.class)
@ComponentScan(basePackages = "com.example.hotel.config")
public class AppConfig {
}
