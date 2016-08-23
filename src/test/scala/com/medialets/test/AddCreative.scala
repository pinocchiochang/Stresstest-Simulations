package com.medialets.test


import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class AddCreative extends Simulation {

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

	val uploadHeader = Map(
		"Accept" -> "*/*",
		"Content-Type" -> "multipart/form-data; boundary=---------------------------1246067875402249581330187111",
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

	val newCreative = exec(http("campaignRequest")
		.get("/campaigns/CA-JKQPT6C/creatives/upload/static")
		.headers(loginHeader)
		.resources(http("campaignRequest")
			.get("/internal/campaigns/CA-JKQPT6C"),
			http("switchRequest")
				.get("/internal/session/switch/instances/2")))
		.pause(1)
		.exec(http("uploadRequest")
			.post("/internal/campaigns/CA-JKQPT6C/creatives/upload?type=static")  //response of this is the dynamic uri
      .headers(uploadHeader)
			.body(RawFileBody("ServoNewAds/ImageFile.txt")))
		.pause(1)
		.exec(http("saveRequest")
			.post("/internal/campaigns/CA-JKQPT6C/creatives")
			.headers(sessionHeader)
			.body(RawFileBody("ServoNewAds/NewCreativeHelperFile.txt"))
			.resources(http("creativesRequest")
				.get("/internal/campaigns/CA-JKQPT6C/creatives"),
				http("campaignRequest")
					.get("/internal/campaigns/CA-JKQPT6C"),
				http("setupCreativesRequest")
					.get("/internal/campaigns/CA-JKQPT6C/creatives?count=true&noads=false&tracking=0&limit=5000&offset=0&tagsNeedUpdating=0&q=")
					.headers(campaignHeader),
				http("setupCreativesRequest")
					.get("/internal/campaigns/CA-JKQPT6C/creatives?limit=101&offset=0&sort=id+asc&noads=false&tracking=0&tagsNeedUpdating=0&q=")
					.headers(campaignHeader)))

	val newCreativeUsers = scenario("New Creative").exec(login, newCreative)

	setUp(newCreativeUsers.inject(atOnceUsers(1))).protocols(httpProtocol)
}
