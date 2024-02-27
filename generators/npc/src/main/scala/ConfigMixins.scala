package npc

import chisel3._

import org.chipsalliance.cde.config.{Config, Field, Parameters}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._

class WithNNpcCores(n: Int = 1)
    extends Config((site, here, up) => {
      case TilesLocated(InSubsystem) => {
        val prev     = up(TilesLocated(InSubsystem), site)
        val idOffset = 0
        (0 until n).map { i =>
          NpcTileAttachParams(
            tileParams     = NpcTileParams(tileId = i + idOffset),
            crossingParams = RocketCrossingParams()
          )
        } ++ prev
      }
      case SystemBusKey => up(SystemBusKey, site).copy(beatBytes = 4)
      case XLen         => 32
    })
