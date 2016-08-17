package com.medialets

import java.io.{File, FilenameFilter}
import java.text.SimpleDateFormat
import java.util.Calendar

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.{DefaultFormats, _}
import org.json4s.jackson.Serialization

import scala.io.Source

/**
  * Created by NoahKaplan on 8/1/16.
  */
class LogErrors extends Simulation {
  val httpProtocol = http
    .baseURL(errorLoggingURL)
    .acceptHeader(acceptHeader)
    .acceptEncodingHeader(acceptEncodingHeader)
    .acceptLanguageHeader(acceptLanguageHeader)
    .userAgentHeader(userAgentHeader)
  val simulationNameNoCaps = "mainsimulation"
  val contentHeader = Map("Content-Type" -> "application/json")

  implicit val formats = DefaultFormats

  val scn = scenario("LogErrors")
    .exec({session =>
      def getLatestFileFromDir(dirPath: String): String = {
        val dir = new File(dirPath)
        val files = dir.listFiles(new FilenameFilter {
          override def accept(file: File, string: String) = string.contains(simulationNameNoCaps)
        })
        if (files == null || files.isEmpty) return null

        def loopHelper(acc: File, l: Array[File]): File = {
          if(l.isEmpty) acc
          else {
            if (acc.lastModified() < l.head.lastModified()) loopHelper(l.head, l.tail)
            else loopHelper(acc, l.tail)
          }
        }

        loopHelper(files(0), files).toString
      }
      val logFileDir = (getLatestFileFromDir("./target/gatling") + "/simulation.log").substring(2)

      def readFile(acc: JArray, lines: List[String]): JArray = {
        if(lines.isEmpty) acc
        else {
          val splitLine = lines.head.split("\t")
          if(splitLine(0) == "REQUEST" && splitLine(7) == "KO") {
            val requestName = splitLine(4)
            val format = new SimpleDateFormat("M-d-y")
            val date = format.format(Calendar.getInstance().getTime)
            val errorElem = JObject(List(JField("name", JString(requestName)), JField("date", JString(date))))
            readFile(JArray(acc.arr ++ List[JValue](errorElem)), lines.tail)
          }
          else readFile(acc, lines.tail)
        }
      }

      val fileJson = readFile(JArray(List[JValue]()), Source.fromFile(logFileDir).getLines().toList)
      val loggedErrors = Serialization.write(JObject(List(JField("key", JString(errorLoggingKey)), JField("content", fileJson))))
      session.set("errors", loggedErrors)
    })
    .exec(http("sendErrors")
      .post("/error")
      .headers(contentHeader)
      .body(StringBody("${errors}")))

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
