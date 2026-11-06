// StorageESP: camera-relative, screen-space strokes for rounded storage outlines.
#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>

layout(std140) uniform u_MeshBaseBlockPos { ivec3 BaseBlockPos; };
layout(std140) uniform u_DistanceFade { vec4 DistanceRanges; };

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 Normal;
layout(location = 3) in float LineWidth;
layout(location = 0) out vec4 vertexColor;

float fadeRamp(float start, float end, float distance) {
    return start == end ? step(end, distance) : smoothstep(start, end, distance);
}

void main() {
    vec3 relativePos = Position + vec3(BaseBlockPos - CameraBlockPos) + CameraOffset;
    vec4 projected = ProjMat * ModelViewMat * vec4(relativePos, 1.0);
    vec4 directionEnd = ProjMat * ModelViewMat * vec4(relativePos + Normal, 1.0);
    // Do not divide by zero at the camera plane, or normalize a line viewed exactly end-on.
    float startW = abs(projected.w) < 0.00001 ? 0.00001 : projected.w;
    float endW = abs(directionEnd.w) < 0.00001 ? 0.00001 : directionEnd.w;
    vec2 direction = (directionEnd.xy / endW - projected.xy / startW) * ScreenSize;
    float magnitude = length(direction);
    direction = magnitude > 0.00001 ? direction / magnitude : vec2(1.0, 0.0);
    vec2 offset = vec2(-direction.y, direction.x) * LineWidth / max(ScreenSize, vec2(1.0));
    if (offset.x < 0.0) offset = -offset;
    projected.xy += (gl_VertexIndex % 2 == 0 ? offset : -offset) * projected.w;
    gl_Position = projected;
    float distance = length(relativePos);
    float alpha = fadeRamp(DistanceRanges.x, DistanceRanges.y, distance)
        * (1.0 - fadeRamp(DistanceRanges.z, DistanceRanges.w, distance));
    vertexColor = vec4(Color.rgb, Color.a * alpha);
}
