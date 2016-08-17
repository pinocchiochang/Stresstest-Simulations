package com

import com.typesafe.config.ConfigFactory

/**
  * Created by NoahKaplan on 6/29/16.
  */
package object medialets {
  val appConf = ConfigFactory.load()
  val baseURL = appConf.getString("httpConf.baseURL")
  val errorLoggingURL = appConf.getString("httpConf.errorLoggingURL")
  val errorLoggingKey = appConf.getString("keys.errorLoggingKey")
  val acceptHeader = appConf.getString("httpConf.acceptHeader")
  val acceptEncodingHeader = appConf.getString("httpConf.acceptEncodingHeader")
  val acceptLanguageHeader = appConf.getString("httpConf.acceptLanguageHeader")
  val userAgentHeader = appConf.getString("httpConf.userAgentHeader")
}