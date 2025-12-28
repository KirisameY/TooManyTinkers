#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in ivec2 UV2; // Lightmap
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform vec2 AtlasSize;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertex_color;
out vec4 light_map_color;
out vec4 mapping_data;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec4 normal;


vec4 sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp(uv / 4.0, vec2(0.), vec2(2.)));
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

//    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
    vertex_color = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.));
    light_map_color = texelFetch(Sampler2, UV2 / 16, 0);
    mapping_data = Color;
    texCoord0 = UV0;
    texCoord2 = UV2;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}