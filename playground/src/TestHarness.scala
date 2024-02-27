package smallchip

import chisel3._

import org.chipsalliance.cde.config.{Field, Parameters}
import freechips.rocketchip.diplomacy.LazyModule

class TestHarness(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {})

  val ldut = LazyModule(new SmallSystem)
  val dut  = Module(ldut.module)

  dut.dontTouchPorts()
  ldut.uart.get := DontCare
}
