package com.medialets.model

/**
  * Created by NoahKaplan on 6/24/16.
  */

trait TagConf
trait UpdateCampaignConf
trait AttributionConf
trait CampaignActionType
trait UserAction

case class AdWithTag(count: Long) extends TagConf
case class AdWithoutTag(count: Long) extends TagConf

case class ChangeName(count: Long) extends UpdateCampaignConf
case class AddGoal(count: Long) extends UpdateCampaignConf
case class RemoveGoal(count: Long) extends UpdateCampaignConf
case class AddAlerts(count: Long) extends UpdateCampaignConf
case class RemoveAlerts(count: Long) extends UpdateCampaignConf
case class AddAd(tagConf: TagConf) extends UpdateCampaignConf

case class CampaignActionWAttr(count: Long) extends AttributionConf
case class CampaignActionWOAttr(count: Long) extends AttributionConf

case class GenerateTag(count: Long) extends CampaignActionType
case class ViewCampaign(count: Long) extends CampaignActionType
case class UpdateCampaign(action: UpdateCampaignConf) extends CampaignActionType
case class AddCampaign(adConf: AttributionConf) extends CampaignActionType

case class GenerateReportAction(count: Long) extends UserAction
case class CampaignAction(action: CampaignActionType) extends UserAction

case class UserConfig(username: String, number: Long, duration: Long)
case class UserEntry(config: UserConfig, action: UserAction)

object UsersType {
  type Users = Map[UserConfig, List[UserEntry]]
}