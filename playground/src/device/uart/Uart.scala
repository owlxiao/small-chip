package smallchip.device

import chisel3._
import freechips.rocketchip.amba.apb._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._

class UARTIO extends Bundle {
  val rx = Input(Bool())
  val tx = Output(Bool())
}

class uart_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in    = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val uart  = new UARTIO
  })
}

case class UartParams(address: Seq[AddressSet])
case object PeripheryUARTKey extends Field[Option[UartParams]](None)

class Uart(params: UartParams)(implicit p: Parameters) extends LazyModule {
  val node = APBSlaveNode(
    Seq(
      APBSlavePortParameters(
        Seq(
          APBSlaveParameters(address = params.address, executable = false, supportsRead = true, supportsWrite = true)
        ),
        beatBytes = 4
      )
    )
  )

  private val outer = this

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val uart    = IO(new UARTIO)

    val muart = Module(new uart_top_apb)
    muart.io.clock := clock
    muart.io.reset := reset
    muart.io.in <> in
    uart <> muart.io.uart
  }
}
