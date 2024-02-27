package smallchip

import chisel3._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._
import freechips.rocketchip.util._
import freechips.rocketchip.regmapper._

import npc._
import smallchip.device._
import smallchip.bus._

trait HavePeripheralPort { this: SmallSubSystem =>
  pbus := AXI4ToAPB() := sbus
}

trait HaveCore { this: SmallSubSystem =>
  val cpu = LazyModule(new Core(4))
  sbus := cpu.masterNode
}

class SmallSystem(implicit p: Parameters)
    extends SmallSubSystem
    with HavePeripheralUart
    with HavePeripheralPort
    with HaveCore {
  ElaborationArtefacts.add("graphml", graphML)

  override lazy val module = new Impl
  class Impl extends LazyModuleImp(this) with DontTouch {}
}
