package com.medialets.test


import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class GenerateTags extends Simulation {

  val httpProtocol = http
    .baseURL("https://servo-iad2.medialets.com")
    .inferHtmlResources()
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en,en-US;q=0.8,zh-CN;q=0.5,zh;q=0.3")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:47.0) Gecko/20100101 Firefox/47.0")

  val loginHeader = Map("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

  val tagsHeader = Map("Content-Type" -> "application/json")

  val campaignHeader = Map(
    "Content-Type" -> "application/json",
    "X-Requested-With" -> "XMLHttpRequest")

  val login = new ServoLogIn

  val generateTags =
    //this tag button can be just clicked once, once tag is generate, it cannot be removed, it can only be changed
    exec(http("campaignRequest")
    .post("/internal/campaigns/CA-JKQPT6C/placements/tags")
    .headers(tagsHeader)
    .body(RawFileBody("ServoGenerateTags/GenerateTagsHelperFile.txt"))
    .check(status.is(400)))
    .pause(1)
    //change TAG type, js/html, if a placement has multiple creatives, it cannot be html, ontly js
    .exec(http("setupTagsRequest")
    .post("/internal/campaigns/CA-JKQPT6C/placements/tags?force=true")
    .headers(tagsHeader)
    .body(RawFileBody("ServoGenerateTags/GenerateTagsHelperFile.txt"))
    .resources(http("viewPlacementRequest")
      .get("/internal/campaigns/CA-JKQPT6C/placements/PL-67FQGQN"),
      http("choosePlacementRequest")
        .get("/internal/campaigns/CA-JKQPT6C/placements?count=true&noads=false&tracking=1&limit=5000&offset=0&tagsNeedUpdating=0&q=")
        .headers(campaignHeader),
      http("choosePlacementRequest")
        .get("/internal/campaigns/CA-JKQPT6C/placements/facets?sort=publisher_property_id+asc%2Cplacement_group_id+asc%2Cid+asc&groupBy=publisher_property_id%2Cplacement_group_id&tagsNeedUpdating=0&noads=false&tracking=1&q=")
        .headers(campaignHeader),
      http("choosePlacementRequest")
        .get("/internal/campaigns/CA-JKQPT6C/placements?limit=102&offset=0&sort=publisher_property_id+asc%2Cplacement_group_id+asc%2Cid+asc&noads=false&tracking=1&tagsNeedUpdating=0&q=")
        .headers(campaignHeader)))

  val tagsUsers = scenario("Generate Tag").exec(login(), generateTags)

  setUp(tagsUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
