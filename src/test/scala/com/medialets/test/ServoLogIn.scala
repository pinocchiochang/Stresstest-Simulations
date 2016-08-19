package com.medialets.test

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class ServoLogIn extends Simulation {

  val httpProtocol = http
    .baseURL("https://servo-iad2.medialets.com")
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,zh-CN;q=0.8,zh;q=0.5,en;q=0.3")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:47.0) Gecko/20100101 Firefox/47.0")

  val loginHeader = Map("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

  val sessionHeader = Map("Content-Type" -> "application/json")

  val campaignHeader = Map(
    "Content-Type" -> "application/json",
    "X-Requested-With" -> "XMLHttpRequest")

  //ServoLogIn
  def apply() = {
    exec(http("loginRequest")
      .get("/login")
      .headers(loginHeader))
      .pause(1)
      .exec(http("sessionRequest")
        .post("/internal/session")
        .headers(sessionHeader)
        .body(RawFileBody("ServoLogin2/ServoLogInRequest.txt"))
        .resources(http("campaignsRequest")
          .get("/internal/campaigns?status=&count=true&noads=false&tracking=1&limit=5000&offset=0&tagsNeedUpdating=0&q=")
          .headers(campaignHeader),
          http("submitSessionRequest")
            .get("/internal/campaigns?status=&limit=305&offset=0&sort=&noads=false&tracking=1&tagsNeedUpdating=0&q=")
            .headers(campaignHeader)))

  }

  val loginUsers = scenario("Login").exec(apply)

  setUp(loginUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
