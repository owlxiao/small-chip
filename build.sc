// import Mill dependency
import mill._
import mill.scalalib._
import mill.scalalib.scalafmt.ScalafmtModule

import $file.`generators`.`rocket-chip`.common
import $file.`generators`.`rocket-chip`.cde.common
import $file.`generators`.`rocket-chip`.hardfloat.build

import os.Path

val defaultScalaVersion = "2.13.10"

def defaultVersions(chiselVersion: String) = chiselVersion match {
  case "chisel" =>
    Map(
      "chisel" -> ivy"org.chipsalliance::chisel:6.0.0-M3",
      "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:6.0.0-M3",
      "chiseltest" -> ivy"edu.berkeley.cs::chiseltest:5.0.2"
    )
  case "chisel3" =>
    Map(
      "chisel" -> ivy"edu.berkeley.cs::chisel3:3.6.0",
      "chisel-plugin" -> ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0",
      "chiseltest" -> ivy"edu.berkeley.cs::chiseltest:0.6.2"
    )
}

trait HasChisel extends SbtModule with Cross.Module[String] {
  def chiselModule: Option[ScalaModule] = None

  def chiselPluginJar: T[Option[PathRef]] = None

  def chiselIvy: Option[Dep] = Some(defaultVersions(crossValue)("chisel"))

  def chiselPluginIvy: Option[Dep] = Some(defaultVersions(crossValue)("chisel-plugin"))

  override def scalaVersion = defaultScalaVersion

  override def scalacOptions = super.scalacOptions() ++
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")

  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get)

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
}

object rocketchip extends Cross[RocketChip]("chisel", "chisel3")

trait RocketChip extends millbuild.`generators`.`rocket-chip`.common.RocketChipModule with HasChisel {
  def scalaVersion: T[String] = T(defaultScalaVersion)

  override def millSourcePath = os.pwd / "generators" / "rocket-chip"

  def macrosModule = macros

  def hardfloatModule = hardfloat(crossValue)

  def cdeModule = cde

  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.4"

  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.6"

  object macros extends Macros

  trait Macros extends millbuild.`generators`.`rocket-chip`.common.MacrosModule with SbtModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends Cross[Hardfloat](crossValue)

  trait Hardfloat extends millbuild.`generators`.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = os.pwd / "generators" / "rocket-chip" / "hardfloat" / "hardfloat"

  }

  object cde extends CDE

  trait CDE extends millbuild.`generators`.`rocket-chip`.cde.common.CDEModule with ScalaModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = os.pwd / "generators" / "rocket-chip" / "cde" / "cde"
  }
}

object playground extends Cross[Playground]("chisel", "chisel3")
trait Playground extends SbtModule with ScalafmtModule with HasChisel {
  override def millSourcePath: Path = os.pwd

  override def sources = T.sources {
    super.sources() ++ Seq(
      PathRef(this.millSourcePath / "playground" / "src"),
      PathRef(this.millSourcePath / "generators" / "npc")
    )
  }

  override def moduleDeps = super.moduleDeps ++ Seq(
    rocketchip(crossValue)
  )
}
