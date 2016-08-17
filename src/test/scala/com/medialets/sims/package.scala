package com.medialets

import com.typesafe.config.ConfigFactory
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import org.json4s._
import org.json4s.jackson.JsonMethods._
import java.io.FileReader
import java.util.Random

import faker.Name
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.annotation.tailrec

/**
  * Created by NoahKaplan on 6/29/16.
  */
package object sims {
  implicit val formats = DefaultFormats
  val appConf = ConfigFactory.load()

  private val loginMap = Map[String, String]("username" -> appConf.getString("login.username"), "password" -> appConf.getString("login.password"))
  val loginReq = Serialization.write(loginMap)

  object UpdateType extends Enumeration {
    val Add, Remove = Value
  }

  val contentHeader = Map("Content-Type" -> "application/json")
  val campaignHeader = Map("Content-Type" -> "application/json", "X-Requested-With" -> "XMLHttpRequest")
  val instanceHeader = Map("Accept" -> "*/*", "X-Requested-With" -> "XMLHttpRequest")
  val setupCampaignHeader = Map("X-Requested-With" -> "XMLHttpRequest")
  val loginHeader = Map("Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
  val sessionHeader = Map("Accept" -> "application/json, text/javascript, */*; q=0.01", "Content-Type" -> "application/json")
  val indexHeader = Map("Accept" -> "application/json, text/javascript, */*; q=0.01")
  val uploadHeader = Map(
    "Accept" -> "*/*",
    "Content-Type" -> "multipart/form-data; boundary=---------------------------1246067875402249581330187111",
    "X-Requested-With" -> "XMLHttpRequest")

  def buildChain(keepResettingAttributes: Boolean, count: Long, attributesSetter: => ChainBuilder, action: => ChainBuilder): ChainBuilder = {
    if(keepResettingAttributes) repeatChainBuilder(exec{s => s}, count, attributesSetter.exec(action))
    else repeatChainBuilder(attributesSetter, count, action)
  }

  @tailrec private def repeatChainBuilder(acc: => ChainBuilder, count: Long, chainBuilder: => ChainBuilder): ChainBuilder = {
    if(count == 0) acc
    else repeatChainBuilder(acc.exec(chainBuilder), count - 1, chainBuilder)
  }

  def createCampaignRequest(withAttr: Boolean): ChainBuilder = {
    doIf(withAttr) {
      exec(http("getRandCampaignGoals").get(appConf.getString("urls.goals"))
        .check(regex(""".*""").saveAs("getRandCampaignGoals")))
    }
    .exec { session =>
      val randName = Name.name
      val campaignName = appConf.getString("simulationConf.campaignPrefix") + randName
      val advertiserID = session.attributes("randAdvertiserID").toString
      val brandID = session.attributes("randBrandID").toString
      val agencyID = session.attributes("randAgencyID").toString
      val instanceID = session.attributes("randCampaignInstanceID").toString
      val fileName = if(withAttr) "src/test/resources/NewCampaignWithAttributions/CreateCampaignRequestWithAttr.txt"
                     else "src/test/resources/NewCampaign/CreateCampaignRequest.txt"
      val ret = parse(new FileReader(fileName)).extract[JObject] transformField {
        case JField("name", _) => ("name", JString(campaignName))
        case JField("instanceID", _) => ("instanceID", JInt(instanceID.toInt))
        case JField("basic", basicInfo) => ("basic", basicInfo transformField {
          case JField("agency", _) => ("agency", JObject(List(JField("id", JInt(agencyID.toInt)))))
          case JField("advertiser", _) => ("advertiser", JObject(List(JField("id", JInt(advertiserID.toInt)))))
          case JField("brand", _) => ("brand", JObject(List(JField("id", JString(brandID)))))
        })
      }
      println("\nRandom campaign created! Information: Name - " + campaignName + ", Advertiser ID - " +
        advertiserID + ", Agency - " +  agencyID + ", Brand Name - " + brandID + ", Instance ID - " + instanceID + "\n")
      if(withAttr) {
        val rawRandCampaignGoals = parse(session.attributes("getRandCampaignGoals").toString()).extract[List[JObject]]
        if(rawRandCampaignGoals.isEmpty) throw new IllegalArgumentException("You do not have the sufficient goals added to run this simulation!")
        val lastAddedGoal = rawRandCampaignGoals.last
        val lookback = lastAddedGoal \ "postbackLookback" \ "view"
        val campaignGoalToAdd = JObject(List(JField("id", lastAddedGoal \ "id"), JField("lookbackClick", lookback), JField("lookbackView", lookback)))
        val retWithAttr = ret transformField {
          case JField("campaignGoals", _) => ("campaignGoals", JArray(List(campaignGoalToAdd)))
        }
        session.set("newRandCampBody", Serialization.write(retWithAttr))
      }
      else {
        session.set("newRandCampBody", Serialization.write(ret))
      }
    }
  }

  def setRandCampaignAttributes: ChainBuilder = {
    exec(http("getRandCampaignDetails")
      .get(appConf.getString("urls.campaign")).check(regex(""".*""").saveAs("randCampaignDetails")))
    .exec { session =>
      val randName = Name.name
      val rawRandCampaignDetails = parse(session.attributes("randCampaignDetails").toString()).extract[JObject]
      val JInt(randCampaignInstanceID) = rawRandCampaignDetails \ "instanceId"
      val JInt(randCampaignAdvertiserID) = rawRandCampaignDetails \ "basic" \ "advertiser" \ "id"
      val JString(randCampaignBrandID) = rawRandCampaignDetails \ "basic" \ "brand" \ "id"
      val newName = appConf.getString("simulationConf.campaignPrefix") + randName
      val JString(oldName) = rawRandCampaignDetails \ "name"
      val randCampaignID = session.attributes("randCampaignID").toString
      val newNameDescStr = "\nCampaign name has changed! Information: New name: " + newName + ", Old name: " + oldName + ", ID: " + randCampaignID + "\n"
      session.setAll(
        ("randCampaignNewNameDetails", Serialization.write(rawRandCampaignDetails transformField {
          case JField("name", _) => ("name", JString(newName))
        })),
        ("randCampaignInstanceID", randCampaignInstanceID), ("randAdvertiserID", randCampaignAdvertiserID), ("randBrandID", randCampaignBrandID),
        ("randCampaignDetails", session.attributes("randCampaignDetails").toString), ("newNameDescStr", newNameDescStr))
    }
  }

  def findRandCampaign(mustHaveAds: Boolean = false): ChainBuilder = {
    exec(http("getAllCampaignsRequest")
      .get(if(!appConf.getString("simulationConf.campaignPrefix").isEmpty) {
        appConf.getString("urls.search") + appConf.getString("simulationConf.campaignPrefix")
      }
      else appConf.getString("urls.campaigns")).check(regex(""".*""").saveAs("campaignIDs")))
    .exec { session =>
      val campaignListRaw = parse(session.attributes("campaignIDs").toString()).extract[List[JObject]]
      val campaignList = campaignListRaw.foldLeft(List[JObject]()) {(a, e) => if(mustHaveAds && e \ "ads" \ "total" == JInt(0)) {a} else e :: a}
      if(campaignList.isEmpty) throw new IllegalArgumentException("You do not have the proper campaigns added to run this simulation!")
      val JString(randCampaignID) = getRandElem(campaignList) \ "id"
      session.set("randCampaignID", randCampaignID)
    }
  }

  def setCreateCampaignAttributes: ChainBuilder = {
    exec(http("getAllAgencies")
      .get(appConf.getString("urls.agencies")).check(regex(""".*""").saveAs("agencies")))
      .exec(http("getAllAdvertisers")
        .get(appConf.getString("urls.advertisers")).check(regex(""".*""").saveAs("advertisers")))
    .exec { session =>
      val agenciesListRaw = parse(session.attributes("agencies").toString).extract[List[JObject]]
      val advertisersListRaw = parse(session.attributes("advertisers").toString).extract[List[JObject]]
      val JInt(randAgencyID) = getRandElem(agenciesListRaw) \ "id"
      val JInt(randAdvertiserID) = getRandElem(advertisersListRaw) \ "id"
      session.setAll(("randAgencyID", randAgencyID), ("randAdvertiserID", randAdvertiserID))
    }
    .exec(http("getAllBrands")
      .get(appConf.getString("urls.advertiserBrands")).check(regex(""".*""").saveAs("brands")))
    .exec { session =>
      val brandsListRaw = parse(session.attributes("brands").toString).extract[List[JObject]]
      val JString(randBrandID) = getRandElem(brandsListRaw) \ "id"
      session.set("randBrandID", randBrandID)
    }
  }

  def setUpdateAlertsAttributes(updateAlertsType: UpdateType.Value): ChainBuilder = {
    exec { session =>
      if(updateAlertsType == UpdateType.Add) {
        val randCampaign = parse(session.attributes("randCampaignDetails").toString()).extract[JObject]
        val ret = Serialization.write(randCampaign transformField {
          case JField("noImps", _) => ("noImps", JBool(false))
          case JField("noClicks", _) => ("noClicks", JBool(false))
          case JField("noConversions", _) => ("noConversions", JBool(false))
          case JField("statImps", _) => ("statImps", JString("normal"))
          case JField("statClicks", _) => ("statClicks", JString("normal"))
          case JField("statConversions", _) => ("statConversions", JString("normal"))
          case JField("pixels", _) => ("campaignGoalDeletes", JArray(List()))
        })
        session.set("addAlertsBody", ret)
      }
      else {
        val ret = Serialization.write(parse(session.attributes("randCampaignDetails").toString()).extract[JObject] transformField {
          case JField("noImps", _) => ("noImps", JBool(true))
          case JField("noClicks", _) => ("noClicks", JBool(true))
          case JField("noConversions", _) => ("noConversions", JBool(true))
          case JField("statImps", _) => ("statImps", JString("off"))
          case JField("statClicks", _) => ("statClicks", JString("off"))
          case JField("statConversions", _) => ("statConversions", JString("off"))
          case JField("pixels", _) => ("campaignGoalDeletes", JArray(List()))
        })
        session.set("remAlertsBody", ret)
      }
    }
  }

  def setGoalsAttributes(goalsSetupType: UpdateType.Value): ChainBuilder = {
    if(goalsSetupType == UpdateType.Add) {
      exec(setPropertiesPrelimAttributes, setPropertiesAttributes, setActionsPrelimAttributes, setActionsAttributes, createGoal, setUpdateGoalAttributes(UpdateType.Add))
    }
    else {
      setUpdateGoalAttributes(UpdateType.Remove)
    }
  }

  def setAdAttributes: ChainBuilder = {
    exec(setPlacementAttributes, setCreativeAttributes, setAddAdAttributes)
  }

  def setTagAttributes: ChainBuilder = {
    exec { session =>
      val parsedTagStub = parse(new FileReader("src/test/resources/GenerateTags/GenerateTagsHelperFile.txt")).extract[List[Map[String, Any]]]
      val placementID = session.attributes("placementID").toString
      session.set("tagBody", Serialization.write(List(parsedTagStub.head + ("placementID" -> placementID))))
    }
  }

  private def setAddAdAttributes: ChainBuilder = {
    val randName = Name.name

    exec { session =>
      val parsedAddAdStub = parse(new FileReader("src/test/resources/NewAds/NewAdsHelperFile.txt")).extract[List[JObject]]
      val placementID = session.attributes("placementID").toString
      val placementDbId = session.attributes("placementDbId").toString
      val creativeID = session.attributes("creativeID").toString
      val creativeDbId = session.attributes("creativeDbId").toString
      val ret = Serialization.write(JArray(List(parsedAddAdStub.head transformField {
        case JField("placement", _) => ("placement", JObject(List(JField("id", JString(placementID)), JField("dbId", JInt(placementDbId.toInt)))))
        case JField("creative", _) => ("creative", JObject(List(JField("id", JString(creativeID)), JField("dbId", JInt(creativeDbId.toInt)))))
        case JField("name", _) => ("name", JString(randName))
      })))
      session.set("addAdBody", ret)
    }
  }

  private def setPlacementAttributes: ChainBuilder = {
    createPlacement
    .exec(http("getRandCampaignPlacements").get(appConf.getString("urls.campaignPlacements"))
      .check(regex(""".*""").saveAs("getCampaignPlacements")))
    .exec { session =>
      val rawPlacements = parse(session.attributes("getCampaignPlacements").toString()).extract[List[JObject]]
      val lastPlacementAdded = rawPlacements.last
      val JString(placementID) = lastPlacementAdded \ "id"
      val JInt(placementDbId) = lastPlacementAdded \ "dbId"
      session.setAll(("placementID", placementID), ("placementDbId", placementDbId))
    }
  }

  private def setCreativeAttributes: ChainBuilder = {
    createCreative
    .exec(http("getRandCampaignCreatives").get(appConf.getString("urls.campaignCreatives"))
      .check(regex(""".*""").saveAs("getCampaignCreatives")))
    .exec { session =>
      val rawCreatives = parse(session.attributes("getCampaignCreatives").toString()).extract[List[JObject]]
      val lastCreativeAdded = rawCreatives.last
      val JString(creativeID) = lastCreativeAdded \ "id"
      val JInt(creativeDbId) = lastCreativeAdded \ "dbId"
      session.setAll(("creativeID", creativeID), ("creativeDbId", creativeDbId))
    }
  }

  private def createPlacement: ChainBuilder = {
    val randName = Name.name

    exec(http("placementRequest")
    .get(appConf.getString("urls.campaignPlacementsExternal"))
    .headers(loginHeader)
    .resources(http("switchRequest")
      .get(appConf.getString("urls.switchInstance")),
      http("campaignRequest")
        .get(appConf.getString("urls.campaign"))))
    .exec(http("propertyIDRequest")
      .get(appConf.getString("urls.createPlacement"))
      .headers(loginHeader)
      .resources(http("campaignRequest")
        .get(appConf.getString("urls.campaign")),
        http("propertyRequest")
          .get(appConf.getString("urls.properties"))
          .headers(instanceHeader),
        http("switchRequest")
          .get(appConf.getString("urls.switchInstance")),
        http("campaignRequest")
          .get(appConf.getString("urls.campaign"))))
    .exec { session =>
      val randName = Name.name
      val parsedCreativeStub = parse(new FileReader("src/test/resources/NewAds/NewPlacementHelperFile.txt")).extract[Map[String, Any]]
      println("\nCreated a new campaign placement! Information: Campaign ID - " + session.attributes("randCampaignID") + ", Name - " + randName + "\n")
      session.set("placementBody", Serialization.write(parsedCreativeStub + ("name" -> randName)))
    }
    .exec(http("addPlacementRequest")
      .post({val str = appConf.getString("urls.campaignPlacements"); str})
      .headers(contentHeader)
      .body(StringBody("${placementBody}"))
      .resources(http("campaignRequest")
        .get(appConf.getString("urls.campaign")),
        http("propertyRequest")
          .get(appConf.getString("urls.properties"))
          .headers(instanceHeader),
        http("switchRequest")
          .get(appConf.getString("urls.switchInstance")),
        http("campaignRequest")
          .get(appConf.getString("urls.campaign")),
        http("placementLimitRequest")
          .get(appConf.getString("urls.campaignPlacementsWithLimit"))))
  }

  private def createCreative: ChainBuilder = {
    val randName = Name.name

    exec(http("campaignRequest")
      .get(appConf.getString("urls.campaignCreativesUpload"))
      .headers(loginHeader)
      .resources(http("campaignRequest")
        .get(appConf.getString("urls.campaign")),
        http("switchRequest")
          .get(appConf.getString("urls.switchInstance"))))
    .exec(http("uploadRequest")
      .post({val str = appConf.getString("urls.internalCampaignCreativesUpload"); str})
      .headers(uploadHeader).check(regex(""".*""").saveAs("imageUri"))
      .body(RawFileBody("NewAds/ImageFile.txt")))
    .exec { session =>
      val parsedCreativeStub = parse(new FileReader("src/test/resources/NewAds/NewCreativeHelperFile.txt")).extract[List[Map[String, Any]]]
      val parsedUri = parse(session.attributes("imageUri").toString).extract[List[JObject]]
      val JString(uri) = parsedUri.head \ "uri"
      println("\nCreated a new campaign creative! Information: Campaign ID - " + session.attributes("randCampaignID") + ", Name - " + randName + "\n")
      session.set("creativeBody", Serialization.write(parsedCreativeStub.head + ("name" -> randName) + ("uri" -> uri)))
    }
    .exec(http("saveRequest")
      .post({val str = appConf.getString("urls.campaignCreatives"); str})
      .headers(contentHeader)
      .body(StringBody("${creativeBody}"))
      .resources(http("creativesRequest")
        .get(appConf.getString("urls.campaignCreatives")),
        http("campaignRequest")
          .get(appConf.getString("urls.campaign")),
        http("setupCreativesRequest")
          .get(appConf.getString("urls.campaignCreativeSetup1"))
          .headers(campaignHeader),
        http("setupCreativesRequest")
          .get(appConf.getString("urls.campaignCreativeSetup2"))
          .headers(campaignHeader)))
  }

  private def setUpdateGoalAttributes(updateGoalsType: UpdateType.Value): ChainBuilder = {
    exec(http("getRandCampaignGoals").get(if(updateGoalsType == UpdateType.Add) appConf.getString("urls.goals")
                                          else appConf.getString("urls.campaignGoals"))
      .check(regex(""".*""").saveAs("getRandCampaignGoals")))
    .exec { session =>
      val rawRandCampaignGoals = parse(session.attributes("getRandCampaignGoals").toString()).extract[List[JObject]]

      if(updateGoalsType == UpdateType.Add) {
        if(rawRandCampaignGoals.isEmpty) throw new IllegalArgumentException("You do not have the sufficient goals added to run this simulation!")
        val lastAddedGoal = rawRandCampaignGoals.last
        val lookback = lastAddedGoal \ "postbackLookback" \ "view"
        val campaignGoalToAdd = JObject(List(JField("brandGoalId", lastAddedGoal \ "id"), JField("lookbackClick", lookback), JField("lookbackView", lookback)))
        val randCampaign = parse(session.attributes("randCampaignDetails").toString()).extract[JObject]
        val ret = Serialization.write(randCampaign transformField {
          case JField("campaignGoals", _) => ("campaignGoals", JArray(List(campaignGoalToAdd)))
          case JField("pixels", _) => ("campaignGoalDeletes", JArray(List()))
        })
        session.set("addGoalBody", ret)
      }
      else {
        val ret = if(rawRandCampaignGoals.isEmpty) {
            Serialization.write(parse(session.attributes("randCampaignDetails").toString()).extract[JObject] transformField {
              case JField("campaignGoals", _) => ("campaignGoals", JArray(List()))
              case JField("pixels", _) => ("campaignGoalDeletes", JArray(List()))
            })
          }
          else {
            val lastAddedGoal = rawRandCampaignGoals.last
            Serialization.write(parse(session.attributes("randCampaignDetails").toString()).extract[JObject] transformField {
              case JField("campaignGoals", _) => ("campaignGoals", JArray(List()))
              case JField("pixels", _) => ("campaignGoalDeletes", JArray(List(lastAddedGoal \ "id")))
            })
          }
        session.set("remGoalBody", ret)
      }
    }
  }

  private def setActionsAttributes: ChainBuilder = {
    createAction().exec(setActionsPrelimAttributes)
  }

  private def setPropertiesAttributes: ChainBuilder = {
    doIf(session => session.attributes("noProperties").toString == "true") {
      createProperty().exec(setPropertiesPrelimAttributes)
    }
  }

  private def setPropertiesPrelimAttributes: ChainBuilder = {
    exec(http("getExistingProperties").get(appConf.getString("urls.brandProperties"))
         .check(regex(""".*""").saveAs("existingProperties")))
    .exec { session =>
      val propertiesList = parse(session.attributes("existingProperties").toString()).extract[List[JObject]]
      if (propertiesList.isEmpty) {
        session.set("noProperties", "true")
      }
      else {
        val randProperty = getRandElem(propertiesList)
        val JString(randPropertyID) = randProperty \ "id"
        session.setAll(("randPropertyID", randPropertyID), ("randProperty", Serialization.write(randProperty)), ("noProperties", "false"))
      }
    }
  }

  private def setActionsPrelimAttributes: ChainBuilder = {
    exec(http("getPropertyDetails").get(appConf.getString("urls.property")).check(regex(""".*""").saveAs("propertyDetails")))
    .exec { session =>
      val propertyDetails = parse(session.attributes("propertyDetails").toString()).extract[JObject]
      val JArray(actionsList) = propertyDetails \ "actions"
      if (actionsList.isEmpty) {
        session.set("noActions", "true")
      }
      else {
        val randAction = actionsList.last
        val JInt(randActionID) = randAction \ "id"
        session.setAll(("randActionID", randActionID), ("randAction", Serialization.write(randAction)), ("noActions", "false"))
      }
    }
  }

  private def genGoalStringBody(newName: String, randAction: String, randProperty: String, advertiserID: String, brandID: String): String = {
    val parsedGoalStub = parse(new FileReader("src/test/resources/NewAttribution/NewGoalHelperFile.txt")).extract[JObject]
    val parsedAction = parse(randAction).extract[JObject] merge JObject(List(JField("isConversion", JBool(true))))
    val wrappedAction = JArray(List(parsedAction))

    Serialization.write(parsedGoalStub transformField {
      case JField("name", _) => ("name", JString(newName))
      case JField("aid", _) => ("aid", JInt(advertiserID.toInt))
      case JField("bid", _) => ("bid", JString(brandID))
      case JField("advertiserProperty", _) => ("advertiserProperty", parse(randProperty).extract[JObject])
      case JField("actions", _) => ("actions", wrappedAction)
    })
  }

  private def createGoal: ChainBuilder = {
    val randName = Name.name

    exec(http("propertyRequest")
    .get(appConf.getString("urls.brandProperties")))
    .exec { session =>
      println("\nRandom goal created! Information: Name - " + randName + ", Advertiser ID - " +
        session.attributes("randAdvertiserID") + ", Brand ID - " + session.attributes("randBrandID") +
        ", Attached property ID - " +  session.attributes("randPropertyID") + ", Attached action ID - " +  session.attributes("randActionID") + "\n")
      session.set("goalBody", genGoalStringBody(randName, session.attributes("randAction").toString, session.attributes("randProperty").toString,
                                                session.attributes("randAdvertiserID").toString, session.attributes("randBrandID").toString))
    }
    .exec(http("goalRequest")
      .post({val str = appConf.getString("urls.goals"); str})
      .headers(sessionHeader)
      .body(StringBody("${goalBody}"))
      .resources(http("goalRequest")
      .get(appConf.getString("urls.goals"))))
  }

  private def createAction(): ChainBuilder = {
    val randName = Name.name

    exec(http("adsLimitRequest")
    .get(appConf.getString("urls.advertisers"))
    .resources(http("advertiserPropertyActionTypesRequest")
      .get(appConf.getString("urls.actionTypes"))))
    .exec(http("actionRequest")
      .post({val str = appConf.getString("urls.propertyActions"); str})
      .headers(sessionHeader)
      .body(StringBody(changeName(randName, "src/test/resources/NewAttribution/NewActionHelperFile.txt")))
      .resources(http("adsLimitRequest")
        .get(appConf.getString("urls.advertisers")),
        http("propertyRequest")
          .get(appConf.getString("urls.property")),
        http("advertiserPropertyActionTypesRequest")
          .get(appConf.getString("urls.actionTypes"))))
    .exec { session =>
      println("\nRandom action created! Information: Name - " + randName + ", Advertiser ID - " +
        session.attributes("randAdvertiserID") + ", Attached property ID - " +  session.attributes("randPropertyID") + "\n")
      session
    }
  }

  private def createProperty(): ChainBuilder = {
    val randName = Name.name

    exec(http("brandsRequest")
      .get(appConf.getString("urls.brands"))
      .headers(loginHeader)
      .resources(http("adsLimitRequest")
        .get(appConf.getString("urls.advertisers"))))
    .exec(http("adsLimitRequest")
    .get(appConf.getString("urls.advertisers")))
    .exec(http("propertiesRequest")
      .post({val str = appConf.getString("urls.advertiserProperty"); str})
      .headers(sessionHeader)
      .body(StringBody(changeName(randName, "src/test/resources/NewAttribution/NewPropertyHelperFile.txt")))
      .resources(http("advertisersLimitRequest")
        .get(appConf.getString("urls.advertisers"))))
    .exec { session =>
      println("\nRandom property created! Information: Name - " + randName + ", Advertiser ID - " + session.attributes("randAdvertiserID") + "\n")
      session
    }
  }

  private def getRandElem[T] (l: List[T]): T = {
    val rand = new Random(System.currentTimeMillis())
    l(rand.nextInt(l.length))
  }

  private def changeName(newName: String, fileName: String): String = {
    val parsedBody = parse(new FileReader(fileName)).extract[Map[String, Any]]
    Serialization.write(parsedBody + ("name" -> newName))
  }
}