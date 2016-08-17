package com.medialets.sims

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

object CampaignSims {
  def viewCampaign(count: Long) = buildChain(false, count, exec(findRandCampaign(), setRandCampaignAttributes), viewSingleCampaign)
  def addCampaignWAttr(count: Long) = buildChain(false, count, exec(findRandCampaign(), setRandCampaignAttributes, setCreateCampaignAttributes,
                                                 setGoalsAttributes(UpdateType.Add)), createCampaign(true))
  def addCampaignWOAttr(count: Long) = buildChain(false, count, exec(findRandCampaign(), setRandCampaignAttributes, setCreateCampaignAttributes), createCampaign(false))
  def changeCampaignName(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes), updateCampaignName)
  def addCampaignGoal(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes, setGoalsAttributes(UpdateType.Add)), updateCampaignGoal(UpdateType.Add))
  def remCampaignGoal(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes, setGoalsAttributes(UpdateType.Remove)), updateCampaignGoal(UpdateType.Remove))
  def addAlerts(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes, setUpdateAlertsAttributes(UpdateType.Add)), updateAlerts(UpdateType.Add))
  def remAlerts(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes, setUpdateAlertsAttributes(UpdateType.Remove)),  updateAlerts(UpdateType.Remove))
  def addAdWithTag(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes, setAdAttributes, setTagAttributes), createAd(true))
  def addAdWithoutTag(count: Long) = buildChain(true, count, exec(findRandCampaign(), setRandCampaignAttributes, setAdAttributes), createAd(false))

  private def createAd(genTag: Boolean): ChainBuilder = {
    exec(http("adsRequest")
      .get(appConf.getString("urls.campaignAdvertisements"))
      .headers(loginHeader)
      .resources(http("switchRequest")
        .get(appConf.getString("urls.switchInstance")),
        http("campaignRequest")
          .get(appConf.getString("urls.campaign")),
        http("selectPlacementRequest")
          .get(appConf.getString("urls.setupPlacement"))
          .headers(campaignHeader),
        http("selectCreativeRequest")
          .get(appConf.getString("urls.selectCreative"))
          .headers(campaignHeader)))
    .exec(http("newAdsRequest")
      .post({val str = appConf.getString("urls.internalCampaignAdvertisements"); str})
      .headers(contentHeader)
      .body(StringBody("${addAdBody}"))
      .resources(http("gobackCampaignRequest")
      .get(appConf.getString("urls.campaign"))))
    .exec { session =>
      println("\nAdded a new campaign ad! Information: Campaign ID - " + session.attributes("randCampaignID") + "\n")
      session
    }
    .doIf(genTag) {
      exec(http("createTagsRequest")
        .post({val str = appConf.getString("urls.placementTags"); str})
        .headers(contentHeader)
        .body(StringBody("${tagBody}")))
      .exec(http("checkTagRequest")
        .get(appConf.getString("urls.placement")))
      .exec { session =>
        println("Tag generated! Information: Campaign ID - " + session.attributes("randCampaignID") + ", Placement ID - " + session.attributes("placementID") + "\n")
        session
      }
    }
  }

  private def updateCampaignGoal(updateGoalsType: UpdateType.Value) =
    exec(http("campaignRequest")
      .put({val str = appConf.getString("urls.campaign"); str})
      .headers(sessionHeader)
      .body(StringBody(if (updateGoalsType == UpdateType.Add) "${addGoalBody}" else "${remGoalBody}"))
      .resources(http("switchRequest")
        .get(appConf.getString("urls.switchInstance"))
        .headers(indexHeader),
        http("instancesRequest")
          .get(appConf.getString("urls.instanceUsers"))
          .headers(instanceHeader),
        http("switchRequest")
          .get(appConf.getString("urls.switchInstance"))
          .headers(indexHeader),
        http("campaignRequest")
          .get(appConf.getString("urls.campaign"))
          .headers(indexHeader),
        http("attrSetUpRequest")
          .get(appConf.getString("urls.goals"))
          .headers(indexHeader)))
    .exec { session =>
      if(updateGoalsType == UpdateType.Add) {
        println("\nAdded campaign goal! Information: Campaign ID - " + session.attributes("randCampaignID") + "\n")
      }
      else {
        println("\nRemoved campaign goal! Information: Campaign ID - " + session.attributes("randCampaignID") + "\n")
      }
      session
    }

  private def createCampaign(withAttr: Boolean): ChainBuilder = {
    exec(http("setupAgenciesRequest")
      .get(appConf.getString("urls.agencies"))
      .headers(setupCampaignHeader))
    .exec(http("setupAdvertisersRequest")
      .get(appConf.getString("urls.advertisers"))
      .headers(setupCampaignHeader))
    .exec(createCampaignRequest(withAttr))
    .exec(http("saveRequest")
      .post({val str = appConf.getString("urls.campaigns"); str})
      .headers(contentHeader)
      .body(StringBody("${newRandCampBody}")))
  }

  private def viewSingleCampaign: ChainBuilder = {
    exec(http("viewCampaignRequest")
      .get(appConf.getString("urls.externalCampaign"))
      .headers(loginHeader)
      .resources(http("switchRequest")
        .get(appConf.getString("urls.switchInstance")),
        http("viewInstanceRequest")
          .get(appConf.getString("urls.instanceUsers"))
          .headers(instanceHeader),
        http("switchRequest")
          .get(appConf.getString("urls.switchInstance")),
        http("campaignsRequest")
          .get(appConf.getString("urls.campaign"))))
    .exec(viewPlacement, viewCreative, viewAdvertisement, viewTag)
    .exec { session =>
      println("\nRandom campaign was viewed (overview, placements, creatives, advertisements, and tags)! Information: Campaign ID - " +
              session.attributes("randCampaignID") + "\n")
      session
    }
  }

  private val viewPlacement = exec(http("switchRequest")
    .get(appConf.getString("urls.switchInstance"))
    .resources(http("viewRequest")
      .get(appConf.getString("urls.campaign")),
      http("placementSetupRequest")
        .get(appConf.getString("urls.placementSetup1"))
        .headers(campaignHeader),
      http("placementSetupRequest")
        .get(appConf.getString("urls.placementSetup2"))
        .headers(campaignHeader),
      http("placementSetupRequest")
        .get(appConf.getString("urls.placementSetup3"))
        .headers(campaignHeader)))

  private val viewCreative = exec(http("viewRequest")
    .get(appConf.getString("urls.externalCampaignCreatives"))
    .headers(loginHeader)
    .resources(http("switchRequest")
      .get(appConf.getString("urls.switchInstance")),
      http("viewCampaignRequest")
        .get(appConf.getString("urls.campaign")),
      http("creativeSetupRequest")
        .get(appConf.getString("urls.campaignCreativeSetup1"))
        .headers(campaignHeader),
      http("creativeSetupRequest")
        .get(appConf.getString("urls.setupCreative"))
        .headers(campaignHeader)))

  private val viewAdvertisement = exec(http("viewAdsRequest")
    .get(appConf.getString("urls.campaignAdvertisements"))
    .headers(loginHeader)
    .resources(http("viewRequest")
      .get(appConf.getString("urls.campaign")),
      http("switchRequest")
        .get(appConf.getString("urls.switchInstance")),
      http("creativeSetupRequest")
        .get(appConf.getString("urls.selectCreative"))
        .headers(campaignHeader),
      http("placementSetupRequest")
        .get(appConf.getString("urls.setupPlacement"))
        .headers(campaignHeader)))

  private val viewTag = exec(http("viewTagRequest")
    .get(appConf.getString("urls.campaignTags"))
    .headers(loginHeader))
    .exec(http("viewCampaignRequest")
      .get(appConf.getString("urls.campaign"))
      .resources(http("switchRequest")
        .get(appConf.getString("urls.switchInstance")),
        http("placementSetupRequest")
          .get(appConf.getString("urls.placementSetup1"))
          .headers(campaignHeader),
        http("placementSetupRequest")
          .get(appConf.getString("urls.placementSetup2"))
          .headers(campaignHeader),
        http("placementSetupRequest")
          .get(appConf.getString("urls.placementSetup3"))
          .headers(campaignHeader)))

  private def updateCampaignName: ChainBuilder = {
    exec(http("updateCampaignRequest")
      .put({val str = appConf.getString("urls.campaign"); str})
      .headers(sessionHeader)
      .body(StringBody("${randCampaignNewNameDetails}"))
      .resources(http("sessionRequest")
        .get(appConf.getString("urls.switchInstance"))
        .headers(indexHeader),
        http("instancesRequest")
          .get(appConf.getString("urls.instanceUsers"))
          .headers(instanceHeader),
        http("switchRequest")
          .get(appConf.getString("urls.switchInstance"))
          .headers(indexHeader),
        http("campaignRequest")
          .get(appConf.getString("urls.campaign"))
          .headers(indexHeader)))
    .exec { session =>
      println(session.attributes("newNameDescStr").toString)
      session
    }
  }

  private def updateAlerts(updateAlertsType: UpdateType.Value): ChainBuilder = {
    exec(http("updateAlertRequest")
      .put({val str = appConf.getString("urls.campaign"); str})
      .headers(contentHeader)
      .body(StringBody(if (updateAlertsType == UpdateType.Add) "${addAlertsBody}" else "${remAlertsBody}"))
      .resources(http("switchRequest")
      .get(appConf.getString("urls.switchInstance")),
      http("usersRequest")
        .get(appConf.getString("urls.instanceUsers"))
        .headers(instanceHeader),
      http("campaignRequest")
        .get(appConf.getString("urls.campaign")),
      http("switchRequest")
        .get(appConf.getString("urls.switchInstance")),
      http("goalRequest")
        .get(appConf.getString("urls.goals"))))
    .exec { session =>
      if(updateAlertsType == UpdateType.Add) {
        println("\nEnabled campaign alerts! Information: Campaign ID - " + session.attributes("randCampaignID") + "\n")
      }
      else {
        println("\nDisabled campaign alerts! Information: Campaign ID - " + session.attributes("randCampaignID") + "\n")
      }
      session
    }
  }
}