/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.castMap
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.unaryPlus
import com.google.common.collect.Maps
import net.kyori.text.BuildableComponent
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder
import net.kyori.text.TextComponent.space
import net.kyori.text.adapter.bukkit.TextAdapter
import net.kyori.text.event.ClickEvent
import net.kyori.text.format.TextColor
import net.kyori.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale
import java.util.Optional

fun Iterable<CommandSender>.sendMessage(text: Component) = TextAdapter.sendComponent(this, text)
fun Iterable<CommandSender>.sendActionBar(text: Component) = TextAdapter.sendActionBar(this, text)

fun CommandSender.sendMessage(text: Component) = TextAdapter.sendComponent(this, text)
fun CommandSender.sendActionBar(text: Component) = TextAdapter.sendActionBar(this, text)

/**
 * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
 * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
 *
 * @param mcLocaleString The locale string, in the format provided by the Minecraft client
 * @return A Locale object matching the provided locale string
 */
fun String.toLocale(): Locale {
    val parts = this.split("_", limit = 3)
    return when (parts.size) {
        0 -> Locale.getDefault();
        1 -> Locale(parts[0]);
        2 -> Locale(parts[0], parts[1]);
        3 -> Locale(parts[0], parts[1], parts[2]);
        else -> throw IllegalArgumentException("Provided locale '$this' was not in a valid format!");
    }
}

class BukkitMessageFormatter(val pex: PermissionsExPlugin,
    bCmd: BukkitCommander): MessageFormatter(bCmd, pex.manager) {

    override val Map.Entry<String, String>.friendlyName: String?
        get() = pex.manager.getSubjects(key).typeInfo.getAssociatedObject(value).castMap<CommandSender, String> { name }

    override fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.callback(callback: (Commander) -> Unit): B {
        val command = pex.callbackController.registerCallback(source = cmd, func = {callback(it)})
        decoration(TextDecoration.UNDERLINED, true)
        color(hlColor)
        clickEvent(ClickEvent.runCommand(transformCommand(command)))
        return this
    }
}

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
class BukkitCommander internal constructor(
    pex: PermissionsExPlugin,
    private val commandSource: CommandSender
) : Commander {
    override val formatter: BukkitMessageFormatter = BukkitMessageFormatter(pex, this)
    override val name: String
        get() = commandSource.name

    override fun hasPermission(permission: String): Boolean = commandSource.hasPermission(permission)

    override val locale: Locale
        get() = if (commandSource is Player) commandSource.locale.toLocale() else Locale.getDefault()

    override val subjectIdentifier: Optional<Map.Entry<String, String>>
        get() = if (commandSource is Player) {
            Optional.of(
                Maps.immutableEntry(
                    PermissionsEx.SUBJECTS_USER,
                    commandSource.uniqueId.toString()
                )
            )
        } else Optional.empty()

    private fun sendMessageInternal(formatted: Component) {
        commandSource.sendMessage(PEXComponentRenderer.render(formatted, locale))
    }

    override fun msg(text: Component) {
        sendMessageInternal(text coloredIfNecessary TextColor.DARK_AQUA)
    }


    override fun debug(text: Component) {
        sendMessageInternal(text coloredIfNecessary TextColor.GRAY)
    }

    override fun error(text: Component, err: Throwable?) {
        sendMessageInternal(text coloredIfNecessary TextColor.RED)
    }

    override fun msgPaginated(
        title: Component,
        header: Component?,
        text: Iterable<Component>
    ) {
        msg { send ->
            val marker = +"#"
            send(listOf(marker, title, marker).join(space()))
            if (header != null) {
                send(header)
            }
            text.forEach(send)
            send(+"#############################")
        }
    }


}