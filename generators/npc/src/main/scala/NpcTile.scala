package npc

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, RawParam, StringParam}

import org.chipsalliance.cde.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.subsystem.{RocketCrossingParams}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem.TileCrossingParamsLike
import freechips.rocketchip.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.prci.ClockSinkParameters

case class NpcCoreParams(
  val bootFreqHz:       BigInt = BigInt(1700000000),
  val pmpEnable:        Int    = 0,
  val pmpGranularity:   Int    = 0,
  val pmpNumRegions:    Int    = 4,
  val mhpmCounterNum:   Int    = 0,
  val mhpmCounterWidth: Int    = 0,
  val rv32e:            Int    = 0,
  val rv32m:            String = "RV32MFast",
  val rv32b:            String = "RV32BNone",
  val regFile:          String = "RegFileFF",
  val branchTargetALU:  Int    = 0,
  val wbStage:          Int    = 0,
  val branchPredictor:  Int    = 0,
  val dbgHwBreakNum:    Int    = 1,
  val dmHaltAddr:       Int    = 0x1a110800,
  val dmExceptionAddr:  Int    = 0x1a110808)
    extends CoreParams {
  val useVM:               Boolean              = false
  val useHypervisor:       Boolean              = false
  val useUser:             Boolean              = true
  val useSupervisor:       Boolean              = false
  val useDebug:            Boolean              = false
  val useAtomics:          Boolean              = false
  val useAtomicsOnlyForIO: Boolean              = false
  val useCompressed:       Boolean              = false
  override val useVector:  Boolean              = false
  val useSCIE:             Boolean              = false
  val useRVE:              Boolean              = true
  val mulDiv:              Option[MulDivParams] = Some(MulDivParams()) // copied from Rocket
  val fpu:                 Option[FPUParams]    = None //floating point not supported
  val fetchWidth:          Int                  = 1
  val decodeWidth:         Int                  = 1
  val retireWidth:         Int                  = 2
  val instBits:            Int                  = if (useCompressed) 16 else 32
  val nLocalInterrupts:    Int                  = 15
  val nPMPs:               Int                  = 0
  val nBreakpoints:        Int                  = 0
  val useBPWatch:          Boolean              = false
  val nPerfCounters:       Int                  = 29
  val haveBasicCounters:   Boolean              = true
  val haveFSDirty:         Boolean              = false
  val misaWritable:        Boolean              = false
  val haveCFlush:          Boolean              = false
  val nL2TLBEntries:       Int                  = 0
  val mtvecInit:           Option[BigInt]       = Some(BigInt(0))
  val mtvecWritable:       Boolean              = true
  val nL2TLBWays:          Int                  = 0
  val lrscCycles:          Int                  = 80
  val mcontextWidth:       Int                  = 0
  val scontextWidth:       Int                  = 0
  val useNMI:              Boolean              = false
  val nPTECacheEntries:    Int                  = 0
  val useBitManip:         Boolean              = false
  val useBitManipCrypto:   Boolean              = false
  val useCryptoNIST:       Boolean              = false
  val useCryptoSM:         Boolean              = false
  val traceHasWdata:       Boolean              = false
  val useConditionalZero:  Boolean              = false
}

case class NpcTileAttachParams(
  tileParams:     NpcTileParams,
  crossingParams: RocketCrossingParams)
    extends CanAttachTile {
  type TileType = NpcTile
}

case class NpcTileParams(
  name:     Option[String] = Some("npc_tile"),
  tileId:   Int            = 0,
  hartId:   Int            = 0,
  val core: NpcCoreParams  = NpcCoreParams())
    extends InstantiableTileParams[NpcTile] {
  val beuAddr:         Option[BigInt]       = None
  val blockerCtrlAddr: Option[BigInt]       = None
  val btb:             Option[BTBParams]    = None
  val boundaryBuffers: Boolean              = false
  val dcache:          Option[DCacheParams] = None //no dcache
  val icache:          Option[ICacheParams] = None //optional icache, currently in draft so turning option off
  val clockSinkParams: ClockSinkParameters  = ClockSinkParameters()
  def instantiate(
    crossing: TileCrossingParamsLike,
    lookup:   LookupByHartIdImpl
  )(
    implicit p: Parameters
  ): NpcTile = {
    new NpcTile(this, crossing, lookup)
  }
  val baseName   = name.getOrElse("ibex_tile")
  val uniqueName = s"${baseName}_$tileId"
}

class NpcTile(
  val npcParams: NpcTileParams,
  crossing:      ClockCrossingType,
  lookup:        LookupByHartIdImpl,
  q:             Parameters)
    extends BaseTile(npcParams, crossing, lookup, q)
    with SinksExternalInterrupts
    with SourcesExternalNotifications {
  // Private constructor ensures altered LazyModule.p is used implicitly
  def this(
    params:   NpcTileParams,
    crossing: TileCrossingParamsLike,
    lookup:   LookupByHartIdImpl
  )(
    implicit p: Parameters
  ) =
    this(params, crossing.crossingType, lookup, p)

  // Require TileLink nodes
  val intOutwardNode = IntIdentityNode()
  val masterNode     = visibilityNode
  val slaveNode      = TLIdentityNode()

  // masterNode := tlMasterXbar.node
  tlOtherMastersNode := tlMasterXbar.node
  masterNode :=* tlOtherMastersNode
  DisableMonitors { implicit p => tlSlaveXbar.node :*= slaveNode }

  // Implementation class (See below)
  override lazy val module = new NpcTileModuleImp(this)

  // Required entry of CPU device in the device tree for interrupt purpose
  val cpuDevice: SimpleDevice = new SimpleDevice("cpu", Seq("ysyx,npc", "riscv")) {
    override def parent = Some(ResourceAnchors.cpus)
    override def describe(resources: ResourceBindings): Description = {
      val Description(name, mapping) = super.describe(resources)
      Description(
        name,
        mapping ++
          cpuProperties ++
          nextLevelCacheProperty ++
          tileProperties
      )
    }
  }

  ResourceBinding {
    Resource(cpuDevice, "reg").bind(ResourceAddress(staticIdForMetadataUseOnly))
  }

  // # of bits used in TileLink ID for master node. 4 bits can support 16 master nodes, but you can have a longer ID if you need more.
  val idBits = 4
  val memAXI4Node = AXI4MasterNode(
    Seq(
      AXI4MasterPortParameters(masters =
        Seq(AXI4MasterParameters(name = "npc-mem-port-axi4", id = IdRange(0, 1 << idBits)))
      )
    )
  )
  val memoryTap = TLIdentityNode() // Every bus connection should have their own tap node

  (tlMasterXbar.node // tlMasterXbar is the bus crossbar to be used when this core / tile is acting as a master; otherwise, use tlSlaveXBar
    := memoryTap
    := TLBuffer()
    := TLFIFOFixer(TLFIFOFixer.all) // fix FIFO ordering
    := TLWidthWidget(masterPortBeatBytes) // reduce size of TL
    := AXI4ToTL() // convert to TL
    := AXI4UserYanker(Some(2)) // remove user field on AXI interface. need but in reality user intf. not needed
    := AXI4Fragmenter() // deal with multi-beat xacts
    := memAXI4Node)

  def connectInterrupts(debug: Bool, msip: Bool, mtip: Bool, m_s_eip: UInt): Unit = {
    val (interrupts, _) = intSinkNode.in(0)
    debug   := interrupts(0)
    msip    := interrupts(1)
    mtip    := interrupts(2)
    m_s_eip := Cat(interrupts(4), interrupts(3))
  }
}

class NpcTileModuleImp(outer: NpcTile) extends BaseTileModuleImp(outer) {
  // annotate the parameters
  Annotated.params(this, outer.npcParams)

  val core = Module(new npc)

  outer.memAXI4Node.out.foreach {
    case (out, edgeOut) =>
      core.io.io_master <> out
  }

  core.io.io_interrupt := DontCare
  core.io.io_slave     := DontCare
  core.io.clock        := clock
  core.io.reset        := reset
}
