package com.medialets.sims

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * Created by NoahKaplan on 6/23/16.
  */
object LoginSim {
  val login = exec(http("loginRequest")
    .get(appConf.getString("urls.login"))
    .headers(loginHeader))
    .exec(http("sessionRequest")
      .post({val str = appConf.getString("urls.session"); str})
      .headers(contentHeader)
      .body(StringBody(loginReq))
      .resources(http("campaignsRequest")
        .get(appConf.getString("urls.campaignsLanding"))
        .headers(campaignHeader),
        http("submitSessionRequest")
          .get(appConf.getString("urls.sessionSubmit"))
          .headers(campaignHeader)))
}
