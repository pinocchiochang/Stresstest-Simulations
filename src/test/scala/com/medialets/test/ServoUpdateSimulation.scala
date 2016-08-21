package com.medialets.test

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class ServoUpdateSimulation extends Simulation {

  val httpProtocol = http
    .baseURL("https://servo-iad2.medialets.com")
    .inferHtmlResources()
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:47.0) Gecko/20100101 Firefox/47.0")

  val loginHeader = Map("Origin" -> "https://servo-iad2.medialets.com")

  val sessionHeader = Map(
    "Accept" -> "application/json, text/javascript, */*; q=0.01",
    "Content-Type" -> "application/json")

  val campaignHeader = Map(
    "Accept" -> "application/json, text/javascript, */*; q=0.01",
    "Content-Type" -> "application/json",
    "X-Requested-With" -> "XMLHttpRequest")

  val indexHeader = Map("Accept" -> "application/json, text/javascript, */*; q=0.01")

  val instanceHeader = Map(
    "Accept" -> "*/*",
    "X-Requested-With" -> "XMLHttpRequest")

  val responseSegmentUrl = "https://api.mixpanel.com:443"
  val responseCloudfrontUrl = "https://d2dq2ahtl5zl1z.cloudfront.net:443/analytics.js/v1/415n300sxr/analytics.min.js"

  val loginWithCA = exec(http("sessionRequest")
    .post("/internal/session")
    .headers(sessionHeader)
    .body(RawFileBody("ServoUpdateCampaign/UpdateSimLoginRequest.txt"))
    .resources(http("authRequest")
      .get(responseSegmentUrl + "/decide/?verbose=1&version=1&lib=web&token=205dbaafcdb4babb3b2e5a74b8f162e9&distinct_id=1&ip=1&_=1467302432844")
      .headers(loginHeader)))
    pause(1) //after update, delete the original one
    .exec(http("campaignRequest")
      .get("/internal/campaigns?status=&count=true&noads=false&tracking=1&limit=5000&offset=0&tagsNeedUpdating=0&q=noah")
      .headers(campaignHeader)
      .resources(http("campaignRequest")
        .get("/internal/campaigns?status=&limit=131&offset=0&sort=&noads=false&tracking=1&tagsNeedUpdating=0&q=noah")
        .headers(campaignHeader)))

  val viewCampaign = exec(http("viewCampaignRequest")
    .get("/campaigns/CA-TJPNH7B")
    .resources(http("cloudfrontRequest")
      .get(responseCloudfrontUrl + "")
      .headers(loginHeader),
      http("sessionRequest")
        .get("/internal/session/switch/instances/2")
        .headers(indexHeader),
      http("instanceRequest")
        .get("/internal/instances/2/users")
        .headers(instanceHeader),
      http("sessionRequest")
        .get("/internal/session/switch/instances/2")
        .headers(indexHeader),
      http("gotoCampaignsRequest")
        .get("/internal/campaigns/CA-TJPNH7B")
        .headers(indexHeader),
      http("advertisersRequest")
        .get("/internal/advertisers/1/brands/BR-9AFDN42/goals")
        .headers(indexHeader),
      http("reportRequest")
        .get("/api/v2/reporting/campaigns/CA-TJPNH7B/reporting/overview/total")
        .headers(indexHeader)
        .check(status.is(404))))

  val updateCampaign = exec(http("updateCampaignRequest")
    .put("/internal/campaigns/CA-TJPNH7B")
    .headers(sessionHeader)
    .body(RawFileBody("ServoUpdateCampaign/UpdateSimUpdateNameRequest.txt"))
    .resources(http("sessionRequest")
        .get("/internal/session/switch/instances/2")
        .headers(indexHeader),
      http("instancesRequest")
        .get("/internal/instances/2/users")
        .headers(instanceHeader),
      http("switchRequest")
        .get("/internal/session/switch/instances/2")
        .headers(indexHeader),
      http("campaignRequest")
        .get("/internal/campaigns/CA-TJPNH7B")
        .headers(indexHeader),
      http("advertisersRequest")
        .get("/internal/advertisers/1/brands/BR-9AFDN42/goals")
        .headers(indexHeader),
      http("reportingRequest")
        .get("/api/v2/reporting/campaigns/CA-TJPNH7B/reporting/overview/total")
        .headers(indexHeader)
        .check(status.is(404))))

  val updateUsers = scenario("Update campaign").exec(loginWithCA, viewCampaign, updateCampaign)

  setUp(updateUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
