/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
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

package io.github.moulberry.notenoughupdates.core;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.util.Matrix4f;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BackgroundBlur {
	private static class OutputStuff {
		public Framebuffer framebuffer;
		public Shader blurShaderHorz = null;
		public Shader blurShaderVert = null;

		public OutputStuff(Framebuffer framebuffer, Shader blurShaderHorz, Shader blurShaderVert) {
			this.framebuffer = framebuffer;
			this.blurShaderHorz = blurShaderHorz;
			this.blurShaderVert = blurShaderVert;
		}
	}

	private static final HashMap<Float, OutputStuff> blurOutput = new HashMap<>();
	private static final HashMap<Float, Long> lastBlurUse = new HashMap<>();
	private static long lastBlur = 0;
	private static final HashSet<Float> requestedBlurs = new HashSet<>();

	private static int fogColour = 0;
	private static boolean registered = false;

	public static void registerListener() {
		if (!registered) {
			registered = true;
			MinecraftForge.EVENT_BUS.register(new BackgroundBlur());
		}
	}

	private static boolean shouldBlur = true;

	public static void markDirty() {
		if (Minecraft.getMinecraft().theWorld != null) {
			shouldBlur = true;
		}
	}

	public static void processBlurs() {
		if (shouldBlur) {
			shouldBlur = false;

			long currentTime = System.currentTimeMillis();

			for (float blur : requestedBlurs) {
				lastBlur = currentTime;
				lastBlurUse.put(blur, currentTime);

				int width = Minecraft.getMinecraft().displayWidth;
				int height = Minecraft.getMinecraft().displayHeight;

				OutputStuff output = blurOutput.computeIfAbsent(blur, k -> {
					Framebuffer fb = new Framebuffer(width, height, false);
					fb.setFramebufferFilter(GL11.GL_NEAREST);
					return new OutputStuff(fb, null, null);
				});

				if (output.framebuffer.framebufferWidth != width || output.framebuffer.framebufferHeight != height) {
					output.framebuffer.createBindFramebuffer(width, height);
					if (output.blurShaderHorz != null) {
						output.blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
					}
					if (output.blurShaderVert != null) {
						output.blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
					}
				}

				blurBackground(output, blur);
			}

			Set<Float> remove = new HashSet<>();
			for (Map.Entry<Float, Long> entry : lastBlurUse.entrySet()) {
				if (currentTime - entry.getValue() > 30 * 1000) {
					remove.add(entry.getKey());
				}
			}
			remove.remove((float) NotEnoughUpdates.INSTANCE.config.itemlist.bgBlurFactor);

			for (Map.Entry<Float, OutputStuff> entry : blurOutput.entrySet()) {
				if (remove.contains(entry.getKey())) {
					entry.getValue().framebuffer.deleteFramebuffer();
					Shader blurShaderHorz = entry.getValue().blurShaderHorz;
					if (blurShaderHorz != null) {
						blurShaderHorz.deleteShader();
					}
					Shader blurShaderVert = entry.getValue().blurShaderVert;
					if (blurShaderVert != null) {
						blurShaderVert.deleteShader();
					}
				}
			}

			lastBlurUse.keySet().removeAll(remove);
			blurOutput.keySet().removeAll(remove);

			requestedBlurs.clear();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onScreenRender(RenderGameOverlayEvent.Pre event) {
		if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
			processBlurs();
		}
		Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
	}

	@SubscribeEvent
	public void onFogColour(EntityViewRenderEvent.FogColors event) {
		fogColour = 0xff000000;
		fogColour |= ((int) (event.red * 255) & 0xFF) << 16;
		fogColour |= ((int) (event.green * 255) & 0xFF) << 8;
		fogColour |= (int) (event.blue * 255) & 0xFF;
	}

	private static Framebuffer blurOutputHorz = null;

	/**
	 * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
	 * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
	 * <p>
	 * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
	 * apply scales and translations manually.
	 */
	private static Matrix4f createProjectionMatrix(int width, int height) {
		Matrix4f projMatrix = new Matrix4f();
		projMatrix.setIdentity();
		projMatrix.m00 = 2.0F / (float) width;
		projMatrix.m11 = 2.0F / (float) (-height);
		projMatrix.m22 = -0.0020001999F;
		projMatrix.m33 = 1.0F;
		projMatrix.m03 = -1.0F;
		projMatrix.m13 = 1.0F;
		projMatrix.m23 = -1.0001999F;
		return projMatrix;
	}

	private static final double lastBgBlurFactor = -1;

	private static void blurBackground(OutputStuff output, float blurFactor) {
		if (!OpenGlHelper.isFramebufferEnabled() || !OpenGlHelper.areShadersSupported()) return;

		int width = Minecraft.getMinecraft().displayWidth;
		int height = Minecraft.getMinecraft().displayHeight;

		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);

		if (blurOutputHorz == null) {
			blurOutputHorz = new Framebuffer(width, height, false);
			blurOutputHorz.setFramebufferFilter(GL11.GL_NEAREST);
		}
		if (blurOutputHorz == null || output == null) {
			return;
		}
		if (blurOutputHorz.framebufferWidth != width || blurOutputHorz.framebufferHeight != height) {
			blurOutputHorz.createBindFramebuffer(width, height);
			Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
		}

		if (output.blurShaderHorz == null) {
			try {
				output.blurShaderHorz = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
					output.framebuffer, blurOutputHorz
				);
				output.blurShaderHorz.getShaderManager().getShaderUniform("BlurDir").set(1, 0);
				output.blurShaderHorz.setProjectionMatrix(createProjectionMatrix(width, height));
			} catch (Exception ignored) {
			}
		}
		if (output.blurShaderVert == null) {
			try {
				output.blurShaderVert = new Shader(Minecraft.getMinecraft().getResourceManager(), "blur",
					blurOutputHorz, output.framebuffer
				);
				output.blurShaderVert.getShaderManager().getShaderUniform("BlurDir").set(0, 1);
				output.blurShaderVert.setProjectionMatrix(createProjectionMatrix(width, height));
			} catch (Exception ignored) {
			}
		}
		if (output.blurShaderHorz != null && output.blurShaderVert != null) {
			if (output.blurShaderHorz.getShaderManager().getShaderUniform("Radius") == null) {
				//Corrupted shader?
				return;
			}

			output.blurShaderHorz.getShaderManager().getShaderUniform("Radius").set(blurFactor);
			output.blurShaderVert.getShaderManager().getShaderUniform("Radius").set(blurFactor);

			GL11.glPushMatrix();
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Minecraft.getMinecraft().getFramebuffer().framebufferObject);
			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, output.framebuffer.framebufferObject);
			GL30.glBlitFramebuffer(0, 0, width, height,
				0, 0, output.framebuffer.framebufferWidth, output.framebuffer.framebufferHeight,
				GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
			);

			output.blurShaderHorz.loadShader(0);
			output.blurShaderVert.loadShader(0);
			GlStateManager.enableDepth();
			GL11.glPopMatrix();
		}
		Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
	}

	public static void renderBlurredBackground(
		float blurStrength, int screenWidth, int screenHeight,
		int x, int y, int blurWidth, int blurHeight
	) {
		renderBlurredBackground(blurStrength, screenWidth, screenHeight, x, y, blurWidth, blurHeight, false);
	}

	/**
	 * Renders a subsection of the blurred framebuffer on to the corresponding section of the screen.
	 * Essentially, this method will "blur" the background inside the bounds specified by [x->x+blurWidth, y->y+blurHeight]
	 */
	public static void renderBlurredBackground(
		float blurStrength, int screenWidth, int screenHeight,
		int x, int y, int blurWidth, int blurHeight, boolean forcedUpdate
	) {
		if (!OpenGlHelper.isFramebufferEnabled() || !OpenGlHelper.areShadersSupported()) return;
		if (blurStrength < 0.5) return;
		requestedBlurs.add(blurStrength);

		long currentTime = System.currentTimeMillis();
		if (currentTime - lastBlur > 300) {
			shouldBlur = true;
			if (currentTime - lastBlur > 400 && forcedUpdate) return;
		}

		if (blurOutput.isEmpty()) return;

		OutputStuff out = blurOutput.get(blurStrength);
		if (out == null) {
			out = blurOutput.values().iterator().next();
		}

		float uMin = x / (float) screenWidth;
		float uMax = (x + blurWidth) / (float) screenWidth;
		float vMin = (screenHeight - y) / (float) screenHeight;
		float vMax = (screenHeight - y - blurHeight) / (float) screenHeight;

		GlStateManager.depthMask(false);
		Gui.drawRect(x, y, x + blurWidth, y + blurHeight, fogColour);
		out.framebuffer.bindFramebufferTexture();
		GlStateManager.color(1f, 1f, 1f, 1f);
		RenderUtils.drawTexturedRect(x, y, blurWidth, blurHeight, uMin, uMax, vMin, vMax);
		out.framebuffer.unbindFramebufferTexture();
		GlStateManager.depthMask(true);
	}
}
