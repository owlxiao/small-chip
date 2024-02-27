package smallchip

import chisel3._

import freechips.rocketchip.prci._
import org.chipsalliance.cde.config.{Field, Parameters}
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.devices.debug.{DebugModuleKey, ExportDebug, HasPeripheryDebug, TLDebugModule}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.apb._

/*
class SmallchipSubSystem(implicit p: Parameters) extends BaseSubsystem with HasTiles with HasTileInputConstants {
  val coreMonitorBundles = Nil
  lazy val debugOpt: Option[TLDebugModule] = None
  lazy val debugNode        = debugOpt.map(_.intnode).getOrElse(IntSyncXbar() := NullIntSyncSource())
  val resetVectorSourceNode = BundleBridgeSource[UInt]()
  tileResetVectorNode := resetVectorSourceNode

  override lazy val module = new SmallchipSubsystemModuleImp(this)

}

class SmallchipSubsystemModuleImp[+L <: SmallchipSubSystem](_outer: L)
    extends BaseSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasTilesModuleImp {}
 */

abstract class SmallSubSystem(implicit p: Parameters) extends LazyModule {
  val sbus = AXI4Xbar()
  val pbus = LazyModule(new APBFanout).node
}
