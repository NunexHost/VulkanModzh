package net.vulkanmod.render.chunk;

import net.vulkanmod.render.vertex.TerrainRenderType;

public record virtualSegmentBuffer(int chunkAreaIndex, int renderSectionIndex, int byteOffset, int size_t, TerrainRenderType r) {


}
