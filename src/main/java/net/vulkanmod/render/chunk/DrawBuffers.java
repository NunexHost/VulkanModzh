package net.vulkanmod.render.chunk;

import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {

    private static final int VERTEX_SIZE = TerrainShaderManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    public final int index;
    private final Vector3i origin;
    final StaticQueue<DrawBuffers.DrawParameters[]> sectionQueue = new StaticQueue<>(512);
    private boolean allocated = false;
    AreaBuffer SvertexBuffer, TvertexBuffer;
    AreaBuffer indexBuffer;
    public DrawBuffers(int areaIndex, Vector3i origin) {

        this.index = areaIndex;
        this.origin = origin;
    }

    public void allocateBuffers() {
        this.SvertexBuffer = new AreaBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 3145728, VERTEX_SIZE);
        this.TvertexBuffer = new AreaBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, 524288, VERTEX_SIZE);
        this.indexBuffer = new AreaBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, 131072, INDEX_SIZE);

        this.allocated = true;
    }

    public DrawParameters upload(int xOffset, int yOffset, int zOffset, UploadBuffer buffer, DrawParameters drawParameters, TerrainRenderType r) {
        int vertexOffset = drawParameters.vertexOffset;
        int firstIndex = 0;
        drawParameters.baseInstance = encodeSectionOffset(xOffset, yOffset, zOffset);

        if(!buffer.indexOnly) {
            drawParameters.vertexBufferSegment = ((buffer.autoIndices) ? this.SvertexBuffer : this.TvertexBuffer).upload(drawParameters.index, buffer.getVertexBuffer(), buffer.getVertexBuffer().remaining(), drawParameters.vertexBufferSegment,  this.index, r);
//            drawParameters.vertexOffset = drawParameters.vertexBufferSegment.getOffset() / VERTEX_SIZE;
            vertexOffset  = drawParameters.vertexBufferSegment.byteOffset() / VERTEX_SIZE;

            //debug
//            if(drawParameters.vertexBufferSegment.getOffset() % VERTEX_SIZE != 0) {
//                throw new RuntimeException("misaligned vertex buffer");
//            }
        }

        if(!buffer.autoIndices) {
            drawParameters.indexBufferSegment = this.indexBuffer.upload(drawParameters.index, buffer.getIndexBuffer(), buffer.getIndexBuffer().remaining(), drawParameters.indexBufferSegment,  this.index, r);;
//            drawParameters.firstIndex = drawParameters.indexBufferSegment.getOffset() / INDEX_SIZE;
            firstIndex = drawParameters.indexBufferSegment.byteOffset() / INDEX_SIZE;
        }

//        AreaUploadManager.INSTANCE.enqueueParameterUpdate(
//                new ParametersUpdate(drawParameters, buffer.indexCount, firstIndex, vertexOffset));

        drawParameters.indexCount = buffer.indexCount;
        drawParameters.firstIndex = firstIndex;
        drawParameters.vertexOffset = vertexOffset;



        buffer.release();

        return drawParameters;
    }

    private static int encodeSectionOffset(int xOffset, int yOffset, int zOffset) {
        final int xOffset1 = (xOffset & 127)>>4;
        final int zOffset1 = (zOffset & 127)>>4;
        final int yOffset1 = yOffset >> 4;
        return zOffset1 << 16 | yOffset1 << 8 | xOffset1;
    }

    public void buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, TerrainRenderType terrainRenderType, double camX, double camY, double camZ, long layout) {
        int stride = 20;
        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

        int drawCount = 0;


        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = stack.malloc(20 * sectionQueue.size());

            long bufferPtr = MemoryUtil.memAddress0(byteBuffer);

            VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
            if (isTranslucent) {
                vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
            }
            updateChunkAreaOrigin(camX, camY, camZ, commandBuffer, stack.nmalloc(16), layout);
            var iterator = sectionQueue.iterator(isTranslucent);
            while (iterator.hasNext()) {
                DrawParameters drawParameters = iterator.next()[terrainRenderType.ordinal()];

                long ptr = bufferPtr + (drawCount * 20L);
                VUtil.UNSAFE.putInt(ptr, drawParameters.indexCount);
                VUtil.UNSAFE.putInt(ptr + 4, 1);
                VUtil.UNSAFE.putInt(ptr + 8, drawParameters.firstIndex);
                VUtil.UNSAFE.putInt(ptr + 12, drawParameters.vertexOffset);
                VUtil.UNSAFE.putInt(ptr + 16, drawParameters.baseInstance);

                drawCount++;
            }

            byteBuffer.position(0);

            indirectBuffer.recordCopyCmd(byteBuffer);

            long id = stack.npointer((isTranslucent ? TvertexBuffer : SvertexBuffer).getId());
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, id, (stack.npointer(0)));

//            pipeline.bindDescriptorSets(Drawer.getCommandBuffer(), WorldRenderer.getInstance().getUniformBuffers(), Drawer.getCurrentFrame());
//        pipeline.bindDescriptorSets(Renderer.getCommandBuffer(), Renderer.getCurrentFrame());
            vkCmdDrawIndexedIndirect(commandBuffer, indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, stride);
        }

//            fakeIndirectCmd(Drawer.getCommandBuffer(), indirectBuffer, drawCount, uboBuffer);

//        MemoryUtil.memFree(byteBuffer);

    }

    private void updateChunkAreaOrigin(double camX, double camY, double camZ, VkCommandBuffer commandBuffer, long ptr, long layout) {
        VUtil.UNSAFE.putFloat(ptr + 0, (float) (this.origin.x - camX));
        VUtil.UNSAFE.putFloat(ptr + 4, (float) -camY);
        VUtil.UNSAFE.putFloat(ptr + 8, (float) (this.origin.z - camZ));

        nvkCmdPushConstants(commandBuffer, layout, VK_SHADER_STAGE_VERTEX_BIT, 0, 12, ptr);
    }

    public void buildDrawBatchesDirect(TerrainRenderType terrainRenderType, double camX, double camY, double camZ, long layout) {


        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        try(MemoryStack stack = MemoryStack.stackPush()) {
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.npointer((isTranslucent ? TvertexBuffer : SvertexBuffer).getId()), stack.npointer(0));
            updateChunkAreaOrigin(camX, camY, camZ, commandBuffer, stack.nmalloc(16), layout);
        }

        if(isTranslucent) {
            vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }


        for (Iterator<DrawBuffers.DrawParameters[]> iter = sectionQueue.iterator(isTranslucent); iter.hasNext(); ) {
            DrawParameters drawParameters = iter.next()[terrainRenderType.ordinal()];

            vkCmdDrawIndexed(commandBuffer, drawParameters.indexCount, 1, drawParameters.firstIndex, drawParameters.vertexOffset, drawParameters.baseInstance);

        }


    }
    /*Fixes slow VAF (VertexAttributeFetch) Memory Latency hardware bugs For old pre Turing Nvidia GPUs: (Due to a high memory latency/memory subsystem flaw)
        * This Boosts Perf by roughly 30%+ on GTX 1000 series cards, but causes very high CPU overhead,
        * This Hardware Performance bug is not applicable to AMD or Nvidia RTX 2000+ Cards as they use much better designed memory subsystems,
        * So this is primarily for debugging purposes only atm
   /* public void FastVertexAttributeFetch(TerrainRenderType terrainRenderType) {


            long npointer = stack.npointer((isTranslucent ? TvertexBuffer : SvertexBuffer).getId());
            long npointer1 = stack.nmalloc(Pointer.POINTER_SIZE);
            for (Iterator<DrawBuffers.DrawParameters[]> iter = sectionQueue.iterator(isTranslucent); iter.hasNext(); ) {
                DrawParameters drawParameters = iter.next()[terrainRenderType.ordinal()];
                VUtil.UNSAFE.putLong(npointer1, drawParameters.vertexOffset * 20L);
                nvkCmdBindVertexBuffers(commandBuffer, 0, 1, npointer, npointer1);
                vkCmdDrawIndexed(commandBuffer, drawParameters.indexCount, 1, drawParameters.firstIndex, 0, drawParameters.baseInstance);

            }
        }


    }*/

    public void releaseBuffers() {
        if(!this.allocated)
            return;

        this.SvertexBuffer.freeBuffer();
        this.TvertexBuffer.freeBuffer();
        this.indexBuffer.freeBuffer();

        this.SvertexBuffer = null;
        this.TvertexBuffer = null;
        this.indexBuffer = null;
        this.allocated = false;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public static class DrawParameters {
        private final int index;
        int indexCount;
        int firstIndex;
        int vertexOffset;
        int baseInstance;
        virtualSegmentBuffer vertexBufferSegment;
        virtualSegmentBuffer indexBufferSegment;

        DrawParameters(int index) {
            this.index=index;
        }

        public void reset(ChunkArea chunkArea) {
            this.indexCount = 0;
            this.firstIndex = 0;
            this.vertexOffset = 0;

            if(chunkArea != null && chunkArea.drawBuffers.isAllocated() && this.vertexBufferSegment != null) {
//                this.chunkArea.drawBuffers.vertexBuffer.setSegmentFree(segmentOffset);
                if(this.indexBufferSegment==null) chunkArea.drawBuffers.SvertexBuffer.setSegmentFree(this.vertexBufferSegment.renderSectionIndex());
                else
                {
                    chunkArea.drawBuffers.TvertexBuffer.setSegmentFree(this.vertexBufferSegment.renderSectionIndex());
                    chunkArea.drawBuffers.indexBuffer.setSegmentFree(this.indexBufferSegment.renderSectionIndex());
                }
            }
        }
    }

}
