#version 150

uniform mat4 projection;

uniform vec2 size;

in vec2 posIN;

in vec2 texIN;

out vec2 texCoord;

void main(void) {
	gl_Position = projection * vec4(posIN * size, 0.0, 1.0);
	texCoord = texIN;
}