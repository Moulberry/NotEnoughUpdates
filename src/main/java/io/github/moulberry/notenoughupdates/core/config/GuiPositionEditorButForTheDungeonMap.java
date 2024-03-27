/*
 * Copyright (C) 2022-2023 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.core.config;

import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiPositionEditorButForTheDungeonMap extends GuiScreen { // nea is gonna LOVE this file's existence
	private final Position position;
	private final Position originalPosition;
	private final int elementWidth;
	private final int elementHeight;
	private final Runnable renderCallback;
	private final Runnable positionChangedCallback;
	private final Runnable closedCallback;
	private boolean clicked = false;
	private int grabbedX = 0;
	private int grabbedY = 0;

	private int guiScaleOverride = -1;

	public GuiPositionEditorButForTheDungeonMap(
		Position position, int elementWidth, int elementHeight,
		Runnable renderCallback,
		Runnable positionChangedCallback,
		Runnable closedCallback
	) {
		this.position = position;
		this.originalPosition = position.clone();
		this.elementWidth = elementWidth;
		this.elementHeight = elementHeight;
		this.renderCallback = renderCallback;
		this.positionChangedCallback = positionChangedCallback;
		this.closedCallback = closedCallback;
	}

	public GuiPositionEditorButForTheDungeonMap withScale(int scale) {
		this.guiScaleOverride = scale;
		return this;
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		closedCallback.run();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		ScaledResolution scaledResolution;
		if (guiScaleOverride >= 0) {
			scaledResolution = Utils.pushGuiScale(guiScaleOverride);
		} else {
			scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		}

		this.width = scaledResolution.getScaledWidth();
		this.height = scaledResolution.getScaledHeight();
		mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
		mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

		drawDefaultBackground();

		if (clicked) {
			grabbedX += position.moveX(mouseX - grabbedX, elementWidth, scaledResolution);
			grabbedY += position.moveY(mouseY - grabbedY, elementHeight, scaledResolution);
		}

		renderCallback.run();

		int x = position.getAbsX(scaledResolution, elementWidth);
		int y = position.getAbsY(scaledResolution, elementHeight);

		if (position.isCenterX()) x -= elementWidth / 2;
		if (position.isCenterY()) y -= elementHeight / 2;
		Gui.drawRect(x, y, x + elementWidth, y + elementHeight, 0x80404040);

		if (guiScaleOverride >= 0) {
			Utils.pushGuiScale(-1);
		}

		scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		Utils.drawStringCentered("Position Editor", scaledResolution.getScaledWidth() / 2, 8, true, 0xffffff);
		Utils.drawStringCentered(
			"R to Reset - Arrow keys/mouse to move",
			scaledResolution.getScaledWidth() / 2, 18, true, 0xffffff);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		if (mouseButton == 0) {
			ScaledResolution scaledResolution;
			if (guiScaleOverride >= 0) {
				scaledResolution = Utils.pushGuiScale(guiScaleOverride);
			} else {
				scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
			}
			mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
			mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

			int x = position.getAbsX(scaledResolution, elementWidth);
			int y = position.getAbsY(scaledResolution, elementHeight);
			if (position.isCenterX()) x -= elementWidth / 2;
			if (position.isCenterY()) y -= elementHeight / 2;

			if (mouseX >= x && mouseY >= y &&
				mouseX <= x + elementWidth && mouseY <= y + elementHeight) {
				clicked = true;
				grabbedX = mouseX;
				grabbedY = mouseY;
			}

			if (guiScaleOverride >= 0) {
				Utils.pushGuiScale(-1);
			}
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_R) {
			position.set(originalPosition);
		} else if (!clicked) {
			boolean shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
			int dist = shiftHeld ? 10 : 1;
			if (keyCode == Keyboard.KEY_DOWN) {
				position.moveY(dist, elementHeight, new ScaledResolution(Minecraft.getMinecraft()));
			} else if (keyCode == Keyboard.KEY_UP) {
				position.moveY(-dist, elementHeight, new ScaledResolution(Minecraft.getMinecraft()));
			} else if (keyCode == Keyboard.KEY_LEFT) {
				position.moveX(-dist, elementWidth, new ScaledResolution(Minecraft.getMinecraft()));
			} else if (keyCode == Keyboard.KEY_RIGHT) {
				position.moveX(dist, elementWidth, new ScaledResolution(Minecraft.getMinecraft()));
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		clicked = false;
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

		if (clicked) {
			ScaledResolution scaledResolution;
			if (guiScaleOverride >= 0) {
				scaledResolution = Utils.pushGuiScale(guiScaleOverride);
			} else {
				scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
			}
			mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
			mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

			grabbedX += position.moveX(mouseX - grabbedX, elementWidth, scaledResolution);
			grabbedY += position.moveY(mouseY - grabbedY, elementHeight, scaledResolution);
			positionChangedCallback.run();

			if (guiScaleOverride >= 0) {
				Utils.pushGuiScale(-1);
			}
		}
	}
}
