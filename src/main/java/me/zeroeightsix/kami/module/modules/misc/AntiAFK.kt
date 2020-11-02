package me.zeroeightsix.kami.module.modules.misc

import baritone.api.pathing.goals.GoalXZ
import me.zeroeightsix.kami.event.events.BaritoneSettingsInitEvent
import me.zeroeightsix.kami.event.events.PacketEvent
import me.zeroeightsix.kami.event.events.SafeTickEvent
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.ModuleConfig.setting
import me.zeroeightsix.kami.setting.Setting
import me.zeroeightsix.kami.util.BaritoneUtils
import me.zeroeightsix.kami.util.TimerUtils
import me.zeroeightsix.kami.util.event.listener
import me.zeroeightsix.kami.util.text.MessageDetectionHelper
import me.zeroeightsix.kami.util.text.MessageSendHelper.sendServerMessage
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.gameevent.InputEvent
import kotlin.math.max
import kotlin.random.Random

/**
 * TODO: Path finding to stay inside 1 chunk
 * TODO: Render which chunk is selected
 */
@Module.Info(
        name = "AntiAFK",
        category = Module.Category.MISC,
        description = "Prevents being kicked for AFK"
)
object AntiAFK : Module() {
    private val delay = setting("ActionDelay", 50, 0..100, 5)
    private val variation = setting("Variation", 25, 0..50, 5)
    private val autoReply = setting("AutoReply", true)
    private val swing = setting("Swing", true)
    private val jump = setting("Jump", true)
    private val turn = setting("Turn", true)
    private val walk = setting("Walk", true)
    private val radius = setting("Radius", 64, 8..128, 8)
    private val inputTimeout = setting("InputTimeout(m)", 0, 0..15, 1)

    private var startPos: BlockPos? = null
    private var nextActionTick = 0
    private var squareStep = 0
    private var baritoneDisconnectOnArrival = false
    private val inputTimer = TimerUtils.TickTimer(TimerUtils.TimeUnit.MINUTES)

    override fun getHudInfo(): String? {
        return ((System.currentTimeMillis() - inputTimer.time) / 1000L).toString()
    }

    override fun onEnable() {
        inputTimer.reset()
        baritoneDisconnectOnArrival()
    }

    override fun onDisable() {
        startPos = null
        BaritoneUtils.settings()?.disconnectOnArrival?.value = baritoneDisconnectOnArrival
        BaritoneUtils.cancelEverything()
    }

    init {
        listener<BaritoneSettingsInitEvent> {
            baritoneDisconnectOnArrival()
        }

        listener<PacketEvent.Receive> {
            if (!autoReply.value || it.packet !is SPacketChat) return@listener
            if (MessageDetectionHelper.isDirect(true, it.packet.getChatComponent().unformattedText)) {
                sendServerMessage("/r I am currently AFK and using KAMI Blue!")
            }
        }

        listener<InputEvent.MouseInputEvent> {
            if (inputTimeout.value != 0 && isInputting()) {
                inputTimer.reset()
            }
        }

        listener<InputEvent.KeyInputEvent> {
            if (inputTimeout.value != 0 && isInputting()) {
                inputTimer.reset()
            }
        }

        listener<SafeTickEvent> {
            if (inputTimeout.value != 0) {
                if (BaritoneUtils.isActive) inputTimer.reset()
                if (!inputTimer.tick(inputTimeout.value.toLong(), false)) {
                    startPos = null
                    return@listener
                }
            }

            if (mc.player.ticksExisted >= nextActionTick) {
                val random = if (variation.value > 0) (0..variation.value).random() else 0
                nextActionTick = mc.player.ticksExisted + max(delay.value, 1) + random

                when ((getAction())) {
                    Action.SWING -> mc.player.swingArm(EnumHand.MAIN_HAND)
                    Action.JUMP -> mc.player.jump()
                    Action.TURN -> mc.player.rotationYaw = Random.nextDouble(-180.0, 180.0).toFloat()
                }

                if (walk.value && !BaritoneUtils.isActive) {
                    if (startPos == null) startPos = mc.player.position
                    startPos?.let {
                        when (squareStep) {
                            0 -> baritoneGotoXZ(it.x, it.z + radius.value)
                            1 -> baritoneGotoXZ(it.x + radius.value, it.z + radius.value)
                            2 -> baritoneGotoXZ(it.x + radius.value, it.z)
                            3 -> baritoneGotoXZ(it.x, it.z)
                        }
                    }
                    squareStep = (squareStep + 1) % 4
                }
            }
        }
    }

    private fun baritoneDisconnectOnArrival() {
        BaritoneUtils.settings()?.disconnectOnArrival?.let {
            baritoneDisconnectOnArrival = it.value
            it.value = false
        }
    }

    private fun isInputting(): Boolean {
        return (mc.gameSettings.keyBindAttack.isKeyDown
                || mc.gameSettings.keyBindUseItem.isKeyDown
                || mc.gameSettings.keyBindJump.isKeyDown
                || mc.gameSettings.keyBindSneak.isKeyDown
                || mc.gameSettings.keyBindForward.isKeyDown
                || mc.gameSettings.keyBindBack.isKeyDown
                || mc.gameSettings.keyBindLeft.isKeyDown
                || mc.gameSettings.keyBindRight.isKeyDown)
    }

    private fun getAction(): Action? {
        if (!swing.value && !jump.value && !turn.value) return null
        val action = Action.values().random()
        return if (action.setting.value) action else getAction()
    }

    private enum class Action(val setting: Setting<Boolean>) {
        SWING(swing),
        JUMP(jump),
        TURN(turn)
    }

    private fun baritoneGotoXZ(x: Int, z: Int) {
        BaritoneUtils.primary?.customGoalProcess!!.setGoalAndPath(GoalXZ(x, z))
    }

    init {
        walk.listeners.add {
            BaritoneUtils.cancelEverything()
        }
    }
}