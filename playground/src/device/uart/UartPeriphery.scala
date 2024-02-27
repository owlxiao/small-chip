package smallchip.device

import chisel3._
import freechips.rocketchip.diplomacy.{AddressSet, InModuleBody, LazyModule}
import org.chipsalliance.cde.config._
import smallchip._

trait HavePeripheralUart { this: SmallSubSystem =>

  val uart = p(PeripheryUARTKey).map { params =>
    val luart = LazyModule(new Uart(params))
    luart.node := pbus

    InModuleBody {
      val io = IO(chiselTypeOf(luart.module.uart))
      io <> luart.module.uart
      io
    }
  }
}

class WithUart
    extends Config((site, here, up) => {
      case PeripheryUARTKey => Some(UartParams(AddressSet.misaligned(0x10000000, 0x1000)))
    })
