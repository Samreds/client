package me.zeroeightsix.kami.module.modules.misc

import me.zeroeightsix.kami.command.Command
import me.zeroeightsix.kami.event.events.ConnectionEvent
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.ModuleConfig.setting
import me.zeroeightsix.kami.util.BaritoneUtils
import me.zeroeightsix.kami.util.event.listener
import me.zeroeightsix.kami.util.text.MessageSendHelper

@Module.Info(
        name = "AutoMine",
        description = "Automatically mines chosen ores",
        category = Module.Category.MISC
)
object AutoMine : Module() {
    private val iron = setting("Iron", true)
    private val diamond = setting("Diamond", true)
    private val gold = setting("Gold", false)
    private val coal = setting("Coal", false)
    private val log = setting("Logs", false)

    override fun onEnable() {
        if (mc.player == null) {
            disable()
            return
        }
        run()
    }

    private fun run() {
        if (mc.player == null || isDisabled) return
        var current = ""
        if (iron.value) current += " iron_ore"
        if (diamond.value) current += " diamond_ore"
        if (gold.value) current += " gold_ore"
        if (coal.value) current += " coal_ore"
        if (log.value) current += " log log2"

        if (current.startsWith(" ")) {
            current = current.substring(1)
        }
        val total = current.split(" ")

        if (current.length < 2) {
            MessageSendHelper.sendBaritoneMessage("Error: you have to choose at least one thing to mine. To mine custom blocks run the &7" + Command.getCommandPrefix() + "b mine block&f command")
            BaritoneUtils.cancelEverything()
            return
        }

        MessageSendHelper.sendBaritoneCommand("mine", *total.toTypedArray())
    }

    override fun onDisable() {
        BaritoneUtils.cancelEverything()
    }

    init {
        with( { run() }) {
            iron.listeners.add(this)
            diamond.listeners.add(this)
            gold.listeners.add(this)
            coal.listeners.add(this)
            log.listeners.add(this)
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }
    }
}
