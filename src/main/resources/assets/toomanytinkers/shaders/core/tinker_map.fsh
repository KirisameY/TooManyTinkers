#version 150

uniform sampler2D Sampler0; // GrayScale / FullTex
uniform sampler2D Sampler3; // Map
uniform vec4 ColorModulator;

uniform vec2 AtlasSize;
uniform vec2 MapSize;

in float vertexDistance;
in vec4 vertexColor; // out data
in vec2 texCoord0;
in vec2 texCoord2;
in vec4 normal;

out vec4 fragColor;

void main() {
    vec4 originSample = texture(Sampler0, texCoord0);
    if (originSample.a < 0.05) {
        discard;
    }

    // if does not have:
    // 0.5   - enable
    // 0.25  - 3d
    // 0.125 - UV as 32x
    float flags = vertexColor.a * 255;

    if (flags >= 127.5) {
        fragColor = originSample * ColorModulator;
        return;
    }

    // get flags
    float f_d1or3 = step(63.5, mod(flags, 128));
    float f_uv16or32 = step(31.5, mod(flags, 64));

    // get rgb info
    vec2 unit = vertexColor.rg * 255.;
    float row = vertexColor.b * 255.; // for 1d

    vec4 finalColor;
    if (f_d1or3 > .5) {
        // calc color for 1d
        vec2 uv1d = unit * 256. + vec2(originSample.r * 255., row) + vec2(.5, .5);
        uv1d /= MapSize;
        vec4 color1d = texture(Sampler3, uv1d);
        finalColor = color1d * ColorModulator;
        //finalColor = vec4(vertexColor.rgb, 1) + step(100, color1d);
    }
    else {
        // calc uv for 3d
        float uvSize = 32. - f_uv16or32 * 16.;
        vec2 uv = mod(texCoord0 * AtlasSize, uvSize) / uvSize;

        // todo: sample 2d color
        finalColor = vec4(1, uv, 1) * originSample * ColorModulator;
    }

    //    if (finalColor.a < 0.05) {
    //        discard;
    //    }
    fragColor = finalColor;
    // todo: 没做光影，我得想办法补上
}