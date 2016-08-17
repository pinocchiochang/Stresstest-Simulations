package com.medialets

import com.medialets.model.UsersType.Users
import com.medialets.model._
import com.medialets.parser.ConfParser
import com.medialets.sims._
import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, PopulationBuilder, ScenarioBuilder}
import io.gatling.http.Predef._

import scala.annotation.tailrec
import scala.concurrent.duration._

/**
  * Created by NoahKaplan on 6/17/16.
  */
class MainSimulation extends Simulation {
  case class VirtualUser(actions: ScenarioBuilder, numberToInject: Long, durationOfInjection: Long)

  val httpProtocol = http
    .baseURL(baseURL)
    .acceptHeader(acceptHeader)
    .acceptEncodingHeader(acceptEncodingHeader)
    .acceptLanguageHeader(acceptLanguageHeader)
    .userAgentHeader(userAgentHeader)
  val users:Users = ConfParser.run("userConfig.txt")

  private def getScenario(action: UserAction): ChainBuilder = {
    action match {
      case CampaignAction(ViewCampaign(count)) => CampaignSims.viewCampaign(count)
      case CampaignAction(AddCampaign(CampaignActionWAttr(count))) => CampaignSims.addCampaignWAttr(count)
      case CampaignAction(AddCampaign(CampaignActionWOAttr(count))) => CampaignSims.addCampaignWOAttr(count)
      case CampaignAction(UpdateCampaign(ChangeName(count))) => CampaignSims.changeCampaignName(count)
      case CampaignAction(UpdateCampaign(AddGoal(count))) => CampaignSims.addCampaignGoal(count)
      case CampaignAction(UpdateCampaign(RemoveGoal(count))) => CampaignSims.remCampaignGoal(count)
      case CampaignAction(UpdateCampaign(AddAlerts(count))) => CampaignSims.addAlerts(count)
      case CampaignAction(UpdateCampaign(RemoveAlerts(count))) => CampaignSims.remAlerts(count)
      case CampaignAction(UpdateCampaign(AddAd(AdWithTag(count)))) => CampaignSims.addAdWithTag(count)
      case CampaignAction(UpdateCampaign(AddAd(AdWithoutTag(count)))) => CampaignSims.addAdWithoutTag(count)
      case GenerateReportAction(count) => GenReportSims.genRandomReport(count)
    }
  }

  private def getVirtualUsers(users: Users): List[VirtualUser] = {
    def getVirtualUser(userConfig: UserConfig) : VirtualUser = {
      def getAction(userEntry: UserEntry): UserAction = {val UserEntry(_, ret) = userEntry; ret}
      @tailrec def addActions(acc: ScenarioBuilder, userEntries: List[UserEntry]): ScenarioBuilder = {
        if(userEntries.isEmpty) acc
        else addActions(acc.exec(getScenario(getAction(userEntries.head))), userEntries.tail)
      }

      val UserConfig(username, number, duration) = userConfig
      VirtualUser(addActions(scenario(username).exec(LoginSim.login), users(userConfig)), number, duration)
    }

    @tailrec def helper(acc: List[VirtualUser], userConfigs: List[UserConfig]): List[VirtualUser] = {
      if(userConfigs.isEmpty) acc
      else helper(getVirtualUser(userConfigs.head) :: acc, userConfigs.tail)
    }

    helper(List[VirtualUser](), users.keySet.toList)
  }

  @tailrec final def getPopulationBuilders(acc: List[PopulationBuilder], users: List[VirtualUser]): List[PopulationBuilder] = {
    if(users.isEmpty) acc
    else getPopulationBuilders(users.head.actions.inject(rampUsers(users.head.numberToInject.toInt) over
      (users.head.durationOfInjection seconds)) :: acc, users.tail)
  }

  setUp(getPopulationBuilders(List[PopulationBuilder](), getVirtualUsers(users))).protocols(httpProtocol)
}
