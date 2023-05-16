package net.coderbot.iris.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.coderbot.iris.gl.IrisRenderSystem;
import net.coderbot.iris.gl.sampler.SamplerLimits;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

public class ShaderStorageBufferHolder {
	private ShaderStorageBuffer[] buffers;
	private boolean destroyed;

	public ShaderStorageBufferHolder(Path packRoot, Int2ObjectArrayMap<IntObjectPair<Optional<String>>> overrides) {
		destroyed = false;
		buffers = new ShaderStorageBuffer[Collections.max(overrides.keySet()) + 1];
		overrides.forEach((index, buffer2) -> {
			int size = buffer2.firstInt();
			if (size > IrisRenderSystem.getVRAM()) {
				throw new OutOfVideoMemoryError("We only have " + toMib(IrisRenderSystem.getVRAM()) + "MiB of RAM to work with, but the pack is requesting " + size + "! Can't continue.");
			}
			int buffer = GlStateManager._glGenBuffers();

			GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, buffer);
			IrisRenderSystem.bufferStorage(GL43C.GL_SHADER_STORAGE_BUFFER, size, 0);
			if (buffer2.second().isPresent()) {
				try {
					String path = buffer2.second().get();
					if (path.startsWith("/")) {
						// NB: This does not guarantee the resulting path is in the shaderpack as a double slash could be used,
						// this just fixes shaderpacks like Continuum 2.0.4 that use a leading slash in texture paths
						path = path.substring(1);
					}

					byte[] input = Files.readAllBytes(packRoot.resolve(path));
					ByteBuffer byteBuffer = MemoryUtil.memAlloc(input.length);
					byteBuffer.put(input);
					byteBuffer.flip();
					IrisRenderSystem.bufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, size, byteBuffer);
					MemoryUtil.memFree(byteBuffer);
				} catch (IOException e) {
					throw new RuntimeException("Can't read SSBO file!", e);
				}
			} else {
				IrisRenderSystem.clearBufferSubData(GL43C.GL_SHADER_STORAGE_BUFFER, GL43C.GL_R8, 0, size, GL43C.GL_RED, GL43C.GL_BYTE, new int[]{0});
			}

			if (index > SamplerLimits.get().getMaxShaderStorageUnits()) {
				throw new IllegalStateException("We don't have enough SSBO units??? (index: " + index + ", max: " + SamplerLimits.get().getMaxShaderStorageUnits());
			}
			IrisRenderSystem.bindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, index, buffer);
			buffers[index] = new ShaderStorageBuffer(buffer, index, size);
		});
		GlStateManager._glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
	}


	private static long toMib(long x) {
		return x / 1024L / 1024L;
	}

	public void setupBuffers() {
		if (destroyed) {
			throw new IllegalStateException("Tried to use destroyed buffer objects");
		}

		for (ShaderStorageBuffer buffer : buffers) {
			buffer.bind();
		}
	}

	public void destroyBuffers() {
		for (ShaderStorageBuffer buffer : buffers) {
			buffer.destroy();
		}
		buffers = null;
		destroyed = true;
	}

	private static class OutOfVideoMemoryError extends RuntimeException {
		public OutOfVideoMemoryError(String s) {
			super(s);
		}
	}
}
