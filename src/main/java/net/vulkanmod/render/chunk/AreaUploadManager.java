package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.vulkan.*;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import org.apache.commons.lang3.Validate;

import static net.vulkanmod.render.chunk.DrawBuffers.tVirtualBufferIdx;
import static net.vulkanmod.vulkan.queue.Queue.TransferQueue;

import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.queue.TransferQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryBarrier;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;


public class AreaUploadManager {
    public static final int FRAME_NUM = 2;
    public static AreaUploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new AreaUploadManager();
    }

    Queue queue = Device.getTransferQueue();

    ObjectArrayList<AreaBuffer.Segment>[] recordedUploads;
    CommandPool.CommandBuffer[] commandBuffers;

    LongOpenHashSet dstBuffers = new LongOpenHashSet();

    int currentFrame;

    public void init() {
        this.commandBuffers = new CommandPool.CommandBuffer[FRAME_NUM];
        this.recordedUploads = new ObjectArrayList[FRAME_NUM];

        for (int i = 0; i < FRAME_NUM; i++) {
            this.recordedUploads[i] = new ObjectArrayList<>();
        }
    }

    public synchronized void submitUploads() {
        if(this.commandBuffers[currentFrame]==null)
            return;
        var srcStaging = Vulkan.getStagingBuffer(this.currentFrame).getId();
        tVirtualBufferIdx.uploadSubset(srcStaging, this.commandBuffers[currentFrame]);


        queue.submitCommands(this.commandBuffers[currentFrame]);
    }

    public void uploadAsync2(VirtualBuffer virtualBuffer, long bufferId, long dstBufferSize, long dstOffset, long bufferSize, long src) {
        Validate.isTrue(dstOffset<dstBufferSize);

        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = TransferQueue.beginCommands();
//            this.commandBuffers[currentFrame] = GraphicsQueue.getInstance().beginCommands();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(this.currentFrame);
        stagingBuffer.copyBuffer2((int) bufferSize, src);

//        TransferQueue.uploadBufferCmd(this.commandBuffers[currentFrame], stagingBuffer.getId(), stagingBuffer.getOffset(), bufferId, dstOffset, bufferSize);
        final SubCopyCommand k = new SubCopyCommand(stagingBuffer.getOffset(), dstOffset, bufferSize);
//        this.recordedUploads[this.currentFrame].add(k);
        virtualBuffer.addSubCpy(k);
    }

    public void uploadAsync(AreaBuffer.Segment uploadSegment, long bufferId, long dstOffset, long bufferSize, long src) {

        if(commandBuffers[currentFrame] == null)
            this.commandBuffers[currentFrame] = queue.beginCommands();

        VkCommandBuffer commandBuffer = commandBuffers[currentFrame].getHandle();

        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer(this.currentFrame);
        stagingBuffer.copyBuffer2((int) bufferSize, src);

        if(!dstBuffers.add(bufferId)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
                barrier.sType$Default();
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        barrier,
                        null,
                        null);
            }

            dstBuffers.clear();
        }

        TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.getId(), stagingBuffer.getOffset(), bufferId, dstOffset, bufferSize);

        this.recordedUploads[this.currentFrame].add(uploadSegment);
    }

    public void updateFrame() {
        this.currentFrame = (this.currentFrame + 1) % FRAME_NUM;
        waitUploads(this.currentFrame);

        this.dstBuffers.clear();
    }

    void waitUploads() {
        this.waitUploads(currentFrame);
    }
    private void waitUploads(int frame) {
        CommandPool.CommandBuffer commandBuffer = commandBuffers[frame];
        if(commandBuffer == null)
            return;
        Synchronization.waitFence(commandBuffers[frame].getFence());

        for(AreaBuffer.Segment uploadSegment : this.recordedUploads[frame]) {
            uploadSegment.setReady();
        }

        this.commandBuffers[frame].reset();
        this.commandBuffers[frame] = null;
        this.recordedUploads[frame].clear();
    }

    public synchronized void waitAllUploads() {
        for(int i = 0; i < this.commandBuffers.length; ++i) {
            waitUploads(i);
        }
    }

}
