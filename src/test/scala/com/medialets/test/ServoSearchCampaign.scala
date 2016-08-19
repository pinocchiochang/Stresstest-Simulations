package com.medialets.test

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class ServoSearchCampaign extends Simulation {

  val httpProtocol = http
    .baseURL("https://servo-iad2.medialets.com")
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,zh-CN;q=0.8,zh;q=0.5,en;q=0.3")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:47.0) Gecko/20100101 Firefox/47.0")

  val searchCampaignHeader = Map(
    "Content-Type" -> "application/json",
    "X-Requested-With" -> "XMLHttpRequest")

  val login = new ServoLogIn

  val campaignFeeder = csv("SearchCampaignName/searchCampaignName.csv").random
  // Search Campaign
  val searchCampaign = feed(campaignFeeder)
    .exec(http("searchRequest for ${searchQuery}")
    .get("/internal/campaigns?status=&count=true&noads=false&tracking=1&limit=5000&offset=0&tagsNeedUpdating=0&q=${searchQuery}")
    .headers(searchCampaignHeader)
    .resources(http("rollScanRequest")
      .get("/internal/campaigns?status=&limit=110&offset=0&sort=&noads=false&tracking=1&tagsNeedUpdating=0&q=${searchQuery}")
      .headers(searchCampaignHeader)))

  val searchUsers = scenario("search campaign").exec(login(), searchCampaign)

  setUp(searchUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
