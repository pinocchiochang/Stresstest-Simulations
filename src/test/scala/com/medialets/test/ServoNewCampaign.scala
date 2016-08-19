package com.medialets.test

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class ServoNewCampaign extends Simulation {

  val httpProtocol = http
    .baseURL("https://servo-iad2.medialets.com")
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,zh-CN;q=0.8,zh;q=0.5,en;q=0.3")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:47.0) Gecko/20100101 Firefox/47.0")

  val createCampaignHeader = Map("Content-Type" -> "application/json")

  val instanceHeader = Map(
    "Accept" -> "*/*",
    "X-Requested-With" -> "XMLHttpRequest")

  val setupCampaignHeader = Map("X-Requested-With" -> "XMLHttpRequest")

  val login = new ServoLogIn

  //create a simple campaign, no attributions, no placements, no creatives, no ads
  val createCampaign = exec(http("sessionRequest")
    .get("/internal/session/switch/instances/2")
    .resources(http("instancesRequest")
      .get("/internal/instances/2/users")
      .headers(instanceHeader)))
    .pause(1)
    .exec(http("setupAgenciesRequest")
      .get("/internal/agencies")
      .headers(setupCampaignHeader))
    .pause(1)
    .exec(http("setupAdvertisersRequest")
      .get("/internal/advertisers")
      .headers(setupCampaignHeader))
    .pause(1)
    .exec(http("setupBrandRequest")
      .get("/internal/advertisers/1")
      .headers(setupCampaignHeader))
    .pause(1)
    .exec(http("saveRequest")
      .post("/internal/campaigns")
      .headers(createCampaignHeader)
      .body(RawFileBody("ServoNewCampaign/ServoCreateCampaignRequest.txt"))
      .resources(http("advertisersRequest")
        .get("/internal/advertisers/1/brands/BR-9AFDN42/goals")))


  val loginUsers = scenario("Login").exec(login())
  val createUsers = scenario("Create a simple campaign").exec(login(), createCampaign)

  setUp(createUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
