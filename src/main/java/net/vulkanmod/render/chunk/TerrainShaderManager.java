package net.vulkanmod.render.chunk;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.chunk.build.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

public abstract class TerrainShaderManager {
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    private static GraphicsPipeline terrainSolidShader;
    private static GraphicsPipeline terrainTranslucentShader;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
//        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }
    private static void createBasicPipelines() {
        String resourcePath = SPIRVUtils.class.getResource("/assets/vulkanmod/shaders/basic/").toExternalForm();
        SPIRVUtils.SPIRV Vert               = compileShaderAbsoluteFile(String.format("%s/%s/%s.vsh", resourcePath, "terrain_direct", "terrain_direct"), SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV FragSolid          = compileShaderAbsoluteFile(String.format("%s/%s/%s.fsh", resourcePath, "terrain_direct", "terrain_direct_solid"), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        SPIRVUtils.SPIRV FragTranslucent    = compileShaderAbsoluteFile(String.format("%s/%s/%s.fsh", resourcePath, "terrain_direct", "terrain_direct_translucent"), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        terrainTranslucentShader    = createPipeline(Vert, FragSolid);
        terrainSolidShader          = createPipeline(Vert, FragTranslucent);
    }

    private static GraphicsPipeline createPipeline(SPIRVUtils.SPIRV Vert, SPIRVUtils.SPIRV Frag) {

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(CustomVertexFormat.COMPRESSED_TERRAIN, String.format("basic/%s/%s", "terrain_direct", "terrain_direct"));
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders2(Vert, Frag);
        return pipelineBuilder.createGraphicsPipeline();
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return renderType==TerrainRenderType.TRANSLUCENT ? terrainSolidShader : terrainTranslucentShader;
    }

    public static GraphicsPipeline getTerrainTranslucentShader() {
        return terrainTranslucentShader;
    }

    public static GraphicsPipeline getTerrainSolidShader() {
        return terrainSolidShader;
    }

    public static void destroyPipelines() {
        terrainSolidShader.cleanUp();
        terrainTranslucentShader.cleanUp();
    }
}
