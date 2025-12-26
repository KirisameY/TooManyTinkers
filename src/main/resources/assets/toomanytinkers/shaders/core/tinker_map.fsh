#version 150

uniform sampler2D Sampler0; // 原始灰度部件图
uniform sampler2D Sampler2; // 你的渐变调色板纹理
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor; // 从顶点传来的颜色
in vec2 texCoord0;
in vec2 texCoord2;
in vec4 normal;

out vec4 fragColor;

void main() {
    // 1. 采样灰度图
    vec4 graySample = texture(Sampler0, texCoord0);

    // 如果灰度图完全透明，直接丢弃
    if (graySample.a < 0.1) {
        discard;
    }

    // 2. 获取 Material ID
    // 假设你在 Mixin 里把 ID 放进了 vertexColor.r (红色通道)
    // 或者你通过 TintIndex 传入，Minecraft 会把它转换成 vertexColor
    float materialId = vertexColor.r * 255.0; // 假设传入时是归一化的 float

    // 3. 计算渐变图的 UV
    // 灰度值 graySample.r 决定横轴（渐变进度）
    // materialId 决定纵轴（哪种材料）
    // 假设渐变图是 256x256，每一行是一种材料
    vec2 gradientUV = vec2(graySample.r, (materialId + 0.5) / 256.0);

    // 4. 第二次采样：获取最终颜色
    vec4 finalColor = texture(Sampler2, gradientUV);

    // 5. 应用光照、雾气等标准处理 (简化版)
    vec4 color = finalColor * ColorModulator;

    // 简单的 Alpha 测试
    if (color.a < 0.1) {
        discard;
    }

    fragColor = color;
}