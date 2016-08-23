package com.medialets.test


import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class AddPlacement extends Simulation {

  val httpProtocol = http
    .baseURL("https://servo-iad2.medialets.com")
    .inferHtmlResources()
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en,en-US;q=0.8,zh-CN;q=0.5,zh;q=0.3")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:47.0) Gecko/20100101 Firefox/47.0")

  val loginHeader = Map("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

  val sessionHeader = Map("Content-Type" -> "application/json")

  val campaignHeader = Map(
    "Content-Type" -> "application/json",
    "X-Requested-With" -> "XMLHttpRequest")

  val propertyHeader = Map(
    "Accept" -> "*/*",
    "X-Requested-With" -> "XMLHttpRequest")

  val login = exec(http("loginRequest")
    .get("/login")
    .headers(loginHeader))
    .pause(1)
    .exec(http("sessionRequest")
      .post("/internal/session")
      .headers(sessionHeader)
      .body(RawFileBody("ServoNewAds/LoginHelperFile.txt"))
      .resources(http("setupCampaignRequest")
        .get("/internal/campaigns?status=&count=true&noads=false&tracking=1&limit=5000&offset=0&tagsNeedUpdating=0&q=")
        .headers(campaignHeader),
        http("setupCampaignRequest")
          .get("/internal/campaigns?status=&limit=305&offset=0&sort=&noads=false&tracking=1&tagsNeedUpdating=0&q=")
          .headers(campaignHeader)))

  val newPlacement = exec(http("placementRequest")
    .get("/campaigns/CA-JKQPT6C/placements")
    .headers(loginHeader)
    .resources(http("switchRequest")
      .get("/internal/session/switch/instances/2"),
      http("campaignRequest")
        .get("/internal/campaigns/CA-JKQPT6C")))
    .pause(1)
    .exec(http("propertyIDRequest")
      .get("/campaigns/CA-JKQPT6C/placements/create?property_id=1")
      .headers(loginHeader)
      .resources(http("campaignRequest")
        .get("/internal/campaigns/CA-JKQPT6C"),
        http("propertyRequest")
          .get("/internal/properties")
          .headers(propertyHeader),
        http("propertyRequest")
          .get("/internal/properties/1"),
        http("switchRequest")
          .get("/internal/session/switch/instances/2"),
        http("campaignRequest")
          .get("/internal/campaigns/CA-JKQPT6C")))
    .pause(1)
    .exec(http("addPlacementRequest")
      .post("/internal/campaigns/CA-JKQPT6C/placements")
      .headers(sessionHeader)
      .body(RawFileBody("ServoNewAds/NewPlacementHelperFile.txt"))
      .resources(http("campaignRequest")
        .get("/internal/campaigns/CA-JKQPT6C"),
        http("propertyRequest")
          .get("/internal/properties")
          .headers(propertyHeader),
        http("switchRequest")
          .get("/internal/session/switch/instances/2"),
        http("campaignRequest")
          .get("/internal/campaigns/CA-JKQPT6C"),
        http("placementLimitRequest")
          .get("/internal/campaigns/CA-JKQPT6C/placements?limit=5000")))

  val newPlacementUsers = scenario("New Placement").exec(login, newPlacement)

  setUp(newPlacementUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
