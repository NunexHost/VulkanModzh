#version 460

#include "light.glsl"

layout(binding = 0) uniform UniformBufferObject {
   mat4 MVP;
};

layout(push_constant) uniform pushConstant {
    vec3 ChunkOffset;
};

layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
//layout(location = 3) out vec4 normal;

//Compressed Vertex
layout(location = 0) in ivec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in uint UV0;
layout(location = 3) in ivec2 UV2;
//layout(location = 4) in vec3 Normal;

const vec3 POSITION_INV = vec3(1.0 / 1900.0);
const vec4 UNPACK_FACTOR = vec4(127.*16.);
void main() {
    vec4 xyz = fma(unpackSnorm4x8(gl_InstanceIndex),UNPACK_FACTOR,vec4(fma(Position,POSITION_INV,ChunkOffset), 1));
    gl_Position = MVP * xyz;


    vertexColor = Color * sample_lightmap(Sampler2, UV2);

    texCoord0 = unpackUnorm2x16(UV0);

//    normal = MVP * vec4(Normal, 0.0);
}

// //Default Vertex
// //layout(location = 0) in vec3 Position;
// //layout(location = 1) in vec4 Color;
// //layout(location = 2) in vec2 UV0;
// //layout(location = 3) in ivec2 UV2;
// //layout(location = 4) in vec3 Normal;
//
// layout(location = 0) in vec3 Position;
// layout(location = 1) in vec4 Color;
// layout(location = 2) in vec2 UV0;
// layout(location = 3) in ivec2 UV2;
// layout(location = 4) in vec3 Normal;
//
// void main() {
//     gl_Position = MVP * vec4(Position + ChunkOffset, 1.0);
//
//     vertexDistance = length((ModelViewMat * vec4(Position + ChunkOffset, 1.0)).xyz);
//     vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
//     texCoord0 = UV0;
//     //    normal = MVP * vec4(Normal, 0.0);
// }
//
// #endif

//void main() {
//    gl_Position = MVP * vec4(Position + ChunkOffset, 1.0);
//
//    vertexDistance = length((ModelViewMat * vec4(Position + ChunkOffset, 1.0)).xyz);
//    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
//    texCoord0 = UV0;
//    //    normal = MVP * vec4(Normal, 0.0);
//}

