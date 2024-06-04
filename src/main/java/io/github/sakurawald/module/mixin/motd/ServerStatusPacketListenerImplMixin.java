/*
 * This file is part of MiniMOTD, licensed under the MIT License.
 *
 * Copyright (c) 2020-2023 Jason Penilla
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.sakurawald.module.mixin.motd;

import io.github.sakurawald.Fuji;
import io.github.sakurawald.module.ModuleManager;
import io.github.sakurawald.module.initializer.motd.MotdModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerQueryNetworkHandler;

@Mixin(ServerQueryNetworkHandler.class)
abstract class ServerStatusPacketListenerImplMixin {

    @Unique
    private static final MotdModule module = ModuleManager.getInitializer(MotdModule.class);

    
    @Redirect(method = "onRequest", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerQueryNetworkHandler;metadata:Lnet/minecraft/server/ServerMetadata;"))
    public ServerMetadata $handleStatusRequest(final ServerQueryNetworkHandler instance) {
        ServerMetadata vanillaStatus = Fuji.SERVER.getServerMetadata();
        if (vanillaStatus == null) {
            Fuji.LOGGER.warn("ServerStatus is null, use default.");
            return new ServerMetadata(module.getRandomDescription(), Optional.empty(), Optional.empty(), module.getRandomIcon(), false);
        }

        return new ServerMetadata(module.getRandomDescription(), vanillaStatus.comp_1274(), vanillaStatus.comp_1275(), module.getRandomIcon(), vanillaStatus.secureChatEnforced());
    }
}