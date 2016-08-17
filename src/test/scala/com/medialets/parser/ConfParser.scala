package com.medialets.parser

import com.medialets.model._
import com.medialets.model.UsersType.Users
import java.io.FileReader
import scala.util.parsing.combinator.JavaTokenParsers

/**
  * Created by NoahKaplan on 6/23/16.
  */
object ConfParser extends JavaTokenParsers {
  protected override val whiteSpace = """(?s)(?:\s|/\*.*?\*/)+""".r
  val dot = "."
  val username = """[a-zA-Z0-9]*""".r

  val count: Parser[Long] = "count" ~ "=" ~ wholeNumber ^^ {case _ ~ num => num.toLong}
  val campaignAttrConf: Parser[AttributionConf] =
    "withAttribution" ~ dot ~ count ^^ {case _ ~ c => CampaignActionWAttr(c)} |
    "withoutAttribution" ~ dot ~ count ^^ {case _ ~ c => CampaignActionWOAttr(c)}
  val adTagConf: Parser[TagConf] =
    "withTag" ~ dot ~ count ^^ {case _ ~ c => AdWithTag(c)} |
    "withoutTag" ~ dot ~ count ^^ {case _ ~ c => AdWithoutTag(c)}
  val updateCampaignConf: Parser[UpdateCampaignConf] =
    "addGoal" ~ dot ~ count ^^ {case _ ~ c => AddGoal(c)} |
    "removeGoal" ~ dot ~ count ^^ {case _ ~ c => RemoveGoal(c)} |
    "changeName" ~ dot ~ count ^^ {case _ ~ c => ChangeName(c)} |
    "addAlerts" ~ dot ~ count ^^ {case _ ~ c => AddAlerts(c)} |
    "removeAlerts" ~ dot ~ count ^^ {case _ ~ c => RemoveAlerts(c)} |
    "addAd" ~ dot ~ adTagConf ^^ {case _ ~ tConf => AddAd(tConf)}
  val campaignActionType: Parser[CampaignActionType] =
    "add" ~ dot ~ campaignAttrConf ^^ {case _ ~ aConf => AddCampaign(aConf)} |
    "view" ~ dot ~ count ^^ {case _ ~ c => ViewCampaign(c)} |
    "update" ~ dot ~ updateCampaignConf ^^ {case _ ~ uConf => UpdateCampaign(uConf)}
  val campaignAction: Parser[CampaignAction] = "campaignAction" ~ dot ~ campaignActionType ^^ {case _ ~ attrConf => CampaignAction(attrConf)}
  val generateReportAction: Parser[GenerateReportAction] = "generateReportAction" ~ dot ~ count ^^ {case _ ~ c => GenerateReportAction(c)}
  val userAction: Parser[UserAction] = campaignAction | generateReportAction
  val userConfig: Parser[UserConfig] =
    username ~ "(" ~ wholeNumber ~ "," ~ wholeNumber ~ ")" ^^ {case u ~ _ ~ num ~ _ ~ dur ~ _ => UserConfig(u, num.toLong, dur.toLong)}
  val userEntry: Parser[UserEntry] = userConfig ~ dot ~ userAction ^^ {case config ~ _ ~ action => UserEntry(config, action)}
  val users: Parser[Users] = rep(userEntry) ^^ {l => l.groupBy(_.config)}

  def run(filename: String) = parseAll(users, new FileReader(filename)) match {
    case Success(result, _) => result
    case failure: NoSuccess => scala.sys.error(failure.msg)
  }
}