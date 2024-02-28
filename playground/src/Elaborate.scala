import smallchip._
import npc._
import circt.stage._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system.BaseConfig
import org.chipsalliance.cde.config._
import freechips.rocketchip.devices.tilelink.{BootROMLocated, CLINTKey, PLICKey}
import freechips.rocketchip.devices.debug.{DebugModuleKey}
import freechips.rocketchip.util._

import smallchip.device._

class DebugConfig
    extends Config(
      new Config((site, up, here) => {
        case DebugModuleKey => None
      })
    )
class WithNoCLINT
    extends Config((site, here, up) => {
      case CLINTKey => None
    })

class NpcConfig
    extends Config(
      new WithNNpcCores(1)
        ++ new WithCoherentBusTopology
        ++ new BaseConfig
        ++ new WithNExtTopInterrupts(9)
        ++ new WithScratchpadsOnly
        ++ new WithNBanks(0)
        ++ new WithNoMemPort
        ++ new DebugConfig
        ++ new WithNoCLINT
        ++ new WithUart
    )

object Elaborate extends App {
  val config    = new NpcConfig
  def top       = new TestHarness()(config)
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))

  ElaborationArtefacts.files.foreach {
    case (ext, contents) => os.write.over(os.pwd / "build" / s"system.${ext}", contents())
  }
}
