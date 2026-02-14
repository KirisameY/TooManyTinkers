#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0; // GrayScale / FullTex
uniform sampler2D Sampler2; // LightMap
uniform sampler2D Sampler3; // TexMap

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform vec2 AtlasSize;
uniform vec2 MapSize;

in float vertexDistance;
in vec4 vertex_color;
in vec4 light_map_color;
in vec4 mapping_data; // out data
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in vec4 normal;


out vec4 fragColor;

// 0 - 非测试
// 1 - 覆盖测试
// 2 - UV测试
// 3 - 原始纹理
#define TEST 0

void main() {

    #if TEST == 1

    // 覆盖测试
    if (texture(Sampler0, texCoord0).a < 0.1) {
        discard;
    }
    fragColor = vec4(1.);

    #elif TEST == 2

    // UV测试
    float f_uv16or32 = step(31.5, mod(mapping_data.a * 255, 64));
    float uvSize = 32. - f_uv16or32 * 16.;
    vec2 uv = mod(texCoord0 * AtlasSize, uvSize) / uvSize;
    fragColor = vec4(1., uv, 1.);

    #elif TEST == 3

    // 原始纹理
    vec4 origin = texture(Sampler0, texCoord0);
    if (origin.a < 0.1) {
        discard;
    }
    fragColor = origin;

    #endif

    #if TEST > 0
    return;
    #endif


    vec4 originSample = texture(Sampler0, texCoord0);
    if (originSample.a < 0.1) {
        discard;
    }

    // if does not have:
    // 0x80 0.5    - enable
    // 0x40 0.25   - 3d
    // 0x20 0.125  - UV as 32x
    // 0x10 0.0625 - Texture as 32x

    float flags = mapping_data.a * 255;

    vec4 color;
    if (flags >= 127.5) {
        color = originSample * mapping_data;
    }
    else {
        // get flags
        float f_d1or3 = step(63.5, mod(flags, 128));
        float f_uv16or32 = step(31.5, mod(flags, 64));
        float f_tex16or32 = step(15.5, mod(flags, 32));

        // get rgb info
        vec2 unit = mapping_data.rg * 255.;
        float row = mapping_data.b * 255.; // for 1d

        vec2 uv1d;
        {
            // calc color for 1d
            uv1d = unit * 256. + vec2(originSample.r * 255., row) + vec2(.5, .5);
            uv1d /= MapSize;
        }
        
        vec2 uv3d;
        {
            // calc uv for 3d
            vec2 uv16 = mod(texCoord0 * AtlasSize, 16.) / 16.;
            vec2 uv32 = mod(texCoord0 * AtlasSize, 32.) / 32.;
            float uvScaleOrNot = (1 - f_uv16or32) * f_tex16or32;
            vec2 uv = uv32 * uvScaleOrNot + uv16 * (1 - uvScaleOrNot);

            float grey = originSample.r * 255.;
            vec2 uvg = vec2(floor(grey / 16. + .01), mod(grey, 16.));

            // uv shift for 32x tex
            float unitShiftOrNot = (1 - f_uv16or32) * (1 - f_tex16or32);
            float unitShift = (step(0.5, uv32.y) * 1. + step(0.5, uv32.x) * 2.);
            unit.x += floor((unit.y + unitShift) / (MapSize.y / 16.));
            unit.y += unitShift * unitShiftOrNot;
            unit.y = mod(unit.y, MapSize.y / 16.);

            uv3d = unit * 256. + uvg * 16. + uv * 16.;
            uv3d /= MapSize;
        }

        vec2 uvf = uv1d * f_d1or3 + uv3d * (1 - f_d1or3);
        color = texture(Sampler3, uvf);
    }

    if (color.a < 0.1) {
        discard;
    }

    color *= vertex_color * ColorModulator;
    color *= light_map_color;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

//void main() {
//    vec4 color = texture(Sampler0, texCoord0);
//    if (color.a < 0.1) {
//        discard;
//    }
//    color *= vertex_color * ColorModulator;
//    color *= light_map_color;
//    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
//}
