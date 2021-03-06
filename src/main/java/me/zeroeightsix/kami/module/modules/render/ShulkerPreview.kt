package me.zeroeightsix.kami.module.modules.render

import me.zeroeightsix.kami.mixin.client.gui.MixinGuiScreen
import me.zeroeightsix.kami.module.Module

/**
 * @see MixinGuiScreen.renderToolTip
 */
@Module.Info(
        name = "ShulkerPreview",
        category = Module.Category.RENDER,
        description = "Previews shulkers in the game GUI"
)
object ShulkerPreview : Module()