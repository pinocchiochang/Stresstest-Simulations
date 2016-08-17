package com.medialets.sims

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

/**
  * Created by NoahKaplan on 6/23/16.
  */
object GenReportSims {
  def genRandomReport(count: Long) = buildChain(false, count, exec(findRandCampaign(true), setRandCampaignAttributes), genSimpleReport)

  private def genSimpleReport: ChainBuilder = {
    exec(http("reportRequest")
      .get(appConf.getString("urls.campaignReportMedia"))
      .headers(loginHeader)
      .resources(http("viewPlacementsRequest")
        .get(appConf.getString("urls.campaignPlacements")),
        http("sessionSwitchRequest")
          .get(appConf.getString("urls.switchInstance")),
        http("campaignsRequest")
          .get(appConf.getString("urls.campaign")),
        http("reportSetRequest")
          .get(appConf.getString("urls.campaignReport"))
          .headers(campaignHeader)))
    .exec { session =>
      println("\nCampaign report generated! Information: Campaign ID - " + session.attributes("randCampaignID") + "\n")
      session
    }
  }
}
