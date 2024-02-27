package npc

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.subsystem._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._

object CPUAXI4BundleParameters {
  def apply() = AXI4BundleParameters(addrBits = 32, dataBits = 64, idBits = 4)
}

class npc extends BlackBox {
  val io = IO(new Bundle {
    val clock        = Input(Clock())
    val reset        = Input(Reset())
    val io_interrupt = Input(Bool())
    val io_master    = AXI4Bundle(CPUAXI4BundleParameters())
    val io_slave     = Flipped(AXI4Bundle(CPUAXI4BundleParameters()))
  })
}

class Core(idBits: Int)(implicit p: Parameters) extends LazyModule {
  val masterNode = AXI4MasterNode(
    p(ExtIn)
      .map(params =>
        AXI4MasterPortParameters(masters = Seq(AXI4MasterParameters(name = "cpu", id = IdRange(0, 1 << idBits))))
      )
      .toSeq
  )
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (master, _) = masterNode.out(0)
    // val interrupt   = IO(Input(Bool()))
    // val slave       = IO(Flipped(AXI4Bundle(CPUAXI4BundleParameters())))

    val cpu = Module(new npc)
    cpu.io.clock        := clock
    cpu.io.reset        := reset
    cpu.io.io_interrupt := DontCare
    cpu.io.io_slave <> DontCare
    master <> cpu.io.io_master
  }
}
